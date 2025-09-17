package com.example.researchproject;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;

public class NavigateActivity extends AppCompatActivity {

    private TextView textLocation;
    private ImageView floorImage;
    private String locationType;
    private String floor;
    private View userPositionMarker;
    private float ux;
    private float uy;
    private float finalX;
    private float finalY;

    private Map<String, Map<String, PointF[]>> destinationPositionMap = new HashMap<>();
    private Map<String, Map<String, String[]>> labelMap = new HashMap<>();
    private Map<String, Map<String, Integer>> baseImageWidthMap = new HashMap<>();
    private Map<String, Map<String, Integer>> baseImageHeightMap = new HashMap<>();
    private Map<String, Map<String, Float>> scalingRatioMap = new HashMap<>();

    private PointF[] currentdestinationCoords = null;
    private int currentBaseImageWidth = 1080; // default
    private int currentBaseImageHeight = 1080; // default
    private float currentScalingRatio = 2.0f; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_navigate);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeDestinationConfig();

        locationType = getIntent().getStringExtra("locationType");
        floor = getIntent().getStringExtra("floor");
        ux = getIntent().getFloatExtra("lx", 0f);
        uy = getIntent().getFloatExtra("ly", 0f);


        if (locationType != null && floor != null) {
            Map<String, PointF[]> beaconFloorMap = destinationPositionMap.get(locationType);
            Map<String, Integer> widthMap = baseImageWidthMap.get(locationType);
            Map<String, Integer> heightMap = baseImageHeightMap.get(locationType); // ✅ NEW
            Map<String, Float> scaleMap = scalingRatioMap.get(locationType);

            if (beaconFloorMap != null) {
                currentdestinationCoords = beaconFloorMap.get(floor);
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

        textLocation = findViewById(R.id.textLocation);
        floorImage = findViewById(R.id.floorImage);
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
                    floorImage.setImageResource(R.drawable.lab01_wp);
                }
            }
//
//            ux = 193;
//            uy = 395;

