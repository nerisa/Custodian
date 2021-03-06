package com.nerisa.thesis.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.R;
import com.nerisa.thesis.model.Monument;
import com.nerisa.thesis.util.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class AddMonumentActivity extends AppCompatActivity {

    private static final String TAG = AddMonumentActivity.class.getSimpleName();

    private Monument monument;

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private ImageView mImageView;
    private String userChoosenTask;
    private static Uri monumentImageUri;

    private List<Address> addresses;
    private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?APPID=f4e5b61f71a2ee7595ffa19d67e8aea2&units=metric";

    private static GoogleSignInClient mGoogleSignInClient;

    private static String mNoiseFileName = null;
    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;
    private PlayButton   mPlayButton = null;
    private MediaPlayer mPlayer = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;

    private StorageReference mStorageRef;

    private static boolean isImageUploadDone = false;
    private static boolean isAudioUploadDone = false;

    View progressBarHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (null != intent){
            monument = intent.getParcelableExtra(Constant.MONUMENT);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Record to the external cache directory for visibility
        mNoiseFileName = getExternalCacheDir().getAbsolutePath();

        setContentView(R.layout.activity_add_monument);

        Geocoder geocoder = new Geocoder(AddMonumentActivity.this);
        try{
            addresses = geocoder.getFromLocation(monument.getLatitude(), monument.getLongitude(), 1);
        } catch (IOException e){
            e.printStackTrace();
        }
        TextView monumentAddress = (TextView) findViewById(R.id.monument_location);
        monumentAddress.setText(addresses.get(0).getAddressLine(0));

        getTemperature(monument.getLatitude(), monument.getLongitude());

        if (intent.hasExtra(Constant.MONUMENT_INFO_PRESENT)){
            EditText monumentName = (EditText) findViewById(R.id.monument_name);
//            EditText monumentCreator = (EditText) findViewById(R.id.monument_creator);
            EditText monumentDesc  = (EditText) findViewById(R.id.monument_desc);
            monumentName.setText(monument.getName());
            monumentDesc.setText(monument.getDesc());
        }

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
//        setContentView(ll);

        mImageView = (ImageView) findViewById(R.id.monument_view);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        progressBarHolder = findViewById(R.id.progress_overlay);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null){
            Intent intent = new Intent(AddMonumentActivity.this, MainActivity.class);
            startActivity(intent);
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
                                Intent intent = new Intent(AddMonumentActivity.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Record to the external cache directory for visibility
//        mNoiseFileName = getExternalCacheDir().getAbsolutePath();
//        LinearLayout ll = (LinearLayout) findViewById(R.id.voice_layout);
//        mRecordButton = new RecordButton(this);
//        ll.addView(mRecordButton,
//                new LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        0));
//        mPlayButton = new PlayButton(this);
//        ll.addView(mPlayButton,
//                new LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        0));
//        setContentView(ll);

        mImageView = (ImageView) findViewById(R.id.monument_view);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
            case Utility.REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    permissionToRecordAccepted = Boolean.TRUE;
                    startRecording();
                    break;
                }
        }
    }

//========================Monument Image===================================
    public void selectImage(View imageButton) {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(AddMonumentActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=Utility.checkStoragePermission(AddMonumentActivity.this);

                if (items[item].equals("Take Photo")) {
                    userChoosenTask ="Take Photo";
                    if(result)
                        cameraIntent();

                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask ="Choose from Library";
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent()
    {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
        }
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        monumentImageUri = Uri.fromFile(destination);
        mImageView.setImageBitmap(thumbnail);
    }

    private void onSelectFromGalleryResult(Intent data) {
        try {
            if (data != null) {
                Log.d(TAG, "data was not null");
                monumentImageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), monumentImageUri);
                mImageView.setImageBitmap(bitmap);
            } else {
                Log.e(TAG, "no image selected");
                Toast.makeText(this, "Please try again", Toast.LENGTH_LONG)
                        .show();
            }
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "Please try again", Toast.LENGTH_LONG)
                    .show();
        }
    }

