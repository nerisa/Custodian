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
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.nerisa.thesis.AppController;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.R;
import com.nerisa.thesis.model.MarkerTag;
import com.nerisa.thesis.model.Monument;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerDragListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;

    private boolean mLocationPermissionGranted;

    private Monument monument = new Monument();
    private boolean isMarkerDragged = false;

    private static List<Monument> wikiMonuments = new ArrayList<>();
    private static List<Monument> custodianMonuments = new ArrayList<>();

    private static GoogleSignInClient mGoogleSignInClient;


    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 250; // 10 meters


    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 15;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }

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
    protected void onResume() {
        super.onResume();

        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
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
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
//        mMap.clear();
        mLastLocation = location;
        LatLng currentPos = new LatLng(mLastLocation.getLatitude(),
                mLastLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(currentPos)
                .title("Add your monument here")
                .draggable(true));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, DEFAULT_ZOOM));
        getNearbyMonuments(currentPos);
        getWikiMonuments(currentPos);
        displayNearbyMonuments();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private LatLng getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        LatLng currentPos = mDefaultLocation;
        try {
            if (mLocationPermissionGranted) {
                Log.d(TAG, "Location permission is granted. Getting device's location");
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                currentPos = new LatLng(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions()
                        .position(currentPos)
                        .title("Add your monument here")
                        .draggable(true));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, DEFAULT_ZOOM));
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
        return currentPos;
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
        updateLocationUI();
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        LatLng userPos = getDeviceLocation();
        getNearbyMonuments(userPos);
        getWikiMonuments(userPos);
        displayNearbyMonuments();
        createLocationRequest();

        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerDragListener(this);

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
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
                mLastLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if(marker.getTag() == null) {
            Intent addMonument = new Intent(MapsActivity.this, AddMonumentActivity.class);
            if (!isMarkerDragged) {
                LatLng markerPos = marker.getPosition();
                Log.d(TAG, "Monument in (" + String.valueOf(markerPos.latitude) + "," + String.valueOf(markerPos.longitude) + ") is being added");
                monument.setLongitude(markerPos.longitude);
                monument.setLatitude(markerPos.latitude);
            }
            addMonument.putExtra(Constant.MONUMENT, monument);
            startActivity(addMonument);
        } else {
            showMonumentInformation((MarkerTag) marker.getTag());
        }
    }

    private void showMonumentInformation(MarkerTag marker) {
        if (marker.getType() == MarkerTag.MarkerType.WIKI) {
            String url = String.format(Constant.WIKI_REST_URL, marker.getWikiName());
            Log.d(TAG, "Getting monument info from wiki: " + url);
            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d(TAG, "Got response for monument: " + response.toString());
                            Monument monument = Monument.mapWikiDetailedResponse(response);
                            Intent monumentInfo = new Intent(MapsActivity.this, WikiInfoActivity.class);
                            monumentInfo.putExtra(Constant.MONUMENT, monument);
                            startActivity(monumentInfo);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error
                    Log.e("Error.Response", error.toString());
                }
            });
            AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest, "tag");
        } else if (marker.getType() == MarkerTag.MarkerType.MONUMENT) {
            Long monumentId = (Long) marker.getMonumentId();
            String url = String
                    .format(Constant.SERVER_URL + Constant.MONUMENT_URL + "/%1$s",
                            monumentId.toString());
//            String url = String
//                    .format(Constant.SERVER_URL + Constant.MONUMENT_URL+"/%1$s",
//                            "3");
            Log.d(TAG, "Getting monument details with url: " + url);

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d(TAG, "Got response for monument: " + response.toString());
                            Monument monument = Monument.mapResponse(response);
                            Intent monumentInfo = new Intent(MapsActivity.this, MonumentInfoActivity.class);
                            monumentInfo.putExtra(Constant.MONUMENT, monument);
                            startActivity(monumentInfo);
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
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        isMarkerDragged = true;
    }

    @Override
    public void onMarkerDrag(Marker marker) {}

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng markerPos = marker.getPosition();
        monument.setLongitude(markerPos.longitude);
        monument.setLatitude(markerPos.latitude);
    }

    private void getNearbyMonuments(LatLng userPos){
        //TODO put actual coordinates
        String url = String
                .format(Constant.SERVER_URL + Constant.MONUMENTS_URL +"?lat=%1$s&lon=%2$s",
                        userPos.latitude,
                        userPos.longitude);
//        String url = String
//                .format(Constant.SERVER_URL + Constant.MONUMENTS_URL +"?lat=%1$s&lon=%2$s",
//                        "48.624061",
//                        "2.444167");
        Log.i(TAG, "Getting nearby monuments using url " + url);
        JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response) {
                        // response
                        Log.d(TAG, "Nearby monuments: " + response.toString());
                        for(int i = 0; i< response.length(); i++){
                            try {
                                Monument monument = Monument.mapResponse((JSONObject) response.get(i));
                                custodianMonuments.add(monument);
//                                LatLng monumentPos = new LatLng(monument.getLatitude(), monument.getLongitude());
//                                Marker monumentMarker = mMap.addMarker(new MarkerOptions()
//                                        .position(monumentPos)
//                                        .title(monument.getName())
//                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_monument))
//                                        .draggable(false));
//                                monumentMarker.setTag(monument.getId());
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
                Log.e(TAG, error.toString());
            }
        });
        AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest,"tag");
    }

    private void getWikiMonuments(LatLng userPos){
        String wikiApi = String.format(Constant.WIKI_API_URL, userPos.latitude, userPos.longitude);
        Log.d(TAG + "/getWikiMonuments", "Getting wiki articles from " + wikiApi);
        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.GET, wikiApi, null,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d(TAG + "getWikiMonuments", "Response from wiki: " + response.toString());
                        try {
                            JSONObject outerObject = (JSONObject) response.get("query");
                            JSONArray results = (JSONArray) outerObject.get("geosearch");
                            for(int i=0; i < results.length(); i++){
                                JSONObject result = (JSONObject) results.get(i);
                                if(result.getString("type").equalsIgnoreCase("landmark")){
                                    Log.d(TAG, "a monument found in wiki");
                                    Monument monument = Monument.mapWikiResponse(result);
                                    wikiMonuments.add(monument);
//                                    LatLng monumentPos = new LatLng(monument.getLatitude(), monument.getLongitude());
//                                    Marker monumentMarker = mMap.addMarker(new MarkerOptions()
//                                            .position(monumentPos)
//                                            .title(monument.getName())
//                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_wikipedia))
//                                            .draggable(false));
//                                    monumentMarker.setTag(monument.getReference());
                                }
                            }
                        }catch (JSONException e){
                            Log.e(TAG, "Wiki response parse error: " + e.getStackTrace());
                        }

                    }
                }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error) {
                // error
                Log.e(TAG, error.toString());
            }
        });
        AppController.getInstance(getApplicationContext()).addToRequestQueue(postRequest,"tag");
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }catch (SecurityException e){
            e.printStackTrace();
        }

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    private void displayNearbyMonuments(){
        wikiMonuments.removeAll(custodianMonuments);
        for(Monument monument: wikiMonuments){
            Log.d(TAG + "/displayNearbyMonuments", "Displaying Wiki info: " + monument.getName());
            LatLng monumentPos = new LatLng(monument.getLatitude(), monument.getLongitude());
            Marker monumentMarker = mMap.addMarker(new MarkerOptions()
                    .position(monumentPos)
                    .title(monument.getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_wikipedia))
                    .draggable(false));
            MarkerTag tag = new MarkerTag(monument.getReference(), monument.getName());
            monumentMarker.setTag(tag);
        }
        for(Monument monument: custodianMonuments){
            Log.d(TAG + "/displayNearbyMonuments", "Displaying monument: " + monument.getName());
            LatLng monumentPos = new LatLng(monument.getLatitude(), monument.getLongitude());
            Marker monumentMarker = mMap.addMarker(new MarkerOptions()
                    .position(monumentPos)
                    .title(monument.getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_monument))
                    .draggable(false));
            MarkerTag tag = new MarkerTag(monument.getId());
            monumentMarker.setTag(tag);
        }
    }
}