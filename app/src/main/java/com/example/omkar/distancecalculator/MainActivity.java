package com.example.omkar.distancecalculator;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
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
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    boolean start;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = true;
        final Button startButton = (Button)  findViewById(R.id.btnStart);

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (startButton.getText().equals("START")){
                    startButton.setText("STOP");
                    final EditText host = (EditText)  findViewById(R.id.editTextHost);
                    final EditText username = (EditText)  findViewById(R.id.editTextUsername);
                    if(start){
                        startLocationTracker(host.getText().toString(), username.getText().toString());
                    }
                    else
                        startLocationTracker(host.getText().toString(), username.getText().toString());
                }else{
                    startButton.setText("START");
                    stopLocationTracker();
                    start = true;
                }
            }
        });
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    Timer timer;
    int delay = 2000; // delay for 2 sec.
    int period = 15000; // repeat every 5 secs.

    void startLocationTracker(final String host, final String username){
        timer = new Timer();
        Log.i("Main Activity", "Started");
        timer.scheduleAtFixedRate(new TimerTask(){
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
        }, delay, period);
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
        if (timer != null){
            timer.cancel();
            timer.purge();
        }
        Toast.makeText(this, "Total Distance: "+totalDistance, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null){
            timer.cancel();
            timer.purge();
        }
    }
}
