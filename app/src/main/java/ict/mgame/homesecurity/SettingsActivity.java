package ict.mgame.homesecurity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Button;
import java.util.Locale;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private static SettingsActivity instance; // Static reference for callbacks

    private ListView lvBluetoothDevices;
    private android.widget.LinearLayout layoutNoDevice;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnResetPassword;
    // DHT11 UI
    private TextView tvTemperature;
    private TextView tvHumidity;
    private Button btnRefreshDht;
    // Alarm Test UI
    private Button btnStartBuzzer;
    private Button btnStopBuzzer;
    // Buzzer Alarm Switch
    private SwitchMaterial switchBuzzerAlarm;
    // Background Service UI
    private MaterialButton btnBackgroundService;
    private TextView tvBackgroundStatus;
    // Theme UI
    private AutoCompleteTextView themeDropdown;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private ArrayAdapter<String> deviceAdapter;

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
        lvBluetoothDevices = findViewById(R.id.lvBluetoothDevices);
        layoutNoDevice = findViewById(R.id.layoutNoDevice);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        // DHT11 Views
        tvTemperature = findViewById(R.id.tvTemperature);
        tvHumidity = findViewById(R.id.tvHumidity);
        btnRefreshDht = findViewById(R.id.btnRefreshDht);
        
        // Alarm Test Views
        btnStartBuzzer = findViewById(R.id.btnStartBuzzer);
        btnStopBuzzer = findViewById(R.id.btnStopBuzzer);

        // Buzzer Alarm Switch
        switchBuzzerAlarm = findViewById(R.id.switchBuzzerAlarm);
        setupBuzzerAlarmSwitch();

        // Background Service Views
        btnBackgroundService = findViewById(R.id.btnBackgroundService);
        tvBackgroundStatus = findViewById(R.id.tvBackgroundStatus);
        setupBackgroundService();

        // Theme Dropdown
        themeDropdown = findViewById(R.id.themeDropdown);
        setupThemeDropdown();

        // Load saved DHT values
        loadDhtValuesToUI();

        btnRefreshDht.setOnClickListener(v -> requestDhtData());
        
        // Alarm Test Listeners
        btnStartBuzzer.setOnClickListener(v -> sendAlarmCommand('a'));
        btnStopBuzzer.setOnClickListener(v -> sendAlarmCommand('x'));

        // Initialize Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();
        bluetoothDevices = new ArrayList<>();
        deviceAdapter = new ArrayAdapter<>(this, R.layout.item_bluetooth_device, android.R.id.text1, deviceList);
        lvBluetoothDevices.setAdapter(deviceAdapter);

        // Load Bluetooth Devices
        loadConnectedBluetoothDevices();

        // Set List Item Click Listener
        lvBluetoothDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = bluetoothDevices.get(position);
            android.content.Intent resultIntent = new android.content.Intent();
            resultIntent.putExtra("device_address", device.getAddress());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // Set Reset Password Button Listener
        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });
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

        try {
            // Note: In a real app, you need to handle runtime permissions for BLUETOOTH_CONNECT
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            deviceList.clear();
            bluetoothDevices.clear();

            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    // Showing Name and Address
                    deviceList.add(device.getName() + "\n" + device.getAddress());
                    bluetoothDevices.add(device);
                }
                lvBluetoothDevices.setVisibility(View.VISIBLE);
                layoutNoDevice.setVisibility(View.GONE);
            } else {
                lvBluetoothDevices.setVisibility(View.GONE);
                layoutNoDevice.setVisibility(View.VISIBLE);
            }
            deviceAdapter.notifyDataSetChanged();
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission missing to scan Bluetooth devices", Toast.LENGTH_SHORT).show();
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

    // DHT11 persistence keys
    private static final String PREFS_NAME = "HomeSecurityPrefs";
    private static final String KEY_DHT_TEMP = "dht_temp";
    private static final String KEY_DHT_HUM = "dht_humidity";
    private static final String KEY_BUZZER_ALARM = "buzzer_alarm_enabled";

    private void loadDhtValuesToUI() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float temp = prefs.getFloat(KEY_DHT_TEMP, Float.NaN);
        float hum = prefs.getFloat(KEY_DHT_HUM, Float.NaN);
        if (!Float.isNaN(temp)) {
            tvTemperature.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
        } else {
            tvTemperature.setText("-- °C");
        }
        if (!Float.isNaN(hum)) {
            tvHumidity.setText(String.format(Locale.getDefault(), "%.0f %%", hum));
        } else {
            tvHumidity.setText("-- %");
        }
    }

    public static void saveDhtValues(Context ctx, float temp, float humidity) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putFloat(KEY_DHT_TEMP, temp).putFloat(KEY_DHT_HUM, humidity).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    // Optional: call this from activity instance when live update arrives
    public void onDhtUpdate(float temp, float humidity) {
        saveDhtValues(this, temp, humidity);
        runOnUiThread(() -> {
            tvTemperature.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
            tvHumidity.setText(String.format(Locale.getDefault(), "%.0f %%", humidity));
        });
    }
    
    private void sendAlarmCommand(char command) {
        // Send command to MainActivity via static method
        boolean sent = MainActivity.sendBluetoothCommand(command);
        if (sent) {
            String action = (command == 'a') ? "Start" : "Stop";
            Toast.makeText(this, action + " buzzer command sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Not connected to Arduino", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void requestDhtData() {
        // Request DHT11 data from Arduino
        boolean sent = MainActivity.sendBluetoothCommand('t');
        if (sent) {
            Toast.makeText(this, "Requesting temperature & humidity data...", Toast.LENGTH_SHORT).show();
            // Data will be updated automatically when Arduino responds
            // The response is parsed in MainActivity's ConnectedThread and saved via saveDhtValues()
            // UI will be updated after a short delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                loadDhtValuesToUI();
            }, 1000); // Wait 1 second for Arduino to respond
        } else {
            Toast.makeText(this, "Not connected to Arduino. Showing last saved data.", Toast.LENGTH_LONG).show();
            loadDhtValuesToUI();
        }
    }
    
    // Static method to update UI from MainActivity when DHT data is received
    public static void notifyDhtDataReceived(float temp, float humidity) {
        if (instance != null) {
            instance.runOnUiThread(() -> {
                instance.tvTemperature.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
                instance.tvHumidity.setText(String.format(Locale.getDefault(), "%.0f %%", humidity));
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null; // Clear static reference
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBackgroundServiceUI();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupBackgroundService() {
        updateBackgroundServiceUI();
        
        btnBackgroundService.setOnClickListener(v -> {
            if (BackgroundDetectionService.isRunning()) {
                // Stop service
                Intent serviceIntent = new Intent(this, BackgroundDetectionService.class);
                stopService(serviceIntent);
                Toast.makeText(this, "Background detection stopped", Toast.LENGTH_SHORT).show();
            } else {
                // Start service
                Intent serviceIntent = new Intent(this, BackgroundDetectionService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(this, "Background detection started", Toast.LENGTH_SHORT).show();
            }
            
            // Update UI after a short delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                updateBackgroundServiceUI();
            }, 500);
        });
    }

    private void updateBackgroundServiceUI() {
        boolean isRunning = BackgroundDetectionService.isRunning();
        
        if (isRunning) {
            btnBackgroundService.setText("Stop Background Detection");
            btnBackgroundService.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause));
            tvBackgroundStatus.setText("Status: Running");
            tvBackgroundStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            btnBackgroundService.setText("Start Background Detection");
            btnBackgroundService.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_media_play));
            tvBackgroundStatus.setText("Status: Stopped");
            tvBackgroundStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
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

    // Static method to check if buzzer alarm is enabled
    public static boolean isBuzzerAlarmEnabled(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_BUZZER_ALARM, true); // Default ON
    }

    private void setupThemeDropdown() {
        String[] themeOptions = new String[]{
            "Follow Device Mode",
            "Light Mode",
            "Dark Mode"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
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
