package com.example.researchproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class HouseActivity extends AppCompatActivity {
    private final String FLOOR1_WIFI = "SLT-Fiber-2.4G";
    private final String FLOOR2_WIFI = "ZONG MBB-E5573-7A0B";

    private TextView floorText;
    private ImageView floorImage;
    private String currentFloor = ""; // track last floor
    private Button localizeBtn;

    private final int SCAN_INTERVAL = 30000; // 30 seconds
    private final android.os.Handler scanHandler = new android.os.Handler();
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            checkFloorBasedOnWifi();
            scanHandler.postDelayed(this, SCAN_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_house);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        floorText = findViewById(R.id.floorText);
        floorImage = findViewById(R.id.floorImage);
        localizeBtn = findViewById(R.id.localizeButton);
        localizeBtn.setOnClickListener(v -> {
            if (currentFloor == null || currentFloor.isEmpty()) {
                Toast.makeText(HouseActivity.this, "Floor not detected yet", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(HouseActivity.this, LocalizeActivity.class);
            intent.putExtra("locationType", "house");
            intent.putExtra("floor", currentFloor);
            startActivity(intent);
        });
        scanHandler.post(scanRunnable); // Start scanning
    }

    private void checkFloorBasedOnWifi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1003);
            return;
        }

        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(this, "Wi-Fi scan failed", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ScanResult> scanResults = wifiManager.getScanResults();
        Integer floor1Rssi = null;
        Integer floor2Rssi = null;

        for (ScanResult result : scanResults) {
            if (result.SSID.equals(FLOOR1_WIFI)) {
                floor1Rssi = result.level;
            } else if (result.SSID.equals(FLOOR2_WIFI)) {
                floor2Rssi = result.level;
            }
        }

        if (floor1Rssi == null && floor2Rssi == null) {
            scanHandler.removeCallbacks(scanRunnable);
            Toast.makeText(this, "You seem to have exited the building.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Calculate distance from RSSI
        Double distance1 = (floor1Rssi != null) ? estimateDistanceFromRssi(floor1Rssi) : null;
        Double distance2 = (floor2Rssi != null) ? estimateDistanceFromRssi(floor2Rssi) : null;

        String detectedFloor = null;

        if (distance1 != null && distance2 != null) {
            detectedFloor = (distance1 < distance2) ? "1st Floor" : "2nd Floor";
        } else if (distance1 != null) {
            detectedFloor = "1st Floor";
        } else if (distance2 != null) {
            detectedFloor = "2nd Floor";
        }

        // Only update UI if floor changed
        if (detectedFloor != null && !detectedFloor.equals(currentFloor)) {
            currentFloor = detectedFloor;
            floorText.setText("You are on the " + currentFloor + ".");
            if (detectedFloor.equals("1st Floor")) {
                floorImage.setImageResource(R.drawable.floor_1); // Floor-1.png → floor_1.png
            } else {
                floorImage.setImageResource(R.drawable.floor_2); // Floor-2.png → floor_2.png
            }
        }
    }

    private double estimateDistanceFromRssi(int rssi) {
        int txPower = -45; // RSSI at 1 meter (may vary per device/WiFi)
        double n = 3.0;     // Environmental factor (higher for thick walls)
        return Math.pow(10.0, (txPower - rssi) / (10.0 * n));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanHandler.removeCallbacks(scanRunnable);
    }
}
