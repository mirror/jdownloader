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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

import org.appwork.utils.Application;

//Similar to SafeUrlMe (safeurl.me) and SflnkgNt (safelinking.net)
//NOTE: Nearly the complete code is copied from SflnkgNt and works just fine!
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xsharez.com" }, urls = { "https?://(www\\.)?xsharez\\.com/(p|d)/[a-z0-9]+" }, flags = { 0 })
public class XSharezCom extends PluginForDecrypt {

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

    private String AGENT = null;
    private String cType = "notDetected";

    public XSharezCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> cryptedLinks = new ArrayList<String>();
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] { 500 });
        } catch (Throwable e) {
        }
        if (AGENT == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            AGENT = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", AGENT);
        br.setFollowRedirects(false);

        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("(\"This link does not exist\\.\"|ERROR \\- this link does not exist|>404 Page/File not found<)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">Not yet checked</span>")) { throw new DecrypterException("Not yet checked"); }
        if (br.containsHTML("To use reCAPTCHA you must get an API key from")) { throw new DecrypterException("Server error, please contact the xsharez.com support!"); }
        /* unprotected links */
        if (parameter.matches("https?://(www\\.)??xsharez\\.com/d/[a-z0-9]+")) {
            br.getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("location=\"(https?[^\"]+)").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("XSharez: Sever issues? continuing...");
                logger.warning("XSharez: Please confirm via browser, and report any bugs to JDownloader Developement Team. :" + parameter);
            }
            if (!parameter.equals(finallink)) { // prevent loop
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            /* protected links */
            String protocol = parameter.startsWith("https:") ? "https:" : "http:";
            HashMap<String, String> captchaRegex = new HashMap<String, String>();
            captchaRegex.put("recaptcha", "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)");
            captchaRegex.put("basic", "(https?://xsharez\\.com/includes/captcha_factory/securimage/securimage_(show\\.php\\?hash=[a-z0-9]+|register\\.php\\?hash=[^\"]+sid=[a-z0-9]{32}))");
            captchaRegex.put("threeD", "\"(https?://xsharez\\.com/includes/captcha_factory/3dcaptcha/3DCaptcha\\.php)\"");
            captchaRegex.put("fancy", "class=\"captcha_image ajax\\-fc\\-container\"");
            captchaRegex.put("qaptcha", "class=\"protected\\-captcha\"><div id=\"QapTcha\"");
            captchaRegex.put("solvemedia", "api(\\-secure)?\\.solvemedia\\.com/(papi)?");

            /* search for protected form */
            Form protectedForm = br.getFormbyProperty("id", "protected-form");
            if (protectedForm != null) {
                boolean password = protectedForm.getRegex("type=\"password\" name=\"link-password\"").matches(); // password?
                String captcha = br.getRegex("<div id=\"captcha\\-wrapper\">(.*?)</div></div></div>").getMatch(0); // captcha?
                if (captcha != null) prepareCaptchaAdress(captcha, captchaRegex, protocol);

                for (int i = 0; i <= 5; i++) {
                    String data = "post-protect=1";
                    if (password) data += "&link-password=" + getUserInput(null, param);

                    Browser captchaBr = null;
                    if (!"notDetected".equals(cType)) captchaBr = br.cloneBrowser();

                    switch (CaptchaTyp.valueOf(cType)) {
                    case recaptcha:
                        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.parse();
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        data += "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_challenge_field=" + getCaptchaCode(cf, param);
                        break;
                    case basic:
                        data += "&securimage_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("basic")).getMatch(0), param);
                        break;
                    case threeD:
                        data += "&3dcaptcha_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("threeD")).getMatch(0), param);
                        break;
                    case fancy:
                        captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        captchaBr.getPage("https://xsharez.com/includes/captcha_factory/fancycaptcha.php?hash=" + new Regex(parameter, "/p/(.+)").getMatch(0));
                        data += "&fancy-captcha=" + captchaBr.toString().trim();
                        break;
                    case qaptcha:
                        captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        captchaBr.postPage("https://xsharez.com/includes/captcha_factory/Qaptcha.jquery.php?hash=" + new Regex(parameter, "/p/(.+)").getMatch(0), "action=qaptcha");
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

                    if (captchaRegex.containsKey(cType) || data.contains("link-password")) {
                        br.postPage(parameter, data);
                        if (br.getHttpConnection().getResponseCode() == 500) {
                            logger.warning("XSharez: 500 Internal Server Error. Link: " + parameter);
                            continue;
                        }
                        password = br.getRegex("type=\"password\" name=\"link-password\"").matches(); // password correct?
                    }

                    if (!"notDetected".equals(cType) && br.containsHTML(captchaRegex.get(cType)) || password || br.containsHTML("<strong>Prove you are human</strong>")) {
                        prepareCaptchaAdress(captcha, captchaRegex, protocol);
                        continue;
                    }
                    break;
                }
                if (!"notDetected".equals(cType) && (br.containsHTML(captchaRegex.get(cType)) || br.containsHTML("<strong>Prove you are human</strong>"))) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                if (password) { throw new DecrypterException(DecrypterException.PASSWORD); }
            }
            if (br.containsHTML(">All links are dead\\.<|>Links dead<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }

            /* Container handling (if no containers found, use webprotection) */
            if (br.containsHTML("\\.dlc")) {
                decryptedLinks = loadcontainer(".dlc", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
            }

            if (br.containsHTML("\\.rsdf")) {
                decryptedLinks = loadcontainer(".rsdf", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
            }

            if (br.containsHTML("\\.ccf")) {
                decryptedLinks = loadcontainer(".ccf", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
            }

            /* Webprotection decryption */
            if (br.getRedirectLocation() != null && br.getRedirectLocation().equals(parameter)) br.getPage(parameter);

            for (String[] s : br.getRegex("<div class=\"links\\-container result\\-form\">(.*?)</div>").getMatches()) {
                for (String[] ss : new Regex(s[0], "<a href=\"(.*?)\"").getMatches()) {
                    for (String sss : ss) {
                        if (parameter.equals(sss)) continue;
                        cryptedLinks.add(sss);
                    }
                }
            }

            for (String link : cryptedLinks) {
                if (link.matches(".*xsharez\\.com/d/.+")) {
                    br.getPage(link);
                    link = br.getRedirectLocation();
                    link = link == null ? br.getRegex("location=\"(http[^\"]+)").getMatch(0) : link;
                    if (link == null) {
                        logger.warning("XSharez: Sever issues? continuing...");
                        logger.warning("XSharez: Please confirm via browser, and report any bugs to developement team. :" + parameter);
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
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer(String format, CryptedLink param) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String containerLink = br.getRegex("\"(https?://xsharez\\.com/c/[a-z0-9]+" + format + ")").getMatch(0);
        if (containerLink == null) {
            logger.warning("Contailerlink for link " + param.toString() + " for format " + format + " could not be found.");
            return new ArrayList<DownloadLink>();
        }
        String test = Encoding.htmlDecode(containerLink);
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(test);
            if (con.getResponseCode() == 200) {
                try {
                    /* does not exist in 09581 */
                    file = Application.getResource("tmp/xsharezcom/" + test.replaceAll("(:|/|\\?)", "") + format);
                } catch (Throwable e) {
                    file = JDUtilities.getResourceFile("tmp/xsharezcom/" + test.replaceAll("(:|/|\\?)", "") + format);
                }
                if (file == null) return new ArrayList<DownloadLink>();
                file.deleteOnExit();
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                    if (decryptedLinks.size() > 0) return decryptedLinks;
                } else {
                    return new ArrayList<DownloadLink>();
                }
            }
            return new ArrayList<DownloadLink>();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private void prepareCaptchaAdress(String captcha, HashMap<String, String> captchaRegex, String protocol) {
        br.getRequest().setHtmlCode(captcha);

        for (Entry<String, String> next : captchaRegex.entrySet()) {
            if (br.containsHTML(next.getValue())) {
                cType = next.getKey();
            }
        }

        logger.info("notDetected".equals(cType) ? "Captcha not detected." : "Detected captcha type \"" + cType + "\" for this host.");

        /* detect javascript */
        String javaScript = null;
        for (String js : br.getRegex("<script type=\"text/javascript\">(.*?)</script>").getColumn(0)) {
            if (!new Regex(js, captchaRegex.get(cType)).matches()) continue;
            javaScript = js;
        }
        if (javaScript == null) return;

        /* execute javascript */
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            /* creating pseudo functions: document.location.protocol + document.write(value) */
            engine.eval("var document = { loc : function() { var newObj = new Object(); function protocol() { return \"" + protocol + "\"; } newObj.protocol = protocol(); return newObj; }, write : function(a) { return a; }}");
            engine.eval("document.location = document.loc();");
            result = engine.eval(javaScript);
        } catch (Throwable e) {
            return;
        }
        String res = result != null ? result.toString() : null;
        br.getRequest().setHtmlCode(res);
    }

}