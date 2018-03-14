package com.nerisa.thesis.activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.custodian.R;
import com.nerisa.thesis.model.Monument;

import java.io.IOException;
import java.util.List;

public class PostsActivity extends AppCompatActivity {

    private static final String TAG = MonumentInfoActivity.class.getSimpleName();
    private static GoogleSignInClient mGoogleSignInClient;
    private static Monument monument;
    private static List<Address> addresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);
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



        Geocoder geocoder = new Geocoder(PostsActivity.this);
        try{
            addresses = geocoder.getFromLocation(monument.getLatitude(), monument.getLongitude(), 1);
        } catch (IOException e){
            e.printStackTrace();
        }
        FirebaseStorage storage = FirebaseStorage.getInstance();
        Log.d("check this.............", monument.getMonumentPhoto());
        StorageReference storageReference = storage.getReferenceFromUrl(monument.getMonumentPhoto());
//        StorageReference storageReference = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/v0/b/custodian-3e7c1.appspot.com/o/images%2Fb6cad247-9053-4956-82eb-cb9e1c6bd5cb?alt=media&token=ab602c71-302b-4413-b997-2b33ed05b1d9");

        TextView monumentAddress = (TextView) findViewById(R.id.address);
        TextView monumentName = (TextView) findViewById(R.id.name);
        TextView monumentCreator = (TextView) findViewById(R.id.creator);
        ImageView monumentImage = (ImageView) findViewById(R.id.image);

        monumentAddress.setText(addresses.get(0).getAddressLine(0));
        monumentName.setText(monument.getName());
        monumentCreator.setText(monument.getCreator());
        Glide.with(this /* context */)
                .using(new FirebaseImageLoader())
                .load(storageReference)
                .into(monumentImage);
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
                                Intent intent = new Intent(PostsActivity.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}