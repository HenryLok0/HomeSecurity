package ict.mgame.homesecurity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private int currentPage = 0;
    private TextView tvTitle, tvDescription;
    private ImageView ivOnboarding;
    private MaterialButton btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        ivOnboarding = findViewById(R.id.ivOnboarding);
        btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            if (currentPage == 0) {
                // Page 2: Permissions
                currentPage = 1;
                tvTitle.setText("Permissions Needed");
                tvDescription.setText("We need Camera access for monitoring and Bluetooth to connect with your security sensors.");
                ivOnboarding.setImageResource(android.R.drawable.ic_menu_camera);
                btnNext.setText("Next");
            } else if (currentPage == 1) {
                // Page 3: Ready
                currentPage = 2;
                tvTitle.setText("You're All Set!");
                tvDescription.setText("Start monitoring your home now.");
                ivOnboarding.setImageResource(android.R.drawable.ic_lock_idle_lock);
                btnNext.setText("Finish");
            } else {
                // Finish
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean("onboarding_completed", true).apply();
                
                startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}
