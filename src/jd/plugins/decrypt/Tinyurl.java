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

public class Tinyurl extends PluginForDecrypt {

	static private String host = "tinyurl.com";
	private String version = "1.0.0.0";
	private Pattern patternSupported = Pattern.compile("http://.*tinyurl\\.com/.*");
	private Pattern patternLink = Pattern.compile("http://tinyurl\\.com/.*");
	
    public Tinyurl() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Tinyurl-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
    @Override public boolean isClipboardEnabled() { return true; }
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, 1));

    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			
    			//Besonderen Link herausfinden
    			if (countOccurences(parameter, patternLink)>0) {
    				String[] result = parameter.split("/");
	    			reqinfo = getRequest(new URL("http://tinyurl.com/preview.php?num=" + result[result.length-1]));	    			
    			}
    			
    			//Link der Liste hinzufügen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			decryptedLinks.add(getBetween(reqinfo.getHtmlCode(),"id=\"redirecturl\" href=\"","\">Proceed to"));
    			
    			//Decrypt abschliessen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
    			logger.info(decryptedLinks.size() + " downloads decrypted");
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			e.printStackTrace();
    		}
    	}
    	return null;
    }
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }
}