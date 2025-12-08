package ict.mgame.homesecurity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class HomeDataFragment extends Fragment implements SensorDataManager.OnDataUpdateListener {

    private LineGraphView graphSound;
    private LineGraphView graphLight;
    private LineGraphView graphTemp;
    private LineGraphView graphHum;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        graphSound = view.findViewById(R.id.graphSound);
        graphLight = view.findViewById(R.id.graphLight);
        graphTemp = view.findViewById(R.id.graphTemp);
        graphHum = view.findViewById(R.id.graphHum);

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
    public void onResume() {
        super.onResume();
        SensorDataManager.getInstance().addListener(this);
        updateGraphs();
    }

    @Override
    public void onPause() {
        super.onPause();
        SensorDataManager.getInstance().removeListener(this);
    }

    @Override
    public void onDataUpdated() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateGraphs);
        }
    }

    private void updateGraphs() {
        if (getContext() == null) return;
        
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
