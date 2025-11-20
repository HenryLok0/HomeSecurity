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
    private TextView tvNotification;
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
        tvNotification = findViewById(R.id.tvNotification);

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
        
        // Example usage: Simulate a detection after 5 seconds (for demonstration)
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDetectionStatus("John Doe", true);
            }
        }, 5000);
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

    /**
     * Updates the notification bar with detection information.
     * @param personName The name of the person detected.
     * @param isFamily Whether the person is a family member.
     */
    public void updateDetectionStatus(String personName, boolean isFamily) {
        String message;
        int color;
        int backgroundColor;

        if (isFamily) {
            message = "Family member detected: " + personName;
            color = 0xFF2E7D32; // Green 800
            backgroundColor = 0xFFE8F5E9; // Green 50
        } else {
            message = "Unknown person detected: " + personName;
            color = 0xFFC62828; // Red 800
            backgroundColor = 0xFFFFEBEE; // Red 50
        }

        tvNotification.setText(message);
        tvNotification.setTextColor(color);
        
        // Find the CardView parent to change background color
        if (tvNotification.getParent() instanceof androidx.cardview.widget.CardView) {
            ((androidx.cardview.widget.CardView) tvNotification.getParent()).setCardBackgroundColor(backgroundColor);
        }
    }
}