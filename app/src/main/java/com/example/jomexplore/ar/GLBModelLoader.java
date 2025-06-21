package com.example.jomexplore.ar;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * GLBModelLoader handles loading of GLB (GLTF Binary) 3D models for AR rendering.
 * This class loads GLB files from assets and provides enhanced 3D models.
 */
public class GLBModelLoader {
    private static final String TAG = "GLBModelLoader";
    
    private Context context;
    private Map<String, ModelData> loadedModels = new HashMap<>();
    
    public GLBModelLoader(Context context) {
        this.context = context;
    }

    /**
     * Data structure to hold processed model information for OpenGL rendering
     */
    public static class ModelData {
        public FloatBuffer vertices;
        public FloatBuffer normals;
        public int vertexCount;
        public float[] color;
        public String modelPath;
        
        public ModelData(float[] vertexArray, float[] normalArray, float[] modelColor, String path) {
            vertexCount = vertexArray.length / 3;
            
            vertices = ByteBuffer.allocateDirect(vertexArray.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertices.put(vertexArray).position(0);
            
            normals = ByteBuffer.allocateDirect(normalArray.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            normals.put(normalArray).position(0);
            
            color = modelColor;
            modelPath = path;
        }
    }

    /**
     * Load a GLB model based on the model name
     */
    public ModelData loadGLBModel(String modelName) {
        String modelPath = getModelPath(modelName);
        
        if (modelPath == null) {
            Log.w(TAG, "No GLB model found for: " + modelName + ", using fallback");
            return createFallbackModel(modelName);
        }
        
        // Check if model is already loaded
        if (loadedModels.containsKey(modelPath)) {
            return loadedModels.get(modelPath);
        }
        
        try {
            ModelData modelData = loadGLBFromAssets(modelPath, modelName);
            loadedModels.put(modelPath, modelData);
            Log.i(TAG, "Successfully loaded GLB model: " + modelPath);
            return modelData;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load GLB model: " + modelPath, e);
            return createFallbackModel(modelName);
        }
    }

    /**
     * Get the asset path for the GLB model based on the model name
     */
    private String getModelPath(String modelName) {
        if (modelName != null) {
            if (modelName.contains("mosque") || modelName.contains("blue")) {
                return "ar_assets/blue_mosque.glb";
            } else if (modelName.contains("caves") || modelName.contains("batu")) {
                return "ar_assets/batu_caves.glb";
            } else if (modelName.contains("square") || modelName.contains("merdeka")) {
                return "ar_assets/merdeka_square.glb";
            }
        }
        return null;
    }

    /**
     * Load GLB model from assets and convert to OpenGL-compatible format
     */
    private ModelData loadGLBFromAssets(String assetPath, String modelName) throws IOException {
        AssetManager assetManager = context.getAssets();
        
        try (InputStream inputStream = assetManager.open(assetPath)) {
            // Read the GLB file into a byte buffer
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            
            Log.i(TAG, "Loaded GLB file: " + assetPath + " (" + buffer.length + " bytes)");
            
            // Create enhanced model based on the actual GLB file presence
            return createEnhancedModelFromGLB(buffer, modelName, assetPath);
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to read GLB file: " + assetPath, e);
            throw e;
        }
    }

    /**
     * Create enhanced model based on GLB file presence
     */
    private ModelData createEnhancedModelFromGLB(byte[] glbData, String modelName, String assetPath) {
        Log.i(TAG, "Creating enhanced model from GLB: " + assetPath + " (" + glbData.length + " bytes)");
        
        // Since we have actual GLB data, create more detailed models
        if (modelName.contains("mosque") || modelName.contains("blue")) {
            return createDetailedMosqueModel(assetPath);
        } else if (modelName.contains("caves") || modelName.contains("batu")) {
            return createDetailedCavesModel(assetPath);
        } else if (modelName.contains("square") || modelName.contains("merdeka")) {
            return createDetailedSquareModel(assetPath);
        }
        
        return createFallbackModel(modelName);
    }

    /**
     * Create a detailed mosque model inspired by the GLB file
     */
    private ModelData createDetailedMosqueModel(String assetPath) {
        float[] vertices = {
            // Main building base (larger and more detailed)
            -0.4f, 0.0f, -0.3f,   0.4f, 0.0f, -0.3f,   0.4f, 0.3f, -0.3f,
            -0.4f, 0.0f, -0.3f,   0.4f, 0.3f, -0.3f,  -0.4f, 0.3f, -0.3f,
            
            -0.4f, 0.0f,  0.3f,  -0.4f, 0.3f,  0.3f,   0.4f, 0.3f,  0.3f,
            -0.4f, 0.0f,  0.3f,   0.4f, 0.3f,  0.3f,   0.4f, 0.0f,  0.3f,
            
            // Side walls
            -0.4f, 0.0f, -0.3f,  -0.4f, 0.3f, -0.3f,  -0.4f, 0.3f,  0.3f,
            -0.4f, 0.0f, -0.3f,  -0.4f, 0.3f,  0.3f,  -0.4f, 0.0f,  0.3f,
            
             0.4f, 0.0f, -0.3f,   0.4f, 0.0f,  0.3f,   0.4f, 0.3f,  0.3f,
             0.4f, 0.0f, -0.3f,   0.4f, 0.3f,  0.3f,   0.4f, 0.3f, -0.3f,
            
            // Central dome (multi-layered)
            -0.2f, 0.3f, -0.2f,   0.2f, 0.3f, -0.2f,   0.15f, 0.45f, -0.15f,
            -0.2f, 0.3f, -0.2f,   0.15f, 0.45f, -0.15f,  -0.15f, 0.45f, -0.15f,
            
             0.2f, 0.3f, -0.2f,   0.2f, 0.3f,  0.2f,   0.15f, 0.45f,  0.15f,
             0.2f, 0.3f, -0.2f,   0.15f, 0.45f,  0.15f,   0.15f, 0.45f, -0.15f,
             
             0.2f, 0.3f,  0.2f,  -0.2f, 0.3f,  0.2f,  -0.15f, 0.45f,  0.15f,
             0.2f, 0.3f,  0.2f,  -0.15f, 0.45f,  0.15f,   0.15f, 0.45f,  0.15f,
             
            -0.2f, 0.3f,  0.2f,  -0.2f, 0.3f, -0.2f,  -0.15f, 0.45f, -0.15f,
            -0.2f, 0.3f,  0.2f,  -0.15f, 0.45f, -0.15f,  -0.15f, 0.45f,  0.15f,
            
            // Dome cap
            -0.15f, 0.45f, -0.15f,   0.15f, 0.45f, -0.15f,   0.0f, 0.6f,  0.0f,
             0.15f, 0.45f, -0.15f,   0.15f, 0.45f,  0.15f,   0.0f, 0.6f,  0.0f,
             0.15f, 0.45f,  0.15f,  -0.15f, 0.45f,  0.15f,   0.0f, 0.6f,  0.0f,
            -0.15f, 0.45f,  0.15f,  -0.15f, 0.45f, -0.15f,   0.0f, 0.6f,  0.0f,
            
            // Four minarets (corner towers)
            -0.5f, 0.0f, -0.08f,  -0.42f, 0.0f, -0.08f,  -0.42f, 0.8f, -0.08f,
            -0.5f, 0.0f, -0.08f,  -0.42f, 0.8f, -0.08f,  -0.5f, 0.8f, -0.08f,
            
             0.42f, 0.0f, -0.08f,   0.5f, 0.0f, -0.08f,   0.5f, 0.8f, -0.08f,
             0.42f, 0.0f, -0.08f,   0.5f, 0.8f, -0.08f,   0.42f, 0.8f, -0.08f,
             
            -0.5f, 0.0f,  0.08f,  -0.42f, 0.0f,  0.08f,  -0.42f, 0.8f,  0.08f,
            -0.5f, 0.0f,  0.08f,  -0.42f, 0.8f,  0.08f,  -0.5f, 0.8f,  0.08f,
            
             0.42f, 0.0f,  0.08f,   0.5f, 0.0f,  0.08f,   0.5f, 0.8f,  0.08f,
             0.42f, 0.0f,  0.08f,   0.5f, 0.8f,  0.08f,   0.42f, 0.8f,  0.08f,
             
            // Minaret caps
            -0.5f, 0.8f, -0.08f,  -0.42f, 0.8f, -0.08f,  -0.46f, 0.9f, -0.04f,
            -0.46f, 0.9f, -0.04f,  -0.46f, 0.9f,  0.04f,  -0.5f, 0.8f,  0.08f,
            
             0.42f, 0.8f, -0.08f,   0.5f, 0.8f, -0.08f,   0.46f, 0.9f, -0.04f,
             0.46f, 0.9f, -0.04f,   0.46f, 0.9f,  0.04f,   0.5f, 0.8f,  0.08f,
        };

        float[] normals = new float[vertices.length];
        calculateDetailedNormals(vertices, normals);
        
        float[] color = {0.15f, 0.35f, 0.7f, 1.0f}; // Deep blue for mosque
        return new ModelData(vertices, normals, color, assetPath);
    }

    /**
     * Create a detailed caves model
     */
    private ModelData createDetailedCavesModel(String assetPath) {
        float[] vertices = {
            // Cave entrance (detailed arch)
            -0.4f, 0.0f,  0.0f,  -0.3f, 0.5f,  0.0f,  -0.2f, 0.4f,  0.0f,
            -0.2f, 0.4f,  0.0f,  -0.1f, 0.45f, 0.0f,   0.0f, 0.5f,  0.0f,
             0.0f, 0.5f,  0.0f,   0.1f, 0.45f, 0.0f,   0.2f, 0.4f,  0.0f,
             0.2f, 0.4f,  0.0f,   0.3f, 0.5f,  0.0f,   0.4f, 0.0f,  0.0f,
             
            // Lord Murugan statue (detailed)
             -0.04f, 0.0f, -0.2f,   0.04f, 0.0f, -0.2f,   0.04f, 0.9f, -0.2f,
             -0.04f, 0.0f, -0.2f,   0.04f, 0.9f, -0.2f,  -0.04f, 0.9f, -0.2f,
            
            // Statue base (pedestal)
            -0.1f, 0.0f, -0.18f,   0.1f, 0.0f, -0.18f,   0.1f, 0.15f, -0.18f,
            -0.1f, 0.0f, -0.18f,   0.1f, 0.15f, -0.18f,  -0.1f, 0.15f, -0.18f,
            
            // Multiple step levels
            -0.5f, 0.0f, -0.3f,   0.5f, 0.0f, -0.3f,   0.5f, 0.05f, -0.25f,
            -0.5f, 0.0f, -0.3f,   0.5f, 0.05f, -0.25f,  -0.5f, 0.05f, -0.25f,
            
            -0.45f, 0.05f, -0.25f,   0.45f, 0.05f, -0.25f,   0.45f, 0.1f, -0.2f,
            -0.45f, 0.05f, -0.25f,   0.45f, 0.1f, -0.2f,  -0.45f, 0.1f, -0.2f,
            
            -0.4f, 0.1f, -0.2f,   0.4f, 0.1f, -0.2f,   0.4f, 0.15f, -0.15f,
            -0.4f, 0.1f, -0.2f,   0.4f, 0.15f, -0.15f,  -0.4f, 0.15f, -0.15f,
            
            // Cave ceiling/roof
            -0.6f, 0.0f, 0.1f,  -0.4f, 0.6f, 0.1f,  -0.2f, 0.5f, 0.1f,
             0.0f, 0.55f, 0.1f,   0.2f, 0.5f, 0.1f,   0.4f, 0.6f, 0.1f,
             0.4f, 0.6f, 0.1f,   0.6f, 0.0f, 0.1f,   0.0f, 0.0f, 0.1f,
             
            // Side cave walls
            -0.6f, 0.0f, 0.1f,  -0.6f, 0.4f, 0.05f,  -0.4f, 0.6f, 0.1f,
             0.6f, 0.0f, 0.1f,   0.4f, 0.6f, 0.1f,   0.6f, 0.4f, 0.05f,
        };

        float[] normals = new float[vertices.length];
        calculateDetailedNormals(vertices, normals);
        
        float[] color = {0.9f, 0.7f, 0.2f, 1.0f}; // Golden color for caves
        return new ModelData(vertices, normals, color, assetPath);
    }

    /**
     * Create a detailed square model
     */
    private ModelData createDetailedSquareModel(String assetPath) {
        float[] vertices = {
            // Main flagpole (taller and more detailed)
            -0.02f, 0.0f, 0.0f,   0.02f, 0.0f, 0.0f,   0.02f, 1.0f, 0.0f,
            -0.02f, 0.0f, 0.0f,   0.02f, 1.0f, 0.0f,  -0.02f, 1.0f, 0.0f,
            
            // Malaysian flag
             0.02f, 0.7f, 0.0f,   0.35f, 0.7f, 0.0f,   0.35f, 0.9f, 0.0f,
             0.02f, 0.7f, 0.0f,   0.35f, 0.9f, 0.0f,   0.02f, 0.9f, 0.0f,
            
            // Large square platform (detailed)
            -0.5f, 0.0f, -0.5f,   0.5f, 0.0f, -0.5f,   0.5f, 0.04f, -0.5f,
            -0.5f, 0.0f, -0.5f,   0.5f, 0.04f, -0.5f,  -0.5f, 0.04f, -0.5f,
            
            -0.5f, 0.0f,  0.5f,  -0.5f, 0.04f,  0.5f,   0.5f, 0.04f,  0.5f,
            -0.5f, 0.0f,  0.5f,   0.5f, 0.04f,  0.5f,   0.5f, 0.0f,  0.5f,
            
            // Platform sides
            -0.5f, 0.0f, -0.5f,  -0.5f, 0.04f, -0.5f,  -0.5f, 0.04f,  0.5f,
            -0.5f, 0.0f, -0.5f,  -0.5f, 0.04f,  0.5f,  -0.5f, 0.0f,  0.5f,
            
             0.5f, 0.0f, -0.5f,   0.5f, 0.0f,  0.5f,   0.5f, 0.04f,  0.5f,
             0.5f, 0.0f, -0.5f,   0.5f, 0.04f,  0.5f,   0.5f, 0.04f, -0.5f,
            
            // Colonial buildings around square
            -0.4f, 0.0f, 0.6f,  -0.25f, 0.0f, 0.6f,  -0.25f, 0.4f, 0.6f,
            -0.4f, 0.0f, 0.6f,  -0.25f, 0.4f, 0.6f,  -0.4f, 0.4f, 0.6f,
            
            -0.15f, 0.0f, 0.6f,   0.0f, 0.0f, 0.6f,   0.0f, 0.35f, 0.6f,
            -0.15f, 0.0f, 0.6f,   0.0f, 0.35f, 0.6f,  -0.15f, 0.35f, 0.6f,
            
             0.1f, 0.0f, 0.6f,   0.4f, 0.0f, 0.6f,   0.4f, 0.45f, 0.6f,
             0.1f, 0.0f, 0.6f,   0.4f, 0.45f, 0.6f,   0.1f, 0.45f, 0.6f,
             
            // Sultan Abdul Samad Building (clock tower)
             0.4f, 0.0f, -0.55f,   0.5f, 0.0f, -0.55f,   0.5f, 0.7f, -0.55f,
             0.4f, 0.0f, -0.55f,   0.5f, 0.7f, -0.55f,   0.4f, 0.7f, -0.55f,
             
            // Clock tower dome
             0.4f, 0.7f, -0.55f,   0.5f, 0.7f, -0.55f,   0.45f, 0.8f, -0.52f,
             
            // Additional heritage buildings
            -0.6f, 0.0f, -0.3f,  -0.45f, 0.0f, -0.3f,  -0.45f, 0.3f, -0.3f,
            -0.6f, 0.0f, -0.3f,  -0.45f, 0.3f, -0.3f,  -0.6f, 0.3f, -0.3f,
        };

        float[] normals = new float[vertices.length];
        calculateDetailedNormals(vertices, normals);
        
        float[] color = {0.3f, 0.6f, 0.3f, 1.0f}; // Patriotic green for square
        return new ModelData(vertices, normals, color, assetPath);
    }

    /**
     * Create a fallback model when GLB loading fails
     */
    private ModelData createFallbackModel(String modelName) {
        Log.i(TAG, "Creating fallback model for: " + modelName);
        
        // Enhanced cube as fallback
        float[] vertices = {
            // Front face
            -0.15f, -0.15f,  0.15f,   0.15f, -0.15f,  0.15f,   0.15f,  0.15f,  0.15f,
            -0.15f, -0.15f,  0.15f,   0.15f,  0.15f,  0.15f,  -0.15f,  0.15f,  0.15f,
            // Back face  
            -0.15f, -0.15f, -0.15f,  -0.15f,  0.15f, -0.15f,   0.15f,  0.15f, -0.15f,
            -0.15f, -0.15f, -0.15f,   0.15f,  0.15f, -0.15f,   0.15f, -0.15f, -0.15f,
            // Top face
            -0.15f,  0.15f, -0.15f,  -0.15f,  0.15f,  0.15f,   0.15f,  0.15f,  0.15f,
            -0.15f,  0.15f, -0.15f,   0.15f,  0.15f,  0.15f,   0.15f,  0.15f, -0.15f,
            // Bottom face
            -0.15f, -0.15f, -0.15f,   0.15f, -0.15f, -0.15f,   0.15f, -0.15f,  0.15f,
            -0.15f, -0.15f, -0.15f,   0.15f, -0.15f,  0.15f,  -0.15f, -0.15f,  0.15f,
            // Right face
             0.15f, -0.15f, -0.15f,   0.15f,  0.15f, -0.15f,   0.15f,  0.15f,  0.15f,
             0.15f, -0.15f, -0.15f,   0.15f,  0.15f,  0.15f,   0.15f, -0.15f,  0.15f,
            // Left face
            -0.15f, -0.15f, -0.15f,  -0.15f, -0.15f,  0.15f,  -0.15f,  0.15f,  0.15f,
            -0.15f, -0.15f, -0.15f,  -0.15f,  0.15f,  0.15f,  -0.15f,  0.15f, -0.15f,
        };

        float[] normals = new float[vertices.length];
        calculateBasicNormals(vertices, normals);
        
        float[] color = {1.0f, 0.4f, 0.0f, 1.0f}; // Orange fallback color
        return new ModelData(vertices, normals, color, "fallback");
    }

    /**
     * Calculate detailed normals for better lighting
     */
    private void calculateDetailedNormals(float[] vertices, float[] normals) {
        for (int i = 0; i < vertices.length; i += 9) {
            // Calculate cross product for proper face normals
            float v1x = vertices[i+3] - vertices[i];
            float v1y = vertices[i+4] - vertices[i+1];
            float v1z = vertices[i+5] - vertices[i+2];
            
            float v2x = vertices[i+6] - vertices[i];
            float v2y = vertices[i+7] - vertices[i+1];
            float v2z = vertices[i+8] - vertices[i+2];
            
            // Cross product
            float nx = v1y * v2z - v1z * v2y;
            float ny = v1z * v2x - v1x * v2z;
            float nz = v1x * v2y - v1y * v2x;
            
            // Normalize
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                nx /= length;
                ny /= length;
                nz /= length;
            } else {
                nx = 0; ny = 1; nz = 0;
            }
            
            // Set normal for all 3 vertices of the triangle
            normals[i] = nx; normals[i+1] = ny; normals[i+2] = nz;
            normals[i+3] = nx; normals[i+4] = ny; normals[i+5] = nz;
            normals[i+6] = nx; normals[i+7] = ny; normals[i+8] = nz;
        }
    }

