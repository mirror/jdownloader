package jd;

import java.awt.Toolkit;
import java.io.File;
import java.net.CookieHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.InfoFileWriter;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginOptional;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import sun.misc.Service;

/**
 * @author coalado
 */

public class JDInit {

    private static Logger logger = JDUtilities.getLogger();
    private boolean installerVisible=false;

    public JDInit() {

    }

    void init() {
        CookieHandler.setDefault(null);
      

    }
/**
 * Bilder werden dynamisch aus dem Homedir geladen.
 */
    public void loadImages() {
        ClassLoader cl = JDUtilities.getJDClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
     
        
       File dir=JDUtilities.getResourceFile("jd/img/");
        
        String[] images = dir.list();
        if(images==null||images.length==0){
            logger.severe("Could not find the img directory");
            return;
        }
        for( int i=0; i<images.length;i++){
            if(images[i].toLowerCase().endsWith(".png")){
                File f=new File(images[i]);
                
               logger.finer("Loaded image: "+f.getName().split("\\.")[0]+" from "+cl.getResource("jd/img/"+f.getName()));
                JDUtilities.addImage(f.getName().split("\\.")[0], toolkit.getImage(cl.getResource("jd/img/"+f.getName())));
            }
            
        }
//        
//        JDUtilities.addImage("add", toolkit.getImage(cl.getResource("img/add.png")));
//        JDUtilities.addImage("configuration", toolkit.getImage(cl.getResource("img/configuration.png")));
//        JDUtilities.addImage("delete", toolkit.getImage(cl.getResource("img/delete.png")));
//        JDUtilities.addImage("dnd", toolkit.getImage(cl.getResource("img/dnd.png")));
//        JDUtilities.addImage("clipboard", toolkit.getImage(cl.getResource("img/clipboard.png")));
//        JDUtilities.addImage("clipboard", toolkit.getImage(cl.getResource("img/clipboard.png")));
//        JDUtilities.addImage("down", toolkit.getImage(cl.getResource("img/down.png")));
//        JDUtilities.addImage("exit", toolkit.getImage(cl.getResource("img/shutdown.png")));
//        JDUtilities.addImage("led_empty", toolkit.getImage(cl.getResource("img/led_empty.gif")));
//        JDUtilities.addImage("led_green", toolkit.getImage(cl.getResource("img/led_green.gif")));
//        JDUtilities.addImage("load", toolkit.getImage(cl.getResource("img/load.png")));
//        JDUtilities.addImage("log", toolkit.getImage(cl.getResource("img/log.png")));
//        JDUtilities.addImage("jd_logo", toolkit.getImage(cl.getResource("img/jd_logo.png")));
//        JDUtilities.addImage("jd_logo_large", toolkit.getImage(cl.getResource("img/jd_logo_large.png")));
//        JDUtilities.addImage("jd_logo_blog", toolkit.getImage(cl.getResource("img/jd_blog.png")));
//        JDUtilities.addImage("reconnect", toolkit.getImage(cl.getResource("img/reconnect.png")));
//        JDUtilities.addImage("save", toolkit.getImage(cl.getResource("img/save.png")));
//        JDUtilities.addImage("start", toolkit.getImage(cl.getResource("img/start.png")));
//        JDUtilities.addImage("stop", toolkit.getImage(cl.getResource("img/stop.png")));
//        JDUtilities.addImage("up", toolkit.getImage(cl.getResource("img/up.png")));
//        JDUtilities.addImage("update", toolkit.getImage(cl.getResource("img/update.png")));
//        JDUtilities.addImage("search", toolkit.getImage(cl.getResource("img/search.png")));
//        JDUtilities.addImage("bottom", toolkit.getImage(cl.getResource("img/bottom.png")));
//        JDUtilities.addImage("bug", toolkit.getImage(cl.getResource("img/bug.png")));
//        JDUtilities.addImage("home", toolkit.getImage(cl.getResource("img/home.png")));
//        JDUtilities.addImage("loadContainer", toolkit.getImage(cl.getResource("img/loadContainer.png")));
//        JDUtilities.addImage("ok", toolkit.getImage(cl.getResource("img/ok.png")));
//        JDUtilities.addImage("pause", toolkit.getImage(cl.getResource("img/pause.png")));
//        JDUtilities.addImage("pause_disabled", toolkit.getImage(cl.getResource("img/pause_disabled.png")));
//        JDUtilities.addImage("pause_active", toolkit.getImage(cl.getResource("img/pause_active.png")));
//        JDUtilities.addImage("shutdown", toolkit.getImage(cl.getResource("img/shutdown.png")));
//        JDUtilities.addImage("top", toolkit.getImage(cl.getResource("img/top.png")));
    }

