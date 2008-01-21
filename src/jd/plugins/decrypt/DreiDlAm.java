package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;


public class DreiDlAm extends PluginForDecrypt {

    static private final String host  = "3dl.am";

    private String  version           = "0.5.0.0";
    
  //by b0ffed8a0d8922c50178568def005e91
    
    static private final Pattern patternSupported = getSupportPattern("http://[*]3dl.am/download/[+]/[+]");
    
    public DreiDlAm() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "b0ffed8a0d8922c50178568def005e91";
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
    			URL url = new URL(parameter);
    			
    			RequestInfo reqinfo = getRequest(url);
    			this.default_password.add(getBetween(reqinfo.getHtmlCode(), "Passwort:</b></td><td><input type='text' value='", "'"));

    			reqinfo = postRequest(new URL(getBetween(reqinfo.getHtmlCode(), "\"center\">\n			<form action=\"", "\"")), "");

       			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "' target='_blank'>°<");
       			progress.setRange(links.size());
       			
       			for(int i=0; i<links.size(); i++) {
       				decryptedLinks.add(this.createDownloadlink((new Regexp(getRequest(new URL(links.get(i).get(0))).getHtmlCode(), "<frame src=\"(.*?)\"")).getFirstMatch()));
       				progress.increase(1);
       			}
					
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