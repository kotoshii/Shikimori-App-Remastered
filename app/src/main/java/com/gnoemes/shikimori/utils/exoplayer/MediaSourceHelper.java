package com.gnoemes.shikimori.utils.exoplayer;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gnoemes.shikimori.entity.series.domain.VideoFormat;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;

import java.util.ArrayList;
import java.util.List;

public  class MediaSourceHelper {

    private DataSource.Factory factory;
    private VideoFormat format;
    private MediaSource videoSource;
    private MediaSource audioSource;
    private MediaSource subtitlesSource;
    private List<MediaSource> videoSources;

    public MediaSourceHelper(DataSource.Factory factory) {
        this.factory = factory;
        videoSources = new ArrayList<>();
    }

    public static MediaSourceHelper withFactory(DataSource.Factory factory) {
        return new MediaSourceHelper(factory);
    }

    public MediaSourceHelper withFormat(VideoFormat format) {
        this.format = format;
        return this;
    }

    public MediaSourceHelper withVideoUrls(@NonNull String... urls) {
        for (String url : urls) {
            if (!TextUtils.isEmpty(url)) {
                videoSources.add(getMediaSourceFactory()
                        .createMediaSource(Uri.parse(url)));
            }
        }

        videoSource = new ConcatenatingMediaSource(videoSources.toArray(new MediaSource[videoSources.size()]));
        return this;
    }

    public MediaSourceHelper withVideoUrl(@NonNull String url) {
        if (!TextUtils.isEmpty(url)) {
            videoSource = getMediaSourceFactory()
                    .createMediaSource(Uri.parse(url));
        }
        return this;
    }

    /**
     * Some hostings ship video and sound as separate files, so the two are merged back together.
     * Always an mp4, whatever container the video itself uses.
     */
    public MediaSourceHelper withAudioUrl(@Nullable String url) {
        if (TextUtils.isEmpty(url)) return this;

        audioSource = new ExtractorMediaSource.Factory(factory)
                .createMediaSource(Uri.parse(url));
        return this;
    }

    public MediaSourceHelper withSubtitles(@Nullable String url, Format format) {
        if (url == null) return this;

        subtitlesSource = new SingleSampleMediaSource.Factory(factory)
                .createMediaSource(Uri.parse(url), format, C.TIME_UNSET);
        return this;
    }

    public MediaSource get() {
        List<MediaSource> sources = new ArrayList<>();
        sources.add(videoSource);
        if (audioSource != null) sources.add(audioSource);
        if (subtitlesSource != null) sources.add(subtitlesSource);

        if (sources.size() == 1) return videoSource;
        else return new MergingMediaSource(sources.toArray(new MediaSource[sources.size()]));
    }

    private AdsMediaSource.MediaSourceFactory getMediaSourceFactory() {
        switch (format) {
            case MP4:
                return new ExtractorMediaSource.Factory(factory);
            case HLS:
                return new HlsMediaSource.Factory(factory);
            case DASH:
                return new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(factory), factory);
            default:
                throw new IllegalArgumentException(format.getType() + " format not implemented");
        }
    }
}
