package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.DownloadLink;

public class RockHouseIn extends PluginForDecrypt {

    final static String host             = "rock-house.in";

    private String      version          = "1.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]rock-house.in/warez/warez\\_download.php\\?id=[+]");

    public RockHouseIn() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Rock-House.in-1.0.0.";
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
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "<td><a href=\'°\' target=\'_blank\'>");

    			default_password.add(jd.utils.JDUtilities.htmlDecode(getBetween(reqinfo.getHtmlCode(), "<td class=\'button\'>Passwort:</td><td class=\'button\'>", "<")));
    			for(int i=0; i<links.size(); i++) {
    				decryptedLinks.add(this.createDownloadlink(links.get(i).get(0).replaceAll("\n", "")));
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