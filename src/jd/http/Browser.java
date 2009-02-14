//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.requests.FormData;
import jd.http.requests.GetRequest;
import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;
import jd.http.requests.Request;
import jd.http.requests.RequestVariable;
import jd.parser.JavaScript;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.parser.html.XPath;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;
import jd.utils.SnifferException;
import jd.utils.Sniffy;

public class Browser {
    public class BrowserException extends IOException {

        private static final long serialVersionUID = 1509988898224037320L;

        public BrowserException(String string) {
            super(string);

        }
    }

    private static JDProxy GLOBAL_PROXY = null;

    public static void setGlobalProxy(JDProxy p) {
        GLOBAL_PROXY = p;
    }

    public static JDProxy getGlobalProxy() {
        return GLOBAL_PROXY;
    }

    private static HashMap<String, HashMap<String, Cookie>> COOKIES = new HashMap<String, HashMap<String, Cookie>>();
    private HashMap<String, HashMap<String, Cookie>> cookies = new HashMap<String, HashMap<String, Cookie>>();

    private boolean debug = false;

    private static HashMap<String, Long> LATEST_PAGE_REQUESTS = new HashMap<String, Long>();
    private static HashMap<URL, Browser> URL_LINK_MAP = new HashMap<URL, Browser>();
    private HashMap<String, Long> latestRequestTimes = new HashMap<String, Long>();
    private HashMap<String, String[]> logins = new HashMap<String, String[]>();
    private String latestReqTimeCtrlID = null;
    private long waittimeBetweenRequests = 0L;
    private boolean exclusiveReqTimeCtrl = true;

    private synchronized long getLastestRequestTime() {
        if (latestReqTimeCtrlID == null) return 0L;
        if (!exclusiveReqTimeCtrl) {
            if (LATEST_PAGE_REQUESTS.containsKey(latestReqTimeCtrlID)) { return LATEST_PAGE_REQUESTS.get(latestReqTimeCtrlID); }
        } else {
            if (latestRequestTimes.containsKey(latestReqTimeCtrlID)) { return latestRequestTimes.get(latestReqTimeCtrlID); }
        }
        return 0L;
    }

    private synchronized void setLatestRequestTime(long time) {
        if (latestReqTimeCtrlID == null) return;
        if (!exclusiveReqTimeCtrl) {
            LATEST_PAGE_REQUESTS.put(latestReqTimeCtrlID, time);
        } else {
            latestRequestTimes.put(latestReqTimeCtrlID, time);
        }
    }

    public String getLatestReqTimeCtrlID() {
        return latestReqTimeCtrlID;
    }

    public void setLatestReqTimeCtrlID(String latestReqTimeCtrlID) {
        this.latestReqTimeCtrlID = latestReqTimeCtrlID;
    }

    public void setReqTimeCtrlExclusive(boolean value) {
        exclusiveReqTimeCtrl = value;

    }

    public void setWaittimeBetweenPageRequests(long value) {
        waittimeBetweenRequests = value;
    }

    private void waitForPageAccess() {
        if (latestReqTimeCtrlID == null) return;
        while (true) {
            long time = Math.max(0, waittimeBetweenRequests - (System.currentTimeMillis() - getLastestRequestTime()));
            if (time > 0) {
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            } else {
                break;
            }
        }
        setLatestRequestTime(System.currentTimeMillis());
    }

    public void clearCookies(String url) {
        String host = url;
        try {
            host = Browser.getHost(url);
        } catch (MalformedURLException e) {
        }
        Iterator<String> it = getCookies().keySet().iterator();
        String check = null;
        while (it.hasNext()) {
            check = it.next();
            if (check.contains(host)) {
                cookies.remove(check);
                break;
            }

        }
    }

