package org.jdownloader.controlling.ffmpeg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    public static class MetaDataEntry extends LinkedHashMap<String, Object> {

        private final String type;

        public MetaDataEntry(final String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

    }

    private final Map<KEY, String>    values  = new LinkedHashMap<KEY, String>();

    private final List<MetaDataEntry> entries = new ArrayList<MetaDataEntry>();

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
        return values.size() == 0 && entries.size() == 0;
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

    public void addEntry(final MetaDataEntry entry) {
        if (entry != null && entry.size() > 0) {
            this.entries.add(entry);
        }
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

    private String toEscapedString(Object value) {
        if (value == null) {
            return "";
        }
        String ret = value.toString();
        ret = ret.replace("\n", "\r").replaceAll("(=|;|#|\r)", "\\\\$0").replaceAll("(\\\\[^=;#\r]{1})", "\\\\$0");
        return ret;
    }

    public String getFFmpegMetaData() {
        final StringBuilder ret = new StringBuilder();
        ret.append(";FFMETADATA1\n");
        for (final KEY key : KEY.values()) {
            final String value = getValue(key);
            if (value != null) {
                ret.append(key.getKey());
                ret.append("=");
                ret.append(toEscapedString(value));
                ret.append("\n");
            }
        }
        for (final MetaDataEntry metaDataEntry : entries) {
            final String type = metaDataEntry.getType();
            if (type != null && metaDataEntry.size() > 0) {
                ret.append("[" + type + "]");
                ret.append("\n");
                for (Entry<String, Object> entry : metaDataEntry.entrySet()) {
                    ret.append(entry.getKey());
                    ret.append("=");
                    ret.append(toEscapedString(entry.getValue()));
                    ret.append("\n");
                }

            }
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return getFFmpegMetaData();
    }

}
