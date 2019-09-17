//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.appwork.utils.Application;
import jd.nutils.encoding.Encoding;
import org.appwork.utils.StringUtils;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cript.to" }, urls = { "https?://(?:www\\.)?cript\\.to/folder/[A-Za-z0-9]+" })
public class CriptTo extends PluginForDecrypt {
	private final String NO_SOLVEMEDIA = "1";

    public CriptTo(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> dupelist = new ArrayList<String>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Inhalt im Usenet gefunden - Weiterleitung erfolgt sofort ...")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);

        boolean failed = true;
        String code = null;
        for (int i = 0; i <= 3; i++) {
        	if(i > 0){
        		br.getPage(parameter);
        	}
        	String postData = "";
            if (this.br.containsHTML("\"g\\-recaptcha\"")) {
            	postData += "captcha_driver=recaptcha";
                postData += "&do=captcha";
                
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                if (StringUtils.isEmpty(recaptchaV2Response)) {
	                    if (i < 3) {
                        	continue;
                    	} else {
                        	throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    	}
                    }
                postData += "&g-recaptcha-response=" + recaptchaV2Response;
                postData += "&submit=confirm";
                final String linksafe_csrf_token = br.getRegex("<input type=\"hidden\" name=\"linksafe_csrf_token\" value=\"([^\"]*)\"").getMatch(0);
				postData += "&linksafe_csrf_token="+linksafe_csrf_token;
			}else if (this.br.containsHTML("Simple Captcha")) {
				postData += "captcha_driver=simplecaptcha";
                postData += "&do=captcha";
                
				final String captcha = br.getRegex("<img src=\"([^\"]*)\" alt=\"Simple Captcha\"").getMatch(0);
				if (captcha != null && captcha.contains("simplecaptcha")) {
					final String captchacode = this.getCaptchaCode(captcha, param);
					if (StringUtils.isEmpty(captchacode)) {
	                    if (i < 3) {
                        	continue;
                    	} else {
                        	throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    	}
                    }
		            postData += "&simplecaptcha="+captchacode;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
				postData += "&submit=confirm";
				final String linksafe_csrf_token = br.getRegex("<input type=\"hidden\" name=\"linksafe_csrf_token\" value=\"([^\"]*)\"").getMatch(0);
				postData += "&linksafe_csrf_token="+linksafe_csrf_token;
			}else if (this.br.containsHTML("circlecaptcha")) {
				postData += "captcha_driver=circlecaptcha";
                postData += "&do=captcha";
                
				final String captcha = br.getRegex("<input type=\"image\" style=\"cursor:crosshair;\" src=\"([^\"]*)\" alt=\"Circle Captcha\"").getMatch(0);
				if (captcha != null && captcha.contains("circlecaptcha")) {
					final File file = this.getLocalCaptchaFile();
                	getCaptchaBrowser(br).getDownload(file, captcha);
					final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, null, "Click on the open circle");
                	if (cp == null) {
                		if (i < 3) {
                        	continue;
                    	} else {
                    		throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    	}
                	}
		            postData += "&button.x="+cp.getX();
		            postData += "&button.y="+cp.getY();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
				final String linksafe_csrf_token = br.getRegex("<input type=\"hidden\" name=\"linksafe_csrf_token\" value=\"([^\"]*)\"").getMatch(0);
				postData += "&linksafe_csrf_token="+linksafe_csrf_token;
			} else if (this.br.containsHTML("solvemedia\\.com/papi/")) {
				if (getPluginConfig().getBooleanProperty(NO_SOLVEMEDIA, false) == false) {
					postData += "captcha_driver=solvemedia";
	                postData += "&do=captcha";
	            
					final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
	                File cf = null;
	                
	                try {
	                	cf = sm.downloadCaptcha(getLocalCaptchaFile());
	                } catch (final Exception e) {
	                	if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
	                		throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
	                	}
	               		throw e;
	                }
	                final String smcode = getCaptchaCode("solvemedia", cf, param);
	                if (StringUtils.isEmpty(code)) {
	                    if (i < 3) {
	                        continue;
	                    } else {
	                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
	                    }
	                }
	
	                final String chid = sm.getChallenge(smcode);
	                //postData += "&adcopy_response="+smcode;
	                postData += "&adcopy_response=manual_challenge";
	                postData += "&adcopy_challenge="+chid;
	                postData += "&submit=confirm";
					final String linksafe_csrf_token = br.getRegex("<input type=\"hidden\" name=\"linksafe_csrf_token\" value=\"([^\"]*)\"").getMatch(0);
					postData += "&linksafe_csrf_token="+linksafe_csrf_token;
				}else{
					if (i < 3) {
                        continue;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
				}
            }else{//no captcha possible?
            	throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not find captcha form");
            }
            this.br.postPage(this.br.getURL(), postData);
            if (br.containsHTML("Wrong captcha solution")) {
                this.br.getPage(parameter);
                continue;
            }
            validateLastChallengeResponse();
            failed = false;
            break;
        }
        if (failed) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        final String[] linkkeys = br.getRegex("href=\"javascript:void\\(0\\);\" onclick=\"popup\\('([^\"]*)'").getColumn(0);
        if (linkkeys == null || linkkeys.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        this.br.setFollowRedirects(false);
        for (final String linkkey : linkkeys) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            if (dupelist.contains(linkkey)) {
                continue;
            }
            dupelist.add(linkkey);
            this.br.getPage(linkkey);
            final String finallink = this.br.getRedirectLocation();
            if (finallink.matches(".+cript\\.to/bot")) {//only one click captcha? No captcha rotation?
            	for (int i = 0; i <= 3; i++) {
            		String postData = "";
            		this.br.setFollowRedirects(true);
	        		br.getPage(linkkey);
	        		this.br.setFollowRedirects(false);
		        	
            		if (this.br.containsHTML("circlecaptcha")) {
						final String captcha = br.getRegex("<input type=\"image\" style=\"cursor:crosshair;\" src=\"([^\"]*)\" alt=\"Circle Captcha\"").getMatch(0);
						if (captcha != null && captcha.contains("circlecaptcha")) {
							final File file = this.getLocalCaptchaFile();
		                	getCaptchaBrowser(br).getDownload(file, captcha);
							final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, null, "Click on the open circle or single color circle");
		                	if (cp == null) {
		                		if (i < 3) {
		                        	continue;
		                    	} else {
		                    		throw new PluginException(LinkStatus.ERROR_CAPTCHA);
		                    	}
		                	}
				            postData += "button.x="+cp.getX();
				            postData += "&button.y="+cp.getY();
		                } else {
		                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
		                }
						final String linksafe_csrf_token = br.getRegex("<input type=\"hidden\" name=\"linksafe_csrf_token\" value=\"([^\"]*)\"").getMatch(0);
						postData += "&linksafe_csrf_token="+linksafe_csrf_token;
						this.br.postPage(finallink, postData);
						final String finallink2 = this.br.getRedirectLocation();
						if (finallink2 == null || finallink2.matches(".+cript\\.to/.+")) {
                			continue;
            			}
            			validateLastChallengeResponse();
            			decryptedLinks.add(createDownloadlink(finallink2));
            			break;
					}else{
						logger.warning("Unknown captcha: " + parameter);
					}
                }
            }else{
            	if (finallink == null || finallink.matches(".+cript\\.to/.+")) {
                	continue;
            	}
            	decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        return decryptedLinks;
    }
    
    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NO_SOLVEMEDIA, JDL.L("plugins.decrypter.cryptto.nosolvemedia", "No solvemedia?")).setDefaultValue(true));//It's true because solvemedia was always wrong with the code in this plugin in my tests
	}
}