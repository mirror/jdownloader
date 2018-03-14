//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaShowDialogTwo;
import org.jdownloader.captcha.v2.challenge.xsolver.CaptXSolver;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+(?:\\?hostid=all&clearing=[a-f0-9]+)?" })
public class LnkCrptWs extends antiDDoSForDecrypt {
    /**
     * if packed js contain 'soft hyphen' encoding as \u00ad(unicode) or %C2%AD(uft-8) then result is broken in rhino
     * decodeURIComponent('\u00ad') --> is empty.
     */
    public static class JavaScriptUnpacker {
        public static String decode(String packedJavaScript) {
            /* packed with extended ascii */
            if (new Regex(packedJavaScript, "c%a\\+161").matches()) {
                String packed[] = new Regex(packedJavaScript, ("\\}\\(\'(.*?)\',(\\d+),(\\d+),\'(.*?)\'\\.split\\(\'\\|\\'\\),(\\d+)")).getRow(0);
                if (packed == null) {
                    return null;
                }
                return nativeDecode(packed[0], Integer.parseInt(packed[1]), Integer.parseInt(packed[2]), packed[3].split("\\|"), Integer.parseInt(packed[4]));
            }
            return rhinoDecode(packedJavaScript);
        }

        private static String nativeDecode(String p, int a, int c, String k[], int e) {
            LinkedHashMap<String, String> lm = new LinkedHashMap<String, String>();
            while (c > 0) {
                c--;
                lm.put(e(c, a), k[c]);
            }
            for (Entry<String, String> next : lm.entrySet()) {
                p = p.replace(next.getKey(), next.getValue());
            }
            return p;
        }

        private static String rhinoDecode(String eval) {
            Object result = new Object();
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                result = engine.eval(eval);
            } catch (final Throwable e) {
            }
            return result != null ? String.valueOf(result) : null;
        }

