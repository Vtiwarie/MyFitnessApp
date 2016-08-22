package com.mysampleapp;

import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;

/**
 * Created by Vishaan on 8/20/2016.
 */
public class DistanceTracker {
    private static final String LOG_TAG = DistanceTracker.class.getSimpleName();

    private static final String DISTANCE_TRACKER_DATESET_NAME = "distance_tracker";

    private static final String DISTANCE_WALKED_KEY_DISTANCE = "distance_walked";

    private static DistanceTracker mInstance;

    private static final int DEFAULT_DISTANCE_WALKED = 0;
    private int mDistanceWalked;

    private DistanceTracker() {
        mDistanceWalked = DEFAULT_DISTANCE_WALKED;
    }

    public int getDistanceWalked() {
        return mDistanceWalked;
    }

    public void setDistanceWalked(int mDistanceWalked) {
        this.mDistanceWalked = mDistanceWalked;
    }

    public static DistanceTracker getInstance() {
        if(mInstance == null) {
            mInstance = new DistanceTracker();
        } else {
            mInstance = new DistanceTracker();
            final IdentityManager identityManager = AWSMobileClient.defaultMobileClient()
                    .getIdentityManager();
            identityManager.addSignInStateChangeListener(
                    new IdentityManager.SignInStateChangeListener() {
                        @Override
                        public void onUserSignedIn() {
                            Log.d(LOG_TAG, "load from dataset on user sign in");
                            mInstance.loadFromDataset();
                        }

                        @Override
                        public void onUserSignedOut() {
                            Log.d(LOG_TAG, "wipe user data after sign out");
                            AWSMobileClient.defaultMobileClient().getSyncManager().wipeData();
                            mInstance.setDistanceWalked(DEFAULT_DISTANCE_WALKED);
//                            final Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
//                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }
                    });
        }
        return mInstance;
    }

    /**
     * Loads user settings from local dataset into memory.
     */
    public void loadFromDataset() {
        Dataset dataset = getDataset();
        final String dataTextColor = dataset.get(DISTANCE_WALKED_KEY_DISTANCE);
        if (dataTextColor != null) {
            mDistanceWalked = Integer.valueOf(dataTextColor);
        }
    }

    /**
     * Saves in memory user settings to local dataset.
     */
    public void saveToDataset() {
        Dataset dataset = getDataset();
        dataset.put(DISTANCE_WALKED_KEY_DISTANCE, String.valueOf(mDistanceWalked));
    }

    /**
     * Gets the Cognito dataset that stores user settings.
     *
     * @return Cognito dataset
     */
    public Dataset getDataset() {
        return AWSMobileClient.defaultMobileClient()
                .getSyncManager()
                .openOrCreateDataset(DISTANCE_TRACKER_DATESET_NAME);
    }

}
