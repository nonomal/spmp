package com.spectre7.spmp.ui.layout

import com.spectre7.spmp.platform.PlatformContext
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.PlayerAccessibilityService
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.spectre7.composesettings.ui.SettingsInterface
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.composesettings.ui.SettingsPageWithItems
import com.spectre7.settings.model.*
import com.spectre7.settings.ui.SettingsItemThemeSelector
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.PlatformAlertDialog
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.spmp.ui.theme.ThemeData
import com.spectre7.spmp.ui.theme.ThemeManager
import com.spectre7.utils.*
import com.spectre7.utils.getString
import com.spectre7.spmp.platform.YoutubeMusicLogin

private enum class Page { ROOT, YOUTUBE_MUSIC_LOGIN }
private enum class Category {
    GENERAL,
    FEED,
    THEME,
    LYRICS,
    DOWNLOAD,
    ACCESSIBILITY_SERVICE;

    fun getIcon(filled: Boolean = false): ImageVector = when (this) {
        GENERAL -> if (filled) Icons.Filled.Settings else Icons.Outlined.Settings
        FEED -> if (filled) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted
        THEME -> if (filled) Icons.Filled.Palette else Icons.Outlined.Palette
        LYRICS -> if (filled) Icons.Filled.MusicNote else Icons.Outlined.MusicNote
        DOWNLOAD -> if (filled) Icons.Filled.Download else Icons.Outlined.Download
        ACCESSIBILITY_SERVICE -> if (filled) Icons.Filled.Accessibility else Icons.Outlined.Accessibility
    }

    fun getTitle(): String = when (this) {
        GENERAL -> getString("s_group_general")
        FEED -> getString("s_group_home_feed")
        THEME -> getString("s_group_theming")
        LYRICS -> getString("s_group_lyrics")
        DOWNLOAD -> getString("s_group_download")
        ACCESSIBILITY_SERVICE -> getString("s_group_acc_service")
    }
}

