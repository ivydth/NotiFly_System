package com.example.notifly_system;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * GlowView
 *
 * A custom View that paints a radial gradient glow —
 * bright and vivid at the centre, fading smoothly to
 * fully transparent at the edges. Place this BEHIND the
 * card in the layout.
 *
 * Call setGlowColor(int color) to change the accent color
 * at runtime, then call invalidate() to repaint.
 */
public class GlowView extends View {

    private Paint  paint;
    private int    glowColor = Color.WHITE;
    private boolean shaderDirty = true;

    public GlowView(Context context) {
        super(context);
        init();
    }

    public GlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Hardware layer so alpha animations are GPU-composited cheaply
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    /**
     * Set the accent color of the glow.
     * Fully opaque accent → transparent over the radius.
     */
    public void setGlowColor(int color) {
        // Strip any existing alpha — we control it through the gradient stops
        glowColor   = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
        shaderDirty = true;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        shaderDirty = true; // rebuild shader for new size
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        if (shaderDirty) {
            buildShader(w, h);
            shaderDirty = false;
        }

        // Draw an ellipse to make the glow wider than it is tall,
        // like a pool of light sitting on a surface
        canvas.drawOval(0, 0, w, h, paint);
    }

    private void buildShader(int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.max(cx, cy);

        // Gradient stops:
        //   center  → full color at 75% opacity (vivid core)
        //   mid     → same color at 35% opacity
        //   edge    → fully transparent
        int centerColor = Color.argb(190, Color.red(glowColor),
                                          Color.green(glowColor),
                                          Color.blue(glowColor));
        int midColor    = Color.argb(90,  Color.red(glowColor),
                                          Color.green(glowColor),
                                          Color.blue(glowColor));
        int edgeColor   = Color.argb(0,   Color.red(glowColor),
                                          Color.green(glowColor),
                                          Color.blue(glowColor));

        RadialGradient shader = new RadialGradient(
                cx, cy, radius,
                new int[]  { centerColor, midColor, edgeColor },
                new float[] { 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        );

        paint.setShader(shader);
    }
}
