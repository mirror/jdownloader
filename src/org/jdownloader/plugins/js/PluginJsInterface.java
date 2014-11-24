package org.jdownloader.plugins.js;

import java.util.Map.Entry;

import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;

import org.appwork.utils.reflection.Clazz;
import org.jdownloader.scripting.envjs.EnvJSBrowser;
import org.jdownloader.scripting.envjs.EnvJSBrowser.DebugLevel;
import org.jdownloader.scripting.envjs.PermissionFilter;
import org.jdownloader.scripting.envjs.XHRResponse;

public class PluginJsInterface {

    private PluginForDecrypt        plugin;
    private CryptedLink             link;
    private JavaScriptCrawlerPlugin controller;

    public PluginJsInterface(JavaScriptCrawlerPlugin plg, CryptedLink param) {
        this.plugin = plg.getPlugin();
        this.link = param;
        this.controller = plg;

    }

    public BrowserWrapperInterface createBrowser(final Function inlineJsFilter, final Function requestFilter) {

        final EnvJSBrowser envJs = new EnvJSBrowser(plugin.getBrowser());

        envJs.setDebugLevel(DebugLevel.INFO);
        envJs.setPermissionFilter(new PermissionFilter() {

            @Override
            public String onBeforeExecutingInlineJavaScript(String type, String js) {

                if (inlineJsFilter != null) {
                    js = (String) inlineJsFilter.call(controller.getContext(), controller.getScope(), controller.getScope(), new Object[] { type, js });
                }
                if (js == null) {

                    // ads ... do not evaluate
                    return "console.log('Blocked js')";
                }
                return js;
            }

            @Override
            public Request onBeforeXHRRequest(Request request) {

                if (requestFilter == null || Boolean.TRUE.equals(requestFilter.call(controller.getContext(), controller.getScope(), controller.getScope(), new Object[] { request.getUrl() }))) {
                    // try {
                    // only load websites with the same domain.
                    // if (StringUtils.equalsIgnoreCase(new URL(request.getUrl()).getHost(), new URL(parameter).getHost())) {
                    return request;
                }
                // }
                // } catch (MalformedURLException e) {
                // e.printStackTrace();
                // }

                // do not load the request
                return null;
            }

            @Override
            public Request onBeforeLoadingExternalJavaScript(String type, String src, Request request) {
                // do not load external js
                return null;
            }

            @Override
            public void onAfterXHRRequest(Request request, XHRResponse ret) {

                // getLogger().info(request + "");
            }

            @Override
            public String onAfterLoadingExternalJavaScript(String type, String src, String sourceCode, Request request) {

                return sourceCode;
            }
        });
        controller.registerBrowser(envJs);
        return new EnvJsBrowserWrapper(envJs);
    }

    public void log(Object object) {

        String msg = "";
        if (object == null) {
            msg = "null";
        } else if (Clazz.isPrimitive(object.getClass()) || Clazz.isPrimitiveWrapper(object.getClass())) {
            msg = "" + object;
        } else if (object.getClass().isArray()) {

            for (Object o : (Object[]) object) {
                if (msg.length() > 0) {
                    msg += ", ";
                }
                msg += o;
            }
        } else if (object instanceof NativeObject) {
            for (Entry<Object, Object> p : ((NativeObject) object).entrySet()) {
                if (msg.length() > 0) {
                    msg += ", \r\n";
                }
                msg += (p.getKey() + ": " + p.getValue());
            }

        } else {
            msg = "" + object;
        }
        plugin.getLogger().info(msg);
        // ScriptRuntime.constructError("Stacktrace", msg).printStackTrace();

    }

    public String getSupportedPattern() {
        return plugin.getLazyC().getPatternSource();
    }

    public String getCaptchaCode(String captchaPath) throws JSExceptionWrapper {
        try {
            return plugin.getCaptchaCode(captchaPath, link);
        } catch (Exception e) {
            throw new JSExceptionWrapper(e);
        }
    }

    public void addLink(String link) {
        DownloadLink dl;
        dl = new DownloadLink(null, null, plugin.getHost(), Encoding.urlDecode(link, true), true);
        plugin.distribute(dl);
    }

    public RegexWrapper regex(String src, String pattern) {
        return new RegexWrapper(src, pattern);
    }
}