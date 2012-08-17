package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.HashMap;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.Storable;

public class FilePackageStorable implements Storable {

    private FilePackage                     filePackage;
    private java.util.List<DownloadLinkStorable> links;

    @SuppressWarnings("unused")
    private FilePackageStorable(/* Storable */) {
        this.filePackage = FilePackage.getInstance();
    }

    public FilePackageStorable(FilePackage filePackage) {
        this.filePackage = filePackage;
        links = new ArrayList<DownloadLinkStorable>(filePackage.size());
        synchronized (filePackage) {
            for (DownloadLink link : filePackage.getChildren()) {
                links.add(new DownloadLinkStorable(link));
            }
        }
    }

    public long getUID() {
        return filePackage.getUniqueID().getID();
    }

    public void setUID(long id) {
        filePackage.getUniqueID().setID(id);
    }

    public String getName() {
        return filePackage.getName();
    }

    public void setName(String name) {
        filePackage.setName(name);
    }

    public HashMap<String, Object> getProperties() {
        /* WORKAROUND for Idiots using null as HashMap Key :p */
        HashMap<String, Object> properties = filePackage.getProperties();
        if (properties != null) properties.remove(null);
        return filePackage.getProperties();
    }

    public void setProperties(HashMap<String, Object> props) {
        filePackage.setProperties(props);
    }

    public long getCreated() {
        return filePackage.getCreated();
    }

    public void setCreated(long time) {
        filePackage.setCreated(time);
    }

    public String getDownloadFolder() {
        return filePackage.getDownloadDirectory();
    }

    public void setDownloadFolder(String dest) {
        filePackage.setDownloadDirectory(dest);
    }

    public java.util.List<DownloadLinkStorable> getLinks() {
        return links;
    }

    public void setLinks(java.util.List<DownloadLinkStorable> links) {
        if (links != null) {
            this.links = links;
            synchronized (filePackage) {
                for (DownloadLinkStorable link : links) {
                    filePackage.add(link._getDownloadLink());
                }
            }
        }
    }

    public FilePackage _getFilePackage() {
        return filePackage;
    }
}
