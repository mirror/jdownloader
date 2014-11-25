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
import jd.parser.Regex;
import jd.parser.html.Form;
import net.sourceforge.htmlunit.corejs.javascript.Callable;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ContextFactory;
import net.sourceforge.htmlunit.corejs.javascript.EcmaError;
import net.sourceforge.htmlunit.corejs.javascript.ErrorReporter;
import net.sourceforge.htmlunit.corejs.javascript.Evaluator;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.IdFunctionObject;
import net.sourceforge.htmlunit.corejs.javascript.JavaScriptException;
import net.sourceforge.htmlunit.corejs.javascript.NativeFunction;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.Script;
import net.sourceforge.htmlunit.corejs.javascript.ScriptRuntime;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebugFrame;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebuggableScript;
import net.sourceforge.htmlunit.corejs.javascript.debug.Debugger;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.scripting.ContextCallback;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;

public class EnvJSBrowser implements ContextCallback {
    static {
        try {
            JSHtmlUnitPermissionRestricter.init();
        } catch (IllegalStateException e) {
            // e.printStackTrace();
            // already set globally
        }
    }
    // private Context cx;
    private Global                                                      scope;
    private long                                                        id;
    private Browser                                                     br;
    private LogSource                                                   logger;

    private static AtomicLong                                           CCOUNTER  = new AtomicLong(0l);
    private static ConcurrentHashMap<Long, WeakReference<EnvJSBrowser>> INSTANCES = new ConcurrentHashMap<Long, WeakReference<EnvJSBrowser>>();

    public static EnvJSBrowser get(long id) {

        return INSTANCES.get(id).get();
    }

    public String getCookieStringByUrl(String url) {
        return Request.getCookieString(br.getCookies(url));
    }

