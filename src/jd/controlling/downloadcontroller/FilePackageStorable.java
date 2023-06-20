package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Map;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageControllerComparator;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkStorable;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.Storable;
import org.appwork.storage.StorableAllowPrivateAccessModifier;
import org.appwork.swing.exttable.ExtColumn;

public class FilePackageStorable implements Storable {
    private FilePackage                          filePackage;
    private java.util.List<DownloadLinkStorable> links;

    @SuppressWarnings("unused")
    @StorableAllowPrivateAccessModifier
    private FilePackageStorable(/* Storable */) {
        this.filePackage = FilePackage.getInstance();
        links = new ArrayList<DownloadLinkStorable>();
    }

    public FilePackageStorable(FilePackage filePackage, final boolean includeChildren) {
        this.filePackage = filePackage;
        if (!includeChildren) {
            links = new ArrayList<DownloadLinkStorable>();
        } else {
            links = new ArrayList<DownloadLinkStorable>(filePackage.size());
            boolean readL = filePackage.getModifyLock().readLock();
            try {
                for (DownloadLink link : filePackage.getChildren()) {
                    links.add(new DownloadLinkStorable(link));
                }
            } finally {
                filePackage.getModifyLock().readUnlock(readL);
            }
        }
    }

    public FilePackageStorable(FilePackage filePackage) {
        this(filePackage, false);
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

    public String getSorterId() {
        final PackageControllerComparator<AbstractNode> lSorter = filePackage.getCurrentSorter();
        if (lSorter == null) {
            return null;
        } else {
            final boolean asc = lSorter.isAsc();
            return ((asc ? ExtColumn.SORT_ASC : ExtColumn.SORT_DESC) + "." + lSorter.getID());
        }
    }

    public void setSorterId(String id) {
        try {
            if (id == null) {
                filePackage.setCurrentSorter(null);
            } else {
                filePackage.setCurrentSorter(PackageControllerComparator.getComparator(id));
            }
        } catch (Throwable t) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(t);
        }
    }

    public void setName(String name) {
        filePackage.setName(name);
    }

    public Map<String, Object> getProperties() {
        /* WORKAROUND for Idiots using null as HashMap Key :p */
        return filePackage.getProperties();
    }

    @AllowNonStorableObjects(value = { Object.class })
    public void setProperties(Map<String, Object> props) {
        filePackage.setProperties(props);
    }

    public long getCreated() {
        return filePackage.getCreated();
    }

    public void setModified(long modified) {
        filePackage.setModified(modified);
    }

    public long getModified() {
        return filePackage.getModified();
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
            filePackage.getModifyLock().writeLock();
            try {
                for (DownloadLinkStorable link : links) {
                    filePackage.add(link._getDownloadLink());
                }
            } finally {
                filePackage.getModifyLock().writeUnlock();
            }
        }
    }

    public FilePackage _getFilePackage() {
        return filePackage;
    }
}
