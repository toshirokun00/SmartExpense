package com.smartexpense.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The bills one paycheck is planned to cover.
 *
 * [dueByBillId] pins the cycle each planned bill belongs to. Paying a bill moves
 * its stored due date forward, so the cycle is resolved once here and reused for
 * display: a paid bill keeps its row on the paycheck that covered it and only
 * its status changes.
 */
data class PaycheckPlan(
    val billIds: List<Long> = emptyList(),
    val dueByBillId: Map<Long, Long> = emptyMap()
)

/**
 * Works out the bills every paycheck covers, in pay-date order.
 *
 * Each cutoff owns the window from its pay date up to the next cutoff's pay
 * date. A 15th cutoff therefore covers the bills due Nov 15-29 and the
 * end-of-month cutoff covers Nov 30-Dec 14; a bill due on a payday belongs to
 * that paycheck. The first cutoff also claims every overdue cycle, and the last
 * one extends by the previous period so it still covers a full window.
 *
 * Saved assignments win. A cutoff with saved bill ids covers exactly those
 * bills, pinned to the first cycle it can claim, so a bill the user unchecked
 * is not picked up again by a later cutoff. A cutoff with nothing saved is
 * filled with the unclaimed cycles inside its window.
 *
 * Cycles are counted from the first unsettled cycle, so paying a bill does not
 * shrink the plan: the settled cycle stays attached to the cutoff that covered
 * it and the next cutoff takes the following cycle. [paymentsByBill] holds the
 * payment timestamps per bill, used to recover those cycles after the row has
 * advanced. Each bill appears at most once per cutoff.
 */
fun planPaychecks(
    paychecks: List<Paycheck>,
    bills: List<Bill>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    paymentsByBill: Map<Long, List<Long>> = emptyMap()
): Map<Long, PaycheckPlan> {
    val ordered = paychecks
        .filter { !it.isSkipped }
        .sortedBy { it.payDateMillis }
    if (ordered.isEmpty()) return emptyMap()

    val payDates = ordered.map { it.payDate(zoneId) }
    val windows = payDates.mapIndexed { index, payDate ->
        CutoffWindow(
            start = if (index == 0) null else payDate,
            end = windowsEnd(payDates, index)
        )
    }
    val horizon = windows.last().end

    val billsById = bills.associateBy { it.id }
    val activeBills = bills.filter { it.isActive && !it.skipped }
    val cyclesByBill = activeBills.associate { bill ->
        bill.id to billCycles(
            bill = bill,
            horizon = horizon,
            paidAtMillis = paymentsByBill[bill.id].orEmpty(),
            zoneId = zoneId
        )
    }

    val claimed = mutableMapOf<Long, MutableSet<Long>>()

    fun isClaimed(cycle: BillCycle): Boolean =
        claimed[cycle.billId]?.contains(cycle.dueMillis) == true

    fun claim(cycle: BillCycle) {
        claimed.getOrPut(cycle.billId) { mutableSetOf() } += cycle.dueMillis
    }

    // Saved assignments claim their cycle first, in pay-date order, so a bill
    // moved to an earlier cutoff is not planned by an auto cutoff as well.
    val pinsByCutoff = ordered.mapIndexed { index, paycheck ->
        val window = windows[index]
        val dueByBillId = LinkedHashMap<Long, Long>()
        for (billId in paycheck.billIds) {
            if (billId in dueByBillId) continue
            val cycles = cyclesByBill[billId].orEmpty()
            // Prefer a cycle inside this cutoff's own window. Claiming an
            // earlier cycle first would let a later cutoff steal it from the
            // paycheck that actually covers that month.
            val cycle = cycles.firstOrNull { candidate ->
                !isClaimed(candidate) &&
                    (window.start == null || !candidate.date.isBefore(window.start)) &&
                    candidate.date.isBefore(window.end)
            } ?: cycles.firstOrNull { candidate ->
                !isClaimed(candidate) &&
                    (window.start == null || !candidate.date.isBefore(window.start))
            } ?: billsById[billId]?.let { bill ->
                BillCycle(
                    billId = bill.id,
                    date = Instant.ofEpochMilli(paycheck.payDateMillis)
                        .atZone(zoneId)
                        .toLocalDate(),
                    dueMillis = coveredOccurrenceDueMillis(
                        bill = bill,
                        payDateMillis = paycheck.payDateMillis,
                        paidAtMillis = paymentsByBill[bill.id].orEmpty(),
                        zoneId = zoneId
                    )
                )
            }
            if (cycle != null) {
                claim(cycle)
                dueByBillId[billId] = cycle.dueMillis
            }
        }
        dueByBillId
    }

    val plans = mutableMapOf<Long, PaycheckPlan>()
    for ((index, paycheck) in ordered.withIndex()) {
        if (paycheck.billIds.isNotEmpty()) {
            plans[paycheck.id] = PaycheckPlan(
                billIds = paycheck.billIds.distinct(),
                dueByBillId = pinsByCutoff[index]
            )
            continue
        }

        val window = windows[index]
        val cycles = activeBills.mapNotNull { bill ->
            cyclesByBill[bill.id].orEmpty().firstOrNull { candidate ->
                !isClaimed(candidate) &&
                    (window.start == null || !candidate.date.isBefore(window.start)) &&
                    candidate.date.isBefore(window.end)
            }?.also { cycle -> claim(cycle) }
        }.sortedWith(compareBy({ it.date }, { it.billId }))

        plans[paycheck.id] = PaycheckPlan(
            billIds = cycles.map { it.billId },
            dueByBillId = cycles.associate { it.billId to it.dueMillis }
        )
    }
    return plans
}