@Composable
fun PrefsPage(pill_menu: PillMenu, playerProvider: () -> PlayerViewContext, close: () -> Unit) {
    var current_category: Category by remember { mutableStateOf(Category.GENERAL) }

    val interface_lang = remember { SettingsValueState<Int>(Settings.KEY_LANG_UI.name).init(Settings.prefs, Settings.Companion::provideDefault) }
    var language_data by remember { mutableStateOf(SpMp.languages.values.elementAt(interface_lang.value)) }
    OnChangedEffect(interface_lang.value) {
        language_data = SpMp.languages.values.elementAt(interface_lang.value)
    }

    val ytm_auth = remember {
        SettingsValueState(
            Settings.KEY_YTM_AUTH.name,
            converter = { set ->
                set?.let { YoutubeMusicAuthInfo(it as Set<String>) } ?: YoutubeMusicAuthInfo()
            }
        ).init(Settings.prefs, Settings.Companion::provideDefault)
    }

    lateinit var settings_interface: SettingsInterface

    val pill_menu_action_overrider: @Composable PillMenu.Action.(i: Int) -> Boolean = remember { { i ->
        if (i == 0) {
            var go_back by remember { mutableStateOf(false) }
            LaunchedEffect(go_back) {
                if (go_back) {
                    settings_interface.goBack()
                }
            }

            ActionButton(
                Icons.Filled.ArrowBack
            ) {
                go_back = true
            }
            true
        }
        else {
            false
        }
    } }

    var show_reset_confirmation by remember { mutableStateOf(false) }

    var reset by remember { mutableStateOf(false) }
    OnChangedEffect(reset) {
        settings_interface.current_page.resetKeys()
    }

    if (show_reset_confirmation) {
        PlatformAlertDialog(
            { show_reset_confirmation = false },
            confirmButton = {
                FilledTonalButton(
                    {
                        reset = !reset
                        show_reset_confirmation = false
                    }
                ) {
                    Text(getString("action_confirm_action"))
                }
            },
            dismissButton = { TextButton( { show_reset_confirmation = false } ) { Text(getString("action_deny_action")) } },
            title = { Text(getString("prompt_confirm_action")) },
            text = {
                Text(getString("prompt_confirm_settings_page_reset"))
            }
        )
    }

    LaunchedEffect(Unit) {
        pill_menu.addExtraAction {
            if (it == 1) {
                ActionButton(
                    Icons.Filled.Refresh
                ) {
                    show_reset_confirmation = true
                }
            }
        }
        pill_menu.addAlongsideAction {
            Row(fill_modifier
                .border(1.dp, background_colour, CircleShape)
                .padding(horizontal = 5.dp)
            ) {
                for (category in Category.values()) {
                    Box(
                        Modifier.fillMaxWidth(1f / (Category.values().size - category.ordinal).toFloat()),
                        contentAlignment = Alignment.Center
                    ) {

                        Crossfade(category == current_category) { current ->
                            val button_colour = if (current) background_colour else Color.Transparent
                            ShapedIconButton(
                                { current_category = category },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = button_colour,
                                    contentColor = button_colour.getContrasted()
                                )
                            ) {
                                Icon(category.getIcon(current), null)
                            }
                        }
                    }
                }
            }
        }
    }

    settings_interface = remember {
        SettingsInterface(
            { Theme.current },
            Page.ROOT.ordinal,
            SpMp.context,
            Settings.prefs,
            Settings.Companion::provideDefault,
            pill_menu,
            {
                when (Page.values()[it]) {
                    Page.ROOT -> getRootPage({ current_category }, interface_lang, language_data, ytm_auth, playerProvider)
                    Page.YOUTUBE_MUSIC_LOGIN -> getYoutubeMusicLoginPage(ytm_auth)
                }
            },
            { page: Int? ->
                if (page == Page.ROOT.ordinal) {
                    pill_menu.removeActionOverrider(pill_menu_action_overrider)
                }
                else {
                    pill_menu.addActionOverrider(pill_menu_action_overrider)
                }
            },
            {
                close()
            }
        )
    }

    BoxWithConstraints(
        Modifier
            .background { Theme.current.background }
            .pointerInput(Unit) {}
    ) {
        settings_interface.Interface(
            SpMp.context.getScreenHeight() - SpMp.context.getStatusBarHeight(),
            content_padding = PaddingValues(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp)
        )
    }
}

private fun getRootPage(
    getCategory: () -> Category,
    interface_lang: SettingsValueState<Int>,
    language_data: Map<String, String>,
    ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>,
    playerProvider: () -> PlayerViewContext
): SettingsPage {
    return SettingsPageWithItems(
        { getCategory().getTitle() },
        {
            when (getCategory()) {
                Category.GENERAL -> getGeneralCategory(interface_lang, language_data, ytm_auth, playerProvider)
                Category.FEED -> getFeedCategory()
                Category.THEME -> getThemeCategory(Theme.manager)
                Category.LYRICS -> getLyricsCategory()
                Category.DOWNLOAD -> getDownloadCategory()
                Category.ACCESSIBILITY_SERVICE -> getAccessibilityServiceCategory()
            }
        }
    )
}

