package org.jdownloader.downloader.hls;

import java.util.ArrayList;

public class M3U8Playlist {

    public static class M3U8Segment {
        private final String url;

        private boolean      isLoaded = false;

        public boolean isLoaded() {
            return isLoaded;
        }

        public void setLoaded(boolean isLoaded) {
            this.isLoaded = isLoaded;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = Math.max(size, 0);
        }

        public String getUrl() {
            return url;
        }

        public long getDuration() {
            return duration;
        }

        private final long    duration;
        private volatile long size = -1;

        public M3U8Segment(final String url, long duration) {
            this.url = url;
            if (duration < 0) {
                this.duration = -1;
            } else {
                this.duration = duration;
            }
        }
    }

    public int addSegment(final String segmentURL, final long segmentDuration) {
        final M3U8Segment segment = new M3U8Segment(segmentURL, segmentDuration);
        return addSegment(segment);
    }

    private final ArrayList<M3U8Segment> segments = new ArrayList<M3U8Segment>();

    public boolean isSegmentLoaded(final int index) {
        if (index >= 0 && index < segments.size()) {
            return segments.get(index).isLoaded();
        } else {
            return false;
        }
    }

    public void setSegmentLoaded(final int index, boolean loaded) {
        if (index >= 0 && index < segments.size()) {
            segments.get(index).setLoaded(loaded);
        }
    }

    public void clearLoadedSegments() {
        for (final M3U8Segment segment : segments) {
            segment.setLoaded(false);
        }
    }

    public M3U8Segment getSegment(final int index) {
        if (index >= 0 && index < segments.size()) {
            return segments.get(index);
        } else {
            return null;
        }
    }

    public boolean containsSegmentURL(final String segmentURL) {
        for (final M3U8Segment segment : segments) {
            if (segmentURL.equals(segment.getUrl())) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return segments.size();
    }

    public int addSegment(M3U8Segment segment) {
        if (segment != null) {
            int index = segments.indexOf(segment);
            if (index == -1) {
                index = segments.size();
                segments.add(segment);
            }
            return index;
        }
        return -1;
    }

    public M3U8Segment setSegment(int index, M3U8Segment segment) {
        if (segment != null && index >= 0 && index < segments.size()) {
            return segments.set(index, segment);
        } else {
            return null;
        }
    }

    public long getEstimatedDuration() {
        long duration = 0;
        for (final M3U8Segment segment : segments) {
            duration += Math.max(0, segment.getDuration());
        }
        return duration;
    }

    public long getEstimatedSize() {
        long size = -1;
        long duration = 0;
        long unknownSizeDuration = 0;
        for (final M3U8Segment segment : segments) {
            if (segment.getSize() != -1) {
                size += segment.getSize();
                duration += segment.getDuration();
            } else {
                unknownSizeDuration += segment.getDuration();
            }
        }
        if (unknownSizeDuration == 0 && duration > 0) {
            return size + 1;
        } else if (size > 0 && duration > 0) {
            final double averagePerSecond = (Math.max(1, size)) / (duration / 1000d);
            return size + (long) (averagePerSecond * (unknownSizeDuration / 1000d));
        } else {
            return -1;
        }
    }

    public M3U8Segment removeSegment(int index) {
        if (index >= 0 && index < segments.size()) {
            return segments.remove(index);
        } else {
            return null;
        }
    }

}
