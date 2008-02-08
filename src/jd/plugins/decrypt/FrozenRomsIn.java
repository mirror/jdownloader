package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;

public class FrozenRomsIn extends PluginForDecrypt {

    final static String host             = "frozen-roms.in";

    private String      version          = "0.1.0";
    
    private Pattern     patternSupported = getSupportPattern(
    		"http://[*]frozen-roms\\.in/(details_[0-9]+|get_[0-9]+_[0-9]+).html");

    public FrozenRomsIn() {
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

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
    		
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            RequestInfo reqinfo;
            Vector<Vector<String>> getLinks = new Vector<Vector<String>>();
            
    		try {
    			
    			if ( parameter.indexOf("get") != -1 ) {
    				
    				Vector<String> tempVector = new Vector<String>();
    				tempVector.add(getBetween(parameter,"http://frozen-roms.in/get_",".html"));
    				getLinks.add(tempVector);
    				
    			} else {
    				
    				reqinfo = getRequest(new URL(parameter));
    				getLinks = getAllSimpleMatches(reqinfo.getHtmlCode(),
    						"href=\"http://frozen-roms.in/get_°.html\"");
    				
    			}
    			
    			progress.setRange(getLinks.size());
    			
    			for ( int i=0; i<getLinks.size(); i++ ) {
    				
    				reqinfo = getRequest(new URL(
    						"http://frozen-roms.in/get_"+getLinks.get(i).get(0)+".html"));
    				decryptedLinks.add(this.createDownloadlink(
    						reqinfo.getConnection().getHeaderField("Location")));
    				progress.increase(1);
    				
    			}
    			
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