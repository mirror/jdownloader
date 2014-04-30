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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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

    private final String                   CAPTCHATEXT    = ">Security Code";
    private final String                   CAPTCHAFAILED  = ">The security code is incorrect";
    private final String                   PASSWORDTEXT   = ">Password :";
    private final String                   PASSWORDFAILED = ">The password is incorrect";
    private final String                   JDDETECTED     = "JDownloader is prohibited.";
    private static AtomicReference<String> agent          = new AtomicReference<String>(null);
    private static AtomicReference<Object> cookieMonster  = new AtomicReference<Object>();
    private static AtomicInteger           maxConProIns   = new AtomicInteger(1);
    private static AtomicLong              lastUsed       = new AtomicLong(0);
    private boolean                        coLoaded       = false;
    private Browser                        cbr            = new Browser();
    private static Object                  ctrlLock       = new Object();
    private boolean                        test           = false;

    @SuppressWarnings("unchecked")
    private Browser prepBrowser(final Browser prepBr) {
        // loading previous cookie session results in less captchas
        synchronized (cookieMonster) {
            // link agent to cookieMonster via synchronized
            if (agent.get() == null) {
                /* we first have to load the plugin, before we can reference it */
                JDUtilities.getPluginForHost("mediafire.com");
                agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
            }
            if (cookieMonster.get() != null && cookieMonster.get() instanceof HashMap<?, ?>) {
                final HashMap<String, String> cookies = (HashMap<String, String>) cookieMonster.get();
                for (Map.Entry<String, String> entry : cookies.entrySet()) {
                    prepBr.setCookie(this.getHost(), entry.getKey(), entry.getValue());

                }
                coLoaded = true;
            }
        }
        prepBr.getHeaders().put("User-Agent", agent.get());

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
        if (parameter.endsWith("dl-protect.com/en")) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }
        // prevent more than one thread starting across the different versions of JD
        synchronized (ctrlLock) {
            // has to be this side of lock otherwise loading of cookies before lock will always be blank or wrong.
            prepBrowser(br);
            // little wait between ties?
            if (lastUsed.get() == 0) {
                // magic
            } else if ((System.currentTimeMillis() - lastUsed.get()) <= 5000) {
                // hoodo
                Thread.sleep(3731 + new Random().nextInt(3000));
            }
            getPage(parameter);
            if (cbr.containsHTML(">Unfortunately, the link you are looking for is not found")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (cbr.containsHTML(PASSWORDTEXT) || cbr.containsHTML(CAPTCHATEXT)) {
                int repeat = 2;
                for (int i = 0; i != repeat; i++) {
                    Form importantForm = getForm();
                    if (importantForm == null) {
                        logger.warning("Decrypter broken 1 for link: " + parameter);
                        return null;
                    }
                    if (cbr.containsHTML(PASSWORDTEXT)) {
                        final String pwd = getUserInput(null, param);
                        importantForm.put("pwd", pwd);
                    }
                    if (cbr.containsHTML(CAPTCHATEXT)) {
                        String[] test = cbr.getRegex("<img[^>]+src=\"(/template/images/[^\"]+)").getColumn(0);
                        if (test != null) {
                            for (final String t : test) {
                                final Browser brAds = br.cloneBrowser();
                                try {
                                    brAds.openGetConnection(t);
                                } catch (final Exception e) {
                                }
                            }
                        }

                        String captchaLink = getCaptchaLink(importantForm.getHtmlCode());
                        if (captchaLink == null) {
                            logger.warning("Decrypter broken 2 for link: " + parameter);
                            return null;
                        }
                        captchaLink = "http://www.dl-protect.com" + captchaLink;
                        String code = null;
                        if (true) {
                            Browser obr = br.cloneBrowser();
                            br.getHeaders().put("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/webp, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
                            code = getCaptchaCode(captchaLink, param);
                            br = obr.cloneBrowser();
                        }
                        br.cloneBrowser().getPage("/pub_footer.html");
                        String formName = "secure";
                        importantForm.put(formName, code);
                        // importantForm.put("secure", "");
                        importantForm.put("submitform", "");
                    }
                    importantForm.put("i", ""/* Encoding.Base64Encode(String.valueOf(System.currentTimeMillis())) */);
                    sendForm(importantForm);
                    if ((getCaptchaLink(cbr.toString()) != null && cbr.containsHTML(CAPTCHAFAILED)) || (cbr.containsHTML(PASSWORDFAILED) || cbr.containsHTML(PASSWORDTEXT))) {
                        if (i + 1 == repeat) {
                            // dump session
                            nullSession(parameter);
                            throw new DecrypterException("Excausted Retry!");
                        } else {
                            continue;
                        }
                    } else {
                        break;
                    }
                }
            }
            // if (cbr.containsHTML("No htmlCode read")) {
            // getPage(parameter);
            // }

            if (cbr.containsHTML(">Please click on continue to see")) {
                br.cloneBrowser().getPage("/pub_footer.html");
                Form continueForm = getContinueForm();
                if (continueForm == null) {
                    logger.warning("Decrypter broken 3 for link: " + parameter);
                    return null;
                }
                if (coLoaded) {
                    // continueForm.remove("submitform");
                    continueForm.put("submitform", "");
                }
                sendForm(continueForm);
            }
            if (JDHash.getMD5(JDDETECTED).equals(JDHash.getMD5(br.toString()))) {
                nullSession(parameter);
                throw new DecrypterException("D-TECTED!");
            }
            br.cloneBrowser().getPage("/pub_footer.html");
            String linktext = cbr.getRegex("class=\"divlink link\"\\s+id=\"slinks\"><a(.*?)</table>").getMatch(0);
            if (linktext == null) {
                if (br.containsHTML(">Your link :</div>")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken 4 for link: " + parameter);
                return null;
            }
            final String[] links = new Regex(linktext, "href=\"([^\"\\']+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken 5 for link: " + parameter);
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
            synchronized (cookieMonster) {
                cookieMonster.set((Object) cookies);
            }

            // rmCookie(parameter);
            lastUsed.set(System.currentTimeMillis());

            return decryptedLinks;
        }

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

    private Form getContinueForm() {
        Form theForm = cbr.getFormbyProperty("name", "submitform");
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

    private void nullSession(String parameter) {
        logger.warning("No longer using previously saved session information!, please try adding the link again! : " + parameter);
        synchronized (cookieMonster) {
            agent.set(null);
            cookieMonster.set(new Object());
        }
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

}