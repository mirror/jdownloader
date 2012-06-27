package org.jdownloader.api.downloads;

import java.util.List;

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

    public void setId(long id) {
    }

    public String getDldir() {
        return pkg.getDownloadDirectory();
    }

    public void setDldir(String dldir) {
    }

    public String getComment() {
        String comment = pkg.getComment();
        if (comment == null || comment.length() == 0) return null;
        return comment;
    }

    public void setComment(String comment) {
    }

    /**
     * @return the added
     */
    public long getAdded() {
        return pkg.getCreated();
    }

    public void setAdded(long added) {
    }

    /**
     * @return the links
     */
    public List<DownloadLinkAPIStorable> getLinks() {
        return links;
    }

    /**
     * @param links
     *            the links to set
     */
    public void setLinks(List<DownloadLinkAPIStorable> links) {
        this.links = links;
    }

    private List<DownloadLinkAPIStorable> links;
    private FilePackage                   pkg;

    private FilePackageAPIStorable() {
    }

    public FilePackageAPIStorable(FilePackage pkg) {
        this.pkg = pkg;
    }
}
