package com.overscroll.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overscroll.app.service.NotificationHelper
import com.overscroll.app.service.ScrollCountRepository
import com.overscroll.app.data.settings.AppSettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Overlay bubble service (PRD §5.3).
 *
 * Shows a small floating, draggable bubble with the live Reels count.
 * Now acts as a single pill-shaped view with the blob mascot and count beside it.
 * Toggle on/off from Settings (PRD §5.6).
 *
 * Uses WindowManager + TYPE_APPLICATION_OVERLAY for the floating window.
 * Runs as a foreground service.
 */
@AndroidEntryPoint
class OverlayBubbleService : LifecycleService() {

    @Inject lateinit var scrollCountRepository: ScrollCountRepository
    @Inject lateinit var appSettingsDataStore: AppSettingsDataStore

    private lateinit var windowManager: WindowManager
    private var rootView: View? = null
    private var composeLifecycleOwner: ComposeLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Drag/tap state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var hasMoved = false

    companion object {
        private const val TAG = "OverlayBubble"
        private const val DRAG_THRESHOLD_DP = 8
        private const val BUBBLE_SIZE_DP = 52
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Overlay service created (onCreate)")
        NotificationHelper.createChannels(this)
        startAsForeground()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        observeCount()
        observeInstagramActive()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Overlay service onStartCommand called")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        composeLifecycleOwner?.destroy()
        composeLifecycleOwner = null
        
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove overlay view", e)
            }
        }
        rootView = null
        Log.i(TAG, "Overlay service destroyed")
        super.onDestroy()
    }

    // ── Foreground service setup ──────────────────────────────────────────────

    private fun startAsForeground() {
        val notification = NotificationHelper.createOverlayNotification(this)
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID_OVERLAY,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
    }

    // ── Overlay creation ────────────────────────────────────────────────     

    private fun createOverlay() {
        val ctx: Context = this
        val bubbleSize = dpToPx(BUBBLE_SIZE_DP)

        // Set up custom LifecycleOwner for Compose in WindowManager
        val lifecycleOwner = ComposeLifecycleOwner()
        composeLifecycleOwner = lifecycleOwner
        lifecycleOwner.attachToLifecycle()

        val composeView = androidx.compose.ui.platform.ComposeView(ctx).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            
            setContent {
                val activeApp by scrollCountRepository.currentActiveApp.collectAsState()
                val todayCounts by scrollCountRepository.todayCounts.collectAsState()
                val count = if (activeApp == "com.overscroll.app") {
                    todayCounts.values.sum()
                } else {
                    todayCounts[activeApp] ?: 0
                }
                
                val thresholdA by appSettingsDataStore.thresholdA.collectAsState(initial = 20)
                val thresholdB by appSettingsDataStore.thresholdB.collectAsState(initial = 50)
                
                // Root padding prevents clipping of scaling animations
                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = Color(0xE6181824),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 16.dp)
                    ) {
                            MascotFace(
                                count = count,
                                thresholdA = thresholdA,
                                thresholdB = thresholdB
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = count.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                }
            }
        }
        
        val root = composeView

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Compensate for the 16dp invisible padding so the visual margin is correct
            x = dpToPx(12) - dpToPx(16)
            y = dpToPx(180) - dpToPx(16)
        }

        // ── Touch handling (drag) ──
        root.setOnTouchListener(createTouchListener(params))

        rootView = root
        layoutParams = params

        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Cannot add overlay: permission denied")
            return
        }

        Log.i(TAG, "About to call windowManager.addView")
        try {
            windowManager.addView(root, params)
            Log.i(TAG, "Overlay bubble successfully added to window")
        } catch (e: Exception) {
            Log.e(TAG, "Exception caught during windowManager.addView", e)
        }
    }

    // ── Count observation ────────────────────────────────────────────────────

    private fun observeCount() {
        // No-op. The ComposeView handles state observation reactively.
    }


    private fun observeInstagramActive() {
        lifecycleScope.launch {
            var hideJob: kotlinx.coroutines.Job? = null
            scrollCountRepository.isAnyAppActive.collect { isActive ->
                if (rootView != null && layoutParams != null && rootView?.isAttachedToWindow == true) {
                    if (isActive) {
                        hideJob?.cancel()
                        if (rootView?.visibility != View.VISIBLE) {
                            rootView?.visibility = View.VISIBLE
                            try {
                                windowManager.updateViewLayout(rootView, layoutParams)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update view layout on visibility change", e)
                            }
                        }
                    } else {
                        hideJob?.cancel()
                        hideJob = launch {
                            kotlinx.coroutines.delay(400) // Debounce hiding to prevent flickering during app transitions
                            if (rootView?.visibility != View.GONE) {
                                rootView?.visibility = View.GONE
                                try {
                                    windowManager.updateViewLayout(rootView, layoutParams)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to update view layout on visibility change", e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Touch handling (drag) ──────────────────────────────────────────

    private fun createTouchListener(
        params: WindowManager.LayoutParams,
    ): View.OnTouchListener {
        val dragThreshold = dpToPx(DRAG_THRESHOLD_DP)

        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!hasMoved && (Math.abs(dx) > dragThreshold || Math.abs(dy) > dragThreshold)) {
                        hasMoved = true
                    }

                    if (hasMoved) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(rootView, params)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to update layout during drag", e)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (hasMoved) {
                        // Snap to nearest edge after drag
                        snapToEdge(params)
                    }
                    true
                }

                else -> false
            }
        }
    }

    /**
     * After a drag ends, snap the bubble to the nearest screen edge.
     * This keeps the bubble from floating awkwardly in the middle of the screen.
     */
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val displayWidth = resources.displayMetrics.widthPixels
        
        // Use the width of the whole pill container rather than just the bubble
        val viewWidth = rootView?.width ?: dpToPx(BUBBLE_SIZE_DP)
        val margin = dpToPx(8)
        val padding = dpToPx(16) // The invisible padding we added to prevent clipping

        val targetX = if (params.x + viewWidth / 2 < displayWidth / 2) {
            margin - padding // Snap left
        } else {
            displayWidth - viewWidth - margin + padding // Snap right
        }

        // Animate the snap
        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 200
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animator ->
                params.x = animator.animatedValue as Int
                try {
                    windowManager.updateViewLayout(rootView, params)
                } catch (e: Exception) {
                    cancel()
                }
            }
            start()
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
