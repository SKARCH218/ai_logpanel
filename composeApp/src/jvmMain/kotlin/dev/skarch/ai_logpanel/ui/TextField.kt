package dev.skarch.ai_logpanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 명령어 입력 바
@Composable
fun CommandInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onExecute: () -> Unit,
    enabled: Boolean,
    pointColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF252932),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter &&
                            event.type == KeyEventType.KeyUp &&
                            enabled &&
                            value.isNotBlank()) {
                            onExecute()
                            true
                        } else {
                            false
                        }
                    },
                placeholder = { Text("명령어 입력...", color = Color(0xFF6B7280)) },
                singleLine = true,
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = pointColor,
                    unfocusedBorderColor = Color(0xFF374151),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color(0xFF6B7280),
                    disabledBorderColor = Color(0xFF374151)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedButton(
                onClick = onExecute,
                enabled = enabled && value.isNotBlank(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = pointColor,
                    disabledContentColor = Color(0xFF6B7280)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (enabled) pointColor else Color(0xFF374151)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("실행", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color(0xFF9CA3AF),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF4B5563),
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color(0xFF374151),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF2196F3),
                focusedPlaceholderColor = Color(0xFF4B5563),
                unfocusedPlaceholderColor = Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(10.dp),
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
        )
    }
}

@Composable
fun FormTextFieldLarge(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color(0xFF9CA3AF),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF4B5563),
                    fontSize = 16.sp
                )
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color(0xFF374151),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF2196F3),
                focusedPlaceholderColor = Color(0xFF4B5563),
                unfocusedPlaceholderColor = Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}