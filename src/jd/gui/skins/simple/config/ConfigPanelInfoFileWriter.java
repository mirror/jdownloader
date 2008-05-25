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
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.config.Configuration;
import jd.controlling.interaction.InfoFileWriter;
import jd.gui.UIInterface;
import jd.utils.JDLocale;

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
            addGUIConfigEntry(ce);
        }
        add(new JScrollPane(panel), BorderLayout.NORTH);
    }
    @Override
    public void load() {
        this.loadConfigEntries();
    }
    @Override
    public String getName() {
        return JDLocale.L("gui.config.infoFileWriter.name","Info Datei");
    }
}
