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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class LinkProtectIn extends PluginForDecrypt {

    static private final String host = "linkprotect.in";
    
    static private final Pattern patternName = Pattern.compile("Ordnername: <b>(.*?)</b>");
    static private final Pattern patternPassword = Pattern.compile("<input type=\"text\" name=\"pw\" class=\"[a-zA-Z0-9]{1,50}\" size=\"[0-9]{1,3}\" />");
    static private final Pattern patternPasswordWrong = Pattern.compile("<b>Passwort falsch!</b>");
    static private final Pattern patternCaptcha = Pattern.compile("<img src=\"(.*?securimage_show.*?)\"");
    static private final Pattern patternDownload = Pattern.compile("http://[\\w\\.]*?linkprotect\\.in/includes/dl.php\\?id=[a-zA-Z0-9]{1,50}");
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linkprotect\\.in/index.php\\?site=folder&id=[a-zA-Z0-9]{1,50}", Pattern.CASE_INSENSITIVE);

    public LinkProtectIn() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        
        try {

            boolean lp_continue = false;
            Matcher matcher;
            Matcher matcherpw;
            Matcher matcherpwwrong;
            
            Form form = new Form();
            String password = "";
            
            /* zuerst mal den evtl captcha abarbeiten */
            br.getPage(parameter);
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                matcher = patternCaptcha.matcher(br + "");
                matcherpw = patternPassword.matcher(br + "");
                matcherpwwrong = patternPasswordWrong.matcher(br + "");
                if (matcher.find()) {
                    String source = br.toString();
                    form = br.getForm(0);

                    String captchaAddress = "http://" + getHost() + "/" + matcher.group(1);
                    
                    /* Ein try Block weil sonst ein Error ausgelöst wird, wenn ein LinkProtectIn Link beim Start von JD in der Zwischenablage exisitiert */
                    try { 
                        File captchaFile = this.getLocalCaptchaFile(this);
                        Browser br2 = new Browser();
                        if (!Browser.download(captchaFile, br2.openGetConnection(captchaAddress)) || !captchaFile.exists()) {
                            /* Fehler beim Captcha */
                            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                            return decryptedLinks;
                        }
                        
                        br.setCookie(br.getURL(), "PHPSESSID", br2.getCookie(br2.getURL(), "PHPSESSID"));
                        String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                        if (captchaCode == null) {
                            /* abbruch geklickt */
                            return decryptedLinks;
                        }
                        captchaCode = captchaCode.toUpperCase();
                        form.put("code", captchaCode);
                    
                        /* Herausfinden ob ein Passwort benötigt wird und ggf. abfragen */
                        matcher = patternPassword.matcher(source);
                        if(matcher.find()) {
                            password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                            form.put("pw", password);
                        }
                    } catch (Exception e) { }
                    
                    br.setFollowRedirects(true);
                    br.submitForm(form);
                } else if(matcherpw.find()) {
                    /* Herausfinden ob ein Passwort benötigt wird und ggf. abfragen (Falls nur ein PW ohne Captcha Abfrage!) */
                    String source = br.toString();
                    form = br.getForm(0);
                    password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein!");
                    if(password == null) return decryptedLinks;
                    
                    form.put("pw", password);
                    br.setFollowRedirects(true);
                    br.submitForm(form);
                } else if(matcherpwwrong.find()) {
                    password = JDUtilities.getController().getUiInterface().showUserInputDialog("Das eingegebene Passwort [" + password + "] ist falsch. Bitte versuche es erneut!");
                    if(password == null) return decryptedLinks;
                    
                    form.put("pw", password);
                    br.setFollowRedirects(true);
                    br.submitForm(form);
                } else {
                    lp_continue = true;
                    break;
                }
            }

            if (lp_continue == true) {
                /* Links extrahieren */
                String[] links = jd.parser.HTMLParser.getHttpLinks(br + "", host);
                FilePackage fp = new FilePackage();
                matcher = patternName.matcher(br + "");
                if(matcher.find()) fp.setName(new Regex(br + "", patternName.pattern()).getMatch(0));
                br.setFollowRedirects(false);
                
                for (int i=0; i <= links.length - 1; i++) {
                    matcher = patternDownload.matcher(links[i]);
                    if (matcher.find()) {
                        /* EinzelLink gefunden */
                        String link = matcher.group(0);
                        br.getPage(link);
                        String finalLink = br.getRedirectLocation();
                        DownloadLink dlLink = createDownloadlink(finalLink);
                        dlLink.setFilePackage(fp);
                        decryptedLinks.add(dlLink);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return decryptedLinks;
        }
        return decryptedLinks;
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
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2542 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
