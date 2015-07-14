package jd.plugins.components;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BrightcoveClipData {

    public String ext = ".mp4";
    public String displayName;
    public String shortDescription;
    public String publisherName;
    public String videoCodec;
    public String downloadurl;
    public long   width;
    public long   height;
    public long   length;
    public long   creationDate;
    public long   encodingRate;
    public long   size;
    public long   mediaDeliveryType;

    public BrightcoveClipData() {
        //
    }

    @Override
    public String toString() {
        return displayName + "_" + width + "x" + height;
    }

    public String getStandardFilename() {
        return formatDate(creationDate) + "_" + encodeUnicode(publisherName) + "_" + encodeUnicode(displayName) + "_" + width + "x" + height + "_" + videoCodec + ext;
    }

    private String formatDate(final long date) {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date);
        }
        return formattedDate;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

}