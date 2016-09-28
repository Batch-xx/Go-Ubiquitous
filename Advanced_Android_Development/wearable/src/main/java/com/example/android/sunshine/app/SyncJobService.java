package com.example.android.sunshine.app;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by BKBachelor on 9/28/2016.
 */

public class SyncJobService extends JobService {
    public static int SYNC_TEMP_ID = 1;
    private static String SYNC_TEMP_REQUEST = "/sync_temp_request";
    private GoogleApiClient mGoogleClientAPI;
    private String TAG = "SyncJobService";

    @Override
    public void onCreate() {
        mGoogleClientAPI = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d(TAG, "Google Client API CONNECTED");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Google Client API SUSPENDED");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.e(TAG, "Google Client API FAILED");
                    }
                })
                .addApi(Wearable.API)
                .build();
        super.onCreate();
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG,"SyncJobService has started...");
        Log.d(TAG, "Job ID: " + jobParameters.getJobId() + " STARTED");

        int job_id = jobParameters.getJobId();
        switch (job_id){
            case 1:
                requestTemperature();
                break;
            default:
                Log.e(TAG, "Invalid job ID received....");
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG,"SyncJobService has stopped...");
        Log.d(TAG, "Job ID: " + jobParameters.getJobId() + " STOPPED");
        return false;
    }



    private void requestTemperature(){
        Log.d(TAG, "Requesting Temperature....");
        Wearable.MessageApi.sendMessage(mGoogleClientAPI, String.valueOf(SYNC_TEMP_ID),
                SYNC_TEMP_REQUEST,null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if(!sendMessageResult.getStatus().isSuccess()){
                    Log.e(TAG, "FAILED to send message ID: " + SYNC_TEMP_ID);
                }
            }
        });
    }

}
