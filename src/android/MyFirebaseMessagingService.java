package com.gae.scaffolder.plugin;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.gae.scaffolder.plugin.FCMPluginActivity;

/**
 * Created by Felipe Echanique on 08/06/2016.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMPlugin";
    private static final String ANSWER = "answer";
    private static final String DECLINE = "decline";
    
    public MyFirebaseMessagingService(){
        super();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "==> MyFirebaseMessagingService onMessageReceived");
		
		if( remoteMessage.getNotification() != null){
			Log.d(TAG, "\tNotification Title: " + remoteMessage.getNotification().getTitle());
			Log.d(TAG, "\tNotification Message: " + remoteMessage.getNotification().getBody());
		}
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("wasTapped", false);
		for (String key : remoteMessage.getData().keySet()) {
                Object value = remoteMessage.getData().get(key);
                Log.d(TAG, "\tKey: " + key + " Value: " + value);
				data.put(key, value);
        }
		
		Log.d(TAG, "\tNotification Data: " + data.toString());
        FCMPlugin.sendPushPayload( data );
    }
    // [END receive_message]

    @Override
    public void handleIntent(Intent intent) {

        if(intent.getStringExtra("session_id") != null) {
            Log.d("isAppBackground",""+isAppIsInBackground(getApplicationContext()));
            if(isAppIsInBackground(getApplicationContext())){
                String callType = "Audio Call";
                String callButtonString = "ACCEPT AUDIO CALL";
                if( intent.getStringExtra("call_type").equals("video")){
                    callType = "Video Call";
                    callButtonString = "ACCEPT VIDEO CALL";
                }
                
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

                Intent answerIntent = new Intent(getApplicationContext(), NotificationActionService.class).setAction(ANSWER);
                answerIntent.putExtra("session_id",""+intent.getStringExtra("session_id"));
                PendingIntent answerPendingIntent = PendingIntent.getService(this, 0, answerIntent, PendingIntent.FLAG_ONE_SHOT);

                Intent declineIntent = new Intent(getApplicationContext(), NotificationActionService.class).setAction(DECLINE);
                PendingIntent declinePendingIntent = PendingIntent.getService(this, 0, declineIntent, PendingIntent.FLAG_ONE_SHOT);

                Notification.Builder notificationBuilder = new Notification.Builder(this)
                        .setSmallIcon(getApplicationInfo().icon)
                        .setContentTitle(intent.getStringExtra("title"))
                        .setContentText(callType)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .addAction(getApplicationInfo().icon, callButtonString , answerPendingIntent)
                        .addAction(getApplicationInfo().icon, "DECLINE", declinePendingIntent);

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2000, notificationBuilder.build());

                Intent startIntent = new Intent(this, RingtonePlayingService.class);
                this.startService(startIntent);
            }else{
                super.handleIntent(intent);
            }
            

        }
    }

    private boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }



    private void removeFirebaseOrigianlNotificaitons() {

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null )
            return;

        //check api level for getActiveNotifications()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        //check there are notifications
        StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
        if (activeNotifications == null)
            return;

        //remove all notification created by library(super.handleIntent(intent))
        for (StatusBarNotification tmp : activeNotifications) {
            String tag = tmp.getTag();
            int id = tmp.getId();
            if (tag != null && tag.contains("FCM-Notification"))
                notificationManager.cancel(tag, id);
        }
    }
}
