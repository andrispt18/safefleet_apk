package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DrowsinessState
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.SafetyCritical
import com.example.ui.theme.SafetySafe
import com.example.ui.theme.SafetyWarning
import com.example.ui.theme.TextSecondary

@Composable
fun DrowsinessGauge(
    score: Int,
    state: DrowsinessState,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 350),
        label = "gaugeScore"
    )

    val gaugeColor by animateColorAsState(
        targetValue = when (state) {
            DrowsinessState.SAFE -> SafetySafe
            DrowsinessState.WARNING -> SafetyWarning
            DrowsinessState.DROWSY, DrowsinessState.CRITICAL -> SafetyCritical
            DrowsinessState.NO_FACE -> CockpitBorder
        },
        animationSpec = tween(durationMillis = 300),
        label = "gaugeColor"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("drowsiness_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.toPx() - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Background track (240 degree arc)
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active progress arc
            val sweep = (animatedScore / 100f) * 240f
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to SafetySafe,
                        0.5f to SafetyWarning,
                        1.0f to SafetyCritical
                    ),
                    startAngle = 150f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = gaugeColor
                ),
                modifier = Modifier.testTag("gauge_score_value")
            )
            Text(
                text = state.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = gaugeColor
                ),
                modifier = Modifier.testTag("gauge_state_label")
            )
            Text(
                text = "DROWSINESS INDEX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            )
        }
    }
}
