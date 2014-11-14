package org.jdownloader.scripting.envjs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.EcmaError;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.logging.LogController;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;

public class EnvJS {
    static {
        try {
            JSHtmlUnitPermissionRestricter.init();
        } catch (IllegalStateException e) {

            // already set globally
        }
    }
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

    // if(script.type){
    // types = script.type.split(";");
    // for(i=0;i<types.length;i++){
    // if(Envjs.scriptTypes[types[i].toLowerCase()]){
    // //ok this script type is allowed
    // break;
    // }
    // if(i+1 == types.length){
    // log.debug('wont load script type %s', script.type);
    // return false;
    // }
    // }
    // }else if(!Envjs.scriptTypes['']){
    // log.debug('wont load anonymous script type ""');
    // return false;
    // }
    //
    // try{
    // if(!script.src.length ){
    // if(Envjs.scriptTypes[""]){
    // log.debug('handling inline scripts %s %s', script.src, Envjs.scriptTypes[""] );
    // Envjs.loadInlineScript(script);
    // return true;
    // }else{
    // return false;
    // }
    // }
    // }catch(e){
    // log.error("Error loading script. %s", e);
    // Envjs.onScriptLoadError(script, e);
    // return false;
    // }
    //
    //
    // log.debug("loading allowed external script %s", script.src);
    //
    // //lets you register a function to execute
    // //before the script is loaded
    // if(Envjs.beforeScriptLoad){
    // for(src in Envjs.beforeScriptLoad){
    // if(script.src.match(src)){
    // Envjs.beforeScriptLoad[src](script);
    // }
    // }
    // }
    // base = "" + script.ownerDocument.location;
    // //filename = Envjs.uri(script.src.match(/([^\?#]*)/)[1], base );
    // log.debug('loading script from base %s', base);
    // filename = Envjs.uri(script.src, base);
    // try {
    // xhr = new XMLHttpRequest();
    // xhr.open("GET", filename, false/*syncronous*/);
    // log.debug("loading external script %s", filename);
    // xhr.onreadystatechange = function(){
    // log.debug("readyState %s", xhr.readyState);
    // if(xhr.readyState === 4){
    // Envjs['eval'](
    // script.ownerDocument.ownerWindow,
    // xhr.responseText,
    // filename
    // );
    // }
    // log.debug('finished evaluating %s', filename);
    // };
    // xhr.send(null, false);
    // } catch(ee) {
    // log.error("could not load script %s \n %s", filename, ee );
    // Envjs.onScriptLoadError(script, ee);
    // return false;
    // }
    // //lets you register a function to execute
    // //after the script is loaded
    // if(Envjs.afterScriptLoad){
    // for(src in Envjs.afterScriptLoad){
    // if(script.src.match(src)){
    // Envjs.afterScriptLoad[src](script);
    // }
    // }
    // }
    public String modifyInlineScript(String type, String js) throws IOException {
        return js;
    }

    public Boolean preInitBoolean(Boolean b, boolean b2) {
        return true;
    }

    public Integer preInitInteger(Integer b, int b2) {
        return 0;
    }

    public Long preInitLong(Long b, long b2) {
        return 0l;
    }

    public Double preInitDouble(Double b, double b2) {
        return 0d;
    }

    public Float preInitFloat(Float b, float b2) {
        return 0f;
    }

    public String loadExternalScript(String type, String src, String url, Object window) {

        String ret = "console.log(\"Script Blocked: " + url + "\");";
        return ret;
    }

    private HashMap<Object, Object> toMap(NativeObject obj, HashMap<Object, HashMap<Object, Object>> refMap) {
        HashMap<Object, Object> ret = new HashMap<Object, Object>();
        if (refMap == null) {
            refMap = new HashMap<Object, HashMap<Object, Object>>();
        }
        if (refMap.containsKey(obj)) {
            return null;
        }
        refMap.put(obj, ret);
        for (Entry<Object, Object> e : obj.entrySet()) {
            Object v = e.getValue();
            if (v instanceof NativeObject) {
                ret.put(e.getKey().toString(), toMap((NativeObject) v, refMap));
            } else {
                ret.put(e.getKey().toString(), v == null ? null : v.toString());
            }

        }
        return ret;
    }

