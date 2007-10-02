package jd.plugins;

import java.util.Vector;

import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 * 
 * @author astaldo
 */
public abstract class PluginForContainer extends PluginForDecrypt{
    /**
     * Diese Methode liefert eine URL zurück, von der aus der Download gestartet werden kann
     * @param downloadLink Der DownloadLink, dessen URL zurückgegeben werden soll
     * @return Die URL als String
     */
    public abstract String getUrlDownloadDecrypted(DownloadLink downloadLink);
    /**
     * Hiermit werden alle im Plugin selbst gespeicherten DownloadLinks zurückgegeben
     * 
     * @return Alle im Plugin selbst gespeicherten DownloadLinks
     */
    public abstract Vector<DownloadLink> getContainedDownloadLinks();
    /**
     * Liefert alle in der Containerdatei enthaltenen Dateien als DownloadLinks zurück.
     * Dabei wird das Plugin Schritt für Schritt abgearbeitet
     * 
     * @param filename Die Containerdatei
     * @return Ein Vector mit DownloadLinks
     */
    public Vector<DownloadLink> getAllDownloadLinks(String filename){
        PluginStep step = null;
        while ((step = nextStep(step)) != null) {
            doStep(step, filename);
        }
        return getContainedDownloadLinks();
    }
    /**
     * Findet anhand des Hostnamens ein passendes Plugiln
     * 
     * @param data Hostname
     * @return Das gefundene Plugin oder null
     */
    protected PluginForHost findHostPlugin(String data){
        Vector<PluginForHost> pluginsForHost = JDUtilities.getPluginsForHost();
        PluginForHost pHost;
        for (int i = 0; i < pluginsForHost.size(); i++) {
            pHost = pluginsForHost.get(i);
            if (pHost.isClipboardEnabled() && pHost.canHandle(data)) {
                return pHost;
            }
        }
        return null;
    }
}
