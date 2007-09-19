package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.JDUtilities;

public class Rapidsafenet extends PluginForDecrypt {

    static private final String  host = "rapidsafe.net";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://www.rapidsafe.net/[+]");;
	
    public Rapidsafenet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
      
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Rapidsafe.net-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
    @Override public boolean isClipboardEnabled() { return true; }
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, 1));
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			
    			//Links auslesen und konvertieren
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			decryptedLinks.add((JDUtilities.htmlDecode(getBetween(reqinfo.getHtmlCode(),"&nbsp;<FORM ACTION=\"","\" METHOD=\"post\" ID=\"postit\""))));
    		}
    		catch(IOException e) {
    			e.printStackTrace();
    		}
    		
    		//Decrypt abschliessen
    		firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
    		logger.info(decryptedLinks.size() + " downloads decrypted");
    		step.setParameter(decryptedLinks);
    	}
    	
    	return null;
    }
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }
}