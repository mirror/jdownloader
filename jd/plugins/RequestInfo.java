package jd.plugins;
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
    private String headers  = null;
    /**
     * Cookie
     */
    private String cookie   = null;

    public RequestInfo(String htmlCode, String location, String cookie, String headers){
        this.htmlCode = htmlCode;
        this.location = location;
        this.cookie   = cookie;
        this.headers  = headers;
    }
    public String getHeaders()  { return headers;  }
    public String getHtmlCode() { return htmlCode; }
    public String getLocation() { return location; }
    public String getCookie()   { return cookie;   }
}
