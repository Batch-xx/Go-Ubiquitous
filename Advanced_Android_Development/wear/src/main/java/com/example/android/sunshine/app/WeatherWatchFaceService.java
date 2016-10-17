package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class WeatherWatchFaceService extends CanvasWatchFaceService {
    private boolean mIsRegisteredReceiver = false;
    private static String TAG = "WeatherWatchFaceService";

    public WeatherWatchFaceService() {
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 0;

        //Offsets
        private int mTimeYOffset = 0;
        private int mDateYOffset = 0;
        private int mTempTOffset = 0;

        //Line Height
        private int mTimeLineHt = 0;
        private int mDateLineHt = 0;
        private int mTempLineHt = 0;

        //Paint
        Paint mBackgroundPaint = null;
        Paint mDataPaint = null;
        Paint mTimePaint = null;
        Paint mTempPaint = null;


        private Calendar mCalendar;
        private Date mDate= null;

        //device features
        private boolean mLowBitAmbient;

        //Graphic objects
        private Paint mHourPaint;
        private Paint mMinutePaint;

        //handler to update the timer once a second in the interactive mode
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        // add watch update code form interactive mode
                        break;
                }
            }

            @Override
            public void dispatchMessage(Message msg) {
                super.dispatchMessage(msg);
            }
        };

        // receiver to update the time zone
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };


        @Override
        public void onCreate(SurfaceHolder holder) {
            /*initialize your watch face */
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
            .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
            .setShowSystemUiTime(false)
            .build());

            Resources resources = WeatherWatchFaceService.this.getResources();
            mTimeYOffset = resources.getDimensionPixelOffset(R.dimen.time_y_offset);
            mDateYOffset = resources.getDimensionPixelOffset(R.dimen.date_y_offset);
            mTempTOffset = resources.getDimensionPixelOffset(R.dimen.temp_y_offset);

            mTimeLineHt = resources.getDimensionPixelOffset(R.dimen.time_line_ht);
            mDateLineHt = resources.getDimensionPixelOffset(R.dimen.date_line_ht);
            mTempLineHt = resources.getDimensionPixelOffset(R.dimen.time_line_ht);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Utility.BACKGROUND_COLOR_INTERACTIVE);
            mTimePaint = new Paint();
            mTimePaint.setColor(Utility.TIME_COLOR_INTERACTIVE);
            mDataPaint = new Paint();
            mDataPaint.setColor(Utility.DATE_COLOR_INTERACTIVE);
            mTimePaint = new Paint();
            mTimePaint.setColor(Utility.TEMP_COLOR_INTERACTIVE);

            mCalendar = Calendar.getInstance();
            mDate = new Date();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            /*get device features (burn-in, Low bit ambient */
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /*the time changed */
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /*the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            /*draw your watch face*/
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "Visibility changed to: " + visible);

            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());


            } else {
                unregisterReciever();
            }
        }

        private void registerReceiver() {
            if (mIsRegisteredReceiver) {
                return;
            }

            mIsRegisteredReceiver = true;

            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            WeatherWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReciever() {
            if (!mIsRegisteredReceiver) {
                return;
            }
            mIsRegisteredReceiver = false;
            WeatherWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }
        private void updateTimer(){
            Log.d(TAG, "Updating timer");

            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if(shouldTimerBeRunning()){
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }

        }

        private boolean shouldTimerBeRunning(){
            return isVisible() && !isInAmbientMode();
        }
    }
}
