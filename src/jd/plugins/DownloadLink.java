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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.config.Property;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.download.DownloadInterface;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download festgehalten. Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink extends Property implements Serializable, AbstractPackageChildrenNode<FilePackage>, CheckableLink {

    private static final String VARIANT_SUPPORT = "VARIANT_SUPPORT";

    public static enum AvailableStatus {
        UNCHECKED(_GUI._.linkgrabber_onlinestatus_unchecked()),
        FALSE(_GUI._.linkgrabber_onlinestatus_offline()),
        UNCHECKABLE(_GUI._.linkgrabber_onlinestatus_uncheckable()),
        TRUE(_GUI._.linkgrabber_onlinestatus_online());

        private final String exp;

        private AvailableStatus(String exp) {
            this.exp = exp;
        }

        public String getExplanation() {
            return exp;
        }
    }

    private static final String                                         PROPERTY_MD5                        = "MD5";
    private static final String                                         PROPERTY_SHA1                       = "SHA1";
    private static final String                                         PROPERTY_PASS                       = "pass";
    private static final String                                         PROPERTY_FINALFILENAME              = "FINAL_FILENAME";
    private static final String                                         PROPERTY_FORCEDFILENAME             = "FORCED_FILENAME";
    private static final String                                         PROPERTY_COMMENT                    = "COMMENT";
    private static final String                                         PROPERTY_PRIORITY                   = "PRIORITY";
    private static final String                                         PROPERTY_FINISHTIME                 = "FINISHTIME";
    private static final String                                         PROPERTY_ENABLED                    = "ENABLED";
    private static final String                                         PROPERTY_PWLIST                     = "PWLIST";
    private static final String                                         PROPERTY_LINKDUPEID                 = "LINKDUPEID";
    private static final String                                         PROPERTY_SPEEDLIMIT                 = "SPEEDLIMIT";
    private static final String                                         PROPERTY_VERIFIEDFILESIZE           = "VERIFIEDFILESIZE";
    public static final String                                          PROPERTY_RESUMEABLE                 = "PROPERTY_RESUMEABLE";
    public static final String                                          PROPERTY_FINALLOCATION              = "FINALLOCATION";
    public static final String                                          PROPERTY_CUSTOM_LOCALFILENAME       = "CUSTOM_LOCALFILENAME";
    public static final String                                          PROPERTY_CUSTOM_LOCALFILENAMEAPPEND = "CUSTOM_LOCALFILENAMEAPPEND";
    public static final String                                          PROPERTY_LASTFPNAME                 = "LASTFPNAME";
    public static final String                                          PROPERTY_LASTFPDEST                 = "LASTFPDEST";
    public static final String                                          PROPERTY_DOWNLOADTIME               = "DOWNLOADTIME";
    public static final String                                          PROPERTY_ARCHIVE_ID                 = "ARCHIVE_ID";
    public static final String                                          PROPERTY_EXTRACTION_STATUS          = "EXTRACTION_STATUS";
    public static final String                                          PROPERTY_CUSTOM_MESSAGE             = "CUSTOM_MESSAGE";

    public static final int                                             LINKTYPE_CONTAINER                  = 1;

    public static final int                                             LINKTYPE_NORMAL                     = 0;

    private static final long                                           serialVersionUID                    = 1981079856214268373L;

    private static final String                                         UNKNOWN_FILE_NAME                   = "unknownFileName";
    private static final String                                         PROPERTY_CHUNKS                     = "CHUNKS";

    private transient AvailableStatus                                   availableStatus                     = AvailableStatus.UNCHECKED;

    private long[]                                                      chunksProgress                      = null;

    /** Aktuell heruntergeladene Bytes der Datei */
    private long                                                        downloadCurrent                     = 0;

    private transient NullsafeAtomicReference<SingleDownloadController> downloadLinkController              = new NullsafeAtomicReference<SingleDownloadController>(null);

    /** Maximum der heruntergeladenen Datei (Dateilaenge) */
    private long                                                        downloadMax                         = -1;

    private String                                                      browserurl                          = null;

    private FilePackage                                                 filePackage;

    /** Hoster des Downloads */
    private String                                                      host;

    /* do not remove to keep stable compatibility */
    @SuppressWarnings("unused")
    private boolean                                                     isEnabled;

    @Deprecated
    private LinkStatus                                                  linkStatus;

    private int                                                         linkType                            = LINKTYPE_NORMAL;

    /** Beschreibung des Downloads */
    /* kann sich noch ändern, NICHT final */
    private String                                                      name;

    private transient PluginForHost                                     defaultplugin;

    private transient PluginForHost                                     liveplugin;

    /*
     * we need to keep this some time to perform conversion from variable to property
     */
    private String                                                      finalFileName;

    /**
     * /** Von hier soll der Download stattfinden
     */
    private String                                                      urlDownload;

    private transient NullsafeAtomicReference<PluginProgress>           pluginProgress                      = new NullsafeAtomicReference<PluginProgress>(null);

    private transient ImageIcon                                         icon                                = null;

    private long                                                        created                             = -1l;

    private transient UniqueAlltimeID                                   uniqueID                            = new UniqueAlltimeID();
    private transient AbstractNodeNotifier                              propertyListener;

    private transient DomainInfo                                        domainInfo                          = null;
    private transient Boolean                                           resumeable                          = null;
    private transient NullsafeAtomicReference<SkipReason>               skipReason                          = new NullsafeAtomicReference<SkipReason>(null);
    private transient NullsafeAtomicReference<ConditionalSkipReason>    conditionalSkipReason               = new NullsafeAtomicReference<ConditionalSkipReason>(null);
    private transient NullsafeAtomicReference<FinalLinkState>           finalLinkState                      = new NullsafeAtomicReference<FinalLinkState>(null);
    private transient AtomicBoolean                                     enabled                             = new AtomicBoolean(false);
    private transient UniqueAlltimeID                                   previousParent                      = null;
    private transient NullsafeAtomicReference<ExtractionStatus>         extractionStatus                    = new NullsafeAtomicReference<ExtractionStatus>();
    private transient NullsafeAtomicReference<LinkStatus>               currentLinkStatus                   = new NullsafeAtomicReference<LinkStatus>(null);
    private transient PartInfo                                          partInfo;
    private transient NullsafeAtomicReference<Property>                 tempProperties                      = new NullsafeAtomicReference<Property>(null);

    /**
     * these properties will not be saved/restored
     * 
     * @return
     */
    public Property getTempProperties() {
        while (true) {
            Property ret = tempProperties.get();
            if (ret != null) return ret;
            ret = new Property();
            if (tempProperties.compareAndSet(null, ret)) { return ret; }
        }
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
        setName(name);
        downloadMax = -1;
        setHost(host);
        this.isEnabled = isEnabled;
        enabled.set(isEnabled);
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
        return getLongProperty(PROPERTY_DOWNLOADTIME, 0l);
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
        skipReason = new NullsafeAtomicReference<SkipReason>(null);
        conditionalSkipReason = new NullsafeAtomicReference<ConditionalSkipReason>(null);
        enabled = new AtomicBoolean(isEnabled);
        downloadLinkController = new NullsafeAtomicReference<SingleDownloadController>(null);
        finalLinkState = new NullsafeAtomicReference<FinalLinkState>(null);
        currentLinkStatus = new NullsafeAtomicReference<LinkStatus>(null);
        pluginProgress = new NullsafeAtomicReference<PluginProgress>(null);
        availableStatus = AvailableStatus.UNCHECKED;
        try {
            if (linkStatus != null) {
                if (linkStatus.getStatus() == LinkStatus.FINISHED || linkStatus.hasStatus(LinkStatus.FINISHED)) {
                    setFinalLinkState(FinalLinkState.FINISHED);
                } else if (linkStatus.getStatus() == LinkStatus.ERROR_FILE_NOT_FOUND || linkStatus.hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                    setFinalLinkState(FinalLinkState.OFFLINE);
                } else if (linkStatus.getStatus() == LinkStatus.ERROR_FATAL || linkStatus.hasStatus(LinkStatus.ERROR_FATAL)) {
                    setFinalLinkState(FinalLinkState.FAILED_FATAL);
                }
            }
        } catch (final Throwable e) {
        }
        linkStatus = null;
    }

    public UniqueAlltimeID getUniqueID() {
        return uniqueID;
    }

    public Priority getPriorityEnum() {
        try {
            return Priority.getPriority(getPriority());
        } catch (final Throwable e) {
            return Priority.DEFAULT;
        }
    }

    @Deprecated
    /**
     * @deprecated use #getPriorityEnum
     * @return
     */
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
    }

    public int getCustomSpeedLimit() {
        return this.getIntegerProperty(PROPERTY_SPEEDLIMIT, 0);
    }

    @Deprecated
    /**
     * @deprecated Use #setPriorityEnum instead
     * @param pr
     * 
     */
    public void setPriority(int pr) {
        int oldPrio = getPriority();
        int priority = 0;
        if (pr >= -1 && pr < 4) {
            priority = pr;
        }
        if (oldPrio == priority) return;
        if (priority == 0) {
            this.setProperty(PROPERTY_PRIORITY, Property.NULL);
        } else {
            this.setProperty(PROPERTY_PRIORITY, priority);
        }
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.PRIORITY, priority));
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
        SingleDownloadController dlc = getDownloadLinkController();
        DownloadInterface dli = null;
        if (dlc != null && (dli = dlc.getDownloadInstance()) != null) {
            if (dli.getTotalLinkBytesLoadedLive() == 0 && downloadCurrent != 0) {
                return downloadCurrent;
            } else {
                return dli.getTotalLinkBytesLoadedLive();
            }
        }
        return downloadCurrent;
    }

    public SingleDownloadController getDownloadLinkController() {
        return downloadLinkController.get();
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
        SingleDownloadController dlc = getDownloadLinkController();
        DownloadInterface dli = null;
        if (dlc != null && (dli = dlc.getDownloadInstance()) != null) { return dli.getManagedConnetionHandler().getSpeed(); }
        return 0;
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

    public String getFinalFileOutput() {
        return this.getStringProperty(PROPERTY_FINALLOCATION, null);
    }

    public String getFileOutput() {
        return getFileOutput(false, false);
    }

    public String getFileOutput(boolean ignoreUnsafe, boolean ignoreCustom) {
        String ret = getFinalFileOutput();
        if (!StringUtils.isEmpty(ret)) {
            /* we have a fixed final location */
            return ret;
        }
        String downloadDirectory = getDownloadDirectory();
        String fileName = getCustomFileOutputFilename();
        if (!StringUtils.isEmpty(fileName) && !ignoreCustom) {
            /* we have a customized fileOutputFilename */
            return new File(downloadDirectory, fileName).getAbsolutePath();
        }
        fileName = getName(ignoreUnsafe);
        if (StringUtils.isEmpty(fileName)) return null;
        String customAppend = getCustomFileOutputFilenameAppend();
        if (!StringUtils.isEmpty(customAppend) && !ignoreCustom) fileName = fileName + customAppend;
        return new File(downloadDirectory, fileName).getAbsolutePath();
    }

    public String getDownloadDirectory() {
        FilePackage fp = getFilePackage();
        String downloadDirectory = getFilePackage().getDownloadDirectory();
        if (FilePackage.isDefaultFilePackage(fp)) {
            /* downloadLink has no longer a FilePackage parent, so fetch latest downloadDirectory from property(set by setFilePackage) */
            downloadDirectory = getStringProperty(PROPERTY_LASTFPDEST, null);
        }
        if (StringUtils.isEmpty(downloadDirectory)) throw new WTFException("what the fuck just happened here? defaultFilePackage: " + FilePackage.isDefaultFilePackage(fp));
        return downloadDirectory;
    }

    /**
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

    /**
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

    /**
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

    /**
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
        if (newHost == null) return;
        if (Application.getJavaVersion() >= Application.JAVA17) {
            host = newHost.toLowerCase(Locale.ENGLISH).intern();
        } else {
            host = newHost;
        }
    }

    public LinkStatus getLinkStatus() {
        Thread current = Thread.currentThread();
        if (current instanceof UseSetLinkStatusThread) { return currentLinkStatus.get(); }
        SingleDownloadController controller = getDownloadLinkController();
        if (controller != null) return controller.getLinkStatus();
        throw new WTFException("Cannot use getLinkStatus outside UseSetLinkStatusThread/SingleDownloadController");
    }

    public void setLinkStatus(LinkStatus linkStatus) {
        Thread current = Thread.currentThread();
        if (current instanceof UseSetLinkStatusThread) {
            currentLinkStatus.set(linkStatus);
        } else
            throw new WTFException("Cannot setLinkStatus outside UseSetLinkStatusThread");
    }

    public int getLinkType() {
        return linkType;
    }

    public String getName() {
        return getName(false);
    }

    /**
     * 
     * 
     * priority of returned fileName
     * 
     * 1.) forcedFileName (eg manually set)
     * 
     * 2.) finalFileName (eg set by plugin where the final is 100% safe, eg API)
     * 
     * 3.) unsafeFileName (eg set by plugin when no api is available, or no filename provided) ======= Liefert den Datei Namen dieses Downloads zurueck. Wurde
     * der Name mit setfinalFileName(String) festgelegt wird dieser Name zurueckgegeben >>>>>>> .r21593
     * 
     * @param ignoreUnsafe
     * @return
     */
    public String getName(boolean ignoreUnsafe) {
        String ret = this.getForcedFileName();
        if (ret != null) return ret;
        ret = this.getFinalFileName();
        if (ret != null) return ret;
        if (ignoreUnsafe) return null;
        try {

            if (name != null) return name;
            if (this.getDownloadURL() != null) {
                String urlName = new File(new URL(this.getDownloadURL()).toURI()).getName();
                if (urlName != null) return urlName;
            }
            return UNKNOWN_FILE_NAME;
        } catch (Exception e) {
            return UNKNOWN_FILE_NAME;
        }
    }

    public PartInfo getPartInfo() {
        if (partInfo == null) {
            partInfo = new PartInfo(this);
        }
        return partInfo;
    }

    private void setPartInfo(PartInfo info) {
        partInfo = info;
    }

    /**
     * returns fileName set by plugin (setFinalFileName)
     * 
     * @return
     */
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

    /**
     * Sets DownloadLinks Unquie ID
     * 
     * @param id
     * @since JD2
     */
    public void setLinkID(String id) {
        if (StringUtils.isEmpty(id)) {
            this.setProperty(PROPERTY_LINKDUPEID, Property.NULL);
        } else {
            this.setProperty(PROPERTY_LINKDUPEID, id);
        }
    }

    /*
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

    /*
     * WARNING: do not use withing plugins!
     */
    public AvailableStatus getAvailableStatus() {
        return availableStatus;
    }

    public void setAvailableStatus(AvailableStatus availableStatus) {
        if (this.availableStatus == availableStatus) return;
        this.availableStatus = availableStatus;
        switch (availableStatus) {
        case FALSE:
            if (getFinalLinkState() == null) setFinalLinkState(FinalLinkState.OFFLINE);
            break;
        case TRUE:
            if (FinalLinkState.OFFLINE.equals(getFinalLinkState())) setFinalLinkState(null);
            break;
        }
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.AVAILABILITY, availableStatus));
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

    public boolean hasNotificationListener() {
        AbstractNodeNotifier pl = propertyListener;
        if (pl != null && pl.hasNotificationListener()) return true;
        pl = filePackage;
        if (pl != null && pl.hasNotificationListener()) return true;
        return false;
    }

    public void reset() {
        setCustomFileOutputFilenameAppend(null);
        setCustomFileOutputFilename(null);
        setFinalFileName(null);
        setFinalLinkState(null);
        long size = getKnownDownloadSize();
        setVerifiedFileSize(-1);
        if (size >= 0) setDownloadSize(size);
        setChunksProgress(null);
        setDownloadCurrent(0);
        setFinishedDate(-1l);
        addDownloadTime(-1);
        setAvailableStatus(AvailableStatus.UNCHECKED);
        setSkipReason(null);
        setConditionalSkipReason(null);
        setEnabled(true);
        setPartInfo(null);
        setExtractionStatus(null);
        try {
            getDefaultPlugin().resetDownloadlink(this);
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.RESET, null));
    }

    public void resume() {
        try {
            getDefaultPlugin().resumeDownloadlink(this);
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
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
        if (hasNotificationListener() && this.getCurrentDownloadInterface() == null) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, null);
    }

    private DownloadInterface getCurrentDownloadInterface() {
        SingleDownloadController dlc = getDownloadLinkController();
        DownloadInterface dli = null;
        if (dlc != null && (dli = dlc.getDownloadInstance()) != null) return dli;
        return null;
    }

    public void setDownloadLinkController(SingleDownloadController downloadLinkController) {
        this.downloadLinkController.set(downloadLinkController);
    }

    /**
     * Setzt die Groesse der herunterzuladenden Datei
     * 
     * @param downloadMax
     *            Die Groesse der Datei
     */
    public void setDownloadSize(long downloadMax) {
        if (this.downloadMax == downloadMax) return;
        this.downloadMax = Math.max(-1, downloadMax);
        if (hasNotificationListener() && this.getCurrentDownloadInterface() == null) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, null);
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * changes the enabled status of this DownloadLink, aborts download if its currently running
     */
    public void setEnabled(boolean isEnabled) {
        if (enabled.getAndSet(isEnabled) == isEnabled) { return; }
        if (isEnabled == false) {
            setProperty(PROPERTY_ENABLED, isEnabled);
        } else {
            setProperty(PROPERTY_ENABLED, Property.NULL);
        }
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.ENABLED, isEnabled));
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isSkipped() {
        return skipReason.get() != null;
    }

    /**
     * changes the enabled status of this DownloadLink, aborts download if its currently running
     */
    public void setSkipReason(SkipReason skipReason) {
        SkipReason old = this.skipReason.getAndSet(skipReason);
        if (old == skipReason) { return; }
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.SKIPPED, skipReason));
    }

    public void setFinalLinkState(FinalLinkState finalLinkState) {
        if (this.finalLinkState.getAndSet(finalLinkState) == finalLinkState) { return; }
        if (FinalLinkState.CheckFinished(finalLinkState)) {
            setResumeable(false);
        }
        if (finalLinkState == null || !FinalLinkState.CheckFinished(finalLinkState)) {
            setFinishedDate(-1);
        }
        if (finalLinkState == FinalLinkState.OFFLINE) {
            setAvailable(false);
        }
        if (finalLinkState != FinalLinkState.FAILED_FATAL) {
            setProperty(PROPERTY_CUSTOM_MESSAGE, Property.NULL);
        }
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.FINAL_STATE, finalLinkState));
    }

    public FinalLinkState getFinalLinkState() {
        return finalLinkState.get();
    }

    public void setConditionalSkipReason(ConditionalSkipReason conditionalSkipReason) {
        if (this.conditionalSkipReason.getAndSet(conditionalSkipReason) == conditionalSkipReason) return;
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.CONDITIONAL_SKIPPED, conditionalSkipReason));
    }

    public ConditionalSkipReason getConditionalSkipReason() {
        return conditionalSkipReason.get();
    }

    public SkipReason getSkipReason() {
        return skipReason.get();
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
        if (plugin != null) {
            plugin.setDownloadLink(this);
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
        setPartInfo(null);
        this.setIcon(null);
        if (hasNotificationListener()) {
            String newName = getName();
            if (!StringUtils.equals(oldName, newName)) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, newName));
        }
    }

    /**
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
        setPartInfo(null);
        setIcon(null);
        if (hasNotificationListener()) {
            String newName = getName();
            if (!StringUtils.equals(oldName, newName)) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, newName));
        }
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
        String oldName = getName();
        finalFileName = null;
        if (!StringUtils.isEmpty(newfinalFileName)) {
            if (new Regex(newfinalFileName, Pattern.compile("r..\\.htm.?$", Pattern.CASE_INSENSITIVE)).matches()) {
                System.out.println("Use Workaround for stupid >>rar.html<< uploaders!");
                newfinalFileName = newfinalFileName.substring(0, newfinalFileName.length() - new Regex(newfinalFileName, Pattern.compile("r..(\\.htm.?)$", Pattern.CASE_INSENSITIVE)).getMatch(0).length());
            }
            this.setProperty(PROPERTY_FINALFILENAME, newfinalFileName = CrossSystem.alleviatePathParts(newfinalFileName));
            setName(newfinalFileName);
        } else {
            this.setProperty(PROPERTY_FINALFILENAME, Property.NULL);
        }
        setIcon(null);
        setPartInfo(null);
        if (hasNotificationListener()) {
            String newName = getName();
            if (!StringUtils.equals(oldName, newName)) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, newName));
        }
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
        if (getPreviousParentNodeID() == null) { return getName(); }
        if (getPreviousParentNodeID().equals(getParentNode().getUniqueID())) return getName();
        return getName() + " previousParentNode:" + getPreviousParentNodeID();

    }

    /**
     * returns real downloadMAx Value. use #getDownloadSize if you are not sure
     * 
     * @return
     */
    public long getKnownDownloadSize() {
        long ret = getVerifiedFileSize();
        if (ret >= 0) return ret;
        return downloadMax;
    }

    /**
     * DO NOT USE in 09581 Stable
     * 
     * @return
     * @since JD2
     */
    public String getDownloadPassword() {
        return getStringProperty(PROPERTY_PASS, null);
    }

    /**
     * DO NOT USE in 09581 Stable
     * 
     * @return
     * @since JD2
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
            this.setProperty(PROPERTY_SHA1, Property.NULL);
        }
    }

    public void firePropertyChanged(DownloadLinkProperty.Property property, Object param) {
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, property, param));
        }

    }

    public String getMD5Hash() {
        return getStringProperty(PROPERTY_MD5, (String) null);
    }

    public void setPluginProgress(PluginProgress progress) {
        if (pluginProgress.getAndSet(progress) == progress) return;
        System.out.println("Progress " + progress);
        if (hasNotificationListener()) notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.PLUGIN_PROGRESS, progress));
    }

    public PluginProgress getPluginProgress() {
        return pluginProgress.get();
    }

    public void setSha1Hash(String sha1) {
        if (StringUtils.isEmpty(sha1)) {
            this.setProperty(PROPERTY_SHA1, Property.NULL);
        } else {
            this.setProperty(PROPERTY_SHA1, sha1);
            this.setProperty(PROPERTY_MD5, Property.NULL);
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
            icon = getIcon(name);
        }
        return icon;
    }

    public static ImageIcon getIcon(String name) {
        ImageIcon newIcon = null;
        String ext = Files.getExtension(name);
        if (ext != null) {
            try {
                Icon ico = CrossSystem.getMime().getFileIcon(ext, 16, 16);
                newIcon = IconIO.toImageIcon(ico);
            } catch (Throwable e) {
                LogController.CL().log(e);
            }
        }
        if (newIcon == null) newIcon = NewTheme.I().getIcon("url", 16);
        return newIcon;
    }

    public DomainInfo getDomainInfo() {
        if (domainInfo == null) {
            DomainInfo newDomainInfo = null;
            if (defaultplugin != null) {
                newDomainInfo = defaultplugin.getDomainInfo(this);
            }
            if (newDomainInfo == null) {
                newDomainInfo = DomainInfo.getInstance(getHost());
            }
            domainInfo = newDomainInfo;
        }
        return domainInfo;
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
        if (filePackage == this.filePackage) {
            previousParent = null;
            return;
        }
        if (FilePackage.isDefaultFilePackage(filePackage)) filePackage = null;
        if (this.filePackage != null && filePackage != null) {
            this.filePackage.remove(this);
        }
        if (this.filePackage != null) {
            this.previousParent = this.filePackage.getUniqueID();
        }
        if (filePackage == null && this.filePackage != null) {
            this.setProperty(PROPERTY_LASTFPNAME, this.filePackage.getName());
            this.setProperty(PROPERTY_LASTFPDEST, this.filePackage.getDownloadDirectory());
        } else {
            this.setProperty(PROPERTY_LASTFPNAME, Property.NULL);
            this.setProperty(PROPERTY_LASTFPDEST, Property.NULL);
        }
        this.filePackage = filePackage;
    }

    public UniqueAlltimeID getPreviousParentNodeID() {
        return previousParent;
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

    public void setPriorityEnum(Priority priority) {
        setPriority(priority.getId());
    }

    public String getArchiveID() {
        return getStringProperty(DownloadLink.PROPERTY_ARCHIVE_ID);
    }

    public void setArchiveID(String id) {
        if (!StringUtils.isEmpty(id)) {
            setProperty(DownloadLink.PROPERTY_ARCHIVE_ID, id);
        } else {
            setProperty(DownloadLink.PROPERTY_ARCHIVE_ID, Property.NULL);
        }
    }

    public ExtractionStatus getExtractionStatus() {
        if (extractionStatus.isValueSet()) return extractionStatus.get();
        String string = getStringProperty(PROPERTY_EXTRACTION_STATUS, null);
        ExtractionStatus ret = null;
        try {
            if (string != null) ret = ExtractionStatus.valueOf(string);
        } catch (Exception e) {
        }
        extractionStatus.set(ret);
        return ret;
    }

    public void setExtractionStatus(ExtractionStatus newExtractionStatus) {
        ExtractionStatus old = extractionStatus.getAndSet(newExtractionStatus);
        if (old == newExtractionStatus) return;
        if (newExtractionStatus == null) {
            setProperty(DownloadLink.PROPERTY_EXTRACTION_STATUS, Property.NULL);
        } else {
            setProperty(DownloadLink.PROPERTY_EXTRACTION_STATUS, newExtractionStatus.name());
        }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.EXTRACTION_STATUS, newExtractionStatus));
        }
    }

    public void setVariantSupport(boolean b) {
        if (b) {
            setProperty(VARIANT_SUPPORT, b);
        } else {
            setProperty(VARIANT_SUPPORT, Property.NULL);
        }
    }

    public boolean hasVariantSupport() {
        return getBooleanProperty(VARIANT_SUPPORT, false) && !Application.isJared(DownloadLink.class);
    }

}