//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.utils.JDUtilities;

//Similar to SafeUrlMe (safeurl.me) and XSharezCom (xsharez.com)
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "safelinking.net" }, urls = { "https?://(www\\.)?(safelinking\\.net/(p|d)/[a-z0-9]+|sflk\\.in/[A-Za-z0-9]+)" }, flags = { 0 })
public class SflnkgNt extends PluginForDecrypt {

    public SflnkgNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final GeneralSafelinkingHandling gsh = new GeneralSafelinkingHandling(this.br, param, this.getHost());
        gsh.enableHTTPS();
        gsh.startUp();
        String parameter = param.toString();
        if (parameter.matches("https?://(www\\.)?sflk\\.in/[A-Za-z0-9]+")) {
            br.getPage(gsh.getAddedLink());
            String newparameter = br.getRedirectLocation();
            if (newparameter == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (!newparameter.matches("https?://(www\\.)?safelinking\\.net/(p|d)/[a-z0-9]+")) {
                logger.warning("Decrypter broken for link (received invalid redirect link): " + parameter);
                return null;
            }
            newparameter = newparameter.replaceAll("http://", "https://");
            gsh.setAddedLink(newparameter);
            br.getPage(gsh.getAddedLink());
        }
        br.setFollowRedirects(false);
        br.getPage(gsh.getAddedLink());
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("safelinking.net/404")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        if (gsh.getAddedLink().contains("/d/")) {
            gsh.decryptSingleLink();
        } else {
            gsh.handleCaptcha(gsh.getAddedLink(), param);
            gsh.decryptMultipleLinks(gsh.getAddedLink(), param);
        }
        decryptedLinks = gsh.getDecryptedLinks();
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    public class GeneralSafelinkingHandling {
        /**
         * A class to handle sites similar to safelinking.net ->Google "Secure your links with a captcha, a password and much more" to find such sites
         */

        public GeneralSafelinkingHandling(final Browser br, final CryptedLink param, final String host) {
            this.br = br;
            this.HOST = host;
            this.addedLink = param.toString();
            this.cryptedLink = param;
        }

        Browser                         br             = new Browser();
        private String                  HOST           = null;
        private String                  AGENT          = null;
        private String                  cType          = "notDetected";
        private String                  addedLink      = null;
        private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        private CryptedLink             cryptedLink    = null;
        private boolean                 HTTPS          = false;
        private String                  PROTOCOL       = "http://";

        public ArrayList<DownloadLink> handleSafelinkingAuto(final CryptedLink param) throws Exception {
            // Prepare browser and correct parameter
            startUp();
            decrypt();
            return decryptedLinks;
        }

        public void startUp() {
            try {
                /* not available in old versions (before jd2) */
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
            if (HTTPS) {
                this.addedLink = this.addedLink.replaceAll("http://", "https://");
                PROTOCOL = "https://";
            } else {
                this.addedLink = this.addedLink.replaceAll("https://", "http://");
                PROTOCOL = "http://";
            }
        }

        public void decrypt() throws Exception {
            br.getPage(this.addedLink);
            handleErrors();
            /* unprotected links */
            if (this.addedLink.contains("/d/")) {
                decryptSingleLink();
            } else {
                handleCaptcha(this.addedLink, this.cryptedLink);
                decryptMultipleLinks(this.addedLink, this.cryptedLink);
            }
        }

        public String getAddedLink() {
            return this.addedLink;
        }

        public void setAddedLink(final String newAddedLink) {
            this.addedLink = newAddedLink;
        }

        public void enableHTTPS() {
            this.HTTPS = true;
        }

        public void disableHTTPS() {
            this.HTTPS = false;
        }

        public ArrayList<DownloadLink> getDecryptedLinks() {
            return this.decryptedLinks;
        }

        public int getCaptchaTypeNumber() {
            if (cType.equals("solvemedia")) {
                return 1;
            } else if (cType.equals("recaptcha")) {
                return 2;
            } else if (cType.equals("basic")) {
                return 3;
            } else if (cType.equals("threeD")) {
                return 4;
            } else if (cType.equals("fancy")) {
                return 5;
            } else if (cType.equals("qaptcha")) {
                return 6;
            } else if (cType.equals("simple")) {
                return 7;
            } else if (cType.equals("dotty")) {
                return 8;
            } else if (cType.equals("cool")) {
                return 9;
            } else if (cType.equals("standard")) {
                return 10;
            } else if (cType.equals("cats")) { return 11; }
            // Not detected or other case
            return 0;
        }

        private void handleCaptcha(final String parameter, final CryptedLink param) throws Exception {
            HashMap<String, String> captchaRegex = new HashMap<String, String>();
            captchaRegex.put("recaptcha", "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)");
            captchaRegex.put("basic", "(https?://" + HOST + "/includes/captcha_factory/securimage/securimage_(show\\.php\\?hash=[a-z0-9]+|register\\.php\\?hash=[^\"]+sid=[a-z0-9]{32}))");
            captchaRegex.put("threeD", "\"(https?://" + HOST + "/includes/captcha_factory/3dcaptcha/3DCaptcha\\.php)\"");
            captchaRegex.put("fancy", "class=\"captcha_image ajax\\-fc\\-container\"");
            captchaRegex.put("qaptcha", "class=\"protected\\-captcha\"><div id=\"QapTcha\"");
            captchaRegex.put("solvemedia", "api(\\-secure)?\\.solvemedia\\.com/(papi)?");

            /* search for protected form */
            Form protectedForm = br.getFormbyProperty("id", "protected-form");
            if (protectedForm != null) {
                boolean password = protectedForm.getRegex("type=\"password\" name=\"link-password\"").matches(); // password?
                String captcha = br.getRegex("<div id=\"captcha\\-wrapper\">(.*?)</div></div></div>").getMatch(0); // captcha?
                if (captcha != null) prepareCaptchaAdress(captcha, captchaRegex);

                for (int i = 0; i <= 5; i++) {
                    String data = "post-protect=1";
                    if (password) data += "&link-password=" + getUserInput(null, param);

                    Browser captchaBr = null;
                    if (!"notDetected".equals(cType)) captchaBr = br.cloneBrowser();

                    switch (getCaptchaTypeNumber()) {
                    case 1:
                        PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                        jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                        File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        String code = getCaptchaCode(cf, param);
                        String chid = sm.getChallenge(code);
                        data += "&solvemedia_response=" + code.replace(" ", "+") + "&adcopy_challenge=" + chid + "&adcopy_response=" + code.replace(" ", "+");
                        break;
                    case 2:
                        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                        rc.parse();
                        rc.load();
                        File cfRe = rc.downloadCaptcha(getLocalCaptchaFile());
                        data += "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + getCaptchaCode(cfRe, param);
                        break;
                    case 3:
                        data += "&securimage_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("basic")).getMatch(0), param);
                        break;
                    case 4:
                        data += "&3dcaptcha_response_field=" + getCaptchaCode(br.getRegex(captchaRegex.get("threeD")).getMatch(0), param);
                        break;
                    case 5:
                        captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        captchaBr.getPage(PROTOCOL + HOST + "/includes/captcha_factory/fancycaptcha.php?hash=" + new Regex(parameter, "/p/(.+)").getMatch(0));
                        data += "&fancy-captcha=" + captchaBr.toString().trim();
                        break;
                    case 6:
                        captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        captchaBr.postPage(PROTOCOL + HOST + "/includes/captcha_factory/Qaptcha.jquery.php?hash=" + new Regex(parameter, "/p/(.+)").getMatch(0), "action=qaptcha");
                        if (!captchaBr.containsHTML("\"error\":false")) {
                            logger.warning("Decrypter broken for link: " + parameter + "\n");
                            logger.warning("Qaptcha handling broken");
                            if (password) { throw new DecrypterException("Decrypter for " + HOST + " is broken"); }
                        }
                        data += "&iQapTcha=";
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    case 9:
                        break;
                    case 10:
                        break;
                    case 11:
                        break;
                    }

                    if (captchaRegex.containsKey(cType) || data.contains("link-password")) {
                        br.postPage(parameter, data);
                        if (br.getHttpConnection().getResponseCode() == 500) {
                            logger.warning(HOST + ": 500 Internal Server Error. Link: " + parameter);
                            continue;
                        }
                        password = br.getRegex("type=\"password\" name=\"link-password\"").matches(); // password
                                                                                                      // correct?
                    }

                    if (!"notDetected".equals(cType) && br.containsHTML(captchaRegex.get(cType)) || password || br.containsHTML("<strong>Prove you are human</strong>")) {
                        prepareCaptchaAdress(captcha, captchaRegex);
                        continue;
                    }
                    break;
                }
                if (!"notDetected".equals(cType) && (br.containsHTML(captchaRegex.get(cType)) || br.containsHTML("<strong>Prove you are human</strong>"))) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                if (password) { throw new DecrypterException(DecrypterException.PASSWORD); }
            }
        }

        private void prepareCaptchaAdress(String captcha, HashMap<String, String> captchaRegex) {
            br.getRequest().setHtmlCode(captcha);

            for (Entry<String, String> next : captchaRegex.entrySet()) {
                if (br.containsHTML(next.getValue())) {
                    cType = next.getKey();
                }
            }

            logger.info("notDetected".equals(cType) ? "Captcha not detected." : "Detected captcha type \"" + cType + "\" for this " + HOST + ".");

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
                /*
                 * creating pseudo functions: document.location.protocol + document.write(value)
                 */
                engine.eval("var document = { loc : function() { var newObj = new Object(); function protocol() { return \"" + PROTOCOL + "\"; } newObj.protocol = protocol(); return newObj; }, write : function(a) { return a; }}");
                engine.eval("document.location = document.loc();");
                result = engine.eval(javaScript);
            } catch (Throwable e) {
                return;
            }
            String res = result != null ? result.toString() : null;
            br.getRequest().setHtmlCode(res);
        }

        private ArrayList<DownloadLink> loadcontainer(String format, CryptedLink param) throws IOException, PluginException {
            Browser brc = br.cloneBrowser();
            String containerLink = br.getRegex("\"(https?://" + this.HOST + "/c/[a-z0-9]+" + format + ")").getMatch(0);
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
                        file = org.appwork.utils.Application.getResource("tmp/generalsafelinking/" + test.replaceAll("(:|/|\\?)", "") + format);
                    } catch (Throwable e) {
                        file = JDUtilities.getResourceFile("tmp/generalsafelinking/" + test.replaceAll("(:|/|\\?)", "") + format);
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

        public ArrayList<DownloadLink> decryptMultipleLinks(final String parameter, final CryptedLink param) throws IOException, PluginException {
            ArrayList<String> cryptedLinks = new ArrayList<String>();
            // TODO: Add handling for offline links/containers

            /* Container handling (if no containers found, use webprotection) */
            if (br.containsHTML("\\.dlc")) {
                this.decryptedLinks = loadcontainer(".dlc", param);
                if (this.decryptedLinks != null && this.decryptedLinks.size() > 0) return this.decryptedLinks;
            }

            if (br.containsHTML("\\.rsdf")) {
                decryptedLinks = loadcontainer(".rsdf", param);
                if (this.decryptedLinks != null && this.decryptedLinks.size() > 0) return this.decryptedLinks;
            }

            if (br.containsHTML("\\.ccf")) {
                this.decryptedLinks = loadcontainer(".ccf", param);
                if (this.decryptedLinks != null && this.decryptedLinks.size() > 0) return this.decryptedLinks;
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

            for (final String link : cryptedLinks) {
                DownloadLink dl = null;
                if (link.matches(".*" + HOST + "/d/.+")) {
                    br.getPage(link);
                    dl = decryptSingleLink();
                    if (dl == null) {
                        continue;
                    }
                } else {
                    dl = createDownloadlink(link);
                    try {
                        distribute(dl);
                    } catch (Throwable e) {
                        /* does not exist in 09581 */
                    }
                }
                this.decryptedLinks.add(dl);
            }
            if (this.decryptedLinks == null || this.decryptedLinks.size() == 0) {
                logger.warning("Decrypter out of date for link: " + parameter);
                return null;
            }
            return decryptedLinks;
        }

        public DownloadLink decryptSingleLink() {
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("location=\"(https?[^\"]+)").getMatch(0);
            }
            if (finallink == null) {
                logger.warning(HOST + ": Sever issues? continuing...");
                logger.warning(HOST + ": Please confirm via browser, and report any bugs to JDownloader Developement Team. :" + this.addedLink);
            }
            if (!this.addedLink.equals(finallink)) { // prevent loop
                final DownloadLink dl = createDownloadlink(finallink);
                this.decryptedLinks.add(dl);
                return dl;
            } else {
                // Shouldn't happen
                return null;
            }
        }

        private void handleErrors() throws DecrypterException {
            if (br.containsHTML(">404 Page/File not found<|>The page/file you\\'ve requested has been moved")) {
                logger.info("Link offline: " + this.getAddedLink());
                throw new DecrypterException("offline");
            }
        }
        /** End of class inside class */
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}