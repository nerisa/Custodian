package com.nerisa.thesis.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.bumptech.glide.Glide;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.R;
import com.nerisa.thesis.adapter.RecyclerTouchListener;
import com.nerisa.thesis.adapter.VerticalWarningAdapter;
import com.nerisa.thesis.adapter.WarningAdapter;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.model.Warning;
import com.nerisa.thesis.util.NotificationUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = NotificationActivity.class.getSimpleName();

    private List<Warning> warningList = new ArrayList<>();
    private RecyclerView recyclerView;
    VerticalWarningAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerView = (RecyclerView) findViewById(R.id.warning_recycler_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mAdapter = new VerticalWarningAdapter(Glide.with(this), warningList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Warning warning = warningList.get(position);
//                Toast.makeText(getApplicationContext(), warning.getDesc() + " is selected!", Toast.LENGTH_SHORT).show();
                showWarningDetails(warning);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        getWarnings();

    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(NotificationActivity.this,MapsActivity.class);
        startActivity(intent);
        return;
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    private void getWarnings(){
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PREF,0);
        Boolean isCustodian = sharedPreferences.getBoolean(Constant.USER_CUSTODIAN_KEY, Boolean.FALSE);
        Log.d(TAG,String.valueOf(sharedPreferences.getLong(Constant.MONUMENT_ID_KEY, 0)));
        Log.d(TAG,isCustodian.toString());

        if(isCustodian) {
            Long monumentId = sharedPreferences.getLong(Constant.MONUMENT_ID_KEY, 0);
            String url = String
                    .format(Constant.SERVER_URL + Constant.MONUMENT_URL + "/%1$s" + Constant.WARNING_LIST_URL,
                            monumentId);


            JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            // response
                            Log.d(TAG, response.toString());

                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    System.out.println(response.get(i).toString());
                                    Warning warning = new Gson().fromJson(response.get(i).toString(), Warning.class);
                                    if (!warning.isVerified()) {
                                        System.out.println("here");
                                        warningList.add(warning);
                                    }
                                } catch (JSONException e) {
                                    Log.w(TAG, "Could not parse json repsonse");
                                }
                            }
                            TextView noWarningText = (TextView) findViewById(R.id.no_warning_content);
                            if(!warningList.isEmpty()) {
                                noWarningText.setVisibility(View.GONE);
                                mAdapter.notifyDataSetChanged();
                            } else {
                                noWarningText.setText("No warnings have been posted yet.");
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error
                    Log.d("Error.Response", error.toString());
                }
            });
            AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest, "tag");
        }
    }

    private void showWarningDetails(Warning warning){
        Intent warningIntent = new Intent(NotificationActivity.this, WarningInfoActivity.class);

        warningIntent.putExtra(Constant.WARNING, warning);
        warningIntent.putExtra(Constant.UNVERIFIED_WARNING, Boolean.TRUE);
        startActivity(warningIntent);
    }
}
