package com.example.android.sunshine.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements DataApi.DataListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    private static final String WEATHER_MOBILE_PATH = "/weather_mobile";
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .addApi(Wearable.API)
                            .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
       mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient,this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        if(dataEventBuffer.getStatus().isSuccess()){
            Log.d(TAG, "Data change item  received SUCCESS");

            for(DataEvent event : dataEventBuffer){
                DataItem item = event.getDataItem();
                if(event.getType() == DataEvent.TYPE_CHANGED){
                    if(item.getUri().getPath().compareTo(WEATHER_MOBILE_PATH) == 0){
                        DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                        double high = map.get("HIGH");
                        double low = map.get("LOW");
                        String desc = map.get("DESC");

                        Log.d(TAG, "High: " + high);
                        Log.d(TAG, "Low: " + low);
                        Log.d(TAG, "Desc: " + desc);
                    }
                }else if(event.getType() == DataEvent.TYPE_DELETED){

                }
            }
        }else{
            Log.d(TAG, "Data change item received FAILED");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG,"Google API Client connected SUCCESS");
        Wearable.DataApi.addListener(mGoogleApiClient,this);

        getWeatherUpdate();
    }

    private void getWeatherUpdate(){
        final PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallbacks<DataItemBuffer>() {
            @Override
            public void onSuccess(@NonNull DataItemBuffer dataItems) {
                Log.d(TAG, "Received Data Item Callback SUCCESS");
                for(DataItem item : dataItems){
                    if(item.getUri().getPath().compareTo(WEATHER_MOBILE_PATH)==0){
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                        DataMap map = dataMapItem.getDataMap();
                        double high = map.get("HIGH");
                        double low = map.get("LOW");
                        String desc = map.get("DESC");

                        Log.d(TAG, "High: " + high);
                        Log.d(TAG, "Low: " + low);
                        Log.d(TAG, "Desc: " + desc);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.d(TAG,"Received Data Item Callback FAILED");
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google API Client SUSPENDED");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Google API Client FAILED");
    }
}
