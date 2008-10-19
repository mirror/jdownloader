//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.net.URL;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.SingleDownloadController;
import jd.controlling.SpeedMeter;
import jd.event.ControlEvent;
import jd.http.Encoding;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download
 * festgehalten. Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink extends Property implements Serializable, Comparable<DownloadLink> {

    public static final int LINKTYPE_CONTAINER = 1;

    public static final int LINKTYPE_JDU = 2;

    public static final int LINKTYPE_NORMAL = 0;

    /**
     * Logger für Meldungen
     */
    private static Logger logger = JDUtilities.getLogger();

    private static final long serialVersionUID = 1981079856214268373L;

    public static final String UNKNOWN_FILE_NAME = "unknownFileName.file";

    private transient Boolean available = null;

    private long[] chunksProgress = null;

    /**
     * Containername
     */
    private String container;

    /**
     * Dateiname des Containers
     */
    private String containerFile;

    /**
     * Index dieses DownloadLinks innerhalb der Containerdatei
     */
    private int containerIndex = -1;

    /**
     * Aktuell heruntergeladene Bytes der Datei
     */
    private long downloadCurrent;

    private transient DownloadInterface downloadInstance;

    private transient SingleDownloadController downloadLinkController;

    /**
     * Maximum der heruntergeladenen Datei (Dateilänge)
     */
    private long downloadMax;

    private String subdirectory = null;

    private String browserurl = null;

    private FilePackage filePackage;

    /**
     * Hoster des Downloads
     */
    private String host;

    /**
     * Zeigt an, ob dieser Downloadlink aktiviert ist
     */
    private boolean isEnabled;

    private boolean isMirror = false;

    /**
     * Lokaler Pfad zum letzten captchafile
     */
    private File latestCaptchaFile = null;

    private LinkStatus linkStatus;

    private int linkType = LINKTYPE_NORMAL;

    private int globalSpeedLimit = -1;
    private int localSpeedLimit = -1;

    /**
     * Beschreibung des Downloads
     */
    private String name;

    private int partID = -1;

    /**
     * Das Plugin, das für diesen Download zuständig ist
     */
    private transient PluginForHost plugin;

    /**
     * Falls vorhanden, das Plugin für den Container, aus der dieser Download
     * geladen wurde
     */
    private transient PluginsC pluginForContainer;

    private String sourcePluginComment = null;

    private Vector<String> sourcePluginPasswords = null;

    /**
     * Speedmeter zum berechnen des downloadspeeds
     */
    private transient SpeedMeter speedMeter;

    /**
     * Wird dieser Wert gesetzt, so wird der Download unter diesem Namen (nicht
     * Pfad) abgespeichert.
     */
    private String finalFileName;

    private transient String tempUrlDownload;

    /**
     * Von hier soll de Download stattfinden
     */
    private String urlDownload;

    /**
     * Password welches einem weiteren Decrypter-Plugin übergeben werden soll
     * (zb FolderPassword)
     */
    private String decrypterPassword;

    private String mD5Hash;

    private transient PluginProgress pluginProgress;

    private String sha1Hash;

    /**
     * Erzeugt einen neuen DownloadLink
     * 
     * @param plugin
     *            Das Plugins, das für diesen Download zuständig ist
     * @param name
     *            Bezeichnung des Downloads
     * @param host
     *            Anbieter, von dem dieser Download gestartet wird
     * @param urlDownload
     *            Die Download URL (Entschlüsselt)
     * @param isEnabled
     *            Markiert diesen DownloadLink als aktiviert oder deaktiviert
     */
    public DownloadLink(PluginForHost plugin, String name, String host, String urlDownload, boolean isEnabled) {
        this.plugin = plugin;
        setName(name);
        sourcePluginPasswords = new Vector<String>();

        downloadMax = 0;
        this.host = host == null ? null : host.toLowerCase();
        this.isEnabled = isEnabled;
        speedMeter = new SpeedMeter();

        this.setUrlDownload(urlDownload);

        if (name == null && urlDownload != null) {
            this.name = Plugin.extractFileNameFromURL(getDownloadURL());
        }
    }

    public DownloadLink addSourcePluginPassword(String sourcePluginPassword) {

        if (sourcePluginPasswords.indexOf(sourcePluginPassword) < 0 && sourcePluginPassword != null && sourcePluginPassword.trim().length() > 0) {
            sourcePluginPasswords.add(sourcePluginPassword);
        }

        return this;
    }

    public void addSourcePluginPasswords(Vector<String> sourcePluginPasswords) {
        for (int i = 0; i < sourcePluginPasswords.size(); i++) {
            addSourcePluginPassword(sourcePluginPasswords.get(i));
        }

    }

    /**
     * Übernimmt das subdirectory von einem anderen Downloadlink. Zum erstellen
     * eines eigenen subdirectorys steht addSubdirectory(String s) zur
     * verfügung.
     * 
     * @param downloadLink
     */
    public void setSubdirectory(DownloadLink downloadLink) {
        subdirectory = downloadLink.getSubdirectory();
    }

    public void addSpeedValue(int speed) {
        getSpeedMeter().addSpeedValue(speed);
    }

    public int compareTo(DownloadLink o) {
        return getDownloadURL().compareTo(o.getDownloadURL());
    }

    /**
     * Gibt ein arry mit den Chunkfortschritten zurück. Dieses Array wird von
     * der downlaodinstanz zu resumezwecken verwendet
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
        return containerFile;
    }

    public int getContainerIndex() {
        return containerIndex;
    }

    /**
     * Liefert die bisher heruntergeladenen Bytes zurück
     * 
     * @return Anzahl der heruntergeladenen Bytes
     */
    public long getDownloadCurrent() {
        return downloadCurrent;
    }

    public DownloadInterface getDownloadInstance() {
        return downloadInstance;
    }

    public SingleDownloadController getDownloadLinkController() {
        return downloadLinkController;
    }

    /**
     * Die Größe der Datei
     * 
     * @return Die Größe der Datei
     */
    public long getDownloadSize() {

        return Math.max(getDownloadCurrent(), downloadMax);
    }

    /**
     * Gibt die aktuelle Downloadgeschwindigkeit in bytes/sekunde zurück
     * 
     * @return Downloadgeschwindigkeit in bytes/sekunde
     */
    public int getDownloadSpeed() {
        if (!getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) { return -1; }
        return getSpeedMeter().getSpeed();
    }

    /**
     * Liefert die URL zurück, unter der dieser Download stattfinden soll
     * 
     * @return Die Download URL
     */
    public String getDownloadURL() {

        if (linkType == LINKTYPE_CONTAINER) {
            if (tempUrlDownload != null) { return tempUrlDownload; }
            String ret;
            if (pluginForContainer != null) {
                ret = pluginForContainer.extractDownloadURL(this);
                if (ret == null) {
                    logger.severe(this + " is a containerlink. Container could not be extracted. Is your JD Version up2date?");
                }
                return ret;
            } else {
                logger.severe(this + " is a containerlink, but no plugin could be found");
                return null;

            }

        }
        return urlDownload;
    }

    public String getBrowserUrl() {
        if (browserurl != null) { return browserurl; }
        return getDownloadURL();
    }

    public void setBrowserUrl(String url) {
        browserurl = url;
    }

    /**
     * Gibt den Darstellbaren Dateinamen zurück. Dabei handelt es sich nicht
     * zwangsläufig um einen Valid-Filename. Dieser String eignet sich zur
     * darstellung des link und kann zusatzinformationen wie dateigröße oder
     * verfügbarkeit haben Diese Zusatzinformationen liefert das zugehörige
     * Plugin ACHTUNG: Weil der Dateiname kein zuverlässiger Dateiname sein muss
     * darf diese FUnktion nicht verwendet werden um eine datei zu benennen.
     * 
     * @return Erweiterter "Dateiname"
     */
    public String getFileInfomationString() {

        return getPlugin().getFileInformationString(this);

    }

    /**
     * Liefert die Datei zurück, in die dieser Download gespeichert werden soll
     * 
     * @return Die Datei zum Abspeichern
     */
    public String getFileOutput() {
        return JDUtilities.validatePath(getFileOutput0());

    }

    public String getFileOutput0() {
        if (subdirectory != null) {
            if (getFilePackage() != null && getFilePackage().getDownloadDirectory() != null && getFilePackage().getDownloadDirectory().length() > 0) {
                return new File(new File(getFilePackage().getDownloadDirectory(), File.separator + subdirectory), getName()).getAbsolutePath();
            } else {

                return null;
            }
        } else {
            if (getFilePackage() != null && getFilePackage().getDownloadDirectory() != null && getFilePackage().getDownloadDirectory().length() > 0) {
                return new File(new File(getFilePackage().getDownloadDirectory()), getName()).getAbsolutePath();
            } else {
                return null;
            }
        }
    }

    /**
     * Gibt das Filepacket des Links zurück. Kann auch null sein!! (Gui
     * abhängig)
     * 
     * @return FilePackage
     */
    public FilePackage getFilePackage() {
        if (filePackage == null) {
            setFilePackage(JDUtilities.getController().getDefaultFilePackage());
        }
        return filePackage;
    }

    /**
     * Gibt den Hoster dieses Links azurück.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost() {
        return host.toLowerCase();
    }

    /**
     * Gibt den Pfad zum zuletzt gespeichertem Captchabild für diesen download
     * zurück
     * 
     * @return captcha pfad
     */
    public File getLatestCaptchaFile() {
        return latestCaptchaFile;
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
     * Gibt das SpeedLimit zurück! Beachtet das Lokale Speed Limit! in bytes/s
     * 
     * @return
     */
    public int getSpeedLimit() {
        if (this.localSpeedLimit <= 0) {
            if (this.globalSpeedLimit <= 0) {
                this.globalSpeedLimit = Integer.MAX_VALUE;
            }
        } else {
            this.globalSpeedLimit = this.localSpeedLimit;
        }
        return this.globalSpeedLimit;
    }

    /**
     * Gibt das Lokale Speed Limit zurück! in bytes/s
     * 
     * @return
     */
    public int getLocalSpeedLimit() {
        return this.localSpeedLimit;
    }

    /**
     * Setzt das Lokale Speed Limit! in bytes/s
     * 
     * @param maximalspeed
     */
    public void setLocalSpeedLimit(int maximalspeed) {
        if (maximalspeed <= 0) {
            this.localSpeedLimit = -1;
        } else {
            this.localSpeedLimit = maximalspeed;
        }
    }

    /**
     * Setzt das Globale Speed Limit! in bytes/s
     * 
     * @param maximalspeed
     */
    public void setSpeedLimit(int maximalspeed) {
        maximalspeed = Math.max(20, maximalspeed);
        // logger.info(this + " LINKSPEED: " + maximalspeed);
        int diff = Math.abs(this.globalSpeedLimit - maximalspeed);
        if (diff > 500) {
            this.globalSpeedLimit = maximalspeed;
        }
    }

    /**
     * Liefert den Datei Namen dieses Downloads zurück. Wurde der Name mit
     * setfinalFileName(String) festgelegt wird dieser Name zurückgegeben
     * 
     * @return Name des Downloads
     */
    public String getName() {
        String urlName;
        if (getFinalFileName() == null) {
            try {
                return name == null ? ((urlName = new File(new URL(this.getDownloadURL()).toURI()).getName()) != null ? urlName : UNKNOWN_FILE_NAME) : name;
            } catch (Exception e) {
                return UNKNOWN_FILE_NAME;
            }
        }
        return getFinalFileName();

    }

    public int getPartByName() {
        return partID;
    }

    /**
     * Liefert das Plugin zurück, daß diesen DownloadLink handhabt
     * 
     * @return Das Plugin
     */
    public PluginForHost getPlugin() {
        return plugin;
    }

    public PluginsC getPluginForContainer() {
        return pluginForContainer;
    }

    public String getSourcePluginComment() {
        return sourcePluginComment;
    }

    public String getSourcePluginPassword() {
        if (sourcePluginPasswords.size() == 0) { return null; }
        if (sourcePluginPasswords.size() == 1) { return sourcePluginPasswords.get(0); }
        String ret = "{";
        for (int i = 0; i < sourcePluginPasswords.size(); i++) {
            if (sourcePluginPasswords.get(i).trim().length() > 0) {
                ret += "\"" + sourcePluginPasswords.get(i) + "\"";
                if (i < sourcePluginPasswords.size() - 1) {
                    ret += ", ";
                }
            }
        }
        ret += "}";
        return ret;
    }

    public Vector<String> getSourcePluginPasswords() {
        return sourcePluginPasswords;
    }

    /**
     * Gibt den internen Speedmeter zurück
     * 
     * @return Speedmeter
     */
    public SpeedMeter getSpeedMeter() {
        if (speedMeter == null) {
            speedMeter = new SpeedMeter();
        }
        return speedMeter;
    }

    /**
     * Gibt den Finalen Downloadnamen zurück. Wird null zurückgegeben, so wird
     * der dateiname von den jeweiligen plugins automatisch ermittelt.
     * 
     * @return Statischer Dateiname
     */
    public String getFinalFileName() {
        return finalFileName;
    }

    /**
     * @return true falls der download abgebrochen wurde
     */
    public boolean isAborted() {
        if (getDownloadLinkController() == null) { return false;

        }
        return getDownloadLinkController().isAborted();
    }

    /**
     * Gibt zurück ob Dieser Link schon auf verfügbarkeit getestet wurde.+ Diese
     * FUnktion führt keinen!! Check durch. Sie prüft nur ob schon geprüft
     * worden ist. anschießend kann mit isAvailable() die verfügbarkeit
     * überprüft werden
     * 
     * @return Link wurde schon getestet (true) nicht getestet(false)
     */
    public boolean isAvailabilityChecked() {
        return available != null;

    }

    /**
     * Führt einen verfügbarkeitscheck durch. GIbt true zurück wenn der link
     * online ist
     * 
     * @return true/false
     */
    public boolean isAvailable() {
        if (available != null) { return available; }
        try {
            available = ((PluginForHost) getPlugin()).getFileInformation(this);
        } catch (Exception e) {
            e.printStackTrace();
            available = false;
        }
        try {
            ((PluginForHost) getPlugin()).getBrowser().getHttpConnection().disconnect();
        } catch (Exception e) {
        }
        return available;
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
     * Zeigt ob der Speed limitiert ist!
     * 
     * @return
     */
    public boolean isLimited() {
        if (this.localSpeedLimit > 0) return true;
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) != 0;
    }

    public boolean isMirror() {
        return isMirror;
    }

    public void requestGuiUpdate() {
        JDUtilities.getController().fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, this);

    }

    /**
     * Setzt alle DownloadWErte zurück
     */
    public void reset() {
        if (getLinkStatus().isPluginActive()) {
            setAborted(true);
            while (getLinkStatus().isPluginActive()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }
        downloadMax = 0;

        chunksProgress = null;
        downloadLinkController = null;
        downloadCurrent = 0;
        linkStatus.reset();
        this.setEnabled(true);
        linkStatus = new LinkStatus(this);
        if (new File(this.getFileOutput()).exists()) {
            if (!new File(this.getFileOutput()).delete()) {
                logger.severe(JDLocale.L("system.download.errors.couldnotoverwrite", "Could not overwrite existing file"));
            }
        }
        if (new File(this.getFileOutput() + ".part").exists()) {
            if (!new File(this.getFileOutput() + ".part").delete()) {
                logger.severe(JDLocale.L("system.download.errors.couldnotoverwrite", "Could not overwrite existing file"));
            }
        }
        finalFileName = null;
        localSpeedLimit = -1;
    }

    /**
     * kann mit setAborted(true) den Download abbrechen
     * 
     * @param aborted
     */
    public void setAborted(boolean aborted) {
        if (aborted == false) {
            // logger.severe("cannot unabort a link. use reset()");
            return;
        }
        if (getDownloadLinkController() == null) {
            logger.severe("Tried to abort download even it has no downloadController");
            linkStatus.setInProgress(false);
            return;

        }
        getDownloadLinkController().abortDownload();

    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * Die Downloadklasse kann hier ein array mit den Fortschritten der chunks
     * ablegen. Damit können downloads resumed werden
     * 
     * @param is
     */
    public void setChunksProgress(long[] is) {
        chunksProgress = is;

    }

    public void setContainerFile(String containerFile) {
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
     * Setzt die Größe der herunterzuladenden Datei
     * 
     * @param downloadMax
     *            Die Größe der Datei
     */
    public void setDownloadSize(long downloadMax) {

        this.downloadMax = downloadMax;
    }

    /**
     * Setzt ein Subdirectory für den DeonloadLink neu
     * 
     * @param downloadPath
     *            der neue downloadPfad
     */
    public void addSubdirectory(String subdir) {
        if (subdir != null && name.length() > 0) {
            if (subdirectory != null) {
                subdirectory = new StringBuilder(subdirectory).append(File.separator).append(JDUtilities.removeEndingPoints(JDUtilities.validateFileandPathName(subdir))).toString();
            } else {
                subdirectory = JDUtilities.removeEndingPoints(JDUtilities.validateFileandPathName(subdir));
            }
        } else {
            subdirectory = null;
        }
    }

    public String getSubdirectory() {
        return subdirectory;
    }

    /**
     * Verändert den Aktiviert-Status
     * 
     * @param isEnabled
     *            Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;

        if (!isEnabled) {
            setAborted(true);
        } else {
            setAborted(false);

        }
        if (isEnabled == true) {

            setAborted(false);
            if (host != null && plugin == null) {
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
     * Setzt das FilePackage für diesen download
     * 
     * @param FilePackage
     */
    public void setFilePackage(FilePackage filePackage) {

        if (filePackage == this.filePackage && filePackage != null) {
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

    /**
     * Speichert das zuletzt geladene captchabild für diesen link
     * 
     * @param dest
     */
    public void setLatestCaptchaFile(File dest) {
        latestCaptchaFile = dest;

    }

    public void setLinkType(int linktypeContainer) {
        if (linktypeContainer == linkType) { return; }
        if (linkType == LINKTYPE_CONTAINER) {
            logger.severe("You are not allowd to Change the Linktype of " + this);
            return;
        }
        if (linktypeContainer == LINKTYPE_CONTAINER && urlDownload != null) {
            logger.severe("This link already has a value for urlDownload");
            urlDownload = null;
        }
        linkType = linktypeContainer;

    }

    /**
     * Setzt nachträglich das Plugin. Wird nur zum Laden der Liste benötigt
     * 
     * @param plugin
     *            Das für diesen Download zuständige Plugin
     */
    public void setLoadedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public void setLoadedPluginForContainer(PluginsC pluginForContainer) {
        this.pluginForContainer = pluginForContainer;
        container = pluginForContainer.getHost();
    }

    public void setMirror(boolean isMirror) {
        this.isMirror = isMirror;
    }

    /**
     * Setzt den Namen des Downloads neu
     * 
     * @param name
     *            Neuer Name des Downloads
     */
    public void setName(String name) {
        if (name != null && name.length() > 0) {
            this.name = JDUtilities.removeEndingPoints(JDUtilities.validateFileandPathName(name));
            updatePartID();
        }
    }

    public DownloadLink setSourcePluginComment(String sourcePluginComment) {
        this.sourcePluginComment = sourcePluginComment;
        return this;
    }

    public DownloadLink setSourcePluginPasswords(Vector<String> sourcePluginPassword) {
        sourcePluginPasswords = sourcePluginPassword;
        return this;
    }

    /**
     * setzt den Statischen Dateinamen. Ist dieser wert != null, sow ird er zum
     * Speichern der Datei verwendet. ist er ==null, so wird der dateiName im
     * Plugin automatisch ermittelt. ACHTUNG: Diese Funktion sollte nicht ! von
     * den Plugins verwendet werden. Sie dient dazu der Gui die Möglichkeit zu
     * geben unabhängig von den Plugins einen Downloadnamen festzulegen.
     * userinputs>automatische erkenung Plugins solten setName(String) verwenden
     * um den Speichernamen anzugeben.
     * 
     */
    public void setFinalFileName(String newfinalFileName) {
        if (newfinalFileName != null && newfinalFileName.length() > 0) {
            finalFileName = JDUtilities.removeEndingPoints(JDUtilities.validateFileandPathName(newfinalFileName));
            updatePartID();
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
            urlDownload = urlDownload.trim();
        }
        if (linkType == LINKTYPE_CONTAINER) {
            tempUrlDownload = urlDownload;
            this.urlDownload = null;
            return;
        }
        if (urlDownload != null) {
            this.urlDownload = urlDownload;
        } else {
            this.urlDownload = null;
        }

    }

    /**
     * Diese methhode Frag das eigene Plugin welche Informationen über die File
     * bereit gestellt werden. Der String eignet Sich zur darstellung in der UI
     * 
     * @return STring
     */
    @Override
    public String toString() {
        return getName() + "-> " + getFileOutput() + "(" + getHost() + ")";
    }

    private void updatePartID() {
        String name = getName();
        String ext;
        int index;
        partID = -1;
        while (name.length() > 0) {
            index = name.lastIndexOf(".");
            if (index <= 0) {
                index = name.length() - 1;
            }
            ext = name.substring(index + 1);
            name = name.substring(0, index);
            try {
                partID = Integer.parseInt(Encoding.filterString(ext, "1234567890"));
                break;
            } catch (Exception e) {
            }

        }

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
     * gibt das Password zurück, welches vom Decrypter-Plugin genutzt werden
     * kann (zb. FolderPassword)
     */
    public String getDecrypterPassword() {
        return this.decrypterPassword;
    }

    /**
     * setzt das Password, welches vom Decrypter-Plugin genutzt werden kann (zb.
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
      this.sha1Hash=hash;
        
    }

    public String getSha1Hash() {
        return sha1Hash;
    }

}
