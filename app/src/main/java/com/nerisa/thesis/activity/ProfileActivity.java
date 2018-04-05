package com.nerisa.thesis.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.nerisa.thesis.R;
import com.nerisa.thesis.constant.Constant;

public class ProfileActivity extends AppCompatActivity {

    private static GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
        String userEmail = preferences.getString(Constant.USER_EMAIL_KEY, "Your email here");
        String userType = (preferences.getBoolean(Constant.USER_CUSTODIAN_KEY, Boolean.FALSE) ? "Custodian" : "Visitor");
        String level = preferences.getString(Constant.LEVEL_KEY,"");

        TextView emailContainer = (TextView) findViewById(R.id.user_email);
        TextView typeContainer = (TextView) findViewById(R.id.user_status);
        TextView noviceContainer = (TextView) findViewById(R.id.novice);
        TextView intermediateContainer = (TextView) findViewById(R.id.intermediate);
        TextView expertContainer = (TextView) findViewById(R.id.expert);

        emailContainer.setText(userEmail);
        typeContainer.setText(userType);
        if(level.equals("NOVICE")){
            noviceContainer.setBackgroundResource(R.drawable.customborder_blue);
            noviceContainer.setTextColor(Color.parseColor("#00BC8C"));
        } else if (level.equals("SEASONED")){
            noviceContainer.setBackgroundResource(R.drawable.customborder_blue);
            noviceContainer.setTextColor(Color.parseColor("#00BC8C"));
            intermediateContainer.setBackgroundResource(R.drawable.customborder_blue);
            intermediateContainer.setTextColor(Color.parseColor("#00BC8C"));
        } else if (level.equals("EXPERT")){
            noviceContainer.setBackgroundResource(R.drawable.customborder_blue);
            noviceContainer.setTextColor(Color.parseColor("#00BC8C"));
            intermediateContainer.setBackgroundResource(R.drawable.customborder_blue);
            intermediateContainer.setTextColor(Color.parseColor("#00BC8C"));
            expertContainer.setBackgroundResource(R.drawable.customborder_blue);
            expertContainer.setTextColor(Color.parseColor("#00BC8C"));
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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
}