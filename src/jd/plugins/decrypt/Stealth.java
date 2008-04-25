//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.plugins.CRequest.CaptchaInfo;
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

    private String              version          = "1.0.0.4";

    private Pattern             patternSupported = getSupportPattern("http://[*]stealth\\.to/\\?id\\=[a-zA-Z0-9]+");


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
        return host+version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    			CaptchaInfo<File, String> captchaInfo = null;
    			request.getRequest(parameter);
    			for (;;) { // for() läuft bis kein Captcha mehr abgefragt
                    // wird
                    if (request.toString().contains("captcha_img.php")) {
                        if(captchaInfo!=null && captchaInfo.captchaFile!=null && captchaInfo.captchaCode != null){
                            JDUtilities.appendInfoToFilename(this, captchaInfo.captchaFile,captchaInfo.captchaCode, false);
                        }
                        String sessid = new Regexp(request.getCookie(), "PHPSESSID=([a-zA-Z0-9]*)").getFirstMatch();
                        if(sessid==null){
                            logger.severe("Error sessionid: "+request.getCookie());
                        
                            step.setParameter(decryptedLinks);
                            return step;
                            
                        }
                        logger.finest("Captcha Protected");
                        String captchaAdress = "http://stealth.to/captcha_img.php?PHPSESSID=" + sessid;
                        captchaInfo = request.getCaptchaCode(this, captchaAdress);
                        Form form = request.getForm();
                        form.put("txtCode", captchaInfo.captchaCode);
                        request.setRequestInfo(form);
                    }
                    else {
                        if(captchaInfo!=null && captchaInfo.captchaFile!=null && captchaInfo.captchaCode != null){
                            JDUtilities.appendInfoToFilename(this, captchaInfo.captchaFile,captchaInfo.captchaCode, true);
                        }
                        break;
                    }
                }
    			RequestInfo reqhelp = postRequest(new URL("http://stealth.to/ajax.php"), null, parameter, null, "id=" + getBetween(request.getHtmlCode(), "<div align=\"center\"><a id=\"", "\" href=\"") + "&typ=hit", true);
    			ArrayList<ArrayList<String>> links = getAllSimpleMatches(request.getHtmlCode(), "dl = window.open(\"°\"");
    			progress.setRange( links.size());
				
				for(int j=0; j<links.size(); j++) {
					reqhelp = getRequest(new URL("http://stealth.to/" + links.get(j).get(0)));
    				decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(getBetween(reqhelp.getHtmlCode(), "iframe src=\"", "\""))));
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
