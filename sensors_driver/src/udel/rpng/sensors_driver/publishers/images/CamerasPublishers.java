package udel.rpng.sensors_driver.publishers.images;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Looper;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.TextView;

import udel.rpng.sensors_driver.MainActivity;
import udel.rpng.sensors_driver.R;
import udel.rpng.sensors_driver.publishers.images.CompressedImagePublisher;
import udel.rpng.sensors_driver.publishers.images.RawImageListener;
import udel.rpng.sensors_driver.publishers.images.RawImagePublisher;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;


public class CamerasPublishers implements NodeMain {

    private MainActivity mainActivity;
    private String robotName;
    private CameraThread fpThread;
    private int[] camera_ids;

    private SurfaceHolder[] surfaceHolders;
    private SurfaceView[] surfaceViews;
    private Camera[] cameras;
    private Camera.Size[] previewSizes;
    private byte[][] previewBuffers;
    private RawImageListener[] rawImageListeners;
    private BufferingPreviewCallback[] bufferingPreviewCallbacks;


    public CamerasPublishers(MainActivity mainActivity, String robotName) {
        this.mainActivity = mainActivity;
        this.robotName = robotName;
        this.camera_ids = new int[]{0};
        this.surfaceHolders = new SurfaceHolder[camera_ids.length];
        this.surfaceViews = new SurfaceView[camera_ids.length];
        this.cameras = new Camera[camera_ids.length];
        this.previewSizes = new Camera.Size[camera_ids.length];
        this.previewBuffers = new byte[camera_ids.length][];
        this.rawImageListeners = new RawImageListener[camera_ids.length];
        this.bufferingPreviewCallbacks = new BufferingPreviewCallback[camera_ids.length];
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_sensors_driver/camera_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        //        Camera temp = Camera.open(1);
//        System.out.println("Focal Length: "+temp.getParameters().getFocalLength());
//        System.out.println("White Balance: "+temp.getParameters().getWhiteBalance());
//        System.out.println("Horiz. Angle: "+temp.getParameters().getHorizontalViewAngle());
//        System.out.println("Zoom: "+temp.getParameters().getZoom());
//        System.out.println("Size: " + temp.getParameters().getJpegThumbnailSize().height + ":" + temp.getParameters().getJpegThumbnailSize().width);

        try {
            // If we have a camera, publish it

//                this.imagePublisher = node.newPublisher("/android/" + robotName + "/camera/image/compressed", sensor_msgs.CompressedImage._TYPE);
//                this.rawImagePublisher = node.newPublisher("/android/" + robotName + "/camera/image/raw", sensor_msgs.Image._TYPE);
//                this.cameraInfoPublisher = node.newPublisher("/android/" + robotName + "/camera/info", sensor_msgs.CameraInfo._TYPE);

            // Add our new image publishers
            for(int i=0; i<camera_ids.length; i++) {
                rawImageListeners[i] = new CompressedImagePublisher(connectedNode, robotName, camera_ids[i]);
            }

            // If we have cameras, start our publishing thread
            if(camera_ids.length > 0) {
                this.fpThread = new CameraThread();
                this.fpThread.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShutdown(Node node) {
        if (this.fpThread == null) {
            return;
        }

        this.fpThread.shutdown();

        try {
            this.fpThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    private final class BufferingPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //Preconditions.checkArgument(camera == CamerasPublishers.this.camera);
            //Preconditions.checkArgument(data == previewBuffer);
            for(int i=0; i<camera_ids.length; i++) {
                if(cameras[i]!=null && cameras[i]==camera && rawImageListeners[i]!=null) {
                    rawImageListeners[i].onNewRawImage(data, previewSizes[i]);
                    camera.addCallbackBuffer(previewBuffers[i]);
                }
            }
        }
    }

    private final class SurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                for(int i=0; i<camera_ids.length; i++) {
                    cameras[i].setPreviewDisplay(surfaceHolders[i]);
                    cameras[i].addCallbackBuffer(previewBuffers[i]);
                    cameras[i].setPreviewCallback(bufferingPreviewCallbacks[i]);
                    cameras[i].startPreview();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
//            try {
//                for(int i=0; i<camera_ids.length; i++) {
//                    if (cameras[i] != null)
//                        cameras[i].setPreviewDisplay(holder);
//                }
//            } catch (IOException e) {
//                throw new RosRuntimeException(e);
//            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            for(int i=0; i<camera_ids.length; i++) {
                if (cameras[i] != null) {
                    cameras[i].setPreviewCallbackWithBuffer(null);
                    cameras[i].stopPreview();
                    cameras[i].release();
                    cameras[i] = null;
                }
            }
        }
    }

    private class CameraThread extends Thread {

        private Looper threadLooper;

        public CameraThread() {

        }

        public void run() {
            Looper.prepare();
            this.threadLooper = Looper.myLooper();

            for(int i=0; i<camera_ids.length; i++) {
                try {
                    // Open our camera
                    cameras[i] = Camera.open(camera_ids[i]);
                    previewSizes[i] = cameras[i].getParameters().getPreviewSize();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                // All cameras need a surface to send data to
                surfaceViews[i] = new SurfaceView(mainActivity.getBaseContext());
                surfaceHolders[i] = surfaceViews[i].getHolder();
                surfaceHolders[i].addCallback(new SurfaceHolderCallback());
                //surfaceHolders[i].setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

                // Create our buffer playback, which calls our image listeners
                bufferingPreviewCallbacks[i] = new BufferingPreviewCallback();
                setupBufferingPreviewCallback(i);
            }

            // Add our surface to the main view
            // Note if a surface is not added, the camera wil not publish
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    //LinearLayout layout = new LinearLayout(mainActivity.getBaseContext());
                    LinearLayout layout = (LinearLayout) mainActivity.findViewById(R.id.view_main);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 0f);
                    for(int i=0; i<camera_ids.length; i++) {
                        if(surfaceViews[i] == null) {
                            TextView textView = new TextView(mainActivity.getBaseContext());
                            textView.setTextSize(40);
                            textView.setText("Failed Camera " + camera_ids[i]);
                            textView.setGravity(Gravity.CENTER);
                            layout.addView(textView, params);
                            continue;
                        }
                        layout.addView(surfaceViews[i], params);
                    }
                    mainActivity.addContentView(layout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                }
            });

            Looper.loop();
        }

        private void setupBufferingPreviewCallback(int camera_count) {
            int format = cameras[camera_count].getParameters().getPreviewFormat();
            int bits_per_pixel = ImageFormat.getBitsPerPixel(format);
            previewBuffers[camera_count] = new byte[previewSizes[camera_count].height * previewSizes[camera_count].width * bits_per_pixel / 8];
            cameras[camera_count].addCallbackBuffer(previewBuffers[camera_count]);
            cameras[camera_count].setPreviewCallbackWithBuffer(bufferingPreviewCallbacks[camera_count]);
        }

        public void shutdown() {
            if (this.threadLooper != null) {
                this.threadLooper.quit();
            }
        }

    }

}

