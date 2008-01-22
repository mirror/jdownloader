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

public class NetfolderIn extends PluginForDecrypt {

    static private String host             = "netfolder.in";

    private String        version          = "1.0.0.0";
    //netfolder.in/folder.php?folder_id=b8b3b0b 
    static private final Pattern patternSupported = Pattern.compile("http://.*?netfolder\\.in/folder\\.php\\?folder_id\\=[a-zA-Z0-9]{7}", Pattern.CASE_INSENSITIVE);

    public NetfolderIn() {
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
        return "Netfolder.in-1.0.0.";
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
    			String password = "";
    			
    			while (true) {
                 	int check = countOccurences(reqinfo.getHtmlCode(), Pattern.compile("input type=\"password\" name=\"password\""));
                 	
                 	if (check > 0) {
                 		password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                 		
                 		if(password == null) {
                 			step.setParameter(decryptedLinks);
                 			return null;
                 		}
                 		
                 		reqinfo = postRequest(url, "password=" + password + "&save=Absenden");
                 	} else {
                 		break;
                 	}
     			}
    			
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "href=\"http://netload.in/°\"");
    			
    			progress.setRange( links.size());
    			
    			// Link der Liste hinzufügen
    			for(int i=0; i<links.size(); i++) {
    				decryptedLinks.add(this.createDownloadlink("http://netload.in/" + links.get(i).get(0)));
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