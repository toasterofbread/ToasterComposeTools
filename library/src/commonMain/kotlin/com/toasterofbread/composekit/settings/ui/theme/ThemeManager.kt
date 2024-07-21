package dev.toastbits.composekit.settings.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class ThemeManager(
    initial_theme: ThemeValues,
    private val composable_coroutine_scope: CoroutineScope
) {
    private val animated_theme_values: AnimatedThemeValues = AnimatedThemeValues(initial_theme)
    private var current_thumbnail_colour: Color? = null

    val values: ThemeValues = animated_theme_values
    var preview_theme: ThemeValues? by mutableStateOf(null)
        private set
    var current_theme: ThemeValues by mutableStateOf(initial_theme)
        private set

    abstract fun selectAccentColour(values: ThemeValues, thumbnail_colour: Color?): Color

    fun isPreviewActive(): Boolean = preview_theme != null

    fun setTheme(new_theme: ThemeValues) {
        if (new_theme == current_theme) {
            return
        }

        current_theme = new_theme
        updateColours()
    }

    fun setPreviewTheme(new_preview_theme: ThemeValues?) {
        if (new_preview_theme == preview_theme) {
            return
        }

        preview_theme = new_preview_theme
        updateColours()
    }

    fun onThumbnailColourChanged(thumbnail_colour: Color?) {
        if (thumbnail_colour == current_thumbnail_colour) {
            return
        }

        current_thumbnail_colour = thumbnail_colour
        updateColours()
    }

    private fun updateColours() {
        composable_coroutine_scope.launch {
            val current_values: ThemeValues = preview_theme ?: current_theme
            val accent_colour: Color = selectAccentColour(current_values, current_thumbnail_colour)
            val theme_values: ThemeValues = ThemeValuesData.of(current_values).copy(accent = accent_colour)
            animated_theme_values.updateColours(theme_values)
        }
    }
}