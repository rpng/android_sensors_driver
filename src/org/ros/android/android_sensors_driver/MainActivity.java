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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivitySherlock;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;

///< @TODO REMOVE
import java.net.URI;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 */


public class MainActivity extends RosActivitySherlock
{  
  private NavSatFixPublisher fix_pub;
  private ImuPublisher imu_pub;
  private MagneticFieldPublisher magnetic_field_pub;
  private FluidPressurePublisher fluid_pressure_pub;
  private IlluminancePublisher illuminance_pub;
  private TemperaturePublisher temperature_pub;
  
  private LocationManager mLocationManager;
  private SensorManager mSensorManager;
  

  public MainActivity()
  {
	  super("Ros Android Sensors Driver", "Ros Android Sensors Driver");
  }
  
  @Override
  protected void onPause()
  {
	  super.onPause();
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
	  super.onCreate(savedInstanceState);
	  //addPreferencesFromResource(R.xml.preferences);
	  setContentView(R.layout.activity_main);
	  
	  mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
	  mSensorManager = (SensorManager)this.getSystemService(SENSOR_SERVICE);
  }
  
  @Override
  protected void onResume()
  {
		super.onResume();
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor)
  { 
    URI masterURI = getMasterUri();
    //masterURI = URI.create("http://192.168.15.247:11311/");
    //masterURI = URI.create("http://10.0.1.157:11311/");
    
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
    nodeConfiguration.setMasterUri(masterURI);
    nodeConfiguration.setNodeName("android_sensors_driver_magnetic_field");
    this.magnetic_field_pub = new MagneticFieldPublisher(mSensorManager);
    nodeMainExecutor.execute(this.magnetic_field_pub, nodeConfiguration);

    NodeConfiguration nodeConfiguration2 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
    nodeConfiguration2.setMasterUri(masterURI);
    nodeConfiguration2.setNodeName("android_sensors_driver_nav_sat_fix");
    this.fix_pub = new NavSatFixPublisher(mLocationManager);
    nodeMainExecutor.execute(this.fix_pub, nodeConfiguration2);
	  
    NodeConfiguration nodeConfiguration3 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
    nodeConfiguration3.setMasterUri(masterURI);
    nodeConfiguration3.setNodeName("android_sensors_driver_imu");
    this.imu_pub = new ImuPublisher(mSensorManager);
    nodeMainExecutor.execute(this.imu_pub, nodeConfiguration3);

    NodeConfiguration nodeConfiguration4 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
    nodeConfiguration4.setMasterUri(masterURI);
    nodeConfiguration4.setNodeName("android_sensors_driver_pressure");
    this.fluid_pressure_pub = new FluidPressurePublisher(mSensorManager);
    nodeMainExecutor.execute(this.fluid_pressure_pub, nodeConfiguration4);

    NodeConfiguration nodeConfiguration5 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
    nodeConfiguration5.setMasterUri(masterURI);
    nodeConfiguration5.setNodeName("android_sensors_driver_illuminance");
    this.illuminance_pub = new IlluminancePublisher(mSensorManager);
    nodeMainExecutor.execute(this.illuminance_pub, nodeConfiguration5);

    NodeConfiguration nodeConfiguration6 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
    nodeConfiguration6.setMasterUri(masterURI);
    nodeConfiguration6.setNodeName("android_sensors_driver_temperature");
    this.temperature_pub = new TemperaturePublisher(mSensorManager);
    nodeMainExecutor.execute(this.temperature_pub, nodeConfiguration6);
    
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {	  
      MenuInflater inflater = getSupportMenuInflater();
      inflater.inflate(R.menu.activity_main, menu);
      return super.onCreateOptionsMenu(menu);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
          case R.id.menu_help:
        	  AlertDialog.Builder builder = new AlertDialog.Builder(this);
              builder.setCancelable(true);
              builder.setTitle("About Android Sensors Driver");
              builder.setMessage("Android Sensors Driver is an open source project!  For more information," +
              		" go to the 'Wiki'.  To report bugs or request features click 'Report'.");
              builder.setInverseBackgroundForced(true);
              builder.setNegativeButton("OK",
                      new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                              dialog.dismiss();
                          }
                      });
              builder.setNeutralButton("Wiki",
                      new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                    	      Intent i = new Intent(Intent.ACTION_VIEW);
                    	      Uri u = Uri.parse("http://www.ros.org/wiki/android_sensors_driver");
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
              builder.setPositiveButton("Report",
                      new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                    	      Intent i = new Intent(Intent.ACTION_VIEW);
                    	      Uri u = Uri.parse("https://github.com/ros-android/android_sensors_driver/issues/new");
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
              break; 
      }
      return super.onOptionsItemSelected(item);
  }
}
