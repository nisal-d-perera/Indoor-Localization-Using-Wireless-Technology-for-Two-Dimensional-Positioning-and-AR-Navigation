package com.example.researchproject;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.optim.SimpleVectorValueChecker;
import org.apache.commons.math3.util.Pair;


public class LocalizeActivity extends AppCompatActivity {

    private static final String TAG = "LocalizeActivity";
    private static final int PERMISSION_REQUEST_CODE = 1003;

    private final String BEACON1_MAC = "D2:F3:3F:9E:53:30";
    private final String BEACON2_MAC = "F6:B9:A1:BE:E1:7B";
    private final String BEACON3_MAC = "E0:2A:F9:41:D5:02";

    private ImageView floorImage;
    private TextView beaconDistancesText;
    private TextView textLocation;
    private Button navigateBtn;
    private View userPositionMarker;
    private boolean stabilizationComplete = false;

    private Double beacon1Distance = null;
    private Double beacon2Distance = null;
    private Double beacon3Distance = null;

    private double avg1;
    private double avg2;
    private double avg3;

    private static final double TX_POWER_B1 = -59.0;
    private static final double TX_POWER_B2 = -59.0;
    private static final double TX_POWER_B3 = -59.0;

    private final List<Double> beacon1RssiList = new ArrayList<>();
    private final List<Double> beacon2RssiList = new ArrayList<>();
    private final List<Double> beacon3RssiList = new ArrayList<>();

    private Map<String, Map<String, PointF[]>> beaconPositionMap = new HashMap<>();
    private Map<String, Map<String, Integer>> baseImageWidthMap = new HashMap<>();
    private Map<String, Map<String, Integer>> baseImageHeightMap = new HashMap<>();
    private Map<String, Map<String, Float>> scalingRatioMap = new HashMap<>();

    private PointF[] currentBeaconCoords = null;
    private int currentBaseImageWidth = 1080; // default
    private int currentBaseImageHeight = 1080; // default
    private float currentScalingRatio = 2.0f; // default

    private float ux;
    private float uy;


    private final int BLE_SCAN_INTERVAL = 5000; // 5 seconds
    private final Handler bleHandler = new Handler(Looper.getMainLooper());
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private ScanCallback bleScanCallback;

