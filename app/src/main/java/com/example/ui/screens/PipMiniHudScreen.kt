package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DrowsinessState
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.SafetyCritical
import com.example.ui.theme.SafetySafe
import com.example.ui.theme.SafetyWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DriverMonitorViewModel

@Composable
fun PipMiniHudScreen(
    viewModel: DriverMonitorViewModel,
    modifier: Modifier = Modifier
) {
    val drowsinessState by viewModel.drowsinessState.collectAsState()
    val breakdown by viewModel.drowsinessBreakdown.collectAsState()
    val monitoringStatus by viewModel.monitoringStatus.collectAsState()

    val stateColor = when (drowsinessState) {
        DrowsinessState.SAFE -> SafetySafe
        DrowsinessState.WARNING -> SafetyWarning
        DrowsinessState.DROWSY, DrowsinessState.CRITICAL -> SafetyCritical
        DrowsinessState.NO_FACE -> CockpitBorder
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (drowsinessState == DrowsinessState.DROWSY || drowsinessState == DrowsinessState.CRITICAL) Color(0xFF450A0A) else CockpitBackground)
            .padding(8.dp)
            .testTag("pip_mini_hud_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Status & Speed Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(stateColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .border(1.dp, stateColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = drowsinessState.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = stateColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${monitoringStatus.currentSpeedKmh.toInt()} km/h",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Central Score Gauge Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (drowsinessState == DrowsinessState.DROWSY) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = SafetyCritical,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = "${breakdown.drowsinessScore}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = stateColor,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 36.sp
                    )
                )
                Text(
                    text = "/100",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(start = 2.dp, top = 8.dp)
                )
            }

            Text(
                text = if (drowsinessState == DrowsinessState.DROWSY) "CRITICAL ALERT!" else "FATIGUE INDEX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (drowsinessState == DrowsinessState.DROWSY) SafetyCritical else TextSecondary
                )
            )
        }
    }
}
