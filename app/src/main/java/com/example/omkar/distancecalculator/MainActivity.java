package com.example.omkar.distancecalculator;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    // start variable to send the additional variable in the first request
    boolean start;
    // random integer which acts as a constant. This same constant is used to retrieve when user responds with the request.
    final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = true;
        final Button startButton = (Button)  findViewById(R.id.btnStart);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (startButton.getText().toString().equalsIgnoreCase("START")){
                    startButton.setText("STOP");
                    final EditText host = (EditText)  findViewById(R.id.editTextHost);
                    final EditText username = (EditText)  findViewById(R.id.editTextUsername);
                    startLocationTracker(host.getText().toString(), username.getText().toString());
                }else{
                    startButton.setText("START");
                    stopLocationTracker();
                    start = true;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("Main Activity", "Permission granted for retrieving the FINE LOCATION");
                } else {
                    //Permission-denied
                    Log.w("Main Activity", "Permission not granted for retrieving the FINE LOCATION");
                    Toast.makeText(this, "Permission not granted for retrieving the FINE LOCATION", Toast.LENGTH_SHORT).show();
                    final Button startButton = (Button)  findViewById(R.id.btnStart);
                    startButton.setEnabled(false);
                }
            }
        }
    }

    ScheduledThreadPoolExecutor exec; // Better than timer and timertask.
    int period = 5000; // repeat every 5 secs. Period is the interval between multiple timer calls.

    void startLocationTracker(final String host, final String username){
        exec = new ScheduledThreadPoolExecutor(1); //5 denotes the number of threads in pool for parallel processing.
        Log.i("Main Activity", "Timer Start.");
        exec.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location == null) {
                            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                        if (location != null){
                            double longitude = location.getLongitude();
                            double latitude = location.getLatitude();
                            Log.i("Main Activity", "Posted. Lat : "+latitude + ", Lon: "+longitude);
                            sendPostRequestToServer(latitude, longitude, username, host);
                        }
                        else{
                            Log.w("Main Activity", "Location cannot be retrieved.");
                        }
                    }
                    else{
                        Log.w("Main Activity", "Permission not found for retrieving the FINE LOCATION");
                    }
                }
            }
        },0, period, TimeUnit.SECONDS);
    }

    double totalDistance = 0.0;

    void sendPostRequestToServer(final double latitude, final double longitude, String username, String host){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        final String URL = "http://"+host+"/locationupdate";
        // Post params to be sent to the server
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("latitude", String.valueOf(latitude));
        params.put("longitude", String.valueOf(longitude));
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        params.put("timestamp", ts);
        if (start){
            params.put("start", "true");
            start = false;
        }else{
            params.put("start", "false");
        }
        JsonObjectRequest request_json = new JsonObjectRequest(Request.Method.POST, URL, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            Log.i("Main Activity", "Distance: "+response.getString("total_distance"));
                            totalDistance = Double.parseDouble(response.getString("total_distance"));
                        }catch(org.json.JSONException e){
                            Log.e("Main Activity", "Error parsing JSON from response", e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Main Activity", "Error fetching response from POST request", error);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
//                params.put("Content-Type","application/json");
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        queue.add(request_json);

    }

    void stopLocationTracker(){
//        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
        Log.i("Main Activity", "Stoppped");
        if (exec != null){
            exec.shutdown();
        }
        Toast.makeText(this, "Total Distance: "+totalDistance, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exec != null){
            exec.shutdown();
        }
    }
}
