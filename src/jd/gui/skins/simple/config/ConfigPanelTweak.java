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
import java.util.logging.Level;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;

/**
 * @author JD-Team
 *
 */
public class ConfigPanelTweak extends ConfigPanel{



/**
     * 
     */
    private static final long serialVersionUID = 4145243293360008779L;
private Configuration configuration;
   
    
    
  
    
public ConfigPanelTweak(Configuration configuration, UIInterface uiinterface){
        super(uiinterface);
      this.configuration=configuration;
        initPanel();

        load();
    
    }
    public void save(){
        logger.info("save");
        this.saveConfigEntries();
       
     }
   

    public void initPanel() {
      
        GUIConfigEntry ce;
//      ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, JDLocale.L("gui.config.general.webupdate.osFilter", "Webupdate: Alle Erweiterungen aktualisieren (auch OS-fremde)")).setDefaultValue(false).setExpertEntry(true));
//      addGUIConfigEntry(ce);
      
       
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_TWEAK_DOWNLOAD_CPU, JDLocale.L("gui.config.tweak.downloadcpu", "[Prozessor] CPU-Last beim Download verringern")).setDefaultValue(false));
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_TWEAK_GUI_INTERVAL, JDLocale.L("gui.config.tweak.guiInterval", "[Prozessor] Downloadstatus weniger oft aktualisieren")).setDefaultValue(false));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("gui.config.tweak.log", "[Arbeitsspeicher] Das Loggerlevel kann zum Speichersparen auf 'Off' gestellt werden.")));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[]{Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF}, "(*) "+JDLocale.L("gui.config.general.loggerLevel", "Level für's Logging")).setDefaultValue(Level.WARNING));
        addGUIConfigEntry(ce);
        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "(*) "+ JDLocale.L("gui.config.tweak.rs", "Für diese Einstellung ist ein Neustart erforderlich")));
        addGUIConfigEntry(ce);
        
        
        add(panel, BorderLayout.NORTH);
    }
    @Override
    public void load() {
  this.loadConfigEntries();
        
    }
    
    @Override
    public String getName() {
        
        return JDLocale.L("gui.config.tweak.name","Rechen- und Speicherverbrauch verringern");
    }
    
}
