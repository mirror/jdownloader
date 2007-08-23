package jd;

import java.awt.Toolkit;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.logging.Logger;

import jd.controlling.interaction.HTTPReconnect;
import jd.gui.GUIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.router.RouterData;

// TODO Wulfskin: Reconnect Paket
//
// TODO Konfiguration speichern (XML)
// TODO Richtiges UIInterface
// TODO Links verschlüsseln (einfach AES)


/**
 * Start der Applikation
 *
 * @author astaldo
 */
public class Main {
    public static void main(String args[]){
        Main main = new Main();
        main.go();
    }
    private void go(){
        
//  Bitte mal testen        
//        RouterData routerData = new RouterData();
//        routerData.setConnectionConnect("http://www.google.de");
//        routerData.setConnectionDisconnect("Forms/DiagADSL_1?DiagDSLDisconnect=PPPoE%20Trennung");
//        Configuration configuration = new Configuration();
//        configuration.setRouterData(routerData);
//        configuration.setRouterIP("192.168.168.72");
//        configuration.setRouterUsername("administrator");
//        configuration.setRouterPassword("geheim"); // Ja es gibt DAUs! :-)
//        JDUtilities.setConfiguration(configuration);
//        HTTPReconnect httpReconnect = new HTTPReconnect();
//        httpReconnect.interact();
        
        Logger logger = Plugin.getLogger();
        URLClassLoader cl = JDUtilities.getURLClassLoader();
        URL configURL = cl.getResource(JDUtilities.CONFIG_PATH);
        if(configURL != null){
            try {
                File fileInput = new File(configURL.toURI());
                Object obj = JDUtilities.loadObject(null, fileInput);
                if(obj instanceof Configuration){
                    JDUtilities.setConfiguration((Configuration)obj);
                }
            }
            catch (URISyntaxException e1) { e1.printStackTrace(); }
        }
        else{
            logger.warning("no configuration loaded");
            
        }
//        JDUtilities.saveObject(null, new Configuration(), JDUtilities.getJDHomeDirectory(), "jdownloader", ".config");
        JDUtilities.loadPlugins();
        loadImages();
        GUIInterface guiInterface = new SimpleGUI();
        
        Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
        while(iteratorHost.hasNext()){
            iteratorHost.next().addPluginListener(guiInterface);
        }
        Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
        while(iteratorDecrypt.hasNext()){
            iteratorDecrypt.next().addPluginListener(guiInterface);
        }
    }
    /**
     * Die Bilder werden aus der JAR Datei nachgeladen
     */
    private void loadImages(){
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        JDUtilities.addImage("add",      toolkit.getImage(cl.getResource("img/add.png")));
        JDUtilities.addImage("delete",   toolkit.getImage(cl.getResource("img/delete.png")));
        JDUtilities.addImage("down",     toolkit.getImage(cl.getResource("img/down.png")));
        JDUtilities.addImage("led_empty",toolkit.getImage(cl.getResource("img/led_empty.gif")));
        JDUtilities.addImage("led_green",toolkit.getImage(cl.getResource("img/led_green.gif")));
        JDUtilities.addImage("load",     toolkit.getImage(cl.getResource("img/load.png")));
        JDUtilities.addImage("mind",     toolkit.getImage(cl.getResource("img/mind.png")));
        JDUtilities.addImage("save",     toolkit.getImage(cl.getResource("img/save.png")));
        JDUtilities.addImage("start",    toolkit.getImage(cl.getResource("img/start.png")));
        JDUtilities.addImage("stop",     toolkit.getImage(cl.getResource("img/stop.png")));
        JDUtilities.addImage("up",       toolkit.getImage(cl.getResource("img/up.png")));
        JDUtilities.addImage("exit",     toolkit.getImage(cl.getResource("img/shutdown.png")));
        JDUtilities.addImage("log",     toolkit.getImage(cl.getResource("img/log.png")));
    }
}
