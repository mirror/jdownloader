//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkprotect.in" }, urls = { "http://[\\w\\.]*?linkprotect\\.in/index.php\\?site=folder&id=[\\w]{1,50}" }, flags = { 0 })
public class LnkPrtctn extends PluginForDecrypt {

    static private final Pattern patternName          = Pattern.compile("Ordnername: <b>(.*?)</b>");
    static private final Pattern patternPassword      = Pattern.compile("<input type=\"text\" name=\"pw\" class=\"[a-zA-Z0-9]{1,50}\" size=\"[0-9]{1,3}\" />");
    static private final Pattern patternPasswordWrong = Pattern.compile("<b>Passwort falsch!</b>");
    static private final Pattern patternCaptcha       = Pattern.compile("<img src=\"(.*?securimage_show.*?)\"");
    static private final Pattern patternDownload      = Pattern.compile("http://[\\w\\.]*?linkprotect\\.in/includes/dl.php\\?id=[a-zA-Z0-9]{1,50}");

    public LnkPrtctn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

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

                String captchaAddress = "http://linkprotect.in/" + matcher.group(1);

                File captchaFile = this.getLocalCaptchaFile();
                Browser br2 = new Browser();
                Browser.download(captchaFile, br2.openGetConnection(captchaAddress));

                br.setCookie(br.getURL(), "PHPSESSID", br2.getCookie(br2.getURL(), "PHPSESSID"));
                String captchaCode = getCaptchaCode(captchaFile, param);
                captchaCode = captchaCode.toUpperCase();
                form.put("code", captchaCode);

                /*
                 * Herausfinden ob ein Passwort benötigt wird und ggf. abfragen
                 */
                matcher = patternPassword.matcher(source);
                if (matcher.find()) {
                    password = getUserInput(null, param);
                    form.put("pw", password);
                }

                br.setFollowRedirects(true);
                br.submitForm(form);
            } else if (matcherpw.find()) {
                /*
                 * Herausfinden ob ein Passwort benötigt wird und ggf. abfragen
                 * (Falls nur ein PW ohne Captcha Abfrage!)
                 */
                form = br.getForm(0);
                password = getUserInput(null, param);

                form.put("pw", password);
                br.setFollowRedirects(true);
                br.submitForm(form);
            } else if (matcherpwwrong.find()) {
                password = getUserInput(null, param);
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
            String[] links = jd.parser.html.HTMLParser.getHttpLinks(br + "", "linkprotect.in");
            FilePackage fp = FilePackage.getInstance();
            matcher = patternName.matcher(br + "");
            if (matcher.find()) fp.setName(new Regex(br + "", patternName.pattern()).getMatch(0));
            br.setFollowRedirects(false);

            for (int i = 0; i <= links.length - 1; i++) {
                matcher = patternDownload.matcher(links[i]);
                if (matcher.find()) {
                    /* EinzelLink gefunden */
                    String link = matcher.group(0);
                    br.getPage(link);
                    String finalLink = br.getRedirectLocation();
                    DownloadLink dlLink = createDownloadlink(finalLink);
                    fp.add(dlLink);
                    decryptedLinks.add(dlLink);
                }
            }
        }

        return decryptedLinks;
    }

    // @Override

}
