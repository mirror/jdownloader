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
public class YoumirrorBiz extends PluginForDecrypt {

	final static String host = "youmirror.biz";

	private String version = "3.0.0.0";

	// youmirror.biz/folder/bg2yt2jkzzodocv
	// youmirror.biz/file/30ucgz4t96hxoz5
	// http://youmirror.biz/adfree/file/erikxrrc0zdowhx
	private Pattern patternSupported =  Pattern.compile("http://.*?youmirror.biz/.*?(file|folder)/[a-zA-Z0-9]{15}", Pattern.CASE_INSENSITIVE);


	private static final String[] USEARRAY = new String[] { "rapidshare.com",
			"bluehost.to", "uploaded.to", "share-online.biz", "megaupload.com",
			"simpleupload.net", "sharebase.de", "archiv.to", "share.gulli.com",
			"speedyshare.com", "netload.in", "load.to", "datenklo.net",
			"cocoshare.cc" };

	private Pattern folderLinks = Pattern.compile(
			"<li><a href=\"(.*?file.*?)\">",
			Pattern.CASE_INSENSITIVE);

	public YoumirrorBiz() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
		this.setConfigEelements();
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
        if(link==null)
            return false;
        link=link.toLowerCase();
        for (int i = 0; i < USEARRAY.length; i++) {
            if (link.matches(".*" + USEARRAY[i] + ".*")) {
                return getProperties()
                .getBooleanProperty(USEARRAY[i], true);
            }
        }
        return false;
    }
	private Vector<DownloadLink> getLinks(String parameter) {
		Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
		try {
			RequestInfo reqinfo = getRequest(new URL(parameter));
			Form[] forms = Form.getForms(reqinfo);
			for (int i = 0; i < forms.length; i++) {
				Form form = forms[i];
				String location = new Regexp(form.getRequestInfo().getHtmlCode(), "<iframe .*? src=\"(.*?)\"").getFirstMatch();
				if(getUseConfig(location))
					decryptedLinks.add(createDownloadlink(location));
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
			if (parameter.matches("http://.*?youmirror.biz/.*?folder/[a-zA-Z0-9]{15}")) {
				try {
					URL url = new URL(parameter);
					RequestInfo reqinfo = getRequest(url);
					Matcher matcher = folderLinks
							.matcher(reqinfo.getHtmlCode());
					String hst = "http://" + url.getHost();
					String[] links = new Regexp(matcher).getMatches(1);
					progress.setRange(links.length);
					for (int i = 0; i < links.length; i++) {
						decryptedLinks.addAll(getLinks(hst + links[i]));
	    				progress.increase(1);
					}
					while (matcher.find()) {

						
					}

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				progress.setRange(1);
				decryptedLinks.addAll(getLinks(parameter));
				progress.increase(1);
			}
			step.setParameter(decryptedLinks);
		}
		return null;
	}

	private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL,
                "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
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