package com.example.jomexplore;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

/**
 * The main entry point of the application.
 * This activity displays the main screen and provides navigation to other features,
 * such as the AI Scan functionality.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "onCreate: Starting MainActivity");
            setContentView(R.layout.activity_main);

            // Find the AI Scan button and set a click listener.
            Button btnAiScan = findViewById(R.id.btn_ai_scan);
            Button btnSignIn = findViewById(R.id.btn_sign_in);
            Button btnTicketStore = findViewById(R.id.btn_ticket_store);
            Button btnAchievements = findViewById(R.id.btn_achievements);
            Button btnLeaderboard = findViewById(R.id.btn_leaderboard);
            Button btnRewards = findViewById(R.id.btn_rewards);
            Button btnMap = findViewById(R.id.btn_map);

            if (btnAiScan == null || btnSignIn == null || btnTicketStore == null || btnAchievements == null || btnLeaderboard == null || btnRewards == null || btnMap == null) {
                Log.e(TAG, "A button was not found in the layout");
                Toast.makeText(this, "UI initialization error", Toast.LENGTH_LONG).show();
                return;
            }
            
            btnAiScan.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "AI Scan button clicked");
                    // Create an intent to start CameraActivity.
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(intent);
                    Log.d(TAG, "CameraActivity started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting CameraActivity", e);
                    Toast.makeText(MainActivity.this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            
            // Sign In button click listener
            btnSignIn.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Sign In feature coming soon!", Toast.LENGTH_SHORT).show();
            });

            // Ticket Store button click listener
            btnTicketStore.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Ticket Store feature coming soon!", Toast.LENGTH_SHORT).show();
            });

            // Achievements button click listener
            btnAchievements.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Achievements feature coming soon!", Toast.LENGTH_SHORT).show();
            });

            // Leaderboard button click listener
            btnLeaderboard.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Leaderboard feature coming soon!", Toast.LENGTH_SHORT).show();
            });

            // Rewards button click listener
            btnRewards.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Rewards feature coming soon!", Toast.LENGTH_SHORT).show();
            });

            // Map button click listener
            btnMap.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Map feature coming soon!", Toast.LENGTH_SHORT).show();
            });
            
            Log.d(TAG, "MainActivity initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Failed to initialize app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 