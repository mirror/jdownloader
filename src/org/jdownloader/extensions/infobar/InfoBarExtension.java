package org.jdownloader.extensions.infobar;

import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.infobar.translate.InfobarTranslation;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuWindowContainer;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.logging.LogController;

import jd.SecondLevelLaunch;
import jd.plugins.AddonPanel;

public class InfoBarExtension extends AbstractExtension<InfoBarConfig, InfobarTranslation> implements MenuExtenderHandler {

    @Override
    public boolean isDefaultEnabled() {
        return false;
    }

    private InfoDialog                             infoDialog;

    private ExtensionConfigPanel<InfoBarExtension> configPanel;

    public ExtensionConfigPanel<InfoBarExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public InfoBarExtension() throws StartException {
        setTitle(T.jd_plugins_optional_infobar_jdinfobar());

    }

    public Class<EnableInfoBarGuiAction> getShowGuiAction() {
        return EnableInfoBarGuiAction.class;

    }

    public void setGuiEnable(boolean b) {
        if (b) {

            if (infoDialog == null) {
                infoDialog = new InfoDialog(this);
                infoDialog.setEnableDropLocation(getSettings().isDragAndDropEnabled());

            }
            infoDialog.showDialog();
        } else {
            if (infoDialog != null) {
                infoDialog.hideDialog();
            }
        }
        getSettings().setGuiEnabled(b);
    }

    @Override
    public String getIconKey() {
        return "info";
    }

    @Override
    protected void stop() throws StopException {
        if (infoDialog != null) {
            infoDialog.hideDialog();
        }
        MenuManagerMainmenu.getInstance().unregisterExtender(this);
        MenuManagerMainToolbar.getInstance().unregisterExtender(this);
    }

    @Override
    public boolean isHeadlessRunnable() {
        return false;
    }

    @Override
    protected void start() throws StartException {
        if (org.appwork.utils.Application.isHeadless()) {
            throw new StartException("Not available in Headless Mode");
        }
        LogController.CL().info("InfoBar: OK");
        if (!Application.isHeadless()) {
            MenuManagerMainmenu.getInstance().registerExtender(this);
            MenuManagerMainToolbar.getInstance().registerExtender(this);
        }
        if (CFG_INFOBAR.GUI_ENABLED.isEnabled()) {
            SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

                @Override
                public void run() {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            setGuiEnable(true);

                        }

                    };
                }
            });
        }
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

    @Override
    public String getDescription() {
        return T.jd_plugins_optional_infobar_jdinfobar_description();
    }

    @Override
    public AddonPanel<InfoBarExtension> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {

        configPanel = new InfoBarConfigPanel(this);
    }

}