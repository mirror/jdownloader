package jd.plugins.decrypt;
import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Form;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;

// http://youmirror.biz/folder/bg2yt2jkzzodocv
// http://youmirror.biz/file/30ucgz4t96hxoz5
// http://youmirror.biz/adfree/file/erikxrrc0zdowhx

public class YoumirrorBiz extends PluginForDecrypt {

	final static String host = "youmirror.biz";

	private String version = "3.1.0";

	private Pattern patternSupported =  Pattern.compile("http://.*?youmirror.biz/.*?(file|folder)/[a-zA-Z0-9]{15}", Pattern.CASE_INSENSITIVE);


	private static final String[] USEARRAY = new String[] {
			"Bluehost.to", "Rapidshare.com", "Sharebase.de", "Uploaded.to", "CoCoshare.cc",
			"Share.Gulli.com", "Load.to", "MegaUpload.com", "Share-Online.biz", "Archiv.to",
			"SpeedyShare.com", "Fast-Load.net", "SimpleUpload.net", "Netload.in", "DatenKlo.net"
			};

	private Pattern folderLinks = Pattern.compile(
			"<li><a href=\"(.*?file.*?)\">",
			Pattern.CASE_INSENSITIVE);
	
	private String iframeLink = "<iframe .*? src=\"(.*?)\"";

	public YoumirrorBiz() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
		this.setConfigEelements();
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
	
    private boolean getUseConfig(String link) {
        
    	if(link==null) return false;
        link=link.toLowerCase();
        
        for (int i = 0; i < USEARRAY.length; i++) {
            
        	if (link.matches(".*" + USEARRAY[i].toLowerCase() + ".*")) {
                return getProperties().getBooleanProperty(USEARRAY[i], true);
            }
        	
        }
        
        return false;
        
    }
    
	private Vector<DownloadLink> getLinks(String parameter, boolean isFolder) {
		
		Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
		
		try {
			
			RequestInfo reqinfo = getRequest(new URL(parameter));
			Form[] forms = Form.getForms(reqinfo);
			if (!isFolder) progress.setRange(forms.length);
			
			for (int i = 0; i < forms.length; i++) {
				Form form = forms[i];
				String location = new Regexp(form.getRequestInfo().getHtmlCode(), iframeLink).getFirstMatch();
				if(getUseConfig(location)) decryptedLinks.add(createDownloadlink(location));
				if (!isFolder) progress.increase(1);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return decryptedLinks;
		
	}

	@Override
	public PluginStep doStep(PluginStep step, String parameter) {
		
		if (step.getStep() == PluginStep.STEP_DECRYPT) {
			
			Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
			
			if ( parameter.contains("/folder/") ) {
				
				try {
					
					URL url = new URL(parameter);
					RequestInfo reqinfo = getRequest(url);
					Matcher matcher = folderLinks.matcher(reqinfo.getHtmlCode());
					String host = "http://" + url.getHost();
					String[] links = new Regexp(matcher).getMatches(1);
					progress.setRange(links.length);
					
					for (int i = 0; i < links.length; i++) {
						decryptedLinks.addAll(getLinks(host + links[i], true));
	    				progress.increase(1);
					}
					
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
			} else {
				decryptedLinks.addAll(getLinks(parameter, false));
			}
			
			step.setParameter(decryptedLinks);
			
		}
		
		return null;
		
	}

	private void setConfigEelements() {
		
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL,
        		JDLocale.L("plugins.decrypt.general.hosterSelection", "Hoster Auswahl")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        
        for (int i = 0; i < USEARRAY.length; i++) {
            config.addEntry(cfg = new ConfigEntry(
                    ConfigContainer.TYPE_CHECKBOX, getProperties(),
                    USEARRAY[i], USEARRAY[i]));
            cfg.setDefaultValue(true);
        }
        
	}

	@Override
	public boolean doBotCheck(File file) {
		return false;
	}
	
}