package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class TechnorockerInfo extends PluginForDecrypt {

    final static String host             = "technorocker.info";

    private String      version          = "0.1.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]technorocker\\.info/opentrack\\.php\\?id=[0-9]+");

    public TechnorockerInfo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "eXecuTe";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host+"-"+version;
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
            
            default_password.add("technorocker");
            
    		try {
    			
    			RequestInfo reqinfo = getRequest(new URL(parameter));
    			String link = getBetween(reqinfo.getHtmlCode(),"<a href=\"", "\"><b>here</b>");
    			progress.setRange(1);
    			decryptedLinks.add(this.createDownloadlink(link));
    			progress.increase(1);
    			step.setParameter(decryptedLinks);
    				
    		} catch(IOException e) {
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