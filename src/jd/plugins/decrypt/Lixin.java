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
import jd.utils.JDUtilities;

public class Lixin extends PluginForDecrypt {

    static private final String  host = "lix.in";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://lix.in/[+]");
	
    public Lixin() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Lix.in-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }

    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, 1));
    			
    			//Letzten Teil der URL herausfiltern und postrequest durchführen
    			String[] result = parameter.split("/");
    			RequestInfo reqinfo = postRequest(url, "tiny=" + result[result.length-1] + "&submit=continue");
    			
    			//Link herausfiltern
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			decryptedLinks.add((getBetween(reqinfo.getHtmlCode(), "name=\"ifram\" src=\"", "\" marginwidth")));
    			
    			//Decrypten abschliessen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
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
