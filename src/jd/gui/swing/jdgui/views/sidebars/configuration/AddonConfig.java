package jd.gui.swing.jdgui.views.sidebars.configuration;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.utils.locale.JDL;

public class AddonConfig extends ConfigPanel {

    private static final long serialVersionUID = 5561326475681668634L;

    private static HashMap<String, AddonConfig> MAP;

    private ConfigContainer container;

    private String name;

    private AddonConfig(ConfigContainer container, String name) {
        super();
        this.container = container;
        this.name = name;
        initPanel();
        load();
    }

    public String getBreadcrum() {
        return JDL.L("jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.breadcrum", "") + JDL.L("jd.gui.swing.jdgui.views.sidebars.configuration.AddonConfig.breadcrum.deliminator", " - ") + name;
    }

    @Override
    public void initPanel() {

        panel = new ConfigPanel(container);
        ArrayList<ConfigEntry> cont = new ArrayList<ConfigEntry>();
        for (ConfigEntry cfgEntry : container.getEntries()) {
            if (cfgEntry.getType() == ConfigContainer.TYPE_CONTAINER) {
                cont.add(cfgEntry);

            }

        }

        final JTabbedPane tabbed = new JTabbedPane();

        tabbed.addChangeListener(new ChangeListener() {

            private ConfigPanel latestSelection;

            public void stateChanged(ChangeEvent e) {

                try {
                    ConfigPanel comp = (ConfigPanel) tabbed.getSelectedComponent();
                    if (comp == latestSelection) return;
                    if (latestSelection != null) {
                        latestSelection.setHidden();
                    }
                    latestSelection = comp;
                    if(comp!=null)comp.setShown();
                    revalidate();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }

            }

        });

        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);
        for (ConfigEntry c : cont) {
            ConfigPanel p = new ConfigPanel(c.getContainer());

            tabbed.add(c.getContainer().getTitle(), p);
        }

        this.add(tabbed);

    }
/**
 * Caches panels...
 * @param container2
 * @param name2
 * @return
 */
    public synchronized static AddonConfig getInstance(ConfigContainer container2, String name2) {
        if (MAP == null) MAP = new HashMap<String, AddonConfig>();

        AddonConfig p = MAP.get(container2 + "_" + name2);
        if (p != null) return p;

        MAP.put(container2 + "_" + name2, p = new AddonConfig(container2, name2));
        return p;
    }

}
