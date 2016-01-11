/*
 * Copyright (c) 2011, Chad Rockey
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Android Sensors Driver nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.ros.android.android_sensors_driver.publishers.images2;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.ros.android.android_sensors_driver.R;
import org.ros.node.ConnectedNode;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 */

public class CameraPublisher implements NodeMain
{
    private ArrayList<Sample2View> mViews;
    private ArrayList<View> mViewList;
    private ArrayList<Integer> camera_ids;
    private ArrayList<ImageParams.ViewMode> cameras_viewmode;
    private ArrayList<ImageParams.CompressionLevel> cameras_compression;
    private String robotName;
    private Activity mainActivity;
    private ConnectedNode node = null;

    LinearLayout layout;
    LinearLayout.LayoutParams params;

    public CameraPublisher(Activity mainAct, ArrayList<Integer> camera_ids, String robotName, ArrayList<ImageParams.ViewMode> cameras_viewmode, ArrayList<ImageParams.CompressionLevel> cameras_compression) {
        this.mainActivity = mainAct;
        this.camera_ids = camera_ids;
        this.robotName = robotName;
        this.cameras_viewmode = cameras_viewmode;
        this.cameras_compression = cameras_compression;
        // Layout variables
        layout = (LinearLayout) mainActivity.findViewById(R.id.view_main);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("android_sensors_driver/camera_publisher");
    }

    public void onStart(final ConnectedNode node) {
        this.node = node;
        this.mViews = new ArrayList<>();
        this.mViewList = new ArrayList<>();
        // See if we can load opencv
        try {
            if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this.mainActivity, mOpenCVCallBack)) {
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

    @SuppressWarnings("deprecation")
    public void resume() {
        // Loop through all the current views
        for(int i=0;i<mViews.size(); i++) {
            // Check to see if we still have control of the cameras
            if ((null != mViews.get(i)) && !mViews.get(i).openCamera()) {
                TextView textView = new TextView(mainActivity.getBaseContext());
                textView.setTextSize(40);
                textView.setText("Camera " + (camera_ids.get(i)+1));
                textView.setGravity(Gravity.CENTER);
                //View temp = mViewList.get(i);
                //temp = textView;
            }
            // If we do, ensure we have the view added
            else {
                //View temp = mViewList.get(i);
                //temp = mViews.get(i);
            }
        }
    }

    public void releaseCameras() {
        // Release the cameras
        for(int i=0;i<mViews.size(); i++){
            if (null != mViews.get(i)) {
                mViews.get(i).releaseCamera();
            }
        }
    }

    public void onError(Node node, Throwable throwable) {
    }

    @Override
    public void onShutdown(Node arg0) {
        // Release
        releaseCameras();
        // Have to run in the UI thread, clear all the views
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(int i=0; i<mViewList.size(); i++)
                    layout.removeView(mViewList.get(i));
            }
        });
    }

    @Override
    public void onShutdownComplete(Node arg0) {
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
            // Create and set views
            for(int i=0; i<camera_ids.size(); i++) {
                // Create a new camera node
                Sample2View temp = new Sample2View(mainActivity.getBaseContext(), node, camera_ids.get(i), robotName, cameras_viewmode.get(i), cameras_compression.get(i));
                mViews.add(temp);
                mViewList.add(temp);
            }
            // Add the camera views
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(int i=0; i<mViews.size(); i++) {
                        // Check to see if we opened successfully
                        if (!mViews.get(i).openCamera()) {
                            TextView textView = new TextView(mainActivity.getBaseContext());
                            textView.setTextSize(40);
                            textView.setText("Camera " + (camera_ids.get(i) + 1));
                            textView.setGravity(Gravity.CENTER);
                            layout.addView(textView, params);
                            mViewList.add(textView);
                        }
                        // If we did, ensure we have the view added
                        else {
                            layout.addView(mViews.get(i), params);
                        }
                    }
                }
            });
        }
    };

}

