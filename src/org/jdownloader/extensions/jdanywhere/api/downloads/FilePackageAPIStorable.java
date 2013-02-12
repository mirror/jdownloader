package org.jdownloader.extensions.jdanywhere.api.downloads;

import jd.controlling.downloadcontroller.DownloadWatchDog;
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
        long speed = DownloadWatchDog.getInstance().getDownloadSpeedbyFilePackage(pkg);
        return Math.max(speed, 0);
    }

    public long getDone() {
        return pkg.getView().getDone();
    }

    public long getSize() {
        return pkg.getView().getSize();
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