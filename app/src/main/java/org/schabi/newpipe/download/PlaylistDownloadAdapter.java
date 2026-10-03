package org.schabi.newpipe.download;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.Localization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistDownloadAdapter
        extends RecyclerView.Adapter<PlaylistDownloadAdapter.ViewHolder> {
    private final List<PlaylistItemDownloadEntry> items;
    private final Set<Integer> selectedIndices;
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int totalCount);
    }

    public PlaylistDownloadAdapter(@NonNull final List<PlaylistItemDownloadEntry> items) {
        this.items = items;
        this.selectedIndices = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            selectedIndices.add(i);
        }
    }

    public void setOnSelectionChangedListener(
            final OnSelectionChangedListener selectionListener) {
        this.listener = selectionListener;
    }

    public void selectAll(final boolean selectAll) {
        selectedIndices.clear();
        if (selectAll) {
            for (int i = 0; i < items.size(); i++) {
                selectedIndices.add(i);
            }
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedIndices.size(), items.size());
        }
    }

    public List<PlaylistItemDownloadEntry> getSelectedItems() {
        final List<PlaylistItemDownloadEntry> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (selectedIndices.contains(i)) {
                result.add(items.get(i));
            }
        }
        return result;
    }

    public boolean isAllSelected() {
        return !items.isEmpty() && selectedIndices.size() == items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.playlist_download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final PlaylistItemDownloadEntry item = items.get(position);
        holder.titleView.setText(item.getName());

        final String uploader = item.getUploaderName();
        final String durationStr = item.getDuration() >= 0
                ? Localization.getDurationString(item.getDuration()) : "";
        if (!uploader.isEmpty() && !durationStr.isEmpty()) {
            holder.detailsView.setText(uploader + " • " + durationStr);
        } else if (!uploader.isEmpty()) {
            holder.detailsView.setText(uploader);
        } else {
            holder.detailsView.setText(durationStr);
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedIndices.contains(position));

        final View.OnClickListener clickListener = v -> {
            final int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            if (selectedIndices.contains(pos)) {
                selectedIndices.remove(pos);
                holder.checkBox.setChecked(false);
            } else {
                selectedIndices.add(pos);
                holder.checkBox.setChecked(true);
            }
            if (listener != null) {
                listener.onSelectionChanged(selectedIndices.size(), items.size());
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.checkBox.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView titleView;
        final TextView detailsView;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.item_checkbox);
            titleView = itemView.findViewById(R.id.item_title);
            detailsView = itemView.findViewById(R.id.item_details);
        }
    }
}
