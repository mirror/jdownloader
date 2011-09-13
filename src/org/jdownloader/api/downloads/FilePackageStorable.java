package org.jdownloader.api.downloads;

import java.util.List;

import jd.plugins.FilePackage;

import org.appwork.storage.Storable;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class FilePackageStorable implements Storable {

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
     * @return the pw
     */
    public String[] getPw() {
        String[] list = pkg.getPasswordList();
        if (list == null || list.length == 0) return null;
        return list;
    }

    public void setPw(String[] pw) {
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
    public List<DownloadLinkStorable> getLinks() {
        return links;
    }

    /**
     * @param links
     *            the links to set
     */
    public void setLinks(List<DownloadLinkStorable> links) {
        this.links = links;
    }

    private List<DownloadLinkStorable> links;
    private FilePackage                pkg;

    private FilePackageStorable() {
    }

    public FilePackageStorable(FilePackage pkg) {
        this.pkg = pkg;
    }
}
