package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class SceneloadTo extends PluginForDecrypt {

    static private final String host             = "sceneload.to";
    private String              version          = "1.0.0.0";
    private Pattern             patternSupported = getSupportPattern("http://[*]sceneload.to/\\?id=[+]");
    private Pattern				PAT_CAPTCHA			 = Pattern.compile("Bitte gebe den <b>Sicherheitscode</b> korrekt ein");

    public SceneloadTo() {
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
        return "Sceneload.to-1.0.0.";
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
    			File captchaFile = null;
    			String capTxt = "";
    			
    			String pass = getBetween(getMatches(reqinfo.getHtmlCode(), Pattern.compile("Passwort(.*?)\">(.*?)<")).get(0), "\">", "<");
    			if(!pass.equals("n/a"))
    				this.default_password.add(pass);
    			
    			for (;;) {
                    Matcher matcher = PAT_CAPTCHA.matcher(reqinfo.getHtmlCode());

                    if (matcher.find()) {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                        }

                        logger.finest("Captcha Protected");
                        String captchaAdress = "http://www.sceneload.to" + getBetween(reqinfo.getHtmlCode(), "<TD><IMG SRC=\"", "\"");
                        captchaFile = getLocalCaptchaFile(this);
                        JDUtilities.download(captchaFile, captchaAdress);

                        capTxt = JDUtilities.getCaptcha(this, "hardcoremetal.biz", captchaFile, false);
                        
                        String posthelp = getFormInputHidden(getBetween(reqinfo.getHtmlCode(), "NAME=\"download_form\"", "</FORM>"));
                        
                        reqinfo = postRequest(url, posthelp + "&code=" + capTxt);
                    }
                    else {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                        }
                        break;
                    }
                }
    		
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "ACTION=\"°\"");
    			
    			for(int i=0; i<links.size(); i++)
    				decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
    			
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