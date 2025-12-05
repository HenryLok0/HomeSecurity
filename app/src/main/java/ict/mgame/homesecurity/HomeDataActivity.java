package ict.mgame.homesecurity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeDataActivity extends AppCompatActivity implements SensorDataManager.OnDataUpdateListener {

    private LineGraphView graphSound;
    private LineGraphView graphLight;
    private LineGraphView graphTemp;
    private LineGraphView graphHum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_data);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Home Data");
        }

        graphSound = findViewById(R.id.graphSound);
        graphLight = findViewById(R.id.graphLight);
        graphTemp = findViewById(R.id.graphTemp);
        graphHum = findViewById(R.id.graphHum);

        // Configure graphs
        graphSound.setRange(0, 100);
        graphSound.setLineColor(Color.RED);

        graphLight.setRange(0, 100);
        graphLight.setLineColor(Color.YELLOW);

        graphTemp.setRange(0, 50); // 0-50 C
        graphTemp.setLineColor(Color.parseColor("#FFA500")); // Orange

        graphHum.setRange(0, 100);
        graphHum.setLineColor(Color.BLUE);

        // Initial load
        updateGraphs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorDataManager.getInstance().addListener(this);
        updateGraphs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SensorDataManager.getInstance().removeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataUpdated() {
        runOnUiThread(this::updateGraphs);
    }

    private void updateGraphs() {
        List<SensorDataManager.SensorData> history = SensorDataManager.getInstance().getHistory();
        
        List<Float> soundData = new ArrayList<>();
        List<Float> lightData = new ArrayList<>();
        List<Float> tempData = new ArrayList<>();
        List<Float> humData = new ArrayList<>();

        for (SensorDataManager.SensorData data : history) {
            soundData.add((float) data.soundPercent);
            lightData.add((float) data.lightPercent);
            tempData.add(data.temperature);
            humData.add(data.humidity);
        }

        graphSound.setData(soundData);
        graphLight.setData(lightData);
        graphTemp.setData(tempData);
        graphHum.setData(humData);
    }
}
