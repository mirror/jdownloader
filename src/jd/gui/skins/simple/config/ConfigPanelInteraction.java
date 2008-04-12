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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.ConfigEntry;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kann die ConfigContainer instanz einer Interaction verwenden um
 * den gewünschten Config Dialog anzuzeigen
 * 
 * @author JD-Team
 * 
 */
public class ConfigPanelInteraction extends ConfigPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -6647985066225177059L;

    /**
     * serialVersionUID
     */
    @SuppressWarnings("unused")
    private Logger            logger           = JDUtilities.getLogger();

    protected Interaction       interaction;

    public ConfigPanelInteraction(UIInterface uiinterface, Interaction interaction) {
        super(uiinterface);
        this.interaction = interaction;
        initPanel();

        load();
    }

    public void save() {
        this.saveConfigEntries();
    }

    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void initPanel() {

        Vector<ConfigEntry> entries = interaction.getConfig().getEntries();
       
        ConfigEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.elementAt(i);

            GUIConfigEntry ce = null;
            ce = new GUIConfigEntry(entry);

            if (ce != null) addGUIConfigEntry(ce);

            
        }
        add(panel, BorderLayout.CENTER);

    }

    @Override
    public String getName() {
if(interaction==null){return JDLocale.L("gui.config.interaction.noAction","no Action");}
        return JDLocale.L("gui.config.interaction.getName","Interaction Konfiguration: ")+interaction.getInteractionName();
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

}
