package ict.mgame.homesecurity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HomeSecurityApp";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
    }

    private Button btnSettings;
    private Button btnTakePhoto;
    private Button btnRecordVideo;
    private Button btnHistory;
    private Button btnNotifications;
    private Button btnLogout;
    private MaterialButton btnMotionSensor;
    private TextView tvStatus;
    private TextView tvNotification;
    private PreviewView viewFinder;
    private ImageView remoteCameraView;

    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;

    // Motion detection variables
    private boolean isMotionSensorEnabled = false;
    private long lastMotionNotificationTime = 0;
    private static final long MOTION_NOTIFICATION_COOLDOWN_MS = 3000; // 3 seconds cooldown
    private byte[] previousFrame;
    private static final double MOTION_THRESHOLD = 15.0; // Adjusted for slow motion detection
    // Alerts storage
    private static final String PREFS_NAME = "HomeSecurityPrefs";
    private static final String ALERTS_KEY = "motion_alerts"; // stored as JSON array string
    private static final int MAX_STORED_ALERTS = 200;

    // Bluetooth variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private boolean isRemoteCameraActive = false;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SPP UUID

    private final androidx.activity.result.ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("device_address");
                    if (address != null) {
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                        connectToDevice(device);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        btnSettings = findViewById(R.id.btnSettings);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        btnHistory = findViewById(R.id.btnHistory);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnLogout = findViewById(R.id.btnLogout);
        btnMotionSensor = findViewById(R.id.btnMotionSensor);
        tvStatus = findViewById(R.id.tvStatus);
        tvNotification = findViewById(R.id.tvNotification);
        viewFinder = findViewById(R.id.viewFinder);
        remoteCameraView = findViewById(R.id.remoteCameraView);

        tvStatus.setText("System Status: Online");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Set Listeners
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });

        btnTakePhoto.setOnClickListener(v -> takePhoto());

        btnRecordVideo.setOnClickListener(v -> captureVideo());

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> showNotificationHistory());

        btnLogout.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Motion Sensor Button Listener
        btnMotionSensor.setOnClickListener(v -> toggleMotionSensor());
        updateMotionSensorButton();

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HomeSecurity-Image");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                }
        );
    }

    private void captureVideo() {
        if (videoCapture == null) return;

        btnRecordVideo.setEnabled(false);

        Recording curRecording = recording;
        if (curRecording != null) {
            // Stop recording
            curRecording.stop();
            recording = null;
            return;
        }

        // Start recording
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HomeSecurity-Video");
        }

        MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions.Builder(
                getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        .setContentValues(contentValues)
        .build();

        recording = videoCapture.getOutput()
                .prepareRecording(this, mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), recordEvent -> {
                    if (recordEvent instanceof VideoRecordEvent.Start) {
                        btnRecordVideo.setText("Stop");
                        btnRecordVideo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000)); // Red
                        btnRecordVideo.setEnabled(true);
                    } else if (recordEvent instanceof VideoRecordEvent.Finalize) {
                        if (!((VideoRecordEvent.Finalize) recordEvent).hasError()) {
                            String msg = "Video capture succeeded: " + ((VideoRecordEvent.Finalize) recordEvent).getOutputResults().getOutputUri();
                            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, msg);
                        } else {
                            recording.close();
                            recording = null;
                            Log.e(TAG, "Video capture ends with error: " + ((VideoRecordEvent.Finalize) recordEvent).getError());
                        }
                        btnRecordVideo.setText("Record");
                        btnRecordVideo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F)); // Default Red
                        btnRecordVideo.setEnabled(true);
                    }
                });
    }

    private void startCamera() {
        if (isRemoteCameraActive) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                
                // Motion Detection Image Analysis
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        if (isMotionSensorEnabled) {
                            analyzeFrameForMotion(image);
                        } else {
                            previousFrame = null;
                        }
                        image.close();
                    }
                });

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture, videoCapture, imageAnalysis);
                } catch (Exception exc) {
                    Log.e(TAG, "Use case binding failed", exc);
                    try {
                        cameraProvider.unbindAll();
                        cameraProvider.bindToLifecycle(
                                this, cameraSelector, preview, imageCapture, imageAnalysis);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Video recording disabled: Device limit reached", Toast.LENGTH_LONG).show();
                            btnRecordVideo.setEnabled(false);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Use case binding failed again", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Camera initialization failed", Toast.LENGTH_SHORT).show());
                    }
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraProvider failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void showNotificationHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(ALERTS_KEY, null);
        String[] events;
        if (json == null) {
            events = new String[]{"No motion alerts"};
        } else {
            try {
                JSONArray arr = new JSONArray(json);
                int n = arr.length();
                if (n == 0) {
                    events = new String[]{"No motion alerts"};
                } else {
                    events = new String[n];
                    for (int i = 0; i < n; i++) {
                        // show newest first
                        events[i] = arr.optString(n - 1 - i);
                    }
                }
            } catch (JSONException e) {
                events = new String[]{"No motion alerts"};
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Motion Events");
        builder.setItems(events, null);
        builder.setPositiveButton("Close", null);
        builder.show();
    }
    
    public void updateDetectionStatus(String personName, boolean isFamily) {
        // Implementation kept for compatibility
    }

    private void toggleMotionSensor() {
        isMotionSensorEnabled = !isMotionSensorEnabled;
        if (!isMotionSensorEnabled) {
            previousFrame = null;
        }
        updateMotionSensorButton();
        Toast.makeText(this, "Motion sensor " + (isMotionSensorEnabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }

    private void updateMotionSensorButton() {
        if (btnMotionSensor == null) return;
        if (isMotionSensorEnabled) {
            btnMotionSensor.setText("Sensor ON");
            btnMotionSensor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32)); // Green
            btnMotionSensor.setIconTint(android.content.res.ColorStateList.valueOf(0xFFFFFFFF)); // White
        } else {
            btnMotionSensor.setText("Sensor OFF");
            btnMotionSensor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x66000000)); // Semi-transparent black
            btnMotionSensor.setIconTint(android.content.res.ColorStateList.valueOf(0xFFFFFFFF)); // White
        }
    }

    private void analyzeFrameForMotion(ImageProxy image) {
        // Get Y plane from NV21 format
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        
        byte[] currentFrame = new byte[yBuffer.remaining()];
        yBuffer.get(currentFrame);

        if (previousFrame != null && currentFrame.length == previousFrame.length) {
            // Calculate average difference - improved algorithm for slow motion
            double difference = calculateMotionDifference(previousFrame, currentFrame);
            
            Log.d(TAG, "Motion difference: " + difference);
            
            if (difference > MOTION_THRESHOLD) {
                handleMotionDetected();
            }
        }
        
        previousFrame = currentFrame;
    }

    private double calculateMotionDifference(byte[] frame1, byte[] frame2) {
        if (frame1.length == 0 || frame2.length == 0) {
            return 0;
        }

        long difference = 0;
        int sampleStep = 8; // Sample every 8th pixel for performance
        int sampleCount = 0;

        for (int i = 0; i < frame1.length; i += sampleStep) {
            int val1 = frame1[i] & 0xFF;
            int val2 = frame2[i] & 0xFF;
            difference += Math.abs(val2 - val1);
            sampleCount++;
        }

        if (sampleCount == 0) {
            return 0;
        }

        return (double) difference / sampleCount;
    }

    private void handleMotionDetected() {
        long currentTime = System.currentTimeMillis();
        
        // Show notification only if cooldown has passed
        if (currentTime - lastMotionNotificationTime >= MOTION_NOTIFICATION_COOLDOWN_MS) {
            lastMotionNotificationTime = currentTime;
            
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(currentTime);
            String message = "Motion detected at " + time;
            runOnUiThread(() -> {
                tvNotification.setText(message);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            });
            // persist alert
            addMotionAlert(message);
            
            Log.d(TAG, "Motion detected at " + currentTime);
        }
    }

    private void addMotionAlert(String message) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String json = prefs.getString(ALERTS_KEY, null);
            JSONArray arr = (json == null) ? new JSONArray() : new JSONArray(json);

            // append, keep size bounded
            arr.put(message);
            if (arr.length() > MAX_STORED_ALERTS) {
                // remove oldest (index 0) until size ok
                JSONArray newArr = new JSONArray();
                int start = arr.length() - MAX_STORED_ALERTS;
                for (int i = start; i < arr.length(); i++) {
                    newArr.put(arr.optString(i));
                }
                arr = newArr;
            }

            prefs.edit().putString(ALERTS_KEY, arr.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save motion alert", e);
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                    isRemoteCameraActive = true;
                    switchToRemoteCamera();
                });

                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show());
                try {
                    bluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
            }
        }).start();
    }

    private void disconnectBluetooth() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        isRemoteCameraActive = false;
        switchToLocalCamera();
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    private void switchToRemoteCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.e(TAG, "Failed to unbind camera", e);
            }
        }, ContextCompat.getMainExecutor(this));

        viewFinder.setVisibility(View.GONE);
        remoteCameraView.setVisibility(View.VISIBLE);
        tvStatus.setText("System Status: Remote Camera");
    }

    private void switchToLocalCamera() {
        remoteCameraView.setVisibility(View.GONE);
        viewFinder.setVisibility(View.VISIBLE);
        tvStatus.setText("System Status: Online");
        startCamera();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private boolean isRunning = true;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error creating input stream", e);
            }
            mmInStream = tmpIn;
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            while (isRunning) {
                try {
                    if (mmInStream.available() > 0) {
                        // Note: This assumes the Arduino sends a valid image stream (e.g. JPEG)
                        // If sending raw bytes, you need to buffer and construct the image manually.
                        Thread.sleep(100); // Wait for data
                        Bitmap bitmap = BitmapFactory.decodeStream(mmInStream);
                        if (bitmap != null) {
                            runOnUiThread(() -> remoteCameraView.setImageBitmap(bitmap));
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "disconnected", e);
                    break;
                }
            }
        }

        public void cancel() {
            isRunning = false;
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}