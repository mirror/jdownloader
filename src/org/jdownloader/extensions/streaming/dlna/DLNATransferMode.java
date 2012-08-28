package org.jdownloader.extensions.streaming.dlna;

public class DLNATransferMode {
    public static final String INTERACTIVE = "Interactive";
    public static final String BACKGROUND  = "Background";
    public static final String STREAMING   = "Streaming";

    public static String valueOf(String value) {
        if (value.equalsIgnoreCase(INTERACTIVE)) return INTERACTIVE;
        if (value.equalsIgnoreCase(BACKGROUND)) return BACKGROUND;
        if (value.equalsIgnoreCase(STREAMING)) return STREAMING;

        return null;
    }

}
