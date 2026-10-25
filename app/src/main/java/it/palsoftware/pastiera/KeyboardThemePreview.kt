package it.palsoftware.pastiera

import android.content.Context
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.palsoftware.pastiera.data.mappings.KeyMappingLoader
import it.palsoftware.pastiera.data.mappings.AltModifierMappingResolver
import it.palsoftware.pastiera.data.mappings.DeviceSymMappingRepository
import it.palsoftware.pastiera.data.variation.VariationRepository
import it.palsoftware.pastiera.inputmethod.StatusBarController
import it.palsoftware.pastiera.inputmethod.statusbar.StatusBarButtonRegistry
import it.palsoftware.pastiera.inputmethod.suggestions.ui.FullSuggestionsBar
import it.palsoftware.pastiera.inputmethod.ui.LedStatusView
import it.palsoftware.pastiera.inputmethod.ui.VariationBarView

private const val HARDWARE_PREVIEW_MAX_CHROME_SCALE = 1.6f
private const val HARDWARE_PREVIEW_EXTRA_HEIGHT_DP = 12f
private const val HARDWARE_PREVIEW_BOTTOM_INDICATOR_HEIGHT_DP = 6.5f
private const val SUGGESTIONS_PREVIEW_BASE_HEIGHT_DP = 36f
private const val VARIATIONS_PREVIEW_BASE_HEIGHT_DP = 55f
private const val SOFTWARE_PREVIEW_TALL_SCREEN_HEIGHT_FRACTION = 0.46f
private const val SOFTWARE_PREVIEW_SQUARE_SCREEN_HEIGHT_FRACTION = 0.78f
private const val SOFTWARE_PREVIEW_SQUARE_SCREEN_RATIO = 1.25f
private const val SOFTWARE_PREVIEW_MIN_HEIGHT_DP = 360f
private const val SOFTWARE_PREVIEW_TALL_MAX_HEIGHT_DP = 520f
private const val SOFTWARE_PREVIEW_SQUARE_MAX_HEIGHT_DP = 500f
private const val SOFTWARE_PREVIEW_KEYBOARD_BOTTOM_PADDING_DP = 12f
private const val SOFTWARE_PREVIEW_LED_HEIGHT_DP = 6.5f
private const val SOFTWARE_PREVIEW_MODIFIER_DOUBLE_TAP_MS = 450L

@Composable
internal fun HardwareKeyboardThemePreview(theme: KeyboardThemePreset) {
    val context = LocalContext.current
    val showBottomIndicators = SettingsManager.getModifierIndicatorShowsBottomStrip(context)
    val previewHeightDp = hardwareKeyboardPreviewHeightDp(theme, showBottomIndicators)
    val viewportHeightDp = hardwareKeyboardPreviewViewportHeightDp(showBottomIndicators)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(viewportHeightDp.dp)
            .clipToBounds()
            .background(Color(theme.background)),
        contentAlignment = Alignment.BottomCenter
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeightDp.dp),
            factory = { viewContext -> createHardwareKeyboardThemePreviewView(viewContext, theme) },
            update = { view -> updateHardwareKeyboardThemePreviewView(view, theme) }
        )
    }
}

private fun createHardwareKeyboardThemePreviewView(
    context: Context,
    theme: KeyboardThemePreset
): LinearLayout {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(theme.background)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    val suggestionsBar = FullSuggestionsBar(context).apply {
        themeOverride = theme.toKeyboardThemeColors()
    }
    root.addView(suggestionsBar.ensureView())
    suggestionsBar.showPreview(listOf("Werk", "Team", "Park"))
    suggestionsBar.setModifierMenuIndicatorsEnabled(SettingsManager.getModifierIndicatorShowsStatusBar(context))
    suggestionsBar.updateModifierIndicators(hardwarePreviewLedSnapshot())

    val variationBar = VariationBarView(context, buttonRegistry = StatusBarButtonRegistry()).apply {
        themeOverride = theme.toKeyboardThemeColors()
    }
    root.addView(variationBar.ensureView())
    variationBar.showVariations(hardwarePreviewVariationsSnapshot(), inputConnection = null)

    val leds = LedStatusView(context).apply {
        themeOverride = theme.toKeyboardThemeColors()
    }
    if (SettingsManager.getModifierIndicatorShowsBottomStrip(context)) {
        root.addView(leds.ensureView())
        leds.update(hardwarePreviewLedSnapshot())
    }

    root.setTag(R.id.keyboard_theme_preview_suggestions_bar, suggestionsBar)
    root.setTag(R.id.keyboard_theme_preview_variation_bar, variationBar)
    root.setTag(R.id.keyboard_theme_preview_led_view, leds)
    return root
}

