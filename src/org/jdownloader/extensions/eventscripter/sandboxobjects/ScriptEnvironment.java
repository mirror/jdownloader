package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

import jd.controlling.AccountController;
import jd.controlling.TaskQueue;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.reconnect.Reconnecter;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
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
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
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
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.SoundSettings;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class ScriptEnvironment {
    private static HashMap<String, Object>                       GLOBAL_PROPERTIES = new HashMap<String, Object>();
    @ScriptAPI(description = "JDownloader Installation Directory")
    public static String                                         JD_HOME           = Application.getResource("").getAbsolutePath();
    private static LogSource                                     LOGGER            = LogController.getInstance().getLogger("ScriptEnvironment");
    private static HashMap<ScriptEntry, HashMap<String, Object>> SCRIPT_PROPERTIES = new HashMap<ScriptEntry, HashMap<String, Object>>();

    @ScriptAPI(description = "Show a Message Box", parameters = { "myObject1", "MyObject2", "..." }, example = "alert(JD_HOME);")
    public static void alert(Object... objects) {
        final ScriptThread env = getScriptThread();
        if (objects != null && objects.length == 1) {
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

    static void askForPermission(final String string) throws EnvironmentException {
        final ScriptThread env = getScriptThread();
        final String md5 = Hash.getMD5(env.getScript().getScript());
        ConfirmDialog d = new ConfirmDialog(0 | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, T.T.permission_title(), T.T.permission_msg(env.getScript().getName(), env.getScript().getEventTrigger().getLabel(), string), new AbstractIcon(IconKey.ICON_SERVER, 32), T.T.allow(), T.T.deny()) {
            @Override
            public String getDontShowAgainKey() {
                return "ASK_FOR_PERMISSION_" + md5 + "_" + string;
            }

            @Override
            protected int getPreferredWidth() {
                return 600;
            }

            @Override
            public boolean isRemoteAPIEnabled() {
                return true;
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

    @ScriptAPI(description = "Call the MyJDownloader API", parameters = { "\"namespace\"", "\"methodname\"", "parameter1", "parameter2", "..." }, example = "callAPI(\"downloadsV2\", \"queryLinks\", { \"name\": true})")
    public static Object callAPI(String namespace, String method, Object... parameters) throws EnvironmentException {
        askForPermission("call the Remote API: " + namespace + "/" + method);
        try {
            final Object ret = RemoteAPIController.getInstance().call(namespace, method, parameters);
            final ScriptThread env = getScriptThread();
            // convert to javascript object
            final String js = "(function(){ return " + JSonStorage.serializeToJson(ret) + ";}());";
            final Object retObject = env.evalTrusted(js);
            return retObject;
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

    @ScriptAPI(description = "Call a local Process. Blocks Until the process returns", parameters = { "\"commandline1\"", "\"commandline2\"", "\"...\"" }, example = "var pingResultString = callSync(\"ping\",\"jdownloader.org\");")
    public static String callSync(final String... commands) throws EnvironmentException {
        askForPermission("Execute a local process");
        try {
            ProcessBuilder pb = ProcessBuilderFactory.create(commands);
            pb.redirectErrorStream(true);
            ProcessOutput ret = ProcessBuilderFactory.runCommand(pb);
            return ret.getStdOutString();
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

    protected static boolean doCollectClass(Class<? extends Object> cl) {
        Package pkg = cl.getPackage();
        Package sPkg = ScriptEnvironment.class.getPackage();
        if (pkg == null || !pkg.getName().startsWith(sPkg.getName())) {
            return false;
        }
        return true;
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
            if (doCollectClass(cl)) {
                sb.append("/* === ").append(Utils.cleanUpClass(cl.getSimpleName())).append(" === */").append("\r\n");
                getAPIDescriptionForClass(sb, cl);
            }
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

    @ScriptAPI(description = "Set the Speedlimit in bytes/second. Values<=0 -> Disable Limiter", parameters = { "speedlimit in bytes/second" })
    public static void setSpeedlimit(int bps) throws EnvironmentException {
        if (bps > 0) {
            CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.setValue(bps);
            CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(true);
        } else {
            CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(false);
        }
    }

    @ScriptAPI(description = "Get a DownloadList Link by it's uuid", parameters = { "uuid" })
    public static DownloadLinkSandBox getDownloadLinkByUUID(long uuid) throws EnvironmentException {
        try {
            final DownloadLink link = DownloadController.getInstance().getLinkByID(uuid);
            if (link != null) {
                return new DownloadLinkSandBox(link);
            } else {
                return null;
            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Get a CrawledLink Link by it's uuid", parameters = { "uuid" })
    public static CrawledLinkSandbox getCrawledLinkByUUID(long uuid) throws EnvironmentException {
        try {
            final CrawledLink link = LinkCollector.getInstance().getLinkByID(uuid);
            if (link != null) {
                return new CrawledLinkSandbox(link);
            } else {
                return null;
            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Get a DownloadList Package by it's uuid", parameters = { "uuid" })
    public static FilePackageSandBox getDownloadPackageByUUID(long uuid) throws EnvironmentException {
        try {
            final FilePackage pkg = DownloadController.getInstance().getPackageByID(uuid);
            if (pkg != null) {
                return new FilePackageSandBox(pkg);
            } else {
                return null;
            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Get a CrawledLink Package by it's uuid", parameters = { "uuid" })
    public static CrawledPackageSandbox getCrawledPackageByUUID(long uuid) throws EnvironmentException {
        try {
            final CrawledPackage pkg = LinkCollector.getInstance().getPackageByID(uuid);
            if (pkg != null) {
                return new CrawledPackageSandbox(pkg);
            } else {
                return null;
            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Get a FilePath Object", parameters = { "Path to a file or folder" })
    public static FilePathSandbox getPath(String fileOrUrl) throws EnvironmentException {
        if (Application.getJavaVersion() >= Application.JAVA17) {
            return new FilePathSandbox17(fileOrUrl);
        } else {
            return new FilePathSandbox(fileOrUrl);
        }
    }

    @ScriptAPI(description = "Gets the value of the specified environment variable", parameters = { "environment variable" })
    public static String getEnv(final String variable) throws EnvironmentException {
        try {
            return System.getenv(variable);
        } catch (SecurityException e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Get an Environment Object")
    public static EnvironmentSandbox getEnvironment() throws EnvironmentException {
        return new EnvironmentSandbox();
    }

    @ScriptAPI(description = "Get an Environment Object")
    public static BrowserSandBox getBrowser() throws EnvironmentException {
        askForPermission("load resources from the internet");
        return new BrowserSandBox();
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

    public static Collection<Class<?>> getRequiredClasses() {
        final ArraySet<Class<?>> clazzes = new ArraySet<Class<?>>();
        collectClasses(ScriptEnvironment.class, clazzes);
        if (Application.getJavaVersion() >= Application.JAVA17) {
            clazzes.add(FilePathSandbox17.class);
            collectClasses(FilePathSandbox17.class, clazzes);
        }
        return clazzes;
    }

    @ScriptAPI(description = "Get a list of all running downloadlinks")
    public static DownloadLinkSandBox[] getRunningDownloadLinks() {
        final Set<SingleDownloadController> list = DownloadWatchDog.getInstance().getRunningDownloadLinks();
        final DownloadLinkSandBox[] ret = new DownloadLinkSandBox[list.size()];
        int i = 0;
        for (final SingleDownloadController dlc : list) {
            ret[i++] = new DownloadLinkSandBox(dlc.getDownloadLink());
        }
        return ret;
    }

    @ScriptAPI(description = "Get a list of all packages")
    public static FilePackageSandBox[] getAllFilePackages() {
        final List<FilePackage> list = DownloadController.getInstance().getPackagesCopy();
        final FilePackageSandBox[] ret = new FilePackageSandBox[list.size()];
        int i = 0;
        for (final FilePackage pkg : list) {
            ret[i++] = new FilePackageSandBox(pkg);
        }
        return ret;
    }

    @ScriptAPI(description = "Create a Checksum for a file. Types: e.g. CRC32, md5, SHA-1, SHA-256")
    public static String getChecksum(String type, String path) throws EnvironmentException {
        askForPermission("Create Checksum of local file");
        try {
            File rel = new File(path);
            if (!rel.isAbsolute()) {
                rel = Application.getResource(path);
            }
            if (StringUtils.equalsIgnoreCase("CRC32", type)) {
                return Hash.getCRC32(rel) + "";
            }
            return Hash.getFileHash(rel, type);
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Get a list of all crawledpackages")
    public static CrawledPackageSandbox[] getAllCrawledPackages() {
        final List<CrawledPackage> list = LinkCollector.getInstance().getPackagesCopy();
        final CrawledPackageSandbox[] ret = new CrawledPackageSandbox[list.size()];
        int i = 0;
        for (final CrawledPackage pkg : list) {
            ret[i++] = new CrawledPackageSandbox(pkg);
        }
        return ret;
    }

    @ScriptAPI(description = "Remove a downloadlink by uuid")
    public static boolean removeDownloadLinkByUUID(final String uuid) {
        final DownloadLink link = DownloadController.getInstance().getLinkByID(Long.parseLong(uuid));
        return link != null && new DownloadLinkSandBox(link).remove();
    }

    @ScriptAPI(description = "Remove a package by uuid")
    public static boolean removeFilePackageByUUID(final String uuid) {
        final FilePackage pkg = DownloadController.getInstance().getPackageByID(Long.parseLong(uuid));
        return pkg != null && new FilePackageSandBox(pkg).remove();
    }

    @ScriptAPI(description = "Remove a crawledlink by uuid")
    public static boolean removeCrawledLinkByUUID(final String uuid) {
        final CrawledLink link = LinkCollector.getInstance().getLinkByID(Long.parseLong(uuid));
        return link != null && new CrawledLinkSandbox(link).remove();
    }

    @ScriptAPI(description = "Remove a crawledpackage by uuid")
    public static boolean removeCrawledPackageByUUID(final String uuid) {
        final CrawledPackage pkg = LinkCollector.getInstance().getPackageByID(Long.parseLong(uuid));
        return pkg != null && new CrawledPackageSandbox(pkg).remove();
    }

    @ScriptAPI(description = "Get a list of all downloadlinks")
    public static DownloadLinkSandBox[] getAllDownloadLinks() {
        final List<DownloadLink> links = DownloadController.getInstance().getAllChildren();
        final DownloadLinkSandBox[] ret = new DownloadLinkSandBox[links.size()];
        int i = 0;
        for (final DownloadLink link : links) {
            ret[i++] = new DownloadLinkSandBox(link);
        }
        return ret;
    }

    @ScriptAPI(description = "Get a list of all crawledlinks")
    public static CrawledLinkSandbox[] getAllCrawledLinks() {
        final List<CrawledLink> links = LinkCollector.getInstance().getAllChildren();
        final CrawledLinkSandbox[] ret = new CrawledLinkSandbox[links.size()];
        int i = 0;
        for (final CrawledLink link : links) {
            ret[i++] = new CrawledLinkSandbox(link);
        }
        return ret;
    }

    @ScriptAPI(description = "Get a list of all running packages")
    public static FilePackageSandBox[] getRunningDownloadPackages() {
        final Set<FilePackage> list = DownloadWatchDog.getInstance().getRunningFilePackages();
        final FilePackageSandBox[] ret = new FilePackageSandBox[list.size()];
        int i = 0;
        for (final FilePackage dlc : list) {
            ret[i++] = new FilePackageSandBox(dlc);
        }
        return ret;
    }

    private static ScriptThread getScriptThread() {
        final Thread ct = Thread.currentThread();
        if (ct instanceof ScriptThread) {
            return (ScriptThread) ct;
        } else if (ct instanceof ScriptReferenceThread) {
            return ((ScriptReferenceThread) ct).getScriptThread();
        } else {
            throw new IllegalStateException();
        }
    }

    @ScriptAPI(description = "Get current total Download Speed in bytes/second")
    public static long getTotalSpeed() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed();
    }

    @ScriptAPI(description = "Get current average Download Speed in bytes/second")
    public static long getAverageSpeed() {
        return DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeedMeter().getSpeedMeter();
    }

    @ScriptAPI(description = "Stop Downloads")
    public static void stopDownloads() {
        DownloadWatchDog.getInstance().stopDownloads();
    }

    @ScriptAPI(description = "Start Downloads")
    public static void startDownloads() {
        DownloadWatchDog.getInstance().startDownloads();
    }

    @ScriptAPI(description = "Pause/Unpause Downloads")
    public static void setDownloadsPaused(boolean paused) {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(paused);
    }

    @ScriptAPI(description = "Check if Download Controller is in IDLE State")
    public static boolean isDownloadControllerIdle() {
        return DownloadWatchDog.getInstance().isIdle();
    }

    @ScriptAPI(description = "Check if Download Controller is in PAUSE State")
    public static boolean isDownloadControllerPaused() {
        return DownloadWatchDog.getInstance().isPaused();
    }

    @ScriptAPI(description = "Check if Download Controller is in RUNNING State")
    public static boolean isDownloadControllerRunning() {
        return DownloadWatchDog.getInstance().isRunning();
    }

    @ScriptAPI(description = "Check if Download Controller is in STOPPING State (Still running, but stop has been pressed)")
    public static boolean isDownloadControllerStopping() {
        return DownloadWatchDog.getInstance().isStopping();
    }

    @ScriptAPI(description = "Log to stderr and to JDownloader Log Files")
    public static void log(Object... objects) {
        if (objects != null && objects.length == 1) {
            if (Clazz.isPrimitiveWrapper(objects[0].getClass()) || Clazz.isString(objects[0].getClass())) {
                LOGGER.info(objects[0] + "");
                return;
            } else if (objects.length > 0) {
                if (objects[0] != null && objects[0].getClass().getPackage().getName().equals(DownloadLinkSandBox.class.getPackage().getName())) {
                    LOGGER.info(objects[0].toString());
                    return;
                }
                try {
                    try {
                        LOGGER.info(new JacksonMapper().objectToString(objects[0]));
                    } catch (Throwable e) {
                        LOGGER.info(format(toJson(objects[0])));
                    }
                } catch (Throwable e) {
                    LOGGER.info(objects[0] + "");
                }
                return;
            }
        }
        try {
            try {
                LOGGER.info(new JacksonMapper().objectToString(objects));
            } catch (Throwable e) {
                LOGGER.info(format(toJson(objects)));
            }
        } catch (Throwable e) {
            LOGGER.info(objects + "");
        }
        return;
    }

    @ScriptAPI(description = "Play a Wav Audio file", parameters = { "myFilePathOrUrl" }, example = "playWavAudio(JD_HOME+\"/themes/standard/org/jdownloader/sounds/captcha.wav\");")
    public static void playWavAudio(String fileOrUrl) throws EnvironmentException {
        try {
            AudioInputStream stream = null;
            Clip clip = null;
            try {
                stream = AudioSystem.getAudioInputStream(new File(fileOrUrl));
                final AudioFormat format = stream.getFormat();
                final DataLine.Info info = new DataLine.Info(Clip.class, format);
                if (AudioSystem.isLineSupported(info)) {
                    clip = (Clip) AudioSystem.getLine(info);
                    clip.open(stream);
                    try {
                        final FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        float db = (20f * (float) Math.log(JsonConfig.create(SoundSettings.class).getCaptchaSoundVolume() / 100f));
                        gainControl.setValue(Math.max(-80f, db));
                        BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
                        muteControl.setValue(true);
                        muteControl.setValue(false);
                    } catch (Exception e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                    final AtomicBoolean runningFlag = new AtomicBoolean(true);
                    clip.addLineListener(new LineListener() {
                        @Override
                        public void update(LineEvent event) {
                            if (event.getType() == Type.STOP) {
                                runningFlag.set(false);
                            }
                        }
                    });
                    clip.start();
                    Thread.sleep(1000);
                    while (clip.isRunning() && runningFlag.get()) {
                        Thread.sleep(100);
                    }
                }
            } finally {
                try {
                    if (clip != null) {
                        final Clip finalClip = clip;
                        Thread thread = new Thread() {
                            public void run() {
                                finalClip.close();
                            };
                        };
                        thread.setName("AudioStop");
                        thread.setDaemon(true);
                        thread.start();
                        thread.join(2000);
                    }
                } catch (Throwable e) {
                }
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (Throwable e) {
                }
            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
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
            final File file = new File(filepath);
            final Object lock = requestLock(file);
            try {
                synchronized (lock) {
                    return IO.readFileToString(file);
                }
            } finally {
                unLock(file);
            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Request a reconnect", parameters = {}, example = "requestReconnect();")
    public static void requestReconnect() throws EnvironmentException {
        try {
            DownloadWatchDog.getInstance().requestReconnect(false);
        } catch (InterruptedException e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Refresh all premium accounts", parameters = { "true|false (Wait for account checks)", "true|false (Force Check)" }, example = "refreshAccounts(true,true);")
    public static void refreshAccounts(final boolean wait, final boolean force) throws EnvironmentException {
        final QueueAction<Void, InterruptedException> action = new QueueAction<Void, InterruptedException>() {
            @Override
            protected Void run() throws InterruptedException {
                final List<AccountCheckJob> jobs = new ArrayList<AccountCheckJob>();
                for (final Account acc : AccountController.getInstance().list()) {
                    if (acc.getPlugin() != null && acc.isEnabled() && acc.isValid()) {
                        final AccountCheckJob job = AccountChecker.getInstance().check(acc, force);
                        if (wait && job != null) {
                            jobs.add(job);
                        }
                    }
                }
                for (final AccountCheckJob job : jobs) {
                    while (!job.isChecked()) {
                        Thread.sleep(100);
                    }
                }
                return null;
            }
        };
        try {
            if (wait) {
                TaskQueue.getQueue().addWait(action);
            } else {
                TaskQueue.getQueue().add(action);
            }
        } catch (InterruptedException e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Perform a reconnect and wait for it", parameters = {}, example = "var success= doReconnect();")
    public static boolean doReconnect() throws EnvironmentException {
        try {
            return DownloadWatchDog.getInstance().requestReconnect(true) == Reconnecter.ReconnectResult.SUCCESSFUL;
        } catch (InterruptedException e) {
            throw new EnvironmentException(e);
        }
    }

    @ScriptAPI(description = "Perform a sleep and wait for x milliseconds", parameters = { "milliseconds" }, example = "sleep(1000);")
    public static void sleep(int millis) throws EnvironmentException {
        try {
            if (millis > 0) {
                Thread.sleep(millis);
            }
        } catch (InterruptedException e) {
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

    private static void showMessageDialog(String string) {
        final ScriptThread env = getScriptThread();
        UIOManager.I().show(ConfirmDialogInterface.class, new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL | Dialog.STYLE_LARGE, T.T.showMessageDialog_title(env.getScript().getName(), env.getScript().getEventTrigger().getLabel()), string, new AbstractIcon(IconKey.ICON_INFO, 32), null, null) {
            @Override
            protected int getPreferredWidth() {
                return 600;
            }

            @Override
            public boolean isRemoteAPIEnabled() {
                return false;
            }

            @Override
            protected void modifyTextPane(JTextPane textField) {
            }

            @Override
            public void pack() {
                this.getDialog().pack();
            }
        });
    }

    public static Object toJSObject(Object ret) {
        final ScriptThread env = getScriptThread();
        // convert to javascript object
        final String js = "(function(){ return " + JSonStorage.serializeToJson(ret) + ";}());";
        return env.evalTrusted(js);
    }

    public static String toJson(Object ret) {
        final ScriptThread env = getScriptThread();
        // convert to javascript object
        final String js = "(function(){ return JSON.stringify(" + JSonStorage.serializeToJson(ret) + ");}());";
        return (String) env.evalTrusted(js);
    }

    private static final HashMap<File, AtomicInteger> LOCKS = new HashMap<File, AtomicInteger>();

    private static synchronized Object requestLock(File name) {
        AtomicInteger lock = LOCKS.get(name);
        if (lock == null) {
            lock = new AtomicInteger(0);
            LOCKS.put(name, lock);
        }
        lock.incrementAndGet();
        return lock;
    }

    private static synchronized void unLock(File name) {
        AtomicInteger lock = LOCKS.get(name);
        if (lock != null) {
            if (lock.decrementAndGet() == 0) {
                LOCKS.remove(name);
            }
        }
    }

    @ScriptAPI(description = "Write a text file", parameters = { "filepath", "myText", "append" }, example = "writeFile(JD_HOME+\"/log.txt\",JSON.stringify(this)+\"\\r\\n\",true);")
    public static void writeFile(String filepath, String string, boolean append) throws EnvironmentException {
        askForPermission("create a local file and write to it");
        try {
            final File file = new File(filepath);
            final Object lock = requestLock(file);
            try {
                synchronized (lock) {
                    IO.writeStringToFile(file, string, append);
                }
            } finally {
                unLock(file);
            }
        } catch (Throwable e) {
            throw new EnvironmentException(e);
        }
    }
}