private fun getAccessibilityServiceCategory(): List<SettingsItem> {
    if (!PlayerAccessibilityService.isSupported()) {
        return emptyList()
    }

    return listOf(
        SettingsItemAccessibilityService(
            getString("s_acc_service_enabled"),
            getString("s_acc_service_disabled"),
            getString("s_acc_service_enable"),
            getString("s_acc_service_disable"),
            object : SettingsItemAccessibilityService.AccessibilityServiceBridge {
                override fun addEnabledListener(
                    listener: (Boolean) -> Unit,
                    context: PlatformContext
                ) {
                    PlayerAccessibilityService.addEnabledListener(listener, context)
                }

                override fun removeEnabledListener(
                    listener: (Boolean) -> Unit,
                    context: PlatformContext
                ) {
                    PlayerAccessibilityService.removeEnabledListener(listener, context)
                }

                override fun isEnabled(context: PlatformContext): Boolean {
                    return PlayerAccessibilityService.isEnabled(context)
                }

                override fun setEnabled(enabled: Boolean) {
                    if (!enabled) {
                        PlayerAccessibilityService.disable()
                        return
                    }

                    val context = SpMp.context
                    if (PlayerAccessibilityService.isSettingsPermissionGranted(context)) {
                        PlayerAccessibilityService.enable(context, true)
                        return
                    }

                    TODO()
//                    val dialog = AlertDialog.Builder(MainActivity.context)
//                    dialog.setCancelable(true)
//                    dialog.setTitle(getString("acc_ser_enable_dialog_title"))
//                    dialog.setMessage(getString("acc_ser_enable_dialog_body"))
//                    dialog.setPositiveButton(getString("acc_ser_enable_dialog_btn_root")) { _, _ ->
//                        PlayerAccessibilityService.enable(MainActivity.context, true)
//                    }
//                    dialog.setNeutralButton(getString("acc_ser_enable_dialog_btn_manual")) { _, _ ->
//                        PlayerAccessibilityService.enable(MainActivity.context, false)
//                    }
//                    dialog.setNegativeButton(getString("action_cancel")) { _, _ -> }
//                    dialog.create().show()
                }
            }
        ),

//        SettingsItemMultipleChoice(
//            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_MODE.name),
//            getString("s_key_vol_intercept_mode"),
//            getString("s_sub_vol_intercept_mode"),
//            PlayerAccessibilityService.PlayerAccessibilityServiceVolumeInterceptMode.values().size,
//            false
//        ) { mode ->
//            when (mode) {
//                0 -> getString("s_option_vol_intercept_mode_always")
//                1 -> getString("s_option_vol_intercept_mode_app")
//                else -> getString("s_option_vol_intercept_mode_never")
//            }
//        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_ACC_SCREEN_OFF.name),
            getString("s_key_acc_screen_off"),
            getString("s_sub_acc_screen_off")
        ) { checked, _, allowChange ->
            if (!checked) {
                allowChange(true)
                return@SettingsItemToggle
            }

            PlayerAccessibilityService.requestRootPermission(allowChange)
        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name),
            getString("s_key_vol_intercept_notification"),
            getString("s_key_vol_intercept_notification")
        ) { checked, _, allowChange ->
            if (!checked) {
                allowChange(true)
                return@SettingsItemToggle
            }

            if (!PlayerAccessibilityService.isOverlayPermissionGranted(SpMp.context)) {
                PlayerAccessibilityService.requestOverlayPermission(SpMp.context) { success ->
                    allowChange(success)
                }
                return@SettingsItemToggle
            }

            allowChange(true)
        }

    )
}

private fun getYoutubeMusicLoginPage(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>): SettingsPage {
    return object : SettingsPage() {
        override val disable_padding: Boolean = true
        override val scrolling: Boolean = false

        @Composable
        override fun PageView(
            content_padding: PaddingValues,
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            YoutubeMusicLogin(Modifier.fillMaxSize()) { auth_info ->
                auth_info.fold({
                    ytm_auth.value = it
                }, {
                    throw RuntimeException(it)
                })
                goBack()
            }
        }

        override suspend fun resetKeys() {
            ytm_auth.reset()
        }
    }
}

