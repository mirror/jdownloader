package jd.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.JDFileFilter;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.plugins.event.PluginListener;
import jd.update.WebUpdater;
import sun.misc.Service;

/**
 * @author astaldo/coalado
 *
 */
public class JDUtilities {
    /**
     * Parametername für den Konfigpath
     */
    public static final String              CONFIG_PATH       = "jDownloader.config";

    /**
     * Titel der Applikation
     */
    public static final String              JD_VERSION        = "0.0.";

    
    public static final String              JD_REVISION  ="$Id$";
    /**
     * Versionsstring der Applikation
     */
    public static final String              JD_TITLE          = "jDownloader";

    /**
     * Ein URLClassLoader, um Dateien aus dem HomeVerzeichnis zu holen
     */
    private static URLClassLoader           urlClassLoader    = null;

    /**
     * Das JD-Home Verzeichnis. Dieses wird nur gesetzt, wenn es aus dem
     * WebStart Cookie gelesen wurde. Diese Variable kann nämlich im
     * KonfigDialog geändert werden 
     */
    private static String                   homeDirectory     = null;

    /**
     * Das ist das File Objekt, daß das HomeDirectory darstellt
     */

    private static File                     homeDirectoryFile = null;

    /**
     * Der DownloadController
     */
    private static JDController             controller        = null;

    /**
     * RessourceBundle für Texte
     */
    private static ResourceBundle           resourceBundle    = null;
 
    /**
     * Angaben über Spracheinstellungen
     */
    private static Locale                   locale            = null;

    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    private static HashMap<String, Image>   images            = new HashMap<String, Image>();

    /**
     * Der Logger für Meldungen
     */
    private static Logger                   logger            = Plugin.getLogger();

    /**
     * Damit werden die JARs rausgesucht
     */
    public static JDFileFilter              filterJar         = new JDFileFilter(null, ".jar", false);

    /**
     * Das aktuelle Verzeichnis (Laden/Speichern)
     */
    private static File                     currentDirectory;

    /**
     * Hier werden alle vorhandenen Plugins zum Dekodieren von Links gespeichert
     */
    private static Vector<PluginForDecrypt> pluginsForDecrypt = new Vector<PluginForDecrypt>();
    /**
     * Hier werden alle vorhandenen Plugins zum Suchen von Links gespeichert
     */
    private static Vector<PluginForSearch> pluginsForSearch = new Vector<PluginForSearch>();

    /**
     * Hier werden alle Plugins für die Anbieter gespeichert
     */
    private static Vector<PluginForHost>    pluginsForHost    = new Vector<PluginForHost>();

    /**
     * Die Konfiguration
     */
    private static Configuration            configuration     = new Configuration();

    /**
     * Genau wie add, aber mit den Standardwerten iPadX,iPadY=0
     * 
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente
     *            erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente
     *            erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in
     *            X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in
     *            Y-Richtung
     * @param insets Abstände der Komponente
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen
     *            Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int fill, int anchor) {
        if (cont == null) {
            logger.severe("Container ==null");
            return;
        }
        if (comp == null) {
            logger.severe("Componente ==null");
            return;
        }
        addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
    }
/**
 * parsed den JD_REVISION String auf
 * @return RevissionID
 */
   
    public static String getRevision(){
        String[] data=JD_REVISION.split(" ");
       if(data.length>2)return data[2];
       return null;
    }
    
