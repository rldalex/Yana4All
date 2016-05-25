package alexandreroland.yana;

import android.app.Service;
import android.content.Intent;

import android.os.IBinder;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by Alexandre Roland le 06/12/2015.
 * rol_25@me.com
 */

public class TextToSpeechService extends Service implements TextToSpeech.OnInitListener {

    private static TextToSpeech mTts;
    private static final String TAG="TextToSpeechService";

    @Override

    public IBinder onBind(Intent arg0) {
    return null;
    }

    @Override
    public void onCreate() {

        mTts = new TextToSpeech(getApplicationContext(), this);
        mTts.setSpeechRate(1.1f);
        mTts.setPitch(1.2f);
        Log.v(TAG, "TextToSpeech création du service");
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }


    @Override
    public void onStart(Intent intent, int startId) {
        Log.v(TAG, "TextToSpeech start service");
        super.onStart(intent, startId);
    }

    @Override
    public void onInit(int status) {
        Log.v(TAG, "initialisation de TextToSpeech");
        if (status == TextToSpeech.SUCCESS) {
            if(mTts.isLanguageAvailable(Locale.FRENCH)==TextToSpeech.LANG_AVAILABLE)
                mTts.setLanguage(Locale.FRENCH);
        }
        else if (status == TextToSpeech.ERROR) {
            Toast.makeText(this, "Désolé! Text To Speech a rencontré une erreur...", Toast.LENGTH_LONG).show();
        }
    }

    public static void yanaVoice(String speech) {
        mTts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
        Log.v(TAG, "Diction de: " + speech);
    }
}
