//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

//Similar to SflnkgNt (safelinking.net)
//They only have fancycaptcha and reCaptcha, the other stuff is just here in case they add more captchatypes
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "safeurl.me" }, urls = { "https?://(www\\.)?safeurl\\.me/(p|d)/[a-z0-9]+" }, flags = { 0 })
public class SafeUrlMe extends PluginForDecrypt {

    private enum CaptchaTyp {
        solvemedia,
        recaptcha,
        basic,
        threeD,
        fancy,
        qaptcha,
        simple,
        dotty,
        cool,
        standard,
        cats,
        notDetected;
    }

    private String PASSWORDPROTECTEDTEXT = "(input type=\"password\" (class=\"\" )?(id=\"input\\-password\" )?name=\"password\"|\\{\"status\":\"error\",\"msg\":\"The password entered is not valid\\.\"\\}|\"msg\":\"Password not entered)";

    public SafeUrlMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> cryptedLinks = new ArrayList<String>();
        String parameter = param.toString();
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] { 500 });
        } catch (Throwable e) {
        }

        br.setFollowRedirects(false);
        if (parameter.matches("http://(www\\.)?safeurl\\.me/d/[a-z0-9]+")) {
            br.getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("location=\"(https?[^\"]+)").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("Safeurl: Sever issues? continuing...");
                logger.warning("Safeurl: Please confirm via browser, and report any bugs to JDownloader Developement Team. :" + parameter);
            }
            /* prevent loop */
            if (!parameter.equals(finallink)) {
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            br.getPage(parameter);
            if (br.containsHTML("(\"This link does not exist\\.\"|ERROR \\- this link does not exist)")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
            if (br.containsHTML(">Not yet checked</span>")) { throw new DecrypterException("Not yet checked"); }

            /* password */
            if (br.containsHTML(PASSWORDPROTECTEDTEXT)) {
                for (int j = 0; j <= 5; j++) {
                    String password = "password=" + getUserInput(null, param);
                    br.postPage(parameter + "/unlock", password);
                    if (br.containsHTML(PASSWORDPROTECTEDTEXT)) continue;
                    break;
                }
                if (br.containsHTML(PASSWORDPROTECTEDTEXT)) { throw new DecrypterException(DecrypterException.PASSWORD); }
                br.getPage(parameter);
            }

            String cType = "notDetected";
            HashMap<String, String> captchaRegex = new HashMap<String, String>();
            captchaRegex.put("recaptcha", "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)");
            captchaRegex.put("basic", "(https?://safeurl\\.me/includes/captcha_factory/securimage/securimage_(show\\.php\\?hash=[a-z0-9]+|register\\.php\\?hash=[^\"]+sid=[a-z0-9]{32}))");
            captchaRegex.put("threeD", "\"(https?://safeurl\\.me/includes/captcha_factory/3dcaptcha/3DCaptcha\\.php)\"");
            captchaRegex.put("fancy", "(class=\"captcha_image ajax\\-fc\\-container\"|src=\"http://safeurl\\.me/assets/js/jquery\\.fancycaptcha.js\"></script>)");
            captchaRegex.put("qaptcha", "class=\"protected\\-captcha\"><div id=\"QapTcha\"");
            captchaRegex.put("solvemedia", "api(\\-secure)?\\.solvemedia\\.com/papi");

            for (Entry<String, String> next : captchaRegex.entrySet()) {
                if (br.containsHTML(next.getValue())) {
                    cType = next.getKey();
                }
            }

            logger.info("notDetected".equals(cType) ? "Captcha not detected." : "Detected captcha type \"" + cType + "\" for this host.");

            for (int i = 0; i <= 5; i++) {
                String data = "captchaVerify=1";

                Browser captchaBr = null;
                if (!"notDetected".equals(cType)) captchaBr = br.cloneBrowser();

                switch (CaptchaTyp.valueOf(cType)) {
                case recaptcha:
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    data += "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + getCaptchaCode(cf, param).replace(" ", "+");
                    break;
                case basic:
                    data += "&securimage_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("basic")).getMatch(0), param);
                    break;
                case threeD:
                    data += "&3dcaptcha_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("threeD")).getMatch(0), param);
                    break;
                case fancy:
                    captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    captchaBr.postPage(parameter, "captchaVerify=1&fancycaptcha=true");
                    data += "&captcha=" + captchaBr.toString().trim();
                    break;
                case qaptcha:
                    captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    captchaBr.postPage("https://safeurl.me/includes/captcha_factory/Qaptcha.jquery.php?hash=" + new Regex(parameter, "safeurl\\.me/p/(.+)").getMatch(0), "action=qaptcha");
                    if (!captchaBr.containsHTML("\"error\":false")) {
                        logger.warning("Decrypter broken for link: " + parameter + "\n");
                        logger.warning("Qaptcha handling broken");
                        return null;
                    }
                    data += "&iQapTcha=";
                    break;
                case simple:
                    break;
                case dotty:
                    break;
                case cool:
                    break;
                case standard:
                    break;
                case cats:
                    break;
                case solvemedia:
                    PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((LnkCrptWs) solveplug).getSolveMedia(br);
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    String code = getCaptchaCode(cf, param);
                    String chid = sm.getChallenge(code);
                    data += "&solvemedia_response=" + code.replace(" ", "+") + "&adcopy_challenge=" + chid + "&adcopy_response=" + code.replace(" ", "+");
                    break;
                }

                if (captchaRegex.containsKey(cType)) {
                    br.postPage(parameter, data);
                    if (br.getHttpConnection().getResponseCode() == 500) logger.warning("Safeurl: 500 Internal Server Error. Link: " + parameter);
                }

                if (!"notDetected".equals(cType) && br.containsHTML(captchaRegex.get(cType))) {
                    continue;
                }
                break;
            }
            if (!"notDetected".equals(cType) && br.containsHTML(captchaRegex.get(cType)) || br.containsHTML("<strong>Prove you are human</strong>")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
            if (br.containsHTML(">All links are dead\\.<|>Links dead<")) {
                logger.info("All links are offline for link: " + parameter);
                return decryptedLinks;
            }

            /* Webprotection decryption */
            for (String s : br.getRegex("<textarea class=\"links\\-plain\\-text\"(.*?)</textarea>").getColumn(0)) {
                for (String[] ss : new Regex(s, "(http.*?)[\r\n]+").getMatches()) {
                    for (String sss : ss) {
                        if (parameter.equals(sss)) continue;
                        cryptedLinks.add(sss);
                    }
                }
            }

            progress.setRange(cryptedLinks.size());

            for (String link : cryptedLinks) {
                if (link.matches(".*safeurl\\.me/d/.+")) {
                    br.getPage(link);
                    link = br.getRedirectLocation();
                    link = link == null ? br.getRegex("location=\"(http[^\"]+)").getMatch(0) : link;
                    if (link == null) {
                        logger.warning("Safeurl: Sever issues? continuing...");
                        logger.warning("Safeurl: Please confirm via browser, and report any bugs to developement team. :" + parameter);
                        continue;
                    }
                }
                DownloadLink dl = createDownloadlink(link);
                try {
                    distribute(dl);
                } catch (Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dl);
                progress.increase(1);
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}