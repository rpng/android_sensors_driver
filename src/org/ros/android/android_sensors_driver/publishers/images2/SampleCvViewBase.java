package org.ros.android.android_sensors_driver.publishers.images2;

import java.util.List;

import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.highgui.Highgui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
* @author axelfurlan@gmail.com (Axel Furlan)
*/

public abstract class SampleCvViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private int camera_id;
    private SurfaceHolder mHolder;
    private VideoCapture mCamera;

    public SampleCvViewBase(Context context, int camera_id) {
        super(context);
        this.camera_id = camera_id;
        this.mHolder = getHolder();
        this.mHolder.addCallback(this);
    }

    public boolean openCamera() {
        System.out.println("Opening camera "+camera_id);
        synchronized (this) {
	        releaseCamera();
	        mCamera = new VideoCapture(camera_id);
	        if (!mCamera.isOpened()) {
	            mCamera.release();
	            mCamera = null;
                System.out.println("Failed to open camera " + camera_id);
	            return false;
	        }
	    }
        return true;
    }
    
    public void releaseCamera() {
        synchronized (this) {
	        if (mCamera != null) {
	                mCamera.release();
	                mCamera = null;
            }
        }
    }
    
    public void setupCamera(int width, int height) {
        System.out.println("Camera "+camera_id+" setup ("+width+", "+height+")");
        synchronized (this) {
            if (mCamera != null && mCamera.isOpened()) {
                List<Size> sizes = mCamera.getSupportedPreviewSizes();
                int mFrameWidth = width;
                int mFrameHeight = height;

                // Selecting optimal camera preview size
                // Minimize the distance to the current camera size
                double minDiff = Double.MAX_VALUE;
                for (Size size : sizes) {
                    if ((Math.abs(size.height - height) + Math.abs(size.width - width))< minDiff) {
                        mFrameWidth = (int) size.width;
                        mFrameHeight = (int) size.height;
                        minDiff = Math.abs(size.height - height);
                    }
                }

                // Configure camera settings
                mCamera.set(Highgui.CV_CAP_PROP_ANDROID_ANTIBANDING, Highgui.CV_CAP_ANDROID_ANTIBANDING_OFF);
                mCamera.set(Highgui.CV_CAP_PROP_ANDROID_FLASH_MODE, Highgui.CV_CAP_ANDROID_FLASH_MODE_OFF);
                mCamera.set(Highgui.CV_CAP_PROP_ANDROID_FOCUS_MODE, Highgui.CV_CAP_ANDROID_FOCUS_MODE_CONTINUOUS_VIDEO);
                mCamera.set(Highgui.CV_CAP_PROP_ANDROID_WHITE_BALANCE, Highgui.CV_CAP_ANDROID_WHITE_BALANCE_FLUORESCENT);
//                mCamera.set(Highgui.CV_CAP_PROP_IOS_DEVICE_EXPOSURE,
//                Log.i(TAG, "setupCamera 6: " + mCamera.get(Highgui.CV_CAP_PROP_IOS_DEVICE_EXPOSURE));
                
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
            }
        }

    }
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        setupCamera(width,height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    protected abstract Bitmap processFrame(VideoCapture capture);

    public void run() {
        while (true) {
            Bitmap bmp = null;

            if(!mHolder.getSurface().isValid())
                continue;

            synchronized (this) {
                // If we do not have a camera, skip
                if (mCamera == null)
                    break;
                // If we can't get a frame skip
                if (!mCamera.grab()) {
                    break;
                }
                // If we have a frame, process it
                bmp = processFrame(mCamera);
            }



//            Canvas canvas = mHolder.lockCanvas();
//            mHolder.unlockCanvasAndPost(canvas);
//            bmp.recycle();
            // TODO: If we have a bitmap then display it for the user
            // TODO: This causes this thread to lag, deal with this later
            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(bmp, null, new Rect(0, 0, getWidth(), getHeight()), null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
                //bmp.recycle();
            }
        }
    }
}