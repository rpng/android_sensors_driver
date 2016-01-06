/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_sensors_driver;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.android_sensors_driver.publishers.FluidPressurePublisher;
import org.ros.android.android_sensors_driver.publishers.IlluminancePublisher;
import org.ros.android.android_sensors_driver.publishers.ImuPublisher;
import org.ros.android.android_sensors_driver.publishers.MagneticFieldPublisher;
import org.ros.android.android_sensors_driver.publishers.NavSatFixPublisher;
import org.ros.android.android_sensors_driver.publishers.TemperaturePublisher;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 */


public class MainActivity extends RosActivity {
    protected final int MASTER_CHOOSER_REQUEST_CODE = 1;
    protected final int currentApiVersion = android.os.Build.VERSION.SDK_INT;
    private NodeMainExecutor nodeMainExecutor;

    protected NavSatFixPublisher fix_pub;
    protected ImuPublisher imu_pub;
    protected MagneticFieldPublisher magnetic_field_pub;
    protected FluidPressurePublisher fluid_pressure_pub;
    protected IlluminancePublisher illuminance_pub;
    protected TemperaturePublisher temperature_pub;
    protected LocationManager mLocationManager;

    protected SensorManager mSensorManager;
    protected String robotName;

    protected Button button1;
    protected Button button2;
    private View mMainView;
    private View mConfigView;
    private int mShortAnimationDuration;


    public MainActivity() {
        super("ROS Sensors Driver", "ROS Sensors Driver");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set our content view as our main activity that has fragments in it
        setContentView(R.layout.activity_main);

        // Load the different views
        mMainView = findViewById(R.id.view_main);
        mConfigView = findViewById(R.id.view_config);

        // Initially hide the main view, we want to configure the unit first
        mMainView.setVisibility(View.GONE);
        mConfigView.setVisibility(View.VISIBLE);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        // Start the services we need
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        // Load our listeners on all the objects
        loadListeners();
    }

