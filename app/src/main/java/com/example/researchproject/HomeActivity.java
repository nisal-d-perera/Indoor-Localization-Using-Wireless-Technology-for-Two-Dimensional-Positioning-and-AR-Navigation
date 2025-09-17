package com.example.researchproject;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class HomeActivity extends AppCompatActivity {

    CardView campusCard;
    CardView homeCard;
    ImageView qrScan;
    private boolean isScannerInstalled = false;
    private GmsBarcodeScanner scanner;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private final double TARGET_LAT = 7.118691;
    private final double TARGET_LNG = 79.918086;
    private final float RADIUS_METERS = 35;
    private final double CAMPUS_LAT = 7.198894;  // <-- Change to your campus latitude
    private final double CAMPUS_LNG = 79.865756;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        installGoogleScanner();
        initVars();
        registerUiListener();

        campusCard = findViewById(R.id.campusCard);

        campusCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replace 'TargetActivity.class' with your actual destination activity
                Intent intent = new Intent(HomeActivity.this, CampusActivity.class);
                startActivity(intent);
            }
        });

        homeCard = findViewById(R.id.homeCard);

        homeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replace 'TargetActivity.class' with your actual destination activity
                Intent intent = new Intent(HomeActivity.this, HouseActivity.class);
                startActivity(intent);
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ImageView locationSearch = findViewById(R.id.locationSearch);
        locationSearch.setOnClickListener(v -> checkLocationPermissionAndFetch());

    }

    private void installGoogleScanner() {
        ModuleInstallClient moduleInstall = ModuleInstall.getClient(this);

        ModuleInstallRequest moduleInstallRequest =
                ModuleInstallRequest.newBuilder()
                        .addApi(GmsBarcodeScanning.getClient(this))
                        .build();

        moduleInstall.installModules(moduleInstallRequest)
                .addOnSuccessListener(unused -> isScannerInstalled = true)
                .addOnFailureListener(e -> {
                    isScannerInstalled = false;
                    Toast.makeText(HomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void initVars() {
        qrScan = findViewById(R.id.qrscan);

        GmsBarcodeScannerOptions options = initializeGoogleScanner();
        scanner = GmsBarcodeScanning.getClient(this, options);
    }

    private GmsBarcodeScannerOptions initializeGoogleScanner() {
        return new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build();
    }

    private void registerUiListener() {
        qrScan.setOnClickListener(v -> {
            if (isScannerInstalled) {
                startScanning();
            } else {
                Toast.makeText(HomeActivity.this, "Please try again...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startScanning() {
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String result = barcode.getRawValue();
                    if (result != null) {
                        if (result.contains("BCICampus")) {
                            Intent intent = new Intent(HomeActivity.this, CampusActivity.class);
                            startActivity(intent);
                        }
                    }
                })
                .addOnCanceledListener(() ->
                        Toast.makeText(HomeActivity.this, "Cancelled", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(HomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            // Permission already granted
            fetchCurrentLocation();
        }
    }

//    private void fetchCurrentLocation() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            try {
//                fusedLocationClient.getLastLocation()
//                        .addOnSuccessListener(this, location -> {
//                            if (location != null) {
//                                double currentLat = location.getLatitude();
//                                double currentLng = location.getLongitude();
//
//                                float[] result = new float[1];
//                                Location.distanceBetween(currentLat, currentLng, TARGET_LAT, TARGET_LNG, result);
//
//                                if (result[0] <= RADIUS_METERS) {
//                                    Intent intent = new Intent(HomeActivity.this, HouseActivity.class);
//                                    startActivity(intent);
//                                } else {
//                                    Toast.makeText(this, "You are not near the target location.", Toast.LENGTH_SHORT).show();
//                                }
//                            } else {
//                                Toast.makeText(this, "Unable to fetch location. Try again.", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//            } catch (SecurityException e) {
//                e.printStackTrace();
//                Toast.makeText(this, "Location permission error.", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            // Request permission if not granted
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST);
//        }
//    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                double currentLat = location.getLatitude();
                                double currentLng = location.getLongitude();

                                // ✅ [CHANGED] Check distance to both House and Campus
                                float[] houseDistance = new float[1];
                                float[] campusDistance = new float[1];

                                Location.distanceBetween(currentLat, currentLng, TARGET_LAT, TARGET_LNG, houseDistance);
                                Location.distanceBetween(currentLat, currentLng, CAMPUS_LAT, CAMPUS_LNG, campusDistance);

                                if (houseDistance[0] <= RADIUS_METERS) {
                                    // ✅ [UNCHANGED] Inside house area
                                    Intent intent = new Intent(HomeActivity.this, HouseActivity.class);
                                    startActivity(intent);
                                } else if (campusDistance[0] <= RADIUS_METERS) {
                                    // ✅ [ADDED] Inside campus area
                                    Intent intent = new Intent(HomeActivity.this, CampusActivity.class);
                                    startActivity(intent);
                                } else {
                                    // ✅ [CHANGED] New general message
                                    Toast.makeText(this, "Not in range of House or Campus.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "Unable to fetch location. Try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(this, "Location permission error.", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // ✅ Important

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}