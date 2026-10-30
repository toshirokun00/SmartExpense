package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.smartexpense.core.common.util.MoneyInputUtil

/**
 * Text field for currency/money amounts that formats with thousands commas in real-time
 * and maintains accurate cursor position as the user types (preventing cursor jumping).
 */
@Composable
fun MoneyAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    readOnly: Boolean = false
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val result = MoneyInputUtil.formatAmountWithCursor(
                input = newValue.text,
                rawCursor = newValue.selection.end,
                previousInput = textFieldValue.text
            )
            textFieldValue = TextFieldValue(
                text = result.formatted,
                selection = TextRange(result.cursorPosition)
            )
            onValueChange(result.formatted)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        label = { Text(label) },
        readOnly = readOnly,
        modifier = modifier
    )
}
