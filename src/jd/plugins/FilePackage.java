//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jd.config.Property;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildComparator;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.UniqueSessionID;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

/**
 * Diese Klasse verwaltet Pakete
 * 
 * @author JD-Team
 */
public class FilePackage extends Property implements Serializable, AbstractPackageNode<DownloadLink, FilePackage> {

    private static final long            serialVersionUID = -8859842964299890820L;

    private String                       downloadDirectory;

    private ArrayList<DownloadLink>      downloadLinkList;
    private transient static FilePackage FP               = null;

    static {
        FP = new FilePackage() {
            private static final long serialVersionUID = 1L;

            @Override
            public void _add(DownloadLink... links) {
            }

            @Override
            public void remove(DownloadLink... links) {
            }

            @Override
            public void setControlledBy(PackageController<FilePackage, DownloadLink> controller) {
            }

            @Override
            public UniqueSessionID getUniqueID() {
                return null;
            }
        };
        FP.setName(_JDT._.controller_packages_defaultname());
        FP.downloadLinkList = new ArrayList<DownloadLink>() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public DownloadLink set(int index, DownloadLink element) {
                return null;
            }

            @Override
            public boolean add(DownloadLink e) {
                return true;
            }

            @Override
            public void add(int index, DownloadLink element) {
            }

            @Override
            public boolean addAll(Collection<? extends DownloadLink> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends DownloadLink> c) {
                return false;
            }

        };
    }

    /**
     * returns defaultFilePackage, used only to avoid NullPointerExceptions, you cannot add/remove links in it
     * 
     * @return
     */
    public static FilePackage getDefaultFilePackage() {
        return FP;
    }

    public static boolean isDefaultFilePackage(FilePackage fp) {
        return FP == fp;
    }

    private String                                                 name              = null;

    private long                                                   created           = -1l;

    private transient boolean                                      isExpanded        = false;

    private transient PackageController<FilePackage, DownloadLink> controlledby      = null;
    private transient UniqueSessionID                              uniqueID          = null;
    public static final String                                     PROPERTY_EXPANDED = "EXPANDED";
    private static final String                                    PROPERTY_COMMENT  = "COMMENT";

    /**
     * @return the uniqueID
     */
    public UniqueSessionID getUniqueID() {
        return uniqueID;
    }

    private transient FilePackageView     fpInfo = null;

    private ChildComparator<DownloadLink> sorter;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return uniqueID.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof FilePackage)) return false;
        return ((FilePackage) obj).uniqueID == this.uniqueID;
    }

    /**
     * return a new FilePackage instance
     * 
     * @return
     */
    public static FilePackage getInstance() {
        return new FilePackage();
    }

    /**
     * private constructor for FilePackage, sets created timestamp and downloadDirectory
     */
    private FilePackage() {
        uniqueID = new UniqueSessionID();
        downloadDirectory = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        created = System.currentTimeMillis();
        /* till refactoring is complete */
        this.downloadLinkList = new ArrayList<DownloadLink>();
        setName(null);

    }

    /**
     * restore this FilePackage from an ObjectInputStream and do some conversations, restoring some transient variables
     * 
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        /* deserialize object and then fill other stuff(transient..) */
        stream.defaultReadObject();
        isExpanded = getBooleanProperty(PROPERTY_EXPANDED, false);
        uniqueID = new UniqueSessionID();
    }

    /**
     * return this FilePackage created timestamp
     * 
     * @return
     */
    public long getCreated() {
        return created;
    }

    /**
     * set this FilePackage created timestamp
     * 
     * @param created
     */
    public void setCreated(long created) {
        this.created = created;
    }

    /**
     * add given DownloadLink to this FilePackage. delegates the call to DownloadControllerInterface if it is set
     * 
     * @param link
     */
    public void add(DownloadLink link) {
        _add(link);
    }

    /**
     * add the given DownloadLinks to this FilePackage. delegates the call to the DownloadControllerInterface if it is set
     * 
     * @param links
     */
    public void addLinks(ArrayList<DownloadLink> links) {
        if (links == null || links.size() == 0) return;
        _add(links.toArray(new DownloadLink[links.size()]));
    }

    /**
     * add the given DownloadLinks to this FilePackage. delegates the call to the DownloadControllerInterface if it is set
     * 
     * @param links
     */
    public void _add(DownloadLink... links) {
        if (links == null || links.length == 0) return;
        if (this.controlledby == null) {
            synchronized (this) {
                for (DownloadLink link : links) {
                    if (!this.downloadLinkList.contains(link)) {
                        link._setFilePackage(this);
                        this.downloadLinkList.add(link);
                    }
                }
            }
            notifyStructureChanges();
        } else {
            this.controlledby.moveOrAddAt(this, Arrays.asList(links), -1);
        }
    }

    @Override
    public void sort() {
        synchronized (this) {
            if (sorter == null) return;
            Collections.sort(downloadLinkList, sorter);
        }
    }

    @Override
    public void setCurrentSorter(ChildComparator<DownloadLink> comparator) {
        sorter = comparator;
    }

    /**
     * return the download folder of this FilePackage
     * 
     * @return
     */
    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    /**
     * return the name of this FilePackage
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * remove the given DownloadLinks from this FilePackage. delegates remove call to DownloadControllerInterface if it is set
     * 
     * @param link
     */
    public void remove(DownloadLink... links) {
        if (links == null || links.length == 0) return;
        if (this.controlledby == null) {
            synchronized (this) {
                for (DownloadLink link : links) {
                    if ((this.downloadLinkList.remove(link))) {
                        /*
                         * set FilePackage to null if the link was controlled by this FilePackage
                         */
                        if (link.getFilePackage() == this) link._setFilePackage(null);
                    }
                }
            }
            notifyStructureChanges();
        } else {
            this.controlledby.removeChildren(this, Arrays.asList(links), true);
        }
    }

    public void setComment(String comment) {
        if (comment == null || comment.length() == 0) {
            this.setProperty(PROPERTY_COMMENT, Property.NULL);
        } else {
            this.setProperty(PROPERTY_COMMENT, comment);
        }
    }

    public String getComment() {
        return this.getStringProperty(PROPERTY_COMMENT, null);
    }

    /**
     * set the download folder for this FilePackage
     * 
     * @param subFolder
     */
    public void setDownloadDirectory(String folder) {
        if (StringUtils.isEmpty(folder)) {
            folder = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        } else if (!CrossSystem.isAbsolutePath(folder)) {
            Log.L.severe("FilePackage: setDownloadDirectory only allows absolute pathes! Using default one!");
            folder = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        }
        downloadDirectory = folder;
    }

    /**
     * set the name of this FilePackage
     * 
     * @param name
     */
    public void setName(String name) {
        if (StringUtils.isEmpty(name)) {
            name = _JDT._.controller_packages_defaultname();
        }
        this.name = name.trim();
    }

    /**
     * return number of DownloadLinks in this FilePackage
     * 
     * @return
     */
    public int size() {
        return downloadLinkList.size();
    }

    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * return if this FilePackage is in expanded state
     * 
     * @return
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    /**
     * set the expanded state of this FilePackage
     * 
     * @param b
     */
    public void setExpanded(boolean b) {
        if (this.isExpanded == b) return;
        this.isExpanded = b;
        if (b == false) {
            setProperty(PROPERTY_EXPANDED, Property.NULL);
        } else {
            setProperty(PROPERTY_EXPANDED, b);
        }
    }

    public boolean isEnabled() {
        return this.getView().isEnabled();
    }

    public List<DownloadLink> getChildren() {
        return downloadLinkList;
    }

    public PackageController<FilePackage, DownloadLink> getControlledBy() {
        return controlledby;
    }

    public void setControlledBy(PackageController<FilePackage, DownloadLink> controller) {
        controlledby = controller;
    }

    public void notifyStructureChanges() {
        if (fpInfo != null) {
            fpInfo.changeStructure();
        }
    }

    public void setEnabled(boolean b) {
        synchronized (this) {
            for (DownloadLink link : getChildren()) {
                link.setEnabled(b);
            }
        }
    }

    public void nodeUpdated(DownloadLink source) {
        notifyChanges();
    }

    private void notifyChanges() {
        PackageController<FilePackage, DownloadLink> n = getControlledBy();
        if (n != null) {
            n.nodeUpdated(this);
        }
        if (fpInfo != null) {
            fpInfo.changeVersion();
        }
    }

    public int indexOf(DownloadLink child) {
        synchronized (this) {
            return downloadLinkList.indexOf(child);
        }
    }

    @Override
    public FilePackageView getView() {
        if (fpInfo != null) return fpInfo;
        synchronized (this) {
            if (fpInfo == null) fpInfo = new FilePackageView(this);
        }
        return fpInfo;
    }

    @Override
    public long getFinishedDate() {
        return this.getView().getFinishedDate();
    }

    @Override
    public ChildComparator<DownloadLink> getCurrentSorter() {
        return sorter;
    }

}