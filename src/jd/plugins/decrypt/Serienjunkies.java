package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;

public class Serienjunkies extends PluginForDecrypt {
    static private final String  host = "serienjunkies.safehost.be";
    private String version = "2.0.0.0";
    //http://85.17.177.195/sjsafe/f-e657c0c256dd9e58/rc_h324.html
    private Pattern patternSupported = getSupportPattern("http://85.17.177.195/[+]");
    private Pattern patternRapidshareCom = Pattern.compile("rapidshare.com");
    private Pattern patternRapidshareDe = Pattern.compile("rapidshare.de");
    private Pattern patternNetload = Pattern.compile("netload.in");

    public Serienjunkies() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }
    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override public String getCoder() { return "DwD aka James / Botzi";}
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Serienjunkies-2.0.0."; }
    @Override public String getPluginName() { return "SerienJunkies.dl.am"; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
    @Override public boolean isClipboardEnabled() { return true; }
  
   
    
    @Override
    public boolean doBotCheck(File file) {        
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
    	switch (step.getStep()){
    		case PluginStep.STEP_DECRYPT:
    			Vector<String> decryptedLinks = new Vector<String>();
    			try {
    				URL url = new URL(parameter);
    				RequestInfo reqinfo = getRequest(url);
    				Vector<Vector<String>> links;
    				
    				links = getAllSimpleMatches(reqinfo.getHtmlCode(), "href=\"http://85.17.177.195/save/°\"");
    				//System.out.println(links.size());
    				
    				/*for(int i=0; i<links.size(); i++) {
    					System.out.println("href=\"http://85.17.177.195/save/" + links.get(i).get(0));
    				}*/
   
    				reqinfo = getRequest(new URL("http://85.17.177.195/sjsafe/f-b47e1e19598fb50f/rc_bab5501.html"));
    				System.out.println(reqinfo.getHtmlCode());
   
    				String captchaAdress = "http://85.17.177.195" + getBetween(reqinfo.getHtmlCode(), "TD><IMG SRC=\"", "\" ALT=\"\" BORDER=\"0\"");
    				File dest = JDUtilities.getResourceFile("captchas/" + this.getPluginName() + "/captcha_" + (new Date().getTime()) + ".jpg");
    				JDUtilities.download(dest, captchaAdress);

    				String capTxt = Plugin.getCaptchaCode(dest, this);
    				System.out.println(capTxt);
               
			   } 
			   catch (MalformedURLException e) { e.printStackTrace(); } 
			   catch (IOException e) { e.printStackTrace(); }
    	}
    	return null;
    }
    
    private void setConfigEelements() {
    	ConfigEntry cfg;
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHAREDE", "Rapidshare.de"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
    }
}
