package com.COMP3004.NotSnap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    TextureView textureView;
    ImageView ivBitmap;
    LinearLayout llBottom;

    int cameraModeSelection = 0;
    Handler handler;
    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;

    FloatingActionButton btnCapture, btnOk, btnCancel, btnTimer, btnHint;;

    Camera camera;
    FacialDetection facialDetection;

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    private ImageCapture setImageCapture() {
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCapture = new ImageCapture(imageCaptureConfig);


        btnTimer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                handler=new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        imgCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                            @Override
                            public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                                Bitmap bitmap = textureView.getBitmap();
                                showAcceptedRejectedButton(true);
                                ivBitmap.setImageBitmap(bitmap);
                            }

                            @Override
                            public void onError(ImageCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
                                super.onError(useCaseError, message, cause);
                            }
                        });
                    }
                },5000);
            }
        });
        btnCapture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                imgCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        Bitmap bitmap = textureView.getBitmap();
                        showAcceptedRejectedButton(true);
                        ivBitmap.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(ImageCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
                        super.onError(useCaseError, message, cause);
                    }
                });
            }
        });

        return imgCapture;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnTimer=findViewById(R.id.btnTimer);
        btnCapture = findViewById(R.id.btnCapture);
        btnOk = findViewById(R.id.btnAccept);
        btnCancel = findViewById(R.id.btnReject);
        btnHint = findViewById(R.id.btnhint);

        btnOk.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        btnHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, Instruction.class));
            }
        });

        llBottom = findViewById(R.id.llBottom);
        textureView = findViewById(R.id.textureView);
        ivBitmap = findViewById(R.id.ivBitmap);

        imageCapture = setImageCapture();
        facialDetection = new FacialDetection((Activity)this, cameraModeSelection, textureView, ivBitmap);
        imageAnalysis = facialDetection.setImageAnalysis();

        camera = new Camera(this, textureView);

        if (allPermissionsGranted()) {
            camera.startCamera(imageCapture, imageAnalysis);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }


    private void showAcceptedRejectedButton(boolean acceptedRejected) {
        if (acceptedRejected) {
            CameraX.unbind(camera.preview, camera.imageAnalysis);
            llBottom.setVisibility(View.VISIBLE);
            btnCapture.hide();
            btnTimer.hide();
            textureView.setVisibility(View.GONE);
        } else {
            btnCapture.show();
            btnTimer.show();
            llBottom.setVisibility(View.GONE);
            textureView.setVisibility(View.VISIBLE);
            textureView.post(new Runnable() {
                @Override
                public void run() {
                    camera.startCamera(setImageCapture(), facialDetection.setImageAnalysis());
                }
            });
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                camera.startCamera(setImageCapture(), facialDetection.setImageAnalysis());
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.black_white:
                cameraModeSelection = 0;
                camera.startCamera(setImageCapture(), facialDetection.setImageAnalysis());
                return true;

            case R.id.hsv:
                cameraModeSelection = 1;
                camera.startCamera(setImageCapture(), facialDetection.setImageAnalysis());
                return true;

            case R.id.lab:
                cameraModeSelection = 2;
                camera.startCamera(setImageCapture(), facialDetection.setImageAnalysis());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnReject:
                showAcceptedRejectedButton(false);
                break;

            case R.id.btnAccept:
                File file = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "" + System.currentTimeMillis() + "_CameraX.jpg");
                imageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        System.out.println(file.toString());
                        showAcceptedRejectedButton(false);
                        Toast.makeText(getApplicationContext(), "Image saved successfully in Pictures Folder", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Picture capture failed: " + message;
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                    }
                });
                break;
        }
    }
}
