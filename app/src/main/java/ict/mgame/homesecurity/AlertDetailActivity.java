package ict.mgame.homesecurity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AlertDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail);

        ImageView iv = findViewById(R.id.ivAlertFull);
        TextView tvMsg = findViewById(R.id.tvAlertDetailMessage);
        TextView tvTime = findViewById(R.id.tvAlertDetailTime);

        String message = getIntent().getStringExtra("message");
        String time = getIntent().getStringExtra("time");
        String uri = getIntent().getStringExtra("uri");

        if (message != null) tvMsg.setText(message);
        if (time != null) tvTime.setText(time);
        if (uri != null && !uri.isEmpty() && !uri.equals("null")) {
            try {
                iv.setImageURI(Uri.parse(uri));
            } catch (Exception e) {
                iv.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
}
