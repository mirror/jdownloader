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
import jd.parser.Regex;

/**
 * Diese Klasse bildet alle Informationen ab, die bei einem Request
 * herausgefunden werden können
 * 
 * @author astaldo
 */
@Deprecated
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

    public RequestInfo(String htmlCode, String location, String cookie, Map<String, List<String>> headers, int responseCode) {
        this.htmlCode = htmlCode;
        this.cookie = cookie;
        this.headers = headers;
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

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getHtmlCode() {
        return htmlCode;
    }

    /**
     * Macht einen Regexp auf die requestInfo
     * 
     * @param pattern
     * @return
     */
    public Regex getRegex(String pattern) {
        return new Regex(this, pattern);
    }

    public void setConnection(HTTPConnection connection) {
        this.connection = connection;
    }

    @Override
    public String toString() {
        return getHtmlCode();
    }
}
