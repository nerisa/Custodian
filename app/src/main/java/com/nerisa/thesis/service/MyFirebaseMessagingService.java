package com.nerisa.thesis.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nerisa.thesis.activity.MainActivity;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.util.NotificationUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    private NotificationUtils notificationUtils;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage == null)
            return;


        // Check if message contains a notification payload.
        else if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.e(TAG, "Data Payload: " + remoteMessage.getData().toString());

            try {
                JSONObject json = new JSONObject(remoteMessage.getData().toString());
                handleDataMessage(json, remoteMessage.getNotification());
//                handleDataMessage(json);
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }
        }


    }

    private void handleNotification(String title, final String message) {

        if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
            // app is in foreground, broadcast the push message
            Intent pushNotification = new Intent(Constant.PUSH_NOTIFICATION);
            Log.e(TAG, "broadcasted notification");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
                }
            });
            pushNotification.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

        }else{
            // If the app is in background, firebase itself handles the notification
        }
    }

    private void handleDataMessage(JSONObject json, RemoteMessage.Notification notification) {
//    private void handleDataMessage(JSONObject json) {
        Log.d(TAG, "push json: " + json.toString());

        try {
            if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
                // app is in foreground, broadcast the push message
                if(json.getString("type").equals("incentive")){
                    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constant.LEVEL_KEY, json.getString("level"));
                    editor.commit();
                } else {
                    Intent pushNotification = new Intent(Constant.PUSH_NOTIFICATION);
                    pushNotification.putExtra(Constant.DATA, json.getString("monument"));

                    LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);
                }

            } else {
                // app is in background, show the notification in notification tray
                if(json.getString("type").equals("warning")) {
                    Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                    resultIntent.putExtra(Constant.DATA, json.getString("monument"));

                    showNotificationMessage(getApplicationContext(), notification.getTitle(), notification.getBody(), json.getLong("timeStamp"), resultIntent);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Json Exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    /**
     * Showing notification with text only
     */
    private void showNotificationMessage(Context context, String title, String message, Long timeStamp, Intent intent) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent);
    }
}