private fun getAuthSettingsItem(ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>, playerProvider: () -> PlayerViewContext): SettingsItem {
    return object : SettingsItem() {
        override fun initialiseValueStates(
            prefs: ProjectPreferences,
            default_provider: (String) -> Any,
        ) {}

        override fun resetValues() {
            ytm_auth.reset()
        }

        @Composable
        override fun GetItem(
            theme: Theme,
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
        ) {
            Box(Modifier
                .background(Theme.current.vibrant_accent, SETTINGS_ITEM_ROUNDED_SHAPE)
                .padding(horizontal = 10.dp)
            ) {
                Crossfade(ytm_auth.value) { auth ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (auth.initialised) {
                            auth.own_channel.PreviewLong(MediaItem.PreviewParams(
                                playerProvider,
                                Modifier.weight(1f),
                                content_colour = Theme.current.on_accent_provider
                            ))
                        }
                        else {
                            Text(
                                getStringTemp("Not signed in"),
                                Modifier.fillMaxWidth().weight(1f),
                                style = LocalTextStyle.current.copy(color = Theme.current.on_accent)
                            )
                        }

                        Button({
                            if (auth.initialised) {
                                resetValues()
                            }
                            else {
                                openPage(Page.YOUTUBE_MUSIC_LOGIN.ordinal)
                            }
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = Theme.current.background,
                            contentColor = Theme.current.on_background
                        )) {
                            Text(getStringTemp(if (auth.initialised) "Sign out" else "Sign in"))
                        }

                        ShapedIconButton(
                            {

                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Theme.current.background,
                                contentColor = Theme.current.on_background
                            )
                        ) {
                            Icon(Icons.Filled.Info, null)
                        }
                    }
                }
            }
        }
    }
}

private fun getGeneralCategory(
    interface_lang: SettingsValueState<Int>,
    language_data: Map<String, String>,
    ytm_auth: SettingsValueState<YoutubeMusicAuthInfo>,
    playerProvider: () -> PlayerViewContext
): List<SettingsItem> {
    return listOf(
        getAuthSettingsItem(ytm_auth, playerProvider),

        SettingsItemSpacer(10.dp),

        SettingsItemDropdown(
            interface_lang,
            getString("s_key_interface_lang"), getString("s_sub_interface_lang"),
            SpMp.languages.values.first().size,
            { i ->
                language_data.entries.elementAt(i).key
            }
        ) { i ->
            val language = language_data.entries.elementAt(i)
            "${language.key} / ${language.value}"
        },

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_LANG_DATA.name),
            getString("s_key_data_lang"), getString("s_sub_data_lang"),
            SpMp.languages.values.first().size,
            { i ->
                language_data.entries.elementAt(i).key
            }
        ) { i ->
            val language = language_data.entries.elementAt(i)
            "${language.key} / ${language.value}"
        },

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_VOLUME_STEPS.name),
            getString("s_key_vol_steps"),
            getString("s_sub_vol_steps"),
            "0",
            "100",
            range = 0f .. 100f
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_OPEN_NP_ON_SONG_PLAYED.name),
            getString("s_key_open_np_on_song_played"),
            getString("s_sub_open_np_on_song_played")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_PERSISTENT_QUEUE.name),
            getString("s_key_persistent_queue"),
            getString("s_sub_persistent_queue")
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_ADD_SONGS_TO_HISTORY.name),
            getString("s_key_add_songs_to_history"),
            getString("s_sub_add_songs_to_history")
        )
    )
}

private fun getFeedCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_FEED_INITIAL_ROWS.name),
            getString("s_key_feed_initial_rows"),
            getString("s_sub_feed_initial_rows"),
            "1",
            "10",
            range = 1f .. 10f
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_RADIOS.name),
            getString("s_key_feed_show_radios"), null
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_LISTEN_ROW.name),
            getString("s_key_feed_show_listen_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_MIX_ROW.name),
            getString("s_key_feed_show_mix_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_NEW_ROW.name),
            getString("s_key_feed_show_new_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_MOODS_ROW.name),
            getString("s_key_feed_show_moods_row"), null
        ),
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_FEED_SHOW_CHARTS_ROW.name),
            getString("s_key_feed_show_charts_row"), null
        )
    )
}

