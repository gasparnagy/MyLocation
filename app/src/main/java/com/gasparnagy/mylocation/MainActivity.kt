package com.gasparnagy.mylocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import com.gasparnagy.mylocation.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedClient: FusedLocationProviderClient

    private var currentLocation: Location? = null
    private var updatesRunning = false
    private var askedToEnableLocation = false
    private var askedForPermission = false
    private var permissionPermanentlyDenied = false

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
        .setMinUpdateIntervalMillis(1_000L)
        .setWaitForAccurateLocation(true)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { showLocation(it) }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.none { it } && LOCATION_PERMISSIONS.none { shouldShowRequestPermissionRationale(it) }) {
                permissionPermanentlyDenied = true
            }
            startLocation()
        }

    private val enableLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startLocation()
            } else {
                showProblem(getString(R.string.status_location_off)) { promptEnableLocation() }
            }
        }

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

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        binding.shareButton.setOnClickListener { shareLocation() }
    }

    override fun onStart() {
        super.onStart()
        startLocation()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    /** Ensures permission is granted and location (GPS) is switched on, then starts updates. */
    private fun startLocation() {
        val fine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fine && !coarse) {
            if (!askedForPermission) {
                askedForPermission = true
                askForPermission()
            } else {
                showProblem(getString(R.string.status_permission_denied)) { askForPermission() }
            }
            return
        }

        LocationServices.getSettingsClient(this)
            .checkLocationSettings(
                LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                    .setAlwaysShow(true)
                    .build()
            )
            .addOnSuccessListener {
                hideProblem()
                startLocationUpdates()
            }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException && !askedToEnableLocation) {
                    askedToEnableLocation = true
                    launchEnableLocation(e)
                } else {
                    showProblem(getString(R.string.status_location_off)) { promptEnableLocation() }
                }
            }
    }

    private fun askForPermission() {
        if (!permissionPermanentlyDenied) {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        } else {
            // Permission was permanently denied: open the app's settings page instead.
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }
    }

    private fun promptEnableLocation() {
        LocationServices.getSettingsClient(this)
            .checkLocationSettings(
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true).build()
            )
            .addOnSuccessListener { startLocation() }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    launchEnableLocation(e)
                } else {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
    }

    private fun launchEnableLocation(e: ResolvableApiException) {
        enableLocationLauncher.launch(IntentSenderRequest.Builder(e.resolution).build())
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // The settings check is asynchronous; don't start if the activity was stopped meanwhile.
        if (updatesRunning || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        updatesRunning = true
        if (currentLocation == null) binding.statusText.text = getString(R.string.status_waiting)
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        if (!updatesRunning) return
        updatesRunning = false
        fusedClient.removeLocationUpdates(locationCallback)
    }

    private fun showLocation(location: Location) {
        currentLocation = location
        binding.latitudeText.text = formatCoordinate(location.latitude)
        binding.longitudeText.text = formatCoordinate(location.longitude)

        val meters = location.accuracy.roundToInt()
        val (labelRes, colorRes) = when {
            location.accuracy <= GOOD_ACCURACY_METERS -> R.string.precision_good to R.color.precision_good
            location.accuracy <= AVERAGE_ACCURACY_METERS -> R.string.precision_average to R.color.precision_average
            else -> R.string.precision_poor to R.color.precision_poor
        }
        binding.precisionText.text = getString(labelRes, meters)
        binding.precisionText.setTextColor(ContextCompat.getColor(this, colorRes))

        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(location.time))
        binding.statusText.text = if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            getString(R.string.status_updated, time)
        } else {
            getString(R.string.status_updated, time) + "\n" + getString(R.string.status_coarse_only)
        }
        binding.shareButton.isEnabled = true
    }

    private fun shareLocation() {
        val location = currentLocation ?: return
        val lat = formatCoordinate(location.latitude)
        val lon = formatCoordinate(location.longitude)
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
        val text = getString(R.string.share_text, lat, lon, location.accuracy.roundToInt(), mapsUrl)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_chooser_title)))
    }

    private fun showProblem(message: String, action: () -> Unit) {
        binding.statusText.text = message
        binding.enableLocationButton.visibility = View.VISIBLE
        binding.enableLocationButton.setOnClickListener { action() }
    }

    private fun hideProblem() {
        binding.enableLocationButton.visibility = View.GONE
    }

    private fun formatCoordinate(value: Double) = String.format(Locale.US, "%.6f", value)

    companion object {
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val GOOD_ACCURACY_METERS = 10f
        private const val AVERAGE_ACCURACY_METERS = 30f
    }
}
