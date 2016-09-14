package com.example.android.sunshine.wear;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WeatherWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WeatherWatchService";
    private static final long NORMAL_UPDATE_RATE_MS = 500;
    private static Typeface  NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static Typeface  BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine{
        private static final int MSG_UPDATE_TIME = 0;
        private boolean mAmbient = false;
        boolean mRegisteredReceiver = false;
        Paint mBackgroundPaint;
        Paint mDatePaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mAmPmPaint;
        Paint mColonPaint;
        float mColonWidth;

        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;


        final Handler mUpdateTimerHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case MSG_UPDATE_TIME:
                        if(Log.isLoggable(TAG, Log.VERBOSE)){
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if(shouldTimerBeRunning()){
                            long timeMs = System.currentTimeMillis();
                            long delayMs = NORMAL_UPDATE_RATE_MS -
                                    (timeMs % NORMAL_UPDATE_RATE_MS);
                            mUpdateTimerHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME,delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
                invalidate();
            }
        };


        @Override
        public void onCreate(SurfaceHolder holder) {
            if(Log.isLoggable(TAG,Log.DEBUG)){
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);
            Resources resources = WeatherWatchFaceService.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.backgroundColor, null));
            mDatePaint = createTextPaint(resources.getColor(R.color.digitalDateColor,null),NORMAL_TYPEFACE);
            mHourPaint = createTextPaint(resources.getColor(R.color.hourColor,null),BOLD_TYPEFACE);
            mMinutePaint = createTextPaint(resources.getColor(R.color.minuteColor,null),NORMAL_TYPEFACE);
            mSecondPaint = createTextPaint(resources.getColor(R.color.secondColor,null),NORMAL_TYPEFACE);
            mAmPmPaint = createTextPaint(resources.getColor(R.color.am_pmColor, null), NORMAL_TYPEFACE);
            mColonPaint = createTextPaint(resources.getColor(R.color.colonColor,null), NORMAL_TYPEFACE);

            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();
        }


        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface){
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        private  void initFormats(){
            mDayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(WeatherWatchFaceService.this);
            mDateFormat.setCalendar(mCalendar);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /*get device feature (burn-in, low-bit ambient)*/
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            mAmbient = inAmbientMode;
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            /* draw your watch face */
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
        }

        private boolean shouldTimerBeRunning(){
            return isVisible() && !mAmbient;
        }
    }
}