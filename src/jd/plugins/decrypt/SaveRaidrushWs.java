package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

//http://save.raidrush.ws/?id=8b891e864bc42ffa7bfcdaf72503f2a0
//http://save.raidrush.ws/?id=e7ccb3ee67daff310402e5e629ab8a91
//http://save.raidrush.ws/?id=c17ce92bc6154713f66b151b8f55684

public class SaveRaidrushWs extends PluginForDecrypt {

    static private final String host             = "save.raidrush.ws";

    private String              version          = "1.1.0";
    private Pattern             patternSupported = getSupportPattern("http://[*]save\\.raidrush\\.ws/\\?id\\=[a-zA-Z0-9]+");
    private Pattern             patternCount     = Pattern.compile("\',\'FREE\',\'");

    public SaveRaidrushWs() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
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
        return "Save.Raidrush.ws-1.0.0.";
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
    			
    			RequestInfo reqinfo = getRequest(new URL(parameter));

    			progress.setRange( countOccurences(reqinfo.getHtmlCode(), patternCount));
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "get('°','FREE','°');");
    			
    			for(int i=0; i<links.size(); i++) {
    				
    				Vector<String> help = links.get(i);
    				reqinfo = getRequest(new URL("http://save.raidrush.ws/404.php.php?id=" + help.get(0) + "&key=" + help.get(1)));
    				progress.increase(1);
    				decryptedLinks.add(this.createDownloadlink("http://"+reqinfo.getHtmlCode().trim()));
    				
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