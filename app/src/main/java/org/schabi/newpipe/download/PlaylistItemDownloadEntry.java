package org.schabi.newpipe.download;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.database.playlist.PlaylistStreamEntry;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.Serializable;

public class PlaylistItemDownloadEntry implements Parcelable, Serializable {
    private final int serviceId;
    private final String url;
    private final String name;
    private final String uploaderName;
    private final long duration;

    public PlaylistItemDownloadEntry(final int serviceId,
                                     @NonNull final String url,
                                     @NonNull final String name,
                                     @Nullable final String uploaderName,
                                     final long duration) {
        this.serviceId = serviceId;
        this.url = url;
        this.name = name;
        this.uploaderName = uploaderName != null ? uploaderName : "";
        this.duration = duration;
    }

    public static PlaylistItemDownloadEntry fromStreamInfoItem(
            @NonNull final StreamInfoItem item) {
        return new PlaylistItemDownloadEntry(
                item.getServiceId(),
                item.getUrl(),
                item.getName(),
                item.getUploaderName(),
                item.getDuration()
        );
    }

    public static PlaylistItemDownloadEntry fromPlaylistStreamEntry(
            @NonNull final PlaylistStreamEntry item) {
        return new PlaylistItemDownloadEntry(
                item.getStreamEntity().getServiceId(),
                item.getStreamEntity().getUrl(),
                item.getStreamEntity().getTitle(),
                item.getStreamEntity().getUploader(),
                item.getStreamEntity().getDuration()
        );
    }

    protected PlaylistItemDownloadEntry(final Parcel in) {
        serviceId = in.readInt();
        url = in.readString();
        name = in.readString();
        uploaderName = in.readString();
        duration = in.readLong();
    }

    public static final Creator<PlaylistItemDownloadEntry> CREATOR =
            new Creator<PlaylistItemDownloadEntry>() {
                @Override
                public PlaylistItemDownloadEntry createFromParcel(final Parcel in) {
                    return new PlaylistItemDownloadEntry(in);
                }

                @Override
                public PlaylistItemDownloadEntry[] newArray(final int size) {
                    return new PlaylistItemDownloadEntry[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(serviceId);
        dest.writeString(url);
        dest.writeString(name);
        dest.writeString(uploaderName);
        dest.writeLong(duration);
    }

    public int getServiceId() {
        return serviceId;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public long getDuration() {
        return duration;
    }
}
