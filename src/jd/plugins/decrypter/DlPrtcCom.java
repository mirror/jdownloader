//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dl-protect.com" }, urls = { "http://(www\\.)?dl\\-protect\\.com/(?!fr)(en/)?[A-Z0-9]+" }, flags = { 0 })
public class DlPrtcCom extends PluginForDecrypt {

    public DlPrtcCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String  CAPTCHATEXT    = ">Security Code";
    private static final String  CAPTCHAFAILED  = ">The security code is incorrect";
    private static final String  PASSWORDTEXT   = ">Password :";
    private static final String  PASSWORDFAILED = ">The password is incorrect";
    private static final String  JDDETECTED     = "JDownloader is prohibited.";
    private static String        agent          = null;
    private static AtomicBoolean mfLoaded       = new AtomicBoolean(false);
    private boolean              coLoaded       = false;
    private String               correctedBR    = "";

    private Browser prepBrowser(Browser prepBr) {
        // load previous agent, could be referenced with cookie session. (not tested)
        if (agent == null) agent = this.getPluginConfig().getStringProperty("agent", null);
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            if (mfLoaded.equals(false)) {
                JDUtilities.getPluginForHost("mediafire.com");
                mfLoaded.set(true);
            }
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent);
        // loading previous cookie session results in less captchas
        final Object ret = this.getPluginConfig().getProperty("cookies", null);
        if (ret != null) {
            final HashMap<String, String> cookies = (HashMap<String, String>) ret;
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                prepBr.setCookie(this.getHost(), entry.getKey(), entry.getValue());
            }
            coLoaded = true;
        }
        // Prefer English language
        if (!coLoaded) prepBr.setCookie(this.getHost(), "l", "en");
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("dl\\-protect\\.com/(en|fr)/", "dl-protect.com/");
        prepBrowser(br);
        br.getPage(parameter);
        if (br.containsHTML(">Unfortunately, the link you are looking for is not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(PASSWORDTEXT) || br.containsHTML(CAPTCHATEXT)) {
            for (int i = 0; i <= 5; i++) {
                Form importantForm = getForm();
                if (importantForm == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (br.containsHTML(PASSWORDTEXT)) {
                    importantForm.put("pwd", getUserInput(null, param));
                }
                if (br.containsHTML(CAPTCHATEXT)) {
                    String captchaLink = getCaptchaLink();
                    captchaLink = "http://www.dl-protect.com" + captchaLink;
                    String code = getCaptchaCode(captchaLink, param);
                    importantForm.put("secure_oo", code);
                }
                importantForm.put("i", Encoding.Base64Encode(String.valueOf(System.currentTimeMillis())));
                br.submitForm(importantForm);
                if (getCaptchaLink() != null || br.containsHTML(CAPTCHAFAILED) || br.containsHTML(PASSWORDFAILED) || br.containsHTML(PASSWORDTEXT)) continue;
                break;
            }
            if (br.containsHTML(CAPTCHAFAILED) && br.containsHTML(CAPTCHAFAILED)) throw new DecrypterException("Wrong captcha and password entered!");
            if (getCaptchaLink() != null || br.containsHTML(CAPTCHAFAILED) || br.containsHTML(CAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
            if (br.containsHTML(PASSWORDTEXT) || br.containsHTML(PASSWORDFAILED)) throw new DecrypterException(DecrypterException.PASSWORD);
        }

        if (br.containsHTML(">Please click on continue to see the content")) {
            Form continueForm = getForm();
            if (continueForm == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (coLoaded) {
                continueForm.remove("submitform");
                continueForm.put("submitform", "");
            }
            br.submitForm(continueForm);
        }
        final String linktext = br.getRegex("class=\"divlink link\" id=\"slinks\"><a(.*?)valign=\"top\" align=\"right\" width=\"500px\" height=\"280px\"><pre style=\"text\\-align:center\">").getMatch(0);
        if (linktext == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] links = new Regex(linktext, "href=\"([^\"\\']+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));

        // saving session info can result in you not having to enter a captcha for each new link viewed!
        final HashMap<String, String> cookies = new HashMap<String, String>();
        final Cookies add = br.getCookies(this.getHost());
        for (final Cookie c : add.getCookies()) {
            cookies.put(c.getKey(), c.getValue());
        }
        this.getPluginConfig().setProperty("cookies", cookies);
        this.getPluginConfig().setProperty("agent", agent);
        this.getPluginConfig().save();

        return decryptedLinks;

    }

    private String getCaptchaLink() throws Exception {
        correctBR();
        // proper captcha url seem to have 32 (md5) char length
        final String captchaLink = new Regex(correctedBR, "src=\"(/captcha\\.php\\?uid=[a-z0-9]{32})\"").getMatch(0);
        return captchaLink;
    }

    private Form getForm() {
        Form theForm = br.getFormbyProperty("name", "ccerure");
        if (theForm == null) theForm = br.getForm(0);
        return theForm;
    }

    /** Remove HTML code which could break the plugin */
    public void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();

        // remove custom rules first!!! As html can change because of generic cleanup rules.

        // fake captchas are lame, so lets remove them here
        // <center><img id="captcha_" src="/captcha.php?uid=a1" style="display:none">
        regexStuff.add("(<img[^>]+display:(\\s+)?(none|hidden)\">)");
        regexStuff.add("(<img[^>]+display:(\\s+)?(none|hidden)'>)");

        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
    }

    private void rmCookie(String parameter) {
        logger.warning("No longer using previously saved session information!, please try adding the link again! : " + parameter);
        this.getPluginConfig().setProperty("cookies", Property.NULL);
        this.getPluginConfig().setProperty("agent", Property.NULL);
        this.getPluginConfig().save();
        coLoaded = false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}