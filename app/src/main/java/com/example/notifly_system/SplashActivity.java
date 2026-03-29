package com.example.notifly_system; // 🔁 Change this to your actual package name

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.LinearGradient;
import android.graphics.Shader;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notifly_system.MainActivity;

public class SplashActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private FrameLayout birdBellContainer;
    private ImageView imgBird;
    private ImageView imgBell;
    private TextView tvAppName;
    private Button btnGetStarted;

    // ── Animation state ────────────────────────────────────────────────────
    private ValueAnimator wingFlapAnimator;   // idle wing sway
    private ValueAnimator bellRingAnimator;   // bell swing
    private boolean animationsStarted = false;

    // ── Timing constants (ms) ──────────────────────────────────────────────
    private static final long FLY_AROUND_DURATION  = 3800;  // fly-around phase
    private static final long RETURN_DURATION       = 700;   // glide back to center
    private static final long UI_FADE_IN_DELAY      = 4800;  // when text/button appear
    private static final long BELL_RING_START_DELAY = 4600;  // first bell ring

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        // Hide system UI for immersive splash
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

    private void applyGradientToTitle() {
        tvAppName.post(() -> {
            float width = tvAppName.getPaint().measureText(tvAppName.getText().toString());

            int[] colors = {
                    0xFF38D6FF,   // cyan-blue (left)
                    0xFF7B5FFF,   // purple (mid)
                    0xFFBB44FF    // violet (right)
            };
            float[] positions = { 0f, 0.5f, 1f };

            LinearGradient gradient = new LinearGradient(
                    0, 0, width, 0,
                    colors, positions,
                    Shader.TileMode.CLAMP
            );

            tvAppName.getPaint().setShader(gradient);
            tvAppName.invalidate();
        });
    }

    // ── View binding ───────────────────────────────────────────────────────
    private void bindViews() {
        birdBellContainer = findViewById(R.id.birdBellContainer);
        imgBird           = findViewById(R.id.imgBird);
        imgBell           = findViewById(R.id.imgBell);
        tvAppName         = findViewById(R.id.tvAppName);
        btnGetStarted     = findViewById(R.id.btnGetStarted);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MASTER SEQUENCE
    // ══════════════════════════════════════════════════════════════════════
    private void startSplashSequence() {
        // Small entry delay so layout is measured first
        new Handler().postDelayed(() -> {
            flyAroundScreen();                          // Phase 1 – fly around
            scheduleReturnToCenter();                   // Phase 2 – glide back
            scheduleIdleAnimations();                   // Phase 3 – idle flap + bell ring
            scheduleFadeInUI();                         // Phase 4 – reveal UI
        }, 300);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 1 — FLY AROUND THE SCREEN
    //  Bird traces a looping figure-8 path across the screen
    // ══════════════════════════════════════════════════════════════════════
    private void flyAroundScreen() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;

        // Keyframe waypoints  (x offset from center, y offset from center)
        float[][] waypoints = {
                {  sw * 0.35f, -sh * 0.30f },   // top-right
                {  sw * 0.38f,  sh * 0.20f },   // right-center
                {  sw * 0.10f,  sh * 0.38f },   // bottom-right
                { -sw * 0.38f,  sh * 0.25f },   // bottom-left
                { -sw * 0.35f, -sh * 0.28f },   // top-left
                {  0f,         -sh * 0.10f },   // top-center
        };

        long segmentDuration = FLY_AROUND_DURATION / waypoints.length;

        AnimatorSet flySet = new AnimatorSet();
        Animator[] segments = new Animator[waypoints.length * 2];

        float startX = birdBellContainer.getTranslationX();
        float startY = birdBellContainer.getTranslationY();

        for (int i = 0; i < waypoints.length; i++) {
            float fromX = (i == 0) ? startX : waypoints[i - 1][0];
            float fromY = (i == 0) ? startY : waypoints[i - 1][1];
            float toX   = waypoints[i][0];
            float toY   = waypoints[i][1];

            ObjectAnimator xAnim = ObjectAnimator.ofFloat(birdBellContainer, "translationX", fromX, toX);
            ObjectAnimator yAnim = ObjectAnimator.ofFloat(birdBellContainer, "translationY", fromY, toY);
            xAnim.setDuration(segmentDuration);
            yAnim.setDuration(segmentDuration);
            xAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            yAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            xAnim.setStartDelay(i * segmentDuration);
            yAnim.setStartDelay(i * segmentDuration);

            segments[i * 2]     = xAnim;
            segments[i * 2 + 1] = yAnim;
        }

        // Subtle scale pulse while flying (looks like wing power)
        ValueAnimator scalePulse = ValueAnimator.ofFloat(1f, 1.08f, 0.95f, 1f);
        scalePulse.setDuration(FLY_AROUND_DURATION);
        scalePulse.setRepeatCount(0);
        scalePulse.addUpdateListener(anim -> {
            float v = (float) anim.getAnimatedValue();
            birdBellContainer.setScaleX(v);
            birdBellContainer.setScaleY(v);
        });

        // Wing rapid flap while flying
        ValueAnimator flyFlap = ValueAnimator.ofFloat(-18f, 18f);
        flyFlap.setDuration(160);
        flyFlap.setRepeatCount(ValueAnimator.INFINITE);
        flyFlap.setRepeatMode(ValueAnimator.REVERSE);
        flyFlap.addUpdateListener(anim -> {
            imgBird.setRotation((float) anim.getAnimatedValue());
        });
        flyFlap.start();

        // Stop rapid flap when fly-around ends
        new Handler().postDelayed(flyFlap::cancel, FLY_AROUND_DURATION);

        flySet.playTogether(segments);
        flySet.playTogether(scalePulse);
        flySet.start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 2 — GLIDE BACK TO ORIGINAL POSITION
    // ══════════════════════════════════════════════════════════════════════
    private void scheduleReturnToCenter() {
        new Handler().postDelayed(() -> {
            ObjectAnimator returnX = ObjectAnimator.ofFloat(birdBellContainer, "translationX", 0f);
            ObjectAnimator returnY = ObjectAnimator.ofFloat(birdBellContainer, "translationY", 0f);
            ObjectAnimator resetScale = ObjectAnimator.ofFloat(birdBellContainer, "scaleX", 1f);
            ObjectAnimator resetScaleY = ObjectAnimator.ofFloat(birdBellContainer, "scaleY", 1f);

            returnX.setDuration(RETURN_DURATION);
            returnY.setDuration(RETURN_DURATION);
            resetScale.setDuration(RETURN_DURATION);
            resetScaleY.setDuration(RETURN_DURATION);

            returnX.setInterpolator(new DecelerateInterpolator(1.8f));
            returnY.setInterpolator(new DecelerateInterpolator(1.8f));

            AnimatorSet returnSet = new AnimatorSet();
            returnSet.playTogether(returnX, returnY, resetScale, resetScaleY);
            returnSet.start();

            // Reset bird rotation
            imgBird.animate().rotation(0f).setDuration(RETURN_DURATION).start();

        }, FLY_AROUND_DURATION);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 3A — IDLE WING SWAY (continuous after landing)
    // ══════════════════════════════════════════════════════════════════════
    private void scheduleIdleAnimations() {
        new Handler().postDelayed(() -> {
            startIdleWingFlap();
            startBellRing();
        }, FLY_AROUND_DURATION + RETURN_DURATION + 200);
    }

    private void startIdleWingFlap() {
        wingFlapAnimator = ValueAnimator.ofFloat(-10f, 10f);
        wingFlapAnimator.setDuration(500);
        wingFlapAnimator.setRepeatCount(ValueAnimator.INFINITE);
        wingFlapAnimator.setRepeatMode(ValueAnimator.REVERSE);
        wingFlapAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        wingFlapAnimator.addUpdateListener(anim -> {
            imgBird.setRotation((float) anim.getAnimatedValue());
        });
        wingFlapAnimator.start();
        animationsStarted = true;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 3B — BELL RINGING (triggered then repeats every 3s)
    // ══════════════════════════════════════════════════════════════════════
    private void startBellRing() {
        Runnable ringRunnable = new Runnable() {
            @Override
            public void run() {
                ringBell();
                new Handler().postDelayed(this, 3000); // ring every 3s
            }
        };
        new Handler().postDelayed(ringRunnable, 200);
    }

    private void ringBell() {
        // Bell swings left-right like a real bell — pivot at top
        imgBell.setPivotX(imgBell.getWidth() / 2f);
        imgBell.setPivotY(0f);

        bellRingAnimator = ValueAnimator.ofFloat(0f, -22f, 22f, -15f, 15f, -8f, 8f, -3f, 3f, 0f);
        bellRingAnimator.setDuration(900);
        bellRingAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bellRingAnimator.addUpdateListener(anim -> {
            imgBell.setRotation((float) anim.getAnimatedValue());
        });
        bellRingAnimator.start();

        // Slight vertical bounce of bird when bell rings (bird reacts)
        ObjectAnimator birdBounce = ObjectAnimator.ofFloat(imgBird, "translationY",
                0f, -6f, 0f, -3f, 0f);
        birdBounce.setDuration(700);
        birdBounce.setInterpolator(new AccelerateDecelerateInterpolator());
        birdBounce.start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHASE 4 — FADE IN APP NAME + BUTTON
    // ══════════════════════════════════════════════════════════════════════
    private void scheduleFadeInUI() {
        new Handler().postDelayed(() -> {
            // App name fade + slide up
            tvAppName.setTranslationY(30f);
            tvAppName.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            // Button fade slightly later
            new Handler().postDelayed(() -> {
                btnGetStarted.animate()
                        .alpha(1f)
                        .setDuration(600)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }, 300);

        }, UI_FADE_IN_DELAY);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET STARTED — Bird + Bell fly off screen together
    // ══════════════════════════════════════════════════════════════════════
    private void setupGetStartedButton() {
        btnGetStarted.setOnClickListener(v -> {
            btnGetStarted.setEnabled(false); // prevent double-tap

            // Stop idle animations
            if (wingFlapAnimator != null) wingFlapAnimator.cancel();
            if (bellRingAnimator  != null) bellRingAnimator.cancel();

            // 1. Button press feedback — scale down/up
            btnGetStarted.animate()
                    .scaleX(0.93f).scaleY(0.93f)
                    .setDuration(100)
                    .withEndAction(() ->
                            btnGetStarted.animate()
                                    .scaleX(1f).scaleY(1f)
                                    .setDuration(80)
                                    .start()
                    ).start();

            // 2. Bird flaps faster before takeoff
            ValueAnimator takeoffFlap = ValueAnimator.ofFloat(-25f, 25f);
            takeoffFlap.setDuration(100);
            takeoffFlap.setRepeatCount(5);
            takeoffFlap.setRepeatMode(ValueAnimator.REVERSE);
            takeoffFlap.addUpdateListener(anim -> imgBird.setRotation((float) anim.getAnimatedValue()));

            // 3. Bell swings once at liftoff
            takeoffFlap.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ringBell();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    flyOffScreen();     // then fly away
                    fadeOutUI();        // and fade UI
                }
            });

            new Handler().postDelayed(takeoffFlap::start, 180);
        });
    }

    /**
     * Bird + Bell container flies upward and off-screen.
     */
    private void flyOffScreen() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // Scale up slightly as it accelerates (feels powerful)
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(birdBellContainer, "scaleX", 1f, 1.3f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(birdBellContainer, "scaleY", 1f, 1.3f);

        // Fly straight up off screen
        ObjectAnimator flyUp = ObjectAnimator.ofFloat(
                birdBellContainer, "translationY", 0f, -(screenHeight * 1.2f));

        // Subtle horizontal drift (feels alive)
        ObjectAnimator drift = ObjectAnimator.ofFloat(
                birdBellContainer, "translationX", 0f, 40f);

        flyUp.setDuration(900);
        drift.setDuration(900);
        scaleUp.setDuration(900);
        scaleUpY.setDuration(900);

        flyUp.setInterpolator(new AccelerateInterpolator(1.6f));
        drift.setInterpolator(new AccelerateDecelerateInterpolator());

        // Rapid wing flap during exit
        ValueAnimator exitFlap = ValueAnimator.ofFloat(-30f, 30f);
        exitFlap.setDuration(80);
        exitFlap.setRepeatCount(ValueAnimator.INFINITE);
        exitFlap.setRepeatMode(ValueAnimator.REVERSE);
        exitFlap.addUpdateListener(anim -> imgBird.setRotation((float) anim.getAnimatedValue()));
        exitFlap.start();

        AnimatorSet flySet = new AnimatorSet();
        flySet.playTogether(flyUp, drift, scaleUp, scaleUpY);
        flySet.start();
        flySet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                exitFlap.cancel();
                navigateToMain();
            }
        });
    }

    /**
     * Fade out text and button when bird takes off.
     */
    private void fadeOutUI() {
        tvAppName.animate().alpha(0f).setDuration(500).start();
        btnGetStarted.animate().alpha(0f).setDuration(500).start();
    }

    /**
     * Navigate to your main/home activity after the splash.
     * 🔁 Replace MainActivity.class with your actual next activity.
     */
    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ── Lifecycle cleanup ──────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wingFlapAnimator != null) wingFlapAnimator.cancel();
        if (bellRingAnimator  != null) bellRingAnimator.cancel();
    }
}