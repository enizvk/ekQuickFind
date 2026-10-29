package com.ekquickfind.app.floating

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.ekquickfind.app.R
import com.ekquickfind.app.search.SearchActivity
import com.ekquickfind.app.shortcuts.ShortcutsPublisher
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/** Set while one of ekQuickFind's own screens is showing, so the button gets out of the way. */
object FloatingButtonState {
    val appVisible = MutableStateFlow(false)

    /** Set while its settings are open, so size and opacity changes show live. */
    val previewing = MutableStateFlow(false)
}

private data class Look(
    val sizeDp: Int,
    val opacity: Float,
    val onRight: Boolean,
    val yFraction: Float,
)

private fun UserPreferences.look() = Look(floatingButtonSizeDp, floatingButtonOpacity, floatingButtonOnRight, floatingButtonY)

/**
 * The floating button over other apps. A foreground service keeps it alive in every app; the button itself
 * is a small overlay window. Tap opens search, drag moves it, letting go snaps it to the nearer edge.
 */
@AndroidEntryPoint
class FloatingButtonService : Service() {
    @Inject lateinit var prefs: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var button: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var look = UserPreferences().look()
    private var snapping: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        addButton()
        scope.launch {
            combine(prefs.preferences, FloatingButtonState.appVisible, FloatingButtonState.previewing) { p, appVisible, preview ->
                p to (appVisible && !preview)
            }.distinctUntilChanged()
                .collect { (p, appVisible) ->
                    if (!p.floatingButton) {
                        stopSelf()
                        return@collect
                    }
                    apply(p.look())
                    button?.visibility = if (appVisible) View.GONE else View.VISIBLE
                }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ) = START_STICKY

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotation changes the screen size; put the button back on its edge.
        apply(look, force = true)
    }

    override fun onDestroy() {
        scope.cancel()
        snapping?.cancel()
        button?.let { runCatching { windowManager.removeView(it) } }
        button = null
        super.onDestroy()
    }

    private fun px(dp: Int) = (dp * resources.displayMetrics.density).roundToInt()

    private fun geometry(sizePx: Int): BubbleGeometry {
        val bounds = windowManager.currentWindowMetrics.bounds
        return BubbleGeometry(bounds.width(), bounds.height(), sizePx, px(EDGE_MARGIN_DP))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addButton() {
        val view =
            FrameLayout(this).apply {
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        // Neutral gray so it doesn't compete with the app underneath.
                        setColor(BUTTON_GRAY)
                    }
                elevation = px(6).toFloat()
                contentDescription = "Open ekQuickFind"
                addView(
                    ImageView(context).apply { setImageResource(R.drawable.ic_tile_search) },
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
                )
            }
        val size = px(look.sizeDp)
        val lp =
            WindowManager
                .LayoutParams(
                    size,
                    size,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    // Never takes focus or touches outside itself, so the app underneath works as usual.
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                ).apply { gravity = Gravity.TOP or Gravity.START }
        view.setOnTouchListener(DragHandler())
        windowManager.addView(view, lp)
        button = view
        params = lp
        apply(look, force = true)
    }

    private fun apply(
        next: Look,
        force: Boolean = false,
    ) {
        val view = button ?: return
        val lp = params ?: return
        if (!force && next == look) return
        look = next
        val size = px(next.sizeDp)
        val pad = (size * ICON_INSET).roundToInt()
        (view.getChildAt(0) as ImageView).setPadding(pad, pad, pad, pad)
        view.alpha = next.opacity
        val (x, y) = geometry(size).place(next.onRight, next.yFraction)
        lp.width = size
        lp.height = size
        lp.x = x
        lp.y = y
        runCatching { windowManager.updateViewLayout(view, lp) }
    }

    private fun openSearch(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        startActivity(Intent(this, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun snapToEdge(view: View) {
        val lp = params ?: return
        val geometry = geometry(lp.width)
        val spot = geometry.snap(lp.x, lp.y)
        val (targetX, targetY) = geometry.place(spot.onRight, spot.yFraction)
        snapping?.cancel()
        snapping =
            ValueAnimator.ofInt(lp.x, targetX).apply {
                duration = SNAP_MS
                addUpdateListener {
                    lp.x = it.animatedValue as Int
                    lp.y = targetY
                    runCatching { windowManager.updateViewLayout(view, lp) }
                }
                start()
            }
        look = look.copy(onRight = spot.onRight, yFraction = spot.yFraction)
        scope.launch { prefs.update { it.copy(floatingButtonOnRight = spot.onRight, floatingButtonY = spot.yFraction) } }
    }

    private inner class DragHandler : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false
        private val slop = ViewConfiguration.get(this@FloatingButtonService).scaledTouchSlop

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(
            view: View,
            event: MotionEvent,
        ): Boolean {
            val lp = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapping?.cancel()
                    downX = event.rawX
                    downY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && !isTap(dx, dy, slop)) dragging = true
                    if (dragging) {
                        lp.x = startX + dx.roundToInt()
                        lp.y = startY + dy.roundToInt()
                        runCatching { windowManager.updateViewLayout(view, lp) }
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) snapToEdge(view) else openSearch(view)
                MotionEvent.ACTION_CANCEL -> if (dragging) snapToEdge(view)
            }
            return true
        }
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Floating button", NotificationManager.IMPORTANCE_MIN))
        val settings =
            PendingIntent.getActivity(
                this,
                3,
                Intent(ShortcutsPublisher.ACTION_OPEN_SETTINGS).setPackage(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return Notification
            .Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile_search)
            .setContentTitle("Floating button is on")
            .setContentText("Tap to change it in ekQuickFind settings")
            .setContentIntent(settings)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "floating-button"
        private const val NOTIFICATION_ID = 7201
        private const val EDGE_MARGIN_DP = 6
        private const val SNAP_MS = 180L
        private const val ICON_INSET = 0.22f
        private const val BUTTON_GRAY = 0xFF5F6368.toInt()

        /** Starts or stops the button to match [enabled]; quietly does nothing when Android won't allow it right now. */
        fun sync(
            context: Context,
            enabled: Boolean,
        ) {
            val intent = Intent(context, FloatingButtonService::class.java)
            if (enabled && Settings.canDrawOverlays(context)) {
                runCatching { context.startForegroundService(intent) }
            } else {
                context.stopService(intent)
            }
        }
    }
}
