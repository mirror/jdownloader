//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pururin.com" }, urls = { "^http://(www\\.)?pururin\\.com/view/\\d+/\\d+/[\\-a-z0-9]+_\\d+\\.html$" }, flags = { 0 })
public class PururinCom extends PluginForHost {

    // raztoki embed video player template.

    private String dllink = null;

    /**
     * @author raztoki
     * */
    public PururinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pururin.com/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        getPage(downloadLink.getDownloadURL());
        dllink = br.getRegex("\"(/f/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.urlDecode(dllink, false);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            // only way to check for made up links... or offline is here
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                int links_length = downloadLink.getIntegerProperty("links_length", 1);
                DecimalFormat df_links = new DecimalFormat("00");
                if (links_length > 999) {
                    df_links = new DecimalFormat("0000");
                } else if (links_length > 99) {
                    df_links = new DecimalFormat("000");
                }
                // now we have a connection to the link we should format again! and provide proper file extension.
                final String[] fn = new Regex(getFileNameFromHeader(con), "([^/]+)(_|-)(\\d+)(\\.[a-z0-9]{3,4})$").getRow(0);
                downloadLink.setFinalFileName(fn[0] + "-" + df_links.format(Integer.parseInt(fn[2])) + fn[3]);
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private boolean                        prepBrSet       = false;
    private int                            responseCode52x = 0;
    private static AtomicReference<String> userAgent       = new AtomicReference<String>(null);
    private static HashMap<String, String> antiDDoSCookies = new HashMap<String, String>();

    private Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        // required for native cloudflare support, without the need to repeat requests.
        try {
            /* not available in old stable */
            prepBr.setAllowedResponseCodes(new int[] { 503, 520, 522 });
        } catch (Throwable e) {
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
        if (userAgent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", userAgent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setConnectTimeout(90 * 1000);
        prepBrSet = true;
        return prepBr;
    }

    /**
     * Gets page <br />
     * - natively supports silly cloudflare anti DDoS crapola
     * 
     * @author raztoki
     */
    public void getPage(final String page) throws Exception {
        if (page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!prepBrSet) {
            prepBrowser(br);
        }
        final boolean follows_redirects = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            try {
                br.getPage(page);
            } catch (Exception e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
                // should only be picked up now if not JD2
                if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 503 && br.getHttpConnection().getHeaderField("server") != null && br.getHttpConnection().getHeaderField("server").toLowerCase(Locale.ENGLISH).contains("cloudflare-nginx")) {
                    logger.warning("Cloudflare anti DDoS measures enabled, your version of JD can not support this. In order to go any further you will need to upgrade to JDownloader 2");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Cloudflare anti DDoS measures enabled");
                } else {
                    throw e;
                }
            }
            antiDDoS();
        } finally {
            br.setFollowRedirects(follows_redirects);
        }
    }

    public void postPage(String page, final String postData) throws Exception {
        if (page == null || postData == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!prepBrSet) {
            prepBrowser(br);
        }
        // stable sucks
        if (isJava7nJDStable() && page.startsWith("https")) {
            page = page.replaceFirst("https://", "http://");
        }
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        try {
            br.postPage(page, postData);
            antiDDoS();
        } finally {
            br.getHeaders().put("Content-Type", null);
        }
    }

