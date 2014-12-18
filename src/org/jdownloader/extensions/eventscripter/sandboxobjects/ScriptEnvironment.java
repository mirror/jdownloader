package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import javax.swing.JTextPane;

import jd.controlling.downloadcontroller.DownloadController;
import jd.http.Browser;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OSFamily;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.processes.ProcessOutput;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.extensions.eventscripter.EnvironmentException;
import org.jdownloader.extensions.eventscripter.ScriptAPI;
import org.jdownloader.extensions.eventscripter.ScriptEntry;
import org.jdownloader.extensions.eventscripter.ScriptReferenceThread;
import org.jdownloader.extensions.eventscripter.ScriptThread;
import org.jdownloader.extensions.eventscripter.T;
import org.jdownloader.extensions.eventscripter.Utils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.ArraySet;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.SoundSettings;

public class ScriptEnvironment {
    @ScriptAPI(description = "JDownloader Installation Directory")
    public static String                                         JD_HOME           = Application.getResource("").getAbsolutePath();
    private static HashMap<String, Object>                       GLOBAL_PROPERTIES = new HashMap<String, Object>();
    private static HashMap<ScriptEntry, HashMap<String, Object>> SCRIPT_PROPERTIES = new HashMap<ScriptEntry, HashMap<String, Object>>();

