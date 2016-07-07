package org.jdownloader.extensions.eventscripter;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;
import org.jdownloader.scripting.JSShutterDelegate;

import jd.http.Browser;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.EcmaError;
import net.sourceforge.htmlunit.corejs.javascript.ScriptRuntime;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.WrappedException;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

public class ScriptThread extends Thread implements JSShutterDelegate {
    private ScriptEntry             script;
    private HashMap<String, Object> props;
    private Global                  scope;
    private Context                 cx;
    private LogSource               logger;
    private ScriptThread            delegate;
    private EventScripterExtension  extension;

    public LogSource getLogger() {
        return logger;
    }

    public ScriptThread(EventScripterExtension eventScripterExtension, ScriptEntry script, HashMap<String, Object> props, LogSource logSource) {
        this.script = script;
        this.props = props;
        this.logger = logSource;
        this.extension = eventScripterExtension;
    }

    @Override
    public synchronized void start() {
        super.start();
        if (script.getEventTrigger().isSynchronous()) {
            try {
                join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        synchronized (script) {

            if (!script.isEnabled()) {
                return;
            }
            scope = new Global();
            cx = Context.enter();
            cx.setOptimizationLevel(-1);
            scope.init(cx);
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(Context.VERSION_1_5);

            try {

                String preloadClasses = preInitClasses();

                evalTrusted(preloadClasses);
                // required by some libraries
                evalTrusted("global=this;");

                initEnvironment();
                cleanupClasses();
                evalUNtrusted(script.getScript());
                // ProcessBuilderFactory.runCommand(commandline);
            } catch (Throwable e) {
                logger.log(e);
                notifyAboutException(e);

            } finally {
                Context.exit();
            }
        }
    }

    public void notifyAboutException(Throwable e) {

        Dialog.getInstance().showExceptionDialog("An Error Occured", e.getMessage(), e);
        if (script != null) {
            script.setEnabled(false);
            extension.refreshScripts();
        }
    }

    private String preInitClasses() {
        HashSet<String> dupes = new HashSet<String>();
        dupes.add("net.sourceforge.htmlunit.corejs.javascript.Function");
        dupes.add("void");
        Class[] classes = new Class[] { Boolean.class, Integer.class, Long.class, String.class, Double.class, Float.class, net.sourceforge.htmlunit.corejs.javascript.EcmaError.class, ScriptEnvironment.class, EnvironmentException.class };

        String preloadClasses = "";
        for (Class c : classes) {
            if (c.isArray()) {
                c = c.getComponentType();
            }
            if (!dupes.add(c.getName())) {
                continue;
            }
            if (c.isPrimitive()) {
                continue;
            }
            preloadClasses += "load=" + c.getName() + ";\r\n";

        }
        Collection<Class<?>> clazzes = ScriptEnvironment.getRequiredClasses();
        clazzes.addAll(script.getEventTrigger().getAPIClasses());
        clazzes.add(Object.class);
        for (Class<?> c : clazzes) {

            if (c.isArray()) {
                // preloadClasses += "load=" + c.getName() + ";\r\n";
                c = c.getComponentType();

            }
            if (!dupes.add(c.getName())) {
                continue;
            }
            if (c.isPrimitive()) {
                continue;
            }
            preloadClasses += "load=" + c.getName() + ";\r\n";

        }
        for (Field f : ScriptEnvironment.class.getDeclaredFields()) {
            if (f.getAnnotation(ScriptAPI.class) != null) {
                Class<?> c = f.getType();
                if (c.isArray()) {
                    c = c.getComponentType();
                }
                if (!dupes.add(c.getName())) {
                    continue;
                }
                if (Clazz.isPrimitive(c)) {
                    continue;
                }
                preloadClasses += "load=" + c.getName() + ";\r\n";

            }
        }
        preloadClasses += "delete load;";
        return preloadClasses;
    }

    private void initEnvironment() throws IllegalAccessException {
        for (Method f : ScriptEnvironment.class.getDeclaredMethods()) {
            if (f.getAnnotation(ScriptAPI.class) != null) {
                evalTrusted(f.getName() + "=" + ScriptEnvironment.class.getName() + "." + f.getName() + ";");

            }
        }

        for (Field f : ScriptEnvironment.class.getDeclaredFields()) {
            if (f.getAnnotation(ScriptAPI.class) != null) {
                ScriptableObject.putProperty(scope, f.getName(), ScriptEnvironment.toJSObject(f.get(null)));

            }
        }

        for (Entry<String, Object> es : props.entrySet()) {
            ScriptableObject.putProperty(scope, es.getKey(), es.getValue());
            // convert to real js objects
            // evalTrusted(es.getKey() + " = " + new SimpleMapper().objectToString() + ";");

        }
    }

    private void evalUNtrusted(String script) {
        cx.evaluateString(getScope(), script, "", 1, null);
    }

    public Object evalTrusted(String preloadClasses) {
        // System.out.println(preloadClasses);
        return JSHtmlUnitPermissionRestricter.evaluateTrustedString(cx, getScope(), preloadClasses, "", 1, null);
    }

    private void cleanupClasses() {
        ArrayList<String> list = new ArrayList<String>(JSHtmlUnitPermissionRestricter.LOADED);

        Collections.sort(list, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });

        // Cleanup

        ScriptableObject.deleteProperty(scope, "Packages");
        for (String s : list) {
            while (true) {

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
    }

    public void requireJavascript(final String fileOrUrl) throws IOException {
        ConfirmDialog d = new ConfirmDialog(0 | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, T.T.securityLoading_title(), T.T.securityLoading(fileOrUrl), new AbstractIcon(IconKey.ICON_SERVER, 32), null, null) {

            @Override
            public String getDontShowAgainKey() {

                return "ASK_TO_REQUIRE_JS_" + fileOrUrl;

            }

            @Override
            protected int getPreferredWidth() {
                return 600;
            }

            public void windowClosing(final WindowEvent arg0) {
                setReturnmask(false);
                this.dispose();
            }

        };

        d.setDoNotShowAgainSelected(true);

        // Integer ret = JSonStorage.getPlainStorage("Dialogs").get(d.getDontShowAgainKey(), -1);
        // if (ret != null && ret > 0) {
        // return;
        // }
        if (d.show().getCloseReason() == CloseReason.OK) {
            if (fileOrUrl.matches("^https?\\:\\/\\/.+")) {
                // url
                Browser br = new Browser();
                String js = br.getPage(fileOrUrl);
                logger.info(js);
                evalUNtrusted(js);

            } else {
                File file = new File(fileOrUrl);
                if (!file.exists()) {
                    file = Application.getResource(fileOrUrl);
                }
                String js = IO.readFileToString(file);
                logger.info(js);
                evalUNtrusted(js);

            }

        }
    }

    public Context getContext() {
        return cx;
    }

    public Global getScope() {

        return scope;
    }

    public ScriptEntry getScript() {
        return script;
    }

    private HashSet<String> loadedLibrary = new HashSet<String>();

    public void ensureLibrary(String string) {
        synchronized (this) {
            if (loadedLibrary.add(string)) {
                try {
                    evalTrusted(IO.readURLToString(ScriptEntry.class.getResource(string)));
                } catch (IOException e) {
                    throw new WTFException(e);
                }

            }
        }
    }

    /**
     * create a native javaobject for settings
     *
     * @param settings
     * @return
     */
    public Object toNative(Object settings) {
        String json = JSonStorage.serializeToJson(settings);
        // final String reviverjs = "(function(key,value) { return value; })";
        // final Callable reviver = (Callable) cx.evaluateString(scope, reviverjs, "reviver", 0, null);
        //
        // Object nativ = NativeJSON.parse(cx, scope, json, reviver);
        // String tmpName = "json_" + System.currentTimeMillis();
        Object ret = cx.evaluateString(scope, "(function(){ return " + json + ";})();", "", 0, null);
        // for (Entry<Object, Object> es : ((NativeObject) ret).entrySet()) {
        // ((NativeObject) ret).setAttributes(es.getKey() + "", NativeObject.READONLY);
        // }

        return ret;
    }

    @Override
    public boolean isClassVisibleToScript(boolean trusted, String className) {
        if (trusted) {

            return true;

        } else if (className.startsWith("adapter")) {

            return true;

        } else if (className.equals("net.sourceforge.htmlunit.corejs.javascript.EcmaError")) {

            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Javascript error occured");

            return true;
        } else if (className.equals("net.sourceforge.htmlunit.corejs.javascript.ConsString")) {

            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Javascript error occured");

            return true;
        } else if (className.equals("net.sourceforge.htmlunit.corejs.javascript.JavaScriptException")) {

            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Javascript error occured");

            return true;
        } else if (className.equals(org.jdownloader.extensions.eventscripter.EnvironmentException.class.getName())) {

            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Environment error occured");

            return true;
        } else if (className.equals("net.sourceforge.htmlunit.corejs.javascript.WrappedException")) {
            WrappedException.class.getName();
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Script RuntimeException occured");

            return true;
        } else if (className.equals("net.sourceforge.htmlunit.corejs.javascript.EvaluatorException")) {

            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Javascript error occured");

            return true;
        } else {
            EcmaError ret = ScriptRuntime.constructError("Security Violation", "Security Violation " + className);
            throw ret;

        }
    }

}
