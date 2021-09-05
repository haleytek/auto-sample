# Auto Sample

## 1. Permissions and using the Platform key

Accessing vehicle data requires that you add permissions to AndroidManifest.xml, just like accessing location or camera in a mobile app. The full list of available permissions can be found in the [car service manifest in AOSP](https://android.googlesource.com/platform/packages/services/Car/+/refs/heads/master/service/AndroidManifest.xml).
Look at the `<permission>` elements and note the `protectionLevel`. Protection levels of `normal` or `dangerous` can be accessed in the usual way. However, `signature` or `privileged` require a more explicit relationship between the app and the device it's installed on.

Signature and privileged permissions are usually grouped together. In production they will usually be granted as privileged, but during development it's more convenient to grant them as signature. You can access signature permissions if you sign your app with the *platform.keystore* file in this project. See *app/build.gradle* to see how to use this file.

This is a development key and not considered secret or sensitive. 

*WARNING:* Do not rely on permissions which only have signature for protectionLevel because these will probably not be granted in production!

## 2. Access vehicle data with CarPropertyManager

Vehicle data is made available to Android apps via `CarPropertyManager`, similar to how GPS data can be accessed via `LocationManager`. First initialize the CarPropertyManager.

```kotlin
val car = Car.createCar(context)
val carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
```

The API provides typical *get*, *set* and *subscribe* functions. Properties may be R/W or read-only. Available properties can be found as constants in the `VehiclePropertyIds` class. Properties can have multiple values, for different areas of the car, e.g. a temperature setting for the driver and a separate setting for the passenger. Other properties are global to the whole car such as vehicle speed.

```kotlin
// Get
val speed = carPropertyManager.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)

// Subscribe
carPropertyManager.registerCallback(vehicleSpeedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_ONCHANGE)

// Callback
val vehicleSpeedCallback: CarPropertyEventCallback = object : CarPropertyEventCallback {
    override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
        speedTextView.text = "Speed: ${carPropertyValue.value}"
    }

    override fun onErrorEvent(i: Int, i1: Int) {}
}
```

Look at the [VehiclePropertyIds source code](https://android.googlesource.com/platform/packages/services/Car/+/master/car-lib/src/android/car/VehiclePropertyIds.java) 

## 3. Use car-ui lib for consistent visual styling

The car-ui lib is a library of views and styles that allow apps to automatically inherit the look and feel of the device they are running on. It's similar to AppCompat for mobile apps except the look and feel is controlled by device manufacturers and can vary significantly between devices.

### 3.1 Themes

Make sure your themes inherit from `@style/Theme.CarUi` or one of its sub-themes. Do not use an AppCompat or MaterialComponents theme.

### 3.2 Activities

Activities should inehrit from `FragmentActivity`. Not from `AppCompatActivity`.

### 3.3 Car-ui components

Make sure to use the car-ui versions of components wherever available. e.g.

```xml
<com.android.car.ui.toolbar.Toolbar
    android:id="@+id/toolbar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:title="Auto Sample" />
```

For more details you can check the [car-ui lib source code](https://android.googlesource.com/platform/packages/apps/Car/libs/+/refs/heads/master/car-ui-lib/) and the [car-ui lib sample app](https://android.googlesource.com/platform/packages/apps/Car/libs/+/refs/heads/master/car-ui-lib/paintbooth/).

## 4. Enable activities while the vehicle is being driven

The default behaviour in Android Automotive is to block activities while the vehicle is being driven. If you want to display activities while driving then you must opt-in and ensure that it is suitable for use while driving. First add the following to your activity in the manifest.

```xml
<meta-data
    android:name="distractionOptimized"
    android:value="true" />
```

Now that an activity is accessible while driving it is the responsibility of the developer to ensure that the UI and content is optimized for this use case.