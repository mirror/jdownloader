package org.jdownloader.extensions.streaming.mediainfo;

import java.util.HashMap;

import org.appwork.utils.formatter.SizeFormatter;

public class MediaInfoTrack extends HashMap<String, String> {

    private String trackType;

    public MediaInfoTrack(String trackType) {

        this.trackType = trackType;
    }

    public long getContentLength() {
        String length = get("File_size");
        try {
            return SizeFormatter.getSize(length);

        } catch (Throwable e) {
            return -1;
        }
    }

    public long getBitrate() {
        try {

            return Long.parseLong(get("Overall_bit_rate").replaceAll("\\D", "")) * 1024;
        } catch (Throwable e) {
            return -1;
        }
    }

    public String getFormat() {
        return get("Format");
    }

    public String getUniqueID() {
        String best = get("Unique_ID");
        if (best == null) return null;
        int i = best.indexOf(" ");
        if (i > 0) {
            return best.substring(0, i);
        } else {
            return best;
        }

    }

    public long getDuration() {
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        String value = get("Duration");
        if (value == null) return -1;
        int ih = value.indexOf("h");
        if (ih > 0) {
            hours = Integer.parseInt(value.substring(0, ih));
            ih += 2;
        } else {
            ih = 0;
        }

        int im = value.indexOf("mn", ih);
        if (im > 0) {
            minutes = Integer.parseInt(value.substring(ih, im));
            im += 3;
        } else {
            im = 0;
        }

        int is = value.indexOf("s", im);
        if (is > 0) {
            seconds = Integer.parseInt(value.substring(im, is));
        }
        // System.out.println(TimeFormatter.formatMilliSeconds(3300000, 0));
        return seconds * 1000l + minutes * 60 * 1000l + hours * 60 * 60 * 1000l;
    }

}
