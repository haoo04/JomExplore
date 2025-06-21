package com.example.jomexplore;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.content.Intent;

import com.example.jomexplore.utils.BitmapUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RecognitionResultActivity displays the results of the image classification.
 * It shows the captured image, the name of the recognized landmark, and provides an option
 * to view the landmark in Augmented Reality (AR).
 */
public class RecognitionResultActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView resultText;
    private Button btnViewAR;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_result);

        // Initialize UI components.
        imageView = findViewById(R.id.captured_image);
        resultText = findViewById(R.id.recognition_result);
        btnViewAR = findViewById(R.id.btn_view_ar);

        // Retrieve data from the intent.
        String imagePath = getIntent().getStringExtra("image_path");
        String recognitionResult = getIntent().getStringExtra("recognition_result");

        // Load and display the captured image.
        if (imagePath != null) {
            loadAndDisplayImage(imagePath);
        }

        // Display the recognition result text.
        if (recognitionResult != null) {
            resultText.setText(recognitionResult);
        }

        // Set a click listener for the "View in AR" button.
        btnViewAR.setOnClickListener(v -> {
            try {
                android.util.Log.d("RecognitionResultActivity", "View AR button clicked");
                android.util.Log.d("RecognitionResultActivity", "Recognition result: " + recognitionResult);
                
                String modelName = getModelNameFromResult(recognitionResult);
                if (modelName != null) {
                    android.util.Log.d("RecognitionResultActivity", "Starting ARActivity with model: " + modelName);
                    // If a model is available, start the ARActivity.
                    Intent intent = new Intent(RecognitionResultActivity.this, ARActivity.class);
                    intent.putExtra("model_name", modelName);
                    startActivity(intent);
                    android.util.Log.d("RecognitionResultActivity", "ARActivity start requested");
                } else {
                    android.util.Log.w("RecognitionResultActivity", "No model name determined for result: " + recognitionResult);
                    // Inform the user if no 3D model is available.
                    Toast.makeText(this, "No 3D model available for this landmark.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                android.util.Log.e("RecognitionResultActivity", "Error starting ARActivity", e);
                Toast.makeText(this, "Error starting AR view: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Determines the model name based on the recognition result string.
     * @param result The string containing the name of the recognized landmark.
     * @return The model identifier, or null if not found.
     */
    private String getModelNameFromResult(String result) {
        if (result == null) return null;
        
        android.util.Log.d("RecognitionResultActivity", "Getting model name for result: " + result);
        
        String modelName = null;
        if (result.contains("Merdeka Square")) {
            modelName = "merdeka_square";
        } else if (result.contains("Blue Mosque")) {
            modelName = "blue_mosque";
        } else if (result.contains("Batu Caves")) {
            modelName = "batu_caves";
        }
        
        android.util.Log.d("RecognitionResultActivity", "Model name determined: " + modelName);
        return modelName;
    }

    /**
     * Loads the captured image from the given path and displays it in the ImageView.
     * This method uses a ViewTreeObserver to get the dimensions of the ImageView
     * and then loads a properly scaled and oriented bitmap on a background thread.
     * @param path The file path of the image to load.
     */
    private void loadAndDisplayImage(String path) {
        // Use a ViewTreeObserver to wait for the layout to be complete to get the view's dimensions.
        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove the listener to avoid it being called repeatedly.
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int targetW = imageView.getWidth();
                int targetH = imageView.getHeight();

                // Use a fallback size if the view dimensions are not available.
                if (targetW <= 0 || targetH <= 0) {
                    targetW = 1024; // Fallback size
                    targetH = 1024;
                }

                final int finalTargetW = targetW;
                final int finalTargetH = targetH;

                // Load the bitmap on a background thread.
                executor.execute(() -> {
                    try {
                        final Bitmap bitmap = BitmapUtils.getCorrectlyOrientedBitmap(path, finalTargetW, finalTargetH);
                        // Update the UI on the main thread.
                        runOnUiThread(() -> {
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                            } else {
                                Toast.makeText(RecognitionResultActivity.this, "Failed to decode image.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(RecognitionResultActivity.this, "Failed to load image.", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the executor service to release resources.
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
} 