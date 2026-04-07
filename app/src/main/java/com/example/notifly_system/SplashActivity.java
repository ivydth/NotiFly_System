package com.example.notifly_system;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import pl.droidsonroids.gif.GifImageView;

public class SplashActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private GifImageView birdAnimation;
    private TextView tvAppName;
    private Button btnGetStarted;
    private View transitionOverlay;

    // ── Timing constants (ms) ──────────────────────────────────────────────
    private static final long FLY_AROUND_DURATION = 4000;
    private static final long RETURN_DURATION      = 800;
    private static final long UI_FADE_IN_DELAY     = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        // Immersive fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        bindViews();
        applyGradientToTitle();
        startSplashSequence();
        setupGetStartedButton();
    }

    // ── View binding ───────────────────────────────────────────────────────
    private void bindViews() {
        birdAnimation     = findViewById(R.id.birdAnimation);
        tvAppName         = findViewById(R.id.tvAppName);
        btnGetStarted     = findViewById(R.id.btnGetStarted);
        transitionOverlay = findViewById(R.id.transitionOverlay);
    }

    // ── Gradient title ─────────────────────────────────────────────────────
    private void applyGradientToTitle() {
        tvAppName.post(() -> {
            float width = tvAppName.getPaint().measureText(tvAppName.getText().toString());
            int[] colors = { 0xFF38D6FF, 0xFF7B5FFF, 0xFFBB44FF };
            float[] positions = { 0f, 0.5f, 1f };
            LinearGradient gradient = new LinearGradient(
                    0, 0, width, 0, colors, positions, Shader.TileMode.CLAMP);
            tvAppName.getPaint().setShader(gradient);
            tvAppName.invalidate();
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MASTER SEQUENCE
    // ══════════════════════════════════════════════════════════════════════
    private void startSplashSequence() {
        new Handler().postDelayed(() -> {
            flyAroundScreen();        // Phase 1 – bird flies smoothly around
            scheduleReturnToCenter(); // Phase 2 – bird glides back
            scheduleFadeInUI();       // Phase 3 – reveal text + button
        }, 300);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 1 — SMOOTH FLY AROUND
    // ══════════════════════════════════════════════════════════════════════
    private void flyAroundScreen() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;

        float[][] waypoints = {
                {  sw * 0.35f, -sh * 0.28f },
                {  sw * 0.38f,  sh * 0.18f },
                {  sw * 0.10f,  sh * 0.35f },
                { -sw * 0.35f,  sh * 0.22f },
                { -sw * 0.33f, -sh * 0.26f },
                {  0f,         -sh * 0.08f },
        };

        long segmentDuration = FLY_AROUND_DURATION / waypoints.length;
        Animator[] segments = new Animator[waypoints.length * 2];

        float startX = birdAnimation.getTranslationX();
        float startY = birdAnimation.getTranslationY();

        for (int i = 0; i < waypoints.length; i++) {
            float fromX = (i == 0) ? startX : waypoints[i - 1][0];
            float fromY = (i == 0) ? startY : waypoints[i - 1][1];
            float toX   = waypoints[i][0];
            float toY   = waypoints[i][1];

            ObjectAnimator xAnim = ObjectAnimator.ofFloat(
                    birdAnimation, "translationX", fromX, toX);
            ObjectAnimator yAnim = ObjectAnimator.ofFloat(
                    birdAnimation, "translationY", fromY, toY);

            xAnim.setDuration(segmentDuration);
            yAnim.setDuration(segmentDuration);
            xAnim.setInterpolator(new LinearInterpolator());
            yAnim.setInterpolator(new LinearInterpolator());
            xAnim.setStartDelay(i * segmentDuration);
            yAnim.setStartDelay(i * segmentDuration);

            segments[i * 2]     = xAnim;
            segments[i * 2 + 1] = yAnim;
        }

        AnimatorSet flySet = new AnimatorSet();
        flySet.playTogether(segments);
        flySet.start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 2 — SMOOTH GLIDE BACK TO CENTER
    // ══════════════════════════════════════════════════════════════════════
    private void scheduleReturnToCenter() {
        new Handler().postDelayed(() -> {
            ObjectAnimator returnX = ObjectAnimator.ofFloat(
                    birdAnimation, "translationX", 0f);
            ObjectAnimator returnY = ObjectAnimator.ofFloat(
                    birdAnimation, "translationY", 0f);

            returnX.setDuration(RETURN_DURATION);
            returnY.setDuration(RETURN_DURATION);
            returnX.setInterpolator(new DecelerateInterpolator(2.5f));
            returnY.setInterpolator(new DecelerateInterpolator(2.5f));

            AnimatorSet returnSet = new AnimatorSet();
            returnSet.playTogether(returnX, returnY);
            returnSet.start();

        }, FLY_AROUND_DURATION);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 3 — FADE IN APP NAME + BUTTON
    // ══════════════════════════════════════════════════════════════════════
    private void scheduleFadeInUI() {
        new Handler().postDelayed(() -> {

            tvAppName.setTranslationY(30f);
            tvAppName.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            new Handler().postDelayed(() ->
                            btnGetStarted.animate()
                                    .alpha(1f)
                                    .setDuration(600)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .start()
                    , 300);

        }, UI_FADE_IN_DELAY);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET STARTED BUTTON
    // ══════════════════════════════════════════════════════════════════════
   private void setupGetStartedButton() {
    btnGetStarted.setOnClickListener(v -> {
        btnGetStarted.setEnabled(false);
        btnGetStarted.animate()
                .scaleX(0.93f).scaleY(0.93f)
                .setDuration(100)
                .withEndAction(() ->
                        btnGetStarted.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(80)
                                .withEndAction(() -> {
                                    smoothSwoopAndZoom();
                                    // delay navigation until smoothSwoopAndZoom finishes
                                    btnGetStarted.postDelayed(() -> {
                                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }, 500); // adjust 500ms to match your smoothSwoopAndZoom duration
                                })
                                .start()
                ).start();
    });
}
    // ══════════════════════════════════════════════════════════════════════
    //  TRANSITION — SMOOTH SWOOP THEN ZOOM TO FILL SCREEN
    // ══════════════════════════════════════════════════════════════════════
    private void smoothSwoopAndZoom() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;

        ObjectAnimator swoopX = ObjectAnimator.ofFloat(
                birdAnimation, "translationX", 0f, sw * 0.22f, 0f);
        ObjectAnimator swoopY = ObjectAnimator.ofFloat(
                birdAnimation, "translationY", 0f, -sh * 0.18f, 0f);

        swoopX.setDuration(900);
        swoopY.setDuration(900);
        swoopX.setInterpolator(new LinearInterpolator());
        swoopY.setInterpolator(new LinearInterpolator());

        tvAppName.animate().alpha(0f).setDuration(500).start();
        btnGetStarted.animate().alpha(0f).setDuration(500).start();

        AnimatorSet swoopSet = new AnimatorSet();
        swoopSet.playTogether(swoopX, swoopY);
        swoopSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                zoomToFillScreen();
            }
        });
        swoopSet.start();
    }

    private void zoomToFillScreen() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(
                birdAnimation, "scaleX", 1f, 40f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(
                birdAnimation, "scaleY", 1f, 40f);
        ObjectAnimator overlayFade = ObjectAnimator.ofFloat(
                transitionOverlay, "alpha", 0f, 1f);

        scaleX.setDuration(650);
        scaleY.setDuration(650);
        overlayFade.setDuration(650);
        overlayFade.setStartDelay(250);

        scaleX.setInterpolator(new AccelerateInterpolator(1.8f));
        scaleY.setInterpolator(new AccelerateInterpolator(1.8f));

        AnimatorSet zoomSet = new AnimatorSet();
        zoomSet.playTogether(scaleX, scaleY, overlayFade);
        zoomSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navigateToMain();
            }
        });
        zoomSet.start();
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (birdAnimation != null) birdAnimation.setImageDrawable(null);
    }
}
