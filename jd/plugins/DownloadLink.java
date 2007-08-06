package jd.plugins;

import java.io.File;
import java.net.URLConnection;

import javax.swing.JProgressBar;


/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download festgehalten.
 * Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink {
    /**
     * Beschreibung des Downloads
     */
    private String name;
    /**
     * Hoster des Downloads
     */
    private String host;
    /**
     * Zeigt an, ob dieser Downloadlink aktiviert ist
     */
    private boolean isActive;
    /**
     * Das Plugin, das für diesen Download zuständig ist
     */
    private Plugin plugin;
    /**
     * Die Fortschrittsanzeige
     */
    private JProgressBar progressBar = new JProgressBar();
    /**
     * Hierhin soll die Datei gespeichert werden.
     */
    private File fileOutput;
    private URLConnection urlConnection;
    /**
     * Erzeugt einen neuen DownloadLink
     * 
     * @param plugin Das Plugins, das für diesen Download zuständig ist
     * @param name Bezeichnung des Downloads
     * @param host Anbieter, von dem dieser Download gestartet wird
     * @param isActive Markiert diesen DownloadLink als aktiviert oder deaktiviert
     */
    public DownloadLink(PluginForHost plugin, String name, String host, boolean isActive){
        this.plugin = plugin;
        this.name   = name;
        this.host   = host;
        this.isActive = isActive;
    }
    /**
     * Liefert den Namen dieses Downloads zurück
     * @return Name des Downloads
     */
    public String getName()                    { return name; }
    /**
     * Gibt den Hoster dieses Links azurück.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost()                    { return host; }
    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isActive()                  { return isActive;               }
    /**
     * Verändert den Aktiviert-Status
     * 
     * @param isActive Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setActive(boolean isActive)    { this.isActive = isActive;      }
    public Plugin getPlugin()                  { return plugin;                 }
    public JProgressBar getProgressBar()       { return progressBar;            }
    public File getFileOutput()                { return fileOutput;             }
    public URLConnection getUrlConnection()    { return urlConnection;          }
    public void setFileOutput(File fileOutput)               { this.fileOutput = fileOutput;  }
    public void setUrlConnection(URLConnection urlConnection) { this.urlConnection = urlConnection;   }
}
