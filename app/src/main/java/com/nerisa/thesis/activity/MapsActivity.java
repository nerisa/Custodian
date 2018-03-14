package com.nerisa.thesis.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.custodian.R;
import com.nerisa.thesis.model.Monument;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerDragListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private boolean mLocationPermissionGranted;
    private LocationCallback mLocationCallback;
    private Monument monument = new Monument();
    private boolean isMarkerDragged = false;
    private static final String ADD_MONUMENT_MARKER = "new marker";

    private static GoogleSignInClient mGoogleSignInClient;


    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;


    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 15;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null){
            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "=============menu created=============");
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
                                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                                startActivity(intent);
                                // [END_EXCLUDE]
                            }
                        });
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // Prompt the user for permission.

        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        LatLng userPos = getDeviceLocation();
        displayNearbyMonuments(userPos);

        createLocationRequest();

        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerDragListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mMap.clear();
        mLastKnownLocation = location;
        LatLng currentPos = new LatLng(mLastKnownLocation.getLatitude(),
                mLastKnownLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(currentPos)
                .title("You are here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, DEFAULT_ZOOM));
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private LatLng getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        final LatLng[] result = new LatLng[1];
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {

                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            LatLng currentPos = new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude());
                            result[0] = currentPos;
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentPos)
                                    .title("Add your monument here")
                                    .draggable(true));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, DEFAULT_ZOOM));
//
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            result[0] = mDefaultLocation;
                            mMap.addMarker(new MarkerOptions()
                                    .position(mDefaultLocation)
                                    .title("Add your monument here")
                                    .draggable(true));
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
        return result[0];
    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
//        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if(marker.getTag() == null) {
            Intent addMonument = new Intent(this, AddMonumentActivity.class);
            if (!isMarkerDragged) {
                LatLng markerPos = marker.getPosition();
                Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>");
                Log.d(TAG, String.valueOf(markerPos.latitude));
                Log.d(TAG, String.valueOf(markerPos.longitude));
                monument.setLongitude(markerPos.longitude);
                monument.setLatitude(markerPos.latitude);
            }
            addMonument.putExtra(Constant.MONUMENT, monument);
            startActivity(addMonument);
        } else {
            Long monumentId = (Long) marker.getTag();
//            String url = String
//                    .format(Constant.SERVER_URL + Constant.MONUMENT_URL +"/%1$s",
//                            monumentId.toString());
            String url = String
                    .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s",
                            "1");
            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d(TAG, response.toString());
                            Monument monument = new Gson().fromJson(response.toString(), Monument.class);
                            Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>");
                            Log.d(TAG, monument.getMonumentPhoto());
                            Log.d(TAG, monument.getName());
                            Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>");
                            Intent monumentInfo = new Intent(MapsActivity.this, MonumentInfoActivity.class);
                            monumentInfo.putExtra(Constant.MONUMENT, monument);
                            startActivity(monumentInfo);
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
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        isMarkerDragged = true;
        Log.d(TAG, "Marker is being dragged");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.d(TAG, "Marker is being dragged");
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng markerPos = marker.getPosition();
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>");
        Log.d(TAG, String.valueOf(markerPos.latitude));
        Log.d(TAG, String.valueOf(markerPos.longitude));
        monument.setLongitude(markerPos.longitude);
        monument.setLatitude(markerPos.latitude);
    }

    private void displayNearbyMonuments(LatLng userPos){
//        String url = String
//                .format(Constant.SERVER_URL + Constant.MONUMENTS_URL +"?lat=%1$s&lon=%2$s",
//                        userPos.latitude,
//                        userPos.longitude);
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENTS_URL +"?lat=%1$s&lon=%2$s",
                        "48.624061",
                        "2.444167");
        JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response) {
                        // response
                        Log.d("Response", response.toString());
                        for(int i = 0; i< response.length(); i++){
                            try {
                                Monument monument = new Gson().fromJson(response.get(i).toString(), Monument.class);
                                LatLng monumentPos = new LatLng(monument.getLatitude(), monument.getLongitude());
                                Marker monumentMarker = mMap.addMarker(new MarkerOptions()
                                        .position(monumentPos)
                                        .title(monument.getName())
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_monument))
                                        .draggable(false));
                                monumentMarker.setTag(monument.getId());

                            }catch (JSONException e){
                                Log.w(TAG, "Could not parse monument data");
                            }
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
}