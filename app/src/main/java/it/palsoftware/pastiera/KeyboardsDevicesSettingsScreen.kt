package it.palsoftware.pastiera

import android.hardware.input.InputManager
import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.palsoftware.pastiera.commands.PastieraCommandSource
import it.palsoftware.pastiera.data.mappings.KeyMappingLoader
import it.palsoftware.pastiera.inputmethod.DeviceSpecific

enum class KeyboardsDevicesDestination { Main, BuiltIn }

@Composable
fun KeyboardsDevicesSettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavModeSettingsClick: (Int?) -> Unit,
    destination: KeyboardsDevicesDestination,
    onDestinationChange: (KeyboardsDevicesDestination) -> Unit
) {
    when (destination) {
        KeyboardsDevicesDestination.Main -> KeyboardsDevicesMainScreen(
            modifier = modifier,
            onBack = onBack,
            onBuiltIn = { onDestinationChange(KeyboardsDevicesDestination.BuiltIn) },
            onPowerKeyboard = {  },
            onNavModeSettingsClick = onNavModeSettingsClick
        )
        KeyboardsDevicesDestination.BuiltIn -> HardwareKeyboardSettingsScreen(
            modifier = modifier,
            onBack = { onDestinationChange(KeyboardsDevicesDestination.Main) }
        )
    }
}

@Composable
private fun KeyboardsDevicesMainScreen(
    modifier: Modifier,
    onBack: () -> Unit,
    onBuiltIn: () -> Unit,
    onPowerKeyboard: () -> Unit,
    onNavModeSettingsClick: (Int?) -> Unit
) {
    val context = LocalContext.current
    var inputDeviceRevision by remember { mutableStateOf(0) }

    DisposableEffect(context) {
        val inputManager = context.getSystemService(InputManager::class.java)
        val listener = object : InputManager.InputDeviceListener {
            private fun refreshKeyboardState() {
                inputDeviceRevision += 1
            }

            override fun onInputDeviceAdded(deviceId: Int) = refreshKeyboardState()
            override fun onInputDeviceRemoved(deviceId: Int) = refreshKeyboardState()
            override fun onInputDeviceChanged(deviceId: Int) = refreshKeyboardState()
        }
        inputManager.registerInputDeviceListener(listener, null)
        onDispose { inputManager.unregisterInputDeviceListener(listener) }
    }

    val autoEnabled = true
    val builtInDetected = remember(inputDeviceRevision) {
        DeviceSpecific.detectedInputProfiles().any {
            it.kind == DeviceSpecific.InputDeviceKind.BUILT_IN
        }
    }
    val shortcutKeyCode = remember {
        KeyMappingLoader.loadCtrlKeyMappings(context.assets, context)
            .entries.firstOrNull { (_, mapping) ->
                mapping.type == "command" &&
                    mapping.value == PastieraCommandSource.COMMAND_TOGGLE_SOFTWARE_KEYBOARD_MODE
            }
            ?.key
    }
    val shortcut = shortcutKeyCode?.let { "Ctrl+${KeyEvent.keyCodeToString(it).removePrefix("KEYCODE_")}" }

    Scaffold(topBar = { SettingsSubscreenHeader(stringResource(R.string.keyboards_devices_title), onBack) }) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SectionDivider(stringResource(R.string.keyboard_switching_section_title))
            InfoRow(
                title = stringResource(R.string.keyboard_switching_current_title),
                description = stringResource(R.string.keyboard_switching_status_built_in),
                value = stringResource(R.string.keyboard_source_physical)
            )

            SectionDivider(stringResource(R.string.keyboard_sources_section_title))
            NavigationRow(
                icon = Icons.Filled.PhoneAndroid,
                title = stringResource(R.string.built_in_keyboards_title),
                description = if (builtInDetected) stringResource(R.string.keyboard_source_device_bound_detected) else stringResource(R.string.keyboard_source_device_bound),
                linkId = SettingLinkIds.KEYBOARDS_DEVICES_BUILT_IN_KEYBOARDS,
                onClick = onBuiltIn
            )
        }
    }
}

@Composable
private fun SettingsSubscreenHeader(title: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back_content_description))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun NavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    linkId: String? = null,
    onClick: () -> Unit
) {
    Surface(Modifier.fillMaxWidth().settingRow(linkId, onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    linkId: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(Modifier.fillMaxWidth().settingRow(linkId)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange)
        }
    }
}

@Composable
private fun InfoRow(title: String, description: String, value: String) {
    Surface(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
