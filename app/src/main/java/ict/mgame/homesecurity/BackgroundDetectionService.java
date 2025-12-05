package ict.mgame.homesecurity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class BackgroundDetectionService extends Service {
    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "background_detection_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private static boolean isServiceRunning = false;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        createNotificationChannel();
        
        // Acquire wake lock to keep CPU running in background
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HomeSecurity::BackgroundDetection");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle stop action from notification
        if (intent != null && "STOP_SERVICE".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        Log.d(TAG, "Service started");
        
        // Start foreground service with notification
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        isServiceRunning = true;
        
        // The service will keep the process alive in the background
        // MainActivity will continue to handle motion detection if it is not destroyed
        
        return START_STICKY; // Service will restart if killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Background Motion Detection",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitoring for motion in the background");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        // Intent to open MainActivity when notification is tapped
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Action to stop the service
        Intent stopIntent = new Intent(this, BackgroundDetectionService.class);
        stopIntent.setAction("STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Home Security Active")
            .setContentText("Monitoring for motion detection in background")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true) // Cannot be dismissed by user
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }

    public static boolean isRunning() {
        return isServiceRunning;
    }
}
