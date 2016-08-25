package com.mysampleapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.amazonaws.mobile.AWSMobileClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.mysampleapp.model.DistanceDS;
import com.mysampleapp.model.DistanceTrackerModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    //log tags
    private final static String LOG_TAG = MainActivity.class.getSimpleName() + 1;

    //title
    private final static String BUNDLE_KEY_TOOLBAR_TITLE = "My Fitness App";

    //toolbar
    private Toolbar mToolbar;

    //Google api client
    private GoogleApiClient mGoogleAPIClient;

    //store user's current location
    private Location mCurrentLocation;

    //location request object for google map API
    private LocationRequest mLocationRequest;

    //data source to connect with database for local storage
    private DistanceDS mDistanceDS;

    //list of tracker objects, each item representing the distance traveled and date
    private List<DistanceTrackerModel> mTrackers;

    //store the the current tracker
    private DistanceTrackerModel mCurrentTrack;

    //recycler view
    private RecyclerView mRecyclerView;

    //adapter for recycler view
    private MyAdapter mAdapter;

    //store the initial distance for the day, so we can track how many intervals of 1000 feet we have walked
    private double mInitialDistance = 0.0;

    //alarm to set off notifications
    private Alarm mAlarm;

    //counter to store the intervals of 1000 feet that have been traveled
    private int mIntervalCount = 0;

    //update location every X seconds
    private static final long UPDATE_INTERVAL = 1000;

    //the maximum amount of feet used to start the alarm, which notifies that the user should start walking
    private static final int MINIMUM_FEET_TO_TRIGGER_ALARM = 20;

    //broadcast receiver action to react to alarms and perform actions
    private static String BROADCAST_ACTION = "com.mysampleapp.ACTION_ALERT";

    //request codes for various operations
    private static int BROADCAST_REQUEST_CODE = 0;
    private static final int NOTIFICATION_ID = 0;
    private static final int REQUEST_LOCATION = 0;
    public static final int REQUEST_LOCATION_UPDATES = 1;

    //view holder for recycler view
    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView mDistance;
        private TextView mDate;

        public MyViewHolder(View itemView) {
            super(itemView);
            mDistance = (TextView) itemView.findViewById(R.id.txt_distance_traveled);
            mDate = (TextView) itemView.findViewById(R.id.txt_date);
        }

        public void bind(DistanceTrackerModel tracker) {
            mDistance.setText("Distance Traveled: " + NumberFormat.getInstance().format((long) tracker.getDistance()) + " feet");
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
            mDate.setText(df.format(tracker.getDate()));
        }
    }

    //adapter for recycler view
    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private List<DistanceTrackerModel> mTrackers;

        public MyAdapter(List<DistanceTrackerModel> trackers) {
            setTrackers(trackers);
        }

        public void setTrackers(List<DistanceTrackerModel> trackers) {
            mTrackers = trackers;
        }
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item_tracker, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.bind(mTrackers.get(position));
        }

        @Override
        public int getItemCount() {
            return mTrackers.size();
        }
    }

    /**
     * Class for managing the alarm to notify users to get up and start walking
     * if idle for an hour.
     */
    public class Alarm {
        private AlarmManager mAlarmManager;
        private PendingIntent mPendingIntent;

        //every hour, send notification if user is idle
        private static final int INTERVAL = 60 * 60 * 1000;

        public Alarm() {
            mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(MainActivity.this, MyAlarmReceiver.class);
            intent.setAction(BROADCAST_ACTION);
            mPendingIntent = PendingIntent.getBroadcast(MainActivity.this, BROADCAST_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private void startAlarm() {
            Log.d(LOG_TAG, "ALARM STARTED");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + INTERVAL, mPendingIntent);
        }

        public boolean isAlarmSet() {
            return (PendingIntent.getBroadcast(MainActivity.this, 0,
                    new Intent(BROADCAST_ACTION),
                    PendingIntent.FLAG_NO_CREATE) != null);
        }
    }

    /**
     * Broadcast receiver to receive alarm and start notification.
     * Triggers when the user is inactive for an hour.
     */
    public static class MyAlarmReceiver extends BroadcastReceiver {
        private Context mContext;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "ALARM BROADCAST RECEIVED");
            mContext = context;
            if(intent.getAction().equals(BROADCAST_ACTION))
            startNotification();
        }

        private void startNotification() {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext)
                            .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                            .setContentTitle("Idle")
                            .setContentText("You have been idle for an hour. Start walking!");

            Intent resultIntent = new Intent(mContext, MainActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
            ((NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Initializes the Toolbar for use with the activity.
     */
    private void setupToolbar(final Bundle savedInstanceState) {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        // Set up the activity to use this mToolbar. As a side effect this sets the Toolbar's title
        // to the activity's title.
        setSupportActionBar(mToolbar);

        // Restore the Toolbar's title.
        getSupportActionBar().setTitle(BUNDLE_KEY_TOOLBAR_TITLE);

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDistanceDS = DistanceDS.getInstance(getApplicationContext());
        mDistanceDS.open();

        if (mGoogleAPIClient == null) {
            mGoogleAPIClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleAPIClient.connect();

        setRequestLocation();

        enableLocation();

        setContentView(R.layout.activity_main2);

        setupToolbar(savedInstanceState);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAlarm = new Alarm();

        mCurrentTrack = mDistanceDS.getByDate(Helper.filterDate(new Date()));
        if (mCurrentTrack != null) {
            Log.d(LOG_TAG, "Today's distance tracker model: " + mCurrentTrack);
            mInitialDistance = mCurrentTrack.getDistance();
        } else {
            Log.d(LOG_TAG, "NO ENTRY FOUND FOR THIS DATE");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mDistanceDS.open();
        if( ! mGoogleAPIClient.isConnected()) {
            mGoogleAPIClient.connect();
        }

        updateUI();

        mCurrentTrack = mDistanceDS.getByDate(Helper.filterDate(new Date()));
        if (mCurrentTrack != null) {
            Log.d(LOG_TAG, "Today's distance tracker model: " + mCurrentTrack);
        } else {
            Log.d(LOG_TAG, "NO ENTRY FOUND FOR THIS DATE");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDistanceDS.close();
        if(mGoogleAPIClient.isConnected()) {
            mGoogleAPIClient.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle bundle) {
        super.onSaveInstanceState(bundle);
        // Save the title so it will be restored properly to match the view loaded when rotation
        // was changed or in case the activity was destroyed.
        if (mToolbar != null) {
            bundle.putCharSequence(BUNDLE_KEY_TOOLBAR_TITLE, mToolbar.getTitle());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_show:
                mDistanceDS.displayEntries(LOG_TAG);
                return true;
            case R.id.menu_item_refresh:
                updateUI();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //refresh the recycler view with fresh daily tracker data
    private void updateUI() {
        Log.d(LOG_TAG, "REFRESHING UI");
        mTrackers = mDistanceDS.getEntries();
        if(mTrackers.isEmpty()) {
            //add placeholder text if no data is not found
            findViewById(R.id.txt_no_data).setVisibility(View.VISIBLE);
            findViewById(R.id.recycler).setVisibility(View.INVISIBLE);
        } else {
            //remove placeholder text if data is found
            findViewById(R.id.txt_no_data).setVisibility(View.INVISIBLE);
            findViewById(R.id.recycler).setVisibility(View.VISIBLE);

            //update recycler view if data is found
            if (mAdapter == null) {
                mAdapter = new MyAdapter(mTrackers);
                mRecyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.setTrackers(mTrackers);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mGoogleAPIClient != null && mGoogleAPIClient.isConnected()) {
            requestLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "Could't connect to google api");
    }

    @Override
    public void onLocationChanged(Location location) {
        handleChangeInDistance(location);
        checkIntervalFor1000Feet();
        updateUI();
    }

    /**
     * handle the distance traveled after each location update, and
     * create database entry for it
     *
     * @param location
     */
    private void handleChangeInDistance(Location location) {
        double distance = 0;
        if (mCurrentLocation != null) {
            distance = convertMeters2Feet(mCurrentLocation.distanceTo(location));
        }
        Log.d(LOG_TAG, "CHANGE IN DISTANCE: " + distance);

        //if there is no data for today, create it
        if (mCurrentTrack == null) {
            mCurrentTrack = new DistanceTrackerModel(0, new Date(), 0.0);
        }
        mCurrentTrack.incrementDistance(distance);
        mDistanceDS.insert(mCurrentTrack);

        mCurrentLocation = location;

        //set an alarm to notify the user to get up and walk, if idle for too long
        if(distance < MINIMUM_FEET_TO_TRIGGER_ALARM) {
            if( ! mAlarm.isAlarmSet()) {
                mAlarm.startAlarm();
            }
        } else {
            //the user is actively walking. Cancel alarm
            mAlarm.mAlarmManager.cancel(mAlarm.mPendingIntent);
            Log.d(LOG_TAG, "ALARM CANCELED");
        }
    }

    /**
     * NOtify the user for every 1000 ft traveled
     */
    private void checkIntervalFor1000Feet() {
        if (((int) mCurrentTrack.getDistance() - (int)mInitialDistance) / 1000 > mIntervalCount) {
            startNotification1000ft();
            mIntervalCount++;
        }
    }

    /**
     * Start notification for 1000 feet traveled
     */
    private void startNotification1000ft() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                        .setContentTitle("Congratulations!!")
                        .setContentText("1000 more feet achieved!");

        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Set location request from Google map API
     */
    private void setRequestLocation() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(2*UPDATE_INTERVAL);
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
        } else {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        final PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleAPIClient, builder.build());
    }

    /**
     * Request location updates from Google maps API
     */
    private void requestLocationUpdates() {
        // Here, thisActivity is the current activity
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_UPDATES);
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient, mLocationRequest, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_LOCATION_UPDATES:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationUpdates();
                } else {
                    Log.d(LOG_TAG, "NO PERMISSIONS - REQUEST_LOCATION_UPDATES");
                }
                break;
            case REQUEST_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation();
                } else {
                    Log.d(LOG_TAG, "NO PERMISSIONS - REQUEST LOCATION");
                }
                break;
        }
    }

    /**
     * Enable location retrieval from Google maps API
     */
    private void enableLocation() {
        // Here, thisActivity is the current activity
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
        }
    }

    /**
     * Conver meters for feet
     *
     * @param meters
     * @return double
     */
    private double convertMeters2Feet(double meters) {
        return meters * 3.28084f;
    }
}
