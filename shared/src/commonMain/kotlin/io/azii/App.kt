
package io.azii

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Background = Color(0xFF131416)
private val SurfaceColor = Color(0xFF1D1F22)
private val Accent = Color(0xFFA8C7FA)
private val Green = Color(0xFF35C978)
private val TextPrimary = Color(0xFFECECEC)
private val TextSecondary = Color(0xFF9299A3)

@Composable
fun App(
    onStartListening: (
        onStarted: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit = { onStarted, _ -> onStarted() },
    onStopListening: () -> Unit = {}
) {
    var selectedScreen by remember { mutableStateOf("Azii") }
    var listeningEnabled by remember { mutableStateOf(false) }

var statusMessage by remember {
    mutableStateOf("Azii выключен")
}

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {

                // Верхняя панель
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Azii",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )

                    Text(
                        text = selectedScreen,
                        color = TextSecondary
                    )
                }

                // Основная область
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedScreen == "Azii") {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {

                            Text(
                                text = "Голосовой помощник",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            // Круглая кнопка Azii
                            Box(
                                modifier = Modifier.size(190.dp),
                                contentAlignment = Alignment.Center
                            ) {

                                if (listeningEnabled) {

                                    val transition =
                                        rememberInfiniteTransition()

                                    // Расходящееся световое кольцо
                                    val ringScale by transition.animateFloat(
                                        initialValue = 0.9f,
                                        targetValue = 1.55f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(
                                                durationMillis = 1400,
                                                easing = FastOutSlowInEasing
                                            ),
                                            repeatMode = RepeatMode.Restart
                                        )
                                    )

                                    val ringAlpha =
                                        (
                                            1f - (ringScale - 0.9f) / 0.65f
                                        ).coerceIn(0f, 1f) * 0.35f

                                    // Лёгкая пульсация самой кнопки
                                    val buttonScale by transition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.06f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(
                                                durationMillis = 700,
                                                easing = FastOutSlowInEasing
                                            ),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(126.dp)
                                            .scale(ringScale)
                                            .alpha(ringAlpha)
                                            .background(
                                                Green,
                                                CircleShape
                                            )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(124.dp)
                                            .scale(buttonScale)
                                            .clip(CircleShape)
                                            .background(Green)
.clickable {
    onStopListening()
    listeningEnabled = false
    statusMessage = "Запись завершена"
},
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Выключить",
                                            color = Color(0xFF102319),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                } else {

                                    Box(
                                        modifier = Modifier
                                            .size(124.dp)
                                            .clip(CircleShape)
                                            .background(Green)
.clickable {
    statusMessage = "Запрашиваем доступ к микрофону"

    onStartListening(
        {
            listeningEnabled = true
            statusMessage = "Azii слушает"
        },
        { error ->
            listeningEnabled = false
            statusMessage = error
        }
    )
},
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Включить",
                                            color = Color(0xFF102319),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }

                            // Статус
                            Text(
                                text = statusMessage,
                                color = if (listeningEnabled) {
                                    Green
                                } else {
                                    TextSecondary
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                    } else {

                        Text(
                            text = selectedScreen,
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }

                // Нижнее меню
                NavigationBar(
                    containerColor = SurfaceColor
                ) {
                    listOf(
                        "Календарь",
                        "Клиенты",
                        "Azii",
                        "Настройки"
                    ).forEach { screen ->

                        NavigationBarItem(
                            selected = selectedScreen == screen,
                            onClick = {
                                selectedScreen = screen
                            },
                            icon = {},
                            label = {
                                Text(screen)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                indicatorColor = Color(0xFF30343A),
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}
