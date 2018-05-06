package com.nerisa.thesis.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.R;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.model.Monument;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private static GoogleSignInClient mGoogleSignInClient;
    SharedPreferences preferences;
    private static final String TAG = ProfileActivity.class.getSimpleName();
    private static Monument monument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    }

    @Override
    public void onStart(){
        super.onStart();
        preferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
        Boolean isCustodian = preferences.getBoolean(Constant.USER_CUSTODIAN_KEY, Boolean.FALSE);
        String userEmail = preferences.getString(Constant.USER_EMAIL_KEY, "Your email here");
        String userType = (isCustodian ? "Custodian" : "Visitor");
        String level = preferences.getString(Constant.LEVEL_KEY,"");

        TextView emailContainer = (TextView) findViewById(R.id.user_email);
        TextView typeContainer = (TextView) findViewById(R.id.user_status);
        TextView noviceContainer = (TextView) findViewById(R.id.novice);
        TextView intermediateContainer = (TextView) findViewById(R.id.intermediate);
        TextView expertContainer = (TextView) findViewById(R.id.expert);

        emailContainer.setText(userEmail);
        typeContainer.setText(userType);
        if(isCustodian) {
            if (level.equals("NOVICE")) {
                noviceContainer.setBackgroundResource(R.drawable.customborder_blue);
                noviceContainer.setTextColor(Color.parseColor("#00BC8C"));
            } else if (level.equals("SEASONED")) {
                noviceContainer.setBackgroundResource(R.drawable.customborder_blue);
                noviceContainer.setTextColor(Color.parseColor("#00BC8C"));
                intermediateContainer.setBackgroundResource(R.drawable.customborder_blue);
                intermediateContainer.setTextColor(Color.parseColor("#00BC8C"));
            } else if (level.equals("EXPERT")) {
                noviceContainer.setBackgroundResource(R.drawable.customborder_blue);
                noviceContainer.setTextColor(Color.parseColor("#00BC8C"));
                intermediateContainer.setBackgroundResource(R.drawable.customborder_blue);
                intermediateContainer.setTextColor(Color.parseColor("#00BC8C"));
                expertContainer.setBackgroundResource(R.drawable.customborder_blue);
                expertContainer.setTextColor(Color.parseColor("#00BC8C"));
            }
            TextView noCustodian = (TextView) findViewById(R.id.not_custodian);
            noCustodian.setVisibility(View.INVISIBLE);
            getMonumentData();
        } else {
            LinearLayout monumentInfoWrapper = (LinearLayout) findViewById(R.id.monument_info_wrapper);
            monumentInfoWrapper.setVisibility(View.INVISIBLE);
        }
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
                                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(ProfileActivity.this,MapsActivity.class);
        startActivity(intent);
        return;
    }

    private void getMonumentData(){
        final Long monumentId = preferences.getLong(Constant.MONUMENT_ID_KEY,0);
        String url = Constant.SERVER_URL + Constant.MONUMENT_URL + "/" + monumentId;
        Log.d(TAG,"Getting monument id with url: " + url);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d(TAG, "Got response for monument: " + response.toString());
                        monument = Monument.mapResponse(response);
                        showMonumentInformation(response);

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

    private void showMonumentInformation(JSONObject response){
        TextView name = (TextView) findViewById(R.id.name);
        TextView address = (TextView) findViewById(R.id.address);
        TextView warningCount = (TextView) findViewById(R.id.warning_count);
        TextView postCount = (TextView) findViewById(R.id.post_count);
        TextView notCustodian = (TextView) findViewById(R.id.no_monument);

        name.setText(monument.getName());
        Geocoder geocoder = new Geocoder(ProfileActivity.this);
        List<Address> addresses = null;
        try{
            addresses = geocoder.getFromLocation(monument.getLatitude(), monument.getLongitude(), 1);
        } catch (IOException e){
            e.printStackTrace();
        }
        address.setText(addresses.get(0).getAddressLine(0));
        try {
            warningCount.setText("Total Warnings: " + response.getInt("warningCount"));
            postCount.setText("Total Posts: " + response.getInt("postCount"));
        } catch (JSONException e){

        }
        notCustodian.setVisibility(View.GONE);
    }

    public void addInformation(View view){
        Intent monumnentInfo = new Intent(ProfileActivity.this, MonumentInfoActivity.class);
        monumnentInfo.putExtra(Constant.MONUMENT, monument);
        startActivity(monumnentInfo);
    }
}