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
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.config.Property;
import jd.controlling.DownloadController;
import jd.controlling.JDLogger;
import jd.controlling.SingleDownloadController;
import jd.http.Browser;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.jdownloader.images.NewTheme;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download
 * festgehalten. Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink extends Property implements Serializable, Comparable<DownloadLink>, PackageLinkNode {

    public static enum AvailableStatus {
        UNCHECKED, FALSE, UNCHECKABLE, TRUE;
    }

    public static final int                    LINKTYPE_CONTAINER       = 1;

    public static final int                    LINKTYPE_NORMAL          = 0;

    private transient static Logger            logger                   = JDLogger.getLogger();

    private static final long                  serialVersionUID         = 1981079856214268373L;

    public static final String                 UNKNOWN_FILE_NAME        = "unknownFileName.file";

    public static final String                 STATIC_OUTPUTFILE        = "STATIC_OUTPUTFILE";

    private transient AvailableStatus          availableStatus          = AvailableStatus.UNCHECKED;

    private long[]                             chunksProgress           = null;

    /** Containername */
    private String                             container;

    /** Dateiname des Containers */
    private String                             containerFile            = null;

    /** Index dieses DownloadLinks innerhalb der Containerdatei */
    private int                                containerIndex           = -1;

    /** Aktuell heruntergeladene Bytes der Datei */
    private long                               downloadCurrent          = 0;

    private transient DownloadInterface        downloadInstance;

    private transient SingleDownloadController downloadLinkController;

    /** Maximum der heruntergeladenen Datei (Dateilaenge) */
    private long                               downloadMax              = 0;

    private String                             subdirectory             = null;

    private String                             browserurl               = null;

    private FilePackage                        filePackage;

    /** Hoster des Downloads */
    private String                             host;

    /** Zeigt an, ob dieser Downloadlink aktiviert ist */
    private boolean                            isEnabled;

    private LinkStatus                         linkStatus;

    private TransferStatus                     transferstatus;

    private int                                linkType                 = LINKTYPE_NORMAL;

    /** Beschreibung des Downloads */
    /* kann sich noch ändern, NICHT final */
    private String                             name;

    private transient PluginForHost            defaultplugin;

    private transient PluginForHost            liveplugin;

    /**
     * Falls vorhanden, das Plugin fuer den Container, aus der dieser Download
     * geladen wurde
     */
    private transient PluginsC                 pluginForContainer;

    private String                             sourcePluginComment      = null;

    private ArrayList<String>                  sourcePluginPasswordList = null;

    /**
     * Wird dieser Wert gesetzt, so wird der Download unter diesem Namen (nicht
     * Pfad) abgespeichert. (z.b. Plugins, DownloadSystem)
     */
    private String                             finalFileName;

    /**
     * if filename is set by jd (eg autorename) or user (manual rename of
     * filename), then this filename has highest priority
     */
    private String                             forcedFileName           = null;

    /**
     * /** Von hier soll der Download stattfinden
     */
    private String                             urlDownload;

    /**
     * Password welches einem weiteren Decrypter-Plugin uebergeben werden soll
     * (zb FolderPassword)
     */
    private String                             decrypterPassword;

    private String                             mD5Hash;

    private transient PluginProgress           pluginProgress;

    private String                             sha1Hash;

    private int                                priority                 = 0;

    private transient ImageIcon                icon                     = null;

    private long                               created                  = -1l;

    private long                               finishedDate             = -1l;

    /**
     * can be set via {@link #setCustomIcon(ImageIcon, String)} to set a custom
     * icon to be shown
     */
    private ImageIcon                          customIcon               = null;
    /**
     * can be set via {@link #setCustomIcon(ImageIcon, String)} to set a custom
     * tooltip to be shown
     */
    private String                             customIconText           = null;

    private transient int                      listOrderID              = 0;

    /**
     * @return the listOrderID
     */
    public int getListOrderID() {
        return listOrderID;
    }

    /**
     * @param listOrderID
     *            the listOrderID to set
     */
    public void setListOrderID(int listOrderID) {
        this.listOrderID = listOrderID;
    }

    /**
     * Erzeugt einen neuen DownloadLink
     * 
     * @param plugin
     *            Das Plugins, das fuer diesen Download zustaendig ist
     * @param name
     *            Bezeichnung des Downloads
     * @param host
     *            Anbieter, von dem dieser Download gestartet wird
     * @param urlDownload
     *            Die Download URL (Entschluesselt)
     * @param isEnabled
     *            Markiert diesen DownloadLink als aktiviert oder deaktiviert
     */
    public DownloadLink(PluginForHost plugin, String name, String host, String urlDownload, boolean isEnabled) {
        this.defaultplugin = plugin;

        priority = 0;
        setName(name);
        sourcePluginPasswordList = new ArrayList<String>();

        downloadMax = 0;
        this.host = host == null ? null : host.toLowerCase();
        this.isEnabled = isEnabled;
        created = System.currentTimeMillis();
        finishedDate = -1l;
        this.setUrlDownload(urlDownload);
        if (plugin != null && this.getDownloadURL() != null) {
            try {
                /*
                 * we have to use new instance here as correctDownloadLink might
                 * use a browser, create new browser instance too
                 */
                PluginForHost plg = defaultplugin.getWrapper().getNewPluginInstance();
                plg.setBrowser(new Browser());
                plg.correctDownloadLink(this);
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
        if (name == null && urlDownload != null) {
            this.name = Plugin.extractFileNameFromURL(getDownloadURL());
        }
    }

    public long getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(long finishedDate) {
        this.finishedDate = finishedDate;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public DownloadLink addSourcePluginPassword(String sourcePluginPassword) {
        if (sourcePluginPassword == null || sourcePluginPassword.trim().length() == 0) return this;
        String pwadd = sourcePluginPassword.trim();
        synchronized (sourcePluginPasswordList) {
            if (!sourcePluginPasswordList.contains(pwadd)) sourcePluginPasswordList.add(pwadd);
            return this;
        }
    }

    public void addSourcePluginPasswords(String[] sourcePluginPasswords) {
        if (sourcePluginPasswords == null || sourcePluginPasswords.length == 0) return;
        for (String sourcePluginPassword : sourcePluginPasswords) {
            addSourcePluginPassword(sourcePluginPassword);
        }
    }

    public void addSourcePluginPasswordList(ArrayList<String> sourcePluginPasswords) {
        if (sourcePluginPasswords == null || sourcePluginPasswords.size() == 0) return;
        for (int i = 0; i < sourcePluginPasswords.size(); i++) {
            addSourcePluginPassword(sourcePluginPasswords.get(i));
        }
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int pr) {
        if (pr >= -1 && pr < 4) {
            this.priority = pr;
        } else
            this.priority = 0;
    }

    /**
     * Uebernimmt das subdirectory von einem anderen Downloadlink. Zum erstellen
     * eines eigenen subdirectorys steht addSubdirectory(String s) zur
     * verfuegung.
     * 
     * @param downloadLink
     */
    public void setSubdirectory(DownloadLink downloadLink) {
        subdirectory = downloadLink.getSubdirectory();
    }

    public int compareTo(DownloadLink o) {
        return this.getDownloadURL().compareTo(o.getDownloadURL());
    }

    /**
     * Gibt ein arry mit den Chunkfortschritten zurueck. Dieses Array wird von
     * der Downlaodinstanz zu resumezwecken verwendet
     * 
     * @return
     */
    public long[] getChunksProgress() {
        return chunksProgress;
    }

    public String getContainer() {
        return container;
    }

    public String getContainerFile() {
        if (containerFile == null) return null;
        if (new File(containerFile).isAbsolute()) {
            containerFile = "container/" + new File(containerFile).getName();
        }
        return containerFile;
    }

    public int getContainerIndex() {
        return containerIndex;
    }

    /**
     * Liefert die bisher heruntergeladenen Bytes zurueck
     * 
     * @return Anzahl der heruntergeladenen Bytes
     */
    public long getDownloadCurrent() {
        return downloadCurrent;
    }

    public long getRemainingKB() {
        return getDownloadSize() - getDownloadCurrent();
    }

    public DownloadInterface getDownloadInstance() {
        return downloadInstance;
    }

    public SingleDownloadController getDownloadLinkController() {
        return downloadLinkController;
    }

    /**
     * Die Groesse der Datei
     * 
     * @return Die Groesse der Datei
     */
    public long getDownloadSize() {
        return Math.max(getDownloadCurrent(), downloadMax);
    }

    /**
     * Gibt die aktuelle Downloadgeschwindigkeit in bytes/sekunde zurueck
     * 
     * @return Downloadgeschwindigkeit in bytes/sekunde
     */
    public long getDownloadSpeed() {
        if (!getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) return 0;
        long currspeed = 0;
        DownloadInterface dli = getDownloadInstance();
        if (dli == null) return 0;
        synchronized (dli.getChunks()) {
            for (Chunk ch : dli.getChunks()) {
                if (ch.inProgress()) currspeed += ch.getSpeed();
            }
        }
        return currspeed;
    }

    /**
     * Liefert die URL zurueck, unter der dieser Download stattfinden soll
     * 
     * @return Die Download URL
     */
    public String getDownloadURL() {

        if (linkType == LINKTYPE_CONTAINER) {
            if (urlDownload != null) { return urlDownload; }
            if (pluginForContainer != null) {
                urlDownload = pluginForContainer.extractDownloadURL(this);
                if (urlDownload == null) {
                    logger.severe(this + " is a containerlink. Container could not be extracted. Is your JD Version up2date?");
                }
                return urlDownload;
            } else {
                logger.severe(this + " is a containerlink, but no plugin could be found");
                return null;

            }

        }
        return urlDownload;
    }

    public String getBrowserUrl() {
        if (browserurl != null) return browserurl;
        return getDownloadURL();
    }

    public void setBrowserUrl(String url) {
        browserurl = url;
    }

    public boolean gotBrowserUrl() {
        return browserurl != null;
    }

    /**
     * Liefert die Datei zurueck, in die dieser Download gespeichert werden soll
     * 
     * @return Die Datei zum Abspeichern
     */
    public String getFileOutput() {
        return getFileOutput0();
    }

    public String getFileOutput0() {
        String sfo = this.getStringProperty(DownloadLink.STATIC_OUTPUTFILE, null);
        if (getFilePackage() == FilePackage.getDefaultFilePackage() && sfo != null && new File(sfo).exists()) return sfo;

        if (getFilePackage() != null && getFilePackage().getDownloadDirectory() != null && getFilePackage().getDownloadDirectory().length() > 0) {
            if (subdirectory != null) {
                return new File(new File(getFilePackage().getDownloadDirectory(), File.separator + subdirectory), getName()).getAbsolutePath();
            } else {
                return new File(new File(getFilePackage().getDownloadDirectory()), getName()).getAbsolutePath();
            }
        } else {
            return null;
        }
    }

    /**
     * Gibt das Filepacket des Links zurueck. Kann auch null sein!! (Gui
     * abhaengig)
     * 
     * @return FilePackage
     */
    public FilePackage getFilePackage() {
        if (filePackage == null) {
            setFilePackage(FilePackage.getDefaultFilePackage());
        }
        return filePackage;
    }

    public boolean isDefaultFilePackage() {
        return (filePackage == null || filePackage == FilePackage.getDefaultFilePackage());
    }

    /**
     * Gibt den Hoster dieses Links azurueck.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost() {
        if (host == null) return null;
        return host;
    }

    public void setHost(String newHost) {
        this.host = newHost;
    }

    public LinkStatus getLinkStatus() {
        if (linkStatus == null) {
            linkStatus = new LinkStatus(this);
        }
        return linkStatus;
    }

    public TransferStatus getTransferStatus() {
        if (transferstatus == null) {
            transferstatus = new TransferStatus();
        }
        return transferstatus;
    }

    public int getLinkType() {
        return linkType;
    }

    /**
     * Liefert den Datei Namen dieses Downloads zurueck. Wurde der Name mit
     * setfinalFileName(String) festgelegt wird dieser Name zurueckgegeben
     * 
     * @return Name des Downloads
     */
    public String getName() {
        String urlName;
        String ret = this.getForcedFileName();
        if (ret != null) return ret;
        ret = this.getFinalFileName();
        if (ret != null) return ret;
        try {
            return name == null ? ((urlName = new File(new URL(this.getDownloadURL()).toURI()).getName()) != null ? urlName : UNKNOWN_FILE_NAME) : name;
        } catch (Exception e) {
            return UNKNOWN_FILE_NAME;
        }
    }

    /**
     * Liefert das Plugin zurueck, dass diesen DownloadLink handhabt
     * 
     * @return Das Plugin
     */
    public PluginForHost getDefaultPlugin() {
        return defaultplugin;
    }

    public PluginForHost getLivePlugin() {
        return liveplugin;
    }

    public PluginsC getPluginForContainer() {
        return pluginForContainer;
    }

    public String getSourcePluginComment() {
        return sourcePluginComment;
    }

    public ArrayList<String> getSourcePluginPasswordList() {
        return sourcePluginPasswordList;
    }

    /**
     * Gibt den Finalen Downloadnamen zurueck. Wird null zurueckgegeben, so wird
     * der dateiname von den jeweiligen plugins automatisch ermittelt.
     * 
     * @return Statischer Dateiname
     */
    public String getFinalFileName() {
        return finalFileName;
    }

    public String getForcedFileName() {
        return forcedFileName;
    }

    /**
     * @return true falls der Download abgebrochen wurde
     */
    public boolean isAborted() {
        SingleDownloadController dlc = getDownloadLinkController();
        if (dlc != null) return dlc.isAborted();
        DownloadInterface dli = this.getDownloadInstance();
        if (dli != null) return dli.externalDownloadStop();
        return false;
    }

    /**
     * Gibt zurueck ob Dieser Link schon auf verfuegbarkeit getestet wurde.+
     * Diese FUnktion fuehrt keinen!! Check durch. Sie prueft nur ob schon
     * geprueft worden ist. anschiessend kann mit isAvailable() die
     * verfuegbarkeit ueberprueft werden
     * 
     * @return Link wurde schon getestet (true) nicht getestet(false)
     */
    public boolean isAvailabilityStatusChecked() {
        return availableStatus != AvailableStatus.UNCHECKED;
    }

    /**
     * Fuehrt einen verfuegbarkeitscheck durch. GIbt true zurueck wenn der link
     * online ist, oder wenn er nicht pruefbar ist
     * 
     * @return true/false
     */
    public boolean isAvailable() {
        return getAvailableStatus() != AvailableStatus.FALSE;
    }

    public AvailableStatus getAvailableStatus() {
        return getAvailableStatus(null);
    }

    public AvailableStatus getAvailableStatus(PluginForHost plgToUse) {
        if (availableStatus != AvailableStatus.UNCHECKED) return availableStatus;
        int wait = 0;
        if (getDefaultPlugin() != null) {
            PluginForHost plg = plgToUse;
            /* we need extra plugin instance and browser instance too here */
            if (plg == null) {
                plg = getDefaultPlugin().getWrapper().getNewPluginInstance();
            }
            if (plg.getBrowser() == null) {
                plg.setBrowser(new Browser());
            }
            plg.init();
            for (int retry = 0; retry < 5; retry++) {
                try {
                    availableStatus = plg.requestFileInformation(this);
                    try {
                        plg.getBrowser().getHttpConnection().disconnect();
                    } catch (Exception e) {
                    }
                    break;
                } catch (UnknownHostException e) {
                    availableStatus = AvailableStatus.UNCHECKABLE;
                    break;
                } catch (PluginException e) {
                    e.fillLinkStatus(this.getLinkStatus());
                    if (this.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) || this.getLinkStatus().hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE) || this.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE)) {
                        availableStatus = AvailableStatus.UNCHECKABLE;
                    } else {
                        availableStatus = AvailableStatus.FALSE;
                    }
                    break;
                } catch (IOException e) {
                    if (e.getMessage().contains("code: 500")) {
                        try {
                            wait += 500;
                            JDLogger.getLogger().finer("500 Error Code, retrying in " + wait);
                            Thread.sleep(wait);
                        } catch (InterruptedException e1) {
                            availableStatus = AvailableStatus.UNCHECKABLE;
                            break;
                        }
                        continue;
                    } else {
                        break;
                    }

                } catch (Throwable e) {
                    JDLogger.exception(e);
                    availableStatus = AvailableStatus.UNCHECKABLE;
                    break;
                }
            }
        }
        if (availableStatus == null || getDefaultPlugin() == null) availableStatus = AvailableStatus.UNCHECKABLE;
        return availableStatus;
    }

    public void setAvailableStatus(AvailableStatus availableStatus) {
        this.availableStatus = availableStatus;
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    public void requestGuiUpdate() {
        DownloadController.getInstance().fireDownloadLinkUpdate(this);
    }

    /** Setzt alle DownloadWErte zurueck */
    public void reset() {
        chunksProgress = null;
        downloadLinkController = null;
        downloadCurrent = 0;
        finishedDate = -1l;
        linkStatus.reset();
        this.availableStatus = AvailableStatus.UNCHECKED;
        this.setEnabled(true);
        this.getTransferStatus().usePremium(false);
        this.getTransferStatus().setResumeSupport(false);
        deleteFile(true, true);
        finalFileName = null;
        PluginForHost plg = liveplugin;
        if (plg == null) {
            plg = defaultplugin.getWrapper().getNewPluginInstance();
        }
        if (plg.getBrowser() == null) {
            plg.setBrowser(new Browser());
        }
        plg.init();
        plg.resetDownloadlink(this);
    }

    /**
     * deletes the final downloaded file if finalfile is true deletes the
     * partfile if partfile is true deletes the downloadfolder if its emptry and
     * NOT equal to default downloadfolder
     */
    public void deleteFile(boolean partfile, boolean finalfile) {
        int maxtries = 5;
        while (getLinkStatus().isPluginActive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                JDLogger.exception(e);
            }
            maxtries--;
            if (maxtries == 0) break;
        }
        if (finalfile && new File(this.getFileOutput()).exists()) {
            if (!new File(this.getFileOutput()).delete()) {
                logger.severe("Could not delete file " + this.getFileOutput());
            }
        }
        if (partfile && new File(this.getFileOutput() + ".part").exists()) {
            if (!new File(this.getFileOutput() + ".part").delete()) {
                logger.severe("Could not delete file " + this.getFileOutput());
            }
        }

        /* try to delete folder (if its empty and NOT the default downloadfolder */
        File dlFolder = new File(this.getFileOutput()).getParentFile();
        if (dlFolder != null && dlFolder.exists() && dlFolder.isDirectory() && dlFolder.listFiles() != null && dlFolder.listFiles().length == 0) {
            if (!new File(JDUtilities.getDefaultDownloadDirectory()).equals(dlFolder)) dlFolder.delete();
        }
    }

    /** returns if partfile or completed file exists on disk */
    public boolean existsFile() {
        return new File(this.getFileOutput()).exists() || new File(this.getFileOutput() + ".part").exists();
    }

    /**
     * Kann mit setAborted(true) den Download abbrechen
     * 
     * @param aborted
     */
    public void setAborted(boolean aborted) {
        if (!aborted) return;
        SingleDownloadController dlc = this.getDownloadLinkController();
        if (dlc != null) {
            dlc.abortDownload();
        }
    }

    public void setAvailable(boolean available) {
        this.availableStatus = available ? AvailableStatus.TRUE : AvailableStatus.FALSE;
    }

    /**
     * Die Downloadklasse kann hier ein array mit den Fortschritten der chunks
     * ablegen. Damit koennen downloads resumed werden
     * 
     * @param is
     */
    public void setChunksProgress(long[] is) {
        chunksProgress = is;
    }

    public void setContainerFile(String containerFile) {
        if (containerFile == null) {
            this.containerFile = null;
            return;
        }
        containerFile = containerFile.replace("\\", "/");

        int index = containerFile.indexOf("/container/");
        if (index > 0) containerFile = containerFile.substring(index + 1);

        this.containerFile = containerFile;
    }

    public void setContainerIndex(int containerIndex) {
        this.containerIndex = containerIndex;
    }

    /**
     * Setzt die Anzahl der heruntergeladenen Bytes fest und aktualisiert die
     * Fortschrittsanzeige
     * 
     * @param downloadedCurrent
     *            Anzahl der heruntergeladenen Bytes
     */
    public void setDownloadCurrent(long downloadedCurrent) {
        downloadCurrent = downloadedCurrent;
    }

    public void setDownloadInstance(DownloadInterface downloadInterface) {
        downloadInstance = downloadInterface;
    }

    public void setDownloadLinkController(SingleDownloadController downloadLinkController) {
        this.downloadLinkController = downloadLinkController;
    }

    /**
     * Setzt die Groesse der herunterzuladenden Datei
     * 
     * @param downloadMax
     *            Die Groesse der Datei
     */
    public void setDownloadSize(long downloadMax) {
        this.downloadMax = downloadMax;
    }

    /**
     * Setzt ein Subdirectory fuer den DeonloadLink neu
     * 
     * @param downloadPath
     *            der neue downloadPfad
     */
    public void addSubdirectory(String subdir) {
        if (subdir != null && name.length() > 0) {
            if (subdirectory != null) {
                subdirectory = new StringBuilder(subdirectory).append(File.separator).append(JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(subdir))).toString();
            } else {
                subdirectory = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(subdir));
            }
        } else {
            subdirectory = null;
        }
    }

    public String getSubdirectory() {
        return subdirectory;
    }

    /**
     * Veraendert den Aktiviert-Status s ERROR_TEMPORARILY_UNAVAILABLE!!
     * 
     * @param isEnabled
     *            Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setEnabled(boolean isEnabled) {

        this.getLinkStatus().removeStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);

        this.isEnabled = isEnabled;
        if (!isEnabled) {
            setAborted(true);
        } else {
            if (host != null && defaultplugin == null) {
                logger.severe("Es ist kein passendes HostPlugin geladen");
                return;
            }
            if (container != null && pluginForContainer == null) {
                logger.severe("Es ist kein passendes ContainerPlugin geladen");
                return;
            }
        }
    }

    /**
     * Setzt das FilePackage fuer diesen Download
     * 
     * @param filePackage
     */
    public void setFilePackage(FilePackage filePackage) {

        if (filePackage != null && filePackage == this.filePackage) {
            if (!filePackage.contains(this)) {
                filePackage.add(this);
            }

            return;
        }
        if (this.filePackage != null) {
            this.filePackage.remove(this);
        }
        this.filePackage = filePackage;

        if (filePackage != null && !filePackage.contains(this)) {
            filePackage.add(this);
        }
    }

    public void setLinkType(int linktypeContainer) {
        if (linktypeContainer == linkType) return;
        if (linkType == LINKTYPE_CONTAINER) {
            logger.severe("You are not allowd to Change the Linktype of " + this);
            return;
        }
        linkType = linktypeContainer;
    }

    /**
     * Setzt nachtraeglich das Plugin. Wird nur zum Laden der Liste benoetigt
     * 
     * @param plugin
     *            Das fuer diesen Download zustaendige Plugin
     */
    public void setDefaultPlugin(PluginForHost plugin) {
        this.defaultplugin = plugin;
    }

    public void setLivePlugin(PluginForHost plugin) {
        this.liveplugin = plugin;
        if (liveplugin != null) {
            liveplugin.setDownloadLink(this);
        }
    }

    public void setLoadedPluginForContainer(PluginsC pluginForContainer) {
        this.pluginForContainer = pluginForContainer;
        container = pluginForContainer.getHost();
    }

    /**
     * Setzt den Namen des Downloads neu
     * 
     * @param name
     *            Neuer Name des Downloads
     */
    public void setName(String name) {
        if (name != null && name.length() > 0) {
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        }
        this.setIcon(null);

    }

    /*
     * use this function to force a name, it has highest priority
     */
    public void forceFileName(String name) {
        if (name == null || name.length() == 0) {
            this.forcedFileName = null;
        } else {
            setFinalFileName(name);
            this.forcedFileName = finalFileName;
        }
    }

    private void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public DownloadLink setSourcePluginComment(String sourcePluginComment) {
        this.sourcePluginComment = sourcePluginComment;
        return this;
    }

    public DownloadLink setSourcePluginPasswordList(ArrayList<String> sourcePluginPassword) {
        sourcePluginPasswordList = sourcePluginPassword;
        return this;
    }

    /**
     * Setzt den Statischen Dateinamen. Ist dieser wert != null, so wird er zum
     * Speichern der Datei verwendet. ist er == null, so wird der dateiName im
     * Plugin automatisch ermittelt. ACHTUNG: Der angegebene Dateiname ist
     * endgueltig. Diese Funktion sollte nach Moeglichkeit nicht von Plugins
     * verwendet werden. Sie gibt der Gui die Moeglichkeit unabhaengig von den
     * Plugins einen Downloadnamen festzulegen. Userinputs>Automatische
     * Erkennung - Plugins sollten {@link #setName(String)} verwenden um den
     * Speichernamen anzugeben.
     */
    public void setFinalFileName(String newfinalFileName) {
        setName(newfinalFileName);
        if (newfinalFileName != null && newfinalFileName.length() > 0) {
            if (new Regex(newfinalFileName, Pattern.compile("r..\\.htm.?$", Pattern.CASE_INSENSITIVE)).matches()) {
                logger.info("Use Workaround for stupid >>rar.html<< uploaders!");
                newfinalFileName = newfinalFileName.substring(0, newfinalFileName.length() - new Regex(newfinalFileName, Pattern.compile("r..(\\.htm.?)$", Pattern.CASE_INSENSITIVE)).getMatch(0).length());
            }
            finalFileName = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(newfinalFileName));
        } else {
            finalFileName = null;
        }
    }

    /**
     * Setzt die URL, von der heruntergeladen werden soll
     * 
     * @param urlDownload
     *            Die URL von der heruntergeladen werden soll
     */
    public void setUrlDownload(String urlDownload) {
        if (urlDownload != null) {
            this.urlDownload = urlDownload.trim();
        } else {
            this.urlDownload = null;
        }
    }

    /**
     * Diese Methhode fragt das eigene Plugin welche Informationen ueber die
     * File bereit gestellt werden. Der String eignet Sich zur Darstellung in
     * der UI
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Gibt Fortschritt in % an (10000 entspricht 100%))
     * 
     * @return
     */
    public int getPercent() {
        if (Math.min(downloadCurrent, downloadMax) <= 0) return 0;
        return (int) (10000 * downloadCurrent / Math.max(1, Math.max(downloadCurrent, downloadMax)));
    }

    /**
     * returns real downloadMAx Value. use #getDownloadSize if you are not sure
     * 
     * @return
     */
    public long getDownloadMax() {
        return downloadMax;
    }

    /**
     * Gibt das Password zurueck, welches vom Decrypter-Plugin genutzt werden
     * kann (zb. FolderPassword)
     */
    public String getDecrypterPassword() {
        return this.decrypterPassword;
    }

    /**
     * Setzt das Password, welches vom Decrypter-Plugin genutzt werden kann (zb.
     * FolderPassword)
     */
    public void setDecrypterPassword(String pw) {
        this.decrypterPassword = pw;
    }

    public void setMD5Hash(String string) {
        this.mD5Hash = string;
    }

    public String getMD5Hash() {
        return mD5Hash;
    }

    public void setPluginProgress(PluginProgress progress) {
        this.pluginProgress = progress;
    }

    public PluginProgress getPluginProgress() {
        return pluginProgress;
    }

    public void setSha1Hash(String hash) {
        this.sha1Hash = hash;
    }

    public String getSha1Hash() {
        return sha1Hash;
    }

    /* TODO: memfresser, anders machen */
    /**
     * ermittel das icon zur datei
     * 
     * @return
     */
    public ImageIcon getIcon() {
        if (icon == null) {
            if (OSDetector.isLinux() || OSDetector.isMac()) {

                icon = NewTheme.I().getIcon("url", 16);

            } else {
                try {
                    icon = JDImage.getScaledImageIcon(JDImage.getFileIcon(JDIO.getFileExtension(getFileOutput())), 16, 16);
                } catch (Exception e) {

                    icon = NewTheme.I().getIcon("url", 16);

                }

            }
        }
        return icon;
    }

    public String getType() {
        return JDIO.getFileType(JDIO.getFileExtension(getFileOutput()));
    }

    public static Set<String> getHosterList(ArrayList<DownloadLink> links) {
        HashMap<String, String> hosters = new HashMap<String, String>();
        for (DownloadLink dl : links) {
            if (!hosters.containsKey(dl.getHost())) {
                hosters.put(dl.getHost(), "");
            }
        }
        return hosters.keySet();
    }

    /**
     * @return the customIcon
     * @see #customIcon
     * @see #setCustomIcon(ImageIcon, String)
     */
    public ImageIcon getCustomIcon() {
        return customIcon;
    }

    /**
     * @return the customIconText
     * @see #customIconText
     * @see #setCustomIcon(ImageIcon, String)
     */
    public String getCustomIconText() {
        return customIconText;
    }

    /**
     * @param customIcon
     *            the customIcon to set
     * @param customIconText
     *            the customIconText to set
     * @see #customIcon
     * @see #getCustomIcon()
     */
    public void setCustomIcon(ImageIcon customIcon, String customIconText) {
        this.customIcon = customIcon;
        this.customIconText = customIconText;
    }

    /**
     * @return is a custom icon set?
     * @see #setCustomIcon(ImageIcon, String)
     */
    public boolean hasCustomIcon() {
        return this.customIcon != null && this.customIconText != null;
    }

    public DownloadLinkInfo getDownloadLinkInfo() {
        return DownloadLinkInfoCache.getInstance().get(this);
    }

}