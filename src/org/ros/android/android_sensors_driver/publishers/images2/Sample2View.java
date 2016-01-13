package org.ros.android.android_sensors_driver.publishers.images2;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.ros.android.android_sensors_driver.MainActivity;
import org.ros.android.android_sensors_driver.R;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

/**
* @author axelfurlan@gmail.com (Axel Furlan)
*/

class Sample2View extends SampleCvViewBase {
    private Mat mRgba;
    private Mat mGray;
    private Mat mIntermediateMat;

    private int camera_id;
    private ImageParams.ViewMode viewMode;
    private ImageParams.CompressionLevel compressionLevel;
        
    private ByteBuffer bb;
    private Bitmap bmp;
    
    private Publisher<sensor_msgs.CompressedImage> imagePublisher;
    private Publisher<sensor_msgs.Image> rawImagePublisher;
    private final Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;
    
    private ChannelBufferOutputStream stream;
    private MainActivity mainActivity;
    
    sensor_msgs.CameraInfo cameraInfo;
    private final ConnectedNode connectedNode;

    public Sample2View(MainActivity mainActivity, Context context, ConnectedNode connectedNode, int camera_id, String robotName, ImageParams.ViewMode viewMode, ImageParams.CompressionLevel compressionLevel) {
        super(context, camera_id);
        this.connectedNode = connectedNode;
        this.camera_id = camera_id;
        this.viewMode = viewMode;
        this.compressionLevel = compressionLevel;
        this.mainActivity = mainActivity;
        // Create the publishers
        this.imagePublisher = connectedNode.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/image/compressed", sensor_msgs.CompressedImage._TYPE);
        this.cameraInfoPublisher = connectedNode.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/camera_info", sensor_msgs.CameraInfo._TYPE);
        this.rawImagePublisher = connectedNode.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/image/raw", sensor_msgs.Image._TYPE);
        // Our image variables
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        bmp = null;
        bb = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (this) {
            // Initialize Mats before usage
            mGray = new Mat();
            mRgba = new Mat();
            mIntermediateMat = new Mat();
        }
        super.surfaceCreated(holder);
    }

    @Override
    protected Bitmap processFrame(VideoCapture capture)
    {
        // Check for nulls
        if(mGray == null || mRgba == null || mIntermediateMat == null)
            return null;
        // Use opencv to handle our image based on our configuration
        switch (viewMode){
            case GRAY:
	            capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
	            Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
	            break;
            case RGBA:
	            capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
                //Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_RGB2BGRA, 4);
                //Core.putText(mRgba, "OpenCV + Android", new Point(10, 100), 3, 2, new Scalar(255, 0, 0, 255), 3);
	            break;
            case CANNY:
	            capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
	            Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
	            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
				break;
        }

        // Time to publish these messages
        Time currentTime = connectedNode.getCurrentTime();

        try
        {
            // If we have a null bitmap, or it has changed size, create a new one
            if(bmp == null || mRgba.rows() != bmp.getHeight() || mRgba.cols()!=bmp.getWidth()) {
                bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            }

            // Create our buffer for raw image
            if(compressionLevel == ImageParams.CompressionLevel.NONE || bb == null) {
                bb = ByteBuffer.allocate(bmp.getRowBytes()*bmp.getHeight());
                bb.clear();
            }

            Utils.matToBitmap(mRgba, bmp);
        	
        	cameraInfo = cameraInfoPublisher.newMessage();
            cameraInfo.getHeader().setFrameId("camera");
            cameraInfo.getHeader().setStamp(currentTime);
            cameraInfo.setWidth(mRgba.cols());
            cameraInfo.setHeight(mRgba.rows());
            cameraInfoPublisher.publish(cameraInfo);

            // If we are not "RAW" then compress
            if(compressionLevel != ImageParams.CompressionLevel.NONE) {
                // Create and configure our compresseed image message
            	sensor_msgs.CompressedImage image = imagePublisher.newMessage();
                //image.setFormat("png");
                image.setFormat("jpeg");
	            image.getHeader().setStamp(currentTime);
	            image.getHeader().setFrameId("camera");

                // Create our stream, and compress
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                bmp.compress(Bitmap.CompressFormat.JPEG, compressionLevel.getLevel(), baos);

                // Stream the data
	        	stream.buffer().writeBytes(baos.toByteArray());
                image.setData(stream.buffer().copy());
                stream.buffer().clear();

                // Publish
	        	imagePublisher.publish(image);

            } else {
	        	// Raw image
	            sensor_msgs.Image rawImage = rawImagePublisher.newMessage();
	            rawImage.getHeader().setStamp(currentTime);
	            rawImage.getHeader().setFrameId("camera");
	            rawImage.setEncoding("rgba8");
	            rawImage.setWidth(bmp.getWidth());
	            rawImage.setHeight(bmp.getHeight());
	            rawImage.setStep(bmp.getWidth()*4);

                bmp.copyPixelsToBuffer(bb);

                stream.buffer().clear();
                stream.buffer().writeBytes(bb.array());
                bb.clear();

	        	rawImage.setData(stream.buffer().copy());
	        	stream.buffer().clear();

	            rawImagePublisher.publish(rawImage);
            }

            return bmp;
        } catch(OutOfMemoryError e) {
            System.err.println("Frame conversion and publishing throws an out of memory error: " + e.getMessage());
            bmp.recycle();
            // Let the user know
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(mainActivity, "Error: Out Of Memory", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
            // Shutdown
            connectedNode.shutdown();
            return null;
        } catch(Exception e) {
            System.err.println("Frame conversion and publishing throws an exception: " + e.getMessage());
            bmp.recycle();
            return null;
        }
    }

    @Override
    public void run() {
        super.run();
        synchronized (this) {
            // Explicitly deallocate Mats
            if (mRgba != null)
                mRgba.release();
            if (mGray != null)
                mGray.release();
            if (mIntermediateMat != null)
                mIntermediateMat.release();
            if(bb != null)
                bb.clear();
            if(bmp != null)
                bmp.recycle();
            try {
                stream.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
            mRgba = null;
            mGray = null;
            mIntermediateMat = null;
            bb = null;
            bmp = null;
        }
    }
}
