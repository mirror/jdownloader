package jd;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.controlling.JDController;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginListener;
import jd.update.WebUpdater;
import sun.misc.Service;

public class JDUtilities {
    public static final String              CONFIG_PATH       = "jDownloader.config";

    /**
     * Titel der Applikation
     */
    public static final String              JD_TITLE          = "jDownloader 0.0.1";

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
            URL url = null;
            try {
                url = homeDir.toURL();
                urlClassLoader = new URLClassLoader(new URL[] { url }, null);
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
        String host = plugin.getHost();
        if (JDUtilities.getConfiguration().useJAC() && JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host)) {

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
                    urls[i] = files[i].toURL();
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
                        URL jarURL = files[i].toURL();
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
    }

    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public void registerListenerPluginsForDecrypt(PluginListener listener) {
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
    public void registerListenerPluginsForHost(PluginListener listener) {
        Iterator<PluginForHost> iterator = pluginsForHost.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }

    public static File chooseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new JDFileFilter(null, null, true));
        fileChooser.showOpenDialog(null);
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null || selectedFile.isDirectory())
            return selectedFile;
        else
            return selectedFile.getParentFile();
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
            }
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
        }
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
    public static PluginForHost getPluginForHost(String host) {
        for (int i = 0; i < pluginsForHost.size(); i++) {
            if (pluginsForHost.get(i).getHost().equals(host)) return pluginsForHost.get(i);
        }
        return null;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static void setConfiguration(Configuration configuration) {
        JDUtilities.configuration = configuration;
    }

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
        String fileName = getURLClassLoader().getResource(".") + arg;
        try {
            fileName = URLDecoder.decode(fileName, "UTF8");
        }
        catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }
        try {
            return new File(new URI(urlEncode(fileName)));
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
        return "";
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

            String allowed = "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/:";
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
            urlString = URLDecoder.decode(urlString, "UTF-8");

            URL url = new URL(urlString);
            URLConnection con = url.openConnection();
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

        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(command, null, new File(command.split(" ")[0]).getParentFile());

        BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        while ((br.readLine()) != null) {

        }

    }

    /**
     * Führt einen Befehl aus, wartet bis dieser abgearbeitet wurde und gibt
     * dessen rückgabe als String zurück
     * 
     * @param command
     * @return
     * @throws IOException
     */
    public static String runCommandWaitAndReturn(String command) throws IOException {
        String ret = "";

        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(command, null, new File(command.split(" ")[0]).getParentFile());

        BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line;

        while ((line = br.readLine()) != null) {
            ret += line;

        }

        return ret;
    }

    /**
     * Führt einen befehl aus und wartet nicht! bis dieser abgearbeitet wurde
     * 
     * @param command
     * @throws IOException
     */
    public static void runCommand(String command) throws IOException {
        if (command == null) {
            return;
        }

        Runtime rt = Runtime.getRuntime();
        rt.exec(command, null, new File(command.split(" ")[0]).getParentFile());

    }

    /**
     * Gibt den verwendeten Controller zurück
     * 
     * @return
     */
    public static JDController getController() {
        return controller;
    }

    /**
     * Setzt den Controller
     * 
     * @param controller
     */
    public static void setController(JDController con) {
        controller = con;
    }

    /**
     * Ersetzt die Platzhalter in einem String
     * 
     * @param command
     * @return
     */
    public static String replacePlaceHolder(String command) {
        if (controller == null) return command;
        command = command.replaceAll("\\%LASTFILE", controller.getLastFinishedFile());
        command = command.replaceAll("\\%CAPTCHAIMAGE", controller.getLastCaptchaImage());
        return command;
    }

}
