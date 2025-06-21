package com.example.jomexplore.ar;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import com.google.ar.core.Coordinates2d;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * ARRenderer handles the rendering for the Augmented Reality (AR) view.
 * It manages the ARCore session, renders the camera background, detected planes,
 * and point clouds. It also handles user taps to place anchors in the AR scene.
 */
public class ARRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = ARRenderer.class.getSimpleName();

    private final android.app.Activity activity;
    private Session session;
    private boolean installRequested;
    private String modelName;
    
    // A list to hold the anchors placed in the AR scene.
    private final List<Anchor> anchors = new ArrayList<>();
    
    // Rendering components for the AR scene.
    private BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private PlaneRenderer planeRenderer = new PlaneRenderer();
    private PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private ModelRenderer modelRenderer = new ModelRenderer();

    /**
     * Constructor for ARRenderer.
     * @param activity The application activity.
     * @param modelName The name of the 3D model to be rendered.
     */
    public ARRenderer(android.app.Activity activity, String modelName) {
        this.activity = activity;
        this.modelName = modelName;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated called");
        
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Prepare the rendering objects on the GL thread.
        try {
            Log.d(TAG, "Initializing background renderer");
            backgroundRenderer.createOnGlThread(activity);
            
            Log.d(TAG, "Initializing plane renderer");
            planeRenderer.createOnGlThread(activity, "trigrid.png");
            
            Log.d(TAG, "Initializing point cloud renderer");
            pointCloudRenderer.createOnGlThread(activity);
            
            Log.d(TAG, "Initializing model renderer with model: " + modelName);
            modelRenderer.createOnGlThread(activity, modelName);
            
            Log.d(TAG, "All renderers initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize renderers", e);
            // Notify the activity about the error
            activity.runOnUiThread(() -> {
                android.widget.Toast.makeText(activity, "Failed to initialize AR renderers: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session of the change in display geometry.
        if (session != null) {
            session.setDisplayGeometry(0, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear the screen.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            // Draw a simple background color when session is not available
            GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            Log.v(TAG, "Session is null, clearing with gray background");
            return;
        }

        try {
            // Set the camera texture name so ARCore can use it.
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from the ARCore session.
            Frame frame = session.update();
            Camera camera = frame.getCamera();
            
            TrackingState trackingState = camera.getTrackingState();
            Log.v(TAG, "Camera tracking state: " + trackingState);

            // If the camera is not tracking, don't draw anything.
            if (trackingState == TrackingState.PAUSED) {
                // Draw a dark background to indicate paused state
                GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
                Log.v(TAG, "Camera tracking paused, clearing with dark background");
                return;
            }

            // Draw the camera background.
            backgroundRenderer.draw(frame);
            Log.v(TAG, "Camera background rendered");

            // If tracking, draw the 3D objects.
            if (trackingState == TrackingState.TRACKING) {
                // Get projection and camera matrices.
                float[] projmtx = new float[16];
                camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

                float[] viewmtx = new float[16];
                camera.getViewMatrix(viewmtx, 0);

                // Visualize tracked points (point cloud).
                try {
                    PointCloud pointCloud = frame.acquirePointCloud();
                    pointCloudRenderer.update(pointCloud);
                    pointCloudRenderer.draw(viewmtx, projmtx);
                    pointCloud.release();
                    Log.v(TAG, "Point cloud rendered");
                } catch (Exception e) {
                    Log.w(TAG, "Error rendering point cloud", e);
                }

                // Visualize detected planes.
                try {
                    Collection<Plane> planes = session.getAllTrackables(Plane.class);
                    Log.v(TAG, "Detected " + planes.size() + " planes");
                    planeRenderer.drawPlanes(
                            planes,
                            camera.getDisplayOrientedPose(),
                            projmtx);
                    Log.v(TAG, "Planes rendered");
                } catch (Exception e) {
                    Log.w(TAG, "Error rendering planes", e);
                }

                // Render 3D models at the anchor points.
                try {
                    if (anchors.isEmpty()) {
                        // For debugging - render a test model at origin if no anchors placed
                        if (modelRenderer != null) {
                            float[] identityMatrix = new float[16];
                            Matrix.setIdentityM(identityMatrix, 0);
                            // Move model slightly forward so it's visible
                            Matrix.translateM(identityMatrix, 0, 0.0f, 0.0f, -1.0f);
                            modelRenderer.draw(viewmtx, projmtx, identityMatrix);
                            Log.v(TAG, "Rendered test model at origin (no anchors placed)");
                        }
                    } else {
                        Log.v(TAG, "Rendering " + anchors.size() + " anchored models");
                        for (Anchor anchor : anchors) {
                            if (anchor.getTrackingState() != TrackingState.TRACKING) {
                                continue;
                            }
                            
                            // Get the model matrix for this anchor.
                            float[] modelMatrix = new float[16];
                            anchor.getPose().toMatrix(modelMatrix, 0);
                            
                            // Render the model at the anchor's position.
                            modelRenderer.draw(viewmtx, projmtx, modelMatrix);
                        }
                        Log.v(TAG, "All anchored models rendered");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error rendering models", e);
                }
            }
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame", e);
            // Set a red tint to indicate camera error
            GLES20.glClearColor(0.3f, 0.0f, 0.0f, 1.0f);
        } catch (Throwable t) {
            Log.e(TAG, "Exception on the OpenGL thread", t);
            // Set a yellow tint to indicate general error
            GLES20.glClearColor(0.3f, 0.3f, 0.0f, 1.0f);
        }
    }

    /**
     * Creates and resumes the ARCore session.
     * Handles ARCore installation and availability checks.
     */
    public void createSession() {
        Log.d(TAG, "createSession() called");
        
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                Log.d(TAG, "Checking ARCore installation");
                // Request ARCore installation if needed.
                switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    case INSTALL_REQUESTED:
                        Log.d(TAG, "ARCore installation requested");
                        installRequested = true;
                        return;
                    case INSTALLED:
                        Log.d(TAG, "ARCore is installed");
                        break;
                }

                // Create a new ARCore session.
                Log.d(TAG, "Creating new ARCore session");
                session = new Session(activity);
                Log.d(TAG, "ARCore session created successfully");
                
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session: " + message, exception);
                // Show error message to user
                final String finalMessage = message;
                activity.runOnUiThread(() -> {
                    android.widget.Toast.makeText(activity, finalMessage, android.widget.Toast.LENGTH_LONG).show();
                });
                return;
            }
        }

        // Resume the session.
        try {
            if (session != null) {
                Log.d(TAG, "Resuming ARCore session");
                session.resume();
                Log.d(TAG, "ARCore session resumed successfully");
            }
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available. Try restarting the app.", e);
            session = null;
            activity.runOnUiThread(() -> {
                android.widget.Toast.makeText(activity, "Camera not available", android.widget.Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error resuming session", e);
            session = null;
            final String errorMessage = "Error starting AR: " + e.getMessage();
            activity.runOnUiThread(() -> {
                android.widget.Toast.makeText(activity, errorMessage, android.widget.Toast.LENGTH_LONG).show();
            });
        }
    }

    /**
     * Pauses the ARCore session.
     */
    public void pauseSession() {
        if (session != null) {
            session.pause();
        }
    }

    /**
     * Destroys the ARCore session to release resources.
     */
    public void destroySession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    /**
     * Handles a tap event on the screen.
     * Performs a hit test to find a plane or point to place an anchor.
     * @param x The x-coordinate of the tap.
     * @param y The y-coordinate of the tap.
     */
    public void onTap(float x, float y) {
        if (session == null) {
            return;
        }

        try {
            Frame frame = session.update();
            List<HitResult> hits = frame.hitTest(x, y);
            
            // Iterate through the hit results and create an anchor at the first valid hit.
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                        || (trackable instanceof Point)) {
                    // Create an anchor at the hit location.
                    Anchor anchor = hit.createAnchor();
                    anchors.add(anchor);
                    
                    Log.d(TAG, "Placed anchor for model: " + modelName);
                    break;
                }
            }
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onTap", e);
        }
    }

    // Renderer classes for AR components
    
    /** Renders the camera feed as the background. */
    private static class BackgroundRenderer {
        private static final String VERTEX_SHADER =
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "   gl_Position = a_Position;\n" +
                "   v_TexCoord = a_TexCoord;\n" +
                "}";

        private static final String FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform samplerExternalOES u_Texture;\n" +
                "void main() {\n" +
                "   gl_FragColor = texture2D(u_Texture, v_TexCoord);\n" +
                "}";

        private int mProgram;
        private int mPositionHandle;
        private int mTexCoordHandle;
        private int mTextureHandle;
        private int mTextureId = -1;
        private FloatBuffer mVertexBuffer;
        private FloatBuffer mTexCoordBuffer;

        private static final float[] VERTEX_COORDS = {
                -1.0f, -1.0f,
                +1.0f, -1.0f,
                -1.0f, +1.0f,
                +1.0f, +1.0f,
        };

        private static final float[] TEX_COORDS = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };

        public void createOnGlThread(Context context) throws IOException {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
            mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
            mTextureHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");

            mVertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORDS.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mVertexBuffer.put(VERTEX_COORDS).position(0);

            mTexCoordBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTexCoordBuffer.put(TEX_COORDS).position(0);

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mTextureId = textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }

        public int getTextureId() { 
            return mTextureId; 
        }

        public void draw(Frame frame) {
            if (frame.hasDisplayGeometryChanged()) {
                frame.transformCoordinates2d(
                        Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                        mVertexBuffer,
                        Coordinates2d.TEXTURE_NORMALIZED,
                        mTexCoordBuffer);
            }

            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthMask(false);

            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glEnableVertexAttribArray(mTexCoordHandle);
            GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
            GLES20.glUniform1i(mTextureHandle, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTexCoordHandle);
            GLES20.glDepthMask(true);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
    }

    /** Renders detected planar surfaces. */
    private static class PlaneRenderer {
        private static final String VERTEX_SHADER =
                "uniform mat4 u_Model;\n" +
                "uniform mat4 u_ModelViewProjection;\n" +
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "   gl_Position = u_ModelViewProjection * a_Position;\n" +
                "   v_TexCoord = a_TexCoord;\n" +
                "}";

        private static final String FRAGMENT_SHADER =
                "precision highp float;\n" +
                "uniform sampler2D u_Texture;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "   gl_FragColor = texture2D(u_Texture, v_TexCoord);\n" +
                "}";

        private int mProgram;
        private int mModelUniform;
        private int mModelViewProjectionUniform;
        private int mPositionAttribute;
        private int mTexCoordAttribute;
        private int mTextureUniform;
        private int mTextureId;

        public void createOnGlThread(Context context, String texturePath) throws IOException {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            mModelUniform = GLES20.glGetUniformLocation(mProgram, "u_Model");
            mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");
            mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position");
            mTexCoordAttribute = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
            mTextureUniform = GLES20.glGetUniformLocation(mProgram, "u_Texture");

            // Create a simple grid texture
            mTextureId = createGridTexture();
        }

        public void drawPlanes(Collection<Plane> planes, com.google.ar.core.Pose pose, float[] projmtx) {
            if (planes.isEmpty()) {
                return;
            }

            GLES20.glUseProgram(mProgram);
            GLES20.glDepthMask(false);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            for (Plane plane : planes) {
                if (plane.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }

                float[] modelMatrix = new float[16];
                plane.getCenterPose().toMatrix(modelMatrix, 0);

                float[] modelViewProjectionMatrix = new float[16];
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, projmtx, 0, modelMatrix, 0);

                GLES20.glUniformMatrix4fv(mModelUniform, 1, false, modelMatrix, 0);
                GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

                // Draw a simple quad for the plane
                drawQuad();
            }

            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDepthMask(true);
        }

        private void drawQuad() {
            float[] vertices = {
                -1.0f, 0.0f, -1.0f, 0.0f, 0.0f,
                 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
                -1.0f, 0.0f,  1.0f, 0.0f, 1.0f,
                 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,
            };

            FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertexBuffer.put(vertices).position(0);

            GLES20.glEnableVertexAttribArray(mPositionAttribute);
            GLES20.glVertexAttribPointer(mPositionAttribute, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer);
            
            vertexBuffer.position(3);
            GLES20.glEnableVertexAttribArray(mTexCoordAttribute);
            GLES20.glVertexAttribPointer(mTexCoordAttribute, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            GLES20.glUniform1i(mTextureUniform, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(mPositionAttribute);
            GLES20.glDisableVertexAttribArray(mTexCoordAttribute);
        }

        private int createGridTexture() {
            int[] textureIds = new int[1];
            GLES20.glGenTextures(1, textureIds, 0);
            int textureId = textureIds[0];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Create a simple white texture
            byte[] pixels = new byte[4];
            pixels[0] = (byte) 255; // R
            pixels[1] = (byte) 255; // G
            pixels[2] = (byte) 255; // B
            pixels[3] = (byte) 100; // A (semi-transparent)

            ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
            buffer.put(pixels).position(0);

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

            return textureId;
        }
    }

    /** Renders the point cloud of detected feature points. */
    private static class PointCloudRenderer {
        private static final String VERTEX_SHADER =
                "uniform mat4 u_ModelViewProjection;\n" +
                "uniform vec4 u_Color;\n" +
                "uniform float u_PointSize;\n" +
                "attribute vec4 a_Position;\n" +
                "void main() {\n" +
                "   gl_Position = u_ModelViewProjection * a_Position;\n" +
                "   gl_PointSize = u_PointSize;\n" +
                "}";

        private static final String FRAGMENT_SHADER =
                "precision mediump float;\n" +
                "uniform vec4 u_Color;\n" +
                "void main() {\n" +
                "   gl_FragColor = u_Color;\n" +
                "}";

        private int mProgram;
        private int mPositionAttribute;
        private int mModelViewProjectionUniform;
        private int mColorUniform;
        private int mPointSizeUniform;
        private int mVbo;
        private int mVboSize;
        private int mNumPoints;

        public void createOnGlThread(Context context) throws IOException {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position");
            mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");
            mColorUniform = GLES20.glGetUniformLocation(mProgram, "u_Color");
            mPointSizeUniform = GLES20.glGetUniformLocation(mProgram, "u_PointSize");

            int[] buffers = new int[1];
            GLES20.glGenBuffers(1, buffers, 0);
            mVbo = buffers[0];
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

            mVboSize = 0;
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 0, null, GLES20.GL_DYNAMIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }

        public void update(PointCloud pointCloud) {
            mNumPoints = pointCloud.getPoints().remaining() / 4;
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

            int pointCloudDataSize = pointCloud.getPoints().remaining() * 4;
            if (pointCloudDataSize > mVboSize) {
                mVboSize = pointCloudDataSize;
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
            }
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, pointCloudDataSize, pointCloud.getPoints());
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }

        public void draw(float[] viewmtx, float[] projmtx) {
            float[] modelViewProjectionMatrix = new float[16];
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projmtx, 0, viewmtx, 0);

            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mPositionAttribute);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
            GLES20.glVertexAttribPointer(mPositionAttribute, 4, GLES20.GL_FLOAT, false, 16, 0);

            GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);
            GLES20.glUniform4f(mColorUniform, 1.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glUniform1f(mPointSizeUniform, 5.0f);

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mNumPoints);
            GLES20.glDisableVertexAttribArray(mPositionAttribute);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }
    }

    /** Renders 3D models. */
    private static class ModelRenderer {
        private static final String VERTEX_SHADER =
                "uniform mat4 u_ModelViewProjection;\n" +
                "uniform mat4 u_Model;\n" +
                "attribute vec4 a_Position;\n" +
                "attribute vec3 a_Normal;\n" +
                "varying vec3 v_Normal;\n" +
                "void main() {\n" +
                "   gl_Position = u_ModelViewProjection * a_Position;\n" +
                "   v_Normal = normalize((u_Model * vec4(a_Normal, 0.0)).xyz);\n" +
                "}";

        private static final String FRAGMENT_SHADER =
                "precision mediump float;\n" +
                "uniform vec4 u_Color;\n" +
                "varying vec3 v_Normal;\n" +
                "void main() {\n" +
                "   float lightIntensity = dot(v_Normal, normalize(vec3(0.5, 1.0, 0.5)));\n" +
                "   lightIntensity = max(0.3, lightIntensity);\n" +
                "   gl_FragColor = u_Color * lightIntensity;\n" +
                "}";

        private int mProgram;
        private int mPositionAttribute;
        private int mNormalAttribute;
        private int mModelViewProjectionUniform;
        private int mModelUniform;
        private int mColorUniform;
        private ModelLoader.ModelData mModelData;

        public void createOnGlThread(Context context, String modelName) throws IOException {
            Log.d(TAG, "ModelRenderer.createOnGlThread called with model: " + modelName);
            
            try {
                int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
                int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

                mProgram = GLES20.glCreateProgram();
                GLES20.glAttachShader(mProgram, vertexShader);
                GLES20.glAttachShader(mProgram, fragmentShader);
                GLES20.glLinkProgram(mProgram);

                // Check if the program linked successfully
                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: " + GLES20.glGetProgramInfoLog(mProgram));
                    throw new IOException("Failed to link shader program");
                }

                mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position");
                mNormalAttribute = GLES20.glGetAttribLocation(mProgram, "a_Normal");
                mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");
                mModelUniform = GLES20.glGetUniformLocation(mProgram, "u_Model");
                mColorUniform = GLES20.glGetUniformLocation(mProgram, "u_Color");

                Log.d(TAG, "Shader program created successfully");
                Log.d(TAG, "Attribute locations - Position: " + mPositionAttribute + ", Normal: " + mNormalAttribute);
                Log.d(TAG, "Uniform locations - MVP: " + mModelViewProjectionUniform + ", Model: " + mModelUniform + ", Color: " + mColorUniform);

                // Load the model using ModelLoader
                Log.i(TAG, "Loading 3D model: " + modelName);
                mModelData = ModelLoader.loadModel(context, modelName);
                
                if (mModelData == null) {
                    Log.e(TAG, "Failed to load model data for: " + modelName);
                    throw new IOException("Model data is null");
                }
                
                if (mModelData.modelPath.startsWith("ar_assets/")) {
                    Log.i(TAG, "Successfully loaded GLB model from: " + mModelData.modelPath);
                } else {
                    Log.i(TAG, "Using procedural model for: " + modelName);
                }
                
                Log.d(TAG, "Model has " + mModelData.vertexCount + " vertices");
                
            } catch (Exception e) {
                Log.e(TAG, "Error in ModelRenderer.createOnGlThread", e);
                throw new IOException("Failed to create ModelRenderer", e);
            }
        }

        public void draw(float[] viewMatrix, float[] projectionMatrix, float[] modelMatrix) {
            if (mModelData == null) {
                Log.w(TAG, "ModelRenderer: mModelData is null, cannot draw");
                return;
            }
            
            if (mProgram == 0) {
                Log.w(TAG, "ModelRenderer: shader program not initialized");
                return;
            }
            
            // Check for valid vertex count
            if (mModelData.vertexCount <= 0) {
                Log.w(TAG, "ModelRenderer: No vertices to draw (count: " + mModelData.vertexCount + ")");
                return;
            }
            
            float[] modelViewProjectionMatrix = new float[16];
            float[] temp = new float[16];
            Matrix.multiplyMM(temp, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, temp, 0);

            GLES20.glUseProgram(mProgram);

            GLES20.glEnableVertexAttribArray(mPositionAttribute);
            GLES20.glVertexAttribPointer(mPositionAttribute, 3, GLES20.GL_FLOAT, false, 0, mModelData.vertices);

            GLES20.glEnableVertexAttribArray(mNormalAttribute);
            GLES20.glVertexAttribPointer(mNormalAttribute, 3, GLES20.GL_FLOAT, false, 0, mModelData.normals);

            GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);
            GLES20.glUniformMatrix4fv(mModelUniform, 1, false, modelMatrix, 0);
            GLES20.glUniform4fv(mColorUniform, 1, mModelData.color, 0);

            // Log the draw call for debugging
            Log.v(TAG, "Drawing model with " + mModelData.vertexCount + " vertices");
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mModelData.vertexCount);

            GLES20.glDisableVertexAttribArray(mPositionAttribute);
            GLES20.glDisableVertexAttribArray(mNormalAttribute);
        }
    }

    // Utility method to load shaders with error checking
    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        
        // Check compilation status
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] != GLES20.GL_TRUE) {
            String info = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Could not compile shader " + type + ":" + info);
        }
        
        return shader;
    }
} 