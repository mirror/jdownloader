package org.jdownloader.extensions;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.io.J7FileList;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.UpdateController;

public class ExtensionController implements MenuExtenderHandler {
    private static final String              TMP_INVALIDEXTENSIONS = "invalidextensions";
    private static final ExtensionController INSTANCE              = new ExtensionController();

    /**
     * get the only existing instance of ExtensionController. This is a singleton
     *
     * @return
     */
    public static ExtensionController getInstance() {
        return ExtensionController.INSTANCE;
    }

    private volatile List<LazyExtension>         list = new ArrayList<LazyExtension>();
    private final ExtensionControllerEventSender eventSender;
    private final LogSource                      logger;

    /**
     * Create a new instance of ExtensionController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ExtensionController() {
        eventSender = new ExtensionControllerEventSender();
        logger = LogController.getInstance().getLogger(ExtensionController.class.getName());
    }

    private File getCache() {
        return Application.getTempResource("extensioncache/extensionInfos.json");
    }

    public ExtensionControllerEventSender getEventSender() {
        return eventSender;
    }

    private boolean                             cacheInvalidated      = false;
    private volatile List<UninstalledExtension> uninstalledExtensions = new ArrayList<UninstalledExtension>();

    public boolean isCacheInvalidated() {
        return cacheInvalidated;
    }

    public void invalidateCache() {
        this.cacheInvalidated = true;
    }

    protected void validateCache() {
        cacheInvalidated = false;
        FileCreationManager.getInstance().delete(Application.getTempResource(TMP_INVALIDEXTENSIONS), null);
    }

    public void init() {
        synchronized (this) {
            java.util.List<LazyExtension> ret = new ArrayList<LazyExtension>();
            final long t = System.currentTimeMillis();
            try {
                if (isCacheInvalidated()) {
                    try {
                        /* do a fresh scan */
                        ret = load();
                    } catch (Throwable e) {
                        logger.severe("@ExtensionController: update failed!");
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                } else {
                    /* try to load from cache */
                    try {
                        ret = loadFromCache();
                    } catch (Throwable e) {
                        logger.severe("@ExtensionController: cache failed!");
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                    if (ret.size() == 0) {
                        try {
                            /* do a fresh scan */
                            ret = load();
                        } catch (Throwable e) {
                            logger.severe("@ExtensionController: update failed!");
                            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                        }
                    }
                }
            } finally {
                logger.info("@ExtensionController: init in" + (System.currentTimeMillis() - t) + "ms :" + ret.size());
                for (final LazyExtension lazyExtension : ret) {
                    logger.info("@ExtensionController:" + lazyExtension.getName());
                }
            }
            if (ret.size() == 0) {
                logger.severe("@ExtensionController: WTF, no extensions!");
            }
            try {
                Collections.sort(ret, new Comparator<LazyExtension>() {
                    public int compare(LazyExtension o1, LazyExtension o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            if (!org.appwork.utils.Application.isHeadless()) {
                MenuManagerMainmenu.getInstance().registerExtender(this);
                MenuManagerMainToolbar.getInstance().registerExtender(this);
            }
            list = Collections.unmodifiableList(ret);
            initUninstalledExtensions();
            if (!org.appwork.utils.Application.isHeadless()) {
                SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                    public void run() {
                        List<LazyExtension> llist = list;
                        for (LazyExtension plg : llist) {
                            if (plg._getExtension() != null && plg._getExtension().getGUI() != null) {
                                plg._getExtension().getGUI().restore();
                            }
                        }
                    }
                });
            }
        }
        getEventSender().fireEvent(new ExtensionControllerEvent(this, ExtensionControllerEvent.Type.UPDATED));
    }

    private void initUninstalledExtensions() {
        ArrayList<UninstalledExtension> ret = new ArrayList<UninstalledExtension>();
        HashSet<String> set = new HashSet<String>();
        for (LazyExtension l : list) {
            set.add(l.getClassname());
        }
        if (set.add("org.jdownloader.extensions.eventscripter.EventScripterExtension") || !Application.isJared(null)) {
            ret.add(new UninstalledExtension("eventscripter", IconKey.ICON_EVENT, _GUI.T.ExtensionController_initUninstalledExtensions_EventScripterExtension(), _GUI.T.ExtensionController_initUninstalledExtensions_EventScripterExtension_description()));
        }
        if (set.add("org.jdownloader.extensions.folderwatchV2.FolderWatchExtension") || !Application.isJared(null)) {
            ret.add(new UninstalledExtension("folderwatch", IconKey.ICON_FOLDER_ADD, _GUI.T.ExtensionController_initUninstalledExtensions_FolderWatchExtension(), _GUI.T.ExtensionController_initUninstalledExtensions_FolderWatchExtension_description()));
        }
        if (set.add("org.jdownloader.extensions.schedulerV2.SchedulerExtension") || !Application.isJared(null)) {
            ret.add(new UninstalledExtension("scheduler", IconKey.ICON_WAIT, _GUI.T.ExtensionController_initUninstalledExtensions_SchedulerExtension(), _GUI.T.ExtensionController_initUninstalledExtensions_SchedulerExtension_description()));
        }
        if (!Application.isHeadless()) {
            if (set.add("org.jdownloader.extensions.infobar.InfoBarExtension") || !Application.isJared(null)) {
                ret.add(new UninstalledExtension("infobar", IconKey.ICON_INFO, _GUI.T.ExtensionController_initUninstalledExtensions_INFOBAR(), _GUI.T.ExtensionController_initUninstalledExtensions_INFOBAR_description()));
            }
            if (set.add("org.jdownloader.extensions.chat.ChatExtension") || !Application.isJared(null)) {
                ret.add(new UninstalledExtension("chat", IconKey.ICON_CHAT, _GUI.T.ExtensionController_initUninstalledExtensions_JDChat(), _GUI.T.ExtensionController_initUninstalledExtensions_JDChat_description()));
            }
            if (set.add("org.jdownloader.extensions.translator.TranslatorExtension") || !Application.isJared(null)) {
                ret.add(new UninstalledExtension("translator", IconKey.ICON_LANGUAGE, _GUI.T.ExtensionController_initUninstalledExtensions_TranslatorExtension(), _GUI.T.ExtensionController_initUninstalledExtensions_TranslatorExtension_description()));
            }
        }
        if (UpdateController.getInstance().isHandlerSet()) {
            // reinstall extensions if we could not load them
            ArrayList<String> list = new ArrayList<String>();
            for (UninstalledExtension ue : ret) {
                if (UpdateController.getInstance().isExtensionInstalled(ue.getId())) {
                    list.add(ue.getId());
                }
            }
            try {
                // UpdateController.getInstance().runExtensionUnInstallation(ue.getId());
                UpdateController.getInstance().runExtensionsFullUpdate(list);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        uninstalledExtensions = Collections.unmodifiableList(ret);
    }

    private java.util.List<LazyExtension> loadFromCache() throws InstantiationException, IllegalAccessException, ClassNotFoundException, StartException {
        java.util.List<LazyExtension> cache = JSonStorage.restoreFrom(getCache(), true, null, new TypeRef<ArrayList<LazyExtension>>() {
        }, new ArrayList<LazyExtension>());
        java.util.List<LazyExtension> lst = new ArrayList<LazyExtension>(cache);
        for (Iterator<LazyExtension> it = lst.iterator(); it.hasNext();) {
            LazyExtension l = it.next();
            if (l.getJarPath() == null || !new File(l.getJarPath()).exists()) {
                throw new InstantiationException("Jar Path " + l.getJarPath() + " is invalid");
            }
            l.validateCache();
            if (l._isEnabled()) {
                // if exception occures here, we do a complete rescan. cache
                // might be out of date
                l.init();
            }
        }
        return lst;
    }

    private synchronized java.util.List<LazyExtension> load() {
        final List<LazyExtension> ret;
        if (Application.isJared(ExtensionController.class)) {
            ret = loadJared();
        } else {
            ret = loadUnpacked();
        }
        JSonStorage.saveTo(getCache(), ret);
        validateCache();
        return ret;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<LazyExtension> loadJared() {
        java.util.List<LazyExtension> ret = new ArrayList<LazyExtension>();
        File[] addons = null;
        if (Application.getJavaVersion() >= Application.JAVA17) {
            try {
                addons = J7FileList.findFiles(Pattern.compile("^.*\\.jar$", Pattern.CASE_INSENSITIVE), Application.getResource("extensions"), true).toArray(new File[0]);
            } catch (IOException e) {
                logger.log(e);
            }
        }
        if (addons == null || addons.length == 0) {
            addons = Application.getResource("extensions").listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
        }
        if (addons != null) {
            logger.info("Extensions:" + Arrays.toString(addons));
            HashSet<File> dupes = new HashSet<File>();
            HashSet<URL> urlDupes = new HashSet<URL>();
            main: for (File jar : addons) {
                try {
                    URLClassLoader cl = new URLClassLoader(new URL[] { jar.toURI().toURL() }, getClass().getClassLoader());
                    String resource = AbstractExtension.class.getPackage().getName().replace('.', '/');
                    final Enumeration<URL> urls = cl.getResources(resource);
                    URL url;
                    Pattern pattern = Pattern.compile(Pattern.quote(AbstractExtension.class.getPackage().getName().replace('.', '/')) + "/(\\w+)/(\\w+Extension)\\.class");
                    boolean atLeastOneSourceFound = false;
                    while (urls.hasMoreElements()) {
                        atLeastOneSourceFound = true;
                        url = urls.nextElement();
                        if (urlDupes.add(url)) {
                            if ("jar".equalsIgnoreCase(url.getProtocol())) {
                                // jarred addon (JAR)
                                File jarFile = new File(new URL(url.getPath().substring(0, url.getPath().lastIndexOf('!'))).toURI());
                                if (dupes.add(jarFile)) {
                                    FileInputStream fis = null;
                                    JarInputStream jis = null;
                                    try {
                                        jis = new JarInputStream(fis = new FileInputStream(jarFile));
                                        JarEntry e;
                                        while ((e = jis.getNextJarEntry()) != null) {
                                            try {
                                                Matcher matcher = pattern.matcher(e.getName());
                                                if (matcher.find()) {
                                                    String pkg = matcher.group(1);
                                                    String clazzName = matcher.group(2);
                                                    Class<?> clazz = new URLClassLoader(new URL[] { jar.toURI().toURL() }, getClass().getClassLoader()).loadClass(AbstractExtension.class.getPackage().getName() + "." + pkg + "." + clazzName);
                                                    if (AbstractExtension.class.isAssignableFrom(clazz)) {
                                                        initModule((Class<AbstractExtension<?, ?>>) clazz, ret, jarFile);
                                                        continue main;
                                                    }
                                                }
                                            } catch (Throwable e1) {
                                                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
                                            }
                                        }
                                    } finally {
                                        try {
                                            jis.close();
                                        } catch (final Throwable e) {
                                        }
                                        try {
                                            fis.close();
                                        } catch (final Throwable e) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!atLeastOneSourceFound) {
                        logger.severe("Seems Like " + jar + " was built without directory option. This slows down the loading process!");
                        JarFile jf = new JarFile(jar);
                        Enumeration<JarEntry> entries = jf.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry s = entries.nextElement();
                            if (s.getName().startsWith(resource)) {
                                URI uri = jar.toURI();
                                url = new URL(uri.toString() + "!" + s.getName());
                                if (urlDupes.add(url)) {
                                    if (dupes.add(jar)) {
                                        FileInputStream fis = null;
                                        JarInputStream jis = null;
                                        try {
                                            jis = new JarInputStream(fis = new FileInputStream(jar));
                                            JarEntry e;
                                            while ((e = jis.getNextJarEntry()) != null) {
                                                try {
                                                    Matcher matcher = pattern.matcher(e.getName());
                                                    if (matcher.find()) {
                                                        String pkg = matcher.group(1);
                                                        String clazzName = matcher.group(2);
                                                        URLClassLoader classloader = new URLClassLoader(new URL[] { jar.toURI().toURL() }, getClass().getClassLoader());
                                                        Class<?> clazz = classloader.loadClass(AbstractExtension.class.getPackage().getName() + "." + pkg + "." + clazzName);
                                                        if (AbstractExtension.class.isAssignableFrom(clazz)) {
                                                            initModule((Class<AbstractExtension<?, ?>>) clazz, ret, jar);
                                                            continue main;
                                                        }
                                                    }
                                                } catch (Throwable e1) {
                                                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
                                                }
                                            }
                                        } finally {
                                            try {
                                                jis.close();
                                            } catch (final Throwable e) {
                                            }
                                            try {
                                                fis.close();
                                            } catch (final Throwable e) {
                                            }
                                        }
                                    }
                                }
                            }
                            System.out.println(s);
                        }
                    }
                } catch (Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<LazyExtension> loadUnpacked() {
        java.util.List<LazyExtension> retl = new ArrayList<LazyExtension>();
        final URL ret = getClass().getResource("/");
        File root;
        if ("file".equalsIgnoreCase(ret.getProtocol())) {
            try {
                root = new File(ret.toURI());
            } catch (URISyntaxException e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                logger.finer("Did not load unpacked Extensions from " + ret);
                return retl;
            }
        } else {
            logger.finer("Did not load unpacked Extensions from " + ret);
            return retl;
        }
        root = new File(root, AbstractExtension.class.getPackage().getName().replace('.', '/'));
        if (!root.exists()) {
            // workaround. this way extensions work in eclipse when started with the updater launcher as well
            root = new File(root.getAbsolutePath().replace("JDownloaderUpdater", "JDownloader"));
        }
        logger.finer("Load Extensions from: " + root.getAbsolutePath());
        File[] folders = root.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (folders != null) {
            ClassLoader cl = getClass().getClassLoader();
            main: for (File f : folders) {
                File[] modules = null;
                if (Application.getJavaVersion() >= Application.JAVA17) {
                    try {
                        modules = J7FileList.findFiles(Pattern.compile("^.*Extension\\.class$", Pattern.CASE_INSENSITIVE), f, true).toArray(new File[0]);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                }
                if (modules == null) {
                    modules = f.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith("Extension.class");
                        }
                    });
                }
                boolean loaded = false;
                if (modules != null) {
                    for (File module : modules) {
                        Class<?> cls;
                        try {
                            cls = cl.loadClass(AbstractExtension.class.getPackage().getName() + "." + module.getParentFile().getName() + "." + module.getName().substring(0, module.getName().length() - 6));
                            if (AbstractExtension.class.isAssignableFrom(cls)) {
                                initModule((Class<AbstractExtension<?, ?>>) cls, retl, f);
                                loaded = true;
                                continue main;
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warning("Did not init Extension " + module + " : " + e.getMessage());
                        } catch (Throwable e) {
                            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                            UIOManager.I().showConfirmDialog(0, e.getMessage(), Exceptions.getStackTrace(e));
                            // UIOManager.I().show(ExceptionDialogInterface.class, new
                            // ExceptionDialog(UIOManager.LOGIC_DONT_SHOW_AGAIN_DELETE_ON_EXIT | UIOManager.BUTTONS_HIDE_CANCEL, "Error",
                            // e.getMessage(), e, null, null));
                        }
                    }
                    if (!loaded) {
                        logger.warning("Could not load any Extension Module from " + f);
                    }
                }
            }
        }
        return retl;
    }

    private java.util.List<LazyExtension> initModule(Class<AbstractExtension<?, ?>> cls, java.util.List<LazyExtension> list, File jarFile) throws InstantiationException, IllegalAccessException, StartException, IOException, ClassNotFoundException {
        long t = System.currentTimeMillis();
        if (list == null) {
            list = new ArrayList<LazyExtension>();
        }
        String id = cls.getName().substring(27);
        logger.fine("Load Extension: " + id);
        LazyExtension extension = LazyExtension.create(id, cls);
        if (!extension.isHeadlessRunnable() && org.appwork.utils.Application.isHeadless()) {
            logger.info("Ignored Extension (Headless): " + id + " Load Duration:" + (System.currentTimeMillis() - t) + "ms");
            return list;
        }
        extension.setJarPath(jarFile.getAbsolutePath());
        extension._setPluginClass(cls);
        if (extension._isEnabled()) {
            extension.init();
        }
        list.add(extension);
        logger.info("Loaded Extension: " + id + " Load Duration:" + (System.currentTimeMillis() - t) + "ms");
        return list;
    }

    public boolean isExtensionActive(Class<? extends AbstractExtension<?, ?>> class1) {
        List<LazyExtension> llist = list;
        for (LazyExtension l : llist) {
            if (class1.getName().equals(l.getClassname())) {
                if (l._getExtension() != null && l._getExtension().isEnabled()) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<LazyExtension> getExtensions() {
        return list;
    }

    /**
     * Returns a list of all currently running extensions
     *
     * @return
     */
    public java.util.List<AbstractExtension<?, ?>> getEnabledExtensions() {
        java.util.List<AbstractExtension<?, ?>> ret = new ArrayList<AbstractExtension<?, ?>>();
        List<LazyExtension> llist = list;
        for (LazyExtension aew : llist) {
            if (aew._getExtension() != null && aew._getExtension().isEnabled()) {
                ret.add(aew._getExtension());
            }
        }
        return ret;
    }

    public <T extends AbstractExtension<?, ?>> LazyExtension getExtension(Class<T> class1) {
        List<LazyExtension> llist = list;
        for (LazyExtension l : llist) {
            if (class1.getName().equals(l.getClassname())) {
                return l;
            }
        }
        return null;
    }

    public <T extends AbstractExtension<?, ?>> LazyExtension getExtension(String classname) {
        List<LazyExtension> llist = list;
        for (LazyExtension l : llist) {
            if (classname.equals(l.getClassname())) {
                return l;
            }
        }
        return null;
    }

    public void invalidateCacheIfRequired() {
        if (Application.getTempResource(TMP_INVALIDEXTENSIONS).exists()) {
            invalidateCache();
        }
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException, ExtensionNotLoadedException {
        if (list == null || list.size() == 0) {
            //
            throw new ExtensionNotLoadedException(className);
        }
        ClassNotFoundException exc = null;
        for (AbstractExtension<?, ?> ae : getEnabledExtensions()) {
            if (className.startsWith(ae.getClass().getPackage().getName())) {
                try {
                    return Class.forName(className, true, ae.getClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    exc = e;
                }
            }
        }
        if (exc != null) {
            throw exc;
        }
        for (LazyExtension le : getExtensions()) {
            String packageName = le.getClassname();
            packageName = packageName.substring(0, packageName.lastIndexOf("."));
            if (className.startsWith(packageName)) {
                //
                throw new ExtensionNotLoadedException(le.getClassname() + " - " + className);
            }
        }
        throw new ClassNotFoundException(className);
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        java.util.List<LazyExtension> pluginsOptional = new ArrayList<LazyExtension>(getExtensions());
        Collections.sort(pluginsOptional, new Comparator<LazyExtension>() {
            public int compare(LazyExtension o1, LazyExtension o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        if (manager instanceof MenuManagerMainToolbar) {
            return updateMainToolbar(pluginsOptional, mr);
        } else if (manager instanceof MenuManagerMainmenu) {
            return updateMainMenu(pluginsOptional, mr);
        }
        return null;
    }

    private MenuItemData updateMainMenu(List<LazyExtension> pluginsOptional, MenuContainerRoot mr) {
        MenuContainerRoot root = new MenuContainerRoot();
        root._setOwner(MenuManagerMainmenu.getInstance());
        ExtensionsMenuContainer container = new ExtensionsMenuContainer();
        OptionalContainer opt = new OptionalContainer(false);
        root.add(container);
        root.add(opt);
        // AddonsMenuWindowContainer windows = new AddonsMenuWindowContainer();
        // container.add(windows);
        for (final LazyExtension wrapper : pluginsOptional) {
            try {
                if (wrapper.isQuickToggle()) {
                    MenuItemData m = new MenuItemData(new ActionData(ExtensionQuickToggleAction.class, wrapper.getClassname()));
                    container.add(m);
                } else {
                    // MenuItemData m = new MenuItemData(new ActionData(ExtensionQuickToggleAction.class, wrapper.getClassname()));
                    //
                    // opt.add(m);
                }
            } catch (Exception e) {
                logger.log(e);
            }
        }
        return root;
    }

    private MenuItemData updateMainToolbar(List<LazyExtension> pluginsOptional, MenuContainerRoot mr) {
        // AddonsMenuWindowContainer windows = new AddonsMenuWindowContainer();
        // container.add(windows);
        OptionalContainer opt = new OptionalContainer(false);
        ExtensionsMenuContainer container = new ExtensionsMenuContainer();
        opt.add(container);
        for (final LazyExtension wrapper : pluginsOptional) {
            try {
                if (wrapper.isQuickToggle()) {
                    MenuItemData m = new MenuItemData(new ActionData(ExtensionQuickToggleAction.class, wrapper.getClassname()));
                    container.add(m);
                }
            } catch (Exception e) {
                logger.log(e);
            }
        }
        return opt;
    }

    public void setEnabled(LazyExtension object, boolean value) {
        if (value) {
            try {
                object._setEnabled(true);
                if (object instanceof LazyExtension) {
                    if (object._getExtension().getGUI() != null && object._getExtension().isGuiOptional()) {
                        if (JDGui.bugme(WarnLevel.NORMAL)) {
                            ConfirmDialogInterface io = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, object.getName(), _JDT.T.gui_settings_extensions_show_now(object.getName())).show();
                            if (io.getCloseReason() == CloseReason.OK) {
                                // activate panel
                                object._getExtension().getGUI().setActive(true);
                                // bring panel to front
                                object._getExtension().getGUI().toFront();
                            }
                        } else {
                            // activate panel
                            object._getExtension().getGUI().setActive(true);
                            // bring panel to front
                            object._getExtension().getGUI().toFront();
                        }
                    } else if (!object._getExtension().isGuiOptional()) {
                        // activate panel
                        object._getExtension().getGUI().setActive(true);
                        // bring panel to front
                        object._getExtension().getGUI().toFront();
                    }
                }
            } catch (StartException e1) {
                Dialog.getInstance().showExceptionDialog(_JDT.T.dialog_title_exception(), e1.getMessage(), e1);
            } catch (StopException e1) {
                e1.printStackTrace();
            }
        } else {
            try {
                object._setEnabled(false);
            } catch (StartException e1) {
                e1.printStackTrace();
            } catch (StopException e1) {
                Dialog.getInstance().showExceptionDialog(_JDT.T.dialog_title_exception(), e1.getMessage(), e1);
            }
        }
    }

    public List<UninstalledExtension> getUninstalledExtensions() {
        return uninstalledExtensions;
    }
}
