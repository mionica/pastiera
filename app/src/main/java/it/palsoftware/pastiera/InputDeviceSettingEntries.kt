package it.palsoftware.pastiera

/** Static rows and aggregate editors; individual user/device data is not indexed. */
internal fun inputDeviceSettingEntries(): List<SettingEntry> = listOf(
    SettingEntry("text_expansion.snippets.enabled", R.string.snippets_enable_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.snippets.tab", R.string.expansion_tab_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.snippets.enter", R.string.expansion_enter_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.snippets.space", R.string.expansion_space_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.snippets.prefix_space", R.string.expansion_prefix_space_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.emoji.enabled", R.string.emoji_shortcodes_enable_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.symbols.enabled", R.string.symbol_shortcodes_enable_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.emoji_symbols.tab", R.string.expansion_tab_title, summaryRes = R.string.emoji_symbols_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.emoji_symbols.enter", R.string.expansion_enter_title, summaryRes = R.string.emoji_symbols_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.emoji_symbols.exact_close", R.string.emoji_symbols_close_title, summaryRes = R.string.emoji_symbols_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.emoji_symbols.space", R.string.expansion_space_title, summaryRes = R.string.emoji_symbols_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.emoji_symbols.prefix_space", R.string.expansion_prefix_space_title, summaryRes = R.string.emoji_symbols_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.snippets.prefix", R.string.snippets_prefix_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.snippets.presentation", R.string.expansion_presentation_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.emoji_symbols.presentation", R.string.expansion_presentation_title, summaryRes = R.string.emoji_symbols_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("text_expansion.snippets.manage", R.string.snippets_manage_title, summaryRes = R.string.snippets_title, route = SettingRoute(SettingsDestination.TextInput)),
    SettingEntry("hardware.titan2_layout", R.string.titan2_layout_title, route = SettingRoute(SettingsDestination.KeyboardsDevices, keyboardsDevicesDestination = KeyboardsDevicesDestination.BuiltIn)),
    SettingEntry("hardware.profile", R.string.hardware_keyboard_profile_select_label, route = SettingRoute(SettingsDestination.KeyboardsDevices, keyboardsDevicesDestination = KeyboardsDevicesDestination.BuiltIn)),
    SettingEntry("hardware.currency", R.string.keyboard_currency_symbol_title, route = SettingRoute(SettingsDestination.KeyboardsDevices, keyboardsDevicesDestination = KeyboardsDevicesDestination.BuiltIn)),
    SettingEntry("sym.auto_close", R.string.sym_auto_close_title, route = SettingRoute(SettingsDestination.Modifiers, symCustomization = true)),
    SettingEntry("sym.auto_close_touch", R.string.sym_auto_close_touch_title, route = SettingRoute(SettingsDestination.Modifiers, symCustomization = true)),
    SettingEntry("sym.emoji_height", R.string.emoji_picker_expanded_height_title, route = SettingRoute(SettingsDestination.Modifiers, symCustomization = true)),
    SettingEntry("sym.pages", R.string.sym_swap_pages_title, route = SettingRoute(SettingsDestination.Modifiers, symCustomization = true))
)
