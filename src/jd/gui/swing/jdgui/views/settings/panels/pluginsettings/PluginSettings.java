package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import javax.swing.ImageIcon;

import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.translate._JDT;

public class PluginSettings extends AbstractConfigPanel {
    private static final long   serialVersionUID = 1L;
    private PluginSettingsPanel psp;

    public String getTitle() {
        return _GUI._.PluginSettings_getTitle();
    }

    public PluginSettings() {
        super();
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("plugin", 32);
    }

    @Override
    protected void onShow() {
        this.addHeader(getTitle(), NewTheme.I().getIcon("plugin", 32));
        this.addDescriptionPlain(_JDT._.gui_settings_plugins_description(HostPluginController.getInstance().list().size()));

        add(getPanel());

        super.onShow();
    }

    private PluginSettingsPanel getPanel() {
        if (psp != null) return psp;
        psp = new PluginSettingsPanel();
        return psp;
    }

    @Override
    public void save() {
        PluginSettingsPanel lpsp = psp;
        if (lpsp != null) {
            lpsp.setHidden();
        }
        psp = null;
        this.removeAll();
    }

    @Override
    public void updateContents() {
        PluginSettingsPanel lpsp = psp;
        if (lpsp != null) {
            lpsp.setShown();
        }
    }

    public void setPlugin(Class<? extends PluginForHost> class1) {
        if (psp == null) {
            JsonConfig.create(GraphicalUserInterfaceSettings.class).setActivePluginConfigPanel(class1.getName());
        } else {
            psp.setPlugin(class1);
        }
    }
}