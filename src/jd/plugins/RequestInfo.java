package jd.plugins;

import java.util.List;
import java.util.Map;

/**
 * Diese Klasse bildet alle Informationen ab, die bei einem Request herausgefunden werden können
 *
 * @author astaldo
 */
public class RequestInfo {
    /**
     * Der Quelltext der Seite
     */
    private String htmlCode = null;
    /**
     * Die (Soll)Adresse der Seite
     */
    private String location = null;
    /**
     * Die zurückgelieferten Header
     */
    private  Map<String,List<String>> headers  = null;
    /**
     * Der zurückgelieferte Code
     */
    private int responseCode;
    /**
     * Cookie
     */
    private String cookie   = null;

    public RequestInfo(String htmlCode, String location, String cookie, Map<String,List<String>> headers, int responseCode){
        this.htmlCode = htmlCode;
        this.location = location;
        this.cookie   = cookie;
        this.headers  = headers;
    }
    public Map<String,List<String>> getHeaders() { return headers;      }
    public String getHtmlCode()                  { return htmlCode;     }
    public String getLocation()                  { return location;     }
    public String getCookie()                    { return cookie;       }
    public int getResponseCode()                 { return responseCode; }
}
