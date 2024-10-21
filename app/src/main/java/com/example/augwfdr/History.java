package com.example.augwfdr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class History extends AppCompatActivity {

    private static final String URL = "https://busgoapplication.com/get_driver_reports.php?bus_number=";// Replace with actual email
    private TextView textView;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String bus_number;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        bus_number = getIntent().getStringExtra("bus_number");
        textView = findViewById(R.id.textView);
        fetchHistoryData(bus_number);
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to MainActivity when the back button is pressed
                Intent intent = new Intent(History.this, MainActivity.class);
                intent.putExtra("bus_number",bus_number);
                startActivity(intent);
                finish();
            }
        });
    }

    private void fetchHistoryData(String bus_number) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = URL + bus_number;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        displayHistory(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        textView.setText("Error fetching data: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bus_number", bus_number);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    private void displayHistory(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            StringBuilder displayResult = new StringBuilder();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                displayResult.append("Bus Number: ").append(jsonObject.getString("bus_number")).append("\n")
                        .append("Date: ").append(jsonObject.getString("date")).append("\n")
                        .append("Time: ").append(jsonObject.getString("time")).append("\n")
                        .append("Route: ").append(jsonObject.getString("route")).append("\n\n");
            }
            textView.setText(displayResult.toString());
        } catch (JSONException e) {
            textView.setText("Error parsing data: " + e.getMessage());
        }
    }
}