package com.liam.kaptalismusaufhalter.guard

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
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

/**
 * Hosts a Compose UI in a system overlay window (SYSTEM_ALERT_WINDOW), outside of any
 * Activity. ComposeView needs a Lifecycle/ViewModelStore/SavedStateRegistry owner to work,
 * which an Activity normally provides for free - here we supply a minimal manual one.
 */
class OverlayController(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun isShowing(): Boolean = composeView != null

    fun show(content: @Composable (dismiss: () -> Unit) -> Unit) {
        if (isShowing()) return

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner
        owner.registry.currentState = Lifecycle.State.RESUMED

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { content { hide() } }
        }

        // minSdk is 26 (Android O), so TYPE_APPLICATION_OVERLAY (added in O) always applies -
        // no need for the pre-O TYPE_SYSTEM_ALERT fallback.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        wm.addView(view, params)
        composeView = view
    }

    fun hide() {
        val view = composeView ?: return
        lifecycleOwner?.registry?.currentState = Lifecycle.State.DESTROYED
        try {
            windowManager?.removeView(view)
        } catch (e: IllegalArgumentException) {
            // view was already detached
        }
        composeView = null
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
