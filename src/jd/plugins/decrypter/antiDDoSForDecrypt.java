package jd.plugins.decrypter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;

/**
 *
 * @author raztoki
 *
 */
@SuppressWarnings({ "deprecation", "unused" })
public abstract class antiDDoSForDecrypt extends PluginForDecrypt {

    public antiDDoSForDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected CryptedLink param = null;

    protected boolean useRUA() {
        return false;
    }

    // cloudflare

    protected static HashMap<String, String>      antiDDoSCookies = new HashMap<String, String>();
    protected final WeakHashMap<Browser, Boolean> browserPrepped  = new WeakHashMap<Browser, Boolean>();
    protected static AtomicReference<String>      agent           = new AtomicReference<String>(null);
    private boolean                               prepBrSet       = false;

    protected Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        // required for native cloudflare support, without the need to repeat requests.
        try {
            prepBr.addAllowedResponseCodes(new int[] { 429, 503, 520, 522 });
        } catch (final Throwable t) {
        }
        if ((browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            return prepBr;
        }
        synchronized (antiDDoSCookies) {
            if (!antiDDoSCookies.isEmpty()) {
                for (final Map.Entry<String, String> cookieEntry : antiDDoSCookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    prepBr.setCookie(this.getHost(), key, value);
                }
            }
        }
        if (useRUA()) {
            if (agent.get() == null) {
                /* we first have to load the plugin, before we can reference it */
                JDUtilities.getPluginForHost("mediafire.com");
                agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
            }
            prepBr.getHeaders().put("User-Agent", agent.get());
        }
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Pragma", null);
        // we now set
        browserPrepped.put(prepBr, Boolean.TRUE);
        return prepBr;
    }

    /**
     * Gets page <br />
     * - natively supports silly cloudflare anti DDoS crapola
     *
     * @author raztoki
     */
    public void getPage(final Browser ibr, final String page) throws Exception {
        if (page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!prepBrSet) {
            prepBrowser(ibr);
        }
        final boolean follows_redirects = ibr.isFollowingRedirects();
        URLConnectionAdapter con = null;
        ibr.setFollowRedirects(true);
        try {
            con = ibr.openGetConnection(page);
            readConnection(con, ibr);
            antiDDoS(ibr);
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
            ibr.setFollowRedirects(follows_redirects);
        }
    }

    /**
     * Wrapper into getPage(importBrowser, page), where browser = br;
     *
     * @author raztoki
     *
     * */
    public void getPage(final String page) throws Exception {
        getPage(br, page);
    }

    public void postPage(final Browser ibr, String page, final String postData) throws Exception {
        if (page == null || postData == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!prepBrSet) {
            prepBrowser(ibr);
        }
        // stable sucks
        if (isJava7nJDStable() && page.startsWith("https")) {
            page = page.replaceFirst("^https://", "http://");
        }
        ibr.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        URLConnectionAdapter con = null;
        try {
            con = ibr.openPostConnection(page, postData);
            readConnection(con, ibr);
            antiDDoS(ibr);
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
            ibr.getHeaders().put("Content-Type", null);
        }
    }

    /**
     * Wrapper into postPage(importBrowser, page, postData), where browser == this.br;
     *
     * @author raztoki
     *
     * */
    public void postPage(String page, final String postData) throws Exception {
        postPage(br, page, postData);
    }

