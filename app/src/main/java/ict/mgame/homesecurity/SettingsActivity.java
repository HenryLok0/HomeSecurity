package ict.mgame.homesecurity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

import ict.mgame.homesecurity.R;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar seekSensitivity;
    private RadioGroup rgCameraMode;
    private SwitchCompat switchNotifications;
    private EditText etWifiSSID;
    private EditText etWifiPassword;
    private CheckBox cbStorage;
    private Button btnSaveSettings;
    private Button btnConnectBluetooth;
    private TextView tvConnectedDevice;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HomeSecurityPrefs";
    private BluetoothAdapter bluetoothAdapter;

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
        btnConnectBluetooth = findViewById(R.id.btnConnectBluetooth);
        tvConnectedDevice = findViewById(R.id.tvConnectedDevice);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Load saved settings
        loadSettings();

        // Set Save Button Listener
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        // Set Bluetooth Connect Button Listener
        btnConnectBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPairedDevices();
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

        // Load Bluetooth Device
        String deviceName = sharedPreferences.getString("bluetooth_device_name", "None");
        tvConnectedDevice.setText("Connected: " + deviceName);
    }

    private void showPairedDevices() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions if not granted (simplified for this example)
            // In a real app, you should request permissions properly
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                 ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
                 return;
             }
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> deviceList = new ArrayList<>();
        final ArrayList<String> deviceAddresses = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(device.getName() + "\n" + device.getAddress());
                deviceAddresses.add(device.getAddress());
            }

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice, deviceList);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Bluetooth Device");
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String selectedDeviceName = deviceList.get(which).split("\n")[0];
                    String selectedDeviceAddress = deviceAddresses.get(which);
                    
                    // Save selected device
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("bluetooth_device_name", selectedDeviceName);
                    editor.putString("bluetooth_device_address", selectedDeviceAddress);
                    editor.apply();

                    tvConnectedDevice.setText("Connected: " + selectedDeviceName);
                    Toast.makeText(SettingsActivity.this, "Connected to " + selectedDeviceName, Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        } else {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
        }
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