    /**
     * This function loads all needed listeners for the project
     * Handles all the views inside the main_activity
     */
    public void loadListeners() {

        final Context context = this;

        button1 = (Button) findViewById(R.id.button1);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
//                Intent intent = new Intent(context, ConfigActivity.class);
//                startActivity(intent);
                System.out.println("HIT 4");
            }
        });


    }

    @Override
    protected void onDestroy() { super.onDestroy(); }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() { super.onResume(); }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        this.nodeMainExecutor = nodeMainExecutor;
    }

    /**
     * When the views are loaded, load in our activity_main menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Callback for menus buttons
     * If it is the help button display the popup with information
     * If it is the configuration button switch to the configuration view
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If help button, display popup
        if (item.getItemId() == R.id.menu_help) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(getResources().getString(R.string.help_title));
            builder.setMessage(getResources().getString(R.string.help_message));
            builder.setInverseBackgroundForced(true);
            builder.setNegativeButton(getResources().getString(R.string.help_ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.setNeutralButton(getResources().getString(R.string.help_wiki),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            Uri u = Uri.parse("https://github.com/rpng/android_sensors_driver/wiki");
                            try {
                                // Start the activity
                                i.setData(u);
                                startActivity(i);
                            } catch (ActivityNotFoundException e) {
                                // Raise on activity not found
                                Toast toast = Toast.makeText(MainActivity.this, "Browser not found.", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
            builder.setPositiveButton(getResources().getString(R.string.help_report),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            Uri u = Uri.parse("https://github.com/rpng/android_sensors_driver/issues/new");
                            try {
                                // Start the activity
                                i.setData(u);
                                startActivity(i);
                            } catch (ActivityNotFoundException e) {
                                // Raise on activity not found
                                Toast toast = Toast.makeText(MainActivity.this, "Browser not found.", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        // If config button switch our views
        else if(item.getItemId() == R.id.menu_config) {
            toggleConfigView();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Cross-fades between {@link #mMainView} and {@link #mConfigView}
     * This is called when the config button is pressed, or after submitting the config form
     * http://developer.android.com/training/animation/crossfade.html#views
     */
    private void toggleConfigView() {
        // Decide which view to hide and which to show.
        boolean state = mConfigView.getVisibility() != View.VISIBLE;
        final View showView = state ? mConfigView : mMainView;
        final View hideView = state ? mMainView : mConfigView;

        // Set the "show" view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);

        // Animate the "show" view to 100% opacity, and clear any animation listener set on
        // the view. Remember that listeners are not limited to the specific animation
        // describes in the chained method calls. Listeners are set on the
        // ViewPropertyAnimator object for the view, which persists across several
        // animations.
        showView.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);

        // Animate the "hide" view to 0% opacity. After the animation ends, set its visibility
        // to GONE as an optimization step (it won't participate in layout passes, etc.)
        hideView.animate()
            .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * This function creates all the event nodes that publish information
     * Each sensor gets its own node, and is responsible for publishing its events
     */
    private void construct_publishers(NodeMainExecutor nodeMainExecutor) {
        URI masterURI = getMasterUri();

        int sensorDelay = 10000; // 10,000 us == 100 Hz for Android 3.1 and above
        if (currentApiVersion <= android.os.Build.VERSION_CODES.HONEYCOMB) {
            sensorDelay = SensorManager.SENSOR_DELAY_UI; // 16.7Hz for older devices.  They only support enum values, not the microsecond version.
        }

        int tempSensor;
        if (currentApiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tempSensor = Sensor.TYPE_AMBIENT_TEMPERATURE; // Use newer temperature if possible
        }
        else {
            //noinspection deprecation
            tempSensor = Sensor.TYPE_TEMPERATURE; // Older temperature
        }

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration.setMasterUri(masterURI);
        nodeConfiguration.setNodeName("android_sensors_driver_magnetic_field");
        this.magnetic_field_pub = new MagneticFieldPublisher(mSensorManager, sensorDelay, robotName);
        nodeMainExecutor.execute(this.magnetic_field_pub, nodeConfiguration);

        NodeConfiguration nodeConfiguration2 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration2.setMasterUri(masterURI);
        nodeConfiguration2.setNodeName("android_sensors_driver_nav_sat_fix");
        this.fix_pub = new NavSatFixPublisher(mLocationManager, robotName);
        nodeMainExecutor.execute(this.fix_pub, nodeConfiguration2);

        NodeConfiguration nodeConfiguration3 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration3.setMasterUri(masterURI);
        nodeConfiguration3.setNodeName("android_sensors_driver_imu");
        this.imu_pub = new ImuPublisher(mSensorManager, sensorDelay, robotName);
        nodeMainExecutor.execute(this.imu_pub, nodeConfiguration3);

        NodeConfiguration nodeConfiguration4 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration4.setMasterUri(masterURI);
        nodeConfiguration4.setNodeName("android_sensors_driver_pressure");
        this.fluid_pressure_pub = new FluidPressurePublisher(mSensorManager, sensorDelay, robotName);
        nodeMainExecutor.execute(this.fluid_pressure_pub, nodeConfiguration4);

        NodeConfiguration nodeConfiguration5 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration5.setMasterUri(masterURI);
        nodeConfiguration5.setNodeName("android_sensors_driver_illuminance");
        this.illuminance_pub = new IlluminancePublisher(mSensorManager, sensorDelay, robotName);
        nodeMainExecutor.execute(this.illuminance_pub, nodeConfiguration5);

        NodeConfiguration nodeConfiguration6 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration6.setMasterUri(masterURI);
        nodeConfiguration6.setNodeName("android_sensors_driver_temperature");
        this.temperature_pub = new TemperaturePublisher(mSensorManager, sensorDelay, tempSensor, robotName);
        nodeMainExecutor.execute(this.temperature_pub, nodeConfiguration6);
    }
}