private fun updateHardwareKeyboardThemePreviewView(view: android.view.View, theme: KeyboardThemePreset) {
    view.setBackgroundColor(theme.background)
    val colors = theme.toKeyboardThemeColors()
    (view.getTag(R.id.keyboard_theme_preview_suggestions_bar) as? FullSuggestionsBar)?.apply {
        themeOverride = colors
        showPreview(listOf("Werk", "Team", "Park"))
        setModifierMenuIndicatorsEnabled(SettingsManager.getModifierIndicatorShowsStatusBar(view.context))
        updateModifierIndicators(hardwarePreviewLedSnapshot())
    }
    (view.getTag(R.id.keyboard_theme_preview_variation_bar) as? VariationBarView)?.apply {
        themeOverride = colors
        resetVariationsState()
        showVariations(hardwarePreviewVariationsSnapshot(), inputConnection = null)
    }
    (view.getTag(R.id.keyboard_theme_preview_led_view) as? LedStatusView)?.apply {
        themeOverride = colors
        val ledView = ensureView()
        if (SettingsManager.getModifierIndicatorShowsBottomStrip(view.context)) {
            if (ledView.parent !== view && view is LinearLayout) {
                view.addView(ledView)
            }
            update(hardwarePreviewLedSnapshot())
        } else if (ledView.parent === view && view is LinearLayout) {
            view.removeView(ledView)
        }
    }
}

private class BottomAnchoredKeyboardPreviewFrame(context: Context) : FrameLayout(context) {
    var contentRoot: android.view.View? = null
        set(value) {
            field = value
            requestLayout()
        }

    var desiredPreviewHeightPx: Int = 0
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    init {
        clipChildren = true
        clipToPadding = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val frameHeight = when (heightMode) {
            MeasureSpec.UNSPECIFIED -> desiredPreviewHeightPx
            else -> heightSize
        }
        val childWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val childHeight = desiredPreviewHeightPx.coerceAtLeast(0)

        contentRoot?.measure(
            MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
        )

        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(frameHeight + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = contentRoot ?: return
        val childLeft = paddingLeft
        val childBottom = height - paddingBottom
        val childTop = childBottom - child.measuredHeight
        child.layout(
            childLeft,
            childTop,
            childLeft + child.measuredWidth,
            childBottom
        )
    }
}

private class AdditiveVerticalKeyboardPreviewLayout(context: Context) : LinearLayout(context) {
    var keyboardSurface: android.view.View? = null
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val surface = keyboardSurface
        if (surface == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        var totalChildHeight = 0
        var maxWidth = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == android.view.View.GONE) continue
            measureChildWithMargins(
                child,
                widthMeasureSpec,
                0,
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                0
            )
            val params = child.layoutParams as ViewGroup.MarginLayoutParams
            totalChildHeight += child.measuredHeight + params.topMargin + params.bottomMargin
            maxWidth = maxOf(maxWidth, child.measuredWidth + params.leftMargin + params.rightMargin)
        }

        setMeasuredDimension(
            resolveSize(maxWidth + paddingLeft + paddingRight, widthMeasureSpec),
            paddingTop + paddingBottom + totalChildHeight
        )
    }
}

private fun buildVirtualPreviewAltLabels(context: Context): Map<Int, String> =
    AltModifierMappingResolver.resolve(context.assets, context)
        .filterKeys { it in VIRTUAL_PREVIEW_KEY_CODES }
        .filterValues { it.isNotBlank() }

