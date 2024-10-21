package com.example.augwfdr;

import static com.example.augwfdr.R.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.app.Dialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private double currentLatitude;
    private double currentLongitude;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DrawerLayout drawerLayout;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    FloatingActionButton fab;
    private double finalLatitude = 14.509501685679016;
    private double finalLongitude = 120.99079511166886;
    private String busNumber;
    private String BusNumber;
    private List<OverlayWithIW> routeOverlays = new ArrayList<>();
    private String elapsedTimeString = "";
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize osmdroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(layout.activity_main);

        // Retrieve the bus number from the intent
        Intent intent = getIntent();
        busNumber = intent.getStringExtra("bus_number");

        // If the bus number is null, handle the error
        if (busNumber == null || busNumber.isEmpty()) {
            Toast.makeText(this, "Bus number not found!", Toast.LENGTH_SHORT).show();
            finish();  // Closing the MainActivity if bus_number is not found
            return;
        }

        mapView = findViewById(id.mapview);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location permissions
        if (checkLocationPermission()) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            // Permissions are granted, initialize map with GPS
            initializeMap();
            startLocationUpdates();
            retrieveBusNumberAndStartLocationUpdates();
            requestLocationUpdates();
        }
        double latitude = intent.getDoubleExtra("latitude", -1);
        double longitude = intent.getDoubleExtra("longitude", -1);
        if (latitude != -1 && longitude != -1) {
            moveToLocation(latitude, longitude);
        }
        //NAVIGATION SETUP
        Toolbar toolbar = findViewById(id.toolbar);
        setSupportActionBar(toolbar);
        FragmentManager manager = this.getSupportFragmentManager();
        drawerLayout = findViewById(id.drawer_layout);
        NavigationView navigationView = findViewById(id.nav_view);
        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, string.open_nav, string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                drawerLayout.closeDrawer(GravityCompat.START);

                if (id == R.id.nav_profile) {
                    Intent intent = new Intent(MainActivity.this, AccountView.class);
                    intent.putExtra("bus_number", busNumber);  // Pass the bus number to the next activity
                    startActivity(intent);
                } else if (id == R.id.nav_announcement) {
                    Intent intent = new Intent(MainActivity.this, Announcements.class);
                    intent.putExtra("bus_number", busNumber);  // Pass the bus number to the next activity
                    startActivity(intent);
                } else if (id == R.id.nav_settings) {
                    Intent intent = new Intent(MainActivity.this, Settings.class);
                    intent.putExtra("bus_number", busNumber);  // Pass the bus number to the next activity
                    startActivity(intent);
                } else if (id == R.id.nav_history) {
                    Intent intent = new Intent(MainActivity.this, History.class);
                    intent.putExtra("bus_number", busNumber);  // Pass the bus number to the next activity
                    startActivity(intent);
                } else if (id == R.id.nav_about) {
                    Intent intent = new Intent(MainActivity.this, AboutUs.class);
                    intent.putExtra("bus_number", busNumber);  // Pass the bus number to the next activity
                    startActivity(intent);
                } else if (id == R.id.nav_logout) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }

                new Handler().postDelayed(() -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }, 250); // delay in milliseconds
                return true;
            }
        });
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomDialog();
            }
        });
    }

    // Check if location permissions are granted
    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void moveToLocation(double latitude, double longitude) {
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);
        mapView.getController().setCenter(geoPoint);
        mapView.getController().setZoom(15.0);  // Adjust zoom level as needed
    }

    // Request location permission
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    private void startLocationUpdates() {
        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateMapLocation(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        updateMapLocation(location);
                    }
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
        }
    }
    private void initializeMap() {
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);

        // Enable zoom controls
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Create MyLocation overlay
        myLocationOverlay = new MyLocationNewOverlay(mapView);
        mapView.getOverlays().add(myLocationOverlay);

        // Enable My Location overlay (blue dot representing user's location)
        GpsMyLocationProvider locationProvider = new GpsMyLocationProvider(this);
        myLocationOverlay.enableMyLocation(locationProvider);

        // Enable follow location for continuous updates
        myLocationOverlay.enableFollowLocation();

        // Center the map on the user's location
        Location lastKnownLocation = locationProvider.getLastKnownLocation();
        if (lastKnownLocation != null) {
            mapView.getController().setCenter(new GeoPoint(lastKnownLocation));
        }

        // Set an appropriate zoom level
        mapView.getController().setZoom(12.0);
    }

    private void updateMapLocation(Location location) {
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapView.getController().setCenter(geoPoint);

        // Clear existing markers
        mapView.getOverlays().clear();

        // Add a marker to the current location
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setTitle("You are here");
        mapView.getOverlays().add(marker);

        mapView.invalidate();  // Refresh the map
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void requestLocationUpdates() {
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request location updates
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Update KalmanFilter with initial location
                                kalmanFilter = new KalmanFilter(location.getLatitude(), location.getLongitude());
                                // Update TextViews with initial location
                                updateTextViewsWithLocationInfo(location.getLatitude(), location.getLongitude());
                            }
                        }
                    });
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
    private void updateTextViewsWithLocationInfo(double latitude, double longitude) {
        // Update KalmanFilter object with initial location
        double[] currentState = kalmanFilter.getCurrentState();
        double currentLatitude = currentState[0];
        double currentLongitude = currentState[1];
        kalmanFilter = new KalmanFilter(latitude, longitude);
        kalmanFilter.setFinalDestination(finalLatitude, finalLongitude);

        // Retrieve average speed from KalmanFilter instance
        double averageSpeedKmh = kalmanFilter.getAverageSpeed(); // Use the instance method to get the average speed


        // Calculate distance to final destination
        double distanceToFinal = kalmanFilter.calculateDistanceToFinal();

        // Calculate remaining time
        double remainingTimeInMinutes = kalmanFilter.calculateRemainingTime();


        TextView estimatedLatitudeTextView = findViewById(R.id.estimatedLatitudeTextView);
        TextView estimatedLongitudeTextView = findViewById(R.id.estimatedLongitudeTextView);
        TextView distanceTextView = findViewById(R.id.distanceTextView);
        TextView remainingTimeTextView = findViewById(R.id.remainingTimeTextView);

        // Update TextViews with the calculated values
        estimatedLatitudeTextView.setText(String.format("Estimated Latitude: %.4f", currentLatitude));
        estimatedLongitudeTextView.setText(String.format("Estimated Longitude: %.4f", currentLongitude));
        distanceTextView.setText(String.format("Distance from Destination: %.2f km", distanceToFinal));
        remainingTimeTextView.setText(String.format("Remaining Time: %.2f minutes", remainingTimeInMinutes));

        // Send remaining time to server
        sendRemainingTimeToServer(BusNumber, remainingTimeInMinutes);
    }
    private void showBottomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        LinearLayout mendezLayout = dialog.findViewById(R.id.mendez);
        LinearLayout genMarAlvarezLayout = dialog.findViewById(R.id.genmaralvarez);
        LinearLayout caviteCityLayout = dialog.findViewById(R.id.cavitecity);
        LinearLayout toMendez = dialog.findViewById(R.id.pitx2Mendez);
        LinearLayout toGenMar = dialog.findViewById(R.id.pitx2GenMar);
        LinearLayout toCavite = dialog.findViewById(R.id.pitx2Cavite);
        ImageView cancelButton = dialog.findViewById(R.id.cancelButton);

        mendezLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCoordinatesForMendez();
                dialog.dismiss();
            }
        });

        genMarAlvarezLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCoordinatesForGMA();
                dialog.dismiss();

            }
        });

        caviteCityLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCoordinatesForPadua();
                dialog.dismiss();

            }
        });

        toMendez.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCoordinatesToMendez();
                dialog.dismiss();

            }
        });

        toGenMar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCoordinatesToGenMar();
                dialog.dismiss();
            }
        });

        toCavite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCoordinatesToCavite();
                dialog.dismiss();

            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    private void getCoordinatesForMendez() {
        RoadManager roadManager = new OSRMRoadManager(this, "MyOwnUserAgent/1.0");
        GeoPoint startLocation = new GeoPoint(14.120316698482938, 120.90910396933543);
        mapView.getController().setCenter(startLocation);
        mapView.getController().setZoom(18.0); // Set an appropriate zoom level
        // Update currentLatitude and currentLongitude
        currentLatitude = startLocation.getLatitude();
        currentLongitude = startLocation.getLongitude();
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        GeoPoint start = new GeoPoint(14.120285484470212, 120.9090610539933);
        waypoints.add(start);
        GeoPoint midPoint = new GeoPoint(14.09774747103367, 120.91895306088588);
        waypoints.add(midPoint);
        GeoPoint midPoint1 = new GeoPoint(14.115821456856606, 120.96172893048771);
        waypoints.add(midPoint1);
        GeoPoint midPoint2 = new GeoPoint(14.287145901121493, 120.95939357047006);
        waypoints.add(midPoint2);
        GeoPoint midPoint3 = new GeoPoint(14.29086498610908, 120.95894245399518);
        waypoints.add(midPoint3);
        GeoPoint midPoint4 = new GeoPoint(14.322870844073693, 120.93810110822255);
        waypoints.add(midPoint4);
        GeoPoint midPoint5 = new GeoPoint(14.32722125685193, 120.93538134826866);
        waypoints.add(midPoint5);
        GeoPoint midPoint6 = new GeoPoint(14.32947065440676, 120.93959651748473);
        waypoints.add(midPoint6);
        GeoPoint destination = new GeoPoint(14.509501685679016, 120.99079511166886);
        waypoints.add(destination);
        fetchTrafficData(waypoints, new TrafficDataCallback() {
            @Override
            public void onTrafficDataFetched(JSONObject trafficData) {
                updateMapWithTrafficData(waypoints, trafficData);
            }
        });
        finalLatitude = 14.509501685679016;
        finalLongitude = 120.99079511166886;
        kalmanFilter.setFinalDestination(finalLatitude, finalLongitude);
        updateRouteOnServer(BusNumber, "MENDEZ-PITX");
    }

    private void getCoordinatesForGMA() {
        RoadManager roadManager = new OSRMRoadManager(this, "MyOwnUserAgent/1.0");
        // Replace this with the actual coordinates for your predefined locations
        GeoPoint startLocation = new GeoPoint(14.287866614713472, 121.00013006251946);
        mapView.getController().setCenter(startLocation);
        mapView.getController().setZoom(18.0); // Set an appropriate zoom level
        // Update currentLatitude and currentLongitude
        currentLatitude = startLocation.getLatitude();
        currentLongitude = startLocation.getLongitude();
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        GeoPoint start = new GeoPoint(14.287866614713472, 121.00013006251946);
        waypoints.add(start);
        GeoPoint midPoint = new GeoPoint(14.417718804975594, 120.97462599632549);
        waypoints.add(midPoint);
        GeoPoint destination = new GeoPoint(14.509501685679016, 120.99079511166886);
        waypoints.add(destination);
        fetchTrafficData(waypoints, new TrafficDataCallback() {
            @Override
            public void onTrafficDataFetched(JSONObject trafficData) {
                updateMapWithTrafficData(waypoints, trafficData);
            }
        });
        finalLatitude = 14.509501685679016;
        finalLongitude = 120.99079511166886;
        kalmanFilter.setFinalDestination(finalLatitude, finalLongitude);
        updateRouteOnServer(BusNumber, "GMA-PITX");
    }

    private void getCoordinatesForPadua() {
        RoadManager roadManager = new OSRMRoadManager(this, "MyOwnUserAgent/1.0");
        // Replace this with the actual coordinates for your predefined locations
        GeoPoint startLocation = new GeoPoint(14.487014389993611, 120.90254173128984);
        mapView.getController().setCenter(startLocation);
        mapView.getController().setZoom(18.0); // Set an appropriate zoom level
        // Update currentLatitude and currentLongitude
        currentLatitude = startLocation.getLatitude();
        currentLongitude = startLocation.getLongitude();
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        GeoPoint start = new GeoPoint(14.487014389993611, 120.90254173128984);
        waypoints.add(start);
        GeoPoint midPoint = new GeoPoint(14.432854584276043, 120.88625216276692);
        waypoints.add(midPoint);
        GeoPoint destination = new GeoPoint(14.509501685679016, 120.99079511166886);
        waypoints.add(destination);
        fetchTrafficData(waypoints, new TrafficDataCallback() {
            @Override
            public void onTrafficDataFetched(JSONObject trafficData) {
                updateMapWithTrafficData(waypoints, trafficData);
            }
        });
        finalLatitude = 14.509501685679016;
        finalLongitude = 120.99079511166886;
        kalmanFilter.setFinalDestination(finalLatitude, finalLongitude);
        updateRouteOnServer(BusNumber, "CAVITE CITY-PITX");
    }

    private void getCoordinatesToMendez() {
        RoadManager roadManager = new OSRMRoadManager(this, "MyOwnUserAgent/1.0");
        // Replace this with the actual coordinates for your predefined locations
        GeoPoint startLocation = new GeoPoint(14.510180438746158, 120.99092025344035);
        mapView.getController().setCenter(startLocation);
        mapView.getController().setZoom(18.0); // Set an appropriate zoom level
        // Update currentLatitude and currentLongitude
        currentLatitude = startLocation.getLatitude();
        currentLongitude = startLocation.getLongitude();
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        GeoPoint start = new GeoPoint(14.510180438746158, 120.99092025344035);
        waypoints.add(start);
        GeoPoint midPoint = new GeoPoint(14.507612441596951, 120.9900697772432);
        waypoints.add(midPoint);
        GeoPoint midPoint1 = new GeoPoint(14.508188904924342, 120.98847654514006);
        waypoints.add(midPoint1);
        GeoPoint midPoint2 = new GeoPoint(14.505918098104532, 120.98760482721214);
        waypoints.add(midPoint2);
        GeoPoint midPoint3 = new GeoPoint(14.505911191154253, 120.98761502616001);
        waypoints.add(midPoint3);
        GeoPoint midPoint4 = new GeoPoint(14.505032101110979, 120.98961403136235);
        waypoints.add(midPoint4);
        GeoPoint midPoint5 = new GeoPoint(14.472488808130505, 120.96490833919837);
        waypoints.add(midPoint5);
        GeoPoint midPoint6 = new GeoPoint(14.468315766773333, 120.9628235380594);
        waypoints.add(midPoint6);
        GeoPoint midPoint7 = new GeoPoint(14.32721275382425, 120.93539767019473);
        waypoints.add(midPoint7);
        GeoPoint midPoint8 = new GeoPoint(14.317320073476273, 120.9429680086375);
        waypoints.add(midPoint8);
        GeoPoint midPoint9 = new GeoPoint(14.115147907516517, 120.96175954517834);
        waypoints.add(midPoint9);
        GeoPoint destination = new GeoPoint(14.120111154056406, 120.90921410865793);
        waypoints.add(destination);
        fetchTrafficData(waypoints, new TrafficDataCallback() {
            @Override
            public void onTrafficDataFetched(JSONObject trafficData) {
                updateMapWithTrafficData(waypoints, trafficData);
            }
        });
        finalLatitude = 14.120111154056406;
        finalLongitude = 120.90921410865793;
        kalmanFilter.setFinalDestination(finalLatitude, finalLongitude);
        updateRouteOnServer(BusNumber, "PITX-MENDEZ");
    }

    private void getCoordinatesToGenMar() {
        RoadManager roadManager = new OSRMRoadManager(this, "MyOwnUserAgent/1.0");
        // Replace this with the actual coordinates for your predefined locations
        GeoPoint startLocation = new GeoPoint(14.510180438746158, 120.99092025344035);
        mapView.getController().setCenter(startLocation);
        mapView.getController().setZoom(18.0); // Set an appropriate zoom level
        // Update currentLatitude and currentLongitude
        currentLatitude = startLocation.getLatitude();
        currentLongitude = startLocation.getLongitude();
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        GeoPoint midPoint = new GeoPoint(14.507612441596951, 120.9900697772432);
        waypoints.add(midPoint);
        GeoPoint midPoint1 = new GeoPoint(14.508188904924342, 120.98847654514006);
        waypoints.add(midPoint1);
        GeoPoint midPoint2 = new GeoPoint(14.505918098104532, 120.98760482721214);
        waypoints.add(midPoint2);
        GeoPoint midPoint3 = new GeoPoint(14.505911191154253, 120.98761502616001);
        waypoints.add(midPoint3);
        GeoPoint midPoint4 = new GeoPoint(14.505032101110979, 120.98961403136235);
        waypoints.add(midPoint4);
        GeoPoint midPoint5 = new GeoPoint(14.472476604888842, 120.96445705070369);
        waypoints.add(midPoint5);
        GeoPoint midPoint6 = new GeoPoint(14.468315766773333, 120.9628235380594);
        waypoints.add(midPoint6);
        GeoPoint destination = new GeoPoint(14.287221247673578, 121.00038124642944);
        waypoints.add(destination);
        fetchTrafficData(waypoints, new TrafficDataCallback() {
            @Override
            public void onTrafficDataFetched(JSONObject trafficData) {
                updateMapWithTrafficData(waypoints, trafficData);
            }
        });
        finalLatitude = 14.287221247673578;
        finalLongitude = 121.00038124642944;
        kalmanFilter.setFinalDestination(finalLatitude, finalLongitude);
        updateRouteOnServer(BusNumber, "PITX-GMA");
    }

    private void getCoordinatesToCavite() {
        RoadManager roadManager = new OSRMRoadManager(this, "MyOwnUserAgent/1.0");
        // Replace this with the actual coordinates for your predefined locations
        GeoPoint startLocation = new GeoPoint(14.510180438746158, 120.99092025344035);
        mapView.getController().setCenter(startLocation);
        mapView.getController().setZoom(18.0); // Set an appropriate zoom level
        // Update currentLatitude and currentLongitude
        currentLatitude = startLocation.getLatitude();
        currentLongitude = startLocation.getLongitude();
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        GeoPoint midPoint = new GeoPoint(14.507612441596951, 120.9900697772432);
        waypoints.add(midPoint);
        GeoPoint midPoint1 = new GeoPoint(14.508188904924342, 120.98847654514006);
        waypoints.add(midPoint1);
        GeoPoint midPoint2 = new GeoPoint(14.505918098104532, 120.98760482721214);
        waypoints.add(midPoint2);
        GeoPoint midPoint3 = new GeoPoint(14.505911191154253, 120.98761502616001);
        waypoints.add(midPoint3);
        GeoPoint midPoint4 = new GeoPoint(14.505032101110979, 120.98961403136235);
        waypoints.add(midPoint4);
        GeoPoint midPoint5 = new GeoPoint(14.472201034136477, 120.96452669489108);
        waypoints.add(midPoint5);
        GeoPoint midPoint6 = new GeoPoint(14.47014658577686, 120.95936469249348);
        waypoints.add(midPoint6);
        GeoPoint midPoint7 = new GeoPoint(14.45583594044895, 120.91795506318235);
        waypoints.add(midPoint7);
        GeoPoint midPoint8 = new GeoPoint(14.451438652686832, 120.91593881571526);
        waypoints.add(midPoint8);
        GeoPoint midPoint9 = new GeoPoint(14.446864685390548, 120.91168483223228);
        waypoints.add(midPoint9);
        GeoPoint midPoint10 = new GeoPoint(14.446449101637459, 120.91049393146962);
        waypoints.add(midPoint10);
        GeoPoint midPoint11 = new GeoPoint(14.445898451974866, 120.90873976677202);
        waypoints.add(midPoint11);
        GeoPoint midPoint12 = new GeoPoint(14.446807542655119, 120.90729137397446);
        waypoints.add(midPoint12);
        GeoPoint midPoint13 = new GeoPoint(14.44518676124101, 120.90657254189244);
        waypoints.add(midPoint13);
        GeoPoint midPoint14 = new GeoPoint(14.444398859886283, 120.90402325275969);
        waypoints.add(midPoint14);
        GeoPoint midPoint15 = new GeoPoint(14.443723528422527, 120.90254267345551);
        waypoints.add(midPoint15);
        GeoPoint midPoint16 = new GeoPoint(14.442346884840521, 120.90363165026206);
        waypoints.add(midPoint16);
        GeoPoint midPoint17 = new GeoPoint(14.435256450933483, 120.88881541249839);
        waypoints.add(midPoint17);
        GeoPoint midPoint18 = new GeoPoint(14.433427781222173, 120.8850603198243);
        waypoints.add(midPoint18);
        GeoPoint midPoint19 = new GeoPoint(14.433968071559052, 120.8784298994172);
        waypoints.add(midPoint19);
        GeoPoint midPoint20 = new GeoPoint(14.451309823384491, 120.87804886140782);
        waypoints.add(midPoint20);
        GeoPoint midPoint21 = new GeoPoint(14.479531793351892, 120.89128965213057);
        waypoints.add(midPoint21);
        GeoPoint midPoint22 = new GeoPoint(14.48152629118139, 120.90150350365629);
        waypoints.add(midPoint22);
        GeoPoint destination = new GeoPoint(14.486637109963517, 120.90252274304017);
        waypoints.add(destination);
        fetchTrafficData(waypoints, new TrafficDataCallback() {
            @Override
            public void onTrafficDataFetched(JSONObject trafficData) {
                updateMapWithTrafficData(waypoints, trafficData);
            }
        });
        finalLatitude = 14.486637109963517;
        finalLongitude = 120.90252274304017;
        kalmanFilter.setFinalDestination(finalLatitude, finalLongitude);
        updateRouteOnServer(BusNumber, "PITX-CAVITE CITY");
    }
    private void fetchTrafficData(ArrayList<GeoPoint> waypoints, TrafficDataCallback callback) {
        // Build the waypoints string for the request with via for non-stopover waypoints
        StringBuilder waypointsString = new StringBuilder();
        for (int i = 1; i < waypoints.size() - 1; i++) { // Skip the first and last waypoints
            if (waypointsString.length() > 0) {
                waypointsString.append("|");
            }
            waypointsString.append("via:")
                    .append(waypoints.get(i).getLatitude()).append(",").append(waypoints.get(i).getLongitude());
        }

        // Build the URL for the Google Maps Directions API request
        String apiKey = "AIzaSyCV5rAXNR8UWGohQpnrTpz1wf3fbmomDNI";
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + waypoints.get(0).getLatitude() + "," + waypoints.get(0).getLongitude()
                + "&destination=" + waypoints.get(waypoints.size() - 1).getLatitude() + "," + waypoints.get(waypoints.size() - 1).getLongitude();
        if (waypointsString.length() > 0) {
            url += "&waypoints=" + waypointsString.toString();
        }
        url += "&departure_time=now" // Include departure time parameter
                + "&mode=driving" // Set mode to driving
                + "&key=" + apiKey;

        // Make the request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Log the entire response
                        Log.d("TrafficDataResponse", response.toString());

                        // Pass the response to the callback
                        callback.onTrafficDataFetched(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Log the error
                        Log.e("TrafficDataError", error.toString());
                        error.printStackTrace();
                    }
                });
        // Add the request to the RequestQueue
        requestQueue.add(jsonObjectRequest);
    }
    public interface TrafficDataCallback {
        void onTrafficDataFetched(JSONObject trafficData);
    }
    private void updateMapWithTrafficData(ArrayList<GeoPoint> waypoints, JSONObject trafficData) {
        int trafficDuration = parseTrafficData(trafficData);
        int travelDuration = parseDurationData(trafficData);
        // Clear existing overlays
        for (Overlay overlay : routeOverlays) {
            mapView.getOverlays().remove(overlay);
        }
        routeOverlays.clear();
        // Add the new route with traffic data
        RoadManager roadManager = new OSRMRoadManager(this, "MyOwnUserAgent/1.0");
        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        mapView.getOverlays().add(roadOverlay);
        // Optionally, change the color of the roadOverlay based on traffic data
        // Example threshold in seconds (10 minutes)
        if (trafficDuration >= travelDuration + 600) {
            roadOverlay.getOutlinePaint().setColor(Color.RED);
        } else if (trafficDuration >= travelDuration + 300 && trafficDuration < travelDuration + 600) {
            roadOverlay.getOutlinePaint().setColor(Color.parseColor("#FFA500"));
        } else {
            roadOverlay.getOutlinePaint().setColor(Color.GREEN);
        }
        startLocationUpdates();
        mapView.invalidate();
    }
    private int parseTrafficData(JSONObject trafficData) {
        int trafficDuration = 0;
        try {
            // Log the entire response to debug
            Log.d("TrafficDataResponse", trafficData.toString());

            // Check if "routes" array exists and is not empty
            JSONArray routes = trafficData.getJSONArray("routes");
            if (routes.length() > 0) {
                // Get the first route
                JSONObject route = routes.getJSONObject(0);
                // Check if "legs" array exists and is not empty
                JSONArray legs = route.getJSONArray("legs");
                if (legs.length() > 0) {
                    // Get the first leg
                    JSONObject leg = legs.getJSONObject(0);
                    // Log the leg details
                    Log.d("TrafficDataLeg", leg.toString());

                    // Check if "duration_in_traffic" object exists
                    if (leg.has("duration_in_traffic")) {
                        // Get the duration in traffic
                        JSONObject durationInTraffic = leg.getJSONObject("duration_in_traffic");
                        if (durationInTraffic.has("value")) {
                            trafficDuration = durationInTraffic.getInt("value");
                            Log.d("TrafficData", "Duration in traffic: " + trafficDuration);
                            Toast.makeText(this, "API WORKING", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("TrafficData", "Duration in traffic value not found");
                        }
                    } else {
                        Log.e("TrafficData", "Duration in traffic not found");
                    }
                } else {
                    Log.e("TrafficData", "Legs array is empty");
                }
            } else {
                Log.e("TrafficData", "Routes array is empty");
            }
        } catch (JSONException e) {
            Log.e("TrafficData", "JSON parsing error: " + e.getMessage());
            e.printStackTrace();
        }
        return trafficDuration;
    }

    private int parseDurationData(JSONObject trafficData) {
        int travelDuration = 0;
        try {
            // Log the entire response to debug
            Log.d("DurationDataResponse", trafficData.toString());

            // Check if "routes" array exists and is not empty
            JSONArray routes = trafficData.getJSONArray("routes");
            if (routes.length() > 0) {
                // Get the first route
                JSONObject route = routes.getJSONObject(0);
                // Check if "legs" array exists and is not empty
                JSONArray legs = route.getJSONArray("legs");
                if (legs.length() > 0) {
                    // Get the first leg
                    JSONObject leg = legs.getJSONObject(0);
                    // Log the leg details
                    Log.d("DurationDataLeg", leg.toString());

                    // Check if "duration" object exists
                    if (leg.has("duration")) {
                        // Get the duration in traffic
                        JSONObject duration = leg.getJSONObject("duration");
                        if (duration.has("value")) {
                            travelDuration = duration.getInt("value");
                            Log.d("DurationData", "Average Duration: " + travelDuration);
                            Toast.makeText(this, "API WORKING", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("DurationData", "Duration in traffic value not found");
                        }
                    } else {
                        Log.e("DurationData", "Duration in traffic not found");
                    }
                } else {
                    Log.e("DurationData", "Legs array is empty");
                }
            } else {
                Log.e("DurationData", "Routes array is empty");
            }
        } catch (JSONException e) {
            Log.e("DurationData", "JSON parsing error: " + e.getMessage());
            e.printStackTrace();
        }
        return travelDuration;
    }
    private void updateRouteOnServer(final String bus_number, final String route) {
        try {
            String requestUrl = "https://busgoapplication.com/dvrupdate_route.php"; // Replace with your PHP script URL
            StringRequest stringRequest = new StringRequest(Request.Method.POST, requestUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Handle response
                            Toast.makeText(MainActivity.this, "Route Updated Successfully!", Toast.LENGTH_SHORT).show();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // Handle error
                    Toast.makeText(MainActivity.this, "Failed to Update Route!", Toast.LENGTH_SHORT).show();
                    Log.e("UpdateRouteError", "Failed to update route: " + error.toString());
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bus_number", bus_number);
                    params.put("route", route);
                    return params;
                }
            };

            // Add the request to the RequestQueue.
            Volley.newRequestQueue(this).add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error sending data to server", Toast.LENGTH_SHORT).show();
        }
    }
    public String getElapsedTimeString() {
        return elapsedTimeString;
    }
    private void retrieveBusNumberAndStartLocationUpdates() {
        String url = "https://busgoapplication.com/dvrsignup.php";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        BusNumber = response.trim();
                        if (BusNumber == null || BusNumber.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No bus number retrieved", Toast.LENGTH_SHORT).show();
                        } else {
                            startLocationUpdates();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error retrieving bus number", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(stringRequest);
    }
    private void sendRemainingTimeToServer(String busNumber, double remainingTimeInMinutes) {
        String url = "https://busgoapplication.com/dvrTimeEst.php";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Handle the response from the server
                        Log.i("ServerResponse", "Response: " + response);
                        //Toast.makeText(MainActivity.this, "Time arrival updated successfully!", Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle the error
                        String errorMessage = "Error updating time arrival!";
                        if (error.networkResponse != null) {
                            errorMessage += " Status code: " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    errorMessage += " Response body: " + responseBody;
                                } catch (UnsupportedEncodingException e) {
                                    Log.e("EncodingError", "Unsupported encoding", e);
                                }
                            }
                        }
                        Log.e("VolleyError", errorMessage);
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bus_number", busNumber);
                params.put("remaining_time", String.valueOf(remainingTimeInMinutes));
                return params;
            }
        };

        // Add the request to the RequestQueue
        requestQueue.add(postRequest);
    }
    private void sendTime(String BusNumber, String date, String time, String route) {
        try {
            String requestUrl = "https://busgoapplication.com/driverReports.php"; // Replace with your PHP script URL
            String urlParameters = "bus_number=" + URLEncoder.encode(BusNumber, "UTF-8") +
                    "&date=" + URLEncoder.encode(date, "UTF-8") +
                    "&time=" + URLEncoder.encode(time, "UTF-8") +
                    "&route=" + URLEncoder.encode(route, "UTF-8");
            String fullUrl = requestUrl + "?" + urlParameters;

            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest stringRequest = new StringRequest(Request.Method.POST, requestUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Handle response
                            Toast.makeText(MainActivity.this, "Feedback Sent!", Toast.LENGTH_SHORT).show();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // Handle error
                    Toast.makeText(MainActivity.this, "Feedback Not Sent!", Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("bus_number", BusNumber);
                    params.put("date", date);
                    params.put("time", time);
                    params.put("route", route);
                    return params;
                }
            };

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error sending data to server", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateSpeedUI(Location previousLocation, Location currentLocation) {
        if (previousLocation != null && currentLocation != null) {
            float speedInMetersPerSecond = currentLocation.getSpeed();
            float speedInKilometersPerHour = speedInMetersPerSecond * 3.6f; // Convert to km/h
            speedTextView.setText("Speed: " + String.format("%.2f", speedInKilometersPerHour) + " km/h");

            // Update KalmanFilter average speed
            kalmanFilter.setAverageSpeed(speedInKilometersPerHour);
        }
    }
    private void onNewLocationReceived(Location location) {
        updateSpeedUI(previousLocation, location);
        previousLocation = location;

        // Update KalmanFilter with the new location
        double[] measurement = {location.getLatitude(), location.getLongitude()};
        kalmanFilter.predict();
        kalmanFilter.update(measurement);

        // Update the text views with KalmanFilter information
        updateTextViewsWithLocationInfo(kalmanFilter.getCurrentState()[0], kalmanFilter.getCurrentState()[1]);
    }
}

