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

public class AnimeANet extends PluginForDecrypt {

	final static String host = "animea.net";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://www.animea.net/download/[+]/[*]");
	
    public AnimeANet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "AnimeA.net-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
  
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
            parameter = parameter.replaceAll(" ", "+");
            
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);

    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "href=javascript:reqLink(\'°\')>");
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, links.size()));
    			
    			for(int i=0; i<links.size(); i++) {
    				reqinfo = postRequest(new URL("http://www.animea.net/download_link.php?e_id=" + links.get(i).get(0)), "submit=Open");
    				decryptedLinks.add(getBetween(reqinfo.getHtmlCode(), "width=\"12\" height=\"11\" /><a href=\"", "\" target=\"_blank\">Download"));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
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
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }
}