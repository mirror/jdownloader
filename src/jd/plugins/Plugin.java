package jd.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jd.JDUtilities;
import jd.Property;
import jd.controlling.interaction.Interaction;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;

/**
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins. Alle Plugins
 * müssen von dieser Klasse abgeleitet werden.
 * 
 * Alle Plugins verfügen über einen Event Mechanismus
 * 
 * Hinweise zum Pluginsystem findet ihr hier
 * http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html
 * 
 * @author astaldo
 */
public abstract class Plugin {
    /**
     * TODO folgende methoden werden benoetigt setCaptchaAdress(); //damit setzt
     * man die Internetadresse des Captchafiles getCaptchaCode(); //damit kann
     * man sich den Captchacode als String holen getCaptchaFilePath(); //
     * getCaptchaFile();
     */
    /**
     * Puffer für Lesevorgänge
     */
    public final int           READ_BUFFER = 128 * 1024;

    /**
     * Name des Loggers
     */
    public static String       LOGGER_NAME = "java_downloader";

    /**
     * Versionsinformationen
     */
    public static final String VERSION     = "jDownloader_20070830_0";

    /**
     * Zeigt an, ob das Plugin abgebrochen werden soll
     */
    public PluginConfig        config;
protected  RequestInfo requestInfo;
    protected boolean          aborted     = false;

    /**
     * Liefert den Namen des Plugins zurück
     * 
     * @return Der Name des Plugins
     */
    public abstract String getPluginName();

    /**
     * Liefert eine einmalige ID des Plugins zurück
     * 
     * @return Plugin ID
     */
    public abstract String getPluginID();

    /**
     * Hier wird der Author des Plugins ausgelesen
     * 
     * @return Der Author des Plugins
     */
    public abstract String getCoder();

    /**
     * Liefert die Versionsbezeichnung dieses Plugins zurück
     * 
     * @return Versionsbezeichnung
     */
    public abstract String getVersion();

    /**
     * Ein regulärer Ausdruck, der anzeigt, welche Links von diesem Plugin
     * unterstützt werden
     * 
     * @return Ein regulärer Ausdruck
     * @see Pattern
     */
    public abstract Pattern getSupportedLinks();

    /**
     * Liefert den Anbieter zurück, für den dieses Plugin geschrieben wurde
     * 
     * @return Der unterstützte Anbieter
     */
    public abstract String getHost();

    /**
     * Führt einen Botcheck für den captcha file aus
     * 
     * @param file
     * @return true:istBot; false: keinBot
     */
    public abstract boolean doBotCheck(File file);

    /**
     * Diese Methode zeigt an, ob das Plugin auf Änderungen in der
     * Zwischenablage reagiert oder nicht
     * 
     * @return Wahr, wenn die Zwischenablage von diesem Plugin interpretiert
     *         werden soll
     */
    public abstract boolean isClipboardEnabled();

    /**
     * Gibt die Date zurück in die der aktuelle captcha geladne werden soll.
     * 
     * @param plugin
     * @return
     */
    public File getLocalCaptchaFile(Plugin plugin) {
        File dest = JDUtilities.getResourceFile("captchas/" + plugin.getPluginName() + "/captcha_" + (new Date().getTime()) + ".jpg");
        return dest;
    }

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit PLugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_USE_PREMIUM  = "USE_PREMIUM";

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit PLugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_PREMIUM_PASS = "PREMIUM_PASS";

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit PLugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_PREMIUM_USER = "PREMIUM_USER";

    /**
     * Führt den aktuellen Schritt aus
     * 
     * @param step
     * @param parameter
     * @return der gerade ausgeführte Schritt
     */
    public abstract PluginStep doStep(PluginStep step, Object parameter);

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    public Vector<PluginListener> pluginListener = null;

    /**
     * Hier werden alle notwendigen Schritte des Plugins hinterlegt
     */
    protected Vector<PluginStep>  steps;

    /**
     * Enthält den aktuellen Schritt des Plugins
     */
    protected PluginStep          currentStep    = null;

    /**
     * Properties zum abspeichern der einstellungen
     */
    private Property              properties;

    private String statusText;

