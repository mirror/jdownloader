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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

//Similar to SafeUrlMe (safeurl.me)
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "safelinking.net" }, urls = { "https?://(www\\.)?safelinking\\.net/(p|d)/[a-z0-9]+" }, flags = { 0 })
public class SflnkgNt extends PluginForDecrypt {

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

    private static String PASSWORDPROTECTEDTEXT = "type=\"password\" name=\"link-password\"";

    public static String readInputStreamToString(InputStream fis) throws UnsupportedEncodingException, IOException {
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(fis, "UTF8"));
            String line;
            StringBuilder ret = new StringBuilder();
            String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }
                ret.append(line);
            }
            return ret.toString();
        } finally {
            try {
                f.close();
            } catch (Throwable e) {
            }

        }
    }

    private static String AGENT = null;

    public SflnkgNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> cryptedLinks = new ArrayList<String>();
        String parameter = param.toString().replaceAll("http://", "https://");
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
        get(parameter);
        if (br.containsHTML("(\"This link does not exist\\.\"|ERROR \\- this link does not exist)")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        if (br.containsHTML(">Not yet checked</span>")) { throw new DecrypterException("Not yet checked"); }
        if (br.containsHTML("To use reCAPTCHA you must get an API key from")) { throw new DecrypterException("Server error, please contact the safelinking.net support!"); }
        if (parameter.matches("https?://[\\w\\.]*?safelinking\\.net/d/[a-z0-9]+")) {
            br.getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("location=\"(https?[^\"]+)").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("SafeLinking: Sever issues? continuing...");
                logger.warning("SafeLinking: Please confirm via browser, and report any bugs to JDownloader Developement Team. :" + parameter);
            }
            // prevent loop
            if (!parameter.equals(finallink)) {
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            String cType = "notDetected";
            HashMap<String, String> captchaRegex = new HashMap<String, String>();
            captchaRegex.put("recaptcha", "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)");
            captchaRegex.put("basic", "(https?://safelinking\\.net/includes/captcha_factory/securimage/securimage_(show\\.php\\?hash=[a-z0-9]+|register\\.php\\?hash=[^\"]+sid=[a-z0-9]{32}))");
            captchaRegex.put("threeD", "\"(https?://safelinking\\.net/includes/captcha_factory/3dcaptcha/3DCaptcha\\.php)\"");
            captchaRegex.put("fancy", "class=\"captcha_image ajax\\-fc\\-container\"");
            captchaRegex.put("qaptcha", "class=\"protected\\-captcha\"><div id=\"QapTcha\"");
            captchaRegex.put("solvemedia", "api(\\-secure)?\\.solvemedia\\.com/papi");

            /* cleanup html source */
            String cleanUp = br.toString();
            cleanUp = cleanUp.replaceAll("<div class=\"hidden dialog\" title=\"Register on Safelinking\".*?</div>", "");
            br.getRequest().setHtmlCode(cleanUp);

            for (Entry<String, String> next : captchaRegex.entrySet()) {
                if (br.containsHTML(next.getValue())) {
                    cType = next.getKey();
                }
            }

            logger.info("notDetected".equals(cType) ? "Captcha not detected." : "Detected captcha type \"" + cType + "\" for this host.");

            for (int i = 0; i <= 5; i++) {
                String data = "post-protect=1";
                if (br.containsHTML(PASSWORDPROTECTEDTEXT)) {
                    data += "&link-password=" + getUserInput(null, param);
                }

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
                    captchaBr.getPage("https://safelinking.net/includes/captcha_factory/fancycaptcha.php?hash=" + new Regex(parameter, "safelinking\\.net/p/(.+)").getMatch(0));
                    data += "&fancy-captcha=" + captchaBr.toString().trim();
                    break;
                case qaptcha:
                    captchaBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    captchaBr.postPage("https://safelinking.net/includes/captcha_factory/Qaptcha.jquery.php?hash=" + new Regex(parameter, "safelinking\\.net/p/(.+)").getMatch(0), "action=qaptcha");
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
                    sm.setSecure(true);
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    String code = getCaptchaCode(cf, param);
                    String chid = sm.verify(code);
                    data += "&solvemedia_response=" + code.replace(" ", "+") + "&adcopy_challenge=" + chid + "&adcopy_response=" + code.replace(" ", "+");
                    break;
                }

                if (captchaRegex.containsKey(cType) || data.contains("link-password")) {
                    post(parameter, data, true);
                    if (br.getHttpConnection().getResponseCode() == 500) logger.warning("SafeLinking: 500 Internal Server Error. Link: " + parameter);
                }

                if (!"notDetected".equals(cType) && br.containsHTML(captchaRegex.get(cType)) || br.containsHTML(PASSWORDPROTECTEDTEXT) || br.containsHTML("<strong>Prove you are human</strong>")) {
                    continue;
                }
                break;
            }
            if (!"notDetected".equals(cType) && br.containsHTML(captchaRegex.get(cType)) || br.containsHTML("<strong>Prove you are human</strong>")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
            if (br.containsHTML(PASSWORDPROTECTEDTEXT)) { throw new DecrypterException(DecrypterException.PASSWORD); }
            if (br.containsHTML(">All links are dead\\.<|>Links dead<")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
            // container handling (if no containers found, use webprotection
            if (br.containsHTML("\\.dlc")) {
                decryptedLinks = loadcontainer(".dlc", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) { return decryptedLinks; }
            }

            if (br.containsHTML("\\.rsdf")) {
                decryptedLinks = loadcontainer(".rsdf", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) { return decryptedLinks; }
            }

            if (br.containsHTML("\\.ccf")) {
                decryptedLinks = loadcontainer(".ccf", param);
                if (decryptedLinks != null && decryptedLinks.size() > 0) { return decryptedLinks; }
            }

            // Webprotection decryption
            if (br.getRedirectLocation() != null && br.getRedirectLocation().equals(parameter)) get(parameter);
            for (String[] s : br.getRegex("<div class=\"links\\-container result\\-form\">(.*?)</div>").getMatches()) {
                for (String[] ss : new Regex(s[0], "<a href=\"(.*?)\"").getMatches()) {
                    for (String sss : ss) {
                        if (parameter.equals(sss)) continue;
                        cryptedLinks.add(sss);
                    }
                }
            }

            progress.setRange(cryptedLinks.size());

            for (String link : cryptedLinks) {
                if (link.matches(".*safelinking\\.net/d/.+")) {
                    get(link);
                    link = br.getRedirectLocation();
                    link = link == null ? br.getRegex("location=\"(http[^\"]+)").getMatch(0) : link;
                    if (link == null) {
                        logger.warning("SafeLinking: Sever issues? continuing...");
                        logger.warning("SafeLinking: Please confirm via browser, and report any bugs to developement team. :" + parameter);
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

    private void get(String parameter) throws UnsupportedEncodingException, IOException {
        try {
            br.getPage(parameter);
        } catch (Throwable t) {
            String str = readInputStreamToString(br.getHttpConnection().getErrorStream());
            br.getRequest().setHtmlCode(str);
        }
    }

    private ArrayList<DownloadLink> loadcontainer(String format, CryptedLink param) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        String containerLink = br.getRegex("\"(https?://safelinking\\.net/c/[a-z0-9]+" + format + ")").getMatch(0);
        if (containerLink == null) {
            logger.warning("Contailerlink for link " + param.toString() + " for format " + format + " could not be found.");
            return null;
        }
        String test = Encoding.htmlDecode(containerLink);
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(test);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/safelinknet/" + test.replaceAll("(:|/|\\?)", "") + format);
                if (file == null) { return null; }
                file.deleteOnExit();
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                }
            } else {
                return null;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }

        if (file != null && file.exists() && file.length() > 100) {
            if (decryptedLinks.size() > 0) { return decryptedLinks; }
        } else {
            return null;
        }
        return null;
    }

    private void post(String param, String data, boolean useWorkaround) throws MalformedURLException, IOException {
        try {
            br.postPage(param, data);
        } catch (Throwable t) {
            String str = readInputStreamToString(br.getHttpConnection().getErrorStream());
            br.getRequest().setHtmlCode(str);
        }
    }

}