package com.nerisa.thesis.activity;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.R;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.model.Monument;
import com.nerisa.thesis.model.NoiseData;
import com.nerisa.thesis.util.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AddNoiseRecording extends AppCompatActivity {

    private static final String TAG = AddNoiseRecording.class.getSimpleName();
    Monument monument;
    private static String mNoiseFileName = null;
    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;
    private PlayButton mPlayButton = null;
    private MediaPlayer mPlayer = null;
    private static GoogleSignInClient mGoogleSignInClient;
    private static List<Address> addresses;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;

    private StorageReference mStorageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_noise_recording);

        LinearLayout ll = (LinearLayout) findViewById(R.id.voice_layout);
        mRecordButton = new RecordButton(this);
        ll.addView(mRecordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        mPlayButton = new PlayButton(this);
        ll.addView(mPlayButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));

        mStorageRef = FirebaseStorage.getInstance().getReference();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent intent = getIntent();
        if (null != intent){
            monument = intent.getParcelableExtra(Constant.MONUMENT);
        }

        Geocoder geocoder = new Geocoder(AddNoiseRecording.this);
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
        ImageView monumentImage = (ImageView) findViewById(R.id.image);

        monumentAddress.setText(addresses.get(0).getAddressLine(0));
        monumentName.setText(monument.getName());
//        monumentCreator.setText(monument.getCreator());
        Glide.with(this /* context */)
                .using(new FirebaseImageLoader())
                .load(storageReference)
                .into(monumentImage);
        mNoiseFileName = getExternalCacheDir().getAbsolutePath();
    }

    public void uploadAudioToFirebase(View view){
        Log.d(TAG, "audio upload was called");

        StorageReference audioRef = mStorageRef.child("audio/" + UUID.randomUUID().toString());
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("audio/3gpp")
                .build();
        Uri audioUri = Uri.fromFile(new File(mNoiseFileName));
        audioRef.putFile(audioUri, metadata)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        monument.setNoiseRecording(downloadUrl.toString());
                        Log.d(TAG, "audio upload was finished");
                        uploadNoiseData(downloadUrl.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(AddNoiseRecording.this, "The monument noise profile failed to upload. Please try again later.", Toast.LENGTH_LONG)
                                .show();
                    }
                });

    }

    private void onRecord(boolean start) {

        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mNoiseFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        boolean result = Utility.checkAudioPermission(AddNoiseRecording.this);
        if(result) {
            mRecorder = new MediaRecorder();
            mNoiseFileName += System.currentTimeMillis() + ".3gp";
            System.out.println(mNoiseFileName);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mNoiseFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "prepare() failed");
            }

            mRecorder.start();
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    class RecordButton extends android.support.v7.widget.AppCompatButton {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG + "/RecordButton", "Record clicked");
                permissionToRecordAccepted = Utility.checkAudioPermission(AddNoiseRecording.this);
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends android.support.v7.widget.AppCompatButton {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void uploadNoiseData(String fileName){
        NoiseData noiseData = new NoiseData();
        noiseData.setFile(fileName);
        noiseData.setDate(new Date().getTime());

        String url = Constant.SERVER_URL + Constant.MONUMENT_URL + "/" + monument.getId() + Constant.NOISE_URL;
        Log.d(TAG,"Posting noise data using url: " + url);


        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(noiseData);
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(json);
        } catch (JSONException e){
            Log.d(TAG,"exception");
        }

        Log.d(TAG, "JSON body for request: " + jsonObj.toString());
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d(TAG, "Noise data added successfully for this monument");
                        Intent intent = new Intent(AddNoiseRecording.this, MonumentInfoActivity.class);
                        intent.putExtra(Constant.MONUMENT, monument);
                        startActivity(intent);
                        Toast.makeText(AddNoiseRecording.this, "Data added successfully", Toast.LENGTH_LONG)
                                .show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // error
                Log.d(TAG, "Error: " + error.getMessage());
            }
        });
        AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest, "tag");
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
                                Intent intent = new Intent(AddNoiseRecording.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
