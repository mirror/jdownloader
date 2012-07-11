package org.jdownloader.extensions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.sidebar.CheckBoxedEntry;
import jd.plugins.ExtensionConfigInterface;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

/**
 * Wraps around an extension to avoid extension init if the extension is not enabled
 * 
 * @author thomas
 * 
 */
public class LazyExtension implements Storable, CheckBoxedEntry {

    private Class<AbstractExtension<?, ?>> clazz;

    private boolean                        settings;

    private String                         configInterface;

    public static LazyExtension create(String id, Class<AbstractExtension<?, ?>> cls) throws StartException, InstantiationException, IllegalAccessException, IOException {
        LazyExtension ret = new LazyExtension();
        AbstractExtension<?, ?> plg = (AbstractExtension<?, ?>) cls.newInstance();

        String path = "tmp/extensioncache/" + id + ".png";
        File iconCache = Application.getResource(path);
        iconCache.getParentFile().mkdirs();
        iconCache.delete();
        ret.description = plg.getDescription();
        ImageIO.write(IconIO.toBufferedImage(plg.getIcon(32).getImage()), "png", iconCache);
        ret.iconPath = path;
        ret.linuxRunnable = plg.isLinuxRunnable();
        ret.macRunnable = plg.isMacRunnable();
        ret.settings = plg.hasConfigPanel();
        ret.name = plg.getName();
        ret.lng = _JDT.getLanguage();
        ret.version = plg.getVersion();
        ret.windowsRunnable = plg.isWindowsRunnable();
        ret.classname = cls.getName();
        ret.extension = plg;
        ret.configInterface = plg.getConfigClass().getName();
        ret.quickToggleEnabled = plg.isQuickToggleEnabled();
        plg.init();

        //
        //
        //
        //
        // if (!plg.isWindowsRunnable() && CrossSystem.isWindows()) throw new
        // IllegalArgumentException("Module is not for windows");
        // if (!plg.isMacRunnable() && CrossSystem.isMac()) throw new
        // IllegalArgumentException("Module is not for mac");
        // if (!plg.isLinuxRunnable() && CrossSystem.isLinux()) throw new
        // IllegalArgumentException("Module is not for linux");

        return ret;
    }

