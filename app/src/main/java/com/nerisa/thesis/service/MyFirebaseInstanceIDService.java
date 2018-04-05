package com.nerisa.thesis.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.model.User;

import org.json.JSONException;
import org.json.JSONObject;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        // Saving reg id to shared preferences
        storeRegIdInPref(refreshedToken);
        Log.e(TAG, "token refresed" + refreshedToken);

        // sending reg id to your server
        sendRegistrationToServer(refreshedToken);

        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(Constant.REGISTRATION_COMPLETE);
        registrationComplete.putExtra("token", refreshedToken);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF, 0);
        Long userId = pref.getLong(Constant.USER_ID_KEY, 0);
        if(userId != 0) {
            String url = Constant.SERVER_URL + Constant.USER_URL + "/" + userId;
            String userEmail = pref.getString(Constant.USER_EMAIL_KEY, "");
            User user = new User(userEmail, token);

            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(user);
            JSONObject jsonObj = null;
            try {
                jsonObj = new JSONObject(json);
            } catch (JSONException e) {
                Log.d(TAG, "exception");
            }

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.PUT, url, jsonObj,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d(TAG, response.toString());

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error
                    Log.d(TAG, error.toString());
                }
            });
            AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest, "tag");
        }
    }

    private void storeRegIdInPref(String token) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(Constant.USER_TOKEN_KEY, token);
        editor.commit();
    }
}