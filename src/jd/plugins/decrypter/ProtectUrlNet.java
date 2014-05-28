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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect-url.net" }, urls = { "http://(www\\.)?protect-url\\.net/([a-z0-9]+-lnk|check\\.[a-z0-9]+)\\.html" }, flags = { 0 })
public class ProtectUrlNet extends PluginForDecrypt {

    public ProtectUrlNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DecrypterScript_linkid=_linkcheck.php */
    private final String                   PASSWRONG     = "window\\.location = \"linkcheck\\.php\\?linkid=[a-z0-9]+\\&message=wrong\"";
    private final String                   security      = "<font color=red>ACCÈS REFUSÉ : PROXY DÉTECTÉ</font>";
    private static AtomicReference<String> agent         = new AtomicReference<String>(null);
    private static AtomicReference<Object> cookieMonster = new AtomicReference<Object>();
    private static AtomicInteger           maxConProIns  = new AtomicInteger(1);
    private static AtomicLong              lastUsed      = new AtomicLong(0);
    private Browser                        cbr           = new Browser();
    private static Object                  ctrlLock      = new Object();

    private boolean                        debug         = false;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
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
            String uid = new Regex(parameter, "([a-z0-9]+)-lnk\\.html").getMatch(0);
            if (uid == null) {
                uid = new Regex(parameter, "check\\.([a-z0-9]+)\\.html").getMatch(0);
                if (uid == null) {
                    logger.warning("Could not find 'uid'");
                    return null;
                }
            }
            String host = new Regex(parameter, "https?://[^/]+").getMatch(-1);
            getPage(host + "/" + uid + "-lnk.html");
            if (br.containsHTML(security)) {
                Thread.sleep(1000);
                // this switches to french and you no longer need referrer! haxor
                br.setCookie(this.getHost(), "PURL_Lang", "fr");
                br.setCookie(this.getHost(), "googtranz", "1");
                br.getPage(br.getURL());
            }
            if (br.containsHTML("=images/erreur-redirect") || br.getURL().contains("/erreurflood.php")) {
                logger.info("Limit reached, cannot decrypt at the moment: " + parameter);
                return decryptedLinks;
            }
            for (int i = 0; i <= 3; i++) {
                String postData = null;
                if (br.containsHTML(">Sécurité Anti-Robot:|id=captx name=captx") || br.getURL().contains("protect-url.net/check.")) {
                    postData = "captx=ok&linkid=" + new Regex(parameter, "protect-url\\.net/([^<>\"]*?)-lnk\\.html").getMatch(0) + "&ref=";
                }
                if (br.containsHTML(">Mot de Passe:<")) {
                    final String passCode = getUserInput("Enter password for: " + parameter, param);
                    postData += "password=" + passCode;
                }
                if (postData != null) {
                    String freak = br.getRegex("newCookie\\('(PURL_FreakWorld-[a-z0-9]+)','oui").getMatch(0);
                    if (freak != null) {
                        br.setCookie(this.getHost(), freak, "oui");
                    }
                    br.setCookie(this.getHost(), "PURL_PopPub", "1");
                    br.setCookie(this.getHost(), "PURL_NavDossier", "Ooops");
                    br.postPage("http://protect-url.net/linkid.php", postData);
                    if (br.containsHTML(PASSWRONG)) {
                        br.getPage(parameter);
                        continue;
                    }
                }
                break;
            }
            if (br.containsHTML(PASSWRONG)) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            String fpName = br.getRegex("<b>Titre:</b>[\t\n\r ]+</td>[\t\n\r ]+<td style='border:1px;font-weight:bold;font-size:90%;font-family:Arial,Helvetica,sans-serif;'>([^<>\"]*?)</td>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<img id='gglload' src='images/icon-magnify\\.png' style=\"vertical-align: middle;\"></span>([^<>\"]*?) </td>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<span class=\"notranslate\">(.*?)</span>").getMatch(0);
                }
            }
            final String[] l = br.getRegex("monhtsec\\(('|\")(.*?)\\1\\)").getColumn(1);
            if (l != null && l.length != 0) {
                for (String singleLink : l) {
                    if (!singleLink.startsWith("http")) {
                        singleLink = "http://" + singleLink;
                    }
                    if (!this.canHandle(singleLink)) {
                        decryptedLinks.add(createDownloadlink(singleLink));
                    }
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
            // saving session info can result in you not having to enter a captcha for each new link viewed!
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(this.getHost());
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            synchronized (cookieMonster) {
                cookieMonster.set(cookies);
            }

            // rmCookie(parameter);
            lastUsed.set(System.currentTimeMillis());

            if (debug) {
                int i = 0;
                while (i != 10) {
                    logger.info(parameter + " == " + decryptedLinks.size());
                    i++;
                }
                return new ArrayList<DownloadLink>();
            } else {
                return decryptedLinks;
            }
        }
    }

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
                    // don't load lang or googranz cache cookie.
                    if (!entry.getKey().matches("googtranz|PURL_Lang")) {
                        prepBr.setCookie(this.getHost(), entry.getKey(), entry.getValue());
                    }

                }
            }
        }
        prepBr.getHeaders().put("User-Agent", agent.get());

        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("Pragma", null);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.setRequestIntervalLimit(this.getHost(), 1500);
        prepBr.setCookie(this.getHost(), "PURL_Lang", "en");
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    private void nullSession(String parameter) {
        logger.warning("No longer using previously saved session information!, please try adding the link again! : " + parameter);
        synchronized (cookieMonster) {
            agent.set(null);
            cookieMonster.set(new Object());
        }
        maxConProIns.set(1);
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
        return false;
    }

}