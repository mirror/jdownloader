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

package jd.gui.skins.simple.config;

import java.awt.ComponentOrientation;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import net.miginfocom.swing.MigLayout;

public class ConfigEntriesPanel extends ConfigPanel {

    private static final long serialVersionUID = -7983057329558110899L;

    private ConfigContainer container = null;

    private Logger logger = jd.controlling.JDLogger.getLogger();

    private Vector<ConfigEntriesPanel> subPanels = null;

    private JTabbedPane tabbedPane = null;

    private boolean inited = false;

    public ConfigEntriesPanel(ConfigContainer container) {
        this(container, false);
    }

    public ConfigEntriesPanel(ConfigContainer container, boolean idle) {
        super();
        this.container = container;
        if (!idle) {
            init();
        }
    }

    public void init() {

        initPanel();
        load();

    }

    private void addTabbedPanel(String title, ConfigEntriesPanel configPanelPlugin) {
        subPanels.add(configPanelPlugin);
        tabbedPane.add(title, configPanelPlugin);
    }

    public Vector<ConfigEntriesPanel> getSubPanels() {
        return subPanels;
    }

    //@Override
    public void initPanel() {
        if (inited) return;
        this.inited = true;

        if (container.getContainerNum() == 0) {

            Vector<ConfigEntry> entries = container.getEntries();
            for (ConfigEntry cfgEntry : entries) {
                GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
                if (ce != null) addGUIConfigEntry(ce);
            }

            add(panel);
        } else {
            subPanels = new Vector<ConfigEntriesPanel>();
            tabbedPane = new JTabbedPane();

            tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            tabbedPane.setTabPlacement(SwingConstants.TOP);
            tabbedPane.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {

                    ((ConfigEntriesPanel) tabbedPane.getSelectedComponent()).init();

                }

            });
            Vector<ConfigEntry> entries = container.getEntries();

            ArrayList<ConfigContainer> container = new ArrayList<ConfigContainer>();
            ConfigContainer general = new ConfigContainer(this);
            container.add(general);
            for (ConfigEntry cfgEntry : entries) {
                if (cfgEntry.getContainer() == null) {
                    general.addEntry(cfgEntry);
                } else {
                    container.add(cfgEntry.getContainer());
                }
            }
            if (general.getEntries().size() == 0) {
                container.remove(0);
            }

            for (ConfigContainer cfg : container) {
                addTabbedPanel(cfg.getTitle(), new ConfigEntriesPanel(cfg, true));

            }
            this.setLayout(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));
            add(tabbedPane);
        }

    }

    //@Override
    public void load() {
        loadConfigEntries();
    }

    public PropertyType hasChanges() {
        PropertyType ret = super.hasChanges();

        if (subPanels != null) {
            for (int i = 0; i < subPanels.size(); i++) {
                ret = ret.getMax(subPanels.get(i).hasChanges());
            }
        }
        return ret;

    }

    //@Override
    public void save() {
        if (subPanels != null) {
            for (int i = 0; i < subPanels.size(); i++) {
            
                subPanels.get(i).save();
            }
        }

        if (container != null) {
            logger.finer("Save " + container.getTitle());
        } else {
            logger.finer("Save normal panel" + this);
        }

        saveConfigEntries();
    }

}
