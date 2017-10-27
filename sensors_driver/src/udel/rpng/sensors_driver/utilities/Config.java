package udel.rpng.sensors_driver.utilities;


import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import udel.rpng.sensors_driver.MainActivity;
import udel.rpng.sensors_driver.R;
import udel.rpng.sensors_driver.publishers.FluidPressurePublisher;
import udel.rpng.sensors_driver.publishers.IlluminancePublisher;
import udel.rpng.sensors_driver.publishers.ImuPublisher;
import udel.rpng.sensors_driver.publishers.MagneticFieldPublisher;
import udel.rpng.sensors_driver.publishers.NavSatFixPublisher;
import udel.rpng.sensors_driver.publishers.TemperaturePublisher;
import udel.rpng.sensors_driver.publishers.images3.ImageParams;
import udel.rpng.sensors_driver.publishers.images3.CameraManager;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.ArrayList;

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
    protected LinearLayout camera_list;

    protected String old_robot_name;
    protected boolean old_fluid;
    protected boolean old_illuminance;
    protected boolean old_imu;
    protected boolean old_magnetic;
    protected boolean old_navsat;
    protected boolean old_temp;
    protected ArrayList<Boolean> old_camera_list;
    protected ArrayList<ImageParams.ViewMode> old_camera_viewmode;
    protected ArrayList<ImageParams.CompressionLevel> old_camera_compression;


    protected FluidPressurePublisher pub_fluid;
    protected IlluminancePublisher pub_illuminance;
    protected ImuPublisher pub_imu;
    protected MagneticFieldPublisher pub_magnetic;
    protected NavSatFixPublisher pub_navsat2;
    protected TemperaturePublisher pub_temp;
    protected CameraManager pub_cameras;

    //protected LocationManager mLocationManager;
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
        old_camera_list = new ArrayList<Boolean>();
        old_camera_viewmode = new ArrayList<ImageParams.ViewMode>();
        old_camera_compression = new ArrayList<ImageParams.CompressionLevel>();

        // Load our camera listing in
        load_cameras();

        // Start the services we need
        //mLocationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
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
        } else {
            //noinspection deprecation
            tempSensor = Sensor.TYPE_TEMPERATURE; // Older temperature
        }

        // If we have a new name, then we need to redo all publishing nodes
        if(!old_robot_name.equals(robot_name_text)) {
            nodeMainExecutor.shutdownNodeMain(pub_fluid);
            nodeMainExecutor.shutdownNodeMain(pub_illuminance);
            nodeMainExecutor.shutdownNodeMain(pub_imu);
            nodeMainExecutor.shutdownNodeMain(pub_magnetic);
            nodeMainExecutor.shutdownNodeMain(pub_navsat2);
            nodeMainExecutor.shutdownNodeMain(pub_temp);
            nodeMainExecutor.shutdownNodeMain(pub_cameras);
            old_fluid = false;
            old_illuminance = false;
            old_imu = false;
            old_magnetic = false;
            old_navsat = false;
            old_temp = false;
            old_camera_list = new ArrayList<Boolean>();
        }

        // Fluid node startup
        if(checkbox_fluid.isChecked() != old_fluid && checkbox_fluid.isChecked()) {
            NodeConfiguration nodeConfiguration1 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration1.setMasterUri(masterURI);
            nodeConfiguration1.setNodeName("sensors_driver_pressure");
            this.pub_fluid = new FluidPressurePublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_fluid, nodeConfiguration1);
        }
        // Fluid node shutdown
        else if(checkbox_fluid.isChecked() != old_fluid) {
            nodeMainExecutor.shutdownNodeMain(pub_fluid);
            pub_fluid = null;
        }

        // Illuminance node startup
        if(checkbox_illuminance.isChecked() != old_illuminance && checkbox_illuminance.isChecked()) {
            NodeConfiguration nodeConfiguration2 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration2.setMasterUri(masterURI);
            nodeConfiguration2.setNodeName("sensors_driver_illuminance");
            this.pub_illuminance = new IlluminancePublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_illuminance, nodeConfiguration2);
        }
        // Illuminance node shutdown
        else if(checkbox_illuminance.isChecked() != old_illuminance) {
            nodeMainExecutor.shutdownNodeMain(pub_illuminance);
            pub_illuminance = null;
        }

        // IMU node startup
        if(checkbox_imu.isChecked() != old_imu && checkbox_imu.isChecked()) {
            NodeConfiguration nodeConfiguration3 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration3.setMasterUri(masterURI);
            nodeConfiguration3.setNodeName("sensors_driver_imu");
            this.pub_imu = new ImuPublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_imu, nodeConfiguration3);
        }
        // IMU node shutdown
        else if(checkbox_imu.isChecked() != old_imu) {
            nodeMainExecutor.shutdownNodeMain(pub_imu);
            pub_imu = null;
        }

        // Magnetic node startup
        if(checkbox_magnetic.isChecked() != old_magnetic && checkbox_magnetic.isChecked()) {
            NodeConfiguration nodeConfiguration4 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration4.setMasterUri(masterURI);
            nodeConfiguration4.setNodeName("driver_magnetic_field");
            this.pub_magnetic = new MagneticFieldPublisher(mSensorManager, sensorDelay, robot_name_text);
            nodeMainExecutor.execute(this.pub_magnetic, nodeConfiguration4);
        }
        // Magnetic node shutdown
        else if(checkbox_magnetic.isChecked() != old_magnetic) {
            nodeMainExecutor.shutdownNodeMain(pub_magnetic);
            pub_magnetic = null;
        }

        // Navigation satellite node startup
        if(checkbox_navsat.isChecked() != old_navsat && checkbox_navsat.isChecked()) {
            NodeConfiguration nodeConfiguration5 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration5.setMasterUri(masterURI);
            nodeConfiguration5.setNodeName("driver_navsatfix_publisher");
            this.pub_navsat2 = new NavSatFixPublisher(mainActivity, robot_name_text);
            nodeMainExecutor.execute(this.pub_navsat2, nodeConfiguration5);
        }
        // Navigation satellite node shutdown
        else if(checkbox_navsat.isChecked() != old_navsat) {
            nodeMainExecutor.shutdownNodeMain(pub_navsat2);
            pub_navsat2 = null;
        }

        // Temperature node startup
        if(checkbox_temp.isChecked() != old_temp && checkbox_temp.isChecked()) {
            NodeConfiguration nodeConfiguration6 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration6.setMasterUri(masterURI);
            nodeConfiguration6.setNodeName("sensors_driver_temperature");
            this.pub_temp = new TemperaturePublisher(mSensorManager, sensorDelay, tempSensor, robot_name_text);
            nodeMainExecutor.execute(this.pub_temp, nodeConfiguration6);
        }
        // Temperature node shutdown
        else if(checkbox_temp.isChecked() != old_temp) {
            nodeMainExecutor.shutdownNodeMain(pub_temp);
            pub_temp = null;
        }

        // Camera node startup, restart all nodes when we change cameras
        if(hasCamerasChanged()) {
            // Ensure all cameras are shutdown
            nodeMainExecutor.shutdownNodeMain(pub_cameras);
            pub_cameras = null;
            // List of enabled cameras
            ArrayList<Integer> cameras = new ArrayList<Integer>();
            ArrayList<ImageParams.ViewMode> cameras_viewmode = new ArrayList<ImageParams.ViewMode>();
            ArrayList<ImageParams.CompressionLevel> cameras_compression = new ArrayList<ImageParams.CompressionLevel>();
            // Find all cameras that have been enabled
            for(int i=0; i<camera_list.getChildCount(); i++) {
                if(((CheckBox)((LinearLayout)camera_list.getChildAt(i)).getChildAt(0)).isChecked()) {
                    cameras.add(i);
                    cameras_viewmode.add((ImageParams.ViewMode)((Spinner)((LinearLayout)((LinearLayout) camera_list.getChildAt(i)).getChildAt(1)).getChildAt(1)).getSelectedItem());
                    cameras_compression.add((ImageParams.CompressionLevel)((Spinner)((LinearLayout) ((LinearLayout) camera_list.getChildAt(i)).getChildAt(2)).getChildAt(1)).getSelectedItem());

                }
            }
            // If we have cameras enabled, create the node
            if(cameras.size() > 0) {
                NodeConfiguration nodeConfiguration7 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
                nodeConfiguration7.setMasterUri(masterURI);
                nodeConfiguration7.setNodeName("sensors_driver_cameras");
                pub_cameras = new CameraManager(mainActivity, cameras, robot_name_text, cameras_viewmode, cameras_compression);
                nodeMainExecutor.execute(pub_cameras, nodeConfiguration7);
            }
        }

        // Finally, update our old states
        old_robot_name = robot_name.getText().toString();
        old_fluid = checkbox_fluid.isChecked();
        old_illuminance = checkbox_illuminance.isChecked();
        old_imu = checkbox_imu.isChecked();
        old_magnetic = checkbox_magnetic.isChecked();
        old_navsat = checkbox_navsat.isChecked();
        old_temp = checkbox_temp.isChecked();

        // Update camera params
        for(int i=0; i<camera_list.getChildCount(); i++) {
            old_camera_list.add(i, ((CheckBox)((LinearLayout)camera_list.getChildAt(i)).getChildAt(0)).isChecked());
            old_camera_viewmode.add(i, (ImageParams.ViewMode)((Spinner)((LinearLayout) ((LinearLayout) camera_list.getChildAt(i)).getChildAt(1)).getChildAt(1)).getSelectedItem());
            old_camera_compression.add(i, (ImageParams.CompressionLevel)((Spinner)((LinearLayout) ((LinearLayout) camera_list.getChildAt(i)).getChildAt(2)).getChildAt(1)).getSelectedItem());
        }

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

    /**
     * This loops through all cameras on the device
     * A checkbox with the camera stats is then added to the config screen
     */
    public void load_cameras() {
        // Init
        cameras = new ArrayList<String>();
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
        camera_list = (LinearLayout) mainActivity.findViewById(R.id.camera_list);
        for (String entry : cameras) {
            // Camera checkbox
            CheckBox checkbox = new CheckBox(mainActivity);
            checkbox.setText(entry);
            LinearLayout.LayoutParams params_1 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params_1.setMargins(5, 5, 5, 5);
            checkbox.setLayoutParams(params_1);
            checkbox.setTextAppearance(mainActivity, android.R.style.TextAppearance_Holo_Medium);
            // Camera view mode text
            TextView text_viewmode = new TextView(mainActivity);
            text_viewmode.setText("View Mode: ");
            LinearLayout.LayoutParams params_2 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params_2.setMargins(70, 0, 0, 0);
            text_viewmode.setLayoutParams(params_2);
            text_viewmode.setTextAppearance(mainActivity, android.R.style.TextAppearance_Holo_Medium);
            // Camera view mode dropdown
            Spinner dropdown_viewmode = new Spinner(mainActivity);
            ImageParams.ViewMode[] items_viewmode = new ImageParams.ViewMode[] {
                    ImageParams.ViewMode.GRAY,
                    ImageParams.ViewMode.RGBA,
                    ImageParams.ViewMode.CANNY
            };
            ArrayAdapter<ImageParams.ViewMode> adapter_viewmode =
                    new ArrayAdapter<ImageParams.ViewMode>(mainActivity, R.layout.custom_spinner, items_viewmode);
            adapter_viewmode.setDropDownViewResource(R.layout.custom_spinner_item);
            dropdown_viewmode.setAdapter(adapter_viewmode);
            // Camera compression text
            TextView text_compression = new TextView(mainActivity);
            text_compression.setText("Compression: ");
            text_compression.setLayoutParams(params_2);
            text_compression.setTextAppearance(mainActivity, android.R.style.TextAppearance_Holo_Medium);
            // Camera compression level dropdown
            Spinner dropdown_compresion = new Spinner(mainActivity);
            ImageParams.CompressionLevel[] items_compression = new ImageParams.CompressionLevel[] {
                    ImageParams.CompressionLevel.VERY_HIGH,
                    ImageParams.CompressionLevel.HIGH,
                    ImageParams.CompressionLevel.MEDIUM,
                    ImageParams.CompressionLevel.LOW,
                    ImageParams.CompressionLevel.NONE,
            };
            ArrayAdapter<ImageParams.CompressionLevel> adapter_compression =
                    new ArrayAdapter<ImageParams.CompressionLevel>(mainActivity, R.layout.custom_spinner, items_compression);
            adapter_compression.setDropDownViewResource(R.layout.custom_spinner_item);
            dropdown_compresion.setAdapter(adapter_compression);

            // Add view mode text and dropdown
            LinearLayout container_1 = new LinearLayout(camera_list.getContext());
            container_1.setOrientation(LinearLayout.HORIZONTAL);
            container_1.addView(text_viewmode);
            container_1.addView(dropdown_viewmode);

            // Add compression text and dropdown
            LinearLayout container_2 = new LinearLayout(camera_list.getContext());
            container_2.setOrientation(LinearLayout.HORIZONTAL);
            container_2.addView(text_compression);
            container_2.addView(dropdown_compresion);

            // Add camera checkbox, compression layout
            LinearLayout container_3 = new LinearLayout(camera_list.getContext());
            container_3.setOrientation(LinearLayout.VERTICAL);
            container_3.addView(checkbox);
            container_3.addView(container_1);
            container_3.addView(container_2);

            // Add camera group to the entire layout
            camera_list.addView(container_3);
        }
    }

    /**
     * This function checks to see if any of the cameras's states have changed
     * This could be a enable/disable or a change in the quality or image type
     */
    public boolean hasCamerasChanged() {
        // Check to see if we have a new camera list
        if(old_camera_list.isEmpty() || old_camera_viewmode.isEmpty() || old_camera_compression.isEmpty())
            return true;
        // Loop through current ones
        for(int i=0; i<camera_list.getChildCount(); i++) {
            if(((CheckBox)((LinearLayout)camera_list.getChildAt(i)).getChildAt(0)).isChecked()!=old_camera_list.get(i))
                return true;
            if((ImageParams.ViewMode)((Spinner)((LinearLayout) ((LinearLayout) camera_list.getChildAt(i)).getChildAt(1)).getChildAt(1)).getSelectedItem()!=old_camera_viewmode.get(i))
                return true;
            if((ImageParams.CompressionLevel)((Spinner)((LinearLayout) ((LinearLayout) camera_list.getChildAt(i)).getChildAt(2)).getChildAt(1)).getSelectedItem()!=old_camera_compression.get(i))
                return true;
        }
        return false;
    }
}
