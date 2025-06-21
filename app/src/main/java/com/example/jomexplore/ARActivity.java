package com.example.jomexplore;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;

import com.example.jomexplore.ar.ARRenderer;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.CameraNotAvailableException;

/**
 * ARActivity is responsible for displaying 3D models in an Augmented Reality view.
 * It checks for ARCore support on the device and loads the appropriate model based on
 * the recognition result from the previous activity.
 */
public class ARActivity extends AppCompatActivity {

    private static final String TAG = "ARActivity";
    
    private String modelName;
    private TextView arStatusText;
    private Button backButton;
    private GLSurfaceView glSurfaceView;
    private ARRenderer arRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "onCreate: Starting ARActivity");
            
            // Retrieve the detected model name from the Intent.
            modelName = getIntent().getStringExtra("model_name");
            Log.d(TAG, "Model name received: " + modelName);
            
            if (modelName == null || modelName.isEmpty()) {
                Log.w(TAG, "No model name provided, using default");
                modelName = "blue_mosque"; // Default model
            }
            
            // Check if ARCore is supported on the device before proceeding.
            if (!checkIsSupportedDeviceOrFinish()) {
                Log.e(TAG, "ARCore not supported on this device");
                Toast.makeText(this, "ARCore is not supported or not installed.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            Log.d(TAG, "ARCore support verified");
            setContentView(R.layout.activity_ar);

            // Initialize UI components.
            glSurfaceView = findViewById(R.id.gl_surface_view);
            arStatusText = findViewById(R.id.ar_status_text);
            backButton = findViewById(R.id.btn_back);
            
            if (glSurfaceView == null || arStatusText == null || backButton == null) {
                Log.e(TAG, "Failed to find required UI components");
                Toast.makeText(this, "UI initialization failed", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            Log.d(TAG, "UI components initialized successfully");
            
            // Set up the user interface.
            setupUI();
            
            // Initialize the renderer and set it to the GLSurfaceView.
            try {
                // Configure OpenGL ES 2.0 context for ARCore
                glSurfaceView.setEGLContextClientVersion(2);
                glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
                
                arRenderer = new ARRenderer(this, modelName);
                glSurfaceView.setRenderer(arRenderer);
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                glSurfaceView.setWillNotDraw(false);
                Log.d(TAG, "ARRenderer initialized and set to GLSurfaceView");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize ARRenderer", e);
                Toast.makeText(this, "Failed to initialize AR renderer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // Set up touch listener for placing AR objects
            setupTouchListener();
            
            Log.d(TAG, "ARActivity onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Failed to initialize AR: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        
        try {
            if (glSurfaceView != null) {
                glSurfaceView.onResume();
                Log.d(TAG, "GLSurfaceView resumed");
            }
            
            if (arRenderer != null) {
                arRenderer.createSession();
                Log.d(TAG, "AR session created/resumed");
            }
            
            // Start debugging timer
            startARDebugging();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
            Toast.makeText(this, "Error resuming AR session: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        
        try {
            if (glSurfaceView != null) {
                glSurfaceView.onPause();
                Log.d(TAG, "GLSurfaceView paused");
            }
            
            if (arRenderer != null) {
                arRenderer.pauseSession();
                Log.d(TAG, "AR session paused");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        
        try {
            if (arRenderer != null) {
                arRenderer.destroySession();
                Log.d(TAG, "AR session destroyed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    /**
     * Sets up the user interface, including text views and button listeners.
     */
    private void setupUI() {
        // Update the status text to display the name of the recognized model.
        String statusText = "AR View: " + getModelDisplayName() + "\nTap to place model";
        arStatusText.setText(statusText);
        
        // Set up the back button to return to the previous activity.
        backButton.setOnClickListener(v -> {
            finish(); // Closes this activity and returns to the previous one.
        });
    }

    /**
     * Sets up touch listener for the GLSurfaceView to handle tap events for placing AR objects.
     */
    private void setupTouchListener() {
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    // Get the touch coordinates
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    
                    Log.d(TAG, "Touch event at coordinates: (" + x + ", " + y + ")");
                    
                    // Pass the touch event to the AR renderer
                    if (arRenderer != null) {
                        Log.d(TAG, "Passing touch event to ARRenderer");
                        arRenderer.onTap(x, y);
                        
                        // Update status text to show model placement
                        runOnUiThread(() -> {
                            String statusText = "AR View: " + getModelDisplayName() + "\nModel placed! Tap to place more";
                            arStatusText.setText(statusText);
                            Log.d(TAG, "Updated status text after touch");
                        });
                    } else {
                        Log.w(TAG, "ARRenderer is null, cannot process touch event");
                    }
                    
                    return true;
                }
                return false;
            }
        });
        
        Log.d(TAG, "Touch listener setup completed");
    }

    /**
     * Determines the display name for the 3D model based on the model's filename.
     * @return A user-friendly name for the detected heritage site model.
     */
    private String getModelDisplayName() {
        if (modelName != null) {
            if (modelName.contains("mosque")) {
                return "Blue Mosque Model";
            } else if (modelName.contains("caves")) {
                return "Batu Caves Model";
            } else if (modelName.contains("square")) {
                return "Merdeka Square Model";
            }
        }
        return "Heritage Site Model"; // Default name if no specific model is identified.
    }

    /**
     * Checks if the device supports ARCore. If not, it finishes the activity.
     * This method checks the availability of ARCore and handles cases where it's unsupported or needs an update.
     * @return `true` if the device is supported, `false` otherwise.
     */
    private boolean checkIsSupportedDeviceOrFinish() {
        Log.d(TAG, "Checking ARCore support");
        
        try {
            ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
            Log.d(TAG, "ARCore availability: " + availability);
            
            if (availability.isTransient()) {
                Log.d(TAG, "ARCore availability is transient, rechecking...");
                // Re-query at 5Hz while compatibility is being checked in the background.
                new android.os.Handler().postDelayed(this::checkIsSupportedDeviceOrFinish, 200);
                return true; // Allow the activity to continue while checking
            }
            
            if (availability.isSupported()) {
                Log.d(TAG, "ARCore is supported");
                return true;
            } else {
                Log.e(TAG, "ARCore is not supported: " + availability);
                String message;
                switch (availability) {
                    case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                        message = "This device does not support AR";
                        break;
                    case UNKNOWN_CHECKING:
                        message = "Checking AR support...";
                        break;
                    case UNKNOWN_ERROR:
                        message = "Unknown AR error occurred";
                        break;
                    case UNKNOWN_TIMED_OUT:
                        message = "AR support check timed out";
                        break;
                    default:
                        message = "ARCore is not supported on this device";
                        break;
                }
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking ARCore support", e);
            Toast.makeText(this, "Error checking AR support: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
    }

    /**
     * Debugging method to periodically check AR status
     */
    private void startARDebugging() {
        android.os.Handler handler = new android.os.Handler();
        Runnable debugRunnable = new Runnable() {
            @Override
            public void run() {
                if (arRenderer != null) {
                    Log.d(TAG, "=== AR DEBUG STATUS ===");
                    Log.d(TAG, "Model name: " + modelName);
                    Log.d(TAG, "GLSurfaceView: " + (glSurfaceView != null ? "OK" : "NULL"));
                    Log.d(TAG, "ARRenderer: " + (arRenderer != null ? "OK" : "NULL"));
                    Log.d(TAG, "=======================");
                }
                
                // Schedule next debug check in 5 seconds
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(debugRunnable);
    }
} 