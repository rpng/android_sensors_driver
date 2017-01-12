# android_sensors_driver
ROS Driver for Android Sensors

checkout screenshots

![Screenshot 1](android_sensors_driver/screenshots/Screenshot_2016-01-11-11-53-21.png?raw=true)
![Screenshot 1](android_sensors_driver/screenshots/Screenshot_2016-01-11-11-48-04.png?raw=true)


# How to compile and install?

* Downlaod Android Studio and install https://developer.android.com/studio/index.html
* run `git clone https://github.com/plieningerweb/android_sensors_driver.git android_sensor_project`
* open android_sensor_porject as existing project in Android Studio
* Press run button
* Build will fail but suggest to install missing frameworks. Click on the link, install missing, and press run again.
* You can choose to run on a simulator or on your phone
  * Phone
    * Your phone must have developer mode enabled.
    * Enter IP of rosmaster
  * Emulator
    * IP of localhost of emulator computer is 10.0.0.2
    * Enter 10.0.0.2 in your app in emulator, if ros is running at your local computer as well