private fun getThemeCategory(theme_manager: ThemeManager): List<SettingsItem> {
    return listOf(
        SettingsItemThemeSelector (
            SettingsValueState(Settings.KEY_CURRENT_THEME.name),
            getString("s_key_current_theme"), null,
            getString("s_theme_editor_title"),
            {
                check(theme_manager.themes.isNotEmpty())
                theme_manager.themes.size
            },
            { theme_manager.themes[it] },
            { index: Int, edited_theme: ThemeData ->
                theme_manager.updateTheme(index, edited_theme)
            },
            { theme_manager.addTheme(Theme.default.copy(name = getString("theme_title_new"))) },
            { theme_manager.removeTheme(it) }
        ),

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_ACCENT_COLOUR_SOURCE.name),
            getString("s_key_accent_source"), null,
            3, false
        ) { choice ->
            when (AccentColourSource.values()[choice]) {
                AccentColourSource.THEME     -> getString("s_option_accent_theme")
                AccentColourSource.THUMBNAIL -> getString("s_option_accent_thumbnail")
                AccentColourSource.SYSTEM    -> getString("s_option_accent_system")
            }
        },

        SettingsItemMultipleChoice(
            SettingsValueState(Settings.KEY_NOWPLAYING_THEME_MODE.name),
            getString("s_key_np_theme_mode"), null,
            3, false
        ) { choice ->
            when (choice) {
                0 ->    getString("s_option_np_accent_background")
                1 ->    getString("s_option_np_accent_elements")
                else -> getString("s_option_np_accent_none")
            }
        }
    )
}

private fun getLyricsCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_ENABLED.name),
            getString("s_key_lyrics_follow_enabled"), getString("s_sub_lyrics_follow_enabled")
        ),

        SettingsItemSlider(
            SettingsValueState(Settings.KEY_LYRICS_FOLLOW_OFFSET.name),
            getString("s_key_lyrics_follow_offset"), getString("s_sub_lyrics_follow_offset"),
            getString("s_option_lyrics_follow_offset_top"), getString("s_option_lyrics_follow_offset_bottom"), steps = 5,
            getValueText = null
        ),

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_DEFAULT_FURIGANA.name),
            getString("s_key_lyrics_default_furigana"), null
        ),

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_LYRICS_TEXT_ALIGNMENT.name),
            getString("s_key_lyrics_text_alignment"), null, 3
        ) { i ->
            when (i) {
                0 ->    getString("s_option_lyrics_text_alignment_left")
                1 ->    getString("s_option_lyrics_text_alignment_center")
                else -> getString("s_option_lyrics_text_alignment_right")
            }
        },

        SettingsItemToggle(
            SettingsValueState(Settings.KEY_LYRICS_EXTRA_PADDING.name),
            getString("s_key_lyrics_extra_padding"), getString("s_sub_lyrics_extra_padding")
        )
    )
}

private fun getDownloadCategory(): List<SettingsItem> {
    return listOf(
        SettingsItemToggle(
            SettingsValueState(Settings.KEY_AUTO_DOWNLOAD_ENABLED.name),
            getString("s_key_auto_download_enabled"), null
        ),

        SettingsItemSlider(
            SettingsValueState<Int>(Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.name),
            getString("s_key_auto_download_threshold"), getString("s_sub_auto_download_threshold"),
            range = 1f .. 10f,
            min_label = "1",
            max_label = "10"
        ),

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_STREAM_AUDIO_QUALITY.name),
            getString("s_key_stream_audio_quality"), getString("s_sub_stream_audio_quality"), 3
        ) { i ->
            when (i) {
                Song.AudioQuality.HIGH.ordinal ->   getString("s_option_audio_quality_high")
                Song.AudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else ->                             getString("s_option_audio_quality_low")
            }
        },

        SettingsItemDropdown(
            SettingsValueState(Settings.KEY_DOWNLOAD_AUDIO_QUALITY.name),
            getString("s_key_download_audio_quality"), getString("s_sub_download_audio_quality"), 3
        ) { i ->
            when (i) {
                Song.AudioQuality.HIGH.ordinal ->   getString("s_option_audio_quality_high")
                Song.AudioQuality.MEDIUM.ordinal -> getString("s_option_audio_quality_medium")
                else ->                             getString("s_option_audio_quality_low")
            }
        }
    )
}