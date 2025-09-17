package com.example.researchproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathActivity extends AppCompatActivity {

    private TextView textLocation;
    private ImageView floorImage;
    private TextView textDestination;
    private String locationType;
    private String floor;
    private String destination;
    private float userX;
    private float userY;
    private float destinationX;
    private float destinationY;
    private float destiX;
    private float destiY;
    private View userPositionMarker;
    private Button arNavigation;
    private List<Point> path;

    private Map<String, Map<String, Integer>> baseImageWidthMap = new HashMap<>();
    private Map<String, Map<String, Integer>> baseImageHeightMap = new HashMap<>();
    private Map<String, Map<String, Float>> scalingRatioMap = new HashMap<>();

    private int currentBaseImageWidth = 1080; // default
    private int currentBaseImageHeight = 1080; // default
    private float currentScalingRatio = 2.0f; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_path);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        locationType = getIntent().getStringExtra("locationType");
        floor = getIntent().getStringExtra("floor");
        destination = getIntent().getStringExtra("destinationLabel");
        userX = getIntent().getFloatExtra("userX", 0f);
        userY = getIntent().getFloatExtra("userY", 0f);

        destinationX = getIntent().getFloatExtra("destinationX", 0f);
        destinationY = getIntent().getFloatExtra("destinationY", 0f);

        initializeDestinationConfig();

        if (locationType != null && floor != null) {
            Map<String, Integer> widthMap = baseImageWidthMap.get(locationType);
            Map<String, Integer> heightMap = baseImageHeightMap.get(locationType); // ✅ NEW
            Map<String, Float> scaleMap = scalingRatioMap.get(locationType);

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
        textDestination = findViewById(R.id.DestiText);
        userPositionMarker = findViewById(R.id.userPositionMarker);
        if (userPositionMarker != null) {
            userPositionMarker.setVisibility(View.INVISIBLE);
        }

        if (floorImage != null && locationType != null && floor != null) {
            if (locationType.equalsIgnoreCase("house")) {
                textLocation.setText("My Home");
                if (floor.equalsIgnoreCase("1st Floor")) {
                    floorImage.setImageResource(R.drawable.wroom_1);
                    textDestination.setText("Your Location to " + destination);
                } else if (floor.equalsIgnoreCase("2nd Floor")) {
                    floorImage.setImageResource(R.drawable.wroom_21);
                    textDestination.setText("Your Location to " + destination);
                }
            }
            if (locationType.equalsIgnoreCase("campus")) {
                textLocation.setText("BCI Campus");
                if (floor.equalsIgnoreCase("1st Floor")) {
                    floorImage.setImageResource(R.drawable.r_lab);
                    textDestination.setText("Your Location to " + destination);
                } else if (floor.equalsIgnoreCase("2nd Floor")) {
                    floorImage.setImageResource(R.drawable.lab01_wp);
                    textDestination.setText("Your Location to " + destination);
                }
            }
        }



        floorImage.post(() -> drawDestinationMarker());
        floorImage.post(() -> userPositionMarker(userX, userY));
//        floorImage.post(() -> {
//
//            Bitmap src = ((BitmapDrawable) floorImage.getDrawable()).getBitmap();
//// 2. Copy it into a mutable bitmap
//            Bitmap debugBmp = src.copy(Bitmap.Config.ARGB_8888, true);
//
//// 3. Detect the walkable pixels
//            List<Point> walkablePts = detectWalkablePixels(debugBmp, 30);
//            Log.d("PathActivity", "Found walkable pixels: " + walkablePts.size());
//
//// 4. Paint them in pure red on debugBmp
//            for (Point p : walkablePts) {
//                debugBmp.setPixel(p.x, p.y, Color.RED);
//            }
//
//// 5. Show debugBmp in your ImageView so you can see exactly which pixels matched
//            floorImage.setImageBitmap(debugBmp);
//        });
//        floorImage.post(this::processAndDrawPath);

        floorImage.post(() -> {
            // 1) Grab the original bitmap
            Bitmap src = ((BitmapDrawable) floorImage.getDrawable()).getBitmap();

            // 2) Make a mutable copy so we can draw on it
            Bitmap debugBmp = src.copy(Bitmap.Config.ARGB_8888, true);


            int width  = debugBmp.getWidth();
            int height = debugBmp.getHeight();
            int targetColor = Color.parseColor("#c8c6c6");
            int tolerance   = 30;

            // 3) Build the walkable grid
            boolean[][] walkable = new boolean[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pix = debugBmp.getPixel(x, y);
                    walkable[y][x] = isSimilarColor(pix, targetColor, tolerance);
                }
            }

            // 4) Prepare start & goal
            // Point start = new Point((int) userX, (int) userY);
            Point rawUserPos = new Point((int) userX, (int) userY);
            Point start = checkUserPosition(walkable, rawUserPos.x, rawUserPos.y);
            Point goal  = new Point((int) destinationX, (int) destinationY);



            // If they differ, draw a dotted line:
            if (!rawUserPos.equals(start)) {
                Canvas canvas = new Canvas(debugBmp);
                Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                dotPaint.setColor(Color.BLUE);
                dotPaint.setStyle(Paint.Style.STROKE);
                dotPaint.setStrokeWidth(5f);
                // very short on (2px), longer off (10px) → dots
                dotPaint.setPathEffect(new DashPathEffect(new float[]{15f,10f}, 0));

                Path dotPath = new Path();
                dotPath.moveTo(rawUserPos.x, rawUserPos.y);
                dotPath.lineTo(start.x, start.y);
                canvas.drawPath(dotPath, dotPaint);
            }


            // 5) Run A*
            AStarPathfinder pf = new AStarPathfinder(true);
            path = pf.findPath(walkable, start, goal);

            if (path != null && !path.isEmpty()) {
                // Create a Canvas backed by your mutable bitmap
                Canvas canvas = new Canvas(debugBmp);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(8f);            // ← adjust this for thicker/thinner
                paint.setStrokeCap(Paint.Cap.ROUND); // ← optional: rounded line ends

                // Draw line segments between each pair of consecutive points
                for (int i = 1; i < path.size(); i++) {
                    Point a = path.get(i - 1);
                    Point b = path.get(i);
                    canvas.drawLine(a.x, a.y, b.x, b.y, paint);

                }
            }
            else {
                Log.d("PathActivity", "👀 No path found!");
            }


            // 7) Push the result back into your ImageView
            floorImage.setImageBitmap(debugBmp);

            // after floorImage.setImageBitmap(debugBmp);
            FrameLayout layout = findViewById(R.id.frame);

            // pick the first step of your path
            if (path != null && path.size() >= 2) {
                Point from = path.get(0);
                Point to   = path.get(1);

                // compute screen coords of 'from'
                RectF disp = getImageDisplayRect(floorImage);
                float sx = disp.left  + (from.x / (float)debugBmp.getWidth())  * disp.width();
                float sy = disp.top   + (from.y / (float)debugBmp.getHeight()) * disp.height();

                // compute the normalized direction vector
                float dx = to.x - from.x, dy = to.y - from.y;
                float dist = (float)Math.hypot(dx, dy);
                float ux = dx / dist, uy = dy / dist;

                // choose how far from the user (in pixels on the image)
                // e.g. 50 px ahead along the path
                float offsetAlongPath = 50f;

                // convert that to screen‐space offset
                float arrowX = sx + ux * (offsetAlongPath * disp.width()  / debugBmp.getWidth());
                float arrowY = sy + uy * (offsetAlongPath * disp.height() / debugBmp.getHeight());

                // rotation angle in degrees
                float angle = (float)Math.toDegrees(Math.atan2(dy, dx));

                // add the arrow ImageView
                ImageView arrow = new ImageView(this);
                arrow.setImageResource(R.drawable.ic_arrow_head_24);  // right‑pointing asset
                arrow.setRotation(angle);

                int sizeDp = 48;
                int sizePx = (int)(sizeDp * getResources().getDisplayMetrics().density);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
                arrow.setLayoutParams(lp);

                // center the arrow bitmap on (arrowX, arrowY)
                arrow.setX(arrowX - sizePx/2f);
                arrow.setY(arrowY - sizePx/2f);

                layout.addView(arrow);
            }

        });

        arNavigation = findViewById(R.id.startButton);
        arNavigation.setOnClickListener(v -> {
//            ArrayList<int[]> pointArrayList = new ArrayList<>();
//            for (Point p : path) {
//                pointArrayList.add(new int[]{p.x, p.y});
//            }
            Intent intent = new Intent(PathActivity.this, ARNavigationActivity.class);
//            intent.putExtra("path", pointArrayList);
            startActivity(intent);
        });


    }


    private void initializeDestinationConfig() {

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

//    private void userPositionMarker(float finalX, float finalY){
//
//        float markerWidth = userPositionMarker.getWidth() / 2f;
//        float markerHeight = userPositionMarker.getHeight() / 2f;
//
//        userPositionMarker.setX(finalX - markerWidth);
//        userPositionMarker.setY(finalY - markerHeight);
//        userPositionMarker.setVisibility(View.VISIBLE);
//    }

    private void userPositionMarker(float px, float py){

        RectF imageRect = getImageDisplayRect(floorImage);
        float x = imageRect.left + (px / currentBaseImageWidth) * imageRect.width();
        float y = imageRect.top + (py / currentBaseImageHeight) * imageRect.height();

        // Clamp to image rect
        x = Math.max(imageRect.left, Math.min(x, imageRect.right));
        y = Math.max(imageRect.top, Math.min(y, imageRect.bottom));

        final float finalX = x;
        final float finalY = y;

        float markerWidth = userPositionMarker.getWidth() / 2f;
        float markerHeight = userPositionMarker.getHeight() / 2f;

        userPositionMarker.setX(finalX - markerWidth);
        userPositionMarker.setY(finalY - markerHeight);
        userPositionMarker.setVisibility(View.VISIBLE);
    }

//    private void drawdesinationMarkers() {
//        floorImage.post(() -> {
//            FrameLayout layout = findViewById(R.id.frame);
//            Drawable drawable = floorImage.getDrawable();
//            if (drawable == null) return;
//
//            RectF imageBounds = getImageDisplayRect(floorImage);
//            destiX = ((destinationX - imageBounds.left)  / imageBounds.width())  * currentBaseImageWidth;
//            destiY = ((destinationY - imageBounds.top)   / imageBounds.height()) * currentBaseImageHeight;
//
//
//            addLocationMarker(layout, new PointF(destinationX, destinationY), destination);
//
//        });
//    }

    private void drawDestinationMarker() {
        floorImage.post(() -> {
            FrameLayout layout = findViewById(R.id.frame);
            Drawable drawable = floorImage.getDrawable();
            if (drawable == null) return;

            // 1) Get the on‑screen bounds of the ImageView’s bitmap
            RectF imageBounds = getImageDisplayRect(floorImage);

            // 2) Map your bitmap coords → view coords
            float px = imageBounds.left
                    + (destinationX / (float)currentBaseImageWidth) * imageBounds.width();
            float py = imageBounds.top
                    + (destinationY / (float)currentBaseImageHeight) * imageBounds.height();

            // 3) Draw the marker at the scaled position
            addLocationMarker(layout, new PointF(px, py), destination);
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


    private void addLocationMarker(FrameLayout layout, PointF position, String labelText) {
        // Create marker pin
        ImageView marker = new ImageView(this);
        int markerWidth = 40;
        int markerHeight = 60;

        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(markerWidth, markerHeight);
        marker.setLayoutParams(markerParams);
        marker.setImageResource(R.drawable.location_red);

        // Align the bottom-center of the marker to the given point
        float markerX = position.x - (markerWidth / 2f);
        float markerY = position.y - markerHeight + 10f; // 10f = optional fine adjustment

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

        float dotX = position.x - (dotSize / 2f);
        float dotY = position.y - (dotSize / 2f);
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
        float labelX = position.x + (markerWidth / 2f) + 6f;
        float labelY = markerY + (markerHeight / 2f) - 10f;

        label.setX(labelX);
        label.setY(labelY);
        layout.addView(label);

    }


    private boolean similar(int a, int b, int t) {
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        return Math.abs(ar-br) <= t &&
                Math.abs(ag-bg) <= t &&
                Math.abs(ab-bb) <= t;
    }

    private boolean isSimilarColor(int pixelColor, int targetColor, int tolerance) {
        int pr = Color.red(pixelColor),   pg = Color.green(pixelColor),   pb = Color.blue(pixelColor);
        int tr = Color.red(targetColor),  tg = Color.green(targetColor),  tb = Color.blue(targetColor);

        return Math.abs(pr - tr) <= tolerance &&
                Math.abs(pg - tg) <= tolerance &&
                Math.abs(pb - tb) <= tolerance;
    }

    private List<Point> detectWalkablePixels(Bitmap bmp, int tolerance) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int target = Color.parseColor("#c8c6c6");

        List<Point> walkables = new ArrayList<>(width * height / 10);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pix = bmp.getPixel(x, y);
                if (isSimilarColor(pix, target, tolerance)) {
                    walkables.add(new Point(x, y));
                }
            }
        }
        return walkables;
    }

    private Point checkUserPosition(boolean[][] walkable, int userX, int userY) {
        int height = walkable.length;
        int width = walkable[0].length;

        // 1. If user is already on a walkable pixel
        if (userY >= 0 && userY < height && userX >= 0 && userX < width && walkable[userY][userX]) {
            return new Point(userX, userY);
        }

        // 2. Search nearby for the closest walkable pixel
        int maxRadius = 100;  // Max distance to search
        double minDist = Double.MAX_VALUE;
        Point closest = new Point(userX, userY); // fallback

        for (int dy = -maxRadius; dy <= maxRadius; dy++) {
            for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                int nx = userX + dx;
                int ny = userY + dy;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && walkable[ny][nx]) {
                    double dist = Math.hypot(dx, dy);
                    if (dist < minDist) {
                        minDist = dist;
                        closest.set(nx, ny);
                    }
                }
            }
        }

        return closest;
    }


}