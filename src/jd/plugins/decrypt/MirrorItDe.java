package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;

public class MirrorItDe extends PluginForDecrypt {

	final static String host = "mirrorit.de";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://[*]mirrorit.de/\\?id=[+]");
	
	private Pattern patternRapidshare = Pattern.compile("Rapidshare");
	private Pattern patternUploaded = Pattern.compile("Uploaded.de");
	private Pattern patternSharebase = Pattern.compile("ShareBase");
	private Pattern patternShareonline = Pattern.compile("Share-Online.biz");
	private Pattern patternMegaupload = Pattern.compile("MegaUpload.com");
	private Pattern patternFilefactory = Pattern.compile("FileFactory");
	private Pattern patternNetload = Pattern.compile("Netload.in");
	private Pattern patternSimpleupload = Pattern.compile("SimpleUpload");
	private Pattern patternLoad = Pattern.compile("Load.to");
	
    public MirrorItDe() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Mirrorit.de-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
    @Override public boolean isClipboardEnabled() { return true; }
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			int count = 0;
    			
    			//Links zählen
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternRapidshare);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED",true)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternUploaded);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREBASE",false)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternSharebase);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREONLINE",false)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternShareonline);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD",true)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternMegaupload);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_FILEFACTORY",true)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternFilefactory);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD",true)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternNetload);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",false)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternSimpleupload);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_LOAD",false)){
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternLoad);
    			}
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, count));
    			
    			//Links herausfiltern
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "class=\"five-stars\" onclick=\"rate(\'°\', 5)°launchDownloadURL(\'°\', \'°\'");
    			for(int i=0; i<links.size(); i++) {
    				if((links.get(i).get(0).equalsIgnoreCase("rapidsharecom") && (Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) || ((Boolean) this.getProperties().getProperty("USE_UPLOADED",true) && links.get(i).get(0).equalsIgnoreCase("uploadedto")) || ((Boolean) this.getProperties().getProperty("USE_SHAREBASE",false) && links.get(i).get(0).equalsIgnoreCase("sharebasede")) || ((Boolean) this.getProperties().getProperty("USE_SHAREONLINE",false) && links.get(i).get(0).equalsIgnoreCase("shareonlinebiz")) || ((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD",true) && links.get(i).get(0).equalsIgnoreCase("megauploadcom")) || ((Boolean) this.getProperties().getProperty("USE_FILEFACTORY",true) && links.get(i).get(0).equalsIgnoreCase("filefactorycom"))  || ((Boolean) this.getProperties().getProperty("USE_NETLOAD",true) && links.get(i).get(0).equalsIgnoreCase("netloadin")) || ((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",false) && links.get(i).get(0).equalsIgnoreCase("simpleuploadnet")) || ((Boolean) this.getProperties().getProperty("USE_LOAD",false) && links.get(i).get(0).equalsIgnoreCase("loadto"))) {
	    				reqinfo = getRequest(new URL("http://www.mirrorit.de/Out?id=" + URLDecoder.decode(links.get(i).get(2), "UTF-8") + "&num=" + links.get(i).get(3)));
	    				reqinfo = getRequest(new URL(reqinfo.getLocation()));
	    				decryptedLinks.add(reqinfo.getLocation());
	    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    				}
    			}
    			    			
    			//Decrypt abschliessen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			e.printStackTrace();
    		}
    	}
    	return null;
    }
    
    private void setConfigEelements() {
    	ConfigEntry cfg;
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_UPLOADED", "Uploaded.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_FILEFACTORY", "Filefactory.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SHAREBASE", "Sharebase.de"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SHAREONLINE", "Share-Online.biz"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SIMPLEUPLOAD", "Simpleupload.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_LOAD", "Load.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_MEGAUPLOAD", "Megaupload.com"));
        cfg.setDefaultValue(false);
    }
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }
}