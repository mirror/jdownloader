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

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.JDLogger;
import jd.controlling.SingleDownloadController;
import jd.controlling.SpeedMeter;
import jd.event.JDBroadcaster;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download
 * festgehalten. Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */

class DownloadLinkBroadcaster extends JDBroadcaster<DownloadLinkListener, DownloadLinkEvent> {

    // @Override
    protected void fireEvent(DownloadLinkListener listener, DownloadLinkEvent event) {
        listener.handle_DownloadLinkEvent(event);
    }

}

public class DownloadLink extends Property implements Serializable, Comparable<DownloadLink> {

    public static enum AvailableStatus {
        UNCHECKED, FALSE, UNCHECKABLE, TRUE;
        
      
    }

    public static final int LINKTYPE_CONTAINER = 1;

    public static final int LINKTYPE_JDU = 2;

    public static final int LINKTYPE_NORMAL = 0;

    // Logger für Meldungen
    private static Logger logger = jd.controlling.JDLogger.getLogger();

    private static final long serialVersionUID = 1981079856214268373L;

    public static final String UNKNOWN_FILE_NAME = "unknownFileName.file";

    public static final String STATIC_OUTPUTFILE = "STATIC_OUTPUTFILE";

    private transient AvailableStatus availableStatus = AvailableStatus.UNCHECKED;

    private long[] chunksProgress = null;

    private transient DownloadLinkBroadcaster broadcaster = new DownloadLinkBroadcaster();

    // Containername
    private String container;

    // Dateiname des Containers
    private String containerFile = null;

    // Index dieses DownloadLinks innerhalb der Containerdatei
    private int containerIndex = -1;

    // Aktuell heruntergeladene Bytes der Datei
    private long downloadCurrent;

    private transient DownloadInterface downloadInstance;

    private transient SingleDownloadController downloadLinkController;

    // Maximum der heruntergeladenen Datei (Dateilänge)
    private long downloadMax;

    private String subdirectory = null;

    private String browserurl = null;

    private FilePackage filePackage;

    // Hoster des Downloads
    private String host;

    // Zeigt an, ob dieser Downloadlink aktiviert ist
    private boolean isEnabled;

    private boolean isMirror = false;

    // Lokaler Pfad zum letzten captchafile
    private File latestCaptchaFile = null;

    private LinkStatus linkStatus;

    private int linkType = LINKTYPE_NORMAL;

    private int globalSpeedLimit = -1;

    // Beschreibung des Downloads
    private String name;

    // Das Plugin, das für diesen Download zuständig ist
    private transient PluginForHost plugin;

    /**
     * Falls vorhanden, das Plugin für den Container, aus der dieser Download
     * geladen wurde
     */
    private transient PluginsC pluginForContainer;

    private String sourcePluginComment = null;

    private ArrayList<String> sourcePluginPasswordList = null;

    // Speedmeter zum berechnen des Downloadspeeds
    private transient SpeedMeter speedMeter;

    /**
     * Wird dieser Wert gesetzt, so wird der Download unter diesem Namen (nicht
     * Pfad) abgespeichert.
     */
    private String finalFileName;

    // Von hier soll de Download stattfinden
    private String urlDownload;

    /**
     * Password welches einem weiteren Decrypter-Plugin übergeben werden soll
     * (zb FolderPassword)
     */
    private String decrypterPassword;

    private String mD5Hash;

    private transient PluginProgress pluginProgress;

    private String sha1Hash;

    private int priority = 0;

    private transient ImageIcon icon;

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
        priority = 0;
        setName(name);
        sourcePluginPasswordList = new ArrayList<String>();

        downloadMax = 0;
        this.host = host == null ? null : host.toLowerCase();
        this.isEnabled = isEnabled;
        speedMeter = new SpeedMeter();

        this.setUrlDownload(urlDownload);

