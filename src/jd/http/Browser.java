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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.JavaScript;
import jd.parser.Regex;
import jd.parser.XPath;
import jd.parser.Form.InputField;
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

    private static HashMap<String, HashMap<String, Cookie>> COOKIES = new HashMap<String, HashMap<String, Cookie>>();
    private HashMap<String, HashMap<String, Cookie>> cookies = new HashMap<String, HashMap<String, Cookie>>();
    private static HashMap<String, Auth> AUTHS = new HashMap<String, Auth>();
    private HashMap<String, Auth> auths = new HashMap<String, Auth>();
    private boolean debug = false;

    private static HashMap<String, Long> LATEST_PAGE_REQUESTS = new HashMap<String, Long>();
    private HashMap<String, Long> latestRequestTimes = new HashMap<String, Long>();
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

    public void forwardCookies(HTTPConnection con) throws MalformedURLException {
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

    public Browser() {

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

    public Form getFormbyValue(String name) {
        for (Form f : getForms()) {
            if (f.hasSubmitValue(name)) return f;
        }
        return null;
    }

    public Form getFormbyName(String name) {
        for (Form f : getForms()) {
            if (f.formProperties.get("name") != null && f.formProperties.get("name").equals(name)) return f;
        }
        return null;
    }

    public Form getFormbyID(String id) {
        for (Form f : getForms()) {
            if (f.formProperties.get("id") != null && f.formProperties.get("id").equals(id)) return f;
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
        string = getURL(string);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(string);
        }

        if (snifferCheck()) {
            // throw new SnifferException();
        }
        GetRequest request = new GetRequest(string);
        if (proxy != null) request.setProxy(proxy);
        request.getHeaders().put("Accept-Language", acceptLanguage);
        doAuth(request);
        // request.setFollowRedirects(doRedirects);
        forwardCookies(request);
        if (sendref) request.getHeaders().put("Referer", currentURL.toString());
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }
        waitForPageAccess();
        request.connect();
        if (isDebug()) JDUtilities.getLogger().finest("\r\n" + request.printHeaders());
        String ret = null;

        checkContentLengthLimit(request);
        ret = request.read();

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            ret = this.getPage((String) null);
        } else {

            currentURL = new URL(string);
        }
        return ret;

    }

    private void doAuth(Request request) {
        String host = request.getUrl().getHost();
        if (cookiesExclusive) {
            if (auths.containsKey(host)) {
                request.getHeaders().put("Authorization", auths.get(host).getAuthHeader());
            }
            if (auths.containsKey(null)) {
                request.getHeaders().put("Authorization", auths.get(null).getAuthHeader());
            }
        } else {
            if (AUTHS.containsKey(host)) {
                request.getHeaders().put("Authorization", AUTHS.get(host).getAuthHeader());
            }

        }

    }

    private void checkContentLengthLimit(Request request) throws BrowserException {
        if (request == null || request.getHttpConnection() == null || request.getHttpConnection().getHeaderField("Content-Length") == null) return;
        if (Integer.parseInt(request.getHttpConnection().getHeaderField("Content-Length")) > limit) throw new BrowserException("Content-length too big");
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public String getRedirectLocation() {
        if (request == null) { return null; }
        return request.getLocation();

    }

    public Request getRequest() {

        return request;
    }

    public HTTPConnection openFormConnection(Form form) throws IOException {
        if (form == null) return null;
        String base = null;
        if (request != null) base = request.getUrl().toString();
        String action = form.getAction(base);
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuilder stbuffer = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, InputField> entry : form.getVars().entrySet()) {
                if (entry.getKey() != null) {
                    if (first) {
                        first = false;
                    } else {
                        stbuffer.append("&");
                    }
                    stbuffer.append(entry.getKey());
                    stbuffer.append("=");
                    if (entry.getValue().getValue() != null) {
                        stbuffer.append(entry.getValue().getValue());
                    } else {
                        stbuffer.append("");
                    }
                }
            }
            String varString = stbuffer.toString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return openGetConnection(action);

        case Form.METHOD_POST:

            return this.openPostConnection(action, form.getVarsMap());
        }
        return null;

    }

    public Request createFormRequest(Form form) throws Exception {
        if (form == null) return null;
        String base = null;
        if (request != null) base = request.getUrl().toString();
        String action = form.getAction(base);
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuilder stbuffer = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, InputField> entry : form.getVars().entrySet()) {
                if (entry.getKey() != null) {
                    if (first) {
                        first = false;
                    } else {
                        stbuffer.append("&");
                    }
                    stbuffer.append(entry.getKey());
                    stbuffer.append("=");
                    if (entry.getValue().getValue() != null) {
                        stbuffer.append(entry.getValue().getValue());
                    } else {
                        stbuffer.append("");
                    }
                }
            }
            String varString = stbuffer.toString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return createGetRequest(action);

        case Form.METHOD_POST:

            return createPostRequest(action, form.getVarsMap());
        }
        return null;

    }

    public HTTPConnection openGetConnection(String string) throws IOException {
        string = getURL(string);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(string);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        GetRequest request = new GetRequest(string);
        if (proxy != null) request.setProxy(proxy);
        doAuth(request);
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
            request.getHeaders().putAll(headers);
        }
        waitForPageAccess();
        request.connect();
        if (isDebug()) JDUtilities.getLogger().finest("\r\n" + request.printHeaders());

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            this.openGetConnection(null);
        } else {

            currentURL = new URL(string);
        }
        return this.request.getHttpConnection();

    }

    public Request createGetRequest(String string) throws Exception {
        string = getURL(string);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(string);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        GetRequest request = new GetRequest(string);
        if (proxy != null) request.setProxy(proxy);
        doAuth(request);
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
            request.getHeaders().putAll(headers);
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

    public Request createGetRequestfromOldRequest(Request oldrequest) throws Exception {
        String string = getURL(oldrequest.getLocation());
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(string);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        GetRequest request = new GetRequest(string);
        if (proxy != null) request.setProxy(proxy);
        request.setCookies(oldrequest.getCookies());
        doAuth(request);
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
            request.getHeaders().putAll(headers);
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

    private HTTPConnection openPostConnection(String url, HashMap<String, String> post) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest(url);
        if (proxy != null) request.setProxy(proxy);
        doAuth(request);
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
            request.getPostData().putAll(post);
        }
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }
        waitForPageAccess();
        request.connect();
        if (isDebug()) JDUtilities.getLogger().finest("\r\n" + request.printHeaders());
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            this.openGetConnection(null);
        } else {

            currentURL = new URL(url);
        }
        return this.request.getHttpConnection();

    }

    public Request createPostRequestfromOldRequest(Request oldrequest, String postdata) throws IOException {
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
        PostRequest request = new PostRequest(url);
        if (proxy != null) request.setProxy(proxy);
        request.setCookies(oldrequest.getCookies());
        doAuth(request);
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
            request.getPostData().putAll(post);
        }
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }
        return request;

    }

    public Request createPostRequest(String url, HashMap<String, String> post) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest(url);
        if (proxy != null) request.setProxy(proxy);
        doAuth(request);
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
            request.getPostData().putAll(post);
        }
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }
        return request;

    }

    public Request createPostRequest(String url, String post) throws MalformedURLException, IOException {

        return createPostRequest(url, Request.parseQuery(post));
    }

    public HTTPConnection openPostConnection(String url, String post) throws IOException {

        return openPostConnection(url, Request.parseQuery(post));
    }

    public String postPage(String url, HashMap<String, String> post) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest(url);
        if (proxy != null) request.setProxy(proxy);
        doAuth(request);
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
        if (post != null) request.getPostData().putAll(post);
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }

        String ret = null;
        waitForPageAccess();
        request.connect();
        if (isDebug()) JDUtilities.getLogger().finest("\r\n" + request.printHeaders());
        checkContentLengthLimit(request);
        ret = request.read();

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            ret = this.getPage((String) null);
        } else {

            currentURL = new URL(url);
        }
        return ret;

    }

    public JavaScript getJavaScript() {
        return new JavaScript(this);
    }

    public String postPage(String url, String post) throws IOException {

        return postPage(url, Request.parseQuery(post));
    }

    public String postPageRaw(String url, String post) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest(url);
        if (proxy != null) request.setProxy(proxy);
        doAuth(request);
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
        if (post != null) request.setPostDataString(post);
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }

        String ret = null;
        waitForPageAccess();
        request.connect();
        if (isDebug()) JDUtilities.getLogger().finest("\r\n" + request.printHeaders());
        checkContentLengthLimit(request);
        ret = request.read();

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            ret = this.getPage((String) null);
        } else {

            currentURL = new URL(url);
        }
        return ret;
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

    public String submitForm(Form form) throws IOException {
        String base = null;
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        if (request != null) base = request.getUrl().toString();
        String action = form.getAction(base);
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuilder stbuffer = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, InputField> entry : form.getVars().entrySet()) {
                if (entry.getKey() != null) {
                    if (first) {
                        first = false;
                    } else {
                        stbuffer.append("&");
                    }
                    stbuffer.append(entry.getKey());
                    stbuffer.append("=");
                    if (entry.getValue().getValue() != null) {
                        stbuffer.append(entry.getValue().getValue());
                    } else {
                        stbuffer.append("");
                    }
                }
            }
            String varString = stbuffer.toString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return this.getPage(action);

        case Form.METHOD_POST:

            return this.postPage(action, form.getVarsMap());

        case Form.METHOD_FILEPOST:

            HTTPPost up = new HTTPPost(action, doRedirects);
            if (proxy != null) up.setProxy(proxy);
            up.doUpload();

            up.getConnection().setRequestProperty("Accept", "*/*");
            up.getConnection().setRequestProperty("Accept-Language", acceptLanguage);
            up.getConnection().setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4");
            forwardCookies(up.getConnection());
            up.getConnection().setRequestProperty("Referer", currentURL.toString());
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    up.getConnection().setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            up.connect();
            for (Map.Entry<String, InputField> entry : form.getVars().entrySet()) {
                if (entry.getValue().getValue() != null) {
                    up.sendVariable(entry.getKey(), entry.getValue().getValue());
                }
            }
            up.setForm("filecontent");
            up.sendFile(form.getFileToPost().toString(), form.getFiletoPostName());
            up.close();
            // Dummy request um das ganze kompatibel zu machen
            Request request = new Request(up.getConnection()) {

                @Override
                public void postRequest(HTTPConnection httpConnection) throws IOException {

                }

                @Override
                public void preRequest(HTTPConnection httpConnection) throws IOException {

                }

            };
            if (proxy != null) request.setProxy(proxy);
            if (request.getHeaders() != null && headers != null) {
                request.getHeaders().putAll(headers);
            }
            if (request.getHeaders() != null) {
                request.getHeaders().put("Accept-Language", acceptLanguage);
            }

            request.setFollowRedirects(doRedirects);
            forwardCookies(request);
            if (request.getHeaders() != null) {
                request.getHeaders().put("Referer", currentURL.toString());
            }
            String ret = null;
            checkContentLengthLimit(request);
            ret = request.read();
            request.setHtmlCode(ret);
            updateCookies(request);
            this.request = request;

            currentURL = new URL(action);

            return ret;

        }
        return null;
    }

    @Override
    public String toString() {
        if (request == null) { return "Browser. no request yet"; }
        return request.toString();
    }

    /**
     * @return Use Browser.toString instead.
     * @deprecated replaced by <code>Browser.toString</code>.
     * @see #toString()
     */
    @Deprecated
    public String GetHtmlCode() {
        return this.toString();
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

    public String loadConnection(HTTPConnection con) throws IOException {
        checkContentLengthLimit(request);
        if (con == null) return request.read();
        return Request.read(con);

    }

    public HTTPConnection getHttpConnection() {
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
    public static void download(File file, HTTPConnection con) throws IOException {

        if (file.isFile()) {
            if (!file.delete()) {
                System.out.println("Konnte Datei nicht überschreiben " + file);
                throw new IOException("Could not overwrite file: " + file);
            }
        }
        if (!file.getParentFile().exists()) {
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

    public static void downloadBinary(String filepath, String fileurl) throws IOException {
        fileurl = Encoding.urlEncode_light(fileurl.replaceAll("\\\\", "/"));
        File file = new File(filepath);
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
        fileurl = URLDecoder.decode(fileurl, "UTF-8");

        URL url = new URL(fileurl);
        HTTPConnection con = new HTTPConnection(url.openConnection());

        BufferedInputStream input = new BufferedInputStream(con.getInputStream());

        byte[] b = new byte[1024];
        int len;
        while ((len = input.read(b)) != -1) {
            output.write(b, 0, len);
        }
        output.close();
        input.close();

    }

    public void downloadFile(File file, String urlString) throws IOException {

        urlString = URLDecoder.decode(urlString, "UTF-8");

        HTTPConnection con = this.openGetConnection(urlString);
        con.setInstanceFollowRedirects(true);
        Browser.download(file, con);

    }

    /**
     * Lädt eine url lokal herunter
     * 
     * @param file
     * @param urlString
     * @return Erfolg true/false
     * @throws IOException
     */
    public static void download(File file, String urlString) throws IOException {

        urlString = URLDecoder.decode(urlString, "UTF-8");
        URL url = new URL(urlString);
        HTTPConnection con = new HTTPConnection(url.openConnection());
        con.setInstanceFollowRedirects(true);
        Browser.download(file, con);

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
        br.auths = auths;
        br.cookiesExclusive = cookiesExclusive;
        br.debug = debug;
        return br;
    }

    public Form[] getForms(String downloadURL) throws IOException {
        this.getPage(downloadURL);
        return this.getForms();

    }

    public HTTPConnection openFormConnection(int i) throws IOException {
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
            this.auths.clear();

            for (Iterator<Entry<String, HashMap<String, Cookie>>> it = COOKIES.entrySet().iterator(); it.hasNext();) {
                Entry<String, HashMap<String, Cookie>> next = it.next();
                HashMap<String, Cookie> tmp;
                cookies.put(next.getKey(), tmp = new HashMap<String, Cookie>());
                tmp.putAll(next.getValue());

            }

            auths.putAll(AUTHS);

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

        HashMap<String, Auth> auths = this.cookiesExclusive ? this.auths : AUTHS;
        if (user == null && pass == null) {
            auths.remove(domain);
        }
        Auth auth = new Auth(domain, user, pass);
        auths.put(domain, auth);
    }

    public String submitForm(String formname) throws IOException {
        return this.submitForm(getFormbyValue(formname));

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

    public String getXPathElement(String xPath) {
        return new XPath(this.toString(), xPath).getFirstMatch();

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

    public void setProxy(JDProxy proxy) {
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

}
