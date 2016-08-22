package com.mysampleapp.demo.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@DynamoDBTable(tableName = "myfitnessapp-mobilehub-852076933-Locations")

public class LocationsDO {
    private String _userId;
    private Double _date;
    private Double _distance;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBRangeKey(attributeName = "date")
    @DynamoDBIndexHashKey(attributeName = "date", globalSecondaryIndexName = "date_index")
    public Double getDate() {
        return _date;
    }

    public void setDate(final Double _date) {
        this._date = _date;
    }
    @DynamoDBAttribute(attributeName = "distance")
    public Double getDistance() {
        return _distance;
    }

    public void setDistance(final Double _distance) {
        this._distance = _distance;
    }

}
