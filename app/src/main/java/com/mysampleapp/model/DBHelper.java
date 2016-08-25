package com.mysampleapp.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Vishaan on 8/22/2016.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final class Contract {
        public static final String TABLE_NAME = "DistanceTracker";

        public static final String ID = "id";
        public static final String DISTANCE = "distance";
        public static final String DATE = "date";
    }

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "db_distance_tracker";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String sql = "CREATE TABLE IF NOT EXISTS " + Contract.TABLE_NAME + " (" +
                Contract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Contract.DATE + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                Contract.DISTANCE + " INTEGER NOT NULL " +
            ");";

        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Contract.TABLE_NAME);
        onCreate(db);
    }
}
