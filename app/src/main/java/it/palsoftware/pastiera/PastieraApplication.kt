package it.palsoftware.pastiera

import android.app.Application
import android.os.Handler
import android.os.Looper
import it.palsoftware.pastiera.inputmethod.subtype.AdditionalSubtypeUtils

class PastieraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsManager.initializeAltShiftLayoutSwitchDefault(this)
        SettingsManager.enforceTitan2EliteRoundedCornersOnce(this)
        AppPackageChangeMonitor.register(this)
        Handler(Looper.getMainLooper()).post {
            AdditionalSubtypeUtils.registerAdditionalSubtypes(this)
        }
    }

    companion object {
        private const val SOFTWARE_KEYBOARD_MODE_SHORTCUT_ID = "software_keyboard_mode_toggle"
    }
}
