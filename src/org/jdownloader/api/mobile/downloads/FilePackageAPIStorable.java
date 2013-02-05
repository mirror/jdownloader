package org.jdownloader.api.mobile.downloads;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.Storable;

public class FilePackageAPIStorable implements Storable {
    public String getName() {
        return pkg.getName();
    }

    public void setName(String name) {
    }

    public long getId() {
        return pkg.getUniqueID().getID();
    }

    public String getDldir() {
        return pkg.getDownloadDirectory();
    }

    public String getComment() {
        String comment = pkg.getComment();
        if (comment == null || comment.length() == 0) return null;
        return comment;
    }

    public void setComment(String comment) {
    }

    public long getAdded() {
        return pkg.getCreated();
    }

    public long getSpeed() {
        long speed = 0;
        synchronized (pkg) {
            for (DownloadLink link : pkg.getChildren()) {
                speed += link.getDownloadSpeed();
            }
        }
        return speed;
    }

    public long getDone() {
        long done = 0;
        synchronized (pkg) {
            for (DownloadLink link : pkg.getChildren()) {
                done += link.getDownloadCurrent();
            }
        }
        return done;
    }

    public long getSize() {
        long size = 0;
        synchronized (pkg) {
            for (DownloadLink link : pkg.getChildren()) {
                size += link.getDownloadSize();
            }
        }
        return size;
    }

    public int getEnabled() {
        int enabled = -1;
        synchronized (pkg) {
            for (DownloadLink link : pkg.getChildren()) {
                if (enabled != 2) {
                    if (link.isEnabled()) {
                        if (enabled == -1) {
                            enabled = 1;
                        } else if (enabled == 0) {
                            enabled = 2;
                            break;
                        }
                    } else {
                        if (enabled == -1) {
                            enabled = 0;
                        } else if (enabled == 1) {
                            enabled = 2;
                            break;
                        }
                    }
                }
            }
        }
        return enabled;
    }

    // public List<DownloadLinkAPIStorable> getLinks() {
    // return links;
    // }

    // public void setLinks(List<DownloadLinkAPIStorable> links) {
    // this.links = links;
    // }

    // private List<DownloadLinkAPIStorable> links;
    private FilePackage pkg;

    @SuppressWarnings("unused")
    private FilePackageAPIStorable() {
    }

    public FilePackageAPIStorable(FilePackage pkg) {
        this.pkg = pkg;
    }
}