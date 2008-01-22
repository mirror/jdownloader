package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class CryptGetMoviesOrg extends PluginForDecrypt {

    final static String host             = "crypt.get-movies.org";

    private String      version          = "1.0.0.0";


    private Pattern     patternSupported = getSupportPattern("http://crypt\\.get-movies\\.org/[\\d]{4}");

    public CryptGetMoviesOrg() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("www.get-movies.6x.to");
        default_password.add("get-movies.6x.to");
        default_password.add("get-movies.org");
        default_password.add("www.get-movies.org");
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
        return "crypt.get-movies.org-1.0.0.";
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

    			progress.setRange( 1);
    			
    			decryptedLinks.add(this.createDownloadlink(getBetween(reqinfo.getHtmlCode(), "frameborder=\"no\" width=\"100%\" src=\"", "\"></iframe>")));
    		progress.increase(1);
    			
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