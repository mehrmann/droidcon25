package de.kodierer.droidcon25

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.kodierer.droidcon25.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentTheme by remember { mutableStateOf(AppTheme.ROYAL) }

            Droidcon25DemoApplicationTheme(theme = currentTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ColorDemoScreen(
                        currentTheme = currentTheme,
                        onThemeChange = { currentTheme = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorDemoScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello Droidcon",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ThemeSelector(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        ColorSwatchGrid()
    }
}

@Composable
fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.primaryDark
            ),
            border = BorderStroke(1.dp, colors.primaryDark)
        ) {
            Text(
                text = "Theme: ${currentTheme.displayName}",
                color = colors.primaryDark
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = theme.displayName,
                            color = colors.onSurface
                        )
                    },
                    onClick = {
                        onThemeChange(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class ColorSwatch(
    val name: String,
    val color: Color,
    val onColor: Color
)

@Composable
fun ColorSwatchGrid() {
    val colors = LocalAppColors.current

    val colorSwatches = listOf(
        ColorSwatch("Primary", colors.primary, colors.onPrimary),
        ColorSwatch("Primary Light", colors.primaryLight, colors.onPrimary),
        ColorSwatch("Primary Dark", colors.primaryDark, colors.onPrimary),
        ColorSwatch("Secondary", colors.secondary, colors.onSecondary),
        ColorSwatch("Secondary Light", colors.secondaryLight, colors.onSecondary),
        ColorSwatch("Secondary Dark", colors.secondaryDark, colors.onSecondary),
        ColorSwatch("Primary Container", colors.primaryContainer, colors.onPrimaryContainer),
        ColorSwatch("Secondary Container", colors.secondaryContainer, colors.onSecondaryContainer),
        ColorSwatch("Tertiary", colors.tertiary, colors.onTertiary)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(colorSwatches) { swatch ->
            ColorSwatchItem(swatch)
        }
    }
}

@Composable
fun ColorSwatchItem(swatch: ColorSwatch) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = swatch.color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = swatch.name,
                color = swatch.onColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("#%06X", swatch.color.toArgb() and 0xFFFFFF),
                color = swatch.onColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorDemoPreview() {
    Droidcon25DemoApplicationTheme {
        ColorDemoScreen(
            currentTheme = AppTheme.ROYAL,
            onThemeChange = { }
        )
    }
}
