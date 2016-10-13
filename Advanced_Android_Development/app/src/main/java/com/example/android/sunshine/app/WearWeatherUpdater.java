package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by BKBatchelor on 10/12/2016.
 */

public class WearWeatherUpdater implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String WEATHER_MOBILE_PATH = "/weather_mobile";
    private Context context;
    private GoogleApiClient mGoogleApiClient;
    private String TAG = "WearWeatherService";

    public WearWeatherUpdater(Context context){
        this.context = context;

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    public  void updateWearable() {
        mGoogleApiClient.blockingConnect();
        final int INDEX_WEATHER_ID = 0;
        final int INDEX_MAX_TEMP = 1;
        final int INDEX_MIN_TEMP = 2;
        final int INDEX_SHORT_DESC = 3;

        final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
        };


        String locationQuery = Utility.getPreferredLocation(context);
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                locationQuery, System.currentTimeMillis());

        Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);
            String desc = cursor.getString(INDEX_SHORT_DESC);
            String artUrl = Utility.getArtUrlForWeatherCondition(context, weatherId);
            cursor.close();

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(WEATHER_MOBILE_PATH);
            putDataMapReq.getDataMap().putDouble("HIGH", high);
            putDataMapReq.getDataMap().putDouble("LOW", low);
            putDataMapReq.getDataMap().putString("DESC", desc);
            putDataMapReq.setUrgent();

            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapReq.asPutDataRequest()).await();
        }
    }




    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google API Client is CONNECTED");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google API Client is SUSPENDED");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Google API Client is FAILED");
    }
}
