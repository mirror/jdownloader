package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * http://stealth.to/?id=13wz0z8lds3nun4dihetpsqgzte4t2
 * 
 * http://stealth.to/?id=ol0fhnjxpogioavfnmj3aub03s10nt
 * 
 * @author astaldo
 * 
 */
public class Stealth extends PluginForDecrypt {
    static private final String host             = "Stealth.to";

    private String              version          = "1.0.0.3";

    private Pattern             patternSupported = getSupportPattern("http://[*]stealth\\.to/\\?id\\=[a-zA-Z0-9]+");

    private Pattern             patternCaptcha   = Pattern.compile("captcha_img.php\\?PHPSESSID");

    public Stealth() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "Astaldo";
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
    public String getHost() {
        return host;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return "STEALTH-1.0.0.";
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			String capTxt = "";
    			File captchaFile = null;
    			
    			for (;;) { // for() läuft bis kein Captcha mehr abgefragt
                    // wird
                    Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());

                    if (matcher.find()) {
                        if(captchaFile!=null && capTxt != null){
                            JDUtilities.appendInfoToFilename(captchaFile,capTxt, false);
                        }
                        String[] help = reqinfo.getCookie().split("=");
                        if(help.length<2){
                            logger.severe("Error sessionid: "+reqinfo.getCookie());
                        
                            step.setParameter(decryptedLinks);
                            return step;
                            
                        }
                        logger.finest("Captcha Protected");
                        String captchaAdress = "http://stealth.to/captcha_img.php?PHPSESSID=" + help[2];
                        captchaFile = getLocalCaptchaFile(this);
                        JDUtilities.download(captchaFile, captchaAdress);

                        capTxt = Plugin.getCaptchaCode(captchaFile, this);
                        String postData = "txtCode="+capTxt+"&eintrag=OK";

                        reqinfo = postRequest(url, "PHPSESSID=" + help[2], parameter, null, postData, true);
                    }
                    else {
                        if(captchaFile!=null && capTxt != null){
                            JDUtilities.appendInfoToFilename(captchaFile,capTxt, true);
                        }
                        break;
                    }
                }
    			
    			RequestInfo reqhelp = postRequest(new URL("http://stealth.to/ajax.php"), null, parameter, null, "id=" + getBetween(reqinfo.getHtmlCode(), "<div align=\"center\"><a id=\"", "\" href=\"") + "&typ=hit", true);
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "dl = window.open(\"°\"");
    			progress.setRange( links.size());
				
				for(int j=0; j<links.size(); j++) {
					reqhelp = getRequest(new URL("http://stealth.to/" + links.get(j).get(0)));
    				decryptedLinks.add(JDUtilities.htmlDecode(getBetween(reqhelp.getHtmlCode(), "iframe src=\"", "\"")));
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
}
