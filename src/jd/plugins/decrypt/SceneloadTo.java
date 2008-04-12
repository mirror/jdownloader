//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;
import jd.plugins.DownloadLink;

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
    		
    			ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "ACTION=\"°\"");
    			
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