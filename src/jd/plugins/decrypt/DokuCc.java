package jd.plugins.decrypt; import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
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

	                String[] links = new Regexp(reqinfo.getHtmlCode(), "<p><strong>[^<]+(</strong><a href.*?)</p>").getMatches(1);
	    			for (int i = 0; i < links.length; i++) {
	    				String[] dls = getHttpLinks(links[i], parameter);
	    				for (int j = 0; j < dls.length; j++) {
							decryptedLinks.add(createDownloadlink(dls[j]));
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