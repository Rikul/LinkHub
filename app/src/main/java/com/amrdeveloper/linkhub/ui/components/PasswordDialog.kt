package com.amrdeveloper.linkhub.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.amrdeveloper.linkhub.R
import com.amrdeveloper.linkhub.util.PasswordUtils

@Composable
fun PasswordDialog(
    hashedPassword: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_password)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isError = false
                },
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                isError = isError,
                supportingText = {
                    if (isError) Text(stringResource(R.string.error_incorrect_password))
                },
                singleLine = true,
                modifier = Modifier.semantics { contentDescription = "Password Text" }

            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (PasswordUtils.verifyPassword(password, hashedPassword)) {
                    onSuccess()
                } else {
                    isError = true
                }
            }) {
                Text(
                    text = stringResource(R.string.ok),
                    color = colorResource(R.color.light_blue_600)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = colorResource(R.color.light_blue_600)
                )
            }
        }
    )
}