        private static String e(int c, int a) {
            return (c < a ? "" : e(c / a, a)) + String.valueOf((char) (c % a + 161));
        }
    }

    public static String IMAGEREGEX(final String b) {
        final KeyCaptchaShowDialogTwo v = new KeyCaptchaShowDialogTwo();
        /*
         * CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset!
         */
        final byte[] o = JDHash.getMD5(Encoding.Base64Decode("Yzg0MDdhMDhiM2M3MWVhNDE4ZWM5ZGM2NjJmMmE1NmU0MGNiZDZkNWExMTRhYTUwZmIxZTEwNzllMTdmMmI4Mw==") + JDHash.getMD5("V2UgZG8gbm90IGVuZG9yc2UgdGhlIHVzZSBvZiBKRG93bmxvYWRlci4=")).getBytes();
        /*
         * CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8
         */
        if (b != null) {
            return new String(v.D(o, JDHexUtils.getByteArray(b)));
        }
        return new String(v.D(o, JDHexUtils.getByteArray("E3CEACB19040D08244C9E5C29D115AE220F83AB417")));
    }

    private final HashMap<String, String> map = new HashMap<String, String>();
    private final HashMap<String, String> cln = new HashMap<String, String>();

    public LnkCrptWs(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        setBrowserExclusive();
        final String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);
        final String parameters = new Regex(parameter, containerId + "(\\?hostid=all&clearing=[a-f0-9]+)?").getMatch(0);
        parameter = "http://linkcrypt.ws/dir/" + containerId + (parameters != null ? parameters : "");
        if (!loadAndSolveCaptcha(param, progress, decryptedLinks, parameter, containerId)) {
            return decryptedLinks;
        }
        return doThis(param, progress, decryptedLinks, parameter, containerId);
    }

    private final Form getPasswordForm() {
        final Form[] passwords = br.getForms();
        con: for (final Form password : passwords) {
            Boolean istrue = false;
            for (final InputField i : password.getInputFields()) {
                if ("password".equalsIgnoreCase(i.getKey())) {
                    istrue = true;
                } else if ("email".equalsIgnoreCase(i.getType())) {
                    continue con;
                }
            }
            if (istrue) {
                return password;
            }
        }
        return null;
    }

    private ArrayList<DownloadLink> doThis(final CryptedLink param, final ProgressController progress, final ArrayList<DownloadLink> decryptedLinks, final String parameter, final String containerId) throws Exception {
        // debug yo!
        map.clear();
        URLConnectionAdapter con;
        // check for a password. Store latest password in DB
        Form password = getPasswordForm();
        if (password != null) {
            String latestPassword = getPluginConfig().getStringProperty("PASSWORD", null);
            if (latestPassword != null) {
                password.put("password", Encoding.urlEncode(latestPassword));
                submitForm(password);
                //
            }
            // no defaultpassword, or defaultpassword is wrong
            for (int i = 0; i <= 3; i++) {
                password = getPasswordForm();
                if (password != null) {
                    latestPassword = Plugin.getUserInput(null, param);
                    password.put("password", Encoding.urlEncode(latestPassword));
                    submitForm(password);
                    password = getPasswordForm();
                    if (password != null) {
                        if (i + 1 == 3) {
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        }
                        continue;
                    }
                    getPluginConfig().setProperty("PASSWORD", latestPassword);
                    getPluginConfig().save();
                    break;
                }
                break;
            }
        }
        // Look for containers
        String[] containers = br.getRegex("eval(.*?)[\r\n]+").getColumn(0);
        final String tmpc = br.getRegex("<div id=\"containerfiles\"(.*?)</script>").getMatch(0);
        if (tmpc != null) {
            containers = new Regex(tmpc, "eval(.*?)[\r\n]+").getColumn(0);
        }
        if (containers == null || containers.length == 0) {
            // some new bullshit here where they want you to click on menu and select each hoster to get access to clicknload. -raz 20151128
            // so here we go...
            final String[] hosters = br.getRegex("<a href=(\"|')([^\r\n]*/dir/" + containerId + "\\?hostid=(?!all)[\\w\\-]+)\\1").getColumn(1);
            if (hosters != null && hosters.length != 0) {
                containers = new String[hosters.length];
                for (int i = 0; i != hosters.length; i++) {
                    final Browser br2 = br.cloneBrowser();
                    final String host = hosters[i];
                    getPage(br2, host);
                    final String result = br2.getRegex("eval(.*?)[\r\n]+").getMatch(0);
                    if (result != null) {
                        containers[i] = result;
                    }
                }
            }
        }
        String decryptedJS = null;
        for (String c : containers) {
            decryptedJS = JavaScriptUnpacker.decode(c);
            String[] row = new Regex(decryptedJS, "href=\"(http.*?)\".*?(dlc|ccf|rsdf)").getRow(0);// all
            // container
            if (row == null) {
                row = new Regex(decryptedJS, "href=\"([^\"]+)\"[^>]*>.*?<img.*?image/(.*?)\\.").getRow(0); // cnl
            }
            if (row == null) {
                row = new Regex(decryptedJS, "(https?://linkcrypt\\.ws/container/[^\"]+)\".*?https?://linkcrypt\\.ws/image/([a-z]+)\\.").getRow(0); // fallback
            }
            // supports cnl when in js document write
            if (row == null && decryptedJS.contains("127.0.0.1:9666/flash/")) {
                // process CnL
                processCnL(decryptedJS, decryptedLinks, parameter);
                continue;
            }
            if (row == null) {
                // containers can have unknown file extensions in URL, but ID which is referenced in HTML. to combat that we need todo the
                // following
                final String unknown[] = new Regex(decryptedJS, "\\$\\((\"|')#(.*?)\\1.*?(\"|')(https?://.*?linkcrypt.ws/container/.*?)\\3").getRow(0);
                // unknown[1] and [3] will contain the info we need
                // find the container type
                if (unknown != null && unknown.length == 4) {
                    final String ctype = br.getRegex("<[\\w+][^>]+id=(\"|')(" + unknown[1] + ")\\1>(.*?)<").getMatch(2);
                    if (ctype != null) {
                        map.put(ctype.toLowerCase(Locale.ENGLISH), unknown[3]);
                    } else {
                        // when ctype is null,, container type isn't really _needed_. we can randomise yo.
                        map.put(new Random().nextInt(1000) + "", unknown[3]);
                    }
                }
            }
        }
        if (Boolean.TRUE.equals(CnLSuccessful) && !decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        final Form preRequest = br.getForm(0);
        if (preRequest != null) {
            final String url = preRequest.getRegex("https?://.*/captcha\\.php\\?id=\\d+").getMatch(-1);
            if (url != null) {
                con = null;
                try {
                    con = br.cloneBrowser().openGetConnection(url);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        /* CNL --> Container --> Webdecryption */
        final Form[] webDecryptForms = br.getFormsByActionRegex(".*linkcrypt\\.ws/out\\.html");
        boolean webDecryption = webDecryptForms != null ? true : false;
        // CNL proccessed above!
        // Container
        for (Entry<String, String> next : map.entrySet()) {
            if (!next.getKey().equalsIgnoreCase("cnl")) {
                final File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + next.getKey(), true);
                if (!container.exists()) {
                    container.createNewFile();
                }
                try {
                    getCaptchaBrowser(br).getDownload(container, next.getValue());
                    if (container != null) {
                        logger.info("Container found: " + container);
                        decryptedLinks.addAll(loadContainerFile(container));
                        container.delete();
                        if (!decryptedLinks.isEmpty()) {
                            return decryptedLinks;
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // Webdecryption
        if (webDecryption) {
            // new 20152216
            progress.setRange(webDecryptForms.length);
            for (final Form form : webDecryptForms) {
                Browser clone;
                if (form.getInputField("file") != null && form.getInputField("file").getValue() != null && form.getInputField("file").getValue().length() > 0) {
                    progress.increase(1);
                    clone = br.cloneBrowser();
                    submitForm(clone, form);
                    final String[] srcs = clone.getRegex("<frame scrolling.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getColumn(0);
                    for (String col : srcs) {
                        if (col.contains("out.pl=head")) {
                            continue;
                        }
                        col = Encoding.htmlDecode(col);
                        if (col.contains("out.pl")) {
                            getPage(clone, col);
                            // Thread.sleep(600);
                            if (clone.containsHTML("eval")) {
                                final String[] evals = clone.getRegex("eval(.*?)[\r\n]+").getColumn(0);
                                for (final String c : evals) {
                                    String code = JavaScriptUnpacker.decode(c);
                                    if (code == null) {
                                        continue;
                                    }
                                    if (code.contains("ba2se") || code.contains("premfree")) {
                                        code = code.replaceAll("\\\\", "");
                                        String versch = new Regex(code, "ba2se=\'(.*?)\'").getMatch(0);
                                        if (versch == null) {
                                            versch = new Regex(code, ".*?='([^']*)'").getMatch(0);
                                            versch = Encoding.Base64Decode(versch);
                                            versch = new Regex(versch, "<iframe.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getMatch(0);
                                        }
                                        versch = Encoding.Base64Decode(versch);
                                        versch = Encoding.htmlDecode(new Regex(versch, "100.*?src=\"(.*?)\"></iframe>").getMatch(0));
                                        if (versch != null) {
                                            final DownloadLink dl = createDownloadlink(versch);
                                            try {
                                                distribute(dl);
                                            } catch (final Throwable e) {
                                                /* does not exist in 09581 */
                                            }
                                            decryptedLinks.add(dl);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 23.7.14
                    String link = clone.getRegex("'window\\.location = \"([^\"]+)").getMatch(0);
                    if (link == null) {
                        // 20151116
                        link = clone.getRegex("var url = ('|\")(.*?)\\1").getMatch(1);
                        if (link == null) {
                            // 20151226
                            link = clone.getRegex("top\\.location\\.href\\s*\\=\\s*doNotTrack\\('(.*?)'\\)").getMatch(0);
                        }
                    }
                    if (link != null) {
                        final DownloadLink dl = createDownloadlink(link);
                        distribute(dl);
                        decryptedLinks.add(dl);
                    } else {
                        link = clone.getRedirectLocation();
                        if (link != null) {
                            final DownloadLink dl = createDownloadlink(link);
                            distribute(dl);
                            decryptedLinks.add(dl);
                        }
                    }
                    try {
                        if (this.isAbort()) {
                            return decryptedLinks;
                        }
                    } catch (Throwable e) {
                        /* does not exist in 09581 */
                    }
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            // logger.info("No links found, let's see if CNL2 is available!");
            // if (isCnlAvailable) {
            // LocalBrowser.openDefaultURL(new URL(parameter));
            // throw new DecrypterException(JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
            // }
            logger.warning("Link probably offline: " + parameter);
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        try {
            validateLastChallengeResponse();
        } catch (final Throwable e) {
        }
        return decryptedLinks;
    }

    private Boolean CnLSuccessful = null;

    private void processCnL(final String s, final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws UnsupportedEncodingException {
        final Browser cnlbr = br.cloneBrowser();
        String decryptedJS = s.replaceAll("\\\\", "").replaceAll("\\'", "'");
        cnlbr.getRequest().setHtmlCode(decryptedJS);
        Form cnlForm = cnlbr.getForm(0);
        if (cnlForm != null) {
            HashMap<String, String> infos = new HashMap<String, String>();
            infos.put("crypted", Encoding.urlDecode(cnlForm.getInputField("crypted").getValue(), false));
            infos.put("jk", Encoding.urlDecode(cnlForm.getInputField("jk").getValue(), false));
            String source = cnlForm.getInputField("source").getValue();
            if (StringUtils.isEmpty(source)) {
                source = parameter.toString();
            } else {
                source = Encoding.urlDecode(source, true);
            }
            infos.put("source", source);
            String json = JSonStorage.toString(infos);
            final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
            decryptedLinks.add(dl);
            CnLSuccessful = Boolean.TRUE;
            return;
        }
        CnLSuccessful = Boolean.FALSE;
    }

    private final String  captchaTypeKeyCaptcha = "<!-- KeyCAPTCHA code|<input [^>]*name=(\"|')capcode\\1";
    private static Object LOCK                  = new Object();

    public boolean loadAndSolveCaptcha(final CryptedLink param, final ProgressController progress, final ArrayList<DownloadLink> decryptedLinks, final String parameter, final String containerId) throws IOException, InterruptedException, Exception, DecrypterException {
        synchronized (LOCK) {
            if (isAbort()) {
                return false;
            }
            br.clearCookies(parameter);
            getPage(parameter);
            // here is some smoozed bullshit hijack
            if (br.containsHTML("<div class=\"red\" id=\"download_free\">")) {
                getPage(br.getURL());
                // this sets a cookie, but number changes all the time.
                // hintmsg=xxxxxxxxxx x = \d
            }
            for (int i = 0; i < 5; i++) {
                if (isAbort()) {
                    return false;
                }
                if (br.containsHTML("TextX")) {
                    // since we are currently not able to auto solve TextX Captcha, we try to get another one
                    Thread.sleep(500);
                    br.clearCookies(parameter);
                    getPage(parameter);
                    continue;
                } else {
                    break;
                }
            }
            System.out.println("TextX " + br.containsHTML("TextX"));
            System.out.println("CaptX " + br.containsHTML("CaptX"));
            System.out.println("KeyCAPTCHA " + br.containsHTML(captchaTypeKeyCaptcha));
            if (br.containsHTML("<title>Linkcrypt\\.ws // Error 404</title>")) {
                final String msg = "This link might be offline!";
                final String additional = br.getRegex("<h2>\r?\n?(.*?)<").getMatch(0);
                if (additional != null && !(additional.matches("\\s+") || "".equals(additional))) {
                    logger.info(additional);
                }
                try {
                    decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "([\\w]+)$").getMatch(0), msg));
                } catch (final Throwable t) {
                    logger.info(msg + " :: " + parameter);
                }
                return false;
            }
            final String important[] = { "/js/jquery.js", "/dir/image/Warning.png" };
            URLConnectionAdapter con = null;
            for (final String template : important) {
                final Browser br2 = br.cloneBrowser();
                try {
                    con = br2.openGetConnection(template);
                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            // Different captcha types
            if (br.containsHTML(captchaTypeKeyCaptcha)) {
                boolean done = false;
                KeyCaptcha kc;
                // START solve keycaptcha automatically
                for (int i = 0; i < 3; i++) {
                    String result = null;
                    result = handleCaptchaChallenge(new KeyCaptcha(this, br, createDownloadlink(parameter)).createChallenge(this));
                    postPage(parameter, "capcode=" + Encoding.urlEncode(result));
                    if (!br.containsHTML(captchaTypeKeyCaptcha)) {
                        done = true;
                        break;
                    }
                }
                if (!done) {
                    // manual failover
                    for (int i = 0; i <= 3; i++) {
                        final String result = handleCaptchaChallenge(new KeyCaptcha(this, br, createDownloadlink(parameter)).createChallenge(true, this));
                        if (result == null) {
                            continue;
                        }
                        if ("CANCEL".equals(result)) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        postPage(parameter, "capcode=" + Encoding.urlEncode(result));
                        if (!br.containsHTML(captchaTypeKeyCaptcha)) {
                            break;
                        }
                    }
                    if (br.containsHTML(captchaTypeKeyCaptcha)) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
            }
            if (br.containsHTML("CaptX|TextX")) {
                boolean valid = true;
                final int max_attempts = 4;
                for (int attempts = 0; attempts < max_attempts; attempts++) {
                    if (valid && attempts > 0) {
                        break;
                    }
                    final Form[] captchas = br.getForms();
                    String url = null;
                    for (final Form captcha : captchas) {
                        if (captcha != null && br.containsHTML("CaptX|TextX")) {
                            url = captcha.getRegex("src=\"(.*?secid.*?)\"").getMatch(0);
                            if (url != null) {
                                valid = false;
                                final String capDescription = captcha.getRegex("<b>(.*?)</b>").getMatch(0);
                                final File file = this.getLocalCaptchaFile();
                                getCaptchaBrowser(br).getDownload(file, url);
                                // remove black bars
                                final Point p;
                                final byte[] bytes = IO.readFile(file);
                                if (br.containsHTML("CaptX") && attempts < 2) {
                                    // try autosolve
                                    p = CaptXSolver.solveCaptXCaptcha(bytes);
                                } else {
                                    p = null;
                                }
                                if (p == null) {
                                    // solve by user
                                    BufferedImage image = CaptXSolver.toBufferedImage(new ByteArrayInputStream(bytes));
                                    ImageIO.write(image, "png", file);
                                    final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, "LinkCrypt.ws | " + String.valueOf(max_attempts - attempts), capDescription);
                                    captcha.put("x", cp.getX() + "");
                                    captcha.put("y", cp.getY() + "");
                                } else {
                                    captcha.put("x", p.x + "");
                                    captcha.put("y", p.y + "");
                                }
                                submitForm(captcha);
                                if (!br.containsHTML("(Our system could not identify you as human beings\\!|Your choice was wrong\\! Please wait some seconds and try it again\\.)")) {
                                    valid = true;
                                    break;
                                } else {
                                    getPage("/dir/" + containerId);
                                }
                            }
                        }
                    }
                }
                if (!valid) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            return true;
        }
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);
        try {
            ret.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_DECRYPTER);
        } catch (Throwable e) {
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 5;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setCustomCharset("ISO-8859-1");
            prepBr.getHeaders().put("Cache-Control", null);
            prepBr.getHeaders().put("Accept-Charset", null);
            prepBr.getHeaders().put("Accept", "*/*");
            prepBr.getHeaders().put("Accept-Language", "en-EN");
            prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
            try {
                prepBr.setCookie("linkcrypt.ws", "language", "en");
            } catch (final Throwable e) {
            }
            prepBr.setKeepResponseContentBytes(true);
        }
        return prepBr;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}
