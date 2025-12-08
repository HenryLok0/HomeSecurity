package ict.mgame.homesecurity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AlertsActivity extends AppCompatActivity {
    private static final String TAG = "AlertsActivity";
    private RecyclerView rvAlerts;
    private AlertsAdapter adapter;
    private List<AlertItem> items;
    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);
        // Setup toolbar with Up (back) button like Gallery
        Toolbar toolbar = findViewById(R.id.toolbarAlerts);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                // Back button removed as per requirement
                // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Alerts");
            }
            // toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvAlerts = findViewById(R.id.rvAlerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));

        items = loadAlertsFromPrefs();
        adapter = new AlertsAdapter(items, v -> {
            // on item click -> open detail
            int pos = rvAlerts.getChildAdapterPosition(v);
            if (pos >= 0 && pos < items.size()) {
                AlertItem ai = items.get(pos);
                Intent intent = new Intent(AlertsActivity.this, AlertDetailActivity.class);
                intent.putExtra("message", ai.message);
                intent.putExtra("time", ai.time);
                intent.putExtra("uri", ai.uri);
                startActivity(intent);
            }
        }, () -> invalidateOptionsMenu());
        rvAlerts.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_alerts, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem delete = menu.findItem(R.id.action_delete);
        MenuItem select = menu.findItem(R.id.action_select);
        if (adapter != null && adapter.isSelectionMode()) {
            select.setTitle("Cancel");
            delete.setVisible(!adapter.getSelectedPositions().isEmpty());
        } else {
            select.setTitle("Select");
            delete.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_select) {
            boolean newMode = !adapter.isSelectionMode();
            adapter.setSelectionMode(newMode);
            invalidateOptionsMenu();
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
                    Intent intent = new Intent(AlertsActivity.this, AlertDetailActivity.class);
                    intent.putExtra("message", ai.message);
                    intent.putExtra("time", ai.time);
                    intent.putExtra("uri", ai.uri);
                    startActivity(intent);
                }
            }, () -> invalidateOptionsMenu());
            rvAlerts.setAdapter(adapter);
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAlertsFromPrefs(java.util.List<AlertItem> toRemove) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
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
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
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
