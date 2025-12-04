package ict.mgame.homesecurity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class MediaViewerActivity extends AppCompatActivity {

    private ImageView ivFullImage;
    private VideoView videoView;
    private FloatingActionButton btnDelete;
    private FloatingActionButton btnShare;
    private ImageButton btnClose;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);

        ivFullImage = findViewById(R.id.ivFullImage);
        videoView = findViewById(R.id.videoView);
        btnDelete = findViewById(R.id.btnDelete);
        btnShare = findViewById(R.id.btnShare);
        btnClose = findViewById(R.id.btnClose);

        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null) {
            file = new File(filePath);
            if (file.exists()) {
                if (filePath.endsWith(".mp4")) {
                    showVideo(file);
                } else {
                    showImage(file);
                }
            } else {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        btnDelete.setOnClickListener(v -> {
            if (file != null && file.exists()) {
                if (file.delete()) {
                    Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnShare.setOnClickListener(v -> shareFile());

        btnClose.setOnClickListener(v -> finish());
    }

    private void showImage(File file) {
        ivFullImage.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        Glide.with(this).load(file).into(ivFullImage);
    }

    private void showVideo(File file) {
        ivFullImage.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.fromFile(file));
        videoView.start();
    }

    private void shareFile() {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri fileUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
            );

            String mimeType = file.getName().endsWith(".mp4") ? "video/mp4" : "image/*";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}