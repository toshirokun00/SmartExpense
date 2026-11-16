package com.smartexpense.domain

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.coveredOccurrenceDueMillis
import com.smartexpense.domain.model.isOccurrencePaid
import com.smartexpense.domain.model.planPaychecks
import com.smartexpense.domain.model.visiblePaychecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PaycheckTest {

    private val zone = ZoneOffset.UTC

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    private fun bill(
        id: Long,
        dueDate: Long,
        amount: Double = 100.0,
        paidAtMillis: Long? = null,
        skipped: Boolean = false,
        isActive: Boolean = true
    ) = Bill(
        id = id,
        name = "Bill $id",
        amount = amount,
        dueDateMillis = dueDate,
        paidAtMillis = paidAtMillis,
        skipped = skipped,
        isActive = isActive
    )

    @Test
    fun `covered cycle stays on the paycheck after the bill is paid`() {
        // Spotify: monthly, due Nov 15 and covered by the Nov 15 paycheck.
        // Paying it advances the row to Dec 15 and clears the paid flag.
        val spotify = Bill(
            id = 4,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 12, 15),
            frequency = BillFrequency.MONTHLY
        )

        val occurrence = coveredOccurrenceDueMillis(
            bill = spotify,
            payDateMillis = millis(2026, 11, 15),
            paidAtMillis = listOf(millis(2026, 11, 20)),
            zoneId = zone
        )

        assertEquals(millis(2026, 11, 15), occurrence)
        assertTrue(isOccurrencePaid(spotify, occurrence))
    }

    @Test
    fun `an unpaid covered cycle shows its own due date`() {
        val spotify = Bill(
            id = 4,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )

        val occurrence = coveredOccurrenceDueMillis(
            bill = spotify,
            payDateMillis = millis(2026, 11, 15),
            zoneId = zone
        )

        assertEquals(millis(2026, 11, 15), occurrence)
        assertFalse(isOccurrencePaid(spotify, occurrence))
    }

    @Test
    fun `a due date with a time of day is not mistaken for a settled cycle`() {
        // The bill form defaults to "now", so a bill due today can carry a time
        // of day while the resolved cycle is local midnight. Comparing raw
        // millis used to flag that current cycle as paid with no payments.
        val spotify = Bill(
            id = 4,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15) + (16 * 60 + 26) * 60_000L,
            frequency = BillFrequency.MONTHLY
        )

        val occurrence = coveredOccurrenceDueMillis(
            bill = spotify,
            payDateMillis = millis(2026, 11, 15),
            zoneId = zone
        )

        assertEquals(millis(2026, 11, 15), occurrence)
        assertFalse(isOccurrencePaid(spotify, occurrence, zone))
    }

    @Test
    fun `a one-time unpaid bill due later today stays unpaid`() {
        val meralco = Bill(
            id = 5,
            name = "Meralco",
            amount = 4_000.0,
            dueDateMillis = millis(2026, 11, 20) + 15 * 60 * 60_000L
        )

        assertFalse(isOccurrencePaid(meralco, meralco.dueDateMillis, zone))
    }

    @Test
    fun `a later paycheck covers the next cycle of a paid recurring bill`() {
        val spotify = Bill(
            id = 4,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 12, 15),
            frequency = BillFrequency.MONTHLY
        )

        val occurrence = coveredOccurrenceDueMillis(
            bill = spotify,
            payDateMillis = millis(2026, 12, 15),
            paidAtMillis = listOf(millis(2026, 11, 20)),
            zoneId = zone
        )

        assertEquals(millis(2026, 12, 15), occurrence)
        assertFalse(isOccurrencePaid(spotify, occurrence))
    }

    @Test
    fun `a monthly bill shows the cycle of every month's paycheck`() {
        val spotify = Bill(
            id = 4,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )

        assertEquals(
            millis(2026, 11, 15),
            coveredOccurrenceDueMillis(spotify, millis(2026, 11, 15), zoneId = zone)
        )
        assertEquals(
            millis(2026, 12, 15),
            coveredOccurrenceDueMillis(spotify, millis(2026, 12, 15), zoneId = zone)
        )
        assertEquals(
            millis(2027, 2, 15),
            coveredOccurrenceDueMillis(spotify, millis(2027, 2, 15), zoneId = zone)
        )
        assertFalse(
            isOccurrencePaid(spotify, coveredOccurrenceDueMillis(spotify, millis(2027, 2, 15), zoneId = zone))
        )
    }

    @Test
    fun `a payment recorded before the pay date still pins the settled cycle`() {
        // Meralco's November cycle was paid on Nov 11, which advanced the row
        // to Dec 9. The Nov 29 cutoff still shows the cycle that was settled
        // instead of pretending nothing was paid.
        val meralco = Bill(
            id = 1,
            name = "Meralco",
            amount = 4750.0,
            dueDateMillis = millis(2026, 12, 9),
            frequency = BillFrequency.MONTHLY
        )

        val occurrence = coveredOccurrenceDueMillis(
            bill = meralco,
            payDateMillis = millis(2026, 11, 29),
            paidAtMillis = listOf(millis(2026, 11, 11)),
            zoneId = zone
        )

        assertEquals(millis(2026, 11, 9), occurrence)
        assertTrue(isOccurrencePaid(meralco, occurrence))
    }

    @Test
    fun `one-time bills keep their stored due date and paid state`() {
        val unpaid = Bill(
            id = 1,
            name = "Rent",
            amount = 500.0,
            dueDateMillis = millis(2026, 11, 10)
        )
        val paid = unpaid.copy(paidAtMillis = millis(2026, 11, 10))

        val occurrence = coveredOccurrenceDueMillis(
            bill = unpaid,
            payDateMillis = millis(2026, 11, 15),
            zoneId = zone
        )

        assertEquals(millis(2026, 11, 10), occurrence)
        assertFalse(isOccurrencePaid(unpaid, occurrence))
        assertTrue(isOccurrencePaid(paid, occurrence))
    }

    @Test
    fun `paycheck totals only count assigned bills`() {
        val bills = listOf(
            bill(id = 1, dueDate = 0L, amount = 500.0),
            bill(id = 2, dueDate = 0L, amount = 300.0),
            bill(id = 3, dueDate = 0L, amount = 999.0)
        )
        val paycheck = Paycheck(
            id = 7,
            amount = 1_000.0,
            payDateMillis = millis(2026, 11, 15),
            label = "",
            billIds = listOf(1, 2)
        )

        assertEquals(listOf(1L, 2L), paycheck.billsCovered(bills).map { it.id })
        assertEquals(800.0, paycheck.assignedTotal(bills), 0.001)
        assertEquals(200.0, paycheck.remainingAfterBills(bills), 0.001)
    }

    @Test
    fun `display label falls back when blank`() {
        val paycheck = Paycheck(amount = 1.0, payDateMillis = 0L, label = "  ")
        assertEquals("Paycheck", paycheck.displayLabel())

        val labelled = paycheck.copy(label = "1st salary")
        assertEquals("1st salary", labelled.displayLabel())
    }

    @Test
    fun `visible paychecks include all past months and the next month`() {
        val now = millis(2026, 11, 13)
        val paychecks = listOf(
            paycheckOn(2026, 9, 30),
            paycheckOn(2026, 10, 31),
            paycheckOn(2026, 11, 1),
            paycheckOn(2026, 11, 30),
            paycheckOn(2026, 12, 1),
            paycheckOn(2026, 12, 31),
            paycheckOn(2027, 1, 1)
        )

        val visible = visiblePaychecks(paychecks, now, zone)

        assertEquals(
            listOf(
                millis(2026, 9, 30),
                millis(2026, 10, 31),
                millis(2026, 11, 1),
                millis(2026, 11, 30),
                millis(2026, 12, 1),
                millis(2026, 12, 31)
            ),
            visible.map { it.payDateMillis }
        )
    }

    @Test
    fun `visible paychecks cross the year boundary correctly`() {
        val now = millis(2026, 12, 20)
        val paychecks = listOf(
            paycheckOn(2026, 12, 31),
            paycheckOn(2027, 1, 15),
            paycheckOn(2027, 1, 31),
            paycheckOn(2027, 2, 1)
        )

        val visible = visiblePaychecks(paychecks, now, zone)

        assertEquals(
            listOf(
                millis(2026, 12, 31),
                millis(2027, 1, 15),
                millis(2027, 1, 31)
            ),
            visible.map { it.payDateMillis }
        )
    }

    @Test
    fun `a recurring bill shows its own cycle on each cutoff`() {
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = listOf(
            Paycheck(
                id = 1,
                amount = 1_000.0,
                payDateMillis = millis(2026, 11, 15),
                scheduleClientId = "schedule-1"
            ),
            Paycheck(
                id = 2,
                amount = 1_000.0,
                payDateMillis = millis(2026, 12, 15),
                scheduleClientId = "schedule-1"
            )
        )

        val plans = planPaychecks(paychecks, listOf(spotify), zone)

        assertEquals(listOf(1L), plans[1L]?.billIds)
        assertEquals(millis(2026, 11, 15), plans[1L]?.dueByBillId?.get(1L))
        assertEquals(listOf(1L), plans[2L]?.billIds)
        assertEquals(millis(2026, 12, 15), plans[2L]?.dueByBillId?.get(1L))
    }

    @Test
    fun `a one-time bill is not carried to the next cutoff`() {
        val rent = bill(id = 1, dueDate = millis(2026, 11, 15))
        val paychecks = listOf(
            Paycheck(
                id = 1,
                amount = 1_000.0,
                payDateMillis = millis(2026, 11, 15),
                scheduleClientId = "schedule-1"
            ),
            Paycheck(
                id = 2,
                amount = 1_000.0,
                payDateMillis = millis(2026, 12, 15),
                scheduleClientId = "schedule-1"
            )
        )

        val plans = planPaychecks(paychecks, listOf(rent), zone)

        assertEquals(listOf(1L), plans[1L]?.billIds)
        assertEquals(emptyList<Long>(), plans[2L]?.billIds)
    }

    @Test
    fun `a paid bill stays on the cutoff that covered it`() {
        // Spotify: monthly, due Nov 15. Paying it advances the row to Dec 15
        // and clears the paid flag, but the Nov 15 cutoff must keep the bill.
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 12, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = listOf(
            Paycheck(
                id = 1,
                amount = 1_000.0,
                payDateMillis = millis(2026, 11, 15),
                scheduleClientId = "schedule-1"
            ),
            Paycheck(
                id = 2,
                amount = 1_000.0,
                payDateMillis = millis(2026, 12, 15),
                scheduleClientId = "schedule-1"
            )
        )
        val payments = mapOf(1L to listOf(millis(2026, 11, 15)))

        val plans = planPaychecks(paychecks, listOf(spotify), zone, payments)

        assertEquals(listOf(1L), plans[1L]?.billIds)
        assertEquals(millis(2026, 11, 15), plans[1L]?.dueByBillId?.get(1L))
        assertTrue(isOccurrencePaid(spotify, plans[1L]?.dueByBillId?.getValue(1L) ?: 0L))

        // The next cutoff still shows its own December cycle, unpaid.
        assertEquals(listOf(1L), plans[2L]?.billIds)
        assertEquals(millis(2026, 12, 15), plans[2L]?.dueByBillId?.get(1L))
        assertFalse(isOccurrencePaid(spotify, plans[2L]?.dueByBillId?.getValue(1L) ?: 0L))
    }

    @Test
    fun `a manual paycheck still covers the bills in its window`() {
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = listOf(
            Paycheck(
                id = 1,
                amount = 1_000.0,
                payDateMillis = millis(2026, 11, 15),
                scheduleClientId = "schedule-1"
            ),
            Paycheck(
                id = 2,
                amount = 1_000.0,
                payDateMillis = millis(2026, 12, 15)
            )
        )

        val plans = planPaychecks(paychecks, listOf(spotify), zone)

        assertEquals(listOf(1L), plans[1L]?.billIds)
        // The December cycle falls inside the December window, so this manual
        // cutoff covers it too.
        assertEquals(listOf(1L), plans[2L]?.billIds)
        assertEquals(millis(2026, 12, 15), plans[2L]?.dueByBillId?.get(1L))
    }

    @Test
    fun `unchecking a recurring bill keeps the next month on its own cycle`() {
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val internet = Bill(
            id = 2,
            name = "Internet",
            amount = 300.0,
            dueDateMillis = millis(2026, 12, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = listOf(
            Paycheck(
                id = 1,
                amount = 1_000.0,
                payDateMillis = millis(2026, 11, 15),
                scheduleClientId = "schedule-1"
            ),
            // December keeps only the internet bill, so Spotify stops there.
            Paycheck(
                id = 2,
                amount = 1_000.0,
                payDateMillis = millis(2026, 12, 15),
                billIds = listOf(2L),
                scheduleClientId = "schedule-1"
            ),
            Paycheck(
                id = 3,
                amount = 1_000.0,
                payDateMillis = millis(2027, 1, 15),
                scheduleClientId = "schedule-1"
            )
        )

        val plans = planPaychecks(paychecks, listOf(spotify, internet), zone)

        assertEquals(listOf(1L), plans[1L]?.billIds)
        assertEquals(listOf(2L), plans[2L]?.billIds)
        // Spotify is back on January's own cycle, not December's.
        assertEquals(listOf(1L, 2L), plans[3L]?.billIds)
        assertEquals(millis(2027, 1, 15), plans[3L]?.dueByBillId?.get(1L))
    }

    @Test
    fun `each cutoff covers the bills due before the next pay date`() {
        val bills = listOf(
            // Overdue bills join the first cutoff.
            bill(id = 1, dueDate = millis(2026, 11, 14)),
            // A bill due on payday belongs to that paycheck.
            bill(id = 2, dueDate = millis(2026, 11, 15)),
            bill(id = 3, dueDate = millis(2026, 11, 29)),
            bill(id = 4, dueDate = millis(2026, 11, 30)),
            bill(id = 5, dueDate = millis(2026, 12, 14))
        )
        val paychecks = listOf(
            Paycheck(id = 1, amount = 1_000.0, payDateMillis = millis(2026, 11, 15)),
            Paycheck(id = 2, amount = 1_000.0, payDateMillis = millis(2026, 11, 30)),
            Paycheck(id = 3, amount = 1_000.0, payDateMillis = millis(2026, 12, 15))
        )

        val plans = planPaychecks(paychecks, bills, zone)

        assertEquals(listOf(1L, 2L, 3L), plans[1L]?.billIds)
        assertEquals(listOf(4L, 5L), plans[2L]?.billIds)
        assertEquals(emptyList<Long>(), plans[3L]?.billIds)
    }

    @Test
    fun `the last cutoff covers the same period as the one before it`() {
        // No January cutoff is stored yet, so the Nov 30 paycheck still covers
        // the fifteen days it usually would.
        val bills = listOf(
            bill(id = 1, dueDate = millis(2026, 12, 14)),
            bill(id = 2, dueDate = millis(2026, 12, 15))
        )
        val paychecks = listOf(
            Paycheck(id = 1, amount = 1_000.0, payDateMillis = millis(2026, 11, 15)),
            Paycheck(id = 2, amount = 1_000.0, payDateMillis = millis(2026, 11, 30))
        )

        val plans = planPaychecks(paychecks, bills, zone)

        assertEquals(listOf(1L), plans[2L]?.billIds)
    }

    @Test
    fun `a saved cutoff keeps an unchecked bill off the next cutoff`() {
        val unchecked = bill(id = 1, dueDate = millis(2026, 11, 20))
        val saved = bill(id = 2, dueDate = millis(2026, 11, 20))
        val paychecks = listOf(
            Paycheck(
                id = 1,
                amount = 1_000.0,
                payDateMillis = millis(2026, 11, 15),
                billIds = listOf(2L)
            ),
            Paycheck(id = 2, amount = 1_000.0, payDateMillis = millis(2026, 11, 30))
        )

        val plans = planPaychecks(paychecks, listOf(unchecked, saved), zone)

        assertEquals(listOf(2L), plans[1L]?.billIds)
        assertEquals(emptyList<Long>(), plans[2L]?.billIds)
    }

    @Test
    fun `a bill covered early is not planned again by the next cutoff`() {
        val internet = bill(id = 1, dueDate = millis(2026, 12, 10))
        val paychecks = listOf(
            Paycheck(
                id = 1,
                amount = 1_000.0,
                payDateMillis = millis(2026, 11, 15),
                billIds = listOf(1L)
            ),
            Paycheck(id = 2, amount = 1_000.0, payDateMillis = millis(2026, 11, 30))
        )

        val plans = planPaychecks(paychecks, listOf(internet), zone)

        assertEquals(millis(2026, 12, 10), plans[1L]?.dueByBillId?.get(1L))
        assertEquals(emptyList<Long>(), plans[2L]?.billIds)
    }

    @Test
    fun `a saved cutoff does not steal an earlier cutoff's cycle`() {
        val maynilad = Bill(
            id = 1,
            name = "Maynilad",
            amount = 500.0,
            dueDateMillis = millis(2026, 11, 30),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = listOf(
            Paycheck(id = 1, amount = 1_000.0, payDateMillis = millis(2026, 11, 30)),
            Paycheck(id = 2, amount = 1_000.0, payDateMillis = millis(2026, 12, 15)),
            Paycheck(
                id = 3,
                amount = 1_000.0,
                payDateMillis = millis(2026, 12, 30),
                billIds = listOf(1L)
            )
        )

        val plans = planPaychecks(paychecks, listOf(maynilad), zone)

        // The December cutoff covers December's cycle; November's stays on the
        // November paycheck.
        assertEquals(millis(2026, 11, 30), plans[1L]?.dueByBillId?.get(1L))
        assertEquals(emptyList<Long>(), plans[2L]?.billIds)
        assertEquals(millis(2026, 12, 30), plans[3L]?.dueByBillId?.get(1L))
    }

    private fun paycheckOn(year: Int, month: Int, day: Int) = Paycheck(
        amount = 1_000.0,
        payDateMillis = millis(year, month, day)
    )
}
