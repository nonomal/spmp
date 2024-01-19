package com.toasterofbread.spmp.model.settings.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.getThemeCategoryItems
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode

data object ThemeSettings: SettingsCategory("theme") {
    override val keys: List<SettingsKey> = Key.entries.toList()

    override fun getPage(): Page? =
        Page(
            getString("s_cat_theme"),
            getString("s_cat_desc_theme"),
            { getThemeCategoryItems(it) }
        ) { Icons.Outlined.Palette }

    enum class Key: SettingsKey {
        CURRENT_THEME,
        THEMES,
        ACCENT_COLOUR_SOURCE,
        NOWPLAYING_THEME_MODE,
        NOWPLAYING_DEFAULT_GRADIENT_DEPTH,
        NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY,
        NOWPLAYING_DEFAULT_IMAGE_SHADOW_RADIUS;

        override val category: SettingsCategory get() = ThemeSettings

        @Suppress("UNCHECKED_CAST")
        override fun <T> getDefaultValue(): T =
            when (this) {
                ACCENT_COLOUR_SOURCE -> AccentColourSource.THUMBNAIL.ordinal
                CURRENT_THEME -> 0
                THEMES -> "[]"
                NOWPLAYING_THEME_MODE -> ThemeMode.DEFAULT.ordinal
                NOWPLAYING_DEFAULT_GRADIENT_DEPTH -> 1f
                NOWPLAYING_DEFAULT_BACKGROUND_IMAGE_OPACITY -> 0.0f
                NOWPLAYING_DEFAULT_IMAGE_SHADOW_RADIUS -> 0.5f
            } as T
    }
}

enum class AccentColourSource {
    THEME, THUMBNAIL
}