    /**
     * parsed den JD_REVISION String auf
     * @return Letztes Änderungs datum
     */
    public static String getLastChangeDate(){
        String[] data=JD_REVISION.split(" ");
       if(data.length>3){
           String[] date=data[3].split("-");
           if(date.length!=3)return null;
           return date[2]+"."+date[1]+"."+date[0];
       }
       return null;
    }
    /**
     * parsed den JD_REVISION String auf
     * @return Letzte änderungsuhrzeit
     */
    public static String getLastChangeTime(){
        String[] data=JD_REVISION.split(" ");
        if(data.length>4){
            return data[4].substring(0,data[4].length()-1);
        
        }
        return null;
    }
    /**
     * parsed den JD_REVISION String auf
     * @return Name des programmierers der die letzten Änderungen durchgeführt hat
     */
    public static String getLastChangeAuthor(){
        String[] data=JD_REVISION.split(" ");
        if(data.length>5){
            return data[5];
        }
        return null;
    }
    /**
     * Diese Klasse fuegt eine Komponente einem Container hinzu
     * 
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente
     *            erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente
     *            erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in
     *            X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in
     *            Y-Richtung
     * @param insets Abständer der Komponente
     * @param iPadX Leerraum zwischen einer GridBagZelle und deren Inhalt
     *            (X-Richtung)
     * @param iPadY Leerraum zwischen einer GridBagZelle und deren Inhalt
     *            (Y-Richtung)
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen
     *            Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int iPadX, int iPadY, int fill, int anchor) {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = x;
        cons.gridy = y;
        cons.gridwidth = width;
        cons.gridheight = height;
        cons.weightx = weightX;
        cons.weighty = weightY;
        cons.fill = fill;
        cons.anchor = anchor;
        if (insets != null) cons.insets = insets;
        cons.ipadx = iPadX;
        cons.ipady = iPadY;
        cont.add(comp, cons);
    }

    /**
     * Liefert einen Punkt zurück, mit dem eine Komponente auf eine andere
     * zentriert werden kann
     * 
     * @param parent Die Komponente, an der ausgerichtet wird
     * @param child Die Komponente die ausgerichtet werden soll
     * @return Ein Punkt, mit dem diese Komponente mit der setLocation Methode
     *         zentriert dargestellt werden kann
     */
    public static Point getCenterOfComponent(Component parent, Component child) {
        Point center;
        if (parent == null || !parent.isShowing()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = screenSize.width;
            int height = screenSize.height;
            center = new Point(width / 2, height / 2);
        }
        else {
            center = parent.getLocationOnScreen();
            center.x += parent.getWidth() / 2;
            center.y += parent.getHeight() / 2;
        }

        // Dann Auszurichtende Komponente in die Berechnung einfließen lassen
        center.x -= child.getWidth() / 2;
        center.y -= child.getHeight() / 2;
        return center;
    }

    /**
     * Liefert eine Zeichenkette aus dem aktuellen ResourceBundle zurück
     * 
     * @param key Identifier der gewünschten Zeichenkette
     * @return Die gewünschte Zeichnenkette
     */
    public static String getResourceString(String key) {
        if (resourceBundle == null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            resourceBundle = ResourceBundle.getBundle("LanguagePack", locale);
        }
        String result = key;
        try {
            result = resourceBundle.getString(key);
        }
        catch (MissingResourceException e) {
            logger.warning("resource missing." + e.getKey());
        }
        return result;
    }

    /**
     * Liefert einer char aus dem aktuellen ResourceBundle zurück
     * 
     * @param key Identifier des gewünschten chars
     * @return der gewünschte char
     */
    public static char getResourceChar(String key) {
        char result = 0;
        String s = getResourceString(key);
        if (s != null && s.length() > 0) {
            result = s.charAt(0);
        }
        return result;
    }

    /**
     * Liefert aus der Map der geladenen Bilder ein Element zurück
     * 
     * @param imageName Name des Bildes das zurückgeliefert werden soll
     * @return Das gewünschte Bild oder null, falls es nicht gefunden werden
     *         kann
     */
    public static Image getImage(String imageName) {
        return images.get(imageName);
    }

