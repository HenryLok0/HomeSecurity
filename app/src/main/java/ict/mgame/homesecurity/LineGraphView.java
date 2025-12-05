package ict.mgame.homesecurity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LineGraphView extends View {

    private List<Float> dataPoints = new ArrayList<>();
    private Paint linePaint;
    private Paint axisPaint;
    private int lineColor = Color.BLUE;
    private float minValue = 0;
    private float maxValue = 100;

    public LineGraphView(Context context) {
        super(context);
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        axisPaint = new Paint();
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(2f);
        axisPaint.setTextSize(30f);
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    public void setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        invalidate();
    }

    public void setData(List<Float> data) {
        this.dataPoints = new ArrayList<>(data);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints == null || dataPoints.size() < 2) return;

        float width = getWidth();
        float height = getHeight();
        float padding = 50f;

        // Draw axes
        canvas.drawLine(padding, height - padding, width, height - padding, axisPaint); // X
        canvas.drawLine(padding, height - padding, padding, 0, axisPaint); // Y

        // Draw path
        Path path = new Path();
        float xStep = (width - padding) / (dataPoints.size() - 1);
        float range = maxValue - minValue;
        if (range == 0) range = 1;

        for (int i = 0; i < dataPoints.size(); i++) {
            float val = dataPoints.get(i);
            // Clamp value
            if (val < minValue) val = minValue;
            if (val > maxValue) val = maxValue;

            float x = padding + i * xStep;
            float y = (height - padding) - ((val - minValue) / range) * (height - 2 * padding);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, linePaint);
        
        // Draw current value text
        if (!dataPoints.isEmpty()) {
            float lastVal = dataPoints.get(dataPoints.size() - 1);
            canvas.drawText(String.format("%.1f", lastVal), width - 100, 50, axisPaint);
        }
    }
}
