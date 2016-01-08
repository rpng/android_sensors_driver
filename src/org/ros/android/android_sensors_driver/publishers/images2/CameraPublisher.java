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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.ros.android.android_sensors_driver.MainActivity;
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
    private String robotName;
    private Activity mainActivity;
    private ConnectedNode node = null;

    public CameraPublisher(Activity mainAct, ArrayList<Integer> camera_ids, String robotName) {
        this.mainActivity = mainAct;
        this.camera_ids = camera_ids;
        this.robotName = robotName;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("android_sensors_driver/camera_publisher");
    }

    public void onStart(final ConnectedNode node) {
        this.node = node;
        this.mViews = new ArrayList<>();
        this.mViewList = new ArrayList<>();
        // See if we can load opencv
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this.mainActivity, mOpenCVCallBack)) {
            Toast toast = Toast.makeText(mainActivity, "Cannot connect to OpenCV Manager", Toast.LENGTH_SHORT);
            toast.show();
            System.out.println("Cannot connect to OpenCV Manager");
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

    public void releaseCamera() {
        for(int i=0;i<mViews.size(); i++){
            if (null != mViews.get(i))
                mViews.get(i).releaseCamera();
        }
    }

    public void onError(Node node, Throwable throwable) {
    }

    @Override
    public void onShutdown(Node arg0) {
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
            // Create our layout we will put the views in
            LinearLayout layout = new LinearLayout(mainActivity.getBaseContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            // Create and set views
            for(Integer camera_id: camera_ids) {
                // Create a new camera node
                Sample2View temp = new Sample2View(mainActivity.findViewById(R.id.view_main).getContext(), node, camera_id, robotName);
                mViews.add(temp);
                // Check to see if we opened successfully
                if (!temp.openCamera()) {
                    TextView textView = new TextView(mainActivity.findViewById(R.id.view_main).getContext());
                    textView.setTextSize(40);
                    textView.setText("Camera " + (camera_id+1));
                    textView.setGravity(Gravity.CENTER);
                    layout.addView(textView, params);
                    mViewList.add(textView);
                }
                // If we did, ensure we have the view added
                else {
                    layout.addView(temp, params);
                    mViewList.add(temp);
                }
            }
            mainActivity.addContentView(layout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
    };

}

