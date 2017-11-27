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
import java.io.ObjectInputStream.GetField;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import jd.config.Property;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.HistoryEntry;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.DownloadLinkDatabindingInterface.Key;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashInfo.TYPE;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.DefaultDownloadLinkViewImpl;
import org.jdownloader.controlling.DownloadLinkView;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.UrlProtection;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.linkcrawler.GenericVariants;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download festgehalten. Die Informationen werden dann in einer Tabelle
 * dargestellt
 *
 * @author astaldo
 */
public class DownloadLink extends Property implements Serializable, AbstractPackageChildrenNode<FilePackage>, CheckableLink {
    public static enum AvailableStatus {
        UNCHECKED(_GUI.T.linkgrabber_onlinestatus_unchecked()),
        FALSE(_GUI.T.linkgrabber_onlinestatus_offline()),
        UNCHECKABLE(_GUI.T.linkgrabber_onlinestatus_uncheckable()),
        TRUE(_GUI.T.linkgrabber_onlinestatus_online());
        private final String exp;

        private AvailableStatus(String exp) {
            this.exp = exp;
        }

        public String getExplanation() {
            return exp;
        }
    }

    private static final String                         PROPERTY_MD5                        = "MD5";
    private static final String                         PROPERTY_HASHINFO                   = "HASHINFO";
    private static final String                         PROPERTY_MIRRORID                   = "MID";
    private static final String                         PROPERTY_SHA1                       = "SHA1";
    private static final String                         PROPERTY_SHA256                     = "SHA256";
    private static final String                         PROPERTY_PASS                       = "pass";
    private static final String                         PROPERTY_FINALFILENAME              = "FINAL_FILENAME";
    private static final String                         PROPERTY_FORCEDFILENAME             = "FORCED_FILENAME";
    private static final String                         PROPERTY_COMMENT                    = "COMMENT";
    private static final String                         PROPERTY_PRIORITY                   = "PRIORITY2";
    private static final String                         PROPERTY_FINISHTIME                 = "FINISHTIME";
    private static final String                         PROPERTY_PWLIST                     = "PWLIST";
    private static final String                         PROPERTY_LINKDUPEID                 = "LINKDUPEID";
    private static final String                         PROPERTY_SPEEDLIMIT                 = "SPEEDLIMIT";
    private static final String                         PROPERTY_VERIFIEDFILESIZE           = "VERIFIEDFILESIZE";
    public static final String                          PROPERTY_RESUMEABLE                 = "PROPERTY_RESUMEABLE";
    public static final String                          PROPERTY_CUSTOM_LOCALFILENAME       = "CUSTOM_LOCALFILENAME";
    public static final String                          PROPERTY_CUSTOM_LOCALFILENAMEAPPEND = "CUSTOM_LOCALFILENAMEAPPEND";
    public static final String                          PROPERTY_DOWNLOADTIME               = "DOWNLOADTIME";
    public static final String                          PROPERTY_ARCHIVE_ID                 = "ARCHIVE_ID";
    public static final String                          PROPERTY_EXTRACTION_STATUS          = "EXTRACTION_STATUS";
    public static final String                          PROPERTY_CUSTOM_MESSAGE             = "CUSTOM_MESSAGE";
    public static final String                          PROPERTY_MIME_HINT                  = "MIME_HINT";
    private static final long                           serialVersionUID                    = 1981079856214268373L;
    private static final String                         UNKNOWN_FILE_NAME                   = "unknownFileName";
    private static final String                         PROPERTY_CHUNKS                     = "CHUNKS";
    private static final String                         URL_ORIGIN                          = "URL_ORIGIN";
    private static final String                         URL_REFERRER                        = "URL_REFERRER";
    private static final String                         URL_CONTAINER                       = "URL_CONTAINER";
    private static final String                         URL_CONTENT                         = "URL_CONTENT";
    private static final String                         URL_CUSTOM                          = "URL_CUSTOM";
    private static final String                         VARIANT_SUPPORT                     = "VARIANT_SUPPORT";
    public static final String                          PROPERTY_JOB_ID                     = "JOB_ID";
    private transient volatile AvailableStatus          availableStatus                     = AvailableStatus.UNCHECKED;
    @Deprecated
    private long[]                                      chunksProgress                      = null;
    /** Aktuell heruntergeladene Bytes der Datei */
    private long                                        downloadCurrent                     = 0;
    private transient volatile SingleDownloadController downloadLinkController              = null;
    /** Maximum der heruntergeladenen Datei (Dateilaenge) */
    private long                                        downloadMax                         = -1;
    private FilePackage                                 filePackage;
    /** Hoster des Downloads */
    private String                                      host;
    private boolean                                     isEnabled;
    /** Beschreibung des Downloads */
    /* kann sich noch ändern, NICHT final */
    private String                                      name;
    private transient PluginForHost                     defaultplugin;
    private transient PluginForHost                     liveplugin;
    /**
     * Do not rename urlDownload. We need this field to restore old downloadlinks from the jd09 database
     */
    private String                                      urlDownload;
    private transient volatile List<PluginProgress>     pluginProgress                      = null;
    private long                                        created                             = -1l;
    private transient UniqueAlltimeID                   uniqueID                            = null;
    private transient AbstractNodeNotifier              propertyListener;
    private transient DomainInfo                        domainInfo                          = null;
    private transient volatile SkipReason               skipReason                          = null;
    private transient volatile ConditionalSkipReason    conditionalSkipReason               = null;
    private transient volatile FinalLinkState           finalLinkState                      = null;
    private transient UniqueAlltimeID                   previousParent                      = null;
    private transient volatile ExtractionStatus         extractionStatus                    = null;
    private transient Property                          tempProperties                      = null;
    private transient DownloadLinkView                  view                                = null;
    private transient LinkInfo                          linkInfo                            = null;
    private transient volatile long                     lastAvailableStatusChange           = -1;
    private transient volatile FilePackage              lastValidFilePackage                = null;
    private transient volatile String[]                 cachedName                          = null;
    private transient UrlProtection                     urlProtection                       = UrlProtection.UNSET;
    private transient List<HistoryEntry>                history                             = null;
    private transient Boolean                           partOfAnArchive                     = null;
    private transient List<String>                      sourcePluginPasswordList            = null;
    public static final String                          RELATIVE_DOWNLOAD_FOLDER_PATH       = "subfolderbyplugin";

    public Boolean isPartOfAnArchive() {
        return partOfAnArchive;
    }

    public void setPartOfAnArchive(final Boolean notAnArchive) {
        if (!Boolean.FALSE.equals(notAnArchive) || Files.getExtension(getName()) != null) {
            this.partOfAnArchive = notAnArchive;
        }
    }

    public long getJobID() {
        final long jobID = getLongProperty(PROPERTY_JOB_ID, -1l);
        return jobID;
    }

    public FilePackage getLastValidFilePackage() {
        final FilePackage lFilePackage = lastValidFilePackage;
        if (lFilePackage != null) {
            return lFilePackage;
        }
        /* stable import */
        return filePackage;
    }

    /**
     * these properties will not be saved/restored
     *
     * @return
     */
    public Property getTempProperties() {
        if (tempProperties != null) {
            return tempProperties;
        }
        synchronized (this) {
            Property ltempProperties = tempProperties;
            if (ltempProperties == null) {
                ltempProperties = new Property();
                tempProperties = ltempProperties;
            }
            return ltempProperties;
        }
    }

