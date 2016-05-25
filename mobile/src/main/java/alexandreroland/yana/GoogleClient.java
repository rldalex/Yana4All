package alexandreroland.yana;

import android.app.Service;
import android.content.Intent;

import android.os.Bundle;
import android.os.IBinder;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.Locale;

/**
 * Created by Alexandre Roland le 06/12/2015.
 * rol_25@me.com
 */

public class GoogleClient extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG="GoogleClientService";

    public static GoogleApiClient googleClient;

    @Override

    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {

        Log.v(TAG, "GoogleClient création du service");

        // Construit une client Google pour l'API Wearable
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        super.onCreate();
    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onDestroy();
    }

    // Send a message when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {
        String message = "Méthode onConnected GoogleClient";
        //Requires a new thread to avoid blocking the UI
        //new ClientService.SendToDataLayerThread("/start_activity", message).start();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.v(TAG, "GoogleClient start service");
        googleClient.connect();
        super.onStart(intent, startId);
    }

    public void terminerProcess(){
        onDestroy();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }
}
