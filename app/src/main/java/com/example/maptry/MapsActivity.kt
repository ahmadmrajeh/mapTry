package com.example.maptry

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.maptry.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


@Suppress("DEPRECATION")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    android.location.LocationListener {

    private var googleApiClient: GoogleApiClient? = null
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    companion object {
        const val LOCATION_SETTING_REQUEST = 999
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        title = "Location App"





        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapLongClick(map)

        val button = Button(this)
        button.text = "Location"
        addContentView(
            button,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        button.setOnClickListener {
            enableLoc()
        }


    }


    private fun getLocation() {
        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
            return

        } else {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(
                LocationManager.FUSED_PROVIDER,
                5000,
                5f,
                this@MapsActivity
            )

            val location: Location? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
            } else {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            val currentLatLng = LatLng(location?.latitude ?: 120.0, location?.longitude ?: 120.0)
            if (currentLatLng.latitude != 90.0) {
                //     val snippet = snippest(currentLatLng )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                map.addMarker(MarkerOptions().position(currentLatLng))
                setMapLongClick(map)

            } else {   showTost("enable GPS from Settings")}
        }

    }

    override fun onLocationChanged(location: Location) {

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
                showTost("Permission Granted")
            } else {
                showTost("Permission Denied")
            }
        }
    }

    private fun showTost(label: String) {
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()

    }


    private fun setMapLongClick(mapClick: GoogleMap) {

        mapClick.setOnMapLongClickListener { latLng ->
            val snippet = snippest(latLng)

            mapClick.addMarker(
                MarkerOptions().position(latLng)
                    .title("Droped Pin")
                    .snippet(snippet)
            )
        }
    }

    private fun snippest(latLng: LatLng): String {
        val snippet = String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )
        return snippet
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Companion.LOCATION_SETTING_REQUEST) {

            if (resultCode == RESULT_OK) {
                getLocation()
            }
            if (resultCode == RESULT_CANCELED) {
                // Write your code if there's no result
                showTost("enable GPS from Settings")
            }

        }

    }
     private fun enableLoc() {
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(this@MapsActivity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    override fun onConnected(bundle: Bundle?) {}
                    override fun onConnectionSuspended(i: Int) {
                        googleApiClient!!.connect()
                    }
                })
                .addOnConnectionFailedListener { connectionResult ->
                    Log.d(
                        "Location error",
                        "Location error " + connectionResult.errorCode
                    )
                }.build()
            googleApiClient!!.connect()
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = (30 * 1000).toLong()
            locationRequest.fastestInterval = (5 * 1000).toLong()
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            builder.setAlwaysShow(true)
            val result: PendingResult<LocationSettingsResult> =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
            result.setResultCallback { it ->
                val status: Status = it.status
                when (status.statusCode) {

                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {

                        status.startResolutionForResult(
                            this@MapsActivity,
                            Companion.LOCATION_SETTING_REQUEST
                        )
                    } catch (e: SendIntentException) {
                        showTost(e.message.toString())
                     }
                }
            }

        } else {
            getLocation()
        }
    }


}