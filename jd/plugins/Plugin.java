package jd.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
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
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins.
 * Alle Plugins müssen von dieser Klasse abgeleitet werden.
 * 
 * Alle Plugins verfügen über einen Event Mechanismus
 *  
 * Hinweise zum Pluginsystem findet ihr hier
 * http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html
 * 
 * @author astaldo
 */
public abstract class Plugin{
    /**
     * Puffer für Lesevorgänge
     */
    public final int READ_BUFFER = 4000;
    /**
     * Name des Loggers
     */
    public static String LOGGER_NAME ="astaldo.java_downloader";
    /**
     * Liefert den Namen des Plugins zurück
     * @return Der Name des Plugins
     */
    public abstract String               getPluginName();
    /**
     * Hier wird der Author des Plugins ausgelesen
     * 
     * @return Der Author des Plugins
     */
    public abstract String               getCoder();
    /**
     * Liefert die Versionsbezeichnung dieses Plugins zurück
     * @return Versionsbezeichnung
     */
    public abstract String               getVersion();
    /**
     * Ein regulärer Ausdruck, der anzeigt, welche Links von diesem Plugin unterstützt werden
     * 
     * @return Ein regulärer Ausdruck
     * @see Pattern
     */
    public abstract Pattern              getSupportedLinks();
    /**
     * Liefert den Anbieter zurück, für den dieses Plugin geschrieben wurde
     * 
     * @return Der unterstützte Anbieter
     */
    public abstract String               getHost();
    /**
     * Diese Methode zeigt an, ob das Plugin auf Änderungen in der Zwischenablage reagiert oder nicht
     * @return Wahr, wenn die Zwischenablage von diesem Plugin interpretiert werden soll
     */
    public abstract boolean              isClipboardEnabled();
    /**
     * Diese Methode liefert den nächsten Schritt zurück, den das Plugin vornehmen wird.
     * Falls der letzte Schritt erreicht ist, wird null zurückgegeben
     * 
     * @param parameter Ein Übergabeparameter
     * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
     */
    public abstract PluginStep           getNextStep(Object parameter);
    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen Listener
     * werden benachrichtigt, wenn mittels {@link #firePluginEvent(PluginEvent)} ein
     * Event losgeschickt wird.
     */
    public Vector<PluginListener> pluginListener=null;
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
    
    protected Plugin(){
        pluginListener = new Vector<PluginListener>();
        steps = new Vector<PluginStep>();
    }
    /**
     * Liefert die Klasse zurück, mit der Nachrichten ausgegeben werden können
     * Falls dieser Logger nicht existiert, wird ein neuer erstellt
     * 
     * @return LogKlasse
     */
    public static Logger getLogger(){
        if (logger == null){
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
     * Hier wird geprüft, ob das Plugin diesen Text oder einen Teil davon handhaben kann.
     * Dazu wird einfach geprüft, ob ein Treffer des Patterns vorhanden ist.
     * 
     * @param data der zu prüfende Text
     * @return wahr, falls ein Treffer gefunden wurde.
     */
    public synchronized boolean canHandle(String data){
        Pattern pattern = getSupportedLinks();
        if(pattern!=null){
            Matcher matcher = pattern.matcher(data);
            if(matcher.find()){
                return true;
            }
        }
        return false;
    }
    /**
     * Findet ein einzelnes Vorkommen und liefert den vollständigen Treffer oder eine Untergruppe zurück
     * 
     * @param data Der zu durchsuchende Text
     * @param pattern Das Muster, nach dem gesucht werden soll
     * @param group Die Gruppe, die zurückgegeben werden soll. 0 ist der vollständige Treffer.
     * @return Der Treffer
     */
    public String getFirstMatch(String data, Pattern pattern, int group){
        String hit = null;
        if(pattern!=null){
            Matcher matcher = pattern.matcher(data);
            if(matcher.find()){
                hit = matcher.group(group);
            }
        }
        return hit;
    }
    /**
     * Diese Methode findet alle Vorkommnisse des Pluginpatterns in dem Text, und gibt die Treffer als Vector zurück
     * 
     * @param data Der zu durchsuchende Text
     * @param pattern Das Muster, nach dem gesucht werden soll
     * @return Alle Treffer in dem Text
     */
    public Vector<String> getMatches(String data, Pattern pattern){
        Vector<String> hits = null;
        if(pattern!=null){
            Matcher matcher = pattern.matcher(data);
            if(matcher.find()){
                hits = new Vector<String>();
                int position=0;
                while(matcher.find(position)){
                    hits.add(matcher.group());
                    position = matcher.start()+matcher.group().length();
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
    protected int countOccurences(String data, Pattern pattern){
        int position   = 0;
        int occurences = 0;

        if(pattern!=null){
            Matcher matcher = pattern.matcher(data);
            while(matcher.find(position)){
                occurences ++;
                position = matcher.start()+matcher.group().length();
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
    public String cutMatches(String data){
        return data.replaceAll(getSupportedLinks().pattern(), "--CUT--");
    }
    /**
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link Die URL, die ausgelesen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public RequestInfo getRequest(URL link)throws IOException{
        return getRequest(link, null, null, false);
    }
    /**
     * TODO postRequest : referrer
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link Der Link, an den die GET Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public RequestInfo getRequest(URL link, String cookie, String referrer, boolean redirect)throws IOException{
        HttpURLConnection httpConnection = (HttpURLConnection)link.openConnection();
        return readFromURL(httpConnection);
    }
    /**
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param link Der Link, an den die POST Anfrage geschickt werden soll
     * @param parameter Die Parameter, die übergeben werden sollen
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public RequestInfo postRequest(URL link, String parameter)throws IOException{
        return postRequest(link, null, null, parameter, false);
    }
    /**
     * TODO postRequest : referrer
     * 
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param link Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie Cookie
     * @param referrer Referrer
     * @param parameter Die Parameter, die übergeben werden sollen
     * @param redirect Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public RequestInfo postRequest(URL link, String cookie, String referrer, String parameter, boolean redirect) throws IOException{
        HttpURLConnection httpConnection = (HttpURLConnection)link.openConnection();
        httpConnection.setInstanceFollowRedirects(redirect);
        httpConnection.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
        if(parameter != null)
            wr.write(parameter);
        wr.flush();
        
    
        RequestInfo requestInfo = readFromURL(httpConnection);
        wr.close();
        return requestInfo;
    }
    /**
     * Liest Daten von einer URL
     * 
     * @param urlInput Die URL Verbindung, von der geselen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    private RequestInfo readFromURL(HttpURLConnection urlInput)throws IOException{
        BufferedReader rd = new BufferedReader(new InputStreamReader(urlInput.getInputStream()));
        String line;
        StringBuffer htmlCode = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            htmlCode.append(line);
        }
        String location = urlInput.getHeaderField("location");
        String cookie   = urlInput.getHeaderField("Cookie");
        rd.close();
        RequestInfo requestInfo = new RequestInfo(htmlCode.toString(),location,cookie,null);
        return requestInfo;
    }
    /**
     * Speichert einen InputStream binär auf der Festplatte ab
     * 
     * @param is InputStream dessen Daten gespeichert werden sollen
     * @param fileOutput Ausgabedatei
     * @param urlConnection Wenn bereits vom Plugin eine vorkonfigurierte URLConnection vorhanden ist, wird diese
     *                      hier übergeben und benutzt. Ansonsten erfolgt ein normaler GET Download von 
     *                      der URL, die im DownloadLink hinterlegt ist
     * @return wahr, wenn alle Daten ausgelesen und gespeichert wurden
     */
    protected boolean download(DownloadLink downloadLink, URLConnection urlConnection)
    {
        File fileOutput = downloadLink.getFileOutput();
        InputStream is;
        int downloadedBytes=0;
        try{
            byte buffer[] = new byte[READ_BUFFER];
            int count;
            
            //Falls keine urlConnection übergeben wurde
            if(urlConnection == null)
                is = downloadLink.getUrlDownload().openConnection().getInputStream();
            else
                is = urlConnection.getInputStream();
            FileOutputStream fos = new FileOutputStream(fileOutput);
            downloadLink.setInProgress(true);
            do{
                count = is.read(buffer);
                if (count != -1){
                    fos.write(buffer, 0, count);
                    downloadedBytes +=READ_BUFFER;
                    downloadLink.setDownloadedBytes(downloadedBytes);
                    firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_DATA_CHANGED,null));
                }   
            }
            while (count != -1); // Muss -1 sein und nicht buffer.length da durch 
                                //  eine langsame Internetverbindung der Puffer nicht immer komplett gefüllt ist            
            fos.close();
            is.close();
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH,null));
            return true;
        }
        catch (FileNotFoundException e){
            logger.severe("fileDescription is wrong. "+e.getLocalizedMessage());
        }
        catch (SecurityException e){
            logger.severe("not enough rights to write the file. "+e.getLocalizedMessage());
        }
        catch (IOException e){
            logger.severe("error occurred while writing to file. "+e.getLocalizedMessage());
        }
        return false;
    }
    /**
     * Diese Methode erstellt einen einzelnen String aus einer HashMap mit 
     * Parametern für ein Post-Request.
     * 
     * @param parameters HashMap mit den Parametern
     * @return Codierter String
     */
    protected String createPostParameterFromHashMap(HashMap<String, String> parameters){
        StringBuffer parameterLine = new StringBuffer();
        Iterator<String> iterator = parameters.keySet().iterator();
        String key;
        while(iterator.hasNext()){
            key = iterator.next();
            parameterLine.append(key);
            parameterLine.append("=");
            parameterLine.append(parameters.get(key));
            if(iterator.hasNext())
                parameterLine.append("&");
        }
        return parameterLine.toString();
    }
   
    ///////////////////////////////////////////////////////
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

            while(recIt.hasNext()) { 
                ((PluginListener)recIt.next()).pluginEvent(pluginEvent); 
            }
        }
    } 
}
