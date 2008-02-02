package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class SerienFreaksTv extends PluginForDecrypt {
    private static final String  CODER              = "Bo0nZ/Coa fixed";

    private static final String  HOST               = "serienfreaks.tv";

    private static final String  PLUGIN_NAME        = HOST;

    private static final String  PLUGIN_VERSION     = "1.0.0.0";

    private static final String  PLUGIN_ID          = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    //http://serienfreaks.tv/?id=5554
    private static final Pattern PAT_SUPPORTED      = getSupportPattern("http://[*]serienfreaks.tv/category/[+]/[+]");

    private Pattern	PAT_CAPTCHA = Pattern.compile("<TD><IMG SRC=\"/gfx/secure/index.php");
    private Pattern	PAT_NO_CAPTCHA = Pattern.compile("Der Sicherheitescode wurde");

    /*
     * Konstruktor
     */
    public SerienFreaksTv() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("serienfreaks.tv");
        default_password.add("serienfreaks.dl.am");

        this.initConfig();
    }

    /*
     * Funktionen
     */
    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    			URL url = new URL(parameter);
    			
    			RequestInfo reqinfo = getRequest(url);
    			File captchaFile = null;
    			String capTxt = "";
    			String host = url.getHost();
    			
    			if(!host.startsWith("http"))
    				host = "http://" + host;
    			
    			String pass = getBetween(getMatches(reqinfo.getHtmlCode(), Pattern.compile("Passwort(.*?)\">(.*?)<")).get(0), "\">", "<");
    			if(!pass.equals("n/a"))
    				this.default_password.add(pass);
    			
    			for (;;) {
                    Matcher matcher = PAT_CAPTCHA.matcher(reqinfo.getHtmlCode());

                    if (matcher.find()) {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(captchaFile, capTxt, false);
                        }

                        logger.finest("Captcha Protected");
                        String captchaAdress = host + getBetween(reqinfo.getHtmlCode(), "<TD><IMG SRC=\"", "\"");
                        captchaFile = getLocalCaptchaFile(this);
                        JDUtilities.download(captchaFile, captchaAdress);

                        capTxt = JDUtilities.getCaptcha(this, "serienfreaks.tv", captchaFile, false);
                        String posthelp = getFormInputHidden(getBetween(reqinfo.getHtmlCode(), "</TD>\n												</TR>\n											</TABLE>\n\n											<FORM ACTION=\"", "</FORM>"));
                        reqinfo = postRequest(new URL("http://" + reqinfo.getConnection().getURL().getHost() + getAllSimpleMatches(reqinfo.getHtmlCode(), "<FORM ACTION=\"°\"").get(2).get(0)), posthelp + "&code=" + capTxt);
                    }
                    else {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(captchaFile, capTxt, true);
                        }

                       	Matcher matcher_no = PAT_NO_CAPTCHA.matcher(reqinfo.getHtmlCode());
                                               	
                        if(matcher_no.find()) {
                        	System.out.println("test");
                        	reqinfo = getRequest(url);
                        }
                        else {
                        	break;
                        }
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

    private void initConfig() {
      //  ConfigEntry cfg;
      
        ConfigEntry cfgLabel1 = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hier kannst du deine bevorzugten Hoster angeben (durch Semikolon getrennt).");
        ConfigEntry cfgLabel2 = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Sofern vorhanden wird dann von diesem Hoster geladen, im anderen Fall wird der");
        ConfigEntry cfgLabel3 = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Standard-Hoster verwendet.");
        ConfigEntry cfgTextField = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), "FAVORITES", "Hoster: ");
        cfgTextField.setDefaultValue("Rapidshare.com;Xirror.com");

        config.addEntry(cfgLabel1);
        config.addEntry(cfgLabel2);
        config.addEntry(cfgLabel3);
        config.addEntry(cfgTextField);
    }

}
