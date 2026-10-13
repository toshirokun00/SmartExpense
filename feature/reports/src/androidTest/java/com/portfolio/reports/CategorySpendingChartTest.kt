package com.portfolio.reports

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.reports.component.CategorySpendingChart
import com.smartexpense.domain.model.CategorySpending
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CategorySpendingChartTest{

   @get:Rule
   val composeRule = createComposeRule()

   @Test
   fun emptyCategoryBreakdownDisplaysEmptyMessage() {
      composeRule.setContent {
         MaterialTheme {
            CategorySpendingChart(
               categoryBreakdown = emptyList(),
               categoryNames = emptyMap()
            )
         }

      }

      composeRule.onNodeWithText("No category spending to display.").assertIsDisplayed()
   }

   @Test
   fun categoryBreakdownDisplaysCategoryNameAndAmount() {
      composeRule.setContent {
       MaterialTheme {
          CategorySpendingChart(
             categoryBreakdown = listOf(
                CategorySpending(categoryId = 1L,
                   totalAmount = 100.0,
                   percentage = 100.0)),
             categoryNames = mapOf(1L to "Food")
          )
       }
      }

      composeRule.onNodeWithText("Food").assertIsDisplayed()
      composeRule.onNodeWithText("₱100.00").assertIsDisplayed()
      composeRule.onNodeWithText("100.0%").assertIsDisplayed()
   }

   @Test
   fun missingCategoryNameDisplaysUnknownCategory() {
      composeRule.setContent {
         MaterialTheme {
            CategorySpendingChart(
               categoryBreakdown =  listOf(CategorySpending(
                  categoryId = 99L,
                  totalAmount = 50.0,
                  percentage = 100.0
               )),
               categoryNames = emptyMap()
            )
         }
      }

      composeRule.onNodeWithText("Unknown Category").assertIsDisplayed()
   }


   @Test
   fun zeroSpendingCategoryDisplaysZeroAmountAndPercentage() {
      composeRule.setContent {
         MaterialTheme {
            CategorySpendingChart(
               categoryBreakdown = listOf(CategorySpending(
                  categoryId = 1L,
                  totalAmount = 0.0,
                  percentage = 0.0
               )),
               categoryNames = mapOf(1L to "Transport")
            )
         }
      }
      composeRule.onNodeWithText("Transport").assertIsDisplayed()
      composeRule.onNodeWithText("₱0.00").assertIsDisplayed()
      composeRule.onNodeWithText("0.0%").assertIsDisplayed()
   }


}
