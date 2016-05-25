package alexandreroland.yana;

import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Alexandre Roland le 06/12/2015.
 * rol_25@me.com
 */

public class ClientService extends IntentService {

    static Socket clientTalk;
    static SocketAddress remoteAddr;

    static BufferedReader in;
    static BufferedWriter out;
    static InputStream inStream;

    private static String TAG = "ClientService";
    private static int error = 0;
    static String adresse_ip_lan = "";
    static String adresse_ip_wan = "";
    static String network_state = "";
    public static String network_type = "";
    public static String network_type_memory = "";
    private static String token = "";
    private static String password = "";
    private static String user = "";
    public static String nom_ssid = "";
    private static String localisation = "";
    public String reponse = "";

    private MediaPlayer mediaPlayer;

    static int emplacement_mot_commande_vocal;

    public static ArrayList listeCommandesVocal = new ArrayList();

    Handler handler = new Handler(Looper.getMainLooper());
    /**
     * Un constructeur est requis, et doit appeler la méthode
     * superIntentService(String)
     * constructeur avec un nom pour le « worker thread »
     */
    public ClientService() {
        super("ClientService");
    }
    /**
     * L'IntentService appelle la méthode par défaut du « worker thread » avec
     * l'intent passé en paramètre de la méthode. Quand cette méthode est terminée
     * le service s'arrête de lui-même
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Les traitement lourd se font ici
        // ceci ne bloque pas le « thread UI » puisque nous sommes dans le worker thread

        adresse_ip_lan = intent.getStringExtra("iplan"); // récupere l'adresse ip sur la page configuration
        adresse_ip_wan = intent.getStringExtra("ipwan"); // récupere l'adresse ip sur la page configuration
        localisation = intent.getStringExtra("localisation");// récupere la localisation sur la page configuration
        nom_ssid = intent.getStringExtra("ssid");// récupere la localisation sur la page configuration
        password = intent.getStringExtra("password");// récupere la localisation sur la page configuration
        user = intent.getStringExtra("user");// récupere la localisation sur la page configuration

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        try {
            if (wifiInfo.getSSID().equalsIgnoreCase("\"" + nom_ssid + "\"")) {
                token = getTocken(adresse_ip_lan,user,password);//récuperation du token
                socket_connection(adresse_ip_lan);//connection au serveur
                network_type = "WIFI";
            } else {
                token = getTocken(adresse_ip_wan,user,password);
                socket_connection(adresse_ip_wan);
                network_type = "MOBILE";
            }
            if(error==0) {
                Log.i(TAG, "Initialisation du client");
                initialisation_client();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Configuration.progressBar.setMessage("Récuperation des commandes vocales...");
                    }
                });
                Thread.sleep(1000);
                Configuration.progressBar.dismiss();
                final Intent YANA = new Intent(ClientService.this, YANA.class);
                YANA.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(YANA);
                showToastMessage("Vous êtes à présent connecté avec YANA.");

                startService(new Intent(this, TextToSpeechService.class));//On lance le service TTS et on connect la montre au téléphone
                //Permet de laisser le processus de réposne vocal toujours actif
                while (true) {

                    getReponse();// Attends les réponses de YANA
                    Log.i(TAG, "NetWorkType: " + network_type);
                    Log.i(TAG, "NetWorkTypeMemory: " + network_type_memory);
                    Log.i(TAG, "SSIDInfo: " + wifiInfo.getSSID());
                    Log.i(TAG, "StateInfo: " + network_state);

                    if (!network_type.equals(network_type_memory)) {//Si on change entre Wifi et Data
                        showProgressBar("Reconnexion avec Yana en cours...");
                        network_type = network_type_memory;
                        in.close();
                        out.close();
                        inStream.close();
                        clientTalk.close();
                        if (network_type_memory.equals("MOBILE")) {
                            socket_connection(adresse_ip_wan);
                            initialisation_client();
                        } else {
                            socket_connection(adresse_ip_lan);
                            initialisation_client();
                        }
                        showProgressBar("Reconnexion avec Yana terminée...");
                        sleep(1000);
                        alexandreroland.yana.YANA.progressBar.dismiss();
                    }
                }
            }
        }catch (final UnknownHostException unknow){
            unknow.printStackTrace();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Configuration.progressBar.setMessage(unknow.getMessage());
                }
            });
            sleep(3000);
            Configuration.progressBar.dismiss();
        }catch (final SocketTimeoutException timeout){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Configuration.progressBar.setMessage(timeout.getMessage());
                }
            });
            sleep(3000);
            Configuration.progressBar.dismiss();
        }catch (final ConnectException connect){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Configuration.progressBar.setMessage(connect.getMessage());
                }
            });
            sleep(3000);
            Configuration.progressBar.dismiss();
        }catch (IOException e){
            e.printStackTrace();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Configuration.progressBar.setMessage("Connexion avec le serveur impossible.\nVérifiez vos informations.");
                }
            });
            sleep(3000);
            Configuration.progressBar.dismiss();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void socket_connection(String ip)throws IOException{
        remoteAddr = new InetSocketAddress(ip,9999);
        clientTalk = new Socket();
        clientTalk.connect(remoteAddr,6000);
        clientTalk = new Socket(ip, 9999);
        clientTalk.setSoTimeout(5);
        out = new BufferedWriter(new OutputStreamWriter(clientTalk.getOutputStream(), Charset.forName("UTF-8")));//StandardCharsets.UTF_8 (API 19)
        inStream = clientTalk.getInputStream();
        in = new BufferedReader(new InputStreamReader(clientTalk.getInputStream()));

    }

    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        try {
            if(clientTalk.isConnected()) {
                out.close();
                in.close();
                clientTalk.close();
            }
            listeCommandesVocal.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initilaisation du client pour yana
     * */
    public void initialisation_client() {
        try {
            Log.i("Initialisation Client", "Initialisation du client android");
            listeCommandesVocal.clear();//Permet d'effacer les commandes vocales lors d'un changement de connexion LAN/WAN
            JSONObject clientInfos = new JSONObject();
            clientInfos.put("action", "CLIENT_INFOS");
            clientInfos.put("type", "speak");
            clientInfos.put("location", localisation);
            clientInfos.put("token", token);
            send_json_data(clientInfos);

            JSONObject catchCommande = new JSONObject();
            catchCommande.put("action", "GET_SPEECH_COMMANDS");
            send_json_data(catchCommande);

            getReponse();

            new SendToDataLayerThread("/message_path", "start").start();//envoie de la confirmation de lancement du service à la montre
            Log.i("Initialisation Client", "Lancement de l'activity YANA");

            commande_vocal_interne();

        } catch (JSONException jsonerror){
            jsonerror.printStackTrace();
        }catch (IOException e){
            Log.i(TAG," IOexeption2");
        }
    }