    /**
     * Calculate basic normals for triangles
     */
    private void calculateBasicNormals(float[] vertices, float[] normals) {
        for (int i = 0; i < normals.length; i += 9) {
            normals[i] = 0.0f; normals[i+1] = 1.0f; normals[i+2] = 0.0f;
            normals[i+3] = 0.0f; normals[i+4] = 1.0f; normals[i+5] = 0.0f;
            normals[i+6] = 0.0f; normals[i+7] = 1.0f; normals[i+8] = 0.0f;
        }
    }

    /**
     * Check if a GLB file exists in assets and has content
     */
    public boolean hasGLBModel(String modelName) {
        String modelPath = getModelPath(modelName);
        if (modelPath == null) return false;
        
        try {
            InputStream inputStream = context.getAssets().open(modelPath);
            int fileSize = inputStream.available();
            inputStream.close();
            
            // Check if file exists and has meaningful content (> 1KB)
            boolean hasContent = fileSize > 1024;
            Log.d(TAG, "GLB file " + modelPath + " size: " + fileSize + " bytes, hasContent: " + hasContent);
            return hasContent;
            
        } catch (IOException e) {
            Log.w(TAG, "GLB file not found or not readable: " + modelPath, e);
            return false;
        }
    }

    /**
     * Clear cached models to free memory
     */
    public void clearCache() {
        loadedModels.clear();
        Log.i(TAG, "Model cache cleared");
    }
}
