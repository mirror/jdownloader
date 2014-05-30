package org.jdownloader.scripting.envjs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.logging.LogController;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;

public class EnvJS {
    private Context                                              cx;
    private Global                                               scope;
    private long                                                 id;
    private Browser                                              br;
    private LogSource                                            logger;
    private static AtomicLong                                    CCOUNTER  = new AtomicLong(0l);
    private static ConcurrentHashMap<Long, WeakReference<EnvJS>> INSTANCES = new ConcurrentHashMap<Long, WeakReference<EnvJS>>();

    public static EnvJS get(long id) {

        return INSTANCES.get(id).get();
    }

    public String getCookieStringByUrl(String url) {
        return Request.getCookieString(br.getCookies(url));
    }

    public void log(String message) {
        logger.info("console.log >" + message);
    }

    public void setCookie(String url, String cookie) {

        final String date = br.getRequest().getHttpConnection().getHeaderField("Date");
        final String host = Browser.getHost(br.getRequest().getHttpConnection().getURL());

        Cookies cookies = Cookies.parseCookies(cookie, host, date);
        br.getCookies(host).add(cookies);
    }

    public String readFromFile(String url) {
        return url;
    }

    public String xhrRequest(String url, String method, String data, String requestHeaders) throws IOException {

        HashMap<String, String> headers = JSonStorage.restoreFromString(requestHeaders, new TypeRef<HashMap<String, String>>() {
        });
        br.getHeaders().put(new HTTPHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36"));
        if (headers != null) {
            br.getHeaders().putAll(headers);
        }
        if ("get".equalsIgnoreCase(method)) {
            br.getPage(url);
        }
        XHRResponse ret = new XHRResponse();
        for (Entry<String, List<String>> s : br.getRequest().getResponseHeaders().entrySet()) {
            ret.getResponseHeader().put(s.getKey(), s.getValue().get(0));
        }
        ret.setEncoding(br.getRequest().getHttpConnection().getHeaderField("Content-Encoding"));
        ret.setReponseMessage(br.getRequest().getHttpConnection().getResponseMessage());
        ret.setResponseCode(br.getRequest().getHttpConnection().getResponseCode());
        ret.setResponseText(br.getRequest().getHtmlCode());

        return JSonStorage.serializeToJson(ret);
    }

    public EnvJS() {
        id = CCOUNTER.incrementAndGet();
        INSTANCES.put(id, new WeakReference<EnvJS>(this));
        logger = LogController.getInstance().getLogger(EnvJS.class.getName());
    }

    public void breakIt() {
        System.out.println("Break");
    }

    public void require(String path) {
        System.out.println();
    }

    public void init() {
        cx = Context.enter();

        scope = new Global(cx);
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_1_5);
        br = new Browser();

        // org.mozilla.javascript.EcmaError: ReferenceError: "JSON" is not defined. (js#508) exceptions in the log are ok.
        try {
            jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, "var EnvJSinstanceID=" + id + ";", "js", 1, null);
            jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, IO.readURLToString(EnvJS.class.getResource("rhino.js")), "js", 1, null);
            jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, IO.readURLToString(EnvJS.class.getResource("window.js")), "js", 1, null);
            jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, IO.readURLToString(EnvJS.class.getResource("platform/rhino.js")), "js", 1, null);

            // jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope,
            // "Envjs.scriptTypes[\"text/javascript\"] = {\"text/javascript\"   :true,   \"text/envjs\"        :true};", "js", 1, null);
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    public void eval(String js) {

        // cx.evaluateString(scope, js, "js", 1, null);
        jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, js, "js", 1, null);
    }

    public void setDocument(String url, String html) {

        String js = " document.async=true; document.baseURI = '" + url + "';HTMLParser.parseDocument(\"" + html.replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\", document);Envjs.wait();";
        jd.http.ext.security.JSPermissionRestricter.evaluateTrustedString(cx, scope, js, "js", 1, null);
    }

    public String getDocument() {
        Object result = cx.evaluateString(scope, "var f=function(){return document.innerHTML;}; f();", "js", 1, null);
        if (result != null) {
            return result + "";
        }
        return null;
    }
}