    /**
     * Fügt ein Bild zur Map hinzu
     * 
     * @param imageName Name des Bildes, daß hinzugefügt werden soll
     * @param image Das hinzuzufügende Bild
     */
    public static void addImage(String imageName, Image image) {
        Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, null);
        images.put(imageName, image);
    }

    /**
     * Liefert das Basisverzeichnis für jD zurück.
     * 
     * @return ein File, daß das Basisverzeichnis angibt
     */
    public static File getJDHomeDirectoryFromEnvironment() {
        String envDir = System.getenv("JD_HOME");
        if (envDir == null) {
            logger.warning("environment variable JD_HOME not set");
            envDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".jd_home/";
            logger.info("JD_HOME from user.home :" + envDir);
        }
        else
            logger.info("JD_HOME from environment variable:" + envDir);

        if (envDir == null) {
            envDir = "." + System.getProperty("file.separator") + ".jd_home/";
            logger.info("JD_HOME from current directory:" + envDir);
        }

        File jdHomeDir = new File(envDir);
        if (!jdHomeDir.exists()) {
            jdHomeDir.mkdirs();
        }
        return jdHomeDir;
    }

    /**
     * Liest JD-HOME aus dem WebStart Cache. Ist ein solcher nicht vorhanden,
     * wird der Pfad aus der Umgebungsvariable genommen. Ist dieser auch nicht
     * vorhanden, wird einfach in das aktuelle Verzeichnis geschrieben
     * 
     * @return Das Homeverzeichnis
     */
    @SuppressWarnings("unchecked")
    public static File getJDHomeDirectory() {
        String homeDir = null;
        if (homeDirectoryFile != null) return homeDirectoryFile;
        try {
            if (Class.forName("javax.jnlp.ServiceManager") != null) {
                Class webStartHelper = Class.forName("jd.JDWebStartHelper");
                Method method = webStartHelper.getDeclaredMethod("getJDHomeDirectoryFromWebStartCookie", new Class[] {});
                homeDir = (String) method.invoke(webStartHelper, (Object[]) null);
            }
        }
        catch (ClassNotFoundException e) {
        }
        catch (SecurityException e) {
        }
        catch (NoSuchMethodException e) {
        }
        catch (IllegalArgumentException e) {
        }
        catch (IllegalAccessException e) {
        }
        catch (InvocationTargetException e) {
        }
        catch (Exception e) {
        }

        if (homeDir != null) {
            setHomeDirectory(homeDir);
            return homeDirectoryFile;
        }
        return getJDHomeDirectoryFromEnvironment();
    }

    /**
     * Schreibt das Home Verzeichnis in den Webstart Cache
     * 
     * @param newHomeDir Das neue JD-HOME
     */
    @SuppressWarnings("unchecked")
    public static void writeJDHomeDirectoryToWebStartCookie(String newHomeDir) {
        try {
            Class webStartHelper = Class.forName("jd.JDWebStartHelper");
            Method method = webStartHelper.getDeclaredMethod("writeJDHomeDirectoryToWebStartCookie", new Class[] { String.class });
            String homeDir = (String) method.invoke(webStartHelper, newHomeDir);
            setHomeDirectory(homeDir);

        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Liefert einen URLClassLoader zurück, um Dateien aus dem Stammverzeichnis
     * zu laden
     * 
     * @return URLClassLoader
     */
    public static URLClassLoader getURLClassLoader() {
        if (urlClassLoader == null) {
            File homeDir = getJDHomeDirectory();
            String url = null;
            try {
                // Url Encode des pfads für den Classloader
                url = urlEncode(new File((homeDir.getAbsolutePath())).toURI().toURL().toString());
                logger.info("Create Classloader: for: " + url + "  -->" + new URL(url));
                urlClassLoader = new URLClassLoader(new URL[] { new URL(url) }, null);
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return urlClassLoader;
    }

    /**
     * Diese Methode erstellt einen neuen Captchadialog und liefert den
     * eingegebenen Text zurück.
     * 
     * @param controller Der Controller
     * @param plugin Das Plugin, das dieses Captcha fordert (Der Host wird
     *            benötigt)
     * @param file
     * @return Der vom Benutzer eingegebene Text
     */
    public static String getCaptcha(JDController controller, Plugin plugin, File file) {
        if(controller==null)controller=getController();
        String host = plugin.getHost();
        
        logger.info("JAC has Method for: "+host+": "+JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host));
        if (JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host)) {

            JFrame jf = new JFrame();
            Image captchaImage = new JFrame().getToolkit().getImage(file.getAbsolutePath());
            MediaTracker mediaTracker = new MediaTracker(jf);
            mediaTracker.addImage(captchaImage, 0);
            try {
                mediaTracker.waitForID(0);
            }
            catch (InterruptedException e) {
                return null;
            }
            mediaTracker.removeImage(captchaImage);
            JAntiCaptcha jac = new JAntiCaptcha(getJACMethodsDirectory(), host);
            Captcha captcha = jac.createCaptcha(captchaImage);

            String captchaCode = jac.checkCaptcha(captcha);
            logger.info(captchaCode);
            return captchaCode;

        }
        else { 
            return controller.getCaptchaCodeFromUser(plugin, file);
        }

    }

    /**
     * Hier werden alle Plugins im aktuellen Verzeichnis geparsed (und im
     * Classpath)
     */
    @SuppressWarnings("unchecked")
    public static void loadPlugins() {
        try {
            // Alle JAR Dateien, die in diesem Verzeichnis liegen, werden dem
            // Classloader hinzugefügt.
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            File files[] = new File(JDUtilities.getJDHomeDirectory() + "/plugins").listFiles(JDUtilities.filterJar);
            if (!(classLoader instanceof URLClassLoader)) {
                URL urls[] = new URL[files.length];
                for (int i = 0; i < files.length; i++) {
                    logger.info("loaded plugins from:" + files[i]);
                    urls[i] = files[i].toURI().toURL();
                }
                classLoader = new URLClassLoader(urls);
            }
            else {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
                Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });

                addURL.setAccessible(true);
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        logger.info("loaded plugins from:" + files[i]);
                        URL jarURL = files[i].toURI().toURL();
                        addURL.invoke(urlClassLoader, new Object[] { jarURL });
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Iterator iterator;

        // Zuerst Plugins zum Dekodieren verschlüsselter Links
        iterator = Service.providers(PluginForDecrypt.class);
        while (iterator.hasNext()) {
            PluginForDecrypt p = (PluginForDecrypt) iterator.next();
            pluginsForDecrypt.add(p);
            logger.info("Decrypt-Plugin : " + p.getPluginName());
        }

        // Danach die Plugins der verschiedenen Anbieter
        iterator = Service.providers(PluginForHost.class);
        while (iterator.hasNext()) {
            PluginForHost p = (PluginForHost) iterator.next();
            pluginsForHost.add(p);
            logger.info("Host-Plugin    : " + p.getPluginName());
        }
        
        // Danach die Plugins der verschiedenen Suchengines
        iterator = Service.providers(PluginForSearch.class);
        while (iterator.hasNext()) {
//            logger.info(iterator.next().toString());
            PluginForSearch p = (PluginForSearch) iterator.next();
           pluginsForSearch.add(p);
            logger.info("Search-Plugin    : " + p.getPluginName());
        }
    }

    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public static void registerListenerPluginsForDecrypt(PluginListener listener) {
        Iterator<PluginForDecrypt> iterator = pluginsForDecrypt.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }

    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public static void registerListenerPluginsForHost(PluginListener listener) {
        Iterator<PluginForHost> iterator = pluginsForHost.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }
    
    
    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public static void registerListenerPluginsForSearch(PluginListener listener) {
        Iterator<PluginForSearch> iterator = pluginsForSearch.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }

    /**
     * Lädt ein Objekt aus einer Datei
     * 
     * @param frame Ein übergeordnetes Fenster
     * @param fileInput Falls das Objekt aus einer bekannten Datei geladen
     *            werden soll, wird hier die Datei angegeben. Falls nicht, kann
     *            der Benutzer über einen Dialog eine Datei aussuchen
     * @param asXML Soll das Objekt von einer XML Datei aus geladen werden?
     * @return Das geladene Objekt
     */
    public static Object loadObject(JFrame frame, File fileInput, boolean asXML) {
        logger.info("load file: " + fileInput + " (xml:" + asXML + ")");
        Object objectLoaded = null;
        if (fileInput == null) {
            JFileChooser fileChooserLoad = new JFileChooser();
            if (currentDirectory != null) fileChooserLoad.setCurrentDirectory(currentDirectory);
            if (fileChooserLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileInput = fileChooserLoad.getSelectedFile();
                currentDirectory = fileChooserLoad.getCurrentDirectory();
            }
        }
        if (fileInput != null) {

            String hash = getLocalHash(fileInput);
            try {

                FileInputStream fis = new FileInputStream(fileInput);
                if (asXML) {
                    XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(fis));
                    objectLoaded = xmlDecoder.readObject();
                    xmlDecoder.close();
                }
                else {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    objectLoaded = ois.readObject();
                    ois.close();
                }
                // Object15475dea4e088fe0e9445da30604acd1
                // Object80d11614908074272d6b79abe91eeca1
                logger.info("Loaded Object (" + hash + "): ");
                return objectLoaded;
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Speichert ein Objekt
     * 
     * @param frame ein Fenster
     * @param objectToSave Das zu speichernde Objekt
     * @param fileOutput Das File, in das geschrieben werden soll. Falls das
     *            File ein Verzeichnis ist, wird darunter eine Datei erstellt
     *            Falls keins angegeben wird, soll der Benutzer eine Datei
     *            auswählen
     * @param name Dateiname
     * @param extension Dateiendung (mit Punkt)
     * @param asXML Soll das Objekt in eine XML Datei gespeichert werden?
     */
    public static void saveObject(JFrame frame, Object objectToSave, File fileOutput, String name, String extension, boolean asXML) {
        String hashPre;
        if (fileOutput == null) {
            JDFileFilter fileFilter = new JDFileFilter(name, extension, true);
            JFileChooser fileChooserSave = new JFileChooser();
            fileChooserSave.setFileFilter(fileFilter);
            fileChooserSave.setSelectedFile(fileFilter.getFile());
            if (currentDirectory != null) fileChooserSave.setCurrentDirectory(currentDirectory);
            if (fileChooserSave.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileOutput = fileChooserSave.getSelectedFile();
                currentDirectory = fileChooserSave.getCurrentDirectory();
            }
        }
        if (fileOutput != null) {
            if (fileOutput.isDirectory()) {
                fileOutput = new File(fileOutput, name + extension);
                logger.info("save file: " + fileOutput + " (xml:" + asXML + ") object: " + objectToSave + " - " + extension);
            }
            hashPre = getLocalHash(fileOutput);
            if (fileOutput.exists()) fileOutput.delete();
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);
                if (asXML) {
                    XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(fos));
                    xmlEncoder.writeObject(objectToSave);
                    xmlEncoder.close();
                }
                else {
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(objectToSave);
                    oos.close();
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            String hashPost = getLocalHash(fileOutput);

            if (hashPost == null) {
                logger.severe("Schreibfehler: " + fileOutput + " Datei wurde nicht erstellt");
            }
            else if (hashPost.equals(hashPre)) {
                logger.severe("Schreibfehler: " + fileOutput + " Datei wurde nicht überschrieben");
            }
            else {
                logger.finer("Schreibvorgang: " + fileOutput + " erfolgreich: " + hashPost);
            }
        }
        else {
            logger.severe("Schreibfehler: Fileoutput: null");
        }

    }

    /**
     * Formatiert Sekunden in das zeitformat stunden:minuten:sekunden
     * @param eta toURI().toURL();
     * 
     * @return formatierte Zeit
     */
    public static String formatSeconds(int eta) {

        int hours = eta / (60 * 60);
        eta -= hours * 60 * 60;
        int minutes = eta / 60;
        int seconds = eta - minutes * 60;
        if (hours == 0) {
            return fillInteger(minutes,2,"0") + ":" + fillInteger(seconds,2,"0");
        }
        
        
        return fillInteger(hours,2,"0") + ":" + fillInteger(minutes,2,"0") + ":" + fillInteger(seconds,2,"0");
    }
    /**
     * Hängt an i solange fill vorne an bis die zechenlänge von i gleich num ist
     * @param i
     * @param num
     * @param fill
     * @return aufgefüllte Zeichenkette
     */
    public static String fillInteger(int i,int num,String fill){
        String ret=""+i;
        while(ret.length()<num)ret=fill+ret;
        return ret;
        
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
     * Liefert alle geladenen Plugins zum Suchen zurück
     * 
     * @return Plugins zum Suchen
     */
    public static Vector<PluginForSearch> getPluginsForSearch() {
        return pluginsForSearch; 
    }
    /**
     * Gibt alle Ketegorien zurück für die Suchplugins exestieren. Die Kategorien werden in den Plugins selbst als String definiert
     * @return Alle Search kategorien
     */
    public static Vector<String> getPluginsForSearchCategories() {
        Vector<String> ret= new Vector<String>();
        
        for( int i=0; i<pluginsForSearch.size();i++){
            for(int b=0; b<pluginsForSearch.get(i).getCategories().length;b++){
                if(ret.indexOf(pluginsForSearch.get(i).getCategories()[b])<0)ret.add(pluginsForSearch.get(i).getCategories()[b]);
            }
        }
       Collections.sort(ret);
       return ret;
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
     * GIbt den MD5 hash eines Strings zurück
     * @param arg
     * @return MD% hash von arg
     */
    public static String getMD5(String arg) {
        try {

            MessageDigest md = MessageDigest.getInstance("md5");
            
            byte[] digest = md.digest( arg.getBytes() );
            String ret="";
            for ( byte d : digest ){
                  ret+=Integer.toHexString( d & 0xFF);
            }
            return ret;
        
        } catch (NoSuchAlgorithmException e) {
        
        }
        return "";
    }
    /**
     * Sucht ein passendes Plugin für einen Anbieter
     * 
     * @param host Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForHost getPluginForHost(String host) {
        for (int i = 0; i < pluginsForHost.size(); i++) {
            if (pluginsForHost.get(i).getHost().equals(host)) return pluginsForHost.get(i);
        }
        return null;
    }

    /**
     * @return Configuration instanz
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Setzt die Konfigurations instanz
     * @param configuration
     */
    public static void setConfiguration(Configuration configuration) {
        JDUtilities.configuration = configuration;
    }

    /**
     * @author astaldo
     * @return homeDirectory
     */
    public static String getHomeDirectory() {
        return homeDirectory;
    }

    /**
     * Diese Funktion gibt den Pfad zum JAC-Methodenverzeichniss zurück
     * 
     * @author coalado
     * @return gibt den Pfad zu den JAC Methoden zurück
     */
    public static String getJACMethodsDirectory() {
        String sep = System.getProperty("file.separator");
        return getJDHomeDirectory() + sep + "jd" + sep + "captcha" + sep + "methods";
    }

    /**
     * Gibt ein FileOebject zu einem Resourcstring zurück
     * 
     * @author coalado
     * @param arg
     * @return File zu arg
     */
    public static File getResourceFile(String arg) {
        URLClassLoader cl = getURLClassLoader();
        if (cl == null) {
            logger.severe("Classloader ==null: ");
            return null;
        }
        URL clURL = getURLClassLoader().getResource(".");
        if (clURL == null) {
            logger.severe(". resource ==null: ");
            return null;
        }
        String fileName = clURL + arg;
        try {
            fileName = URLDecoder.decode(fileName, "UTF8");
        }
        catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }
        try {
//            logger.info("get resource: " + urlEncode(fileName));
            URI uri = new URI(urlEncode(fileName));
            if (uri == null) {
                logger.severe("null resource!: " + arg);
                return null;
            }
            return new File(uri);
        }
        catch (URISyntaxException e) {

            e.printStackTrace();
            return null;
        }
    }

    /**
     * public static String getLocalHash(File f) Gibt einen MD% Hash der file
     * zurück
     * 
     * @author coalado
     * @param f
     * @return Hashstring Md5
     */
    public static String getLocalHash(File f) {
        try {
            if (!f.exists()) return null;
            MessageDigest md;
            md = MessageDigest.getInstance("md5");
            byte[] b = new byte[1024];
            InputStream in = new FileInputStream(f);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);
            }
            byte[] digest = md.digest();
            String ret = "";
            for (int i = 0; i < digest.length; i++) {
                String tmp = Integer.toHexString(digest[i] & 0xFF);
                if (tmp.length() < 2) tmp = "0" + tmp;
                ret += tmp;
            }
            in.close();
            return ret;

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @author coalado Macht ein urlRawEncode und spart dabei die angegebenen
     *         Zeichen aus
     * @param str
     * @return str URLCodiert
     */
    public static String urlEncode(String str) {
        try {
            str = URLDecoder.decode(str, "UTF-8");

            String allowed = "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/\\:&=;";
            String ret = "";
            int i;
            for (i = 0; i < str.length(); i++) {
                char letter = str.charAt(i);
                if (allowed.indexOf(letter) >= 0) {
                    ret += letter;
                }
                else {
                    ret += "%" + Integer.toString(letter, 16);
                }
            }

            return ret;
        }
        catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }
        return str;
    }

    /**
     * 
     * "http://rapidshare.com&#x2F;&#x66;&#x69;&#x6C;&#x65;&#x73;&#x2F;&#x35;&#x34;&#x35;&#x34;&#x31;&#x34;&#x38;&#x35;&#x2F;&#x63;&#x63;&#x66;&#x32;&#x72;&#x73;&#x64;&#x66;&#x2E;&#x72;&#x61;&#x72;";
     * Wandelt alle hexkodierten zeichen in diesem Format in normalen text um
     * 
     * @param str
     * @return decoded string
     */
    public static String htmlDecode(String str) {
        // http://rs218.rapidshare.com/files/&#0052;&#x0037;&#0052;&#x0034;&#0049;&#x0032;&#0057;&#x0031;/STE_S04E04.Borderland.German.dTV.XviD-2Br0th3rs.part1.rar
       if(str==null)return null;
        String pattern = "\\&\\#x(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find();) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 16);
                str = str.replaceFirst("\\&\\#x(.*?)\\;", c + "");
            }
        }
        pattern = "\\&\\#(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find();) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 10);
                str = str.replaceFirst("\\&\\#(.*?)\\;", c + "");
            }
        }
        return HTMLEntities.unhtmlentities(str);
    }

    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(File file, String content) {

        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    logger.severe("Konnte Datei nicht löschen " + file);
                    return false;
                }
            }

            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            BufferedWriter f = new BufferedWriter(new FileWriter(file));

            f.write(content);
            f.close();

            return true;

        }
        catch (Exception e) {
            // e.printStackTrace();
            return false;

        }

    }

    /**
     * @author coalado
     * @param str
     * @return str als UTF8Decodiert
     */
    public static String UTF8Decode(String str) {
        try {
            return new String(str.getBytes(), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @author coalado
     * @param str
     * @return str als UTF8 Kodiert
     */
    public static String UTF8Encode(String str) {
        try {
            return new String(str.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Setzt das Homedirectory und erstellt es notfalls neu
     * @param homeDirectory
     */
    public static void setHomeDirectory(String homeDirectory) {
        JDUtilities.homeDirectory = homeDirectory;
        homeDirectoryFile = new File(homeDirectory);
        if (!homeDirectoryFile.exists()) homeDirectoryFile.mkdirs();
    }

    /**
     * Lädt eine url lokal herunter
     * 
     * @param file
     * @param urlString
     * @return Erfolg true/false
     */
    public static boolean download(File file, String urlString) {

   
            try {
                urlString = URLDecoder.decode(urlString, "UTF-8");
        

            URL url = new URL(urlString);
            URLConnection con = url.openConnection();
            return download(file,con);
            }
            catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
return false;
    }
    /**
     * Lädt über eine URLConnection eine datei ehrunter. Zieldatei ist file. 
     * @param file
     * @param con
     * @return Erfolg true/false
     */
    public static boolean download(File file, URLConnection con) {

        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    logger.severe("Konnte Datei nicht überschreiben " + file);
                    return false;
                }
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
           
            BufferedInputStream input = new BufferedInputStream(con.getInputStream());

            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();

            return true;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return false;

        }
        catch (Exception e) {
            e.printStackTrace();
            return false;

        }

    }
    /**
     * TODO: Serverpfad in de Config aufnehmen Gleicht das homedir mit dem
     * server ab. Der Serverpfad steht noch in WebUpdater.java
     * 
     * @author coalado
     * @return Anzahl der aktualisierten Files
     */
    public static int doWebupdate() {
        WebUpdater wu = new WebUpdater(null);
        wu.run();
        return wu.getUpdatedFiles();

    }

    /**
     * Führt einen Shell befehl aus und wartet bis dieser abgearbeitet ist
     * 
     * @param command
     * @throws IOException
     */
    public static void runCommandAndWait(String command) throws IOException {
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command, null, new File(command.split(" ")[0]).getParentFile());

            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            while ((br.readLine()) != null) {

            }
        }
        catch (Exception e) {
            logger.severe("Programmaufruf fehlgeschlagen: " + e.getMessage());

        }
    }

    /**
     * Führt einen Befehl aus, wartet bis dieser abgearbeitet wurde und gibt
     * dessen rückgabe als String zurück
     * 
     * @param command
     * @return Ausgabe des aufgerufenen befehls
     * @throws IOException
     */
    public static String runCommandWaitAndReturn(String command) throws IOException {
        String ret = "";
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command, null, new File(command.split(" ")[0]).getParentFile());

            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;

            while ((line = br.readLine()) != null) {
                ret += line;

            }
        }
        catch (Exception e) {
            logger.severe("Programmaufruf fehlgeschlagen: " + e.getMessage());
            return null;
        }
        return ret;
    }

    /**
     * Führt einen befehl aus und wartet nicht! bis dieser abgearbeitet wurde
     * 
     * @param command
     * @throws Exception 
     */
    public static void runCommand(String command) throws Exception {
        if (command == null) {
            return;
        }
        try {
            Runtime rt = Runtime.getRuntime();
            rt.exec(command, null, new File(command.split(" ")[0]).getParentFile());
        }
        catch (Exception e) {
            logger.severe("Programmaufruf fehlgeschlagen: " + e.getMessage());

        }
    }

    /**
     * Gibt den verwendeten Controller zurück
     * 
     * @return gerade verwendete controller-instanz
     */
    public static JDController getController() {
        return controller;
    }

    /**
     * Setzt den Controller
     * @param con controller
     * 
     */
    public static void setController(JDController con) {
        controller = con;
    }

    /**
     * Ersetzt die Platzhalter in einem String
     * 
     * @param command
     * @return Neuer String mit ersetzen Platzhaltern
     */
    public static String replacePlaceHolder(String command) {
        if (controller == null) return command;
        command = command.replaceAll("\\%LASTFILE", controller.getLastFinishedFile());
        command = command.replaceAll("\\%CAPTCHAIMAGE", controller.getLastCaptchaImage());
        return command;
    }