//            ux = 110;
//            uy = 395;

            floorImage.post(() -> drawdesinationMarkers());
            floorImage.post(() -> userPositionMarker(ux, uy));
        }
    }

    private void initializeDestinationConfig() {
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
                new PointF(260, 870), new PointF(690, 870), new PointF(400, 130)
        });
        campusFloors.put("2nd Floor", new PointF[]{
                new PointF(70, 63), new PointF(20, 921), new PointF(471, 307)
        });

        destinationPositionMap.put("house", houseFloors);
        destinationPositionMap.put("campus", campusFloors);

        Map<String, String[]> houseLabels = new HashMap<>();
        houseLabels.put("1st Floor", new String[]{
                "Location 1", "Location 2", "Location 3"
        });
        houseLabels.put("2nd Floor", new String[]{
                "Lecturer Table", "Door", "Projector"
        });

        Map<String, String[]> campusLabels = new HashMap<>();
        campusLabels.put("1st Floor", new String[]{
                "Location 1", "Location 2", "Location 3"
        });
        campusLabels.put("2nd Floor", new String[]{
                "Lecturer Table", "Door", "Projector"
        });

        labelMap.put("house", houseLabels);
        labelMap.put("campus", campusLabels);

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

        // Scaling RatiosA
        Map<String, Float> houseScaling = new HashMap<>();
        houseScaling.put("1st Floor", 2.5f);
        houseScaling.put("2nd Floor", 2.0f);

        Map<String, Float> campusScaling = new HashMap<>();
        campusScaling.put("1st Floor", 1.0f);
        campusScaling.put("2nd Floor", 1.0f);

        scalingRatioMap.put("house", houseScaling);
        scalingRatioMap.put("campus", campusScaling);
    }

    private void userPositionMarker(float px, float py){

        RectF imageRect = getImageDisplayRect(floorImage);
        float x = imageRect.left + (px / currentBaseImageWidth) * imageRect.width();
        float y = imageRect.top + (py / currentBaseImageHeight) * imageRect.height();

        // Clamp to image rect
        x = Math.max(imageRect.left, Math.min(x, imageRect.right));
        y = Math.max(imageRect.top, Math.min(y, imageRect.bottom));

        finalX = x;
        finalY = y;

        float markerWidth = userPositionMarker.getWidth() / 2f;
        float markerHeight = userPositionMarker.getHeight() / 2f;

        userPositionMarker.setX(finalX - markerWidth);
        userPositionMarker.setY(finalY - markerHeight);
        userPositionMarker.setVisibility(View.VISIBLE);
    }

    private void drawdesinationMarkers() {
        floorImage.post(() -> {
            FrameLayout layout = findViewById(R.id.frame);
            Drawable drawable = floorImage.getDrawable();
            if (drawable == null) return;

            RectF imageBounds = getImageDisplayRect(floorImage);

            // Beacon positions in original image (1080x1080)
            PointF[] destinations = currentdestinationCoords;
            if (destinations == null || destinations.length != 3) return;
            String[] labels = null;
            if (labelMap.containsKey(locationType) && labelMap.get(locationType).containsKey(floor)) {
                labels = labelMap.get(locationType).get(floor);
            }

            for (int i = 0; i < currentdestinationCoords.length; i++) {
                PointF orig = currentdestinationCoords[i];   // True bitmap coords
                PointF screenPos = destinations[i];

                float px = imageBounds.left + (screenPos.x / currentBaseImageWidth) * imageBounds.width();
                float py = imageBounds.top + (screenPos.y / currentBaseImageHeight) * imageBounds.height();

                String label = (labels != null && i < labels.length) ? labels[i] : "Label";

                // Add marker and text label
                addLocationMarker(layout, new PointF(px, py), labels[i], currentdestinationCoords[i]);
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


    private void addLocationMarker(FrameLayout layout,
                                   PointF screenPosition,
                                   String labelText,
                                   PointF originalBitmapPosition) {
        // Create marker pin
        ImageView marker = new ImageView(this);
        int markerWidth = 40;
        int markerHeight = 60;

        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(markerWidth, markerHeight);
        marker.setLayoutParams(markerParams);
        marker.setImageResource(R.drawable.location_red);

        // Align the bottom-center of the marker to the given point
        float markerX = screenPosition.x - (markerWidth / 2f);
        float markerY = screenPosition.y - markerHeight + 10f; // 10f = optional fine adjustment

        marker.setX(markerX);
        marker.setY(markerY);

        layout.addView(marker);

        // ✅ Optional: Debug dot (centered at exact position)
        int dotSize = 12;
        View dot = new View(this);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dotSize, dotSize);
        dot.setLayoutParams(dotParams);

        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        circleDrawable.setColor(Color.RED);
        circleDrawable.setSize(dotSize, dotSize); // Not strictly necessary for View background
        dot.setBackground(circleDrawable);

        float dotX = screenPosition.x - (dotSize / 2f);
        float dotY = screenPosition.y - (dotSize / 2f);
        dot.setX(dotX);
        dot.setY(dotY);

        layout.addView(dot);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(14f);
        label.setTextColor(Color.BLACK);
        //label.setBackgroundColor(Color.WHITE);
        label.setPadding(6, 2, 6, 2);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        label.setLayoutParams(textParams);

        // Place label to the right of the marker
        float labelX = screenPosition.x + (markerWidth / 2f) + 6f;
        float labelY = markerY + (markerHeight / 2f) - 10f;

        label.setX(labelX);
        label.setY(labelY);
        layout.addView(label);

        label.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, PathActivity.class);

            // Sending all required data
            intent.putExtra("locationType", locationType);
            intent.putExtra("floor", floor);

            intent.putExtra("userX", ux);
            intent.putExtra("userY", uy);

            // Destination marker
            intent.putExtra("destinationX", originalBitmapPosition.x);
            intent.putExtra("destinationY", originalBitmapPosition.y);
            intent.putExtra("destinationLabel", labelText);

            startActivity(intent);
        });
    }

}