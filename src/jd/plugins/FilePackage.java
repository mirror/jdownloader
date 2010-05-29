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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import jd.config.Property;
import jd.controlling.DownloadController;
import jd.event.JDBroadcaster;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class FilePackageBroadcaster extends JDBroadcaster<FilePackageListener, FilePackageEvent> {

    @Override
    protected void fireEvent(FilePackageListener listener, FilePackageEvent event) {
        listener.onFilePackageEvent(event);
    }

}

/**
 * Diese Klasse verwaltet Pakete
 * 
 * @author JD-Team
 */
public class FilePackage extends Property implements Serializable, DownloadLinkListener, FilePackageListener {

    /**
     * Zählt die instanzierungen durch um eine ID zu erstellen
     */
    private static int counter = 0;

    private static final long serialVersionUID = -8859842964299890820L;

    private static final long UPDATE_INTERVAL = 2000;

    private String comment;

    private String downloadDirectory;

    private ArrayList<DownloadLink> downloadLinkList;
    private transient static FilePackage FP = null;

    public synchronized static FilePackage getDefaultFilePackage() {
        if (FP == null) {
            FP = new FilePackage();
            FP.setName(JDL.L("controller.packages.defaultname", "various"));
        }
        return FP;
    }

    /**
     * Eindeutige PaketID
     */
    private String id;

    private boolean sortasc = false;

    private int linksFailed;

    private int linksFinished;

    private int linksInProgress;

    private String name = null;

    private String password2;

    private boolean extractAfterDownload = true;

    private long totalBytesLoaded_v2;

    private long totalDownloadSpeed_v2;

    private long totalEstimatedPackageSize_v2;

    private long updateTime;

    private long updateTime1;

    private boolean isFinished;

    private transient FilePackageBroadcaster broadcaster = new FilePackageBroadcaster();

    private Integer links_Disabled;

    private String ListHoster = null;

    private long created = -1l;

    private long finishedDate = -1l;

    @Deprecated
    private Vector<DownloadLink> downloadLinks;

