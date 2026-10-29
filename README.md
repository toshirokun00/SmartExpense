# SmartExpense
A personal finance app where users can track expenses, manage budgets, receive reminders, view spending analytics, and optionally sync data with a backend.

## Supabase configuration

The app needs two values at build time:

| Name | Description |
| --- | --- |
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_PUBLISHABLE_KEY` | Supabase publishable (client) key |

Locally they are read from `local.properties` (gitignored). In CI they are read from environment variables, so add both as repository secrets under **Settings → Secrets and variables → Actions**. The release workflow validates that they are present and fails before building if either is missing; without them the app compiles but crashes at startup with `SUPABASE_URL is not configured`.

## CSV and PDF report exports

The Reports screen can export the selected date range as either transaction details or a report summary, in CSV or PDF format. Files are generated from the local Room data and can be saved through the Android Storage Access Framework or shared through the Android Sharesheet. The selected format controls the save picker MIME type and the share MIME type (`text/csv` or `application/pdf`).

Transaction CSV columns:

```text
transaction_id,expense_date,category_name,description,amount,currency
```

Summary CSV columns:

```text
section,label,value,percentage,date
```

Summary rows use the `summary` section for the date range, currency, total, count, and average. `category` rows contain category amounts and percentages, and `daily` rows contain ISO dates and daily totals.

Dates are ISO 8601 (`YYYY-MM-DD`), amounts are plain decimal strings with two fraction digits, and the app's PHP currency is exported explicitly. Text fields are CSV-escaped, and values that could be interpreted as spreadsheet formulas are prefixed with an apostrophe in the generated file only.

PDF exports use the same report calculations and include a title, selected date range, and currency. Transaction PDFs contain a paginated table with transaction ID, date, category, description, amount, and a total row. Summary PDFs contain totals, transaction count, average expense, category breakdown with percentages, and daily spending. PDF rendering happens in the Android feature layer with `android.graphics.pdf.PdfDocument`; the domain provides platform-agnostic report data.

Export filenames follow these patterns:

```text
smartexpense_transactions_2026-01-01_2026-01-31.csv
smartexpense_report_2026-01-01_2026-01-31.csv
smartexpense_transactions_2026-01-01_2026-01-31.pdf
smartexpense_report_2026-01-01_2026-01-31.pdf
```

Share files live in the app's private cache directory and are exposed only through a restricted `FileProvider` content URI. The app never shares a `file://` URI.
