package com.nerisa.thesis.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.R;
import com.nerisa.thesis.model.Monument;

import java.io.IOException;
import java.util.List;

public class MonumentInfoActivity extends AppCompatActivity {

    private static final String TAG = MonumentInfoActivity.class.getSimpleName();
    private static GoogleSignInClient mGoogleSignInClient;
    private static Monument monument;
    private static List<Address> addresses;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_monument_info);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent intent = getIntent();
        if (null != intent){
            Log.d("check", "Monument is here");
            monument = intent.getParcelableExtra(Constant.MONUMENT);
            Log.d(TAG, String.valueOf(monument.getName()));
            Log.d(TAG, String.valueOf(monument.getCreator()));
            Log.d(TAG, String.valueOf(monument.getDesc()));
            Log.d(TAG, String.valueOf(monument.getLongitude()));
            Log.d(TAG, String.valueOf(monument.getLatitude()));
            Log.d(TAG, String.valueOf(monument.getMonumentPhoto()));
        }

        Geocoder geocoder = new Geocoder(MonumentInfoActivity.this);
        try{
            addresses = geocoder.getFromLocation(monument.getLatitude(), monument.getLongitude(), 1);
        } catch (IOException e){
            e.printStackTrace();
        }
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(monument.getMonumentPhoto());
//        StorageReference storageReference = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/v0/b/custodian-3e7c1.appspot.com/o/images%2Fb6cad247-9053-4956-82eb-cb9e1c6bd5cb?alt=media&token=ab602c71-302b-4413-b997-2b33ed05b1d9");

        TextView monumentAddress = (TextView) findViewById(R.id.address);
        TextView monumentName = (TextView) findViewById(R.id.name);
//        TextView monumentCreator = (TextView) findViewById(R.id.creator);
        TextView monumentDesc = (TextView) findViewById(R.id.desc);
        ImageView monumentImage = (ImageView) findViewById(R.id.image);

        monumentAddress.setText(addresses.get(0).getAddressLine(0));
        monumentName.setText(monument.getName());
//        monumentCreator.setText(monument.getCreator());
        monumentDesc.setText(monument.getDesc());
        Glide.with(this /* context */)
                .using(new FirebaseImageLoader())
                .load(storageReference)
                .into(monumentImage);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
        Long userMonumentId = sharedPreferences.getLong(Constant.MONUMENT_ID_KEY, 0);
        if(userMonumentId != monument.getId()){
            Button addDataButton = (Button) findViewById(R.id.add_data);
            addDataButton.setVisibility(View.GONE);
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
                                Intent intent = new Intent(MonumentInfoActivity.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showWarnings(View view){
        Intent warningIntent = new Intent(MonumentInfoActivity.this, WarningActivity.class);
        warningIntent.putExtra(Constant.MONUMENT, monument);
        startActivity(warningIntent);
    }

    public void showPosts(View view){
        Intent postsIntent = new Intent(MonumentInfoActivity.this, PostsActivity.class);
        postsIntent.putExtra(Constant.MONUMENT, monument);
        startActivity(postsIntent);
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(MonumentInfoActivity.this,MapsActivity.class);
        startActivity(intent);
        return;
    }

    public void isUserNearby(View view) {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            System.out.println(">>>>>>>>>>>>>>>");
                            if (location != null) {
                                userLocation = location;
                                float[] distanceResult = new float[5];
                                Location.distanceBetween(monument.getLatitude(), monument.getLongitude(), userLocation.getLatitude(), userLocation.getLongitude(), distanceResult);
                                System.out.println(distanceResult[0]);
                                if(distanceResult[0] <= 10.0){
                                    addData();
                                } else {
                                    Toast.makeText(MonumentInfoActivity.this, "Please go near your monument to provide the data", Toast.LENGTH_LONG)
                                            .show();
                                }
                            }
                        }
                    });


        }catch (SecurityException e) {
            Log.d(TAG, "Error: " + e.getMessage());
            Toast.makeText(MonumentInfoActivity.this, "Location permission are not turned on for this app.", Toast.LENGTH_LONG)
                    .show();
        }catch (IllegalArgumentException e) {
            Log.d(TAG, "Error: " + e.getMessage());
            Toast.makeText(MonumentInfoActivity.this, "Please try again later.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void addData(){
        Intent noiseActivity = new Intent(MonumentInfoActivity.this, AddNoiseRecording.class);
        noiseActivity.putExtra(Constant.MONUMENT, monument);
        startActivity(noiseActivity);
    }
}
