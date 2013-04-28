package org.jdownloader.extensions.jdanywhere.api.storable;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.Storable;

public class FilePackageStorable implements Storable {
    public String getName() {
        return pkg.getName();
    }

    public void setName(String name) {
    }

    public long getId() {
        return pkg.getUniqueID().getID();
    }

    // public String getComment() {
    // String comment = pkg.getComment();
    // if (comment == null || comment.length() == 0) return "";
    // return comment;
    // }

    public long getAdded() {
        return pkg.getCreated();
    }

    public long getSpeed() {
        long speed = DownloadWatchDog.getInstance().getDownloadSpeedbyFilePackage(pkg);
        return Math.max(0, speed);
    }

    public long getDone() {
        return pkg.getView().getDone();
    }

    public long getSize() {
        return pkg.getView().getSize();
    }

    // public String getDirectory() {
    // return pkg.getDownloadDirectory();
    // }
    //
    // public String getPassword() {
    // String password = "---";
    // for (DownloadLink link : pkg.getChildren()) {
    // if (password == null || password.length() == 0 || password.equals("---"))
    // password = link.getDownloadPassword();
    // else if (!link.getDownloadPassword().equals(password)) { return ""; }
    // }
    // if (password == null || password.length() == 0 || password.equals("---")) return "";
    // return password;
    // }

    public List<String> getHoster() {
        List<String> links = new ArrayList<String>(pkg.size());
        for (DownloadLink link : pkg.getChildren()) {
            if (!links.contains(link.getHost())) links.add(link.getHost());
        }
        return links;
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
    private FilePackageStorable() {
    }

    public FilePackageStorable(FilePackage pkg) {
        this.pkg = pkg;
    }
}