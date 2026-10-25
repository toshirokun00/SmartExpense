package com.smartexpense.reports.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormatting
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.export.ReportPdfData
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseReport
import java.io.OutputStream
import java.time.ZoneId
import kotlin.math.max

/**
 * Renders the platform-agnostic PDF export data with Android's PdfDocument.
 *
 * This class is intentionally outside the domain layer: the domain decides
 * what the report contains, while this renderer owns page layout and Android
 * drawing primitives. It never touches storage; the writer supplies the
 * destination stream.
 */
class ReportPdfRenderer(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    fun write(
        export: ReportExportFile,
        outputStream: OutputStream
    ) {
        val content = export.content as? ReportExportContent.Pdf
            ?: throw IllegalArgumentException("Export does not contain PDF data")

        val document = PdfDocument()

        try {
            val pageWriter = PageWriter(
                document = document,
                data = content.data,
                zoneId = zoneId
            )

            when (content.data.type) {
                ReportExportType.TRANSACTIONS -> pageWriter.renderTransactions()
                ReportExportType.SUMMARY -> pageWriter.renderSummary()
            }

            pageWriter.finish()
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private class PageWriter(
        private val document: PdfDocument,
        private val data: ReportPdfData,
        private val zoneId: ZoneId
    ) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = CONTENT_TOP

        private var currentSectionTitle: String? = null
        private var currentTable: TableSpec? = null
        private var currentSectionDrawn = false

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 33, 36)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(95, 99, 104)
            textSize = 10f
            typeface = Typeface.DEFAULT
        }

        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 33, 36)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 64, 67)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 33, 36)
            textSize = 9.5f
            typeface = Typeface.DEFAULT
        }

        private val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 33, 36)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(95, 99, 104)
            textSize = 10f
            typeface = Typeface.DEFAULT
        }

        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(128, 134, 139)
            textSize = 8f
            typeface = Typeface.DEFAULT
        }

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(218, 220, 224)
            strokeWidth = 0.6f
        }

        fun renderTransactions() {
            beginDocument()

            val total = data.expenses.sumOf(Expense::amount)
            startSection("Transactions", TRANSACTION_TABLE)

            if (data.expenses.isEmpty()) {
                drawEmpty("No transactions in this period.")
                return
            }

            data.expenses.forEach(::drawTransactionRow)
            drawTransactionTotal(total)
        }

        fun renderSummary() {
            beginDocument()

            val report = data.report ?: ExpenseReport()
            startSection("Summary", SUMMARY_TABLE)

            drawSimpleRow(
                values = listOf(
                    "Period",
                    "${ReportExportFormatting.formatDate(data.startDateInclusive, zoneId)} to " +
                        ReportExportFormatting.formatInclusiveEndDate(
                            data.endDateExclusive,
                            zoneId
                        )
                ),
                table = SUMMARY_TABLE
            )
            drawSimpleRow(
                values = listOf(
                    "Total Spending",
                    money(report.totalAmount)
                ),
                table = SUMMARY_TABLE,
                bold = true
            )
            drawSimpleRow(
                values = listOf(
                    "Transactions",
                    report.expenseCount.toString()
                ),
                table = SUMMARY_TABLE
            )
            drawSimpleRow(
                values = listOf(
                    "Average Expense",
                    money(report.averageAmount)
                ),
                table = SUMMARY_TABLE
            )
            drawSimpleRow(
                values = listOf(
                    "Currency",
                    ReportExportFormatting.CURRENCY_CODE
                ),
                table = SUMMARY_TABLE
            )

            startSection("Spending by Category", CATEGORY_TABLE)

            val categories = report.categoryBreakdown.sortedWith(
                compareByDescending<CategorySpending> { it.totalAmount }
                    .thenBy { it.categoryId }
            )

            if (categories.isEmpty()) {
                drawEmpty("No category spending for this period.")
            } else {
                categories.forEach { category ->
                    drawSimpleRow(
                        values = listOf(
                            data.categoryNames[category.categoryId]
                                ?: ReportExportFormatting.UNKNOWN_CATEGORY,
                            money(category.totalAmount),
                            "${ReportExportFormatting.formatPercentage(category.percentage)}%"
                        ),
                        table = CATEGORY_TABLE
                    )
                }
            }

            startSection("Daily Spending", DAILY_TABLE)

            val dailySpending = report.dailySpending.sortedBy(DailySpending::date)

            if (dailySpending.isEmpty()) {
                drawEmpty("No daily spending for this period.")
            } else {
                dailySpending.forEach { daily ->
                    drawSimpleRow(
                        values = listOf(
                            ReportExportFormatting.formatDate(daily.date, zoneId),
                            money(daily.totalAmount)
                        ),
                        table = DAILY_TABLE
                    )
                }
            }
        }

        fun finish() {
            finishPage()
        }

        private fun beginDocument() {
            if (page == null) {
                startPage()
            }
        }

        private fun startPage() {
            pageNumber += 1

            val newPage = document.startPage(
                PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber
                ).create()
            )

            page = newPage
            canvas = newPage.canvas
            y = CONTENT_TOP
            currentSectionDrawn = false

            drawDocumentHeader()
            drawSectionHeader()
        }

        private fun finishPage() {
            val currentPage = page ?: return
            val currentCanvas = canvas ?: return

            drawFooter(currentCanvas)
            document.finishPage(currentPage)

            page = null
            canvas = null
        }

        private fun drawDocumentHeader() {
            val currentCanvas = canvas ?: return
            val title = when (data.type) {
                ReportExportType.TRANSACTIONS -> "SmartExpense Transactions"
                ReportExportType.SUMMARY -> "SmartExpense Report"
            }
            val subtitle = "Date range: " +
                "${ReportExportFormatting.formatDate(data.startDateInclusive, zoneId)} to " +
                ReportExportFormatting.formatInclusiveEndDate(
                    data.endDateExclusive,
                    zoneId
                ) +
                "  |  Currency: ${ReportExportFormatting.CURRENCY_CODE}"

            currentCanvas.drawText(title, MARGIN, y + 20f, titlePaint)
            currentCanvas.drawText(subtitle, MARGIN, y + 37f, subtitlePaint)

            y += 48f
            currentCanvas.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                linePaint
            )
            y += 14f
        }

        private fun drawFooter(currentCanvas: Canvas) {
            val footerY = PAGE_HEIGHT - 34f

            currentCanvas.drawLine(
                MARGIN,
                footerY,
                PAGE_WIDTH - MARGIN,
                footerY,
                linePaint
            )
            currentCanvas.drawText(
                "SmartExpense  |  Page $pageNumber",
                MARGIN,
                PAGE_HEIGHT - 20f,
                footerPaint
            )
        }

        private fun startSection(
            title: String,
            table: TableSpec
        ) {
            currentSectionTitle = title
            currentTable = table
            currentSectionDrawn = false

            ensureSpace(
                SECTION_TITLE_HEIGHT +
                    TABLE_HEADER_HEIGHT +
                    TABLE_HEADER_GAP +
                    table.rowHeight
            )

            if (!currentSectionDrawn) {
                drawSectionHeader()
            }
        }

        private fun drawSectionHeader() {
            if (currentSectionDrawn) {
                return
            }

            val currentCanvas = canvas ?: return
            val sectionTitle = currentSectionTitle ?: return

            currentCanvas.drawText(
                sectionTitle,
                MARGIN,
                y + 14f,
                sectionPaint
            )
            y += SECTION_TITLE_HEIGHT

            currentTable?.let { table ->
                drawTableHeader(table)
            }

            currentSectionDrawn = true
        }

        private fun drawTableHeader(table: TableSpec) {
            val currentCanvas = canvas ?: return
            val baseline = y + 15f

            table.columns.forEach { column ->
                drawCell(
                    text = column.label,
                    column = column,
                    baseline = baseline,
                    paint = headerPaint
                )
            }

            y += TABLE_HEADER_HEIGHT
            currentCanvas.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                linePaint
            )
            y += TABLE_HEADER_GAP
        }

        private fun drawSimpleRow(
            values: List<String>,
            table: TableSpec,
            bold: Boolean = false
        ) {
            ensureSpace(table.rowHeight)

            val currentCanvas = canvas ?: return
            val baseline = y + 15f
            val paint = if (bold) boldBodyPaint else bodyPaint

            table.columns.forEachIndexed { index, column ->
                drawCell(
                    text = values.getOrElse(index) { "" },
                    column = column,
                    baseline = baseline,
                    paint = paint
                )
            }

            y += table.rowHeight
            currentCanvas.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                linePaint
            )
        }

        private fun drawTransactionRow(expense: Expense) {
            val descriptionLayout = wrappedLayout(
                text = expense.description,
                width = TRANSACTION_DESCRIPTION_COLUMN.width - 6f,
                paint = bodyPaint
            )
            val rowHeight = max(
                TRANSACTION_TABLE.rowHeight,
                descriptionLayout.height + 8f
            )

            ensureSpace(rowHeight)

            val currentCanvas = canvas ?: return
            val baseline = y + 14f

            drawCell(
                text = expense.id.toString(),
                column = TRANSACTION_ID_COLUMN,
                baseline = baseline,
                paint = bodyPaint
            )
            drawCell(
                text = ReportExportFormatting.formatDate(expense.date, zoneId),
                column = TRANSACTION_DATE_COLUMN,
                baseline = baseline,
                paint = bodyPaint
            )
            drawCell(
                text = data.categoryNames[expense.categoryId]
                    ?: ReportExportFormatting.UNKNOWN_CATEGORY,
                column = TRANSACTION_CATEGORY_COLUMN,
                baseline = baseline,
                paint = bodyPaint
            )

            drawLayout(
                layout = descriptionLayout,
                x = TRANSACTION_DESCRIPTION_COLUMN.x,
                top = y + 4f
            )

            drawCell(
                text = money(expense.amount),
                column = TRANSACTION_AMOUNT_COLUMN,
                baseline = baseline,
                paint = bodyPaint
            )

            y += rowHeight
            currentCanvas.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                linePaint
            )
        }

        private fun drawTransactionTotal(total: Double) {
            ensureSpace(TRANSACTION_TABLE.rowHeight)

            val currentCanvas = canvas ?: return
            val baseline = y + 15f

            drawCell(
                text = "Total",
                column = TRANSACTION_DESCRIPTION_COLUMN,
                baseline = baseline,
                paint = boldBodyPaint
            )
            drawCell(
                text = money(total),
                column = TRANSACTION_AMOUNT_COLUMN,
                baseline = baseline,
                paint = boldBodyPaint
            )

            y += TRANSACTION_TABLE.rowHeight
            currentCanvas.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                linePaint
            )
        }

        private fun drawEmpty(message: String) {
            ensureSpace(24f)

            val currentCanvas = canvas ?: return
            currentCanvas.drawText(message, MARGIN, y + 15f, emptyPaint)
            y += 24f
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (page == null) {
                startPage()
            }

            if (y + requiredHeight > CONTENT_BOTTOM) {
                finishPage()
                startPage()
            }
        }

        private fun drawCell(
            text: String,
            column: TableColumn,
            baseline: Float,
            paint: Paint
        ) {
            val currentCanvas = canvas ?: return
            val textPaint = TextPaint(paint)
            val displayText = TextUtils.ellipsize(
                text,
                textPaint,
                (column.width - 6f).coerceAtLeast(1f),
                TextUtils.TruncateAt.END
            ).toString()
            val x = when (column.align) {
                Paint.Align.RIGHT -> column.x + column.width
                Paint.Align.CENTER -> column.x + column.width / 2f
                else -> column.x
            }
            val originalAlign = paint.textAlign

            paint.textAlign = column.align
            currentCanvas.drawText(displayText, x, baseline, paint)
            paint.textAlign = originalAlign
        }

        private fun wrappedLayout(
            text: String,
            width: Float,
            paint: Paint
        ): StaticLayout {
            return StaticLayout.Builder.obtain(
                text,
                0,
                text.length,
                TextPaint(paint),
                width.toInt().coerceAtLeast(1)
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(0f, 1f)
                .setMaxLines(5)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        }

        private fun drawLayout(
            layout: StaticLayout,
            x: Float,
            top: Float
        ) {
            val currentCanvas = canvas ?: return

            currentCanvas.save()
            currentCanvas.translate(x, top)
            layout.draw(currentCanvas)
            currentCanvas.restore()
        }

        private fun money(amount: Double): String {
            return "${ReportExportFormatting.CURRENCY_CODE} " +
                ReportExportFormatting.formatAmount(amount)
        }

        private data class TableColumn(
            val label: String,
            val x: Float,
            val width: Float,
            val align: Paint.Align = Paint.Align.LEFT
        )

        private data class TableSpec(
            val columns: List<TableColumn>,
            val rowHeight: Float = 22f
        )

        private companion object {
            const val PAGE_WIDTH = 595
            const val PAGE_HEIGHT = 842
            const val MARGIN = 40f
            const val CONTENT_TOP = 40f
            const val CONTENT_BOTTOM = 790f
            const val SECTION_TITLE_HEIGHT = 24f
            const val TABLE_HEADER_HEIGHT = 20f
            const val TABLE_HEADER_GAP = 4f

            val SUMMARY_TABLE = TableSpec(
                columns = listOf(
                    TableColumn("Metric", 40f, 250f),
                    TableColumn("Value", 290f, 265f, Paint.Align.RIGHT)
                )
            )

            val CATEGORY_TABLE = TableSpec(
                columns = listOf(
                    TableColumn("Category", 40f, 270f),
                    TableColumn("Amount", 310f, 120f, Paint.Align.RIGHT),
                    TableColumn("Share", 430f, 125f, Paint.Align.RIGHT)
                )
            )

            val DAILY_TABLE = TableSpec(
                columns = listOf(
                    TableColumn("Date", 40f, 300f),
                    TableColumn("Amount", 340f, 215f, Paint.Align.RIGHT)
                )
            )

            val TRANSACTION_ID_COLUMN = TableColumn(
                "ID",
                40f,
                36f,
                Paint.Align.RIGHT
            )
            val TRANSACTION_DATE_COLUMN = TableColumn(
                "Date",
                82f,
                62f
            )
            val TRANSACTION_CATEGORY_COLUMN = TableColumn(
                "Category",
                150f,
                95f
            )
            val TRANSACTION_DESCRIPTION_COLUMN = TableColumn(
                "Description",
                251f,
                200f
            )
            val TRANSACTION_AMOUNT_COLUMN = TableColumn(
                "Amount",
                457f,
                98f,
                Paint.Align.RIGHT
            )

            val TRANSACTION_TABLE = TableSpec(
                columns = listOf(
                    TRANSACTION_ID_COLUMN,
                    TRANSACTION_DATE_COLUMN,
                    TRANSACTION_CATEGORY_COLUMN,
                    TRANSACTION_DESCRIPTION_COLUMN,
                    TRANSACTION_AMOUNT_COLUMN
                )
            )
        }
    }
}
