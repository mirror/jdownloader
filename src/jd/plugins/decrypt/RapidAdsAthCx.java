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

public class RapidAdsAthCx extends PluginForDecrypt {

    static private final String host             = "rapidads.ath.cx";

    private String              version          = "1.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]rapidads.ath.cx/crypter/[+]");

    public RapidAdsAthCx() {
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
        return "RapidAds.ath.cx-1.0.0.";
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
    			
    			progress.setRange(1);
    			RequestInfo reqinfo = getRequest(url, null, null, true);
    			String[] helpa = getBetween(reqinfo.getHtmlCode(), "<p><p><form action=\"", "\"").split("&#");
    			String help = "";
    			
    			for(int i=0; i<helpa.length; i++) {
    				if(!helpa[i].equals(""))
    					help = help + String.valueOf((char) Integer.parseInt(helpa[i]));
    			}
    			
    			progress.increase(1);
    			decryptedLinks.add(this.createDownloadlink(help));
    			
    			// Decrypten abschliessen
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
