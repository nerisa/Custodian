package com.nerisa.thesis.activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.adapter.PostAdapter;
import com.nerisa.thesis.adapter.WarningAdapter;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.custodian.R;
import com.nerisa.thesis.model.Monument;
import com.nerisa.thesis.model.Post;
import com.nerisa.thesis.model.Warning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostsActivity extends AppCompatActivity {

    private static final String TAG = MonumentInfoActivity.class.getSimpleName();
    private static GoogleSignInClient mGoogleSignInClient;
    private static Monument monument;
    private static List<Address> addresses;

    private List<Post> postList = new ArrayList<>();
    private RecyclerView recyclerView;
    PostAdapter mAdapter;

    private static Post post;

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

        recyclerView = (RecyclerView) findViewById(R.id.post_recycler_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mAdapter = new PostAdapter(postList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        getPosts();
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

    private void getPosts(){
        //        String url = String
//                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.POST_URL,
//                        monument.getId());
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.POST_URL,
                        "1");

        JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response) {
                        // response
                        Log.d(TAG, response.toString());
                        if(response.length() == 0){
                            RelativeLayout layout = (RelativeLayout) findViewById(R.id.posts_container);
                            layout.setVisibility(View.GONE);
                        } else {
                            TextView noWarningText = (TextView) findViewById(R.id.no_post_content);
                            noWarningText.setVisibility(View.GONE);
                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    Post post = new Gson().fromJson(response.get(i).toString(), Post.class);
                                    postList.add(post);
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

    public void addPost(View v){
//        String url = String
//                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.POST_URL,
//                        monument.getId());
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s" + Constant.POST_URL,
                        "1");

        EditText postDesc = (EditText) findViewById(R.id.new_post_desc);
        post = new Post();
        post.setDesc(postDesc.getText().toString());
        post.setDate(new Date().getTime());
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(post);
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(json);
            Log.d("ppppppp", jsonObj.toString());
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
                        Post post = new Gson().fromJson(response.toString(), Post.class);
                        postList.add(post);
                        mAdapter.notifyDataSetChanged();
                        EditText postDesc = (EditText) findViewById(R.id.new_post_desc);
                        postDesc.setText("");
                        Toast.makeText(PostsActivity.this, "Your post has been added.", Toast.LENGTH_LONG)
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
