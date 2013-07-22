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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dl-protect.com" }, urls = { "http://(www\\.)?dl\\-protect\\.com/(?!fr)(en/)?[A-Z0-9]+" }, flags = { 0 })
@SuppressWarnings("deprecation")
public class DlPrtcCom extends PluginForDecrypt {

    public DlPrtcCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String         CAPTCHATEXT    = ">Security Code";
    private final String         CAPTCHAFAILED  = ">The security code is incorrect";
    private final String         PASSWORDTEXT   = ">Password :";
    private final String         PASSWORDFAILED = ">The password is incorrect";
    private final String         JDDETECTED     = "JDownloader is prohibited.";
    private String               agent          = null;
    private static AtomicInteger maxConProIns   = new AtomicInteger(1);
    private boolean              coLoaded       = false;
    private Browser              cbr            = new Browser();

    @SuppressWarnings("unchecked")
    private Browser prepBrowser(Browser prepBr) {
        // load previous agent, could be referenced with cookie session. (not tested)
        if (agent == null) agent = this.getPluginConfig().getStringProperty("agent", null);
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
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
        prepBr.getHeaders().put("Pragma", null);
        prepBr.getHeaders().put("Accept-Charset", null);
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("dl\\-protect\\.com/(en|fr)/", "dl-protect.com/");
        prepBrowser(br);
        getPage(parameter);
        if (cbr.containsHTML(">Unfortunately, the link you are looking for is not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (cbr.containsHTML(PASSWORDTEXT) || cbr.containsHTML(CAPTCHATEXT)) {
            for (int i = 0; i <= 5; i++) {
                Form importantForm = getForm();
                if (importantForm == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (cbr.containsHTML(PASSWORDTEXT)) {
                    importantForm.put("pwd", getUserInput(null, param));
                }
                if (cbr.containsHTML(CAPTCHATEXT)) {
                    String captchaLink = getCaptchaLink(importantForm.getHtmlCode());
                    if (captchaLink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    captchaLink = "http://www.dl-protect.com" + captchaLink;
                    String code = getCaptchaCode(captchaLink, param);
                    String formName = "secure";
                    importantForm.put(formName, code);
                    importantForm.remove("secures");
                }
                importantForm.put("i", Encoding.Base64Encode(String.valueOf(System.currentTimeMillis())));
                sendForm(importantForm);
                if (getCaptchaLink(cbr.toString()) != null || cbr.containsHTML(CAPTCHAFAILED) || cbr.containsHTML(PASSWORDFAILED) || cbr.containsHTML(PASSWORDTEXT)) continue;
                break;
            }
            if (cbr.containsHTML(CAPTCHAFAILED) && cbr.containsHTML(CAPTCHAFAILED)) throw new DecrypterException("Wrong captcha and password entered!");
            if (getCaptchaLink(cbr.toString()) != null || cbr.containsHTML(CAPTCHAFAILED) || cbr.containsHTML(CAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
            if (cbr.containsHTML(PASSWORDTEXT) || cbr.containsHTML(PASSWORDFAILED)) throw new DecrypterException(DecrypterException.PASSWORD);
        }

        if (cbr.containsHTML(">Please click on continue to see the content")) {
            Form continueForm = getForm();
            if (continueForm == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (coLoaded) {
                continueForm.remove("submitform");
                // continueForm.put("submitform", "");
            }
            sendForm(continueForm);
        }
        if (JDHash.getMD5(JDDETECTED).equals(JDHash.getMD5(br.toString()))) {
            rmCookie(parameter);
        }
        String linktext = cbr.getRegex("class=\"divlink link\"\\s+id=\"slinks\"><a(.*?)valign=\"top\" align=\"right\" width=\"500px\" height=\"280px\"><pre style=\"text\\-align:center\">").getMatch(0);
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

        // once the first session is saved, lets ramp things up!
        // if (maxConProIns.equals(1)) maxConProIns.set(4);
        // rmCookie(parameter);
        return decryptedLinks;

    }

    private String getCaptchaLink(String source) throws Exception {
        // proper captcha url seem to have 32 (md5) char length
        String captchaLink = new Regex(source, "src=\"(\\s+)?(/captcha\\.php\\?uid=_?[a-z0-9]{32})(\\s+)?\"").getMatch(1);
        if (captchaLink == null) {
            captchaLink = new Regex(source, "(/captcha\\.php\\?uid=_?[a-z0-9]{32})").getMatch(0);
            if (captchaLink == null) {
                captchaLink = new Regex(source, "src=[^>]+(/captcha\\.php\\?[^\"]+)").getMatch(0);
                if (captchaLink == null) {
                    captchaLink = new Regex(source, "(/captcha\\.php\\?[^\"']+)").getMatch(0);
                }
            }
        }
        return captchaLink;
    }

    private Form getForm() {
        Form theForm = cbr.getFormbyProperty("name", "ccerure");
        if (theForm == null) theForm = cbr.getForm(0);
        return theForm;
    }

    /**
     * Removes patterns which could break the plugin due to fake/hidden HTML, or false positives caused by HTML comments.
     * 
     * @throws Exception
     * @author raztoki
     */
    public void correctBR() throws Exception {
        String toClean = br.toString();

        ArrayList<String> regexStuff = new ArrayList<String>();

        // remove custom rules first!!! As html can change because of generic cleanup rules.

        // generic cleanup
        // this checks for fake or empty forms from original source and corrects
        for (final Form f : br.getForms()) {
            if (!f.containsHTML("(<input[^>]+type=\"submit\"(>|[^>]+(?!\\s*disabled\\s*)([^>]+>|>))|<input[^>]+type=\"button\"(>|[^>]+(?!\\s*disabled\\s*)([^>]+>|>))|<form[^>]+onSubmit=(\"|').*?(\"|')(>|[\\s\r\n][^>]+>))")) {
                toClean = toClean.replace(f.getHtmlCode(), "");
            }
        }
        // fake captchas are lame, so lets remove them here
        // <center><img id="captcha_" src="/captcha.php?uid=a1" style="display:none">
        // regexStuff.add("((?!<script[^>]+)<!--.*?-->)");
        regexStuff.add("(<img[^>]+display:(\\s+)?(none|hidden)[^>]+>)");
        regexStuff.add("(\\{[^\\}]+getElementById\\('captcha'\\)[^\\}]+/captcha\\.php[^\\}]+)");

        for (String aRegex : regexStuff) {
            String results[] = new Regex(toClean, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    toClean = toClean.replace(result, "");
                }
            }
        }

        cbr = br.cloneBrowser();
        cleanupBrowser(cbr, toClean);
    }

    private void rmCookie(String parameter) {
        logger.warning("No longer using previously saved session information!, please try adding the link again! : " + parameter);
        this.getPluginConfig().setProperty("cookies", Property.NULL);
        this.getPluginConfig().setProperty("agent", Property.NULL);
        this.getPluginConfig().save();
        maxConProIns.set(1);
        coLoaded = false;
    }

    /**
     * This allows backward compatibility for design flaw in setHtmlCode(), It injects updated html into all browsers that share the same
     * request id. This is needed as request.cloneRequest() was never fully implemented like browser.cloneBrowser().
     * 
     * @param ibr
     *            Import Browser
     * @param t
     *            Provided replacement string output browser
     * @author raztoki
     * */
    private void cleanupBrowser(final Browser ibr, final String t) throws Exception {
        String dMD5 = JDHash.getMD5(ibr.toString());
        // preserve valuable original request components.
        final String oURL = ibr.getURL();
        final URLConnectionAdapter con = ibr.getRequest().getHttpConnection();

        Request req = new Request(oURL) {
            {
                boolean okay = false;
                try {
                    final Field field = this.getClass().getSuperclass().getDeclaredField("requested");
                    field.setAccessible(true);
                    field.setBoolean(this, true);
                    okay = true;
                } catch (final Throwable e2) {
                    e2.printStackTrace();
                }
                if (okay == false) {
                    try {
                        requested = true;
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                }

                httpConnection = con;
                setHtmlCode(t);
            }

            public long postRequest() throws IOException {
                return 0;
            }

            public void preRequest() throws IOException {
            }
        };

        ibr.setRequest(req);
        if (ibr.isDebug()) {
            logger.info("\r\ndirtyMD5sum = " + dMD5 + "\r\ncleanMD5sum = " + JDHash.getMD5(ibr.toString()) + "\r\n");
            System.out.println(ibr.toString());
        }
    }

    private void getPage(final String page) throws Exception {
        br.getPage(page);
        correctBR();
    }

    @SuppressWarnings("unused")
    private void postPage(final String page, final String postData) throws Exception {
        br.postPage(page, postData);
        correctBR();
    }

    private void sendForm(final Form form) throws Exception {
        br.submitForm(form);
        correctBR();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public int getMaxConcurrentProcessingInstances() {
        // works best with the cache session, otherwise first import results in many captchas!
        return maxConProIns.get();
    }

}