private fun buildVirtualPreviewCtrlLabels(context: Context): Map<Int, String> =
    KeyMappingLoader.loadCtrlKeyMappings(context.assets, context)
        .mapNotNull { (keyCode, mapping) ->
            if (keyCode !in VIRTUAL_PREVIEW_KEY_CODES) {
                return@mapNotNull null
            }
            virtualPreviewCtrlLabel(mapping)?.let { keyCode to it }
        }
        .toMap()

private fun buildVirtualPreviewCtrlIconRes(context: Context): Map<Int, Int> =
    KeyMappingLoader.loadCtrlKeyMappings(context.assets, context)
        .mapNotNull { (keyCode, mapping) ->
            if (keyCode !in VIRTUAL_PREVIEW_KEY_CODES) {
                return@mapNotNull null
            }
            virtualPreviewCtrlIconRes(mapping)?.let { keyCode to it }
        }
        .toMap()

private fun virtualPreviewCtrlLabel(mapping: KeyMappingLoader.CtrlMapping): String? {
    return when (mapping.type) {
        "action" -> when (mapping.value) {
            "copy" -> "Copy"
            "paste" -> "Paste"
            "cut" -> "Cut"
            "undo" -> "Undo"
            "redo" -> "Redo"
            "select_all" -> "All"
            "move_word_left" -> "Word ←"
            "move_word_right" -> "Word →"
            "expand_selection_word_left" -> "Sel ←"
            "expand_selection_word_right" -> "Sel →"
            else -> mapping.value.replace('_', ' ').take(8)
        }
        "keycode" -> mapping.value.removePrefix("KEYCODE_").replace('_', ' ').take(8)
        "command" -> "Cmd"
        "native_ctrl" -> "Ctrl"
        else -> null
    }
}

private fun virtualPreviewCtrlIconRes(mapping: KeyMappingLoader.CtrlMapping): Int? {
    return when (mapping.type) {
        "keycode" -> when (mapping.value) {
            "DPAD_UP" -> R.drawable.keyboard_arrow_up_24
            "DPAD_DOWN" -> R.drawable.keyboard_arrow_down_24
            "DPAD_LEFT" -> R.drawable.keyboard_arrow_left_24
            "DPAD_RIGHT" -> R.drawable.keyboard_arrow_right_24
            "TAB" -> R.drawable.keyboard_tab_24
            "MOVE_HOME" -> R.drawable.first_page_24
            "MOVE_END" -> R.drawable.last_page_24
            "PAGE_UP" -> R.drawable.keyboard_double_arrow_up_24
            "PAGE_DOWN" -> R.drawable.keyboard_double_arrow_down_24
            "ESCAPE" -> R.drawable.close_24
            "FORWARD_DEL" -> R.drawable.delete_24
            else -> null
        }
        "action" -> when (mapping.value) {
            "copy" -> R.drawable.content_copy_24
            "paste" -> R.drawable.content_paste_24
            "cut" -> R.drawable.content_cut_24
            "undo" -> R.drawable.undo_24
            "select_all" -> R.drawable.select_all_24
            "expand_selection_left" -> R.drawable.text_select_move_back_character_filled_24
            "expand_selection_right" -> R.drawable.text_select_move_forward_character_filled_24
            "move_word_left" -> R.drawable.text_select_move_back_word_24
            "move_word_right" -> R.drawable.text_select_move_forward_word_24
            "expand_selection_word_left" -> R.drawable.text_select_move_back_word_filled_24
            "expand_selection_word_right" -> R.drawable.text_select_move_forward_word_filled_24
            "page_start" -> R.drawable.first_page_24
            "page_end" -> R.drawable.last_page_24
            "toggle_minimal_ui" -> R.drawable.collapse_content_24
            "media_play_pause" -> R.drawable.play_pause_24
            "media_previous" -> R.drawable.skip_previous_24
            "media_next" -> R.drawable.skip_next_24
            else -> null
        }
        "command" -> when (mapping.value) {
            "pastiera.toggle_software_keyboard_mode" -> R.drawable.expansion_panels_24
            else -> null
        }
        else -> null
    }
}

