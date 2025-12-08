package ict.mgame.homesecurity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BluetoothManager instance;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private BluetoothListener listener;
    private boolean isConnected = false;
    private boolean isConnecting = false;

    public interface BluetoothListener {
        void onConnecting();
        void onConnected(String deviceName);
        void onConnectionFailed();
        void onDisconnected();
        void onImageReceived(Bitmap bitmap);
        void onEnvDataReceived(float temp, float hum, int sound, int light);
        void onMessageReceived(String message);
    }

    private BluetoothManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static synchronized BluetoothManager getInstance() {
        if (instance == null) {
            instance = new BluetoothManager();
        }
        return instance;
    }

    public void setListener(BluetoothListener listener) {
        this.listener = listener;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void connect(String address) {
        if (bluetoothAdapter == null) return;
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            connect(device);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid address: " + address, e);
            if (listener != null) {
                listener.onConnectionFailed();
            }
        }
    }

    public void connect(BluetoothDevice device) {
        isConnecting = true;
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onConnecting());
        }
        new Thread(() -> {
            try {
                // Note: Permission check should be done before calling this
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothSocket.connect();
                } catch (SecurityException se) {
                    Log.e(TAG, "Permission missing during connection", se);
                    isConnecting = false;
                    if (listener != null) {
                        new Handler(Looper.getMainLooper()).post(() -> listener.onConnectionFailed());
                    }
                    return;
                }

                isConnected = true;
                isConnecting = false;
                if (listener != null) {
                    // device.getName() might throw SecurityException too
                    String name = "Unknown Device";
                    try {
                        name = device.getName();
                    } catch (SecurityException e) {
                        Log.w(TAG, "Cannot get device name", e);
                    }
                    String finalName = name;
                    new Handler(Looper.getMainLooper()).post(() -> listener.onConnected(finalName));
                }

                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                isConnected = false;
                isConnecting = false;
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onConnectionFailed());
                }
                try {
                    if (bluetoothSocket != null) bluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
            }
        }).start();
    }

    public void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        isConnected = false;
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public void sendAlarmOn() {
        if (connectedThread != null) connectedThread.sendAlarmOn();
    }

    public void sendAlarmOff() {
        if (connectedThread != null) connectedThread.sendAlarmOff();
    }

    public void requestEnvData() {
        if (connectedThread != null) connectedThread.requestEnvData();
    }

    public void write(String s) {
        if (connectedThread != null) connectedThread.write(s);
    }

    public void setUpdateInterval(long interval) {
        if (connectedThread != null) {
            connectedThread.setEnvRequestInterval(interval);
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private volatile boolean isRunning = true;
        private long lastEnvRequestTime = 0;
        private long envRequestInterval = 300; // Default 300ms

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

        public void setEnvRequestInterval(long interval) {
            this.envRequestInterval = interval;
        }

        public void run() {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] readTmp = new byte[1024];

            // Request initial data
            new Handler(Looper.getMainLooper()).postDelayed(this::requestEnvData, 1000);

            while (isRunning) {
                try {
                    long now = System.currentTimeMillis();
                    if (now - lastEnvRequestTime >= envRequestInterval) {
                        requestEnvData();
                        lastEnvRequestTime = now;
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

            int start = -1;
            for (int i = 0; i < data.length - 1; i++) {
                if ((data[i] & 0xFF) == 0xFF && (data[i+1] & 0xFF) == 0xD8) {
                    start = i;
                    break;
                }
            }

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

            if (start != -1) {
                int end = -1;
                for (int j = start + 2; j < data.length - 1; j++) {
                    if ((data[j] & 0xFF) == 0xFF && (data[j+1] & 0xFF) == 0xD9) {
                        end = j + 1;
                        break;
                    }
                }

                if (end != -1) {
                    try {
                        byte[] jpeg = new byte[end - start + 1];
                        System.arraycopy(data, start, jpeg, 0, jpeg.length);
                        final Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                        if (bmp != null && listener != null) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onImageReceived(bmp));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to decode JPEG chunk", e);
                    }

                    int remainingLen = data.length - (end + 1);
                    byte[] remaining = new byte[remainingLen];
                    System.arraycopy(data, end + 1, remaining, 0, remainingLen);
                    buffer.reset();
                    try { buffer.write(remaining); } catch (IOException e) {}
                    processBuffer(buffer);
                } else {
                    int remainingLen = data.length - start;
                    byte[] remaining = new byte[remainingLen];
                    System.arraycopy(data, start, remaining, 0, remainingLen);
                    buffer.reset();
                    try { buffer.write(remaining); } catch (IOException e) {}
                }
            } else {
                String textChunk = new String(data);
                int lastNewline = textChunk.lastIndexOf('\n');
                if (lastNewline >= 0) {
                    int remainingLen = data.length - (lastNewline + 1);
                    if (remainingLen > 0) {
                        byte[] remaining = new byte[remainingLen];
                        System.arraycopy(data, lastNewline + 1, remaining, 0, remainingLen);
                        buffer.reset();
                        try { buffer.write(remaining); } catch (IOException e) {}
                    } else {
                        buffer.reset();
                    }
                }
            }

            if (buffer.size() > 500000) {
                buffer.reset();
            }
        }

        private void processTextLine(String line) {
            if (line.isEmpty()) return;
            if (line.startsWith("ENV:")) {
                try {
                    int tempStart = line.indexOf("TEMP=") + 5;
                    int tempEnd = line.indexOf(" ", tempStart);
                    float t = Float.parseFloat(line.substring(tempStart, tempEnd));

                    int humStart = line.indexOf("HUM=") + 4;
                    int humEnd = line.indexOf(" ", humStart);
                    float h = Float.parseFloat(line.substring(humStart, humEnd));

                    int soundStart = line.indexOf("SOUND=") + 6;
                    int soundEnd = line.indexOf("%", soundStart);
                    int s = Integer.parseInt(line.substring(soundStart, soundEnd));

                    int lightStart = line.indexOf("LIGHT=") + 6;
                    int lightEnd = line.indexOf("%", lightStart);
                    int l = Integer.parseInt(line.substring(lightStart, lightEnd));

                    if (listener != null) {
                        new Handler(Looper.getMainLooper()).post(() -> listener.onEnvDataReceived(t, h, s, l));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse ENV line: " + line, e);
                }
            } else if (line.length() > 3 && !line.startsWith("TEMP=") && !line.startsWith("SOUND_RAW=")) {
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onMessageReceived(line));
                }
            }
        }

        public void requestEnvData() {
            write('e');
        }

        public void sendAlarmOn() {
            write('a');
        }

        public void sendAlarmOff() {
            write("x");
        }

        public void write(String s) {
            if (mmOutStream != null) {
                try {
                    mmOutStream.write(s.getBytes());
                    mmOutStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send string: " + s, e);
                }
            }
        }

        private void write(char c) {
            write(String.valueOf(c));
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
