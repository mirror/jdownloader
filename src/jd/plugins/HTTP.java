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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import jd.config.Configuration;
import jd.utils.JDUtilities;

/**
 * Sammlung statischer Funktionen um HTTP Requests durchzuführen
 * 
 * @author JD-Team
 */
@SuppressWarnings("serial")
public class HTTP {

    /**
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link
     *            Die URL, die ausgelesen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo getRequest(URL link) throws IOException {
        return HTTP.getRequest(link, null, null, false);
    }

    /**
     * Liest Daten von einer URL. LIst den encoding type und kann plaintext und
     * gzip unterscheiden
     * 
     * @param urlInput
     *            Die URL Verbindung, von der geselen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo readFromURL(HTTPConnection urlInput) throws IOException {
        // Content-Encoding: gzip
        BufferedReader rd;
        if (urlInput.getHeaderField("Content-Encoding") != null && urlInput.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(urlInput.getInputStream())));
        } else {
            rd = new BufferedReader(new InputStreamReader(urlInput.getInputStream()));
        }
        String line;
        StringBuffer htmlCode = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            htmlCode.append(line + "\n");
        }
        String location = urlInput.getHeaderField("Location");
        String cookie = HTTP.getCookieString(urlInput);
        int responseCode = 0;
        responseCode = urlInput.getResponseCode();
        RequestInfo requestInfo = new RequestInfo(htmlCode.toString(), location, cookie, urlInput.getHeaderFields(), responseCode);
        rd.close();
        return requestInfo;
    }

    /**
     * Gibt header- und cookieinformationen aus ohne den HTMLCode
     * herunterzuladen
     * 
     * @param link
     *            Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie
     *            Cookie
     * @param referrer
     *            Referrer
     * @param parameter
     *            Die Parameter, die übergeben werden sollen
     * @param redirect
     *            Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequestWithoutHtmlCode(URL link, String cookie, String referrer, String parameter, boolean redirect) throws IOException {
        // logger.finer("post: "+link);
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(HTTP.getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(HTTP.getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        httpConnection.setDoOutput(true);
        if (parameter != null) {
            if (parameter == null) parameter = "";
            parameter = parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length() + "");
            
            httpConnection.connect();
            OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
            wr.write(parameter);
            wr.flush();
            wr.close();
        }
        String location = httpConnection.getHeaderField("Location");
        String setcookie = HTTP.getCookieString(httpConnection);
        int responseCode = 0;
        try {
            responseCode = httpConnection.getResponseCode();
        } catch (IOException e) {
        }
        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        // logger.finer("postRequest wo" + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return ri;
    }

    public static RequestInfo postRequest(URL url, String cookie, String referrer, HashMap<String, String> requestProperties, String parameter, boolean redirect, int readTimeout, int requestTimeout) throws IOException {
        // logger.finer("post: "+link+"(cookie:"+cookie+" parameter:
        // "+parameter+")");
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(url.openConnection());
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(requestTimeout);
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + url.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
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
        if (parameter != null) {
            //parameter = parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length() + "");
        }
        httpConnection.setDoOutput(true);
        httpConnection.connect();
    
        httpConnection.post(parameter);
    
        RequestInfo requestInfo = readFromURL(httpConnection);
    
        requestInfo.setConnection(httpConnection);
        // logger.finer("postRequest " + url + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return requestInfo;
    }

    /**
     * 
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param string
     *            Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie
     *            Cookie
     * @param referrer
     *            Referrer
     * @param requestProperties
     *            Hier können noch zusätliche Properties mitgeschickt werden
     * @param parameter
     *            Die Parameter, die übergeben werden sollen
     * @param redirect
     *            Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL string, String cookie, String referrer, HashMap<String, String> requestProperties, String parameter, boolean redirect) throws IOException {
        return postRequest(string, cookie, referrer, requestProperties, parameter, redirect, HTTP.getReadTimeoutFromConfiguration(), HTTP.getConnectTimeoutFromConfiguration());
    
    }

    /**
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param link
     *            Der Link, an den die POST Anfrage geschickt werden soll
     * @param parameter
     *            Die Parameter, die übergeben werden sollen
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL link, String parameter) throws IOException {
        return postRequest(link, null, null, null, parameter, false);
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
    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, HashMap<String, String> requestProperties, boolean redirect) throws IOException {
        // logger.finer("get: "+link);
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(HTTP.getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(HTTP.getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) {
            httpConnection.setRequestProperty("Cookie", cookie);
        }
    
        httpConnection.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
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
    
        httpConnection.connect();
        String location = httpConnection.getHeaderField("Location");
        String setcookie = HTTP.getCookieString(httpConnection);
        int responseCode = 0;
        try {
            responseCode = httpConnection.getResponseCode();
        } catch (IOException e) {
        }
    
        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        // logger.finer("getRequest wo2 " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return ri;
    }

    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, boolean redirect, int readTimeout, int requestTimeout) throws IOException {
        // logger.finer("get: "+link);
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(requestTimeout);
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
        httpConnection.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        String location = httpConnection.getHeaderField("Location");
        String setcookie = HTTP.getCookieString(httpConnection);
        int responseCode = 0;
        try {
            responseCode = httpConnection.getResponseCode();
        } catch (IOException e) {
        }
    
        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        // logger.finer("getReuqest wo " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return ri;
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
        return getRequestWithoutHtmlCode(link, cookie, referrer, redirect, HTTP.getReadTimeoutFromConfiguration(), HTTP.getConnectTimeoutFromConfiguration());
    
    }

    public static RequestInfo headRequest(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        // logger.finer("get: "+link+"(cookie: "+cookie+")");
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(HTTP.getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(HTTP.getConnectTimeoutFromConfiguration());
        httpConnection.setRequestMethod("HEAD");
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null) httpConnection.setRequestProperty("Referer", referrer);
    
        // httpConnection.setRequestProperty("Referer", "http://" +
        // link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        httpConnection.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        RequestInfo requestInfo = readFromURL(httpConnection);
        requestInfo.setConnection(httpConnection);
        // logger.finer("headRequest " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return requestInfo;
    }

    /**
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link
     *            Der Link, an den die GET Anfrage geschickt werden soll
     * @param cookie
     *            Cookie
     * @param referrer
     *            Referrer
     * @param redirect
     *            Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo getRequest(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        // logger.finer("get: "+link+"(cookie: "+cookie+")");
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(HTTP.getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(HTTP.getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null) httpConnection.setRequestProperty("Referer", referrer);
    
        // httpConnection.setRequestProperty("Referer", "http://" +
        // link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO User-Agent als Option ins menu
        // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
        // lassen
        // so ist das Programm nicht so auffallig
        httpConnection.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        RequestInfo requestInfo = readFromURL(httpConnection);
        requestInfo.setConnection(httpConnection);
       
        // logger.finer("getRequest " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return requestInfo;
    }

    public static int getReadTimeoutFromConfiguration() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 60000);
    }

    public static int getConnectTimeoutFromConfiguration() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 60000);
    }

    public static void setReadTimeout(int value) {
        JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, value);
    }

    public static void setConnectTimeout(int value) {
        JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, value);
    }

    /**
     * @author JD-Team Gibt den kompletten Cookiestring zurück, auch wenn die
     *         Cookies über mehrere Header verteilt sind
     * @param con
     * @return cookiestring
     */
    public static String getCookieString(HTTPConnection con) {
        String cookie = "";
        try {
            List<String> list = con.getHeaderFields().get("Set-Cookie");
            ListIterator<String> iter = list.listIterator(list.size());
            boolean last = false;
            while (iter.hasPrevious()) {
                cookie += (last ? "; " : "") + iter.previous().replaceFirst("; expires=.*", "");
                last = true;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return cookie;
    }
    

}
