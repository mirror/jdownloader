//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.UIInterface;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kann die ConfigContainer instanz einer Interaction verwenden um
 * den gewünschten Config Dialog anzuzeigen
 * 
 * @author JD-Team
 * 
 */
public class ConfigPanelDefault extends ConfigPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7983057329558110899L;

    private ConfigContainer container = null;

    /**
     * serialVersionUID
     */
    private Logger logger = JDUtilities.getLogger();

    private Vector<ConfigPanelDefault> subPanels = null;

    private JTabbedPane tabbedPane = null;

    public ConfigPanelDefault(UIInterface uiinterface, ConfigContainer container) {
        super(uiinterface);

        this.container = container;
        container.setActionListener(this);
        initPanel();

        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getID() == ConfigContainer.ACTION_REQUEST_SAVE) {
            save();
        }
    }

    private void addTabbedPanel(String title, ConfigPanelDefault configPanelPlugin) {
        subPanels.add(configPanelPlugin);
        tabbedPane.add(title, configPanelPlugin);

    }

    @Override
    public String getName() {

        return "defaultconfigpanel";
    }

    @Override
    public void initPanel() {
        if (container.getContainerNum() == 0) {
            Vector<ConfigEntry> entries = container.getEntries();
            ConfigEntry entry;
            for (int i = 0; i < entries.size(); i++) {
                entry = entries.elementAt(i);

                GUIConfigEntry ce = null;
                ce = new GUIConfigEntry(entry);
                if (ce != null) {
                    addGUIConfigEntry(ce);
                }

            }
            add(panel, BorderLayout.PAGE_START);
        } else {
            subPanels = new Vector<ConfigPanelDefault>();
            tabbedPane = new JTabbedPane();
            tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            tabbedPane.setTabPlacement(SwingConstants.LEFT);
            Vector<ConfigEntry> entries = container.getEntries();

            Vector<ConfigContainer> container = new Vector<ConfigContainer>();
            ConfigContainer general = new ConfigContainer(this);
            container.add(general);
            for (int i = 0; i < entries.size(); i++) {
                if (entries.elementAt(i).getContainer() == null) {
                    general.addEntry(entries.elementAt(i));
                } else {
                    container.add(entries.elementAt(i).getContainer());
                }
            }
            if (general.getEntries().size() == 0) {
                container.remove(0);
            }
            for (int i = 0; i < container.size(); i++) {
                addTabbedPanel(container.get(i).getTitle(), new ConfigPanelDefault(uiinterface, container.get(i)));

            }
            add(tabbedPane, BorderLayout.CENTER);
        }

    }

    @Override
    public void load() {
        loadConfigEntries();

    }

    @Override
    public void save() {
        if (subPanels != null) {
            for (int i = 0; i < subPanels.size(); i++) {
                logger.info("Saved tab " + i);

                subPanels.get(i).save();

            }

        }
        if (container != null) {
            logger.info("Save " + container.getTitle());
        } else {
            logger.info("Save normal panel" + this);
        }
        saveConfigEntries();

    }

    //
    // /**
    // *
    // */
    // private static final long serialVersionUID = -6647985066225177059L;
    //
    // /**
    // * serialVersionUID
    // */
    // @SuppressWarnings("unused")
    // private Logger logger = JDUtilities.getLogger();
    // private ConfigContainer container = null;
    //
    // private JTabbedPane tabbedPane=null;
    //
    // private Vector<ConfigPanelPlugin> subPanels=null;
    // protected Interaction interaction;
    //
    // public ConfigPanelDefault(UIInterface uiinterface, Interaction interaction) {
    // super(uiinterface);
    // this.interaction = interaction;
    // initPanel();
    // interaction.getConfig().setActionListener(this);
    //
    // load();
    // }
    //
    // public void save() {
    // this.saveConfigEntries();
    // }
    //
    // public void actionPerformed(ActionEvent e) {
    // if (e.getID() == ConfigContainer.ACTION_REQUEST_SAVE) {
    // this.save();
    // }
    // }
    //
    //    
    // public void initPanel() {
    //
    // ConfigContainer container = interaction.getConfig();
    //
    //     
    // if (container.getContainerNum() == 0) {
    // Vector<ConfigEntry> entries = container.getEntries();
    // ConfigEntry entry;
    // for (int i = 0; i < entries.size(); i++) {
    // entry = entries.elementAt(i);
    //
    // GUIConfigEntry ce = null;
    // ce = new GUIConfigEntry(entry);
    // if (ce != null) addGUIConfigEntry(ce);
    //
    // }
    // add(panel, BorderLayout.PAGE_START);
    // }
    // else {
    // this.subPanels= new Vector<ConfigPanelPlugin>();
    // tabbedPane = new JTabbedPane();
    // tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    // tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    // tabbedPane.setTabPlacement(JTabbedPane.LEFT);
    // Vector<ConfigEntry> entries = plugin.getConfig().getEntries();
    //
    // Vector<ConfigContainer> container = new Vector<ConfigContainer>();
    // ConfigContainer general = new ConfigContainer(this);
    // container.add(general);
    // for (int i = 0; i < entries.size(); i++) {
    // if (entries.elementAt(i).getContainer() == null) {
    // general.addEntry(entries.elementAt(i));
    // }
    // else {
    // container.add(entries.elementAt(i).getContainer());
    // }
    // }
    // if(general.getEntries().size()==0)container.remove(0);
    // for (int i = 0; i < container.size(); i++) {
    // this.addTabbedPanel(container.get(i).getTitle(),new
    // ConfigPanelPlugin(uiinterface, plugin, container.get(i)));
    //               
    //
    // }
    // add(tabbedPane, BorderLayout.CENTER);
    // }
    //        
    //        
    //
    // }
    //
    //    
    // public String getName() {
    // if (interaction == null) { return
    // JDLocale.L("gui.config.interaction.noAction", "no Action"); }
    // return JDLocale.L("gui.config.interaction.getName", "Interaction
    // Konfiguration: ") + interaction.getInteractionName();
    // }
    //
    //    
    // public void load() {
    // loadConfigEntries();
    //
    // }

}
