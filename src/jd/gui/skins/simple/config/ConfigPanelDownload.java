package jd.gui.skins.simple.config;

import java.awt.BorderLayout;

import jd.Configuration;
import jd.gui.UIInterface;
import jd.plugins.PluginConfig;

/**
 * @author coalado
 *
 */
public class ConfigPanelDownload extends ConfigPanel{



/**
     * 
     */
    private static final long serialVersionUID = 4145243293360008779L;
   
    
    
  
    
    ConfigPanelDownload(Configuration configuration, UIInterface uiinterface){
        super(configuration, uiinterface);
      
        initPanel();

        load();
    
    }
    public void save(){
        this.saveConfigEntries();
     }
   

    public void initPanel() {
      
        ConfigEntry ce;
        
        ce= new ConfigEntry(PluginConfig.TYPE_SPINNER, configuration, Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, "Timeout beim Lesen [ms]",0,60000);
        ce.setDefaultText(10000);
        ce.setSteps(500);
        addConfigEntry(ce);
        ce= new ConfigEntry(PluginConfig.TYPE_SPINNER, configuration, Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, "Timeout beim Verbinden(Request) [ms]",0,60000);
        ce.setDefaultText(10000);
        ce.setSteps(500);
        addConfigEntry(ce);
        ce= new ConfigEntry(PluginConfig.TYPE_SPINNER, configuration, Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, "Maximale gleichzeitige Downloads",1,20);
        ce.setDefaultText(3);
        addConfigEntry(ce);
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
