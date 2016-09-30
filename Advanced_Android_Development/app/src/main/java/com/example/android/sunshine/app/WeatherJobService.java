package com.example.android.sunshine.app;

import android.app.Activity;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class WeatherJobService extends JobService {


    public static final int UPDATE_TEMP_JOB_ID = 1;

    private final String TAG = "WeatherJobSvc";
    private Activity mMainActivity;

    public WeatherJobService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "WeatherJobService STARTED");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"WeatherJobService DESTROYED");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Messenger callback  = intent.getParcelableExtra("messenger");
        Message msg = Message.obtain();
        msg.what = MainActivity.MSG_SERVICE_OBJ;
        msg.obj = this;

        try {
            callback.send(msg);
        }catch (RemoteException e){
            Log.e(TAG, "Error in passing service object back to activity");
        }
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job STARTED: " + jobParameters.getJobId());
        int job_id = jobParameters.getJobId();
        switch(job_id){
            case UPDATE_TEMP_JOB_ID:
                updateTemperatureJob();
                break;
            default:
                Log.e(TAG, "Invalid job ID");
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job STOPPED: " + jobParameters.getJobId());
        return false;
    }

    public void setUiCallback(MainActivity activity){
        mMainActivity = activity;
    }

    public void scheduleJob(JobInfo job_info){
        Log.d(TAG,"Scheduling Job: " + job_info.getId());
        JobScheduler tm = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.schedule(job_info);
    }

    //Jobs //
    private void updateTemperatureJob(){
        
    }
}
