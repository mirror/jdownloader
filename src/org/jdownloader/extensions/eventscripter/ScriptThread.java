package org.jdownloader.extensions.eventscripter;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import jd.http.Browser;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.exceptions.WTFException;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;

public class ScriptThread extends Thread {
    private ScriptEntry             script;
    private HashMap<String, Object> props;
    private Global                  scope;
    private Context                 cx;
    private LogSource               logger;
    private ScriptThread            delegate;

    public LogSource getLogger() {
        return logger;
    }

    public ScriptThread(ScriptEntry script, HashMap<String, Object> props, LogSource logSource) {
        this.script = script;
        this.props = props;
        this.logger = logSource;
    }

    @Override
    public void run() {
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

    public void notifyAboutException(Throwable e) {
        Dialog.getInstance().showExceptionDialog("An Error Occured", e.getMessage(), e);
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
            if (Clazz.isPrimitive(c)) {
                continue;
            }
            preloadClasses += "load=" + c.getName() + ";\r\n";

        }

        for (Method f : ScriptEnvironment.class.getDeclaredMethods()) {
            if (f.getAnnotation(ScriptAPI.class) != null) {
                for (Class<?> c : f.getParameterTypes()) {
                    if (c.isArray()) {
                        c = c.getComponentType();
                    }
                    if (!dupes.add(c.getName())) {
                        continue;
                    }
                    if (Clazz.isPrimitive(c) || Clazz.isPrimitiveWrapper(c) || Clazz.isString(c)) {
                        continue;
                    }
                    preloadClasses += "load=" + c.getName() + ";\r\n";
                }
                Class<?> c = f.getReturnType();
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
        ConfirmDialog d = new ConfirmDialog(0 | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, T._.securityLoading_title(), T._.securityLoading(fileOrUrl), new AbstractIcon(IconKey.ICON_SERVER, 32), null, null) {

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
                String js = IO.readFileToString(new File(fileOrUrl));
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

}
