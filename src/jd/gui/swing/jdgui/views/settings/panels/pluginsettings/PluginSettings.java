package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import javax.swing.Icon;

import jd.plugins.Account;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.translate._JDT;

public class PluginSettings extends AbstractConfigPanel {
    private static final long   serialVersionUID = 1L;
    private PluginSettingsPanel psp;

    public String getTitle() {
        return _GUI.T.PluginSettings_getTitle();
    }

    public PluginSettings() {
        super();
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_PLUGIN, 32);
    }

    @Override
    protected void onShow() {
        this.addHeader(getTitle(), new AbstractIcon(IconKey.ICON_PLUGIN, 32));
        this.addDescriptionPlain(_JDT.T.gui_settings_plugins_description(HostPluginController.getInstance().list().size()));
        add(getPanel());
        super.onShow();
    }

    private PluginSettingsPanel getPanel() {
        final PluginSettingsPanel lpsp = psp;
        if (lpsp != null) {
            return lpsp;
        }
        psp = new PluginSettingsPanel();
        return psp;
    }

    @Override
    public void save() {
        final PluginSettingsPanel lpsp = psp;
        if (lpsp != null) {
            lpsp.setHidden();
        }
        psp = null;
        this.removeAll();
    }

    @Override
    public void updateContents() {
        final PluginSettingsPanel lpsp = psp;
        if (lpsp != null) {
            lpsp.setShown();
        }
    }

    public void setPlugin(final LazyPlugin<?> plugin) {
        final PluginSettingsPanel lpsp = psp;
        if (lpsp == null) {
            JsonConfig.create(GraphicalUserInterfaceSettings.class).setActivePluginConfigPanel(plugin.getID());
        } else {
            lpsp.setPlugin(plugin);
        }
    }

    public void scrollToAccount(Account account) {
        getPanel().scrollToAccount(account);
    }
}