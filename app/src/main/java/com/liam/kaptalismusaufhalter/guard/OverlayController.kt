package com.liam.kaptalismusaufhalter.guard

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.liam.kaptalismusaufhalter.security.SecurityGuard
import com.liam.kaptalismusaufhalter.security.SecurityPrefs

/**
 * Hosts a Compose UI in a system overlay window (SYSTEM_ALERT_WINDOW), outside of any
 * Activity. ComposeView needs a Lifecycle/ViewModelStore/SavedStateRegistry owner to work,
 * which an Activity normally provides for free - here we supply a minimal manual one.
 *
 * The popup shows what the user is about to buy and what it costs, so it is locked down:
 * - FLAG_SECURE keeps it out of screenshots, screen recordings and MediaProjection capture,
 * - touches are dropped (and reported) while another window covers it (tapjacking),
 * - on Android 14+ it is marked accessibility-data-sensitive, so accessibility services that
 *   aren't genuine accessibility tools can't read its contents.
 */
class OverlayController(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun isShowing(): Boolean = rootView != null

    fun show(onBackPressed: (() -> Unit)? = null, content: @Composable (dismiss: () -> Unit) -> Unit) {
        if (isShowing()) return

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner
        owner.registry.currentState = Lifecycle.State.RESUMED

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { content { hide() } }
        }

        // Compose resolves its lifecycle from the *root view of the window* - which is now this
        // container, not the ComposeView - so the owners must be set here too.
        val container = GuardedFrameLayout(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            // Tapjacking guard: ignore touches while another app's window covers ours, so a
            // malicious overlay can't trick a tap onto "Trotzdem kaufen"/"Warten lassen".
            filterTouchesWhenObscured = true
            onObscuredTouch = { SecurityGuard.reportTapjacking(context) }
            this.onBackPressed = onBackPressed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching { setAccessibilityDataSensitive(View.ACCESSIBILITY_DATA_SENSITIVE_YES) }
            }
            addView(
                composeView,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )
        }

        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (SecurityPrefs(context).screenProtection) flags = flags or WindowManager.LayoutParams.FLAG_SECURE

        // minSdk is 26 (Android O), so TYPE_APPLICATION_OVERLAY (added in O) always applies -
        // no need for the pre-O TYPE_SYSTEM_ALERT fallback.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        wm.addView(container, params)
        rootView = container
    }

    fun hide() {
        val view = rootView ?: return
        lifecycleOwner?.registry?.currentState = Lifecycle.State.DESTROYED
        try {
            windowManager?.removeView(view)
        } catch (e: IllegalArgumentException) {
            // view was already detached
        }
        rootView = null
        lifecycleOwner = null
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
    }
}
