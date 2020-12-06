package com.COMP3004.NotSnap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;
import android.widget.ImageView;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;

public class FacialDetection {
    int cameraModeSelection;
    TextureView textureView;
    ImageView ivBitmap;
    Activity mainActivity;

    public FacialDetection(Activity a, int cameraMode, TextureView t, ImageView i) {
        ivBitmap = i;
        cameraModeSelection = cameraMode;
        textureView = t;
        mainActivity = a;
    }

    public ImageAnalysis setImageAnalysis() {
        // Setup image analysis pipeline that computes average pixel luminance
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();

        if(cameraModeSelection == 0)
        {
            System.out.println("Camera");
        }
        else if(cameraModeSelection == 1)
        {
            System.out.println("Face landmarks");
        }
        else
        {
            System.out.println("Red Rectange");
        }

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                .setImageQueueDepth(1).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        //Analyzing live camera feed begins.

                        final Bitmap bitmap = textureView.getBitmap();

                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ivBitmap.setImageBitmap(bitmap);
                            }
                        });

                    }
                });

        return imageAnalysis;
    }

}