    public boolean hasTempProperties() {
        return tempProperties != null;
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
    public DownloadLink(PluginForHost plugin, String name, String host, String pluginPattern, boolean isEnabled) {
        setDefaultPlugin(plugin);
        setView(new DefaultDownloadLinkViewImpl());
        if (name != null) {
            setName(name);
        }
        this.isEnabled = isEnabled;
        downloadMax = -1;
        setHost(host);
        created = System.currentTimeMillis();
        this.setPluginPatternMatcher(pluginPattern);
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

    /**
     * @deprecated use {@link #getView()}
     * @return
     */
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
        final GetField fields = stream.readFields();
        setView(new DefaultDownloadLinkViewImpl());
        history = null;
        extractionStatus = null;
        linkInfo = null;
        availableStatus = AvailableStatus.UNCHECKED;
        try {
            final int linkType = fields.get("linkType", 0);
            if (linkType == 0) {
                urlProtection = UrlProtection.UNSET;
            } else {
                urlProtection = UrlProtection.PROTECTED_CONTAINER;
            }
        } catch (final Throwable e) {
            urlProtection = UrlProtection.UNSET;
            e.printStackTrace();
        }
        try {
            final String sourcePluginComment = (String) fields.get("sourcePluginComment", null);
            if (sourcePluginComment != null) {
                setComment(sourcePluginComment);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            final Object sourcePluginPasswordList = fields.get("sourcePluginPasswordList", null);
            if (sourcePluginPasswordList != null && sourcePluginPasswordList instanceof List && ((List) sourcePluginPasswordList).size() > 0) {
                final List<String> list = (List<String>) sourcePluginPasswordList;
                list.remove("autopostpw");
                list.remove("{}");
                final Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    final String next = it.next();
                    if (StringUtils.startsWithCaseInsensitive(next, "http://") || StringUtils.startsWithCaseInsensitive(next, "https://")) {
                        it.remove();
                    }
                }
                if (list.size() > 0) {
                    this.sourcePluginPasswordList = list;
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            this.created = fields.get("created", -1l);
        } catch (final Throwable e) {
            this.created = System.currentTimeMillis();
            e.printStackTrace();
        }
        try {
            this.downloadCurrent = fields.get("downloadCurrent", 0l);
        } catch (final Throwable e) {
            this.downloadCurrent = 0;
            e.printStackTrace();
        }
        try {
            chunksProgress = (long[]) fields.get("chunksProgress", null);
        } catch (final Throwable e) {
            this.chunksProgress = null;
            downloadCurrent = 0;
            e.printStackTrace();
        }
        try {
            this.downloadMax = fields.get("downloadMax", -1l);
        } catch (final Throwable e) {
            this.downloadMax = -1l;
            e.printStackTrace();
        }
        try {
            this.isEnabled = fields.get("isEnabled", true);
        } catch (final Throwable e) {
            this.isEnabled = true;
            e.printStackTrace();
        }
        try {
            this.urlDownload = (String) fields.get("urlDownload", null);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            final String name = (String) fields.get("name", null);
            if (name != null) {
                setName(name);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            this.host = (String) fields.get("host", null);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            final String finalFileName = (String) fields.get("finalFileName", null);
            if (finalFileName != null) {
                this.setFinalFileName(finalFileName);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            final LinkStatus linkStatus = (LinkStatus) fields.get("linkStatus", null);
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
    }

    public List<String> getOldPluginPasswordList() {
        return this.sourcePluginPasswordList;
    }

    public UniqueAlltimeID getUniqueID() {
        if (uniqueID == null) {
            synchronized (this) {
                if (uniqueID == null) {
                    uniqueID = new UniqueAlltimeID();
                }
            }
        }
        return uniqueID;
    }

    public Priority getPriorityEnum() {
        try {
            final String priority = getStringProperty(PROPERTY_PRIORITY, null);
            if (priority == null) {
                return Priority.DEFAULT;
            }
            return Priority.valueOf(priority);
        } catch (final Throwable e) {
            return Priority.DEFAULT;
        }
    }

    public int getChunks() {
        return getIntegerProperty(PROPERTY_CHUNKS, 0);
    }

    public void setChunks(int chunks) {
        if (chunks == getChunks()) {
            return;
        }
        if (chunks <= 0) {
            setProperty(PROPERTY_CHUNKS, Property.NULL);
        } else {
            setProperty(PROPERTY_CHUNKS, chunks);
        }
        SingleDownloadController dlc = getDownloadLinkController();
        // DownloadInterface dli = null;
        // if (dlc != null && (dli = dlc.getDownloadInstance()) != null) {
        // if (dli instanceof HTTPDownloader) {
        // ((HTTPDownloader) dli).setChunkNum(chunks);
        // }
        // }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.CHUNKS, chunks));
        }
    }

    public void setCustomSpeedLimit(int limit) {
        if (limit == getCustomSpeedLimit()) {
            return;
        }
        if (limit == 0) {
            setProperty(PROPERTY_SPEEDLIMIT, Property.NULL);
        } else {
            if (limit < 0) {
                limit = 1;
            }
            setProperty(PROPERTY_SPEEDLIMIT, limit);
        }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.SPEED_LIMIT, limit));
        }
    }

    public int getCustomSpeedLimit() {
        return this.getIntegerProperty(PROPERTY_SPEEDLIMIT, 0);
    }

    /**
     *
     *
     * @return use {@link #getView()} for external usage
     */
    @Deprecated
    public long[] getChunksProgress() {
        return chunksProgress;
    }

    /**
     * returns the approximate(live) amount of downloaded bytes
     *
     * @return Anzahl der heruntergeladenen Bytes
     * @deprecated use {@link #getView()} instead
     */
    public long getDownloadCurrent() {
        final SingleDownloadController dlc = getDownloadLinkController();
        DownloadInterface dli = null;
        if (dlc != null && (dli = dlc.getDownloadInstance()) != null) {
            if (dli.getTotalLinkBytesLoadedLive() == 0 && getDownloadCurrentRaw() != 0) {
                return getDownloadCurrentRaw();
            } else {
                return dli.getTotalLinkBytesLoadedLive();
            }
        }
        return getDownloadCurrentRaw();
    }

    /**
     * returns the exact amount of downloaded bytes (depends on DownloadInterface if this value is updated during download or at the end)
     *
     * @return
     */
    public long getDownloadCurrentRaw() {
        return downloadCurrent;
    }

    public SingleDownloadController getDownloadLinkController() {
        return downloadLinkController;
    }

    /**
     * Die Groesse der Datei
     *
     * @return Die Groesse der Datei
     * @deprecated use {@link #getView()} sintead
     */
    public long getDownloadSize() {
        final long verifiedFileSize = getVerifiedFileSize();
        if (verifiedFileSize >= 0) {
            return verifiedFileSize;
        }
        return Math.max(getDownloadCurrent(), downloadMax);
    }

    /**
     * Gibt die aktuelle Downloadgeschwindigkeit in bytes/sekunde zurueck
     *
     * @return Downloadgeschwindigkeit in bytes/sekunde
     * @deprecated use {@link #getView()}
     */
    public long getDownloadSpeed() {
        final SingleDownloadController dlc = getDownloadLinkController();
        DownloadInterface dli = null;
        if (dlc != null && (dli = dlc.getDownloadInstance()) != null) {
            return dli.getManagedConnetionHandler().getSpeed();
        }
        return 0;
    }

    public DownloadLinkView getView() {
        return view;
    }

    public DownloadLinkView setView(DownloadLinkView status) {
        if (status == null) {
            throw new NullPointerException();
        }
        status.setLink(this);
        final DownloadLinkView old;
        synchronized (this) {
            old = view;
            view = status;
        }
        return old;
    }

    /**
     * @deprecated Use #getPluginPatternMatcher() instead
     * @return
     */
    @Deprecated
    public String getDownloadURL() {
        return getPluginPatternMatcher();
    }

    /**
     * Definition:<br>
     * The url to the content. if we copy the content url and paste it back to jdownloader, we should get exactly the same download (if
     * possible). This url should be a valid url for every browser as well. Try to append additional information as get parameters to define
     * variants. http://svn.jdownloader.org/issues/51004
     */
    public String getContentUrl() {
        return getStringProperty(URL_CONTENT);
    }

    /**
     * Browser URL / URL to hoster link. replaces set/get UrlDownload()
     *
     * @since JD2
     */
    public void setContentUrl(final String url) {
        if (!StringUtils.equals(url, getContentUrl())) {
            if (StringUtils.isEmpty(url)) {
                setProperty(URL_CONTENT, Property.NULL);
            } else {
                setProperty(URL_CONTENT, url);
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.URL_CONTENT, url));
            }
        }
    }

