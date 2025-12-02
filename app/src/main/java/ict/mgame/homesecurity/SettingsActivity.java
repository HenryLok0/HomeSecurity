package ict.mgame.homesecurity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Button;
import java.util.Locale;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> deviceList;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private ArrayAdapter<String> deviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        instance = this; // Set static reference

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
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
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
}
