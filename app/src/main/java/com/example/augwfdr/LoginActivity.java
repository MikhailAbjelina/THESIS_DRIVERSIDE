package com.example.augwfdr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.location.Location;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText loginBus;
    private EditText loginPassword;
    private Button loginButton;
    private TextView signupRedirectText;
    private FusedLocationProviderClient fusedLocationClient;
    static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private String loggedInBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        loginBus = findViewById(R.id.login_bus);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String busNumber = loginBus.getText().toString();
                loggedInBus = busNumber;
                final String password = loginPassword.getText().toString().trim();
                if (busNumber.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                } else {
                    authenticateUser(busNumber, password, new AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSuccess() {
                            // After successful authentication, pass bus_number to MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("bus_number", loggedInBus);  // Pass the bus number to MainActivity
                            startActivity(intent);
                            finish();  // Optionally finish LoginActivity so it doesn't stay in the back stack
                        }

                        @Override
                        public void onAuthenticationFailure() {
                            Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    // Define the AuthenticationCallback interface
    public interface AuthenticationCallback {
        void onAuthenticationSuccess();
        void onAuthenticationFailure();
    }

    // Modify authenticateUser to use the callback
    private void authenticateUser(final String busNumber, String password, AuthenticationCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://busgoapplication.com/dvrlogin.php"; // Replace with your API endpoint URL

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d("LoginActivity", "Response: " + response);
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            if (success) {
                                Toast.makeText(LoginActivity.this, "Login Successfully!", Toast.LENGTH_SHORT).show();// Save the logged-in bus number for location data
                                callback.onAuthenticationSuccess();  // Trigger success callback
                            } else {
                                callback.onAuthenticationFailure();  // Trigger failure callback
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "";
                        if (error.networkResponse != null) {
                            errorMessage = "Error code: " + error.networkResponse.statusCode + ", " + new String(error.networkResponse.data);
                        } else {
                            errorMessage = error.toString();
                        }
                        Log.e("LoginActivity", "Error: " + errorMessage);
                        Toast.makeText(LoginActivity.this, "Error connecting to server: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bus_number", busNumber);
                params.put("password", password);
                return params;
            }
        };

        queue.add(postRequest);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndSendToServer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndSendToServer();
            } else {
                Toast.makeText(this, "Location permission is required to send location data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationAndSendToServer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    sendLocationToServer(location);
                }
            }
        });
    }

    private void sendLocationToServer(Location location) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://busgoapplication.com/dvrupdate_location.php"; // Replace with your API endpoint URL

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("LoginActivity", "Location update response: " + response);
                        Toast.makeText(LoginActivity.this, "Location sent successfully", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "";
                        if (error.networkResponse != null) {
                            errorMessage = "Error code: " + error.networkResponse.statusCode + ", " + new String(error.networkResponse.data);
                        } else {
                            errorMessage = error.toString();
                        }
                        Log.e("LoginActivity", "Location update error: " + errorMessage);
                        Toast.makeText(LoginActivity.this, "Error sending location: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bus_number", loggedInBus); // Use the saved bus number
                params.put("latitude", String.valueOf(location.getLatitude()));
                params.put("longitude", String.valueOf(location.getLongitude()));
                return params;
            }
        };

        queue.add(postRequest);
    }
}