//    =======================Record Audio=======================================

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
        boolean result = Utility.checkAudioPermission(AddMonumentActivity.this);
        if(result) {
            mRecorder = new MediaRecorder();
            mNoiseFileName += System.currentTimeMillis() + ".3gp";
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mNoiseFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
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
                permissionToRecordAccepted = Utility.checkAudioPermission(AddMonumentActivity.this);
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

    public void modifyAddress(View view){
        Intent goBack = new Intent(this, MapsActivity.class);
        startActivity(goBack);
    }

    private void getTemperature(double latitude, double longitude){
        String tag_json_obj = "json_obj_req";
        String url = WEATHER_API_URL + "&lat=" + latitude + "&lon=" + longitude;
        final TextView temperatureView = (TextView) findViewById(R.id.temperature);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        try {
                            JSONObject tempResponse = (JSONObject) response.get("main");
                            Double temperature = tempResponse.getDouble("temp");
                            monument.setTemperature(temperature);
                            temperatureView.setText(Math.round(monument.getTemperature()) + " \u00b0C");
                            Log.d(TAG,monument.getTemperature().toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                // hide the progress dialog
            }

        });
        // Adding request to request queue
        AppController.getInstance(getApplicationContext()).addToRequestQueue(jsonObjReq,tag_json_obj);
    }


    public void addMonument(View button){
        Log.d(TAG, "Adding monument");

        EditText monumentName = (EditText) findViewById(R.id.monument_name);
//        EditText monumentCreator = (EditText) findViewById(R.id.monument_creator);
        EditText monumentDesc  = (EditText) findViewById(R.id.monument_desc);

        monument.setName(monumentName.getText().toString());
//        monument.setCreator(monumentCreator.getText().toString());
        monument.setDesc(monumentDesc.getText().toString());
        isAudioUploadDone = Boolean.FALSE;
        isImageUploadDone = Boolean.FALSE;
        Utility.animateView(progressBarHolder, View.VISIBLE, 0.4f, 200);
        uploadImageToFirebase();
        uploadAudioToFirebase();
        return;
    }

    private void uploadImageToFirebase(){
        Log.d(TAG, "image upload was called");
        StorageReference imageRef = mStorageRef.child("images/" + UUID.randomUUID().toString());
        imageRef.putFile(monumentImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        monument.setMonumentPhoto(downloadUrl.toString());
                        Log.d(TAG, "image upload was finished");
                        isImageUploadDone = true;
                        uploadMonumentData();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(AddMonumentActivity.this, "The monument photo failed to upload. Please try again later.", Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    private void uploadAudioToFirebase(){
        Log.d(TAG, "audio upload was called");

        StorageReference audioRef = mStorageRef.child("audio/" + UUID.randomUUID().toString());
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/3gpp")
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
                        isAudioUploadDone = true;
                        uploadMonumentData();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(AddMonumentActivity.this, "The monument noise profile failed to upload. Please try again later.", Toast.LENGTH_LONG)
                                .show();
                    }
                });

    }

    private void uploadMonumentData(){
        if(isAudioUploadDone && isImageUploadDone) {
            Log.d(TAG, "Sending monument data to server:");
            Log.d(TAG, ">>>>>>>>>>>>>monument data>>>>>>>>>>>>>>>>");
            Log.d(TAG, "name: " + String.valueOf(monument.getName()));
            Log.d(TAG, "creator: " + String.valueOf(monument.getCreator()));
            Log.d(TAG, "desc: " + String.valueOf(monument.getDesc()));
            Log.d(TAG, "long: " + String.valueOf(monument.getLongitude()));
            Log.d(TAG, "lat: " + String.valueOf(monument.getLatitude()));
            Log.d(TAG, "photo: " + String.valueOf(monument.getMonumentPhoto()));
            Log.d(TAG, "noise: " + String.valueOf(monument.getNoiseRecording()));
            Log.d(TAG, "temperature: " + String.valueOf(monument.getTemperature()));


            SharedPreferences pref = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF, 0);
            monument.setUserId(pref.getLong(Constant.USER_ID_KEY, 0));
            Log.d(TAG, "user: " + monument.getUserId());
            Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

            JSONObject jsonObj = monument.createJsonToSendToServer();
            Log.d(TAG, "Monument post request: " + jsonObj.toString());
            String url = Constant.SERVER_URL + Constant.MONUMENT_URL;
            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObj,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d(TAG, "Monument added successfully for this user");
                            Log.d(TAG, response.toString());
                            handleCreateMonumentResponse(response);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error
                    Log.d(TAG, "Error: " + error.getMessage());
                    Utility.animateView(progressBarHolder, View.GONE, 0 ,200);
                    Toast.makeText(AddMonumentActivity.this, "The monument could not be added. Please try again later.", Toast.LENGTH_LONG)
                            .show();
                }
            });
            postRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constant.VOLLEY_TIMEOUT_MS,
                    Constant.VOLLEY_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest, "tag");
        }
    }

    private void handleCreateMonumentResponse(JSONObject response){
        Monument monument = Monument.mapResponse(response);

        Log.d(TAG, "Saving monument data for user_id, monument_id:" + monument.getUserId()+ "," +monument.getId());
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(Constant.USER_CUSTODIAN_KEY, Boolean.TRUE);
        editor.putLong(Constant.MONUMENT_ID_KEY, monument.getId());
        editor.apply();
        Utility.animateView(progressBarHolder, View.GONE, 0 ,200);
        Intent monumentInfo = new Intent(AddMonumentActivity.this, MonumentInfoActivity.class);
        monumentInfo.putExtra(Constant.MONUMENT, monument);
        startActivity(monumentInfo);
    }

}