/**
 * Formatiert Byes in einen MB String [MM.MM MB]
 * @param downloadMax
 * @return MegaByte Formatierter String
 */
    public static String formatBytesToMB(long downloadMax) {
        
        return Math.round(downloadMax/(1024*10.24))/100.0+" MB";
    }

    /**
     * GIbt den Integer der sich in src befindet zurück. alle nicht integerzeichen werden ausgefiltert
     * @param src
     * @return Integer in src
     */
    public static int filterInt(String src){
        try{
            
        return Integer.parseInt(filterString(src,"1234567890"));
        }catch(NumberFormatException e){
            return 0;
        }
    }
    /**
     * Filtert alle nicht lesbaren zeichen aus str
     * @param str
     * @return
     */
    public static String filterString(String str) {

        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm;:,._-&%(){}#~+ 1234567890";
        return filterString(str,allowed);
    
    }
   
/**
 * Filtert alle zeichen aus str die in filter nicht auftauchen
 * @param str
 * @param filter
 * @return
 */
    public static String filterString(String str,String filter) {
        if(str==null ||filter==null)return "";
        String allowed = filter;
        String ret = "";
        int i;
        for (i = 0; i < str.length(); i++) {
            char letter = str.charAt(i);
        
             if (allowed.indexOf(letter) >=0) {
                ret +=  letter;
             }
        }

        return ret;
    }
    

    
    

}