    /**
     * Ein Logger, um Meldungen darzustellen
     */
    protected static Logger       logger         = null;

    protected Plugin() {
        pluginListener = new Vector<PluginListener>();
        steps = new Vector<PluginStep>();
        config = new PluginConfig(this);
        // Lädt die Konfigurationseinstellungen aus der Konfig
        if (this.getPluginName() == null) {
            logger.severe("ACHTUNG: die Plugin.getPluginName() Funktion muss einen Wert wiedergeben der zum init schon verfügbar ist, also einen static wert");
        }
     
        if (JDUtilities.getConfiguration().getProperty("PluginConfig_" + this.getPluginName()) != null) {
            properties = (Property) JDUtilities.getConfiguration().getProperty("PluginConfig_" + this.getPluginName());
        }
        else {
            properties = new Property();
        }
        logger.info("Load Plugin Properties: " + "PluginConfig_" + this.getPluginName() + " : " + properties);

    }

    /**
     * Zeigt, daß diese Plugin gestoppt werden soll
     */
    public void abort() {
        aborted = true;
    }

    /**
     * Initialisiert das Plugin vor dem ersten Gebrauch
     */
    public void init() {
        currentStep = null;
    }

    /**
     * Liefert die Klasse zurück, mit der Nachrichten ausgegeben werden können
     * Falls dieser Logger nicht existiert, wird ein neuer erstellt
     * 
     * @return LogKlasse
     */
    public static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(Plugin.LOGGER_NAME);
            Formatter formatter = new LogFormatter();
            logger.setUseParentHandlers(false);

            Handler console = new ConsoleHandler();
            console.setLevel(Level.ALL);
            console.setFormatter(formatter);

