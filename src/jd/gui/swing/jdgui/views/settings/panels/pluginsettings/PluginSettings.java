package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import javax.swing.ImageIcon;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class PluginSettings extends AbstractConfigPanel {

    public String getTitle() {
        return JDT._.gui_settings_plugins_title();
    }

    public PluginSettings() {
        super();
        this.addHeader(getTitle(), Theme.getIcon("plugin", 32));
        this.addDescription(JDT._.gui_settings_plugins_description(HostPluginWrapper.getHostWrapper().size() + DecryptPluginWrapper.getDecryptWrapper().size()));

        add(new PluginSettingsPanel());

    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("plugin", 32);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }
}