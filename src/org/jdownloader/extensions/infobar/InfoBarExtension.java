package org.jdownloader.extensions.infobar;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.plugins.AddonPanel;

import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.infobar.translate.InfobarTranslation;
import org.jdownloader.extensions.infobar.translate.T;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuWindowContainer;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.logging.LogController;

public class InfoBarExtension extends AbstractExtension<InfoBarConfig, InfobarTranslation> implements MenuExtenderHandler {

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    private static final String                    PROPERTY_OPACITY      = "PROPERTY_OPACITY";

    private static final String                    PROPERTY_DROPLOCATION = "PROPERTY_DROPLOCATION";

    private static final String                    PROPERTY_DOCKING      = "PROPERTY_DOCKING";

    private InfoDialog                             infoDialog;

    private ExtensionConfigPanel<InfoBarExtension> configPanel;

    public ExtensionConfigPanel<InfoBarExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public InfoBarExtension() throws StartException {

        setTitle(T._.jd_plugins_optional_infobar_jdinfobar());

    }

    public Class<EnableInfoBarGuiAction> getShowGuiAction() {
        return EnableInfoBarGuiAction.class;

    }

    private void updateOpacity(Object value) {
        if (infoDialog == null) return;
        final int newValue;
        if (value == null) {
            newValue = getPropertyConfig().getIntegerProperty(PROPERTY_OPACITY, 100);
        } else {
            newValue = Integer.parseInt(value.toString());
        }
        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                try {
                    AbstractNotifyWindow.setWindowOpacity(infoDialog, (float) (newValue / 100.0));
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.start();
    }

    public void setGuiEnable(boolean b) {
        if (b) {
            if (infoDialog == null) {
                infoDialog = new InfoDialog(this);
                infoDialog.setEnableDropLocation(getPropertyConfig().getBooleanProperty(PROPERTY_DROPLOCATION, true));
                infoDialog.setEnableDocking(getPropertyConfig().getBooleanProperty(PROPERTY_DOCKING, true));
                if (Application.getJavaVersion() >= Application.JAVA16) updateOpacity(null);
            }
            infoDialog.showDialog();
        } else {
            if (infoDialog != null) infoDialog.hideDialog();
        }
        getSettings().setGuiEnabled(b);
    }

    @Override
    public String getIconKey() {
        return "info";
    }

    @Override
    protected void stop() throws StopException {
        if (infoDialog != null) infoDialog.hideDialog();
        MenuManagerMainmenu.getInstance().unregisterExtender(this);
        MenuManagerMainToolbar.getInstance().unregisterExtender(this);
    }

    @Override
    protected void start() throws StartException {
        LogController.CL().info("InfoBar: OK");
        MenuManagerMainmenu.getInstance().registerExtender(this);
        MenuManagerMainToolbar.getInstance().registerExtender(this);
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {

        if (manager instanceof MenuManagerMainToolbar) {
            return updateMainToolbar(mr);
        } else if (manager instanceof MenuManagerMainmenu) {
            //
            return updateMainMenu(mr);
        }
        return null;
    }

    private MenuItemData updateMainMenu(MenuContainerRoot mr) {
        ExtensionsMenuContainer container = new ExtensionsMenuContainer();
        ExtensionsMenuWindowContainer windows = new ExtensionsMenuWindowContainer();
        windows.add(EnableInfoBarGuiAction.class);
        container.add(windows);
        return container;

    }

    private MenuItemData updateMainToolbar(MenuContainerRoot mr) {
        OptionalContainer opt = new OptionalContainer(false);
        opt.add(EnableInfoBarGuiAction.class);
        return opt;

    }

    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        if (Application.getJavaVersion() >= Application.JAVA16) {
            ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPropertyConfig(), PROPERTY_OPACITY, T._.jd_plugins_optional_infobar_opacity(), 1, 100, 10) {
                private static final long serialVersionUID = 1L;

                @Override
                public void valueChanged(Object newValue) {
                    updateOpacity(newValue);
                    super.valueChanged(newValue);
                }
            };
            ce.setDefaultValue(100);
            config.addEntry(ce);
            config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        }
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPropertyConfig(), PROPERTY_DROPLOCATION, T._.jd_plugins_optional_infobar_dropLocation2()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(Object newValue) {
                if (infoDialog != null) infoDialog.setEnableDropLocation((Boolean) newValue);
                super.valueChanged(newValue);
            }
        }.setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPropertyConfig(), PROPERTY_DOCKING, T._.jd_plugins_optional_infobar_docking()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(Object newValue) {
                if (infoDialog != null) infoDialog.setEnableDocking((Boolean) newValue);
                super.valueChanged(newValue);
            }
        }.setDefaultValue(true));
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_infobar_jdinfobar_description();
    }

    @Override
    public AddonPanel<InfoBarExtension> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {

        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);
    }

}