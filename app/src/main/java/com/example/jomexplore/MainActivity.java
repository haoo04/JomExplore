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
            
            if (btnAiScan == null) {
                Log.e(TAG, "AI Scan button not found in layout");
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
            
            Log.d(TAG, "MainActivity initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Failed to initialize app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 