    @ScriptAPI(description = "Get a DownloadList Link by it's uuid", parameters = { "uuid" })
    public static DownloadLinkSandBox getDownloadLinkByUUID(long uuid) throws EnvironmentException {
        try {

            return new DownloadLinkSandBox(DownloadController.getInstance().getLinkByID(uuid));

        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    public static String toJson(Object ret) {
        final ScriptThread env = getScriptThread();
        // convert to javascript object
        String js = "(function(){ return JSON.stringify(" + JSonStorage.serializeToJson(ret) + ");}());";

        return (String) env.evalTrusted(js);
    }

    public static Object toJSObject(Object ret) {
        final ScriptThread env = getScriptThread();
        // convert to javascript object
        String js = "(function(){ return " + JSonStorage.serializeToJson(ret) + ";}());";

        return env.evalTrusted(js);
    }

    @ScriptAPI(description = "Get a DownloadList Package by it's uuid", parameters = { "uuid" })
    public static FilePackageSandBox getDownloadPackageByUUID(long uuid) throws EnvironmentException {
        try {

            return new FilePackageSandBox(DownloadController.getInstance().getPackageByID(uuid));
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    // @GlobalField(description = "Get a Linkgrabber Link by it's uuid", parameters = { "uuid" })
    // public static Object getGrabbedLinkByUUID(long uuid) throws EnvironmentException {
    // try {
    // CrawledLink ret = LinkCollector.getInstance().getLinkByID(uuid);
    // return JSonStorage.convert(ret, TypeRef.HASHMAP);
    // } catch (Throwable e) {
    // throw new EnvironmentException(e);
    // }
    // }
    // @GlobalField(description = "Get a Linkgrabber Package by it's uuid", parameters = { "uuid" })
    // public static Object getGrabbedPackageByUUID(long uuid) throws EnvironmentException {
    // try {
    // CrawledPackage ret = LinkCollector.getInstance().getPackageByID(uuid);
    // return JSonStorage.convert(ret, TypeRef.HASHMAP);
    // } catch (Throwable e) {
    // throw new EnvironmentException(e);
    // }
    // }
    @ScriptAPI(description = "Set a Property. This property will be available until JD-exit or a script overwrites it. if global is true, the property will be available for al scripts", parameters = { "\"key\"", "anyValue", "global(boolean)" }, example = "var oldValue=setProperty(\"myobject\", { \"name\": true}, false);")
    public static Object setProperty(String key, Object value, boolean global) throws EnvironmentException {
        try {
            synchronized (GLOBAL_PROPERTIES) {
                if (global) {
                    return GLOBAL_PROPERTIES.put(key, value);

                } else {

                    HashMap<String, Object> store = SCRIPT_PROPERTIES.get(getScriptThread().getScript());
                    if (store == null) {
                        store = new HashMap<String, Object>();
                        SCRIPT_PROPERTIES.put(getScriptThread().getScript(), store);
                    }
                    return store.put(key, value);

                }

            }

        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Get a Property. Set global to true if you want to access a global property", parameters = { "\"key\"", "global(boolean)" }, example = "var value=getProperty(\"myobject\", false);")
    public static Object getProperty(String key, boolean global) throws EnvironmentException {
        try {
            synchronized (GLOBAL_PROPERTIES) {
                if (global) {
                    return GLOBAL_PROPERTIES.get(key);

                } else {

                    HashMap<String, Object> store = SCRIPT_PROPERTIES.get(getScriptThread().getScript());
                    if (store == null) {
                        return null;
                    }
                    return store.get(key);

                }

            }

        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Call the MyJDownloader API", parameters = { "\"namespace\"", "\"methodname\"", "parameter1", "parameter2", "..." }, example = "callAPI(\"downloadsV2\", \"queryLinks\", { \"name\": true})")
    public static Object callAPI(String namespace, String method, Object... parameters) throws EnvironmentException {

        askForPermission("call the Remote API: " + namespace + "/" + method);
        String js;
        try {
            Object ret = RemoteAPIController.getInstance().call(namespace, method, parameters);
            final ScriptThread env = getScriptThread();
            // convert to javascript object
            js = "(function(){ return " + JSonStorage.serializeToJson(ret) + ";}());";
            Object retObject = env.evalTrusted(js);

            return retObject;
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Show a Message Box", parameters = { "myObject1", "MyObject2", "..." }, example = "alert(JD_HOME);")
    public static void alert(Object... objects) {
        final ScriptThread env = getScriptThread();
        if (objects.length == 1) {
            if (Clazz.isPrimitiveWrapper(objects[0].getClass()) || Clazz.isString(objects[0].getClass())) {
                showMessageDialog(objects[0] + "");
                return;
            } else {
                try {
                    try {
                        showMessageDialog(new JacksonMapper().objectToString(objects[0]));
                    } catch (Throwable e) {

                        showMessageDialog(format(toJson(objects[0])));
                    }
                } catch (Throwable e) {

                    showMessageDialog(objects[0] + "");
                }
                return;
            }
        }
        try {
            try {
                showMessageDialog(new JacksonMapper().objectToString(objects));
            } catch (Throwable e) {
                showMessageDialog(format(toJson(objects)));

            }
        } catch (Throwable e) {
            showMessageDialog(objects + "");

        }
        return;
    }

    private static String format(String js) {
        final ScriptThread env = getScriptThread();
        try {
            env.ensureLibrary("js_beautifier.js");
            String parametername;
            ScriptableObject.putProperty(env.getScope(), parametername = "text_" + System.currentTimeMillis(), js);

            String ret = env.evalTrusted("js_beautify(" + parametername + ", {   });") + "";
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return js;
    }

    private static void showMessageDialog(String string) {
        final ScriptThread env = getScriptThread();

        UIOManager.I().show(ConfirmDialogInterface.class, new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, T._.showMessageDialog_title(env.getScript().getName(), env.getScript().getEventTrigger().getLabel()), string, new AbstractIcon(IconKey.ICON_INFO, 32), null, null) {
            @Override
            protected void modifyTextPane(JTextPane textField) {

            }

            @Override
            public boolean isRemoteAPIEnabled() {
                return false;
            }

            @Override
            public void pack() {
                this.getDialog().pack();
            }

            @Override
            protected int getPreferredWidth() {
                return 600;
            }
        });
    }

    @ScriptAPI(description = "Loads a website (Method: GET) and returns the source code", parameters = { "URL" }, example = "var myhtmlSourceString=getPage(\"http://jdownloader.org\");")
    public static String getPage(String fileOrUrl) throws EnvironmentException {

        askForPermission("load resources from the internet");

        try {
            return new Browser().getPage(fileOrUrl);
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    private static void askForPermission(final String string) throws EnvironmentException {
        final ScriptThread env = getScriptThread();
        final String md5 = Hash.getMD5(env.getScript().getScript());
        ConfirmDialog d = new ConfirmDialog(0 | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, T._.permission_title(), T._.permission_msg(env.getScript().getName(), env.getScript().getEventTrigger().getLabel(), string), new AbstractIcon(IconKey.ICON_SERVER, 32), T._.allow(), T._.deny()) {

            @Override
            public String getDontShowAgainKey() {

                return "ASK_FOR_PERMISSION_" + md5 + "_" + string;

            }

            @Override
            public boolean isRemoteAPIEnabled() {
                return true;
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
        if (d.show().getCloseReason() != CloseReason.OK) {
            throw new EnvironmentException("Security Warning: User Denied Access to " + string);
        }

    }

    @ScriptAPI(description = "Loads a website (METHOD: POST) and returns the source code", parameters = { "URL", "PostData" }, example = "var myhtmlSourceString=postPage(\"http://support.jdownloader.org/index.php\",\"searchquery=captcha\");")
    public static String postPage(String url, String post) throws EnvironmentException {
        askForPermission("send data to the internet and request resources");
        try {
            return new Browser().postPage(url, post);
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Read a text file", parameters = { "filepath" }, example = "var myString=readFile(JD_HOME+\"/license.txt\");")
    public static String readFile(String filepath) throws EnvironmentException {
        askForPermission("read a local file");
        try {
            return IO.readFileToString(new File(filepath));
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Create a directory", parameters = { "path" }, example = "var myBooleanResult=mkdirs(JD_HOME+\"/mydirectory/\");")
    public static boolean mkdirs(String filepath) throws EnvironmentException {
        askForPermission("create a directory");
        try {
            return new File(filepath).mkdirs();
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Delete a file or a directory", parameters = { "path", "recursive" }, example = "var myBooleanResult=deleteFile(JD_HOME+\"/mydirectory/\",false);")
    public static boolean deleteFile(String filepath, boolean recursive) throws EnvironmentException {
        askForPermission("delete a local fole or directory");

        try {
            if (recursive) {

                Files.deleteRecursiv(new File(filepath), true);
            } else {
                new File(filepath).delete();
            }
            return !new File(filepath).exists();
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Write a text file", parameters = { "filepath", "myText", "append" }, example = "writeFile(JD_HOME+\"/log.txt\",JSON.stringify(this)+\"\\r\\n\",true);")
    public static void writeFile(String filepath, String string, boolean append) throws EnvironmentException {
        askForPermission("create a local file and write to it");
        try {
            IO.writeStringToFile(new File(filepath), string, append);
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Play a Wav Audio file", parameters = { "myFilePathOrUrl" }, example = "playWavAudio(JD_HOME+\"/themes/standard/org/jdownloader/sounds/captcha.wav\");")
    public static void playWavAudio(String fileOrUrl) throws EnvironmentException {

        try {

            AudioInputStream stream = null;
            try {
                stream = AudioSystem.getAudioInputStream(new File(fileOrUrl));
                final AudioFormat format = stream.getFormat();
                final DataLine.Info info = new DataLine.Info(Clip.class, format);
                if (AudioSystem.isLineSupported(info)) {
                    final Clip clip = (Clip) AudioSystem.getLine(info);
                    clip.open(stream);
                    try {
                        final FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

                        float db = (20f * (float) Math.log(JsonConfig.create(SoundSettings.class).getCaptchaSoundVolume() / 100f));

                        gainControl.setValue(Math.max(-80f, db));
                        BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
                        muteControl.setValue(true);

                        muteControl.setValue(false);
                    } catch (Exception e) {
                        Log.exception(e);
                    }
                    clip.start();
                    clip.addLineListener(new LineListener() {

                        @Override
                        public void update(LineEvent event) {
                            if (event.getType() == Type.STOP) {
                                clip.close();
                            }
                        }
                    });
                    while (clip.isRunning()) {
                        Thread.sleep(100);
                    }
                }

            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (Throwable e) {
                }
                // try {
                // clip.close();
                // } catch (Throwable e) {
                //
                // }
            }

        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Loads a Javascript file or url. ATTENTION. The loaded script can access the API as well.", parameters = { "myFilePathOrUrl" }, example = "require(\"https://raw.githubusercontent.com/douglascrockford/JSON-js/master/json.js\");")
    public static void require(String fileOrUrl) throws EnvironmentException {
        askForPermission("load external JavaScript");
        try {
            getScriptThread().requireJavascript(fileOrUrl);
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Call a local Process asynchronous", parameters = { "\"myCallBackFunction\"|null", "\"commandline1\"", "\"commandline2\"", "\"...\"" }, example = "callAsync(function(exitCode,stdOut,errOut){ alert(\"Closed Notepad\");},\"notepad.exe\",JD_HOME+\"\\\\license.txt\");")
    public static void callAsync(final Function callback, final String... commands) throws EnvironmentException {

        askForPermission("Execute a local process");

        try {
            final ScriptThread env = getScriptThread();
            if (commands != null && commands.length > 0) {
                new ScriptReferenceThread(env) {
                    @Override
                    public void run() {
                        try {
                            try {
                                ProcessOutput ret = ProcessBuilderFactory.runCommand(commands);

                                if (callback != null) {

                                    if (CrossSystem.getOSFamily() == OSFamily.WINDOWS) {
                                        executeCallback(callback, ret.getExitCode(), new String(new String(ret.getStdOutData(), "cp850").getBytes("UTF-8"), "UTF-8"), new String(new String(ret.getErrOutData(), "cp850").getBytes("UTF-8"), "UTF-8"));
                                    } else {
                                        executeCallback(callback, ret.getExitCode(), new String(ret.getStdOutData(), "UTF-8"), new String(ret.getErrOutData(), "UTF-8"));

                                    }
                                }
                            } catch (IOException e) {

                                if (callback != null) {
                                    executeCallback(callback, -1, null, Exceptions.getStackTrace(e));

                                }
                                env.getLogger().log(e);
                                env.notifyAboutException(e);
                            }
                        } catch (Throwable e) {
                            env.notifyAboutException(e);
                        }

                    }
                }.start();

            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    private static ScriptThread getScriptThread() {
        Thread ct = Thread.currentThread();
        if (ct instanceof ScriptThread) {
            return (ScriptThread) ct;
        } else if (ct instanceof ScriptReferenceThread) {
            return ((ScriptReferenceThread) ct).getScriptThread();
        } else {
            throw new IllegalStateException();
        }

    }

    @ScriptAPI(description = "Call a local Process. Blocks Until the process returns", parameters = { "\"commandline1\"", "\"commandline2\"", "\"...\"" }, example = "var pingResultString = callSync(\"ping\",\"jdownloader.org\");")
    public static String callSync(final String... commands) throws EnvironmentException {
        askForPermission("Execute a local process");

        try {
            ProcessBuilder pb = ProcessBuilderFactory.create(commands);
            pb.redirectErrorStream();
            ProcessOutput ret = ProcessBuilderFactory.runCommand(pb);
            if (CrossSystem.getOSFamily() == OSFamily.WINDOWS) {
                return new String(new String(ret.getStdOutData(), "cp850").getBytes("UTF-8"), "UTF-8");
            } else {
                return ret.getStdOutString("UTF-8");

            }

        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    protected static void collectClasses(Class<? extends Object> cl, ArraySet<Class<?>> clazzes) {

        for (Method m : cl.getDeclaredMethods()) {
            if (cl == ScriptEnvironment.class && m.getAnnotation(ScriptAPI.class) == null) {
                continue;
            }
            if (m.getReturnType() == Object.class || !Modifier.isPublic(m.getModifiers()) || Clazz.isPrimitive(m.getReturnType()) || Clazz.isPrimitiveWrapper(m.getReturnType()) || Clazz.isString(m.getReturnType())) {
                continue;
            }
            if (clazzes.add(m.getReturnType())) {
                collectClasses(m.getReturnType(), clazzes);
            }
            for (Class<?> cl2 : m.getParameterTypes()) {
                if (cl2 == Object.class || Clazz.isPrimitive(cl2) || Clazz.isPrimitiveWrapper(cl2) || Clazz.isString(cl2)) {
                    continue;
                }
                if (clazzes.add(cl2)) {
                    collectClasses(cl2, clazzes);
                }

            }
            for (Field f : cl.getFields()) {
                if (f.getType() == Object.class || !Modifier.isPublic(m.getModifiers()) || Clazz.isPrimitive(f.getType()) || Clazz.isPrimitiveWrapper(f.getType()) || Clazz.isString(f.getType())) {
                    continue;
                }
                if (clazzes.add(f.getType())) {
                    collectClasses(f.getType(), clazzes);
                }

            }
        }
    }

    public static Collection<Class<?>> getRequiredClasses() {
        ArraySet<Class<?>> clazzes = new ArraySet<Class<?>>();
        collectClasses(ScriptEnvironment.class, clazzes);
        return clazzes;
    }

    public static String getAPIDescription(ArraySet<Class<?>> triggerClazzes) {
        StringBuilder sb = new StringBuilder();
        //
        ArraySet<Class<?>> clazzes = new ArraySet<Class<?>>();

        sb.append("/* =============== ").append("Global API").append(" =============== */").append("\r\n");
        getAPIDescriptionForClass(sb, ScriptEnvironment.class);

        sb.append("/* =========  Properties =========*/\r\n");
        for (Field f : Utils.sort(ScriptEnvironment.class.getDeclaredFields())) {
            ScriptAPI ann = f.getAnnotation(ScriptAPI.class);
            if (ann != null) {
                sb.append("//").append(ann.description()).append(";\r\n");
                sb.append("var my").append(f.getType().getSimpleName().substring(0, 1).toUpperCase(Locale.ENGLISH)).append(f.getType().getSimpleName().substring(1)).append(" = ");
                sb.append(f.getName()).append(";\r\n");

                if (StringUtils.isNotEmpty(ann.example())) {
                    sb.append(ann.example()).append("\r\n");
                }
            }
        }
        collectClasses(ScriptEnvironment.class, clazzes);
        clazzes.addAll(triggerClazzes);
        Collections.sort(clazzes, new Comparator<Class<?>>() {

            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return Utils.cleanUpClass(o1.getSimpleName()).compareTo(Utils.cleanUpClass(o2.getSimpleName()));
            }

        });
        sb.append("/* =============== ").append("Classes").append(" =============== */").append("\r\n");
        for (Class<?> cl : clazzes) {
            sb.append("/* === ").append(Utils.cleanUpClass(cl.getSimpleName())).append(" === */").append("\r\n");
            getAPIDescriptionForClass(sb, cl);

        }
        return sb.toString();
    }

    /**
     * @param sb
     * @param cl
     */
    public static void getAPIDescriptionForClass(StringBuilder sb, Class<?> cl) {
        ScriptAPI clazzAnn = cl.getAnnotation(ScriptAPI.class);

        if (clazzAnn != null && StringUtils.isNotEmpty(clazzAnn.description())) {
            sb.append("/* ").append(clazzAnn.description()).append("*/").append("\r\n");
        }
        sb.append("/* =========  Methods =========*/\r\n");
        for (Method m : Utils.sort(cl.getDeclaredMethods())) {

            if (!Modifier.isPublic(m.getModifiers())) {

                continue;
            }
            ScriptAPI ann = m.getAnnotation(ScriptAPI.class);
            if (cl == ScriptEnvironment.class && ann == null) {
                continue;
            }
            if (!Clazz.isVoid(m.getReturnType())) {
                sb.append("var ").append(Utils.toMy(Utils.cleanUpClass(m.getReturnType().getSimpleName()))).append(" = ");
            }
            if (cl == ScriptEnvironment.class) {
                sb.append(m.getName());
            } else {
                sb.append(Utils.toMy(Utils.cleanUpClass(cl.getSimpleName()))).append(".").append(m.getName());
            }
            sb.append("(");
            boolean first = true;
            int i = 0;
            for (Class<?> p : m.getParameterTypes()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(Utils.toMy(p.getSimpleName()));
                if (ann != null && ann.parameters().length == m.getParameterTypes().length && !StringUtils.isEmpty(ann.parameters()[i])) {
                    sb.append("/*").append(ann.parameters()[i]).append("*/");
                }
                i++;
            }
            sb.append(");");
            if (ann != null && StringUtils.isNotEmpty(ann.description())) {
                sb.append("/*").append(ann.description()).append("*/");
            }
            sb.append("\r\n");
            if (ann != null && StringUtils.isNotEmpty(ann.example())) {
                sb.append("/* Example: */");
                sb.append(ann.example());
                sb.append("\r\n");
            }
        }

    }
}