    public void sendForm(final Form form) throws Exception {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!prepBrSet) {
            prepBrowser(br);
        }
        // stable sucks && lame to the max, lets try and send a form outside of desired protocol. (works with oteupload)
        if (Form.MethodType.POST.equals(form.getMethod())) {
            // if the form doesn't contain an action lets set one based on current br.getURL().
            if (form.getAction() == null || form.getAction().equals("")) {
                form.setAction(br.getURL());
            }
            if (isJava7nJDStable() && (form.getAction().contains("https://") || /* relative path */(!form.getAction().startsWith("http")))) {
                if (!form.getAction().startsWith("http") && br.getURL().contains("https://")) {
                    // change relative path into full path, with protocol correction
                    String basepath = new Regex(br.getURL(), "(https?://.+)/[^/]+$").getMatch(0);
                    String basedomain = new Regex(br.getURL(), "(https?://[^/]+)").getMatch(0);
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
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        }
        try {
            br.submitForm(form);
            antiDDoS();
        } finally {
            br.getHeaders().put("Content-Type", null);
        }
    }

    /**
     * Performs Cloudflare and Incapsula requirements.<br />
     * Auto fill out the required fields and updates antiDDoSCookies session.<br />
     * Always called after Browser Request!
     * 
     * @version 0.03
     * @author raztoki
     **/
    private void antiDDoS() throws Exception {
        if (br == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HashMap<String, String> cookies = new HashMap<String, String>();
        if (br.getHttpConnection() != null) {
            final String URL = br.getURL();
            final int responseCode = br.getHttpConnection().getResponseCode();
            if (br.getHttpConnection().getHeaderField("server") != null && br.getHttpConnection().getHeaderField("server").toLowerCase(Locale.ENGLISH).contains("cloudflare-nginx")) {
                Form cloudflare = br.getFormbyProperty("id", "ChallengeForm");
                if (cloudflare == null) {
                    cloudflare = br.getFormbyProperty("id", "challenge-form");
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
                            apiKey = br.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                            if (apiKey == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        final DownloadLink dllink = new DownloadLink(null, "antiDDoS Provider 'Clouldflare' requires Captcha", this.getHost(), this.getHost(), true);
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.setId(apiKey);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String response = getCaptchaCode(cf, dllink);
                        cloudflare.put("recaptcha_challenge_field", rc.getChallenge());
                        cloudflare.put("recaptcha_response_field", Encoding.urlEncode(response));
                        br.submitForm(cloudflare);
                        if (br.getFormbyProperty("id", "ChallengeForm") != null || br.getFormbyProperty("id", "challenge-form") != null) {
                            logger.warning("Possible plugin error within cloudflare handling");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                } else if (responseCode == 503 && cloudflare != null) {
                    // 503 response code with javascript math section
                    String host = new Regex(URL, "https?://([^/]+)(:\\d+)?/").getMatch(0);
                    String math = br.getRegex("\\$\\('#jschl_answer'\\)\\.val\\(([^\\)]+)\\);").getMatch(0);
                    if (math == null) {
                        math = br.getRegex("a\\.value = ([\\d\\-\\.\\+\\*/]+);").getMatch(0);
                    }
                    if (math == null) {
                        String variableName = br.getRegex("(\\w+)\\s*=\\s*\\$\\('#jschl_answer'\\);").getMatch(0);
                        if (variableName != null) {
                            variableName = variableName.trim();
                        }
                        math = br.getRegex(variableName + "\\.val\\(([^\\)]+)\\)").getMatch(0);
                    }
                    if (math == null) {
                        logger.warning("Couldn't find 'math'");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    // use js for now, but change to Javaluator as the provided string doesn't get evaluated by JS according to Javaluator
                    // author.
                    ScriptEngineManager mgr = new ScriptEngineManager();
                    ScriptEngine engine = mgr.getEngineByName("JavaScript");
                    final long value = ((Number) engine.eval("(" + math + ") + " + host.length())).longValue();
                    cloudflare.put("jschl_answer", value + "");
                    Thread.sleep(5500);
                    br.submitForm(cloudflare);
                    if (br.getFormbyProperty("id", "ChallengeForm") != null || br.getFormbyProperty("id", "challenge-form") != null) {
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
                    String[] metaCookies = br.getRegex("<meta http-equiv=\"set-cookie\" content=\"(.*?; expries=.*?; path=.*?\";?(?: domain=.*?;?)?)\"").getColumn(0);
                    if (metaCookies != null && metaCookies.length != 0) {
                        final List<String> cookieHeaders = Arrays.asList(metaCookies);
                        final String date = br.getHeaders().get("Date");
                        final String host = Browser.getHost(br.getURL());
                        // get current cookies
                        final Cookies ckies = br.getCookies(host);
                        // add meta cookies to current previous request cookies
                        for (int i = 0; i < cookieHeaders.size(); i++) {
                            final String header = cookieHeaders.get(i);
                            ckies.add(Cookies.parseCookies(header, host, date));
                        }
                        // set ckies as current cookies
                        br.getHttpConnection().getRequest().setCookies(ckies);
                    }
                    responseCode52x++;
                    Thread.sleep(2500);
                    getPage(br.getURL());
                } else {
                    // nothing wrong, or something wrong (unsupported format)....
                    // commenting out return prevents caching of cookies per request
                    // return;
                }
                // get cookies we want/need.
                // refresh these with every getPage/postPage/submitForm?
                final Cookies add = br.getCookies(this.getHost());
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
            if (responseCode52x != 0) {
                responseCode52x = 0;
            }
        }
    }

    private boolean isJava7nJDStable() {
        if (System.getProperty("jd.revision.jdownloaderrevision") == null && System.getProperty("java.version").matches("1\\.[7-9].+")) {
            return true;
        } else {
            return false;
        }
    }

    private static AtomicBoolean stableSucks = new AtomicBoolean(false);

    public static void showSSLWarning(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}