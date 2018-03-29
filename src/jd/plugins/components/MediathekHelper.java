package jd.plugins.components;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdownloader.plugins.components.config.MediathekProperties;

import jd.plugins.DownloadLink;

public class MediathekHelper {
    /* TODO: Ensure mp3 (audioonly) compatibility, put less info in filename if not required e.g. leave out protocol if we only have one */
    public static String getMediathekFilename(final DownloadLink dl, final MediathekProperties data, final boolean multipleProtocolsAvailable, final boolean sameResolutionWithDifferentBitratePossible) {
        final String title = data.getTitle();
        final String show = data.getShow();
        final String channel = data.getChannel();
        final String protocol = data.getProtocol();
        final String videoResolution = data.getResolution();
        final String date_formatted = formatDate(data.getReleaseDate());
        final long bitrateTotal = data.getBitrateTotal();
        /* TODO */
        String filename = "";
        if (date_formatted != null) {
            filename = date_formatted + "_";
        }
        filename += channel + "_";
        if (show != null) {
            filename += show + " - ";
        }
        filename += title + "_";
        if (multipleProtocolsAvailable) {
            filename += protocol + "_";
        }
        if (sameResolutionWithDifferentBitratePossible) {
            filename += bitrateTotal;
        }
        if (videoResolution != null) {
            filename += "_" + videoResolution;
        }
        /* Add extension */
        return filename;
    }

    public static String getMediathekFilename(final DownloadLink dl) {
        return getMediathekFilename(dl, dl.bindData(MediathekProperties.class), false, false);
    }

    public static String formatDate(final long date) {
        if (date <= 0) {
            return null;
        }
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = "date_error";
        }
        return formattedDate;
    }
}