            logger.addHandler(console);
            logger.setLevel(Level.ALL);
        }
        return logger;
    }

    /**
     * Gibt das Konfigurationsobjekt der INstanz zurück. Die Gui kann daraus
     * Dialogelement zaubern
     * 
     * @return
     */
    public PluginConfig getConfig() {
        return config;
    }

    /**
     * Gibt ausgehend vom aktuellen step den nächsten zurück
     * 
     * @author coalado
     * @param currentStep
     * @return nächster step
     */
    public PluginStep nextStep(PluginStep currentStep) {
        if (steps == null || steps.size() == 0) return null;
        if (currentStep == null) return steps.firstElement();
        int index = steps.indexOf(currentStep) + 1;
        if (steps.size() > index) return steps.elementAt(index);
        return null;
    }

    /**
     * Gibt ausgehend von übergebenem Schritt den vorherigen zurück
     * 
     * @param currentStep
     * @return
     */
    public PluginStep previousStep(PluginStep currentStep) {
        if (steps == null || steps.size() == 0) return null;
        if (currentStep == null) return steps.firstElement();
        int index = steps.indexOf(currentStep) - 1;
        if (index >= 0) return steps.elementAt(index);
        return null;

    }

    /**
     * @author coalado Setzt den Pluginfortschritt zurück. Wird Gebraucht um
     *         einen Download nochmals zu starten, z.B. nach dem reconnect
     */
    public void resetSteps() {
        currentStep = null;
        for (int i = 0; i < steps.size(); i++) {
            steps.elementAt(i).setStatus(0);
        }
        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DATA_CHANGED, null));
    }

    /**
     * @author olimex Fügt Map als String mit Trennzeichen zusammen TODO:
     *         auslagern
     * @param map Map
     * @param delPair Trennzeichen zwischen Key und Value
     * @param delMap Trennzeichen zwischen Map-Einträgen
     * @return Key-value pairs
     */
    public static String joinMap(Map<String, String> map, String delPair, String delMap) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (first)
                first = false;
            else
                buffer.append(delMap);
            buffer.append(entry.getKey());
            buffer.append(delPair);
            buffer.append(entry.getValue());
        }
        return buffer.toString();
    }

    /**
     * Sammelt Cookies einer HTTP-Connection und fügt dieser einer Map hinzu
     * 
     * @author olimex
     * @param con Connection
     * @param cookieMap Map in der die Cookies eingefügt werden
     */
    public static HashMap<String, String> collectCookies(HttpURLConnection con) {
        Collection<String> cookieHeaders = con.getHeaderFields().get("Set-Cookie");
        HashMap<String, String> cookieMap = new HashMap<String, String>();
        if (cookieHeaders == null) return cookieMap;

        for (String header : cookieHeaders) {
            try {

                StringTokenizer st = new StringTokenizer(header, ";=");
                while (st.hasMoreTokens())
                    cookieMap.put(st.nextToken().trim(), st.nextToken().trim());

            }
            catch (NoSuchElementException e) {
                // ignore
            }
        }
        return cookieMap;

    }

    /**
     * @author coalado Gibt den kompletten Cookiestring zurück, auch wenn die
     *         Cookies über mehrere Header verteilt sind
     * @param con
     * @return cookiestring
     */
    public static String getCookieString(HttpURLConnection con) {
        return joinMap(collectCookies(con), "=", "; ");
    }

    /**
     * @author coalado
     * @return Gibt den aktuellen Schritt oder null zurück
     */
    public PluginStep getCurrentStep() {
        return currentStep;
    }

    /**
     * Hier wird geprüft, ob das Plugin diesen Text oder einen Teil davon
     * handhaben kann. Dazu wird einfach geprüft, ob ein Treffer des Patterns
     * vorhanden ist.
     * 
     * @param data der zu prüfende Text
     * @return wahr, falls ein Treffer gefunden wurde.
     */
    public synchronized boolean canHandle(String data) {
        Pattern pattern = getSupportedLinks();
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    /** 
     * Findet ein einzelnes Vorkommen und liefert den vollständigen Treffer oder
     * eine Untergruppe zurück
     * 
     * @param data Der zu durchsuchende Text
     * @param pattern Das Muster, nach dem gesucht werden soll
     * @param group Die Gruppe, die zurückgegeben werden soll. 0 ist der
     *            vollständige Treffer.
     * @return Der Treffer
     */
    public String getFirstMatch(String data, Pattern pattern, int group) {
        String hit = null;
        if(data==null)return null;
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find() && group <= matcher.groupCount()) {
                hit = matcher.group(group);
            }
        }
        return hit;
    }

    /**
     * Diese Methode findet alle Vorkommnisse des Pluginpatterns in dem Text,
     * und gibt die Treffer als Vector zurück
     * 
     * @param data Der zu durchsuchende Text
     * @param pattern Das Muster, nach dem gesucht werden soll
     * @return Alle Treffer in dem Text
     */
    public Vector<String> getMatches(String data, Pattern pattern) {
        Vector<String> hits = null;
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                hits = new Vector<String>();
                int position = 0;
                while (matcher.find(position)) {
                    hits.add(matcher.group());
                    position = matcher.start() + matcher.group().length();
                }
            }
        }
        return hits;
    }

    /**
     * Zählt, wie oft das Pattern des Plugins in dem übergebenen Text vorkommt
     * 
     * @param data Der zu durchsuchende Text
     * @param pattern Das Pattern, daß im Text gefunden werden soll
     * 
     * @return Anzahl der Treffer
     */
    protected int countOccurences(String data, Pattern pattern) {
        int position = 0;
        int occurences = 0;

        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            while (matcher.find(position)) {
                occurences++;
                position = matcher.start() + matcher.group().length();
            }
        }
        return occurences;
    }

    /**
     * Diese Funktion schneidet alle Vorkommnisse des vom Plugin unterstützten
     * Pattern aus
     * 
     * @param data Text, aus dem das Pattern ausgeschnitter werden soll
     * @return Der resultierende String
     */
    public String cutMatches(String data) {
        return data.replaceAll(getSupportedLinks().pattern(), "--CUT--");
    }

    /**
     * Hier kann man den Text zwischen zwei Suchmustern ausgeben lassen
     * Zeilenumbrueche werden dabei auch unterstuetzt
     * 
     * @param data Der zu durchsuchende Text
     * @param startPattern der Pattern, bei dem die Suche beginnt
     * @param lastPattern der Pattern, bei dem die Suche endet
     * 
     * @return der Text zwischen den gefundenen stellen oder, falls nichts
     *         gefunden wurde, der vollständige Text
     */
    public String getBetween(String data, String startPattern, String lastPattern) {
        Pattern p = Pattern.compile("(?s)" + startPattern + "(.*?)" + lastPattern, Pattern.CASE_INSENSITIVE);
        Matcher match = p.matcher(data);
        if (match.find()) return match.group(1);
        return data;
    }

    /**
     * Diese Methode sucht die vordefinierten input type="hidden" zwischen
     * startpattern und lastpattern und formatiert sie zu einem poststring z.b.
     * würde bei:
     * 
     * <input type="hidden" name="f" value="f50b0f" /> <input type="hidden"
     * name="h" value="390b4be0182b85b0" /> <input type="hidden" name="b"
     * value="9" />
     * 
     * f=f50b0f&h=390b4be0182b85b0&b=9 rauskommen
     * 
     * @param data Der zu durchsuchende Text
     * @param startPattern der Pattern, bei dem die Suche beginnt
     * @param lastPattern der Pattern, bei dem die Suche endet
     * 
     * @return ein String, der als POST Parameter genutzt werden kann und alle
     *         Parameter des Formulars enthält
     */
    public String getFormInputHidden(String data, String startPattern, String lastPattern) {
        return getFormInputHidden(getBetween(data, startPattern, lastPattern));
    }

    /**
     * Diese Methode sucht die vordefinierten input type="hidden" und formatiert
     * sie zu einem poststring z.b. würde bei:
     * 
     * <input type="hidden" name="f" value="f50b0f" /> <input type="hidden"
     * name="h" value="390b4be0182b85b0" /> <input type="hidden" name="b"
     * value="9" />
     * 
     * f=f50b0f&h=390b4be0182b85b0&b=9 ausgegeben werden
     * 
     * @param data Der zu durchsuchende Text
     * 
     * @return ein String, der als POST Parameter genutzt werden kann und alle
     *         Parameter des Formulars enthält
     */
    public String getFormInputHidden(String data) {
        return joinMap(getInputHiddenFields(data), "=", "&");
    }

    public HashMap<String, String> getInputHiddenFields(String data, String startPattern, String lastPattern) {
        return getInputHiddenFields(getBetween(data, startPattern, lastPattern));
    }

    /**
     * Gibt alle Hidden fields als hasMap zurück
     * 
     * @param data
     * @return
     */
    public HashMap<String, String> getInputHiddenFields(String data) {
        Pattern intput1 = Pattern.compile("<[ ]?input([^>]*?type=['\"]?hidden['\"]?[^>]*?)[/]?>", Pattern.CASE_INSENSITIVE);
        Pattern intput2 = Pattern.compile("name=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE);
        Pattern intput3 = Pattern.compile("value=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE);
        Pattern intput4 = Pattern.compile("name=([^\\s]*)", Pattern.CASE_INSENSITIVE);
        Pattern intput5 = Pattern.compile("value=([^\\s]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = intput1.matcher(data);
        Matcher matcher2;
        Matcher matcher3;
        Matcher matcher4;
        Matcher matcher5;

        HashMap<String, String> ret = new HashMap<String, String>();
        boolean iscompl;

        while (matcher1.find()) {
            matcher2 = intput2.matcher(matcher1.group(1) + " ");
            matcher3 = intput3.matcher(matcher1.group(1) + " ");
            matcher4 = intput4.matcher(matcher1.group(1) + " ");
            matcher5 = intput5.matcher(matcher1.group(1) + " ");
            iscompl = false;

            String key, value;
            key = value = null;
            if (matcher2.find()) {
                iscompl = true;
                key = matcher2.group(1);
            }
            else if (matcher4.find()) {
                iscompl = true;
                key = matcher4.group(1);
            }
            if (matcher3.find() && iscompl)
                value = matcher3.group(1);
            else if (matcher5.find() && iscompl)
                value = matcher5.group(1);
            else
                iscompl = false;

            ret.put(key, value);

        }
        return ret;
    }

    /**
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link Die URL, die ausgelesen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo getRequest(URL link) throws IOException {
        return getRequest(link, null, null, false);
    }

    /**
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link Der Link, an den die GET Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo getRequest(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO User-Agent als Option ins menu
        // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
        // lassen
        // so ist das Programm nicht so auffallig
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        RequestInfo requestInfo = readFromURL(httpConnection);
        requestInfo.setConnection(httpConnection);
        return requestInfo;
    }

    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) {

            httpConnection.setRequestProperty("Cookie", cookie);
        }
        // TODO User-Agent als Option ins menu
        // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
        // lassen
        // so ist das Programm nicht so auffallig
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

        String location = httpConnection.getHeaderField("Location");

        String setcookie = getCookieString(httpConnection);
        int responseCode = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
        try {
            responseCode = httpConnection.getResponseCode();
        }
        catch (IOException e) {
        }
        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        return ri;
    }

    /**
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param link Der Link, an den die POST Anfrage geschickt werden soll
     * @param parameter Die Parameter, die übergeben werden sollen
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL link, String parameter) throws IOException {
        return postRequest(link, null, null, null, parameter, false);
    }

    /**
     * 
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param link Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param requestProperties Hier können noch zusätliche Properties
     *            mitgeschickt werden
     * @param parameter Die Parameter, die übergeben werden sollen
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL link, String cookie, String referrer, HashMap<String, String> requestProperties, String parameter, boolean redirect) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        if (requestProperties != null) {
            Set<String> keys = requestProperties.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                httpConnection.setRequestProperty(key, requestProperties.get(key));
            }
        }
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

        httpConnection.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
        if (parameter != null) wr.write(parameter);
        wr.flush();

        RequestInfo requestInfo = readFromURL(httpConnection);
        wr.close();

        requestInfo.setConnection(httpConnection);
        return requestInfo;
    }

    /**
     * Gibt header- und cookieinformationen aus ohne den HTMLCode
     * herunterzuladen
     * 
     * @param link Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param parameter Die Parameter, die übergeben werden sollen
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequestWithoutHtmlCode(URL link, String cookie, String referrer, String parameter, boolean redirect) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        if (parameter != null) {
            httpConnection.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());

            wr.write(parameter);
            wr.flush();
            wr.close();
        }

        String location = httpConnection.getHeaderField("Location");

        String setcookie = getCookieString(httpConnection);
        int responseCode = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
        try {
            responseCode = httpConnection.getResponseCode();
        }
        catch (IOException e) {
        }
        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        return ri;
    }

    /**
     * Liest Daten von einer URL. LIst den encoding type und kann plaintext und gzip unterscheiden
     * 
     * @param urlInput Die URL Verbindung, von der geselen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo readFromURL(HttpURLConnection urlInput) throws IOException {
        
       // Content-Encoding: gzip
        BufferedReader rd ;
       if( urlInput.getHeaderField("Content-Encoding")!=null &&urlInput.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")){
           rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(urlInput.getInputStream())));
        }else{
            rd = new BufferedReader(new InputStreamReader(urlInput.getInputStream()));
        }
        String line;
        StringBuffer htmlCode = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            // so bleibt der html-syntax erhalten
            htmlCode.append(line + "\n");
        }
        // wenn du nur informationen ueber den header oder cookies braust benutz
        // bitte postRequestWithoutHtmlCode
        // ich hab hir mal Location gross und aus cookie Set-Cookie gemacht weil
        // der Server Set-Cookie versendet
        String location = urlInput.getHeaderField("Location");
        String cookie = getCookieString(urlInput);
        int responseCode = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
        try {
            responseCode = urlInput.getResponseCode();
        }
        catch (IOException e) {
        }
        RequestInfo requestInfo = new RequestInfo(htmlCode.toString(), location, cookie, urlInput.getHeaderFields(), responseCode);
        rd.close();
        return requestInfo;
    }

    /**
     * Speichert einen InputStream binär auf der Festplatte ab
     * TODO: Der Sleep  drückt die Geschwindigkeit deutlich
     * @param downloadLink der DownloadLink
     * @param urlConnection Wenn bereits vom Plugin eine vorkonfigurierte
     *            URLConnection vorhanden ist, wird diese hier übergeben und
     *            benutzt. Ansonsten erfolgt ein normaler GET Download von der
     *            URL, die im DownloadLink hinterlegt ist
     * @return wahr, wenn alle Daten ausgelesen und gespeichert wurden
     */
    public boolean download(DownloadLink downloadLink, URLConnection urlConnection) {
        File fileOutput = new File(downloadLink.getFileOutput());
        if (!fileOutput.getParentFile().exists()) {
            fileOutput.getParentFile().mkdirs();
        }
        ByteBuffer hdBuffer = ByteBuffer.allocateDirect(1024*1024*5);
        downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);
        long downloadedBytes = 0;
       
        long start, end, time;
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(READ_BUFFER);

            // Falls keine urlConnection übergeben wurde
            if (urlConnection == null) urlConnection = new URL(downloadLink.getUrlDownloadDecrypted()).openConnection();

            FileOutputStream fos = new FileOutputStream(fileOutput);

            // NIO Channels setzen:
            urlConnection.setReadTimeout(JDUtilities.getConfiguration().getReadTimeout());
            urlConnection.setConnectTimeout(JDUtilities.getConfiguration().getConnectTimeout());
            ReadableByteChannel source = Channels.newChannel(urlConnection.getInputStream());
      
            WritableByteChannel dest = fos.getChannel();

            // Länge aus HTTP-Header speichern:
            int contentLen = urlConnection.getContentLength();
            downloadLink.setDownloadMax(contentLen);
            
            logger.info("starting download");
            start = System.currentTimeMillis();

            // Buffer, laufende Variablen resetten:
            buffer.clear();
            // long bytesLastSpeedCheck = 0;
            // long t1 = System.currentTimeMillis();
            // long t3 = t1;
            for (int i = 0; (!aborted&&!downloadLink.isAborted()); i++) {
                // Thread kurz schlafen lassen, um zu häufiges Event-fire zu
                // verhindern:
                //coalado: nix schlafen.. ich will speed!   Die Events werden jetzt von der GUI kontrolliert

                int bytes = source.read(buffer);
                Thread.sleep(0);
                if (bytes == -1) break;

                // Buffer flippen und in File schreiben:
                buffer.flip();
                
               dest.write(buffer);
                buffer.compact();

                // Laufende Variablen updaten:
                downloadedBytes += bytes;
          
                // bytesLastSpeedCheck += bytes;
                // logger.info("load "+bytesLastSpeedCheck+" - "+bytes);
                // if (((t3=System.currentTimeMillis())-t1)>200) { // Speedcheck
                // alle 10 Runden = 1 sec
                //                      
                //                    
                // // DL-Speed in bytes/sec berechnen:
                // int speed = (int) (bytesLastSpeedCheck * 1000 / (t3 - t1));
                //                 
                //                   
                // //logger.info(bytesLastSpeedCheck+" SPEED "+speed);
                //                   
                // downloadLink.setDownloadSpeed(speed);
                // bytesLastSpeedCheck = 0;
                // t1 = t3;
                // firePluginEvent(new PluginEvent(this,
                // PluginEvent.PLUGIN_DOWNLOAD_SPEED, speed));
                //                   
                //                    
                // }
                downloadLink.addBytes(bytes);
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DOWNLOAD_BYTES, bytes));
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DATA_CHANGED, downloadLink));
                downloadLink.setDownloadCurrent(downloadedBytes);
            }

            if (contentLen != -1 && downloadedBytes != contentLen) {
                logger.info("incomplete download");
                downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_INCOMPLETE);
                return false;
            }

            end = System.currentTimeMillis();
            time = end - start;
            source.close();
            dest.close();
            fos.close();
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_FINISHED);
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, downloadLink));
            logger.info("download finished:" + fileOutput.getAbsolutePath());

            logger.info(downloadedBytes + " bytes in " + time + " ms");
            return true;
        }
        catch (FileNotFoundException e) {
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_INCOMPLETE);
            logger.severe("file not found. " + e.getLocalizedMessage());
        }
        catch (SecurityException e) {
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_INCOMPLETE);
            logger.severe("not enough rights to write the file. " + e.getLocalizedMessage());
        }
        catch (IOException e) {
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_INCOMPLETE);
            logger.severe("error occurred while writing to file. " + e.getLocalizedMessage());
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }
    
    
    /**
     * Holt den Dateinamen aus einem Content-Disposition header. wird dieser nicht gefunden, wird der dateiname aus der url ermittelt
     * @param urlConnection
     * @return
     */
    public String getFileNameFormHeader(URLConnection urlConnection){
       
        String ret= getFirstMatch(urlConnection.getHeaderField("content-disposition"), Pattern.compile("filename=['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE), 1);
           if(ret==null){
               int index=Math.max(urlConnection.getURL().getFile().lastIndexOf("/"),urlConnection.getURL().getFile().lastIndexOf("\\"));
               return urlConnection.getURL().getFile().substring(index+1);
           }
           try {
            ret=URLDecoder.decode(ret,"UTF-8");
        }
        catch (UnsupportedEncodingException e) {};
return ret;
       }
    /**
     * Diese Methode erstellt einen einzelnen String aus einer HashMap mit
     * Parametern für ein Post-Request.
     * 
     * @param parameters HashMap mit den Parametern
     * @return Codierter String
     */
    protected String createPostParameterFromHashMap(HashMap<String, String> parameters) {
        StringBuffer parameterLine = new StringBuffer();
        String parameter;
        Iterator<String> iterator = parameters.keySet().iterator();
        String key;
        while (iterator.hasNext()) {
            key = iterator.next();
            parameter = parameters.get(key);
            try {
                if (parameter != null) parameter = URLEncoder.encode(parameter, "US-ASCII");
            }
            catch (UnsupportedEncodingException e) {
            }
            parameterLine.append(key);
            parameterLine.append("=");
            parameterLine.append(parameter);
            if (iterator.hasNext()) parameterLine.append("&");
        }

        return parameterLine.toString();
    }

    // /////////////////////////////////////////////////////
    // Multicaster
    public void addPluginListener(PluginListener listener) {
        synchronized (pluginListener) {
            pluginListener.add(listener);
        }
    }

    public void removePluginListener(PluginListener listener) {
        synchronized (pluginListener) {
            pluginListener.remove(listener);
        }
    }

    public void firePluginEvent(PluginEvent pluginEvent) {
        synchronized (pluginListener) {
            Iterator<PluginListener> recIt = pluginListener.iterator();

            while (recIt.hasNext()) {
                ((PluginListener) recIt.next()).pluginEvent(pluginEvent);
            }
        }
    }

    /**
     * Gibt den md5hash einer Datei als String aus
     * 
     * @param filepath Dateiname
     * @return MD5 des Datei
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public String md5sum(String filepath) throws NoSuchAlgorithmException, FileNotFoundException {
        File f = new File(filepath);
        return md5sum(f);
    }

    public String md5sum(File file) throws NoSuchAlgorithmException, FileNotFoundException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            return output;
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        }
        finally {
            try {
                is.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
    }

    /**
     * Gibt die matches ohne Dublikate als arraylist aus
     * 
     * @param data
     * @param pattern
     * @return StringArray mit den Matches
     */
    public static String[] getUniqueMatches(String data, Pattern pattern) {
        ArrayList<String> set = new ArrayList<String>();
        Matcher m = pattern.matcher(data);
        while (m.find()) {
            if (!set.contains(m.group())) {
                set.add(m.group());
            }
        }
        return (String[]) set.toArray(new String[set.size()]);
    }

    /**
     * public static String[] getMatches(String source, String pattern) Gibt
     * alle treffer in source nach dem pattern zurück. Platzhalter ist nur !! °
     * 
     * @param source
     * @param pattern als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Alle TReffer
     */
    public static String[] getSimpleMatches(String source, String pattern) {
        // DEBUG.trace("pattern: "+STRING.getPattern(pattern));
        if (source == null || pattern == null) return null;
        Matcher rr = Pattern.compile(getPattern(pattern), Pattern.DOTALL).matcher(source);
        if (!rr.find()) {
            // Keine treffer
        }
        try {
            String[] ret = new String[rr.groupCount()];
            for (int i = 1; i <= rr.groupCount(); i++) {
                ret[i - 1] = rr.group(i);
            }
            return ret;
        }
        catch (IllegalStateException e) {

            return null;
        }
    }
/**
 * Durchsucht source mit pattern ach treffern und gibt Treffer id zurück.
 * Bei dem Pattern muss es sich um einen String Handeln der ° als Platzhalter verwendet. Alle newline Chars in source müssen mit einem °-PLatzhalter belegt werden
 * @param source
 * @param pattern
 * @param id
 * @return String Match
 */
public static String getSimpleMatch(String source, String pattern, int id){
    String[] res= getSimpleMatches( source,  pattern);
    if(res!=null&&res.length>id){
        return res[id];
    }
    return null;
}
    /**
     * public static String getPattern(String str) Gibt ein Regex pattern
     * zurück. ° dient als Platzhalter!
     * 
     * @param str
     * @return REgEx Pattern
     */
    public static String getPattern(String str) {

        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm 1234567890";
        String ret = "";
        int i;
        for (i = 0; i < str.length(); i++) {
            char letter = str.charAt(i);
            // 176 == °
            if (letter == 176) {
                ret += "(.*?)";
            }
            else if (allowed.indexOf(letter) == -1) {

                ret += "\\" + letter;
            }
            else {

                ret += letter;
            }
        }

        return ret;
    }

    /**
     * Schreibt alle treffer von pattern in source in den übergebenen vector
     * Als rückgabe erhält man einen 2D-Vector 
     * @param source
     * @param pattern als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @param container
     * @return Treffer
     */
    public static Vector<Vector<String>> getAllSimpleMatches(String source, String pattern) {
        pattern = getPattern(pattern);
        Vector<Vector<String>> ret = new Vector<Vector<String>>();

        Vector<String> entry;
        String tmp;
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source); r.find();) {
            entry = new Vector<String>();

            for (int x = 1; x <= r.groupCount(); x++) {
                tmp = r.group(x).trim();
                entry.add(JDUtilities.UTF8Decode(tmp));
                // }
            }
            ret.add(entry);

        }

        return ret;
    }
    /**
     * Gibt von allen treffer von pattern in source jeweils den id-ten Match einem vector zurück.
     * Als pattern kommt ein Simplepattern zum einsatz
     * @param source
     * @param pattern
     * @param id
     * @return Matchlist
     */
    public static Vector<String> getAllSimpleMatches(String source, String pattern, int id) {
        pattern = getPattern(pattern);
        Vector<String> ret = new Vector<String>();

     
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source); r.find();) {
          
            if( id<r.groupCount())ret.add(r.group(id).trim());          

        }

        return ret;
    }
    

    public static String getMatch(String source, String pattern, int i, int ii) {
        Vector<Vector<String>> ret = getAllSimpleMatches(source, pattern);
        if (ret.elementAt(i) != null && ret.elementAt(i).elementAt(ii) != null) {
            return ret.elementAt(i).elementAt(ii);
        }
        return null;
    }

    public static String getCaptchaCode(File file, Plugin plugin) {
        DownloadLink dummy = new DownloadLink(plugin, "", "", "", true);
        dummy.setLatestCaptchaFile(file);
        Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_CAPTCHA, dummy, 0);
        Interaction[] interacts = Interaction.getInteractions(Interaction.INTERACTION_DOWNLOAD_CAPTCHA);
        String captchaText = null;
        if (interacts.length > 0) {
            captchaText = (String) interacts[0].getProperty("captchaCode");
            if (captchaText == null) {
                // im NOtfall doch JAC nutzen
                captchaText = JDUtilities.getCaptcha(JDUtilities.getController(), plugin, file);
            }

        }
        else {
            logger.finer("KEINE captchaINteractions... nutze JAC");
            captchaText = JDUtilities.getCaptcha(JDUtilities.getController(), plugin, file);

        }
        return captchaText;

    }

    public Property getProperties() {
        return properties;
    }

    public void setProperties(Property properties) {
        this.properties = properties;
    }
/**
 * 
 * @return gibt den namen des Links an der gerade verarbeitet wird
 */
    public abstract String getLinkName() ;
    
    
    public String getStatusText() {

        return this.statusText;
    }

    public void setStatusText(String value) {
        statusText = value;
    }

}