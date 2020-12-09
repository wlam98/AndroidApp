package com.COMP3004.NotSnap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.ContextMenu;
import android.view.TextureView;
import android.widget.ImageView;

import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.*;
import java.lang.Math;

import android.content.Context;

import org.opencv.core.CvType;
import org.opencv.core.Range;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.Facemark;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Core;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;

public class FacialDetection {
    TextureView textureView;
    ImageView ivBitmap;
    Activity mainActivity;
    CascadeClassifier faceDetector;
    Facemark facemark;
    Context c;

    Mat nose = new Mat();
    Mat toque = new Mat();

    // Constructor
    public FacialDetection(Activity a, TextureView t, ImageView i, Context contxt)
    {
        ivBitmap = i;
        textureView = t;
        mainActivity = a;
        this.c = contxt;

        // Load Face Detector
        faceDetector = new CascadeClassifier();
        // Call Cascade file loader function
        load_cascade(faceDetector);

        // Create an object of the Facemark class
        facemark = Face.createFacemarkLBF();
        // Call yaml file loader function
        load_yaml(facemark);

        nose = load_nose(nose);
        toque = load_toque(toque);
    }

    public ImageAnalysis setImageAnalysis(final int cameraModeSelection)
    {
        // Setup image analysis pipeline that computes average pixel luminance
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                .setImageQueueDepth(1).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees)
                    {
                        //Analyzing live camera feed begins.
                        final Bitmap bitmap = textureView.getBitmap();


                        if(bitmap==null)
                            return;

                        if(cameraModeSelection == 0)
                        {
                            //Utils.matToBitmap(temp, bitmap);
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ivBitmap.setImageBitmap(bitmap);
                                }
                            });
                        }
                        else
                        {

                            System.out.println("Not Normal Camera Mode");
                            Mat src = new Mat();
                            Utils.bitmapToMat(bitmap, src);

                            Mat result = new Mat();
                            Mat temp = src.clone();
                            Mat grey = new Mat();

                            final Bitmap view = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);
                            Mat sticker = new Mat();
                            Utils.bitmapToMat(view, sticker);

                            //Mat nose = new Mat();
                            //nose = load_nose(nose);

                            // Convert to grayscale (Was making the frame rate really low so seemed laggy.)
                            //Imgproc.cvtColor(src, grey, Imgproc.COLOR_BGR2GRAY);

                            MatOfRect faceDetections = new MatOfRect();
                            try
                            {
                                faceDetector.detectMultiScale(temp, faceDetections);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            // Face landmark points displayed on face
                            if(cameraModeSelection == 1)
                            {
                                List<MatOfPoint2f> landmarks = new LinkedList<MatOfPoint2f>();

                                // Face landmark detection
                                boolean success = facemark.fit(temp, faceDetections, landmarks);
                                if (success)
                                {
                                    System.out.println("Face detected. Landmark size is " + landmarks.size());

                                    // If successful, draw key points on the video frame
                                    // Landmarks size is # of face is detected
                                    for (int i = 0; i < landmarks.size(); i++)
                                    {
                                        // Customize the function of drawing facial feature points, which can draw the shape/contour of facial feature points. JAVA version does not do it temporarily
                                        // Face.drawFacemarks(temp, landmarks.get(i));

                                        // OpenCV comes with a function for drawing key points of the face: drawFacemarks
                                        Face.drawFacemarks(temp, landmarks.get(i), new Scalar(255, 0, 0));

                                        //Utils.matToBitmap(temp, bitmap);
                                        Utils.matToBitmap(temp, bitmap);
                                        mainActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ivBitmap.setImageBitmap(bitmap);
                                            }
                                        });

                                    }
                                }
                                else
                                {
                                    System.out.println("Face not detected.");
                                }
                            }
                            // Red Box around Face
                            else if(cameraModeSelection == 2)
                            {
                                // Draw rectangle box around face
                                for(Rect a: faceDetections.toArray())
                                {
                                    Imgproc.rectangle(temp, new Point(a.x, a.y), new Point(a.x + a.width, a.y + a.height), new Scalar(255,0,0));
                                }

                                //Utils.matToBitmap(temp, bitmap);
                                Utils.matToBitmap(temp, bitmap);
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ivBitmap.setImageBitmap(bitmap);
                                    }
                                });
                            }
                            // Pig Nose face filter
                            else if(cameraModeSelection == 3)
                            {
                                List<MatOfPoint2f> landmarks = new LinkedList<MatOfPoint2f>();

                                // Face landmark detection
                                boolean success = facemark.fit(temp, faceDetections, landmarks);
                                if (success)
                                {
                                    System.out.println("Face detected. Landmark size is " + landmarks.size());

                                    // If successful, draw key points on the video frame
                                    // Landmarks size is # of face is detected
                                    for (int i = 0; i < landmarks.size(); i++)
                                    {
                                        // Customize the function of drawing facial feature points, which can draw the shape/contour of facial feature points. JAVA version does not do it temporarily
                                        // Face.drawFacemarks(temp, landmarks.get(i));

                                        // OpenCV comes with a function for drawing key points of the face: drawFacemarks
                                        //Face.drawFacemarks(temp, landmarks.get(i), new Scalar(255, 0, 0));

                                        Point[] landmarkPoints = landmarks.get(0).toArray();

                                        // Finding landmarks on face
                                        Point top_nose = new Point(landmarkPoints[29].x, landmarkPoints[29].y);
                                        Point left_nose = new Point(landmarkPoints[31].x, landmarkPoints[31].y);
                                        Point right_nose = new Point(landmarkPoints[35].x, landmarkPoints[35].y);
                                        Point centre_nose = new Point(landmarkPoints[30].x, landmarkPoints[30].y);

                                        Point left_head = new Point(landmarkPoints[18].x, landmarkPoints[18].y);
                                        Point right_head = new Point(landmarkPoints[25].x, landmarkPoints[25].y);

                                        //Imgproc.circle(temp, top_nose,3, new Scalar(0, 0, 255), 3);
                                        //Imgproc.circle(temp, left_nose,3, new Scalar(255, 0, 0), -1);
                                        //Imgproc.circle(temp, right_nose,3, new Scalar(255, 0, 0), -1);
                                        //Imgproc.circle(temp, centre_nose,3, new Scalar(255, 0, 0), -1);

                                        // Need to convert to integers as pixel width does not exist in float/decimal
                                        int nose_width = (int) ((Math.hypot(left_nose.x - right_nose.x, left_nose.y - right_nose.y))*1.7);
                                        int nose_height = (int) (nose_width * 0.7783417);

                                        // Draw rectangle around nose area (2 points are top left point and bottom right point)
                                        //Imgproc.rectangle(temp, new Point((int)(centre_nose.x - nose_width/2), (int) (centre_nose.y + nose_height/2)), new Point((int) (centre_nose.x + nose_width/2), (int) (centre_nose.y - nose_height/2)), new Scalar(255,0,0));

                                        Point top_left_nose = new Point((int)(centre_nose.x - nose_width/2), (int) (centre_nose.y - nose_height/2));
                                        Point bottom_right_nose = new Point((int) (centre_nose.x + nose_width/2), (int) (centre_nose.y + nose_height/2));

                                        // Resized nose
                                        Mat rNose = new Mat();
                                        System.out.println("Temp Channel = " + temp.channels());
                                        System.out.println("Nose image columns = " + nose.cols());
                                        System.out.println("Nose image rows = " + nose.rows());
                                        System.out.println("Nose size = " + nose.size().toString());

                                        // Resize pig nose
                                        Imgproc.resize(nose, rNose, new Size(nose_width, nose_height));

                                        //final Bitmap b = Bitmap.createBitmap(nose_width, nose_height, Bitmap.Config.ARGB_8888);

                                        int tempRow = temp.rows();
                                        int tempColumn = temp.cols();

                                        System.out.println("Temp Row = " + tempRow);
                                        System.out.println("Temp Column = " + tempColumn);

                                        System.out.println("Sticker Row = " + sticker.rows());
                                        System.out.println("Sticker Column = " + sticker.cols());

                                        System.out.println("Left nose left x = " + (int)(top_left_nose.x));
                                        System.out.println("Left nose right x = " + (int)(top_left_nose.x + nose_width));
                                        System.out.println("Left nose top y = " + (int)(top_left_nose.y));
                                        System.out.println("Left nose bottom y = " + (int)(top_left_nose.y + nose_width));
                                        System.out.println("Nose width = " + nose_width);
                                        System.out.println("Nose height = " + nose_height);


                                        System.out.println("Sticker Channel = " + sticker.channels());
                                        System.out.println("Sticker Type = " + sticker.type());
                                        System.out.println("rNose Channel = " + rNose.channels());
                                        System.out.println("rNose Type = " + rNose.type());

                                        // Works!
                                        rNose.copyTo(sticker.rowRange((int)(top_left_nose.y), (int)(top_left_nose.y + nose_height)).colRange((int)(top_left_nose.x), (int)(top_left_nose.x + nose_width)));

                                        // Works too!
                                        //rNose.copyTo(temp.submat(new Rect((int) (top_left_nose.x), (int) (top_left_nose.y), nose_width, nose_height)));


                                        //Utils.matToBitmap(temp, bitmap);
                                        Utils.matToBitmap(sticker, view);
                                        mainActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ivBitmap.setImageBitmap(view);
                                            }
                                        });

                                        /*
                                        // Create greyscale version of pig nose
                                        Mat nose_grey = new Mat();
                                        Imgproc.cvtColor(rNose, nose_grey, Imgproc.COLOR_BGR2GRAY);

                                        // Create nose mask
                                        Mat mask = new Mat();

                                        Imgproc.threshold(nose_grey, mask, 25, 255, Imgproc.THRESH_BINARY_INV);

                                        // Area around nose
                                        Mat nose_area = temp.submat(new Rect((int) (top_left_nose.x), (int) (top_left_nose.y), nose_width, nose_height));

                                        Mat nose_area_no_nose = new Mat();

                                        Core.bitwise_and(nose_area, nose_area, nose_area_no_nose, mask);

                                        // Print coordinates of face landmarks
                                        for(int j = 0; j < landmarkPoints.length; j++)
                                        {
                                            //System.out.println("(" + landmarkPoints[j].x + ", " + landmarkPoints[j].y + ")");
                                        }*/
                                    }
                                }
                                else
                                {
                                    System.out.println("Face not detected.");
                                }

                            }
                            // Toque filter
                            else if(cameraModeSelection == 4)
                            {
                                List<MatOfPoint2f> landmarks = new LinkedList<MatOfPoint2f>();

                                // Face landmark detection
                                boolean success = facemark.fit(temp, faceDetections, landmarks);
                                if (success)
                                {
                                    System.out.println("Face detected. Landmark size is " + landmarks.size());

                                    // If successful, draw key points on the video frame
                                    // Landmarks size is # of face is detected
                                    for (int i = 0; i < landmarks.size(); i++)
                                    {
                                        // Customize the function of drawing facial feature points, which can draw the shape/contour of facial feature points. JAVA version does not do it temporarily
                                        // Face.drawFacemarks(temp, landmarks.get(i));

                                        // OpenCV comes with a function for drawing key points of the face: drawFacemarks
                                        //Face.drawFacemarks(temp, landmarks.get(i), new Scalar(255, 0, 0));

                                        Point[] landmarkPoints = landmarks.get(0).toArray();

                                        // Finding landmarks on face
                                        Point left_head = new Point(landmarkPoints[18].x, landmarkPoints[18].y);
                                        Point right_head = new Point(landmarkPoints[25].x, landmarkPoints[25].y);
                                        Point middle_head = new Point((landmarkPoints[18].x + (((Math.hypot(left_head.x - right_head.x, left_head.y - right_head.y)))/2)), landmarkPoints[18].y);
                                        Point centre_head = new Point((landmarkPoints[18].x + (((Math.hypot(left_head.x - right_head.x, left_head.y - right_head.y)))/2)), (landmarkPoints[18].y - 1.5*(((Math.hypot(left_head.x - right_head.x, left_head.y - right_head.y)))/2)));

                                        //Imgproc.circle(temp, top_nose,3, new Scalar(0, 0, 255), 3);
                                        //Imgproc.circle(temp, left_nose,3, new Scalar(255, 0, 0), -1);
                                        //Imgproc.circle(temp, right_nose,3, new Scalar(255, 0, 0), -1);
                                        //Imgproc.circle(temp, centre_nose,3, new Scalar(255, 0, 0), -1);

                                        // Need to convert to integers as pixel width does not exist in float/decimal
                                        int head_width = (int) ((Math.hypot(left_head.x - right_head.x, left_head.y - right_head.y))*1.7);
                                        int head_height = (int) (head_width * 0.932);

                                        // Draw rectangle around nose area (2 points are top left point and bottom right point)
                                        //Imgproc.rectangle(temp, new Point((int)(centre_nose.x - nose_width/2), (int) (centre_nose.y + nose_height/2)), new Point((int) (centre_nose.x + nose_width/2), (int) (centre_nose.y - nose_height/2)), new Scalar(255,0,0));

                                        Point top_left_head = new Point((int)(centre_head.x - head_width/2), (int) (centre_head.y - head_height/2));
                                        Point bottom_right_head = new Point((int) (centre_head.x + head_height/2), (int) (centre_head.y + head_height/2));

                                        // Resized toque
                                        Mat rToque = new Mat();

                                        System.out.println("Temp Channel = " + temp.channels());
                                        System.out.println("Nose image columns = " + toque.cols());
                                        System.out.println("Nose image rows = " + toque.rows());
                                        System.out.println("Nose size = " + toque.size().toString());

                                        // Resize pig nose
                                        Imgproc.resize(toque, rToque, new Size(head_width, head_height));

                                        //final Bitmap b = Bitmap.createBitmap(nose_width, nose_height, Bitmap.Config.ARGB_8888);

                                        int tempRow = temp.rows();
                                        int tempColumn = temp.cols();

                                        System.out.println("Temp Row = " + tempRow);
                                        System.out.println("Temp Column = " + tempColumn);

                                        System.out.println("Sticker Row = " + sticker.rows());
                                        System.out.println("Sticker Column = " + sticker.cols());


                                        System.out.println("Sticker Channel = " + sticker.channels());
                                        System.out.println("Sticker Type = " + sticker.type());
                                        System.out.println("rToque Channel = " + rToque.channels());
                                        System.out.println("rToque Type = " + rToque.type());

                                        // Works!
                                        rToque.copyTo(sticker.rowRange((int)(top_left_head.y), (int)(top_left_head.y + head_height)).colRange((int)(top_left_head.x), (int)(top_left_head.x + head_width)));

                                        // Works too!
                                        //rNose.copyTo(temp.submat(new Rect((int) (top_left_nose.x), (int) (top_left_nose.y), nose_width, nose_height)));


                                        //Utils.matToBitmap(temp, bitmap);
                                        Utils.matToBitmap(sticker, view);
                                        mainActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ivBitmap.setImageBitmap(view);
                                            }
                                        });


                                    }
                                }
                                else
                                {
                                    System.out.println("Face not detected.");
                                }

                            }