    /**
     * Permet de récuperer la fin de la commande vocal afin de la rajouter à la commande remplacé
     */
    private void commande_vocal_interne(){
        //listeCommandesVocal.add("Yana, envoie un sms à");//ajout de commande vocal uniquement pour le téléphone
        //listeCommandesVocal.add("Yana, emmene moi à");
        //listeCommandesVocal.add("Yana, met à jour la liste des commandes vocales");
    }

    public static String recup_fin_commande_vocal(String commandeEcoute,String commandeRemplacer){
        String text = null;

        emplacement_mot_commande_vocal = commandeEcoute.indexOf(ClientService.dernierMot(commandeRemplacer));//recherche l'emplacement du dernier mot de la commande vocal dans la commande comprise
        Log.i(TAG,"client service dernier mot " + ClientService.dernierMot(commandeRemplacer));
        Log.i(TAG,"commande ecoute "+ commandeEcoute);
        Log.i(TAG, "emplacement du mot dans la commande vocal: " + emplacement_mot_commande_vocal);

        if(emplacement_mot_commande_vocal!=-1){
            text = commandeEcoute.substring(emplacement_mot_commande_vocal);//recupere l'ensemble de mot à partir du dernier mot de la commande vocal dans la commande comprise
            text = text.replaceFirst(ClientService.dernierMot(commandeRemplacer),"");//supprime le premier mot qui correspond au dernier mot de la commande vocal
        }
        else{
            text = "";
        }
        return text;
    }