    private final Runnable bleScanRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                scanForBeacons();
                bleHandler.postDelayed(this, BLE_SCAN_INTERVAL);
            } catch (Exception e) {
                Log.e(TAG, "Error in bleScanRunnable: " + e.getMessage(), e);
            }
        }
    };

    // Flag to track if permissions are granted
    private boolean permissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_localize);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            initializeBeaconConfig();

            String locationType = getIntent().getStringExtra("locationType");
            String floor = getIntent().getStringExtra("floor");

            if (locationType != null && floor != null) {
                Map<String, PointF[]> beaconFloorMap = beaconPositionMap.get(locationType);
                Map<String, Integer> widthMap = baseImageWidthMap.get(locationType);
                Map<String, Integer> heightMap = baseImageHeightMap.get(locationType); // ✅ NEW
                Map<String, Float> scaleMap = scalingRatioMap.get(locationType);

                if (beaconFloorMap != null) {
                    currentBeaconCoords = beaconFloorMap.get(floor);
                }
                if (widthMap != null) {
                    currentBaseImageWidth = widthMap.getOrDefault(floor, 1080);
                }
                if (heightMap != null) {
                    currentBaseImageHeight = heightMap.getOrDefault(floor, 1080); // ✅ NEW
                }
                if (scaleMap != null) {
                    currentScalingRatio = scaleMap.getOrDefault(floor, 2.5f);
                }
            }


            floorImage = findViewById(R.id.floorImage);
            beaconDistancesText = findViewById(R.id.beaconDistancesText);
            textLocation = findViewById(R.id.textLocation);
            userPositionMarker = findViewById(R.id.userPositionMarker);

            // Make sure userPositionMarker is initially invisible
            if (userPositionMarker != null) {
                userPositionMarker.setVisibility(View.INVISIBLE);
            }

            if (floorImage != null && locationType != null && floor != null) {
                if (locationType.equalsIgnoreCase("house")) {
                    textLocation.setText("My Home");
                    if (floor.equalsIgnoreCase("1st Floor")) {
                        floorImage.setImageResource(R.drawable.wroom_1);
                    } else if (floor.equalsIgnoreCase("2nd Floor")) {
                        floorImage.setImageResource(R.drawable.wroom_21);
                    }
                }
                if (locationType.equalsIgnoreCase("campus")) {
                    textLocation.setText("BCI Campus");
                    if (floor.equalsIgnoreCase("1st Floor")) {
                        floorImage.setImageResource(R.drawable.r_lab);
                    } else if (floor.equalsIgnoreCase("2nd Floor")) {
                        floorImage.setImageResource(R.drawable.lab01);
                    }
                }
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                stabilizationComplete = true;
                Log.d(TAG, "1-minute stabilization complete");
            }, 30_000);

            floorImage.post(() -> drawBeaconMarkers());

            // Check if required resources exist
            verifyRequiredResources();

            // Request permissions first - we'll initialize Bluetooth and start scanning after permissions are granted
            requestRequiredPermissions();

            navigateBtn = findViewById(R.id.navigateButton);
            navigateBtn.setOnClickListener(v -> {
                if (locationType == null || floor == null) {
                    Toast.makeText(LocalizeActivity.this, "Floor not detected yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(LocalizeActivity.this, NavigateActivity.class);
                intent.putExtra("locationType", locationType);
                intent.putExtra("floor", floor);
                intent.putExtra("lx", ux);
                intent.putExtra("ly", uy);
                startActivity(intent);
            });
        }catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void verifyRequiredResources() {
        try {
            // Check if the drawable resources exist
            int floor1Id = getResources().getIdentifier("floor_1", "drawable", getPackageName());
            int floor2Id = getResources().getIdentifier("floor_2", "drawable", getPackageName());
            int roomId = getResources().getIdentifier("wroom_1", "drawable", getPackageName());
            int labId = getResources().getIdentifier("lab", "drawable", getPackageName());
            int positionMarkerId = getResources().getIdentifier("position_marker", "drawable", getPackageName());

            if (floor1Id == 0 || floor2Id == 0 || roomId == 0 || labId == 0) {
                Toast.makeText(this, "Required floor plan images are missing", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Missing drawable resources: floor_1 or lab");
            }

            if (positionMarkerId == 0) {
                Toast.makeText(this, "Position marker drawable is missing", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Missing drawable resource: position_marker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying resources: " + e.getMessage(), e);
        }
    }

    private void requestRequiredPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Always check for fine location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Only check for Bluetooth permissions on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            // All permissions already granted
            permissionsGranted = true;
            initializeAndStartScanning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            permissionsGranted = allGranted;

            if (allGranted) {
                // Now we can initialize and start scanning
                initializeAndStartScanning();
            } else {
                Toast.makeText(this, "Permissions required for indoor positioning", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeAndStartScanning() {
        try {
            // Initialize Bluetooth
            initializeBluetooth();

            // Start Bluetooth scanning if available
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bleScanner != null) {
                bleHandler.post(bleScanRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing scanning: " + e.getMessage(), e);
        }
    }

    private void initializeBluetooth() {
        try {
            // Check if device supports Bluetooth LE
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, "Bluetooth LE not supported on this device", Toast.LENGTH_LONG).show();
                return;
            }

            // Get Bluetooth adapter
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "BluetoothManager not available");
                return;
            }

            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter not available on this device");
                Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                // Enabling Bluetooth programmatically is not allowed on newer Android versions
                // Instead, we show a toast asking the user to enable it
                Toast.makeText(this, "Please enable Bluetooth for positioning", Toast.LENGTH_LONG).show();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(enableBtIntent);
                }
                return;
            }

            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bleScanner == null) {
                Log.e(TAG, "BluetoothLeScanner not available");
                return;
            }

            // Initialize scan callback - we'll log all discovered devices for testing
            bleScanCallback = new ScanCallback() {
                @Override
                 public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    try {
                        // Log all discovered devices to help find your beacons
                        String deviceAddress = result.getDevice().getAddress();
                        int rssi = result.getRssi();
                        Log.d(TAG, "Found BLE device: " + deviceAddress + " with RSSI: " + rssi);

                        processScanResult(result);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing scan result: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.e(TAG, "BLE Scan failed with error: " + errorCode);
                }
            };
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Bluetooth: " + e.getMessage(), e);
        }
    }

    private void scanForBeacons() {
        try {
            if (bleScanner == null) {
                Log.e(TAG, "BLE Scanner is null");
                return;
            }

            // Check for permissions before scanning
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Bluetooth scan permission not granted");
                    return;
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Location permission not granted, required for BLE on older Android");
                    return;
                }
            }

            // Setup scan settings for low latency
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            // Start scan
            bleScanner.startScan(null, scanSettings, bleScanCallback);

            // Stop scan after a short period to save battery
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (bleScanner != null && ActivityCompat.checkSelfPermission(this,
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                    ? Manifest.permission.BLUETOOTH_SCAN
                                    : Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        bleScanner.stopScan(bleScanCallback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping scan: " + e.getMessage(), e);
                }
            }, 2000); // Scan for 2 seconds
        } catch (Exception e) {
            Log.e(TAG, "Error in scanForBeacons: " + e.getMessage(), e);
        }
    }

