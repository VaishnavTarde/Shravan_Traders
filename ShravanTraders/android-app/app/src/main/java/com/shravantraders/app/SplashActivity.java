package com.shravantraders.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private final Handler navHandler = new Handler(Looper.getMainLooper());
    private Runnable navRunnable;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocaleAndTheme(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#FFF9F2")); // Warm cream background matching artwork

        ImageView splashImage = new ImageView(this);
        splashImage.setImageResource(R.drawable.splash_shravan_traders);
        splashImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        splashImage.setAdjustViewBounds(true);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        splashImage.setLayoutParams(lp);

        root.addView(splashImage);

        // Handle window insets properly for status & navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setContentView(root);

        // Navigate automatically after exactly 2000ms (2 seconds)
        navRunnable = () -> {
            if (!isFinishing() && !isDestroyed()) {
                Intent intent;
                if (!Api.token(this).isEmpty()) {
                    intent = new Intent(SplashActivity.this, DashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            }
        };

        navHandler.postDelayed(navRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navHandler != null && navRunnable != null) {
            navHandler.removeCallbacks(navRunnable);
        }
    }
}
