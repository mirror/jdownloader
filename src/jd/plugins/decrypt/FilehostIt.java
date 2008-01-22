package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class FilehostIt extends PluginForDecrypt {

    final static String host             = "filehost.it";

    private String      version          = "1.0.0.0";
//www.filehost.it/multilink/checklinks.php?links=4249 
    private Pattern     patternSupported = getSupportPattern("http://[*]filehost\\.it/(multi|live)link/checklinks.php\\?links=[\\d]+");

    public FilehostIt() {
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
        return "Filehost.it-1.0.0.";
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
    			Vector<Vector<String>> links = new Vector<Vector<String>>();

    			
    			links = getAllSimpleMatches(reqinfo.getHtmlCode(), "<td>\n								<div align=\"center\"><a href=\"°\">");
    			progress.setRange( links.size());
    			
				for(int i=0; i<links.size(); i++) {
					decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
				progress.increase(1);
				}
    			
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