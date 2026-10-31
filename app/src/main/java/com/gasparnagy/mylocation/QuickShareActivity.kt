package com.gasparnagy.mylocation

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gasparnagy.mylocation.databinding.ActivityQuickShareBinding
import kotlin.math.roundToInt

/**
 * Opened by a https://mylocation.gasparnagy.com/where-are-you link: waits for a good fix (or the best one
 * within [TIMEOUT_SECONDS]), copies the location to the clipboard and closes, so the user is back
 * in the chat and can paste it.
 */
class QuickShareActivity : AppCompatActivity(), LocationTracker.Listener {

    private lateinit var binding: ActivityQuickShareBinding
    private val tracker = LocationTracker(this, this)
    private val handler = Handler(Looper.getMainLooper())

    private var bestLocation: Location? = null
    private var timeoutStarted = false
    private var timedOut = false
    private var copied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.hintText.text = getString(R.string.quick_hint, TIMEOUT_SECONDS)
        binding.cancelButton.setOnClickListener { finish() }
        binding.copyNowButton.setOnClickListener { bestLocation?.let { copyAndClose(it) } }
    }

    override fun onStart() {
        super.onStart()
        if (!copied) tracker.start()
    }

    override fun onStop() {
        super.onStop()
        tracker.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onWaitingForFix() {
        if (bestLocation == null) binding.statusText.text = getString(R.string.quick_waiting)
        if (!timeoutStarted) {
            timeoutStarted = true
            handler.postDelayed({
                timedOut = true
                bestLocation?.let { copyAndClose(it) }
            }, TIMEOUT_SECONDS * 1000L)
        }
    }

    override fun onLocation(location: Location) {
        if (copied) return
        val best = bestLocation
        if (best == null || location.accuracy <= best.accuracy) bestLocation = location
        val shown = bestLocation ?: location

        binding.statusText.text = getString(Precision.of(shown).quickLabel, shown.accuracy.roundToInt())
        binding.statusText.setTextColor(ContextCompat.getColor(this, Precision.of(shown).color))
        binding.copyNowButton.isEnabled = true

        // After the timeout, take the best fix we have instead of waiting for a good one.
        if (Precision.of(shown) == Precision.GOOD || timedOut) copyAndClose(shown)
    }

    override fun onProblem(problem: LocationTracker.Problem, action: () -> Unit) {
        binding.statusText.text = getString(
            when (problem) {
                LocationTracker.Problem.LOCATION_OFF -> R.string.quick_location_off
                LocationTracker.Problem.PERMISSION_DENIED -> R.string.quick_permission_denied
            }
        )
        binding.enableLocationButton.visibility = View.VISIBLE
        binding.enableLocationButton.setOnClickListener { action() }
    }

    override fun onProblemResolved() {
        binding.enableLocationButton.visibility = View.GONE
    }

    private fun copyAndClose(location: Location) {
        if (copied) return
        copied = true
        tracker.stop()
        Clipboard.copyLocation(this, location)

        binding.statusText.text = getString(R.string.copied)
        binding.hintText.visibility = View.GONE
        binding.copyNowButton.isEnabled = false
        binding.cancelButton.isEnabled = false
        handler.postDelayed({ finish() }, CLOSE_DELAY_MILLIS)
    }

    companion object {
        private const val TIMEOUT_SECONDS = 30
        private const val CLOSE_DELAY_MILLIS = 1_500L
    }
}
