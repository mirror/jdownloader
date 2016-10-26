package org.jdownloader.controlling.ffmpeg;

import java.util.Calendar;
import java.util.HashMap;

public class FFmpegMetaData {

    public static enum KEY {
        TITLE("title"),
        ARTIST("artist"),
        ALBUM("album"),
        COMMENT("comment"),
        DATE("date");

        private final String key;

        private KEY(final String key) {
            this.key = key;
        }

        public final String getKey() {
            return key;
        }
    }

    private final HashMap<KEY, String> values = new HashMap<KEY, String>();

    public String getValue(final KEY key) {
        return values.get(key);
    }

    public String setYear(Calendar year) {
        if (year != null) {
            return setValue(KEY.DATE, String.valueOf(year.get(Calendar.YEAR)));
        } else {
            return setValue(KEY.DATE, null);
        }
    }

    public String getYear() {
        return getValue(KEY.DATE);
    }

    public boolean isEmpty() {
        return values.size() == 0;
    }

    public String setComment(final String comment) {
        return setValue(KEY.COMMENT, comment);
    }

    public String getComment() {
        return getValue(KEY.COMMENT);
    }

    public String setArtist(final String artist) {
        return setValue(KEY.ARTIST, artist);
    }

    public String getArtist() {
        return getValue(KEY.ARTIST);
    }

    public String setTitle(final String title) {
        return setValue(KEY.TITLE, title);
    }

    public String getTitle() {
        return getValue(KEY.TITLE);
    }

    protected String setValue(final KEY key, String value) {
        if (value == null) {
            return removeKey(key);
        } else {
            return values.put(key, value);
        }
    }

    protected String removeKey(final KEY key) {
        return values.remove(key);
    }

    public String getFFmpegMetaData() {
        final StringBuilder ret = new StringBuilder();
        ret.append(";FFMETADATA1\n");
        for (final KEY key : KEY.values()) {
            final String value = getValue(key);
            if (value != null) {
                ret.append(key.getKey());
                ret.append("=");
                ret.append(value.replace("\r", "\n").replaceAll("(=|;|#|\r)", "\\\\$0").replaceAll("(\\\\[^=;#\r]{1})", "\\\\$0"));
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return getFFmpegMetaData();
    }

}
