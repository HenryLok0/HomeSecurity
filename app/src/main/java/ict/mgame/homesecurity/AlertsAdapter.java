package ict.mgame.homesecurity;

import android.net.Uri;
import com.bumptech.glide.Glide;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.VH> {
    private final List<AlertsActivity.AlertItem> items;
    private final View.OnClickListener clickListener;

    public AlertsAdapter(List<AlertsActivity.AlertItem> items, View.OnClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.alert_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AlertsActivity.AlertItem it = items.get(position);
        holder.tvMessage.setText(it.message);
        holder.tvTime.setText(it.time == null || it.time.isEmpty() ? "" : it.time);
        if (it.uri != null && !it.uri.isEmpty() && !it.uri.equals("null")) {
            try {
                Glide.with(holder.ivThumb.getContext())
                        .load(Uri.parse(it.uri))
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivThumb);
            } catch (Exception e) {
                holder.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        holder.itemView.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvMessage;
        TextView tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivAlertThumb);
            tvMessage = itemView.findViewById(R.id.tvAlertMessage);
            tvTime = itemView.findViewById(R.id.tvAlertTime);
        }
    }
}
