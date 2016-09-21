package com.example.android.sunshine.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WeatherWatchService";
    private static final long NORMAL_UPDATE_RATE_MS = 500;
    private static Typeface  NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static Typeface  BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener
            , GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
        private static final int MSG_UPDATE_TIME = 0;
        static final String COLON_STRING = ":";
        private boolean mIsAmbientMode = false;
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

        float mXOffest, mYOffset;


        boolean mShouldDrawColons;

        int mInteractiveBackgroundColor;
        int mAmbientBackgroundColor;
        int mInteractiveHourColor;
        int mAmbientHourColor;
        int mInteractiveMinuteColor;
        int mAmbientMinuteColor;
        int mInteractiveAmPmColor;
        int mAmbientAmPmColor;
        int mInteractiveColonColor;
        int mAmbientColonColor;
        boolean mLowBitAmbient;

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

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(
                WeatherWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
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

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
            .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
            .setShowSystemUiTime(false)
            .build());

            Resources resources = WeatherWatchFaceService.this.getResources();

            mYOffset = resources.getDimension(R.dimen.y_offset_round);

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

            mInteractiveBackgroundColor =  resources.getColor(R.color.backgroundColor,null);
            mAmbientBackgroundColor = resources.getColor(R.color.ambientBackgroundColor,null);
            mInteractiveHourColor = resources.getColor(R.color.hourColor, null);
            mAmbientHourColor =  resources.getColor(R.color.ambientHourColor,null);
            mInteractiveMinuteColor = resources.getColor(R.color.minuteColor,null);
            mAmbientMinuteColor = resources.getColor(R.color.ambientMinuteColor,null);
            mInteractiveAmPmColor = resources.getColor(R.color.am_pmColor,null);
            mAmbientMinuteColor = resources.getColor(R.color.ambientAmPmColor,null);
            mInteractiveColonColor = resources.getColor(R.color.colonColor,null);
            mAmbientColonColor = resources.getColor(R.color.ambientColonColor,null);




            initFormats();
        }


        @Override
        public void onDestroy() {
            mUpdateTimerHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface){
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if(Log.isLoggable(TAG, Log.DEBUG)){
                Log.d(TAG, "onVisiblityChanged:" + visible);
            }
            super.onVisibilityChanged(visible);

            if(visible) {
                mGoogleApiClient.connect();
                registerReciever();

                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
            }else{
                unregisterReciever();

                if(mGoogleApiClient != null && mGoogleApiClient.isConnected()){
                    Wearable.DataApi.removeListener(mGoogleApiClient,this);
                    mGoogleApiClient.disconnect();
                }
            }
            updateTimer();
        }
        private  void initFormats(){
            mDayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(WeatherWatchFaceService.this);
            mDateFormat.setCalendar(mCalendar);
        }

        private void registerReciever(){
            if(mRegisteredReceiver){
                return;
            }
            mRegisteredReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            WeatherWatchFaceService.this.registerReceiver(mReceiver, filter);
        }

        private void unregisterReciever(){
            if(!mRegisteredReceiver){
                return;
            }
            mRegisteredReceiver = false;
            WeatherWatchFaceService.this.unregisterReceiver(mReceiver);
        }

        private void updateTimer(){
            if(Log.isLoggable(TAG, Log.DEBUG)){
                Log.d(TAG,"updateTimer");
            }
            mUpdateTimerHandler.removeMessages(MSG_UPDATE_TIME);
            if(shouldTimerBeRunning()){
                mUpdateTimerHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            mIsAmbientMode = inAmbientMode;
            adjustPaintToColorMode(mBackgroundPaint, mInteractiveBackgroundColor, mAmbientBackgroundColor);
            adjustPaintToColorMode(mHourPaint, mInteractiveHourColor, mAmbientHourColor);
            adjustPaintToColorMode(mMinutePaint, mInteractiveMinuteColor, mAmbientMinuteColor);
            adjustPaintToColorMode(mAmPmPaint, mInteractiveAmPmColor, mAmbientAmPmColor);
            adjustPaintToColorMode(mColonPaint, mInteractiveColonColor, mAmbientColonColor);

            if(mLowBitAmbient){
                boolean antiAlias = !inAmbientMode;
                mDatePaint.setAntiAlias(antiAlias);
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mAmPmPaint.setAntiAlias(antiAlias);
                mColonPaint.setAntiAlias(antiAlias);
            }
            invalidate();
            updateTimer();
        }



        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            Resources resources =  WeatherWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffest = resources.getDimension(isRound ? R.dimen.x_offest_round : R.dimen.x_offest_square);
            float textSize = resources.getDimension(isRound ? R.dimen.text_size_round : R.dimen.text_size_square);
            float amPmTextSize = resources.getDimension(isRound ? R.dimen.am_pm_text_round : R.dimen.am_pm_text_sqaure);

            mDatePaint.setTextSize(resources.getDimension(R.dimen.date_text_size));
            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mSecondPaint.setTextSize(textSize);
            mAmPmPaint.setTextSize(textSize);
            mColonPaint.setTextSize(textSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            boolean is24Hour = DateFormat.is24HourFormat(WeatherWatchFaceService.this);

            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            canvas.drawRect(0,0,bounds.width(), bounds.height(), mBackgroundPaint);

            float x = mXOffest;
            String hourString;
            if(is24Hour){
                hourString = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
            }else{
                int hour = mCalendar.get(Calendar.HOUR);
                if(hour == 0){
                    hour = 12;
                }
                hourString = String.valueOf(hour);
            }
            canvas.drawText(hourString,x,mYOffset, mHourPaint);
        }



        private boolean shouldTimerBeRunning(){
            return isVisible() && !mIsAmbientMode;
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {

        }

        private void adjustPaintToColorMode(Paint paint, int interactiveColor, int ambientColor){
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }
    }
}