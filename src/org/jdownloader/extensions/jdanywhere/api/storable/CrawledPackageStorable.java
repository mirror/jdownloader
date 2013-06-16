package org.jdownloader.extensions.jdanywhere.api.storable;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.Storable;

public class CrawledPackageStorable implements Storable {

    public String getName() {
        return pkg.getName();
    }

    public long getId() {
        return pkg.getUniqueID().getID();
    }

    public String getDirectory() {
        return pkg.getDownloadFolder();
    }

    public String getComment() {
        String comment = pkg.getComment();
        if (comment == null || comment.length() == 0) return "";
        return comment;
    }

    public long getAdded() {
        return pkg.getCreated();
    }

    public List<CrawledLinkStoreable> getLinks() {
        return links;
    }

    public long getSize() {
        long size = 0;
        for (CrawledLink link : pkg.getChildren()) {
            size += link.getSize();
        }
        return size;
    }

    public List<String> getHoster() {
        List<String> links = new ArrayList<String>(pkg.getChildren().size());
        for (CrawledLink link : pkg.getChildren()) {
            if (!links.contains(link.getHost())) links.add(link.getHost());
        }
        return links;
    }

    public String getPassword() {
        String password = "---";
        for (CrawledLink link : pkg.getChildren()) {
            if (password == null || password.length() == 0 || password.equals("---"))
                password = link.getDownloadLink().getDownloadPassword();
            else if (!link.getDownloadLink().getDownloadPassword().equals(password)) {
                password = "";
                break;
            }
        }
        if (password == null || password.length() == 0 || password.equals("---")) password = "";
        return password;
    }

    public int getEnabled() {
        int enabled = -1;
        synchronized (pkg) {
            for (CrawledLink link : pkg.getChildren()) {
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

    public void setLinks(List<CrawledLinkStoreable> links) {
        this.links = links;
    }

    private List<CrawledLinkStoreable> links;
    private CrawledPackage             pkg;

    @SuppressWarnings("unused")
    private CrawledPackageStorable() {
    }

    public CrawledPackageStorable(CrawledPackage pkg) {
        this.pkg = pkg;
    }
}