    public void sendForm(final Browser ibr, final Form form) throws Exception {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!prepBrSet) {
            prepBrowser(ibr);
        }
        // stable sucks && lame to the max, lets try and send a form outside of desired protocol. (works with oteupload)
        if (Form.MethodType.POST.equals(form.getMethod())) {
            // if the form doesn't contain an action lets set one based on current br.getURL().
            if (form.getAction() == null || form.getAction().equals("")) {
                form.setAction(ibr.getURL());
            }
            if (isJava7nJDStable() && (form.getAction().contains("https://") || /* relative path */(!form.getAction().startsWith("http")))) {
                if (!form.getAction().startsWith("http") && ibr.getURL().contains("https://")) {
                    // change relative path into full path, with protocol correction
                    String basepath = new Regex(ibr.getURL(), "(https?://.+)/[^/]+$").getMatch(0);
                    String basedomain = new Regex(ibr.getURL(), "(https?://[^/]+)").getMatch(0);
                    String path = form.getAction();
                    String finalpath = null;
                    if (path.startsWith("/")) {
                        finalpath = basedomain.replaceFirst("https://", "http://") + path;
                    } else if (!path.startsWith(".")) {
                        finalpath = basepath.replaceFirst("https://", "http://") + path;
                    } else {
                        // lacking builder for ../relative paths. this will do for now.
                        logger.info("Missing relative path builder. Must abort now... Try upgrading to JDownloader 2");
                        throw new PluginException(LinkStatus.ERROR_FATAL);
                    }
                    form.setAction(finalpath);
                } else {
                    form.setAction(form.getAction().replaceFirst("https?://", "http://"));
                }
                if (!stableSucks.get()) {
                    showSSLWarning(this.getHost());
                }
            }
            ibr.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        }
        URLConnectionAdapter con = null;
        try {
            con = ibr.openFormConnection(form);
            readConnection(con, ibr);
            antiDDoS(ibr);
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
            ibr.getHeaders().put("Content-Type", null);
        }
    }

    /**
     * Wrapper into sendForm(importBrowser, form), where browser == this.br;
     *
     * @author raztoki
     *
     * */
    public void sendForm(final Form form) throws Exception {
        sendForm(br, form);
    }

    private void sendRequest(final Browser ibr, final Request request) throws Exception {
        URLConnectionAdapter con = null;
        try {
            con = ibr.openRequestConnection(request);
            readConnection(con, ibr);
            antiDDoS(ibr);
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
            ibr.getHeaders().put("Content-Type", null);
        }
    }

    /**
     * Wrapper into sendRequest(importBrowser, form), where browser == this.br;
     *
     * @author raztoki
     *
     * */
    private void sendRequest(final Request request) throws Exception {
        sendRequest(br, request);
    }

    /**
     * @author razotki
     * @author jiaz
     * @param con
     * @param ibr
     * @throws IOException
     * @throws PluginException
     */
    private void readConnection(final URLConnectionAdapter con, final Browser ibr) throws IOException, PluginException {
        InputStream is = null;
        try {
            /* beta */
            try {
                con.setAllowedResponseCodes(new int[] { con.getResponseCode() });
            } catch (final Throwable e2) {
            }
            is = con.getInputStream();
        } catch (IOException e) {
            // stable fail over
            is = con.getErrorStream();
        }
        String t = readInputStream(is);
        if (t != null) {
            logger.fine("\r\n" + t);
            ibr.getRequest().setHtmlCode(t);
        }
    }

    /**
     * @author razotki
     * @author jiaz
     * @param is
     * @return
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private String readInputStream(final InputStream is) throws UnsupportedEncodingException, IOException {
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(is, "UTF8"));
            String line;
            final StringBuilder ret = new StringBuilder();
            final String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                }
                ret.append(line);
            }
            return ret.toString();
        } finally {
            try {
                is.close();
            } catch (final Throwable e) {
            }
        }
    }

    private int responseCode429 = 0;
    private int responseCode52x = 0;

    /**
     * Performs Cloudflare and Incapsula requirements.<br />
     * Auto fill out the required fields and updates antiDDoSCookies session.<br />
     * Always called after Browser Request!
     *
     * @version 0.03
     * @author raztoki
     **/
    private void antiDDoS(final Browser ibr) throws Exception {
        if (ibr == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HashMap<String, String> cookies = new HashMap<String, String>();
        if (ibr.getHttpConnection() != null) {
            final String URL = ibr.getURL();
            final int responseCode = ibr.getHttpConnection().getResponseCode();
            if (requestHeadersHasKeyNValueContains(ibr, "server", "cloudflare-nginx")) {
                Form cloudflare = ibr.getFormbyProperty("id", "ChallengeForm");
                if (cloudflare == null) {
                    cloudflare = ibr.getFormbyProperty("id", "challenge-form");
                }
                if (responseCode == 403 && cloudflare != null) {
                    // new method seems to be within 403
                    if (cloudflare.hasInputFieldByName("recaptcha_response_field")) {
                        // they seem to add multiple input fields which is most likely meant to be corrected by js ?
                        // we will manually remove all those
                        while (cloudflare.hasInputFieldByName("recaptcha_response_field")) {
                            cloudflare.remove("recaptcha_response_field");
                        }
                        while (cloudflare.hasInputFieldByName("recaptcha_challenge_field")) {
                            cloudflare.remove("recaptcha_challenge_field");
                        }
                        // this one is null, needs to be ""
                        if (cloudflare.hasInputFieldByName("message")) {
                            cloudflare.remove("message");
                            cloudflare.put("messsage", "\"\"");
                        }
                        // recaptcha bullshit
                        String apiKey = cloudflare.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                        if (apiKey == null) {
                            apiKey = ibr.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                            if (apiKey == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(ibr);
                        rc.setId(apiKey);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String response = getCaptchaCode("recaptcha", cf, param);
                        cloudflare.put("recaptcha_challenge_field", rc.getChallenge());
                        cloudflare.put("recaptcha_response_field", Encoding.urlEncode(response));
                        ibr.submitForm(cloudflare);
                        if (ibr.getFormbyProperty("id", "ChallengeForm") != null || ibr.getFormbyProperty("id", "challenge-form") != null) {
                            logger.warning("Possible plugin error within cloudflare handling");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                } else if (responseCode == 503 && cloudflare != null) {
                    // 503 response code with javascript math section
                    final String[] line1 = ibr.getRegex("var t,r,a,f, (\\w+)=\\{\"(\\w+)\":([^\\}]+)").getRow(0);
                    String line2 = ibr.getRegex("(\\;" + line1[0] + "." + line1[1] + ".*?t\\.length\\;)").getMatch(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("var a={};\r\nvar t=\"" + ibr.getHost() + "\";\r\n");
                    sb.append("var " + line1[0] + "={\"" + line1[1] + "\":" + line1[2] + "}\r\n");
                    sb.append(line2);

                    ScriptEngineManager mgr = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
                    ScriptEngine engine = mgr.getEngineByName("JavaScript");
                    long answer = ((Number) engine.eval(sb.toString())).longValue();
                    cloudflare.getInputFieldByName("jschl_answer").setValue(answer + "");
                    Thread.sleep(5500);
                    ibr.submitForm(cloudflare);
                    if (ibr.getFormbyProperty("id", "ChallengeForm") != null || ibr.getFormbyProperty("id", "challenge-form") != null) {
                        logger.warning("Possible plugin error within cloudflare handling");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else if (responseCode == 520 || responseCode == 522) {
                    // HTTP/1.1 520 Origin Error
                    // HTTP/1.1 522 Origin Connection Time-out
                    // cache system with possible origin dependency... we will wait and retry
                    if (responseCode52x == 4) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
                    }
                    // this html based cookie, set by <meta (for responseCode 522)
                    // <meta http-equiv="set-cookie" content="cf_use_ob=0; expires=Sat, 14-Jun-14 14:35:38 GMT; path=/">
                    String[] metaCookies = ibr.getRegex("<meta http-equiv=\"set-cookie\" content=\"(.*?; expries=.*?; path=.*?\";?(?: domain=.*?;?)?)\"").getColumn(0);
                    if (metaCookies != null && metaCookies.length != 0) {
                        final List<String> cookieHeaders = Arrays.asList(metaCookies);
                        final String date = ibr.getHeaders().get("Date");
                        final String host = Browser.getHost(ibr.getURL());
                        // get current cookies
                        final Cookies ckies = ibr.getCookies(host);
                        // add meta cookies to current previous request cookies
                        for (int i = 0; i < cookieHeaders.size(); i++) {
                            final String header = cookieHeaders.get(i);
                            ckies.add(Cookies.parseCookies(header, host, date));
                        }
                        // set ckies as current cookies
                        ibr.getHttpConnection().getRequest().setCookies(ckies);
                    }

                    Thread.sleep(2500);
                    // effectively refresh page!
                    try {
                        sendRequest(ibr, ibr.getRequest().cloneRequest());
                    } catch (final Throwable t) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
                    }
                    // new sendRequest saves.
                    return;
                } else if (responseCode == 429 && ibr.containsHTML("<title>Too Many Requests</title>")) {
                    if (responseCode429 == 4) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
                    }
                    responseCode429++;
                    // been blocked! need to wait 1min before next request. (says k2sadmin, each site could be configured differently)
                    Thread.sleep(61000);
                    // try again! -NOTE: this isn't stable compliant-
                    try {
                        sendRequest(ibr, ibr.getRequest().cloneRequest());
                    } catch (final Throwable t) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
                    }
                    return;

                    // new code here...
                    // <script type="text/javascript">
                    // //<![CDATA[
                    // try{if (!window.CloudFlare) {var
                    // CloudFlare=[{verbose:0,p:1408958160,byc:0,owlid:"cf",bag2:1,mirage2:0,oracle:0,paths:{cloudflare:"/cdn-cgi/nexp/dokv=88e434a982/"},atok:"661da6801927b0eeec95f9f3e160b03a",petok:"107d6db055b8700cf1e7eec1324dbb7be6b978d0-1408974417-1800",zone:"fileboom.me",rocket:"0",apps:{}}];CloudFlare.push({"apps":{"ape":"3a15e211d076b73aac068065e559c1e4"}});!function(a,b){a=document.createElement("script"),b=document.getElementsByTagName("script")[0],a.async=!0,a.src="//ajax.cloudflare.com/cdn-cgi/nexp/dokv=97fb4d042e/cloudflare.min.js",b.parentNode.insertBefore(a,b)}()}}catch(e){};
                    // //]]>
                    // </script>

                } else if (ibr.containsHTML("<p>The owner of this website \\(" + Pattern.quote(br.getHost()) + "\\) has banned your IP address") && ibr.containsHTML("<title>Access denied \\| " + Pattern.quote(ibr.getHost()) + " used CloudFlare to restrict access</title>")) {
                    // common when proxies are used?? see keep2share.cc jdlog://5562413173041
                    String ip = ibr.getRegex("your IP address \\((.*?)\\)\\.</p>").getMatch(0);
                    String message = ibr.getHost() + " has banned your IP Address" + (inValidate(ip) ? "!" : "! " + ip);
                    logger.warning(message);
                    throw new PluginException(LinkStatus.ERROR_FATAL, message);
                } else {
                    // nothing wrong, or something wrong (unsupported format)....
                    // commenting out return prevents caching of cookies per request
                    // return;
                }

                // get cookies we want/need.
                // refresh these with every getPage/postPage/submitForm?
                final Cookies add = ibr.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    if (new Regex(c.getKey(), "(cfduid|cf_clearance)").matches()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                }
            }
            // save the session!
            synchronized (antiDDoSCookies) {
                antiDDoSCookies.clear();
                antiDDoSCookies.putAll(cookies);
            }
        }
    }

    /**
     *
     * @author raztoki
     * */
    @SuppressWarnings("unused")
    private boolean requestHeadersHasKeyNValueStartsWith(final Browser ibr, final String k, final String v) {
        if (k == null || v == null || ibr == null || ibr.getHttpConnection() == null) {
            return false;
        }
        if (ibr.getHttpConnection().getHeaderField(k) != null && ibr.getHttpConnection().getHeaderField(k).toLowerCase(Locale.ENGLISH).startsWith(v.toLowerCase(Locale.ENGLISH))) {
            return true;
        }
        return false;
    }

    /**
     *
     * @author raztoki
     * */
    private boolean requestHeadersHasKeyNValueContains(final Browser ibr, final String k, final String v) {
        if (k == null || v == null || ibr == null || ibr.getHttpConnection() == null) {
            return false;
        }
        if (ibr.getHttpConnection().getHeaderField(k) != null && ibr.getHttpConnection().getHeaderField(k).toLowerCase(Locale.ENGLISH).contains(v.toLowerCase(Locale.ENGLISH))) {
            return true;
        }
        return false;
    }

    // stable browser is shite.

    private boolean isJava7nJDStable() {
        if (!isNewJD() && System.getProperty("java.version").matches("1\\.[7-9].+")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isNewJD() {
        return System.getProperty("jd.revision.jdownloaderrevision") != null ? true : false;
    }

    private static Object        DIALOGLOCK  = new Object();

    private static AtomicBoolean stableSucks = new AtomicBoolean(false);

    public void showSSLWarning(final String domain) {
        synchronized (DIALOGLOCK) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            String message = null;
                            String title = null;
                            boolean xSystem = CrossSystem.isOpenBrowserSupported();
                            if ("de".equalsIgnoreCase(lng)) {
                                title = domain + " :: Java 7+ && HTTPS Post Requests.";
                                message = "Wegen einem Bug in in Java 7+ in dieser JDownloader version koennen wir keine HTTPS Post Requests ausfuehren.\r\n";
                                message += "Wir haben eine Notloesung ergaenzt durch die man weiterhin diese JDownloader Version nutzen kann.\r\n";
                                message += "Bitte bedenke, dass HTTPS Post Requests als HTTP gesendet werden. Nutzung auf eigene Gefahr!\r\n";
                                message += "Falls du keine unverschluesselten Daten versenden willst, update bitte auf JDownloader 2!\r\n";
                                if (xSystem) {
                                    message += "JDownloader 2 Installationsanleitung und Downloadlink: Klicke -OK- (per Browser oeffnen)\r\n ";
                                } else {
                                    message += "JDownloader 2 Installationsanleitung und Downloadlink:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                                }
                            } else if ("es".equalsIgnoreCase(lng)) {
                                title = domain + " :: Java 7+ && HTTPS Solicitudes Post.";
                                message = "Debido a un bug en Java 7+, al utilizar esta versión de JDownloader, no se puede enviar correctamente las solicitudes Post en HTTPS\r\n";
                                message += "Por ello, hemos añadido una solución alternativa para que pueda seguir utilizando esta versión de JDownloader...\r\n";
                                message += "Tenga en cuenta que las peticiones Post de HTTPS se envían como HTTP. Utilice esto a su propia discreción.\r\n";
                                message += "Si usted no desea enviar información o datos desencriptados, por favor utilice JDownloader 2!\r\n";
                                if (xSystem) {
                                    message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación: Hacer Click en -Aceptar- (El navegador de internet se abrirá)\r\n ";
                                } else {
                                    message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación, enlace :\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                                }
                            } else {
                                title = domain + " :: Java 7+ && HTTPS Post Requests.";
                                message = "Due to a bug in Java 7+ when using this version of JDownloader, we can not successfully send HTTPS Post Requests.\r\n";
                                message += "We have added a work around so you can continue to use this version of JDownloader...\r\n";
                                message += "Please be aware that HTTPS Post Requests are sent as HTTP. Use at your own discretion.\r\n";
                                message += "If you do not want to send unecrypted data, please upgrade to JDownloader 2!\r\n";
                                if (xSystem) {
                                    message += "Jdownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                                } else {
                                    message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                                }
                            }
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                            if (xSystem && JOptionPane.OK_OPTION == result) {
                                CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                            }
                            stableSucks.set(true);
                        } catch (Throwable e) {
                        }
                    }
                });
            } catch (Throwable e) {
            }
        }
    }

    private String lng = getLanguage();

    private String getLanguage() {
        try {
            if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
                return org.appwork.txtresource.TranslationFactory.getDesiredLocale().getLanguage().toLowerCase(Locale.ENGLISH);
            } else {
                return System.getProperty("user.language");
            }
        } catch (final Throwable ignore) {
            return System.getProperty("user.language");
        }
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

}
