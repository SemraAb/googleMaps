package com.samra.mapskotlin.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.samra.mapskotlin.R
import com.samra.mapskotlin.databinding.ActivityMapsBinding
import com.samra.mapskotlin.model.Place
import com.samra.mapskotlin.roomdb.PlaceDao
import com.samra.mapskotlin.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback  , GoogleMap.OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private  var trackBoolean: Boolean? = null
    private  var selectedLatitude: Double? =null
    private  var selectedLongitude: Double? =null
    private lateinit var db : PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.samra.mapskotlin" , MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude = 0.0
        selectedLongitude = 0.0

        db = Room.databaseBuilder( applicationContext , PlaceDatabase::class.java , "Places").build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this) // uzun clickde ne olacaqsa guncel map a veririk

        var intent = intent
        var info = intent.getStringExtra("info")

        if(info=="new"){
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE
            //casting
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object: LocationListener{
                override fun onLocationChanged(p0: Location) {
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean" , false)
                    //ilk defe save edirik deye defValuesi false olacaq bunun
                    if(trackBoolean == false){
                        var myLocation = LatLng(p0.latitude , p0.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation , 15f))
                        sharedPreferences.edit().putBoolean("trackBoolean" , true).apply()


                    }

                }

                override fun onProviderEnabled(provider: String) {
                    super.onProviderEnabled(provider)
                }
                override fun onProviderDisabled(provider: String) {
                    super.onProviderDisabled(provider)
                }

            }

            if(ContextCompat.checkSelfPermission(this ,Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED){

                if(ActivityCompat.shouldShowRequestPermissionRationale(this , Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make( binding.root,"Permission needed  for location" , Snackbar.LENGTH_INDEFINITE).setAction("Give permission"){
                        // request permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }else{
                    // request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }else{
                // request permission
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0 , 0f ,locationListener)
                var lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null){
                    val userLastLocation = LatLng(lastLocation.latitude , lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation , 15f))
                }
                mMap.isMyLocationEnabled = true

            }

            // lat - en  , lng , long , uzunluq
            //40.98193885287174, 47.85159625196129
            // mark location
            /*   var gabalaHome = LatLng(40.98193885287174 ,47.85159625196129 )
               mMap.addMarker(MarkerOptions().position(gabalaHome).title("Menim evim"))
               mMap.moveCamera(CameraUpdateFactory.newLatLng(gabalaHome))
              */
        }else{
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place

            placeFromMain?.let{
                var latlon = LatLng(it.latitude , it.longitude)
                mMap.addMarker(MarkerOptions().position(latlon).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlon , 15f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
            }
        }
    }

    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                if(ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    // permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0 , 0f ,locationListener)
                    var lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val userLastLocation = LatLng(lastLocation.latitude , lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation , 15f))
                    }
                    mMap.isMyLocationEnabled = true
                }
            }else
                // permission denied
                Toast.makeText(this ,"Permission needed!" , Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude

        binding.saveButton.isEnabled = true
    }
    fun save (view:View){
        if(selectedLatitude!=null && selectedLongitude!=null){
            val place = Place(binding.placeText.text.toString() , selectedLatitude!! , selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ( this::handlerResponse )
            )
        }
    }
    private fun handlerResponse(){
        var intent = Intent(this , MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
    fun delete (view:View){
        placeFromMain?.let{
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ( this::handlerResponse )
            )
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}