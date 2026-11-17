package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme

private val CoeInputMinHeight = 56.dp

/**
 * Campo de texto Outlined estilo `coe-ui-kit` (`.coe-input`):
 * radio de 8.dp (`shapes.small`), altura mínima móvil de 56.dp, fondo blanco,
 * borde primario en foco, rojo en error, verde opcional en `isValid` y texto de error alineado a la izquierda.
 */
@Composable
fun BupasTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isRequired: Boolean = false,
    isError: Boolean = false,
    isValid: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val showError = isError || !errorMessage.isNullOrBlank()
    val displayLabel = if (isRequired) "$label *" else label

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = CoeInputMinHeight),
            label = {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            placeholder = placeholder?.let {
                { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = showError,
            readOnly = readOnly,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = keyboardOptions,
            colors = coeOutlinedTextFieldColors(
                isValid = isValid && !showError,
                readOnly = readOnly
            )
        )
        AnimatedVisibility(
            visible = showError && !errorMessage.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CoeInputErrorText(message = errorMessage.orEmpty())
        }
    }
}

/**
 * Campo de contraseña Outlined estilo `coe-ui-kit` (`.coe-input` / `coe-input-password`).
 */
@Composable
fun BupasPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = true,
    isError: Boolean = false,
    isValid: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val showError = isError || !errorMessage.isNullOrBlank()
    val displayLabel = if (isRequired) "$label *" else label

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = CoeInputMinHeight),
            label = {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = leadingIcon,
            isError = showError,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.small,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        }
                    )
                }
            },
            colors = coeOutlinedTextFieldColors(isValid = isValid && !showError)
        )
        AnimatedVisibility(
            visible = showError && !errorMessage.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CoeInputErrorText(message = errorMessage.orEmpty())
        }
    }
}

/**
 * Campo reutilizable para Código de Verificación 2FA de 6 dígitos (HU-001).
 */
@Composable
fun BupasOtpField(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    remainingSeconds: Int = 180,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    Column(modifier = modifier.fillMaxWidth()) {
        BupasTextField(
            value = code,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }.take(6)
                onCodeChange(filtered)
            },
            label = "Código de Verificación (6 dígitos)",
            placeholder = "000000",
            isRequired = true,
            isError = isError,
            errorMessage = errorMessage,
            enabled = enabled,
            leadingIcon = leadingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        Text(
            text = "Vigencia del código: $timerText",
            style = MaterialTheme.typography.bodySmall,
            color = if (remainingSeconds <= 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = BupasTheme.spacing.xs)
        )
    }
}

/**
 * Control numérico especializado para capturar "Cantidad Acta" por artículo BUPAS en campo (HU-006).
 * Rechaza valores negativos o decimales sin recortar signos (`-`, `.`, `,`), propagando la entrada
 * cruda cuando se provee [onRawQuantityChange] y mostrando estado visual de error en rojo (R2.3).
 */
@Composable
fun BupasQuantityActaField(
    quantityActa: Int?,
    onQuantityChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    onRawQuantityChange: ((String) -> Unit)? = null,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val showError = isError || !errorMessage.isNullOrBlank()
    var rawText by rememberSaveable(quantityActa) { mutableStateOf(quantityActa?.toString().orEmpty()) }
    val applyStep: (Int) -> Unit = { nextQty ->
        rawText = nextQty.toString()
        onRawQuantityChange?.invoke(rawText) ?: onQuantityChange(nextQty)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
        ) {
            if (!readOnly) {
                QuantityStepButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Disminuir Cantidad Acta",
                    onClick = { applyStep(((quantityActa ?: 0) - 1).coerceAtLeast(0)) }
                )
            }
            OutlinedTextField(
                value = if (readOnly) quantityActa?.toString().orEmpty() else rawText,
                onValueChange = { raw ->
                    rawText = raw
                    handleQuantityTextInput(raw, onRawQuantityChange, onQuantityChange)
                },
                modifier = Modifier.width(88.dp),
                readOnly = readOnly,
                singleLine = true,
                isError = showError,
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = coeOutlinedTextFieldColors(isValid = false, readOnly = readOnly)
            )
            if (!readOnly) {
                QuantityStepButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Aumentar Cantidad Acta",
                    onClick = { applyStep((quantityActa ?: 0) + 1) }
                )
            }
        }
        AnimatedVisibility(
            visible = showError && !errorMessage.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CoeInputErrorText(message = errorMessage.orEmpty())
        }
    }
}

@Composable
private fun QuantityStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(
            minWidth = BupasTheme.spacing.minTouchTarget,
            minHeight = BupasTheme.spacing.minTouchTarget
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

private fun handleQuantityTextInput(
    raw: String,
    onRawQuantityChange: ((String) -> Unit)?,
    onQuantityChange: (Int?) -> Unit
) {
    if (onRawQuantityChange != null) {
        onRawQuantityChange(raw)
        return
    }
    if (raw.isEmpty()) {
        onQuantityChange(null)
    } else if (raw.all { it.isDigit() }) {
        raw.toIntOrNull()?.let(onQuantityChange)
    }
}

@Composable
private fun CoeInputErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = BupasTheme.spacing.xs)
    )
}

@Composable
private fun coeOutlinedTextFieldColors(
    isValid: Boolean,
    readOnly: Boolean = false
): TextFieldColors {
    val validBorderColor = BupasTheme.extendedColors.successAccent
    val containerColor = if (readOnly) {
        BupasTheme.extendedColors.readOnlySurface
    } else {
        MaterialTheme.colorScheme.surface
    }
    val unfocusedBorder = when {
        readOnly -> BupasTheme.extendedColors.borderDisabled
        isValid -> validBorderColor
        else -> MaterialTheme.colorScheme.outline
    }
    val focusedBorder = when {
        readOnly -> BupasTheme.extendedColors.borderDisabled
        isValid -> validBorderColor
        else -> MaterialTheme.colorScheme.primary
    }

    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = BupasTheme.extendedColors.readOnlySurface,
        errorContainerColor = containerColor,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.outline,
        focusedBorderColor = focusedBorder,
        unfocusedBorderColor = unfocusedBorder,
        disabledBorderColor = BupasTheme.extendedColors.borderDisabled,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.outline,
        errorLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorCursorColor = MaterialTheme.colorScheme.error,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.outline,
        errorTrailingIconColor = MaterialTheme.colorScheme.error
    )
}

@Preview(showBackground = true)
@Composable
private fun BupasTextFieldsPreview() {
    BupasTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BupasTextField(
                value = "",
                onValueChange = {},
                label = "Usuario",
                isRequired = true,
                isError = true,
                errorMessage = "Campo requerido"
            )
            BupasTextField(
                value = "conductor.bupas",
                onValueChange = {},
                label = "Usuario válido",
                isValid = true
            )
            BupasPasswordField(
                value = "secret123",
                onValueChange = {},
                label = "Contraseña"
            )
            BupasOtpField(
                code = "123",
                onCodeChange = {},
                remainingSeconds = 0,
                isError = true,
                errorMessage = "El Código de verificación ha expirado"
            )
            BupasQuantityActaField(
                quantityActa = 12,
                onQuantityChange = {}
            )
        }
    }
}
