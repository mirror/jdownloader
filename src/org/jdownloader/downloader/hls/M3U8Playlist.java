package org.jdownloader.downloader.hls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.http.Browser;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

public class M3U8Playlist {

    public static class M3U8Segment {

        public static final String toExtInfDuration(final long duration) {
            final String value = Long.toString(duration);
            switch (value.length()) {
            case 0:
                return "0.000";
            case 1:
                return "0.00".concat(value);
            case 2:
                return "0.0".concat(value);
            case 3:
                return "0.".concat(value);
            default:
                return value.replaceFirst("(\\d{3})$", ".$1").replaceFirst("^\\.", "0.");
            }
        }

        public static long fromExtInfDuration(String duration) {
            duration = new Regex(duration, "#EXTINF:(\\d+(\\.\\d+)?)").getMatch(0);
            if (duration != null) {
                if (duration.contains(".")) {
                    return Long.parseLong(duration.replace(".", ""));
                } else {
                    return Long.parseLong(duration) * 1000;
                }
            }
            return -1;
        }

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

    @Override
    public String toString() {
        return "M3U8:Encrypted:" + isEncrypted() + "|Segments:" + size() + "|Duration:" + getEstimatedDuration() + "ms|Estimated Size:" + getEstimatedSize();
    }

    public static List<M3U8Playlist> loadM3U8(final String m3u8, final Browser br) throws IOException {
        final List<M3U8Playlist> ret = new ArrayList<M3U8Playlist>();
        M3U8Playlist current = new M3U8Playlist();
        long lastSegmentDuration = -1;
        for (final String line : Regex.getLines(br.getPage(m3u8))) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                if (current != null && current.size() > 0) {
                    ret.add(current);
                }
                M3U8Playlist before = current;
                current = new M3U8Playlist();
                current.setEncrypted(before.isEncrypted);
                current.setExtTargetDuration(before.getExtTargetDuration());
                lastSegmentDuration = -1;
            }
            if (StringUtils.startsWithCaseInsensitive(line, "concat") || StringUtils.contains(line, "file:")) {
                // http://habrahabr.ru/company/mailru/blog/274855/
            } else if (line.matches("^https?://.+") || !line.trim().startsWith("#")) {
                final String segmentURL = br.getURL(line).toString();
                if (!current.containsSegmentURL(segmentURL)) {
                    current.addSegment(segmentURL, lastSegmentDuration);
                }
                lastSegmentDuration = -1;
            } else {
                if (line.startsWith("#EXTINF:")) {
                    lastSegmentDuration = M3U8Segment.fromExtInfDuration(line);
                } else if (line.startsWith("#EXT-X-KEY")) {
                    current.setEncrypted(true);
                } else if (line.startsWith("#EXT-X-TARGETDURATION")) {
                    final String targetDuration = new Regex(line, "#EXT-X-TARGETDURATION:(\\d+)").getMatch(0);
                    if (targetDuration != null) {
                        current.setExtTargetDuration(Long.parseLong(targetDuration));
                    }
                }
            }
        }
        if (current != null && current.size() > 0) {
            ret.add(current);
        }
        return ret;
    }

    public int addSegment(final String segmentURL, final long segmentDuration) {
        final M3U8Segment segment = new M3U8Segment(segmentURL, segmentDuration);
        return addSegment(segment);
    }

    protected final ArrayList<M3U8Segment> segments       = new ArrayList<M3U8Segment>();

    protected boolean                      isEncrypted    = false;
    protected long                         targetDuration = -1;

    public long getTargetDuration() {
        long ret = -1;
        for (final M3U8Segment segment : segments) {
            ret = Math.max(segment.getDuration(), ret);
        }
        return Math.max(ret, getExtTargetDuration());
    }

    public long getExtTargetDuration() {
        return targetDuration;
    }

    public void setExtTargetDuration(long targetDuration) {
        if (targetDuration <= 0) {
            this.targetDuration = -1;
        } else {
            this.targetDuration = targetDuration;
        }
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

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

    public static long getEstimatedSize(List<M3U8Playlist> list) {
        long size = -1;
        long duration = 0;
        long unknownSizeDuration = 0;
        if (list != null) {
            for (M3U8Playlist playList : list) {
                for (final M3U8Segment segment : playList.segments) {
                    if (segment.getSize() != -1) {
                        size += segment.getSize();
                        duration += segment.getDuration();
                    } else {
                        unknownSizeDuration += segment.getDuration();
                    }
                }
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