    public String getConfigInterface() {
        return extension == null ? configInterface : extension.getConfigClass().getName();
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public boolean isSettings() {
        return extension == null ? settings : extension.hasConfigPanel();
    }

    public void setSettings(boolean settings) {
        this.settings = settings;
    }

    private String                  description;
    private AbstractExtension<?, ?> extension = null;
    private String                  lng;

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    private String      iconPath;

    private boolean     linuxRunnable;

    private boolean     macRunnable;

    private String      name;

    private int         version;

    private boolean     windowsRunnable;

    private boolean     quickToggleEnabled;

    private ClassLoader classLoader;

    public void setQuickToggleEnabled(boolean quickToggleEnabled) {
        this.quickToggleEnabled = quickToggleEnabled;
    }

    public LazyExtension() {
        // required for Storable
    }

    /**
     * get the internal Extension Object. <br>
     * <b>do not remove the "_" in methodname. it is important to ignore this getter during Storable serialisation<b><br>
     * 
     * @return
     */
    public AbstractExtension<?, ?> _getExtension() {

        return extension;
    }

    /**
     * creates an icon in the given size. <br>
     * <b>do not remove the "_" in methodname. it is important to ignore this getter during Storable serialisation<b><br>
     * 
     * @param size
     * @return
     */
    public ImageIcon _getIcon(int size) {
        if (extension == null) {

            return NewTheme.I().getScaledInstance(NewTheme.I().getIcon(Application.getRessourceURL(iconPath)), size);

        } else {
            return extension.getIcon(size);
        }
    }

    public String getDescription() {
        return extension == null ? description : extension.getDescription();
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getName() {

        return extension == null ? name : extension.getName();
    }

    public int getVersion() {
        return extension == null ? version : extension.getVersion();
    }

    public boolean isLinuxRunnable() {
        return extension == null ? linuxRunnable : extension.isLinuxRunnable();
    }

    public boolean isMacRunnable() {
        return extension == null ? macRunnable : extension.isMacRunnable();
    }

    public boolean isWindowsRunnable() {
        return extension == null ? windowsRunnable : extension.isWindowsRunnable();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public void setLinuxRunnable(boolean linuxRunnable) {
        this.linuxRunnable = linuxRunnable;
    }

    public void setMacRunnable(boolean macRunnable) {
        this.macRunnable = macRunnable;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWindowsRunnable(boolean windowsRunnable) {
        this.windowsRunnable = windowsRunnable;
    }

    /**
     * inits the extensions. afterwards, you can access the extensionby calling {@link #_getExtension()}
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws StartException
     */
    public void init() throws InstantiationException, IllegalAccessException, ClassNotFoundException, StartException {
        if (extension != null) return;
        AbstractExtension<?, ?> plg = newInstance();

        plg.init();
        extension = plg;
    }

    /**
     * Checks whether this extension is enabled or not.<br>
     * <b>do not remove the "_" in methodname. it is important to ignore this getter during Storable serialisation<b><br>
     * 
     * @return
     */
    public boolean _isEnabled() {
        if (extension == null) {
            // not init yet. check storage to return if we have to init it
            ExtensionConfigInterface ret = _getSettings();
            if (ret == null) return false;
            return ret.isEnabled();
        } else {
            return extension.isEnabled();
        }
    }

    /**
     * Starts or stops the extension. If the extension has not been initialized yet, we do this
     * 
     * @param b
     * @throws StopException
     * @throws StartException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void _setEnabled(boolean b) throws StartException, StopException {

        if (extension == null) {
            ExtensionConfigInterface ret = _getSettings();
            if (ret == null) return;
            ret.setEnabled(b);
            if (b) {
                try {
                    init();
                } catch (Throwable e) {
                    throw new StartException(e);
                }
            }
        } else {
            extension.setEnabled(b);
        }

    }

    @SuppressWarnings("unchecked")
    public ExtensionConfigInterface _getSettings() {
        try {

            return AbstractExtension.createStore(getClassname(), (Class<? extends ExtensionConfigInterface>) Class.forName(getConfigInterface(), true, getClassLoader()));
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    private synchronized ClassLoader getClassLoader() {

        if (classLoader == null) {
            if (jarPath == null || !jarPath.endsWith(".jar")) {

                classLoader = LazyExtension.class.getClassLoader();
            } else {
                // jared cache loader
                try {
                    Log.L.info("Use " + jarPath + " classloader");
                    classLoader = new URLClassLoader(new URL[] { new File(jarPath).toURI().toURL() });
                } catch (MalformedURLException e) {
                    Log.L.info("WTF");
                    e.printStackTrace();
                    throw new WTFException(e);
                }
            }
        }

        return classLoader;
    }

    public boolean isQuickToggleEnabled() {
        return extension == null ? quickToggleEnabled : extension.isQuickToggleEnabled();
    }

    private String                           classname;

    protected Class<AbstractExtension<?, ?>> pluginClass;

    public void _setPluginClass(Class<AbstractExtension<?, ?>> pluginClass) {
        this.pluginClass = pluginClass;
    }

    private Constructor<AbstractExtension<?, ?>> constructor;

    private String                               jarPath;

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    private AbstractExtension<?, ?> newInstance() {
        try {
            _getConstructor();
            return constructor.newInstance();
        } catch (final Throwable e) {
            throw new WTFException(e);
        }

    }

    private Constructor<AbstractExtension<?, ?>> _getConstructor() {
        if (constructor != null) return constructor;
        synchronized (this) {
            if (constructor != null) return constructor;
            try {
                constructor = _getPluginClass().getConstructor(new Class[] {});

            } catch (Throwable e) {

                throw new WTFException(e);

            }
            return constructor;
        }
    }

    @SuppressWarnings("unchecked")
    protected Class<AbstractExtension<?, ?>> _getPluginClass() {
        if (pluginClass != null) return pluginClass;
        synchronized (this) {
            if (pluginClass != null) return pluginClass;
            try {
                pluginClass = (Class<AbstractExtension<?, ?>>) Class.forName(classname, true, getClassLoader());
            } catch (Throwable e) {
                throw new WTFException(e);
            }
            return pluginClass;
        }

    }

    public void setJarPath(String absolutePath) {
        jarPath = absolutePath;
    }

    public String getJarPath() {
        return jarPath;
    }

}
