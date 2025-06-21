package com.example.jomexplore.ar;

import android.content.Context;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ModelLoader handles loading of different 3D model types for AR rendering.
 * This class now uses GLBModelLoader to load actual GLB files from assets when available,
 * falling back to procedural models when needed.
 */
public class ModelLoader {
    private static final String TAG = "ModelLoader";
    private static GLBModelLoader glbLoader;

    /**
     * Data structure to hold model information (compatible with GLBModelLoader)
     */
    public static class ModelData {
        public FloatBuffer vertices;
        public FloatBuffer normals;
        public int vertexCount;
        public float[] color;
        public String modelPath;
        
        public ModelData(float[] vertexArray, float[] normalArray, float[] modelColor) {
            this(vertexArray, normalArray, modelColor, "procedural");
        }
        
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
     * Load model data based on the model name
     * Now prioritizes GLB files from assets, falls back to procedural models
     */
    public static ModelData loadModel(Context context, String modelName) {
        // Initialize GLB loader if not already done
        if (glbLoader == null) {
            glbLoader = new GLBModelLoader(context);
        }
        
        Log.i(TAG, "=== Loading model for: " + modelName + " ===");
        
        // Try to load GLB model first
        boolean hasGLB = glbLoader.hasGLBModel(modelName);
        Log.i(TAG, "GLB model available: " + hasGLB);
        
        if (hasGLB) {
            Log.i(TAG, "Loading GLB model from assets for: " + modelName);
            GLBModelLoader.ModelData glbModel = glbLoader.loadGLBModel(modelName);
            
            // Convert GLBModelLoader.ModelData to our ModelData format
            ModelData result = convertGLBModelData(glbModel);
            Log.i(TAG, "Successfully loaded GLB model with " + result.vertexCount + " vertices");
            return result;
        }
        
        // Fallback to procedural models
        Log.i(TAG, "GLB model not found, using procedural model for: " + modelName);
        ModelData result = null;
        String modelType = "unknown";
        
        if (modelName != null) {
            if (modelName.contains("mosque") || modelName.contains("blue")) {
                result = createBlueMosqueModel();
                modelType = "Blue Mosque";
            } else if (modelName.contains("caves") || modelName.contains("batu")) {
                result = createBatuCavesModel();
                modelType = "Batu Caves";
            } else if (modelName.contains("square") || modelName.contains("merdeka")) {
                result = createMerdekaSquareModel();
                modelType = "Merdeka Square";
            }
        }
        
        if (result == null) {
            // Default cube model
            result = createDefaultModel();
            modelType = "Default Cube";
        }
        
        Log.i(TAG, "Created " + modelType + " procedural model with " + result.vertexCount + " vertices");
        Log.i(TAG, "Model color: [" + result.color[0] + ", " + result.color[1] + ", " + result.color[2] + ", " + result.color[3] + "]");
        Log.i(TAG, "=== Model loading completed ===");
        
        return result;
    }
    
    /**
     * Convert GLBModelLoader.ModelData to our ModelData format
     */
    private static ModelData convertGLBModelData(GLBModelLoader.ModelData glbData) {
        // Extract vertex data from GLB model
        float[] vertices = new float[glbData.vertices.capacity()];
        glbData.vertices.position(0);
        glbData.vertices.get(vertices);
        
        // Extract normal data from GLB model
        float[] normals = new float[glbData.normals.capacity()];
        glbData.normals.position(0);
        glbData.normals.get(normals);
        
        return new ModelData(vertices, normals, glbData.color, glbData.modelPath);
    }

