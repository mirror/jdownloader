package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;

public class SharebeeCom extends PluginForDecrypt {

	final static String host = "sharebee.com";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://[*]sharebee.com/[+]");
	
    public SharebeeCom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Sharebee.com-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
 
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			int count = 0;
    			
    			//Anzahl der Links zählen
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD",true)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_ZSHARE",false)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_BADONGO",false)) {
    				count++;
    			}
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, count));
    			
    			//Links auslesen und umdrehen
    			Vector<Vector<String>> g = getAllSimpleMatches(reqinfo.getHtmlCode(), "u=°\');return false;\">°</a>");
    			
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("Rapidshare"))
                            decryptedLinks.add(g.get(i).get(0));
                    }
    			}
    			
    			if((Boolean) this.getProperties().getProperty("USE_ZSHARE",false)) {
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("zSHARE"))
                            decryptedLinks.add(g.get(i).get(0));
                    }
    			}
    			
    			if((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD",true)) {
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("Megaupload"))
                            decryptedLinks.add(g.get(i).get(0));
                    }
    			}
    			
    			if((Boolean) this.getProperties().getProperty("USE_BADONGO",false)) {
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("Badongo"))
                            decryptedLinks.add(g.get(i).get(0));
                    }
    			}
    			    			
    			//Decrypt abschliessen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	return null;
    }
    
    private void setConfigEelements() {
    	ConfigEntry cfg;
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_ZSHARE", "zShare.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_MEGAUPLOAD", "Megaupload.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_BADONGO", "Badongo.com"));
        cfg.setDefaultValue(false);
    }
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }
}