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
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
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

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener{
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
        Paint mMonthPaint;
        Paint mDayPaint;
        Paint mDayofMonthPaint;
        Paint mSeparatePaint;
        Paint mYearPaint;
        Paint mLowTempPaint;
        Paint mHighTempPaint;

        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;

        float   mYTimeOffset,
                mYDateOffset,
                mYSeparateOffset,
                mYTempertureOffsset;

        String mLowString, mHighString;


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
        int mInteractiveDateColor;
        int mAmbientDateColor;
        int mInteractiveSeparateColor;
        int mAmbientSeparateColor;
        int mInteractiveLowTempColor;
        int mAmbientLowTempColor;
        int mInteractiveHighTempColor;
        int mAmbientHighTempColor;
        boolean mLowBitAmbient;

        GoogleApiClient  mGoogleClientApi;

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

            mGoogleClientApi = new GoogleApiClient.Builder(WeatherWatchFaceService.this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            Log.d(TAG,"Google Client Api is CONNECTED");
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            Log.d(TAG, "Google Client APi is SUSPENDED");
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            Log.e(TAG, "Google Client Api FAILED");
                        }
                    })
                    .addApiIfAvailable(Wearable.API)
                    .build();

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
            .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
            .setShowSystemUiTime(false)
            .build());

            Resources resources = WeatherWatchFaceService.this.getResources();

            mYTimeOffset = resources.getDimension(R.dimen.y_time_offset_round);
            mYDateOffset = resources.getDimension(R.dimen.y_date_offset_round);
            mYSeparateOffset = resources.getDimensionPixelOffset(R.dimen.y_separate_offset);
            mYTempertureOffsset = resources.getDimensionPixelOffset(R.dimen.y_temp_offset);


            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.backgroundColor, null));
            mDatePaint = createTextPaint(resources.getColor(R.color.digitalDateColor,null),NORMAL_TYPEFACE);
            mHourPaint = createTextPaint(resources.getColor(R.color.hourColor,null),NORMAL_TYPEFACE);
            mMinutePaint = createTextPaint(resources.getColor(R.color.minuteColor,null),NORMAL_TYPEFACE);
            mSecondPaint = createTextPaint(resources.getColor(R.color.secondColor,null),NORMAL_TYPEFACE);
            mAmPmPaint = createTextPaint(resources.getColor(R.color.am_pmColor, null), NORMAL_TYPEFACE);
            mColonPaint = createTextPaint(resources.getColor(R.color.colonColor,null), NORMAL_TYPEFACE);
            mMonthPaint = createTextPaint(resources.getColor(R.color.dateColor,null), NORMAL_TYPEFACE);
            mDayPaint = createTextPaint(resources.getColor(R.color.dateColor,null),NORMAL_TYPEFACE);
            mYearPaint = createTextPaint(resources.getColor(R.color.dateColor,null),NORMAL_TYPEFACE);
            mDayofMonthPaint = createTextPaint(resources.getColor(R.color.dateColor,null),NORMAL_TYPEFACE);

            mSeparatePaint = new Paint();
            mSeparatePaint.setColor(resources.getColor(R.color.separateColor, null));

            mLowTempPaint  = createTextPaint(resources.getColor(R.color.dateColor,null),NORMAL_TYPEFACE);
            mHighTempPaint =  createTextPaint(resources.getColor(R.color.dateColor,null),NORMAL_TYPEFACE);


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
            mInteractiveDateColor = resources.getColor(R.color.dateColor, null);
            mAmbientDateColor = resources.getColor(R.color.ambientDateColor,null);
            mInteractiveSeparateColor = resources.getColor(R.color.separateColor,null);
            mAmbientSeparateColor = resources.getColor(R.color.ambientSeparateColor,null);
            mInteractiveLowTempColor = resources.getColor(R.color.temperatureColor,null);
            mAmbientLowTempColor = resources.getColor(R.color.ambientTemperatureColor,null);
            mInteractiveHighTempColor = resources.getColor(R.color.temperatureColor,null);
            mAmbientHighTempColor = resources.getColor(R.color.ambientTemperatureColor,null);

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
                mGoogleClientApi.connect();
                registerReciever();

                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
            }else{
                unregisterReciever();

                if(mGoogleClientApi != null && mGoogleClientApi.isConnected()){
                    Wearable.DataApi.removeListener(mGoogleClientApi,this);
                    mGoogleClientApi.disconnect();
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
            mMinutePaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mMonthPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mDayPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mYearPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mDayofMonthPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mLowTempPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mHighTempPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
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
            adjustPaintToColorMode(mDatePaint, mInteractiveDateColor, mAmbientDateColor);
            adjustPaintToColorMode(mMonthPaint, mInteractiveDateColor, mAmbientDateColor);
            adjustPaintToColorMode(mDayPaint, mInteractiveDateColor, mAmbientDateColor);
            adjustPaintToColorMode(mMonthPaint, mInteractiveDateColor, mAmbientDateColor);
            adjustPaintToColorMode(mDayofMonthPaint,mInteractiveDateColor, mAmbientDateColor);
            adjustPaintToColorMode(mSeparatePaint, mInteractiveSeparateColor, mAmbientSeparateColor);
            adjustPaintToColorMode(mLowTempPaint, mInteractiveLowTempColor, mAmbientLowTempColor);
            adjustPaintToColorMode(mHighTempPaint, mInteractiveHighTempColor, mAmbientHighTempColor);

            if(mLowBitAmbient){
                boolean antiAlias = !inAmbientMode;
                mDatePaint.setAntiAlias(antiAlias);
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mAmPmPaint.setAntiAlias(antiAlias);
                mColonPaint.setAntiAlias(antiAlias);
                mMonthPaint.setAntiAlias(antiAlias);
                mDayPaint.setAntiAlias(antiAlias);
                mYearPaint.setAntiAlias(antiAlias);
                mDayofMonthPaint.setAntiAlias(antiAlias);
                mSeparatePaint.setAntiAlias(antiAlias);
                mLowTempPaint.setAntiAlias(antiAlias);
                mHighTempPaint.setAntiAlias(antiAlias);
            }
            invalidate();
            updateTimer();
        }

        private void adjustPaintToColorMode(Paint paint, int interactiveColor, int ambientColor){
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);

            mHourPaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mMinutePaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mMonthPaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mDayPaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mYearPaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mDayofMonthPaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mHighTempPaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
            mLowTempPaint.setTypeface(isInAmbientMode() ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
        }



        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            Resources resources =  WeatherWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
//            mXOffest = resources.getDimension(isRound ? R.dimen.x_offest_round : R.dimen.x_offest_square);
            float timeTextSize = resources.getDimension(isRound ? R.dimen.time_text_size_round : R.dimen.time_text_size_square);
            float dateTextSize = resources.getDimension(isRound ? R.dimen.date_text_size_round : R.dimen.date_text_size_square);
            float amPmTextSize = resources.getDimension(isRound ? R.dimen.am_pm_text_round : R.dimen.am_pm_text_sqaure);

            mDatePaint.setTextSize(resources.getDimension(R.dimen.date_text_size_round));
            mHourPaint.setTextSize(timeTextSize);
            mMinutePaint.setTextSize(timeTextSize);
            mSecondPaint.setTextSize(timeTextSize);
            mAmPmPaint.setTextSize(timeTextSize);
            mColonPaint.setTextSize(timeTextSize);
            mMonthPaint.setTextSize(dateTextSize);
            mDayPaint.setTextSize(dateTextSize);
            mYearPaint.setTextSize(dateTextSize);
            mDayofMonthPaint.setTextSize(dateTextSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            float x_time_center = bounds.width()/2 - 10;
            float x_date_center = bounds.width()/2 + 5;
            float y_colon_offset  = mYTimeOffset - 8;
            boolean is24Hour = DateFormat.is24HourFormat(WeatherWatchFaceService.this);

            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);

            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            //Background
            canvas.drawRect(0,0,bounds.width(), bounds.height(), mBackgroundPaint);

            //Hours
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
            float hourXOffset  = x_time_center -  mHourPaint.measureText(hourString);
            canvas.drawText(hourString,hourXOffset, mYTimeOffset, mHourPaint);

            //colon
            if(isInAmbientMode()  || mShouldDrawColons){
                canvas.drawText(COLON_STRING,x_time_center,y_colon_offset,mColonPaint);
            }

            //Minutes
            String minuteString = formatTwoDigitNumber(mCalendar.get(Calendar.MINUTE));
            float minuteXOffset = x_time_center + mColonPaint.measureText(COLON_STRING);
            canvas.drawText(minuteString, minuteXOffset, mYTimeOffset,mMinutePaint);

            //Month
            String monthString = formatMonth(mCalendar.get(Calendar.MONTH));
            float dateXOffset = x_date_center - mMonthPaint.measureText(monthString);
            canvas.drawText(monthString,dateXOffset,mYDateOffset,mMonthPaint);

            //Day
            String dayString = formatDay(mCalendar.get(Calendar.DAY_OF_WEEK));
            float dayOffset = x_date_center - mMonthPaint.measureText(monthString) -  mDayPaint.measureText(dayString);
            canvas.drawText(dayString,dayOffset,mYDateOffset,mDayPaint);

            //Day of month
            String dayMonthString = String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH));
            float dayMonthOffset = x_date_center;
            canvas.drawText(dayMonthString,dayMonthOffset,mYDateOffset,mDayofMonthPaint);

            //Year
            String yearString = String.valueOf(mCalendar.get(Calendar.YEAR));
            float yearOffset = x_date_center + mDayofMonthPaint.measureText(dayMonthString) + 10;
            canvas.drawText(yearString,yearOffset,mYDateOffset,mYearPaint);

            //Separator
            float start_x =  bounds.width()/2 - 30;
            float end_x =  bounds.width()/2 + 30;
            canvas.drawLine(start_x, mYSeparateOffset, end_x,mYSeparateOffset,mSeparatePaint);

            //low temperature
            mLowString =  mLowString == null ?  "--" : mLowString;
            float lowTempOffset = x_date_center -  mLowTempPaint.measureText(mLowString) - 20;
            canvas.drawText(mLowString, lowTempOffset,mYTempertureOffsset,mLowTempPaint);

            //high temperature
            mHighString =  mHighString == null ?  "--" : mHighString;
            float highTempOffset = x_date_center +  mHighTempPaint.measureText(mHighString) + 20;
            canvas.drawText(mHighString, highTempOffset,mYTempertureOffsset,mHighTempPaint);
        }

        private boolean shouldTimerBeRunning(){
            return isVisible() && !mIsAmbientMode;
        }

