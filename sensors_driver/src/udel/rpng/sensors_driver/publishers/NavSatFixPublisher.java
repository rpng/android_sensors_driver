package udel.rpng.sensors_driver.publishers;

import android.content.IntentSender;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import sensor_msgs.NavSatFix;
import sensor_msgs.NavSatStatus;
import udel.rpng.sensors_driver.MainActivity;
import udel.rpng.sensors_driver.R;

public class NavSatFixPublisher implements NodeMain {

    // "Constant used in the location settings dialog."
    // Not sure why this is needed... -pgeneva
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Fused location provider
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;

    // My current location
    private Location mCurrentLocation;

    // View objects and the main activity
    private TextView tvLocation;
    private String mLastUpdateTime;
    private String robotName;
    private String TAG = "NavSatFixPublisher";
    private MainActivity mainAct;

    // Our ROS publish node
    private Publisher<NavSatFix> publisher;


    public NavSatFixPublisher(MainActivity mainAct, String robotName) {
        // Get our textzone
        this.mainAct = mainAct;
        this.robotName = robotName;
        tvLocation = (TextView) mainAct.findViewById(R.id.titleTextGPS);
        // Set our clients
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mainAct);
        mSettingsClient = LocationServices.getSettingsClient(mainAct);
        // Create a location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                publishMessages(locationResult.getLocations());
                updateUI();
            }
        };
        // Build the location request
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Build the location settings request object
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void updateUI() {
        // Return if we do not have a position
        if (mCurrentLocation == null)
            return;
        // Else we are good to display
        String lat = String.valueOf(mCurrentLocation.getLatitude());
        String lng = String.valueOf(mCurrentLocation.getLongitude());
        tvLocation.setText("At Time: " + mLastUpdateTime + "\n" +
                "Latitude: " + lat + "\n" +
                "Longitude: " + lng + "\n" +
                "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
                "Provider: " + mCurrentLocation.getProvider());
        Log.i(TAG, tvLocation.getText().toString().replace("\n", " | "));
    }

    private void publishMessages(List<Location> locs) {
        // Check that we have a location
        if (locs == null || locs.size() < 1)
            return;
        // We are good, lets publish
        for (Location location : locs) {
            NavSatFix fix = this.publisher.newMessage();
            fix.getHeader().setStamp(new Time(location.getTime()));
            fix.getHeader().setFrameId("gps");
            fix.getStatus().setStatus(NavSatStatus.STATUS_FIX);
            fix.getStatus().setService(NavSatStatus.SERVICE_GPS);
            fix.setLatitude(mCurrentLocation.getLatitude());
            fix.setLongitude(mCurrentLocation.getLongitude());
            fix.setAltitude(mCurrentLocation.getAltitude());
            fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
            double covariance = mCurrentLocation.getAccuracy()*mCurrentLocation.getAccuracy();
            double[] tmpCov = {covariance, 0, 0, 0, covariance, 0, 0, 0, covariance};
            fix.setPositionCovariance(tmpCov);
            publisher.publish(fix);
        }
        // Debug
        Log.i(TAG, "published = "+locs.size());
    }


    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("sensors_driver/navsatfix_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Start location updates
        startLocationUpdates();
        // Create our publisher
        try {
            this.publisher = connectedNode.newPublisher("/android/" + robotName + "/fix", "sensor_msgs/NavSatFix");
        } catch (Exception e) {
            if (connectedNode != null) {
                connectedNode.getLog().fatal(e);
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onShutdown(Node node) {
        stopLocationUpdates();
    }

    @Override
    public void onShutdownComplete(Node node) {}

    @Override
    public void onError(Node node, Throwable throwable) {}

    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================


    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(mainAct, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");
                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        updateUI();
                    }
                })
                .addOnFailureListener(mainAct, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(mainAct, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(mainAct, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateUI();
                    }
                });
    }


    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

}