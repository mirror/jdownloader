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
    private static final LogSource LOGGER = LogController.getInstance().getLogger(PrepareJob.class.getName());
    private String[] actors;
    private String album;

    private String artist;

    private String containerFormat;

    private String   creator;

    private long     date = 0l;

    private String[] dlnaProfiles;

    private DownloadLink           downloadLink;

    private String[] genres;

    private String infoString;

    private String   majorBrand;

    private MediaFolder parent;
    private MediaRoot              root;

    private long   size = -1;

    private String thumbnailPath;

    private String   title;

    public MediaItem(DownloadLink dl) {
        this.downloadLink = dl;
        if (dl.getDefaultPlugin() == null) {
            restorePlugin();
        }

        date = System.currentTimeMillis();
    }

    public String[] getActors() {
        return actors;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getContainerFormat() {
        return containerFormat;
    }
    public String getCreator() {
        return creator == null ? ("(" + downloadLink.getHost() + ") " + downloadLink.getBrowserUrl()) : creator;
    }
    public long getDate() {
        if (date <= 0) date = System.currentTimeMillis();
        return date;
    }
    public String[] getDlnaProfiles() {
        return dlnaProfiles;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public String[] getGenres() {
        return genres;
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("video", 20);
    }
    public String getInfoString() {
        return infoString;
    }
    public String getMajorBrand() {
        return majorBrand;
    }
    public abstract String getMimeTypeString();
    @Override
    public String getName() {
        return downloadLink.getName();
    }

    public MediaFolder getParent() {
        return parent;
    }

    public MediaRoot getRoot() {
        return root;
    }

    public long getSize() {

        return size <= 0 ? downloadLink.getDownloadSize() : size;
    }
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getUniqueID() {
        return downloadLink.getUniqueID().toString();
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

    public void setActors(String[] actors) {
        this.actors = actors;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    // video container type
    public void setContainerFormat(String majorBrand) {
        this.containerFormat = majorBrand;

    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setDlnaProfiles(String[] dlnaProfiles) {
        this.dlnaProfiles = dlnaProfiles;
    }

    public void setDownloadLink(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
    }

    public void setGenres(String[] genres) {
        this.genres = genres;
    }

    public void setInfoString(String result) {
        infoString = result;
    }

    public void setMajorBrand(String major_brand) {
        this.majorBrand = major_brand;
    }

    @Override
    public void setParent(MediaFolder mediaFolder) {
        this.parent = mediaFolder;
    }

    @Override
    public void setRoot(MediaRoot root) {
        this.root = root;
    }

    public void setSize(long l) {
        this.size = l;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public void setTitle(String title) {
        this.title = title;
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
