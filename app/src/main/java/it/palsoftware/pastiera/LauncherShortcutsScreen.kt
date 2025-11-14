package it.palsoftware.pastiera

import android.content.Intent
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.stringResource
import it.palsoftware.pastiera.inputmethod.LauncherShortcutAssignmentActivity

/**
 * Schermata per gestire le scorciatoie del launcher.
 */
@Composable
fun LauncherShortcutsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Carica le scorciatoie salvate
    var shortcuts by remember {
        mutableStateOf(SettingsManager.getLauncherShortcuts(context))
    }
    
    // Activity launcher per avviare LauncherShortcutAssignmentActivity
    val launcherShortcutAssignmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == LauncherShortcutAssignmentActivity.RESULT_ASSIGNED) {
            // Aggiorna le scorciatoie dopo l'assegnazione
            shortcuts = SettingsManager.getLauncherShortcuts(context)
        }
    }
    
    // Funzione helper per avviare l'activity di assegnazione
    fun launchShortcutAssignment(keyCode: Int) {
        val intent = Intent(context, LauncherShortcutAssignmentActivity::class.java).apply {
            putExtra(LauncherShortcutAssignmentActivity.EXTRA_KEY_CODE, keyCode)
        }
        launcherShortcutAssignmentLauncher.launch(intent)
    }
    
    BackHandler {
        onBack()
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.launcher_shortcuts_screen_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        
        HorizontalDivider()
        
        // Info card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.launcher_shortcuts_screen_assign_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.launcher_shortcuts_screen_assign_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        // Lista tasti disponibili (Q-Z, A-M)
        val availableKeys = listOf(
            "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
            "A", "S", "D", "F", "G", "H", "J", "K", "L",
            "Z", "X", "C", "V", "B", "N", "M"
        )
        
        availableKeys.forEach { keyName ->
            val keyCode = when (keyName) {
                "Q" -> KeyEvent.KEYCODE_Q
                "W" -> KeyEvent.KEYCODE_W
                "E" -> KeyEvent.KEYCODE_E
                "R" -> KeyEvent.KEYCODE_R
                "T" -> KeyEvent.KEYCODE_T
                "Y" -> KeyEvent.KEYCODE_Y
                "U" -> KeyEvent.KEYCODE_U
                "I" -> KeyEvent.KEYCODE_I
                "O" -> KeyEvent.KEYCODE_O
                "P" -> KeyEvent.KEYCODE_P
                "A" -> KeyEvent.KEYCODE_A
                "S" -> KeyEvent.KEYCODE_S
                "D" -> KeyEvent.KEYCODE_D
                "F" -> KeyEvent.KEYCODE_F
                "G" -> KeyEvent.KEYCODE_G
                "H" -> KeyEvent.KEYCODE_H
                "J" -> KeyEvent.KEYCODE_J
                "K" -> KeyEvent.KEYCODE_K
                "L" -> KeyEvent.KEYCODE_L
                "Z" -> KeyEvent.KEYCODE_Z
                "X" -> KeyEvent.KEYCODE_X
                "C" -> KeyEvent.KEYCODE_C
                "V" -> KeyEvent.KEYCODE_V
                "B" -> KeyEvent.KEYCODE_B
                "N" -> KeyEvent.KEYCODE_N
                "M" -> KeyEvent.KEYCODE_M
                else -> null
            }
            
            if (keyCode != null) {
                val shortcut = shortcuts[keyCode]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable {
                            launchShortcutAssignment(keyCode)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = keyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Text(
                                    text = if (shortcut != null) {
                                        when (shortcut.type) {
                                            SettingsManager.LauncherShortcut.TYPE_APP -> {
                                                shortcut.appName ?: stringResource(R.string.launcher_shortcuts_app_name_unavailable)
                                            }
                                            SettingsManager.LauncherShortcut.TYPE_SHORTCUT -> {
                                                stringResource(R.string.launcher_shortcuts_shortcut_type, shortcut.action ?: stringResource(R.string.launcher_shortcuts_shortcut_unknown))
                                            }
                                            else -> {
                                                stringResource(R.string.launcher_shortcuts_action_type, shortcut.type)
                                            }
                                        }
                                    } else {
                                        stringResource(R.string.launcher_shortcuts_not_assigned)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (shortcut != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            if (shortcut != null && shortcut.type == SettingsManager.LauncherShortcut.TYPE_APP && shortcut.packageName != null) {
                                Text(
                                    text = shortcut.packageName!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (shortcut != null) {
                            TextButton(
                                onClick = {
                                    SettingsManager.removeLauncherShortcut(context, keyCode)
                                    shortcuts = SettingsManager.getLauncherShortcuts(context)
                                }
                            ) {
                                Text(stringResource(R.string.launcher_shortcuts_remove))
                            }
                        }
                    }
                }
            }
        }
    }
}

