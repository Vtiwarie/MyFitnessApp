package com.mysampleapp.model;

import com.mysampleapp.Helper;

import java.util.Date;

/**
 * Created by Vishaan on 8/22/2016.
 *
 * Data Model for storing the distance traveled
 * and date.
 */
public class DistanceTrackerModel {
    private long id;
    private Date mDate;
    private double mDistance;

    public DistanceTrackerModel(long id, Date date, double distance) {
        setId(id);
        setDate(date);
        setDistance(distance);
    }

    @Override
    public String toString() {
        return new String("Id: " + getId() + "\nDate: " + getDate() + "\nDistance: " + getDistance());
    }

    /**
     * Increment the distance traveled by X
     *
     * @param distance
     */
    public void incrementDistance(double distance) {
        setDistance(getDistance() + distance);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        this.mDate = Helper.filterDate(date);
    }

    public double getDistance() {
        return mDistance;
    }

    public void setDistance(double mDistance) {
        this.mDistance = mDistance;
    }
}
