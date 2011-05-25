package org.jdownloader.extensions;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.Storable;
import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

/**
 * Wraps around an extension to avoid extension init if the extension is not
 * enabled
 * 
 * @author thomas
 * 
 */
public class AbstractExtensionWrapper implements Storable {

    private Class<AbstractExtension<?>> clazz;

    private boolean                     settings;

    private String                      configInterface;

    public static AbstractExtensionWrapper create(String id, Class<AbstractExtension<?>> cls) throws StartException, InstantiationException, IllegalAccessException, IOException {
        AbstractExtensionWrapper ret = new AbstractExtensionWrapper();
        AbstractExtension<?> plg = (AbstractExtension<?>) cls.newInstance();

        ret.clazz = cls;
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
        ret.extension = plg;
        ret.configInterface = plg.getConfigClass().getName();
        ret.author = plg.getAuthor();
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

    private String               author;

    private String               description;
    private AbstractExtension<?> extension = null;
    private String               lng;

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    private String  iconPath;

    private boolean linuxRunnable;

    private boolean macRunnable;

    private String  name;

    private int     version;

    private boolean windowsRunnable;

    private boolean quickToggleEnabled;

    public void setQuickToggleEnabled(boolean quickToggleEnabled) {
        this.quickToggleEnabled = quickToggleEnabled;
    }

    public AbstractExtensionWrapper() {
        // required for Storable
    }

    /**
     * get the internal Extension Object. <br>
     * <b>do not remove the "_" in methodname. it is important to ignore this
     * getter during Storable serialisation<b><br>
     * 
     * @return
     */
    public AbstractExtension<?> _getExtension() {
        return extension;
    }

    /**
     * creates an icon in the given size. <br>
     * <b>do not remove the "_" in methodname. it is important to ignore this
     * getter during Storable serialisation<b><br>
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

    public String getAuthor() {
        return extension == null ? author : extension.getAuthor();
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

    public void setAuthor(String author) {
        this.author = author;
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
     * inits the extensions. afterwards, you can access the extensionby calling
     * {@link #_getExtension()}
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws StartException
     */
    public void init() throws InstantiationException, IllegalAccessException, ClassNotFoundException, StartException {
        if (extension != null) return;
        AbstractExtension<?> plg = (AbstractExtension<?>) _getClazz().newInstance();

        plg.init();
        extension = plg;
    }

    /**
     * Checks whether this extension is enabled or not.<br>
     * <b>do not remove the "_" in methodname. it is important to ignore this
     * getter during Storable serialisation<b><br>
     * 
     * @return
     */
    public boolean _isEnabled() {
        if (extension == null) {
            // not init yet. check storage to return if we have to init it
            return _getSettings().isEnabled();
        } else {
            return extension.isEnabled();
        }
    }

    /**
     * returns the Class for this extension. <br>
     * <b>do not remove the "_" in methodname. it is important to ignore this
     * getter during Storable serialisation<b><br>
     * 
     * @return
     */
    private Class<? extends AbstractExtension<?>> _getClazz() {

        return clazz;

    }

    /**
     * Starts or stops the extension. If the extension has not been initialized
     * yet, we do this
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
            _getSettings().setEnabled(b);
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
            return AbstractExtension.createStore((Class<? extends AbstractExtension<?>>) _getClazz(), (Class<? extends ExtensionConfigInterface>) Class.forName(this.getConfigInterface()));
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * <b>do not remove the "_" in methodname. it is important to ignore this
     * getter during Storable serialisation<b><br>
     * 
     * @param cls
     */
    public void _setClazz(Class<AbstractExtension<?>> cls) {
        clazz = cls;
    }

    public boolean isQuickToggleEnabled() {
        return extension == null ? quickToggleEnabled : extension.isQuickToggleEnabled();
    }

}
