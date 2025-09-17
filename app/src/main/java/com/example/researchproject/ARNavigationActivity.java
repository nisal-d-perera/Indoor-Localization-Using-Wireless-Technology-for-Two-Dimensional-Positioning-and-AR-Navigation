package com.example.researchproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

@SuppressWarnings("deprecation") // Using legacy Camera API intentionally
public class ARNavigationActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 1001;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private boolean surfaceReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure this matches your actual layout file name:
        setContentView(R.layout.activity_arnavigation);

        surfaceView = findViewById(R.id.cameraPreview);
        surfaceHolder = surfaceView.getHolder();

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceReady = true;
                startCameraIfPossible();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                restartPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceReady = false;
                stopAndReleaseCamera();
            }
        });

        // Ask permission if needed
        ensureCameraPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraIfPossible();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAndReleaseCamera();
    }

    private void ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCameraIfPossible() {
        if (!surfaceReady || !hasCameraPermission()) return;
        if (camera != null) return;

        try {
            camera = Camera.open(); // default back camera
            if (camera == null) return;

            // Optional: fix preview orientation for portrait screens
            camera.setDisplayOrientation(90);

            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            stopAndReleaseCamera();
        }
    }

    private void restartPreview() {
        if (camera == null || surfaceHolder.getSurface() == null) return;
        try {
            camera.stopPreview();
        } catch (Exception ignored) {}
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopAndReleaseCamera() {
        if (camera != null) {
            try { camera.stopPreview(); } catch (Exception ignored) {}
            camera.release();
            camera = null;
        }
    }

    // Permission callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA && hasCameraPermission()) {
            startCameraIfPossible();
        }
    }
}


//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.graphics.Point;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.util.DisplayMetrics;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.View;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.view.WindowCompat;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//
//public class ARNavigationActivity extends AppCompatActivity {
//
//    private SurfaceView surfaceView;
//    private SurfaceHolder surfaceHolder;
//    private Camera camera;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_arnavigation);
//
//        surfaceView = findViewById(R.id.cameraPreview);
//        surfaceHolder = surfaceView.getHolder();
//
//        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                camera = Camera.open();
//                try {
//                    camera.setPreviewDisplay(holder);
//                    camera.startPreview();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                if (surfaceHolder.getSurface() == null) return;
//                try {
//                    camera.stopPreview();
//                } catch (Exception ignored) {}
//                try {
//                    camera.setPreviewDisplay(surfaceHolder);
//                    camera.startPreview();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                if (camera != null) {
//                    camera.stopPreview();
//                    camera.release();
//                    camera = null;
//                }
//            }
//        });
//    }

//    private Camera camera;
//    private SurfaceView cameraView;
//    private FrameLayout overlayContainer;
//    private ImageView arrowIv;
//    private TextView distanceTv;
//
//    private List<Point> path;
//    private float pxToCm;  // 1 px = 1 cm
//    private int nextIndex = 1;  // always point from path[0] → path[1]
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        // edge‐to‐edge
//        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
//        setContentView(R.layout.activity_arnavigation);
//
//        cameraView       = findViewById(R.id.camera_view);
//        overlayContainer = findViewById(R.id.overlay_container);
//        arrowIv          = findViewById(R.id.arrow_iv);
//        distanceTv       = findViewById(R.id.distance_tv);
//
//        // scale: px→cm
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//        pxToCm = dm.xdpi / 2.54f;  // pixels per cm
//
//        // get path from Intent
//        path = readPathFromIntent();
//        if (path.size() < 2) {
//            // no navigation possible
//            distanceTv.setText("No route");
//            distanceTv.setVisibility(View.VISIBLE);
//            return;
//        }
//
//        // ask camera perm
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(
//                    this, new String[]{Manifest.permission.CAMERA}, 123);
//        } else {
//            startCamera();
//        }
//    }
//
//    @SuppressWarnings("deprecation")
//    private void startCamera() {
//        camera = Camera.open();
//        SurfaceHolder holder = cameraView.getHolder();
//        holder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(@NonNull SurfaceHolder sh) {
//                try {
//                    camera.setPreviewDisplay(sh);
//                    camera.setDisplayOrientation(90);
//                    camera.startPreview();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                // now that preview is live, show arrow + distance
//                showNavigationCue();
//            }
//            @Override public void surfaceChanged(@NonNull SurfaceHolder sh,int f,int w,int h){}
//            @Override public void surfaceDestroyed(@NonNull SurfaceHolder sh){
//                camera.stopPreview();
//                camera.release();
//                camera = null;
//            }
//        });
//    }
//
//    private List<Point> readPathFromIntent() {
//        ArrayList<int[]> raw =
//                (ArrayList<int[]>) getIntent().getSerializableExtra("path");
//        List<Point> out = new ArrayList<>();
//        if (raw!=null) {
//            for (int[] arr: raw) {
//                if (arr.length==2) out.add(new Point(arr[0], arr[1]));
//            }
//        }
//        return out;
//    }
//
//    private void showNavigationCue() {
//        // compute vector from A→B in bitmap px
//        Point A = path.get(0);
//        Point B = path.get(nextIndex);
//
//        float dx = B.x - A.x;
//        float dy = B.y - A.y;
//
//        // distance in cm
//        float distCm = (float)Math.hypot(dx, dy);
//        float distM  = distCm / 100f;  // convert to meters
//
//        // rotation angle for arrow
//        float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
//
//        // show arrow
//        arrowIv.setRotation(angle);
//        arrowIv.setVisibility(View.VISIBLE);
//
//        String distText = String.format(Locale.getDefault(), "%.1f m", distM);
//        distanceTv.setText(distText);
//
//        // Make the TextView visible
//        distanceTv.setVisibility(View.VISIBLE);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        // 1) First, call the superclass implementation
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        // 2) Then handle your camera‐permission logic
//        if (requestCode == 123 && grantResults.length > 0
//                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            startCamera();
//        }
//    }
//}
