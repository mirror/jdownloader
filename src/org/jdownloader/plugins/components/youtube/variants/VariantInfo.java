package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;

import org.jdownloader.plugins.components.youtube.YoutubeStreamData;

public class VariantInfo implements Comparable<VariantInfo> {

    private final AbstractVariant variant;
    final List<YoutubeStreamData> audioStream;
    final List<YoutubeStreamData> videoStream;

    public List<YoutubeStreamData> getAudioStreams() {
        return audioStream;
    }

    public List<YoutubeStreamData> getVideoStreams() {
        return videoStream;
    }

    public String                 special = "";
    final List<YoutubeStreamData> dataStreams;

    public List<YoutubeStreamData> getDataStreams() {
        return dataStreams;
    }

    @Override
    public String toString() {
        return variant.toString();
    }

    public VariantInfo(AbstractVariant v, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
        this.variant = v;
        this.audioStream = audio;
        this.videoStream = video;
        this.dataStreams = data;

    }

    public AbstractVariant getVariant() {
        return variant;
    }

    // public void fillExtraProperties(DownloadLink thislink, List<VariantInfo> alternatives) {
    // }

    @Override
    public int compareTo(VariantInfo o) {
        return new Double(o.variant.getQualityRating()).compareTo(new Double(variant.getQualityRating()));
    }

}