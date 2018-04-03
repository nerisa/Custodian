package com.nerisa.thesis.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.R;
import com.nerisa.thesis.model.Monument;
import com.nerisa.thesis.model.Warning;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class WarningInfoActivity extends AppCompatActivity {

    private static GoogleSignInClient mGoogleSignInClient;
    private static Warning warning;
    private static Monument monument;
    private static final String TAG = WarningInfoActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_info);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent intent = getIntent();
        if (null != intent){
            if(intent.getExtras().containsKey(Constant.UNVERIFIED_WARNING)){
                getMonument();
            } else {
                monument = intent.getParcelableExtra(Constant.MONUMENT);
            }
            warning = intent.getParcelableExtra(Constant.WARNING);
            System.out.println(warning.isVerified());
        }

        TextView date = (TextView) findViewById(R.id.date);
        ImageView imageView = (ImageView) findViewById(R.id.image);
        LinearLayout buttonWrapper = (LinearLayout) findViewById(R.id.action_button_wrapper);
        if(warning.isVerified()){

            buttonWrapper.setVisibility(View.GONE);
        }
        TextView desc = (TextView) findViewById(R.id.desc);
        date.setText(new Date(warning.getDate()).toString());
        desc.setText(warning.getDesc());

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(warning.getImage());
        Glide.with(this /* context */)
                .using(new FirebaseImageLoader())
                .load(storageReference)
                .into(imageView);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.profile:
                intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.notification:
                intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);
                return true;
            case R.id.sign_out:
                FirebaseAuth.getInstance().signOut();
                mGoogleSignInClient.signOut()
                        .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                // [START_EXCLUDE]
                                Intent intent = new Intent(WarningInfoActivity.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void verifyWarning(View v){
        changeWarningStatus(true);
    }

    public void falseWarning(View v){
        changeWarningStatus(false);
    }

    private void changeWarningStatus(boolean status){
        warning.setVerify(status);
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.WARNING_URL + "/%2$s",
                        monument.getId(), warning.getId());
        Log.d(TAG, "Changing the warning status using url: " + url);

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(warning);
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(json);
            Log.d(TAG, "warning change request: " + jsonObj.toString());
        } catch (JSONException e){
            Log.d(TAG,"exception");
        }

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.PUT, url, jsonObj,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d(TAG, "Response:" + response.toString());
                        Intent intent = new Intent(WarningInfoActivity.this, MonumentInfoActivity.class);
                        intent.putExtra(Constant.MONUMENT, monument);
                        startActivity(intent);
                        Toast.makeText(WarningInfoActivity.this, "The status of the warning has been changed.", Toast.LENGTH_LONG)
                                .show();
                    }
                }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error) {
                // error
                Log.d(TAG, error.toString());
            }
        });
        AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest,"tag");

    }

    private void getMonument(){
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
        Long monumentId = sharedPreferences.getLong(Constant.MONUMENT_ID_KEY,0);
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENT_URL + "/%1$s",
                        monumentId.toString());
        Log.d(TAG, "Getting monument details with url: " + url);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d(TAG, "Got response for monument: " + response.toString());
                        monument = Monument.mapResponse(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // error
                Log.e("Error.Response", error.toString());
            }
        });
        AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest, "tag");
    }
}
