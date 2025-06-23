package com.example.adaptivesecurity.context.collectors
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.adaptivesecurity.context.models.LocationCategory
import com.example.adaptivesecurity.context.models.LocationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationContextCollector(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Known location coordinates
    private val knownLocations = mapOf(
        "HOME" to Pair(40.7128, -74.0060), // New York coordinates
        "WORK" to Pair(37.7749, -122.4194) // San Francisco coordinates
    )

    suspend fun collectLocationContext(): LocationContext? {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasLocationPermission()) {
                    return@withContext null
                }

                val location = getLastKnownLocation() ?: return@withContext null
                val category = determineLocationCategory(location)
                val isKnown = isKnownLocation(location)

                LocationContext(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    locationCategory = category,
                    isKnownLocation = isKnown
                )
            } catch (e: SecurityException) {
                // Handle permission revocation during operation
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        try {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                    bestLocation = location
                }
            }
            return bestLocation
        } catch (e: SecurityException) {
            return null
        }
    }

    private fun determineLocationCategory(location: Location): LocationCategory {
        val nearestLocationName = getNearestKnownLocationName(location)
        return when (nearestLocationName) {
            "HOME" -> LocationCategory.HOME
            "WORK" -> LocationCategory.WORK
            else -> LocationCategory.UNKNOWN
        }
    }

    private fun isKnownLocation(location: Location): Boolean {
        return distanceToNearestKnownLocation(location) < 100 // Within 100 meters
    }

    private fun distanceToNearestKnownLocation(location: Location): Float {
        var minDistance = Float.MAX_VALUE

        knownLocations.values.forEach { (lat, lng) ->
            val knownLocation = Location("").apply {
                latitude = lat
                longitude = lng
            }
            val distance = location.distanceTo(knownLocation)
            if (distance < minDistance) {
                minDistance = distance
            }
        }
        return minDistance
    }

    private fun getNearestKnownLocationName(location: Location): String? {
        var minDistance = Float.MAX_VALUE
        var nearestLocationName: String? = null

        knownLocations.forEach { (name, coords) ->
            val knownLocation = Location("").apply {
                latitude = coords.first
                longitude = coords.second
            }
            val distance = location.distanceTo(knownLocation)
            if (distance < minDistance) {
                minDistance = distance
                nearestLocationName = name
            }
        }
        return if (minDistance < 100) nearestLocationName else null
    }
}
