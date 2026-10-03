package org.schabi.newpipe.download;

import static org.schabi.newpipe.extractor.stream.DeliveryMethod.PROGRESSIVE_HTTP;
import static org.schabi.newpipe.util.ListHelper.getStreamsOfSpecifiedDelivery;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.streams.io.StoredDirectoryHelper;
import org.schabi.newpipe.streams.io.StoredFileHelper;
import org.schabi.newpipe.util.ExtractorHelper;
import org.schabi.newpipe.util.FilenameUtils;
import org.schabi.newpipe.util.ListHelper;
import org.schabi.newpipe.util.SecondaryStreamHelper;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import us.shandian.giga.get.MissionRecoveryInfo;
import us.shandian.giga.postprocessing.Postprocessing;
import us.shandian.giga.service.DownloadManagerService;

public final class PlaylistDownloadHelper {
    private static final String TAG = "PlaylistDownloadHelper";

    private PlaylistDownloadHelper() {
    }

    public static void downloadPlaylistItems(@NonNull final Context context,
                                             @NonNull final List<PlaylistItemDownloadEntry> items,
                                             final String playlistTitle,
                                             final int qualityOptionIndex,
                                             final boolean startPaused,
                                             final StoredDirectoryHelper mainStorageVideo,
                                             final StoredDirectoryHelper mainStorageAudio) {
        if (items.isEmpty()) {
            return;
        }

        final boolean isAudioOption = qualityOptionIndex >= 6;
        final StoredDirectoryHelper baseStorage = isAudioOption
                ? mainStorageAudio : mainStorageVideo;

        if (baseStorage == null) {
            Toast.makeText(context, R.string.no_dir_yet, Toast.LENGTH_LONG).show();
            return;
        }

        final boolean hasTitle = playlistTitle != null && !playlistTitle.isEmpty();
        final StoredDirectoryHelper playlistStorage = hasTitle
                ? baseStorage.createSubdirectory(playlistTitle)
                : baseStorage;

        final String toastMsg = context.getString(
                startPaused ? R.string.download_added_to_queue : R.string.download_has_started);
        Toast.makeText(context, toastMsg + " (" + items.size() + ")", Toast.LENGTH_SHORT).show();

        for (final PlaylistItemDownloadEntry entry : items) {
            ExtractorHelper.getStreamInfo(entry.getServiceId(), entry.getUrl(), false)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            info -> processAndStartDownload(context, info, playlistTitle,
                                    qualityOptionIndex, startPaused, playlistStorage),
                            throwable -> Log.e(TAG, "Error fetching info for "
                                    + entry.getUrl(), throwable)
                    );
        }
    }

    private static void processAndStartDownload(@NonNull final Context context,
                                                @NonNull final StreamInfo info,
                                                final String playlistTitle,
                                                final int qualityOptionIndex,
                                                final boolean startPaused,
                                                @NonNull final StoredDirectoryHelper mainStorage) {
        try {
            final Stream selectedStream;
            Stream secondaryStream = null;
            final char kind;
            String psName = null;
            final long nearLength = 0;

            final List<AudioStream> audioStreams =
                    getStreamsOfSpecifiedDelivery(info.getAudioStreams(), PROGRESSIVE_HTTP);

            if (qualityOptionIndex >= 6) {
                // Audio options
                kind = 'a';
                selectedStream = selectAudioStream(audioStreams, qualityOptionIndex);
                if (selectedStream == null) {
                    return;
                }

                if (selectedStream.getFormat() == MediaFormat.M4A) {
                    psName = Postprocessing.ALGORITHM_M4A_NO_DASH;
                } else if (selectedStream.getFormat() == MediaFormat.WEBMA_OPUS) {
                    psName = Postprocessing.ALGORITHM_OGG_FROM_WEBM_DEMUXER;
                }
            } else {
                // Video options
                kind = 'v';
                final List<VideoStream> videoStreams = ListHelper.getSortedStreamVideosList(
                        context,
                        getStreamsOfSpecifiedDelivery(info.getVideoStreams(), PROGRESSIVE_HTTP),
                        getStreamsOfSpecifiedDelivery(info.getVideoOnlyStreams(),
                                PROGRESSIVE_HTTP),
                        false,
                        false
                );

                selectedStream = selectVideoStream(videoStreams, qualityOptionIndex);
                if (selectedStream == null) {
                    return;
                }

                if (selectedStream instanceof VideoStream
                        && ((VideoStream) selectedStream).isVideoOnly()) {
                    secondaryStream = SecondaryStreamHelper.getAudioStreamFor(context, audioStreams,
                            (VideoStream) selectedStream);
                    if (secondaryStream != null) {
                        if (selectedStream.getFormat() == MediaFormat.MPEG_4) {
                            psName = Postprocessing.ALGORITHM_MP4_FROM_DASH_MUXER;
                        } else {
                            psName = Postprocessing.ALGORITHM_WEBM_MUXER;
                        }
                    }
                }
            }

            String displayTitle = info.getName();
            if (playlistTitle != null && !playlistTitle.isEmpty()) {
                displayTitle = "[" + playlistTitle + "] " + info.getName();
            }

            String filename = FilenameUtils.createFilename(context, displayTitle) + ".";
            final String mime;
            final MediaFormat format = selectedStream.getFormat();
            if (format != null) {
                mime = format.mimeType;
                if (format == MediaFormat.WEBMA_OPUS && kind == 'a') {
                    filename += "opus";
                } else {
                    filename += format.getSuffix();
                }
            } else {
                mime = "video/*";
                filename += "mp4";
            }

            if (!mainStorage.mkdirs()) {
                return;
            }

            final StoredFileHelper storage = mainStorage.createUniqueFile(filename, mime);
            if (storage == null || !storage.canWrite()) {
                return;
            }

            final String[] urls;
            final List<MissionRecoveryInfo> recoveryInfo;
            if (secondaryStream == null) {
                urls = new String[]{selectedStream.getContent()};
                recoveryInfo = List.of(new MissionRecoveryInfo(selectedStream));
            } else {
                urls = new String[]{selectedStream.getContent(), secondaryStream.getContent()};
                recoveryInfo = List.of(
                        new MissionRecoveryInfo(selectedStream),
                        new MissionRecoveryInfo(secondaryStream)
                );
            }

            DownloadManagerService.startMission(context, urls, storage, kind, 3,
                    info, psName, null, nearLength, new ArrayList<>(recoveryInfo), startPaused);
        } catch (final Exception e) {
            Log.e(TAG, "Error starting playlist item download for " + info.getName(), e);
        }
    }

    private static AudioStream selectAudioStream(final List<AudioStream> streams,
                                                  final int optionIndex) {
        if (streams.isEmpty()) {
            return null;
        }
        if (optionIndex == 7) { // M4A
            for (final AudioStream s : streams) {
                if (s.getFormat() == MediaFormat.M4A) {
                    return s;
                }
            }
        } else if (optionIndex == 8) { // WebM
            for (final AudioStream s : streams) {
                if (s.getFormat() == MediaFormat.WEBMA_OPUS) {
                    return s;
                }
            }
        }
        return streams.get(0);
    }

    private static VideoStream selectVideoStream(final List<VideoStream> streams,
                                                  final int optionIndex) {
        if (streams.isEmpty()) {
            return null;
        }
        if (optionIndex == 0) { // Best
            return streams.get(0);
        }
        if (optionIndex == 5) { // Lowest
            return streams.get(streams.size() - 1);
        }

        final String targetRes;
        switch (optionIndex) {
            case 1:
                targetRes = "1080p";
                break;
            case 2:
                targetRes = "720p";
                break;
            case 3:
                targetRes = "480p";
                break;
            case 4:
                targetRes = "360p";
                break;
            default:
                targetRes = null;
                break;
        }

        if (targetRes != null) {
            for (final VideoStream s : streams) {
                if (s.getResolution() != null && s.getResolution().contains(targetRes)) {
                    return s;
                }
            }
        }

        return streams.get(0);
    }
}
