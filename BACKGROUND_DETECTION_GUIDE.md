# Background Detection Feature

## Overview
The Background Detection feature allows the Home Security app to continue monitoring for motion even when you minimize the app or switch to other apps.

## How It Works

### Starting Background Detection
1. Open the **Settings** page
2. Find the **Background Detection** card (below Bluetooth Devices)
3. Tap **"Start Background Detection"** button
4. A persistent notification will appear showing the app is actively monitoring
5. You can now minimize the app - motion detection will continue

### What Happens in Background Mode
- ✅ Motion detection remains active
- ✅ Camera continues to monitor for movement
- ✅ Notifications are sent when motion is detected
- ✅ Photos are captured automatically
- ✅ Buzzer alarm triggers (if enabled)
- ✅ Bluetooth connection to Arduino maintained

### Stopping Background Detection
**Method 1:** Through Settings
1. Open the app and go to Settings
2. Tap **"Stop Background Detection"** button

**Method 2:** Through Notification
1. Swipe down to see notifications
2. Tap the **"Stop"** action on the Home Security notification

### Important Notes

#### Battery Usage
- Background detection uses more battery power
- The app keeps the camera and processing active
- Consider using this feature when:
  - Phone is charging
  - You need continuous monitoring
  - Battery level is sufficient

#### Permissions Required
- Camera access
- Notification access (Android 13+)
- Foreground service permission

#### Technical Details
- Uses Android Foreground Service
- Acquires partial wake lock to keep CPU active
- Service type: `camera` (for motion detection)
- Notification channel: Low priority to minimize interruption
- Service restart: `START_STICKY` (will restart if killed by system)

#### Limitations
- System may kill the service under extreme memory pressure
- Some battery optimization settings may interfere
- On some devices, you may need to disable battery optimization for this app

### Troubleshooting

**Background detection stops unexpectedly:**
1. Go to Android Settings → Apps → Home Security
2. Battery → Allow background activity
3. Disable battery optimization for this app

**Motion detection not working in background:**
1. Ensure the motion sensor was ON before starting background mode
2. Check that camera permissions are granted
3. Verify Arduino is connected via Bluetooth

**Notification not showing:**
1. Check notification permissions are granted
2. Ensure notification channel is not blocked
3. Go to Android Settings → Apps → Home Security → Notifications

## Status Indicator
The status text below the button shows:
- **Status: Running** (green) - Background detection is active
- **Status: Stopped** (gray) - Background detection is not running

## Auto-Start on Boot
Currently, background detection does not start automatically when phone reboots. You must manually start it after each reboot.
