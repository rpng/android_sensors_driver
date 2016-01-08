package org.ros.android.android_sensors_driver.utilities;


import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.android_sensors_driver.MainActivity;
import org.ros.android.android_sensors_driver.R;
import org.ros.android.android_sensors_driver.publishers.CamerasPublishers;
import org.ros.android.android_sensors_driver.publishers.FluidPressurePublisher;
import org.ros.android.android_sensors_driver.publishers.IlluminancePublisher;
import org.ros.android.android_sensors_driver.publishers.ImuPublisher;
import org.ros.android.android_sensors_driver.publishers.MagneticFieldPublisher;
import org.ros.android.android_sensors_driver.publishers.NavSatFixPublisher;
import org.ros.android.android_sensors_driver.publishers.TemperaturePublisher;
import org.ros.android.android_sensors_driver.publishers.images2.CameraPublisher;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private MainActivity mainActivity;
    protected final int currentApiVersion = android.os.Build.VERSION.SDK_INT;
    protected URI masterURI;
    protected NodeMainExecutor nodeMainExecutor;
    protected ArrayList<String> cameras;

    protected EditText robot_name;
    protected CheckBox checkbox_fluid;
    protected CheckBox checkbox_illuminance;
    protected CheckBox checkbox_imu;
    protected CheckBox checkbox_magnetic;
    protected CheckBox checkbox_navsat;
    protected CheckBox checkbox_temp;
    protected Button button_config;

    protected String old_robot_name;
    protected boolean old_fluid;
    protected boolean old_illuminance;
    protected boolean old_imu;
    protected boolean old_magnetic;
    protected boolean old_navsat;
    protected boolean old_temp;


    protected FluidPressurePublisher pub_fluid;
    protected IlluminancePublisher pub_illuminance;
    protected ImuPublisher pub_imu;
    protected MagneticFieldPublisher pub_magnetic;
    protected NavSatFixPublisher pub_navsat;
    protected TemperaturePublisher pub_temp;
    protected CamerasPublishers pub_cameras;

    protected LocationManager mLocationManager;
    protected SensorManager mSensorManager;


    public Config(MainActivity mainActivity) {
        // Save our activity pointer
        this.mainActivity = mainActivity;
        this.nodeMainExecutor = null;

        // Load our references
        robot_name = (EditText) mainActivity.findViewById(R.id.robot_name);
        checkbox_fluid = (CheckBox) mainActivity.findViewById(R.id.checkbox_fluid);
        checkbox_illuminance = (CheckBox) mainActivity.findViewById(R.id.checkbox_illuminance);
        checkbox_imu = (CheckBox) mainActivity.findViewById(R.id.checkbox_imu);
        checkbox_magnetic = (CheckBox) mainActivity.findViewById(R.id.checkbox_magnetic);
        checkbox_navsat = (CheckBox) mainActivity.findViewById(R.id.checkbox_navsat);
        checkbox_temp = (CheckBox) mainActivity.findViewById(R.id.checkbox_temp);
        button_config = (Button) mainActivity.findViewById(R.id.config_submit);

        // Load old variables, booleans default to false
        old_robot_name = robot_name.getText().toString();

        // Load our camera listing in
        load_cameras();

        // Start the services we need
        mLocationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) mainActivity.getSystemService(MainActivity.SENSOR_SERVICE);
    }

    /**
     * This function creates all the event nodes that publish information
     * Each sensor gets its own node, and is responsible for publishing its events
     * If a new name is entered, we restart all them
     * Each node's state is stored so we do not restart already started nodes
     */
    public void update_publishers() {
        // Dis-enable button
        button_config.setEnabled(false);
        // Get the name of the robot
        String robot_name_text = robot_name.getText().toString();
        // 10,000 us == 100 Hz for Android 3.1 and above
        int sensorDelay = 10000;
        // 16.7Hz for older devices.
        // They only support enum values, not the microsecond version.
        if (currentApiVersion <= android.os.Build.VERSION_CODES.HONEYCOMB) {
            sensorDelay = SensorManager.SENSOR_DELAY_UI;
        }

        int tempSensor;
        // Use newer temperature if possible
        if (currentApiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tempSensor = Sensor.TYPE_AMBIENT_TEMPERATURE;
        }
        else {
            //noinspection deprecation
            tempSensor = Sensor.TYPE_TEMPERATURE; // Older temperature
        }

        // If we have a new name, then we need to redo all publishing nodes
        if(!old_robot_name.equals(robot_name_text)) {
            nodeMainExecutor.shutdownNodeMain(pub_fluid);
            nodeMainExecutor.shutdownNodeMain(pub_illuminance);
            nodeMainExecutor.shutdownNodeMain(pub_imu);
            nodeMainExecutor.shutdownNodeMain(pub_magnetic);
            nodeMainExecutor.shutdownNodeMain(pub_navsat);
            nodeMainExecutor.shutdownNodeMain(pub_temp);
            old_fluid = false;
            old_illuminance = false;
            old_imu = false;
            old_magnetic = false;
            old_navsat = false;
            old_temp = false;
        }

        // Fluid node startup
        if(checkbox_fluid.isChecked() != old_fluid && checkbox_fluid.isChecked()) {
            NodeConfiguration nodeConfiguration1 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration1.setMasterUri(masterURI);
            nodeConfiguration1.setNodeName("android_sensors_driver_pressure");
            this.pub_fluid = new FluidPressurePublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_fluid, nodeConfiguration1);
        }
        // Fluid node shutdown
        else if(checkbox_fluid.isChecked() != old_fluid) {
            nodeMainExecutor.shutdownNodeMain(pub_fluid);
        }

        // Illuminance node startup
        if(checkbox_illuminance.isChecked() != old_illuminance && checkbox_illuminance.isChecked()) {
            NodeConfiguration nodeConfiguration2 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration2.setMasterUri(masterURI);
            nodeConfiguration2.setNodeName("android_sensors_driver_illuminance");
            this.pub_illuminance = new IlluminancePublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_illuminance, nodeConfiguration2);
        }
        // Illuminance node shutdown
        else if(checkbox_illuminance.isChecked() != old_illuminance) {
            nodeMainExecutor.shutdownNodeMain(pub_illuminance);
        }

        // IMU node startup
        if(checkbox_imu.isChecked() != old_imu && checkbox_imu.isChecked()) {
            NodeConfiguration nodeConfiguration3 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration3.setMasterUri(masterURI);
            nodeConfiguration3.setNodeName("android_sensors_driver_imu");
            this.pub_imu = new ImuPublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_imu, nodeConfiguration3);
        }
        // IMU node shutdown
        else if(checkbox_imu.isChecked() != old_imu) {
            nodeMainExecutor.shutdownNodeMain(pub_imu);
        }

        // Magnetic node startup
        if(checkbox_magnetic.isChecked() != old_magnetic && checkbox_magnetic.isChecked()) {
            NodeConfiguration nodeConfiguration4 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration4.setMasterUri(masterURI);
            nodeConfiguration4.setNodeName("android_sensors_driver_magnetic_field");
            this.pub_magnetic = new MagneticFieldPublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_magnetic, nodeConfiguration4);
        }
        // Magnetic node shutdown
        else if(checkbox_magnetic.isChecked() != old_magnetic) {
            nodeMainExecutor.shutdownNodeMain(pub_magnetic);
        }

        // Navigation satellite node startup
        if(checkbox_navsat.isChecked() != old_navsat && checkbox_navsat.isChecked()) {
            NodeConfiguration nodeConfiguration5 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration5.setMasterUri(masterURI);
            nodeConfiguration5.setNodeName("android_sensors_driver_nav_sat_fix");
            this.pub_navsat = new NavSatFixPublisher(mLocationManager, robot_name_text);
            nodeMainExecutor.execute(this.pub_navsat, nodeConfiguration5);
        }
        // Navigation satellite node shutdown
        else if(checkbox_navsat.isChecked() != old_navsat) {
            nodeMainExecutor.shutdownNodeMain(pub_navsat);
        }

        // Temperature node startup
        if(checkbox_temp.isChecked() != old_temp && checkbox_temp.isChecked()) {
            NodeConfiguration nodeConfiguration6 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration6.setMasterUri(masterURI);
            nodeConfiguration6.setNodeName("android_sensors_driver_temperature");
            this.pub_temp = new TemperaturePublisher(mSensorManager, sensorDelay, tempSensor, robot_name_text);
            nodeMainExecutor.execute(this.pub_temp, nodeConfiguration6);
        }
        // Temperature node shutdown
        else if(checkbox_temp.isChecked() != old_temp) {
            nodeMainExecutor.shutdownNodeMain(pub_temp);
        }

        // Finally, update our old states
        old_robot_name = robot_name.getText().toString();
        old_fluid = checkbox_fluid.isChecked();
        old_illuminance = checkbox_illuminance.isChecked();
        old_imu = checkbox_imu.isChecked();
        old_magnetic = checkbox_magnetic.isChecked();
        old_navsat = checkbox_navsat.isChecked();
        old_temp = checkbox_temp.isChecked();

        // Re-enable button
        button_config.setEnabled(true);
    }

    /**
     * Sets our node executor
     * This is called when the activity has been loaded
     */
    public void setNodeExecutor(NodeMainExecutor nodeExecutor) {
        this.masterURI = mainActivity.getMasterUri();
        this.nodeMainExecutor = nodeExecutor;
    }

    public void load_cameras() {
        // Init
        cameras = new ArrayList<>();
        boolean error = false;
        // Go through all cameras and get stats
        for(int i=0; i< Camera.getNumberOfCameras(); i++) {
            try {
                Camera temp = Camera.open(i);
                Camera.Parameters params = temp.getParameters();
                cameras.add("Camera " + (i+1) + " (" + params.getPreviewSize().width + "x" + params.getPreviewSize().height + ")  " + params.getHorizontalViewAngle() + "\u00B0");
                temp.release();
            } catch(Exception e) {
                error = true;
            }
        }
        // If we had an error do a Toast
        if(error) {
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(mainActivity,
                            "Unable to list all cameras", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
        // Add the cameras we have to the view
        LinearLayout camera_list = (LinearLayout) mainActivity.findViewById(R.id.camera_list);
        for (String entry : cameras) {
            CheckBox checkbox = new CheckBox(mainActivity);
            checkbox.setText(entry);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(5,5,5,5);
            checkbox.setLayoutParams(params);
            checkbox.setTextAppearance(mainActivity, android.R.style.TextAppearance_Holo_Medium);
            camera_list.addView(checkbox);
        }
    }

    public void startup_cameras() {
        NodeConfiguration nodeConfiguration7 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration7.setMasterUri(masterURI);
        nodeConfiguration7.setNodeName("android_sensors_driver_cameras");
        //this.pub_cameras = new CamerasPublishers(mainActivity, robot_name.getText().toString());
        int[] camera_ids = new int[]{0,1,2};
        CameraPublisher pub_cameras = new CameraPublisher(mainActivity, camera_ids, robot_name.getText().toString());;
        nodeMainExecutor.execute(pub_cameras, nodeConfiguration7);
    }
}
