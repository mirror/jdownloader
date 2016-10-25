package org.jdownloader.controlling.ffmpeg;

import java.util.HashMap;

public class FFmpegMetaData {

    public static enum KEY {
        TITLE("title"),
        ARTIST("artist"),
        COMPOSER("composer"),
        ALBUM("album"),
        DATE("date"),
        TRACK("track"),
        COMMENT("comment"),
        DESCRIPTION("description"),
        SHOW("show");

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

    public boolean hasValues() {
        return values.size() > 0;
    }

    public String setValue(final KEY key, String value) {
        if (value == null) {
            return removeKey(key);
        } else {
            return values.put(key, value);
        }
    }

    public String removeKey(final KEY key) {
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

}
