package com.smartexpense.bills

import app.cash.turbine.test
import com.smartexpense.bills.effect.BillFormEffect
import com.smartexpense.bills.intent.BillFormIntent
import com.smartexpense.bills.viewmodel.BillFormViewModel
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.usecase.AddBillUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteBillUseCase
import com.smartexpense.domain.usecase.GetBillUseCase
import com.smartexpense.domain.usecase.SkipBillUseCase
import com.smartexpense.domain.usecase.UpdateBillUseCase
import com.smartexpense.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BillFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getBillUseCase: GetBillUseCase
    private lateinit var addBillUseCase: AddBillUseCase
    private lateinit var updateBillUseCase: UpdateBillUseCase
    private lateinit var deleteBillUseCase: DeleteBillUseCase
    private lateinit var skipBillUseCase: SkipBillUseCase
    private lateinit var categoryUseCase: CategoryUseCase

    private lateinit var viewModel: BillFormViewModel

    @Before
    fun setup() {
        getBillUseCase = mockk()
        addBillUseCase = mockk()
        updateBillUseCase = mockk()
        deleteBillUseCase = mockk()
        skipBillUseCase = mockk()
        categoryUseCase = mockk()

        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(Category(id = 1L, name = "Utilities"))
        )
        coEvery { addBillUseCase(any()) } returns 1L
        coEvery { updateBillUseCase(any()) } just Runs
        coEvery { deleteBillUseCase(any()) } just Runs
        coEvery { skipBillUseCase(any()) } just Runs

        viewModel = createViewModel()
    }

    private fun createViewModel(): BillFormViewModel = BillFormViewModel(
        getBillUseCase = getBillUseCase,
        addBillUseCase = addBillUseCase,
        updateBillUseCase = updateBillUseCase,
        deleteBillUseCase = deleteBillUseCase,
        skipBillUseCase = skipBillUseCase,
        categoryUseCase = categoryUseCase
    )

    private fun bill(
        id: Long = 1L,
        name: String = "Meralco",
        amount: Double = 900.0,
        dueDateMillis: Long = DUE_DATE,
        frequency: BillFrequency = BillFrequency.MONTHLY,
        repeatInterval: Int = 1,
        amountType: BillAmountType = BillAmountType.STATIC,
        amountEstimated: Boolean = false,
        categoryId: Long? = 1L,
        payee: String = "Meralco",
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        autopay: Boolean = false,
        reminderDaysBefore: Int = 3,
        paidAtMillis: Long? = null,
        skipped: Boolean = false,
        isActive: Boolean = true
    ) = Bill(
        id = id,
        name = name,
        amount = amount,
        dueDateMillis = dueDateMillis,
        frequency = frequency,
        repeatInterval = repeatInterval,
        amountType = amountType,
        amountEstimated = amountEstimated,
        categoryId = categoryId,
        payee = payee,
        paymentMethod = paymentMethod,
        autopay = autopay,
        reminderDaysBefore = reminderDaysBefore,
        paidAtMillis = paidAtMillis,
        skipped = skipped,
        isActive = isActive
    )

    // --------------------------------------------------
    // Text field intents
    // --------------------------------------------------

    @Test
    fun amountChangedUpdatesForm() = runTest {
        viewModel.onIntent(BillFormIntent.AmountChanged("1,250"))

        assertEquals("1,250", viewModel.uiState.value.form.amount)
    }

    @Test
    fun dueDateChangedUpdatesForm() = runTest {
        viewModel.onIntent(BillFormIntent.DueDateChanged(DUE_DATE))

        assertEquals(DUE_DATE, viewModel.uiState.value.form.dueDateMillis)
    }

    @Test
    fun notesChangedUpdatesForm() = runTest {
        viewModel.onIntent(BillFormIntent.NotesChanged("Account 123"))

        assertEquals("Account 123", viewModel.uiState.value.form.notes)
    }

    @Test
    fun payeeChangedUpdatesForm() = runTest {
        viewModel.onIntent(BillFormIntent.PayeeChanged(" Maynilad "))

        assertEquals(" Maynilad ", viewModel.uiState.value.form.payee)
    }

    @Test
    fun intervalAcceptsOnlyTwoDigits() = runTest {
        viewModel.onIntent(BillFormIntent.IntervalChanged("1a2b3"))

        assertEquals("12", viewModel.uiState.value.form.intervalText)
    }

    @Test
    fun reminderDaysToggleAddsAndRemoves() = runTest {
        viewModel.onIntent(BillFormIntent.ReminderDaysToggled(7))
        assertEquals(setOf(3, 7), viewModel.uiState.value.form.reminderDays)

        viewModel.onIntent(BillFormIntent.ReminderDaysToggled(3))
        assertEquals(setOf(7), viewModel.uiState.value.form.reminderDays)
    }

    @Test
    fun reminderTimeIsClampedToValidRange() = runTest {
        viewModel.onIntent(BillFormIntent.ReminderTimeChanged(24 * 60 + 30))
        assertEquals(24 * 60 - 1, viewModel.uiState.value.form.reminderTimeMinutes)

        viewModel.onIntent(BillFormIntent.ReminderTimeChanged(-5))
        assertEquals(0, viewModel.uiState.value.form.reminderTimeMinutes)
    }

    @Test
    fun categoriesAreCollected() = runTest {
        advanceUntilIdle()

        assertEquals(
            listOf(Category(id = 1L, name = "Utilities")),
            viewModel.uiState.value.categories
        )
    }

    // --------------------------------------------------
    // Loading
    // --------------------------------------------------

    @Test
    fun loadNullStartsBlankForm() = runTest {
        viewModel.load(null)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.form.editingBillId)
        assertEquals("", state.form.payee)
        assertEquals("", state.form.amount)
    }

    @Test
    fun loadPopulatesFormFromBill() = runTest {
        coEvery { getBillUseCase(1L) } returns bill(
            amount = 4750.0,
            frequency = BillFrequency.MONTHLY,
            repeatInterval = 2,
            amountType = BillAmountType.VARIABLE,
            categoryId = 1L,
            paymentMethod = PaymentMethod.E_WALLET,
            autopay = true,
            reminderDaysBefore = 7
        )

        viewModel.load(1L)
        advanceUntilIdle()

        val form = viewModel.uiState.value.form
        assertEquals(1L, form.editingBillId)
        assertEquals("Meralco", form.payee)
        assertEquals("4,750", form.amount)
        assertEquals(DUE_DATE, form.dueDateMillis)
        assertTrue(form.isRecurring)
        assertEquals(BillFrequency.MONTHLY, form.frequency)
        assertEquals("2", form.intervalText)
        assertEquals(BillAmountType.VARIABLE, form.amountType)
        assertEquals(1L, form.categoryId)
        assertEquals(PaymentMethod.E_WALLET, form.paymentMethod)
        assertTrue(form.autopay)
        assertEquals(setOf(7), form.reminderDays)
        assertTrue(form.canSkip)
    }

    @Test
    fun loadFallsBackToNameWhenPayeeIsBlank() = runTest {
        coEvery { getBillUseCase(1L) } returns bill(
            name = "Maynilad",
            payee = ""
        )

        viewModel.load(1L)
        advanceUntilIdle()

        assertEquals("Maynilad", viewModel.uiState.value.form.payee)
    }

    @Test
    fun loadPaidBillCannotBeSkipped() = runTest {
        coEvery { getBillUseCase(1L) } returns bill(
            paidAtMillis = PAID_AT,
            skipped = true
        )

        viewModel.load(1L)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.form.canSkip)
    }

    @Test
    fun loadMissingBillEmitsError() = runTest {
        coEvery { getBillUseCase(1L) } returns null

        viewModel.effect.test {
            viewModel.load(1L)
            advanceUntilIdle()

            assertEquals(
                BillFormEffect.ShowError("Bill not found"),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reloadWithSameIdKeepsInProgressEdits() = runTest {
        viewModel.load(null)
        viewModel.onIntent(BillFormIntent.PayeeChanged("Meralco"))

        viewModel.load(null)

        assertEquals("Meralco", viewModel.uiState.value.form.payee)
    }

    // --------------------------------------------------
    // Save validation
    // --------------------------------------------------

    @Test
    fun saveWithoutPayeeShowsError() = runTest {
        viewModel.load(null)

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Save)

            assertEquals(
                BillFormEffect.ShowError("Please enter a payee or biller"),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { addBillUseCase(any()) }
    }

    @Test
    fun saveWithZeroAmountShowsError() = runTest {
        viewModel.load(null)
        viewModel.onIntent(BillFormIntent.PayeeChanged("Meralco"))
        viewModel.onIntent(BillFormIntent.AmountChanged("0"))

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Save)

            assertEquals(
                BillFormEffect.ShowError("Please enter a valid amount"),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { addBillUseCase(any()) }
    }

    @Test
    fun saveWithNonNumericAmountShowsError() = runTest {
        viewModel.load(null)
        viewModel.onIntent(BillFormIntent.PayeeChanged("Meralco"))
        viewModel.onIntent(BillFormIntent.AmountChanged("abc"))

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Save)

            assertEquals(
                BillFormEffect.ShowError("Please enter a valid amount"),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { addBillUseCase(any()) }
    }

    // --------------------------------------------------
    // Add
    // --------------------------------------------------

    @Test
    fun saveNewBillUsesPayeeAsNameAndSendsAllFields() = runTest {
        viewModel.load(null)
        viewModel.onIntent(BillFormIntent.PayeeChanged("  Meralco  "))
        viewModel.onIntent(BillFormIntent.AmountChanged("1,234.50"))
        viewModel.onIntent(BillFormIntent.DueDateChanged(DUE_DATE))
        viewModel.onIntent(BillFormIntent.AmountTypeChanged(BillAmountType.VARIABLE))
        viewModel.onIntent(BillFormIntent.RecurringChanged(true))
        viewModel.onIntent(BillFormIntent.FrequencyChanged(BillFrequency.WEEKLY))
        viewModel.onIntent(BillFormIntent.IntervalChanged("2"))
        viewModel.onIntent(BillFormIntent.CategoryChanged(1L))
        viewModel.onIntent(BillFormIntent.PaymentMethodChanged(PaymentMethod.E_WALLET))
        viewModel.onIntent(BillFormIntent.AutopayChanged(true))
        viewModel.onIntent(BillFormIntent.ReminderDaysToggled(7))
        viewModel.onIntent(BillFormIntent.ActiveChanged(false))
        viewModel.onIntent(BillFormIntent.NotesChanged("Account 123"))

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Save)
            advanceUntilIdle()

            assertEquals(BillFormEffect.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            addBillUseCase(
                match {
                    it.name == "Meralco" &&
                        it.payee == "Meralco" &&
                        it.amount == 1234.5 &&
                        it.dueDateMillis == DUE_DATE &&
                        it.frequency == BillFrequency.WEEKLY &&
                        it.repeatInterval == 2 &&
                        it.amountType == BillAmountType.VARIABLE &&
                        it.amountEstimated &&
                        it.categoryId == 1L &&
                        it.paymentMethod == PaymentMethod.E_WALLET &&
                        it.autopay &&
                        it.reminderDaysBefore == 7 &&
                        !it.isActive &&
                        it.notes == "Account 123"
                }
            )
        }
    }

    @Test
    fun saveNewOneTimeBillClearsFrequency() = runTest {
        viewModel.load(null)
        viewModel.onIntent(BillFormIntent.PayeeChanged("Meralco"))
        viewModel.onIntent(BillFormIntent.AmountChanged("900"))
        viewModel.onIntent(BillFormIntent.RecurringChanged(false))
        viewModel.onIntent(BillFormIntent.Skip) // no id, must be ignored

        viewModel.onIntent(BillFormIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            addBillUseCase(
                match {
                    it.frequency == BillFrequency.NONE &&
                        it.amountEstimated.not() &&
                        it.repeatInterval == 1
                }
            )
        }
        coVerify(exactly = 0) { skipBillUseCase(any()) }
    }

    @Test
    fun saveFailureShowsErrorAndStopsSaving() = runTest {
        coEvery { addBillUseCase(any()) } throws RuntimeException("Boom")

        viewModel.load(null)
        viewModel.onIntent(BillFormIntent.PayeeChanged("Meralco"))
        viewModel.onIntent(BillFormIntent.AmountChanged("900"))

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Save)
            advanceUntilIdle()

            assertEquals(BillFormEffect.ShowError("Boom"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.form.isSaving)
    }

    // --------------------------------------------------
    // Update
    // --------------------------------------------------

    @Test
    fun updateBillPreservesPaymentState() = runTest {
        coEvery { getBillUseCase(1L) } returns bill(
            amountType = BillAmountType.VARIABLE,
            amountEstimated = true,
            paidAtMillis = PAID_AT,
            skipped = true
        )

        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onIntent(BillFormIntent.AmountChanged("1,000"))
        viewModel.onIntent(BillFormIntent.PayeeChanged("Meralco Updated"))

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Save)
            advanceUntilIdle()

            assertEquals(BillFormEffect.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            updateBillUseCase(
                match {
                    it.id == 1L &&
                        it.name == "Meralco Updated" &&
                        it.amount == 1000.0 &&
                        it.amountEstimated &&
                        it.paidAtMillis == PAID_AT &&
                        it.skipped
                }
            )
        }
    }

    @Test
    fun updateMissingBillShowsError() = runTest {
        coEvery { getBillUseCase(1L) } returnsMany listOf(bill(), null)

        viewModel.load(1L)
        advanceUntilIdle()
        viewModel.onIntent(BillFormIntent.AmountChanged("1,000"))

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Save)
            advanceUntilIdle()

            assertEquals(BillFormEffect.ShowError("Bill not found"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { updateBillUseCase(any()) }
    }

    // --------------------------------------------------
    // Delete & skip
    // --------------------------------------------------

    @Test
    fun deleteExistingBillCallsUseCaseAndEmitsDeleted() = runTest {
        coEvery { getBillUseCase(1L) } returns bill()

        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Delete)
            advanceUntilIdle()

            assertEquals(BillFormEffect.Deleted, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { deleteBillUseCase(1L) }
    }

    @Test
    fun deleteWithoutBillDoesNothing() = runTest {
        viewModel.load(null)

        viewModel.onIntent(BillFormIntent.Delete)
        advanceUntilIdle()

        coVerify(exactly = 0) { deleteBillUseCase(any()) }
    }

    @Test
    fun skipExistingBillCallsUseCaseAndEmitsSkipped() = runTest {
        coEvery { getBillUseCase(1L) } returns bill()

        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(BillFormIntent.Skip)
            advanceUntilIdle()

            assertEquals(BillFormEffect.Skipped, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { skipBillUseCase(1L) }
    }

    @Test
    fun skipIsIgnoredWhenBillCannotBeSkipped() = runTest {
        coEvery { getBillUseCase(1L) } returns bill(paidAtMillis = PAID_AT)

        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onIntent(BillFormIntent.Skip)
        advanceUntilIdle()

        coVerify(exactly = 0) { skipBillUseCase(any()) }
    }

    companion object {
        private const val DUE_DATE = 1_763_000_000_000L
        private const val PAID_AT = 1_762_000_000_000L
    }
}
