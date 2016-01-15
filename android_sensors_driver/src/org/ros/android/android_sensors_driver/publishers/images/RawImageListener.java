package org.ros.android.android_sensors_driver.publishers.images;

import android.hardware.Camera.Size;

public interface RawImageListener {

    void onNewRawImage(byte[] data, Size size);

}