/** One cutoff's coverage window: [start] inclusive, [end] exclusive. */
private data class CutoffWindow(
    /** Null on the first cutoff, which also claims every overdue cycle. */
    val start: LocalDate?,
    val end: LocalDate
)

/** One occurrence of a bill, normalized to a local calendar date. */
private data class BillCycle(
    val billId: Long,
    val date: LocalDate,
    val dueMillis: Long
)

/**
 * The end of the window owned by the cutoff at [index]: the next pay date, or
 * for the last cutoff the same length as the period before it.
 */
private fun windowsEnd(payDates: List<LocalDate>, index: Int): LocalDate {
    val payDate = payDates[index]
    payDates.getOrNull(index + 1)?.let { return it }

    val previous = payDates.getOrNull(index - 1) ?: return payDate.plusMonths(1)
    return payDate.plusDays(ChronoUnit.DAYS.between(previous, payDate))
}

/**
 * The cycles of [bill] from its first unsettled one up to [horizon], which is
 * excluded. One-time bills have a single cycle.
 */
private fun billCycles(
    bill: Bill,
    horizon: LocalDate,
    paidAtMillis: List<Long>,
    zoneId: ZoneId
): List<BillCycle> {
    val firstMillis = earliestUnsettledCycleMillis(bill, paidAtMillis, zoneId)
    var date = Instant.ofEpochMilli(firstMillis).atZone(zoneId).toLocalDate()
    if (!date.isBefore(horizon)) return emptyList()

    val cycles = mutableListOf(BillCycle(bill.id, date, firstMillis))
    var guard = 0
    while (guard < MAX_PLANNED_CYCLES) {
        val nextMillis = bill.nextDueDateMillis(zoneId, from = date) ?: break
        val next = Instant.ofEpochMilli(nextMillis).atZone(zoneId).toLocalDate()
        if (!next.isBefore(horizon)) break
        cycles += BillCycle(bill.id, next, nextMillis)
        date = next
        guard++
    }
    return cycles
}

/** Safety bound when walking a bill's cycles forward. */
private const val MAX_PLANNED_CYCLES = 600
