# ROS Driver for Android Sensors
This is a nice android application that allows for publishing of data from a phone to a publish ROS master.
This can be used to record a ROS bag of data or to preform SLAM on a higher powered computer.
Note that this only works on phones that use the Camera1 API (so no newer camera2 api phones).
If you are interested in recording data for a Camera2 API phone check out our other repository [android-dataset-recorder](https://github.com/rpng/android-dataset-recorder).
Also note that on some phones the number of cameras that you can use is limited (and thus if you select more the app will fail).
This is caused by a limit to the bandwidth over the camera buses on the physical phone hardware.
Original codebase is from [here](https://github.com/chadrockey/android_sensors_driver) and has been updated to both compile with the new versions and added new features.
This has been tested on the "Yellowstone" [Tango Tablet Development Kit](https://developers.google.com/tango/hardware/tablet) but should work on all Camera1 API phones.

## How to compile and install?

* Downlaod Android Studio and install https://developer.android.com/studio/index.html
* Run `git clone https://github.com/rpng/android_sensors_driver.git android_sensor_project`
* Open `android_sensor_project` as existing project in Android Studio
* Press run button
* Build will fail but suggest to install missing frameworks. Click on the link, install missing, and press run again.
* You can choose to run on a simulator or on your phone
  * Phone
    * Your phone must have developer mode enabled.
    * Enter IP of rosmaster
  * Emulator
    * IP of localhost of emulator computer is 10.0.0.2
    * Enter 10.0.0.2 in your app in emulator, if ros is running at your local computer as well
* Please open an issue if you are unable to build
* To calibrate your camera, we recommend recording a bag and using [kalibr](https://github.com/ethz-asl/kalibr).

## Published Topics

* Camera compressed feed (if enabled) - `/android/tango1/camera_#/image/compressed`
* Camera raw feed (if no compression) - `/android/tango1/camera_#/image_raw`
* Current GPS position (fused) - `/android/tango1/fix`
* Front luminance sensor value - `/android/tango1/illuminance`
* Current device imu data (100Hz) - `/android/tango1/imu`
* Current device magnetometer data - `/android/tango1/magnetic_field`
* Current device temperature - `/android/tango1/temperature`
* Current device pressure - `/android/tango1/barometric_pressure`



## Screenshots

![Screenshot 1](screenshots/Screenshot_2016-01-11-11-53-21.png)
![Screenshot 3](screenshots/Screenshot_2017-10-27-12-22-12.png)
![Screenshot 4](screenshots/Screenshot_2017-10-27-12-25-37.png)
