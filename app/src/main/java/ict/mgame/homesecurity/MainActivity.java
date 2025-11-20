package ict.mgame.homesecurity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnSettings;
    private Button btnToggleAlarm;
    private TextView tvStatus;
    private boolean isArmed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSettings = findViewById(R.id.btnSettings);
        btnToggleAlarm = findViewById(R.id.btnToggleAlarm);
        tvStatus = findViewById(R.id.tvStatus);

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        btnToggleAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isArmed = !isArmed;
                updateStatus();
            }
        });

        updateStatus();
    }

    private void updateStatus() {
        if (isArmed) {
            tvStatus.setText("System Status: ARMED");
            btnToggleAlarm.setText("Disarm System");
        } else {
            tvStatus.setText("System Status: DISARMED");
            btnToggleAlarm.setText("Arm System");
        }
    }
}