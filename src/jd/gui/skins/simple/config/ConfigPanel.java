//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;

import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDUtilities;

public abstract class ConfigPanel extends JPanel {
    /**
     * serialVersionUID
     */
    private static final long      serialVersionUID = 3383448498625377495L;

    private Vector<GUIConfigEntry> entries          = new Vector<GUIConfigEntry>();

    protected UIInterface          uiinterface;

    protected JPanel               panel;

    protected Logger               logger           = JDUtilities.getLogger();

    protected Insets               insets           = new Insets(1, 5, 1, 5);

    ConfigPanel(UIInterface uiinterface) {

        this.setLayout(new BorderLayout());
        panel = new JPanel(new GridBagLayout());
        this.uiinterface = uiinterface;

    }

    public void addGUIConfigEntry(GUIConfigEntry entry) {
        if (!entry.isExpertEntry() || JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) {
            JDUtilities.addToGridBag(panel, entry, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);
            entries.add(entry);
        }

    }

    public void saveConfigEntries() {
        Iterator<GUIConfigEntry> it = entries.iterator();
        Vector<SubConfiguration> subs= new   Vector<SubConfiguration>();
        while (it.hasNext()) {
            GUIConfigEntry akt = it.next();
            if(akt.getConfigEntry().getPropertyInstance() instanceof SubConfiguration && subs.indexOf(akt.getConfigEntry().getPropertyInstance())<0){
                subs.add((SubConfiguration)akt.getConfigEntry().getPropertyInstance());
                
            }
            // logger.info("entries: "+entries.size()+" :
            // "+akt.getConfigEntry().getPropertyInstance());
            if (akt.getConfigEntry().getPropertyInstance() != null && akt.getConfigEntry().getPropertyName() != null) akt.getConfigEntry().getPropertyInstance().setProperty(akt.getConfigEntry().getPropertyName(), akt.getText());

        }
        Iterator<SubConfiguration> it2 = subs.iterator();
        while(it2.hasNext())it2.next().save();
    }

    public void loadConfigEntries() {
        Iterator<GUIConfigEntry> it = entries.iterator();

        while (it.hasNext()) {
            GUIConfigEntry akt = it.next();

            if (akt.getConfigEntry().getPropertyInstance() != null && akt.getConfigEntry().getPropertyName() != null) akt.setData(akt.getConfigEntry().getPropertyInstance().getProperty(akt.getConfigEntry().getPropertyName()));

        }
    }

    public abstract void initPanel();

    public abstract void save();

    public abstract void load();

    public abstract String getName();
}
