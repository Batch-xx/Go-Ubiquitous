package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
        private static final int MSG_UPDATE_TIME = 0;
        private static final String WEATHER_MOBILE_PATH = "/weather_mobile";

        //Offsets
        private int mTimeYOffset = 0;
        private int mDateYOffset = 0;
        private int mTempYOffset = 0;


        //Graphic objects
        private Paint mBackgroundPaint = null;
        private Paint mDatePaint = null;
        private Paint mTimePaint = null;
        private Paint mTempHiPaint = null;
        private Paint mTempLoPaint = null;
        private Paint mImagePaint = null;
        private Paint mDescPaint = null;

        //Font Properties
        private final Typeface NORMAL_TYPE_TIME = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        private final Typeface BOLD_TYPE_TIME = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

        private final Typeface NORMAL_TYPE_DATE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        private final Typeface BOLD_TYPE_DATE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

        private final Typeface NORMAL_TYPE_TEMP = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        private final Typeface BOLD_TYPE_TEMP = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

        //Updated Values
        private String mLowTemp = "--";
        private String mHiTemp = "--";
        private Bitmap mWeatherImage = null;
        private String mDesc = "-----";


        private Calendar mCalendar;
        private Date mDate = null;
        private SimpleDateFormat mDayOfTheWeek = null;
        private java.text.DateFormat mDateFormat = null;

        //device features
        private boolean mLowBitAmbient;
        private boolean mBurnProtection;

        //Google API Client
        private GoogleApiClient mGoogleApiClient;


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
            mGoogleApiClient = new GoogleApiClient.Builder(WeatherWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = WeatherWatchFaceService.this.getResources();
            mTimeYOffset = resources.getDimensionPixelOffset(R.dimen.time_y_offset);
            mDateYOffset = resources.getDimensionPixelOffset(R.dimen.date_y_offset);
            mTempYOffset = resources.getDimensionPixelOffset(R.dimen.temp_y_offset);

            mBackgroundPaint = new Paint();


            mTimePaint = new Paint();
            mTimePaint.setColor(Utility.TIME_COLOR_INTERACTIVE);
            mTimePaint.setAntiAlias(true);
            mTimePaint.setTypeface(BOLD_TYPE_TIME);
            mTimePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.time_line_ht));

            mDatePaint = new Paint();
            mDatePaint.setColor(Utility.DATE_COLOR_INTERACTIVE);
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTypeface(NORMAL_TYPE_DATE);
            mDatePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.date_line_ht));

            mTempHiPaint = new Paint();
            mTempHiPaint.setColor(Utility.TIME_COLOR_INTERACTIVE);
            mTempHiPaint.setAntiAlias(true);
            mTempHiPaint.setTypeface(NORMAL_TYPE_TEMP);
            mTempHiPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.temp_hi_line_ht));

            mTempLoPaint = new Paint();
            mTempLoPaint.setColor(Utility.TIME_COLOR_INTERACTIVE);
            mTempLoPaint.setAntiAlias(true);
            mTempLoPaint.setTypeface(NORMAL_TYPE_TEMP);
            mTempLoPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.temp_lo_line_ht));

            mDescPaint = new Paint();
            mDescPaint.setColor(Utility.TIME_COLOR_INTERACTIVE);
            mDescPaint.setAntiAlias(true);
            mDescPaint.setTypeface(NORMAL_TYPE_TEMP);
            mDescPaint.setTextAlign(Paint.Align.CENTER);
            mDescPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.decs_line_ht));

            mImagePaint = new Paint();
            mImagePaint.setAntiAlias(true);

            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();
        }


        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mLowBitAmbient) {
                boolean antialias = !inAmbientMode;
                mTimePaint.setAntiAlias(antialias);
                mDatePaint.setAntiAlias(antialias);
                mTempHiPaint.setAntiAlias(antialias);
                mTempLoPaint.setAntiAlias(antialias);
                mDescPaint.setAntiAlias(antialias);
            }

            mTimePaint.setTypeface(inAmbientMode == true ? NORMAL_TYPE_TIME : BOLD_TYPE_TIME);
            mTempHiPaint.setTypeface(inAmbientMode == true ? NORMAL_TYPE_TEMP : BOLD_TYPE_TEMP);
            mTempLoPaint.setTypeface(inAmbientMode == true ? NORMAL_TYPE_TEMP : BOLD_TYPE_TEMP);

            invalidate();
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            //background
            if (isInAmbientMode()) {
                mBackgroundPaint.setColor(Utility.BACKGROUND_COLOR_AMBIENT);
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mBackgroundPaint);
            } else {
                mBackgroundPaint.setColor(Utility.BACKGROUND_COLOR_INTERACTIVE);
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mBackgroundPaint);
            }

            mCalendar.setTimeInMillis(System.currentTimeMillis());
            mCalendar.setTimeZone(TimeZone.getDefault());

            Locale loc = Locale.getDefault();

            String timeText = String.format(loc, "%s:%02d", hourConverter(mCalendar.get(Calendar.HOUR)),
                    mCalendar.get(Calendar.MINUTE));
            float timeTextWidth = mTimePaint.measureText(timeText);
            canvas.drawText(timeText, (bounds.width() / 2) - (int) timeTextWidth / 2,
                    bounds.height() / 2 - mTimeYOffset, mTimePaint);

            String dateText = String.format(loc, "%s %d, %d", monthConverter(mCalendar.get(Calendar.MONTH)),
                    mCalendar.get(Calendar.DAY_OF_MONTH), mCalendar.get(Calendar.YEAR));
            float dateTextWidth = mDatePaint.measureText(dateText);
            canvas.drawText(dateText, (bounds.width() / 2) - (int) dateTextWidth / 2,
                    bounds.height() / 2 + mDateYOffset, mDatePaint);

            if(!isInAmbientMode()){
                if(mWeatherImage != null){
                    canvas.drawBitmap(mWeatherImage,(bounds.width() / 2) - 100, bounds.height() / 2 + mTempYOffset ,mImagePaint);
                }
            }else{
                String desc = String.format(loc, "%s", mDesc);
                float descTextWidth = mDescPaint.measureText("-----");
                canvas.drawText(desc,(bounds.width() / 2) - (int) descTextWidth / 2 - 67 ,
                        bounds.height() / 2 + mTempYOffset + 46, mTempLoPaint);
            }

            String hiTemp = String.format(loc,"%s",mHiTemp);
            float hiTempTextWidth = mTempHiPaint.measureText(mHiTemp);
            canvas.drawText(hiTemp, (bounds.width() / 2) - (int) hiTempTextWidth / 2 + 4,
                    bounds.height() / 2 + mTempYOffset + 52, mTempHiPaint);

            String loTemp = String.format(loc,"%s ", mLowTemp);
            float loTempTextWidth = mTempLoPaint.measureText(mLowTemp);
            canvas.drawText(loTemp, (bounds.width() / 2) - (int) loTempTextWidth / 2 + 55,
                    bounds.height() / 2 + mTempYOffset + 46, mTempLoPaint);

        }
        private String hourConverter(int hour){
            boolean is24Hour = DateFormat.is24HourFormat(WeatherWatchFaceService.this);
            if(is24Hour){
                return  String.format(Locale.getDefault(),"%02d", mCalendar.get(Calendar.HOUR_OF_DAY));
            }else {
                if(hour == 0){
                    hour = 12;
                }
            }
            return String.valueOf(hour);
        }

        private String monthConverter(int month) {
            switch (month) {
                case 0:
                    return "JAN";
                case 1:
                    return "FEB";
                case 2:
                    return "MAR";
                case 3:
                    return "APR";
                case 4:
                    return "MAY";
                case 5:
                    return "JUN";
                case 6:
                    return "JUL";
                case 7:
                    return "AUG";
                case 8:
                    return "SEPT";
                case 9:
                    return "OCT";
                case 10:
                    return "NOV";
                case 11:
                    return "DEC";
                default:
                    return "---";

            }


        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            /* Used to resize images */
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "Visibility changed to: " + visible);

            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());


            } else {
                unregisterReceiver();
            }
            updateTimer();
        }

        private void initFormats() {
            mDayOfTheWeek = new SimpleDateFormat("EEEE", Locale.getDefault());
            mDayOfTheWeek.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(WeatherWatchFaceService.this);
            mDateFormat.setCalendar(mCalendar);
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

        private void unregisterReceiver() {
            if (!mIsRegisteredReceiver) {
                return;
            }
            mIsRegisteredReceiver = false;
            WeatherWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            Log.d(TAG, "Updating timer");

            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }

        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, "Google API Client connected SUCCESS");
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            new WeatherUpdateTask().execute();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "Google API Client is SUSPENDED");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "Google API Client is FAILED");
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            if (dataEventBuffer.getStatus().isSuccess()) {
                Log.d(TAG, "Data change item  received SUCCESS");

                for (DataEvent event : dataEventBuffer) {
                    DataItem item = event.getDataItem();
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        if (item.getUri().getPath().compareTo(WEATHER_MOBILE_PATH) == 0) {
                            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                            mHiTemp = dataMap.getString("HIGH");
                            mLowTemp = dataMap.getString("LOW");
                            mDesc = dataMap.get("DESC");
                            Asset imageAsset = dataMap.getAsset("IMG");
                            new LoadBitmapFromAsset().execute(imageAsset);

                            Log.d(TAG, "High: " + mHiTemp);
                            Log.d(TAG, "Low: " + mLowTemp);
                            Log.d(TAG, "Desc: " + mDesc);
                            invalidate();
                        }
                    } else if (event.getType() == DataEvent.TYPE_DELETED) {

                    }
                }
            } else {
                Log.d(TAG, "Data change item received FAILED");
            }
        }

        private class WeatherUpdateTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                mGoogleApiClient.blockingConnect();
                final PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
                results.setResultCallback(new ResultCallbacks<DataItemBuffer>() {
                    @Override
                    public void onSuccess(@NonNull DataItemBuffer dataItems) {
                        Log.d(TAG, "Received Data Item Callback SUCCESS");
                        for (DataItem item : dataItems) {
                            if (item.getUri().getPath().compareTo(WEATHER_MOBILE_PATH) == 0) {
                                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                                DataMap map = dataMapItem.getDataMap();
                                mHiTemp = map.getString("HIGH");
                                mLowTemp = map.getString("LOW");
                                mDesc  = map.get("DESC");
                                Asset imageAsset = dataMapItem.getDataMap().getAsset("IMG");
                                new LoadBitmapFromAsset().execute(imageAsset);

                                Log.d(TAG, "High: " + mHiTemp);
                                Log.d(TAG, "Low: " + mLowTemp);
                                Log.d(TAG, "Desc: " + mDesc);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull com.google.android.gms.common.api.Status status) {

                    }
                });
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                invalidate();
            }
        }

        private class LoadBitmapFromAsset extends AsyncTask<Asset, Bitmap, Bitmap> {
            @Override
            protected Bitmap doInBackground(Asset... assets) {
                Asset asset = assets[0];
                if (asset == null) {
                    throw new IllegalArgumentException("Asset is null/invalid");
                }

                ConnectionResult result = mGoogleApiClient.blockingConnect();
                if (!result.isSuccess()) {
                    Log.d(TAG, "Google API Client connection FAILED");
                    return null;
                }
                InputStream assetStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset)
                        .await().getInputStream();

                if (assetStream == null) {
                    Log.d(TAG, "Null/invalid asset");
                }

                return BitmapFactory.decodeStream(assetStream);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mWeatherImage = Bitmap.createScaledBitmap(bitmap,70,70,false);
                invalidate();
            }
        }
    }
}
