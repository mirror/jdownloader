package jd.plugins;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import jd.JDCrypt;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download festgehalten.
 * Die Informationen werden dann in einer Tabelle dargestellt
 *
 * @author astaldo
 */
public class DownloadLink implements Serializable{
    /**
     * Link muß noch bearbeitet werden
     */
    public final static int STATUS_TODO                 = 0;
    /**
     * Link wurde erfolgreich heruntergeladen
     */
    public final static int STATUS_DONE                 = 1;
    /**
     * Ein unbekannter Fehler ist aufgetreten
     */
    public final static int STATUS_ERROR_UNKNOWN        = 2;
    /**
     * Captcha Text war falsch
     */
    public final static int STATUS_ERROR_CAPTCHA_WRONG  = 3;
    /**
     * Download Limit wurde erreicht
     */
    public final static int STATUS_ERROR_DOWNLOAD_LIMIT = 4;
    /**
     * Der Download ist gelöscht worden (Darf nicht verteilt werden)
     */
    public final static int STATUS_ERROR_FILE_ABUSED    = 5;
    /**
     * Die Datei konnte nicht gefunden werden
     */
    public final static int STATUS_ERROR_FILE_NOT_FOUND = 6;
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1981079856214268373L;
    /**
     * Beschreibung des Downloads
     */
    private String name;
    /**
     * TODO downloadpath ueber config setzen
     */
    private String downloadPath=".";
    /**
     * Von hier soll de Download stattfinden
     */
    private String urlDownload;
    /**
     * Hoster des Downloads
     */
    private String host;
    /**
     * Zeigt an, ob dieser Downloadlink aktiviert ist
     */
    private boolean isEnabled;
    /**
     * Zeigt, ob dieser DownloadLink grad heruntergeladen wird
     */
    private transient boolean inProgress = false;;
    /**
     * Das Plugin, das für diesen Download zuständig ist
     */
    private transient PluginForHost plugin;
    /**
     * Maximum der heruntergeladenen Datei (Dateilänge)
     */
    private transient int downloadMax;
    /**
     * Aktuell heruntergeladene Bytes der Datei
     */
    private transient int downloadCurrent;
    /**
     * Die DownloadGeschwindigkeit in bytes/sec
     */
    private transient int downloadSpeed;
    /**
     * Hierhin soll die Datei gespeichert werden.
     */
    private File fileOutput;
    /**
     * Logger für Meldungen
     */
    private transient Logger logger = Plugin.getLogger();
    /**
     * Status des DownloadLinks
     */
    private transient int status = STATUS_TODO;
    /**
     * Erzeugt einen neuen DownloadLink
     *
     * @param plugin Das Plugins, das für diesen Download zuständig ist
     * @param name Bezeichnung des Downloads
     * @param host Anbieter, von dem dieser Download gestartet wird
     * @param urlDownload Die Download URL
     * @param isEnabled Markiert diesen DownloadLink als aktiviert oder deaktiviert
     */
    public DownloadLink(PluginForHost plugin, String name, String host, String urlDownload, boolean isEnabled){
        this.plugin      = plugin;
        this.name        = name;
        this.host        = host;
        this.isEnabled   = isEnabled;
        updateFileOutput();
        this.urlDownload = JDCrypt.encrypt(urlDownload);
    }
    /**
     * Liefert den Namen dieses Downloads zurück
     * @return Name des Downloads
     */
    public String getName() { return name; }
    /**
     * Gibt den Hoster dieses Links azurück.
     *
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost() { return host; }
    /**
     * Liefert das Plugin zurück, daß diesen DownloadLink handhabt
     *
     * @return Das Plugin
     */
    public PluginForHost getPlugin() { return plugin; }
    /**
     * Liefert die Datei zurück, in die dieser Download gespeichert werden soll
     *
     * @return Die Datei zum Abspeichern
     */
    public File getFileOutput() { return fileOutput; }
    /**
     * Liefert die URL zurück, unter der dieser Download stattfinden soll
     *
     * @return Die Download URL
     */
    public URL getUrlDownload() {
        URL url=null;
        try {
            url = new URL(JDCrypt.decrypt(urlDownload));
        }
        catch (MalformedURLException e) { e.printStackTrace();  }
        catch (SecurityException e)     {  e.printStackTrace(); }
        return url;
        }
    /**
     * Liefert die bisher heruntergeladenen Bytes zurück
     *
     * @return Anzahl der heruntergeladenen Bytes
     */
    public int getDownloadCurrent() { return downloadCurrent; }
    /**
     * Die Größe der Datei
     *
     * @return Die Größe der Datei
     */
    public int getDownloadMax() { return downloadMax; }
    /**
     * Liefert den Status dieses Downloads zurück
     * 
     * @return Status des Downloads
     */
    public int getStatus()            { return status;        }
    /**
     * Zeigt, ob dieser Download grad in Bearbeitung ist
     *
     * @return wahr, wenn diese Download grad heruntergeladen wird
     */
    public boolean isInProgress(){ return inProgress; }
    /**
     * Zeigt, ob dieser Download aktiviert ist
     *
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isEnabled() { return isEnabled; }
    /**
     * Setzt nachträglich das Plugin.
     * Wird nur zum Laden der Liste benötigt
     *
     * @param plugin Das für diesen Download zuständige Plugin
     */
    public void setPlugin(PluginForHost plugin){ this.plugin = plugin; }
    /**
     * Verändert den Aktiviert-Status
     *
     * @param isEnabled Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setEnabled(boolean isEnabled)  { this.isEnabled = isEnabled; }
    /**
     * Setzt die Ausgabedatei
     *
     * @param fileOutput Die Datei, in der dieser Download gespeichert werden soll
     */
    public void setFileOutput(File fileOutput) { this.fileOutput = fileOutput; }
    /**
     * diese Methode wird aufgerufen wenn der name bzw. der downloadPath geaendert wurde!
     */
    public void updateFileOutput() { this.fileOutput = new File(downloadPath+System.getProperty("file.separator")+name); }
    /**
     * Setzt die URL, von der heruntergeladen werden soll
     *
     * @param urlDownload Die URL von der heruntergeladen werden soll
     */
    public void setUrlDownload(String urlDownload){ this.urlDownload = JDCrypt.encrypt(urlDownload); }
    /**
     * Kennzeichnet den Download als in Bearbeitung oder nicht
     *
     * @param inProgress wahr, wenn die Datei als in Bearbeitung gekennzeichnet werden soll
     */
    public void setInProgress(boolean inProgress) { this.inProgress = inProgress; }
    /**
     * Setzt den Status des Downloads
     * 
     * @param status Der neue Status des Downloads
     */
    public void setStatus(int status) { this.status = status; }
    /**
     * Setzt die Anzahl der heruntergeladenen Bytes fest und aktualisiert die
     * Fortschrittsanzeige
     *
     * @param downloadedCurrent Anzahl der heruntergeladenen Bytes
     */
    public void setDownloadCurrent(int downloadedCurrent) {
        this.downloadCurrent = downloadedCurrent;
    }
    /**
     * Setzt die Größe der herunterzuladenden Datei, und aktualisiert
     * die Fortschrittsanzeige
     *
     * @param downloadMax Die Größe der Datei
     */
    public void setDownloadMax(int downloadMax) {
        this.downloadMax = downloadMax;
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DownloadLink)
            return this.urlDownload.equals(((DownloadLink)obj).urlDownload);
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
     *  Setzt den Downloadpfad neu
     *  
     *  @param downloadPath der neue downloadPfad
     */
    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
        updateFileOutput();
    }
    /**
     *  Setzt den Namen des Downloads neu
     *  
     *  @param name Neuer Name des Downloads
     */
    public void setName(String name) {
        this.name = name;
        updateFileOutput();
    }
    public int getDownloadSpeed() {
        return downloadSpeed;
    }
    public void setDownloadSpeed(int downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }
}