    public String xhrRequest(String url, String method, String data, String requestHeaders) throws IOException {

        HashMap<String, String> headers = JSonStorage.restoreFromString(requestHeaders, new TypeRef<HashMap<String, String>>() {
        });
        br.getHeaders().put(new HTTPHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36"));
        if (headers != null) {
            headers.remove("User-Agent");
            if (headers.get("Referer") != null && "about:blank".equals(headers.get("Referer"))) {
                headers.remove("Referer");
            }
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
        br = createBrowserInstance();
        INSTANCES.put(id, new WeakReference<EnvJS>(this));
        logger = LogController.getInstance().getLogger(EnvJS.class.getName());

    }

    public void breakIt() {
        System.out.println("Break");
    }

    public void exit() {
        throw new WTFException("EXIT");
    }

    public String readRequire(String path) throws IOException {
        if (path.startsWith("envjs/")) {
            path = path.substring(6);
        }
        logger.info("Require: " + path);

        if ("platform/rhino".equals(path)) {

            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("platform/core".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("console".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("init".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }

        if ("timer".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }

        if ("local_settings".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("window".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("dom".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("html".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }

        if ("event".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("parser".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("xhr".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("css".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        if ("base64".equals(path)) {
            return IO.readURLToString(EnvJS.class.getResource(path + ".js"));
        }
        throw new WTFException("Unknown Resource required: " + path);

    }

    public static enum DebugLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        NONE;
    }

    private DebugLevel debugLevel = DebugLevel.DEBUG;

    public DebugLevel getDebugLevel() {
        return debugLevel;
    }

    public void setDebugLevel(DebugLevel debugLevel) {
        this.debugLevel = debugLevel;
    }

    public void init() {
        cx = Context.enter();

        scope = new Global(cx);
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_1_5);

        if (br == null) {
            throw new NullPointerException("browser is null");
        }
        // net.sourceforge.htmlunit.corejs.javascript.EcmaError: ReferenceError: "JSON" is not defined. (js#508) exceptions in the log are
        // ok.
        try {
            // evaluateTrustedString(cx, scope, "var EnvJSinstanceID=" + id + ";", "setInstance", 1, null);
            // evaluateTrustedString(cx, scope, "var DEBUG_LEVEL='" + debugLevel + "';", "setDebugLevel", 1, null);

            // evaluateTrustedString(cx, scope, IO.readURLToString(EnvJS.class.getResource("env.rhino.js")), "oldRhino", 1, null);
            String initSource = readRequire("envjs/init");
            initSource = initSource.replace("%EnvJSinstanceID%", id + "");
            evaluateTrustedString(cx, scope, initSource, "setInstance", 1, null);

            ArrayList<String> list = new ArrayList<String>(JSHtmlUnitPermissionRestricter.LOADED);

            Collections.sort(list, new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    return o2.length() - o1.length();
                }
            });

            // Cleanup

            eval("delete Packages;");
            for (String s : list) {
                while (true) {
                    System.out.println("Delete " + s);
                    try {
                        eval("delete " + s + ";");

                    } catch (Throwable e) {
                        // e.printStackTrace();
                    }
                    int index = s.lastIndexOf(".");
                    if (index > 0) {
                        s = s.substring(0, index);
                    } else {
                        break;
                    }
                }
            }
            // evaluateTrustedString(cx, scope, "delete EnvJs;delete After;", "CleanUp", 1, null);

            // var __context__=__context__;
            // var Envjs=Envjs;
            // var javaInstance=javaInstance;
            // var __this__=__this__;
            // var __argv__=__argv__;
            // var After=After;
            // var log=log;
            // var DEBUG_LEVEL=DEBUG_LEVEL;
            // var require=require;
            // evaluateTrustedString(cx, scope,
            // "Envjs.scriptTypes[\"text/javascript\"] = {\"text/javascript\"   :true,   \"text/envjs\"        :true};", "js", 1, null);
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    public Browser createBrowserInstance() {
        return new Browser();
    }

    public Object evalTrusted(String js) {
        return evaluateTrustedString(cx, scope, js, "evaltrusted:" + js, 1, null);
    }

    public Object eval(String js) {

        return cx.evaluateString(scope, js, "eval:" + js, 1, null);
        //
    }

    public void setDocument(String url, String html) {

        String js = " document.async=true; document.baseURI = '" + url + "';HTMLParser.parseDocument(\"" + html.replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\", document);Envjs.wait();";
        evaluateTrustedString(cx, scope, js, "setDoc", 1, null);
    }

    private LinkedList<String> scriptStack = new LinkedList<String>();
    private Object             globals;

    private Object evaluateTrustedString(Context cx2, Global scope2, String js, String string, int i, Object object) {
        scriptStack.add(string);
        try {

            return JSHtmlUnitPermissionRestricter.evaluateTrustedString(cx2, scope, js, string, i, object);
        } catch (EcmaError e) {
            logger.log(e);
            throw e;
        } finally {
            if (string != scriptStack.removeLast()) {
                throw new WTFException("Stack problem");
            }

        }
    }

    public void setGlobals(Object o) {
        this.globals = o;

    }

    public String getDocument() {
        Object result = cx.evaluateString(scope, "var f=function(){return document.innerHTML;}; f();", "getDocument", 1, null);
        if (result != null) {
            return result + "";
        }
        return null;
    }

    public Browser getBrowser() {
        return br;
    }

    public void require(Object string) throws IOException {
        JSHtmlUnitPermissionRestricter.evaluateTrustedString(cx, scope, readRequire(string + ""), "require_" + string, 1, null);
    }

    public void tick() {
        // put global variables in to reference. and delete it immediately.
        ScriptableObject.putProperty(scope, "envjsglobals", globals);
        eval("var e=envjsglobals.Envjs;delete envjsglobals;  e.tick();  ");

    }

    public void setUserAgent(String ua) {
        br.getHeaders().put("User-Agent", ua);
    }

}
