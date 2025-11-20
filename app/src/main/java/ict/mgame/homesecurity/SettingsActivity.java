package ict.mgame.homesecurity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import ict.mgame.homesecurity.R;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar seekSensitivity;
    private RadioGroup rgCameraMode;
    private SwitchCompat switchNotifications;
    private EditText etWifiSSID;
    private EditText etWifiPassword;
    private CheckBox cbStorage;
    private Button btnSaveSettings;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HomeSecurityPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Views
        seekSensitivity = findViewById(R.id.seekSensitivity);
        rgCameraMode = findViewById(R.id.rgCameraMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        etWifiSSID = findViewById(R.id.etWifiSSID);
        etWifiPassword = findViewById(R.id.etWifiPassword);
        cbStorage = findViewById(R.id.cbStorage);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved settings
        loadSettings();

        // Set Save Button Listener
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void loadSettings() {
        // Load Sensitivity
        int sensitivity = sharedPreferences.getInt("sensitivity", 50);
        seekSensitivity.setProgress(sensitivity);

        // Load Camera Mode
        int cameraModeId = sharedPreferences.getInt("camera_mode", R.id.rbContinuous);
        rgCameraMode.check(cameraModeId);

        // Load Notifications
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        // Load Wi-Fi Settings
        String ssid = sharedPreferences.getString("wifi_ssid", "");
        String password = sharedPreferences.getString("wifi_password", "");
        etWifiSSID.setText(ssid);
        etWifiPassword.setText(password);

        // Load Storage Option
        boolean storageEnabled = sharedPreferences.getBoolean("local_storage_enabled", false);
        cbStorage.setChecked(storageEnabled);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save Sensitivity
        editor.putInt("sensitivity", seekSensitivity.getProgress());

        // Save Camera Mode
        editor.putInt("camera_mode", rgCameraMode.getCheckedRadioButtonId());

        // Save Notifications
        editor.putBoolean("notifications_enabled", switchNotifications.isChecked());

        // Save Wi-Fi Settings
        editor.putString("wifi_ssid", etWifiSSID.getText().toString());
        editor.putString("wifi_password", etWifiPassword.getText().toString());

        // Save Storage Option
        editor.putBoolean("local_storage_enabled", cbStorage.isChecked());

        // Commit changes
        editor.apply();

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }
}
