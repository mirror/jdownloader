package jd.gui.swing.jdgui;

import jd.plugins.DownloadLink;

public class DownloadFeedBack extends DirectFeedback {

    private DownloadLink downloadLink;

    public DownloadFeedBack(boolean positive, DownloadLink downloadLink) {
        super(positive);
        this.downloadLink = downloadLink;
    }

}
