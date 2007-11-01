package jd.gui.skins.simple.config;

import java.awt.BorderLayout;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;

/**
 * @author coalado
 *
 */
public class ConfigPanelDownload extends ConfigPanel{



/**
     * 
     */
    private static final long serialVersionUID = 4145243293360008779L;
private Configuration configuration;
   
    
    
  
    
    ConfigPanelDownload(Configuration configuration, UIInterface uiinterface){
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
    
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, "Timeout beim Lesen [ms]",0,60000).setDefaultValue(10000).setStep(500));
        addGUIConfigEntry(ce);
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, "Timeout beim Verbinden(Request) [ms]",0,60000).setDefaultValue(10000).setStep(500));
    
        addGUIConfigEntry(ce);
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, "Maximale gleichzeitige Downloads",1,20).setDefaultValue(3).setStep(1));
        addGUIConfigEntry(ce);
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_SPINNER, configuration, Configuration.PARAM_MIN_FREE_SPACE, "Downloads stoppen wenn der freie Speicherplatz weniger ist als [MB]",0,10000).setDefaultValue(100).setStep(10));
        addGUIConfigEntry(ce);
        
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_SEPERATOR ));
        
        addGUIConfigEntry(ce);
        
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "IP prüfen über (Website)").setDefaultValue("http://checkip.dyndns.org"));
        addGUIConfigEntry(ce);
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_GLOBAL_IP_PATTERN, "RegEx zum filtern der IP").setDefaultValue("Address\\: ([0-9.]*)\\<\\/body\\>"));
        addGUIConfigEntry(ce);
        add(panel, BorderLayout.NORTH);
    }
    @Override
    public void load() {
  this.loadConfigEntries();
        
    }
    
    @Override
    public String getName() {
        
        return "Netzwerk/Download";
    }
    
}
