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

public class Linkshield extends PluginForDecrypt {

    static private final String  host = "www.linkshield.com";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://[*]linkshield.com/c/[+]");
	
    public Linkshield() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
	
    @Override public String getCoder() { return "Luke"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "linkshield.com-".concat(version); }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
  
    
    @Override 
    public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			
    			//test link: http://www.linkshield.com/c/976_956
    			
				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, 1));
				
				RequestInfo reqinfo = getRequest(new URL(parameter), null, null, true);

				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			decryptedLinks.add((getBetween(reqinfo.getHtmlCode(), "<frame src=(?!blank)", ">")));
    			
    			//Decrypten abschliessen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
    			logger.info(decryptedLinks.size() + " download decrypted");
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	
    	return null;
    }
    
    @Override 
    public boolean doBotCheck(File file) {        
        return false;
    }
}
