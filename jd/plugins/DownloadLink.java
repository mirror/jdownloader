package jd.plugins;


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
     * Das Plugin, das für diesen Download zuständig ist
     */
    private Plugin plugin;
    /**
     * Erzeugt einen neuen DownloadLink
     * 
     * @param plugin Das Plugins, das für diesen Download zuständig ist
     * @param name Bezeichnung des Downloads
     * @param host Anbieter, von dem dieser Download gestartet wird
     */
    public DownloadLink(PluginForHost plugin, String name, String host){
        this.plugin = plugin;
        this.name   = name;
        this.host   = host;
    }
    /**
     * Liefert den Namen dieses Downloads zurück
     * @return Name des Downloads
     */
    public String getName(){ return name; }
    /**
     * Gibt den Hoster dieses Links azurück.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost(){ return host; }
}
