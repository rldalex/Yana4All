package alexandreroland.yana;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Created by Alexandre Roland le 06/12/2015.
 * rol_25@me.com
 */

public class Configuration extends AppCompatActivity {

    private Button server_ip_conf;
    private EditText client_user;
    private EditText client_password;
    private EditText LAN;
    private EditText WAN;
    private EditText SSID;

    private String adresse_ip_lan = null;
    private String adresse_ip_wan = null;
    private String nom_ssid = null;
    private String localisation = null;
    private String url = null;
    private String TAG = "Configuration";
    private String user = null;
    private String password = null;

    private Intent PageInternet;

    private ImageView androidYana;
    private CheckBox save_config;

    public static ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        final SharedPreferences preferences = getApplicationContext().getSharedPreferences("MyPref", 0);
        final SharedPreferences.Editor editor = preferences.edit();

        server_ip_conf = (Button) findViewById(R.id.button_server_ip_conf);
        client_user = (EditText) findViewById(R.id.edittext_server_user);
        client_password = (EditText) findViewById(R.id.edittext_client_password);

        save_config = (CheckBox) findViewById(R.id.checkbox_saveparametres);

        androidYana = (ImageView) findViewById(R.id.imageYana);

        adresse_ip_lan = preferences.getString("preferenceIPlan", "");
        adresse_ip_wan = preferences.getString("preferenceIPwan", "");
        nom_ssid = preferences.getString("preferenceSSID", "");
        user = preferences.getString("preferenceUser", "");
        password = preferences.getString("preferencePassword", "");
        client_user.setText(user);
        client_password.setText(password);

        startService(new Intent(this, GoogleClient.class));

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        androidYana.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.i(TAG, "GetSSID: " + wifiInfo.getSSID() + " SSID: " + "\"" + nom_ssid + "\"" + " IP LAN: " + adresse_ip_lan + " IP WAN: " + adresse_ip_wan);
                if(!adresse_ip_lan.equals("") && wifiInfo.getSSID().equalsIgnoreCase("\"" + nom_ssid + "\"")){
                    url = "http://" + adresse_ip_lan + "/yana-server/";// Adresse IP de YANA LAN
                    PageInternet = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(PageInternet);// Ouvre la page web dans le navigateur
                }
                if(!adresse_ip_wan.equals("") && !(wifiInfo.getSSID().equalsIgnoreCase("\"" + nom_ssid + "\""))){
                    url = "http://" + adresse_ip_wan + "/yana-server/";// Adresse IP de YANA WAN
                    PageInternet = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(PageInternet);// Ouvre la page web dans le navigateur
                }
            }
        });

        server_ip_conf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(Configuration.this);
                // Get the layout inflater
                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                View dialogView = inflater.inflate(R.layout.dialogboxconf, null);//Création d'une vue pour récupere les informations dans l'EditText

                LAN = (EditText) dialogView.findViewById(R.id.edittext_LAN);
                WAN = (EditText) dialogView.findViewById(R.id.edittext_WAN);
                SSID = (EditText) dialogView.findViewById(R.id.edittext_SSID);

                nom_ssid = wifiInfo.getSSID().replace("\"", "");//récupere le nom de la connection wifi actuelle

                LAN.setText(adresse_ip_lan);
                WAN.setText(adresse_ip_wan);
                SSID.setText(nom_ssid);

                builder.setView(dialogView)
                        // Add action buttons
                        .setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Sauvegarder", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                adresse_ip_lan = LAN.getText().toString();
                                adresse_ip_wan = WAN.getText().toString();
                                nom_ssid = SSID.getText().toString();
                                if (adresse_ip_lan.length() == 0 || adresse_ip_wan.length() == 0 || nom_ssid.length() == 0) {
                                    server_ip_conf.setText("Informations manquantes");
                                } else {
                                    server_ip_conf.setText("Configuration OK");
                                }
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        final Button button = (Button) findViewById(R.id.button_connection_server);
        assert button != null;
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                user = client_user.getText().toString();
                password = client_password.getText().toString();

                localisation = Build.MODEL;

                if (save_config.isChecked()) { //On sauvegarde les données si la checkbox est cochée.
                    editor.putString("preferenceIPlan", adresse_ip_lan);
                    editor.putString("preferenceIPwan", adresse_ip_wan);
                    editor.putString("preferenceSSID", nom_ssid);
                    editor.putString("preferenceUser", user);
                    editor.putString("preferencePassword", password);
                    editor.apply();
                }
                else{
                    preferences.edit().clear().apply();// On efface toutes les préférences sauvegardées.
                }
                if (adresse_ip_lan.length() == 0) {
                    showToastMessage("Merci de renseigner l'adresse IP LAN.");
                }
                if (adresse_ip_wan.length() == 0) {
                    showToastMessage("Merci de renseigner l'adresse IP WAN.");
                }
                if (nom_ssid.length() == 0) {
                    showToastMessage("Merci de renseigner le nom de votre réseau wifi.");
                }
                if (user.length() == 0) {
                    showToastMessage("Merci de renseigner votre nom d'utilisateur.");
                }
                if (password.length() == 0) {
                    showToastMessage("Merci de renseigner votre mot de passe.");
                }
                else if(adresse_ip_lan.length() != 0 && adresse_ip_wan.length() != 0 && nom_ssid.length() != 0){
                    progressBar = new ProgressDialog(v.getContext());
                    progressBar.setCancelable(false);
                    progressBar.setMessage("Vérification des droits utilisateur...");
                    progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressBar.show();

                    Intent ClientService = new Intent(Configuration.this, ClientService.class);

                    ClientService.putExtra("iplan", adresse_ip_lan);

                    ClientService.putExtra("ipwan", adresse_ip_wan);
                    ClientService.putExtra("ssid", nom_ssid);
                    ClientService.putExtra("user", user);
                    ClientService.putExtra("password", password);

                    ClientService.putExtra("localisation", localisation);

                    Intent NetworkInfos = new Intent(Configuration.this, CheckNetwork.class);
                    startService(NetworkInfos);

                    startService(ClientService);
                    Log.i(TAG, "Start ClientService");
                }
            }
        });
    }

    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"onDestroy");
        try {
            stopService(new Intent(this,CheckNetwork.class));
            stopService(new Intent(this,GoogleClient.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showToastMessage(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}

