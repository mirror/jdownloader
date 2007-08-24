package jd.plugins.host;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;

/**
 * HostPlugin für gullishare
 * 
 * TODO: Erzwungene Wartezeit (gibt es die überhaupt noch?)
 *
 */
public class Gulli extends PluginForHost {
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://share.gulli.com/.*");
    static private final Pattern PAT_CAPTCHA = Pattern.compile("<img src=\"(/captcha[^\"]*)");
    static private final Pattern PAT_FILE_ID = Pattern.compile("<input type=\"hidden\" name=\"file\" value=\"([^\"]*)");
    static private final Pattern PAT_DOWNLOAD_URL = Pattern.compile("<form action=\"/(download[^\"]*)");

    
    static private final String HOST_URL = "http://share.gulli.com/";
    static private final String DOWNLOAD_URL = "download";
    static private final String HOST = "share.gulli.com";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0";
    static private final String PLUGIN_ID = PLUGIN_NAME+"-"+VERSION;
    static private final String CODER = "olimex";

    /**
     * ID des Files bei gulli
     */
    private String fileId;
    
    /**
     * Map mit allen erhaltenen Cookies
     */
    private Map<String,String> cookieMap = new HashMap<String, String>();

    public Gulli() {
        steps.add(new PluginStep(PluginStep.STEP_CAPTCHA,  null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override public String getCoder()                { return CODER;        }
    @Override public String getPluginName()           { return HOST;             }
    @Override public Pattern getSupportedLinks()      { return PAT_SUPPORTED; }
    @Override public String getHost()                 { return HOST;             }
    @Override public boolean isClipboardEnabled()     { return true;             }
    @Override public String getVersion()              { return PLUGIN_VERSION;          }
    @Override public String getPluginID()             { return PLUGIN_ID; }


    @Override
    public URLConnection getURLConnection() {
        // XXX: ???
        return null;
    }

    @Override
    public PluginStep getNextStep(Object parameter) {
        try {
            DownloadLink downloadLink = (DownloadLink)parameter;
            
            if(currentStep == null){
                currentStep = steps.firstElement();
            } else {
                currentStep = steps.get(1);
            }

            logger.finest(currentStep.toString());
            
            switch(currentStep.getStep()){
            case PluginStep.STEP_CAPTCHA:
            {
                HttpURLConnection con = createConnection(downloadLink.getUrlDownload().toString());
                processPage(con,downloadLink);
            }
            break;
            case PluginStep.STEP_DOWNLOAD:
            {
                String captchaTxt = (String)steps.get(0).getParameter();

                HttpURLConnection con = createPostConnection(DOWNLOAD_URL, "file="+fileId+"&"+"captcha="+captchaTxt);

                processPage(con,downloadLink);
            }
            }
            return currentStep;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Verarbeitet von Gulli erhaltene Seite und führt je nach Inhalt eine
     * Captcha-Erkennung oder einen Download durch
     * @param con
     * @param dlLink
     * @throws IOException
     */
    private void processPage(HttpURLConnection con, DownloadLink dlLink) throws IOException {
        String content = contentToString(con);
        
        fileId = getFirstMatch(content, PAT_FILE_ID, 1);

        extractCookies(con, cookieMap);

        String captchaLocalUrl = getFirstMatch(content, PAT_CAPTCHA, 1);
        
        if (captchaLocalUrl != null) {
            logger.finest("Captcha Page");
            String captchaUrl = "http://share.gulli.com"+captchaLocalUrl;

            currentStep = steps.get(0);
            currentStep.setParameter(captchaUrl);
            currentStep.setStatus(PluginStep.STATUS_USER_INPUT);
        } else {
            String dlUrl = getFirstMatch(content, PAT_DOWNLOAD_URL, 1);

            if (dlUrl == null) {
                logger.finest("Error Page");
                currentStep.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            logger.finest("Download Page");

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
            HttpURLConnection dlcon = createPostConnection(dlUrl, "action=download&file="+fileId);

            int length = dlcon.getContentLength();
            dlLink.setDownloadLength(length);

            if (dlcon.getContentType().startsWith("text")) {
                currentStep.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            if (download(dlLink, dlcon)) {
                currentStep.setStatus(PluginStep.STATUS_DONE);
            } else {
                currentStep.setStatus(PluginStep.STATUS_ERROR);
            }
        }

    }

    
    /**
     * Erzeugt Verbindung zur URL mit gesetzten Cookies 
     * @param url
     * @return
     * @throws IOException
     */
    private HttpURLConnection createConnection(String url) throws IOException {
        logger.finest("URL: "+url);
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("Cookie", join(cookieMap,"=","; "));
        return con;
    }
    
    /**
     * Folgt HTTP-Redirections
     * @param con
     * @return
     * @throws IOException
     */
    private HttpURLConnection followRedir(HttpURLConnection con) throws IOException {
        if (con.getResponseCode() == 302) {
            logger.finest("Redir: "+con.getHeaderField("Location"));
            con = createConnection(con.getHeaderField("Location"));
            con = followRedir(con);
        }
        return con;
    }
    
    /**
     * Erzeugt HTTP-POST Verbindung zur lokaler URL 
     * @param localUrl
     * @param postParameter
     * @return
     * @throws IOException
     */
    private HttpURLConnection createPostConnection(String localUrl, String postParameter) throws IOException {
        HttpURLConnection con = createConnection(HOST_URL+localUrl);

        HttpURLConnection.setFollowRedirects(false);

        con.setDoOutput(true);

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(postParameter);
        wr.flush();

        return followRedir(con);
    }
    
    
    
    /**
     * Sammelt Cookies einer HTTP-Connection und fügt dieser einer Map
     * hinzu
     * TODO: auslagern
     * @param con Connection
     * @param cookieMap Map in der die Cookies eingefügt werden
     */
    private static void extractCookies(HttpURLConnection con, Map<String, String> cookieMap) {
        Collection<String> cookieHeaders = con.getHeaderFields().get("Set-Cookie");

        if (cookieHeaders == null)
            return;

        for(String header: cookieHeaders) {
            try {
                StringTokenizer st = new StringTokenizer(header, ";=");				
                cookieMap.put(st.nextToken(), st.nextToken());
            } catch (NoSuchElementException e) {
                // ignore
            }  		
        }

    }
    
    /**
     * Liest Content von Connection und gibt diesen als String zurück
     * TODO: auslagern
     * @param con Connection
     * @return Content
     * @throws IOException
     */
    public static String contentToString(HttpURLConnection con) throws IOException {
        InputStreamReader in = new InputStreamReader(con.getInputStream());
        StringBuffer sb = new StringBuffer();
        int chr;
        while((chr = in.read()) != -1) {
            sb.append((char)chr);
        }
        return sb.toString();
    }
    
    /**
     * Fügt Map als String mit Trennzeichen zusammen 
     * TODO: auslagern
     * @param map Map
     * @param delPair Trennzeichen zwischen Key und Value
     * @param delMap Trennzeichen zwischen Map-Einträgen
     * @return
     */
    private static String join(Map<String,String> map, String delPair, String delMap) {
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
}
