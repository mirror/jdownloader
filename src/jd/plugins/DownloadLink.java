package jd.plugins;

import java.io.File;
import java.io.Serializable;
import java.util.logging.Logger;

import jd.JDCrypt;
import jd.JDUtilities;
import jd.controlling.SpeedMeter;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download
 * festgehalten. Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink implements Serializable, Comparable {
    /**
     * Link muß noch bearbeitet werden
     */
    public final static int   STATUS_TODO                          = 0;

    /**
     * Link wurde erfolgreich heruntergeladen
     */
    public final static int   STATUS_DONE                          = 1;

    /**
     * Ein unbekannter Fehler ist aufgetreten
     */

    public final static int   STATUS_ERROR_UNKNOWN                 = 2;

    /**
     * Captcha Text war falsch
     */
    public final static int   STATUS_ERROR_CAPTCHA_WRONG           = 3;

    /**
     * Download Limit wurde erreicht
     */
    public final static int   STATUS_ERROR_DOWNLOAD_LIMIT          = 4;

    /**
     * Der Download ist gelöscht worden (Darf nicht verteilt werden)
     */
    public final static int   STATUS_ERROR_FILE_ABUSED             = 5;

    /**
     * Die Datei konnte nicht gefunden werden
     */
    public final static int   STATUS_ERROR_FILE_NOT_FOUND          = 6;

    /**
     * Die Datei konnte nicht gefunden werden
     */
    public final static int   STATUS_ERROR_BOT_DETECTED            = 7;

    /**
     * Ein unbekannter Fehler ist aufgetreten. Der Download Soll wiederholt
     * werden
     */
    public final static int   STATUS_ERROR_UNKNOWN_RETRY           = 8;

    /**
     * Es gab Fehler mit dem captchabild (konnte nicht geladn werden)
     */

    public final static int   STATUS_ERROR_CAPTCHA_IMAGEERROR      = 9;

    /**
     * Zeigt an, dass der Download aus unbekannten Gründen warten muss. z.B.
     * weil Die Ip gerade gesperrt ist, oder eine Session id abgelaufen ist
     */
    public final static int   STATUS_ERROR_STATIC_WAITTIME         = 10;

    /**
     * Zeigt an dass die Logins beim premiumlogin nicht richtig waren
     */
    public static final int   STATUS_ERROR_PREMIUM_LOGIN           = 11;

    /**
     * zeigt einen Premiumspezifischen fehler an
     */
    public static final int   STATUS_ERROR_PREMIUM                 = 12;

    /**
     * Zeigt an dass der Link fertig geladen wurde
     */
    public static final int   STATUS_DOWNLOAD_FINISHED             = 13;

    /**
     * Zeigt an dass der Link nicht vollständig geladen wurde
     */
    public static final int   STATUS_DOWNLOAD_INCOMPLETE           = 14;

    /**
     * Zeigt an dass der Link gerade heruntergeladen wird
     */
    public static final int   STATUS_DOWNLOAD_IN_PROGRESS          = 15;

    /**
     * Der download ist zur Zeit nicht möglich
     */
    public static final int   STATUS_ERROR_TEMPORARILY_UNAVAILABLE = 16;

    /**
     * serialVersionUID
     */

    private FilePackage       filePackage;

    private static final long serialVersionUID                     = 1981079856214268373L;

    /**
     * Statustext der von der GUI abgefragt werden kann
     */
    private transient boolean aborted                              = false;

    private String            statusText                           = "";

    /**
     * Beschreibung des Downloads
     */
    private String            name;

    /**
     * TODO downloadpath ueber config setzen
     */
    private String            downloadPath                         = JDUtilities.getConfiguration().getDownloadDirectory();

    /**
     * Von hier soll de Download stattfinden
     */
    private String            urlDownload;

    /**
     * Hoster des Downloads
     */
    private String            host;

    /**
     * Zeigt an, ob dieser Downloadlink aktiviert ist
     */
    private boolean           isEnabled;

    /**
     * Zeigt, ob dieser DownloadLink grad heruntergeladen wird
     */
    private transient boolean inProgress                           = false;

    /**
     * Das Plugin, das für diesen Download zuständig ist
     */
    private transient Plugin  plugin;

    /**
     * Maximum der heruntergeladenen Datei (Dateilänge)
     */
    private long              downloadMax;

    /**
     * Aktuell heruntergeladene Bytes der Datei
     */
    private long              downloadCurrent;

    private String            password;

    private String            comment;

    /**
     * Hierhin soll die Datei gespeichert werden.
     */
    private String            fileOutput;

    /**
     * Logger für Meldungen
     */
    @SuppressWarnings("unused")
    private transient Logger  logger                               = Plugin.getLogger();

    /**
     * Status des DownloadLinks
     */
    private int               status                               = STATUS_TODO;

    /**
     * Timestamp bis zu dem die Wartezeit läuft
     */
    private transient long    mustWaitTil                          = 0;

    /**
     * Ursprüngliche Wartezeit
     */
    private transient long    waittime                             = 0;

    public DownloadLink() {

    }

    /**
     * Lokaler Pfad zum letzten captchafile
     */
    private File                 latestCaptchaFile = null;

    /**
     * Speedmeter zum berechnen des downloadspeeds
     */
    private transient SpeedMeter speedMeter;

    private transient Boolean    available         = null;

    /**
     * Erzeugt einen neuen DownloadLink
     * 
     * @param plugin Das Plugins, das für diesen Download zuständig ist
     * @param name Bezeichnung des Downloads
     * @param host Anbieter, von dem dieser Download gestartet wird
     * @param urlDownload Die Download URL (Entschlüsselt)
     * @param isEnabled Markiert diesen DownloadLink als aktiviert oder
     *            deaktiviert
     */
    public DownloadLink(Plugin plugin, String name, String host, String urlDownload, boolean isEnabled) {
        this.plugin = plugin;
        this.name = name;
        this.host = host;
        this.isEnabled = isEnabled;
        speedMeter = new SpeedMeter(1000);

        this.urlDownload = JDCrypt.encrypt(urlDownload);
    }

    /**
     * Liefert den Datei Namen dieses Downloads zurück
     * 
     * @return Name des Downloads
     */
    public String getName() {
        return name;
    }

    /**
     * Gibt den Hoster dieses Links azurück.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost() {
        return host;
    }

    /**
     * Legt den Host fest
     * 
     * @param host Der Host für diesen DownloadLink
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Liefert das Plugin zurück, daß diesen DownloadLink handhabt
     * 
     * @return Das Plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Liefert die Datei zurück, in die dieser Download gespeichert werden soll
     * 
     * @return Die Datei zum Abspeichern
     */
    public String getFileOutput() {
        if (getFilePackage() != null && getFilePackage().getDownloadDirectory() != null && getFilePackage().getDownloadDirectory().length() > 0) {
            return new File(new File(getFilePackage().getDownloadDirectory()), name).getAbsolutePath();
        }
        else {
            return new File(new File(downloadPath), name).getAbsolutePath();

        }

    }

    /**
     * Gibt zurück ob Dieser Link schon auf verfügbarkeit getestet wurde.
     * 
     * @return
     */
    public boolean isAvailabilityChecked() {
        return available != null;

    }

    /**
     * Führt einen verfügbarkeitscheck durch
     * 
     * @return
     */
    public boolean isAvailable() {
        if (this.available != null) {
            return available;
        }
        available = ((PluginForHost) getPlugin()).getFileInformation(this);
        return available;
    }

    /**
     * @author coalado
     * @return Die verschlüsselte URL
     */
    public String getEncryptedUrlDownload() {
        return urlDownload;
    }

    /**
     * Liefert die URL zurück, unter der dieser Download stattfinden soll (Ist
     * verschlüsselt)
     * 
     * @return Die Download URL
     */
    public String getUrlDownload() {
        return urlDownload;
    }

    /**
     * Liefert die URL zurück, unter der dieser Download stattfinden soll
     * (Entschlüsselt)
     * 
     * @return Die Download URL
     */
    public String getUrlDownloadDecrypted() {
        if (urlDownload == null) return null;
        return JDCrypt.decrypt(urlDownload);
    }

    /**
     * Liefert die bisher heruntergeladenen Bytes zurück
     * 
     * @return Anzahl der heruntergeladenen Bytes
     */
    public long getDownloadCurrent() {
        return downloadCurrent;
    }

    /**
     * Die Größe der Datei
     * 
     * @return Die Größe der Datei
     */
    public long getDownloadMax() {
        return downloadMax;
    }

    /**
     * Liefert den Status dieses Downloads zurück
     * 
     * @return Status des Downloads
     */
    public int getStatus() {
        return status;
    }

    /**
     * Zeigt, ob dieser Download grad in Bearbeitung ist
     * 
     * @return wahr, wenn diese Download grad heruntergeladen wird
     */
    public boolean isInProgress() {
        return inProgress;
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
     * Setzt nachträglich das Plugin. Wird nur zum Laden der Liste benötigt
     * 
     * @param plugin Das für diesen Download zuständige Plugin
     */
    public void setLoadedPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Verändert den Aktiviert-Status
     * 
     * @param isEnabled Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Setzt die Ausgabedatei
     * 
     * @param fileOutput Die Datei, in der dieser Download gespeichert werden
     *            soll
     */
    public void setFileOutput(String fileOutput) {
        this.fileOutput = fileOutput;
    }

    /**
     * Setzt die URL, von der heruntergeladen werden soll (Ist schon
     * verschlüsselt)
     * 
     * @param urlDownload Die URL von der heruntergeladen werden soll
     */
    public void setUrlDownload(String urlDownload) {
        this.urlDownload = urlDownload;
    }

    /**
     * Kennzeichnet den Download als in Bearbeitung oder nicht
     * 
     * @param inProgress wahr, wenn die Datei als in Bearbeitung gekennzeichnet
     *            werden soll
     */
    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    /**
     * Setzt den Status des Downloads
     * 
     * @param status Der neue Status des Downloads
     */
    public void setStatus(int status) {
        this.status = status;

    }

    /**
     * Setzt die Anzahl der heruntergeladenen Bytes fest und aktualisiert die
     * Fortschrittsanzeige
     * 
     * @param downloadedCurrent Anzahl der heruntergeladenen Bytes
     */
    public void setDownloadCurrent(long downloadedCurrent) {
        this.downloadCurrent = downloadedCurrent;
    }

    /**
     * Setzt die Größe der herunterzuladenden Datei, und aktualisiert die
     * Fortschrittsanzeige
     * 
     * @param downloadMax Die Größe der Datei
     */
    public void setDownloadMax(int downloadMax) {
        this.downloadMax = downloadMax;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DownloadLink)
            return this.urlDownload.equals(((DownloadLink) obj).urlDownload);
        else
            return super.equals(obj);
    }

    /**
     * @return downloadPath Downloadpfad
     */
    public String getDownloadPath() {
        return downloadPath;
    }

    /**
     * Setzt den Downloadpfad neu
     * 
     * @param downloadPath der neue downloadPfad
     */
    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;

    }

    /**
     * Setzt den Namen des Downloads neu
     * 
     * @param name Neuer Name des Downloads
     */
    public void setName(String name) {
        this.name = name;

    }

    /**
     * Setzt den Statustext der in der GUI angezeigt werden kann
     * 
     * @param text
     */
    public void setStatusText(String text) {
        statusText = text;
    }

    /**
     * Erstellt den Statustext, fügt eine eventl Wartezeit hzin und gibt diesen
     * Statusstrin (bevorzugt an die GUI) zurück
     * 
     * @return Statusstring mit eventl Wartezeit
     */

    public String getStatusText() {

        int speed;
        if (getRemainingWaittime() > 0) {
            return this.statusText + "Warten: (" + JDUtilities.formatSeconds((int)(getRemainingWaittime() / 1000)) + "sek)";
        }
        if (this.isInProgress() && (speed = getDownloadSpeed()) > 0) {
            long remainingBytes = this.getDownloadMax() - this.getDownloadCurrent();
            long eta = remainingBytes / speed;

            return "ETA " + JDUtilities.formatSeconds((int) eta) + " @ " + (speed / 1000) + "kb/s.";
        }

        if (!this.isEnabled()) {
            return "deaktiviert";
        }
        return this.statusText;

    }

    /**
     * Setzt alle DownloadWErte zurück
     */
    public void reset() {

        downloadMax = 0;

        downloadCurrent = 0;
    }

    /**
     * Gibt nur den Dateinamen aus der URL extrahiert zurück. Um aufd en
     * dateinamen zuzugreifen sollte bis auf Ausnamen immer
     * DownloadLink.getName() verwendet werden
     * 
     * @return
     */
    public String getFileNameFrom() {
        int index = Math.max(this.getUrlDownloadDecrypted().lastIndexOf("/"), this.getUrlDownloadDecrypted().lastIndexOf("\\"));
        return this.getUrlDownloadDecrypted().substring(index + 1);
    }

    /**
     * Setzt die zeit in ms ab der die Wartezeit vorbei ist.
     * 
     * @param l
     */
    public void setEndOfWaittime(long l) {
        this.mustWaitTil = l;
        waittime = l - System.currentTimeMillis();

    }

    /**
     * Gibt die wartezeit des Downloads zurück
     * 
     * @return Totale Wartezeit
     */
    public int getWaitTime() {
        return (int) waittime;
    }

    /**
     * Gibt die Verbleibende Wartezeit zurück
     * 
     * @return verbleibende wartezeit
     */
    public long getRemainingWaittime() {
        return Math.max(0, this.mustWaitTil - System.currentTimeMillis());
    }

    public void setLatestCaptchaFile(File dest) {
        this.latestCaptchaFile = dest;

    }

    public File getLatestCaptchaFile() {
        return latestCaptchaFile;
    }

    public void addBytes(int bytes) {

        this.getSpeedMeter().addValue(bytes);

    }

    /**
     * Gibt den internen Speedmeter zurück
     * 
     * @return Speedmeter
     */
    public SpeedMeter getSpeedMeter() {
        if (speedMeter == null) {
            speedMeter = new SpeedMeter(1000);
        }
        return speedMeter;
    }

    /**
     * Gibt die aktuelle Downloadgeschwindigkeit in bytes7sekunde zurück
     * 
     * @return
     */
    public int getDownloadSpeed() {
        if (getStatus() != DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) return 0;
        return getSpeedMeter().getSpeed();
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public String getPassword() {
        if (getFilePackage() == null) return null;
        return getFilePackage().getPassword();
    }

    public String getComment() {
        if (getFilePackage() == null) return null;
        return getFilePackage().getComment();
    }

    public FilePackage getFilePackage() {
        return filePackage;
    }

    public void setFilePackage(FilePackage filePackage) {
        this.filePackage = filePackage;
    }

    public int compareTo(Object o) {
        if (o instanceof DownloadLink) {

            if (((DownloadLink) o).getFileNameFrom().compareToIgnoreCase(getFileNameFrom()) > 0) {
                return -1;
            }
            else if (((DownloadLink) o).getFileNameFrom().compareToIgnoreCase(getFileNameFrom()) < 0) {
                return 1;
            }
            else {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Diese methhode Frag das eigene Plugin welche Informationen über die File
     * bereit gestellt werden. Der String eignet Sich zur darstellung in der UI
     * 
     * @return STring
     */
    public String getDisplayedFilename() {
        if (getPlugin() instanceof PluginForHost) {
            return ((PluginForHost) getPlugin()).getDisplayedFilename(this);
        }
        else {
            return getName();
        }
    }

}
