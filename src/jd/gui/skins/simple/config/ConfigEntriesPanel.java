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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.utils.JDUtilities;

public class ConfigEntriesPanel extends ConfigPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7983057329558110899L;

    /**
     * serialVersionUID
     */
    private Logger            logger           = JDUtilities.getLogger();

  

    private ConfigContainer   container        = null;

    private JTabbedPane tabbedPane=null;

    private Vector<ConfigEntriesPanel> subPanels=null;

    private String title;



    public ConfigEntriesPanel(ConfigContainer container,String title) {
        super(JDUtilities.getGUI());
        this.container = container;     
        this.title=title;
        initPanel();
        load();
    }

    public void save() {
        if(subPanels!=null){
            for( int i=0;i<subPanels.size();i++){
                logger.info("Saved tab "+i);
              
                subPanels.get(i).save();
               
                
            }
            
        }
        if(container!=null){
            logger.info("Save "+container.getTitle());
        }else{
            logger.info("Save normal panel"+this);
        }
        this.saveConfigEntries();
        
    }

    public void actionPerformed(ActionEvent e) {

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
                if (ce != null) addGUIConfigEntry(ce);

            }
            add(panel, BorderLayout.PAGE_START);
        }
        else {
            this.subPanels= new Vector<ConfigEntriesPanel>();
            tabbedPane = new JTabbedPane();
            tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            tabbedPane.setTabPlacement(JTabbedPane.TOP);
            Vector<ConfigEntry> entries = container.getEntries();

            Vector<ConfigContainer> container = new Vector<ConfigContainer>();
            ConfigContainer general = new ConfigContainer(this);
            container.add(general);
            for (int i = 0; i < entries.size(); i++) {
                if (entries.elementAt(i).getContainer() == null) {
                    general.addEntry(entries.elementAt(i));
                }
                else {
                    container.add(entries.elementAt(i).getContainer());
                }
            }
            if(general.getEntries().size()==0)container.remove(0);
            for (int i = 0; i < container.size(); i++) {
                this.addTabbedPanel(container.get(i).getTitle(),new ConfigEntriesPanel( container.get(i),container.get(i).getTitle()));
               

            }
            add(tabbedPane, BorderLayout.CENTER);
        }

    }

    private void addTabbedPanel(String title,ConfigEntriesPanel configPanelPlugin) {
        this.subPanels.add(configPanelPlugin);
        tabbedPane.add(title,configPanelPlugin );
        
    }

    @Override
    public String getName() {

        return title;
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

    public Vector<ConfigEntriesPanel> getSubPanels() {
        return subPanels;
    }

}
