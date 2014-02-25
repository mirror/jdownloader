package jd.gui.swing.jdgui;

import jd.plugins.DownloadLink;

public class DownloadFeedBack extends DirectFeedback {

    private DownloadLink downloadLink;

    public DownloadFeedBack(DownloadLink downloadLink) {
        super();
        this.downloadLink = downloadLink;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
    }

}
