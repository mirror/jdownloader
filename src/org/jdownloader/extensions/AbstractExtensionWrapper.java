package org.jdownloader.extensions;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.appwork.storage.Storable;
import org.appwork.utils.Application;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.jdownloader.translate.JDT;

/**
 * Wraps around an extension to avoid extension init if the extension is not
 * enabled
 * 
 * @author thomas
 * 
 */
public class AbstractExtensionWrapper implements Storable {

    private Class<AbstractExtension> clazz;

    public static AbstractExtensionWrapper create(String id, Class<AbstractExtension> cls) throws StartException, InstantiationException, IllegalAccessException, IOException {
        AbstractExtensionWrapper ret = new AbstractExtensionWrapper();
        AbstractExtension plg = (AbstractExtension) cls.newInstance();

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
        ret.name = plg.getName();
        ret.lng = JDT.getLanguage();
        ret.version = plg.getVersion();
        ret.windowsRunnable = plg.isWindowsRunnable();
        ret.extension = plg;
        ret.author = plg.getAuthor();
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

    private String            author;

    private String            description;
    private AbstractExtension extension = null;
    private String            lng;

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
    public AbstractExtension _getExtension() {
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
            // TODO implement an icon cache here?
            return ImageProvider.scaleImageIcon(IconIO.getImageIcon(Application.getRessourceURL(iconPath)), size, size);

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
        AbstractExtension plg = (AbstractExtension) _getClazz().newInstance();

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
            return AbstractExtension.createStore(_getClazz()).isEnabled();
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
    private Class<? extends AbstractExtension> _getClazz() {

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
            AbstractExtension.createStore(_getClazz()).setEnabled(b);
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

    /**
     * <b>do not remove the "_" in methodname. it is important to ignore this
     * getter during Storable serialisation<b><br>
     * 
     * @param cls
     */
    public void _setClazz(Class<AbstractExtension> cls) {
        clazz = cls;
    }

}
