package com.example.jomexplore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.jomexplore.utils.BitmapUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraActivity manages the camera functionality for the application.
 * It handles camera permissions, displays a live camera preview, captures images,
 * and initiates the image classification process.
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private Button captureButton;
    private ImageCapture imageCapture;
    private ImageClassifier imageClassifier;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "onCreate: Starting CameraActivity");
            setContentView(R.layout.activity_camera);

            // Initialize UI components
            previewView = findViewById(R.id.preview_view);
            captureButton = findViewById(R.id.btn_capture);
            
            if (previewView == null || captureButton == null) {
                Log.e(TAG, "Failed to find UI components");
                Toast.makeText(this, "UI components not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Initialize the custom ImageClassifier with error handling
            try {
                imageClassifier = new ImageClassifier(this);
                Log.d(TAG, "ImageClassifier initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize ImageClassifier", e);
                Toast.makeText(this, "Failed to initialize AI model", Toast.LENGTH_LONG).show();
                // Continue without classifier - user can still use camera
            }

            // Create a single-threaded executor for camera operations.
            cameraExecutor = Executors.newSingleThreadExecutor();

            // Set a click listener for the capture button.
            captureButton.setOnClickListener(v -> captureImage());

            // Check for camera permissions before starting the camera.
            if (isCameraPermissionGranted()) {
                Log.d(TAG, "Camera permission already granted");
                startCamera();
            } else {
                Log.d(TAG, "Requesting camera permission");
                requestCameraPermission();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Checks if the app has been granted camera permission.
     * @return `true` if camera permission is granted, `false` otherwise.
     */
    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests camera permission from the user.
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    /**
     * Initializes and starts the camera preview.
     * It gets an instance of ProcessCameraProvider and binds the camera lifecycle to this activity.
     */
    private void startCamera() {
        try {
            Log.d(TAG, "Starting camera");
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                    Log.d(TAG, "Camera started successfully");
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error starting camera", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error starting camera", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Unexpected camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize camera provider", e);
            Toast.makeText(this, "Failed to initialize camera", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Binds the camera preview to the PreviewView.
     * This sets up the camera preview, image capture use case, and camera selector.
     * @param cameraProvider The ProcessCameraProvider to bind the use cases to.
     */
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        try {
            Log.d(TAG, "Binding camera preview");
            
            Preview preview = new Preview.Builder().build();

            // Set up the ImageCapture use case.
            imageCapture = new ImageCapture.Builder().build();

            // Select the back camera as the default.
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            // Attach the preview's surface provider to the preview view.
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            // Unbind any existing use cases before rebinding.
            cameraProvider.unbindAll();
            
            // Bind the camera provider to the activity's lifecycle.
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            
            Log.d(TAG, "Camera preview bound successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera preview", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error setting up camera preview: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    /**
     * Captures an image and saves it to a file.
     * Once the image is saved, it triggers the image processing step.
     */
    private void captureImage() {
        if (imageCapture == null) {
            Log.w(TAG, "ImageCapture not initialized");
            Toast.makeText(this, "Camera not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.d(TAG, "Capturing image");
            
            // Create a directory to store captured images.
            File outputDirectory = new File(getExternalFilesDir(null), "captured_images");
            if (!outputDirectory.exists()) {
                boolean created = outputDirectory.mkdirs();
                if (!created) {
                    Log.e(TAG, "Failed to create output directory");
                    Toast.makeText(this, "Failed to create image directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Create a file to save the captured image.
            File photoFile = new File(outputDirectory, "captured_image_" + System.currentTimeMillis() + ".jpg");

            // Set up output options for the image capture.
            ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            // Take the picture and handle the result in a callback.
            imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                            Log.d(TAG, "Image saved successfully: " + photoFile.getAbsolutePath());
                            // On successful save, process the image.
                            runOnUiThread(() -> {
                                Toast.makeText(CameraActivity.this, "Photo captured successfully", Toast.LENGTH_SHORT).show();
                                processImage(photoFile.getAbsolutePath());
                            });
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Photo capture failed", exception);
                            // Handle image capture errors.
                            runOnUiThread(() -> {
                                Toast.makeText(CameraActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error during image capture", e);
            Toast.makeText(this, "Error capturing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Processes the captured image to classify its content.
     * This method runs on a background thread to avoid blocking the UI.
     * @param imagePath The file path of the captured image.
     */
    private void processImage(String imagePath) {
        // Run image processing on a background thread.
        new Thread(() -> {
            try {
                Log.d(TAG, "Processing image: " + imagePath);
                
                // Load a scaled and correctly oriented bitmap for classification.
                Bitmap bitmap = BitmapUtils.getCorrectlyOrientedBitmap(imagePath, 1024, 1024);
                
                if (bitmap == null) {
                    Log.e(TAG, "Failed to load bitmap from: " + imagePath);
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Failed to load image for processing.", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                Log.d(TAG, "Bitmap loaded successfully, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                
                String recognitionResult;
                if (imageClassifier != null) {
                    // Classify the image using the custom ImageClassifier.
                    recognitionResult = imageClassifier.classifyImage(bitmap);
                    Log.d(TAG, "Classification result: " + recognitionResult);
                } else {
                    Log.w(TAG, "ImageClassifier not available, using default message");
                    recognitionResult = "Image captured successfully. AI model not available.";
                }
                
                // Start the result activity to display the classification result.
                runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(CameraActivity.this, RecognitionResultActivity.class);
                        intent.putExtra("image_path", imagePath);
                        intent.putExtra("recognition_result", recognitionResult);
                        startActivity(intent);
                        Log.d(TAG, "Started RecognitionResultActivity");
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting RecognitionResultActivity", e);
                        Toast.makeText(CameraActivity.this, "Error displaying results: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing image", e);
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                startCamera();
            } else {
                Log.w(TAG, "Camera permission denied");
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_LONG).show();
                finish(); // Close the activity if permission is denied
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        
        if (imageClassifier != null) {
            try {
                imageClassifier.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing ImageClassifier", e);
            }
        }
    }
} 