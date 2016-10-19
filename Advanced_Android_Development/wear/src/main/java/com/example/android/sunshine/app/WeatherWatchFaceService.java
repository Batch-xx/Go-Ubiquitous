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
        private int mTempTOffset = 0;

        //Line Height
        private int mTimeLineHt = 0;
        private int mDateLineHt = 0;
        private int mTempLineHt = 0;

        //Graphic objects
        private Paint mBackgroundPaint = null;
        private Paint mDatePaint = null;
        private Paint mTimePaint = null;
        private Paint mTempPaint = null;

        //Font Properties
        private  final Typeface NORMAL_TYPE_TIME = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        private  final Typeface BOLD_TYPE_TIME = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

        private  final Typeface NORMAL_TYPE_DATE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        private  final Typeface BOLD_TYPE_DATE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

        private  final Typeface NORMAL_TYPE_TEMP = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        private  final Typeface BOLD_TYPE_TEMP = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

        //Updated Values
        private String mLowTemp = "--";
        private String mHiTemp = "--";


        private Calendar mCalendar;
        private Date mDate = null;
        private SimpleDateFormat mDayOfTheWeek = null;
        private java.text.DateFormat mDateFormat = null;

        //device features
        private boolean mLowBitAmbient;
        private boolean mBurnnProtection;

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
            mTempTOffset = resources.getDimensionPixelOffset(R.dimen.temp_y_offset);

            mTimeLineHt = resources.getDimensionPixelOffset(R.dimen.time_line_ht);
            mDateLineHt = resources.getDimensionPixelOffset(R.dimen.date_line_ht);
            mTempLineHt = resources.getDimensionPixelOffset(R.dimen.time_line_ht);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Utility.BACKGROUND_COLOR_INTERACTIVE);

            mTimePaint = new Paint();
            mTimePaint.setColor(Utility.TIME_COLOR_INTERACTIVE);
            mTimePaint.setAntiAlias(true);
            mTimePaint.setTypeface(NORMAL_TYPE_TIME);
            mTimePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.time_line_ht));

            mDatePaint = new Paint();
            mDatePaint.setColor(Utility.DATE_COLOR_INTERACTIVE);
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTypeface(NORMAL_TYPE_DATE);
            mDatePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.date_line_ht));

            mTempPaint = new Paint();
            mTempPaint.setColor(Utility.TIME_COLOR_INTERACTIVE);
            mTempPaint.setAntiAlias(true);
            mTempPaint.setTypeface(NORMAL_TYPE_TEMP);
            mTempPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.temp_line_ht));


            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();
        }



        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnnProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
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
                mTempPaint.setAntiAlias(antialias);
            }
            invalidate();
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            //background
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mBackgroundPaint);

            mCalendar.setTimeInMillis(System.currentTimeMillis());
            mCalendar.setTimeZone(TimeZone.getDefault());

            String date = mDateFormat.format(new Date());
            Locale loc = Locale.getDefault();

            String timeText = String.format(loc, "%d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE));

            canvas.drawText(mHiTemp, bounds.width() / 2, bounds.height() / 2 - 45, mTempPaint);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            /* Uesed to resize imagees */
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
                            mHiTemp = String.valueOf(dataMap.get("HIGH"));
                            mLowTemp = String.valueOf(dataMap.get("LOW"));
                            String desc = dataMap.get("DESC");
                            Asset imageAsset = dataMap.getAsset("IMG");
                            new LoadBitmapFromAsset().execute(imageAsset);

                            Log.d(TAG, "High: " + mHiTemp);
                            Log.d(TAG, "Low: " + mLowTemp);
                            Log.d(TAG, "Desc: " + desc);
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
                                mHiTemp = String.valueOf(map.get("HIGH"));
                                mLowTemp = String.valueOf(map.get("LOW"));
                                String desc = map.get("DESC");
                                Asset imageAsset = dataMapItem.getDataMap().getAsset("IMG");
                                new LoadBitmapFromAsset().execute(imageAsset);

                                Log.d(TAG, "High: " + mHiTemp);
                                Log.d(TAG, "Low: " + mLowTemp);
                                Log.d(TAG, "Desc: " + desc);
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
                    Log.d(TAG, "Null/invaild asset");
                }

                return BitmapFactory.decodeStream(assetStream);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                //TODO place image into placeholder
            }

        }
    }
}
