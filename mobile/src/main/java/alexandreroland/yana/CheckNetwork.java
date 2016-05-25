package alexandreroland.yana;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Alexandre Roland on 08/12/2015.
 * rol_25@me.com
 */
public class CheckNetwork extends Service{

    String TAG = "CheckNetwork";
    public static NetworkChangeReceiver receiver;

    public static boolean isConnected = false;

    @Override

    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkChangeReceiver();
        registerReceiver(receiver, filter);
        Log.i(TAG, "CheckNetwork création du service");
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "CheckNetwork start service");
        super.onStart(intent, startId);
    }

    public void onInit(int status) {
        Log.v(TAG, "initialisation de CheckNetwork");
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            Log.i(TAG, "Notification concernant la connection internet.");
            Log.i(TAG, "MANAGE CONNEXION – onReceive connexion changed");
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo.State state = networkInfo.getState();
            String typeName = networkInfo.getTypeName();
            String extra = networkInfo.getExtraInfo();
            Log.i(TAG, "connexion data change NetworkInfo:" + networkInfo);
            Log.i(TAG, "connexion data change NetworkInfo::State: " + state);
            Log.i(TAG, "connexion data change NetworkInfo::TypeName: " + typeName);
            Log.i(TAG, "connexion data change NetworkInfo::Extra: " + extra);
            alexandreroland.yana.ClientService.network_type_memory = typeName;
            alexandreroland.yana.ClientService.network_state = state.toString();
            if(state.toString().equals("DISCONNECTED")){
                isConnected = false;
            }
            else{
                isConnected = true;
            }
            isNetworkAvailable(context);
        }

        private boolean isNetworkAvailable(Context context) {
            ConnectivityManager connectivity = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null) {
                    for (int i = 0; i < info.length; i++) {
                        if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                            if(!isConnected){
                                //showToastMessage("Vous êtes connecté à internet!");
                                //Configuration.progressBarConnection.dismiss();
                                //isConnected = true;
                            }
                            return true;
                        }
                    }
                }
            }
            //showToastMessage("Vous n'êtes pas connecté à internet!");
            //Configuration.progressBarConnection.dismiss();
            //isConnected = false;
            return false;
        }
    }
}
