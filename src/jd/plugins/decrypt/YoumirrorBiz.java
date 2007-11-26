package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

public class YoumirrorBiz extends PluginForDecrypt {

	final static String host = "youmirror.biz";
	private String version = "1.0.0.0";
	private Pattern patternSupported = getSupportPattern("http://[*]youmirror.biz[*]/f[+]");
	private Pattern patternRapidshare = Pattern.compile("1\" target=\"_blank\">rapidshare.com");//+
	private Pattern patternNetload = Pattern.compile("2\" target=\"_blank\">netload.in");
	private Pattern patternUploaded = Pattern.compile("4\" target=\"_blank\">uploaded.to");//+
	private Pattern patternGulli = Pattern.compile("5\" target=\"_blank\">gulli.com");//+
	private Pattern patternShareonline = Pattern.compile("6\" target=\"_blank\">share-online.biz");//+
	private Pattern patternLoad = Pattern.compile("7\" target=\"_blank\">load.to");//+
	private Pattern patternSimpleupload = Pattern.compile("8\" target=\"_blank\">simpleupload.net");//+
	private Pattern patternCocoshare = Pattern.compile("9\" target=\"_blank\">cocoshare.cc");//+
	private Pattern patternFilehoster = Pattern.compile("10\" target=\"_blank\">filehoster.mobi");
	private Pattern patternMegaupload = Pattern.compile("11\" target=\"_blank\">megaupload.com");//+
	private Pattern patternSpeedyshare = Pattern.compile("12\" target=\"_blank\">speedyshare.com");//+
	private Pattern patternArchiv = Pattern.compile("13\" target=\"_blank\">archiv.to");//+
	private Pattern patternDatenklo = Pattern.compile("/14\" target=\"_blank\">datenklo.net");//+
	private Pattern patternBluehost= Pattern.compile("/15\" target=\"_blank\">bluehost.to");//+
	private Pattern patternSharebase = Pattern.compile("/16\" target=\"_blank\">sharebase.de");//+
	/*
	 * 1	Rapidshare.com
	 * 2	Netload.in
	 * 3	oxedion (offline)
	 * 4	Uploaded.to
	 * 5	Gulli.com
	 * 6	Share-online.biz
	 * 7	Load.to
	 * 8	Simpleupload.net
	 * 9	cocoshare.cc
	 * 10	Filehoster.mobi
	 * 11	Megaupload.com
	 * 12	Speedyshare
	 * 13	archiv.to
	 * 14	Datenklo
	 * 15	Bluehost.to
	 * 16	Sharebase.de
	 */
	
    public YoumirrorBiz() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }
	
    @Override public String getCoder() { return "Botzi"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Youmirror.biz-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			int count = 0;
    			//Links zählen
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true) && countOccurences(reqinfo.getHtmlCode(), patternRapidshare) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternNetload) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED", true) && countOccurences(reqinfo.getHtmlCode(), patternUploaded) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_GULLI", true) && countOccurences(reqinfo.getHtmlCode(), patternGulli) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREONLINE", true) && countOccurences(reqinfo.getHtmlCode(), patternShareonline) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_LOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternLoad) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternSimpleupload) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_COCOSHARE", true) && countOccurences(reqinfo.getHtmlCode(), patternCocoshare) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_FILEHOSTER", true) && countOccurences(reqinfo.getHtmlCode(), patternFilehoster) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternMegaupload) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SPEEDYSHARE", true) && countOccurences(reqinfo.getHtmlCode(), patternSpeedyshare) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_ARCHIV", true) && countOccurences(reqinfo.getHtmlCode(), patternArchiv) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_BLUEHOST", true) && countOccurences(reqinfo.getHtmlCode(), patternBluehost) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_DATENKLO", true) && countOccurences(reqinfo.getHtmlCode(), patternDatenklo) > 0) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREBASE", true) && countOccurences(reqinfo.getHtmlCode(), patternSharebase) > 0) {
    				count++;
    			}
    			
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, count));

    			RequestInfo reqhelp;
    			
    			parameter = parameter.replaceAll("/file/", "/out/");
    			parameter = parameter.replaceAll("/adfree", "");
    			
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true) && countOccurences(reqinfo.getHtmlCode(), patternRapidshare) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/1"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternNetload) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/2"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED", true) && countOccurences(reqinfo.getHtmlCode(), patternUploaded) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/4"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_GULLI", true) && countOccurences(reqinfo.getHtmlCode(), patternGulli) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/5"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREONLINE", true) && countOccurences(reqinfo.getHtmlCode(), patternShareonline) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/6"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_LOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternLoad) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/7"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternSimpleupload) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/8"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_COCOSHARE", true) && countOccurences(reqinfo.getHtmlCode(), patternCocoshare) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/9"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_FILEHOSTER", true) && countOccurences(reqinfo.getHtmlCode(), patternFilehoster) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/10"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD", true) && countOccurences(reqinfo.getHtmlCode(), patternMegaupload) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/11"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SPEEDYSHARE", true) && countOccurences(reqinfo.getHtmlCode(), patternSpeedyshare) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/12"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_ARCHIV", true) && countOccurences(reqinfo.getHtmlCode(), patternArchiv) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/13"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_DATENKLO", true) && countOccurences(reqinfo.getHtmlCode(), patternDatenklo) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/14"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_BLUEHOST", true) && countOccurences(reqinfo.getHtmlCode(), patternBluehost) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/15"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREBASE", true) && countOccurences(reqinfo.getHtmlCode(), patternSharebase) > 0) {
    				reqhelp = getRequest(new URL(parameter + "/16"));
    				decryptedLinks.add(getBetween(reqhelp.getHtmlCode(), "src=\"", "\""));
    				firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
    			}

    			//Decrypt abschliessen
    			firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 JDUtilities.logException(e);
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
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_UPLOADED", "Uploaded.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_GULLI", "Gulli.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SHAREONLINE", "Share-online.biz"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_LOAD", "Load.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SIMPLEUPLOAD", "Simpleupload.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_COCOSHARE", "Cocoshare.cc"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_FILEHOSTER", "Filehoster.mobi"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_MEGAUPLOAD", "Megaupload.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SPEEDYSHARE", "Speedyshare.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_ARCHIV", "Archiv.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_DATENKLO", "Datenklo.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_BLUEHOST", "Bluehost.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SHAREBASE", "Sharebase.de"));
        cfg.setDefaultValue(false);
    }
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }
}