    /**
     * allows for customisation based on plugin source and user plugin setting preferences
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public String getCustomUrl() {
        return getStringProperty(URL_CUSTOM);
    }

    /**
     * allows for customisation based on plugin source and user plugin setting preferences
     *
     * @author raztoki
     * @since JD2
     * @param url
     */
    public void setCustomURL(final String url) {
        if (!StringUtils.equals(url, getContainerUrl())) {
            if (StringUtils.isEmpty(url)) {
                setProperty(URL_CUSTOM, Property.NULL);
            } else {
                setProperty(URL_CUSTOM, url);
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.URL_CUSTOM, url));
            }
        }
    }

    /**
     * Definition:<br>
     * The nearest crawler url that does not equal the content url <br>
     * http://svn.jdownloader.org/issues/51004
     */
    public String getContainerUrl() {
        return getStringProperty(URL_CONTAINER);
    }

    /**
     * "nearest crawler url..."
     *
     * @since JD2
     * @param url
     */
    public void setContainerUrl(final String url) {
        if (!StringUtils.equals(url, getContainerUrl())) {
            if (StringUtils.isEmpty(url)) {
                setProperty(URL_CONTAINER, Property.NULL);
            } else {
                setProperty(URL_CONTAINER, url);
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.URL_CONTAINER, url));
            }
        }
    }

    /**
     * Definition:<br>
     * The origin Url. This is the url, that has been entered by the user.<br>
     * http://svn.jdownloader.org/issues/51004
     */
    public String getOriginUrl() {
        return getStringProperty(URL_ORIGIN);
    }

    public void setOriginUrl(final String url) {
        if (!StringUtils.equals(url, getOriginUrl())) {
            if (StringUtils.isEmpty(url)) {
                setProperty(URL_ORIGIN, Property.NULL);
            } else {
                setProperty(URL_ORIGIN, url);
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.URL_ORIGIN, url));
            }
        }
    }

    /**
     * Definition:<br>
     * The Referrer Url. In some cases, like CNL, or copy and paste from the browser, we can access the currently loaded page in the
     * browser. This can be considered as a kind of Referrer. IF available, we should store it here. http://svn.jdownloader.org/issues/51004
     */
    public String getReferrerUrl() {
        return getStringProperty(URL_REFERRER);
    }

    public void setReferrerUrl(final String url) {
        if (!StringUtils.equals(url, getReferrerUrl())) {
            if (StringUtils.isEmpty(url)) {
                setProperty(URL_REFERRER, Property.NULL);
            } else {
                setProperty(URL_REFERRER, url);
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.URL_REFERRER, url));
            }
        }
    }

    /**
     * {@link #setPluginPatternMatcher(String)} Definition:<br>
     * the url or pattern that matches the plugins' regular expresssions. it is generally use by the plugin to download. The PluginPattern
     * may be an invalid pseudo url, or event no valid url at all. http://svn.jdownloader.org/issues/51004
     */
    public String getPluginPatternMatcher() {
        return urlDownload;
    }

    public String getFileOutput() {
        return getFileOutput(false, false);
    }

    public String getFileOutputForPlugin(boolean ignoreUnsafe, boolean ignoreCustom) {
        SingleDownloadController con = getDownloadLinkController();
        if (con == null) {
            return getFileOutput(ignoreUnsafe, ignoreCustom);
        } else {
            return con.getFileOutput(ignoreUnsafe, ignoreCustom).getAbsolutePath();
        }
    }

    public String getFileOutput(boolean ignoreUnsafe, boolean ignoreCustom) {
        String downloadDirectory = getDownloadDirectory();
        String fileName = getInternalTmpFilename();
        if (!StringUtils.isEmpty(fileName) && !ignoreCustom) {
            /* we have a customized fileOutputFilename */
            return new File(downloadDirectory, fileName).getAbsolutePath();
        }
        fileName = getName(ignoreUnsafe, false);
        if (StringUtils.isEmpty(fileName)) {
            return null;
        }
        String customAppend = getInternalTmpFilenameAppend();
        if (!StringUtils.isEmpty(customAppend) && !ignoreCustom) {
            fileName = fileName + customAppend;
        }
        return new File(downloadDirectory, fileName).getAbsolutePath();
    }

    public String getDownloadDirectory() {
        // workaround to redirect plugin calls via downloadcontroller.
        if (Thread.currentThread() instanceof SingleDownloadController) {
            return ((SingleDownloadController) Thread.currentThread()).getSessionDownloadDirectory();
        }
        final FilePackage fp = getLastValidFilePackage();
        if (fp != null) {
            final String downloadDirectory = fp.getDownloadDirectory();
            if (StringUtils.isEmpty(downloadDirectory)) {
                throw new WTFException("what the fuck just happened here? defaultFilePackage: " + FilePackage.isDefaultFilePackage(fp));
            }
            return downloadDirectory;
        } else {
            throw new WTFException("what the fuck just happened here? no lastValidFilePackage");
        }
    }

    /**
     * Use this if we need a tmp filename for downloading. this tmp is internal! The gui will not display it.
     *
     * @since JD2
     */
    public String getInternalTmpFilename() {
        final String ret = this.getStringProperty(PROPERTY_CUSTOM_LOCALFILENAME, null);
        if (!StringUtils.isEmpty(ret)) {
            /* we have a customized localfilename, eg xy.tmp */
            return ret;
        }
        return null;
    }

    /**
     * Use this if we need a tmp filename for downloading. this tmp is internal! The gui will not display it.
     *
     * @since JD2
     */
    public String getInternalTmpFilenameAppend() {
        final String ret = this.getStringProperty(PROPERTY_CUSTOM_LOCALFILENAMEAPPEND, null);
        if (!StringUtils.isEmpty(ret)) {
            /* we have a customized localfilename, eg xy.tmp */
            return ret;
        }
        return null;
    }

    /**
     * Use this if we need a tmp filename for downloading. this tmp is internal! The gui will not display it.
     *
     * @since JD2
     */
    public void setInternalTmpFilename(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            setProperty(PROPERTY_CUSTOM_LOCALFILENAME, Property.NULL);
        } else {
            fileName = CrossSystem.alleviatePathParts(fileName);
            this.setProperty(PROPERTY_CUSTOM_LOCALFILENAME, fileName);
        }
        cachedName = null;
    }

    /**
     * Use this if we need a tmp filename for downloading. this tmp is internal! The gui will not display it.
     *
     * @since JD2
     */
    public void setInternalTmpFilenameAppend(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            setProperty(PROPERTY_CUSTOM_LOCALFILENAMEAPPEND, Property.NULL);
        } else {
            fileName = CrossSystem.alleviatePathParts(fileName, false);
            this.setProperty(PROPERTY_CUSTOM_LOCALFILENAMEAPPEND, fileName);
        }
        cachedName = null;
    }

    /**
     * return the FilePackage that contains this DownloadLink, if none is set it will return defaultFilePackage
     *
     * @return
     */
    public FilePackage getFilePackage() {
        final FilePackage lFilePackage = filePackage;
        if (lFilePackage == null) {
            return FilePackage.getDefaultFilePackage();
        }
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
        if (newHost == null) {
            return;
        }
        host = deDuplicateString(newHost.toLowerCase(Locale.ENGLISH));
    }

    public static String deDuplicateString(String string) {
        if (string != null && Application.getJavaVersion() >= Application.JAVA17) {
            return string.intern();
        }
        return string;
    }

    public LinkStatus getLinkStatus() {
        final Thread current = Thread.currentThread();
        if (current instanceof UseSetLinkStatusThread) {
            return ((UseSetLinkStatusThread) current).getLinkStatus(this);
        }
        final SingleDownloadController controller = getDownloadLinkController();
        if (controller != null) {
            return controller.getLinkStatus();
        }
        throw new WTFException("Cannot use getLinkStatus outside UseSetLinkStatusThread/SingleDownloadController");
    }

    public UrlProtection getUrlProtection() {
        return urlProtection;
    }

    public String getName() {
        return getName(false, false);
    }

    /**
     *
     *
     * priority of returned fileName<br />
     * 0.) tmpAsynchRenameFilename (e.g. renamed in downloadlist)<br />
     * 1.) forcedFileName (eg manually set)<br />
     * 2.) finalFileName (eg set by plugin where the final is 100% safe, eg API)<br />
     * 3.) unsafeFileName (eg set by plugin when no api is available, or no filename provided)<br />
     *
     * @param ignoreUnsafe
     * @param ignoreForcedFilename
     *            TODO
     * @return
     */
    public String getName(boolean ignoreUnsafe, boolean ignoreForcedFilename) {
        String ret = getCachedName(ignoreUnsafe, ignoreForcedFilename);
        if (StringUtils.isNotEmpty(ret)) {
            return ret;
        }
        ret = ignoreForcedFilename ? null : this.getForcedFileName();
        if (StringUtils.isNotEmpty(ret)) {
            // ret = replaceCustomExtension(ret);
            setCachedName(ignoreUnsafe, ignoreForcedFilename, ret);
            return ret;
        }
        ret = this.getFinalFileName();
        if (StringUtils.isNotEmpty(ret)) {
            ret = replaceCustomExtension(ret);
            setCachedName(ignoreUnsafe, ignoreForcedFilename, ret);
            return ret;
        }
        if (ignoreUnsafe) {
            setCachedName(ignoreUnsafe, ignoreForcedFilename, null);
            return null;
        }
        try {
            final String name = getRawName();
            if (StringUtils.isNotEmpty(name)) {
                ret = replaceCustomExtension(name);
                setCachedName(ignoreUnsafe, ignoreForcedFilename, ret);
                return ret;
            }
            final String url = this.getContentUrlOrPatternMatcher();
            if (StringUtils.isNotEmpty(url)) {
                final String urlName = Plugin.extractFileNameFromURL(url);
                if (StringUtils.isNotEmpty(urlName)) {
                    ret = replaceCustomExtension(urlName);
                    setCachedName(ignoreUnsafe, ignoreForcedFilename, ret);
                    return ret;
                }
            }
            return UNKNOWN_FILE_NAME;
        } catch (Exception e) {
            return UNKNOWN_FILE_NAME;
        }
    }

    public String getRawName() {
        final String lName = name;
        if (!UNKNOWN_FILE_NAME.equals(lName)) {
            return lName;
        }
        return null;
    }

    public boolean isNameSet() {
        return getRawName() != null || getForcedFileName() != null || getFinalFileName() != null;
    }

    public String replaceCustomExtension(String name) {
        if (name == null) {
            return null;
        }
        String cust = getCustomExtension();
        if (cust != null) {
            final int index = name.lastIndexOf(".");
            if (index < 0) {
                return name + "." + cust;
            }
            return name.substring(0, index + 1) + cust;
        }
        return name;
    }

    private void setCachedName(boolean ignoreUnsafe, boolean ignoreForcedFilename, String ret) {
        String[] lCachedName = cachedName;
        if (lCachedName == null) {
            lCachedName = new String[] { null, null, null, null };
            cachedName = lCachedName;
        }
        lCachedName[(ignoreUnsafe ? 1 : 0) * 2 + (ignoreForcedFilename ? 1 : 0)] = ret;
    }

    private String getCachedName(boolean ignoreUnsafe, boolean ignoreForcedFilename) {
        final String[] lCachedName = cachedName;
        if (lCachedName != null) {
            return lCachedName[(ignoreUnsafe ? 1 : 0) * 2 + (ignoreForcedFilename ? 1 : 0)];
        }
        return null;
    }

    public LinkInfo getSetLinkInfo() {
        return linkInfo;
    }

    public LinkInfo getLinkInfo() {
        final LinkInfo lLinkInfo = linkInfo;
        if (lLinkInfo == null) {
            final LinkInfo newLinkInfo = LinkInfo.getLinkInfo(this);
            linkInfo = newLinkInfo;
            return newLinkInfo;
        }
        return lLinkInfo;
    }

    private void setLinkInfo(LinkInfo linkInfo) {
        this.linkInfo = linkInfo;
    }

    /**
     * returns fileName set by plugin (setFinalFileName)
     *
     * @return
     */
    public String getNameSetbyPlugin() {
        final String ret = this.getFinalFileName();
        if (ret != null) {
            return ret;
        }
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
        final Object ret = this.getProperty(PROPERTY_PWLIST);
        if (ret != null && ret instanceof List) {
            return (List<String>) ret;
        }
        return null;
    }

    /**
     * Gibt den Finalen Downloadnamen zurueck. Wird null zurueckgegeben, so wird der dateiname von den jeweiligen plugins automatisch
     * ermittelt.
     *
     * @return Statischer Dateiname
     */
    public String getFinalFileName() {
        return this.getStringProperty(PROPERTY_FINALFILENAME, null);
    }

    public String getForcedFileName() {
        // workaround. all plugin calls should return the forced filename from the singledownloadcontroller - if available
        if (Thread.currentThread() instanceof SingleDownloadController) {
            return ((SingleDownloadController) Thread.currentThread()).getSessionDownloadFilename();
        }
        return this.getStringProperty(PROPERTY_FORCEDFILENAME, null);
    }

    public String getSetLinkID() {
        return this.getStringProperty(PROPERTY_LINKDUPEID, null);
    }

    public String getLinkID() {
        final PluginForHost plugin = getDefaultPlugin();
        if (plugin != null) {
            return plugin.getLinkID(this);
        } else {
            final String linkID = getSetLinkID();
            if (StringUtils.isEmpty(linkID)) {
                return getPluginPatternMatcher();
            } else {
                return linkID;
            }
        }
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
     * Gibt zurueck ob Dieser Link schon auf verfuegbarkeit getestet wurde.+ Diese FUnktion fuehrt keinen!! Check durch. Sie prueft nur ob
     * schon geprueft worden ist. anschiessend kann mit isAvailable() die verfuegbarkeit ueberprueft werden
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
        if (availableStatus == null) {
            availableStatus = AvailableStatus.UNCHECKED;
        }
        if (AvailableStatus.UNCHECKED.equals(availableStatus)) {
            lastAvailableStatusChange = -1;
        } else {
            lastAvailableStatusChange = System.currentTimeMillis();
        }
        if (this.availableStatus == availableStatus) {
            return;
        }
        this.availableStatus = availableStatus;
        switch (availableStatus) {
        case FALSE:
            if (getFinalLinkState() == null) {
                setFinalLinkState(FinalLinkState.OFFLINE);
            }
            break;
        case TRUE:
            if (FinalLinkState.OFFLINE.equals(getFinalLinkState())) {
                setFinalLinkState(null);
            }
            break;
        }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.AVAILABILITY, availableStatus));
        }
    }

    protected void setAvailableStatusUnsafe(final AvailableStatus availableStatus) {
        if (availableStatus == null) {
            this.availableStatus = AvailableStatus.UNCHECKED;
        } else {
            this.availableStatus = availableStatus;
        }
    }

    public long getLastAvailableStatusChange() {
        return lastAvailableStatusChange;
    }

    public void notifyChanges(AbstractNodeNotifier.NOTIFY notify, Object param) {
        final AbstractNodeNotifier pl = propertyListener;
        if (pl != null) {
            pl.nodeUpdated(this, notify, param);
            return;
        }
        final AbstractNodeNotifier pl2 = filePackage;
        if (pl2 != null) {
            pl2.nodeUpdated(this, notify, param);
        }
    }

    public boolean hasNotificationListener() {
        AbstractNodeNotifier pl = propertyListener;
        if (pl != null && pl.hasNotificationListener()) {
            return true;
        }
        pl = filePackage;
        if (pl != null && pl.hasNotificationListener()) {
            return true;
        }
        return false;
    }

    public void reset(List<PluginForHost> resetPlugins) {
        clearHistory();
        setInternalTmpFilenameAppend(null);
        setInternalTmpFilename(null);
        setFinalFileName(null);
        setFinalLinkState(null);
        long size = getView().getBytesTotal();
        setVerifiedFileSize(-1);
        if (size >= 0) {
            setDownloadSize(size);
        }
        setChunksProgress(null);
        setChunks(0);
        setCustomSpeedLimit(0);
        setDownloadCurrent(0);
        setFinishedDate(-1l);
        addDownloadTime(-1);
        setAvailableStatus(AvailableStatus.UNCHECKED);
        setSkipReason(null);
        setConditionalSkipReason(null);
        setEnabled(true);
        setLinkInfo(null);
        setExtractionStatus(null);
        // bindData(HTTPDownloadHints.class).reset();
        if (resetPlugins != null) {
            for (PluginForHost resetPlugin : resetPlugins) {
                try {
                    resetPlugin.resetLink(this);
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
        }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.RESET, null));
        }
    }

    public void clearHistory() {
        synchronized (NULL) {
            if (history != null) {
                history.clear();
            }
        }
    }

    public void resume(List<PluginForHost> resetPlugins) {
        setAvailableStatus(AvailableStatus.UNCHECKED);
        setSkipReason(null);
        setConditionalSkipReason(null);
        setEnabled(true);
        if (resetPlugins != null) {
            for (PluginForHost resetPlugin : resetPlugins) {
                try {
                    resetPlugin.resumeDownloadlink(this);
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
        }
    }

    public void setAvailable(boolean available) {
        setAvailableStatus(available ? AvailableStatus.TRUE : AvailableStatus.FALSE);
    }

    /**
     * do not use this method, only kept for compatibility reasons and some plugins need it
     *
     * @param is
     */
    @Deprecated
    public void setChunksProgress(long[] is) {
        chunksProgress = is;
    }

    /**
     * Setzt die Anzahl der heruntergeladenen Bytes fest und aktualisiert die Fortschrittsanzeige
     *
     * @param downloadedCurrent
     *            Anzahl der heruntergeladenen Bytes
     *
     */
    public void setDownloadCurrent(long downloadedCurrent) {
        if (getDownloadCurrentRaw() == downloadedCurrent) {
            return;
        }
        downloadCurrent = downloadedCurrent;
        if (hasNotificationListener() && this.getCurrentDownloadInterface() == null) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, null);
        }
    }

    private DownloadInterface getCurrentDownloadInterface() {
        SingleDownloadController dlc = getDownloadLinkController();
        DownloadInterface dli = null;
        if (dlc != null && (dli = dlc.getDownloadInstance()) != null) {
            return dli;
        }
        return null;
    }

    /**
     * do not call this method. Only The Downloadwatchdog queue is allowed to call this method
     *
     * @param downloadLinkController
     */
    public void setDownloadLinkController(SingleDownloadController downloadLinkController) {
        final SingleDownloadController old;
        // sync is not required. this method is only called from the downloadwatchdog queue
        old = this.downloadLinkController;
        this.downloadLinkController = downloadLinkController;
        if (old != null && old != downloadLinkController) {
            old.onDetach(this);
        }
        if (old != downloadLinkController && downloadLinkController != null) {
            downloadLinkController.onAttach(this);
        }
        if (old != downloadLinkController) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.DOWNLOAD_CONTROLLER, downloadLinkController));
        }
    }

    /**
     * Setzt die Groesse der herunterzuladenden Datei
     *
     * @param downloadMax
     *            Die Groesse der Datei
     */
    public void setDownloadSize(long downloadMax) {
        if (this.downloadMax == downloadMax) {
            return;
        }
        this.downloadMax = Math.max(-1, downloadMax);
        if (hasNotificationListener() && this.getCurrentDownloadInterface() == null) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.DOWNLOADSIZE, downloadMax));
        }
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     *
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * changes the enabled status of this DownloadLink, aborts download if its currently running
     */
    public void setEnabled(final boolean isEnabled) {
        synchronized (this) {
            if (this.isEnabled == isEnabled) {
                return;
            } else {
                this.isEnabled = isEnabled;
            }
        }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.ENABLED, isEnabled));
        }
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     *
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isSkipped() {
        return skipReason != null;
    }

    /**
     * changes the enabled status of this DownloadLink, aborts download if its currently running
     */
    public SkipReason setSkipReason(SkipReason skipReason) {
        final SkipReason old;
        synchronized (this) {
            old = this.skipReason;
            this.skipReason = skipReason;
        }
        if (old != skipReason && hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.SKIPPED, skipReason));
        }
        return old;
    }

    public FinalLinkState setFinalLinkState(FinalLinkState finalLinkState) {
        final FinalLinkState old;
        synchronized (this) {
            old = this.finalLinkState;
            this.finalLinkState = finalLinkState;
        }
        if (old != finalLinkState) {
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
            if (finalLinkState != null && JsonConfig.create(GeneralSettings.class).isHashRetryEnabled() && finalLinkState.isFailedHash()) {
                final List<DownloadLink> link = Arrays.asList(this);
                DownloadWatchDog.getInstance().reset(link);
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.FINAL_STATE, finalLinkState));
            }
        }
        return old;
    }

    protected void setFinalLinkStateUnsafe(final FinalLinkState finalLinkState) {
        this.finalLinkState = finalLinkState;
    }

    public FinalLinkState getFinalLinkState() {
        return finalLinkState;
    }

    public ConditionalSkipReason setConditionalSkipReason(ConditionalSkipReason conditionalSkipReason) {
        final ConditionalSkipReason old;
        synchronized (this) {
            old = this.conditionalSkipReason;
            this.conditionalSkipReason = conditionalSkipReason;
        }
        if (old != conditionalSkipReason && hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.CONDITIONAL_SKIPPED, conditionalSkipReason));
        }
        return old;
    }

    public ConditionalSkipReason getConditionalSkipReason() {
        return conditionalSkipReason;
    }

    public SkipReason getSkipReason() {
        return skipReason;
    }

    public void setUrlProtection(UrlProtection type) {
        if (type == null) {
            type = UrlProtection.UNSET;
        }
        if (type == getUrlProtection()) {
            return;
        }
        this.urlProtection = type;
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.URL_PROTECTION, type));
        }
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
        final PluginForHost oldLivePlugin = liveplugin;
        this.liveplugin = plugin;
        if (plugin != null) {
            plugin.setDownloadLink(this);
        }
        if (oldLivePlugin != null && oldLivePlugin != plugin) {
            oldLivePlugin.setDownloadLink(null);
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
        if (StringUtils.isEmpty(name)) {
            name = Plugin.extractFileNameFromURL(getContentUrlOrPatternMatcher());
        }
        if (!StringUtils.isEmpty(name)) {
            name = CrossSystem.alleviatePathParts(name);
        }
        if (StringUtils.isEmpty(name)) {
            name = UNKNOWN_FILE_NAME;
        }
        this.name = name;
        cachedName = null;
        final String newName = getName();
        if (!StringUtils.equals(oldName, newName)) {
            setLinkInfo(null);
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, newName));
            }
        }
    }

    protected void setNameUnsafe(String name) {
        this.name = name;
    }

    public void forceForcedFileName(String newForced) {
        String oldForced = getStringProperty(PROPERTY_FORCEDFILENAME);
        if (!StringUtils.equals(StringUtils.nullOrNonEmpty(newForced), StringUtils.nullOrNonEmpty(oldForced))) {
            final String oldName;
            if (StringUtils.isEmpty(newForced)) {
                oldName = getName();
                this.setProperty(PROPERTY_FORCEDFILENAME, Property.NULL);
            } else {
                oldName = getName(false, true);
                newForced = CrossSystem.alleviatePathParts(newForced);
                this.setProperty(PROPERTY_FORCEDFILENAME, newForced);
            }
            cachedName = null;
            final String newName = getName();
            if (!StringUtils.equals(oldName, newName)) {
                setLinkInfo(null);
                if (hasNotificationListener()) {
                    notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, newName));
                }
            }
        }
    }

    /**
     *
     * use this function to force a name, it has highest priority
     */
    public void setForcedFileName(String name) {
        String curForced = getStringProperty(PROPERTY_FORCEDFILENAME);
        if (StringUtils.equals(name, curForced)) {
            return;
        }
        String oldName = getName(false, true);
        if (StringUtils.isEmpty(name)) {
            this.setProperty(PROPERTY_FORCEDFILENAME, Property.NULL);
            oldName = getName();
        } else {
            if (StringUtils.equals(oldName, name)) {
                if (curForced == null) {
                    // name equals normal name
                    return;
                } else {
                    this.setProperty(PROPERTY_FORCEDFILENAME, Property.NULL);
                    oldName = getName();
                }
            } else {
                name = CrossSystem.alleviatePathParts(name);
                // if (org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.FILENAME_TO_LOWER_CASE.isEnabled()) {
                // name = name.toLowerCase(Locale.ENGLISH);
                // }
                this.setProperty(PROPERTY_FORCEDFILENAME, name);
            }
        }
        cachedName = null;
        final String newName = getName();
        if (!StringUtils.equals(oldName, newName)) {
            setLinkInfo(null);
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, newName));
            }
        }
    }

    /**
     * WARNING: DO NOT use in 09581 stable!
     *
     * @since JD2
     */
    public void setComment(String comment) {
        final boolean changed;
        if (comment == null || comment.length() == 0) {
            changed = this.setProperty(PROPERTY_COMMENT, Property.NULL);
        } else {
            changed = this.setProperty(PROPERTY_COMMENT, comment);
        }
        if (changed && hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.COMMENT, comment));
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
     * Filename Setter for Plugins if the plugin is 100% sure that this is the correct filename
     *
     * @param newfinalFileName
     */
    public void setFinalFileName(String newfinalFileName) {
        final String oldName = getName();
        if (!StringUtils.isEmpty(newfinalFileName)) {
            if (new Regex(newfinalFileName, Pattern.compile("r(ar|\\d{2,3})\\.html?$", Pattern.CASE_INSENSITIVE)).matches()) {
                System.out.println("Use Workaround for stupid >>rar.html<< uploaders!");
                newfinalFileName = newfinalFileName.substring(0, newfinalFileName.length() - new Regex(newfinalFileName, Pattern.compile("r(?:ar|\\d{2,3})(\\.html?)$", Pattern.CASE_INSENSITIVE)).getMatch(0).length());
            }
            newfinalFileName = CrossSystem.alleviatePathParts(newfinalFileName);
            this.setProperty(PROPERTY_FINALFILENAME, newfinalFileName);
            cachedName = null;// setName calls getName
            setName(newfinalFileName);
        } else {
            this.setProperty(PROPERTY_FINALFILENAME, Property.NULL);
        }
        cachedName = null;
        final String newName = getName();
        if (!StringUtils.equals(oldName, newName)) {
            setLinkInfo(null);
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, newName));
            }
        }
    }

    /**
     * @deprecated use {@link #setPluginPatternMatcher(String)}
     * @param urlDownload
     */
    @Deprecated
    public void setUrlDownload(String urlDownload) {
        setPluginPatternMatcher(urlDownload);
    }

    public void setPluginPatternMatcher(final String pluginPattern) {
        final String previousURLDownload = getPluginPatternMatcher();
        final String previousLinkID = getLinkID();
        if (pluginPattern != null) {
            if (previousURLDownload != null && previousURLDownload.equals(pluginPattern)) {
                return;
            }
            this.urlDownload = pluginPattern;
        } else {
            this.urlDownload = null;
        }
        cachedName = null;
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.URL_CONTENT, pluginPattern));
        }
        if (previousLinkID == null && previousURLDownload != null && !previousURLDownload.equals(pluginPattern)) {
            /* downloadURL changed, so set original one as linkID, so all dupemaps still work */
            setLinkID(previousURLDownload);
        }
    }

    protected void setPluginPatternMatcherUnsafe(final String pluginPattern) {
        this.urlDownload = pluginPattern;
    }

    /**
     * Diese Methhode fragt das eigene Plugin welche Informationen ueber die File bereit gestellt werden. Der String eignet Sich zur
     * Darstellung in der UI
     */
    @Override
    public String toString() {
        final UniqueAlltimeID lPreviousParentNodeID = getPreviousParentNodeID();
        String host = getHost();
        if (host == null) {
            host = "";
        }
        if (lPreviousParentNodeID == null) {
            return getName().concat("@").concat(host);
        }
        if (lPreviousParentNodeID.equals(getParentNode().getUniqueID())) {
            return getName().concat("@").concat(host);
        }
        return getName().concat("@").concat(host).concat(" previousParentNode:").concat(lPreviousParentNodeID.toString());
    }

    /**
     * returns real downloadMAx Value. use #getDownloadSize if you are not sure
     *
     * @return use {@link #getView()} for external handling
     */
    public long getKnownDownloadSize() {
        long ret = getVerifiedFileSize();
        if (ret >= 0) {
            return ret;
        }
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
        final String oldPassword = getDownloadPassword();
        if (StringUtils.equals(pass, oldPassword) || (StringUtils.isEmpty(pass) && StringUtils.isEmpty(oldPassword))) {
            return;
        }
        if (StringUtils.isEmpty(pass)) {
            this.removeProperty(PROPERTY_PASS);
        } else {
            this.setProperty(PROPERTY_PASS, pass);
        }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.DOWNLOAD_PASSWORD, pass));
        }
    }

    public void setMD5Hash(final String md5) {
        setHashInfo(HashInfo.newInstanceSafe(md5, HashInfo.TYPE.MD5));
    }

    public HashInfo getHashInfo() {
        final String hashInfo = getStringProperty(PROPERTY_HASHINFO, (String) null);
        if (hashInfo != null) {
            return HashInfo.importFromString(hashInfo);
        } else {
            final String sha1 = getStringProperty(PROPERTY_SHA1, null);
            if (sha1 != null) {
                return HashInfo.newInstanceSafe(sha1, TYPE.SHA1);
            }
            final String sha256 = getStringProperty(PROPERTY_SHA256, null);
            if (sha256 != null) {
                return HashInfo.newInstanceSafe(sha256, TYPE.SHA256);
            }
            final String md5 = getStringProperty(PROPERTY_MD5, null);
            if (md5 != null) {
                return HashInfo.newInstanceSafe(md5, TYPE.MD5);
            }
        }
        return null;
    }

    public void setHashInfo(HashInfo hashInfo) {
        final boolean isForced = hashInfo != null && hashInfo.isForced();
        if (!isForced) {
            final HashInfo existingHash = getHashInfo();
            if (existingHash != null && existingHash.isForced()) {
                return;
            }
        }
        final boolean changed;
        if (hashInfo == null || hashInfo.isNone()) {
            changed = this.removeProperty(PROPERTY_HASHINFO);
        } else {
            changed = this.setProperty(PROPERTY_HASHINFO, hashInfo.exportAsString());
        }
        if (changed) {
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.HASHINFO, hashInfo));
                if (hashInfo != null) {
                    switch (hashInfo.getType()) {
                    case CRC32:
                        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.CRC32, hashInfo.getHash()));
                        break;
                    case MD5:
                        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.MD5, hashInfo.getHash()));
                        break;
                    case SHA1:
                        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.SHA1, hashInfo.getHash()));
                        break;
                    case SHA256:
                        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.SHA256, hashInfo.getHash()));
                        break;
                    case SHA512:
                        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.SHA512, hashInfo.getHash()));
                        break;
                    default:
                        break;
                    }
                }
            }
        }
    }

    public void firePropertyChanged(DownloadLinkProperty.Property property, Object param) {
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, property, param));
        }
    }

    @Deprecated
    public String getMD5Hash() {
        final HashInfo hashInfo = getHashInfo();
        if (hashInfo != null && HashInfo.TYPE.MD5.equals(hashInfo.getType())) {
            return hashInfo.getHash();
        }
        return null;
    }

    public void addPluginProgress(final PluginProgress progress) {
        if (progress != null) {
            synchronized (this) {
                List<PluginProgress> lPluginProgress = pluginProgress;
                if (lPluginProgress == null) {
                    /* to avoid concurrentmodificationexception */
                    lPluginProgress = new CopyOnWriteArrayList<PluginProgress>();
                }
                if (!lPluginProgress.contains(progress)) {
                    lPluginProgress.add(0, progress);
                } else if (lPluginProgress.get(0) != progress) {
                    lPluginProgress.add(0, progress);
                    final int index = lPluginProgress.lastIndexOf(progress);
                    lPluginProgress.remove(index);
                } else {
                    return;
                }
                /* pluginProgress must always contain at least 1 item, see getPluginProgress */
                pluginProgress = lPluginProgress;
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.PLUGIN_PROGRESS, progress));
            }
        }
    }

    public boolean removePluginProgress(final PluginProgress remove) {
        if (remove != null) {
            final PluginProgress latest;
            synchronized (this) {
                List<PluginProgress> lPluginProgress = pluginProgress;
                if (lPluginProgress == null || lPluginProgress.contains(remove) == false) {
                    return false;
                }
                if (lPluginProgress.size() > 1) {
                    lPluginProgress.remove(remove);
                    latest = lPluginProgress.get(0);
                } else {
                    latest = null;
                    pluginProgress = null;
                }
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.PLUGIN_PROGRESS, latest));
            }
            return true;
        }
        return false;
    }

    public PluginProgress getPluginProgress() {
        final List<PluginProgress> lPluginProgress = pluginProgress;
        if (lPluginProgress != null) {
            try {
                return lPluginProgress.get(0);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean hasPluginProgress(PluginProgress contains) {
        final List<PluginProgress> lPluginProgress = pluginProgress;
        return lPluginProgress != null && lPluginProgress.contains(contains);
    }

    public void setSha1Hash(final String sha1) {
        setHashInfo(HashInfo.newInstanceSafe(sha1, HashInfo.TYPE.SHA1));
    }

    /**
     * @author raztoki
     * @param sha256
     * @since JD2
     */
    public void setSha256Hash(final String sha256) {
        setHashInfo(HashInfo.newInstanceSafe(sha256, HashInfo.TYPE.SHA256));
    }

    public void setMirrorID(long mirrorID) {
        if (mirrorID <= 0) {
            setProperty(PROPERTY_MIRRORID, Property.NULL);
        } else {
            setProperty(PROPERTY_MIRRORID, mirrorID);
        }
    }

    public long getMirrorID() {
        return getLongProperty(PROPERTY_MIRRORID, -1l);
    }

    @Deprecated
    public String getSha1Hash() {
        final HashInfo hashInfo = getHashInfo();
        if (hashInfo != null && HashInfo.TYPE.SHA1.equals(hashInfo.getType())) {
            return hashInfo.getHash();
        }
        return null;
    }

    public void setMimeHint(final ExtensionsFilterInterface extensionFilter) {
        if (extensionFilter == null) {
            removeProperty(PROPERTY_MIME_HINT);
        } else {
            setProperty(PROPERTY_MIME_HINT, extensionFilter.name());
        }
    }

    public String getMimeHint() {
        return getStringProperty(PROPERTY_MIME_HINT, null);
    }

    @Deprecated
    public String getSha256Hash() {
        final HashInfo hashInfo = getHashInfo();
        if (hashInfo != null && HashInfo.TYPE.SHA256.equals(hashInfo.getType())) {
            return hashInfo.getHash();
        }
        return null;
    }

    /**
     * Do not use in Plugins for old Stable, or use try/catch or set property manually
     *
     * @param size
     */
    public void setVerifiedFileSize(long size) {
        if (size == getVerifiedFileSize()) {
            return;
        }
        setDownloadSize(size);
        final boolean changed;
        if (size < 0) {
            changed = setProperty(DownloadLink.PROPERTY_VERIFIEDFILESIZE, Property.NULL);
        } else {
            changed = setProperty(DownloadLink.PROPERTY_VERIFIEDFILESIZE, size);
        }
        if (changed && hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.DOWNLOADSIZE_VERIFIED, size));
        }
    }

    /**
     * use {@link #getView()} for external handling
     *
     * @return
     */
    public long getVerifiedFileSize() {
        return getLongProperty(PROPERTY_VERIFIEDFILESIZE, -1);
    }

    /**
     * Do not use in Plugins for old Stable, or use try/catch or set property manually
     *
     * @param size
     */
    public void setResumeable(boolean b) {
        if (b == isResumeable()) {
            return;
        }
        setProperty(PROPERTY_RESUMEABLE, b);
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.RESUMABLE, b));
        }
    }

    public boolean isResumeable() {
        final SingleDownloadController controller = getDownloadLinkController();
        if (controller != null) {
            final DownloadLinkCandidate candidate = controller.getDownloadLinkCandidate();
            return candidate.getCachedAccount().getPlugin().isResumeable(this, candidate.getCachedAccount().getAccount());
        } else {
            final PluginForHost plugin = getDefaultPlugin();
            if (plugin != null) {
                return plugin.isResumeable(this, null);
            }
        }
        return false;
    }

    public DomainInfo getDomainInfo() {
        DomainInfo domainInfo = this.domainInfo;
        if (domainInfo == null) {
            domainInfo = DomainInfo.getInstance(getServiceHost());
            this.domainInfo = domainInfo;
        }
        return domainInfo;
    }

    public void setDomainInfo(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
    }

    public String getServiceHost() {
        if (defaultplugin != null) {
            final String ret = defaultplugin.getHost(this, null);
            if (ret != null) {
                return ret;
            }
        }
        return getHost();
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
        if (FilePackage.isDefaultFilePackage(filePackage)) {
            filePackage = null;
        }
        if (this.filePackage != null && filePackage != null) {
            this.filePackage.remove(this);
        }
        if (this.filePackage != null) {
            this.previousParent = this.filePackage.getUniqueID();
        }
        if (filePackage != null) {
            lastValidFilePackage = filePackage;
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

    public AbstractNodeNotifier getNodeChangeListener() {
        return propertyListener;
    }

    public void setPriorityEnum(Priority priority) {
        if (priority == null) {
            priority = Priority.DEFAULT;
        }
        if (getPriorityEnum() != priority) {
            if (Priority.DEFAULT.equals(priority)) {
                setProperty(PROPERTY_PRIORITY, Property.NULL);
            } else {
                setProperty(PROPERTY_PRIORITY, priority.name());
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.PRIORITY, priority));
            }
        }
    }

    public String getArchiveID() {
        return getStringProperty(DownloadLink.PROPERTY_ARCHIVE_ID);
    }

    public void setArchiveID(String id) {
        if (!StringUtils.equals(id, getArchiveID())) {
            if (!StringUtils.isEmpty(id)) {
                setProperty(DownloadLink.PROPERTY_ARCHIVE_ID, id);
                setPartOfAnArchive(Boolean.TRUE);
            } else {
                setProperty(DownloadLink.PROPERTY_ARCHIVE_ID, Property.NULL);
                setPartOfAnArchive(null);
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.ARCHIVE_ID, id));
            }
        } else {
            if (StringUtils.isNotEmpty(id)) {
                setPartOfAnArchive(Boolean.TRUE);
            } else {
                setPartOfAnArchive(null);
            }
        }
    }

    public ExtractionStatus getExtractionStatus() {
        ExtractionStatus lExtractionStatus = extractionStatus;
        if (lExtractionStatus != null) {
            if (ExtractionStatus.NA.equals(lExtractionStatus)) {
                return null;
            }
            return lExtractionStatus;
        }
        final String string = getStringProperty(PROPERTY_EXTRACTION_STATUS, null);
        try {
            if (string != null) {
                lExtractionStatus = ExtractionStatus.valueOf(string);
                if (lExtractionStatus == null || ExtractionStatus.RUNNING.equals(lExtractionStatus)) {
                    extractionStatus = ExtractionStatus.NA;
                    return null;
                }
                extractionStatus = lExtractionStatus;
                if (ExtractionStatus.NA.equals(lExtractionStatus)) {
                    return null;
                }
                return lExtractionStatus;
            } else {
                extractionStatus = ExtractionStatus.NA;
                return null;
            }
        } catch (Exception e) {
            extractionStatus = ExtractionStatus.NA;
            return null;
        }
    }

    public void setExtractionStatus(ExtractionStatus newExtractionStatus) {
        if (extractionStatus != newExtractionStatus) {
            extractionStatus = newExtractionStatus;
            if (newExtractionStatus == null || ExtractionStatus.NA.equals(newExtractionStatus)) {
                newExtractionStatus = null;
                setProperty(DownloadLink.PROPERTY_EXTRACTION_STATUS, Property.NULL);
            } else {
                setProperty(DownloadLink.PROPERTY_EXTRACTION_STATUS, newExtractionStatus.name());
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.EXTRACTION_STATUS, newExtractionStatus));
            }
            return;
        }
    }

    public void setVariantSupport(boolean b) {
        if (b == hasVariantSupport()) {
            return;
        }
        if (b) {
            setProperty(VARIANT_SUPPORT, b);
        } else {
            setProperty(VARIANT_SUPPORT, Property.NULL);
        }
        if (hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.VARIANTS_ENABLED, b));
        }
    }

    public boolean hasVariantSupport() {
        return getBooleanProperty(VARIANT_SUPPORT, false);
    }

    public static <T extends DownloadLinkDatabindingInterface> T bindData(final Property property, final String ID, final Class<T> clazz) {
        @SuppressWarnings("unchecked")
        final T ret = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new InvocationHandler() {
            public String getKey(Method method) {
                String key = null;
                if (method.getName().startsWith("set")) {
                    key = method.getName().substring(3).replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ENGLISH);
                } else if (method.getName().startsWith("is")) {
                    key = method.getName().substring(2).replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ENGLISH);
                } else if (method.getName().startsWith("get")) {
                    key = method.getName().substring(3).replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ENGLISH);
                } else {
                    return null;
                }
                Key keyAnnotation = method.getAnnotation(Key.class);
                if (keyAnnotation != null) {
                    key = keyAnnotation.value();
                }
                if (ID != null) {
                    key = ID + key;
                }
                return key;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("reset")) {
                    if (!Clazz.isVoid(method.getReturnType())) {
                        throw new WTFException("reset must have void as return type.");
                    }
                    HashSet<String> reset = new HashSet<String>();
                    for (Method resetMethod : clazz.getDeclaredMethods()) {
                        String key = getKey(resetMethod);
                        if (key != null && reset.add(key)) {
                            property.removeProperty(key);
                        }
                    }
                    return null;
                }
                String key = getKey(method);
                if (key == null) {
                    throw new WTFException("Only Setter and getter are allowed");
                }
                if (method.getName().startsWith("set")) {
                    if (method.getParameterTypes().length != 1) {
                        throw new WTFException("Setter " + method + " should have 1 parameter. instead: " + Arrays.toString(method.getParameterTypes()));
                    }
                    if (!Clazz.isVoid(method.getReturnType())) {
                        throw new WTFException("Setter " + method + " must not have any return type. Has: " + method.getReturnType());
                    }
                    Class<?> param = method.getParameterTypes()[0];
                    Object arg = args[0];
                    if (Clazz.isPrimitiveWrapper(param) && arg == null) {
                        property.setProperty(key, Property.NULL);
                        return null;
                    }
                    if (Clazz.isEnum(param)) {
                        if (arg == null) {
                            property.setProperty(key, Property.NULL);
                        } else {
                            property.setProperty(key, ((Enum<?>) arg).name());
                        }
                        return null;
                    }
                    property.setProperty(key, arg);
                } else {
                    Type returnType = method.getGenericReturnType();
                    if (method.getParameterTypes().length != 0) {
                        throw new WTFException("Getter " + method + " must not have any parameter. instead: " + Arrays.toString(method.getParameterTypes()));
                    }
                    if (Clazz.isVoid(method.getReturnType())) {
                        throw new WTFException("Getter " + method + " must have a return type. is Void.");
                    }
                    Object value = property.getProperty(key);
                    if (Clazz.isBoolean(returnType)) {
                        if (value == null) {
                            if (Clazz.isPrimitiveWrapper(returnType)) {
                                return null;
                            }
                            return false;
                        }
                        if (value instanceof Boolean) {
                            return value;
                        }
                    } else if (Clazz.isByte(returnType)) {
                        if (value == null) {
                            if (Clazz.isPrimitiveWrapper(returnType)) {
                                return null;
                            }
                            return (byte) 0;
                        }
                        if (value instanceof Number) {
                            return ((Number) value).byteValue();
                        }
                    } else if (Clazz.isDouble(returnType)) {
                        if (value == null) {
                            if (Clazz.isPrimitiveWrapper(returnType)) {
                                return null;
                            }
                            return 0d;
                        }
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        }
                    } else if (Clazz.isEnum(returnType)) {
                        if (value == null) {
                            return null;
                        }
                        if (value instanceof Enum) {
                            return value;
                        }
                        if (value instanceof String) {
                            return Enum.valueOf(((Enum<?>) returnType).getDeclaringClass(), (String) value);
                        }
                    } else if (Clazz.isFloat(returnType)) {
                        if (value == null) {
                            if (Clazz.isPrimitiveWrapper(returnType)) {
                                return null;
                            }
                            return 0f;
                        }
                        if (value instanceof Number) {
                            return ((Number) value).floatValue();
                        }
                    } else if (Clazz.isInteger(returnType)) {
                        if (value == null) {
                            if (Clazz.isPrimitiveWrapper(returnType)) {
                                return null;
                            }
                            return 0;
                        }
                        if (value instanceof Number) {
                            return ((Number) value).intValue();
                        }
                    } else if (Clazz.isLong(returnType)) {
                        if (value == null) {
                            if (Clazz.isPrimitiveWrapper(returnType)) {
                                return null;
                            }
                            return 0l;
                        }
                        if (value instanceof Number) {
                            return ((Number) value).longValue();
                        }
                    } else if (Clazz.isShort(returnType)) {
                        if (value == null) {
                            if (Clazz.isPrimitiveWrapper(returnType)) {
                                return null;
                            }
                            return (short) 0;
                        }
                        if (value instanceof Number) {
                            return ((Number) value).shortValue();
                        }
                    } else if (Clazz.isString(returnType)) {
                        if (value == null) {
                            return null;
                        }
                        if (value instanceof String) {
                            return value;
                        }
                    } else {
                        return value;
                    }
                    throw new WTFException("Cannot restore " + returnType + " from " + value);
                }
                return null;
            }
        });
        return ret;
    }

    public <T extends DownloadLinkDatabindingInterface> T bindData(Class<T> clazz) {
        return bindData(this, null, clazz);
    }

    public void firePropertyChange(DownloadLinkProperty downloadLinkProperty) {
        notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, downloadLinkProperty);
    }

    public void setVariants(List<? extends LinkVariant> list) {
        final String variantsString = JSonStorage.serializeToJson(list);
        if (!StringUtils.equals(variantsString, getStringProperty("VARIANTS"))) {
            this.setProperty("VARIANTS", variantsString);
            getTempProperties().removeProperty("VARIANTS");
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.VARIANTS, list));
            }
        }
    }

    public void setVariant(LinkVariant variant) {
        if (!StringUtils.equals(getStringProperty("VARIANT"), variant._getUniqueId())) {
            this.setProperty("VARIANT", variant._getUniqueId());
            setVariantSupport(true);
            final LinkVariant existingVariant = (LinkVariant) getTempProperties().getProperty("VARIANT");
            if (existingVariant != null && !variant._getUniqueId().equals(existingVariant._getUniqueId())) {
                getTempProperties().setProperty("VARIANT", null);
            }
            final PluginForHost plugin = getDefaultPlugin();
            if (plugin != null) {
                plugin.setLinkID(this, variant);
            } else {
                final boolean isOriginal = variant == null || GenericVariants.ORIGINAL.equals(variant);
                final String orgLinkID = getStringProperty("ORG_LINKID");
                if (isOriginal) {
                    if (orgLinkID != null) {
                        setLinkID(orgLinkID);
                    }
                } else {
                    if (orgLinkID == null) {
                        final String linkID = getLinkID();
                        setProperty("ORG_LINKID", linkID);
                    }
                    setLinkID(orgLinkID + "_" + variant._getUniqueId());
                }
            }
            if (hasNotificationListener()) {
                notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.VARIANT, variant));
            }
        }
    }

    public <T extends LinkVariant> T getVariant(Class<T> type) {
        if (getBooleanProperty("GENERIC_VARIANTS", false) && !GenericVariants.class.equals(type)) {
            return null;
        }
        try {
            final Object variant = getTempProperties().getProperty("VARIANT");
            if (variant != null && type.isAssignableFrom(variant.getClass())) {
                return (T) variant;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final String variantID = getStringProperty("VARIANT");
        if (variantID != null) {
            try {
                final List<T> variants = getVariants(type);
                if (variants != null) {
                    for (final LinkVariant variant : variants) {
                        if (variant._getUniqueId().equals(variantID) && type.isAssignableFrom(variant.getClass())) {
                            getTempProperties().setProperty("VARIANT", variant);
                            return (T) variant;
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isGenericVariantSupport() {
        return getBooleanProperty("GENERIC_VARIANTS", false);
    }

    public <T extends LinkVariant> List<T> getVariants(final Class<T> type) {
        if (isGenericVariantSupport() && !GenericVariants.class.equals(type)) {
            return null;
        }
        try {
            final List<LinkVariant> variants = (List<LinkVariant>) getTempProperties().getProperty("VARIANTS");
            if (variants != null && variants.size() > 0) {
                final LinkVariant castTest = variants.get(0);
                if (type.isAssignableFrom(castTest.getClass())) {
                    return (List<T>) variants;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final String variantsID = getStringProperty("VARIANTS");
        if (variantsID != null) {
            try {
                final ArrayList<T> ret = new ArrayList<T>();
                final TypeRef<ArrayList<T>> tref = new TypeRef<ArrayList<T>>() {
                };
                final ParameterizedType t = (ParameterizedType) tref.getType();
                final Type actual = t.getActualTypeArguments()[0];
                final ArrayList<Object> basic = JSonStorage.restoreFromString(variantsID, new TypeRef<ArrayList<Object>>() {
                });
                for (Object o : basic) {
                    T restored = JSonStorage.convert(o, new TypeRef<T>() {
                        @Override
                        public Type getType() {
                            return type;
                        }
                    });
                    ret.add(restored);
                }
                getTempProperties().setProperty("VARIANTS", ret);
                return ret;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void setCustomExtension(String extension) {
        String old = getCustomExtension();
        setProperty("EXTENSION", extension);
        cachedName = null;
        if (!StringUtils.equals(old, extension) && hasNotificationListener()) {
            notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.NAME, extension));
        }
    }

    public String getCustomExtension() {
        return getStringProperty("EXTENSION");
    }

    public String getContentUrlOrPatternMatcher() {
        final String ret = getContentUrl();
        if (StringUtils.isNotEmpty(ret)) {
            return ret;
        }
        return getPluginPatternMatcher();
    }

    public void addHistoryEntry(HistoryEntry entry) {
        if (entry != null) {
            final int maxEntries = CFG_GENERAL.CFG.getMaxDownloadLinkHistoryEntries();
            if (maxEntries > 0) {
                synchronized (this) {
                    if (history == null) {
                        history = new ArrayList<HistoryEntry>();
                    }
                    while (history.size() > maxEntries) {
                        history.remove(0);
                    }
                    history.add(entry);
                }
                if (hasNotificationListener()) {
                    notifyChanges(AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new DownloadLinkProperty(this, DownloadLinkProperty.Property.HISTORY, entry));
                }
            }
        }
    }

    public List<HistoryEntry> getHistory() {
        synchronized (this) {
            if (history != null) {
                return new ArrayList<HistoryEntry>(history);
            } else {
                return new ArrayList<HistoryEntry>(0);
            }
        }
    }

    public HistoryEntry getLatestHistoryEntry() {
        synchronized (this) {
            if (history == null || history.size() == 0) {
                return null;
            }
            return history.get(history.size() - 1);
        }
    }
}
