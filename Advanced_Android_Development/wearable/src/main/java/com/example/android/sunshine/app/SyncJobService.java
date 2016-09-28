package com.example.android.sunshine.app;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

/**
 * Created by BKBachelor on 9/28/2016.
 */

public class SyncJobService extends JobService {
    private String TAG = "SyncJobService";
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG,"SyncJobService has started...");
        Log.d(TAG, "Job ID: " + jobParameters.getJobId() + " STARTED");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG,"SyncJobService has stopped...");
        Log.d(TAG, "Job ID: " + jobParameters.getJobId() + " STOPPED");
        return false;
    }
}
