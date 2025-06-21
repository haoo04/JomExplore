package com.example.jomexplore;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ImageClassifier handles the loading of a TensorFlow Lite model and performs image classification.
 * It is responsible for preprocessing the input image, running inference with the model,
 * and processing the classification results to provide a user-friendly description.
 */
public class ImageClassifier {
    // Constants for the TFLite model and image processing.
    private static final String MODEL_FILENAME = "heritage_model.tflite";
    private static final int INPUT_SIZE = 224; // The input size of the model.
    private static final int PIXEL_SIZE = 3;   // The number of color channels (R, G, B).
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    private Interpreter interpreter;
    private Context context;

    private static final String TAG = "ImageClassifier";

    /**
     * Constructor for the ImageClassifier.
     * Initializes the TFLite interpreter by loading the model from the assets folder.
     * @param context The application context.
     */
    public ImageClassifier(Context context) {
        this.context = context;
        try {
            android.util.Log.d(TAG, "Initializing ImageClassifier");
            MappedByteBuffer model = loadModelFile();
            if (model != null) {
                interpreter = new Interpreter(model);
                android.util.Log.d(TAG, "TensorFlow Lite interpreter created successfully");
            } else {
                android.util.Log.w(TAG, "Model file could not be loaded");
            }
        } catch (IOException e) {
            android.util.Log.e(TAG, "Failed to load TensorFlow Lite model", e);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Unexpected error initializing ImageClassifier", e);
        }
    }

    /**
     * Loads the TensorFlow Lite model from the assets directory.
     * @return A MappedByteBuffer containing the TFLite model.
     * @throws IOException If the model file cannot be read.
     */
    private MappedByteBuffer loadModelFile() throws IOException {
        try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd("models/" + MODEL_FILENAME);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Classifies the given bitmap by running it through the TFLite model.
     * @param bitmap The input image to classify.
     * @return A string containing the classification result and description.
     */
    public String classifyImage(Bitmap bitmap) {
        if (interpreter == null) {
            android.util.Log.w(TAG, "TensorFlow Lite interpreter not available");
            return "Model not loaded. Please add the 'heritage_model.tflite' file to the 'assets/models/' directory.";
        }

        try {
            android.util.Log.d(TAG, "Starting image classification");
            // Preprocess the image and run inference.
            ByteBuffer byteBuffer = preprocessImage(bitmap);
            if (byteBuffer == null) {
                android.util.Log.e(TAG, "Failed to preprocess image");
                return "Failed to process image for classification.";
            }
            
            float[][] result = new float[1][getOutputSize()];
            interpreter.run(byteBuffer, result);
            android.util.Log.d(TAG, "Classification completed");
            
            // Post-process the results to get a meaningful description.
            return processResults(result[0]);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error during image classification", e);
            return "Error occurred during image classification: " + e.getMessage();
        }
    }

    /**
     * Preprocesses the input bitmap to prepare it for the TFLite model.
     * This includes resizing the image and converting it to a ByteBuffer.
     * @param bitmap The bitmap to preprocess.
     * @return A ByteBuffer containing the preprocessed image data.
     */
    private ByteBuffer preprocessImage(Bitmap bitmap) {
        try {
            if (bitmap == null) {
                android.util.Log.e(TAG, "Input bitmap is null");
                return null;
            }
            
            android.util.Log.d(TAG, "Preprocessing image of size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
            byteBuffer.order(ByteOrder.nativeOrder());
            
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
            if (resizedBitmap == null) {
                android.util.Log.e(TAG, "Failed to resize bitmap");
                return null;
            }
            
            int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
            resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
            
            // Iterate over each pixel and convert it to a float value.
            int pixel = 0;
            for (int i = 0; i < INPUT_SIZE; ++i) {
                for (int j = 0; j < INPUT_SIZE; ++j) {
                    final int val = intValues[pixel++];
                    // Normalize the pixel values.
                    byteBuffer.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    byteBuffer.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    byteBuffer.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
            
            android.util.Log.d(TAG, "Image preprocessing completed");
            return byteBuffer;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error preprocessing image", e);
            return null;
        }
    }

    /**
     * Gets the output size of the model's output tensor.
     * @return The number of output classes.
     */
    private int getOutputSize() {
        if(interpreter != null) {
            return interpreter.getOutputTensor(0).shape()[1];
        }
        return 3; // Default size if interpreter is not available.
    }

    /**
     * Processes the raw output from the TFLite model to determine the final classification.
     * @param results An array of floats representing the confidence scores for each class.
     * @return A formatted string with the landmark name and description if confidence is high enough.
     */
    private String processResults(float[] results) {
        // The labels corresponding to the model's output classes.
        String[] labels = {
                "Batu Caves",
                "Blue Mosque",
                "Merdeka Square",
        };
        
        // Find the class with the highest confidence.
        int maxIndex = 0;
        float maxConfidence = results[0];
        for (int i = 1; i < results.length && i < labels.length; i++) {
            if (results[i] > maxConfidence) {
                maxConfidence = results[i];
                maxIndex = i;
            }
        }
        
        // Check if the highest confidence is above a certain threshold.
        if (maxConfidence > 0.5f) {
            return generateDescription(labels[maxIndex], maxConfidence);
        } else {
            return "Unable to identify this attraction. Please try taking a clearer photo.";
        }
    }

    /**
     * Generates a descriptive string for the identified landmark.
     * @param landmark The name of the landmark.
     * @param confidence The confidence score of the classification.
     * @return A formatted string containing details about the landmark.
     */
    private String generateDescription(String landmark, float confidence) {
        String confidenceString = String.format("%.1f%%", confidence * 100);
        switch (landmark) {
            case "Batu Caves":
                return "Batu Caves\n\nOne of the most popular Hindu shrines outside India, dedicated to Lord Murugan. A large statue of the Hindu God can be seen at the entrance, and visitors must climb a steep flight of 272 steps to reach the main cave.\n\nConfidence: " + confidenceString;
            case "Blue Mosque":
                return "Blue Mosque\n\nA symbol of Islamic architecture in Malaysia, this mosque has a capacity of 15,000 people and is situated among 13 acres of beautiful gardens.\n\nConfidence: " + confidenceString;
            default:
                return landmark + "\n\nThis is an important cultural heritage landmark in Malaysia.\n\nConfidence: " + confidenceString;
        }
    }

    /**
     * Closes the TFLite interpreter to release resources.
     */
    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
} 