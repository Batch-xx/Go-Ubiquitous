package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;

;

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
        private Calendar mCalendar;

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
            mCalendar = Calendar.getInstance();
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
                ;
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
    }
}
