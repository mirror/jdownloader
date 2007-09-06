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
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
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
     * TODO
     * folgende methoden werden benoetigt
     * setCaptchaAdress(); //damit setzt man die Internetadresse des Captchafiles
     * getCaptchaCode();   //damit kann man sich den Captchacode als String holen
     * getCaptchaFilePath(); //
     * getCaptchaFile();
     */
    /**
     * Puffer für Lesevorgänge
     */
    public final int READ_BUFFER = 128*1024;
    /**
     * Name des Loggers
     */
    public static String LOGGER_NAME = "java_downloader";
    /**
     * Versionsinformationen
     */
    public static final String VERSION = "jDownloader_20070830_0";
    /**
     * Zeigt an, ob das Plugin abgebrochen werden soll
     */
    protected boolean aborted = false;
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
     * Diese Methode liefert den nächsten Schritt zurück, den das Plugin
     * vornehmen wird. Falls der letzte Schritt erreicht ist, wird null
     * zurückgegeben
     * 
     * @param parameter Ein Übergabeparameter
     * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
     */
    public abstract PluginStep getNextStep(Object parameter);
    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    public Vector<PluginListener> pluginListener = null;
    /**
     * Hier werden alle notwendigen Schritte des Plugins hinterlegt
     */
    protected Vector<PluginStep> steps;
    /**
     * Enthält den aktuellen Schritt des Plugins
     */
    protected PluginStep currentStep = null;
    /**
     * Ein Logger, um Meldungen darzustellen
     */
    protected static Logger logger = null;

    protected Plugin() {
        pluginListener = new Vector<PluginListener>();
        steps = new Vector<PluginStep>();
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
     * Gibt ausgehend vom aktuellen step den nächsten zurück
     * @author coalado
     * @param currentStep
     * @return nächster step
     */
    public PluginStep nextStep(PluginStep currentStep){
        if(steps==null ||steps.size()==0)return null;
        if(currentStep==null)return steps.firstElement();
        int index= steps.indexOf(currentStep)+1;
        if(steps.size()>index)return steps.elementAt(index);
        return null;
    }
    
    /**
     * @author coalado
     * Setzt den Pluginfortschritt zurück. Wird Gebraucht um einen Download nochmals zu starten, z.B. nach dem reconnect
     */
    public void resetSteps(){
        currentStep=null;
        for(int i=0; i<steps.size();i++){
            steps.elementAt(i).setStatus(0);
        }
        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DATA_CHANGED, null)); 
    }
    /**
     * @author olimex
     * Fügt Map als String mit Trennzeichen zusammen 
     * TODO: auslagern
     * @param map Map
     * @param delPair Trennzeichen zwischen Key und Value
     * @param delMap Trennzeichen zwischen Map-Einträgen
     * @return Key-value pairs
     */
    public static String joinMap(Map<String,String> map, String delPair, String delMap) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for(Map.Entry<String,String> entry: map.entrySet()) {
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
     * @author olimex
     * @param con
     *            Connection
     * @param cookieMap
     *            Map in der die Cookies eingefügt werden
     */
    public static HashMap<String, String> collectCookies(HttpURLConnection con) {
        Collection<String> cookieHeaders = con.getHeaderFields().get("Set-Cookie");
        HashMap<String, String> cookieMap=new HashMap<String, String>();
        if (cookieHeaders == null)
            return cookieMap;

        for (String header : cookieHeaders) {
            try {
               
              
                StringTokenizer st = new StringTokenizer(header, ";=");
                while(st.hasMoreTokens())
                cookieMap.put(st.nextToken().trim(), st.nextToken().trim());

            } catch (NoSuchElementException e) {
                // ignore
            }
        }
        return cookieMap;

    }
    
    
    /**
     * @author coalado
     * Gibt den kompletten Cookiestring zurück, auch wenn die Cookies über mehrere Header verteilt sind
     * @param con
     * @return cookiestring
     */
    public static String getCookieString(HttpURLConnection con){
        return joinMap(collectCookies(con),"=","; ");
    }
    /**
     * @author coalado
     * @return Gibt den aktuellen Schritt oder null zurück
     */
    public PluginStep getCurrentStep(){
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
     * Findet ein einzelnes Vorkommen und liefert den vollständigen Treffer
     * oder eine Untergruppe zurück
     * 
     * @param data Der zu durchsuchende Text
     * @param pattern Das Muster, nach dem gesucht werden soll
     * @param group Die Gruppe, die zurückgegeben werden soll. 0 ist der vollständige Treffer.
     * @return Der Treffer
     */
    public String getFirstMatch(String data, Pattern pattern, int group) {
        String hit = null;
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
     * @return der Text zwischen den gefundenen stellen oder, falls nichts gefunden wurde, der vollständige Text
     */
    public String getBetween(String data, String startPattern, String lastPattern) {
        Pattern p = Pattern.compile("(?s)"+startPattern+"(.*?)"+lastPattern, Pattern.CASE_INSENSITIVE);
        Matcher match = p.matcher(data);
        if(match.find())
            return match.group(1);
        return data;
    }
    
    /**
     * Diese Methode sucht die vordefinierten input type="hidden" zwischen startpattern und lastpattern
     * und formatiert sie zu einem poststring
     * z.b. würde bei:
     * 
                <input type="hidden" name="f" value="f50b0f" />
                <input type="hidden" name="h" value="390b4be0182b85b0" />
                <input type="hidden" name="b" value="9" />    
     *
     * f=f50b0f&h=390b4be0182b85b0&b=9 rauskommen
     * 
     * @param data Der zu durchsuchende Text
     * @param startPattern der Pattern, bei dem die Suche beginnt
     * @param lastPattern der Pattern, bei dem die Suche endet
     * 
     * @return ein String, der als POST Parameter genutzt werden kann und
     *         alle Parameter des Formulars enthält
     */
    public String getFormInputHidden(String data, String startPattern, String lastPattern)
    {
        return getFormInputHidden(getBetween(data, startPattern, lastPattern));
    }
    
    /**
     * Diese Methode sucht die vordefinierten input type="hidden"
     * und formatiert sie zu einem poststring
     * z.b. würde bei:
     * 
                <input type="hidden" name="f" value="f50b0f" />
                <input type="hidden" name="h" value="390b4be0182b85b0" />
                <input type="hidden" name="b" value="9" />    
     *
     * f=f50b0f&h=390b4be0182b85b0&b=9 ausgegeben werden
     * 
     * @param data Der zu durchsuchende Text
     * 
     * @return ein String, der als POST Parameter genutzt werden kann und
     *         alle Parameter des Formulars enthält
     */
    public String getFormInputHidden(String data) {
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
        String string = "";
        boolean iscompl;
        boolean first = true;
        while (matcher1.find()) {
            matcher2 = intput2.matcher(matcher1.group(1) + " ");
            matcher3 = intput3.matcher(matcher1.group(1) + " ");
            matcher4 = intput4.matcher(matcher1.group(1) + " ");
            matcher5 = intput5.matcher(matcher1.group(1) + " ");
            iscompl = false;
            String szwstring = "";
            if (matcher2.find()) {
                iscompl = true;
                szwstring += matcher2.group(1) + "=";
            } else if (matcher4.find()) {
                iscompl = true;
                szwstring += matcher4.group(1) + "=";
            }
            if (matcher3.find() && iscompl)
                szwstring += matcher3.group(1);
            else if (matcher5.find() && iscompl)
                szwstring += matcher5.group(1);
            else
                iscompl = false;
            if (iscompl) {
                if (!first) {
                    string += "&" + szwstring;
                } else {
                    string += szwstring;
                    first = false;
                }
            }
        }
        return string;
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
        //wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://"+ link.getHost());
        if (cookie != null)
            httpConnection.setRequestProperty("Cookie", cookie);
        // TODO User-Agent als Option ins menu
        // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
        // lassen
        // so ist das Programm nicht so auffallig
        httpConnection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
RequestInfo requestInfo=readFromURL(httpConnection);
    requestInfo.setConnection(httpConnection);
        return requestInfo;
    }
    
    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        //wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://"+ link.getHost());
        if (cookie != null){
           
            httpConnection.setRequestProperty("Cookie", cookie);
        }
        // TODO User-Agent als Option ins menu
        // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
        // lassen
        // so ist das Programm nicht so auffallig
        httpConnection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

        String location = httpConnection.getHeaderField("Location");
        
        String setcookie = getCookieString(httpConnection);
        int responseCode =HttpURLConnection.HTTP_NOT_IMPLEMENTED; 
        try {
            responseCode = httpConnection.getResponseCode();
        }
        catch (IOException e) { }
        RequestInfo ri=new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(),responseCode);
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
    public static RequestInfo postRequest(URL link, String parameter)
            throws IOException {
        return postRequest(link, null, null, null, parameter, false);
    }
    /**
     * 
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param link Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param requestProperties Hier können noch zusätliche Properties mitgeschickt werden
     * @param parameter Die Parameter, die übergeben werden sollen
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL link, String cookie,String referrer, HashMap<String, String> requestProperties, String parameter, boolean redirect) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null)
            httpConnection.setRequestProperty("Cookie", cookie);
        if(requestProperties != null){
            Set<String> keys = requestProperties.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while(iterator.hasNext()){
                key = iterator.next();
                httpConnection.setRequestProperty(key, requestProperties.get(key));
            }
        }
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

        httpConnection.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
        if (parameter != null)
            wr.write(parameter);
        wr.flush();

        RequestInfo requestInfo = readFromURL(httpConnection);
        wr.close();
        
        requestInfo.setConnection(httpConnection);
        return requestInfo;
    }
    /**
     * Gibt header- und cookieinformationen aus ohne den HTMLCode herunterzuladen
     * 
     * @param link Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param parameter Die Parameter, die übergeben werden sollen
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequestWithoutHtmlCode(URL link, String cookie, String referrer, String parameter, boolean redirect)throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://"+ link.getHost());
        if (cookie != null)
            httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        if (parameter != null) {
            httpConnection.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());

            wr.write(parameter);
            wr.flush();
            wr.close();
        }

        String location = httpConnection.getHeaderField("Location");
        
        String setcookie = getCookieString(httpConnection);
        int responseCode =HttpURLConnection.HTTP_NOT_IMPLEMENTED; 
        try {
            responseCode = httpConnection.getResponseCode();
        }
        catch (IOException e) { }
        RequestInfo ri=new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(),responseCode);
        ri.setConnection(httpConnection);
        return ri;
    }
    /**
     * Liest Daten von einer URL
     * 
     * @param urlInput Die URL Verbindung, von der geselen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo readFromURL(HttpURLConnection urlInput)throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(urlInput.getInputStream()));
        String line;
        StringBuffer htmlCode = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            //so bleibt der html-syntax erhalten
            htmlCode.append(line + "\n");
        }
        //wenn du nur informationen ueber den header oder cookies braust benutz bitte postRequestWithoutHtmlCode
        //ich hab hir mal Location gross und aus cookie Set-Cookie gemacht weil der Server Set-Cookie versendet
        String location = urlInput.getHeaderField("Location");
        String cookie = getCookieString(urlInput);
        int responseCode =HttpURLConnection.HTTP_NOT_IMPLEMENTED; 
        try {
            responseCode = urlInput.getResponseCode();
        }
        catch (IOException e) { }
        RequestInfo requestInfo = new RequestInfo(htmlCode.toString(), location, cookie, urlInput.getHeaderFields(),responseCode);
        rd.close();
        return requestInfo;
    }
    /**
     * Speichert einen InputStream binär auf der Festplatte ab
     * 
     * @param downloadLink der DownloadLink
     * @param urlConnection Wenn bereits vom Plugin eine vorkonfigurierte URLConnection
     *                      vorhanden ist, wird diese hier übergeben und benutzt.
     *                      Ansonsten erfolgt ein normaler GET Download von der URL, die
     *                      im DownloadLink hinterlegt ist
     * @return wahr, wenn alle Daten ausgelesen und gespeichert wurden
     */
    public boolean download(DownloadLink downloadLink, URLConnection urlConnection) { 
        File fileOutput = downloadLink.getFileOutput(); 
        int downloadedBytes = 0; 
        long start, end, time; 
        try { 
            ByteBuffer buffer = ByteBuffer.allocateDirect(READ_BUFFER); 

            // Falls keine urlConnection übergeben wurde 
            if (urlConnection == null) 
                urlConnection = downloadLink.getUrlDownload().openConnection(); 

            FileOutputStream fos = new FileOutputStream(fileOutput); 

            // NIO Channels setzen: 
            ReadableByteChannel source = Channels.newChannel(urlConnection.getInputStream()); 
            WritableByteChannel dest = fos.getChannel(); 

            // Länge aus HTTP-Header speichern: 
            int contentLen = urlConnection.getContentLength(); 

            downloadLink.setInProgress(true); 
            logger.info("starting download"); 
            start = System.currentTimeMillis(); 

            // Buffer, laufende Variablen resetten: 
            buffer.clear(); 
            int bytesLastSpeedCheck=0; 
            long t1 = System.currentTimeMillis(); 

            for(int i=0;!aborted;i++) { 
                // Thread kurz schlafen lassen, um zu häufiges Event-fire zu verhindern: 
                Thread.sleep(50); 
                int bytes = source.read(buffer); 

                if (bytes==-1) 
                    break; 

                // Buffer flippen und in File schreiben: 
                buffer.flip(); 
                dest.write(buffer); 
                buffer.compact(); 

                // Laufende Variablen updaten: 
                downloadedBytes += bytes; 
                bytesLastSpeedCheck += bytes; 

                if (i % 20 == 0) { // Speedcheck alle 10 Runden = 1 sec 
                    long t2 = System.currentTimeMillis(); 
                    // DL-Speed in bytes/sec berechnen: 
                    int speed = (int)(bytesLastSpeedCheck*1000/(t2-t1)); 
                    downloadLink.setDownloadSpeed(speed);
                    bytesLastSpeedCheck = 0; 
                    t1 = t2; 
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DOWNLOAD_SPEED, speed)); 
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_DATA_CHANGED, downloadLink)); 
                } 

                downloadLink.setDownloadCurrent(downloadedBytes); 
            } 


            if (contentLen != -1 && downloadedBytes != contentLen) { 
                logger.info("incomplete download"); 
                return false; 
            } 

            end = System.currentTimeMillis(); 
            time = end - start; 
            source.close(); 
            dest.close(); 
            fos.close(); 

            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, downloadLink)); 
            logger.info("download finished:"+fileOutput.getAbsolutePath()); 

            logger.info(downloadedBytes + " bytes in " + time +" ms"); 
            return true; 
        } 
        catch (FileNotFoundException e) { logger.severe("file not found. " + e.getLocalizedMessage());                      } 
        catch (SecurityException e)     { logger.severe("not enough rights to write the file. " + e.getLocalizedMessage()); } 
        catch (IOException e)           { logger.severe("error occurred while writing to file. "+ e.getLocalizedMessage()); } 
        catch (InterruptedException e)  { logger.severe("interrupted exception: "+ e.getLocalizedMessage()); } 
        return false; 
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
                if (parameter != null)
                    parameter = URLEncoder.encode(parameter, "US-ASCII");
            } 
            catch (UnsupportedEncodingException e) { }
            parameterLine.append(key);
            parameterLine.append("=");
            parameterLine.append(parameter);
            if (iterator.hasNext())
                parameterLine.append("&");
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
     * @param filepath Dateiname
     * @return MD5 des Datei
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public String md5sum(String filepath) throws NoSuchAlgorithmException, FileNotFoundException
    {
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
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
    }
    /**
     * Gibt die matches ohne Dublikate als arraylist aus
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
}