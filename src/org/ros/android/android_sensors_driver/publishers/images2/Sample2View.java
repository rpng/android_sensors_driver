package org.ros.android.android_sensors_driver.publishers.images2;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

/**
* @author axelfurlan@gmail.com (Axel Furlan)
*/

class Sample2View extends SampleCvViewBase {
    private Mat mRgba;
    private Mat mGray;
    private Mat mIntermediateMat;

    private int camera_id;
    private String robotName;
    private double stats[][];
    private int counter;
    private int numSamples = 50;
    private Time oldTime;
    private Time newTime;
        
    private ByteBuffer bb;
    private Bitmap bmp;
    
    private Publisher<sensor_msgs.CompressedImage> imagePublisher;
    private Publisher<sensor_msgs.Image> rawImagePublisher;
    private final Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;
    
    private ChannelBufferOutputStream stream;
    
    sensor_msgs.CameraInfo cameraInfo;
    private final ConnectedNode connectedNode;

    public Sample2View(Context context, ConnectedNode connectedNode, int camera_id, String robotName) {
        super(context, camera_id);
        this.connectedNode = connectedNode;
        this.camera_id = camera_id;
        // Create the publishers
        this.imagePublisher = connectedNode.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/image/compressed", sensor_msgs.CompressedImage._TYPE);
        this.cameraInfoPublisher = connectedNode.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/camera_info", sensor_msgs.CameraInfo._TYPE);
        this.rawImagePublisher = connectedNode.newPublisher("/android/"+robotName+"/camera_"+(camera_id+1)+"/image/raw", sensor_msgs.Image._TYPE);
        // Our image variables
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        bmp = null;
        bb = null;
        // TODO: Stats (delete or update)
        stats = new double[10][numSamples];
        oldTime = connectedNode.getCurrentTime();
        counter = 0;
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
    	Time[] measureTime = new Time[9];
    	String[] compDescStrings = {"Total processFrame","Grab a new frame","MatToBitmap","Publish cameraInfo",
    								"Create ImageMsg","Compress image","Transfer to Stream","Image.SetData","Publish Image","Total econds per frame"};
    	String[] rawDescStrings = { "Total processFrame","Grab a new frame","MatToBitmap","Publish cameraInfo",
									"Create ImageMsg","Pixel to buffer","Transfer to Stream","Image.SetData","Publish Image","Total seconds per frame"};
    	measureTime[0] = connectedNode.getCurrentTime();
    	
        //switch (MainActivity.viewMode)
        //{
//	        case MainActivity.VIEW_MODE_GRAY:
	            capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
	            Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
//	            break;
//	        case MainActivity.VIEW_MODE_RGBA:
//	            capture.retrieve(mIntermediateMat, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
//                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_RGB2BGRA, 4);
        //Core.putText(mRgba, "OpenCV + Android", new Point(10, 100), 3, 2, new Scalar(255, 0, 0, 255), 3);
//	            break;
//	        case MainActivity.VIEW_MODE_CANNY:
//	            capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
//	            Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
//	            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
		//		break;
        //}
        Time currentTime = connectedNode.getCurrentTime();
        
        measureTime[1] = connectedNode.getCurrentTime();
        
        if(bmp == null || mRgba.rows() != bmp.getHeight() || mRgba.cols()!=bmp.getWidth()) {
            bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            System.out.println("HIT");
        }

//        if(MainActivity.imageCompression == MainActivity.IMAGE_TRANSPORT_COMPRESSION_NONE && bb == null)
//        {
//        	Log.i(TAG,"Buffer 1");
//        	bb = ByteBuffer.allocate(bmp.getRowBytes()*bmp.getHeight());
//        	Log.i(TAG,"Buffer 2");
//        	bb.clear();
//        	Log.i(TAG,"Buffer 3");
//        }
        try
        {
        	Utils.matToBitmap(mRgba, bmp);
        	measureTime[2] = connectedNode.getCurrentTime();
        	
        	cameraInfo = cameraInfoPublisher.newMessage();
            cameraInfo.getHeader().setFrameId("camera");
            cameraInfo.getHeader().setStamp(currentTime);
            cameraInfo.setWidth(640);
            cameraInfo.setHeight(480);
            cameraInfoPublisher.publish(cameraInfo);
            measureTime[3] = connectedNode.getCurrentTime();
            
            //if(MainActivity.imageCompression >= MainActivity.IMAGE_TRANSPORT_COMPRESSION_PNG)
            //{
            	//Compressed image
            	
            	sensor_msgs.CompressedImage image = imagePublisher.newMessage();
	            //if(MainActivity.imageCompression == MainActivity.IMAGE_TRANSPORT_COMPRESSION_PNG)
	            //	image.setFormat("png");
	            //else if(MainActivity.imageCompression == MainActivity.IMAGE_TRANSPORT_COMPRESSION_JPEG)
	            	image.setFormat("jpeg");
	            image.getHeader().setStamp(currentTime);
	            image.getHeader().setFrameId("camera");
	            measureTime[4] = connectedNode.getCurrentTime();
	
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            //if(MainActivity.imageCompression == MainActivity.IMAGE_TRANSPORT_COMPRESSION_PNG)
	            //	bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
	            //else if(MainActivity.imageCompression == MainActivity.IMAGE_TRANSPORT_COMPRESSION_JPEG)
	            	bmp.compress(Bitmap.CompressFormat.JPEG, 20, baos);
	        	measureTime[5] = connectedNode.getCurrentTime();
	
	        	stream.buffer().writeBytes(baos.toByteArray());
	        	measureTime[6] = connectedNode.getCurrentTime();
	
	        	image.setData(stream.buffer().copy());
	        	measureTime[7] = connectedNode.getCurrentTime();
	
	            stream.buffer().clear();
	        	imagePublisher.publish(image);
	        	measureTime[8] = connectedNode.getCurrentTime();
            /*
            }
            else
            {
	        	// Raw image
		        
            	Log.i(TAG,"Raw image 1");
	            sensor_msgs.Image rawImage = rawImagePublisher.newMessage();
	            rawImage.getHeader().setStamp(currentTime);
	            rawImage.getHeader().setFrameId("camera");
	            rawImage.setEncoding("rgba8");
	            rawImage.setWidth(bmp.getWidth());
	            rawImage.setHeight(bmp.getHeight());
	            rawImage.setStep(640);
	            measureTime[4] = connectedNode.getCurrentTime();
		
	            Log.i(TAG,"Raw image 2");
	            
	            bmp.copyPixelsToBuffer(bb);
	            measureTime[5] = connectedNode.getCurrentTime();
	
	            Log.i(TAG,"Raw image 3");
	            
	            stream.buffer().writeBytes(bb.array());
	            bb.clear();
	            measureTime[6] = connectedNode.getCurrentTime();
	
	            Log.i(TAG,"Raw image 4");
	            
	        	rawImage.setData(stream.buffer().copy());
	        	stream.buffer().clear();
	        	measureTime[7] = connectedNode.getCurrentTime();
	
	        	Log.i(TAG,"Raw image 5");
	        	
	            rawImagePublisher.publish(rawImage);
	            measureTime[8] = connectedNode.getCurrentTime();
	            Log.i(TAG,"Raw image 6");
            }
            */

            newTime = connectedNode.getCurrentTime();
            stats[9][counter] = (newTime.subtract(oldTime)).nsecs/1000000.0;
            oldTime = newTime;

        	for(int i=1;i<9;i++)
        	{
        		stats[i][counter] = (measureTime[i].subtract(measureTime[i-1])).nsecs/1000000.0;
        	}
        	
        	
        	stats[0][counter] = measureTime[8].subtract(measureTime[0]).nsecs/1000000.0;
        	
        	counter++;
        	if(counter == numSamples)
        	{
        		double[] sts = new double[10];
        		Arrays.fill(sts, 0.0);
        		
        		for(int i=0;i<10;i++)
            	{
        			for(int j=0;j<numSamples;j++)
        				sts[i] += stats[i][j];
        			
        			sts[i] /= (double)numSamples;
        			
        			//if(MainActivity.imageCompression >= MainActivity.IMAGE_TRANSPORT_COMPRESSION_PNG)
        				//Log.i(TAG,String.format("Mean time for %s:\t\t%4.2fms", compDescStrings[i], sts[i]));
        			//else
        			//	Log.i(TAG,String.format("Mean time for %s:\t\t%4.2fms", rawDescStrings[i], sts[i]));
            	}
        		//Log.i(TAG,"\n\n");
        		counter = 0;
        	}
        	
        	
        	
            return bmp;
        } catch(Exception e)
        {
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

            mRgba = null;
            mGray = null;
            mIntermediateMat = null;
        }
    }
}
