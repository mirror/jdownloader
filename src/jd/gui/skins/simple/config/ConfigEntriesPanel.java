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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultSingleSelectionModel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.skins.SwingGui;
import jd.nutils.JDImage;
import net.miginfocom.swing.MigLayout;

import com.jtattoo.plaf.acryl.AcrylTabbedPaneUI;

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

    public boolean needsViewport() {
        return false;
    }

    public ConfigEntriesPanel(ConfigContainer container, boolean idle) {
        super();
        this.container = container;
        if (!idle) {
            init();
        } else {
            /**
             * Falls configcontainer textfelder haben, die ja üblicherweise
             * scrollbars haben, brauchen wir keinen viewport
             */
            if (container.getContainerNum() == 0) {

                for (ConfigEntry cfgEntry : container.getEntries()) {
                    switch (cfgEntry.getType()) {

                    case ConfigContainer.TYPE_TEXTAREA:
                    case ConfigContainer.TYPE_LISTCONTROLLED:
                        viewport = false;
                        break;
                    }

                }

            }

        }
    }

    public void init() {
        if (SwingGui.getInstance() != null) SwingGui.getInstance().setWaiting(true);
        initPanel();
        load();

    }

    private void addTabbedPanel(String title, ConfigEntriesPanel configPanelPlugin) {
        subPanels.add(configPanelPlugin);
        configPanelPlugin.setTabbed(true);
        JScrollPane sp;
        JComponent comp = configPanelPlugin;
        if (configPanelPlugin.needsViewport()) {
            comp = (sp = new JScrollPane(configPanelPlugin));
            sp.setBorder(null);
        }
        if (configPanelPlugin.container != null && configPanelPlugin.container.getEntries().size() > 0 && configPanelPlugin.container.getEntries().get(0).getGroup() != null) {
            tabbedPane.addTab(title, JDImage.getScaledImageIcon(configPanelPlugin.container.getEntries().get(0).getGroup().getIcon(), 16, 16), comp);
        } else {
            tabbedPane.addTab(title, comp);

        }

    }

    public Vector<ConfigEntriesPanel> getSubPanels() {
        return subPanels;
    }

    // @Override
    public void initPanel() {
        if (inited) return;
        this.inited = true;

        if (container.getContainerNum() == 0) {

            for (ConfigEntry cfgEntry : container.getEntries()) {
                GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
                if (ce != null) addGUIConfigEntry(ce);
            }

            add(panel);
        } else {
            subPanels = new Vector<ConfigEntriesPanel>();
            tabbedPane = new JTabbedPane();
initUI(tabbedPane);
//            tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
//            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
//            tabbedPane.setTabPlacement(SwingConstants.TOP);
            tabbedPane.setModel(new DefaultSingleSelectionModel() {
                /**
                 * 
                 */
                private static final long serialVersionUID = -4014914744929365725L;

                public void setSelectedIndex(int index) {

                    if (tabbedPane.getSelectedComponent() != null) {
                        if (tabbedPane.getSelectedComponent() instanceof JScrollPane) {
                            ((ConfigEntriesPanel) ((JScrollPane) tabbedPane.getSelectedComponent()).getViewport().getView()).save();
                        } else {
                            ((ConfigEntriesPanel) tabbedPane.getSelectedComponent()).save();
                        }

                    }

                    super.setSelectedIndex(index);
                }
            });
            tabbedPane.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    if (tabbedPane.getSelectedComponent() instanceof JScrollPane) {
                        ((ConfigEntriesPanel) ((JScrollPane) tabbedPane.getSelectedComponent()).getViewport().getView()).init();
                    } else {
                        ((ConfigEntriesPanel) tabbedPane.getSelectedComponent()).init();
                    }
                }

            });

            ArrayList<ConfigContainer> container = new ArrayList<ConfigContainer>();
            ConfigContainer general = new ConfigContainer();
            container.add(general);
            for (ConfigEntry cfgEntry : this.container.getEntries()) {
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

    private void initUI(JTabbedPane tabbedPane2) {

            
            if (tabbedPane2.getUI() instanceof AcrylTabbedPaneUI) {

                tabbedPane2.setUI(new AcrylTabbedPaneUI() {
                    public void installDefaults() {
                        super.installDefaults();
                        contentBorderInsets=new Insets(0, 0, 0, 0);
                        tabInsets=new Insets(0, 6, 0, 6);
                    }
                    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
                        // super.paintContentBorder(arg0, arg1, arg2, arg3, arg4,
                        // arg5, arg6)
                        int sepHeight = tabAreaInsets.bottom;
                       
                        if (sepHeight > 0) {
                            switch (tabPlacement) {
                            case TOP: {
                                int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                                Color colors[] = getContentBorderColors(tabPlacement);
                                for (int i = 0; i < colors.length; i++) {
                                    g.setColor(colors[i]);
                                    g.drawLine(x, y + tabAreaHeight - sepHeight + i + 1, x + w, y + tabAreaHeight - sepHeight + i + 1);
                                }

                                break;
                            }

                            }

                        }

                    }

                  

                });

            }
        }

    // @Override
    public void load() {
        loadConfigEntries();
    }

    public PropertyType hasChanges() {
        PropertyType ret = super.hasChanges();
        if (subPanels != null) {
            synchronized (subPanels) {
                for (int i = 0; i < subPanels.size(); i++) {
                    ret = ret.getMax(subPanels.get(i).hasChanges());
                }
            }
        }
        return ret;

    }

    // @Override
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

    public ConfigContainer getConfigContainer() {
        return container;
    }

}
