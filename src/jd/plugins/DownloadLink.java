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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.config.Property;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.http.Browser;
import jd.plugins.download.DownloadInterface;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;
import org.jdownloader.utils.JDFileUtils;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download festgehalten. Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink extends Property implements Serializable, AbstractPackageChildrenNode<FilePackage>, CheckableLink {

    public static enum AvailableStatus {
        UNCHECKED,
        FALSE,
        UNCHECKABLE,
        TRUE;
    }

    private static final String                PROPERTY_MD5                        = "MD5";
    private static final String                PROPERTY_SHA1                       = "SHA1";
    private static final String                PROPERTY_PASS                       = "pass";
    private static final String                PROPERTY_FINALFILENAME              = "FINAL_FILENAME";
    private static final String                PROPERTY_FORCEDFILENAME             = "FORCED_FILENAME";
    private static final String                PROPERTY_COMMENT                    = "COMMENT";
    private static final String                PROPERTY_PRIORITY                   = "PRIORITY";
    private static final String                PROPERTY_FINISHTIME                 = "FINISHTIME";
    private static final String                PROPERTY_ENABLED                    = "ENABLED";
    private static final String                PROPERTY_PWLIST                     = "PWLIST";
    private static final String                PROPERTY_LINKDUPEID                 = "LINKDUPEID";
    private static final String                PROPERTY_SPEEDLIMIT                 = "SPEEDLIMIT";
    private static final String                PROPERTY_VERIFIEDFILESIZE           = "VERIFIEDFILESIZE";
    public static final String                 PROPERTY_RESUMEABLE                 = "PROPERTY_RESUMEABLE";
    public static final String                 PROPERTY_FINALLOCATION              = "FINALLOCATION";
    public static final String                 PROPERTY_CUSTOM_LOCALFILENAME       = "CUSTOM_LOCALFILENAME";
    public static final String                 PROPERTY_CUSTOM_LOCALFILENAMEAPPEND = "CUSTOM_LOCALFILENAMEAPPEND";
    public static final String                 PROPERTY_LASTFPNAME                 = "LASTFPNAME";
    public static final String                 PROPERTY_DOWNLOADTIME               = "DOWNLOADTIME";

    public static final int                    LINKTYPE_CONTAINER                  = 1;

    public static final int                    LINKTYPE_NORMAL                     = 0;

    private static final long                  serialVersionUID                    = 1981079856214268373L;

    private static final String                UNKNOWN_FILE_NAME                   = "unknownFileName";
    private static final String                PROPERTY_CHUNKS                     = "CHUNKS";

    private transient AvailableStatus          availableStatus                     = AvailableStatus.UNCHECKED;

    private long[]                             chunksProgress                      = null;

    /** Aktuell heruntergeladene Bytes der Datei */
    private long                               downloadCurrent                     = 0;

    private transient DownloadInterface        downloadInstance;

    private transient SingleDownloadController downloadLinkController;

    /** Maximum der heruntergeladenen Datei (Dateilaenge) */
    private long                               downloadMax                         = 0;

    private String                             browserurl                          = null;

    private FilePackage                        filePackage;

    /** Hoster des Downloads */
    private String                             host;

    /** Zeigt an, ob dieser Downloadlink aktiviert ist */
    private boolean                            isEnabled;

    private LinkStatus                         linkStatus;

    private int                                linkType                            = LINKTYPE_NORMAL;

    /** Beschreibung des Downloads */
    /* kann sich noch ändern, NICHT final */
    private String                             name;

    private transient PluginForHost            defaultplugin;

    private transient PluginForHost            liveplugin;

    /*
     * we need to keep this some time to perfom conversion from variable to property
     */
    private String                             finalFileName;

    /**
     * /** Von hier soll der Download stattfinden
     */
    private String                             urlDownload;

    private transient PluginProgress           pluginProgress;

    private transient ImageIcon                icon                                = null;

    private long                               created                             = -1l;

    private transient UniqueAlltimeID          uniqueID                            = new UniqueAlltimeID();
    transient private AbstractNodeNotifier     propertyListener;

    transient DomainInfo                       domainInfo                          = null;
    transient Boolean                          resumeable                          = null;
    private SkipReason                         skipReason                          = SkipReason.NONE;

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
        setName(name);
        downloadMax = 0;
        this.host = host == null ? null : host.toLowerCase(Locale.ENGLISH);
        this.isEnabled = isEnabled;
        created = System.currentTimeMillis();
        this.setUrlDownload(urlDownload);
        if (plugin != null && this.getDownloadURL() != null) {
            try {
                plugin.correctDownloadLink(this);
            } catch (Throwable e) {
                LogController.CL().log(e);
            }
        }
        if (name == null && urlDownload != null) {
            setName(Plugin.extractFileNameFromURL(getDownloadURL()));
        }
    }

    public long getFinishedDate() {
        return this.getLongProperty(PROPERTY_FINISHTIME, -1l);
    }

    public void setFinishedDate(long finishedDate) {
        if (finishedDate <= 0) {
            this.setProperty(PROPERTY_FINISHTIME, Property.NULL);
        } else {
            this.setProperty(PROPERTY_FINISHTIME, finishedDate);
        }
    }

    public void addDownloadTime(long time) {
        if (time < 0) {
            setProperty(PROPERTY_DOWNLOADTIME, Property.NULL);
        } else {
            setProperty(PROPERTY_DOWNLOADTIME, time + getDownloadTime());
        }
    }

    public long getDownloadTime() {
        return getLongProperty(PROPERTY_DOWNLOADTIME, 0);
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        /* deserialize object and then fill other stuff(transient..) */
        stream.defaultReadObject();
        uniqueID = new UniqueAlltimeID();
    }

    public UniqueAlltimeID getUniqueID() {
        return uniqueID;
    }

    public int getPriority() {
        return this.getIntegerProperty(PROPERTY_PRIORITY, 0);
    }

    public int getChunks() {
        return getIntegerProperty(PROPERTY_CHUNKS, -1);
    }

    public void setChunks(int chunks) {
        if (chunks <= 0) {
            setProperty(PROPERTY_CHUNKS, Property.NULL);
        } else {
            setProperty(PROPERTY_CHUNKS, chunks);
        }
    }

    public void setCustomSpeedLimit(int limit) {
        if (limit == 0) {
            setProperty(PROPERTY_SPEEDLIMIT, Property.NULL);
        } else {
            if (limit < 0) limit = 1;
            setProperty(PROPERTY_SPEEDLIMIT, limit);
        }
        // DownloadInterface dli = downloadInstance;
        // if (dli != null) dli.manageCustomSpeed(null);
    }

    public int getCustomSpeedLimit() {
        return this.getIntegerProperty(PROPERTY_SPEEDLIMIT, 0);
    }

    public void setPriority(int pr) {
        int oldPrio = getPriority();
        int priority = 0;
        if (pr >= -1 && pr < 4) {
            priority = pr;
        }
        if (priority == 0) {
            this.setProperty(PROPERTY_PRIORITY, Property.NULL);
        } else {
            this.setProperty(PROPERTY_PRIORITY, priority);
        }
        if (oldPrio != priority) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.PRIORITY, priority));
    }

    /**
     * Gibt ein arry mit den Chunkfortschritten zurueck. Dieses Array wird von der Downloadinstanz zu resumezwecken verwendet
     * 
     * @return
     */
    public long[] getChunksProgress() {
        return chunksProgress;
    }

    /**
     * Liefert die bisher heruntergeladenen Bytes zurueck
     * 
     * @return Anzahl der heruntergeladenen Bytes
     */
    public long getDownloadCurrent() {
        DownloadInterface dli = downloadInstance;
        if (dli != null) {
            if (dli.getTotalLinkBytesLoadedLive() == 0 && downloadCurrent != 0) {
                return downloadCurrent;
            } else {
                return dli.getTotalLinkBytesLoadedLive();
            }
        }
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
        DownloadInterface dli = getDownloadInstance();
        if (dli == null || !getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) return 0;
        return dli.getManagedConnetionHandler().getSpeed();
    }

    public String getDownloadURL() {
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

    public String getFileOutput() {
        String ret = this.getStringProperty(PROPERTY_FINALLOCATION, null);
        if (!StringUtils.isEmpty(ret)) {
            /* we have a fixed final location */
            return ret;
        }
        String downloadDirectory = getFilePackage().getDownloadDirectory();
        if (!StringUtils.isEmpty(downloadDirectory)) {
            String fileName = getCustomFileOutputFilename();
            if (StringUtils.isEmpty(fileName)) {
                fileName = getName();
                String customAppend = getCustomFileOutputFilenameAppend();
                if (!StringUtils.isEmpty(customAppend)) fileName = fileName + customAppend;
                return new File(downloadDirectory, fileName).getAbsolutePath();
            } else {
                return new File(downloadDirectory, fileName).getAbsolutePath();
            }
        } else {
            throw new WTFException("what the fuck just happened here?");
        }
    }

    /*
     * @since JD2
     */
    public String getCustomFileOutputFilename() {
        String ret = this.getStringProperty(PROPERTY_CUSTOM_LOCALFILENAME, null);
        if (!StringUtils.isEmpty(ret)) {
            /* we have a customized localfilename, eg xy.tmp */
            return ret;
        }
        return null;
    }

    /*
     * @since JD2
     */
    public String getCustomFileOutputFilenameAppend() {
        String ret = this.getStringProperty(PROPERTY_CUSTOM_LOCALFILENAMEAPPEND, null);
        if (!StringUtils.isEmpty(ret)) {
            /* we have a customized localfilename, eg xy.tmp */
            return ret;
        }
        return null;
    }

    /*
     * @since JD2
     */
    public void setCustomFileOutputFilename(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            setProperty(PROPERTY_CUSTOM_LOCALFILENAME, Property.NULL);
        } else {
            fileName = CrossSystem.alleviatePathParts(fileName);
            this.setProperty(PROPERTY_CUSTOM_LOCALFILENAME, fileName);
        }
    }

    /*
     * @since JD2
     */
    public void setCustomFileOutputFilenameAppend(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            setProperty(PROPERTY_CUSTOM_LOCALFILENAMEAPPEND, Property.NULL);
        } else {
            fileName = CrossSystem.alleviatePathParts(fileName);
            this.setProperty(PROPERTY_CUSTOM_LOCALFILENAMEAPPEND, fileName);
        }
    }

    /**
     * return the FilePackage that contains this DownloadLink, if none is set it will return defaultFilePackage
     * 
     * @return
     */
    public FilePackage getFilePackage() {
        FilePackage lFilePackage = filePackage;
        if (lFilePackage == null) return FilePackage.getDefaultFilePackage();
        return lFilePackage;
    }

    /**
     * Gibt den Hoster dieses Links azurueck.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost() {
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

    public int getLinkType() {
        return linkType;
    }

    /**
     * Liefert den Datei Namen dieses Downloads zurueck. Wurde der Name mit setfinalFileName(String) festgelegt wird dieser Name zurueckgegeben
     * 
     * @return Name des Downloads
     */
    public String getName() {
        String ret = this.getForcedFileName();
        if (ret != null) return ret;
        ret = this.getFinalFileName();
        if (ret != null) return ret;
        try {
            String urlName;
            return name == null ? ((urlName = new File(new URL(this.getDownloadURL()).toURI()).getName()) != null ? urlName : UNKNOWN_FILE_NAME) : name;
        } catch (Exception e) {
            return UNKNOWN_FILE_NAME;
        }
    }

    public String getNameSetbyPlugin() {
        String ret = this.getFinalFileName();
        if (ret != null) return ret;
        return name;
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

    public String getComment() {
        return this.getStringProperty(PROPERTY_COMMENT, null);
    }

    @Deprecated
    public List<String> getSourcePluginPasswordList() {
        Object ret = this.getProperty(PROPERTY_PWLIST);
        if (ret != null && ret instanceof List) return (List<String>) ret;
        return null;
    }

    /**
     * Gibt den Finalen Downloadnamen zurueck. Wird null zurueckgegeben, so wird der dateiname von den jeweiligen plugins automatisch ermittelt.
     * 
     * @return Statischer Dateiname
     */
    public String getFinalFileName() {
        if (finalFileName != null) {
            /* convert existing finalFileName into Property System */
            String lfinalFileName = finalFileName;
            finalFileName = null;
            this.setFinalFileName(lfinalFileName);
        }
        return this.getStringProperty(PROPERTY_FINALFILENAME, null);
    }

    public String getForcedFileName() {
        return this.getStringProperty(PROPERTY_FORCEDFILENAME, null);
    }

    public String getLinkID() {
        String ret = this.getStringProperty(PROPERTY_LINKDUPEID, null);
        return ret;
    }

    /* DO NOT USE in plugins for stable 09581, use set Property instead */
    public void setLinkID(String id) {
        if (StringUtils.isEmpty(id)) {
            this.setProperty(PROPERTY_LINKDUPEID, Property.NULL);
        } else {
            this.setProperty(PROPERTY_LINKDUPEID, id);
        }
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
     * this will abort an ongoing download of this DownloadLink, the abortDownload method of the SingleDownloadController will start a new Thread for
     * terminating of the download, so you will have to check manually when the download finally has stopped
     */
    public void abort() {
        SingleDownloadController dlc = getDownloadLinkController();
        if (dlc != null) {

            dlc.abortDownload();
        }
    }

    /**
     * Gibt zurueck ob Dieser Link schon auf verfuegbarkeit getestet wurde.+ Diese FUnktion fuehrt keinen!! Check durch. Sie prueft nur ob schon geprueft worden
     * ist. anschiessend kann mit isAvailable() die verfuegbarkeit ueberprueft werden
     * 
     * @return Link wurde schon getestet (true) nicht getestet(false)
     */
    public boolean isAvailabilityStatusChecked() {
        return availableStatus != AvailableStatus.UNCHECKED;
    }

    /**
     * Returns if the downloadLInk is available
     * 
     * @return true/false
     */
    public boolean isAvailable() {
        return availableStatus != AvailableStatus.FALSE;
    }

    public AvailableStatus getAvailableStatus() {
        return availableStatus;
    }

    public void setAvailableStatus(AvailableStatus availableStatus) {
        if (this.availableStatus == availableStatus) return;
        this.availableStatus = availableStatus;
        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.AVAILABILITY, availableStatus));
    }

    private void notifyChanges(AbstractNodeNotifier.NOTIFY notify, Object param) {
        AbstractNodeNotifier pl = propertyListener;
        if (pl != null) {
            pl.nodeUpdated(this, notify, param);
            return;
        }
        AbstractNodeNotifier pl2 = filePackage;
        if (pl2 != null) pl2.nodeUpdated(this, notify, param);
    }

    /** Setzt alle DownloadWErte zurueck */
    public void reset() {
        setCustomFileOutputFilenameAppend(null);
        setCustomFileOutputFilename(null);
        setVerifiedFileSize(-1);
        setFinalFileOutput(null);
        /* TODO: remove forced filename */
        chunksProgress = null;
        downloadLinkController = null;
        downloadCurrent = 0;
        this.setFinishedDate(-1l);
        linkStatus.reset();
        addDownloadTime(-1);
        this.availableStatus = AvailableStatus.UNCHECKED;
        setSkipReason(SkipReason.NONE);
        this.setEnabled(true);
        deleteFile(null, true, true);
        setFinalFileName(null);
        try {
            getDefaultPlugin().resetDownloadlink(this);
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        if (this.getDownloadInstance() == null) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.RESET, null));
    }

    /**
     * deletes the final downloaded file if finalfile is true deletes the partfile if partfile is true deletes the downloadfolder if its emptry and NOT equal to
     * default downloadfolder
     * 
     * @param deleteTo
     *            TODO
     */
    public void deleteFile(DeleteTo deleteTo, boolean partfile, boolean finalfile) {
        int maxtries = 5;
        while (getLinkStatus().isPluginActive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LogController.CL().log(e);
            }
            maxtries--;
            if (maxtries == 0) break;
        }

        if (finalfile && new File(this.getFileOutput()).exists()) {
            File f = new File(this.getFileOutput());
            if (!internalDeleteFile(deleteTo, f)) {

                LogController.CL().severe("Could not delete file " + this.getFileOutput());
            }
        }
        if (partfile && new File(this.getFileOutput() + ".part").exists()) {
            if (!internalDeleteFile(deleteTo, new File(this.getFileOutput() + ".part"))) {
                LogController.CL().severe("Could not delete file " + this.getFileOutput());
            }
        }
        this.setProperty(PROPERTY_FINALLOCATION, Property.NULL);

        /* try to delete folder (if its empty and NOT the default downloadfolder */
        File dlFolder = new File(this.getFileOutput()).getParentFile();
        if (dlFolder != null && dlFolder.exists() && dlFolder.isDirectory() && dlFolder.listFiles() != null && dlFolder.listFiles().length == 0) {
            if (!new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()).equals(dlFolder)) internalDeleteFile(deleteTo, dlFolder);
        }
    }

    private boolean internalDeleteFile(DeleteTo deleteTo, File f) {
        if (deleteTo == null) return f.delete();
        switch (deleteTo) {
        case NULL:
            return f.delete();
        case RECYCLE:
            try {
                JDFileUtils.moveToTrash(f);
            } catch (IOException e) {

                LogController.CL().log(e);
                return false;
            }

        }
        return false;
    }

    /** returns if partfile or completed file exists on disk */
    public boolean existsFile() {
        return new File(this.getFileOutput()).exists() || new File(this.getFileOutput() + ".part").exists();
    }

    public void setAvailable(boolean available) {
        setAvailableStatus(available ? AvailableStatus.TRUE : AvailableStatus.FALSE);
    }

    /**
     * Die Downloadklasse kann hier ein array mit den Fortschritten der chunks ablegen. Damit koennen downloads resumed werden
     * 
     * @param is
     */
    public void setChunksProgress(long[] is) {
        chunksProgress = is;
    }

    /**
     * Setzt die Anzahl der heruntergeladenen Bytes fest und aktualisiert die Fortschrittsanzeige
     * 
     * @param downloadedCurrent
     *            Anzahl der heruntergeladenen Bytes
     */
    public void setDownloadCurrent(long downloadedCurrent) {
        if (downloadCurrent == downloadedCurrent) return;
        downloadCurrent = downloadedCurrent;
        if (this.getDownloadInstance() == null) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, null);
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
        if (this.downloadMax == downloadMax) return;
        this.downloadMax = downloadMax;
        if (this.getDownloadInstance() == null) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, null);
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /*
     * changes the enabled status of this DownloadLink, aborts download if its currently running
     */
    public void setEnabled(boolean isEnabled) {
        this.getLinkStatus().removeStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        boolean changed = this.isEnabled != isEnabled;
        this.isEnabled = isEnabled;
        if (isEnabled == false) {
            abort();
        }
        if (changed == false) return;
        if (isEnabled == true) {
            setProperty(PROPERTY_ENABLED, Property.NULL);
        } else {
            setProperty(PROPERTY_ENABLED, isEnabled);
        }
        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.ENABLED, isEnabled));
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isSkipped() {
        return SkipReason.NONE != skipReason;
    }

    /*
     * changes the enabled status of this DownloadLink, aborts download if its currently running
     */
    public void setSkipReason(SkipReason skipReason) {
        if (skipReason == null) skipReason = SkipReason.NONE;
        switch (skipReason) {
        case INVALID_DESTINATION:
            getLinkStatus().setStatusText(_JDT._.downloadlink_status_error_invalid_dest());
            break;
        case NONE:
            getLinkStatus().setStatusText(null);
            break;
        default:
            getLinkStatus().setStatusText(_JDT._.DownloadLink_setSkipped_statusmessage());
        }
        boolean changed = this.skipReason != skipReason;
        if (!changed) return;
        this.skipReason = skipReason;
        if (isSkipped()) {
            abort();
        }
        if (changed) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.SKIPPED, skipReason));
    }

    public void setLinkType(int linktypeContainer) {
        if (linktypeContainer == linkType) return;
        if (linkType == LINKTYPE_CONTAINER) {
            System.out.println("You are not allowd to Change the Linktype of " + this);
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

    /**
     * Setzt den Namen des Downloads neu
     * 
     * @param name
     *            Neuer Name des Downloads
     */
    public void setName(String name) {
        String oldName = getName();
        if (StringUtils.isEmpty(name)) name = Plugin.extractFileNameFromURL(getDownloadURL());
        if (!StringUtils.isEmpty(name)) {
            name = CrossSystem.alleviatePathParts(name);
        }
        if (StringUtils.isEmpty(name)) name = UNKNOWN_FILE_NAME;
        this.name = name;
        this.setIcon(null);
        if (!oldName.equals(getName())) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, getName()));
    }

    /*
     * use this function to force a name, it has highest priority
     */
    public void forceFileName(String name) {
        String oldName = getName();
        if (StringUtils.isEmpty(name)) {
            this.setProperty(PROPERTY_FORCEDFILENAME, Property.NULL);
            oldName = getName();
        } else {
            name = CrossSystem.alleviatePathParts(name);
            this.setProperty(PROPERTY_FORCEDFILENAME, name);
        }
        setIcon(null);
        if (!oldName.equals(getName())) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, getName()));
    }

    private void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    /**
     * WARNING: DO NOT use in 09581 stable!
     * 
     * @since JD2
     */
    public void setComment(String comment) {
        if (comment == null || comment.length() == 0) {
            this.setProperty(PROPERTY_COMMENT, Property.NULL);
        } else {
            this.setProperty(PROPERTY_COMMENT, comment);
        }
    }

    @Deprecated
    public DownloadLink setSourcePluginPasswordList(ArrayList<String> sourcePluginPassword) {
        if (sourcePluginPassword == null || sourcePluginPassword.size() == 0) {
            this.setProperty(PROPERTY_PWLIST, Property.NULL);
        } else {
            this.setProperty(PROPERTY_PWLIST, sourcePluginPassword);
        }
        return this;
    }

    /**
     * Setzt den Statischen Dateinamen. Ist dieser wert != null, so wird er zum Speichern der Datei verwendet. ist er == null, so wird der dateiName im Plugin
     * automatisch ermittelt. ACHTUNG: Der angegebene Dateiname ist endgueltig. Diese Funktion sollte nach Moeglichkeit nicht von Plugins verwendet werden. Sie
     * gibt der Gui die Moeglichkeit unabhaengig von den Plugins einen Downloadnamen festzulegen. Userinputs>Automatische Erkennung - Plugins sollten
     * {@link #setName(String)} verwenden um den Speichernamen anzugeben.
     */
    public void setFinalFileName(String newfinalFileName) {
        String oldName = null;
        finalFileName = null;
        if (!StringUtils.isEmpty(newfinalFileName)) {
            if (new Regex(newfinalFileName, Pattern.compile("r..\\.htm.?$", Pattern.CASE_INSENSITIVE)).matches()) {
                System.out.println("Use Workaround for stupid >>rar.html<< uploaders!");
                newfinalFileName = newfinalFileName.substring(0, newfinalFileName.length() - new Regex(newfinalFileName, Pattern.compile("r..(\\.htm.?)$", Pattern.CASE_INSENSITIVE)).getMatch(0).length());
            }
            this.setProperty(PROPERTY_FINALFILENAME, newfinalFileName = CrossSystem.alleviatePathParts(newfinalFileName));
            setName(newfinalFileName);
            oldName = getName();
        } else {
            this.setProperty(PROPERTY_FINALFILENAME, Property.NULL);
            oldName = getName();
        }
        setIcon(null);
        if (!oldName.equals(getName())) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, getName()));
    }

    /**
     * Setzt die URL, von der heruntergeladen werden soll
     * 
     * @param urlDownload
     *            Die URL von der heruntergeladen werden soll
     */
    public void setUrlDownload(String urlDownload) {
        String previousURLDownload = this.urlDownload;
        if (urlDownload != null) {
            if (previousURLDownload != null && previousURLDownload.equals(urlDownload)) return;
            this.urlDownload = new String(urlDownload.trim());
        } else {
            this.urlDownload = null;
        }
        if (previousURLDownload != null && !previousURLDownload.equals(urlDownload)) {
            if (getLinkID() == null) {
                /* downloadURL changed, so set original one as linkID, so all dupemaps still work */
                setLinkID(previousURLDownload);
            }
        }
    }

    /**
     * Diese Methhode fragt das eigene Plugin welche Informationen ueber die File bereit gestellt werden. Der String eignet Sich zur Darstellung in der UI
     */
    @Override
    public String toString() {
        return getName();
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
     * DO NOT USE in 09581 Stable
     * 
     * @return
     */
    public String getDownloadPassword() {
        return getStringProperty(PROPERTY_PASS, null);
    }

    /**
     * DO NOT USE in 09581 Stable
     * 
     * @return
     */
    public void setDownloadPassword(String pass) {
        if (StringUtils.isEmpty(pass)) {
            this.setProperty(PROPERTY_PASS, Property.NULL);
        } else {
            this.setProperty(PROPERTY_PASS, pass);
        }
    }

    public void setMD5Hash(String md5) {
        if (StringUtils.isEmpty(md5)) {
            this.setProperty(PROPERTY_MD5, Property.NULL);
        } else {
            this.setProperty(PROPERTY_MD5, md5);
        }
    }

    public String getMD5Hash() {
        return getStringProperty(PROPERTY_MD5, (String) null);
    }

    public void setPluginProgress(PluginProgress progress) {
        this.pluginProgress = progress;
    }

    public PluginProgress getPluginProgress() {
        return pluginProgress;
    }

    public void setSha1Hash(String sha1) {
        if (StringUtils.isEmpty(sha1)) {
            this.setProperty(PROPERTY_SHA1, Property.NULL);
        } else {
            this.setProperty(PROPERTY_SHA1, sha1);
        }
    }

    public String getSha1Hash() {
        return getStringProperty(PROPERTY_SHA1, (String) null);
    }

    /**
     * Do not use in Plugins for old Stable, or use try/catch or set property manually
     * 
     * @param size
     */
    public void setVerifiedFileSize(long size) {
        setDownloadSize(size);
        if (size < 0) {
            setProperty(DownloadLink.PROPERTY_VERIFIEDFILESIZE, Property.NULL);
        } else {
            setProperty(DownloadLink.PROPERTY_VERIFIEDFILESIZE, size);
        }
    }

    public long getVerifiedFileSize() {
        return getLongProperty(PROPERTY_VERIFIEDFILESIZE, -1);
    }

    /**
     * Do not use in Plugins for old Stable, or use try/catch or set property manually
     * 
     * @param size
     */
    public void setResumeable(boolean b) {
        resumeable = b;
        if (!b) {
            setProperty(PROPERTY_RESUMEABLE, Property.NULL);
        } else {
            setProperty(PROPERTY_RESUMEABLE, true);
        }
    }

    public boolean isResumeable() {
        if (resumeable != null) return resumeable;
        resumeable = getBooleanProperty(PROPERTY_RESUMEABLE, false);
        return resumeable;
    }

    /* TODO: memfresser, anders machen */
    /**
     * ermittel das icon zur datei
     * 
     * @return
     */
    public ImageIcon getIcon() {
        if (icon == null) {
            String ext = Files.getExtension(getName());
            if (ext != null) {
                try {
                    icon = CrossSystem.getMime().getFileIcon(ext, 16, 16);
                } catch (Throwable e) {
                    LogController.CL().log(e);
                }
            }
            if (icon == null) icon = NewTheme.I().getIcon("url", 16);
        }
        return icon;
    }

    public DomainInfo getDomainInfo(boolean preferOriginalDomainInfo) {
        PluginForHost plugin = this.liveplugin;
        if (plugin != null && preferOriginalDomainInfo == false) {
            /* live plugin available, lets use it */
            return plugin.getDomainInfo();
        } else {
            if (domainInfo == null) {
                if ("ftp".equalsIgnoreCase(getHost()) || "DirectHTTP".equalsIgnoreCase(getHost()) || "http links".equalsIgnoreCase(getHost())) {
                    /* custom iconHost */
                    try {
                        String url = Browser.getHost(new URL(getDownloadURL()));
                        domainInfo = DomainInfo.getInstance(url);
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                }
                if (domainInfo == null) {
                    domainInfo = DomainInfo.getInstance(getHost());
                }
            }
        }
        return domainInfo;
    }

    public DomainInfo getDomainInfo() {
        return getDomainInfo(false);
    }

    public FilePackage getParentNode() {
        return getFilePackage();
    }

    /**
     * set the FilePackage that contains this DownloadLink, DO NOT USE this if you want to add this DownloadLink to a FilePackage
     * 
     * @param filePackage
     */
    public synchronized void _setFilePackage(FilePackage filePackage) {
        if (filePackage == this.filePackage) return;
        if (FilePackage.isDefaultFilePackage(filePackage)) filePackage = null;
        if (this.filePackage != null && filePackage != null) {
            this.filePackage.remove(this);
        }
        if (filePackage == null && this.filePackage != null) {
            this.setProperty(PROPERTY_LASTFPNAME, this.filePackage.getName());
        } else {
            this.setProperty(PROPERTY_LASTFPNAME, Property.NULL);
        }
        this.filePackage = filePackage;
    }

    public void setParentNode(FilePackage parent) {
        _setFilePackage(parent);
    }

    public DownloadLink getDownloadLink() {
        return this;
    }

    public void setNodeChangeListener(AbstractNodeNotifier propertyListener) {
        this.propertyListener = propertyListener;
    }

    @Deprecated
    /**
     * @deprecated use #getDefaultPlugin
     * @return
     */
    public PluginForHost getPlugin() {
        return this.liveplugin;
    }

    /**
     * WARNING: DO NOT use in 09581 stable!
     * 
     * @since JD2
     */
    public void setFinalFileOutput(String absolutePath) {
        if (!StringUtils.isEmpty(absolutePath)) {
            setProperty(DownloadLink.PROPERTY_FINALLOCATION, absolutePath);
        } else {
            setProperty(DownloadLink.PROPERTY_FINALLOCATION, Property.NULL);
        }
    }

}