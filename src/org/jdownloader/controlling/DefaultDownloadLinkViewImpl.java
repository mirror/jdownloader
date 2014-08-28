package org.jdownloader.controlling;

import jd.plugins.DownloadLink;

import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DefaultDownloadLinkViewImpl implements DownloadLinkView {

    protected DownloadLink link;

    public DefaultDownloadLinkViewImpl() {

    }

    public long getBytesLoaded() {
        return link.getDownloadCurrent();
    }

    public void setLink(DownloadLink downloadLink) {
        this.link = downloadLink;
    }

    /**
     * returns the estimated total filesize. This may not be accurate. sometimes this method even returns the same as
     * {@link #getBytesLoaded()} use {@link #getBytesTotal()} if you need a value that is either 0 or the
     */
    public long getBytesTotalEstimated() {
        return link.getDownloadSize();
    }

    public long getBytesTotal() {
        return link.getKnownDownloadSize();
    }

    public long getSpeedBps() {
        return link.getDownloadSpeed();
    }

    public long[] getChunksProgress() {
        return link.getChunksProgress();
    }

    @Override
    public long getBytesTotalVerified() {
        return link.getVerifiedFileSize();
    }

    @Override
    public long getDownloadTime() {
        return link.getDownloadTime();
    }

    @Override
    public String getDisplayName() {
        //
        return link.getName();
    }

    @Override
    public String getDownloadUrl() {
        // http://board.jdownloader.org/showpost.php?p=305216&postcount=12
        if (CFG_GUI.SHOW_BROWSER_URL_IF_POSSIBLE) {
            if (link.hasBrowserUrl()) {
                return link.getBrowserUrl();
            } else {
                return link.getDownloadURL();

            }
        }
        // Workaround for links added before this change
        if (1409057525234l - link.getCreated() > 0) {
            if (link.hasBrowserUrl()) {
                return link.getBrowserUrl();
            } else {
                return link.getDownloadURL();
            }
        }
        switch (link.getUrlProtection()) {
        case PROTECTED_CONTAINER:
        case PROTECTED_DECRYPTER:
            if (link.hasBrowserUrl()) {
                return link.getBrowserUrl();
            } else {
                return null;
            }

        case PROTECTED_INTERNAL_URL:
            if (link.hasBrowserUrl()) {
                return link.getBrowserUrl();
            }

        default:
            return link.getDownloadURL();
        }

    }

}