    public void logToConsole(String message) {
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

    public String modifyInlineScript(String type, String js) throws IOException {
        if (permissionFilter != null) {
            return permissionFilter.onBeforeExecutingInlineJavaScript(type, js);
        }
        return js;
    }

    public String loadExternalScript(String type, String src, String url, Object window) {
        Request request;

        try {
            request = br.createGetRequest(url);

            if (permissionFilter != null) {
                request = permissionFilter.onBeforeLoadingExternalJavaScript(type, src, request);
            }
            if (request != null) {
                br.loadConnection(br.openRequestConnection(request));
                String ret = request.getHtmlCode();
                ;
                if (permissionFilter != null) {
                    return permissionFilter.onAfterLoadingExternalJavaScript(type, src, ret, request);
                }
                return ret;
            } else {
                return "console.log(\"Script Blocked: " + url + "\");";
            }

        } catch (IOException e) {
            logger.log(e);
            return "console.log(\"Script Loading Error: " + e + "\");";
        }

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
        // br.getHeaders().put(new HTTPHeader("User-Agent",
        // "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36"));
        if (headers != null) {
            headers.remove("User-Agent");
            if (headers.get("Referer") != null && "about:blank".equals(headers.get("Referer"))) {
                headers.remove("Referer");
            }
            br.getHeaders().putAll(headers);
        }
        Request request;
        if ("get".equalsIgnoreCase(method)) {
            request = br.createGetRequest(url);
        } else if ("post".equalsIgnoreCase(method)) {
            request = br.createPostRequest(url, data);

        } else {
            throw new WTFException("Not Supported");
        }
        if (permissionFilter != null) {
            request = permissionFilter.onBeforeXHRRequest(request);
        }
        if (request != null) {
            br.loadConnection(br.openRequestConnection(request));
        } else {

            XHRResponse ret = new XHRResponse();
            ret.setReponseMessage(ResponseCode.ERROR_NOT_FOUND.getDescription());
            ret.setResponseCode(ResponseCode.ERROR_NOT_FOUND.getCode());
            ret.setResponseText("Blocked by EnvJS");

            return JSonStorage.serializeToJson(ret);
        }

        XHRResponse ret = new XHRResponse();
        for (Entry<String, List<String>> s : br.getRequest().getResponseHeaders().entrySet()) {
            ret.getResponseHeader().put(s.getKey(), s.getValue().get(0));
        }
        ret.setEncoding(br.getRequest().getHttpConnection().getHeaderField("Content-Encoding"));
        ret.setReponseMessage(br.getRequest().getHttpConnection().getResponseMessage());
        ret.setResponseCode(br.getRequest().getHttpConnection().getResponseCode());
        ret.setResponseText(br.getRequest().getHtmlCode());
        if (permissionFilter != null) {
            permissionFilter.onAfterXHRRequest(request, ret);
        }
        return JSonStorage.serializeToJson(ret);
    }

    private PermissionFilter permissionFilter = null;

    public PermissionFilter getPermissionFilter() {
        return permissionFilter;
    }

    public void setPermissionFilter(PermissionFilter scriptFilter) {
        this.permissionFilter = scriptFilter;
    }

    public EnvJSBrowser() {
        id = CCOUNTER.incrementAndGet();
        br = createBrowserInstance();

        INSTANCES.put(id, new WeakReference<EnvJSBrowser>(this));
        logger = LogController.getInstance().getLogger(EnvJSBrowser.class.getName());

    }

    public EnvJSBrowser(Browser br2) {
        this();
        br = br2;
    }

    @Override
    public String onBeforeSourceCompiling(String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain) {
        return source;
    }

    @Override
    public Script onAfterSourceCompiling(Script ret, String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain) {
        return ret;
    }

    public void breakIt() {
        System.out.println("Break");
    }

    public void exit() {
        // parser stopped
        // throw new WTFException("EXIT");
    }

    public String readRequire(String path) throws IOException {
        if (path.startsWith("envjs/")) {
            path = path.substring(6);
        }
        logger.info("Require: " + path);

        if ("platform/rhino".equals(path)) {

            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("platform/core".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("console".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("init".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }

        if ("timer".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }

        if ("local_settings".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("window".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("dom".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("html".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }

        if ("event".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("parser".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("xhr".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("css".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        if ("base64".equals(path)) {
            return IO.readURLToString(EnvJSBrowser.class.getResource(path + ".js"));
        }
        throw new WTFException("Unknown Resource required: " + path);

    }

    public static final class JsDebugger implements Debugger {
        @Override
        public void handleCompilationDone(Context paramContext, DebuggableScript paramDebuggableScript, String paramString) {
            // System.out.println(" ----> " + paramString);
        }

        private int max = 0;

        @Override
        public DebugFrame getFrame(Context paramContext, final DebuggableScript paramDebuggableScript) {

            return new DebugFrame() {

                private static final String KEY_LAST_LINE   = "DebugFrameImpl#line";
                private static final String KEY_LAST_SOURCE = "DebugFrameImpl#source";

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onEnter(final Context cx, final Scriptable activation, final Scriptable thisObj, final Object[] args) {

                    final StringBuilder sb = new StringBuilder();

                    final String line = getFirstLine(cx);
                    final String source = getSourceName(cx);
                    if (source.startsWith("envjs/parser")) {
                        return;
                    }
                    if (source.startsWith("envjs/console")) {
                        return;
                    }
                    sb.append(source).append(":").append(line);

                    max = Math.max(sb.length(), max);

                    while (sb.length() < max + 3) {
                        sb.append(" ");
                    }
                    Scriptable parent = activation.getParentScope();

                    while (parent != null) {
                        sb.append(" ->");
                        parent = parent.getParentScope();
                    }
                    final String functionName = getFunctionName(thisObj);
                    if ("envjs/platform/core".equals(source) && "toLoggerString".equals(functionName)) {
                        return;
                    }
                    if ("envjs/platform/core".equals(source) && "append".equals(functionName)) {
                        return;
                    }
                    if ("envjs/platform/core".equals(source) && "format".equals(functionName)) {
                        return;
                    }
                    if ("envjs/platform/core".equals(source) && "getDateString".equals(functionName)) {
                        return;
                    }
                    if ("envjs/platform/core".equals(source) && "log".equals(functionName)) {
                        return;
                    }

                    if (functionName == null) {
                        return;
                    }
                    sb.append(functionName).append("(");
                    final int nbParams = paramDebuggableScript.getParamCount();
                    for (int i = 0; i < nbParams; i++) {
                        final String argAsString;
                        if (i < args.length) {
                            argAsString = stringValue(args[i]);
                        } else {
                            argAsString = "undefined";
                        }
                        sb.append(getParamName(i)).append(": ").append(argAsString);
                        if (i < nbParams - 1) {
                            sb.append(", ");
                        }
                    }
                    sb.append(")");

                    System.out.println(sb);
                }

                private String stringValue(final Object arg) {
                    if (arg instanceof NativeFunction) {
                        // Don't return the string value of the function, because it's usually
                        // multiple lines of content and messes up the log.
                        final String name = ((NativeFunction) arg).getFunctionName();
                        return "[function " + name + "]";
                    } else if (arg instanceof IdFunctionObject) {
                        return "[function " + ((IdFunctionObject) arg).getFunctionName() + "]";
                    } else if (arg instanceof Function) {
                        return "[function anonymous]";
                    }
                    String asString = null;
                    try {
                        // try to get the js representation
                        asString = Context.toString(arg);
                        // if (arg instanceof Event) {
                        // asString += "<" + ((Event) arg).jsxGet_type() + ">";
                        // }
                    } catch (final Throwable e) {
                        // seems to be a bug (many bugs) in rhino (TODO: investigate it)
                        asString = String.valueOf(arg);
                    }
                    return asString;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onExceptionThrown(final Context cx, final Throwable t) {

                    ScriptRuntime.constructError("Stacktrace", t.getMessage()).printStackTrace();

                    if (t instanceof JavaScriptException) {
                        final JavaScriptException e = (JavaScriptException) t;

                        System.out.println(getSourceName(cx) + ":" + getFirstLine(cx) + " Exception thrown: " + Context.toString(e.getValue()));

                    } else if (true) {
                        System.out.println(getSourceName(cx) + ":" + getFirstLine(cx) + " Exception thrown: " + t.getCause());
                    }

                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onLineChange(final Context cx, final int lineNumber) {
                    cx.putThreadLocal(KEY_LAST_LINE, lineNumber);
                    cx.putThreadLocal(KEY_LAST_SOURCE, paramDebuggableScript.getSourceName());
                }

                /**
                 * Returns the name of the function corresponding to this frame, if it is a function and it has a name. If the function does
                 * not have a name, this method will try to return the name under which it was referenced. See <a
                 * href="http://www.digital-web.com/articles/scope_in_javascript/">this page</a> for a good explanation of how the
                 * <tt>thisObj</tt> plays into this guess.
                 * 
                 * @param thisObj
                 *            the object via which the function was referenced, used to try to guess a function name if the function is
                 *            anonymous
                 * @return the name of the function corresponding to this frame
                 */
                private String getFunctionName(final Scriptable thisObj) {
                    if (paramDebuggableScript.isFunction()) {
                        final String name = paramDebuggableScript.getFunctionName();
                        if (name != null && name.length() > 0) {
                            // A named function -- we can just return the name.
                            return name;
                        }
                        // An anonymous function -- try to figure out how it was referenced.
                        // For example, someone may have set foo.prototype.bar = function() { ... };
                        // And then called fooInstance.bar() -- in which case it's "named" bar.

                        // on our SimpleScriptable we need to avoid looking at the properties we have defined => TODO: improve it
                        if (thisObj instanceof ScriptableObject) {
                            // return null;
                        }

                        Scriptable obj = thisObj;

                        while (obj != null) {
                            for (final Object id : obj.getIds()) {
                                if (id instanceof String) {
                                    final String s = (String) id;
                                    if (obj instanceof ScriptableObject) {
                                        Object o = ((ScriptableObject) obj).getGetterOrSetter(s, 0, false);
                                        if (o == null) {
                                            o = ((ScriptableObject) obj).getGetterOrSetter(s, 0, true);
                                            if (o != null && o instanceof Callable) {
                                                return "__defineSetter__ " + s;
                                            }
                                        } else if (o instanceof Callable) {
                                            // return "__defineGetter__ " + s;
                                            return null;
                                        }
                                    }
                                    final Object o = obj.get(s, obj);
                                    if (o instanceof NativeFunction) {
                                        final NativeFunction f = (NativeFunction) o;
                                        if (f.getDebuggableView() == paramDebuggableScript) {
                                            if ("debug".equals(s)) {
                                                return null;
                                            }

                                            return s;
                                        }
                                    }
                                }
                            }
                            obj = obj.getPrototype();
                        }
                        // Unable to intuit a name -- doh!
                        return null;
                    }
                    // Just a script -- no function name.
                    return "[script]";
                }

                /**
                 * Returns the name of the parameter at the specified index, or <tt>???</tt> if there is no corresponding name.
                 * 
                 * @param index
                 *            the index of the parameter whose name is to be returned
                 * @return the name of the parameter at the specified index, or <tt>???</tt> if there is no corresponding name
                 */
                private String getParamName(final int index) {
                    if (index >= 0 && paramDebuggableScript.getParamCount() > index) {
                        return paramDebuggableScript.getParamOrVarName(index);
                    }
                    return "???";
                }

                /**
                 * Returns the name of this frame's source.
                 * 
                 * @return the name of this frame's source
                 */
                private String getSourceName(final Context cx) {
                    String source = (String) cx.getThreadLocal(KEY_LAST_SOURCE);
                    if (source == null) {
                        return "unknown";
                    }
                    // only the file name is interesting the rest of the url is mostly noise
                    // source = StringUtils.substringAfterLast(source, "/");
                    // // embedded scripts have something like "foo.html from (3, 10) to (10, 13)"
                    // source = StringUtils.substringBefore(source, " ");
                    return source;
                }

                /**
                 * Returns the line number of the first line in this frame's function or script, or <tt>???</tt> if it cannot be determined.
                 * This is necessary because the line numbers provided by Rhino are unordered.
                 * 
                 * @return the line number of the first line in this frame's function or script, or <tt>???</tt> if it cannot be determined
                 */
                private String getFirstLine(final Context cx) {
                    final Object line = cx.getThreadLocal(KEY_LAST_LINE);
                    if (line == null) {
                        return "unknown";
                    }
                    return String.valueOf(line);
                }

                @Override
                public void onExit(Context paramContext, boolean paramBoolean, Object paramObject) {
                }

                @Override
                public void onDebuggerStatement(Context paramContext) {
                }

            };

        }
    }

    public static enum DebugLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        NONE;
    }

    private DebugLevel debugLevel = DebugLevel.DEBUG;
    private Context    cx;
    private Script     tick;
    private boolean    initDone;

    public DebugLevel getDebugLevel() {
        return debugLevel;
    }

    public void setDebugLevel(DebugLevel debugLevel) {
        this.debugLevel = debugLevel;
    }

    // ("exception",this.category,arguments);
    public boolean logger(String level, String category, String message) {
        try {

            DebugLevel debugLevel = DebugLevel.valueOf(level);
            if (debugLevel.ordinal() >= getDebugLevel().ordinal()) {
                switch (debugLevel) {
                case DEBUG:
                    logger.finest(category + "> " + message);
                    break;
                case ERROR:
                    logger.severe(category + "> " + message);
                    break;
                case INFO:
                    logger.info(category + "> " + message);
                    break;
                case WARN:
                    logger.warning(category + "> " + message);
                    break;
                case NONE:

                }
                return true;
            }
            return true;
        } catch (Throwable e) {
            logger.log(e);
            ;
            return false;
        }
    }

    public void init() {
        final ContextFactory factory = ContextFactory.getGlobal();
        scope = new Global();
        // try {
        // SwingUtilities.invokeAndWait(new Runnable() {
        //
        // @Override
        // public void run() {
        // Main main = new Main("JS Debugger");
        // main.doBreak();
        //
        // main.attachTo(factory);
        //
        // if ((scope instanceof Global)) {
        // Global global = scope;
        // global.setIn(main.getIn());
        // global.setOut(main.getOut());
        // global.setErr(main.getErr());
        // }
        // main.setScope(scope);
        //
        // main.pack();
        // main.setSize(600, 460);
        // main.setVisible(true);
        // main.setSourceProvider(new SourceProvider() {
        //
        // @Override
        // public String getSource(DebuggableScript paramDebuggableScript) {
        // return null;
        // }
        // });
        //
        // }
        // });
        // } catch (InvocationTargetException e1) {
        // e1.printStackTrace();
        // } catch (InterruptedException e1) {
        // e1.printStackTrace();
        // }

        cx = Context.enter(JSHtmlUnitPermissionRestricter.makeContext(this));
        cx.setOptimizationLevel(-1);
        scope.init(cx);
        // net.sourceforge.htmlunit.corejs.javascript.tools.debugger.Main.main(null);

        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_1_5);

        tick = cx.compileString("var e=envjsglobals.Envjs;delete envjsglobals;  e.tick();  ", "ticker", 1, null);
        if (br == null) {
            throw new NullPointerException("browser is null");
        }
        // net.sourceforge.htmlunit.corejs.javascript.EcmaError: ReferenceError: "JSON" is not defined. (js#508) exceptions in the log are
        // ok.
        try {
            // evaluateTrustedString(cx, scope, "var EnvJSinstanceID=" + id + ";", "setInstance", 1, null);
            // evaluateTrustedString(cx, scope, "var DEBUG_LEVEL='" + debugLevel + "';", "setDebugLevel", 1, null);

            // evaluateTrustedString(cx, scope, IO.readURLToString(EnvJS.class.getResource("env.rhino.js")), "oldRhino", 1, null);
            String preloadClasses = "";
            Class[] classes = new Class[] { Boolean.class, Integer.class, Long.class, String.class, Double.class, Float.class, net.sourceforge.htmlunit.corejs.javascript.EcmaError.class, WTFException.class };
            for (Class c : classes) {
                preloadClasses += "load=" + c.getName() + ";\r\n";
            }
            preloadClasses += "delete load;";
            evalTrusted(preloadClasses);
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
            if (getDebugLevel() == DebugLevel.DEBUG) {
                cx.setDebugger(new JsDebugger(), "debugger");
            }
            initDone = true;
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
        } finally {

        }
    }

    public void close() {
        scope = null;
        cx = null;
        Context.exit();
    }

    public Browser createBrowserInstance() {
        return new Browser();
    }

    public Object eval(String js) {

        try {
            return cx.evaluateString(scope, js, "eval:" + js, 1, null);
        } finally {

        }
        //
    }

    public void setDocument(String url, String html) {

        String js = " document.async=true; document.baseURI = '" + url + "';HTMLParser.parseDocument(\"" + html.replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\", document);Envjs.wait();";
        evaluateTrustedString(cx, scope, js, "setDoc", 1, null);
    }

    private LinkedList<String> scriptStack = new LinkedList<String>();
    private Object             globals;

    public Object evalTrusted(String js) {
        return evaluateTrustedString(cx, scope, js, "evaltrusted:" + js, 1, null);
    }

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

        Object result = evalTrusted("var f=function(){return document.innerHTML;}; f();");
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
        tick.exec(cx, scope);

    }

    public Global getScope() {
        return scope;
    }

    public void doBreak(int i) {
        System.out.println("Break " + i);
    }

    public void setUserAgent(String ua) {
        br.getHeaders().put("User-Agent", ua);
    }

    public void getPage(String parameter) {
        ensureInit();
        eval("var loading=true");
        eval("window.addEventListener(\"load\",function(){loading=false})");
        eval("document.location = '" + parameter + "';");

        Script script = cx.compileString("loading", "loading", 1, null);

        while (true) {
            tick();
            if (Boolean.FALSE.equals(script.exec(cx, scope))) {
                break;
            }
        }
        System.out.println(1);

    }

    private void ensureInit() {
        if (scope == null) {
            init();
        }
    }

    public Regex getRegex(String regex) {
        return new Regex(getDocument(), regex);
    }

    public Form[] getForms() {
        Form[] forms = Form.getForms(this);
        return forms;
    }

    public void printStacktrace() {
        logger.log(ScriptRuntime.constructError("Stacktrace", null));
    }

    public Object getGlobals() {
        return globals;
    }

    public void setVariable(String property, String c) {
        ScriptableObject.putProperty(scope, property, c);
    }

}
