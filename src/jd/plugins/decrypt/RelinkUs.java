package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class RelinkUs extends PluginForDecrypt {

    final static String host             = "relink.us";

    private String      version          = "1.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]relink\\.us/go\\.php\\?id=[\\d]{5}");

    public RelinkUs() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "Botzi";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Relink.us-1.0.0.";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "form action=\'°\' method=\'post\'");
    			progress.setRange( links.size());
    			
    			for(int i=0; i<links.size(); i++) {
    				reqinfo = postRequest(new URL("http://relink.us/" + links.get(i).get(0)), "submit=Open");
    				decryptedLinks.add(getBetween(reqinfo.getHtmlCode(), "iframe name=\"pagetext\" height=\"100%\" frameborder=\"no\" width=\"100%\" src=\"", "\""));
    			progress.increase(1);
    			}
    			
    			// Decrypt abschliessen
    			
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