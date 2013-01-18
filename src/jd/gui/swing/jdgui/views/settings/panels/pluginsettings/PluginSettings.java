package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import javax.swing.ImageIcon;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.translate._JDT;

public class PluginSettings extends AbstractConfigPanel {
    private static final long   serialVersionUID = 1L;
    private PluginSettingsPanel psp;

    public String getTitle() {
        return _JDT._.gui_settings_plugins_title();
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

        add(psp = new PluginSettingsPanel());

        super.onShow();
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
}