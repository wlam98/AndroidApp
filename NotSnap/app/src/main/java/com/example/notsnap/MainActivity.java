package com.example.notsnap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    public void captureButtonHandler(View view) {
        Toast.makeText(this, "capture clicked", Toast.LENGTH_SHORT).show();
        setContentView(R.layout.edit_mode);
    }

    public void discardButtonHandler(View view) {
        setContentView(R.layout.activity_main);
    }
}