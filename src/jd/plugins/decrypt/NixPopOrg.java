package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.DownloadLink;

public class NixPopOrg extends PluginForDecrypt {

    static private String host             = "nix-pop.org";
    
    static private String        version          = "1.0.1";
    
    static private final Pattern patternSupported = getSupportPattern("http://[*]nix-pop\\.org/html/main/(show|showvid|showspec)\\.php\\?id=[0-9]+");

    public NixPopOrg() {
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

    			String links[] = getBetween(reqinfo.getHtmlCode(), "copy&paste</legend>", "</fieldset>").trim().split("<br />");
    			
    			progress.setRange(links.length);
    			this.default_password.add(getBetween(reqinfo.getHtmlCode(), "<p><strong>Passwort</strong>:", "<").trim());
    			
    			// Link der Liste hinzufügen
    			for(int i=0; i<links.length; i++) {
    				decryptedLinks.add(this.createDownloadlink(links[i].trim()));
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