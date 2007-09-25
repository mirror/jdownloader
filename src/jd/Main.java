package jd;

import java.awt.Toolkit;
import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.utils.JDUtilities;

/**
 * $Revision: 242 $ 
 * $Author: coalado $
 * Start der Applikation
 *  
 * @author astaldo
 */

// TODO Clipboard Management
// TODO Links speichern / laden / editieren(nochmal laden, Passwort merken)
// TODO LinkGrabber / Pakete
// TODO VerzeichnisauswahlDialog (zB Beim Initialiseren von WebStart Cookie /
// DownloadDir)
// TODO Klänge wiedergeben
// TODO Fehlerbehandlung der Plugins verbessern
// TODO HTTPPost Klasse untersuchen
// TODO Plugin.download überprüfen (Geschwindigkeitsreduzierung durch
// Thread.sleep?)
// TODO Interactions ergänzen (Download nicht möglich, Externes Programm
// ausführen)
// TODO Interaction - Vector
// TODO Plugin Wartezeit static, falls kein HTTP Reconnect genutzt wird
// (Datetime nextPossibleDownloadAt)
// TODO TelnetReconnect (später)
// TODO WebInterface
// TODO GUI Auswahl
// TODO Programmstart Interaction muss eingebaut werden
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
            }

        }
        else {
            logger.warning("no configuration loaded");

        }

        // logger.info( new
        // Vector(Arrays.asList(getSimpleMatches(requestInfo.getHtmlCode(),this.SIMPLEPATTERN_GEN_DOWNLOADLINK)))+"_");
        // logger.info( new
        // Vector(Arrays.asList(getSimpleMatches(requestInfo.getHtmlCode(),this.SIMPLEPATTERN_GEN_DOWNLOADLINK_link)))+"_");
        // var a = String.fromCharCode(Math.abs(-56)); ..>8
        // var d = '4' + String.fromCharCode(Math.sqrt(10000)); --><d
        // GET
        // /files/0d4d724d830bc600131f2e4628dcecb8/%5BA-E_&_Conclave%5D_Trinity_Blood_01_%5BC94AE728%5D.avi

        // Configuration c = new Configuration();
        // c.setDownloadDirectory("D:\\Downloads");
        // JDUtilities.saveObject(null, c, JDUtilities.getJDHomeDirectory(),
        // "jdownloader", ".config", true);
        logger.info("Lade Plugins");
        JDUtilities.loadPlugins();
        logger.info("Lade GUI");
       UIInterface uiInterface = new SimpleGUI();
//        UIInterface uiInterface = new MainGui();
       // Da muss ne bessere Idee her.
//        if (!JDUtilities.getConfiguration().getConfigurationVersion().equals(JDUtilities.JD_VERSION)) {
//            logger.info("Set Config default Values");
//            JDUtilities.getConfiguration().setDefaultValues();
//            File links = JDUtilities.getResourceFile("links.dat");
//            if (links.exists()) {
//                File newFile = new File(links.getAbsolutePath() + ".bup");
//                newFile.delete();
//                links.renameTo(newFile);
//
//            }
//            uiInterface.showMessageDialog("Inkompatible Konfiguration oder Downloadliste gefunden\r\nDefault Einstellungen geladen, Linkliste zurückgestellt");
//
//        }

        logger.info("Erstelle Controller");
        JDController controller = new JDController();
        controller.setUiInterface(uiInterface);
        logger.info("Lade Queue");
        controller.initDownloadLinks();
        // JDUtilities.registerListenerPluginsForDecrypt(controller);
        // JDUtilities.registerListenerPluginsForHost(controller);
        // JDUtilities.registerListenerPluginsForSearch(controller);
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

        Interaction.handleInteraction(Interaction.INTERACTION_APPSTART, false);
    }

    /**
     * Die Bilder werden aus der JAR Datei nachgeladen
     */
    private void loadImages() {
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        JDUtilities.addImage("add", toolkit.getImage(cl.getResource("img/add.png")));
        JDUtilities.addImage("configuration", toolkit.getImage(cl.getResource("img/configuration.png")));
        JDUtilities.addImage("delete", toolkit.getImage(cl.getResource("img/delete.png")));
        JDUtilities.addImage("dnd", toolkit.getImage(cl.getResource("img/dnd.png")));
        JDUtilities.addImage("dnd_big", toolkit.getImage(cl.getResource("img/dnd_big.png")));
        JDUtilities.addImage("dnd_big_filled", toolkit.getImage(cl.getResource("img/dnd_big_filled.png")));
        JDUtilities.addImage("down", toolkit.getImage(cl.getResource("img/down.png")));
        JDUtilities.addImage("exit", toolkit.getImage(cl.getResource("img/shutdown.png")));
        JDUtilities.addImage("led_empty", toolkit.getImage(cl.getResource("img/led_empty.gif")));
        JDUtilities.addImage("led_green", toolkit.getImage(cl.getResource("img/led_green.gif")));
        JDUtilities.addImage("load", toolkit.getImage(cl.getResource("img/load.png")));
        JDUtilities.addImage("log", toolkit.getImage(cl.getResource("img/log.png")));
        JDUtilities.addImage("mind", toolkit.getImage(cl.getResource("img/mind.png")));
        JDUtilities.addImage("reconnect", toolkit.getImage(cl.getResource("img/reconnect.png")));
        JDUtilities.addImage("save", toolkit.getImage(cl.getResource("img/save.png")));
        JDUtilities.addImage("start", toolkit.getImage(cl.getResource("img/start.png")));
        JDUtilities.addImage("stop", toolkit.getImage(cl.getResource("img/stop.png")));
        JDUtilities.addImage("up", toolkit.getImage(cl.getResource("img/up.png")));
        JDUtilities.addImage("update", toolkit.getImage(cl.getResource("img/update.png")));
        JDUtilities.addImage("search", toolkit.getImage(cl.getResource("img/search.png")));
    }
}
