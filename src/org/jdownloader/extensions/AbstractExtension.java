//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions;

import java.lang.reflect.Type;
import java.net.URL;

import javax.swing.Icon;

import jd.config.ConfigContainer;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.plugins.AddonPanel;
import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.JsonConfig;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * Superclass for all extensions
 * 
 * @author thomas
 * 
 */
public abstract class AbstractExtension<ConfigType extends ExtensionConfigInterface, TranslationType extends TranslateInterface> {

    public static final int ADDON_INTERFACE_VERSION = 8;

    private boolean         enabled                 = false;

    /**
     * true if the extension is currently running.
     * 
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    public Icon getIcon(int i) {
        return NewTheme.I().getIcon(getIconKey(), i);
    }

    /**
     * start/stops the extension.
     * 
     * @param enabled
     * @throws StartException
     * @throws StopException
     */
    public synchronized void setEnabled(boolean enabled) throws StartException, StopException {
        if (enabled == this.enabled) return;
        this.enabled = enabled;
        if (enabled) {
            store.setEnabled(true);
            start();
        } else {
            store.setEnabled(false);
            stop();
            if (getGUI() != null) {
                getGUI().setActive(false);
            }
        }

    }

    public void dispose() {
        try {
            stop();
        } catch (StopException e) {
            e.printStackTrace();
        }
        if (getGUI() != null) {
            getGUI().setActive(false);
        }
    }

    /**
     * Converts an ExtensionConfigPanel from the old config containers
     * 
     * @param initSettings
     * @return
     */
    @Deprecated
    protected ExtensionConfigPanel createPanelFromContainer(ConfigContainer initSettings) {

        final AddonConfig cp = AddonConfig.getInstance(initSettings, "", false);
        ExtensionConfigPanel<AbstractExtension<ConfigType, TranslationType>> ret = new ExtensionConfigPanel<AbstractExtension<ConfigType, TranslationType>>(this, false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onShow() {
            }

            @Override
            protected void onHide() {
            }

            @Override
            public void save() {
                cp.setHidden();
            }

            @Override
            public void updateContents() {
                cp.setShown();
            }
        };

        ret.add(cp, "gapleft 25,spanx,growx,pushx,growy,pushy");
        return ret;
    }

    /**
     * Returns the internal storage. Most of the configvalues are for internal use only. This config only contains values which are valid for all extensions
     * 
     * @return
     */
    public ConfigType getSettings() {
        return store;
    }

    /**
     * use {@link #setAutoConnectEnabled(false)} to stop the extension. avoid to call this method directly
     * 
     * @throws StopException
     */
    protected abstract void stop() throws StopException;

    /**
     * use {@link #setAutoConnectEnabled(true)} to start the extension. avoid to call this method directly
     * 
     * @throws StartException
     */
    protected abstract void start() throws StartException;

    private String         name;

    private int            version = -1;

    private ConfigType     store;
    /**
     * The Translationobject. Extent me if you need further Entries
     */
    public TranslationType _;

    protected LogSource    logger;

    public LogSource getLogger() {
        return logger;
    }

    public TranslationType getTranslation() {
        return _;
    }

    public String getName() {
        return name;
    }

    /**
     * 
     * @param translationInterface
     * @param contentType
     *            name of this plugin. Until JD 2.* we should use null here to use the old defaultname. we used to sue this localized name as config key.
     * @throws
     * @throws StartException
     */
    public AbstractExtension() {
        this.name = getClass().getSimpleName();
        version = readVersion(getClass());
        store = buildStore();
        AdvancedConfigManager.getInstance().register(store);
        logger = LogController.getInstance().getLogger(name);
        initTranslation();
    }

    private void initTranslation() {
        Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedTypeImpl) {
            Class<TranslationType> cl = (Class<TranslationType>) ((ParameterizedTypeImpl) type).getActualTypeArguments()[1];
            if (cl == TranslateInterface.class) return;
            _ = TranslationFactory.create(cl);
        }
    }

    protected void setTitle(String title) {
        this.name = title == null ? getClass().getSimpleName() : title;

    }

    /**
     * Creates the correct config based on the Extensions supertype
     * 
     * @param class1
     * @return
     */
    private ConfigType buildStore() {
        return JsonConfig.create(Application.getResource("cfg/" + getClass().getName()), getConfigClass());

    }

    /**
     * returns the config interface class for this extension
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public Class<ConfigType> getConfigClass() {
        Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedTypeImpl) {
            return (Class<ConfigType>) ((ParameterizedTypeImpl) type).getActualTypeArguments()[0];
        } else {
            throw new RuntimeException("Bad Extension Definition. Please add Generic ConfigClass: class " + getClass().getSimpleName() + " extends AbstractExtension<" + getClass().getSimpleName() + "Config>{... with 'public interface " + getClass().getSimpleName() + "Config extends ExtensionConfigInterface{...");
        }
    }

    public static ExtensionConfigInterface createStore(String className, Class<? extends ExtensionConfigInterface> interfaceClass) {
        return JsonConfig.create(Application.getResource("cfg/" + className), interfaceClass);
    }

    /**
     * Gets called once per session as soon as the extension gets loaded the first time
     * 
     * @throws StartException
     */
    protected abstract void initExtension() throws StartException;

    /**
     * Reads the version.dat in the same directory as class1
     * 
     * @param class1
     * @return
     */
    public static int readVersion(Class<? extends AbstractExtension> class1) {

        try {
            URL url = class1.getResource("version.dat");
            if (url == null) return -1;
            return Integer.parseInt(IO.readURLToString(url).trim());
        } catch (Throwable e) {
            return -1;
        }

    }

    public abstract ExtensionConfigPanel<?> getConfigPanel();

    public abstract boolean hasConfigPanel();

    public abstract String getDescription();

    public boolean isLinuxRunnable() {
        return true;
    }

    public abstract String getIconKey();

    public boolean isWindowsRunnable() {
        return true;
    }

    public boolean isMacRunnable() {
        return true;
    }

    public abstract AddonPanel<? extends AbstractExtension<ConfigType, TranslationType>> getGUI();

    public boolean isDefaultEnabled() {
        return false;
    }

    public int getVersion() {
        return version;
    }

    public boolean isQuickToggleEnabled() {
        return false;
    }

    public void init() throws StartException {
        long t = System.currentTimeMillis();
        initExtension();

        if (store.isFreshInstall()) {
            store.setEnabled(this.isDefaultEnabled());
            store.setFreshInstall(false);
        }
        if (store.isEnabled()) {
            try {
                setEnabled(true);
            } catch (StopException e) {
                // cannot happen
            }
        }

        logger.info("Init Duration: " + (System.currentTimeMillis() - t));
    }

    public void handleCommand(String command, String... parameters) {
    }

    public boolean isGuiOptional() {
        return true;
    }

}