//        @Override
//        public void onConnected(@Nullable Bundle bundle) {
//            Log.d(TAG,"*******Google client is CONNECTED***********");
//           Wearable.DataApi.addListener(mGoogleApiClient,this);
//        }
//
//        @Override
//        public void onConnectionSuspended(int i) {
//            Log.e(TAG, "**********GoogleApiClient connection was SUSPENDED************");
//        }

//        @Override
//        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//            Log.e(TAG, "*************GoogleApiClient connection FAILED**************");
//        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            Log.d(TAG,"*******Data changed was occurred");

            for(DataEvent event : dataEventBuffer){
                if(event.getType() == DataEvent.TYPE_CHANGED){
                    DataItem item  = event.getDataItem();
                    if(item.getUri().getPath().compareTo("/weather-temp") == 0){
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        mHighString = dataMap.getString("HIGH_TEMP");
                        mLowString = dataMap.getString("LOW_TEMP");
                    }
                }
            }

        }


        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        private String formatDay(int day){
            switch (day){
                case 1:
                    return "SUN, ";
                case 2:
                    return "MON, ";
                case 3:
                    return "TUE, ";
                case 4:
                    return "WED, ";
                case 5:
                    return "THU, ";
                case 6:
                    return "FRI, ";
                case 7:
                    return "SAT, ";
                default:
                    return "---";
            }
        }
        private String formatMonth(int month){
            switch (month){
                case 1:
                    return "JAN ";
                case 2:
                    return "FEB ";
                case 3:
                    return "MAR ";
                case 4:
                    return "APR ";
                case 5:
                    return "MAY";
                case 6:
                    return "JUN ";
                case 7:
                    return "JUL ";
                case 8:
                    return "AUG ";
                case 9:
                    return "SEP ";
                case 10:
                    return "OCT ";
                case 11:
                    return "NOV ";
                case 12:
                    return "DEC ";
                default:
                    return "---";
            }
        }
    }
}