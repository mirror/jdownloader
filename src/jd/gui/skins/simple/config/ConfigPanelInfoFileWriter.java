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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.border.EmptyBorder;

import jd.config.Configuration;
import jd.controlling.interaction.InfoFileWriter;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelInfoFileWriter extends ConfigPanel {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
   // private JLabel            lblHomeDir;
   // private BrowseFile        brsHomeDir;
   // private Configuration     configuration;
    private InfoFileWriter fileWriter;
    public ConfigPanelInfoFileWriter(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
       // this.configuration = configuration;
        fileWriter= InfoFileWriter.getInstance();
        initPanel();
        load();
    }
    public void save() {
        this.saveConfigEntries();
        
    }
    @Override
    public void initPanel() {
        GUIConfigEntry ce;
        for(int i=0; i<fileWriter.getConfig().getEntries().size();i++){
            ce = new GUIConfigEntry(fileWriter.getConfig().getEntries().get(i));
            if (!ce.isExpertEntry() || JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) {
                EmptyBorder eb = new EmptyBorder(0,0,0,0);
                ce.setBorder(eb);
//                Component[] components = ce.getComponents();
//                for (Component c : components) {
//                    ((JComponent) c).setBorder(eb);
//                }
                panel.add(ce);
                entries.add(ce);
            }
        }
        panel.setLayout(new BorderLayout());
        add(panel, "Center");
    }
    @Override
    public void load() {
        this.loadConfigEntries();
    }
    @Override
    public String getName() {
        return JDLocale.L("gui.config.infoFileWriter.name","Info Datei");
    }
    
    private class BorderLayout implements LayoutManager {
        private int gap = 10;
        public int getGap() {return gap;}
        public void setGap(int gap) {this.gap = gap;}
        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}
        public Dimension preferredLayoutSize(Container parent) {
            Insets insets = parent.getInsets();
            Dimension pref = new Dimension(0, 0);
            for (int i = 0, c = parent.getComponentCount(); i < c; i++) {
              Component m = parent.getComponent(i);
              if (m.isVisible()) {
                Dimension componentPreferredSize = parent.getComponent(i).getPreferredSize(); 
                pref.height += componentPreferredSize.height + gap;
                pref.width = Math.max(pref.width, componentPreferredSize.width);
              }
            }
            pref.width += insets.left + insets.right;
            pref.height += insets.top + insets.bottom;
            return pref;
        }
        public Dimension minimumLayoutSize(Container parent) {return null;}
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            Dimension size = parent.getSize();
            int width = size.width - insets.left - insets.right;
            int height = insets.top;
            for (int i = 0, c = parent.getComponentCount(); i < c; i++) {
              Component m = parent.getComponent(i);
              if (m.isVisible()) {
                  if (i == c-1) m.setBounds(insets.left, height, width, size.height-height-insets.bottom);
                  else m.setBounds(insets.left, height, width, m.getPreferredSize().height);
                height += m.getSize().height + gap;
              }
            }
        }
    }

}
