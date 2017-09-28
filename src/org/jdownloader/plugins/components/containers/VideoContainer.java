package org.jdownloader.plugins.components.containers;

import java.util.Locale;

import jd.plugins.Plugin;

/**
 * Base container for all video information. Extend when you want further features.
 *
 * @author raztoki
 *
 */
public class VideoContainer {

    private String downloadurl = null;
    private String extension   = null;
    private int    width       = -1;
    private int    height      = -1;
    private int    framerate   = -1;
    private int    bitrate     = -1;
    private long   filesize    = -1;
    // used for storing the HLSContiner/Other as historic reference
    private Object container;

    /**
     * @return the downloadurl
     */
    public final String getDownloadurl() {
        return downloadurl;
    }

    /**
     * @return the extension
     */
    public final String getExtension() {
        return extension;
    }

    /**
     * @return the width
     */
    public final int getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public final int getHeight() {
        return height;
    }

    /**
     * @return the framerate
     */
    public final int getFramerate() {
        return framerate;
    }

    /**
     * @return the bitrate
     */
    public final int getBitrate() {
        return bitrate;
    }

    /**
     * @return the filesize
     */
    public final long getFilesize() {
        return filesize;
    }

    /**
     * @param downloadurl
     *            the downloadurl to set
     */
    public final void setDownloadurl(String downloadurl) {
        this.downloadurl = downloadurl;
    }

    public final void setDownloadurlAndExtension(final String downloadurl, final String extention) {
        setDownloadurl(downloadurl);
        setExtension(extention);
    }

    public final void setDownloadurlAndExtension(final String downloadurl) {
        setDownloadurl(downloadurl);
        setExtensionFromUrl(downloadurl);
    }

    /**
     * set the extension based on current
     */
    public final void setExtension() {
        if (this.downloadurl != null) {
            setExtensionFromUrl(this.downloadurl);
        }
    }

    /**
     * @param extension
     *            the extension to set
     */
    public final void setExtension(String extension) {
        this.extension = extension != null ? extension.toLowerCase(Locale.ENGLISH) : null;
    }

    public final void setExtensionFromUrl(final String url) {
        this.extension = Plugin.getFileNameExtensionFromString(url);
    }

    /**
     * @param width
     *            the width to set
     */
    public final void setWidth(int width) {
        this.width = width;
    }

    /**
     * @param height
     *            the height to set
     */
    public final void setHeight(int height) {
        this.height = height;
    }

    /**
     * @param framerate
     *            the framerate to set
     */
    public final void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    /**
     * @param bitrate
     *            the bitrate to set
     */
    public final void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    /**
     * @param filesize
     *            the filesize to set
     */
    public final void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    /**
     * @return the container
     */
    public Object getContainer() {
        return container;
    }

    /**
     * @param container
     *            the container to set
     */
    public void setContainer(Object container) {
        this.container = container;
    }

    @Override
    public String toString() {
        return String.valueOf(getWidth()).concat("x").concat(String.valueOf(getHeight()).concat("@" + String.valueOf(getFramerate())));
    }

}
