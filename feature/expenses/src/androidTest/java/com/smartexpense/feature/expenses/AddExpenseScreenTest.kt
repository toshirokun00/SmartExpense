package com.smartexpense.feature.expenses

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteExpenseUseCase
import com.smartexpense.domain.usecase.GetExpenseByIdUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import com.smartexpense.domain.usecase.UpdateExpenseUseCase
import com.smartexpense.domain.usecase.ValidateAmountUseCase
import com.smartexpense.feature.expenses.ui.AddExpenseScreen
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import com.smartexpense.notification.ExpenseNotificationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Add Expense screen ([AddExpenseScreen]).
 */
@RunWith(AndroidJUnit4::class)
class AddExpenseScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var getExpenseUseCase: GetExpenseUseCase
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    private lateinit var getExpenseByIdUseCase: GetExpenseByIdUseCase
    private lateinit var validateAmountUseCase: ValidateAmountUseCase
    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var settingsUseCase: SettingsUseCase
    private lateinit var notificationManager: ExpenseNotificationManager
    private lateinit var viewModel: ExpenseViewModel

    @Before
    fun setUp() {
        getExpenseUseCase = mockk<GetExpenseUseCase>().also {
            every { it.invoke() } returns flowOf(emptyList())
        }
        addExpenseUseCase = mockk(relaxed = true)
        updateExpenseUseCase = mockk(relaxed = true)
        deleteExpenseUseCase = mockk(relaxed = true)
        getExpenseByIdUseCase = mockk(relaxed = true)
        validateAmountUseCase = mockk()
        categoryUseCase = mockk()
        settingsUseCase = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)

        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(
                Category(id = 1L, name = "Food"),
                Category(id = 2L, name = "Transport")
            )
        )

        viewModel = ExpenseViewModel(
            getExpenseUseCase = getExpenseUseCase,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            validateExpenseAmountUseCase = validateAmountUseCase,
            categoryUseCase = categoryUseCase,
            settingsUseCase = settingsUseCase,
            expenseNotificationManager = notificationManager
        )
    }

    // --- initial state ---

    @Test
    fun addExpenseScreenTopBarTitleIsDisplayed() {
        setContent()

        composeRule.onNodeWithText("Add Expense").assertIsDisplayed()
    }

    @Test
    fun amountFieldIsDisplayed() {
        setContent()

        composeRule.onNodeWithText("Amount").assertIsDisplayed()
    }

    @Test
    fun categoryFieldIsDisplayed() {
        setContent()

        composeRule.onNodeWithText("Category").assertIsDisplayed()
    }

    @Test
    fun addExpenseButtonIsDisplayed() {
        setContent()

        composeRule.onNodeWithText("Add Expense").assertIsDisplayed()
    }

    // --- category selection ---

    @Test
    fun categoriesAreShownInDropdownWhenExpanded() {
        setContent()

        // Tap the category dropdown to expand it.
        composeRule.onNodeWithText("Category").performClick()

        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("Transport").assertIsDisplayed()
    }

    @Test
    fun selectedCategoryAppearsInField() {
        setContent()

        composeRule.onNodeWithText("Category").performClick()
        composeRule.onNodeWithText("Food").performClick()

        // After selection the dropdown value should show the chosen category.
        composeRule.onNodeWithText("Food").assertIsDisplayed()
    }

    // --- validation ---

    @Test
    fun invalidAmountShowsErrorSnackbar() {
        every { validateAmountUseCase(any()) } returns null

        setContent()

        composeRule.onNodeWithText("Amount").performTextInput("abc")
        composeRule.onNodeWithText("Category").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("Add Expense").performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Please enter valid amount").assertIsDisplayed()
    }

    @Test
    fun missingCategoryShowsErrorSnackbar() {
        every { validateAmountUseCase(any()) } returns 100.0

        setContent()

        composeRule.onNodeWithText("Amount").performTextInput("100")
        // Intentionally skip category selection.
        composeRule.onNodeWithText("Add Expense").performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Please select a category").assertIsDisplayed()
    }

    // --- successful creation ---

    @Test
    fun successfulExpenseCreationShowsSnackbar() {
        every { validateAmountUseCase(any()) } returns 150.0
        coEvery { addExpenseUseCase(any()) } returns Unit

        setContent()

        composeRule.onNodeWithText("Amount").performTextInput("150")
        composeRule.onNodeWithText("Category").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("Add Expense").performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Expense Added Successfully").assertIsDisplayed()
    }

    @Test
    fun successfulExpenseCreationCallsAddUseCase() {
        every { validateAmountUseCase(any()) } returns 200.0
        coEvery { addExpenseUseCase(any()) } returns Unit

        setContent()

        composeRule.onNodeWithText("Amount").performTextInput("200")
        composeRule.onNodeWithText("Category").performClick()
        composeRule.onNodeWithText("Transport").performClick()
        composeRule.onNodeWithText("Add Expense").performClick()

        composeRule.waitForIdle()

        coVerify {
            addExpenseUseCase(
                match { it.amount == 200.0 && it.categoryId == 2L }
            )
        }
    }

    @Test
    fun formIsResetAfterSuccessfulExpenseCreation() {
        every { validateAmountUseCase(any()) } returns 50.0
        coEvery { addExpenseUseCase(any()) } returns Unit

        setContent()

        composeRule.onNodeWithText("Amount").performTextInput("50")
        composeRule.onNodeWithText("Category").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("Add Expense").performClick()

        composeRule.waitForIdle()

        // Amount field should be cleared after a successful add.
        composeRule.onNodeWithText("50").assertDoesNotExist()
    }

    // --- navigation ---

    @Test
    fun tappingBackNavigatesBack() {
        var backClicked = false

        composeRule.setContent {
            AddExpenseScreen(
                viewModel = viewModel,
                onBackClick = { backClicked = true }
            )
        }

        composeRule.onNodeWithContentDescription("back").performClick()

        assert(backClicked) { "Expected back navigation but it did not occur" }
    }

    // --- helpers ---

    private fun setContent() {
        composeRule.setContent {
            AddExpenseScreen(
                viewModel = viewModel,
                onBackClick = {}
            )
        }
    }
}

// Extension to find nodes by content description (used for icon buttons).
private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onNodeWithContentDescription(
    label: String
) = onNode(
    androidx.compose.ui.test.hasContentDescription(label)
)
