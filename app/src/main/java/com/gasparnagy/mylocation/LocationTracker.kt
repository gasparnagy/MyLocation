package com.gasparnagy.mylocation

import android.Manifest
import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/**
 * Takes care of the location permission, asks to switch on location (GPS) and delivers location updates.
 * Must be created while the activity is being constructed (it registers activity result launchers);
 * call [start] from onStart and [stop] from onStop.
 */
class LocationTracker(private val activity: AppCompatActivity, private val listener: Listener) {

    enum class Problem { LOCATION_OFF, PERMISSION_DENIED }

    interface Listener {
        fun onLocation(location: Location)
        fun onWaitingForFix()
        fun onProblem(problem: Problem, action: () -> Unit)
        fun onProblemResolved()
    }

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(activity) }

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
            result.lastLocation?.let { listener.onLocation(it) }
        }
    }

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.none { it } &&
                LOCATION_PERMISSIONS.none { activity.shouldShowRequestPermissionRationale(it) }
            ) {
                permissionPermanentlyDenied = true
            }
            start()
        }

    private val enableLocationLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                start()
            } else {
                listener.onProblem(Problem.LOCATION_OFF) { promptEnableLocation() }
            }
        }

    val hasFinePermission: Boolean
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    /** Ensures permission is granted and location (GPS) is switched on, then starts updates. */
    fun start() {
        if (!hasFinePermission && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (!askedForPermission) {
                askedForPermission = true
                askForPermission()
            } else {
                listener.onProblem(Problem.PERMISSION_DENIED) { askForPermission() }
            }
            return
        }

        LocationServices.getSettingsClient(activity)
            .checkLocationSettings(locationSettingsRequest())
            .addOnSuccessListener {
                listener.onProblemResolved()
                startLocationUpdates()
            }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException && !askedToEnableLocation) {
                    askedToEnableLocation = true
                    launchEnableLocation(e)
                } else {
                    listener.onProblem(Problem.LOCATION_OFF) { promptEnableLocation() }
                }
            }
    }

    fun stop() {
        if (!updatesRunning) return
        updatesRunning = false
        fusedClient.removeLocationUpdates(locationCallback)
    }

    private fun locationSettingsRequest() =
        LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true).build()

    private fun askForPermission() {
        if (!permissionPermanentlyDenied) {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        } else {
            // Permission was permanently denied: open the app's settings page instead.
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
            )
        }
    }

    private fun promptEnableLocation() {
        LocationServices.getSettingsClient(activity)
            .checkLocationSettings(locationSettingsRequest())
            .addOnSuccessListener { start() }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    launchEnableLocation(e)
                } else {
                    activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
    }

    private fun launchEnableLocation(e: ResolvableApiException) {
        enableLocationLauncher.launch(IntentSenderRequest.Builder(e.resolution).build())
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // The settings check is asynchronous; don't start if the activity was stopped meanwhile.
        if (updatesRunning || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        updatesRunning = true
        listener.onWaitingForFix()
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    companion object {
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
