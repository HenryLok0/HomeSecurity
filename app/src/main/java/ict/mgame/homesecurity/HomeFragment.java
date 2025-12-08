package ict.mgame.homesecurity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import java.util.Locale;

import android.content.Intent;

public class HomeFragment extends Fragment implements BluetoothManager.BluetoothListener, CameraManager.MotionDetectionListener {

    private CameraManager cameraManager;
    private BluetoothManager bluetoothManager;
    
    private PreviewView viewFinder;
    private ImageView remoteCameraView;
    private TextView tvTemp, tvHumidity, tvLight, tvSound;
    private MaterialButton btnMotionSensor, btnPrivacyMode, btnBackgroundService;
    private View privacyOverlay;
    private ImageButton btnTakePhoto, btnRecordVideo, btnSwitchCamera;

    private boolean isPrivacyMode = false;
    private long lastMotionTime = 0;
    private static final long MOTION_COOLDOWN_MS = 5000; // 5 seconds cooldown

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewFinder = view.findViewById(R.id.viewFinder);
        remoteCameraView = view.findViewById(R.id.remoteCameraView);
        tvTemp = view.findViewById(R.id.tvTemp);
        tvHumidity = view.findViewById(R.id.tvHumidity);
        tvLight = view.findViewById(R.id.tvLight);
        tvSound = view.findViewById(R.id.tvSound);
        btnMotionSensor = view.findViewById(R.id.btnMotionSensor);
        btnBackgroundService = view.findViewById(R.id.btnBackgroundService);
        btnPrivacyMode = view.findViewById(R.id.btnPrivacyMode);
        privacyOverlay = view.findViewById(R.id.privacyOverlay);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnRecordVideo = view.findViewById(R.id.btnRecordVideo);
        btnSwitchCamera = view.findViewById(R.id.btnSwitchCamera);

        bluetoothManager = BluetoothManager.getInstance();
        bluetoothManager.setListener(this);

        cameraManager = new CameraManager(requireContext(), getViewLifecycleOwner(), viewFinder);
        cameraManager.setMotionListener(this);
        
        setupListeners();
        updateBackgroundServiceButton();
        
        if (!isPrivacyMode) {
            cameraManager.startCamera();
        }
    }

    private void setupListeners() {
        btnMotionSensor.setOnClickListener(v -> {
            boolean newState = !cameraManager.isMotionSensorEnabled();
            cameraManager.setMotionSensorEnabled(newState);
            updateMotionButton(newState);
        });

        btnBackgroundService.setOnClickListener(v -> {
            if (BackgroundDetectionService.isServiceRunning) {
                stopBackgroundService();
            } else {
                startBackgroundService();
            }
        });

        btnPrivacyMode.setOnClickListener(v -> {
            isPrivacyMode = !isPrivacyMode;
            if (isPrivacyMode) {
                cameraManager.stopCamera();
                privacyOverlay.setVisibility(View.VISIBLE);
                btnPrivacyMode.setText("Disable Privacy Mode");
                btnPrivacyMode.setIconResource(android.R.drawable.ic_menu_camera);
            } else {
                cameraManager.startCamera();
                privacyOverlay.setVisibility(View.GONE);
                btnPrivacyMode.setText("Enable Privacy Mode");
                btnPrivacyMode.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
            }
        });

        btnTakePhoto.setOnClickListener(v -> cameraManager.takePhoto(new CameraManager.OnPhotoSavedCallback() {
            @Override
            public void onPhotoSaved(String uri) {
                Toast.makeText(getContext(), "Photo saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error saving photo", Toast.LENGTH_SHORT).show();
            }
        }));

        btnRecordVideo.setOnClickListener(v -> cameraManager.toggleVideoRecording(new CameraManager.OnVideoRecordingListener() {
            @Override
            public void onRecordingStarted() {
                btnRecordVideo.setImageTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark));
            }

            @Override
            public void onRecordingStopped(String uri) {
                btnRecordVideo.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.white));
                Toast.makeText(getContext(), "Video saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int error) {
                btnRecordVideo.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.white));
                Toast.makeText(getContext(), "Recording error", Toast.LENGTH_SHORT).show();
            }
        }));

        btnSwitchCamera.setOnClickListener(v -> cameraManager.switchCamera());
    }

    private void updateMotionButton(boolean enabled) {
        if (enabled) {
            btnMotionSensor.setText("Motion Sensor ON");
            btnMotionSensor.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.motion_active));
        } else {
            btnMotionSensor.setText("Motion Sensor OFF");
            btnMotionSensor.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.motion_inactive));
        }
    }

    @Override
    public void onConnected(String deviceName) {
        // Handle connection UI updates if needed
    }

    @Override
    public void onConnectionFailed() {
        Toast.makeText(getContext(), "Bluetooth Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        // Handle disconnection
    }

    @Override
    public void onImageReceived(Bitmap bitmap) {
        remoteCameraView.setImageBitmap(bitmap);
    }

    @Override
    public void onEnvDataReceived(float temp, float hum, int sound, int light) {
        tvTemp.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
        tvHumidity.setText(String.format(Locale.getDefault(), "%.0f%%", hum));
        tvLight.setText(String.format(Locale.getDefault(), "%d%%", light));
        tvSound.setText(String.format(Locale.getDefault(), "%d%%", sound));
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(requireContext(), BackgroundDetectionService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
        updateBackgroundServiceButton();
    }

    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(requireContext(), BackgroundDetectionService.class);
        requireContext().stopService(serviceIntent);
        BackgroundDetectionService.isServiceRunning = false;
        updateBackgroundServiceButton();
    }

    private void updateBackgroundServiceButton() {
        if (BackgroundDetectionService.isServiceRunning) {
            btnBackgroundService.setText("Stop Background Detection");
            btnBackgroundService.setIconResource(android.R.drawable.ic_media_pause);
            btnBackgroundService.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.motion_active));
        } else {
            btnBackgroundService.setText("Start Background Detection");
            btnBackgroundService.setIconResource(android.R.drawable.ic_media_play);
            btnBackgroundService.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.motion_inactive));
        }
    }

    @Override
    public void onMessageReceived(String message) {
        // Handle other messages
    }

    @Override
    public void onMotionDetected() {
        long now = System.currentTimeMillis();
        if (now - lastMotionTime < MOTION_COOLDOWN_MS) {
            return;
        }
        lastMotionTime = now;

        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getContext(), "Motion Detected!", Toast.LENGTH_SHORT).show();
            
            // Take photo automatically
            cameraManager.takePhoto(new CameraManager.OnPhotoSavedCallback() {
                @Override
                public void onPhotoSaved(String uri) {
                    String msg = "Motion detected at " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                    saveAlert(msg, uri);
                    sendNotification(msg);
                }

                @Override
                public void onError(Exception e) {
                    // Even if photo fails, save alert
                    String msg = "Motion detected (Photo failed)";
                    saveAlert(msg, null);
                    sendNotification(msg);
                }
            });

            // Trigger alarm via BluetoothManager if needed
            // bluetoothManager.sendAlarmOn();
        });
    }

    private void saveAlert(String message, String uri) {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(Constants.ALERTS_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            JSONObject obj = new JSONObject();
            obj.put("message", message);
            obj.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            obj.put("uri", uri);
            arr.put(obj);
            prefs.edit().putString(Constants.ALERTS_KEY, arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(String message) {
        if (getContext() == null) return;
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "motion_alert_channel";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Motion Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Motion Detected!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraManager.shutdown();
    }
}
