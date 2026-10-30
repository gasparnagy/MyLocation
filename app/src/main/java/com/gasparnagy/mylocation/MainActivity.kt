package com.gasparnagy.mylocation

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gasparnagy.mylocation.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity(), LocationTracker.Listener {

    private lateinit var binding: ActivityMainBinding
    private val tracker = LocationTracker(this, this)

    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val padding = (24 * resources.displayMetrics.density).toInt()
            view.setPadding(bars.left + padding, bars.top + padding, bars.right + padding, bars.bottom + padding)
            insets
        }

        binding.shareButton.setOnClickListener { shareLocation() }
        binding.copyButton.setOnClickListener {
            currentLocation?.let { Clipboard.copyLocation(this, it) }
        }
    }

    override fun onStart() {
        super.onStart()
        tracker.start()
    }

    override fun onStop() {
        super.onStop()
        tracker.stop()
    }

    override fun onWaitingForFix() {
        if (currentLocation == null) binding.statusText.text = getString(R.string.status_waiting)
    }

    override fun onLocation(location: Location) {
        currentLocation = location
        binding.latitudeText.text = LocationText.coordinate(location.latitude)
        binding.longitudeText.text = LocationText.coordinate(location.longitude)
        binding.precisionText.text = LocationText.precision(this, location)
        binding.precisionText.setTextColor(ContextCompat.getColor(this, Precision.of(location).color))

        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(location.time))
        binding.statusText.text = if (tracker.hasFinePermission) {
            getString(R.string.status_updated, time)
        } else {
            getString(R.string.status_updated, time) + "\n" + getString(R.string.status_coarse_only)
        }
        binding.shareButton.isEnabled = true
        binding.copyButton.isEnabled = true
    }

    override fun onProblem(message: String, action: () -> Unit) {
        binding.statusText.text = message
        binding.enableLocationButton.visibility = View.VISIBLE
        binding.enableLocationButton.setOnClickListener { action() }
    }

    override fun onProblemResolved() {
        binding.enableLocationButton.visibility = View.GONE
    }

    private fun shareLocation() {
        val location = currentLocation ?: return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, LocationText.shareText(this@MainActivity, location))
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_chooser_title)))
    }
}
