package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class RsLayerCom extends PluginForDecrypt {

    final static String host             = "rs-layer.com";

    private String      version          = "0.1.1";

    private Pattern     patternSupported = getSupportPattern("http://[*]rs-layer\\.com/[+]\\.html");

    public RsLayerCom() {
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

    		try {
    			if ( parameter.indexOf("rs-layer.com/link-") != -1 ) {
    				URL url = new URL(parameter);
    				RequestInfo reqinfo = getRequest(url);
    				
    				String link = getBetween(reqinfo.getHtmlCode(),"<iframe src=\"", "\" ");
    				link = decryptEntities(link);
    				
    				progress.setRange(1);
    				decryptedLinks.add(this.createDownloadlink(link));
    				progress.increase(1);
    				step.setParameter(decryptedLinks);
    			} else if ( parameter.indexOf("rs-layer.com/directory-") != -1 ) {
    				URL url = new URL(parameter);
    				RequestInfo reqinfo = getRequest(url);
    				
    				Vector<Vector<String>>layerLinks = getAllSimpleMatches(
    					reqinfo.getHtmlCode(),"onclick=\"getFile('Â°');");
    				
    				progress.setRange(layerLinks.size());
    				
    				for( int i=0; i<layerLinks.size(); i++ ) {
    		    		String layerLink = "http://rs-layer.com/link-"
    		    			+layerLinks.get(i).get(0)+".html";
    					
    		    		RequestInfo request2 = getRequest(new URL(layerLink));
    		    		String link = getBetween(request2.getHtmlCode(),"<iframe src=\"", "\" ");
    		    		
    		    		decryptedLinks.add(this.createDownloadlink(link));
        				progress.increase(1);
    				}
    				step.setParameter(decryptedLinks);
    			}
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
    
    // Zeichencode-Entities (&#124 etc.) in normale Zeichen umwandeln
    private String decryptEntities(String str) {
    	Vector<Vector<String>> codes = getAllSimpleMatches(str,"&#°;");
    	String decodedString = "";
    	
    	for( int i=0; i<codes.size(); i++ ) {
    		int code = Integer.parseInt(codes.get(i).get(0));
    		char[] asciiChar = {(char)code};
    		decodedString += String.copyValueOf(asciiChar);
		}
    	return decodedString;
    }   
}