        if (name == null && urlDownload != null) {
            this.name = Plugin.extractFileNameFromURL(getDownloadURL());
        }
    }

    public synchronized JDBroadcaster<DownloadLinkListener, DownloadLinkEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new DownloadLinkBroadcaster();
        return broadcaster;
    }

    public DownloadLink addSourcePluginPassword(String sourcePluginPassword) {
        if (sourcePluginPasswordList.indexOf(sourcePluginPassword) < 0 && sourcePluginPassword != null && sourcePluginPassword.trim().length() > 0) {
            sourcePluginPasswordList.add(sourcePluginPassword);
        }
        return this;
    }

    public void addSourcePluginPasswords(String[] sourcePluginPasswords) {
        for (String sourcePluginPassword : sourcePluginPasswords) {
            addSourcePluginPassword(sourcePluginPassword);
        }
    }

    public void addSourcePluginPasswordList(ArrayList<String> sourcePluginPasswords) {
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
        return this.getDownloadURL().compareTo(o.getDownloadURL());
    }

    /**
     * Gibt ein arry mit den Chunkfortschritten zurück. Dieses Array wird von
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
        return getFileOutput0();
    }

    public String getFileOutput0() {
        String sfo = this.getStringProperty(DownloadLink.STATIC_OUTPUTFILE, null);
        if (getFilePackage() == FilePackage.getDefaultFilePackage() && sfo != null && new File(sfo).exists()) { return sfo;

        }
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
            setFilePackage(FilePackage.getDefaultFilePackage());
        }
        return filePackage;
    }

    /**
     * Gibt den Hoster dieses Links azurück.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost() {
        if (host == null) return null;
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
        if (this.globalSpeedLimit <= 0) {
            this.globalSpeedLimit = Integer.MAX_VALUE;
        }
        return this.globalSpeedLimit;
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
        if (sourcePluginPasswordList==null||sourcePluginPasswordList.size() == 0) { return null; }
        if (sourcePluginPasswordList.size() == 1) { return sourcePluginPasswordList.get(0); }
        StringBuilder ret = new StringBuilder();
        ret.append('{');
        for (int i = 0; i < sourcePluginPasswordList.size(); i++) {
            if (sourcePluginPasswordList.get(i).trim().length() > 0) {
                ret.append('"');
                ret.append(sourcePluginPasswordList.get(i));
                ret.append('"');
                if (i < sourcePluginPasswordList.size() - 1) {
                    ret.append(new char[] { ',', ' ' });
                }
            }
        }
        ret.append('}');
        return ret.toString();
    }

    public ArrayList<String> getSourcePluginPasswordList() {
        return sourcePluginPasswordList;
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
     * @return true falls der Download abgebrochen wurde
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
    public boolean isAvailabilityStatusChecked() {

        return availableStatus != AvailableStatus.UNCHECKED;

    }

    /**
     * Führt einen verfügbarkeitscheck durch. GIbt true zurück wenn der link
     * online ist, oder wenn er nicht prüfbar ist
     * 
     * @return true/false
     */
    public boolean isAvailable() {
        return getAvailableStatus() != AvailableStatus.FALSE;
    }

    public AvailableStatus getAvailableStatus() {
        if (availableStatus != AvailableStatus.UNCHECKED) { return availableStatus; }
        int wait = 0;

        for (int retry = 0; retry < 5; retry++) {
            try {
                availableStatus = getPlugin().requestFileInformation(this);
                try {
                    getPlugin().getBrowser().getHttpConnection().disconnect();
                } catch (Exception e) {
                }
                break;
            } catch (UnknownHostException e) {
                availableStatus = AvailableStatus.UNCHECKABLE;
                break;
            } catch (PluginException e) {
                e.fillLinkStatus(this.getLinkStatus());

                availableStatus = AvailableStatus.FALSE;
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
                    // logger.severe("Hoster Plugin Version: " +
                    // getPlugin().getVersion());
                    // JDLogger.getLogger().log(java.util.logging.
                    // Level.SEVERE,"Exception occurred",e);
                    break;
                }
            } catch (Exception e) {
                // logger.severe("Hoster Plugin Version: " +
                // getPlugin().getVersion());
                // JDLogger.getLogger().log(java.util.logging.Level
                // .SEVERE,"Exception occurred",e);
                availableStatus = AvailableStatus.UNCHECKABLE;
                break;
            }
        }
        if (availableStatus == null) availableStatus = AvailableStatus.UNCHECKABLE;
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

    /**
     * Zeigt ob der Speed limitiert ist!
     * 
     * @return
     */
    public boolean isLimited() {
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) != 0;
    }

    public boolean isMirror() {
        return isMirror;
    }

    public void requestGuiUpdate() {
        DownloadController.getInstance().fireDownloadLinkUpdate(this);
    }

    // Setzt alle DownloadWErte zurück
    public void reset() {
        if (getLinkStatus().isPluginActive()) {
            setAborted(true);
            while (getLinkStatus().isPluginActive()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {

                    JDLogger.exception(e);
                }
            }
        }
        if (getPlugin() != null) getPlugin().reset_downloadlink(this);
        // downloadMax = 0;
        priority = 0;
        chunksProgress = null;
        downloadLinkController = null;
        downloadCurrent = 0;
        linkStatus.reset();
        this.availableStatus = null;
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
        getPlugin().resetHosterWaitTime();
    }

    /**
     * Kann mit setAborted(true) den Download abbrechen
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
        this.availableStatus = available ? AvailableStatus.TRUE : AvailableStatus.FALSE;
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
        containerFile = containerFile.replace("\\", "/");

        int index;
        if ((index = containerFile.indexOf("/container/")) > 0) {
            containerFile = containerFile.substring(index + 1);

        }

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
     * Verändert den Aktiviert-Status Resets ERROR_TEMPORARILY_UNAVAILABLE!!
     * 
     * @param isEnabled
     *            Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setEnabled(boolean isEnabled) {
        if (this.isEnabled != isEnabled) {
            if (!isEnabled) {
                this.getBroadcaster().fireEvent(new DownloadLinkEvent(this, DownloadLinkEvent.DISABLED));
            } else {
                this.getBroadcaster().fireEvent(new DownloadLinkEvent(this, DownloadLinkEvent.ENABLED));
            }
        }
        this.getLinkStatus().removeStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);

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
     * Setzt das FilePackage für diesen Download
     * 
     * @param FilePackage
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
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        }
        this.setIcon(null);
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
     * setzt den Statischen Dateinamen. Ist dieser wert != null, sow ird er zum
     * Speichern der Datei verwendet. ist er ==null, so wird der dateiName im
     * Plugin automatisch ermittelt. ACHTUNG: Diese Funktion sollte nicht ! von
     * den Plugins verwendet werden. Sie dient dazu der Gui die Möglichkeit zu
     * geben unabhängig von den Plugins einen Downloadnamen festzulegen.
     * userinputs>automatische erkenung Plugins solten setName(String) verwenden
     * um den Speichernamen anzugeben.
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
            urlDownload = urlDownload.trim();
        }
        if (urlDownload != null) {
            this.urlDownload = urlDownload;
        } else {
            this.urlDownload = null;
        }
    }

    /**
     * Diese Methhode fragt das eigene Plugin welche Informationen über die File
     * bereit gestellt werden. Der String eignet Sich zur Darstellung in der UI
     * 
     * @return STring
     */
    // @Override
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
     * Gibt das Password zurück, welches vom Decrypter-Plugin genutzt werden
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

    /**
     * ermittel das icon zur datei . ACHTUNG UNCACHED
     * 
     * @return
     */
    public ImageIcon getUnscaledIcon() {
        return JDImage.getFileIcon(JDIO.getFileExtension(getFileOutput()));

    }

    /**
     * ermittel das icon zur datei
     * 
     * @return
     */
    public ImageIcon getIcon() {
        if (icon == null) {
            if (OSDetector.isLinux() || OSDetector.isMac()) {
                try {
                    Image image = JDTheme.getImage(JDTheme.getTheme() + "/mime/" + JDIO.getFileExtension(this.getName()), 16, 16);
                    icon = new ImageIcon(image);
                } catch (Exception e) {
                    icon = JDTheme.II("gui.images.link", 16, 16);
                }
            } else {
                try {
                    icon = JDImage.getScaledImageIcon(JDImage.getFileIcon(JDIO.getFileExtension(getFileOutput())), 16, 16);
                } catch (Exception e) {
                    try {
                        Image image = JDTheme.getImage(JDTheme.getTheme() + "/mime/" + JDIO.getFileExtension(this.getName()), 16, 16);
                        icon = new ImageIcon(image);
                    } catch (Exception e2) {
                        icon = JDTheme.II("gui.images.link", 16, 16);
                    }
                }

            }
        }
        return icon;
    }
}