package jd;

import java.awt.Toolkit;
import java.io.File;
import java.net.CookieHandler;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.utils.JDUtilities;

/**
 * @author astaldo
 */

// TODO Links speichern / laden / editieren(nochmal laden, Passwort merken)
// TODO Klänge wiedergeben
// TODO HTTPPost Klasse untersuchen
// TODO TelnetReconnect (später)
// TODO WebInterface
// TODO GUI Auswahl
public class Main {
    /**
     * @param args
     */
    public static void main(String args[]) {
        Main main = new Main();
        main.go();
    }

    private void go() {
        Logger logger = Plugin.getLogger();
        loadImages();

        File fileInput = null;

        try {

            fileInput = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        try {
            logger.finer("Load config from: " + fileInput + " (" + JDUtilities.CONFIG_PATH + ")");
            if (fileInput != null && fileInput.exists()) {

                Object obj = JDUtilities.loadObject(null, fileInput, Configuration.saveAsXML);
                if (obj instanceof Configuration) {
                    Configuration configuration = (Configuration) obj;
                    JDUtilities.setConfiguration(configuration);
                    Plugin.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.FINER));
                }
                else {
                    logger.severe("Configuration error: " + obj);
                    logger.severe("Konfigurationskonflikt. Lade Default einstellungen");
                    JDUtilities.getConfiguration().setDefaultValues();
                }

            }
            else {
                logger.warning("no configuration loaded");
                logger.severe("Konfigurationskonflikt. Lade Default einstellungen");
                JDUtilities.getConfiguration().setDefaultValues();
            }
        }
        catch (Exception e) {
            logger.severe("Konfigurationskonflikt. Lade Default einstellungen");
            JDUtilities.getConfiguration().setDefaultValues();
        }

        logger.info("Lade Plugins");
        JDUtilities.loadPlugins();
        logger.info("Lade GUI");
        UIInterface uiInterface = new SimpleGUI();
        // deaktiviere den Cookie Handler. Cookies müssen komplett selbst
        // verwaltet werden. Da JD später sowohl standalone, als auch im
        // Webstart laufen soll muss die Cookieverwaltung selbst übernommen
        // werdn
        CookieHandler.setDefault(null);
        logger.info("Erstelle Controller");
        JDController controller = new JDController();
        controller.setUiInterface(uiInterface);
        logger.info("Lade Queue");
        if (!controller.initDownloadLinks()) {

            File links = JDUtilities.getResourceFile("links.dat");
            if (links.exists()) {
                File newFile = new File(links.getAbsolutePath() + ".bup");
                newFile.delete();
                links.renameTo(newFile);
                uiInterface.showMessageDialog("Linkliste inkompatibel. \r\nBackup angelegt: " + newFile + " Liste geleert!");

            }

        }

        logger.info("Registriere Plugins");
        Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
        while (iteratorHost.hasNext()) {
            iteratorHost.next().addPluginListener(controller);
        }
        Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
        while (iteratorDecrypt.hasNext()) {
            iteratorDecrypt.next().addPluginListener(controller);
        }
        Iterator<PluginForSearch> iteratorSearch = JDUtilities.getPluginsForSearch().iterator();
        while (iteratorSearch.hasNext()) {
            iteratorSearch.next().addPluginListener(controller);
        }
        Iterator<PluginForContainer> iteratorContainer = JDUtilities.getPluginsForContainer().iterator();
        while (iteratorContainer.hasNext()) {
            iteratorContainer.next().addPluginListener(controller);
        }

        Interaction.handleInteraction(Interaction.INTERACTION_APPSTART, false);
    }

    /**
     * Die Bilder werden aus der JAR Datei nachgeladen
     */
    private void loadImages() {
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        try {
            File f = new File(cl.getResource("img").toURI());
            String images[] = f.list();
            for(int i=0;i<images.length;i++){
                if(images[i].indexOf(".")!=-1)
                    JDUtilities.addImage(images[i].substring(0, images[i].indexOf(".")), toolkit.getImage(cl.getResource("img/"+images[i])));
            }
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
