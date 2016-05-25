package alexandreroland.yana;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Alexandre Roland le 06/12/2015.
 * rol_25@me.com
 */

public class YANA extends AppCompatActivity {

    public static String commande = null;
    public static String commande_envoie = null;
    String text_commande_vocal = null;
    private static String TAG = "YANA";

    public static ListView commandeVocal;

    public static ScrollView scroll;

    TextView commandeYana;

    public static ProgressDialog progressBar;

    public static android.support.design.widget.CoordinatorLayout layoutYana;

    public static EditText userBug;

    public static SlidingDrawer slidingDrawer;

    private final int REQ_CODE_SPEECH_INPUT = 100;
    public static LinearLayout conversation;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yana);

        progressBar = new ProgressDialog(this);

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        slidingDrawer = (SlidingDrawer) findViewById(R.id.drawer);

        commandeVocal = (ListView) findViewById(R.id.listview_commande); //List view qui affiche les commandes vocal.
        commandeVocal.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ClientService.listeCommandesVocal));//ajoute les commande vocal dans la liste.

        commandeVocal.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long id) {
                TextView clickedView = (TextView) view;
                commande = clickedView.getText().toString().replace("\n","");
                envoie_commande_server();
                //showToastMessage("Item avec id [" + id + "] - Position [" + position + "] - commande vocal [" + clickedView.getText() + "]");

            }
        });

        conversation = (LinearLayout) findViewById(R.id.conversation); //Layout qui contient la conversation.

        scroll = (ScrollView) findViewById(R.id.scrollview_conversation);// ScrollView de la conversation.

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab); // Boutton flottant pour lancer la reconnaissance vocal.
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput(); // Lance la reconnaissance vocal
            }
        });

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_dashboard) {
            String url = "http://" + ClientService.adresse_ip_lan + "/yana-server";
            Intent PageInternet = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(PageInternet);// Ouvre le dashboard dans le navigateur internet
        }
        if (id == R.id.menu_bug) {
            //showToastMessage("C'est " + item.getTitle().toString() + " qui à été séléctionné");


            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            // Get the layout inflater
            LayoutInflater inflater = this.getLayoutInflater();
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            View dialogView = inflater.inflate(R.layout.dialogboxbug, null);//Création d'une vue pour récupere les informations dans l'EditText

            userBug = (EditText) dialogView.findViewById(R.id.edittext_user_bug_text);

            builder.setView(dialogView)
                    // Add action buttons
                    .setPositiveButton("Envoyer", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {

                            String text = userBug.getText().toString();

                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "roland.alexandrer@gmail.com", null));
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bug YANA Android");
                            emailIntent.putExtra(Intent.EXTRA_TEXT, text);
                            startActivity(Intent.createChooser(emailIntent, "Avec quelle application souhaitez vous envoyer le rapport de bug ?"));
                        }
                    })
                    .setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }
        if (id == R.id.menu_don) {
            //showToastMessage("C'est " + item.getTitle().toString() + " qui à été séléctionné");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            // Get the layout inflater
            LayoutInflater inflater = this.getLayoutInflater();
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            View dialogView = inflater.inflate(R.layout.dialogboxdon, null);//Création d'une vue pour récupere les informations dans l'EditText
            builder.setView(dialogView)
                    // Add action buttons
                    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=rol_25%40me%2ecom&lc=FR&item_name=Roland%20Alexandre&item_number=YanaforAndroid%20Dev&no_note=0&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHostedGuest";
                            Intent PageInternet = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(PageInternet);// Ouvre la page web dans le navigateur
                        }
                    })
                    .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }
        /*if (id == R.id.menu_deconnexion) {
            Intent Configuration = new Intent(YANA.this, Configuration.class);
            Configuration.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Configuration);
            finish();
            close();
        }*/

        return super.onOptionsItemSelected(item);
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
    }

    public void onDestroy() {
        super.onDestroy();
        close();
    }

    @Override
    public void onBackPressed() {
        if (slidingDrawer.isOpened()) {
            slidingDrawer.close();
        } else {
            super.onBackPressed();
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.v(TAG, "L'activitée YANA à recu le message: " + message);
            // Display message in UI
            //showToastMessage(message);
            commande = message;
            envoie_commande_server();
        }
    }

    public void close(){
        new ClientService.SendToDataLayerThread("/message_path", "stop").start();//envoie de la confirmation de fermeture du service à la montre
        try {
            stopService(new Intent(this, ClientService.class));
            stopService(new Intent(this, CheckNetwork.class));
            stopService(new Intent(this,TextToSpeechService.class));
            stopService(new Intent(this,GoogleClient.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showToastMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Affiche google speech input dialog
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void envoie_commande_server(){
        commande_envoie = ClientService.remplace_commande_vocal_recu(commande); //Remplace la commande vocal par une de yana.
        text_commande_vocal = ClientService.recup_fin_commande_vocal(commande, commande_envoie);//Récupere la fin de la commande vocal écouté pour l'ajouter à la commande comparée dans la liste des commandes vocales

        if(commande_envoie.equals("Yana, emmene moi à")) {
            double lat = 0;
            double lng = 0;
            try {
                Location l = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                lat = l.getLatitude();
                lng = l.getLongitude();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            String destination = text_commande_vocal;
            if(destination.equals("")){
                showToastMessage("Je n'ai pas compris où tu souhaites aller =/.");
            }else {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr="+lat+","+lng+"&daddr="+destination));
                startActivity(intent);
            }
        }else{
            Log.v(TAG, "Fin de la commande vocal: " + text_commande_vocal);
            Log.v(TAG, "Fin de la commande vocal: " + text_commande_vocal);

            commandeYana = new TextView(this);//TextView qui affiche les commandes vocal.
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,1.0f);
            params.gravity=Gravity.RIGHT;
            params.setMargins(0, 5, 0, 5); // (gauche, haut, droite, bas);
            commandeYana.setBackgroundResource(R.drawable.bullebleu);// ajoute la bulle de conversation bleu
            commandeYana.setTextColor(Color.WHITE);
            commandeYana.setLayoutParams(params);

            if (text_commande_vocal==null){
                commandeYana.setText(commande_envoie);
            }
            else {
                commandeYana.setText(commande_envoie + text_commande_vocal);
            }

            conversation.addView(commandeYana);

            scroll.postDelayed(new Runnable() { //Un court délai donne au système suffisamment de temps pour prendre en compte les nouveaux textview à afficher.
                @Override
                public void run() {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            }, 100L);

            Log.v(TAG,"Dernier mot de la commande levenshtein: " + ClientService.dernierMot(commande_envoie));
            ClientService.send_vocal_command(commande_envoie,text_commande_vocal);//Envoie de la commande vocal remplacé
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
                    commande = result.get(0); //recupere la commande vocal
                    Log.v(TAG,"Dernier mot de la commande entendu: " + ClientService.dernierMot(commande));
                    Log.v(TAG, "Commande vocal entendu: " + commande);
                    envoie_commande_server();
                }
                break;
            }
        }
    }
}
