package ict.mgame.homesecurity;

import java.util.ArrayList;
import java.util.List;

public class SensorDataManager {
    private static SensorDataManager instance;
    private static final int MAX_HISTORY = 1000; // Keep last 1000 points (approx 8 mins at 0.5s interval)

    public static class SensorData {
        public long timestamp;
        public float temperature;
        public float humidity;
        public int soundPercent;
        public int lightPercent;

        public SensorData(long timestamp, float temperature, float humidity, int soundPercent, int lightPercent) {
            this.timestamp = timestamp;
            this.temperature = temperature;
            this.humidity = humidity;
            this.soundPercent = soundPercent;
            this.lightPercent = lightPercent;
        }
    }

    private final List<SensorData> history = new ArrayList<>();
    private final List<OnDataUpdateListener> listeners = new ArrayList<>();

    public interface OnDataUpdateListener {
        void onDataUpdated();
    }

    private SensorDataManager() {}

    public static synchronized SensorDataManager getInstance() {
        if (instance == null) {
            instance = new SensorDataManager();
        }
        return instance;
    }

    public synchronized void addData(float temp, float hum, int sound, int light) {
        history.add(new SensorData(System.currentTimeMillis(), temp, hum, sound, light));
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        notifyListeners();
    }

    public synchronized List<SensorData> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void addListener(OnDataUpdateListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(OnDataUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (OnDataUpdateListener listener : listeners) {
            listener.onDataUpdated();
        }
    }
}
