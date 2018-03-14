package com.nerisa.thesis.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
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
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.adapter.WarningAdapter;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.custodian.R;
import com.nerisa.thesis.model.Monument;
import com.nerisa.thesis.model.Warning;
import com.nerisa.thesis.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class WarningActivity extends AppCompatActivity {

    private static final String TAG = WarningActivity.class.getSimpleName();
    private static GoogleSignInClient mGoogleSignInClient;
    private static Monument monument;
    private static List<Address> addresses;

    private List<Warning> warningList = new ArrayList<>();
    private RecyclerView recyclerView;
    WarningAdapter mAdapter;

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private ImageView mImageView;
    private String userChoosenTask;
    private static Uri warningImageUri;
    private StorageReference mStorageRef;
    private static Warning warning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning);

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



        Geocoder geocoder = new Geocoder(WarningActivity.this);
        try{
            addresses = geocoder.getFromLocation(monument.getLatitude(), monument.getLongitude(), 1);
        } catch (IOException e){
            e.printStackTrace();
        }
        FirebaseStorage storage = FirebaseStorage.getInstance();
        Log.d("check this.............", monument.getMonumentPhoto());
//        StorageReference storageReference = storage.getReferenceFromUrl(monument.getMonumentPhoto());
        StorageReference storageReference = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/v0/b/custodian-3e7c1.appspot.com/o/images%2Fb6cad247-9053-4956-82eb-cb9e1c6bd5cb?alt=media&token=ab602c71-302b-4413-b997-2b33ed05b1d9");

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

        recyclerView = (RecyclerView) findViewById(R.id.warning_recycler_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mAdapter = new WarningAdapter(Glide.with(this), warningList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        getWarnings();

        mImageView = (ImageView) findViewById(R.id.new_warning_image);
        mStorageRef = FirebaseStorage.getInstance().getReference();
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
                                Intent intent = new Intent(WarningActivity.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void getWarnings(){
//        String url = String
//                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.WARNING_URL,
//                        monument.getId());
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.WARNING_URL,
                        "1");

        JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response) {
                        // response
                        Log.d(TAG, response.toString());
                        if(response.length() == 0){
                            RelativeLayout layout = (RelativeLayout) findViewById(R.id.warnings_container);
                            layout.setVisibility(View.GONE);
                        } else {
                            TextView noWarningText = (TextView) findViewById(R.id.no_warning_content);
                            noWarningText.setVisibility(View.GONE);
                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    Warning warning = new Gson().fromJson(response.get(i).toString(), Warning.class);
                                    warningList.add(warning);
                                } catch (JSONException e) {
                                    Log.w(TAG, "Could not parse json repsonse");
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error) {
                // error
                Log.d("Error.Response", error.toString());
            }
        });
        AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest,"tag");
    }

//    =========================photo select===============================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(userChoosenTask.equals("Take Photo"))
                    cameraIntent();
                else if(userChoosenTask.equals("Choose from Library"))
                    galleryIntent();
            } else {
                //code for deny
            }
        }
    }

    public void selectImage(View imageButton) {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(WarningActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=Utility.checkStoragePermission(WarningActivity.this);

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
        warningImageUri = Uri.fromFile(destination);
        mImageView.setImageBitmap(thumbnail);
    }

    private void onSelectFromGalleryResult(Intent data) {
        try {
            if (data != null) {
                Log.d(TAG, "data was not null");
                warningImageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), warningImageUri);
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




    public void addWarning(View v){
        Log.d(TAG, "button clicked");

        EditText warningDesc = (EditText) findViewById(R.id.new_warning_desc);
        warning = new Warning();
        warning.setDesc(warningDesc.getText().toString());
        warning.setDate(new Date());

        uploadImageToFirebase();
            return;
    }

    private void uploadImageToFirebase(){
        Log.d(TAG, "image upload was called");
        StorageReference imageRef = mStorageRef.child("images/" + UUID.randomUUID().toString());
        imageRef.putFile(warningImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        warning.setImage(downloadUrl.toString());
                        Log.d(TAG, "image upload was finished");
                        uploadWarningData();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(WarningActivity.this, "The warning photo failed to upload. Please try again later.", Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    private void uploadWarningData(){
//        String url = String
//                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.WARNING_URL,
//                        monument.getId());
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.WARNING_URL,
                        "1");
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(monument);
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(json);
        } catch (JSONException e){
            Log.d(TAG,"exception");
        }

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObj,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d(TAG, response.toString());
                        finish();
                        startActivity(getIntent());
                        Toast.makeText(WarningActivity.this, "The warning has been added. The custodian will be notified to verify it.", Toast.LENGTH_LONG)
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
}
