package com.liam.kaptalismusaufhalter.guard

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Container for the popup that notices tapjacking: when another app's window sits on top of ours
 * and a touch lands there, Android marks the event as obscured. With `filterTouchesWhenObscured`
 * the touch is dropped (so a hidden overlay can't steer a tap onto "Trotzdem kaufen"), and this
 * class additionally reports it so the user is warned instead of the attempt vanishing silently.
 */
class GuardedFrameLayout(context: Context) : FrameLayout(context) {

    var onObscuredTouch: (() -> Unit)? = null

    /**
     * Back key / back gesture closes the popup. This is also the safety net for the one scary
     * failure mode of touch filtering: if some other window keeps covering the popup, its buttons
     * can't be tapped - Back must still get the user out instead of leaving a full-screen overlay
     * they can't dismiss.
     */
    var onBackPressed: (() -> Unit)? = null

    private var lastReportedEventTime = Long.MIN_VALUE

    override fun onFilterTouchEventForSecurity(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN &&
            event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0 &&
            // The framework asks once in ViewGroup and again in View for the same event.
            event.eventTime != lastReportedEventTime
        ) {
            lastReportedEventTime = event.eventTime
            onObscuredTouch?.invoke()
        }
        return super.onFilterTouchEventForSecurity(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = onBackPressed
        if (handler != null && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) handler()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
