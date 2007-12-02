package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class SaveRaidrushWs extends PluginForDecrypt {

    static private final String host             = "save.raidrush.ws";

    private String              version          = "1.0.0.0";

    private Pattern             patternSupported = getSupportPattern("http://save.raidrush.ws/\\?id\\=[+]");

    private Pattern             patternCount     = Pattern.compile("\',\'FREE\',\'");

    public SaveRaidrushWs() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));

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
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			
    			RequestInfo reqinfo = getRequest(url);

    			progress.setRange( countOccurences(reqinfo.getHtmlCode(), patternCount));
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "\'°\',\'FREE\',\'°\'");
    			
    			for(int i=0; i<links.size(); i++) {
    				Vector<String> help = links.get(i);
    				reqinfo = getRequest(new URL("http://save.raidrush.ws/c.php?id=" + help.get(0) + "&key=" + help.get(1)));
    			progress.increase(1);
    				decryptedLinks.add("http://"+reqinfo.getHtmlCode().trim());
    			}
    		
    			// Decrypten abschliessen
    			
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