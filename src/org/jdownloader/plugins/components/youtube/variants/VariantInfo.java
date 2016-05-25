package org.jdownloader.plugins.components.youtube.variants;

import org.jdownloader.plugins.components.youtube.StreamCollection;

public class VariantInfo implements Comparable<VariantInfo> {

    private final AbstractVariant variant;
    final StreamCollection        audioStream;
    final StreamCollection        videoStream;

    public StreamCollection getAudioStreams() {
        return audioStream;
    }

    public StreamCollection getVideoStreams() {
        return videoStream;
    }

    public String          special = "";
    final StreamCollection dataStreams;

    public StreamCollection getDataStreams() {
        return dataStreams;
    }

    @Override
    public String toString() {
        return variant.toString();
    }

    public boolean isValid() {
        if (variant == null) {
            return false;
        }
        if (variant.getBaseVariant() == null) {
            return false;
        }
        if (variant.getBaseVariant().getiTagAudio() != null) {
            if (audioStream == null || audioStream.size() == 0) {
                return false;
            }
        }
        if (variant.getBaseVariant().getiTagVideo() != null) {
            if (videoStream == null || videoStream.size() == 0) {
                return false;
            }
        }
        if (variant.getBaseVariant().getiTagData() != null) {
            if (dataStreams == null || dataStreams.size() == 0) {
                return false;
            }
        }
        return true;
    }

    public VariantInfo(AbstractVariant v, StreamCollection audio, StreamCollection video, StreamCollection data) {
        this.variant = v;
        this.audioStream = audio;
        this.videoStream = video;
        this.dataStreams = data;
        v.setVariantInfo(this);
    }

    public AbstractVariant getVariant() {
        return variant;
    }

    // public void fillExtraProperties(DownloadLink thislink, List<VariantInfo> alternatives) {
    // }

    @Override
    public int compareTo(VariantInfo o) {

        return variant.compareTo(o.variant);
    }

    public boolean hasDefaultSegmentsStream() {
        StreamCollection audio = getAudioStreams();
        if (audio != null && audio.size() > 0) {
            if (audio.get(0).getSegments() != null) {
                return true;
            }
        }
        StreamCollection video = getVideoStreams();
        if (video != null && video.size() > 0) {
            if (video.get(0).getSegments() != null) {
                return true;
            }
        }
        return false;
    }

}