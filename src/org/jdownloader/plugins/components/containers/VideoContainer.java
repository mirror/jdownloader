package org.jdownloader.plugins.components.containers;

import java.net.MalformedURLException;
import java.util.Locale;

import org.appwork.storage.Storable;
import org.appwork.utils.parser.UrlQuery;

import jd.plugins.Plugin;

/**
 * Base container for all video information. Extend when you want further features.
 *
 * @author raztoki
 *
 */
public class VideoContainer implements Storable {
    private String downloadurl = null;
    private String extension   = null;
    private int    width       = -1;
    private int    height      = -1;
    private int    framerate   = -1;
    private int    bitrate     = -1;
    private long   filesize    = -1;

    public VideoContainer() {
    }

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

    public final void _setDownloadurlAndExtension(final String downloadurl, final String extention) {
        setDownloadurl(downloadurl);
        setExtension(extention);
    }

    public final void _setDownloadurlAndExtension(final String downloadurl) {
        setDownloadurl(downloadurl);
        _setExtensionFromUrl(downloadurl);
    }

    /**
     * set the extension based on current
     */
    public final void updateExtensionFromUrl() {
        if (this.downloadurl != null) {
            _setExtensionFromUrl(this.downloadurl);
        }
    }

    /**
     * @param extension
     *            the extension to set
     */
    public final void setExtension(String extension) {
        this.extension = extension != null ? extension.toLowerCase(Locale.ENGLISH) : null;
    }

    public final void _setExtensionFromUrl(final String url) {
        String extension = null;
        try {
            final UrlQuery query = UrlQuery.parse(url);
            final String filename = query.get("filename");
            extension = Plugin.getFileNameExtensionFromString(filename);
        } catch (MalformedURLException e) {
        }
        if (extension == null) {
            extension = Plugin.getFileNameExtensionFromString(url);
        }
        this.extension = extension;
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

    @Override
    public String toString() {
        return String.valueOf(getWidth()).concat("x").concat(String.valueOf(getHeight()).concat("@" + String.valueOf(getFramerate())));
    }
}
