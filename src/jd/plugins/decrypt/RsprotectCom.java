package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class RsprotectCom extends PluginForDecrypt {

    final static String host             = "rsprotect.com";

    private String      version          = "1.0.0.0";
    //www.rsprotect.com/rs-QDZkNWZyYTM/uds-AKeys-unp.rar
    private Pattern             patternSupported   = Pattern.compile("http://.*?rsprotect\\.com/r[sc]-[a-zA-Z0-9]{11}/.*", Pattern.CASE_INSENSITIVE);

    public RsprotectCom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "Botzi";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Rsprotect.com-1.0.0.";
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
    			
    			decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(getBetween(reqinfo.getHtmlCode(), "<FORM ACTION=\"", "\" METHOD=\"post\" ID=\"postit\""))));
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