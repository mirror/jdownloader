package org.jdownloader.controlling;

import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;

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
        String name = link.getCustomFinalName();
        if (StringUtils.isNotEmpty(name)) {
            //
            return name;
        }
        return link.getName();
    }

}
