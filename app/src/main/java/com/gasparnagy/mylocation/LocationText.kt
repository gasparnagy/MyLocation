package com.gasparnagy.mylocation

import android.content.Context
import android.location.Location
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import java.util.Locale
import kotlin.math.roundToInt

/** [label] is used on the bilingual main screen, [quickLabel] on the English-only quick share dialog. */
enum class Precision(@StringRes val label: Int, @StringRes val quickLabel: Int, @ColorRes val color: Int) {
    GOOD(R.string.precision_good, R.string.quick_precision_good, R.color.precision_good),
    AVERAGE(R.string.precision_average, R.string.quick_precision_average, R.color.precision_average),
    POOR(R.string.precision_poor, R.string.quick_precision_poor, R.color.precision_poor);

    companion object {
        private const val GOOD_ACCURACY_METERS = 10f
        private const val AVERAGE_ACCURACY_METERS = 30f

        fun of(location: Location) = when {
            location.accuracy <= GOOD_ACCURACY_METERS -> GOOD
            location.accuracy <= AVERAGE_ACCURACY_METERS -> AVERAGE
            else -> POOR
        }
    }
}

object LocationText {

    fun coordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    fun precision(context: Context, location: Location): String =
        context.getString(Precision.of(location).label, location.accuracy.roundToInt())

    /** The text that is shared or copied: coordinates, accuracy and a Google Maps link. */
    fun shareText(context: Context, location: Location): String {
        val lat = coordinate(location.latitude)
        val lon = coordinate(location.longitude)
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$lat%2C$lon"
        return context.getString(R.string.share_text, lat, lon, location.accuracy.roundToInt(), mapsUrl)
    }
}