    /**
     * Create a simplified Blue Mosque model
     */
    private static ModelData createBlueMosqueModel() {
        float[] vertices = {
            // Base structure (rectangular base)
            -0.2f, 0.0f, -0.15f,   0.2f, 0.0f, -0.15f,   0.2f, 0.2f, -0.15f,
            -0.2f, 0.0f, -0.15f,   0.2f, 0.2f, -0.15f,  -0.2f, 0.2f, -0.15f,
            
            -0.2f, 0.0f,  0.15f,  -0.2f, 0.2f,  0.15f,   0.2f, 0.2f,  0.15f,
            -0.2f, 0.0f,  0.15f,   0.2f, 0.2f,  0.15f,   0.2f, 0.0f,  0.15f,
            
            // Dome (pyramid-like structure)
            -0.15f, 0.2f, -0.1f,   0.15f, 0.2f, -0.1f,   0.0f, 0.35f,  0.0f,
             0.15f, 0.2f, -0.1f,   0.15f, 0.2f,  0.1f,   0.0f, 0.35f,  0.0f,
             0.15f, 0.2f,  0.1f,  -0.15f, 0.2f,  0.1f,   0.0f, 0.35f,  0.0f,
            -0.15f, 0.2f,  0.1f,  -0.15f, 0.2f, -0.1f,   0.0f, 0.35f,  0.0f,
            
            // Minarets (small towers)
            -0.3f, 0.0f, -0.05f,  -0.25f, 0.0f, -0.05f,  -0.25f, 0.4f, -0.05f,
            -0.3f, 0.0f, -0.05f,  -0.25f, 0.4f, -0.05f,  -0.3f, 0.4f, -0.05f,
            
             0.25f, 0.0f, -0.05f,   0.3f, 0.0f, -0.05f,   0.3f, 0.4f, -0.05f,
             0.25f, 0.0f, -0.05f,   0.3f, 0.4f, -0.05f,   0.25f, 0.4f, -0.05f,
        };

        float[] normals = new float[vertices.length];
        calculateNormals(vertices, normals);
        
        float[] color = {0.3f, 0.6f, 1.0f, 1.0f}; // Blue color for Blue Mosque
        return new ModelData(vertices, normals, color);
    }

    /**
     * Create a simplified Batu Caves model
     */
    private static ModelData createBatuCavesModel() {
        float[] vertices = {
            // Cave entrance (large arch)
            -0.25f, 0.0f,  0.0f,  -0.15f, 0.3f,  0.0f,  -0.05f, 0.2f,  0.0f,
            -0.05f, 0.2f,  0.0f,   0.05f, 0.2f,  0.0f,   0.15f, 0.3f,  0.0f,
             0.15f, 0.3f,  0.0f,   0.25f, 0.0f,  0.0f,   0.0f, 0.0f,   0.0f,
            
            // Statue (tall figure)
             -0.02f, 0.0f, -0.1f,   0.02f, 0.0f, -0.1f,   0.02f, 0.5f, -0.1f,
             -0.02f, 0.0f, -0.1f,   0.02f, 0.5f, -0.1f,  -0.02f, 0.5f, -0.1f,
            
            // Steps
            -0.3f, 0.0f, -0.2f,   0.3f, 0.0f, -0.2f,   0.3f, 0.05f, -0.15f,
            -0.3f, 0.0f, -0.2f,   0.3f, 0.05f, -0.15f, -0.3f, 0.05f, -0.15f,
        };

        float[] normals = new float[vertices.length];
        calculateNormals(vertices, normals);
        
        float[] color = {0.8f, 0.7f, 0.5f, 1.0f}; // Sandy/cave color
        return new ModelData(vertices, normals, color);
    }

    /**
     * Create a simplified Merdeka Square model
     */
    private static ModelData createMerdekaSquareModel() {
        float[] vertices = {
            // Flagpole
            -0.01f, 0.0f, 0.0f,   0.01f, 0.0f, 0.0f,   0.01f, 0.6f, 0.0f,
            -0.01f, 0.0f, 0.0f,   0.01f, 0.6f, 0.0f,  -0.01f, 0.6f, 0.0f,
            
            // Square base
            -0.3f, 0.0f, -0.3f,   0.3f, 0.0f, -0.3f,   0.3f, 0.02f, -0.3f,
            -0.3f, 0.0f, -0.3f,   0.3f, 0.02f, -0.3f,  -0.3f, 0.02f, -0.3f,
            
            -0.3f, 0.0f,  0.3f,  -0.3f, 0.02f,  0.3f,   0.3f, 0.02f,  0.3f,
            -0.3f, 0.0f,  0.3f,   0.3f, 0.02f,  0.3f,   0.3f, 0.0f,  0.3f,
            
            // Buildings around square
            -0.2f, 0.0f, 0.4f,  -0.1f, 0.0f, 0.4f,  -0.1f, 0.25f, 0.4f,
            -0.2f, 0.0f, 0.4f,  -0.1f, 0.25f, 0.4f,  -0.2f, 0.25f, 0.4f,
            
             0.1f, 0.0f, 0.4f,   0.2f, 0.0f, 0.4f,   0.2f, 0.3f, 0.4f,
             0.1f, 0.0f, 0.4f,   0.2f, 0.3f, 0.4f,   0.1f, 0.3f, 0.4f,
        };

        float[] normals = new float[vertices.length];
        calculateNormals(vertices, normals);
        
        float[] color = {0.6f, 0.8f, 0.4f, 1.0f}; // Green/patriotic color
        return new ModelData(vertices, normals, color);
    }

