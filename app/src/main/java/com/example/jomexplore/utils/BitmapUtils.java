package com.example.jomexplore.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;

/**
 * A utility class for handling Bitmap operations.
 * This class provides methods to load and manipulate bitmaps, such as handling image orientation
 * based on EXIF data and efficiently decoding large images.
 */
public class BitmapUtils {

    /**
     * Decodes an image file into a Bitmap, ensuring it is correctly oriented.
     * This method reads the EXIF orientation tag and rotates the image accordingly.
     * It also scales the image down to the requested dimensions to conserve memory.
     *
     * @param photoPath The absolute path to the image file.
     * @param reqWidth  The required width of the output bitmap.
     * @param reqHeight The required height of the output bitmap.
     * @return A correctly oriented and scaled Bitmap, or null if the file cannot be decoded.
     * @throws IOException If an error occurs while reading the image file or its EXIF data.
     */
    private static final String TAG = "BitmapUtils";

    public static Bitmap getCorrectlyOrientedBitmap(String photoPath, int reqWidth, int reqHeight) throws IOException {
        try {
            android.util.Log.d(TAG, "Loading bitmap from: " + photoPath);
            
            if (photoPath == null || photoPath.isEmpty()) {
                android.util.Log.e(TAG, "Photo path is null or empty");
                return null;
            }
            
            // First, decode with inJustDecodeBounds=true to check the dimensions of the image without loading it into memory.
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(photoPath, options);

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                android.util.Log.e(TAG, "Invalid image dimensions: " + options.outWidth + "x" + options.outHeight);
                return null;
            }

            // Calculate the inSampleSize to scale the image down.
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            android.util.Log.d(TAG, "Using inSampleSize: " + options.inSampleSize);

            // Decode the bitmap with the calculated inSampleSize.
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
            if (bitmap == null) {
                android.util.Log.e(TAG, "Failed to decode bitmap from file: " + photoPath);
                return null; // The file could not be decoded.
            }
            
            android.util.Log.d(TAG, "Bitmap loaded successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        

            // Read EXIF data to determine the image orientation.
            ExifInterface exif = new ExifInterface(photoPath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            android.util.Log.d(TAG, "Image orientation: " + orientation);

            // If the orientation is normal, no rotation is needed.
            if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                android.util.Log.d(TAG, "No rotation needed");
                return bitmap;
            }

            // Create a matrix to apply the necessary rotation/flipping.
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    android.util.Log.d(TAG, "Rotating 90 degrees");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    android.util.Log.d(TAG, "Rotating 180 degrees");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    android.util.Log.d(TAG, "Rotating 270 degrees");
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.preScale(-1.0f, 1.0f);
                    android.util.Log.d(TAG, "Flipping horizontally");
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.preScale(1.0f, -1.0f);
                    android.util.Log.d(TAG, "Flipping vertically");
                    break;
            }

            // Create a new bitmap with the correct orientation.
            Bitmap orientedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            android.util.Log.d(TAG, "Bitmap orientation corrected successfully");
            return orientedBitmap;
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error processing bitmap: " + photoPath, e);
            throw new IOException("Failed to process bitmap", e);
        }
    }

    /**
     * Calculates the `inSampleSize` for downscaling an image to the requested width and height.
     * This helps in decoding large bitmaps efficiently to avoid `OutOfMemoryError`.
     *
     * @param options   The BitmapFactory.Options containing the original image dimensions.
     * @param reqWidth  The required width.
     * @param reqHeight The required height.
     * @return The calculated `inSampleSize` value.
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Get the raw height and width of the image.
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than or equal to the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
} 