package jd;

import java.awt.Toolkit;
import java.io.File;
import java.net.CookieHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginOptional;
import jd.plugins.UserPlugin;
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
    private static Logger logger=JDUtilities.getLogger();

    public static void main(String args[]) {
        if (SingleInstanceController.isApplicationRunning()) {
            JOptionPane.showMessageDialog(null, "jDownloader läuft bereits!", "jDownloader", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
            return;
        }
        SingleInstanceController.bindRMIObject(new SingleInstanceController());
        Main main = new Main();
        main.go();
    }

    private void go() {

        loadImages();
        // der Log kann erst später ausgegeben werden
        String log = "";
        File fileInput = null;
        try {
            fileInput = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        try {
            log += "\r\n" + ("Loaded config from: " + fileInput + " (" + JDUtilities.CONFIG_PATH + ")");
            if (fileInput != null && fileInput.exists()) {
                Object obj = JDUtilities.loadObject(null, fileInput, Configuration.saveAsXML);
                if (obj instanceof Configuration) {
                    Configuration configuration = (Configuration) obj;
                    JDUtilities.setConfiguration(configuration);
                    JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.FINER));
                    JDUtilities.setLocale((Locale) configuration.getProperty(Configuration.PARAM_LOCALE, Locale.getDefault()));
                }
                else {
                    log += "\r\n" + ("Configuration error: " + obj);
                    log += "\r\n" + ("Konfigurationskonflikt. Lade Default einstellungen");
                    JDUtilities.getConfiguration().setDefaultValues();
                }
            }
            else {
                log += "\r\n" + ("no configuration loaded");
                log += "\r\n" + ("Konfigurationskonflikt. Lade Default einstellungen");
                JDUtilities.getConfiguration().setDefaultValues();
            }
        }
        catch (Exception e) {
            log += "\r\n" + ("Konfigurationskonflikt. Lade Default einstellungen");
            JDUtilities.getConfiguration().setDefaultValues();
        }

        // deaktiviere den Cookie Handler. Cookies müssen komplett selbst
        // verwaltet werden. Da JD später sowohl standalone, als auch im
        // Webstart laufen soll muss die Cookieverwaltung selbst übernommen
        // werdn
        CookieHandler.setDefault(null);
        logger.info("Erstelle Controller");
        final JDController controller = new JDController();
      
    
      
                UIInterface uiInterface = new SimpleGUI();
         controller.setUiInterface(uiInterface);
     

        // Startet die Downloads nach dem start
        
         JDUtilities.initFileLogger();
        logger.info(log);
        logger.info("Lade Plugins");
        JDUtilities.loadPlugins();

        logger.info("Lade Queue");
        if (!controller.initDownloadLinks()) {
            File links = JDUtilities.getResourceFile("links.dat");
            if (links != null && links.exists()) {
                File newFile = new File(links.getAbsolutePath() + ".bup");
                newFile.delete();
                links.renameTo(newFile);
                controller.getUiInterface().showMessageDialog("Linkliste inkompatibel. \r\nBackup angelegt: " + newFile + " Liste geleert!");
            }
        }
        logger.info("OS: " + System.getProperty("os.name") + ", " + System.getProperty("os.arch") + ", " + System.getProperty("os.version"));
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

        Iterator<String> iteratorOptional = JDUtilities.getPluginsOptional().keySet().iterator();
        while (iteratorOptional.hasNext()) {
            JDUtilities.getPluginsOptional().get(iteratorOptional.next()).addPluginListener(controller);
        }

        HashMap<String, PluginOptional> pluginsOptional = JDUtilities.getPluginsOptional();

        Iterator<String> iterator = pluginsOptional.keySet().iterator();
        String key;

        while (iterator.hasNext()) {
            key = iterator.next();
            PluginOptional plg = pluginsOptional.get(key);
            if (JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN_" + plg.getPluginName(), false)) {
                try {
                    pluginsOptional.get(key).enable(true);
                }
                catch (Exception e) {
                    logger.severe("Error loading Optional Plugin: " + e.getMessage());
                }
            }
        }
        Interaction.handleInteraction(Interaction.INTERACTION_APPSTART, false);
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_START_DOWNLOADS_AFTER_START, false)) {
            controller.getUiInterface().fireUIEvent(new UIEvent(controller.getUiInterface(), UIEvent.UI_START_DOWNLOADS));
        }

        String hash="";
        if(JDUtilities.getResourceFile("updateLog.txt").exists()){
        hash= JDUtilities.getLocalHash(JDUtilities.getResourceFile("updateLog.txt"));
        }
        String hashChangeLog="";
    
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_HASH, "").equals(hash+hashChangeLog)) {
            logger.info("Returned from Update");
            String lastLog = JDUtilities.getLocalFile(JDUtilities.getResourceFile("updateLog.txt"));
            logger.info("UpdateLog: " + log);
           
           if( JDUtilities.getController().getUiInterface().showConfirmDialog("Update!"+System.getProperty("line.separator") + lastLog+System.getProperty("line.separator")+System.getProperty("line.separator")+"Show Codechanges?")){
               JDUtilities.getController().getUiInterface().showMessageDialog(JDUtilities.getLocalFile(JDUtilities.getResourceFile("changeLog.txt")));
           }
        }
        
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_HASH, hash+hashChangeLog);
        JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectory(), JDUtilities.CONFIG_PATH.split("\\.")[0], "." + JDUtilities.CONFIG_PATH.split("\\.")[1], Configuration.saveAsXML);
        
    }

    /**
     * Die Bilder werden aus der JAR Datei nachgeladen //
     */
    // private void loadImages() {
    // ClassLoader cl = getClass().getClassLoader();
    // Toolkit toolkit = Toolkit.getDefaultToolkit();
    // try {
    //       
    //          
    // File f = new File(cl.getResource("img").toURI());
    // String images[] = f.list();
    // for(int i=0;i<images.length;i++){
    // if(images[i].indexOf(".")!=-1)
    // JDUtilities.addImage(images[i].substring(0, images[i].indexOf(".")),
    // toolkit.getImage(cl.getResource("img/"+images[i])));
    // }
    // }
    // catch (URISyntaxException e) {
    // e.printStackTrace();
    // }
    // }
    private void loadImages() {
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        // ClassLoader cl = getClass().getClassLoader();
        // byte[] bytes = "META-INF/JDOWNLOA.DSA".getBytes();
        // String str="";
        // for( int i=0; i<bytes.length;i++){
        // str+=bytes[i]+", ";
        // }
        // logger.info(str);

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
        JDUtilities.addImage("jd_logo", toolkit.getImage(cl.getResource("img/jd_logo.png")));
        JDUtilities.addImage("reconnect", toolkit.getImage(cl.getResource("img/reconnect.png")));
        JDUtilities.addImage("save", toolkit.getImage(cl.getResource("img/save.png")));
        JDUtilities.addImage("start", toolkit.getImage(cl.getResource("img/start.png")));
        JDUtilities.addImage("stop", toolkit.getImage(cl.getResource("img/stop.png")));
        JDUtilities.addImage("up", toolkit.getImage(cl.getResource("img/up.png")));
        JDUtilities.addImage("update", toolkit.getImage(cl.getResource("img/update.png")));
        JDUtilities.addImage("search", toolkit.getImage(cl.getResource("img/search.png")));
        JDUtilities.addImage("bottom", toolkit.getImage(cl.getResource("img/bottom.png")));
        JDUtilities.addImage("bug", toolkit.getImage(cl.getResource("img/bug.png")));
        JDUtilities.addImage("home", toolkit.getImage(cl.getResource("img/home.png")));
        JDUtilities.addImage("loadContainer", toolkit.getImage(cl.getResource("img/loadContainer.png")));
        JDUtilities.addImage("ok", toolkit.getImage(cl.getResource("img/ok.png")));
        JDUtilities.addImage("pause", toolkit.getImage(cl.getResource("img/pause.png")));
        JDUtilities.addImage("pause_disabled", toolkit.getImage(cl.getResource("img/pause_disabled.png")));
        JDUtilities.addImage("pause_active", toolkit.getImage(cl.getResource("img/pause_active.png")));
        JDUtilities.addImage("shutdown", toolkit.getImage(cl.getResource("img/shutdown.png")));
        JDUtilities.addImage("top", toolkit.getImage(cl.getResource("img/top.png")));
    }
}
