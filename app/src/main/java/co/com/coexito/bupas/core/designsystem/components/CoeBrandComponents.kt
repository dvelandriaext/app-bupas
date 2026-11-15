package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme

private val AuthCardMaxWidth = 420.dp
private const val HeroBlueHeightFraction = 0.43f

/**
 * Logotipo corporativo Coéxito ("coéxito" + isotipo naranja) alineado con Portal de Aplicaciones.
 */
@Composable
fun CoeBrandLogo(
    modifier: Modifier = Modifier
) {
    val primaryBlue = MaterialTheme.colorScheme.primary
    val accentOrange = MaterialTheme.colorScheme.secondary
    val markContentColor = MaterialTheme.colorScheme.onSecondary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "coéxito",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            ),
            color = primaryBlue
        )
        Spacer(modifier = Modifier.width(BupasTheme.spacing.xs))
        CoeBrandMark(
            containerColor = accentOrange,
            emblemColor = markContentColor,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun CoeBrandMark(
    containerColor: Color,
    emblemColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val corner = size.minDimension * 0.14f
        drawRoundRect(
            color = containerColor,
            cornerRadius = CornerRadius(corner, corner)
        )
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.22f)
            cubicTo(w * 0.48f, h * 0.46f, w * 0.34f, h * 0.64f, w * 0.24f, h * 0.72f)
            cubicTo(w * 0.42f, h * 0.70f, w * 0.58f, h * 0.70f, w * 0.76f, h * 0.72f)
            cubicTo(w * 0.64f, h * 0.62f, w * 0.52f, h * 0.46f, w * 0.50f, h * 0.22f)
            close()
        }
        drawPath(
            path = path,
            color = emblemColor,
            style = Stroke(
                width = size.minDimension * 0.10f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawCircle(
            color = emblemColor,
            radius = size.minDimension * 0.08f,
            center = Offset(w * 0.50f, h * 0.24f)
        )
        drawCircle(
            color = emblemColor,
            radius = size.minDimension * 0.07f,
            center = Offset(w * 0.25f, h * 0.72f)
        )
        drawCircle(
            color = emblemColor,
            radius = size.minDimension * 0.07f,
            center = Offset(w * 0.75f, h * 0.72f)
        )
    }
}

/**
 * Contenedor visual corporativo con fondo dividido (banda azul Coéxito superior + base blanca)
 * y tarjeta flotante centrada con el logotipo institucional, inspirado en Portal de Aplicaciones Coéxito.
 */
@Composable
fun CoeAuthSplitLayout(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    footerContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val heroHeight = maxHeight * HeroBlueHeightFraction

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BupasTheme.extendedColors.primaryGradientEnd,
                            MaterialTheme.colorScheme.primary
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BupasTheme.spacing.lg, vertical = BupasTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = AuthCardMaxWidth),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = BupasTheme.spacing.lg,
                            vertical = BupasTheme.spacing.xl
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
                ) {
                    CoeBrandLogo()

                    Spacer(modifier = Modifier.height(BupasTheme.spacing.xs))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(BupasTheme.spacing.sm))

                    content()
                }
            }

            if (footerContent != null) {
                Spacer(modifier = Modifier.height(BupasTheme.spacing.lg))
                Box(modifier = Modifier.widthIn(max = AuthCardMaxWidth)) {
                    footerContent()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CoeAuthSplitLayoutPreview() {
    BupasTheme {
        CoeAuthSplitLayout(
            title = "BUPAS Móvil",
            subtitle = "Coéxito Logística Reversiva • Conductor"
        ) {
            BupasTextField(
                value = "",
                onValueChange = {},
                label = "Usuario",
                isRequired = true
            )
            BupasPrimaryButton(
                text = "Iniciar Sesión",
                onClick = {}
            )
        }
    }
}
