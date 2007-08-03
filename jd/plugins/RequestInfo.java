package jd.plugins;

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
    
    public RequestInfo(String htmlCode, String location, String headers){
        this.htmlCode = htmlCode;
        this.location = location;
        this.headers  = headers;
    }
    public String getHeaders()  { return headers;  }
    public String getHtmlCode() { return htmlCode; }
    public String getLocation() { return location; }
}
