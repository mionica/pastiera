package it.palsoftware.pastiera.inputmethod

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.util.TypedValue

/**
 * Gestisce la status bar visualizzata dall'IME, occupandosi della creazione della view
 * e dell'aggiornamento del testo/stile in base allo stato dei modificatori.
 */
class StatusBarController(
    private val context: Context
) {
    // Listener per la selezione delle variazioni
    var onVariationSelectedListener: VariationButtonHandler.OnVariationSelectedListener? = null

    companion object {
        private const val NAV_MODE_LABEL = "NAV MODE"
        private val DEFAULT_BACKGROUND = Color.parseColor("#000000")
        private val NAV_MODE_BACKGROUND = Color.argb(100, 0, 0, 0)
    }

    data class StatusSnapshot(
        val capsLockEnabled: Boolean,
        val shiftPhysicallyPressed: Boolean,
        val shiftOneShot: Boolean,
        val ctrlLatchActive: Boolean,
        val ctrlPhysicallyPressed: Boolean,
        val ctrlOneShot: Boolean,
        val ctrlLatchFromNavMode: Boolean,
        val altLatchActive: Boolean,
        val altPhysicallyPressed: Boolean,
        val altOneShot: Boolean,
        val symKeyActive: Boolean,
        val variations: List<String> = emptyList(),
        val lastInsertedChar: Char? = null
    ) {
        val navModeActive: Boolean
            get() = ctrlLatchActive && ctrlLatchFromNavMode
    }

    private var statusBarLayout: LinearLayout? = null
    private var modifiersContainer: LinearLayout? = null
    private var emojiMapTextView: TextView? = null
    private var emojiKeyboardContainer: LinearLayout? = null
    private var variationsContainer: LinearLayout? = null
    private var variationButtons: MutableList<TextView> = mutableListOf()
    private var ledContainer: LinearLayout? = null
    private var shiftLed: View? = null
    private var ctrlLed: View? = null
    private var altLed: View? = null
    private var symLed: View? = null
    private var emojiKeyButtons: MutableList<View> = mutableListOf()

    fun getLayout(): LinearLayout? = statusBarLayout

    fun getOrCreateLayout(emojiMapText: String = ""): LinearLayout {
        if (statusBarLayout == null) {
            statusBarLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(DEFAULT_BACKGROUND)
            }

            // Container per gli indicatori dei modificatori (orizzontale, allineato a sinistra)
            // Aggiungiamo padding a sinistra per evitare il tasto di collapse della tastiera IME
            val leftPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                64f, 
                context.resources.displayMetrics
            ).toInt()
            val horizontalPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                16f, 
                context.resources.displayMetrics
            ).toInt()
            val verticalPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                8f, 
                context.resources.displayMetrics
            ).toInt()
            
            modifiersContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(leftPadding, verticalPadding, horizontalPadding, verticalPadding)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                visibility = View.GONE
            }

            // Container per la griglia emoji (quando SYM è attivo) - posizionato in fondo
            val emojiKeyboardHorizontalPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                context.resources.displayMetrics
            ).toInt()
            val emojiKeyboardBottomPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f, // Padding in basso per evitare i controlli IME
                context.resources.displayMetrics
            ).toInt()
            
            emojiKeyboardContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                // Nessun padding in alto, solo laterale e in basso
                setPadding(emojiKeyboardHorizontalPadding, 0, emojiKeyboardHorizontalPadding, emojiKeyboardBottomPadding)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                visibility = View.GONE
            }
            
            // Manteniamo il TextView per retrocompatibilità (nascosto)
            emojiMapTextView = TextView(context).apply {
                visibility = View.GONE
            }

            // Container per i pulsanti delle variazioni (orizzontale, allineato a sinistra)
            // Altezza fissa per mantenere la barra sempre della stessa altezza (aumentata del 10%)
            val variationsContainerHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                55f, // 50f * 1.1 (aumentata del 10%)
                context.resources.displayMetrics
            ).toInt()
            val variationsVerticalPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                8.8f, // 8f * 1.1 (aumentato del 10%)
                context.resources.displayMetrics
            ).toInt()
            
            variationsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                // Padding speculare: sinistro e destro uguali (64dp)
                setPadding(leftPadding, variationsVerticalPadding, leftPadding, variationsVerticalPadding)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    variationsContainerHeight // Altezza fissa invece di WRAP_CONTENT
                )
                visibility = View.INVISIBLE  // INVISIBLE invece di GONE per mantenere lo spazio
            }

            // Container per i LED nel bordo inferiore
            val ledHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                context.resources.displayMetrics
            ).toInt()
            val ledBottomPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                context.resources.displayMetrics
            ).toInt()
            
            ledContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
                setPadding(0, 0, 0, ledBottomPadding)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            // Crea i LED per ogni modificatore nell'ordine: SHIFT - SYM - unused - unused - CONTROL - ALT
            // Dividiamo la larghezza in 6 parti uguali
            shiftLed = createFlatLed(0, ledHeight, false) // Parte 1
            symLed = createFlatLed(0, ledHeight, false)   // Parte 2
            val unused1 = createFlatLed(0, ledHeight, false) // Parte 3 (unused)
            val unused2 = createFlatLed(0, ledHeight, false) // Parte 4 (unused)
            ctrlLed = createFlatLed(0, ledHeight, false)  // Parte 5
            altLed = createFlatLed(0, ledHeight, false)   // Parte 6
            
            // Nascondi le parti unused (rendile completamente trasparenti)
            unused1.visibility = View.INVISIBLE
            unused2.visibility = View.INVISIBLE
            
            ledContainer?.apply {
                // Aggiungi i LED nell'ordine corretto, ognuno occupa 1/6 della larghezza
                addView(shiftLed, LinearLayout.LayoutParams(0, ledHeight, 1f))
                addView(symLed, LinearLayout.LayoutParams(0, ledHeight, 1f))
                addView(unused1, LinearLayout.LayoutParams(0, ledHeight, 1f))
                addView(unused2, LinearLayout.LayoutParams(0, ledHeight, 1f))
                addView(ctrlLed, LinearLayout.LayoutParams(0, ledHeight, 1f))
                addView(altLed, LinearLayout.LayoutParams(0, ledHeight, 1f))
            }
            
            statusBarLayout?.apply {
                addView(modifiersContainer)
                addView(variationsContainer)
                addView(emojiKeyboardContainer) // Griglia emoji prima dei LED
                addView(ledContainer) // LED sempre in fondo
            }
        } else if (emojiMapText.isNotEmpty()) {
            emojiMapTextView?.text = emojiMapText
        }
        return statusBarLayout!!
    }
    
    /**
     * Crea un LED rettangolare e piatto per un modificatore.
     */
    private fun createFlatLed(width: Int, height: Int, isActive: Boolean): View {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(if (isActive) Color.WHITE else Color.argb(80, 255, 255, 255))
            cornerRadius = 0f // Nessun arrotondamento per renderlo piatto
        }
        
        return View(context).apply {
            background = drawable
            layoutParams = LinearLayout.LayoutParams(width, height)
        }
    }
    
    /**
     * Aggiorna lo stato di un LED.
     * @param led Il view del LED da aggiornare
     * @param isLocked Se true, il LED è rosso (lockato), se false e isActive è true è blu (attivo), altrimenti grigio (spento)
     * @param isActive Se true e isLocked è false, il LED è blu (attivo)
     */
    private fun updateLed(led: View?, isLocked: Boolean, isActive: Boolean = false) {
        led?.let {
            val color = when {
                isLocked -> Color.RED // Rosso quando lockato
                isActive -> Color.BLUE // Blu quando attivo (ma non lockato)
                else -> Color.argb(80, 255, 255, 255) // Grigio semi-trasparente quando spento
            }
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
                cornerRadius = 0f // Nessun arrotondamento per renderlo piatto
            }
            it.background = drawable
        }
    }
    
    /**
     * Crea un indicatore per un modificatore (deprecato, mantenuto per compatibilità).
     */
    private fun createModifierIndicator(text: String, isActive: Boolean): TextView {
        val dp8 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            8f, 
            context.resources.displayMetrics
        ).toInt()
        val dp6 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            6f, 
            context.resources.displayMetrics
        ).toInt()
        
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(if (isActive) Color.WHITE else Color.argb(180, 255, 255, 255))
            gravity = Gravity.CENTER
            setPadding(dp6, dp8, dp6, dp8)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp8 // Margine a destra tra gli indicatori
            }
        }
    }
    
    /**
     * Aggiorna la griglia emoji con le mappature SYM.
     */
    private fun updateEmojiKeyboard(symMappings: Map<Int, String>) {
        val container = emojiKeyboardContainer ?: return
        
        // Rimuovi tutti i tasti esistenti
        container.removeAllViews()
        emojiKeyButtons.clear()
        
        // Definizione delle righe della tastiera
        val keyboardRows = listOf(
            listOf(android.view.KeyEvent.KEYCODE_Q, android.view.KeyEvent.KEYCODE_W, android.view.KeyEvent.KEYCODE_E, 
                   android.view.KeyEvent.KEYCODE_R, android.view.KeyEvent.KEYCODE_T, android.view.KeyEvent.KEYCODE_Y, 
                   android.view.KeyEvent.KEYCODE_U, android.view.KeyEvent.KEYCODE_I, android.view.KeyEvent.KEYCODE_O, 
                   android.view.KeyEvent.KEYCODE_P),
            listOf(android.view.KeyEvent.KEYCODE_A, android.view.KeyEvent.KEYCODE_S, android.view.KeyEvent.KEYCODE_D, 
                   android.view.KeyEvent.KEYCODE_F, android.view.KeyEvent.KEYCODE_G, android.view.KeyEvent.KEYCODE_H, 
                   android.view.KeyEvent.KEYCODE_J, android.view.KeyEvent.KEYCODE_K, android.view.KeyEvent.KEYCODE_L),
            listOf(android.view.KeyEvent.KEYCODE_Z, android.view.KeyEvent.KEYCODE_X, android.view.KeyEvent.KEYCODE_C, 
                   android.view.KeyEvent.KEYCODE_V, android.view.KeyEvent.KEYCODE_B, android.view.KeyEvent.KEYCODE_N, 
                   android.view.KeyEvent.KEYCODE_M)
        )
        
        val keyLabels = mapOf(
            android.view.KeyEvent.KEYCODE_Q to "Q", android.view.KeyEvent.KEYCODE_W to "W", android.view.KeyEvent.KEYCODE_E to "E",
            android.view.KeyEvent.KEYCODE_R to "R", android.view.KeyEvent.KEYCODE_T to "T", android.view.KeyEvent.KEYCODE_Y to "Y",
            android.view.KeyEvent.KEYCODE_U to "U", android.view.KeyEvent.KEYCODE_I to "I", android.view.KeyEvent.KEYCODE_O to "O",
            android.view.KeyEvent.KEYCODE_P to "P", android.view.KeyEvent.KEYCODE_A to "A", android.view.KeyEvent.KEYCODE_S to "S",
            android.view.KeyEvent.KEYCODE_D to "D", android.view.KeyEvent.KEYCODE_F to "F", android.view.KeyEvent.KEYCODE_G to "G",
            android.view.KeyEvent.KEYCODE_H to "H", android.view.KeyEvent.KEYCODE_J to "J", android.view.KeyEvent.KEYCODE_K to "K",
            android.view.KeyEvent.KEYCODE_L to "L", android.view.KeyEvent.KEYCODE_Z to "Z", android.view.KeyEvent.KEYCODE_X to "X",
            android.view.KeyEvent.KEYCODE_C to "C", android.view.KeyEvent.KEYCODE_V to "V", android.view.KeyEvent.KEYCODE_B to "B",
            android.view.KeyEvent.KEYCODE_N to "N", android.view.KeyEvent.KEYCODE_M to "M"
        )
        
        val keySpacing = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()
        
        // Calcola la larghezza fissa dei tasti basata sulla riga più lunga
        val maxKeysInRow = keyboardRows.maxOfOrNull { it.size } ?: 10
        val screenWidth = context.resources.displayMetrics.widthPixels
        val horizontalPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f * 2, // padding sinistro + destro
            context.resources.displayMetrics
        ).toInt()
        val availableWidth = screenWidth - horizontalPadding
        val totalSpacing = keySpacing * (maxKeysInRow - 1)
        val fixedKeyWidth = (availableWidth - totalSpacing) / maxKeysInRow
        
        val keyHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            56f,
            context.resources.displayMetrics
        ).toInt()
        
        // Crea ogni riga della tastiera
        for ((rowIndex, row) in keyboardRows.withIndex()) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL // Centra le righe più corte
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Aggiungi margine solo tra le righe, non dopo l'ultima
                    if (rowIndex < keyboardRows.size - 1) {
                        bottomMargin = keySpacing
                    }
                }
            }
            
            for ((index, keyCode) in row.withIndex()) {
                val label = keyLabels[keyCode] ?: ""
                val emoji = symMappings[keyCode] ?: ""
                
                val keyButton = createEmojiKeyButton(label, emoji, keyHeight)
                emojiKeyButtons.add(keyButton)
                
                // Usa larghezza fissa invece di weight
                rowLayout.addView(keyButton, LinearLayout.LayoutParams(fixedKeyWidth, keyHeight).apply {
                    // Aggiungi margine solo se non è l'ultimo tasto della riga
                    if (index < row.size - 1) {
                        marginEnd = keySpacing
                    }
                })
            }
            
            container.addView(rowLayout)
        }
    }
    
    /**
     * Crea un tasto della griglia emoji.
     */
    private fun createEmojiKeyButton(label: String, emoji: String, height: Int): View {
        val keyPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()
        val cornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            context.resources.displayMetrics
        )
        val borderWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            context.resources.displayMetrics
        ).toInt()
        
        val keyLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(keyPadding, keyPadding, keyPadding, keyPadding)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            )
        }
        
        // Background del tasto
        val drawable = GradientDrawable().apply {
            setColor(Color.argb(40, 255, 255, 255)) // Bianco semi-trasparente
            setCornerRadius(cornerRadius)
            setStroke(borderWidth, Color.argb(100, 255, 255, 255)) // Bordo bianco semi-trasparente
        }
        keyLayout.background = drawable
        
        // Emoji (grande) - larghezza fissa per uniformità
        // Usa una dimensione fissa in dp che non dipende dalla larghezza del tasto
        val emojiSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            32f,
            context.resources.displayMetrics
        ).toInt()
        
        // Container per l'emoji con larghezza fissa
        val emojiContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                emojiSize,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        
        val emojiText = TextView(context).apply {
            text = emoji
            textSize = 28f
            gravity = Gravity.CENTER
            // Larghezza e altezza fisse per rendere tutte le emoji della stessa dimensione
            layoutParams = LinearLayout.LayoutParams(
                emojiSize,
                emojiSize
            )
            // Forza la larghezza minima e massima per mantenere uniformità
            minimumWidth = emojiSize
            maxWidth = emojiSize
        }
        
        emojiContainer.addView(emojiText)
        
        // Label (lettera)
        val labelText = TextView(context).apply {
            text = label
            textSize = 12f
            setTextColor(Color.argb(180, 255, 255, 255))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    2f,
                    context.resources.displayMetrics
                ).toInt()
            }
        }
        
        keyLayout.addView(emojiContainer)
        keyLayout.addView(labelText)
        
        return keyLayout
    }
    
    /**
     * Crea una griglia emoji personalizzabile (per la schermata di personalizzazione).
     * Restituisce una View che può essere incorporata in Compose tramite AndroidView.
     * 
     * @param symMappings Le mappature emoji da visualizzare
     * @param onKeyClick Callback chiamato quando un tasto viene cliccato (keyCode, emoji)
     */
    fun createCustomizableEmojiKeyboard(
        symMappings: Map<Int, String>,
        onKeyClick: (Int, String) -> Unit
    ): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bottomPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                context.resources.displayMetrics
            ).toInt()
            setPadding(0, 0, 0, bottomPadding) // Nessun padding orizzontale, solo in basso
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Definizione delle righe della tastiera (stessa struttura della tastiera reale)
        val keyboardRows = listOf(
            listOf(android.view.KeyEvent.KEYCODE_Q, android.view.KeyEvent.KEYCODE_W, android.view.KeyEvent.KEYCODE_E, 
                   android.view.KeyEvent.KEYCODE_R, android.view.KeyEvent.KEYCODE_T, android.view.KeyEvent.KEYCODE_Y, 
                   android.view.KeyEvent.KEYCODE_U, android.view.KeyEvent.KEYCODE_I, android.view.KeyEvent.KEYCODE_O, 
                   android.view.KeyEvent.KEYCODE_P),
            listOf(android.view.KeyEvent.KEYCODE_A, android.view.KeyEvent.KEYCODE_S, android.view.KeyEvent.KEYCODE_D, 
                   android.view.KeyEvent.KEYCODE_F, android.view.KeyEvent.KEYCODE_G, android.view.KeyEvent.KEYCODE_H, 
                   android.view.KeyEvent.KEYCODE_J, android.view.KeyEvent.KEYCODE_K, android.view.KeyEvent.KEYCODE_L),
            listOf(android.view.KeyEvent.KEYCODE_Z, android.view.KeyEvent.KEYCODE_X, android.view.KeyEvent.KEYCODE_C, 
                   android.view.KeyEvent.KEYCODE_V, android.view.KeyEvent.KEYCODE_B, android.view.KeyEvent.KEYCODE_N, 
                   android.view.KeyEvent.KEYCODE_M)
        )
        
        val keyLabels = mapOf(
            android.view.KeyEvent.KEYCODE_Q to "Q", android.view.KeyEvent.KEYCODE_W to "W", android.view.KeyEvent.KEYCODE_E to "E",
            android.view.KeyEvent.KEYCODE_R to "R", android.view.KeyEvent.KEYCODE_T to "T", android.view.KeyEvent.KEYCODE_Y to "Y",
            android.view.KeyEvent.KEYCODE_U to "U", android.view.KeyEvent.KEYCODE_I to "I", android.view.KeyEvent.KEYCODE_O to "O",
            android.view.KeyEvent.KEYCODE_P to "P", android.view.KeyEvent.KEYCODE_A to "A", android.view.KeyEvent.KEYCODE_S to "S",
            android.view.KeyEvent.KEYCODE_D to "D", android.view.KeyEvent.KEYCODE_F to "F", android.view.KeyEvent.KEYCODE_G to "G",
            android.view.KeyEvent.KEYCODE_H to "H", android.view.KeyEvent.KEYCODE_J to "J", android.view.KeyEvent.KEYCODE_K to "K",
            android.view.KeyEvent.KEYCODE_L to "L", android.view.KeyEvent.KEYCODE_Z to "Z", android.view.KeyEvent.KEYCODE_X to "X",
            android.view.KeyEvent.KEYCODE_C to "C", android.view.KeyEvent.KEYCODE_V to "V", android.view.KeyEvent.KEYCODE_B to "B",
            android.view.KeyEvent.KEYCODE_N to "N", android.view.KeyEvent.KEYCODE_M to "M"
        )
        
        val keySpacing = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()
        
        // Calcola la larghezza fissa dei tasti basata sulla riga più lunga
        // Usa ViewTreeObserver per ottenere la larghezza effettiva del container dopo il layout
        val maxKeysInRow = keyboardRows.maxOfOrNull { it.size } ?: 10
        
        // Inizializza con una larghezza temporanea, verrà aggiornata dopo il layout
        var fixedKeyWidth = 0
        
        container.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val containerWidth = container.width
                if (containerWidth > 0) {
                    val totalSpacing = keySpacing * (maxKeysInRow - 1)
                    fixedKeyWidth = (containerWidth - totalSpacing) / maxKeysInRow
                    
                    // Aggiorna tutti i tasti con la larghezza corretta
                    for (i in 0 until container.childCount) {
                        val rowLayout = container.getChildAt(i) as? LinearLayout
                        rowLayout?.let { row ->
                            for (j in 0 until row.childCount) {
                                val keyButton = row.getChildAt(j)
                                val layoutParams = keyButton.layoutParams as? LinearLayout.LayoutParams
                                layoutParams?.let {
                                    it.width = fixedKeyWidth
                                    keyButton.layoutParams = it
                                }
                            }
                        }
                    }
                    
                    // Rimuovi il listener dopo il primo layout
                    container.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
        
        // Valore iniziale basato sulla larghezza dello schermo (verrà aggiornato dal listener)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val totalSpacing = keySpacing * (maxKeysInRow - 1)
        fixedKeyWidth = (screenWidth - totalSpacing) / maxKeysInRow
        
        val keyHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            56f,
            context.resources.displayMetrics
        ).toInt()
        
        // Crea ogni riga della tastiera (stessa struttura della tastiera reale)
        for ((rowIndex, row) in keyboardRows.withIndex()) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL // Centra le righe più corte
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (rowIndex < keyboardRows.size - 1) {
                        bottomMargin = keySpacing
                    }
                }
            }
            
            for ((index, keyCode) in row.withIndex()) {
                val label = keyLabels[keyCode] ?: ""
                val emoji = symMappings[keyCode] ?: ""
                
                // Usa la stessa funzione createEmojiKeyButton della tastiera reale
                val keyButton = createEmojiKeyButton(label, emoji, keyHeight)
                
                // Aggiungi click listener
                keyButton.setOnClickListener {
                    onKeyClick(keyCode, emoji)
                }
                
                // Usa larghezza fissa invece di weight (stesso layout della tastiera reale)
                rowLayout.addView(keyButton, LinearLayout.LayoutParams(fixedKeyWidth, keyHeight).apply {
                    if (index < row.size - 1) {
                        marginEnd = keySpacing
                    }
                })
            }
            
            container.addView(rowLayout)
        }
        
        return container
    }
    
    /**
     * Anima l'apparizione della griglia emoji (slide up + fade in).
     */
    private fun animateEmojiKeyboardIn(view: View) {
        val height = view.height
        if (height == 0) {
            // Se l'altezza non è ancora disponibile, usa una stima
            view.measure(
                View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        val measuredHeight = view.measuredHeight
        val startHeight = 0
        val endHeight = measuredHeight
        
        view.alpha = 0f
        view.translationY = measuredHeight.toFloat()
        view.visibility = View.VISIBLE
        
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                view.alpha = progress
                view.translationY = measuredHeight * (1f - progress)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.translationY = 0f
                    view.alpha = 1f
                }
            })
        }
        animator.start()
    }
    
    /**
     * Anima la scomparsa della griglia emoji (slide down + fade out).
     */
    private fun animateEmojiKeyboardOut(view: View) {
        val height = view.height
        if (height == 0) {
            view.visibility = View.GONE
            return
        }
        
        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                view.alpha = progress
                view.translationY = height * (1f - progress)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.translationY = 0f
                    view.alpha = 1f
                }
            })
        }
        animator.start()
    }
    
    /**
     * Crea un pulsante per una variazione.
     */
    private fun createVariationButton(
        variation: String,
        inputConnection: android.view.inputmethod.InputConnection?,
        buttonWidth: Int
    ): TextView {
        // Converti dp in pixel
        val dp4 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            4f,
            context.resources.displayMetrics
        ).toInt()
        val dp6 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            6f,
            context.resources.displayMetrics
        ).toInt()
        val dp3 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            3f, // Spazio ridotto tra i pulsanti
            context.resources.displayMetrics
        ).toInt()
        
        // Altezza fissa per tutti i pulsanti (quadrati, stessa della larghezza)
        val buttonHeight = buttonWidth
        
        // Crea il background del pulsante (rettangolo senza angoli arrotondati, scuro)
        val drawable = GradientDrawable().apply {
            setColor(Color.rgb(17, 17, 17)) // Grigio quasi nero
            setCornerRadius(0f) // Nessun angolo arrotondato
            // Nessun bordo
        }
        
        // Crea un drawable per lo stato pressed (più chiaro)
        val pressedDrawable = GradientDrawable().apply {
            setColor(Color.rgb(38, 0, 255)) // azzurro quando pressed
            setCornerRadius(0f) // Nessun angolo arrotondato
            // Nessun bordo
        }
        
        // Crea uno StateListDrawable per gestire gli stati (normale e pressed)
        val stateListDrawable = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
            addState(intArrayOf(), drawable) // Stato normale
        }
        
        val button = TextView(context).apply {
            text = variation
            textSize = 17.6f // 16f * 1.1 (aumentato del 10%)
            setTextColor(Color.WHITE) // Testo bianco
            setTypeface(null, android.graphics.Typeface.BOLD) // Testo in grassetto
            gravity = Gravity.CENTER
            // Padding
            setPadding(dp6, dp4, dp6, dp4)
            background = stateListDrawable
            layoutParams = LinearLayout.LayoutParams(
                buttonWidth, // Larghezza calcolata dinamicamente
                buttonHeight  // Altezza fissa (quadrato)
            ).apply {
                marginEnd = dp3 // Margine ridotto tra i pulsanti
            }
            // Rendi il pulsante clickabile
            isClickable = true
            isFocusable = true
        }
        
        // Aggiungi il listener per il click
        button.setOnClickListener(
            VariationButtonHandler.createVariationClickListener(
                variation,
                inputConnection,
                onVariationSelectedListener
            )
        )
        
        return button
    }
    
    /**
     * Crea il pulsante microfono placeholder (8° pulsante, sempre presente).
     */
    private fun createMicrophoneButton(buttonWidth: Int): TextView {
        // Converti dp in pixel
        val dp4 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            4f,
            context.resources.displayMetrics
        ).toInt()
        val dp6 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            6f,
            context.resources.displayMetrics
        ).toInt()
        // Altezza fissa per tutti i pulsanti (quadrati, stessa della larghezza)
        val buttonHeight = buttonWidth
        
        // Crea il background del pulsante (rettangolo senza angoli arrotondati, scuro)
        val drawable = GradientDrawable().apply {
            setColor(Color.rgb(17, 17, 17)) // Grigio quasi nero
            setCornerRadius(0f) // Nessun angolo arrotondato
            // Nessun bordo
        }
        
        // Crea un drawable per lo stato pressed (più chiaro)
        val pressedDrawable = GradientDrawable().apply {
            setColor(Color.rgb(38, 0, 255)) // azzurro quando pressed
            setCornerRadius(0f) // Nessun angolo arrotondato
            // Nessun bordo
        }
        
        // Crea uno StateListDrawable per gestire gli stati (normale e pressed)
        val stateListDrawable = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
            addState(intArrayOf(), drawable) // Stato normale
        }
        
        val button = TextView(context).apply {
            text = "🎤" // Icona microfono placeholder
            textSize = 20f // Dimensione leggermente più grande per l'emoji
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            // Padding
            setPadding(dp6, dp4, dp6, dp4)
            background = stateListDrawable
            layoutParams = LinearLayout.LayoutParams(
                buttonWidth, // Larghezza calcolata dinamicamente
                buttonHeight  // Altezza fissa (quadrato)
            ).apply {
                marginEnd = 0 // Nessun margine dopo l'ultimo pulsante
            }
            // Rendi il pulsante clickabile
            isClickable = true
            isFocusable = true
        }
        
        // Listener placeholder (per ora non fa nulla)
        button.setOnClickListener {
            // TODO: Implementare funzionalità microfono
        }
        
        return button
    }
    
    /**
     * Crea un pulsante placeholder trasparente per riempire gli slot vuoti.
     */
    private fun createPlaceholderButton(buttonWidth: Int): View {
        // Larghezza e altezza fissa per tutti i pulsanti (quadrati, circa 48dp)
        val buttonSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            48f, 
            context.resources.displayMetrics
        ).toInt()
        val dp3 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            3f, // Spazio ridotto tra i pulsanti
            context.resources.displayMetrics
        ).toInt()
        
        // Altezza fissa per tutti i pulsanti (quadrati, stessa della larghezza)
        val buttonHeight = buttonWidth
        
        // Crea un background trasparente
        val drawable = GradientDrawable().apply {
            setColor(Color.TRANSPARENT) // Completamente trasparente
            setCornerRadius(0f) // Nessun angolo arrotondato
        }
        
        val button = View(context).apply {
            background = drawable
            layoutParams = LinearLayout.LayoutParams(
                buttonWidth, // Larghezza calcolata dinamicamente
                buttonHeight  // Altezza fissa (quadrato)
            ).apply {
                marginEnd = dp3 // Margine ridotto tra i pulsanti
            }
            // Non clickabile
            isClickable = false
            isFocusable = false
        }
        
        return button
    }

    fun update(snapshot: StatusSnapshot, emojiMapText: String = "", inputConnection: android.view.inputmethod.InputConnection? = null, symMappings: Map<Int, String>? = null) {
        val layout = statusBarLayout ?: return
        val modifiersContainerView = modifiersContainer ?: return
        val emojiView = emojiMapTextView ?: return
        val variationsContainerView = variationsContainer ?: return
        val emojiKeyboardView = emojiKeyboardContainer ?: return

        if (snapshot.navModeActive) {
            // Nascondi completamente la barra di stato nel nav mode
            // La notifica è sufficiente per indicare che il nav mode è attivo
            layout.visibility = View.GONE
            return
        }
        
        // Mostra la barra quando non siamo in nav mode
        layout.visibility = View.VISIBLE

        layout.setBackgroundColor(DEFAULT_BACKGROUND)
        
        // Nascondi sempre il container dei modificatori testuali (ora usiamo i LED)
        // Quando SYM è attivo, assicuriamoci che sia completamente nascosto (GONE)
        modifiersContainerView.visibility = View.GONE
        
        // Aggiorna i LED nel bordo inferiore
        // Shift: rosso se lockato (Caps Lock), blu se attivo (premuto/one-shot), grigio se spento
        val shiftLocked = snapshot.capsLockEnabled
        val shiftActive = (snapshot.shiftPhysicallyPressed || snapshot.shiftOneShot) && !shiftLocked
        updateLed(shiftLed, shiftLocked, shiftActive)
        
        // Ctrl: rosso se lockato, blu se attivo (premuto/one-shot), grigio se spento
        val ctrlLocked = snapshot.ctrlLatchActive
        val ctrlActive = (snapshot.ctrlPhysicallyPressed || snapshot.ctrlOneShot) && !ctrlLocked
        updateLed(ctrlLed, ctrlLocked, ctrlActive)
        
        // Alt: rosso se lockato, blu se attivo (premuto/one-shot), grigio se spento
        val altLocked = snapshot.altLatchActive
        val altActive = (snapshot.altPhysicallyPressed || snapshot.altOneShot) && !altLocked
        updateLed(altLed, altLocked, altActive)
        
        // SYM: rosso se attivo (sempre lockato quando attivo), grigio se spento
        updateLed(symLed, snapshot.symKeyActive, false)
        
        // Mostra la griglia emoji se SYM è attivo con animazione
        if (snapshot.symKeyActive && symMappings != null) {
            updateEmojiKeyboard(symMappings)
            // Animazione slide up quando appare
            if (emojiKeyboardView.visibility != View.VISIBLE) {
                animateEmojiKeyboardIn(emojiKeyboardView)
            }
            // Quando SYM è attivo, nascondi completamente il container delle variazioni (GONE invece di INVISIBLE)
            variationsContainerView.visibility = View.GONE
        } else {
            // Animazione slide down quando scompare
            if (emojiKeyboardView.visibility == View.VISIBLE) {
                animateEmojiKeyboardOut(emojiKeyboardView)
            }
            // Rimuovi tutti i pulsanti delle variazioni esistenti
            variationsContainerView.removeAllViews()
            variationButtons.clear()
            
            // Calcola la larghezza disponibile per i pulsanti
            val screenWidth = context.resources.displayMetrics.widthPixels
            val leftPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                64f, 
                context.resources.displayMetrics
            ).toInt()
            val rightPadding = leftPadding // Padding speculare
            val availableWidth = screenWidth - leftPadding - rightPadding
            
            // Calcola la larghezza di ogni pulsante
            // 8 pulsanti totali (7 variazioni/placeholder + 1 microfono)
            // 7 spazi tra i pulsanti (ogni spazio è 3dp)
            val spacingBetweenButtons = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                3f, 
                context.resources.displayMetrics
            ).toInt()
            val totalSpacing = spacingBetweenButtons * 7 // 7 spazi tra 8 pulsanti
            val buttonWidth = (availableWidth - totalSpacing) / 8
            
            // Limita a 7 variazioni (prendi solo le prime 7)
            val limitedVariations = if (snapshot.variations.isNotEmpty() && snapshot.lastInsertedChar != null) {
                snapshot.variations.take(7)
            } else {
                emptyList()
            }
            
            // Crea un pulsante per ogni variazione (massimo 7)
            for (variation in limitedVariations) {
                val button = createVariationButton(variation, inputConnection, buttonWidth)
                variationButtons.add(button)
                variationsContainerView.addView(button)
            }
            
            // Aggiungi pulsanti placeholder trasparenti per riempire fino a 7
            val placeholderCount = 7 - limitedVariations.size
            for (i in 0 until placeholderCount) {
                val placeholderButton = createPlaceholderButton(buttonWidth)
                variationsContainerView.addView(placeholderButton)
            }
            
            // Aggiungi sempre il pulsante microfono come 8° pulsante
            val microphoneButton = createMicrophoneButton(buttonWidth)
            variationsContainerView.addView(microphoneButton)
            
            variationsContainerView.visibility = View.VISIBLE
        }
        emojiView.visibility = View.GONE // Sempre nascosto, usiamo la griglia
    }
}


