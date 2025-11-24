package ict.mgame.homesecurity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private List<File> fileList;
    private Set<File> selectedFiles = new HashSet<>();
    private boolean isSelectionMode = false;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(File file);
        void onSelectionChanged(int count);
    }

    public HistoryAdapter(Context context, List<File> fileList, OnItemClickListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.listener = listener;
    }

    public void setSelectionMode(boolean enabled) {
        isSelectionMode = enabled;
        if (!enabled) {
            selectedFiles.clear();
            listener.onSelectionChanged(0);
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = fileList.get(position);

        Glide.with(context)
                .load(file)
                .into(holder.ivThumbnail);

        if (file.getName().endsWith(".mp4")) {
            holder.ivPlayIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivPlayIcon.setVisibility(View.GONE);
        }

        if (isSelectionMode) {
            if (selectedFiles.contains(file)) {
                holder.viewSelection.setVisibility(View.VISIBLE);
                holder.ivCheck.setVisibility(View.VISIBLE);
            } else {
                holder.viewSelection.setVisibility(View.GONE);
                holder.ivCheck.setVisibility(View.GONE);
            }
        } else {
            holder.viewSelection.setVisibility(View.GONE);
            holder.ivCheck.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(file);
            } else {
                listener.onItemClick(file);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(file);
                return true;
            }
            return false;
        });
    }

    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }
        notifyDataSetChanged();
        listener.onSelectionChanged(selectedFiles.size());
        
        if (selectedFiles.isEmpty()) {
            isSelectionMode = false;
        }
    }

    public void clearSelection() {
        selectedFiles.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    public Set<File> getSelectedFiles() {
        return new HashSet<>(selectedFiles);
    }

    public void removeFiles(List<File> filesToRemove) {
        fileList.removeAll(filesToRemove);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        ImageView ivPlayIcon;
        View viewSelection;
        ImageView ivCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);
            viewSelection = itemView.findViewById(R.id.viewSelection);
            ivCheck = itemView.findViewById(R.id.ivCheck);
        }
    }
}