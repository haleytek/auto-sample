package com.haleytek.autosample

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.CarPropertyManager.CarPropertyEventCallback
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// When using car-ui lib activities inherit from FragmentActivity and we should use style/Theme.CarUi.
class MainActivity : FragmentActivity() {
    private val resultPermissionRequest = 10
    private val requiredPermissions = listOf(Car.PERMISSION_SPEED)

    private val parkedTextView: TextView by lazy { findViewById(R.id.textView_parked) }
    private val speedTextView: TextView by lazy { findViewById(R.id.textView_speed) }
    private val uxRestrictionsTextView: TextView by lazy { findViewById(R.id.textView_uxRestrictions) }

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager
    private lateinit var carUxRestrictionsManager: CarUxRestrictionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (notGrantedPermissions().isEmpty()) {
            init()
        } else {
            requestRequiredPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        carPropertyManager.unregisterCallback(parkingBrakeCallback)
        carPropertyManager.unregisterCallback(vehicleSpeedCallback)
        carUxRestrictionsManager.unregisterListener()
        car.disconnect()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (notGrantedPermissions().isEmpty()) {
            init()
        }
    }

    private fun init() {
        car = Car.createCar(applicationContext)

        // Car properties
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        carPropertyManager.registerCallback(parkingBrakeCallback, VehiclePropertyIds.PARKING_BRAKE_ON, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        carPropertyManager.registerCallback(vehicleSpeedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_ONCHANGE)

        // UXRestrictions
        carUxRestrictionsManager = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
        carUxRestrictionsManager.registerListener(uxRestrictionsListener)
        uxRestrictionsListener.onUxRestrictionsChanged(carUxRestrictionsManager.currentCarUxRestrictions)
    }

    // This is where you get info about car properties as they are updated. When registering the callback you can specify how often you
    // want to be notified. You can also just get the current value without registering a callback. Some properties have multiple values
    // for different areas in the car. Compare carPropertyValue.getAreaId() against the constants defined in classes named VehicleArea{Xyz}
    private val parkingBrakeCallback: CarPropertyEventCallback = object : CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            Log.d("AUTOSAMPLE", carPropertyValue.toString())
            parkedTextView.text = "Parked: ${carPropertyValue.value}"
        }

        override fun onErrorEvent(i: Int, i1: Int) {}
    }
    private val vehicleSpeedCallback: CarPropertyEventCallback = object : CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            Log.d("AUTOSAMPLE", carPropertyValue.toString())
            speedTextView.text = "Speed: ${carPropertyValue.value}"
        }

        override fun onErrorEvent(i: Int, i1: Int) {}
    }

    // Get information about UxRestrictions. Your activities will be available while driving so its up to you to check
    // the UXRestrictions and ensure your activities behave accordingly. UXRestrictions in an Android Automotive concept
    // but can be customised by OEMs who decide what is allowed during certain driving conditions.
    private val uxRestrictionsListener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { carUxRestrictions ->
        Log.d("AUTOSAMPLE", carUxRestrictions.toString())
        val noVideo = carUxRestrictions.activeRestrictions and CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO == CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO
        uxRestrictionsTextView.text = "Can play video: ${!noVideo}"
    }

    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, notGrantedPermissions().toTypedArray(), resultPermissionRequest)
    }

    private fun notGrantedPermissions(): List<String> {
        return requiredPermissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
    }
}