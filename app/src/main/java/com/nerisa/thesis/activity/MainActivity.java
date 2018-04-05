package com.nerisa.thesis.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.R;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.model.User;
import com.nerisa.thesis.util.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static GoogleSignInClient mGoogleSignInClient;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;

    View progressBarHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        findViewById(R.id.sign_in_button).setOnClickListener(this);
//        findViewById(R.id.skip_button).setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        progressBarHolder = findViewById(R.id.progress_overlay);

        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(account != null && currentUser != null){
            if (getIntent().getExtras() != null && getIntent().getExtras().getString("type") != null) {
                Log.d(TAG, "Got a notification for " + getIntent().getExtras().getString("type"));
                if(getIntent().getExtras().getString("type").equals("warning")) {
                    Long monumentId = Long.valueOf(getIntent().getExtras().getString("monument"));
                    Intent notification = new Intent(MainActivity.this, NotificationActivity.class);
                    notification.putExtra(Constant.MONUMENT, monumentId);
                    startActivity(notification);
                } else if (getIntent().getExtras().getString("type").equals("incentive")){
                    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constant.LEVEL_KEY, getIntent().getExtras().getString("level"));
                    editor.commit();
                    Intent profile = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(profile);

                }

            } else {
                showMap();
            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                startSignIn();
                break;
        }
    }

    public void startSignIn(){
        Log.d(TAG, "Starting the sign in process");


        Utility.animateView(progressBarHolder, View.VISIBLE, 0.4f, 200);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void showMap(){
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);

        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {

            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG,"User signed in successfully for " + account.getEmail());
            firebaseAuthWithGoogle(account);
            // Signed in successfully, show authenticated UI.

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());

            Utility.animateView(progressBarHolder, View.GONE, 0 ,200);

            Toast toast = Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            sendNewUserDataToServer(user);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            Utility.animateView(progressBarHolder, View.GONE, 0 ,200);
                            Toast toast = Toast.makeText(MainActivity.this, "Login Failed", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });
    }

    private void sendNewUserDataToServer(FirebaseUser user){
        Log.d(TAG, "Sending signed in user's data to server: " + user.getEmail());
        User newUser = new User(user.getEmail(), FirebaseInstanceId.getInstance().getToken());

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(newUser);
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(json);
        } catch (JSONException e){
            Log.d(TAG,"exception");
        }

        String url = Constant.SERVER_URL + Constant.USER_URL;
        System.out.println(jsonObj);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObj,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response

                        Log.d(TAG, "User registered with the server: " + response.toString());
                        storeUserData(response);
                        Utility.animateView(progressBarHolder, View.GONE, 0 ,200);
                        showMap();

                    }
                }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error) {
                // error
                Log.d(TAG, "Error sending user data to server:" + error.getStackTrace());
                FirebaseAuth.getInstance().signOut();
                mGoogleSignInClient.signOut();
                Utility.animateView(progressBarHolder, View.GONE, 0 ,200);

            }
        });
        AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest,"tag");
    }

    private void storeUserData(JSONObject response) {

        User user = new Gson().fromJson(response.toString(), User.class);
        Log.d(TAG, "Storing user's data in the app: " + user.getEmail());
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(Constant.USER_TOKEN_KEY, user.getToken());
        editor.putLong(Constant.USER_ID_KEY, user.getId());
        editor.putBoolean(Constant.USER_CUSTODIAN_KEY, user.isCustodian());
        editor.putString(Constant.USER_EMAIL_KEY, user.getEmail());
        if(user.isCustodian()){
            editor.putLong(Constant.MONUMENT_ID_KEY, user.getMonumentId());
        }
        editor.commit();

    }
}
