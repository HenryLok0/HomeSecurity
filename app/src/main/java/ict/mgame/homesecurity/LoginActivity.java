package ict.mgame.homesecurity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnAction;
    private TextView tvSwitchMode;
    private TextView tvCardTitle;

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

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check if user is already logged in
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            navigateToMain();
            return;
        }

        updateUI();

        btnAction.setOnClickListener(v -> handleAction());

        tvSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            tvCardTitle.setText("Login");
            btnAction.setText("Login");
            tvSwitchMode.setText("Don't have an account? Create one");
        } else {
            tvCardTitle.setText("Create Account");
            btnAction.setText("Create Account");
            tvSwitchMode.setText("Already have an account? Login");
        }
        etUsername.setError(null);
        etPassword.setError(null);
    }

    private void handleAction() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
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
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.apply();
            
            navigateToMain();
        } else {
            Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show();
        }
    }

    private void createAccount(String username, String password) {
        if (sharedPreferences.contains(username)) {
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(username, password);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();

        Toast.makeText(this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
        // Automatically login or switch to login mode?
        // Let's switch to login mode for clarity, or just log them in.
        // User request: "can login can create account"
        // Let's log them in directly for better UX.
        navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Prevent going back to login screen with back button
    }
}
