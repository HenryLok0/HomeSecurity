package ict.mgame.homesecurity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private static SettingsActivity instance; // Static reference for callbacks

    private android.widget.LinearLayout llBluetoothList;
    private android.widget.LinearLayout layoutNoDevice;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnResetPassword;
    
    // Update Interval UI
    private SeekBar seekUpdateInterval;
    private TextView tvUpdateIntervalLabel;
    
    // Alarm Test UI
    private Button btnStartBuzzer;
    private Button btnStopBuzzer;
    // Buzzer Alarm Switch
    private SwitchMaterial switchBuzzerAlarm;
    
    // Theme UI
    private AutoCompleteTextView themeDropdown;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices;

    // Preferences Keys
    private static final String PREFS_NAME = "HomeSecurityPrefs";
    private static final String KEY_BUZZER_ALARM = "buzzer_alarm_enabled";
    private static final String KEY_UPDATE_INTERVAL = "update_interval_ms";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        instance = this; // Set static reference

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Views
        llBluetoothList = findViewById(R.id.llBluetoothList);
        layoutNoDevice = findViewById(R.id.layoutNoDevice);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        // Update Interval Views
        seekUpdateInterval = findViewById(R.id.seekUpdateInterval);
        tvUpdateIntervalLabel = findViewById(R.id.tvUpdateIntervalLabel);
        
        // Alarm Test Views
        btnStartBuzzer = findViewById(R.id.btnStartBuzzer);
        btnStopBuzzer = findViewById(R.id.btnStopBuzzer);

        // Buzzer Alarm Switch
        switchBuzzerAlarm = findViewById(R.id.switchBuzzerAlarm);
        setupBuzzerAlarmSwitch();

        // Theme Dropdown
        themeDropdown = findViewById(R.id.themeDropdown);
        setupThemeDropdown();

        // Setup Update Interval Control
        setupUpdateIntervalControl();
        
        // Alarm Test Listeners
        btnStartBuzzer.setOnClickListener(v -> sendAlarmCommand('a'));
        btnStopBuzzer.setOnClickListener(v -> sendAlarmCommand('x'));

        // Initialize Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevices = new ArrayList<>();

        // Load Bluetooth Devices
        loadConnectedBluetoothDevices();

        // Set Reset Password Button Listener
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void loadConnectedBluetoothDevices() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth to see devices", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            bluetoothDevices.clear();
            llBluetoothList.removeAllViews();

            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    bluetoothDevices.add(device);
                    
                    // Create item view
                    View itemView = getLayoutInflater().inflate(R.layout.item_bluetooth_device, llBluetoothList, false);
                    TextView tvDevice = itemView.findViewById(android.R.id.text1);
                    tvDevice.setText(device.getName() + "\n" + device.getAddress());
                    
                    // Set click listener
                    itemView.setOnClickListener(v -> {
                        android.content.Intent resultIntent = new android.content.Intent();
                        resultIntent.putExtra("device_address", device.getAddress());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                    
                    llBluetoothList.addView(itemView);
                }
                llBluetoothList.setVisibility(View.VISIBLE);
                layoutNoDevice.setVisibility(View.GONE);
            } else {
                llBluetoothList.setVisibility(View.GONE);
                layoutNoDevice.setVisibility(View.VISIBLE);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission missing to scan Bluetooth devices", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadConnectedBluetoothDevices();
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resetPassword() {
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmPassword.getText().toString();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please enter both password fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the new password (mock implementation)
        // In a real app, save to SharedPreferences or Database
        Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_SHORT).show();
        
        // Clear fields
        etNewPassword.setText("");
        etConfirmPassword.setText("");
    }

    private void sendAlarmCommand(char command) {
        // Send command to Arduino via BluetoothManager
        if (BluetoothManager.getInstance().isConnected()) {
            BluetoothManager.getInstance().write(String.valueOf(command));
            String action = (command == 'a') ? "Start" : "Stop";
            Toast.makeText(this, action + " buzzer command sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Not connected to Arduino", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null; // Clear static reference
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupBuzzerAlarmSwitch() {
        // Load saved preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_BUZZER_ALARM, true); // Default ON
        switchBuzzerAlarm.setChecked(isEnabled);

        // Set listener to save preference when toggled
        switchBuzzerAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BUZZER_ALARM, isChecked).apply();
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Buzzer alarm " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupUpdateIntervalControl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Default 300ms (0.3s)
        long savedInterval = prefs.getLong(KEY_UPDATE_INTERVAL, 300);
        
        // SeekBar range: 0 to 50 (representing 0.1s to 5.1s)
        // Value = (progress * 100) + 100 ms
        // 300ms -> progress = 2
        int progress = (int) ((savedInterval - 100) / 100);
        if (progress < 0) progress = 0;
        if (progress > 50) progress = 50;
        
        seekUpdateInterval.setMax(50);
        seekUpdateInterval.setProgress(progress);
        updateIntervalLabel(savedInterval);
        
        // Apply to BluetoothManager immediately if connected
        BluetoothManager.getInstance().setUpdateInterval(savedInterval);

        seekUpdateInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long interval = (progress * 100L) + 100L;
                updateIntervalLabel(interval);
                prefs.edit().putLong(KEY_UPDATE_INTERVAL, interval).apply();
                BluetoothManager.getInstance().setUpdateInterval(interval);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void updateIntervalLabel(long intervalMs) {
        float seconds = intervalMs / 1000f;
        tvUpdateIntervalLabel.setText(String.format("Interval: %.1fs", seconds));
    }

    // Static method to check if buzzer alarm is enabled
    public static boolean isBuzzerAlarmEnabled(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_BUZZER_ALARM, true); // Default ON
    }
    
    // Static method to get update interval
    public static long getUpdateInterval(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(KEY_UPDATE_INTERVAL, 300);
    }

    private void setupThemeDropdown() {
        String[] themeOptions = new String[]{
            "Follow Device Mode",
            "Light Mode",
            "Dark Mode"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            R.layout.item_dropdown,
            themeOptions
        );
        themeDropdown.setAdapter(adapter);

        // Load saved theme preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        String selectedTheme;
        switch (savedTheme) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                selectedTheme = "Light Mode";
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                selectedTheme = "Dark Mode";
                break;
            default:
                selectedTheme = "Follow Device Mode";
                break;
        }
        themeDropdown.setText(selectedTheme, false);

        // Set item click listener
        themeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            int themeMode;
            switch (position) {
                case 1: // Light Mode
                    themeMode = AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case 2: // Dark Mode
                    themeMode = AppCompatDelegate.MODE_NIGHT_YES;
                    break;
                default: // Follow Device Mode
                    themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    break;
            }

            // Get current theme mode to check if it's different
            int currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            
            if (currentTheme != themeMode) {
                // Save preference
                prefs.edit().putInt("theme_mode", themeMode).apply();

                // Apply theme immediately by recreating the activity
                AppCompatDelegate.setDefaultNightMode(themeMode);
                
                // Recreate activity to apply theme
                recreate();
            }
        });
    }
}