//    private void processScanResult(ScanResult result) {
//        try {
//            String deviceAddress = result.getDevice().getAddress();
//            int rssi = result.getRssi();
//
//            // Process beacon based on MAC address
//            if (deviceAddress.equals(BEACON1_MAC)) {
//                beacon1Distance = estimateDistanceFromBleRssi(rssi);
//                Log.d(TAG, "Beacon 1 distance: " + beacon1Distance + " meters");
//            } else if (deviceAddress.equals(BEACON2_MAC)) {
//                beacon2Distance = estimateDistanceFromBleRssi(rssi);
//                Log.d(TAG, "Beacon 2 distance: " + beacon2Distance + " meters");
//            } else if (deviceAddress.equals(BEACON3_MAC)) {
//                beacon3Distance = estimateDistanceFromBleRssi(rssi);
//                Log.d(TAG, "Beacon 3 distance: " + beacon3Distance + " meters");
//            }
//
//            // For testing, we'll also use the first three found devices as our beacons
//            if (beacon1Distance == null && !deviceAddress.equals(BEACON1_MAC)) {
//                beacon1Distance = estimateDistanceFromBleRssi(rssi);
//                Log.d(TAG, "Using device " + deviceAddress + " as Beacon 1, distance: " + beacon1Distance + " meters");
//            } else if (beacon2Distance == null && !deviceAddress.equals(BEACON2_MAC) &&
//                    beacon1Distance != null && !deviceAddress.equals(BEACON1_MAC)) {
//                beacon2Distance = estimateDistanceFromBleRssi(rssi);
//                Log.d(TAG, "Using device " + deviceAddress + " as Beacon 2, distance: " + beacon2Distance + " meters");
//            } else if (beacon3Distance == null && !deviceAddress.equals(BEACON3_MAC) &&
//                    beacon1Distance != null && !deviceAddress.equals(BEACON1_MAC) &&
//                    beacon2Distance != null && !deviceAddress.equals(BEACON2_MAC)) {
//                beacon3Distance = estimateDistanceFromBleRssi(rssi);
//                Log.d(TAG, "Using device " + deviceAddress + " as Beacon 3, distance: " + beacon3Distance + " meters");
//            }
//
//            // Update UI with beacon distances
//            updateBeaconDistancesUI();
//        } catch (Exception e) {
//            Log.e(TAG, "Error processing scan result: " + e.getMessage(), e);
//        }
//    }

    private void initializeBeaconConfig() {
        // Beacon positions for each floor
        Map<String, PointF[]> houseFloors = new HashMap<>();
        houseFloors.put("1st Floor", new PointF[]{
                new PointF(215, 287), new PointF(525, 996), new PointF(864, 626)
        });
        houseFloors.put("2nd Floor", new PointF[]{
                new PointF(215, 287), new PointF(525, 996), new PointF(864, 626)
        });

        Map<String, PointF[]> campusFloors = new HashMap<>();
        campusFloors.put("1st Floor", new PointF[]{
                new PointF(311, 250), new PointF(311, 496), new PointF(311, 742)
        });
        campusFloors.put("2nd Floor", new PointF[]{
                new PointF(15, 490), new PointF(280, 180), new PointF(350, 750)
        });

        beaconPositionMap.put("house", houseFloors);
        beaconPositionMap.put("campus", campusFloors);

        // Image base sizes (replace if different)
        Map<String, Integer> houseWidths = new HashMap<>();
        houseWidths.put("1st Floor", 1080);
        houseWidths.put("2nd Floor", 1080);

        Map<String, Integer> campusWidths = new HashMap<>();
        campusWidths.put("1st Floor", 622);
        campusWidths.put("2nd Floor", 635);

        baseImageWidthMap.put("house", houseWidths);
        baseImageWidthMap.put("campus", campusWidths);

        Map<String, Integer> houseHeights = new HashMap<>();
        houseHeights.put("1st Floor", 1080);
        houseHeights.put("2nd Floor", 1080);

        Map<String, Integer> campusHeights = new HashMap<>();
        campusHeights.put("1st Floor", 993);
        campusHeights.put("2nd Floor", 985);

        baseImageHeightMap.put("house", houseHeights);
        baseImageHeightMap.put("campus", campusHeights);

        // Scaling Ratios
        Map<String, Float> houseScaling = new HashMap<>();
        houseScaling.put("1st Floor", 2.5f);
        houseScaling.put("2nd Floor", 2.0f);

        Map<String, Float> campusScaling = new HashMap<>();
        campusScaling.put("1st Floor", 1.0f);
        campusScaling.put("2nd Floor", 1.0f);

        scalingRatioMap.put("house", houseScaling);
        scalingRatioMap.put("campus", campusScaling);
    }


    private void processScanResult(ScanResult result) {
        try {
            String deviceAddress = result.getDevice().getAddress();
            int rssi = result.getRssi();
            double avg, dist;

            // Smooth and estimate distances for known beacons
            if (deviceAddress.equals(BEACON1_MAC)) {
                avg1 = getSmoothedRssi(beacon1RssiList, rssi);
                beacon1Distance = estimateDistance(avg1, TX_POWER_B1);

            } else if (deviceAddress.equals(BEACON2_MAC)) {
                avg2 = getSmoothedRssi(beacon2RssiList, rssi);
                beacon2Distance = estimateDistance(avg2, TX_POWER_B2);

            } else if (deviceAddress.equals(BEACON3_MAC)) {
                avg3 = getSmoothedRssi(beacon3RssiList, rssi);
                beacon3Distance = estimateDistance(avg3, TX_POWER_B3);
            }

            // Update UI with all beacon distances
            updateBeaconDistancesUI();

        } catch (Exception e) {
            Log.e(TAG, "Error processing scan result: " + e.getMessage(), e);
        }
    }

    private double getSmoothedRssi(List<Double> rssiList, double newRssi) {
        if (rssiList.size() >= 5) rssiList.remove(0); // keep last 5 values
        rssiList.add(newRssi);
        return rssiList.stream().mapToDouble(Double::doubleValue).average().orElse(newRssi);
    }



