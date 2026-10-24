package com.smartexpense.domain

import com.smartexpense.domain.export.ReportCsvFormatter
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReportCsvFormatterTest {

    private val zoneId = ZoneId.of("UTC")
    private val formatter = ReportCsvFormatter(zoneId)

    @Test
    fun transactionsWriteHeadersAndColumnsInOrder() {
        val csv = formatter.transactions(
            expenses = listOf(expense(id = 7L, amount = 12.3)),
            categoryNames = mapOf(1L to "Food")
        )

        assertEquals(
            "transaction_id,expense_date,category_name,description,amount,currency\r\n" +
                "7,2026-01-15,Food,Lunch,12.30,PHP",
            csv
        )
    }

    @Test
    fun transactionsEscapeCommasQuotesAndMultilineDescriptions() {
        val csv = formatter.transactions(
            expenses = listOf(
                expense(
                    id = 8L,
                    amount = 5.0,
                    description = "Lunch, \"special\"\nwith team"
                )
            ),
            categoryNames = mapOf(1L to "Food, Dining")
        )

        assertEquals(
            "transaction_id,expense_date,category_name,description,amount,currency\r\n" +
                "8,2026-01-15,\"Food, Dining\",\"Lunch, \"\"special\"\"\nwith team\",5.00,PHP",
            csv
        )
    }

    @Test
    fun transactionsNeutralizeSpreadsheetFormulaFields() {
        val csv = formatter.transactions(
            expenses = listOf(
                expense(
                    id = 1L,
                    amount = 1.0,
                    description = "=SUM(A1:A2)"
                ),
                expense(
                    id = 2L,
                    amount = 2.0,
                    description = "+cmd"
                ),
                expense(
                    id = 3L,
                    amount = 3.0,
                    description = "-cmd"
                ),
                expense(
                    id = 4L,
                    amount = 4.0,
                    description = "@cmd"
                )
            ),
            categoryNames = mapOf(1L to "=Food")
        )

        assertTrue(csv.contains("1,2026-01-15,'=Food,'=SUM(A1:A2),1.00,PHP"))
        assertTrue(csv.contains("2,2026-01-15,'=Food,'+cmd,2.00,PHP"))
        assertTrue(csv.contains("3,2026-01-15,'=Food,'-cmd,3.00,PHP"))
        assertTrue(csv.contains("4,2026-01-15,'=Food,'@cmd,4.00,PHP"))
    }

    @Test
    fun transactionsSortByDateThenIdDeterministically() {
        val csv = formatter.transactions(
            expenses = listOf(
                expense(id = 3L, amount = 30.0, day = 16),
                expense(id = 2L, amount = 20.0, day = 15),
                expense(id = 1L, amount = 10.0, day = 15)
            ),
            categoryNames = emptyMap()
        )

        val rows = csv.lines()
        assertEquals("1,2026-01-15,Unknown Category,Lunch,10.00,PHP", rows[1])
        assertEquals("2,2026-01-15,Unknown Category,Lunch,20.00,PHP", rows[2])
        assertEquals("3,2026-01-16,Unknown Category,Lunch,30.00,PHP", rows[3])
    }

    @Test
    fun transactionsWithNoRowsStillHaveAValidHeader() {
        val csv = formatter.transactions(
            expenses = emptyList(),
            categoryNames = emptyMap()
        )

        assertEquals(
            "transaction_id,expense_date,category_name,description,amount,currency",
            csv
        )
    }

    @Test
    fun moneyUsesDecimalStringFormatting() {
        val csv = formatter.transactions(
            expenses = listOf(
                expense(id = 1L, amount = 0.1 + 0.2),
                expense(id = 2L, amount = 1.005)
            ),
            categoryNames = emptyMap()
        )

        assertTrue(csv.contains(",0.30,PHP"))
        assertTrue(csv.contains(",1.01,PHP"))
    }

    @Test
    fun summaryContainsRangeTotalsCategoryPercentagesAndDailyTotals() {
        val csv = formatter.summary(
            report = ExpenseReport(
                totalAmount = 300.0,
                expenseCount = 2,
                averageAmount = 150.0,
                categoryBreakdown = listOf(
                    CategorySpending(
                        categoryId = 1L,
                        totalAmount = 200.0,
                        percentage = 66.666
                    ),
                    CategorySpending(
                        categoryId = 2L,
                        totalAmount = 100.0,
                        percentage = 33.333
                    )
                ),
                dailySpending = listOf(
                    DailySpending(
                        date = timestamp(2026, 1, 5),
                        totalAmount = 300.0
                    )
                )
            ),
            categoryNames = mapOf(1L to "Food", 2L to "Transport"),
            startDateInclusive = timestamp(2026, 1, 1),
            endDateExclusive = timestamp(2026, 2, 1)
        )

        val rows = csv.lines()
        assertEquals("section,label,value,percentage,date", rows[0])
        assertTrue(rows.contains("summary,Start Date,2026-01-01,,"))
        assertTrue(rows.contains("summary,End Date,2026-01-31,,"))
        assertTrue(rows.contains("summary,Currency,PHP,,"))
        assertTrue(rows.contains("summary,Total Expenses,300.00,,"))
        assertTrue(rows.contains("summary,Transaction Count,2,,"))
        assertTrue(rows.contains("summary,Average Expense,150.00,,"))
        assertTrue(rows.contains("category,Food,200.00,66.67,"))
        assertTrue(rows.contains("category,Transport,100.00,33.33,"))
        assertTrue(rows.contains("daily,Daily Total,300.00,,2026-01-05"))
    }

    @Test
    fun summaryEscapesFormulaTextInCategoryNames() {
        val csv = formatter.summary(
            report = ExpenseReport(
                totalAmount = 10.0,
                expenseCount = 1,
                averageAmount = 10.0,
                categoryBreakdown = listOf(
                    CategorySpending(
                        categoryId = 1L,
                        totalAmount = 10.0,
                        percentage = 100.0
                    )
                )
            ),
            categoryNames = mapOf(1L to "=Food, Inc."),
            startDateInclusive = timestamp(2026, 1, 1),
            endDateExclusive = timestamp(2026, 1, 2)
        )

        assertTrue(csv.contains("category,\"'=Food, Inc.\",10.00,100.00,"))
    }

    @Test
    fun emptySummaryStillContainsRangeAndZeroTotals() {
        val csv = formatter.summary(
            report = ExpenseReport(),
            categoryNames = emptyMap(),
            startDateInclusive = timestamp(2026, 1, 1),
            endDateExclusive = timestamp(2026, 1, 2)
        )

        assertTrue(csv.startsWith("section,label,value,percentage,date\r\n"))
        assertTrue(csv.contains("summary,Total Expenses,0.00,,"))
        assertTrue(csv.contains("summary,Transaction Count,0,,"))
        assertTrue(csv.contains("summary,Average Expense,0.00,,"))
    }

    private fun expense(
        id: Long,
        amount: Double,
        description: String = "Lunch",
        day: Int = 15
    ): Expense {
        return Expense(
            id = id,
            amount = amount,
            categoryId = 1L,
            description = description,
            date = timestamp(2026, 1, day)
        )
    }

    private fun timestamp(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