private val VIRTUAL_PREVIEW_KEY_CODES: Set<Int> = (
    (KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z).toList() +
        listOf(
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_ENTER
        )
).toSet()

private fun hardwareKeyboardPreviewHeightDp(
    theme: KeyboardThemePreset,
    showBottomIndicators: Boolean
): Float =
    suggestionsPreviewHeightDp(theme) +
        variationsPreviewHeightDp(theme) +
        HARDWARE_PREVIEW_EXTRA_HEIGHT_DP -
        if (showBottomIndicators) 0f else HARDWARE_PREVIEW_BOTTOM_INDICATOR_HEIGHT_DP

private fun hardwareKeyboardPreviewViewportHeightDp(showBottomIndicators: Boolean): Float =
    SUGGESTIONS_PREVIEW_BASE_HEIGHT_DP * HARDWARE_PREVIEW_MAX_CHROME_SCALE +
        VARIATIONS_PREVIEW_BASE_HEIGHT_DP * HARDWARE_PREVIEW_MAX_CHROME_SCALE +
        HARDWARE_PREVIEW_EXTRA_HEIGHT_DP -
        if (showBottomIndicators) 0f else HARDWARE_PREVIEW_BOTTOM_INDICATOR_HEIGHT_DP

private fun suggestionsPreviewHeightDp(theme: KeyboardThemePreset): Float =
    SUGGESTIONS_PREVIEW_BASE_HEIGHT_DP * theme.suggestionsHeightScale.coerceIn(0.65f, 1.6f)

private fun variationsPreviewHeightDp(theme: KeyboardThemePreset): Float =
    VARIATIONS_PREVIEW_BASE_HEIGHT_DP * theme.variationsHeightScale.coerceIn(0.65f, 1.6f)

private fun hardwarePreviewVariationsSnapshot(): StatusBarController.StatusSnapshot =
    StatusBarController.StatusSnapshot(
        capsLockEnabled = false,
        shiftPhysicallyPressed = false,
        shiftOneShot = false,
        ctrlLatchActive = true,
        ctrlPhysicallyPressed = false,
        ctrlOneShot = false,
        ctrlLatchFromNavMode = true,
        altLatchActive = false,
        altPhysicallyPressed = false,
        altOneShot = false,
        symPage = 1,
        clipboardCount = 2,
        variations = listOf("@", "\"", ":", "!", "?", ",", ".")
    )

private fun hardwarePreviewLedSnapshot(): StatusBarController.StatusSnapshot =
    hardwarePreviewVariationsSnapshot().copy(
        shiftPhysicallyPressed = true,
        symPage = 2,
        variations = emptyList()
    )

private fun showVirtualPreviewVariations(variationBar: VariationBarView) {
    variationBar.showVariations(
        StatusBarController.StatusSnapshot(
            capsLockEnabled = false,
            shiftPhysicallyPressed = false,
            shiftOneShot = false,
            ctrlLatchActive = false,
            ctrlPhysicallyPressed = false,
            ctrlOneShot = false,
            ctrlLatchFromNavMode = false,
            altLatchActive = false,
            altPhysicallyPressed = false,
            altOneShot = false,
            symPage = 0,
            clipboardCount = 2,
            variations = listOf("@", "\"", ":", "!", "?", ",", ".")
        ),
        inputConnection = null
    )
}

private fun virtualPreviewLedSnapshot(): StatusBarController.StatusSnapshot =
    StatusBarController.StatusSnapshot(
        capsLockEnabled = false,
        shiftPhysicallyPressed = true,
        shiftOneShot = false,
        ctrlLatchActive = false,
        ctrlPhysicallyPressed = false,
        ctrlOneShot = false,
        ctrlLatchFromNavMode = false,
        altLatchActive = false,
        altPhysicallyPressed = false,
        altOneShot = false,
        symPage = 0
    )

private fun dpToPx(context: Context, dp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    ).toInt()
}
