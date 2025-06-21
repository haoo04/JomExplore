package com.example.jomexplore;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class RecognitionResultActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView resultText;
    private Button btnViewAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_result);

        imageView = findViewById(R.id.captured_image);
        resultText = findViewById(R.id.recognition_result);
        btnViewAR = findViewById(R.id.btn_view_ar);

        // Get the image path and recognition result from intent
        String imagePath = getIntent().getStringExtra("image_path");
        String recognitionResult = getIntent().getStringExtra("recognition_result");

        // Display the captured image
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            imageView.setImageBitmap(bitmap);
        }

        // Display the recognition result
        if (recognitionResult != null) {
            resultText.setText(recognitionResult);
        }

        // AR view button click listener
        btnViewAR.setOnClickListener(v -> {
            // TODO: Launch AR activity
            // Intent arIntent = new Intent(this, ARActivity.class);
            // startActivity(arIntent);
        });
    }
} 