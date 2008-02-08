package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class LeecherWs extends PluginForDecrypt {

    final static String host             = "leecher.ws";

    private String      version          = "0.1.0";
    
    private Pattern     patternSupported = getSupportPattern(
    		"(http://[*]leecher\\.ws/folder/[+])"
    		+ "|(http://[*]leecher\\.ws/out/[+]/[0-9]+)");

    public LeecherWs() {
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
            Vector<Vector<String>> outLinks = new Vector<Vector<String>>();
            
    		try {
    			
    			if ( parameter.indexOf("out") != -1 ) {
    				
    				Vector<String> tempVector = new Vector<String>();
    				tempVector.add(parameter.substring(parameter.lastIndexOf("leecher.ws/out/")+15));
    				outLinks.add(tempVector);
    				
    			} else {
    				
    				reqinfo = getRequest(new URL(parameter));
    				outLinks = getAllSimpleMatches(reqinfo.getHtmlCode(),
					"href=\"http://www.leecher.ws/out/°\"");
    				
    			}
    			
    			progress.setRange(outLinks.size());
    			
    			for ( int i=0; i<outLinks.size(); i++ ) {
    				
    				reqinfo = getRequest(new URL(
    						"http://leecher.ws/out/"+outLinks.get(i).get(0)));
    				String cryptedLink = getBetween(reqinfo.getHtmlCode(),"<iframe src=\"","\"");
    				decryptedLinks.add(this.createDownloadlink(decryptAsciiEntities(cryptedLink)));
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
    
    // Zeichencode-Entities (&#124 etc.) in normale Zeichen umwandeln
    private String decryptAsciiEntities(String str) {
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