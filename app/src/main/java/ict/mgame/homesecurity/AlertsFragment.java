package ict.mgame.homesecurity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {
    private static final String TAG = "AlertsFragment";
    private RecyclerView rvAlerts;
    private AlertsAdapter adapter;
    private List<AlertItem> items;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        Toolbar toolbar = view.findViewById(R.id.toolbarAlerts);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Alerts");
            }
        }

        rvAlerts = view.findViewById(R.id.rvAlerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));

        items = loadAlertsFromPrefs();
        adapter = new AlertsAdapter(items, v -> {
            // on item click -> open detail
            int pos = rvAlerts.getChildAdapterPosition(v);
            if (pos >= 0 && pos < items.size()) {
                AlertItem ai = items.get(pos);
                Intent intent = new Intent(requireContext(), AlertDetailActivity.class);
                intent.putExtra("message", ai.message);
                intent.putExtra("time", ai.time);
                intent.putExtra("uri", ai.uri);
                startActivity(intent);
            }
        }, () -> requireActivity().invalidateOptionsMenu());
        rvAlerts.setAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_alerts, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem delete = menu.findItem(R.id.action_delete);
        MenuItem select = menu.findItem(R.id.action_select);
        if (adapter != null && adapter.isSelectionMode()) {
            select.setTitle("Cancel");
            delete.setVisible(!adapter.getSelectedPositions().isEmpty());
        } else {
            select.setTitle("Select");
            delete.setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_select) {
            boolean newMode = !adapter.isSelectionMode();
            adapter.setSelectionMode(newMode);
            requireActivity().invalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_delete) {
            // confirm and delete selected
            java.util.Set<Integer> sel = adapter.getSelectedPositions();
            if (sel.isEmpty()) return true;
            // Build list of items to remove
            java.util.List<AlertItem> toRemove = new java.util.ArrayList<>();
            for (Integer p : sel) {
                if (p >= 0 && p < items.size()) toRemove.add(items.get(p));
            }
            deleteAlertsFromPrefs(toRemove);
            // reload
            items = loadAlertsFromPrefs();
            adapter = new AlertsAdapter(items, v -> {
                int pos = rvAlerts.getChildAdapterPosition(v);
                if (pos >= 0 && pos < items.size()) {
                    AlertItem ai = items.get(pos);
                    Intent intent = new Intent(requireContext(), AlertDetailActivity.class);
                    intent.putExtra("message", ai.message);
                    intent.putExtra("time", ai.time);
                    intent.putExtra("uri", ai.uri);
                    startActivity(intent);
                }
            }, () -> requireActivity().invalidateOptionsMenu());
            rvAlerts.setAdapter(adapter);
            requireActivity().invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAlertsFromPrefs(java.util.List<AlertItem> toRemove) {
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(Constants.ALERTS_KEY, null);
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                String message = obj.optString("message", "");
                String time = obj.optString("time", "");
                String uri = obj.optString("uri", null);
                boolean match = false;
                for (AlertItem ai : toRemove) {
                    if (ai.message.equals(message) && ai.time.equals(time) && ((ai.uri == null && (uri == null || uri.equals("null"))) || (ai.uri != null && ai.uri.equals(uri)))) {
                        match = true;
                        break;
                    }
                }
                if (!match) out.put(obj);
            }
            prefs.edit().putString(Constants.ALERTS_KEY, out.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to delete alerts", e);
        }
    }

    private List<AlertItem> loadAlertsFromPrefs() {
        List<AlertItem> list = new ArrayList<>();
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(Constants.ALERTS_KEY, null);
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = arr.length() - 1; i >= 0; i--) { // newest first
                org.json.JSONObject obj = arr.getJSONObject(i);
                String message = obj.optString("message", "Motion detected");
                String time = obj.optString("time", "");
                String uri = obj.optString("uri", null);
                list.add(new AlertItem(message, time, uri));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed parse alerts", e);
        }
        return list;
    }
}
