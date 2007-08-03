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
import java.util.Iterator;
import java.util.Vector;
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
 * http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider
 * 
 * @author astaldo
 */
public abstract class Plugin{
    /**
     * Puffer für Lesevorgänge
     */
    public final int READ_BUFFER = 4000;
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
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen Listener
     * werden benachrichtigt, wenn mittels {@link #firePluginEvent(PluginEvent)} ein
     * Event losgeschickt wird.
     */
    public Vector<PluginListener> pluginListener=null;
    /**
     * Ein Logger, um Meldungen darzustellen
     */
    private Logger logger = Logger.getLogger(Plugin.LOGGER_NAME);
    
    protected Plugin(){
        pluginListener = new Vector<PluginListener>();
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
     * Diese Methode findet alle Vorkommnisse des Pluginpatterns in dem Text, und gibt die Treffer als Vector zurück
     * 
     * @param data Der zu durchsuchende Text
     * @return Alle Treffer in dem Text
     */
    public Vector<String> getMatches(String data){
        Vector<String> hits = null;
        Pattern pattern = getSupportedLinks();
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
        rd.close();
        RequestInfo requestInfo = new RequestInfo(htmlCode.toString(),location,null);
        return requestInfo;
    }
    /**
     * Speichert einen InputStream binär auf der Festplatte ab
     * 
     * @param is InputStream dessen Daten gespeichert werden sollen
     * @param fileOutput Ausgabedatei
     * @return wahr, wenn alle Daten ausgelesen und gespeichert wurden
     */
    public boolean saveByteFile(InputStream is, File fileOutput)
    {
        try{
            byte buffer[] = new byte[READ_BUFFER];
            int count;
            FileOutputStream fos = new FileOutputStream(fileOutput);
            do{
                count = is.read(buffer);
                if (count != -1){
                    fos.write(buffer, 0, count);
                }   
            }
            while (count != -1); // Muss -1 sein und nicht buffer.length da durch 
                                //  eine langsame Internetverbindung der Puffer nicht immer komplett gefüllt ist            
            fos.close();
            return true;
        }
        catch (FileNotFoundException e){
            logger.severe("Der Pfad dieser Datei ist ungültig."+e.getLocalizedMessage());
        }
        catch (SecurityException e){
            logger.severe("Keine Schreibrechte."+e.getLocalizedMessage());
        }
        catch (IOException e){
            logger.severe("Fehler beim Schreiben in die Datei."+e.getLocalizedMessage());
        }
        return false;
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
