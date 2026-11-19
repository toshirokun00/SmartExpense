# SmartExpense

SmartExpense is a personal finance app for Android that helps you understand where your money goes. Track daily expenses and income, organize transactions by category, set budgets, manage recurring bills, and view spending reports — all from one clean dashboard.

The app works offline-first with a local Room database, and optionally backs up your data to Supabase so you can sign in on another device and restore your expenses, budgets, bills, paycheck plans, and bill payment history.

## Features

### Dashboard
- At-a-glance overview of spending, budgets, and bills due this week
- Quick actions for expenses, budgets, reports, recurring expenses, bills, paychecks, and the money tracker
- Recent expenses list and personalized spending insights

### Expense Tracking
- Add, edit, and delete expenses and income
- Assign categories, payment methods, dates, notes, and merchant details
- Attach receipt photos to expenses
- Philippine peso (₱) formatting throughout the app
- Search, filter, and sort transactions

### Budgets
- Create budgets per category for a chosen period
- Track spending against each budget with progress indicators
- Configurable budget alert thresholds and notifications

### Bills
- Add one-time or recurring bills with weekly, monthly, quarterly, or yearly schedules
- Support for fixed and variable bill amounts
- Set due dates, categories, payees, and payment methods
- Bill reminders with configurable days before and notification time
- Mark bills as paid or skip an occurrence
- Auto-pay indicator for automatically debited bills
- Payment history grouped by month with totals and a month filter
- See how much you paid this month and last month at a glance

### Paychecks
- Record upcoming income with an amount, pay date, and optional label
- Assign the bills each paycheck should cover
- Unpaid bills due on or before the pay date are suggested automatically, and the selection can be adjusted
- See how much of each paycheck is covered and what is left after bills
- Create recurring payroll schedules: weekly, every two weeks, monthly, or semi-monthly (for example every 15th and 30th)
- Each cutoff is generated as its own editable paycheck, and recurring paychecks can be stopped without deleting existing entries
- Bills already covered by another paycheck are never suggested or listed again, so every bill is planned only once
- Each paycheck card lists the bills it covers and how much is left after them
- The paycheck list opens on the current month, with previous/next and a month picker to jump anywhere
- Paycheck plans sync to Supabase and restore on another device

### Money Tracker
- Per-paycheck envelopes show income, assigned bills, and expenses for each cutoff
- Expenses are counted from a payday until the next payday, matching the real pay cycle
- See the money left for each paycheck and an overall plan total
- Overspent cycles are highlighted, and each envelope opens its paycheck for adjustments

### Recurring Expenses
- Create recurring expense schedules
- Automatic processing of due recurring transactions
- Edit or remove schedules at any time

### Reports & Exports
- Spending by category and daily spending summaries
- Select a custom reporting period
- Export transaction details or report summaries as CSV or PDF
- Save exports to storage or share them through the Android share sheet

### Categories
- Create and manage custom expense categories
- Assign categories to expenses, budgets, and bills

### Sync & Account
- Email/password authentication with email confirmation
- Local-first storage that works offline
- Cloud backup and restore through Supabase
- Account switching with automatic data refresh on sign-in

### Personalization & Settings
- Light and dark theme support
- Expense and budget notifications
- Profile management
- About screen with app information and data privacy details
