package ict.mgame.homesecurity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private HistoryAdapter adapter;
    private List<File> fileList;
    private MenuItem menuDelete;
    private MenuItem menuShare;
    private MenuItem menuManage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        loadFiles();
    }

    private void loadFiles() {
        fileList = new ArrayList<>();

        // Load from Pictures/HomeSecurity-Image
        File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "HomeSecurity-Image");
        if (imageDir.exists() && imageDir.isDirectory()) {
            File[] files = imageDir.listFiles();
            if (files != null) {
                fileList.addAll(Arrays.asList(files));
            }
        }

        // Load from Movies/HomeSecurity-Video
        File videoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HomeSecurity-Video");
        if (videoDir.exists() && videoDir.isDirectory()) {
            File[] files = videoDir.listFiles();
            if (files != null) {
                fileList.addAll(Arrays.asList(files));
            }
        }

        // Sort by date descending
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.compare(o2.lastModified(), o1.lastModified());
            }
        });

        if (fileList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new HistoryAdapter(requireContext(), fileList, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_history, menu);
        menuDelete = menu.findItem(R.id.action_delete);
        menuShare = menu.findItem(R.id.action_share);
        menuManage = menu.findItem(R.id.action_manage);

        menuDelete.setVisible(false);
        menuShare.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_manage) {
            if (adapter != null) {
                boolean newMode = !adapter.isSelectionMode();
                adapter.setSelectionMode(newMode);
                onSelectionChanged(adapter.getSelectedFiles().size());
            }
            return true;
        } else if (id == R.id.action_delete) {
            deleteSelectedFiles();
            return true;
        } else if (id == R.id.action_share) {
            shareSelectedFiles();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedFiles() {
        Set<File> selected = adapter.getSelectedFiles();
        List<File> deletedFiles = new ArrayList<>();
        for (File file : selected) {
            if (file.delete()) {
                deletedFiles.add(file);
            }
        }
        adapter.removeFiles(deletedFiles);
        adapter.clearSelection();
        Toast.makeText(requireContext(), "Deleted " + deletedFiles.size() + " files", Toast.LENGTH_SHORT).show();

        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void shareSelectedFiles() {
        Set<File> selected = adapter.getSelectedFiles();

        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ArrayList<Uri> uris = new ArrayList<>();
            for (File file : selected) {
                if (file.exists()) {
                    Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
                    uris.add(uri);
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(requireContext(), "No valid files to share", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent shareIntent = new Intent();
            if (uris.size() == 1) {
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            } else {
                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            }

            // Determine MIME type based on files
            String mimeType = "*/*";
            if (!selected.isEmpty()) {
                File firstFile = selected.iterator().next();
                if (firstFile.getName().toLowerCase().endsWith(".mp4")) {
                    mimeType = "video/*";
                } else if (firstFile.getName().toLowerCase().endsWith(".jpg") ||
                        firstFile.getName().toLowerCase().endsWith(".jpeg")) {
                    mimeType = "image/*";
                }
            }

            shareIntent.setType(mimeType);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share files via"));
            adapter.clearSelection();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to share files: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(File file) {
        Intent intent = new Intent(requireContext(), MediaViewerActivity.class);
        intent.putExtra("file_path", file.getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) {
        boolean isSelectionMode = adapter != null && adapter.isSelectionMode();

        if (menuManage != null) {
            menuManage.setTitle(isSelectionMode ? "Cancel" : "Manage");
        }

        if (count > 0) {
            menuDelete.setVisible(true);
            menuShare.setVisible(true);
            if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(count + " selected");
            }
        } else {
            menuDelete.setVisible(false);
            menuShare.setVisible(false);
            if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(isSelectionMode ? "Select items" : "History");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFiles(); // Reload in case files were deleted in viewer
    }
}
