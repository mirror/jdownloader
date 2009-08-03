package jd.gui.swing.jdgui.views.sidebars.configuration;

import javax.swing.JTabbedPane;

import jd.OptionalPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.GUIConfigEntry;
import jd.utils.locale.JDL;

public class AddonConfig extends ConfigPanel {

    private OptionalPluginWrapper pluginWrapper;

    public AddonConfig(OptionalPluginWrapper plg) {
        super();
        this.pluginWrapper = plg;

        initPanel();
        load();
    }

    public String getBreadcrum() {
        return JDL.L("jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.breadcrum", "") + JDL.L("jd.gui.swing.jdgui.views.sidebars.configuration.AddonConfig.breadcrum.deliminator", " - ") + pluginWrapper.getHost();
    }

    @Override
    public void initPanel() {
        ConfigContainer container = pluginWrapper.getPlugin().getConfig();

        for (ConfigEntry cfgEntry : container.getEntries()) {
            GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
            if (ce != null) addGUIConfigEntry(ce);
        }

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);

    }

    @Override
    public void load() {
        // TODO Auto-generated method stub

    }

    @Override
    public void save() {
        // TODO Auto-generated method stub

    }

}
