package com.example.augwfdr;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class Announcements extends AppCompatActivity {

    private ListView announcementsListView;
    private Button goBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        announcementsListView = findViewById(R.id.announcementsListView);
        goBackButton = findViewById(R.id.goBackButton);

        // Sample data for announcements
        List<String> announcements = new ArrayList<>();
        announcements.add("Announcement 1: Important update regarding schedules.");
        announcements.add("Announcement 2: New bus routes available.");
        announcements.add("Announcement 3: Maintenance work scheduled for this weekend.");
        // Add more announcements as needed

        // Set up the ListView with an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, announcements);
        announcementsListView.setAdapter(adapter);

        // Set up the Go Back button
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // Close the current activity and return to the previous one
            }
        });
    }
}