    public static String sansAccent(String s)
    {
        String strTemp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(strTemp).replaceAll("");
    }

    protected void getReponse() throws IOException {
        String fluxRecu = null;
        StringBuffer strBuff = new StringBuffer("");
        int h = 0;
        int i =0;

        sleep(2000);// Permet d'attendre que le flux de données soit rempli

        Log.i("ClientService", "inStream.available():" + String.valueOf(inStream.available()));

        while (inStream.available() > 0) {//Tan que le flux de donnée contient des données
            byte[] packetData = new byte[inStream.available()];// On créé un tableau de byte de la taille du flux de données
            inStream.read(packetData, 0, packetData.length);// On remplit le tableau de donnée avec les données
            strBuff.append(new String(packetData, 0, packetData.length));//On ajoute les données dans un StringBuffer
        }

        fluxRecu = strBuff.toString(); //On converti le StringBuffer en String
        String[] fluxRecuSplit = fluxRecu.split("<EOF>");//On créé un tableau de String que l'on remplit avec les données recu splitées par le mot clef EOF

        while (h < fluxRecuSplit.length) {
            fluxRecu = fluxRecuSplit[h];
            h++;
            try {
                final JSONObject jsonObject = new JSONObject(fluxRecu);
                if(jsonObject.getString("action").equals("ADD_COMMAND")) {

                    JSONObject jsonRecu = new JSONObject(fluxRecu);
                    JSONObject jsonCompris = jsonRecu.getJSONObject("command");
                    listeCommandesVocal.add(jsonCompris.getString("command") + "\n");
                    Log.i(TAG, "Liste des commandes vocal: " + listeCommandesVocal.get(i).toString());
                    i++;
                }

                if(jsonObject.getString("action").equals("talk")) {
                    reponse = jsonObject.getString("message");
                    TextToSpeechService.yanaVoice(reponse);//Dicte la réponse de Yana
                    reponseYana(reponse);
                    Log.i(TAG,"réponse de yana: "+reponse);

                    if(reponse.equals("Est ce que tu me trouve jolie?")){
                        reponse = "Je suis désolé je suis trop timide pour me montrer sur un téléphone";
                    }
                    new SendToDataLayerThread("/message_path", reponse).start();//envoie de la réponse sur la montre android
                }

                if(jsonObject.getString("action").equals("sound")){
                    final String son = jsonObject.getString("file");

                    if(son.equals("poule.wav")){
                        reponse = "Cot Cot Cot! Cot Cot Cot!";
                        mediaPlayer = MediaPlayer.create(this, R.raw.poule);
                        mediaPlayer.start();
                        reponseYana(reponse);
                    }

                    if(son.equals("rot.wav")){
                        reponse = "Bbbbrrrooooohhhhhhhhhhh!";
                        mediaPlayer = MediaPlayer.create(this, R.raw.rot);
                        mediaPlayer.start();
                        reponseYana(reponse);
                    }

                    if(son.equals("pet.wav")){
                        reponse = "Prrrouuuuuuut!";
                        mediaPlayer = MediaPlayer.create(this, R.raw.pet);
                        mediaPlayer.start();
                        reponseYana(reponse);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                }
        }
    }

    public TextView reponseYana(String reponse){

        final TextView reponseYana = new TextView(getApplicationContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        params.gravity = Gravity.LEFT;
        params.setMargins(0, 5, 0, 5); // (gauche, haut, droite, bas);
        reponseYana.setTextColor(Color.BLACK);
        reponseYana.setBackgroundResource(R.drawable.bullegrise);// ajoute la bulle de conversation verte
        reponseYana.setLayoutParams(params);
        reponseYana.setText(reponse);

        handler.post(new Runnable() {
            @Override
            public void run() {
                YANA.conversation.addView(reponseYana);//ajoute la vue dans le UI Thread
            }
        });

        YANA.scroll.postDelayed(new Runnable() { //Un court délai donne au système suffisamment de temps pour prendre en compte les nouveaux textview à afficher.
            @Override
            public void run() {
                YANA.scroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 100L);

        return reponseYana;
    }

    public static class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructeur pour envoyer le message au data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(GoogleClient.googleClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(GoogleClient.googleClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.i(TAG, "Message: {" + message + "} envoyé à: " + node.getDisplayName());
                }
                else {
                    // Log une erreur
                    Log.i(TAG, "ERREUR: impossible d'envoer le message");
                }
            }
        }
    }

    /**
     * Compare la commande vocal avec la liste des commandes de yana.
     * */
    public static String remplace_commande_vocal_recu(String str) {
        String keyword = str;
        String[] data = liste_commandes_vocal(listeCommandesVocal).split("\n");
        List<Integer> dist = new ArrayList<Integer>();
        for (int i = 0; i < data.length; i++)
        {
            dist.add(LevenshteinDistance.distance(data[i], keyword));
        }
        Collections.sort(dist);
        for (int i = 0; i < data.length; i++)
        {
            if (LevenshteinDistance.distance(data[i], keyword) == dist.get(0))
            {
                //Log.i(TAG,"Commande vocal remplacé: " + data[i].toString());
                str = data[i].toString();
            }
        }
        return str;
    }

    /**
     * Retourne la liste des commandes vocal.
     * */

    public static String liste_commandes_vocal(ArrayList list){

        String commandeVocalListe="";
        for(int i = 0; i < list.size(); i++)
        {
            commandeVocalListe = commandeVocalListe + list.get(i);
        }
        return commandeVocalListe;
    }

    public static String dernierMot(String str){
        String string = str;
        String[] parts = string.split(" ");
        String resultat = parts[parts.length-1];
        return resultat; //retour le dernier mot d'une chaine de charactere.
    }

    /**
     * Envoie des commandes vocal au serveur YANA.
     * */
    public static void send_vocal_command(String commande,String text){
        try {
            JSONObject jsonAction = new JSONObject();
            jsonAction.put("action", "CATCH_COMMAND");
            if(text==null){
                jsonAction.put("command", "");
            }
            else {
                jsonAction.put("command", commande);
            }
            jsonAction.put("confidence", "0,95");
            jsonAction.put("text", text);
            send_json_data(jsonAction);
            Log.i("Envoie de la commande", jsonAction.toString());
        } catch (JSONException e){
                e.printStackTrace();
        }

    }

    /**
     * Envoie les données JSON à yana.
     * */
    public static void send_json_data(JSONObject jsonData){
        try {

            out.write(jsonData.toString());
            out.write("<EOF>");
            out.flush();

        } catch (IOException e){
            e.printStackTrace();
            Log.i(TAG," IOexeption3");
        }
    }

    public void showToastMessage(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
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
        });
    }
    public void showProgressBar(final String message){
        handler.post(new Runnable() {
            @Override
            public void run() {
                YANA.progressBar.setMessage(message);
                YANA.progressBar.setCancelable(false);
                YANA.progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                YANA.progressBar.setProgress(0);
                YANA.progressBar.setMax(100);
                YANA.progressBar.show();
                }
        });
    }
    private void sleep(long time){
        try {
            Thread.sleep(time);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private static String sha1(String password) {
        String sha1 = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(password.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sha1;
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static String md5(String password){
        StringBuffer MD5Hash = new StringBuffer();
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(password.getBytes());
            byte messageDigest[] = digest.digest();


            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                MD5Hash.append(h);
            }
        }

        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return MD5Hash.toString();
    }

    private String getTocken(String ip, String user, String password) {
        String token = httpRequest("http://"+ip+"/yana-server/api/?object=user&method=attributes&login=" + user + "&password=" + sha1(md5(password)));

        // Parsing des données JSON
        try {
            JSONObject json_data = new JSONObject(token);
            JSONObject json_user  = json_data.getJSONObject("user");
            token = json_user.getString("token");
            Log.i(TAG, "Méthode de connexion sécurisé. Le token est " + token);
            error = 0;
        } catch (JSONException e) {
            try {
                token = httpRequest("http://"+ip+"/yana-server/action.php?action=GET_TOKEN&login=" + user + "&password=" + URLEncoder.encode(password, "UTF-8"));
                JSONObject json_data = new JSONObject(token);
                token = json_data.getString("token");
                Log.i(TAG, "Méthode de connexion non sécurisé. Le token est " + token);
                error = 0;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Configuration.progressBar.setMessage("Attention !!! Votre serveur Yana n'est pas à jour. Votre mot de passe est transmit en clair sur le réseau, pensez à mettre à jour yana grâce à la commande \"git pull\" pour résoudre ce problème...");
                    }
                });
                sleep(6000);
                Configuration.progressBar.dismiss();
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (JSONException e1) {
                Log.i(TAG, "Méthode de connexion non sécurisé. Impossible de lire le tocken");
                Log.i(TAG, "Méthode de connexion sécurisé. Impossible de lire le tocken");
                error = 1;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Configuration.progressBar.setMessage("Nom d'utililsateur ou mot de passe incorrect...");
                    }
                });
                sleep(3000);
                Configuration.progressBar.dismiss();
            }
            token = "";
        }
        return token;
    }

    private static String getName(String user, String password) {
        String name = httpRequest("http:/"+clientTalk.getInetAddress().toString()+"/yana-server/api/?object=user&method=attributes&login=" + user + "&password=" + sha1(md5(password)));

        // Parsing des données JSON
        try {
            JSONObject json_data = new JSONObject(token);
            JSONObject json_user  = json_data.getJSONObject("user");
            name = json_user.getString("name");
            Log.i(TAG, "Le nom est " + name);
        } catch (JSONException e) {
            Log.e(TAG, "Impossible de lire le nom. Mauvais utilisateur ou mot de passe " + e.toString());
        }
        return name;
    }

    private static String getFirsName(String user, String password) {
        String firstname = httpRequest("http:/"+clientTalk.getInetAddress().toString()+"/yana-server/api/?object=user&method=attributes&login=" + user + "&password=" + sha1(md5(password)));

        // Parsing des données JSON
        try {
            JSONObject json_data = new JSONObject(firstname);
            JSONObject json_user  = json_data.getJSONObject("user");
            firstname = json_user.getString("firstname");
            Log.i(TAG, "Le nom de famille est " + firstname);
        } catch (JSONException e) {
            Log.e(TAG, "Impossible de lire le nom de famille. Mauvais utilisateur ou mot de passe " + e.toString());
        }
        return firstname;
    }

    private static String getMail(String user, String password) {
        String mail = httpRequest("http:/"+clientTalk.getInetAddress().toString()+"/yana-server/api/?object=user&method=attributes&login=" + user + "&password=" + sha1(md5(password)));

        // Parsing des données JSON
        try {
            JSONObject json_data = new JSONObject(mail);
            JSONObject json_user  = json_data.getJSONObject("user");
            mail = json_user.getString("mail");
            Log.i(TAG, "Le mail est " + mail);
        } catch (JSONException e) {
            Log.e(TAG, "Impossible de lire le mail. Mauvais utilisateur ou mot de passe " + e.toString());
        }
        return mail;
    }

    private static String httpRequest(String url){
        String result = "";

        // Envoi de la requête avec HTTPPost
        try {
            HttpClient httpclient = new DefaultHttpClient();
            //HttpPost httppost = new HttpPost("http://"+adresse_ip+"/yana-server/action.php?action=GET_TOKEN&login=" + user + "&password=" + URLEncoder.encode(password, "UTF-8"));
            //HttpPost httppost = new HttpPost("http://"+ip+"/yana-server/api/?object=user&method=attributes&login=" + user + "&password=" + sha1(md5(password)));
            HttpPost httppost = new HttpPost(url);
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();

            //Conversion de la réponse en chaine
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();//ferme le flux en entrer
            result = sb.toString();//retourne des donnes en JSON
        } catch (Exception e) {
            Log.e(TAG, "Error converting result " + e.toString());
        }
        return result;
    }

}
