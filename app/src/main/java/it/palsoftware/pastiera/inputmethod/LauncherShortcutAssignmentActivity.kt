package it.palsoftware.pastiera.inputmethod

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.palsoftware.pastiera.*
import android.view.KeyEvent
import android.widget.ImageView
import android.view.ViewGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.stringResource
import it.palsoftware.pastiera.R

/**
 * Activity per assegnare una scorciatoia del launcher a un tasto.
 * Viene mostrata quando si preme un tasto non assegnato nel launcher.
 * Usa un BottomSheet che appare sopra il launcher.
 */
class LauncherShortcutAssignmentActivity : ComponentActivity() {
    companion object {
        const val EXTRA_KEY_CODE = "key_code"
        const val RESULT_ASSIGNED = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val keyCode = intent.getIntExtra(EXTRA_KEY_CODE, -1)
        if (keyCode == -1) {
            finish()
            return
        }
        
        // Usa un tema trasparente per mostrare il bottom sheet sopra il launcher
        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { finish() }, // Tocca fuori per chiudere
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Overlay semi-trasparente
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
                    ) {}
                    
                    // Bottom Sheet
                    LauncherShortcutAssignmentBottomSheet(
                        keyCode = keyCode,
                        onAppSelected = { app ->
                            SettingsManager.setLauncherShortcut(
                                this@LauncherShortcutAssignmentActivity,
                                keyCode,
                                app.packageName,
                                app.appName
                            )
                            setResult(RESULT_ASSIGNED)
                            finish()
                        },
                        onDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

/**
 * Bottom Sheet per assegnare un'app a un tasto.
 * Appare dal basso e non occupa tutto lo schermo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherShortcutAssignmentBottomSheet(
    keyCode: Int,
    onAppSelected: (InstalledApp) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Carica le app installate
    val installedApps by remember {
        mutableStateOf(AppListHelper.getInstalledApps(context))
    }
    
    var searchQuery by remember { mutableStateOf("") }
    
    // Funzione helper per ottenere la lettera del tasto
    fun getKeyLetter(keyCode: Int): Char? {
        return when (keyCode) {
            KeyEvent.KEYCODE_Q -> 'Q'
            KeyEvent.KEYCODE_W -> 'W'
            KeyEvent.KEYCODE_E -> 'E'
            KeyEvent.KEYCODE_R -> 'R'
            KeyEvent.KEYCODE_T -> 'T'
            KeyEvent.KEYCODE_Y -> 'Y'
            KeyEvent.KEYCODE_U -> 'U'
            KeyEvent.KEYCODE_I -> 'I'
            KeyEvent.KEYCODE_O -> 'O'
            KeyEvent.KEYCODE_P -> 'P'
            KeyEvent.KEYCODE_A -> 'A'
            KeyEvent.KEYCODE_S -> 'S'
            KeyEvent.KEYCODE_D -> 'D'
            KeyEvent.KEYCODE_F -> 'F'
            KeyEvent.KEYCODE_G -> 'G'
            KeyEvent.KEYCODE_H -> 'H'
            KeyEvent.KEYCODE_J -> 'J'
            KeyEvent.KEYCODE_K -> 'K'
            KeyEvent.KEYCODE_L -> 'L'
            KeyEvent.KEYCODE_Z -> 'Z'
            KeyEvent.KEYCODE_X -> 'X'
            KeyEvent.KEYCODE_C -> 'C'
            KeyEvent.KEYCODE_V -> 'V'
            KeyEvent.KEYCODE_B -> 'B'
            KeyEvent.KEYCODE_N -> 'N'
            KeyEvent.KEYCODE_M -> 'M'
            else -> null
        }
    }
    
    // Filtra e ordina le app in base alla query di ricerca e alla lettera del tasto
    val filteredApps = remember(installedApps, searchQuery, keyCode) {
        val apps = if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Ordina: prima le app che iniziano con la lettera del tasto, poi le altre
        val keyLetter = getKeyLetter(keyCode)?.lowercaseChar()
        if (keyLetter != null && searchQuery.isBlank()) {
            val appsStartingWithLetter = apps.filter { 
                it.appName.isNotEmpty() && it.appName[0].lowercaseChar() == keyLetter 
            }.sortedBy { it.appName.lowercase() }
            
            val otherApps = apps.filter { 
                it.appName.isEmpty() || it.appName[0].lowercaseChar() != keyLetter 
            }.sortedBy { it.appName.lowercase() }
            
            appsStartingWithLetter + otherApps
        } else {
            // Se c'è una ricerca attiva, ordina normalmente
            apps.sortedBy { it.appName.lowercase() }
        }
    }
    
    // Funzione helper per ottenere il nome del tasto
    @Composable
    fun getKeyName(keyCode: Int): String {
        val keyName = when (keyCode) {
            KeyEvent.KEYCODE_Q -> "Q"
            KeyEvent.KEYCODE_W -> "W"
            KeyEvent.KEYCODE_E -> "E"
            KeyEvent.KEYCODE_R -> "R"
            KeyEvent.KEYCODE_T -> "T"
            KeyEvent.KEYCODE_Y -> "Y"
            KeyEvent.KEYCODE_U -> "U"
            KeyEvent.KEYCODE_I -> "I"
            KeyEvent.KEYCODE_O -> "O"
            KeyEvent.KEYCODE_P -> "P"
            KeyEvent.KEYCODE_A -> "A"
            KeyEvent.KEYCODE_S -> "S"
            KeyEvent.KEYCODE_D -> "D"
            KeyEvent.KEYCODE_F -> "F"
            KeyEvent.KEYCODE_G -> "G"
            KeyEvent.KEYCODE_H -> "H"
            KeyEvent.KEYCODE_J -> "J"
            KeyEvent.KEYCODE_K -> "K"
            KeyEvent.KEYCODE_L -> "L"
            KeyEvent.KEYCODE_Z -> "Z"
            KeyEvent.KEYCODE_X -> "X"
            KeyEvent.KEYCODE_C -> "C"
            KeyEvent.KEYCODE_V -> "V"
            KeyEvent.KEYCODE_B -> "B"
            KeyEvent.KEYCODE_N -> "N"
            KeyEvent.KEYCODE_M -> "M"
            else -> null
        }
        return keyName ?: stringResource(R.string.launcher_shortcut_assignment_key_name, keyCode)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.85f),
        dragHandle = {
            HorizontalDivider(
                modifier = Modifier
                    .width(40.dp)
                    .padding(vertical = 8.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.launcher_shortcut_assignment_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.launcher_shortcut_assignment_key, getKeyName(keyCode)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.launcher_shortcut_assignment_close)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo di ricerca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.launcher_shortcut_assignment_search_placeholder)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.launcher_shortcut_assignment_search_description)
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lista delle app (limita l'altezza)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    stringResource(R.string.launcher_shortcut_assignment_no_apps)
                                } else {
                                    stringResource(R.string.launcher_shortcut_assignment_no_results, searchQuery)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredApps,
                        key = { app -> app.packageName }
                    ) { app ->
                        AppListItem(
                            app = app,
                            onClick = {
                                onAppSelected(app)
                            }
                        )
                    }
                }
            }
            
            // Spazio in basso per evitare che il contenuto tocchi il bordo
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Item della lista per un'app (versione riutilizzabile).
 */
@Composable
private fun AppListItem(
    app: InstalledApp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona app usando AndroidView
            // Usa key per forzare il recomposition quando cambia l'app
            key(app.packageName) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            android.widget.ImageView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                setImageDrawable(app.icon)
                            }
                        },
                        update = { imageView ->
                            imageView.setImageDrawable(app.icon)
                        },
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Nome app (package name nascosto)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

