package org.jdownloader.plugins.js;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.PluginForDecrypt;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ContextFactory;
import net.sourceforge.htmlunit.corejs.javascript.ErrorReporter;
import net.sourceforge.htmlunit.corejs.javascript.Evaluator;
import net.sourceforge.htmlunit.corejs.javascript.EvaluatorException;
import net.sourceforge.htmlunit.corejs.javascript.Script;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.scripting.ContextCallback;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;
import org.jdownloader.scripting.envjs.EnvJSBrowser;

public class JavaScriptCrawlerPlugin implements ContextCallback {
    static {
        try {
            JSHtmlUnitPermissionRestricter.init();
        } catch (Throwable e) {

        }
    }

    private PluginForDecrypt plugin;

    public PluginForDecrypt getPlugin() {
        return plugin;
    }

    private URL     srcUrl;

    private Global  scope;

    private Context context;

    public JavaScriptCrawlerPlugin(PluginForDecrypt plg, URL resource, ProgressController progress) {
        this.plugin = plg;
        this.srcUrl = resource;

    }

    protected LogSource getLogger() {
        return plugin.getLogger();
    }

    public void decryptIt(CryptedLink param) throws IOException {
        String jsSource = IO.readURLToString(srcUrl);
        final ContextFactory factory = ContextFactory.getGlobal();
        scope = new Global();

        context = Context.enter(JSHtmlUnitPermissionRestricter.makeContext(this));
        context.setOptimizationLevel(-1);

        scope.init(context);

        context.setOptimizationLevel(-1);
        context.setLanguageVersion(Context.VERSION_1_5);

        try {
            String preloadClasses = "";
            Class[] classes = new Class[] { PluginJsInterface.class, RegexWrapper.class, EnvJsBrowserWrapper.class, JSExceptionWrapper.class, Boolean.class, Integer.class, Long.class, String.class, Double.class, Float.class, net.sourceforge.htmlunit.corejs.javascript.EcmaError.class, WTFException.class };
            for (Class c : classes) {
                preloadClasses += "load=" + c.getName() + ";\r\n";
            }
            preloadClasses += "delete load;";

            JSHtmlUnitPermissionRestricter.evaluateTrustedString(context, scope, preloadClasses, preloadClasses, 0, null);
            context.evaluateString(scope, jsSource, "plg:" + srcUrl, 0, null);

            ArrayList<String> list = new ArrayList<String>(JSHtmlUnitPermissionRestricter.LOADED);

            Collections.sort(list, new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    return o2.length() - o1.length();
                }
            });

            ScriptableObject.deleteProperty(scope, "Packages");
            for (String s : list) {
                while (true) {
                    System.out.println("Delete " + s);
                    try {
                        ScriptableObject.deleteProperty(scope, s);

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
            ScriptableObject.putProperty(scope, "utils", new PluginJsInterface(this, param));
            ScriptableObject.putProperty(scope, "url", param.getCryptedUrl());
            context.evaluateString(scope, "var u=url;delete url;decryptUrl(u)", "var u=url;delete url;decryptUrl(u)", 0, null);
        } catch (EvaluatorException e) {
            getLogger().log(e);
            Dialog.getInstance().showExceptionDialog("Error in JavaScript Plugin", e.getMessage(), e);
        } catch (Throwable e) {
            Dialog.getInstance().showExceptionDialog("Error in JavaScript Plugin", e.getMessage(), e);
            throw new WTFException(e);
        } finally {
            for (EnvJSBrowser b : browserList) {
                b.close();
            }
        }

    }

    public Global getScope() {
        return scope;
    }

    @Override
    public String onBeforeSourceCompiling(String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain) {
        return source;
    }

    @Override
    public Script onAfterSourceCompiling(Script ret, String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain) {
        return ret;
    }

    List<EnvJSBrowser> browserList = new ArrayList<EnvJSBrowser>();

    public void registerBrowser(EnvJSBrowser envJs) {
        browserList.add(envJs);
    }

    public Context getContext() {
        return context;
    }

}
