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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
    private View btnGallery;
    private View btnNotifications;
    private MaterialButton btnLogout;
    private MaterialButton btnMotionSensor;
    private MaterialButton btnSwitchCamera;
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
    public static final String PREFS_NAME = "HomeSecurityPrefs";
    public static final String ALERTS_KEY = "motion_alerts"; // stored as JSON array string
    private static final int MAX_STORED_ALERTS = 200;
    // motion detection smoothing
    private static final int MOTION_WINDOW_SAMPLES = 3; // average over 3 frames
    private int motionSampleCount = 0;
    private double motionAccumulator = 0.0;

    // Bluetooth variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private static ConnectedThread connectedThread; // Changed to static for SettingsActivity access
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
        
        // Apply saved theme preference
        SharedPreferences prefs = getSharedPreferences("HomeSecurityPrefs", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
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
        btnGallery = findViewById(R.id.btnGallery);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnLogout = findViewById(R.id.btnLogout);
        btnMotionSensor = findViewById(R.id.btnMotionSensor);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        tvStatus = findViewById(R.id.tvStatus);
        tvNotification = findViewById(R.id.tvNotification);
        viewFinder = findViewById(R.id.viewFinder);
        remoteCameraView = findViewById(R.id.remoteCameraView);

        // Check for null views
        if (tvStatus == null || tvNotification == null || viewFinder == null) {
            Toast.makeText(this, "Error: Failed to initialize views. Please restart the app.", Toast.LENGTH_LONG).show();
            return;
        }

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

        btnGallery.setOnClickListener(v -> {
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

        // Camera Switch Button Listener
        btnSwitchCamera.setOnClickListener(v -> toggleCameraView());

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
        startCamera(false);
    }

    private void startCamera(boolean forceStart) {
        if (isRemoteCameraActive && !forceStart) return;

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
                Toast.makeText(this, "Camera permissions are required for full functionality. Some features may be limited.", Toast.LENGTH_LONG).show();
                // Don't finish() - allow app to continue with limited functionality
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        disconnectBluetooth();
    }

    private void showNotificationHistory() {
        // Open the full AlertsActivity with a list view (migrated from dialog-based UI)
        try {
            Intent intent = new Intent(this, AlertsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open AlertsActivity", e);
        }
    }
    
    public void updateDetectionStatus(String personName, boolean isFamily) {
        // Implementation kept for compatibility
    }

    private void toggleMotionSensor() {
        isMotionSensorEnabled = !isMotionSensorEnabled;
        if (!isMotionSensorEnabled) {
            previousFrame = null;
        }
        
        // Animate the button
        Animation scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        btnMotionSensor.startAnimation(scaleAnim);
        
        updateMotionSensorButton();
        
        String message = "Motion sensor " + (isMotionSensorEnabled ? "enabled" : "disabled");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        // Update status indicator
        updateStatusIndicator();
    }

    private void updateMotionSensorButton() {
        if (btnMotionSensor == null) return;
        if (isMotionSensorEnabled) {
            btnMotionSensor.setText("Sensor ON");
            btnMotionSensor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.motion_active)
            ));
            btnMotionSensor.setIconTint(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.white)
            ));
            
            // Add pulse animation when active
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
            btnMotionSensor.startAnimation(pulse);
        } else {
            btnMotionSensor.setText("Sensor OFF");
            btnMotionSensor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.motion_inactive)
            ));
            btnMotionSensor.setIconTint(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.white)
            ));
            
            // Clear animation when inactive
            btnMotionSensor.clearAnimation();
        }
    }
    
    private void updateStatusIndicator() {
        View statusIndicator = findViewById(R.id.statusIndicator);
        TextView tvStatusSubtext = findViewById(R.id.tvStatusSubtext);
        
        if (statusIndicator != null && tvStatusSubtext != null) {
            if (isMotionSensorEnabled) {
                statusIndicator.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_status_active));
                tvStatusSubtext.setText("Motion detection active");
                
                // Pulse animation for status indicator
                Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
                statusIndicator.startAnimation(pulse);
            } else {
                statusIndicator.clearAnimation();
                tvStatusSubtext.setText("All systems operational");
            }
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
            // smoothing: moving average over MOTION_WINDOW_SAMPLES
            motionAccumulator += difference;
            motionSampleCount++;
            if (motionSampleCount >= MOTION_WINDOW_SAMPLES) {
                double avg = motionAccumulator / motionSampleCount;
                Log.d(TAG, "Motion avg over " + motionSampleCount + " frames: " + avg);
                if (avg > MOTION_THRESHOLD) {
                    handleMotionDetected();
                }
                // reset window
                motionAccumulator = 0.0;
                motionSampleCount = 0;
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
                
                // Animate notification card
                View cvNotification = findViewById(R.id.cvNotification);
                if (cvNotification != null) {
                    Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
                    cvNotification.startAnimation(slideIn);
                    
                    // Change card color to warning
                    if (cvNotification instanceof com.google.android.material.card.MaterialCardView) {
                        com.google.android.material.card.MaterialCardView cardView = 
                            (com.google.android.material.card.MaterialCardView) cvNotification;
                        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.warning));
                        cardView.setStrokeColor(ContextCompat.getColor(this, R.color.danger));
                        
                        // Reset color after 5 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            cardView.setCardBackgroundColor(0xFFE8F5E9);
                            cardView.setStrokeColor(ContextCompat.getColor(this, R.color.success));
                        }, 5000);
                    }
                }
            });
            // attempt to capture a photo; when saved, the callback will persist the alert with photo URI
            takePhotoForAlert(message);
            Log.d(TAG, "Motion detected at " + currentTime);
            
            // Trigger Arduino alarm: send 'a', wait 5s, send 'x'
            triggerAlarm();
        }
    }

    private void triggerAlarm() {
        // Check if buzzer alarm is enabled
        boolean buzzerEnabled = SettingsActivity.isBuzzerAlarmEnabled(this);
        
        if (buzzerEnabled) {
            // Send alarm ON command to Arduino (if connected via Bluetooth)
            if (connectedThread != null && isRemoteCameraActive) {
                connectedThread.sendAlarmOn();
                
                // Schedule alarm OFF after 3 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (connectedThread != null) {
                        connectedThread.sendAlarmOff();
                    }
                }, 3000); // 3 seconds delay
                
                Log.d(TAG, "Alarm triggered: Buzzer ON for 3 seconds");
            }
            
            // Show notification to user on phone
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "⚠️ Motion Detected! Buzzer activated for 3 seconds", Toast.LENGTH_LONG).show();
            });
        } else {
            // Buzzer disabled - notification only
            Log.d(TAG, "Motion detected: Buzzer disabled, notification only");
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "⚠️ Motion Detected! (Buzzer alarm disabled)", Toast.LENGTH_LONG).show();
            });
        }
    }
    
    // Static method for SettingsActivity to send commands
    public static boolean sendBluetoothCommand(char command) {
        if (connectedThread != null) {
            try {
                if (command == 'a') {
                    connectedThread.sendAlarmOn();
                } else if (command == 'x') {
                    connectedThread.sendAlarmOff();
                } else if (command == 't') {
                    connectedThread.requestDhtData();
                } else if (command == 's') {
                    connectedThread.requestSoundData();
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to send command from Settings", e);
                return false;
            }
        }
        return false;
    }

    private void takePhotoForAlert(String message) {
        if (imageCapture == null) {
            addMotionAlert(message, null);
            return;
        }

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

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String uri = (outputFileResults.getSavedUri() != null) ? outputFileResults.getSavedUri().toString() : null;
                addMotionAlert(message, uri);
                Log.d(TAG, "Alert photo saved: " + uri);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Failed to save alert photo", exception);
                addMotionAlert(message, null);
            }
        });
    }

    private void addMotionAlert(String message) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String json = prefs.getString(ALERTS_KEY, null);
            JSONArray arr = (json == null) ? new JSONArray() : new JSONArray(json);
            // store as object with possible image URI
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("message", message);
            obj.put("uri", org.json.JSONObject.NULL);
            arr.put(obj);
            if (arr.length() > MAX_STORED_ALERTS) {
                int start = arr.length() - MAX_STORED_ALERTS;
                JSONArray newArr = new JSONArray();
                for (int i = start; i < arr.length(); i++) {
                    newArr.put(arr.opt(i));
                }
                arr = newArr;
            }
            prefs.edit().putString(ALERTS_KEY, arr.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save motion alert", e);
        }
    }

    private void addMotionAlert(String message, String uriString) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String json = prefs.getString(ALERTS_KEY, null);
            JSONArray arr = (json == null) ? new JSONArray() : new JSONArray(json);
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("message", message);
            if (uriString != null) obj.put("uri", uriString); else obj.put("uri", org.json.JSONObject.NULL);
            arr.put(obj);
            if (arr.length() > MAX_STORED_ALERTS) {
                int start = arr.length() - MAX_STORED_ALERTS;
                JSONArray newArr = new JSONArray();
                for (int i = start; i < arr.length(); i++) newArr.put(arr.opt(i));
                arr = newArr;
            }
            prefs.edit().putString(ALERTS_KEY, arr.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save motion alert", e);
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        // Show connecting status
        runOnUiThread(() -> {
            tvStatus.setText("System Status: Connecting...");
            tvStatus.setTextColor(0xFFFF9800); // Orange color for connecting
        });
        
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> {
                        tvStatus.setText("System Status: Permission Denied");
                        tvStatus.setTextColor(0xFFD32F2F); // Red
                    });
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
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("System Status: Connection Failed ✗");
                    tvStatus.setTextColor(0xFFD32F2F); // Red
                });
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
        runOnUiThread(() -> {
            // Show camera switch button when connected via Bluetooth
            btnSwitchCamera.setVisibility(View.VISIBLE);
            updateCameraSwitchButton(false); // Start with Arduino camera (OV7670)
            
            // Initially show Arduino camera
            viewFinder.setVisibility(View.GONE);
            remoteCameraView.setVisibility(View.VISIBLE);
            tvStatus.setText("System Status: Connected ✓");
            tvStatus.setTextColor(0xFF2E7D32); // Green color for connected state
        });
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.e(TAG, "Failed to unbind camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void switchToLocalCamera() {
        runOnUiThread(() -> {
            remoteCameraView.setVisibility(View.GONE);
            viewFinder.setVisibility(View.VISIBLE);
            tvStatus.setText("System Status: Online");
            tvStatus.setTextColor(0xFF000000); // Black (default)
            
            // Hide camera switch button when using local camera only
            btnSwitchCamera.setVisibility(View.GONE);
        });
        
        startCamera();
    }

    private void toggleCameraView() {
        if (isRemoteCameraActive) {
            // Toggle between phone camera and Arduino camera
            boolean showingRemote = (remoteCameraView.getVisibility() == View.VISIBLE);
            
            if (showingRemote) {
                // Switch to phone camera
                runOnUiThread(() -> {
                    remoteCameraView.setVisibility(View.GONE);
                    viewFinder.setVisibility(View.VISIBLE);
                    updateCameraSwitchButton(true);
                    tvStatus.setText("System Status: Phone Camera");
                });
                
                // Force start camera even though Bluetooth is connected
                startCamera(true);
            } else {
                // Switch to Arduino camera
                ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
                cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        cameraProvider.unbindAll();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to unbind camera", e);
                    }
                }, ContextCompat.getMainExecutor(this));
                
                runOnUiThread(() -> {
                    viewFinder.setVisibility(View.GONE);
                    remoteCameraView.setVisibility(View.VISIBLE);
                    updateCameraSwitchButton(false);
                    tvStatus.setText("System Status: Arduino Camera");
                });
            }
        }
    }

    private void updateCameraSwitchButton(boolean isPhoneCamera) {
        if (btnSwitchCamera == null) return;
        
        // Animate button transition
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        
        btnSwitchCamera.startAnimation(fadeOut);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isPhoneCamera) {
                    btnSwitchCamera.setText("Phone");
                    btnSwitchCamera.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(MainActivity.this, R.color.camera_phone)
                    ));
                } else {
                    btnSwitchCamera.setText("Arduino");
                    btnSwitchCamera.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(MainActivity.this, R.color.camera_arduino)
                    ));
                }
                btnSwitchCamera.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private volatile boolean isRunning = true;
        private long lastDhtRequestTime = 0;
        private static final long DHT_REQUEST_INTERVAL = 5000; // Request every 5 seconds

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error creating streams", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "BEGIN mConnectedThread");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] readTmp = new byte[1024];
            
            // Request initial DHT11 reading after connection established
            new Handler(Looper.getMainLooper()).postDelayed(this::requestDhtData, 1000); 
            
            while (isRunning) {
                try {
                    // Periodic DHT11 data request (every 5 seconds)
                    long now = System.currentTimeMillis();
                    if (now - lastDhtRequestTime >= DHT_REQUEST_INTERVAL) {
                        requestDhtData();
                        lastDhtRequestTime = now;
                    }
                    
                    if (mmInStream == null) break;

                    int available = mmInStream.available();
                    if (available > 0) {
                        int toRead = Math.min(available, readTmp.length);
                        int read = mmInStream.read(readTmp, 0, toRead);
                        if (read > 0) {
                            buffer.write(readTmp, 0, read);
                            processBuffer(buffer);
                        }
                    } else {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    break;
                }
            }
            cancel();
        }

        private void processBuffer(ByteArrayOutputStream buffer) {
            byte[] data = buffer.toByteArray();
            if (data.length == 0) return;

            // 1. Find JPEG Start
            int start = -1;
            for (int i = 0; i < data.length - 1; i++) {
                if ((data[i] & 0xFF) == 0xFF && (data[i+1] & 0xFF) == 0xD8) {
                    start = i;
                    break;
                }
            }

            // 2. Process text before JPEG (or all text if no JPEG)
            int textEnd = (start != -1) ? start : data.length;
            if (textEnd > 0) {
                byte[] textBytes = new byte[textEnd];
                System.arraycopy(data, 0, textBytes, 0, textEnd);
                
                String textChunk = new String(textBytes);
                int lastNewline = textChunk.lastIndexOf('\n');
                
                if (lastNewline >= 0) {
                    String toProcess = textChunk.substring(0, lastNewline);
                    String[] lines = toProcess.split("\\r?\\n");
                    for (String line : lines) {
                        processTextLine(line.trim());
                    }
                }
            }

            // 3. Process JPEG
            if (start != -1) {
                // Check for JPEG End
                int end = -1;
                for (int j = start + 2; j < data.length - 1; j++) {
                    if ((data[j] & 0xFF) == 0xFF && (data[j+1] & 0xFF) == 0xD9) {
                        end = j + 1;
                        break;
                    }
                }

                if (end != -1) {
                    // Found complete JPEG
                    try {
                        byte[] jpeg = new byte[end - start + 1];
                        System.arraycopy(data, start, jpeg, 0, jpeg.length);
                        final Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                        if (bmp != null) {
                            runOnUiThread(() -> remoteCameraView.setImageBitmap(bmp));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to decode JPEG chunk", e);
                    }
                    
                    // Remove everything up to end of JPEG
                    int remainingLen = data.length - (end + 1);
                    byte[] remaining = new byte[remainingLen];
                    System.arraycopy(data, end + 1, remaining, 0, remainingLen);
                    
                    buffer.reset();
                    try {
                        buffer.write(remaining);
                    } catch (IOException e) {}
                    
                    // Recurse
                    processBuffer(buffer);
                } else {
                    // Partial JPEG. Keep from 'start' onwards.
                    int remainingLen = data.length - start;
                    byte[] remaining = new byte[remainingLen];
                    System.arraycopy(data, start, remaining, 0, remainingLen);
                    
                    buffer.reset();
                    try {
                        buffer.write(remaining);
                    } catch (IOException e) {}
                }
            } else {
                // No JPEG start found. Keep remaining text after last newline.
                String textChunk = new String(data);
                int lastNewline = textChunk.lastIndexOf('\n');
                if (lastNewline >= 0) {
                     int remainingLen = data.length - (lastNewline + 1);
                     if (remainingLen > 0) {
                         byte[] remaining = new byte[remainingLen];
                         System.arraycopy(data, lastNewline + 1, remaining, 0, remainingLen);
                         buffer.reset();
                         try {
                             buffer.write(remaining);
                         } catch (IOException e) {}
                     } else {
                         buffer.reset();
                     }
                }
            }
            
            // Safety limit
            if (buffer.size() > 500000) { // 500KB
                Log.w(TAG, "Buffer too large, clearing");
                buffer.reset();
            }
        }

        private void processTextLine(String line) {
            if (line.isEmpty()) return;
            
            if (line.toUpperCase().contains("TEMP=") && line.toUpperCase().contains("HUM=")) {
                try {
                    int tempStart = line.toUpperCase().indexOf("TEMP=") + 5;
                    int tempEnd = line.indexOf(" ", tempStart);
                    if (tempEnd == -1) tempEnd = line.indexOf(",", tempStart);
                    if (tempEnd == -1) tempEnd = line.length();
                    String tempStr = line.substring(tempStart, tempEnd).trim();
                    
                    int humStart = line.toUpperCase().indexOf("HUM=") + 4;
                    int humEnd = line.indexOf(" ", humStart);
                    if (humEnd == -1) humEnd = line.indexOf("%", humStart);
                    if (humEnd == -1) humEnd = line.length();
                    String humStr = line.substring(humStart, humEnd).trim();
                    
                    float t = Float.parseFloat(tempStr);
                    float h = Float.parseFloat(humStr);
                    
                    SettingsActivity.saveDhtValues(MainActivity.this, t, h);
                    SettingsActivity.notifyDhtDataReceived(t, h);
                    
                    runOnUiThread(() -> {
                        if (tvStatus != null) {
                            tvStatus.setText(String.format(Locale.getDefault(), "System Status: Remote Camera — %.1f°C %.0f%%", t, h));
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse DHT line: " + line, e);
                }
            } else if (line.toUpperCase().contains("SOUND_RAW=") && line.toUpperCase().contains("SOUND_PERCENT=")) {
                try {
                    int rawStart = line.toUpperCase().indexOf("SOUND_RAW=") + 10;
                    int rawEnd = line.indexOf(",", rawStart);
                    if (rawEnd == -1) rawEnd = line.length();
                    String rawStr = line.substring(rawStart, rawEnd).trim();

                    int percentStart = line.toUpperCase().indexOf("SOUND_PERCENT=") + 14;
                    int percentEnd = line.indexOf("%", percentStart);
                    if (percentEnd == -1) percentEnd = line.length();
                    String percentStr = line.substring(percentStart, percentEnd).trim();

                    int raw = Integer.parseInt(rawStr);
                    int percent = Integer.parseInt(percentStr);

                    SettingsActivity.saveSoundValues(MainActivity.this, raw, percent);
                    SettingsActivity.notifySoundDataReceived(raw, percent);

                    boolean monitorEnabled = SettingsActivity.isSoundMonitoringEnabled(MainActivity.this);
                    int threshold = SettingsActivity.getSoundThresholdPercent(MainActivity.this);
                    if (monitorEnabled && percent >= threshold) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastMotionNotificationTime >= MOTION_NOTIFICATION_COOLDOWN_MS) {
                            lastMotionNotificationTime = currentTime;
                            runOnUiThread(() -> {
                                if (tvNotification != null) {
                                    tvNotification.setText("Sound level exceeded: " + percent + "% (>= " + threshold + "%)");
                                }
                            });
                            triggerAlarm();
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse SOUND line: " + line, e);
                }
            } else if (line.toUpperCase().contains("LIGHT_RAW=") && line.toUpperCase().contains("LIGHT_PERCENT=")) {
                try {
                    int rawStart = line.toUpperCase().indexOf("LIGHT_RAW=") + 10;
                    int rawEnd = line.indexOf(",", rawStart);
                    if (rawEnd == -1) rawEnd = line.length();
                    String rawStr = line.substring(rawStart, rawEnd).trim();

                    int percentStart = line.toUpperCase().indexOf("LIGHT_PERCENT=") + 14;
                    int percentEnd = line.indexOf("%", percentStart);
                    if (percentEnd == -1) percentEnd = line.length();
                    String percentStr = line.substring(percentStart, percentEnd).trim();

                    int raw = Integer.parseInt(rawStr);
                    int percent = Integer.parseInt(percentStr);

                    SettingsActivity.saveLightValues(MainActivity.this, raw, percent);
                    SettingsActivity.notifyLightDataReceived(raw, percent);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse LIGHT line: " + line, e);
                }
            } else {
                // Handle generic messages (e.g., "SYSTEM OFF", "ALARM ON")
                // Filter out short/empty lines or known prefixes to avoid noise
                if (line.length() > 3 && !line.startsWith("TEMP=") && !line.startsWith("SOUND_RAW=")) {
                    Log.i(TAG, "Arduino Message: " + line);
                    runOnUiThread(() -> {
                        // Show important messages to user
                        if (line.contains("SYSTEM") || line.contains("ALARM") || line.contains("ERROR")) {
                            Toast.makeText(MainActivity.this, "Arduino: " + line, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        public void requestDhtData() {
            if (mmOutStream != null) {
                try {
                    mmOutStream.write('t');
                    mmOutStream.flush();
                    Log.d(TAG, "Sent 't' command to Arduino");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send DHT request", e);
                }
            }
        }

        public void requestSoundData() {
            if (mmOutStream != null) {
                try {
                    mmOutStream.write('s');
                    mmOutStream.flush();
                    Log.d(TAG, "Sent 's' command to Arduino");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send SOUND request", e);
                }
            }
        }

        // Send alarm ON command to Arduino
        public void sendAlarmOn() {
            if (mmOutStream != null) {
                try {
                    mmOutStream.write('a');
                    mmOutStream.flush();
                    Log.d(TAG, "Sent 'a' (ALARM ON) to Arduino");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send alarm ON", e);
                }
            }
        }

        // Send alarm OFF command to Arduino
        public void sendAlarmOff() {
            if (mmOutStream != null) {
                try {
                    mmOutStream.write('x');
                    mmOutStream.flush();
                    Log.d(TAG, "Sent 'x' (ALARM OFF) to Arduino");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send alarm OFF", e);
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