/*
                            //Utils.matToBitmap(temp, bitmap);
                            Utils.matToBitmap(nose, b);
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ivBitmap.setImageBitmap(b);
                                }
                            });
                            */

                        }
                    }
                });

        return imageAnalysis;
    }

    public Mat load_toque(Mat img){
        try {
            InputStream is = mainActivity.getResources().openRawResource(R.raw.beanie);
            File toqueDir = mainActivity.getDir("toque", Context.MODE_PRIVATE);
            File toqueFile = new File(toqueDir, "beanie.png");
            FileOutputStream os = new FileOutputStream(toqueFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            img = Imgcodecs.imread(toqueFile.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
            if(img.empty())
            {
                Log.v("MyActivity","--(!)Error loading A\n");
                //return;
            }
            else
            {
                Log.v("MyActivity",
                        "Loaded toque image from " + toqueFile.getAbsolutePath());
                return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("MyActivity", "Failed to load toque image. Exception thrown: " + e);
        }
        System.out.println("weird");
        return img;
    }

    public Mat load_nose(Mat img){
        try {
            InputStream is = mainActivity.getResources().openRawResource(R.raw.pig_nose);
            File noseDir = mainActivity.getDir("nose", Context.MODE_PRIVATE);
            File noseFile = new File(noseDir, "pig_nose.png");
            FileOutputStream os = new FileOutputStream(noseFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            img = Imgcodecs.imread(noseFile.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
            if(img.empty())
            {
                Log.v("MyActivity","--(!)Error loading A\n");
                //return;
            }
            else
            {
                Log.v("MyActivity",
                        "Loaded pig nose image from " + noseFile.getAbsolutePath());
                return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("MyActivity", "Failed to load pig nose image. Exception thrown: " + e);
        }
        System.out.println("weird");
        return img;
    }

    public CascadeClassifier load_cascade(CascadeClassifier face_cascade){
        try {
            InputStream is = mainActivity.getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = mainActivity.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            face_cascade.load(mCascadeFile.getAbsolutePath());
            if(face_cascade.empty())
            {
                Log.v("MyActivity","--(!)Error loading A\n");
                //return;
            }
            else
            {
                Log.v("MyActivity",
                        "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                return face_cascade;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("MyActivity", "Failed to load cascade. Exception thrown: " + e);
        }
        System.out.println("weird");
        return face_cascade;
    }

    public Facemark load_yaml(Facemark f){
        try {
            InputStream is = mainActivity.getResources().openRawResource(R.raw.lbfmodel);
            File yamlDir = mainActivity.getDir("yaml", Context.MODE_PRIVATE);
            File mYaml = new File(yamlDir, "lbfmodel.yaml");
            FileOutputStream os = new FileOutputStream(mYaml);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            System.out.println("Path is " + mYaml.getAbsolutePath());
            //System.out.println(f.loadModel(mCascadeFile.getAbsolutePath()));

            f.loadModel(mYaml.getAbsolutePath());
            if(f.empty())
            {
                Log.v("MyActivity","--(!)Error loading A\n");
                //return;
            }
            else
            {
                Log.v("MyActivity",
                        "Loaded dat file from " + mYaml.getAbsolutePath());
                return f;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("MyActivity", "Failed to load dat file. Exception thrown: " + e);
        }
        System.out.println("weird");
        return f;
    }
}