    public void addListener(FilePackageListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(FilePackageListener l) {
        broadcaster.removeListener(l);
    }

    public static FilePackage getInstance() {
        return new FilePackage();
    }

    public void remove(ArrayList<DownloadLink> links) {
        for (DownloadLink dl : links) {
            this.remove(dl);
        }
    }

    private FilePackage() {
        links_Disabled = new Integer(0);
        downloadDirectory = JDUtilities.getDefaultDownloadDirectory();
        counter++;
        id = System.currentTimeMillis() + "_" + counter;
        downloadLinkList = new ArrayList<DownloadLink>();
        broadcaster = new FilePackageBroadcaster();
        broadcaster.addListener(this);
        created = System.currentTimeMillis();
        finishedDate = -1l;
    }

    private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        /* nach dem deserialisieren sollen die transienten neu geholt werden */
        stream.defaultReadObject();
        links_Disabled = Integer.valueOf(0);
        resetUpdateTimer();
        broadcaster = new FilePackageBroadcaster();
        broadcaster.addListener(this);
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(long finishedDate) {
        this.finishedDate = finishedDate;
    }

    public void add(DownloadLink link) {
        synchronized (DownloadController.ControllerLock) {
            synchronized (downloadLinkList) {
                if (!downloadLinkList.contains(link)) {
                    downloadLinkList.add(link);
                    link.setFilePackage(this);
                    if (!link.isEnabled()) synchronized (links_Disabled) {
                        links_Disabled++;
                    }
                    link.getBroadcaster().addListener(this);
                    broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.DOWNLOADLINK_ADDED, link));
                }
            }
        }
    }

    public int add(int index, DownloadLink link, int repos) {
        synchronized (DownloadController.ControllerLock) {
            boolean newadded = false;
            synchronized (downloadLinkList) {
                if (downloadLinkList.contains(link)) {
                    int posa = this.indexOf(link);
                    if (posa <= index) {
                        index -= ++repos;
                    }
                    downloadLinkList.remove(link);
                    if (index > downloadLinkList.size() - 1) {
                        downloadLinkList.add(link);
                    } else if (index < 0) {
                        downloadLinkList.add(0, link);
                    } else
                        downloadLinkList.add(index, link);
                } else {
                    if (index > downloadLinkList.size() - 1) {
                        downloadLinkList.add(link);
                    } else if (index < 0) {
                        downloadLinkList.add(0, link);
                    } else
                        downloadLinkList.add(index, link);
                    newadded = true;
                }
            }
            if (newadded) {
                if (!link.isEnabled()) synchronized (links_Disabled) {
                    links_Disabled++;
                }
                link.setFilePackage(this);
                link.getBroadcaster().addListener(this);
                broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.DOWNLOADLINK_ADDED, link));
            } else {
                broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_UPDATE));
            }
        }
        return repos;
    }

    public void addLinks(ArrayList<DownloadLink> links) {
        for (DownloadLink dl : links) {
            add(dl);
        }
    }

    public boolean isPostProcessing() {
        return extractAfterDownload;
    }

    public void setPostProcessing(boolean postProcessing) {
        this.extractAfterDownload = postProcessing;
    }

    public void addLinksAt(ArrayList<DownloadLink> links, int index) {
        int repos = 0;
        for (int i = 0; i < links.size(); i++) {
            repos = add(index + i, links.get(i), repos);
        }
    }

    public boolean contains(DownloadLink link) {
        return downloadLinkList.contains(link);
    }

    public DownloadLink get(int index) {
        synchronized (downloadLinkList) {
            try {
                return downloadLinkList.get(index);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    /**
     * @return Gibt den Kommentar ab den der user im Linkgrabber zu diesem Paket
     *         abgegeben hat
     */
    public String getComment() {
        return comment == null ? "" : comment;
    }

    /**
     * @return Gibt den Downloadpfad zurück den der user für dieses paket
     *         festgelegt hat
     */
    public String getDownloadDirectory() {
        return downloadDirectory == null ? JDUtilities.getDefaultDownloadDirectory() : downloadDirectory;
    }

    /**
     * @return Gibt nur den namen des Downloadverzeichnisses zurück. ACHTUNG! es
     *         wird nur der Directory-NAME zurückgegeben, nicht der ganze Pfad
     */
    public String getDownloadDirectoryName() {
        if (!hasDownloadDirectory()) { return "."; }
        return new File(downloadDirectory).getName();
    }

    public ArrayList<DownloadLink> getDownloadLinkList() {
        return downloadLinkList;
    }

    @Deprecated
    public Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
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

    public String getId() {
        return id;
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

    public void resetUpdateTimer() {
        updateTime = 0;
        updateTime1 = 0;
    }

    public String getName() {
        if (name == null) return "";
        return name;
    }

    /**
     * 
     * @return Gibt das Archivpasswort zurück das der User für dieses paket
     *         angegeben hat
     */
    public String getPassword() {
        return password2 == null ? "" : password2;
    }

    /**
     * returns a list of archivepasswords set by downloadlinks
     */
    public ArrayList<String> getPasswordAuto() {
        ArrayList<String> pwList = new ArrayList<String>();
        synchronized (downloadLinkList) {
            for (DownloadLink element : downloadLinkList) {
                if (element.getSourcePluginPasswordList() != null) {
                    for (String pw : element.getSourcePluginPasswordList()) {
                        if (!pwList.contains(pw)) pwList.add(pw);
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
     * Gibt die erste gefundene sfv datei im Paket zurück
     * 
     * @return
     */
    public DownloadLink getSFV() {
        synchronized (downloadLinkList) {
            for (DownloadLink dl : downloadLinkList) {
                if (dl.getFileOutput().toLowerCase().endsWith(".sfv")) return dl;
            }
        }
        return null;
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
     * @return True/false, je nach dem ob ein Downloadirectory festgelegt wurde
     */
    public boolean hasDownloadDirectory() {
        return downloadDirectory != null && downloadDirectory.length() > 0;
    }

    public int indexOf(DownloadLink link) {
        synchronized (downloadLinkList) {
            return downloadLinkList.indexOf(link);
        }
    }

    public void remove(DownloadLink link) {
        if (link == null) return;
        synchronized (DownloadController.ControllerLock) {
            synchronized (downloadLinkList) {
                boolean ret = downloadLinkList.remove(link);
                if (ret) {
                    if (!link.isEnabled()) synchronized (links_Disabled) {
                        links_Disabled--;
                    }
                    link.getBroadcaster().removeListener(this);
                    link.setFilePackage(null);
                    broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.DOWNLOADLINK_REMOVED, link));
                    if (downloadLinkList.size() == 0) broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_EMPTY));
                }
            }
        }
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDownloadDirectory(String subFolder) {
        downloadDirectory = JDUtilities.removeEndingPoints(subFolder);
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(getDefaultFilePackage().name);
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        this.name = this.name.trim();
    }

    public void setPassword(String password) {
        this.password2 = password;
    }

    public int size() {
        synchronized (downloadLinkList) {
            return downloadLinkList.size();
        }
    }

    public void abortDownload() {
        synchronized (downloadLinkList) {
            for (DownloadLink downloadLink : downloadLinkList) {
                downloadLink.setAborted(true);
            }
        }
    }

    public ArrayList<DownloadLink> getLinksListbyStatus(int status) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        synchronized (downloadLinkList) {
            for (DownloadLink dl : downloadLinkList) {
                if (dl.getLinkStatus().hasStatus(status)) {
                    ret.add(dl);
                }
            }
        }
        return ret;
    }

    public String getHoster() {
        if (ListHoster == null) {
            Set<String> hosterList = new HashSet<String>();
            synchronized (downloadLinkList) {
                for (DownloadLink dl : downloadLinkList) {
                    hosterList.add(dl.getHost());
                }
            }
            ListHoster = hosterList.toString();
        }
        return ListHoster;
    }

    public void sort(final int col) {
        sortasc = !sortasc;
        synchronized (downloadLinkList) {

            Collections.sort(downloadLinkList, new Comparator<DownloadLink>() {

                public int compare(DownloadLink a, DownloadLink b) {
                    if (a.getName().endsWith(".sfv")) { return -1; }
                    if (b.getName().endsWith(".sfv")) { return 1; }
                    DownloadLink aa = b;
                    DownloadLink bb = a;
                    if (sortasc) {
                        aa = a;
                        bb = b;
                    }
                    switch (col) {
                    case 0:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 1:
                        return aa.getHost().compareToIgnoreCase(bb.getHost());
                    case 2:
                        if (aa.isAvailabilityStatusChecked() && bb.isAvailabilityStatusChecked()) {
                            return (aa.isAvailable() && !bb.isAvailable()) ? 1 : -1;
                        } else
                            return -1;
                    case 3:
                        return aa.getPercent() < bb.getPercent() ? 1 : -1;
                    default:
                        return -1;
                    }
                }
            });
        }
        broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_UPDATE));
    }

    /**
     * Alles undokumentiert, da selbsterklärend
     */
    // @Override
    public String toString() {
        return this.getName();
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
                    // logger.info(i+"+ "+next.getDownloadMax()/1024+" kb
                    // avg:"+avg+" = +"+totalEstimatedPackageSize);
                    i++;
                } else {
                    if (it.hasNext()) {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize_v2 += avg;
                        }

                        // logger.info(i+"+avg "+avg+" kb
                        // =+"+totalEstimatedPackageSize);

                    } else {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize_v2 += avg / 2;
                            // logger.info(i+"+avg "+(avg/2)+" kb
                            // =+"+totalEstimatedPackageSize);
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

    public int getLinksDisabled() {
        return links_Disabled;
    }

    public boolean isEnabled() {
        synchronized (downloadLinkList) {
            if (downloadLinkList.size() <= getLinksDisabled()) return false;
            return true;
        }
    }

    public void update_linksDisabled() {
        synchronized (links_Disabled) {
            links_Disabled = 0;
            synchronized (downloadLinkList) {
                for (DownloadLink dl : downloadLinkList) {
                    if (!dl.isEnabled()) links_Disabled++;
                }
            }
        }
    }

    public void onDownloadLinkEvent(DownloadLinkEvent event) {
        switch (event.getID()) {
        case DownloadLinkEvent.DISABLED:
            synchronized (links_Disabled) {
                links_Disabled++;
            }
            broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_UPDATE));
            break;
        case DownloadLinkEvent.ENABLED:
            synchronized (links_Disabled) {
                links_Disabled--;
            }
            broadcaster.fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_UPDATE));
            break;
        }
    }

    public void onFilePackageEvent(FilePackageEvent event) {
        switch (event.getID()) {
        case FilePackageEvent.DOWNLOADLINK_ADDED:
        case FilePackageEvent.DOWNLOADLINK_REMOVED:
            ListHoster = null;
            getHoster();
            break;
        }
    }

    public void convert() {
        this.downloadLinkList = new ArrayList<DownloadLink>();
        this.downloadLinkList.addAll(getDownloadLinks());
        this.downloadLinks = null;

    }

}