    public void forwardCookies(Request request) throws MalformedURLException {
        if (request == null) { return; }
        String host = Browser.getHost(request.getUrl());
        HashMap<String, Cookie> cookies = getCookies().get(host);
        if (cookies == null) { return; }

        for (Iterator<Entry<String, Cookie>> it = cookies.entrySet().iterator(); it.hasNext();) {
            Cookie cookie = it.next().getValue();

            // Pfade sollten verarbeitet werden...TODO
            if (cookie.isExpired()) {
                continue;
            }
            request.getCookies().add(cookie);
        }

    }

    public void forwardCookies(URLConnectionAdapter con) throws MalformedURLException {
        if (con == null) { return; }
        String host = Browser.getHost(con.getURL().toString());
        HashMap<String, Cookie> cookies = getCookies().get(host);
        String cs = Request.getCookieString(cookies);
        if (cs != null && cs.trim().length() > 0) con.setRequestProperty("Cookie", cs);
    }

    public String getCookie(String url, String string) throws MalformedURLException {
        String host;

        host = Browser.getHost(url);

        HashMap<String, Cookie> cookies = getCookies().get(host);
        if (cookies != null && cookies.containsKey(string)) {
            return cookies.get(string).getValue();
        } else
            return null;
    }

    public HashMap<String, HashMap<String, Cookie>> getCookies() {

        if (this.cookiesExclusive) return cookies;
        return COOKIES;
    }

    public void setCookie(String url, String key, String value) throws MalformedURLException {
        String host;

        host = Browser.getHost(url);
        HashMap<String, Cookie> cookies;
        if (!getCookies().containsKey(host) || (cookies = getCookies().get(host)) == null) {
            cookies = new HashMap<String, Cookie>();
            getCookies().put(host, cookies);
        }

        Cookie cookie = new Cookie();
        cookie.setHost(host);
        cookie.setKey(key);
        cookie.setValue(value);
        cookies.put(key.trim(), cookie);

    }

    public static String getHost(Object url) throws MalformedURLException {

        String ret = new URL(url + "").getHost();
        int id = 0;
        while ((id = ret.indexOf(".")) != ret.lastIndexOf(".")) {
            ret = ret.substring(id + 1);

        }
        return ret;

    }

    public void updateCookies(Request request) throws MalformedURLException {
        if (request == null) { return; }
        String host = Browser.getHost(request.getUrl());
        HashMap<String, Cookie> cookies = getCookies().get(host);
        if (cookies == null) {
            cookies = new HashMap<String, Cookie>();
            getCookies().put(host, cookies);
        }
        for (Cookie cookie : request.getCookies()) {
            cookies.put(cookie.getKey(), cookie);
        }

    }

    private String acceptLanguage = "de, en-gb;q=0.9, en;q=0.8";
    private int connectTimeout = -1;
    private URL currentURL;

    private boolean doRedirects = false;

    private HashMap<String, String> headers;

    private int limit = 1 * 1024 * 1024;

    private int readTimeout = -1;