    /**
     * Create a default cube model
     */
    private static ModelData createDefaultModel() {
        float[] vertices = {
            // Front face
            -0.1f, -0.1f,  0.1f,   0.1f, -0.1f,  0.1f,   0.1f,  0.1f,  0.1f,
            -0.1f, -0.1f,  0.1f,   0.1f,  0.1f,  0.1f,  -0.1f,  0.1f,  0.1f,
            // Back face  
            -0.1f, -0.1f, -0.1f,  -0.1f,  0.1f, -0.1f,   0.1f,  0.1f, -0.1f,
            -0.1f, -0.1f, -0.1f,   0.1f,  0.1f, -0.1f,   0.1f, -0.1f, -0.1f,
            // Top face
            -0.1f,  0.1f, -0.1f,  -0.1f,  0.1f,  0.1f,   0.1f,  0.1f,  0.1f,
            -0.1f,  0.1f, -0.1f,   0.1f,  0.1f,  0.1f,   0.1f,  0.1f, -0.1f,
            // Bottom face
            -0.1f, -0.1f, -0.1f,   0.1f, -0.1f, -0.1f,   0.1f, -0.1f,  0.1f,
            -0.1f, -0.1f, -0.1f,   0.1f, -0.1f,  0.1f,  -0.1f, -0.1f,  0.1f,
            // Right face
             0.1f, -0.1f, -0.1f,   0.1f,  0.1f, -0.1f,   0.1f,  0.1f,  0.1f,
             0.1f, -0.1f, -0.1f,   0.1f,  0.1f,  0.1f,   0.1f, -0.1f,  0.1f,
            // Left face
            -0.1f, -0.1f, -0.1f,  -0.1f, -0.1f,  0.1f,  -0.1f,  0.1f,  0.1f,
            -0.1f, -0.1f, -0.1f,  -0.1f,  0.1f,  0.1f,  -0.1f,  0.1f, -0.1f,
        };

        float[] normals = {
            // Front face
            0.0f, 0.0f, 1.0f,   0.0f, 0.0f, 1.0f,   0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,   0.0f, 0.0f, 1.0f,   0.0f, 0.0f, 1.0f,
            // Back face
            0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,
            // Top face
            0.0f, 1.0f, 0.0f,   0.0f, 1.0f, 0.0f,   0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,   0.0f, 1.0f, 0.0f,   0.0f, 1.0f, 0.0f,
            // Bottom face
            0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,
            // Right face
            1.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f,
            // Left face
            -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,
        };
        
        float[] color = {1.0f, 0.5f, 0.0f, 1.0f}; // Orange color for default
        return new ModelData(vertices, normals, color);
    }

    /**
     * Calculate basic normals for triangles
     */
    private static void calculateNormals(float[] vertices, float[] normals) {
        for (int i = 0; i < normals.length; i += 9) {
            // For simplicity, use a basic normal calculation
            // In a real implementation, you'd calculate proper face normals
            normals[i] = 0.0f; normals[i+1] = 1.0f; normals[i+2] = 0.0f;
            normals[i+3] = 0.0f; normals[i+4] = 1.0f; normals[i+5] = 0.0f;
            normals[i+6] = 0.0f; normals[i+7] = 1.0f; normals[i+8] = 0.0f;
        }
    }
} 