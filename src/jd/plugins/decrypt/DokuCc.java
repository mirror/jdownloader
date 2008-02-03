package jd.plugins.decrypt; import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class DokuCc extends PluginForDecrypt {
    final static String host             = "doku.cc";

    private String      version          = "1.0.0.0";

    private Pattern patternSupported = Pattern.compile("http://doku\\.cc/[\\d]{4}/[\\d]{2}/[\\d]{2}/.*", Pattern.CASE_INSENSITIVE);
    
    public DokuCc() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("doku.cc");
    }

	@Override
	public PluginStep doStep(PluginStep step, String parameter) {
	       if (step.getStep() == PluginStep.STEP_DECRYPT) {
	            Vector<DownloadLink>decryptedLinks = new Vector<DownloadLink>();
	    		try {
	                URL url = new URL(parameter);
	                RequestInfo reqinfo = getRequest(url);
	                String html = reqinfo.getHtmlCode();
	                
	                //the patterns that are needed to find the links
	    			Pattern patternDownload = Pattern.compile(".*<strong>Download.*");
	    			Pattern patternMirror = Pattern.compile(".*<strong>Mirror.*");
	    			Pattern patternLink = Pattern.compile( "http://.{0,5}lix\\.in/[a-zA-Z0-9]{6,10}", Pattern.CASE_INSENSITIVE );
	    			
	    			//split the html into single lines and process them
	                String [] lines = html.split("\\n");
		        	Lixin lixin = new Lixin();
		        	
	                for (String line : lines) {
	    				Matcher matcherDownload = patternDownload.matcher(line);
	    				Matcher matcherMirror = patternMirror.matcher(line);
	    				
	    				
	    				if(matcherDownload.matches() || matcherMirror.matches()){
	    	    			Vector<String> encryptedLinks = new Vector<String>();
	    	    			
	    					for( Matcher m = patternLink.matcher(line); m.find(); ){
	    						encryptedLinks.add(m.group());
	    					}
	    		        	decryptedLinks.addAll( lixin.decryptLinks(encryptedLinks) );
	    				}
	    			}
	                step.setParameter(decryptedLinks);
	            }
	            catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	        return null;
	}

	@Override
	public boolean doBotCheck(File file) {
		return false;
	}

	@Override
	public String getCoder() {
		return "signed";
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public String getPluginID() {
		return host + "-" + version;
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

}