    private Request request;
    private boolean snifferDetection = false;
    private boolean cookiesExclusive = true;
    private JDProxy proxy;
    private static final Authenticator AUTHENTICATOR = new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
            Browser br = Browser.getAssignedBrowserInstance(this.getRequestingURL());
            return br.getPasswordAuthentication(this.getRequestingURL(), this.getRequestingHost(), this.getRequestingPort());

        }
    };

    public Browser() {

        // JDProxy p = new JDProxy(JDProxy.Type.SOCKS, "localhost", 1080);
        // this.setProxy(p);
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_PROXY, false)) {
            //http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html
            // für evtl authentifizierung:
            //http://www.softonaut.com/2008/06/09/using-javanetauthenticator-for
            // -proxy-authentication/
            // nonProxy Liste ist unnötig, da ja eh kein reconnect möglich
            // wäre
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_HOST, "");
            int port = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PROXY_PORT, 8080);
            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS, "");

            JDProxy pr = new JDProxy(Proxy.Type.HTTP, host, port);

            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            this.setProxy(pr);

        }
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {
            //http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html

            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER_SOCKS, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS_SOCKS, "");
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.SOCKS_HOST, "");
            int port = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.SOCKS_PORT, 1080);

            JDProxy pr = new JDProxy(Proxy.Type.SOCKS, host, port);

            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            this.setProxy(pr);
        }

    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public Form[] getForms() {

        return Form.getForms(this);

    }

    /**
     * Returns the first form with an Submitvalue of name
     * 
     * @param name
     * @return
     */
    public Form getFormBySubmitvalue(String name) {
        for (Form f : getForms()) {
            try {
                f.setPreferredSubmit(name);
                return f;
            } catch (IllegalArgumentException e) {

            }
        }
        return null;
    }

    public Form getFormbyProperty(String property, String name) {
        for (Form f : getForms()) {
            if (f.getStringProperty(property) != null && f.getStringProperty(property).equalsIgnoreCase(name)) return f;
        }
        return null;
    }

    public HashMap<String, String> getHeaders() {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        return headers;
    }

    private boolean snifferCheck() throws SnifferException {
        if (!snifferDetection) return false;
        if (Sniffy.hasSniffer()) {
            JDUtilities.getLogger().severe("Sniffer Software detected");
            throw new SnifferException();
        }
        return false;
    }

    public String getPage(String string) throws IOException {

        this.openRequestConnection(this.createGetRequest(string));
        return this.loadConnection(null);

    }

    private void connect(Request request) throws IOException {
        waitForPageAccess();
        assignURLToBrowserInstance(request.getJDPUrl(), this);
        request.connect();
        assignURLToBrowserInstance(request.getHttpConnection().getURL(), null);

    }

    private static void assignURLToBrowserInstance(URL url, Browser browser) {
        if (browser == null) {
            URL_LINK_MAP.remove(url);
        } else {
            URL_LINK_MAP.put(url, browser);
        }

        // System.out.println("NO LINKED: " + URL_LINK_MAP);
    }

    public static URL reAssignUrlToBrowserInstance(URL url1, URL url2) {
        assignURLToBrowserInstance(url2, getAssignedBrowserInstance(url1));
        URL_LINK_MAP.remove(url1);
        return url2;
    }

    /**
     * Returns the Browserinstance that requestst this url connection
     * 
     * @param port
     * @param host
     */
    public static Browser getAssignedBrowserInstance(URL url) {

        return URL_LINK_MAP.get(url);
    }

    /**
     * Assures that the browser does not download any binary files in textmode
     * 
     * @param request
     * @throws BrowserException
     */
    private void checkContentLengthLimit(Request request) throws BrowserException {
        if (request == null || request.getHttpConnection() == null || request.getHttpConnection().getHeaderField("Content-Length") == null) return;
        if (Long.parseLong(request.getHttpConnection().getHeaderField("Content-Length")) > limit) {
            JDUtilities.getLogger().severe(request.printHeaders());
            throw new BrowserException("Content-length too big");
        }
    }

    /**
     * Returns the current readtimeout
     * 
     * @return
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * If automatic redirectfollowing is disabled, you can get the redirect url
     * if there is any.
     * 
     * @return
     */
    public String getRedirectLocation() {
        if (request == null) { return null; }
        return request.getLocation();

    }

    /**
     * Gets the latest request
     * 
     * @return
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Opens a new connection based on a Form
     * 
     * @param form
     * @return
     * @throws Exception
     */
    public URLConnectionAdapter openFormConnection(Form form) throws Exception {

        return this.openRequestConnection(this.createFormRequest(form));
    }

    /**
     * Creates a new Request object based on a form
     * 
     * @param form
     * @return
     * @throws Exception
     */
    public Request createFormRequest(Form form) throws Exception {
        String base = null;
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        if (request != null) base = request.getUrl().toString();
        String action = form.getAction(base);
        switch (form.getMethod()) {

        case GET:

            String varString = form.getPropertyString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return this.createGetRequest(action);

        case POST:
            if (form.getInputFieldByType("file") == null) {

                return this.createPostRequest(action, form.getRequestVariables());
            } else {

                PostFormDataRequest request = (PostFormDataRequest) createPostFormDataRequest(action);
                if (form.getEncoding() != null) {
                    request.setEncodeType(form.getEncoding());
                }

                for (int i = 0; i < form.getInputFields().size(); i++) {
                    InputField entry = form.getInputFields().get(i);

                    if (entry.getValue() == null) continue;
                    if (entry.getType() != null && entry.getType().equalsIgnoreCase("image")) {

                        request.addFormData(new FormData(entry.getKey() + ".x", entry.getIntegerProperty("x", (int) (Math.random() * 100)) + ""));
                        request.addFormData(new FormData(entry.getKey() + ".y", entry.getIntegerProperty("y", (int) (Math.random() * 100)) + ""));

                    } else if (entry.getType() != null && entry.getType().equalsIgnoreCase("file")) {
                        request.addFormData(new FormData(entry.getKey(), entry.getFileToPost().getName(), entry.getFileToPost()));

                    } else if (entry.getKey() != null && entry.getValue() != null) {

                        request.addFormData(new FormData(entry.getKey(), entry.getValue()));

                    }
                }

                return request;
            }

        }
        return null;

    }

    /**
     * Opens a new get connection
     * 
     * @param string
     * @return
     * @throws IOException
     */
    public URLConnectionAdapter openGetConnection(String string) throws IOException {
        return openRequestConnection(this.createGetRequest(string));

    }

    /**
     * Opens a connection based on the requets object
     * 
     */
    public URLConnectionAdapter openRequestConnection(Request request) throws IOException {
        connect(request);
        if (isDebug()) JDUtilities.getLogger().finest("\r\n" + request.printHeaders());

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            this.openGetConnection(null);
        } else {

            currentURL = request.getUrl();
        }
        return this.request.getHttpConnection();
    }

    /**
     * Creates a new Getrequest
     */

    public Request createGetRequest(String string) throws IOException {
        string = getURL(string);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(string);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        GetRequest request = new GetRequest((string));
        if (selectProxy() != null) request.setProxy(selectProxy());
        // doAuth(request);
        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        request.getHeaders().put("Accept-Language", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        forwardCookies(request);
        if (sendref) request.getHeaders().put("Referer", currentURL.toString());
        if (headers != null) {
            mergeHeaders(request);

        }

        // if (this.doRedirects && request.getLocation() != null) {
        // this.openGetConnection(null);
        // } else {
        //
        // currentURL = new URL(string);
        // }
        // return this.request.getHttpConnection();
        return request;
    }

    private void mergeHeaders(Request request) {
        for (Iterator<Entry<String, String>> it = this.headers.entrySet().iterator(); it.hasNext();) {
            Entry<String, String> next = it.next();
            if (next.getValue() == null) {
                request.getHeaders().remove(next.getKey());
            } else {
                request.getHeaders().put(next.getKey(), next.getValue());
            }
        }

    }

    private JDProxy selectProxy() {
        // TODO Auto-generated method stub
        if (proxy != null) return proxy;
        return GLOBAL_PROXY;
    }

    private Request createGetRequestfromOldRequest(Request oldrequest) throws IOException {
        String string = getURL(oldrequest.getLocation());
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(string);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        GetRequest request = new GetRequest((string));
        if (selectProxy() != null) request.setProxy(selectProxy());
        request.setCookies(oldrequest.getCookies());
        // doAuth(request);
        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        request.getHeaders().put("Accept-Language", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        forwardCookies(request);
        if (sendref) request.getHeaders().put("Referer", currentURL.toString());
        if (headers != null) {
            mergeHeaders(request);
        }

        // if (this.doRedirects && request.getLocation() != null) {
        // this.openGetConnection(null);
        // } else {
        //
        // currentURL = new URL(string);
        // }
        // return this.request.getHttpConnection();
        return request;
    }

    /**
     * TRies to get a fuill url out of string
     */
    private String getURL(String string) {
        if (string == null) string = this.getRedirectLocation();
        if (string == null) return null;
        try {
            new URL(string);
        } catch (Exception e) {
            if (request == null || request.getHttpConnection() == null) return string;
            String path = request.getHttpConnection().getURL().getPath();
            if (string.startsWith("/") || string.startsWith("\\")) return "http://" + request.getHttpConnection().getURL().getHost() + string;
            int id;
            if ((id = path.lastIndexOf("/")) >= 0) {
                path = path.substring(0, id);
            }
            if (path.trim().length() == 0) path = "/";

            // path.substring(path.lastIndexOf("/"))
            string = "http://" + request.getHttpConnection().getURL().getHost() + path + "/" + string;
        }
        return Encoding.urlEncode_light(string);
    }

    /**
     * Opens a Post COnnection based on a variable hashmap
     */
    public URLConnectionAdapter openPostConnection(String url, HashMap<String, String> post) throws IOException {

        return this.openRequestConnection(this.createPostRequest(url, post));

    }

    private Request createPostRequestfromOldRequest(Request oldrequest, String postdata) throws IOException {
        String url = getURL(oldrequest.getLocation());
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        HashMap<String, String> post = Request.parseQuery(postdata);
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest((url));
        if (selectProxy() != null) request.setProxy(selectProxy());
        request.setCookies(oldrequest.getCookies());
        // doAuth(request);
        request.getHeaders().put("Accept-Language", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        forwardCookies(request);
        if (sendref) request.getHeaders().put("Referer", currentURL.toString());
        if (post != null) {
            request.addAll(post);
        }
        if (headers != null) {
            mergeHeaders(request);
        }
        return request;

    }

    public Request createPostFormDataRequest(String url) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostFormDataRequest request = new PostFormDataRequest((url));
        if (selectProxy() != null) request.setProxy(selectProxy());

        request.getHeaders().put("Accept-Language", acceptLanguage);

        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        forwardCookies(request);
        if (sendref) request.getHeaders().put("Referer", currentURL.toString());

        if (headers != null) {
            mergeHeaders(request);
        }
        return request;
    }

    /**
     * Creates a new POstrequest based on a variable hashmap
     */
    public Request createPostRequest(String url, HashMap<String, String> post) throws IOException {

        return this.createPostRequest(url, PostRequest.variableMaptoArray(post));
    }

    /**
     * Creates a new postrequest based an an requestVariable Arraylist
     */
    private Request createPostRequest(String url, ArrayList<RequestVariable> post) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest((url));
        if (selectProxy() != null) request.setProxy(selectProxy());
        // doAuth(request);
        request.getHeaders().put("Accept-Language", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        forwardCookies(request);
        if (sendref) request.getHeaders().put("Referer", currentURL.toString());
        if (post != null) {
            request.addAll(post);

        }
        if (headers != null) {
            mergeHeaders(request);
        }
        return request;
    }

    /**
     * Creates a postrequest based on a querystring
     */
    public Request createPostRequest(String url, String post) throws MalformedURLException, IOException {

        return createPostRequest(url, Request.parseQuery(post));
    }

    /**
     * OPens a new POst connection based on a query string
     */
    public URLConnectionAdapter openPostConnection(String url, String post) throws IOException {

        return openPostConnection(url, Request.parseQuery(post));
    }

    /**
     * loads a new page (post)
     */
    public String postPage(String url, HashMap<String, String> post) throws IOException {
        openPostConnection(url, post);
        return loadConnection(null);

    }

    /**
     * Returns a new Javscriptobject for the current loaded page
     */
    public JavaScript getJavaScript() {
        return new JavaScript(this);
    }

    /**
     * loads a new page (POST)
     */
    public String postPage(String url, String post) throws IOException {

        return postPage(url, Request.parseQuery(post));
    }

    /**
     * loads a new page (post) the postdata is given by the poststring. it wiull
     * be send as it is
     */
    public String postPageRaw(String url, String post) throws IOException {
        PostRequest request = (PostRequest) this.createPostRequest(url, new ArrayList<RequestVariable>());
        if (post != null) request.setPostDataString(post);
        this.openRequestConnection(request);
        return this.loadConnection(null);
    }

    public void setAcceptLanguage(String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setCurrentURL(String string) throws MalformedURLException {

        currentURL = new URL(string);

    }

    public void setFollowRedirects(boolean b) {
        doRedirects = b;

    }

    public boolean isFollowingRedirects() {
        return doRedirects;
    }

    public void setHeaders(HashMap<String, String> h) {
        headers = h;

    }

    public void setLoadLimit(int i) {
        limit = i;

    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getBaseURL() {
        if (request == null) return null;

        String base = request.getUrl().toString();
        if (base.matches("http://.*/.*")) {
            return base.substring(0, base.lastIndexOf("/")) + "/";
        } else {
            return base + "/";
        }

    }

    public String submitForm(Form form) throws Exception {

        this.openFormConnection(form);

        checkContentLengthLimit(request);
        return request.read();
    }

    @Override
    public String toString() {
        if (request == null) { return "Browser. no request yet"; }
        return request.toString();
    }

    public Regex getRegex(String string) {
        return new Regex(this, string);
    }

    public Regex getRegex(Pattern compile) {
        return new Regex(this, compile);
    }

    public boolean containsHTML(String regex) {
        return new Regex(this, regex).matches();
    }

    /**
     * Reads teh contents behind con and returns them. Note: if con==null, the
     * current request is read. This is usefull for redirects. Note #2: if a
     * connection is loaded, data is not stored in the browser instance.
     * 
     * @param con
     * @return
     * @throws IOException
     */
    public String loadConnection(URLConnectionAdapter con) throws IOException {

        if (con == null) {
            checkContentLengthLimit(request);
            return request.read();
        }
        return Request.read(con);

    }

    public URLConnectionAdapter getHttpConnection() {
        if (request == null) return null;
        return request.getHttpConnection();
    }

    /**
     * Lädt über eine URLConnection eine Datei herunter. Zieldatei ist file.
     * 
     * @param file
     * @param con
     * @return Erfolg true/false
     * @throws IOException
     */
    public static void download(File file, URLConnectionAdapter con) throws IOException {

        if (file.isFile()) {
            if (!file.delete()) {
                System.out.println("Konnte Datei nicht löschen " + file);
                throw new IOException("Could not overwrite file: " + file);
            }

        }

        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();

        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));

        BufferedInputStream input = new BufferedInputStream(con.getInputStream());

        byte[] b = new byte[1024];
        int len;
        while ((len = input.read(b)) != -1) {
            output.write(b, 0, len);
        }
        output.close();
        input.close();

    }

    public void getDownload(File file, String urlString) throws IOException {

        urlString = URLDecoder.decode(urlString, "UTF-8");

        URLConnectionAdapter con = this.openGetConnection(urlString);
        con.setInstanceFollowRedirects(true);
        download(file, con);

    }

    /**
     * Downloads the contents behind con to file. if(con ==null), the latest
     * request is downloaded. Usefull for redirects
     * 
     * @param file
     * @param con
     * @throws IOException
     */
    public void downloadConnection(File file, URLConnectionAdapter con) throws IOException {
        if (con == null) con = request.getHttpConnection();
        con.setInstanceFollowRedirects(true);
        download(file, con);

    }

    /**
     * Downloads url to file.
     * 
     * @param file
     * @param urlString
     * @return Erfolg true/false
     * @throws IOException
     */
    public static void download(File file, String url) throws IOException {

        new Browser().getDownload(file, url);

    }

    public Form getForm(int i) {
        Form[] forms = getForms();
        if (forms.length <= i) return null;
        return forms[i];
    }

    public String getHost() {
        if (request == null) return null;
        return request.getUrl().getHost();

    }

    public Browser cloneBrowser() {
        Browser br = new Browser();
        br.exclusiveReqTimeCtrl = exclusiveReqTimeCtrl;
        br.waittimeBetweenRequests = waittimeBetweenRequests;
        br.latestRequestTimes = latestRequestTimes;
        br.latestReqTimeCtrlID = latestReqTimeCtrlID;
        br.acceptLanguage = acceptLanguage;
        br.connectTimeout = connectTimeout;
        br.currentURL = currentURL;
        br.doRedirects = doRedirects;
        br.getHeaders().putAll(getHeaders());
        br.limit = limit;
        br.readTimeout = readTimeout;
        br.request = request;
        br.cookies = cookies;
        br.logins = new HashMap<String, String[]>();
        br.logins.putAll(logins);
        br.cookiesExclusive = cookiesExclusive;
        br.debug = debug;
        br.proxy = proxy;
        return br;
    }

    public Form[] getForms(String downloadURL) throws IOException {
        this.getPage(downloadURL);
        return this.getForms();

    }

    public URLConnectionAdapter openFormConnection(int i) throws Exception {
        return openFormConnection(getForm(i));

    }

    public String getMatch(String string) {
        return getRegex(string).getMatch(0);

    }

    public boolean isSnifferDetection() {
        return snifferDetection;
    }

    public void setSnifferDetection(boolean snifferDetection) {
        this.snifferDetection = snifferDetection;
    }

    public String getURL() {
        if (request == null) { return null; }
        return request.getUrl().toString();

    }

    public void setCookiesExclusive(boolean b) {
        if (cookiesExclusive == b) return;
        this.cookiesExclusive = b;
        if (b) {
            this.cookies.clear();

            for (Iterator<Entry<String, HashMap<String, Cookie>>> it = COOKIES.entrySet().iterator(); it.hasNext();) {
                Entry<String, HashMap<String, Cookie>> next = it.next();
                HashMap<String, Cookie> tmp;
                cookies.put(next.getKey(), tmp = new HashMap<String, Cookie>());
                tmp.putAll(next.getValue());

            }

        } else {
            this.cookies.clear();
        }

    }

    public boolean isCookiesExclusive() {
        return cookiesExclusive;
    }

    public String followConnection() throws IOException {
        String ret = null;

        if (request.getHtmlCode() != null) {
            JDUtilities.getLogger().warning("Request has already been read");
            return null;
        }
        checkContentLengthLimit(request);

        ret = request.read();

        return ret;

    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        if (JDUtilities.getRunType() != JDUtilities.RUNTYPE_LOCAL_JARED) {
            this.debug = debug;
        }
    }

    public void setAuth(String domain, String user, String pass) {
        domain = domain.trim();
        if (domain.indexOf(":") <= 0) {
            domain += ":80";
        }
        logins.put(domain, new String[] { user, pass });
    }

    public String[] getAuth(String domain) {
        domain = domain.trim();
        if (domain.indexOf(":") <= 0) {
            domain += ":80";
        }
        String[] ret = logins.get(domain);
        if (ret == null) {
            // see proxy auth

            if ((selectProxy().getHost() + ":" + selectProxy().getPort()).equalsIgnoreCase(domain)) {
                ret = new String[] { selectProxy().getUser(), selectProxy().getPass() };
            }
        }

        return ret;
    }

    public String submitForm(String formname) throws Exception {
        return this.submitForm(getFormBySubmitvalue(formname));

    }

    public String getPage(URL url) throws IOException {
        return getPage(url + "");

    }

    public void setRequest(Request request) throws MalformedURLException {
        if (request == null) return;
        updateCookies(request);
        this.request = request;

        currentURL = request.getUrl();

    }

    public Request createRequest(Form form) throws Exception {
        return createFormRequest(form);
    }

    public Request createRequest(String downloadURL) throws Exception {

        // TODO Auto-generated method stub
        return createGetRequest(downloadURL);
    }

    public String getXPathElement(String xPath) {
        return new XPath(this.toString(), xPath).getFirstMatch();

    }

    public DownloadInterface openDownload(DownloadLink downloadLink, String link) throws Exception {

        DownloadInterface dl = RAFDownload.download(downloadLink, this.createGetRequest(link));
        try {
            dl.connect(this);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, this.createGetRequestfromOldRequest(dl.getRequest()));
                    try {
                        dl.connect(this);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }

            }
        }
        if (downloadLink.getPlugin().getBrowser() == this) {
            downloadLink.getPlugin().setDownloadInterface(dl);
        }
        return dl;
    }

    public DownloadInterface openDownload(DownloadLink downloadLink, String url, String postdata) throws MalformedURLException, IOException, Exception {
        DownloadInterface dl = RAFDownload.download(downloadLink, this.createPostRequest(url, postdata));
        try {
            dl.connect(this);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, this.createPostRequestfromOldRequest(dl.getRequest(), postdata));
                    try {
                        dl.connect(this);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }

            }
        }
        if (downloadLink.getPlugin().getBrowser() == this) {
            downloadLink.getPlugin().setDownloadInterface(dl);
        }
        return dl;
    }

    public DownloadInterface openDownload(DownloadLink downloadLink, String link, boolean b, int c) throws Exception {

        DownloadInterface dl = RAFDownload.download(downloadLink, this.createRequest(link), b, c);
        try {
            dl.connect(this);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, this.createGetRequestfromOldRequest(dl.getRequest()), b, c);
                    try {
                        dl.connect(this);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }

            }
        }
        if (downloadLink.getPlugin().getBrowser() == this) {
            downloadLink.getPlugin().setDownloadInterface(dl);
        }
        return dl;
    }

    public DownloadInterface openDownload(DownloadLink downloadLink, Form form, boolean resume, int chunks) throws Exception {

        DownloadInterface dl = RAFDownload.download(downloadLink, this.createRequest(form), resume, chunks);
        try {
            dl.connect(this);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, this.createGetRequestfromOldRequest(dl.getRequest()), resume, chunks);
                    try {
                        dl.connect(this);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }

            }
        }
        if (downloadLink.getPlugin().getBrowser() == this) {
            downloadLink.getPlugin().setDownloadInterface(dl);
        }
        return dl;
    }

    public DownloadInterface openDownload(DownloadLink downloadLink, Form form) throws Exception {

        DownloadInterface dl = RAFDownload.download(downloadLink, this.createRequest(form));
        try {
            dl.connect(this);
        } catch (PluginException e) {
            if (e.getValue() == DownloadInterface.ERROR_REDIRECTED) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = RAFDownload.download(downloadLink, this.createGetRequestfromOldRequest(dl.getRequest()));
                    try {
                        dl.connect(this);
                        break;
                    } catch (PluginException e2) {
                        continue;
                    }
                }

            }
        }
        if (downloadLink.getPlugin().getBrowser() == this) {
            downloadLink.getPlugin().setDownloadInterface(dl);
        }
        return dl;
    }

    public void setProxy(JDProxy proxy) {
        if (proxy == null) {
            System.err.println("Browser:No proxy");
            this.proxy = null;
            return;
        }
        // System.err.println("Browser: "+proxy);
        // this.setAuth(proxy.getHost() + ":" + proxy.getPort(),
        // proxy.getUser(), proxy.getPass());
        this.proxy = proxy;
    }

    public JDProxy getProxy() {
        return proxy;
    }

    /**
     * Zeigt debuginformationen auch im Hauptprogramm an
     * 
     * @param b
     */
    public void forceDebug(boolean b) {
        this.debug = b;

    }

    /**
     * Returns the Password Authentication if there are auth set for the given
     * url
     * 
     * @param url
     * @param port
     * @param host
     * @return
     */
    public PasswordAuthentication getPasswordAuthentication(URL url, String host, int port) {
        if (port <= 0) port = 80;
        String[] auth = this.getAuth(host + ":" + port);
        if (auth == null) return null;
        JDUtilities.getLogger().finest("Use Authentication for: " + host + ":" + port + ": " + auth[0] + " - " + auth[1]);
        return new PasswordAuthentication(auth[0], auth[1].toCharArray());
    }

    public static void init() {
        Authenticator.setDefault(AUTHENTICATOR);
        CookieHandler.setDefault(null);
        XTrustProvider.install();

    }

}