//    private double estimateDistanceFromBleRssi(double rssi) {
//        // Calculate distance based on RSSI
//        // Note: This formula needs calibration for your specific beacons
//        int txPower = -59; // Calibrated txPower at 1 meter (adjust for your beacons)
//
//        if (rssi == 0) {
//            return -1.0; // If we cannot determine accuracy, return -1
//        }
//
//        double ratio = rssi / txPower;
//        if (ratio < 1.0) {
//            return Math.pow(ratio, 10);
//        } else {
//            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
//        }
//    }

    private double estimateDistance(double smoothedRssi, double txPower1m) {
        // path‑loss exponent for indoor (tweak 2.7–3.5)
        final double n = 3.0;
        return Math.pow(10.0, (txPower1m - smoothedRssi) / (10.0 * n));
    }


    private void updateBeaconDistancesUI() {
        try {
            // Only update if we have at least one beacon distance
            if (beacon1Distance != null || beacon2Distance != null || beacon3Distance != null) {
//                StringBuilder sb = new StringBuilder("Beacon distances:\n");
                StringBuilder sb = new StringBuilder("User Location:");

                if (beacon1Distance != null) {
//                    sb.append("Beacon 1: ").append(String.format("%.2f", avg1)).append(" m\n");
//                    sb.append("Beacon 1: ").append(String.format("%.2f", beacon1Distance)).append(" m\n");
                }

                if (beacon2Distance != null) {
//                    sb.append("Beacon 2: ").append(String.format("%.2f", avg2)).append(" m\n");
//                    sb.append("Beacon 2: ").append(String.format("%.2f", beacon2Distance)).append(" m\n");
                }

                if (beacon3Distance != null) {
//                    sb.append("Beacon 3: ").append(String.format("%.2f", avg3)).append(" m\n");
//                    sb.append("Beacon 3: ").append(String.format("%.2f", beacon3Distance)).append(" m");
                }

                final String distancesText = sb.toString();

                // Update the TextView with beacon distances
                runOnUiThread(() -> {
                    if (beaconDistancesText != null) {
                        beaconDistancesText.setText(distancesText);
                    }

                    // Only calculate position if we have all three distances
//                    if (beacon1Distance != null && beacon2Distance != null && beacon3Distance != null) {
//                        calculateAndDisplayUserPosition();
//                    }
                    if (stabilizationComplete
                            && beacon1Distance != null
                            && beacon2Distance != null
                            && beacon3Distance != null) {
                        calculateAndDisplayUserPosition();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating beacon distances UI: " + e.getMessage(), e);
        }
    }

//    private void calculateAndDisplayUserPosition() {
//        try {
//            // Safety checks
//            if (userPositionMarker == null || floorImage == null) {
//                Log.e(TAG, "User position marker or floor image is null");
//                return;
//            }
//
//            if (floorImage.getWidth() <= 0 || floorImage.getHeight() <= 0) {
//                Log.d(TAG, "Floor image dimensions not yet available");
//                return;
//            }
//
//            if (beacon1Distance == null || beacon2Distance == null || beacon3Distance == null) {
//                Log.d(TAG, "Not all beacon distances are available");
//                return;
//            }
//
//            // These values would be set based on your actual floor plan and beacon positions
//            final int MAP_WIDTH = floorImage.getWidth();
//            final int MAP_HEIGHT = floorImage.getHeight();
//
//            // Simple position calculation (this is just a placeholder)
//            // You'll need to implement proper trilateration based on your beacon positions
//            double totalDistance = beacon1Distance + beacon2Distance + beacon3Distance;
//            double x = (beacon1Distance / totalDistance) * MAP_WIDTH;
//            double y = (beacon2Distance / totalDistance) * MAP_HEIGHT;
//
//            // Ensure x and y are within bounds
//            x = Math.max(0, Math.min(x, MAP_WIDTH));
//            y = Math.max(0, Math.min(y, MAP_HEIGHT));
//
//            // Store final values for the runnable
//            final float finalX = (float) x;
//            final float finalY = (float) y;
//
//            // Update marker position
//            runOnUiThread(() -> {
//                // Position the marker - account for marker size
//                float markerWidth = userPositionMarker.getWidth() / 2f;
//                float markerHeight = userPositionMarker.getHeight() / 2f;
//
//                if (markerWidth == 0) markerWidth = 7.5f; // Half of the default 15dp
//                if (markerHeight == 0) markerHeight = 7.5f; // Half of the default 15dp
//
//                userPositionMarker.setX(finalX - markerWidth);
//                userPositionMarker.setY(finalY - markerHeight);
//                userPositionMarker.setVisibility(View.VISIBLE);
//
//                Log.d(TAG, "User position updated: x=" + finalX + ", y=" + finalY);
//            });
//        } catch (Exception e) {
//            Log.e(TAG, "Error calculating user position: " + e.getMessage(), e);
//        }


//    private void calculateAndDisplayUserPosition() {
//        try {
//            if (userPositionMarker == null || floorImage == null) {
//                Log.e(TAG, "User position marker or floor image is null");
//                return;
//            }
//
//            if (floorImage.getWidth() <= 0 || floorImage.getHeight() <= 0) {
//                Log.d(TAG, "Floor image dimensions not yet available");
//                return;
//            }
//
//            if (beacon1Distance == null || beacon2Distance == null || beacon3Distance == null) {
//                Log.d(TAG, "Not all beacon distances are available");
//                return;
//            }
//
//            // 1. Define known beacon positions in pixels (based on your floor plan)
//            // These are just examples, replace them with your actual values
//            PointF beacon1 = new PointF(294, 897);       // Top-left corner
//            PointF beacon2 = new PointF(726, 897);     // 200cm from B1 → 200 * 2.5 = 500 px
//            PointF beacon3 = new PointF(400, 110);   // (100cm, 150cm) → (250px, 375px)
//
//            // 2. Convert distances from meters to cm
//            double d1 = beacon1Distance * 100;
//            double d2 = beacon2Distance * 100;
//            double d3 = beacon3Distance * 100;
//
//            // 3. Trilateration in centimeters
//            PointF cmPos = trilaterate(d1, d2, d3,
//                    new PointF(beacon1.x / 2.5f, beacon1.y / 2.5f),
//                    new PointF(beacon2.x / 2.5f, beacon2.y / 2.5f),
//                    new PointF(beacon3.x / 2.5f, beacon3.y / 2.5f));
//
//            // 4. Convert cm to pixels
//            float xPixel = cmPos.x * 2.5f;
//            float yPixel = cmPos.y * 2.5f;
//
//            // Clamp within bounds
//            xPixel = Math.max(0, Math.min(xPixel, floorImage.getWidth()));
//            yPixel = Math.max(0, Math.min(yPixel, floorImage.getHeight()));
//
//            final float finalX = xPixel;
//            final float finalY = yPixel;
//
//            runOnUiThread(() -> {
//                float markerWidth = userPositionMarker.getWidth() / 2f;
//                float markerHeight = userPositionMarker.getHeight() / 2f;
//
//                if (markerWidth == 0) markerWidth = 7.5f;
//                if (markerHeight == 0) markerHeight = 7.5f;
//
//                userPositionMarker.setX(finalX - markerWidth);
//                userPositionMarker.setY(finalY - markerHeight);
//                userPositionMarker.setVisibility(View.VISIBLE);
//
//                Log.d(TAG, "User position updated: x=" + finalX + ", y=" + finalY);
//            });
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error calculating user position: " + e.getMessage(), e);
//        }
//    }

    private void calculateAndDisplayUserPosition() {
        try {
            if (userPositionMarker == null || floorImage == null) return;
            if (floorImage.getDrawable() == null) return;
            if (beacon1Distance == null || beacon2Distance == null || beacon3Distance == null) return;


            if (currentBeaconCoords == null || currentBeaconCoords.length != 3) return;

            PointF[] c = currentBeaconCoords;
            PointF p1 = new PointF(c[0].x/currentScalingRatio, c[0].y/currentScalingRatio);
            PointF p2 = new PointF(c[1].x/currentScalingRatio, c[1].y/currentScalingRatio);
            PointF p3 = new PointF(c[2].x/currentScalingRatio, c[2].y/currentScalingRatio);


            // Distance in cm
            double d1 = beacon1Distance * 100;
            double d2 = beacon2Distance * 100;
            double d3 = beacon3Distance * 100;

            // Trilaterate (returns result in cm)
            PointF cmResult = trilaterateHighAccuracy(d1, d2, d3, p1, p2, p3);

            // Convert result to image pixel coordinates
            float px = cmResult.x * currentScalingRatio;
            float py = cmResult.y * currentScalingRatio;

            ux = px;
            uy = py;

            // Convert image pixels → screen coordinates
            RectF imageRect = getImageDisplayRect(floorImage);
            float x = imageRect.left + (px / currentBaseImageWidth) * imageRect.width();
            float y = imageRect.top + (py / currentBaseImageHeight) * imageRect.height();

            // Clamp to image rect
            x = Math.max(imageRect.left, Math.min(x, imageRect.right));
            y = Math.max(imageRect.top, Math.min(y, imageRect.bottom));

            final float finalX = x;
            final float finalY = y;

            runOnUiThread(() -> {
                float markerWidth = userPositionMarker.getWidth() / 2f;
                float markerHeight = userPositionMarker.getHeight() / 2f;

                userPositionMarker.setX(finalX - markerWidth);
                userPositionMarker.setY(finalY - markerHeight);
                userPositionMarker.setVisibility(View.VISIBLE);
                Log.d(TAG, "User Position: " + finalX + ", " + finalY);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in calculateAndDisplayUserPosition: " + e.getMessage(), e);
        }
    }



//    private PointF trilaterate(double d1, double d2, double d3, PointF p1, PointF p2, PointF p3) {
//        double x1 = p1.x, y1 = p1.y;
//        double x2 = p2.x, y2 = p2.y;
//        double x3 = p3.x, y3 = p3.y;
//
//        double A = 2 * (x2 - x1);
//        double B = 2 * (y2 - y1);
//        double C = d1 * d1 - d2 * d2 - x1 * x1 + x2 * x2 - y1 * y1 + y2 * y2;
//
//        double D = 2 * (x3 - x2);
//        double E = 2 * (y3 - y2);
//        double F = d2 * d2 - d3 * d3 - x2 * x2 + x3 * x3 - y2 * y2 + y3 * y3;
//
//        double denominator = E * A - B * D;
//        if (denominator == 0) {
//            Log.e(TAG, "Trilateration failed: denominator is zero");
//            return new PointF(0, 0);
//        }
//
//        double x = (C * E - F * B) / denominator;
//        double y = (C * D - A * F) / (B * D - A * E);
//
//        return new PointF((float) x, (float) y);
//    }

    private PointF trilaterateHighAccuracy(
            double d1, double d2, double d3,
            PointF p1, PointF p2, PointF p3) {

        double[][] pos = {{p1.x,p1.y},{p2.x,p2.y},{p3.x,p3.y}};
        double[] dist = {d1, d2, d3};

        MultivariateJacobianFunction model = pt -> {
            double x=pt.getEntry(0), y=pt.getEntry(1);
            double[]   res = new double[3];
            double[][] jac = new double[3][2];

            for (int i=0;i<3;i++){
                double dx=x-pos[i][0], dy=y-pos[i][1];
                double md=Math.hypot(dx,dy);
                res[i] = md - dist[i];
                if (md!=0){
                    jac[i][0]=dx/md;
                    jac[i][1]=dy/md;
                }
            }
            return new Pair<>(new ArrayRealVector(res),
                    new Array2DRowRealMatrix(jac));
        };

        double[] guess = {(p1.x+p2.x+p3.x)/3, (p1.y+p2.y+p3.y)/3};

        LeastSquaresProblem prob = new LeastSquaresBuilder()
                .start(guess)
                .model(model)
                .target(new double[]{0,0,0})
                .lazyEvaluation(false)
                .maxEvaluations(1000)
                .maxIterations(1000)
                .checkerPair(new SimpleVectorValueChecker(1e-6,1e-6))
                .build();

        LeastSquaresOptimizer.Optimum opt = new LevenbergMarquardtOptimizer().optimize(prob);
        double[] sol = opt.getPoint().toArray();
        return new PointF((float)sol[0],(float)sol[1]);
    }


    private void drawBeaconMarkers() {
        floorImage.post(() -> {
            FrameLayout layout = findViewById(R.id.frame);
            Drawable drawable = floorImage.getDrawable();
            if (drawable == null) return;

            RectF imageBounds = getImageDisplayRect(floorImage);

            // Beacon positions in original image (1080x1080)
            PointF[] beacons = currentBeaconCoords;
            if (beacons == null || beacons.length != 3) return;

            for (int i = 0; i < beacons.length; i++) {
                PointF point = beacons[i];
                float px = imageBounds.left + (point.x / currentBaseImageWidth) * imageBounds.width();
                float py = imageBounds.top + (point.y / currentBaseImageHeight) * imageBounds.height(); // assuming square
                int color = (i == 0) ? Color.RED : (i == 1) ? Color.BLUE : Color.GREEN;
                addBeaconMarker(layout, new PointF(px, py), color);
            }
        });
    }

    private RectF getImageDisplayRect(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return new RectF();

        Matrix matrix = imageView.getImageMatrix();
        RectF rect = new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(rect);
        return rect;
    }


    private void addBeaconMarker(FrameLayout layout, PointF position, int color) {
        View marker = new View(this);
        int size = 20;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        marker.setLayoutParams(params);
        marker.setBackgroundColor(color);
        marker.setX(position.x - size / 2f);
        marker.setY(position.y - size / 2f);
        marker.setBackground(getResources().getDrawable(R.drawable.beacon_marker_circle)); // Optional: circle shape
        layout.addView(marker);
    }


    private void resetPositionTracking() {
        try {
            // Reset beacon distances
            beacon1Distance = null;
            beacon2Distance = null;
            beacon3Distance = null;

            // Hide position marker until we get new readings
            if (userPositionMarker != null) {
                userPositionMarker.setVisibility(View.INVISIBLE);
            }

            // Clear distance text
            if (beaconDistancesText != null) {
                beaconDistancesText.setText("Scanning for beacons...");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting position tracking: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Only start scanning if permissions are granted
            if (permissionsGranted) {

                // Start Bluetooth scanning if available and not already running
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bleScanner != null &&
                        isHandlerCallbackScheduled(bleHandler, bleScanRunnable)) {
                    bleHandler.post(bleScanRunnable);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }

    private boolean isHandlerCallbackScheduled(Handler handler, Runnable runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return !handler.hasCallbacks(runnable);
        } else {
            // For older Android versions, we'll just always return true
            // This means we might post the same runnable multiple times,
            // but it's better than not scanning at all
            return true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Optional: pause scanning to save battery
        // scanHandler.removeCallbacks(scanRunnable);
        // bleHandler.removeCallbacks(bleScanRunnable);
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();

            // Clean up Bluetooth scanning
            bleHandler.removeCallbacks(bleScanRunnable);

            // Stop BLE scanning
            if (bleScanner != null && bleScanCallback != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                                == PackageManager.PERMISSION_GRANTED) {
                            bleScanner.stopScan(bleScanCallback);
                        }
                    } else {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            bleScanner.stopScan(bleScanCallback);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping BLE scan: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }
}