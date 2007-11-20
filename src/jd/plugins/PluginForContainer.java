package jd.plugins;

import java.io.File;
import java.util.Vector;

import jd.plugins.container.ContainerInfo;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 * 
 * @author astaldo/coalado
 */


public abstract class PluginForContainer extends PluginForDecrypt {
    protected String md5;
    protected Vector<String>       downloadLinksURL;

    protected Vector<DownloadLink> containedLinks   = new Vector<DownloadLink>();
    /**
     * Diese Methode liefert eine URL zurück, von der aus der Download gestartet
     * werden kann
     * 
     * @param downloadLink Der DownloadLink, dessen URL zurückgegeben werden
     *            soll
     * @return Die URL als String
     */
    public abstract String getUrlDownloadDecrypted(DownloadLink downloadLink);

    /**
     * Hiermit werden alle im Plugin selbst gespeicherten DownloadLinks
     * zurückgegeben
     * 
     * @return Alle im Plugin selbst gespeicherten DownloadLinks
     */
    public abstract Vector<DownloadLink> getContainedDownloadLinks();

    /**
     * Wird von der parentklasse für jeden step aufgerufen. Diese Methode muss
     * alle steps abarbeiten und abgecshlossene schritte zurückgeben
     * 
     * @param step
     * @param parameter
     * @return
     */
    public abstract PluginStep doStep(PluginStep step, File container);

    /**
     * Erstellt eine Kopie des Containers im Homedir.
     */
    public PluginStep doStep(PluginStep step, String parameter) {
        String file = (String) parameter;
        File f = new File(file);
        String[] split = f.getAbsolutePath().split("\\.");
        if (split.length < 2) return null;
        String extension = split[split.length - 1];
        md5 = JDUtilities.getLocalHash(f);
        if (ContainerInfo.getMapFileToContainerInfo().containsKey(md5)) {
            downloadLinksURL = ContainerInfo.getMapFileToContainerInfo().get(md5).getDownloadLinksURL();
            return step;
        }
        
        if (f.exists()) {
            
            File res = JDUtilities.getResourceFile("container/" + md5 + "." + extension);
            if (!res.exists()) {
                JDUtilities.copyFile(f, res);

            }
            if (!res.exists()) {
            logger.severe("Could not copy file to homedir");    
            
            }
            return doStep(step, res);

        }
        return null;
    }

    /**
     * Liefert alle in der Containerdatei enthaltenen Dateien als DownloadLinks
     * zurück. Dabei wird das Plugin Schritt für Schritt abgearbeitet
     * 
     * @param filename Die Containerdatei
     * @return Ein Vector mit DownloadLinks
     */
    public Vector<DownloadLink> getAllDownloadLinks(String filename) {
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
    protected PluginForHost findHostPlugin(String data) {
        Vector<PluginForHost> pluginsForHost = JDUtilities.getPluginsForHost();
        PluginForHost pHost;
        for (int i = 0; i < pluginsForHost.size(); i++) {
            pHost = pluginsForHost.get(i);
            if (pHost.canHandle(data)) {
                return pHost;
            }
        }
        return null;
    }
}
