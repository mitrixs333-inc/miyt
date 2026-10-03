package org.schabi.newpipe.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.streams.io.StoredDirectoryHelper;
import org.schabi.newpipe.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import us.shandian.giga.service.DownloadManagerService;
import us.shandian.giga.service.DownloadManagerService.DownloadManagerBinder;

public class PlaylistDownloadDialog extends DialogFragment {
    private static final String KEY_ITEMS = "key_items";

    private List<PlaylistItemDownloadEntry> items = new ArrayList<>();
    private PlaylistDownloadAdapter adapter;

    private StoredDirectoryHelper mainStorageAudio = null;
    private StoredDirectoryHelper mainStorageVideo = null;

    private Spinner qualitySpinner;
    private CheckBox selectAllCheckBox;
    private TextView selectedCountText;
    private Button addButton;
    private Button downloadButton;

    public static PlaylistDownloadDialog newInstance(
            @NonNull final List<PlaylistItemDownloadEntry> items) {
        final PlaylistDownloadDialog dialog = new PlaylistDownloadDialog();
        final Bundle args = new Bundle();
        args.putParcelableArrayList(KEY_ITEMS, new ArrayList<>(items));
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, ThemeHelper.getDialogTheme(requireContext()));

        if (getArguments() != null) {
            final ArrayList<PlaylistItemDownloadEntry> list =
                    getArguments().getParcelableArrayList(KEY_ITEMS);
            if (list != null) {
                items = list;
            }
        }

        final Intent intent = new Intent(requireContext(), DownloadManagerService.class);
        requireContext().startService(intent);
        requireContext().bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                final DownloadManagerBinder binder = (DownloadManagerBinder) service;
                mainStorageAudio = binder.getMainStorageAudio();
                mainStorageVideo = binder.getMainStorageVideo();
                requireContext().unbindService(this);
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_download_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.download_playlist);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> dismiss());

        qualitySpinner = view.findViewById(R.id.quality_spinner);
        selectAllCheckBox = view.findViewById(R.id.select_all_checkbox);
        selectedCountText = view.findViewById(R.id.selected_count_text);
        addButton = view.findViewById(R.id.add_button);
        downloadButton = view.findViewById(R.id.download_button);

        setupQualitySpinner();

        final RecyclerView recyclerView = view.findViewById(R.id.items_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlaylistDownloadAdapter(items);
        recyclerView.setAdapter(adapter);

        updateCountText(adapter.getSelectedItems().size(), items.size());

        adapter.setOnSelectionChangedListener((selectedCount, totalCount) -> {
            updateCountText(selectedCount, totalCount);
            selectAllCheckBox.setOnCheckedChangeListener(null);
            selectAllCheckBox.setChecked(adapter.isAllSelected());
            setupSelectAllListener();
        });

        setupSelectAllListener();

        downloadButton.setOnClickListener(v -> startDownload(false));
        addButton.setOnClickListener(v -> startDownload(true));
    }

    private void setupQualitySpinner() {
        final String[] qualityOptions = new String[] {
                "Best Video Quality",
                "1080p Video",
                "720p Video",
                "480p Video",
                "360p Video",
                "Lowest Video Quality",
                "Audio Only (Best Quality)",
                "Audio Only (M4A)",
                "Audio Only (WebM)"
        };
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, qualityOptions);
        qualitySpinner.setAdapter(spinnerAdapter);
    }

    private void setupSelectAllListener() {
        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                adapter.selectAll(isChecked));
    }

    private void updateCountText(final int selected, final int total) {
        selectedCountText.setText(selected + " / " + total + " selected");
        final boolean enable = selected > 0;
        downloadButton.setEnabled(enable);
        addButton.setEnabled(enable);
    }

    private void startDownload(final boolean startPaused) {
        final List<PlaylistItemDownloadEntry> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_streams_available_download,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        PlaylistDownloadHelper.downloadPlaylistItems(
                requireContext(),
                selected,
                qualitySpinner.getSelectedItemPosition(),
                startPaused,
                mainStorageVideo,
                mainStorageAudio
        );

        dismiss();
    }
}
