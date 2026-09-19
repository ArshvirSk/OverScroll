package com.overscroll.app.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OverlayNudgeService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("MESSAGE") ?: "Time for a break?"
        showOverlay(message)
        return START_NOT_STICKY
    }

    private fun showOverlay(message: String) {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }

        // We'll create the view programmatically to avoid needing an XML layout right now
        val context = this
        
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#E6000000")) // 90% black
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val textView = TextView(context).apply {
            text = message
            setTextColor(android.graphics.Color.WHITE)
            textSize = 24f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, 64)
        }
        layout.addView(textView)

        val btnHome = Button(context).apply {
            text = "Close App (Go Home)"
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                // Send user home
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                stopSelf() // Remove overlay
            }
        }
        layout.addView(btnHome)

        val btnSnooze = Button(context).apply {
            text = "Snooze (I'll stop later)"
            setBackgroundColor(android.graphics.Color.parseColor("#606060"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                stopSelf() // Remove overlay
            }
        }
        val snoozeParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 32
        }
        layout.addView(btnSnooze, snoozeParams)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = layout
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }
}
