package alexandreroland.sendmessage;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static ImageButton vocal_reception;

    private GoogleApiClient googleClient;

    private String TAG = "MainActivity";
    private String commandeVocal;
    private boolean onStartSmartphone;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    Vibrator vibration;

    long[] errorVibration = { 0, 200, 300, 200, 300, 200, 300};// Démarre tout de suite vibre 200ms puis 300ms pause ect...

    private final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibration = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);//Permet la vibration de l'appareil

        preferences = getApplicationContext().getSharedPreferences("MyPref", 0);
        editor = preferences.edit();

        // Construit une client Google pour l'API Wearable
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {


                onStartSmartphone = preferences.getBoolean("ActivityState", false);//false est la valeur par défaut de la préférence si elle n'est pas renseigné
                Log.v(TAG, "onStartSmartphone: " + onStartSmartphone);

                showToastMessage("N'oubliez pas de vous connecter à yana depuis votre smartphone et de laisser l'application en fond de tâche.");
                vocal_reception = (ImageButton) stub.findViewById(R.id.button_vocal);
                vocal_reception.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.v(TAG, "Démarrage de la reconnaissance vocal.");
                        promptSpeechInput();
                    }

                });
            }
        });

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    /**
     * Affiche google speech input dialog
     */
    public void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Récupere les données vocal recu
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    commandeVocal = result.get(0).toString(); //recupere la commande vocal
                    Intent confirmation = new Intent(getApplicationContext(), ConfirmationActivity.class);
                    if(onStartSmartphone){
                        confirmation.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                        confirmation.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.msg_confirmation));
                        new SendToDataLayerThread("/message_path", commandeVocal).start();
                        startActivity(confirmation);
                        vibration.vibrate(200);
                    }else
                    {
                        confirmation.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                        confirmation.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.msg_erreur));
                        startActivity(confirmation);
                        vibration.vibrate(errorVibration, -1);//Effectue seulement ce modèle une fois (-1 signifie ne pas répéter )
                    }
                }
                break;
            }
        }
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();
    }

    // Send a message when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {
        //String message = "Je t'envoie ce message depuis la montre";
        //Requires a new thread to avoid blocking the UI
        //new SendToDataLayerThread("/message_path", message).start();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v(TAG, "Message: {" + message + "} envoyé à: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.v(TAG, "ERREUR: impossible d'envoyer le message");
                }
            }
        }
    }

    public void showToastMessage(String message){

        TextView textview = new TextView(getApplicationContext());
        textview.setText(message);
        textview.setBackgroundColor(Color.WHITE);
        textview.setTextColor(Color.BLACK);
        textview.setPadding(10, 10, 10, 10);
        textview.setGravity(Gravity.CENTER);
        Toast toast = new Toast(getApplicationContext());
        toast.setView(textview);
        toast.setDuration(toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.v(TAG, " Reception d'un message: " + message);
            if(message.equals("start")){
                onStartSmartphone = true;
                editor.putBoolean("ActivityState", true);
                editor.commit();
                Log.v(TAG, "ActivityState: true");
            }
            if (message.equals("stop")) {
                onStartSmartphone = false;
                editor.putBoolean("ActivityState", false);
                editor.commit();
                Log.v(TAG, "ActivityState: false");
            }
            // Display message in UI
            //showToastMessage(message);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            stopService(new Intent(this, ListenerService.class));
            Log.v(TAG,"Service ListenerService: Stop");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
