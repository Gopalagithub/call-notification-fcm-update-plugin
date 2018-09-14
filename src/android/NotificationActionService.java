package com.gae.scaffolder.plugin;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jayachandra on 31/8/18.
 */

public class NotificationActionService extends IntentService{
    public static SharedPreferences sp;

    public NotificationActionService() {
        super("NotificationActionService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if ("answer".equals(action)) {
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
            startActivity(launchIntent);
            Intent stopIntent = new Intent(this, RingtonePlayingService.class);
            stopService(stopIntent);

            callAnswerAPI(intent.getStringExtra("session_id"));


        }
        if("decline".equals(action)){

            Intent stopIntent = new Intent(this, RingtonePlayingService.class);
            stopService(stopIntent);
            String sessionId = intent.getStringExtra("session_id");
            callDeclineAPI( sessionId );
            //MyCordovaClass myCordova=new MyCordovaClass();
        }
        NotificationManager notificationManager = (NotificationManager)getSystemService(this.NOTIFICATION_SERVICE);
        notificationManager.cancel(2000);

    }

    public void callDeclineAPI(String session_id){
        String url= "https://uat-api.flukeassist.io:443/api/v1/tok_sessions/end_call_mobile.json?session_id="+session_id;
        RequestQueue requestQueue=Volley.newRequestQueue(this);
        StringRequest stringRequest=new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("putMethodErrorResponse",""+error);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    

     public void callAnswerAPI(String session_id){

         String url= "https://uat-api.flukeassist.io:443//api/v1/tok_sessions/join_session_mobile.json?session_id="+session_id;
         RequestQueue requestQueue=Volley.newRequestQueue(this);
         StringRequest stringRequest=new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
             @Override
             public void onResponse(String response) {
                 String mToken="";
                                  try {
                     mToken=new JSONObject(response).getString("token");
                 } catch (JSONException e) {
                     e.printStackTrace();
                 }

                 saveFile(mToken,session_id);
             }

         }, new Response.ErrorListener() {
             @Override
             public void onErrorResponse(VolleyError error) {
                 Log.v("putMethodErrorResponse",""+error.getMessage());
             }
         }){
             @Override
             public Map<String, String> getHeaders() throws AuthFailureError {
                 Map<String,String> params = new HashMap<String, String>();
                 params.put("Content-Type", "application/json");
                // params.put("Authorization", "application/json");
                 return params;
             }
         };
         requestQueue.add(stringRequest);
     }
     private void saveFile(String token,String session_id){
//session_id, token
         String fileContents = "{'session_id':'"+session_id+"','token':'"+token+"'}";
         getTempFile(getApplicationContext(), fileContents);

//         File myDir = new File(getCacheDir(), "folder");
//         myDir.mkdir();
//         try {
//             String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyFolder/";
//             File root = new File(rootPath);
//             if (!root.exists()) {
//                 root.mkdirs();
//             }
//             File f = new File(rootPath + "ra_details.txt");
//             if (f.exists()) {
//                 f.delete();
//             }
//             f.createNewFile();
//
//             FileOutputStream out = new FileOutputStream(f);
//             out.write(fileContents.getBytes());
//             out.flush();
//             out.close();
//         } catch (Exception e) {
//             e.printStackTrace();
//         }


     }
    private File getTempFile(Context context, String fileContents) {
        File file = null;
        try {
            String filePath = getCacheDir().getAbsolutePath();
             file = new File(filePath + "/ra_details.json");
             if (file.exists()) {
                 file.delete();
             }
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
             out.write(fileContents.getBytes());
             out.flush();
             out.close();

            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                Log.v("text",""+text);
                br.close();
            }catch (IOException e) {
                Log.v("",""+e);
            }

        } catch (IOException e) {
            Log.v("",""+e);
        }
        return file;
    }
}