    public Configuration loadConfiguration() {
        File fileInput = null;
        try {
            fileInput = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        boolean allOK = true;
        try {

            if (fileInput != null && fileInput.exists()) {
                Object obj = JDUtilities.loadObject(null, fileInput, Configuration.saveAsXML);
                if (obj instanceof Configuration) {
                    Configuration configuration = (Configuration) obj;
                    JDUtilities.setConfiguration(configuration);
                    JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.FINER));
                    JDLocale.setLocale(configuration.getStringProperty(Configuration.PARAM_LOCALE,"german"));
                    JDTheme.setTheme(configuration.getStringProperty(Configuration.PARAM_THEME,"default"));
                }
                else {
                    // log += "\r\n" + ("Configuration error: " + obj);
                    // log += "\r\n" + ("Konfigurationskonflikt. Lade Default
                    // einstellungen");
                    allOK = false;
                    if (JDUtilities.getConfiguration() == null) JDUtilities.getConfiguration().setDefaultValues();
                }
            }
            else {
                logger.info ("no configuration loaded");
                logger.info ("Konfigurationskonflikt. Lade Default einstellungen");
                
                allOK = false;
                if (JDUtilities.getConfiguration() == null) JDUtilities.getConfiguration().setDefaultValues();
            }
        }
        catch (Exception e) {
           logger.info("Konfigurationskonflikt. Lade Default einstellungen");
           e.printStackTrace();
            allOK = false;
            if (JDUtilities.getConfiguration() == null) JDUtilities.setConfiguration(new Configuration());
            JDUtilities.getConfiguration().setDefaultValues();
        }

