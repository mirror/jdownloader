package jd;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import sun.misc.Service;

import jd.captcha.Captcha;
import jd.captcha.JAntiCaptcha;
import jd.gui.skins.simple.CaptchaDialog;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginListener;

public class JDUtilities {
    private static ResourceBundle resourceBundle = null;
    private static Locale locale = null;
    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    private static HashMap<String, Image> images = new HashMap<String, Image>();
    /**
     * Der Logger für Meldungen
     */
    private static Logger logger = Plugin.getLogger();
    /**
     * Damit werden die JARs rausgesucht
     */
    public static FilterJAR filterJar = new FilterJAR();
    /**
     * Hier werden alle vorhandenen Plugins zum Dekodieren von Links gespeichert
     */
    private static Vector<PluginForDecrypt> pluginsForDecrypt = new Vector<PluginForDecrypt>();
    /**
     * Hier werden alle Plugins für die Anbieter gespeichert
     */
    private static Vector<PluginForHost> pluginsForHost = new Vector<PluginForHost>();

    /**
     * Genau wie add, aber mit den Standardwerten iPadX,iPadY=0
     *
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets Abstände der Komponente
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y,
            int width, int height, int weightX, int weightY, Insets insets, int fill, int anchor) {
        addToGridBag(cont,comp,x,y,width,height,weightX,weightY,insets,0,0,fill,anchor);
    }
    /**
     * Diese Klasse fuegt eine Komponente einem Container hinzu
     *
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets Abständer der Komponente
     * @param iPadX Leerraum zwischen einer GridBagZelle und deren Inhalt (X-Richtung)
     * @param iPadY Leerraum zwischen einer GridBagZelle und deren Inhalt (Y-Richtung)
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp,
            int x,       int y,
            int width,   int height,
            int weightX, int weightY,
            Insets insets,
            int iPadX,   int iPadY,
            int fill,    int anchor) {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = x;
        cons.gridy = y;
        cons.gridwidth = width;
        cons.gridheight = height;
        cons.weightx = weightX;
        cons.weighty = weightY;
        cons.fill = fill;
        cons.anchor = anchor;
        if (insets != null)
            cons.insets = insets;
        cons.ipadx = iPadX;
        cons.ipady = iPadY;
        cont.add(comp, cons);
    }
    /**
     * Liefert einen Punkt zurück, mit dem eine Komponente auf eine andere zentriert werden kann
     *
     * @param parent Die Komponente, an der ausgerichtet wird
     * @param child Die Komponente die ausgerichtet werden soll
     * @return Ein Punkt, mit dem diese Komponente mit der setLocation Methode zentriert dargestellt werden kann
     */
    public static Point getCenterOfComponent(Component parent, Component child){
        Point center;
        if (parent == null || !parent.isShowing()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width  = screenSize.width;
            int height = screenSize.height;
            center = new Point(width/2, height/2);
        }
        else{
            center = parent.getLocationOnScreen();
            center.x += parent.getWidth()/2;
            center.y += parent.getHeight()/2;
        }

        //Dann Auszurichtende Komponente in die Berechnung einfließen lassen
        center.x -= child.getWidth()/2;
        center.y -= child.getHeight()/2;
        return center;
    }
    /**
     * Liefert eine Zeichenkette aus dem aktuellen ResourceBundle zurück
     *
     * @param key Identifier der gewünschten Zeichenkette
     * @return Die gewünschte Zeichnenkette
     */
    public static String getResourceString(String key){
        if(resourceBundle== null){
            if(locale == null){
                locale = Locale.getDefault();
            }
            resourceBundle = ResourceBundle.getBundle("LanguagePack", locale);
        }
        return resourceBundle.getString(key);
    }
    /**
     * Liefert aus der Map der geladenen Bilder ein Element zurück
     *
     * @param imageName Name des Bildes das zurückgeliefert werden soll
     * @return Das gewünschte Bild oder null, falls es nicht gefunden werden kann
     */
    public static Image getImage(String imageName){
        return images.get(imageName);
    }
    /**
     * Fügt ein Bild zur Map hinzu
     *
     * @param imageName Name des Bildes, daß hinzugefügt werden soll
     * @param image Das hinzuzufügende Bild
     */
    public static void addImage(String imageName, Image image){
        images.put(imageName, image);
    }
    /**
     * Als FileFilter akzeptiert diese Klasse alle .jar Dateien
     *
     * @author astaldo
     */
    private static class FilterJAR implements FileFilter{
        public boolean accept(File f) {
            if(f.getName().endsWith(".jar"))
                return true;
            else
                return false;
        }
    }
    /**
     * Diese Methode erstellt einen neuen Captchadialog und liefert den eingegebenen Text zurück.
     *
     * @param owner Das übergeordnete Fenster
     * @param plugin Das Plugin, das dieses Captcha fordert (Der Host wird benötigt)
     * @param captchaAddress Adresse des anzuzeigenden Bildes
     * @return Der vom Benutzer eingegebene Text
     */
    public static String getCaptcha(Frame owner, Plugin plugin, String captchaAddress){
        boolean useJAC = false;
        if(useJAC){
            try {
                logger.info("captchaAddress:"+captchaAddress);
                String host = plugin.getHost();
                JAntiCaptcha jac = new JAntiCaptcha(host);
                BufferedImage captchaImage = ImageIO.read(new URL(captchaAddress));
                Captcha captcha= jac.createCaptcha(captchaImage);
                String captchaCode=jac.checkCaptcha(captcha);
                logger.info(captchaCode);
                return captchaCode;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            CaptchaDialog captchaDialog = new CaptchaDialog(owner,plugin,captchaAddress);
            owner.toFront();
            captchaDialog.setVisible(true);
            return captchaDialog.getCaptchaText();
        }
        return null;
    }
    /**
     * Hier werden alle Plugins im aktuellen Verzeichnis geparsed (und im Classpath)
     */
    public static void loadPlugins(){
        try {
            // Alle JAR Dateien, die in diesem Verzeichnis liegen, werden dem
            // Classloader hinzugefügt.
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if(classLoader != null && (classLoader instanceof URLClassLoader)){
                URLClassLoader urlClassLoader = (URLClassLoader)classLoader;
                Method         addURL         = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
                File           files[]        = new File(".").listFiles(JDUtilities.filterJar);

                addURL.setAccessible(true);
                for(int i=0;i<files.length;i++){
                    URL jarURL = files[i].toURL();
                    addURL.invoke(urlClassLoader, new Object[]{jarURL});
                }
            }
        }
        catch (Exception e) { }

        Iterator iterator;

        //Zuerst Plugins zum Dekodieren verschlüsselter Links
        iterator = Service.providers(PluginForDecrypt.class);
        while(iterator.hasNext())
        {
            PluginForDecrypt p = (PluginForDecrypt) iterator.next();
            pluginsForDecrypt.add(p);
            logger.info("Decrypt-Plugin : "+p.getPluginName());
        }

        //Danach die Plugins der verschiedenen Anbieter
        iterator = Service.providers(PluginForHost.class);
        while(iterator.hasNext())
        {
            PluginForHost p = (PluginForHost) iterator.next();
            pluginsForHost.add(p);
            logger.info("Host-Plugin    : "+p.getPluginName());
        }
    }
    
    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public void registerListenerPluginsForDecrypt(PluginListener listener){
        Iterator<PluginForDecrypt> iterator = pluginsForDecrypt.iterator();
        while(iterator.hasNext()){
            iterator.next().addPluginListener(listener);        }
    }
    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public void registerListenerPluginsForHost(PluginListener listener){
        Iterator<PluginForHost> iterator = pluginsForHost.iterator();
        while(iterator.hasNext()){
            iterator.next().addPluginListener(listener);        }
    }
    /**
     * Liefert alle geladenen Plugins zum Entschlüsseln zurück
     * 
     * @return Plugins zum Entschlüsseln
     */
    public static Vector<PluginForDecrypt> getPluginsForDecrypt() {
        return pluginsForDecrypt;
    }
    /**
     * Liefert alle Plugins zum Downloaden von einem Anbieter zurück
     * 
     * @return Plugins zum Downloaden von einem Anbieter 
     */
    public static Vector<PluginForHost> getPluginsForHost() {
        return pluginsForHost;
    }
    /**
     * Sucht ein passendes Plugin für einen Anbieter
     *
     * @param host Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForHost getPluginForHost(String host){
        for(int i=0;i<pluginsForHost.size();i++){
            if(pluginsForHost.get(i).getHost().equals(host))
                return pluginsForHost.get(i);
        }
        return null;
    }
}
