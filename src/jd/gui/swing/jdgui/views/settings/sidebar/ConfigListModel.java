package jd.gui.swing.jdgui.views.settings.sidebar;

import javax.swing.DefaultListModel;

import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelCaptcha;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.panels.addons.ConfigPanelAddons;
import jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.ProxyConfig;
import jd.gui.swing.jdgui.views.settings.panels.gui.Advanced;
import jd.gui.swing.jdgui.views.settings.panels.gui.Linkgrabber;
import jd.gui.swing.jdgui.views.settings.panels.gui.ToolbarController;
import jd.gui.swing.jdgui.views.settings.panels.hoster.ConfigPanelPlugin;
import jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordList;
import jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordListHTAccess;
import jd.gui.swing.jdgui.views.settings.panels.premium.Premium;
import jd.gui.swing.jdgui.views.settings.panels.reconnect.Reconnect;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;

public class ConfigListModel extends DefaultListModel {

    private static final long serialVersionUID = -204494527404304349L;

    public ConfigListModel() {
        super();
        fill();
    }

    private void fill() {
        removeAllElements();

        addElement(new ConfigPanelGeneral());
        addElement(new ProxyConfig());
        addElement(new ToolbarController());
        addElement(new Linkgrabber());
        addElement(new Advanced());
        addElement(new ConfigPanelCaptcha());
        addElement(new Reconnect());
        addElement(new PasswordList());
        addElement(new PasswordListHTAccess());
        addElement(new Premium());
        addElement(new ConfigPanelPlugin());
        addElement(new ConfigPanelAddons());

        for (final AbstractExtension plg : ExtensionController.getInstance().getEnabledExtensions()) {
            if ((!plg.hasSettings() && !plg.hasConfigPanel())) {
                continue;
            }
            if (plg.hasConfigPanel()) {
                addElement(plg.getConfigPanel());
            } else {
                addElement(AddonConfig.getInstance(plg.getSettings(), "", true));
            }
        }
    }

}