        if (!allOK) {
         
        
            installerVisible=true;
            Installer inst = new Installer();
            if (!inst.isAborted() && inst.getHomeDir() != null && inst.getDownloadDir() != null) {

                String newHome = inst.getHomeDir();
                logger.info("Home Dir: " + newHome);
                File homeDirectoryFile = new File(newHome);
                boolean createSuccessfull = true;
                if (!homeDirectoryFile.exists()) createSuccessfull = homeDirectoryFile.mkdirs();
                if (createSuccessfull && homeDirectoryFile.canWrite()) {
                    System.setProperty("jdhome", homeDirectoryFile.getAbsolutePath());
                    String dlDir = inst.getDownloadDir();

                    JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.welcome","Welcome to jDownloader. Download missing files."));

                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, dlDir);

                    JDUtilities.download(new File(homeDirectoryFile, "webupdater.jar"), "http://jdownloader.ath.cx/autoUpdate2/webupdater.jar");

                    JDUtilities.setHomeDirectory(homeDirectoryFile.getAbsolutePath());

                    JDUtilities.saveConfig();
                    logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.RUNTYPE_LOCAL_JARED }, homeDirectoryFile.getAbsolutePath(), 0));
                    System.exit(0);

                }
                logger.info("INSTALL abgebrochen");
                JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.error.noWriteRights","Fehler. Bitte wähle Pfade mit Schreibrechten!"));

                System.exit(1);
                inst.dispose();
            }
            else {
                logger.info("INSTALL abgebrochen2");
                JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.abortInstallation","Fehler. Installation abgebrochen"));
                System.exit(0);
                inst.dispose();
            }
        }
        this.afterConfigIsLoaded();
        return JDUtilities.getConfiguration();
    }

    private void afterConfigIsLoaded() {
      
        
    }

    public JDController initController() {
        return new JDController();
    }

    public UIInterface initGUI(JDController controller) {
        UIInterface uiInterface = new SimpleGUI();
        controller.setUiInterface(uiInterface);
        return uiInterface;
    }

    public Vector<PluginForDecrypt> loadPluginForDecrypt() {
        Vector<PluginForDecrypt> plugins = new Vector<PluginForDecrypt>();
        try {
            JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
            Iterator iterator;
            logger.finer("Load PLugins");
            iterator = Service.providers(PluginForDecrypt.class, jdClassLoader);
            while (iterator.hasNext()) {
                PluginForDecrypt p = (PluginForDecrypt) iterator.next();
                logger.info("Load "+ p);
                plugins.add(p);
            }
            return plugins;
        }
        catch (Exception e) {
            e.printStackTrace();
            return plugins;
        }
    }

    public Vector<PluginForHost> loadPluginForHost() {
        Vector<PluginForHost> plugins = new Vector<PluginForHost>();
        try {
            JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
            Iterator iterator;
            logger.finer("Load PLugins");
            iterator = Service.providers(PluginForHost.class, jdClassLoader);
            while (iterator.hasNext()) {
                PluginForHost p = (PluginForHost) iterator.next();

                plugins.add(p);
            }
            return plugins;
        }
        catch (Exception e) {
            e.printStackTrace();
            return plugins;
        }
    }
 public void loadModules(){
     JDUtilities.getController().setUnrarModule(Unrar.getInstance());
     JDUtilities.getController().setInfoFileWriterModule(InfoFileWriter.getInstance());
     
     
     
 }
    public Vector<PluginForSearch> loadPluginForSearch() {
        Vector<PluginForSearch> plugins = new Vector<PluginForSearch>();
        try {
            JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
            Iterator iterator;
            logger.finer("Load PLugins");
            iterator = Service.providers(PluginForSearch.class, jdClassLoader);
            while (iterator.hasNext()) {
                PluginForSearch p = (PluginForSearch) iterator.next();

                plugins.add(p);
            }
            return plugins;
        }
        catch (Exception e) {
            e.printStackTrace();
            return plugins;
        }
    }

    public Vector<PluginForContainer> loadPluginForContainer() {
        Vector<PluginForContainer> plugins = new Vector<PluginForContainer>();
        try {
            JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
            Iterator iterator;
            logger.finer("Load PLugins");
            iterator = Service.providers(PluginForContainer.class, jdClassLoader);
            while (iterator.hasNext()) {
                PluginForContainer p = (PluginForContainer) iterator.next();

                plugins.add(p);
            }
            return plugins;
        }
        catch (Exception e) {
            e.printStackTrace();
            return plugins;
        }
    }

    public HashMap<String, PluginOptional> loadPluginOptional() {
        HashMap<String, PluginOptional> pluginsOptional = new HashMap<String, PluginOptional>();
        try {
            JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
            Iterator iterator;

            iterator = Service.providers(PluginOptional.class, jdClassLoader);
            while (iterator.hasNext()) {
                try {

                    PluginOptional p = (PluginOptional) iterator.next();
                    pluginsOptional.put(p.getPluginName(), p);
                    logger.info("Optionales-Plugin : " + p.getPluginName());

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return pluginsOptional;    
        }
        catch (Exception e) {
            e.printStackTrace();
            return pluginsOptional;
        }
    }

    public void loadDownloadQueue() {
        if (!JDUtilities.getController().initDownloadLinks()) {
            File links = JDUtilities.getResourceFile("links.dat");
            if (links != null && links.exists()) {
                File newFile = new File(links.getAbsolutePath() + ".bup");
                newFile.delete();
                links.renameTo(newFile);
                JDUtilities.getController().getUiInterface().showMessageDialog(JDLocale.L("sys.warning.linklist.incompatible","Linkliste inkompatibel. \r\nBackup angelegt."));
            }
        }

    }

    public void initPlugins() {
        logger.info("Lade Plugins");
        JDController controller = JDUtilities.getController();
        JDUtilities.setPluginForDecryptList(this.loadPluginForDecrypt());
        JDUtilities.setPluginForHostList(this.loadPluginForHost());
        JDUtilities.setPluginForSearchList(this.loadPluginForSearch());
        JDUtilities.setPluginForContainerList(this.loadPluginForContainer());
        try {
            JDUtilities.setPluginOptionalList(this.loadPluginOptional());
        }
        catch (Exception e1) {
        }

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
    }

    public void checkWebstartFile() {

    }

    public void doWebupdate() {

        new Thread() {
            public void run() {
                logger.finer("Init Webupdater");
                WebUpdater updater = new WebUpdater(null);
                logger.finer("Get available files");
                Vector<Vector<String>> files = updater.getAvailableFiles();
                if(files==null){
                    logger.severe("Webupdater offline");
                    return;
                }
                logger.finer("Files found: " + files);
                int org;
                logger.finer("init progressbar");
                ProgressController progress = new ProgressController(JDLocale.L("init.webupdate.progress.0_title","Webupdate"),org = files.size());
                progress.setStatusText(JDLocale.L("init.webupdate.progress.1_title","Update Check"));
                if (files != null) {

                    updater.filterAvailableUpdates(files, JDUtilities.getResourceFile("."));
                    progress.setStatus(org - files.size());
                    logger.finer("FIles to update: " + files);
                    if (files.size() > 0) {
                        logger.info("New Updates Available! " + files);
                        JDUtilities.download(JDUtilities.getResourceFile("webupdater.jar"), "http://jdownloader.ath.cx/autoUpdate2/webupdater.jar");
                        JDUtilities.download(JDUtilities.getResourceFile("changeLog.txt"), "http://www.syncom.org/projects/jdownloader/log/?format=changelog");

                        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {

                            JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                            logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                            System.exit(0);
                        }
                        else {
                            if (JDUtilities.getController().getUiInterface().showConfirmDialog(files.size() + " update(s) available. Start Webupdater now?")) {
                                JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                                logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                                System.exit(0);
                            }

                        }

                    }

                }

                progress.finalize();

            }
        }.start();
    }

    public void checkUpdate() {
        // TODO Auto-generated method stub

        if (JDUtilities.getResourceFile("webcheck.tmp").exists() && JDUtilities.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")).indexOf("(Revision" + JDUtilities.getRevision() + ")") > 0) {
            JDUtilities.getController().getUiInterface().showTextAreaDialog("Error", "Failed Update detected!", "It seems that the previous webupdate failed.\r\nPlease ensure that your java-version is equal- or above 1.5.\r\nMore infos at http://www.syncom.org/projects/jdownloader/wiki/FAQ.\r\n\r\nErrorcode: \r\n" + JDUtilities.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")));
            JDUtilities.getResourceFile("webcheck.tmp").delete();
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
        }
        else {

            Interaction.handleInteraction(Interaction.INTERACTION_APPSTART, false);
        }

        String hash = "";

        if (JDUtilities.getResourceFile("updateLog.txt").exists()) {
            hash = JDUtilities.getLocalHash(JDUtilities.getResourceFile("updateLog.txt"));
        }

        JDUtilities.getRunType();
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_HASH, "").equals(hash)) {
            logger.info("Returned from Update");
            String lastLog = JDUtilities.getLocalFile(JDUtilities.getResourceFile("updateLog.txt"));
            JDUtilities.getController().getUiInterface().showTextAreaDialog("Update!", "Changes:", lastLog);

        }
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_HASH, hash);
        JDUtilities.saveConfig();
    }

    public boolean installerWasVisible() {
        // TODO Auto-generated method stub
        return installerVisible;
    }

}
