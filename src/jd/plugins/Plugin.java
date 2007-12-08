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
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.interaction.Interaction;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

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
    public final int              READ_BUFFER     = 128 * 1024;

    protected static final String END_OF_LINK     = "[^\"]*";

    /**
     * Der Defualt Accept-language-header. Achtung nicht Ändern. Wird dieser
     * Header geändert müssen die Regexes der Plugins angepasst werden
     */
    public static final String    ACCEPT_LANGUAGE = "de, en-gb;q=0.9, en;q=0.8";

    /**
     * Versionsinformationen
     */
    public static final String    VERSION         = "jDownloader_20070830_0";

    /**
     * Zeigt an, ob das Plugin abgebrochen werden soll
     */
    public ConfigContainer        config;

    protected RequestInfo         requestInfo;

    protected boolean             aborted         = false;

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
     * Erstellt ein SupportPattern. dabei gibt es 2 Platzhalter: [*]= Dieser
     * Platzhalter ist optional. hier KANN ein beliebiges zeichen doer keins
     * stehen; [+] Hier muss mindestens ein beliebiges zeichen stehen Da die
     * links in einer liste "link1"\r\n"link2"\"\n... untersucht werden kann
     * hier eineinfaches pattern verwendet werden. UNd es werden trotzdem
     * zuverlässig alle treffer gefunden.
     * 
     * @param patternString
     * @return Gibt ein patternzurück mit dem links gesucht und überprüft werden
     *         können
     */
    public static Pattern getSupportPattern(String patternString) {
        patternString = patternString.replaceAll("\\[\\*\\]", ".*");
        patternString = patternString.replaceAll("\\[\\+\\]", ".+");
        if (patternString.endsWith(".*") || patternString.endsWith(".+")) {
            patternString = patternString.substring(0, patternString.length() - 2) + "[^\"]" + patternString.substring(patternString.length() - 1);
        }
        // patternString="\""+patternString+"\"";
        // logger.info(patternString);
        return Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    }

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

    protected File getLocalCaptchaFile(Plugin plugin) {
        return getLocalCaptchaFile(plugin, ".jpg");
    }

    /**
     * Gibt die Date zurück in die der aktuelle captcha geladne werden soll.
     * 
     * @param plugin
     * @return Gibt einen Pfadzurück der für die nächste Captchadatei reserviert
     *         ist
     */
    protected File getLocalCaptchaFile(Plugin plugin, String extension) {
        if (extension == null) extension = ".jpg";
        Calendar calendar = Calendar.getInstance();
        String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS", calendar);
        // File dest = JDUtilities.getResourceFile("captchas/" +
        // plugin.getPluginName() + "/captcha_" + (new Date().getTime()) +
        // ".jpg");
        File dest = JDUtilities.getResourceFile("captchas/" + plugin.getPluginName() + "/" + date + extension);
        return dest;
    }

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit Plugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_USE_PREMIUM  = "USE_PREMIUM";

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit Plugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_PREMIUM_PASS = "PREMIUM_PASS";

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit Plugins.
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

    private String                statusText;

    private long                  initTime;

    /**
     * Ein Logger, um Meldungen darzustellen
     */
    public static Logger          logger         = JDUtilities.getLogger();

    protected Plugin() {
        pluginListener = new Vector<PluginListener>();
        this.initTime = System.currentTimeMillis();
        steps = new Vector<PluginStep>();
        config = new ConfigContainer(this);
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
        // logger.info("Load Plugin Properties: " + "PluginConfig_" +
        // this.getPluginName() + " : " + properties);
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
     * Gibt das Konfigurationsobjekt der INstanz zurück. Die Gui kann daraus
     * Dialogelement zaubern
     * 
     * @return gibt die aktuelle Configuration INstanz zurück
     */
    public ConfigContainer getConfig() {
        return config;
    }

    /**
     * Gibt ausgehend vom aktuellen step den nächsten zurück
     * 
     * @param currentStep Der aktuelle Schritt
     * @return nächster step
     */
    public PluginStep nextStep(PluginStep currentStep) {
        if (steps == null || steps.size() == 0) return null;
        if (currentStep == null) {
            currentStep = steps.firstElement();
            return steps.firstElement();
        }
        int index = steps.indexOf(currentStep) + 1;
        if (steps.size() > index) {
            currentStep = steps.elementAt(index);
            return steps.elementAt(index);
        }
        currentStep = null;
        return null;
    }

    /**
     * Gibt ausgehend von übergebenem Schritt den vorherigen zurück
     * 
     * @param currentStep
     * @return Gibt den vorherigen step relativ zu currentstep zurück
     */
    public PluginStep previousStep(PluginStep currentStep) {
        if (steps == null || steps.size() == 0) return null;
        if (currentStep == null) {
            currentStep = steps.firstElement();
            return steps.firstElement();
        }
        int index = steps.indexOf(currentStep) - 1;
        if (index >= 0) {
            currentStep = steps.elementAt(index);
            return steps.elementAt(index);
        }
        currentStep = null;
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
     * @return HashMap mit allen cookies
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
        if (data == null) return false;
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
        if (data == null) return null;
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

    /**
     * Ermittelt alle hidden input felder in einem HTML Text und gibt die hidden
     * variables als hashmap zurück es wird dabei nur der text zwischen start
     * dun endpattern ausgewertet
     * 
     * @param data
     * @param startPattern
     * @param lastPattern
     * @return hashmap mit hidden input variablen zwischen startPattern und
     *         endPattern
     */
    public HashMap<String, String> getInputHiddenFields(String data, String startPattern, String lastPattern) {
        return getInputHiddenFields(getBetween(data, startPattern, lastPattern));
    }

    /**
     * Gibt alle Hidden fields als hasMap zurück
     * 
     * @param data
     * @return hasmap mit allen hidden fields variablen
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

    private static int getReadTimeoutFromConfiguration() {
        return (Integer) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 10000);
    }

    private static int getConnectTimeoutFromConfiguration() {
        return (Integer) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 10000);
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
        // logger.finer("get: "+link+"(cookie: "+cookie+")");
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setReadTimeout(getConnectTimeoutFromConfiguration());
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
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        RequestInfo requestInfo = readFromURL(httpConnection);
        requestInfo.setConnection(httpConnection);
        return requestInfo;
    }

    /**
     * Führt einen getrequest durch. Gibt die headerinfos zurück, lädt aber die
     * datei noch komplett
     * 
     * @param link
     * @param cookie
     * @param referrer
     * @param redirect
     * @return requestinfos mit headerfields. HTML text wird nicht!! geladen
     * @throws IOException
     */
    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        // logger.finer("get: "+link);
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setReadTimeout(getConnectTimeoutFromConfiguration());
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
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        String location = httpConnection.getHeaderField("Location");
        String setcookie = getCookieString(httpConnection);
        int responseCode = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
        try {
            responseCode = httpConnection.getResponseCode();
        }
        catch (IOException e) {
        }
        httpConnection.connect();
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
     * @param string Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param requestProperties Hier können noch zusätliche Properties
     *            mitgeschickt werden
     * @param parameter Die Parameter, die übergeben werden sollen
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL string, String cookie, String referrer, HashMap<String, String> requestProperties, String parameter, boolean redirect) throws IOException {
        // logger.finer("post: "+link+"(cookie:"+cookie+" parameter:
        // "+parameter+")");
        HttpURLConnection httpConnection = (HttpURLConnection) string.openConnection();
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setReadTimeout(getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + string.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        if (requestProperties != null) {
            Set<String> keys = requestProperties.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                httpConnection.setRequestProperty(key, requestProperties.get(key));
            }
        }
        if(parameter!=null){
        parameter=parameter.trim();
        httpConnection.setRequestProperty("Content-Length", parameter.length()+"");
        }
        httpConnection.setDoOutput(true);
        httpConnection.connect();
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
        // logger.finer("post: "+link);
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setReadTimeout(getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        
        if (parameter != null) {
            if(parameter==null)parameter="";
            parameter=parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length()+"");
            httpConnection.setDoOutput(true);
            httpConnection.connect();
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
        httpConnection.connect();
        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        return ri;
    }

    /**
     * Liest Daten von einer URL. LIst den encoding type und kann plaintext und
     * gzip unterscheiden
     * 
     * @param urlInput Die URL Verbindung, von der geselen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo readFromURL(HttpURLConnection urlInput) throws IOException {
        // Content-Encoding: gzip
        BufferedReader rd;
        if (urlInput.getHeaderField("Content-Encoding") != null && urlInput.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(urlInput.getInputStream())));
        }
        else {
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
        responseCode = urlInput.getResponseCode();
        RequestInfo requestInfo = new RequestInfo(htmlCode.toString(), location, cookie, urlInput.getHeaderFields(), responseCode);
        rd.close();
        return requestInfo;
    }

    /**
     * Speichert einen InputStream binär auf der Festplatte ab TODO: Der Sleep
     * drückt die Geschwindigkeit deutlich
     * 
     * @param downloadLink der DownloadLink
     * @param urlConnection Wenn bereits vom Plugin eine vorkonfigurierte
     *            URLConnection vorhanden ist, wird diese hier übergeben und
     *            benutzt. Ansonsten erfolgt ein normaler GET Download von der
     *            URL, die im DownloadLink hinterlegt ist
     * @return wahr, wenn alle Daten ausgelesen und gespeichert wurden
     */
    public boolean download(DownloadLink downloadLink, URLConnection urlConnection) {

        return download(downloadLink, urlConnection, -1);

    }

    public boolean download(DownloadLink downloadLink, URLConnection urlConnection, int bytesToLoad) {
        File fileOutput = new File(downloadLink.getFileOutput() + ".jdd");
        if (fileOutput == null || fileOutput.getParentFile() == null) return false;
        if (!fileOutput.getParentFile().exists()) {
            fileOutput.getParentFile().mkdirs();
        }
        downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);
        long downloadedBytes = 0;
        long start, end, time;
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(READ_BUFFER);
            // Falls keine urlConnection übergeben wurde
            if (urlConnection == null) urlConnection = new URL(downloadLink.getUrlDownloadDecrypted()).openConnection();
            FileOutputStream fos = new FileOutputStream(fileOutput);
            // NIO Channels setzen:
            urlConnection.setReadTimeout(getReadTimeoutFromConfiguration());
            urlConnection.setReadTimeout(getConnectTimeoutFromConfiguration());
            ReadableByteChannel source = Channels.newChannel(urlConnection.getInputStream());
            WritableByteChannel dest = fos.getChannel();
            // Länge aus HTTP-Header speichern:
            int contentLen = urlConnection.getContentLength();
            if (bytesToLoad > 0) {
                contentLen = bytesToLoad;
                logger.info("Load only the first " + bytesToLoad + " kb");
            }
            downloadLink.setDownloadMax(contentLen);
            logger.info("starting download");
            start = System.currentTimeMillis();
            // Buffer, laufende Variablen resetten:
            buffer.clear();
            // long bytesLastSpeedCheck = 0;
            // long t1 = System.currentTimeMillis();
            // long t3 = t1;
            for (int i = 0; (!aborted && !downloadLink.isAborted()); i++) {
                // Thread kurz schlafen lassen, um zu häufiges Event-fire zu
                // verhindern:
                // coalado: nix schlafen.. ich will speed! Die Events werden
                // jetzt von der GUI kontrolliert
                int bytes = source.read(buffer);
                Thread.sleep(0);
                if (bytes == -1) break;
                // Buffer flippen und in File schreiben:
                buffer.flip();
                dest.write(buffer);
                buffer.compact();
                // Laufende Variablen updaten:
                downloadedBytes += bytes;
                downloadLink.addBytes(bytes);
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DOWNLOAD_BYTES, bytes));
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DATA_CHANGED, downloadLink));
                downloadLink.setDownloadCurrent(downloadedBytes);

                if (bytesToLoad > 0 && downloadedBytes >= bytesToLoad) break;
            }
            if (bytesToLoad <= 0 && contentLen != -1 && downloadedBytes < contentLen) {
                logger.info(aborted + " - " + downloadLink.isAborted() + " incomplete download: bytes loaded: " + downloadedBytes + "/" + contentLen);
                downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_INCOMPLETE);
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, downloadLink));
                return false;
            }
            end = System.currentTimeMillis();
            time = end - start;
            source.close();
            dest.close();
            fos.close();
            if (new File(downloadLink.getFileOutput()).exists()) {
                new File(downloadLink.getFileOutput()).delete();
            }
            if (!fileOutput.renameTo(new File(downloadLink.getFileOutput()))) {
                logger.severe("Could not rename file to " + downloadLink.getFileOutput());
                downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_INCOMPLETE);
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, downloadLink));
                return false;
            }
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_FINISHED);
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, downloadLink));
            logger.info("download finished:" + fileOutput.getAbsolutePath());
            logger.info(downloadedBytes + " bytes in " + time + " ms");
            return true;
        }
        catch (FileNotFoundException e) {

            logger.severe("file not found. " + e.getLocalizedMessage());
        }
        catch (SecurityException e) {

            logger.severe("not enough rights to write the file. " + e.getLocalizedMessage());
        }
        catch (IOException e) {

            logger.severe("error occurred while writing to file. " + e.getLocalizedMessage());
        }
        catch (InterruptedException e) {
            logger.severe("interrupted. " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
          
            e.printStackTrace();
        }
        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, downloadLink));
        downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_INCOMPLETE);
        return false;
    }

    /**
     * Holt den Dateinamen aus einem Content-Disposition header. wird dieser
     * nicht gefunden, wird der dateiname aus der url ermittelt
     * 
     * @param urlConnection
     * @return Filename aus dem header (content disposition) extrahiert
     */
    public String getFileNameFormHeader(URLConnection urlConnection) {
        // old: String ret =
        // getFirstMatch(urlConnection.getHeaderField("content-disposition"),
        // Pattern.compile("filename=['\"](.*?)['\"]",
        // Pattern.CASE_INSENSITIVE), 1);
        // logger.info("header dispo:
        // "+urlConnection.getHeaderField("content-disposition"));
        String ret;
        if (urlConnection.getHeaderField("content-disposition") == null || urlConnection.getHeaderField("content-disposition").indexOf("filename=") < 0) {

            int index = Math.max(urlConnection.getURL().getFile().lastIndexOf("/"), urlConnection.getURL().getFile().lastIndexOf("\\"));
            return urlConnection.getURL().getFile().substring(index + 1);
        }

        String cd = urlConnection.getHeaderField("content-disposition").toLowerCase();
        ret = cd.substring(cd.indexOf("filename=") + 9);
        while (ret.startsWith("\"") || ret.startsWith("'"))
            ret = ret.substring(1);
        while (ret.endsWith("\"") || ret.endsWith("'"))
            ret = ret.substring(0, ret.length() - 1);
        try {

            ret = URLDecoder.decode(ret, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
        }
        ;
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
        if (listener == null) return;
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
                // logger.info("OOO"+recIt.next());
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

    /**
     * Gibt den MD5 hash von file zurück
     * 
     * @param file
     * @return MD5 Hash string
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     */
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
     * Bei dem Pattern muss es sich um einen String Handeln der ° als
     * Platzhalter verwendet. Alle newline Chars in source müssen mit einem
     * °-PLatzhalter belegt werden
     * 
     * @param source
     * @param pattern
     * @param id
     * @return String Match
     */
    public static String getSimpleMatch(String source, String pattern, int id) {
        String[] res = getSimpleMatches(source, pattern);
        if (res != null && res.length > id) {
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
     * Schreibt alle Treffer von pattern in source in den übergebenen Vector.
     * Als Rückgabe erhält man einen 2D-Vector
     * 
     * @param source Quelltext
     * @param pattern als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Treffer
     */
    public static Vector<Vector<String>> getAllSimpleMatches(String source, String pattern) {
        return getAllSimpleMatches(source, Pattern.compile(getPattern(pattern), Pattern.DOTALL));
    }

    /**
     * Schreibt alle treffer von pattern in source in den übergebenen vector Als
     * Rückgabe erhält man einen 2D-Vector
     * 
     * @param source Quelltext
     * @param pattern Ein RegEx Pattern
     * @return Treffer
     */
    public static Vector<Vector<String>> getAllSimpleMatches(String source, Pattern pattern) {
        Vector<Vector<String>> ret = new Vector<Vector<String>>();
        Vector<String> entry;
        String tmp;
        for (Matcher r = pattern.matcher(source); r.find();) {
            entry = new Vector<String>();
            for (int x = 1; x <= r.groupCount(); x++) {
                tmp = r.group(x).trim();
                entry.add(JDUtilities.UTF8Decode(tmp));
            }
            ret.add(entry);
        }
        return ret;
    }

    /**
     * Gibt von allen treffer von pattern in source jeweils den id-ten Match
     * einem vector zurück. Als pattern kommt ein Simplepattern zum einsatz
     * 
     * @param source
     * @param pattern
     * @param id
     * @return Matchlist
     */
    public static Vector<String> getAllSimpleMatches(String source, String pattern, int id) {
        pattern = getPattern(pattern);
        Vector<String> ret = new Vector<String>();
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source); r.find();) {
            if (id <= r.groupCount()) ret.add(r.group(id).trim());
        }
        return ret;
    }

    public static Vector<String> getAllSimpleMatches(String source, Pattern pattern, int id) {

        Vector<String> ret = new Vector<String>();
        for (Matcher r = pattern.matcher(source); r.find();) {
            if (id <= r.groupCount()) ret.add(r.group(id).trim());
        }
        return ret;
    }

    /**
     * Gibt über die simplepattern alle den x/y ten treffer aus dem 2D-matches
     * array zurück
     * 
     * @param source
     * @param pattern
     * @param x
     * @param y
     * @return treffer an der stelle x/y im 2d treffer array
     */
    public static String getSimpleMatch(String source, String pattern, int x, int y) {
        Vector<Vector<String>> ret = getAllSimpleMatches(source, pattern);
        if (ret.elementAt(x) != null && ret.elementAt(x).elementAt(y) != null) {
            return ret.elementAt(x).elementAt(y);
        }
        return null;
    }

    /**
     * verwendet die erste Acaptcha Interaction um den captcha auszuwerten
     * 
     * @param file
     * @param plugin
     * @return captchacode
     */
    public static String getCaptchaCode(File file, Plugin plugin) {
        DownloadLink dummy = new DownloadLink(plugin, "", "", "", true);
        dummy.setLatestCaptchaFile(file);
        Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_CAPTCHA, dummy, 0);
        Interaction[] interacts = Interaction.getInteractions(Interaction.INTERACTION_DOWNLOAD_CAPTCHA);
        String captchaText = null;
        if (interacts.length > 0) {
            captchaText = (String) interacts[0].getProperty("captchaCode");
            if (captchaText == null) {
                // im Notfall doch JAC nutzen
                captchaText = JDUtilities.getCaptcha(JDUtilities.getController(), plugin, file);
            }
        }
        else {
            logger.finer("KEINE captchaInteractions... nutze JAC");
            captchaText = JDUtilities.getCaptcha(JDUtilities.getController(), plugin, file);
        }
        return captchaText;
    }

    /**
     * gibt das interne properties objekt zurück indem die Plugineinstellungen
     * gespeichert werden
     * 
     * @return internes property objekt
     */
    public Property getProperties() {
        return properties;
    }

    /**
     * Setzt das interne Property Objekt
     * 
     * @param properties
     */
    public void setProperties(Property properties) {
        this.properties = properties;
    }

    /**
     * 
     * @return gibt den namen des Links an der gerade verarbeitet wird
     */
    public abstract String getLinkName();

    /**
     * Gibt den Statustext des Plugins zurück. kann von der GUI aufgerufen
     * werden
     * 
     * @return Statustext
     */
    public String getStatusText() {
        if (this.statusText == null) this.statusText = "";
        return this.statusText;
    }

    /**
     * Setzte den Statustext des Plugins.
     * 
     * @param value
     */
    public void setStatusText(String value) {
        statusText = value;
    }

    /**
     * Sucht alle Links heraus
     * 
     * @param data ist der Quelltext einer Html-Datei
     * @param url der Link von dem der Quelltext stammt (um die base automatisch
     *            zu setzen)
     * @return Linkliste aus data extrahiert
     */
    /*
     * 
     * public static void testGetHttpLinks() throws IOException { String input =
     * ""; String thisLine; BufferedReader br = new BufferedReader(new
     * FileReader("index.html")); while ((thisLine = br.readLine()) != null)
     * input += thisLine + "\n"; String[] dd = getHttpLinks(input,
     * "http://www.google.de/"); for (int i = 0; i < dd.length; i++)
     * System.out.println(dd[i]); }
     */
    public static String[] getHttpLinks(String data, String url) {
        String[] patternStr = { "(?s)<[ ]?base[^>]*?href=['\"]([^>]*?)['\"]", "(?s)<[ ]?base[^>]*?href=([^'\"][^\\s]*)", "(?s)<[ ]?a[^>]*?href=['\"]([^>]*?)['\"]", "(?s)<[ ]?a[^>]*?href=([^'\"][^\\s]*)", "(?s)<[ ]?form[^>]*?action=['\"]([^>]*?)['\"]", "(?s)<[ ]?form[^>]*?action=([^'\"][^\\s]*)", "www[^\\s>'\"\\)]*", "http://[^\\s>'\"\\)]*" };
        url = url == null ? "" : url;
        Matcher m;
        String link;
        Pattern[] pattern = new Pattern[patternStr.length];
        for (int i = 0; i < patternStr.length; i++) {
            pattern[i] = Pattern.compile(patternStr[i], Pattern.CASE_INSENSITIVE);
        }
        String basename = "";
        String host = "";
        ArrayList<String> set = new ArrayList<String>();
        for (int i = 0; i < 2; i++) {
            m = pattern[i].matcher(data);
            if (m.find()) {
                url = JDUtilities.htmlDecode(m.group(1));
                break;
            }
        }
        if (url != null) {
            url = url.replace("http://", "");
            int dot = url.lastIndexOf('/');
            if (dot != -1)
                basename = url.substring(0, dot + 1);
            else
                basename = "http://" + url + "/";
            dot = url.indexOf('/');
            if (dot != -1)
                host = "http://" + url.substring(0, dot);
            else
                host = "http://" + url;
            url = "http://" + url;
        }
        else
            url = "";
        for (int i = 2; i < 6; i++) {
            m = pattern[i].matcher(data);
            while (m.find()) {
                link = JDUtilities.htmlDecode(m.group(1));
                link = link.replaceAll("http://.*http://", "http://");
                if ((link.length() > 6) && (link.substring(0, 7).equals("http://")))
                    ;
                else if (link.length() > 0) {
                    if (link.length() > 2 && link.substring(0, 3).equals("www")) {
                        link = "http://" + link;
                    }
                    if (link.charAt(0) == '/') {
                        link = host + link;
                    }
                    else if (link.charAt(0) == '#') {
                        link = url + link;
                    }
                    else {
                        link = basename + link;
                    }
                }
                if (!set.contains(link)) {
                    set.add(link);
                }
            }
        }
        data = data.replaceAll("(?s)<.*?>", "");
        m = pattern[6].matcher(data);
        while (m.find()) {
            link = "http://" + m.group();
            link = JDUtilities.htmlDecode(link.replaceAll("http://.*http://", "http://"));
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        m = pattern[7].matcher(data);
        while (m.find()) {
            link = m.group();
            link = JDUtilities.htmlDecode(link.replaceAll("http://.*http://", "http://"));
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        return (String[]) set.toArray(new String[set.size()]);
    }

    /**
     * Gibt alle links die in data gefunden wurden als Stringliste zurück
     * 
     * @param data
     * @return STringliste
     */
    public static String getHttpLinkList(String data) {
        String[] links = getHttpLinks(data, "%HOST%");
        String ret = "";
        for (int i = 0; i < links.length; i++) {
            ret += "\"" + links[i] + "\"\r\n";
        }
        return ret;
    }

    public String getInitID() {
        return this.initTime + "<ID";
    }

    /**
     * Gibt einen Cookie mit Hilfe der cookies.txt von Mozilla bzw. FireFox aus
     * 
     * @param link
     * @param cookiefile
     * @return
     */
    public static String parseMozillaCookie(URL link, File cookiefile) {
        if (cookiefile.isFile()) {
            try {
                Pattern cookiePattern = Pattern.compile(".*?[\\s]+(TRUE|FALSE)[\\s/]+(TRUE|FALSE)[\\s]+[0-9]{10}[\\s]+(.*?)[\\s]+(.*)", Pattern.CASE_INSENSITIVE);
                HashMap<String, String> inp = new HashMap<String, String>();
                String thisLine;
                FileInputStream fin = new FileInputStream(cookiefile);
                BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
                String hostname = link.getHost();
                String hostname2 = hostname + ".*";
                if (hostname.matches(".*?\\..*?\\..*?")) hostname = hostname.replaceFirst(".*?\\.", ".");
                hostname = hostname + ".*";
                while ((thisLine = myInput.readLine()) != null) {
                    if (thisLine.matches(hostname) || thisLine.matches(hostname2)) {

                        Matcher matcher = cookiePattern.matcher(thisLine);
                        if (matcher.find()) {
                            inp.put(matcher.group(3), matcher.group(4));
                        }
                    }
                }
                return joinMap(inp, "=", ";");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Passwort zur Unrarpasswortliste Hinzufügen (Wird im normalfall automatsch
     * gemacht nur notwendig wenn nicht ganz klar ist welches Passwort das
     * richtige ist)
     * 
     * @param password
     */
    public static void addToPasswordlist(String password) {
        JUnrar unrar = new JUnrar(false);
        unrar.addToPasswordlist(password);
    }

    /**
     * Diese Methode sucht nach passwörtern in einem Datensatz
     * 
     * @param data
     * @return
     */
    public static Vector<String> findPasswords(String data) {
        data = data.replaceAll("(?s)<!-- .*? -->", "").replaceAll("(?s)<script .*?>.*?</script>", "").replaceAll("(?s)<.*?>", "").replaceAll("Spoiler:", "");
        Vector<String> ret = new Vector<String>();
        Pattern pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]*?[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            if (matcher.group(2).length() > 5 && !matcher.group(2).matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) ret.add(matcher.group(2));
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?[\\s]*?([^\"'\\s]+)[\\s]", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            if (matcher.group(2).length() > 5 && !matcher.group(2).matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) ret.add(matcher.group(2));
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            if (matcher.group(2).length() > 3 && !matcher.group(2).matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) ret.add(matcher.group(2));
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?([^\"'\\s]+)[\\s]", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            if (matcher.group(2).length() > 3 && !matcher.group(2).matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) ret.add(matcher.group(2));
        }

        return ret;
    }

}
