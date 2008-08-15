//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins;

import java.util.List;
import java.util.Map;

import jd.http.HTTPConnection;
import jd.http.Request;
import jd.parser.Form;
import jd.parser.Regex;

/**
 * Diese Klasse bildet alle Informationen ab, die bei einem Request
 * herausgefunden werden können
 * 
 * @author astaldo
 */
public class RequestInfo {
    private HTTPConnection connection;
    /**
     * Cookie
     */
    private String cookie = null;
    /**
     * Die zurückgelieferten Header
     */
    private Map<String, List<String>> headers = null;
    /**
     * Der Quelltext der Seite
     */
    private String htmlCode = null;
    /**
     * Die (Soll)Adresse der Seite
     */
    private String location = null;
    private Request request;
    /**
     * Der zurückgelieferte Code
     */
    private int responseCode;

    public RequestInfo(String htmlCode, String location, String cookie, Map<String, List<String>> headers, int responseCode) {
        this.htmlCode = htmlCode;
        this.location = location;
        this.cookie = cookie;
        this.headers = headers;
        this.responseCode = responseCode;

    }

    public boolean containsHTML(String pattern) {
        return getHtmlCode().indexOf(pattern) >= 0;
    }

    /**
     * @return the connection
     */
    public HTTPConnection getConnection() {
        return connection;
    }

    public String getCookie() {
        return cookie;
    }

    /**
     * gibt den ersten Match aus
     */
    public String getFirstMatch(String pattern) {
        return getRegexp(pattern).getMatch(0);
    }

    /**
     * gibt die erste Form der requestInfo aus
     * 
     * @return
     */
    public Form getForm() {
        return getForms()[0];
    }

    /**
     * gibt die Forms der requestInfo aus
     * 
     * @param pattern
     * @return
     */
    public Form[] getForms() {
        return Form.getForms(this);
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getHtmlCode() {
        return htmlCode;
    }

    public String getLocation() {
        return location;
    }

    /**
     * Macht einen Regexp auf die requestInfo
     * 
     * @param pattern
     * @return
     */
    public Regex getRegexp(String pattern) {
        return new Regex(this, pattern);
    }

    public Request getRequest() {
        return request;
    }

    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Gibt anhand des Rückgabecodes zurück, ob der Aufrufr erfolgreich war oder
     * nicht. HTTP Codes zwischen -2 und 499 gelten als erfolgreich Negative
     * Codes beudeuten dass der Server ( wie es z.B. machne Router HTTP Server
     * machen) keinen responseCode zurückgegeben hat). In diesem Fall wird
     * trotzdem true zurückgegeben
     * 
     * 
     * @return Wahr, wenn der HTTP Code zwischen -2 und 499 lag
     */
    public boolean isOK() {
        if (responseCode > -2 && responseCode < 500) {
            return true;
        } else {
            return false;
        }
    }

    public void setConnection(HTTPConnection connection) {
        this.connection = connection;
    }

    /**
     * Setzt den htmlCode kann z.B. bei der Form zum Einsatz kommen wenn ein
     * JavaScript die Form verändert
     * 
     * @param htmlCode
     */
    public void setHtmlCode(String htmlCode) {
        this.htmlCode = htmlCode;
    }

    public void setRequest(Request request) {
        this.request = request;

    }

    @Override
    public String toString() {
        return getHtmlCode();
    }
}
