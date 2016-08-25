package com.mysampleapp.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Vishaan on 8/22/2016.
 *
 * Data Source - Used to perform
 * database operations on behalf of
 * the Data Model
 */
public class DistanceDS {
    private static final String LOG_TAG = DistanceDS.class.getSimpleName();

    private static DistanceDS mInstance;
    private DBHelper mDHHelper;
    private SQLiteDatabase db;

    public static DistanceDS getInstance(Context context) {
        if (mInstance == null) {
            synchronized (DistanceDS.class) {
                mInstance = new DistanceDS(context);
            }
        }
        return mInstance;
    }

    private DistanceDS(Context context) {
        mDHHelper = new DBHelper(context);
    }

    public void open() {
        db = mDHHelper.getWritableDatabase();
    }

    public boolean isOpen() {
        return mDHHelper.getWritableDatabase().isOpen();
    }

    public void close() {
        if (isOpen()) {
            mDHHelper.getWritableDatabase().close();
        }
    }

    public long insert(DistanceTrackerModel distanceTrackerModel) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.Contract.DATE, distanceTrackerModel.getDate().getTime());
        values.put(DBHelper.Contract.DISTANCE, distanceTrackerModel.getDistance());
        long id = db.insertOrThrow(DBHelper.Contract.TABLE_NAME, null, values);
        return id;
    }

    public DistanceTrackerModel getByDate(Date date) {
        Cursor cursor = db.query(
                DBHelper.Contract.TABLE_NAME,
                null,
                DBHelper.Contract.DATE + " = ? ",
                new String[]{String.valueOf(date.getTime())},
                null,
                null,
                null
        );

        if (cursor == null || cursor.getCount() < 1) {
            return null;
        }

        cursor.moveToFirst();
        DistanceTrackerModel distanceTrackerModel = new DistanceTrackerModel(
                cursor.getLong(cursor.getColumnIndex(DBHelper.Contract.ID)),
                new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.Contract.DATE))),
                cursor.getDouble(cursor.getColumnIndex(DBHelper.Contract.DISTANCE)));
        return distanceTrackerModel;
    }

    public List<DistanceTrackerModel> getEntries() {
        Cursor cursor = db.query(DBHelper.Contract.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);

        if (cursor == null || cursor.getCount() < 1) {
            return new ArrayList<>();
        }

        List<DistanceTrackerModel> trackers = new ArrayList<>();
        try {
            cursor.moveToFirst();
            do {
                DistanceTrackerModel distanceTrackerModel = new DistanceTrackerModel(
                        cursor.getLong(cursor.getColumnIndex(DBHelper.Contract.ID)),
                        new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.Contract.DATE))),
                        cursor.getDouble(cursor.getColumnIndex(DBHelper.Contract.DISTANCE)));
                trackers.add(distanceTrackerModel);
            } while(cursor.moveToNext());
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }

        return trackers;
    }

    public void displayEntries(String tag) {
        List<DistanceTrackerModel> list = getEntries();
        Log.d(tag, "Total: " + getEntries().size());
        for(DistanceTrackerModel entry : list) {
            Log.d(tag, "ENTRY:\n " + entry);
        }
    }
}
