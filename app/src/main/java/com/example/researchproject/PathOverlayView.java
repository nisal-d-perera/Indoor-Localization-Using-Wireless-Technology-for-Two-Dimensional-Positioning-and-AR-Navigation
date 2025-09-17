package com.example.researchproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.view.View;

import java.util.List;

public class PathOverlayView extends View {
    private List<Point> path;
    private Paint linePaint;
    private Paint arrowPaint;
    private int bmpW, bmpH;

    public PathOverlayView(Context ctx, List<Point> path, int bitmapWidth, int bitmapHeight) {
        super(ctx);
        this.path = path;
        this.bmpW  = bitmapWidth;
        this.bmpH  = bitmapHeight;

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.RED);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(8f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.BLUE);
        arrowPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (path == null || path.size() < 2) return;

        // compute scaling to view
        float scaleX = getWidth()  / (float) bmpW;
        float scaleY = getHeight() / (float) bmpH;

        // draw path polyline
        Path poly = new Path();
        Point p0 = path.get(0);
        poly.moveTo(p0.x * scaleX, p0.y * scaleY);
        for (int i = 1; i < path.size(); i++) {
            Point pi = path.get(i);
            poly.lineTo(pi.x * scaleX, pi.y * scaleY);
            drawArrow(canvas, path.get(i-1), pi, scaleX, scaleY);
        }
        canvas.drawPath(poly, linePaint);
    }

    private void drawArrow(Canvas c, Point s, Point e, float sx, float sy) {
        float x1 = s.x * sx, y1 = s.y * sy;
        float x2 = e.x * sx, y2 = e.y * sy;
        float dx = x2 - x1, dy = y2 - y1;
        float ang = (float)Math.atan2(dy, dx);
        float len = 30f;
        float wing = (float)Math.toRadians(30);

        float ax1 = x2 - len * (float)Math.cos(ang - wing);
        float ay1 = y2 - len * (float)Math.sin(ang - wing);
        float ax2 = x2 - len * (float)Math.cos(ang + wing);
        float ay2 = y2 - len * (float)Math.sin(ang + wing);

        Path arrow = new Path();
        arrow.moveTo(x2, y2);
        arrow.lineTo(ax1, ay1);
        arrow.lineTo(ax2, ay2);
        arrow.close();
        c.drawPath(arrow, arrowPaint);
    }
}