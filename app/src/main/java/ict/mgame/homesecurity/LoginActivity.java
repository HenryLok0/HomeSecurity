package ict.mgame.homesecurity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnAction;
    private TextView tvSwitchMode;
    private TextView tvCardTitle;
    private TextView tvPasswordStrength;
    private TextView tvPasswordRequirements;
    private ProgressBar pbPasswordStrength;
    private LinearLayout passwordStrengthContainer;
    private MaterialCardView cardLogin;
    private View ivLogo;

    private boolean isLoginMode = true;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnAction = findViewById(R.id.btnAction);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        tvCardTitle = findViewById(R.id.tvCardTitle);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        tvPasswordRequirements = findViewById(R.id.tvPasswordRequirements);
        pbPasswordStrength = findViewById(R.id.pbPasswordStrength);
        passwordStrengthContainer = findViewById(R.id.passwordStrengthContainer);
        cardLogin = findViewById(R.id.cardLogin);
        ivLogo = findViewById(R.id.ivLogo);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check if user is already logged in
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            navigateToMain();
            return;
        }

        applyEntranceAnimations();
        updateUI();
        setupPasswordStrengthChecker();

        btnAction.setOnClickListener(v -> {
            animateButtonClick(v);
            handleAction();
        });

        tvSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            animateTransition();
            updateUI();
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            tvCardTitle.setText("Login");
            btnAction.setText("Login");
            btnAction.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_lock_lock));
            tvSwitchMode.setText("Don't have an account? Create one");
            passwordStrengthContainer.setVisibility(View.GONE);
        } else {
            tvCardTitle.setText("Create Account");
            btnAction.setText("Create Account");
            btnAction.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_add));
            tvSwitchMode.setText("Already have an account? Login");
        }
        etUsername.setError(null);
        etPassword.setError(null);
        if (!isLoginMode) {
            etUsername.setText("");
            etPassword.setText("");
        }
    }

    private void applyEntranceAnimations() {
        // Animate logo with scale
        Animation scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        ivLogo.startAnimation(scaleAnim);

        // Animate card with slide up
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
        slideUp.setStartOffset(300);
        cardLogin.startAnimation(slideUp);
    }

    private void setupPasswordStrengthChecker() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isLoginMode) {
                    checkPasswordStrength(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkPasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthContainer.setVisibility(View.GONE);
            return;
        }

        passwordStrengthContainer.setVisibility(View.VISIBLE);

        int strength = 0;
        boolean hasMinLength = password.length() >= 8;
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (hasMinLength) strength += 20;
        if (hasUpperCase) strength += 20;
        if (hasLowerCase) strength += 20;
        if (hasNumber) strength += 20;
        if (hasSpecialChar) strength += 20;

        // Animate progress
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(pbPasswordStrength, "progress", pbPasswordStrength.getProgress(), strength);
        progressAnimator.setDuration(300);
        progressAnimator.start();

        // Update strength text and color
        String strengthText;
        int strengthColor;

        if (strength < 40) {
            strengthText = "Weak";
            strengthColor = ContextCompat.getColor(this, R.color.password_weak);
        } else if (strength < 60) {
            strengthText = "Fair";
            strengthColor = ContextCompat.getColor(this, R.color.password_medium);
        } else if (strength < 80) {
            strengthText = "Good";
            strengthColor = ContextCompat.getColor(this, R.color.password_strong);
        } else {
            strengthText = "Strong";
            strengthColor = ContextCompat.getColor(this, R.color.password_very_strong);
        }

        tvPasswordStrength.setText(strengthText);
        tvPasswordStrength.setTextColor(strengthColor);
        pbPasswordStrength.setProgressTintList(android.content.res.ColorStateList.valueOf(strengthColor));

        // Update requirements text with checkmarks
        StringBuilder requirements = new StringBuilder();
        requirements.append(hasMinLength ? "✓ " : "• ");
        requirements.append("At least 8 characters\n");
        requirements.append(hasUpperCase && hasLowerCase ? "✓ " : "• ");
        requirements.append("Include uppercase & lowercase\n");
        requirements.append(hasNumber ? "✓ " : "• ");
        requirements.append("Include numbers\n");
        requirements.append(hasSpecialChar ? "✓ " : "• ");
        requirements.append("Include special characters");

        tvPasswordRequirements.setText(requirements.toString());
    }

    private void animateButtonClick(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        scaleDownX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f);
                scaleUpX.setDuration(100);
                scaleUpY.setDuration(100);
                scaleUpX.start();
                scaleUpY.start();
            }
        });

        scaleDownX.start();
        scaleDownY.start();
    }

    private void animateTransition() {
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        tvCardTitle.startAnimation(fadeOut);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                tvCardTitle.startAnimation(fadeIn);
                btnAction.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void handleAction() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (isLoginMode) {
            login(username, password);
        } else {
            createAccount(username, password);
        }
    }

    private void login(String username, String password) {
        String storedPassword = sharedPreferences.getString(username, null);

        if (storedPassword != null && storedPassword.equals(password)) {
            Toast.makeText(this, "✓ Login Successful", Toast.LENGTH_SHORT).show();
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.apply();
            
            navigateToMain();
        } else {
            Toast.makeText(this, "✗ Invalid Username or Password", Toast.LENGTH_SHORT).show();
            // Shake animation for error
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            cardLogin.startAnimation(shake);
        }
    }

    private void createAccount(String username, String password) {
        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            Toast.makeText(this, "Please use a stronger password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sharedPreferences.contains(username)) {
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(username, password);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();

        Toast.makeText(this, "✓ Account Created Successfully", Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
