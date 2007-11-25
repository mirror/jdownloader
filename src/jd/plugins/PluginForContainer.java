package jd.plugins;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;

import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 * 
 * @author astaldo/coalado
 */

public abstract class PluginForContainer extends PluginForDecrypt {
    protected String               md5;

    protected Vector<String>       downloadLinksURL;
    private static HashMap<String,PluginForContainer> plugins= new HashMap<String,PluginForContainer>();
    protected Vector<DownloadLink> containedLinks = new Vector<DownloadLink>();

    /**
     * Diese Methode liefert eine URL zurück, von der aus der Download gestartet
     * werden kann
     * 
     * @param downloadLink Der DownloadLink, dessen URL zurückgegeben werden
     *            soll
     * @return Die URL als String
     */

    public String getUrlDownloadDecrypted(DownloadLink downloadLink) {
        return downloadLinksURL.get(downloadLink.getContainerIndex());
    }

    

    /**
     * Wird von der parentklasse für jeden step aufgerufen. Diese Methode muss
     * alle steps abarbeiten und abgecshlossene schritte zurückgeben
     * 
     * @param step
     * @param parameter
     * @return
     */
    public abstract PluginStep doStep(PluginStep step, File container);

    public abstract String encrypt(String plain);

    /**
     * Erstellt eine Kopie des Containers im Homedir.
     */
    public PluginStep doStep(PluginStep step, String parameter) {
        String file = (String) parameter;
       
        File f = new File(file);
        if(md5==null)md5=JDUtilities.getLocalHash(f);
    
        String extension = JDUtilities.getFileExtension(f);
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
     * zurück. Dabei wird das Plugin Schritt für Schritt abgearbeitet. Auf das Stepsystem wird hier verzichtet.
     * 
     * @param filename Die Containerdatei
     * @return Ein Vector mit DownloadLinks
     */
    public Vector<DownloadLink> getContainedDownloadlinks(String filename) {
    
        if(containedLinks==null||containedLinks.size()==0){
            doStep(new PluginStep(PluginStep.STEP_OPEN_CONTAINER, null), filename);
            logger.info(filename+" Parse");
        }
 //logger.info(filename+" Cached");
        return containedLinks;
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


/**
 * Gibt das passende plugin für diesen container zurück. falls schon eins exestiert wird dieses zurückgegeben.
 * @param containerFile
 * @return
 */
    public PluginForContainer getPlugin(String containerFile) {
        if(plugins.containsKey(containerFile))return plugins.get(containerFile);
        try {
            PluginForContainer newPlugin = this.getClass().newInstance();
            plugins.put(containerFile, newPlugin);
            return newPlugin;
        }
        catch (InstantiationException e) {
     
           JDUtilities.logException(e);
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            JDUtilities.logException(e);
            e.printStackTrace();
        }
        return null;
    }
}
