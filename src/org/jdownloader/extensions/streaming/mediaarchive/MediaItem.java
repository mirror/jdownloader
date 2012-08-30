package org.jdownloader.extensions.streaming.mediaarchive;

import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareJob;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public abstract class MediaItem implements MediaNode {
    private DownloadLink           downloadLink;
    private MediaRoot              root;
    private static final LogSource LOGGER = LogController.getInstance().getLogger(PrepareJob.class.getName());

    public MediaItem(DownloadLink dl) {
        this.downloadLink = dl;
        if (dl.getDefaultPlugin() == null) {
            restorePlugin();
        }

        date = System.currentTimeMillis();
    }

    private void restorePlugin() {
        try {
            PluginForHost pluginForHost = null;
            LazyHostPlugin hPlugin = HostPluginController.getInstance().get(downloadLink.getHost());
            if (hPlugin != null) {
                pluginForHost = hPlugin.getPrototype();
            }

            if (pluginForHost == null) {
                try {
                    for (LazyHostPlugin p : HostPluginController.getInstance().list()) {
                        if (p.getPrototype().rewriteHost(downloadLink)) {
                            pluginForHost = p.getPrototype();
                            break;
                        }
                    }
                    if (pluginForHost != null) {
                        LOGGER.info("Plugin " + pluginForHost.getHost() + " now handles " + downloadLink.getName());
                    }
                } catch (final Throwable e) {
                    LOGGER.log(e);
                }
            }
            if (pluginForHost != null) {
                downloadLink.setDefaultPlugin(pluginForHost);
            } else {
                LOGGER.severe("Could not find plugin " + downloadLink.getHost() + " for " + downloadLink.getName());
            }
        } catch (final Throwable e) {
            LOGGER.log(e);
        }

    }

    public abstract String getMimeTypeString();

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    private MediaFolder parent;

    public MediaFolder getParent() {
        return parent;
    }

    @Override
    public void setParent(MediaFolder mediaFolder) {
        this.parent = mediaFolder;
    }

    @Override
    public void setRoot(MediaRoot root) {
        this.root = root;
    }

    public MediaRoot getRoot() {
        return root;
    }

    private String thumbnailPath;
    private String infoString;

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setDownloadLink(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
    }

    @Override
    public String getName() {
        return downloadLink.getName();
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("video", 20);
    }

    public void setInfoString(String result) {
        infoString = result;
    }

    public String getInfoString() {
        return infoString;
    }

    private String containerFormat;
    private long   size = -1;
    private String artist;
    private String album;

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setGenres(String[] genres) {
        this.genres = genres;
    }

    private String   title;
    private String   majorBrand;
    private String   creator;
    private String[] genres;
    private long     date = 0l;

    public void setDate(long date) {
        this.date = date;
    }

    public void setActors(String[] actors) {
        this.actors = actors;
    }

    private String[] actors;
    private String[] dlnaProfiles;

    // video container type
    public void setContainerFormat(String majorBrand) {
        this.containerFormat = majorBrand;

    }

    public String getContainerFormat() {
        return containerFormat;
    }

    public void setSize(long l) {
        this.size = l;
    }

    @Override
    public String getUniqueID() {
        return downloadLink.getUniqueID().toString();
    }

    public long getSize() {

        return size <= 0 ? downloadLink.getDownloadSize() : size;
    }

    public void setMajorBrand(String major_brand) {
        this.majorBrand = major_brand;
    }

    public String getMajorBrand() {
        return majorBrand;
    }

    public String getCreator() {
        return creator == null ? ("(" + downloadLink.getHost() + ") " + downloadLink.getBrowserUrl()) : creator;
    }

    public String[] getGenres() {
        return genres;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDate() {
        if (date <= 0) date = System.currentTimeMillis();
        return date;
    }

    public String[] getActors() {
        return actors;
    }

    public String[] getDlnaProfiles() {
        return dlnaProfiles;
    }

    public void setDlnaProfiles(String[] dlnaProfiles) {
        this.dlnaProfiles = dlnaProfiles;
    }

    public void update(MediaItem node) {
        dlnaProfiles = node.dlnaProfiles;
        this.actors = node.actors;
        this.album = node.album;
        this.artist = node.artist;
        this.containerFormat = node.containerFormat;
        this.creator = node.creator;
        this.date = node.date;
        this.genres = node.genres;
        this.infoString = node.infoString;
        this.majorBrand = node.majorBrand;
        this.size = node.size;
        this.thumbnailPath = node.thumbnailPath;
        this.title = node.title;
    }

}
