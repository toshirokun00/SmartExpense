package com.smartexpense.domain.export

import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseReport
import java.time.ZoneId

/**
 * Pure CSV formatting for SmartExpense report exports.
 *
 * Transaction CSV columns:
 * `transaction_id,expense_date,category_name,description,amount,currency`
 *
 * Summary CSV columns:
 * `section,label,value,percentage,date`
 *
 * The summary file uses one consistent table. Summary rows contain the date
 * range, currency, totals, count, and average; category rows contain localized
 * category names, amounts, and percentages; daily rows contain an ISO date and
 * amount. Dates are ISO 8601 and amounts are plain decimal strings with two
 * fraction digits.
 *
 * Text fields are guarded against spreadsheet formula injection by prefixing
 * values that begin with a formula character with an apostrophe. This affects
 * only the generated CSV, never stored expense or category data.
 */
class ReportCsvFormatter(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    fun transactions(
        expenses: List<Expense>,
        categoryNames: Map<Long, String>
    ): String {
        val rows = expenses
            .sortedWith(compareBy<Expense> { it.date }.thenBy { it.id })
            .map { expense ->
                listOf(
                    plainField(expense.id.toString()),
                    plainField(ReportExportFormatting.formatDate(expense.date, zoneId)),
                    textField(expense.categoryName(categoryNames)),
                    textField(expense.description),
                    plainField(ReportExportFormatting.formatAmount(expense.amount)),
                    plainField(ReportExportFormatting.CURRENCY_CODE)
                ).joinToString(",")
            }

        return buildCsv(TRANSACTION_HEADERS, rows)
    }

    fun summary(
        report: ExpenseReport,
        categoryNames: Map<Long, String>,
        startDateInclusive: Long,
        endDateExclusive: Long
    ): String {
        val startDate = ReportExportFormatting.formatDate(startDateInclusive, zoneId)
        val endDate = ReportExportFormatting.formatInclusiveEndDate(
            endDateExclusive = endDateExclusive,
            zoneId = zoneId
        )

        val rows = buildList {
            add(summaryRow("Start Date", startDate))
            add(summaryRow("End Date", endDate))
            add(summaryRow("Currency", ReportExportFormatting.CURRENCY_CODE))
            add(
                summaryRow(
                    "Total Expenses",
                    ReportExportFormatting.formatAmount(report.totalAmount)
                )
            )
            add(summaryRow("Transaction Count", report.expenseCount.toString()))
            add(
                summaryRow(
                    "Average Expense",
                    ReportExportFormatting.formatAmount(report.averageAmount)
                )
            )

            report.categoryBreakdown
                .sortedWith(
                    compareByDescending<CategorySpending> { it.totalAmount }
                        .thenBy { it.categoryId }
                )
                .forEach { category ->
                    add(
                        csvRow(
                            section = "category",
                            label = textField(
                                categoryNames[category.categoryId]
                                    ?: ReportExportFormatting.UNKNOWN_CATEGORY
                            ),
                            value = plainField(
                                ReportExportFormatting.formatAmount(
                                    category.totalAmount
                                )
                            ),
                            percentage = plainField(
                                ReportExportFormatting.formatPercentage(
                                    category.percentage
                                )
                            ),
                            date = ""
                        )
                    )
                }

            report.dailySpending
                .sortedBy(DailySpending::date)
                .forEach { daily ->
                    add(
                        csvRow(
                            section = "daily",
                            label = "Daily Total",
                            value = plainField(
                                ReportExportFormatting.formatAmount(
                                    daily.totalAmount
                                )
                            ),
                            percentage = "",
                            date = plainField(
                                ReportExportFormatting.formatDate(
                                    daily.date,
                                    zoneId
                                )
                            )
                        )
                    )
                }
        }

        return buildCsv(SUMMARY_HEADERS, rows)
    }

    private fun Expense.categoryName(categoryNames: Map<Long, String>): String {
        return categoryNames[categoryId]
            ?: ReportExportFormatting.UNKNOWN_CATEGORY
    }

    private fun summaryRow(label: String, value: String): String {
        return csvRow(
            section = "summary",
            label = label,
            value = value,
            percentage = "",
            date = ""
        )
    }

    private fun csvRow(
        section: String,
        label: String,
        value: String,
        percentage: String,
        date: String
    ): String {
        return listOf(
            plainField(section),
            label,
            value,
            percentage,
            date
        ).joinToString(",")
    }

    private fun buildCsv(headers: List<String>, rows: List<String>): String {
        return buildList {
            add(headers.joinToString(","))
            addAll(rows)
        }.joinToString(ROW_SEPARATOR)
    }

    /**
     * Escapes a text field and neutralizes values that spreadsheet programs
     * could interpret as formulas.
     */
    private fun textField(value: String): String {
        val guarded = if (startsWithFormulaCharacter(value)) {
            "'$value"
        } else {
            value
        }

        return quoteIfNeeded(guarded)
    }

    private fun plainField(value: String): String {
        return quoteIfNeeded(value)
    }

    private fun quoteIfNeeded(value: String): String {
        val needsQuotes = value.any { character ->
            character == ',' ||
                character == '"' ||
                character == '\n' ||
                character == '\r'
        }

        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun startsWithFormulaCharacter(value: String): Boolean {
        val firstMeaningfulCharacter = value.firstOrNull { !it.isWhitespace() }
            ?: return false

        return firstMeaningfulCharacter in FORMULA_CHARACTERS
    }

    companion object {
        private const val ROW_SEPARATOR = "\r\n"

        private val FORMULA_CHARACTERS = setOf('=', '+', '-', '@', '\t', '\r', '\n')

        private val TRANSACTION_HEADERS = listOf(
            "transaction_id",
            "expense_date",
            "category_name",
            "description",
            "amount",
            "currency"
        )

        private val SUMMARY_HEADERS = listOf(
            "section",
            "label",
            "value",
            "percentage",
            "date"
        )
    }
}
