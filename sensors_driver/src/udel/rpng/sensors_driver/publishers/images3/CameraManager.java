package udel.rpng.sensors_driver.publishers.images3;


import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import udel.rpng.sensors_driver.MainActivity;
import udel.rpng.sensors_driver.R;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.util.ArrayList;

public class CameraManager  implements NodeMain {

    private ArrayList<CameraBridgeViewBase> mViewList;
    private ArrayList<CameraPublisher> mNodes;
    private ArrayList<Integer> camera_ids;
    private ArrayList<ImageParams.ViewMode> cameras_viewmode;
    private ArrayList<ImageParams.CompressionLevel> cameras_compression;

    private int camera_current;
    private String robotName;
    private MainActivity mainActivity;
    private ConnectedNode node = null;

    LinearLayout layout;
    LinearLayout.LayoutParams params;

    public CameraManager(MainActivity mainAct, ArrayList<Integer> camera_ids, String robotName, ArrayList<ImageParams.ViewMode> cameras_viewmode, ArrayList<ImageParams.CompressionLevel> cameras_compression) {
        this.mainActivity = mainAct;
        this.camera_ids = camera_ids;
        this.robotName = robotName;
        this.cameras_viewmode = cameras_viewmode;
        this.cameras_compression = cameras_compression;
        // Layout variables
        layout = (LinearLayout) mainActivity.findViewById(R.id.view_main_content);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    }


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_sensors_driver/camera_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.node = connectedNode;
        this.mViewList = new ArrayList<CameraBridgeViewBase>();
        this.mNodes = new ArrayList<CameraPublisher>();
        // See if we can load opencv
        try {
            if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this.mainActivity, mOpenCVCallBack)) {
                Toast toast = Toast.makeText(mainActivity, "Cannot connect to OpenCV Manager", Toast.LENGTH_SHORT);
                toast.show();
                System.out.println("Cannot connect to OpenCV Manager");
                mainActivity.finish();
            }
        } catch(Exception e) {
            // Debug
            System.out.println("Cannot connect to OpenCV Manager");
            e.printStackTrace();
            // Toast to the user
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(mainActivity, "Cannot connect to OpenCV Manager", Toast.LENGTH_SHORT);
                    toast.show();
                    mainActivity.finish();
                }
            });
        }
    }

    @Override
    public void onShutdown(Node node) {
        for(int i=0; i<mViewList.size(); i++) {
            mViewList.get(i).disableView();
            mViewList.get(i).disableFpsMeter();
            mNodes.get(i).onShutdown(node);
        }
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                for(int i=0; i<mViewList.size(); i++) {
                    layout.removeView(mViewList.get(i));
                }
            }
        });
    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(mainActivity)
    {
        @Override
        @SuppressWarnings("deprecation")
        public void onManagerConnected(int status) {
            // Check if we have successfully loaded opencv
            if(status != LoaderCallbackInterface.SUCCESS) {
                // Tell the user it failed
                mainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast toast = Toast.makeText(mainActivity, "Cannot connect to OpenCV Manager", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
                System.out.println("OpenCV loading FAILED!");
                // Send to super
                super.onManagerConnected(status);
                return;
            }
            // Our first camera
            camera_current = 0;
            // Create and set views
            for(int i=0; i<camera_ids.size(); i++) {
                // Create a new camera node
                JavaCameraView mOpenCvCameraView = new JavaCameraView(mainActivity, camera_ids.get(i));
                CameraPublisher pub = new CameraPublisher(camera_ids.get(i), robotName, cameras_viewmode.get(i), cameras_compression.get(i));
                mOpenCvCameraView.enableView();
                mOpenCvCameraView.enableFpsMeter();
                mOpenCvCameraView.setCvCameraViewListener(pub);
                mViewList.add(mOpenCvCameraView);
                mNodes.add(pub);
                // Start the node
                pub.onStart(node);
            }
            // Add the camera views
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(int i=0; i<mViewList.size(); i++) {
                        layout.addView(mViewList.get(i), params);
                    }
                }
            });
        }
    };
}
