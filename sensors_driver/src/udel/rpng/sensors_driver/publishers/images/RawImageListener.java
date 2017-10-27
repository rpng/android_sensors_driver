package udel.rpng.sensors_driver.publishers.images;

import android.hardware.Camera.Size;

public interface RawImageListener {

    void onNewRawImage(byte[] data, Size size);

}