package org.jdownloader.controlling;

import jd.plugins.DownloadLink;

public interface DownloadLinkView {

    long getBytesLoaded();

    void setLink(DownloadLink downloadLink);

    long getSpeedBps();

    long[] getChunksProgress();

    /**
     * 
     * @return this method may return the final filesize, or the estimated final filesize, or {@link #getBytesLoaded()} or -1
     * @see #getBytesTotal()
     * @see #getBytesTotalVerified()
     */
    long getBytesTotalEstimated();

    /**
     * 
     * @return this method returns the known filesize or -1. The value is not verified. That means it is probably correct, but it may vary for a few bytes or
     *         kilobytes.
     * @see #getBytesTotalEstimated()
     * @see #getBytesTotalVerified()
     */
    long getBytesTotal();

    /**
     * 
     * @return this method returns either the 100% correct final filsize, or -1 * @see #getBytesTotalEstimated()
     * @see #getBytesTotal()
     */
    long getBytesTotalVerified();

    long getDownloadTime();

}
