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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import jd.config.Property;
import jd.controlling.DownloadControllerInterface;
import jd.gui.swing.jdgui.views.downloads.DownloadTable;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

/**
 * Diese Klasse verwaltet Pakete
 * 
 * @author JD-Team
 */
public class FilePackage extends Property implements Serializable, PackageLinkNode {

    private static final AtomicLong      FilePackageIDCounter = new AtomicLong(0);

    private static final long            serialVersionUID     = -8859842964299890820L;

    private static final long            UPDATE_INTERVAL      = 2000;

    private String                       comment;

    private String                       downloadDirectory;

    private ArrayList<DownloadLink>      downloadLinkList;
    private transient static FilePackage FP                   = null;

    static {
        FP = new FilePackage() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void _add(DownloadLink... links) {
            }

            @Override
            public ArrayList<DownloadLink> getControlledDownloadLinks() {
                return new ArrayList<DownloadLink>();
            }

            @Override
            public void remove(DownloadLink... links) {
            }
        };
        FP.setName(_JDT._.controller_packages_defaultname());
    }

    /**
     * returns defaultFilePackage, used only to avoid NullPointerExceptions, you
     * cannot add/remove links in it
     * 
     * @return
     */
    public static FilePackage getDefaultFilePackage() {
        return FP;
    }

    private int                                   linksFailed;

    private int                                   linksFinished;

    private int                                   linksInProgress;

    private String                                name                 = null;

    private String                                password2;

    private boolean                               extractAfterDownload = true;

    private long                                  totalBytesLoaded_v2;

    private long                                  totalDownloadSpeed_v2;

    private long                                  totalEstimatedPackageSize_v2;

    private long                                  updateTime;

    private long                                  updateTime1;

    private boolean                               isFinished;
    /* no longer in use, pay attention when removing */
    @Deprecated
    private Integer                               links_Disabled;
    /* no longer in use, pay attention when removing */
    @Deprecated
    private String                                ListHoster           = null;

    private long                                  created              = -1l;

    private long                                  finishedDate         = -1l;

    private transient boolean                     isExpanded           = false;

    private transient DownloadControllerInterface controlledby         = null;
    private transient long                        uniqueID             = -1;

    /**
     * @return the controlledby
     */
    public DownloadControllerInterface getControlledby() {
        return controlledby;
    }

    /**
     * @param controlledby
     *            the controlledby to set
     */
    public void setControlledby(DownloadControllerInterface controlledby) {
        this.controlledby = controlledby;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (int) (uniqueID ^ (uniqueID >>> 32));
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
     * private constructor for FilePackage, sets created timestamp and
     * downloadDirectory
     */
    private FilePackage() {
        uniqueID = FilePackageIDCounter.incrementAndGet();
        downloadDirectory = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        created = System.currentTimeMillis();
        /* till refactoring is complete */
        this.downloadLinkList = new ArrayList<DownloadLink>();
        setName(null);
    }

    /**
     * restore this FilePackage from an ObjectInputStream and do some
     * conversations, restoring some transient variables
     * 
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        /* deserialize object and then fill other stuff(transient..) */
        stream.defaultReadObject();
        isExpanded = getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false);
        uniqueID = FilePackageIDCounter.incrementAndGet();
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
     * return this FilePackage finish timestamp
     * 
     * @return
     */
    public long getFinishedDate() {
        return finishedDate;
    }

    /**
     * set this FilePackage finish timestamp
     * 
     * @param finishedDate
     */
    public void setFinishedDate(long finishedDate) {
        this.finishedDate = finishedDate;
    }

    /**
     * add given DownloadLink to this FilePackage. delegates the call to
     * DownloadControllerInterface if it is set
     * 
     * @param link
     */
    public void add(DownloadLink link) {
        _add(link);
    }

    /**
     * add the given DownloadLinks to this FilePackage. delegates the call to
     * the DownloadControllerInterface if it is set
     * 
     * @param links
     */
    public void addLinks(ArrayList<DownloadLink> links) {
        if (links == null || links.size() == 0) return;
        _add(links.toArray(new DownloadLink[links.size()]));
    }

    /**
     * add the given DownloadLinks to this FilePackage. delegates the call to
     * the DownloadControllerInterface if it is set
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
        } else {
            this.controlledby.addDownloadLinks(this, links);
        }
    }

    /**
     * return if this FilePackage should be post processed
     * 
     * @return
     */
    public boolean isPostProcessing() {
        return extractAfterDownload;
    }

    /**
     * set whether this FilePackage should be post processed or not
     * 
     * @param postProcessing
     */
    public void setPostProcessing(boolean postProcessing) {
        this.extractAfterDownload = postProcessing;
    }

    /**
     * return the comment of this FilePackage if set
     * 
     * @return
     */
    public String getComment() {
        return comment;
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
     * return all DownloadLinks controlled by this FilePackage
     * 
     * @return
     */
    public ArrayList<DownloadLink> getControlledDownloadLinks() {
        return downloadLinkList;
    }

    /**
     * Gibt die vorraussichtlich verbleibende Downloadzeit für dieses paket
     * zurück
     * 
     * @return
     */
    public long getETA() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        if (totalDownloadSpeed_v2 / 1024 == 0) { return -1; }
        return (Math.max(totalBytesLoaded_v2, totalEstimatedPackageSize_v2) - totalBytesLoaded_v2) / (totalDownloadSpeed_v2);
    }

    /**
     * Gibt die Anzahl der fehlerhaften Links zurück
     * 
     * @return
     */
    public int getLinksFailed() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return linksFailed;
    }

    /**
     * Gibt die Anzahl der fertiggestellten Links zurück
     * 
     * @return
     */
    public int getLinksFinished() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return linksFinished;
    }

    /**
     * Gibt zurück wieviele Links gerade in Bearbeitung sind
     * 
     * @return
     */
    public int getLinksInProgress() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return linksInProgress;
    }

    public boolean isFinished() {
        if (System.currentTimeMillis() - updateTime1 > UPDATE_INTERVAL) {
            updateTime1 = System.currentTimeMillis();
            boolean value = true;
            long lastfinished = 0;
            if (linksFinished > 0) {
                synchronized (downloadLinkList) {
                    for (DownloadLink lk : downloadLinkList) {
                        if (!lk.getLinkStatus().isFinished() && lk.isEnabled()) {
                            value = false;
                            break;
                        } else {
                            if (lk.getFinishedDate() != -1) lastfinished = lastfinished >= lk.getFinishedDate() ? lastfinished : lk.getFinishedDate();
                        }
                    }
                }
            } else {
                value = false;
            }
            isFinished = value;
            if (!isFinished) {
                finishedDate = -1;
            } else if (isFinished && finishedDate == -1) finishedDate = lastfinished;
        }
        return isFinished;
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
     * return post processing password for this FilePackage, if it is set
     * 
     * @deprecated Use {@link #getPasswordList()}
     * @return
     */
    @Deprecated
    public String getPassword() {
        return password2;
    }

    /**
     * return a list of passwords of all DownloadLinks for post processing
     */
    public static Set<String> getPasswordAuto(FilePackage fp) {
        Set<String> pwList = new HashSet<String>();
        if (fp == null) return pwList;
        synchronized (fp) {
            for (DownloadLink element : fp.getControlledDownloadLinks()) {
                ArrayList<String> pws = null;
                if ((pws = element.getSourcePluginPasswordList()) != null) {

                    for (String pw : pws) {
                        if (pw == null) continue;
                        pwList.add(pw);
                    }
                }
            }
        }
        return pwList;
    }

    /**
     * Gibt den Fortschritt des pakets in prozent zurück
     */
    public double getPercent() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return 100.0 * totalBytesLoaded_v2 / Math.max(1, Math.max(totalBytesLoaded_v2, totalEstimatedPackageSize_v2));
    }

    /**
     * Gibt die Anzahl der Verbleibenden Links zurück. Wurden alle Links bereits
     * abgearbeitet gibt diese Methode 0 zurück Da die Methode alle Links
     * durchläuft sollte sie aus Performancegründen mit bedacht eingesetzt
     * werden
     */
    public int getRemainingLinks() {
        updateCollectives();
        return size() - linksFinished;

    }

    /**
     * Gibt die aktuelle Downloadgeschwinigkeit des Pakets zurück
     * 
     * @return
     */
    public long getTotalDownloadSpeed() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return totalDownloadSpeed_v2;
    }

    /**
     * Gibt die geschätzte Gesamtgröße des Pakets zurück
     * 
     * @return
     */
    public long getTotalEstimatedPackageSize() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return Math.max(totalBytesLoaded_v2, totalEstimatedPackageSize_v2);
    }

    /**
     * Gibt zurück wieviele Bytes ingesamt schon in diesem Paket geladen wurden
     * 
     * @return
     */
    public long getTotalKBLoaded() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return totalBytesLoaded_v2;
    }

    public long getRemainingKB() {
        return getTotalEstimatedPackageSize() - getTotalKBLoaded();
    }

    /**
     * remove the given DownloadLinks from this FilePackage. delegates remove
     * call to DownloadControllerInterface if it is set
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
                         * set FilePackage to null if the link was controlled by
                         * this FilePackage
                         */
                        if (link.getFilePackage() == this) link._setFilePackage(null);
                    }
                }
            }
        } else {
            this.controlledby.removeDownloadLinks(this, links);
        }
    }

    /**
     * set the Comment for this FilePackage
     * 
     * @param comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * set the download folder for this FilePackage
     * 
     * @param subFolder
     */
    public void setDownloadDirectory(String subFolder) {
        downloadDirectory = JDUtilities.removeEndingPoints(subFolder);
        if (downloadDirectory == null) {
            downloadDirectory = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        }
    }

    /**
     * set the name of this FilePackage
     * 
     * @param name
     */
    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(_JDT._.controller_packages_defaultname());
        } else {
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        }
        this.name = this.name.trim();
    }

    /**
     * set the Password for post processing to this FilePackage
     * 
     * @deprecated Use {@link #setPasswordList(ArrayList)}
     * @param password
     */
    @Deprecated
    public void setPassword(String password) {
        this.password2 = password;
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
        setProperty(DownloadTable.PROPERTY_EXPANDED, b);
    }

    public void updateCollectives() {
        synchronized (downloadLinkList) {

            totalEstimatedPackageSize_v2 = 0;
            totalDownloadSpeed_v2 = 0;
            linksFinished = 0;
            linksInProgress = 0;
            linksFailed = 0;
            totalBytesLoaded_v2 = 0;
            long avg = 0;
            DownloadLink next;
            int i = 0;

            for (Iterator<DownloadLink> it = downloadLinkList.iterator(); it.hasNext();) {
                next = it.next();

                if (next.getDownloadSize() > 0) {
                    if (next.isEnabled()) {
                        totalEstimatedPackageSize_v2 += next.getDownloadSize();
                    }
                    avg = (i * avg + next.getDownloadSize()) / (i + 1);
                    i++;
                } else {
                    if (it.hasNext()) {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize_v2 += avg;
                        }
                    } else {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize_v2 += avg / 2;
                        }
                    }
                }

                totalDownloadSpeed_v2 += next.getDownloadSpeed();
                if (next.isEnabled()) {
                    totalBytesLoaded_v2 += next.getDownloadCurrent();
                }
                linksInProgress += next.getLinkStatus().isPluginActive() ? 1 : 0;
                if (next.getLinkStatus().isFinished()) {
                    linksFinished += 1;
                }

                if (next.getLinkStatus().isFailed() && next.isEnabled()) {
                    linksFailed++;
                }
            }
        }
        updateTime = System.currentTimeMillis();
    }

    /**
     * return enabled/disabled state of this FilePackage, compares size to
     * disabledLinks
     */
    public boolean isEnabled() {
        // if (controlledLinks.size() <= disabledLinks.get()) return false;
        return true;
    }

    private ArrayList<String> passwordList = new ArrayList<String>();

    public void setPasswordList(ArrayList<String> passwordList) {
        this.passwordList = passwordList;
    }

    public String[] getPasswordList() {
        ArrayList<String> lst = new ArrayList<String>();
        // can be null due to old serialized versions
        if (passwordList != null) {
            lst.addAll(passwordList);
        }
        if (getPassword() != null && getPassword().length() > 0) lst.add(getPassword());
        for (Iterator<String> it = getPasswordAuto(this).iterator(); it.hasNext();) {
            lst.add(it.next());
        }
        return lst.toArray(new String[] {});
    }

    /**
     * Returns a list of all hoster Strings in this package
     * 
     * @return
     */
    public ArrayList<String> getHosterList() {
        ArrayList<String> ret = new ArrayList<String>();
        Set<String> set = new HashSet<String>();

        synchronized (this) {
            for (DownloadLink element : getControlledDownloadLinks()) {
                if (!set.contains(element.getHost())) {
                    set.add(element.getHost());
                    ret.add(element.getHost());
                }
            }
        }
        return ret;
    }

}