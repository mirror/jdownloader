package jd.plugins;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.JProgressBar;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download festgehalten.
 * Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink{
    /**
     * Beschreibung des Downloads
     */
    private String name;
    /**
     * Von hier soll de Download stattfinden
     */
    private URL urlDownload;
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
    private boolean inProgress;
    /**
     * Das Plugin, das für diesen Download zuständig ist
     */
    private Plugin plugin;
    /**
     * Die Fortschrittsanzeige
     */
    private JProgressBar progressBar = null;
    /**
     * Hierhin soll die Datei gespeichert werden.
     */
    private File fileOutput;
    /**
     * Logger für Meldungen
     */
    private Logger logger = Plugin.getLogger();
    /**
     * Die Größe in Bytes dieser Datei
     */
    private int downloadLength;
    /**
     * Die bereits heruntergeladenen Bytes
     */
    private int downloadedBytes;
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
        this.fileOutput = new File("C:\\"+name);
        try {
            this.urlDownload = new URL(urlDownload);
        }
        catch (MalformedURLException e) {logger.severe("url malformed. "+e.toString());}
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
    public Plugin getPlugin() { return plugin; }
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
    public URL getUrlDownload() { return urlDownload; }
    /**
     * Liefert die bisher heruntergeladenen Bytes zurück
     * 
     * @return Anzahl der heruntergeladenen Bytes
     */
    public int getDownloadedBytes() { return downloadedBytes; }
    /**
     * Die Größe der Datei
     * 
     * @return Die Größe der Datei
     */
    public int getDownloadLength() { return downloadLength; }
    /**
     * Diese Fortschrittsanzeige zeigt prozentual, wieviel bereits heruntergeladen wurde
     * 
     * @return Die Fortschrittsanzeige
     */
    public JProgressBar getProgressBar(){ return progressBar; }
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
     * Setzt die URL, von der heruntergeladen werden soll
     * 
     * @param urlDownload Die URL von der heruntergeladen werden soll
     */
    public void setUrlDownload(URL urlDownload){ this.urlDownload = urlDownload; }
    /**
     * Kennzeichnet den Download als in Bearbeitung oder nicht
     * 
     * @param inProgress wahr, wenn die Datei als in Bearbeitung gekennzeichnet werden soll
     */
    public void setInProgress(boolean inProgress) { this.inProgress = inProgress; }
    /**
     * Setzt die Anzahl der heruntergeladenen Bytes fest und aktualisiert die
     * Fortschrittsanzeige
     * 
     * @param downloadedBytes Anzahl der heruntergeladenen Bytes
     */
    public void setDownloadedBytes(int downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
        if(this.progressBar!=null)
            this.progressBar.setValue(downloadedBytes);
    }
    /**
     * Setzt die Größe der herunterzuladenden Datei, und aktualisiert
     * die Fortschrittsanzeige
     * 
     * @param downloadLength Die Größe der Datei
     */
    public void setDownloadLength(int downloadLength) {
        this.downloadLength = downloadLength;
        if(this.progressBar == null)
            this.progressBar = new JProgressBar();
        this.progressBar.setMaximum(downloadLength);
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DownloadLink)
            return this.urlDownload.equals(((DownloadLink)obj).urlDownload);
        else
            return super.equals(obj);
    }
}
