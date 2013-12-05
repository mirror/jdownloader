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

package jd.plugins.hoster;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.net" }, urls = { "http://(www\\.|u\\d+\\.)?letitbit\\.net/d?download/.*?\\.html" }, flags = { 2 })
public class LetitBitNet extends PluginForHost {

    private static Object        LOCK                              = new Object();
    private final String         COOKIE_HOST                       = "http://letitbit.net/";
    private static AtomicInteger maxFree                           = new AtomicInteger(1);
    private static final String  ENABLEUNLIMITEDSIMULTANMAXFREEDLS = "ENABLEUNLIMITEDSIMULTANMAXFREEDLS";
    /*
     * For linkcheck and premium download we're using their API: http://api.letitbit.net/reg/static/api.pdf
     */
    public final String          APIKEY                            = "VjR1U3JGUkNx";
    public final String          APIPAGE                           = "http://api.letitbit.net/";
    private final String         TEMPDISABLED                      = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|>File not found<|id=\"videocaptcha_)";
    private String               agent                             = null;
    private static final boolean PLUGIN_BROKEN                     = false;
    private static Object        PREMIUMLOCK                       = new Object();

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        setAccountwithoutUsername(true);
        enablePremium("http://letitbit.net/page/premium.php");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* convert direct download links to normal links */
        link.setUrlDownload(link.getDownloadURL().replaceAll("/ddownload", "/download").replaceAll("\\?", "%3F").replace("www.", "").replaceAll("http://s\\d+.", "http://"));
    }

    private static void showFreeDialog(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        String tab = "                        ";
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download";
                            message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = domain + " Free Download";
                            message = "You are using the " + domain + " Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkShowFreeDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                if (config.getProperty("premAdShown2") == null) {
                    File checkFile = JDUtilities.getResourceFile("tmp/libit");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("letitbit.net");
                    }
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("premAdShown", Boolean.TRUE);
                config.setProperty("premAdShown2", "shown");
                config.save();
            }
        }
    }

    private static void showFreeBrokenDialog(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        final String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download ist zurzeit defekt";
                            message = "Du versuchst gerade im kostenlosen Modus von " + domain + " zu laden.\r\n";
                            message += "Leider ist dieses Feature zurzeit und evtl. auch auf längere Sicht defekt!\r\n";
                            message += "Der premium Modus sollte allerdings weiterhin problemlos funktionieren.\r\n";
                            message += "Den aktuellen Status zu diesem Problem findest du in unserem Supportforum.\r\n";
                            message += "Willst du den dazugehörigen Forenthread anschauen?\r\n";
                        } else {
                            title = domain + " Free Download is broken at the moment";
                            message = "You're trying to use the  " + domain + " free fode.\r\n";
                            message += "Unfortunately, this feature is broken at the moment and probably won't be fixed in the near future!\r\n";
                            message += "However, the premium mode should work fine.\r\n";
                            message += "You can find the current status of this issue in our support forum.\r\n";
                            message += "Do you want to view the forum thread about this issue?\r\n";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=47255"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkShowFreeBrokenDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("freeBrokenShown", Boolean.FALSE) == false) {
                if (config.getProperty("freeBrokenShown2") == null) {
                    showFreeBrokenDialog("letitbit.net");
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("freeBrokenShown", Boolean.TRUE);
                config.setProperty("freeBrokenShown2", "shown");
                config.save();
            }
        }
    }

    /**
     * Important: Always sync this code with the vip-file.com, shareflare.net and letitbit.net plugins Limits: 20 * 50 = 1000 links per minute
     * */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            prepBrowser(br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /*
                     * we test 50 links at once (probably we could check even more)
                     */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("r=" + Encoding.urlEncode("[\"" + Encoding.Base64Decode(APIKEY) + "\""));
                for (final DownloadLink dl : links) {
                    sb.append(",[\"download/info\",{\"link\":\"" + dl.getDownloadURL() + "\"}]");
                }
                sb.append("]");
                br.setReadTimeout(2 * 60 * 60);
                br.setConnectTimeout(2 * 60 * 60);
                br.postPage(APIPAGE, sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = getFID(dllink);
                    final Regex fInfo = br.getRegex("\"name\":\"([^<>\"]*?)\",\"size\":(\")?(\\d+)(\")?,\"uid\":\"" + fid + "\",\"project\":\"(letitbit\\.net|shareflare\\.net|vip\\-file\\.com)\",\"md5\":\"([a-z0-9]{32}|0)\"");
                    if (br.containsHTML("\"data\":\\[\\[\\]\\]")) {
                        dllink.setAvailable(false);
                    } else {
                        final String md5 = fInfo.getMatch(5);
                        dllink.setFinalFileName(Encoding.htmlDecode(fInfo.getMatch(0)));
                        dllink.setDownloadSize(Long.parseLong(fInfo.getMatch(2)));
                        dllink.setAvailable(true);
                        if (!md5.equals("0")) dllink.setMD5Hash(md5);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([^<>\"/]*?)/[^<>\"/]+\\.html").getMatch(0);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        checkShowFreeDialog();
        if (PLUGIN_BROKEN) {
            checkShowFreeBrokenDialog();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            br.setVerbose(true);
        } catch (Throwable e) {
            /* only available after 0.9xx version */
        }
        maxFree.set(1);
        if (getPluginConfig().getBooleanProperty(ENABLEUNLIMITEDSIMULTANMAXFREEDLS, false)) maxFree.set(-1);
        requestFileInformation(downloadLink);
        String url = getLinkViaSkymonkDownloadMethod(downloadLink.getDownloadURL());
        if (url == null) {
            url = handleFreeFallback(downloadLink);
        } else {
            // Enable unlimited simultan downloads for skymonk users
            maxFree.set(-1);
        }
        if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        if (!dl.getConnection().isOK()) {
            dl.getConnection().disconnect();
            if (dl.getConnection().getResponseCode() == 404) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000); }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("<title>Error</title>") || br.containsHTML("Error")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://letitbit.net/page/terms.php";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    private String getLinkViaSkymonkDownloadMethod(String s) throws IOException {
        if (!getPluginConfig().getBooleanProperty("STATUS", true)) return null;
        Browser skymonk = new Browser();
        skymonk.setCustomCharset("UTF-8");
        skymonk.getHeaders().put("Pragma", null);
        skymonk.getHeaders().put("Cache-Control", null);
        skymonk.getHeaders().put("Accept-Charset", null);
        skymonk.getHeaders().put("Accept-Encoding", null);
        skymonk.getHeaders().put("Accept", "*/*");
        skymonk.getHeaders().put("Accept-Language", "en-EN");
        skymonk.getHeaders().put("User-Agent", null);
        skymonk.getHeaders().put("Referer", null);
        skymonk.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");

        skymonk.postPage("http://api.letitbit.net/internal/index4.php", "action=LINK_GET_DIRECT&link=" + s + "&free_link=1&appid=" + JDHash.getMD5(String.valueOf(Math.random())) + "&version=2.1");
        String[] result = skymonk.getRegex("([^\r\n]+)").getColumn(0);
        if (result == null || result.length == 0) return null;

        if ("NO".equals(result[0].trim())) {
            if (result.length > 1) {
                if ("activation".equals(result[1].trim())) {
                    logger.warning("SkyMonk activation not completed!");
                }
            }
        }

        ArrayList<String> res = new ArrayList<String>();
        for (String r : result) {
            if (r.startsWith("http")) {
                res.add(r);
            }
        }
        if (res.size() > 1) return res.get(1);
        return res.size() == 1 ? res.get(0) : null;
    }

    private String handleFreeFallback(final DownloadLink downloadLink) throws Exception {
        String url = null;
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        handleNonApiErrors(downloadLink);
        submitFreeForm();// born_iframe.php with encrypted form

        // Russians can get the downloadlink here, they don't have to enter captchas
        final String finalLinksText = br.getRegex("eval\\(\\'var _direct_links = new Array\\((\".*?)\\);").getMatch(0);
        if (finalLinksText != null) {
            logger.info("Entering russian handling...");
            url = this.getFinalLink(finalLinksText);
            if (url == null) {
                logger.warning("Russian handling failed...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            String urlPrefix = new Regex(br.getURL(), "http://(www\\.)?([a-z0-9]+\\.)letitbit\\.net/.+").getMatch(1);
            if (urlPrefix == null) urlPrefix = "";
            final String ajaxmainurl = "http://" + urlPrefix + "letitbit.net";

            String dlFunction = getdlFunction();
            if (dlFunction == null) {
                if (!submitFreeForm()) logger.info("letitbit.net: plain form processing --> download3.php");
                if ((dlFunction = getdlFunction()) == null) {
                    if (!submitFreeForm()) logger.info("letitbit.net: encrypted form processing --> download3.php");
                    if ((dlFunction = getdlFunction()) == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }

            String ajaxPostpage = new Regex(dlFunction, "\\$\\.post\\(\"(/ajax/[^<>\"]*?)\"").getMatch(0);
            if (ajaxPostpage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            ajaxPostpage = ajaxmainurl + ajaxPostpage;
            int wait = 60;
            String waittime = br.getRegex("id=\"seconds\" style=\"font\\-size:18px\">(\\d+)</span>").getMatch(0);
            if (waittime == null) waittime = br.getRegex("seconds = (\\d+)").getMatch(0);
            if (waittime != null) {
                logger.info("Waittime found, waittime is " + waittime + " seconds .");
                wait = Integer.parseInt(waittime);
            } else {
                logger.info("No waittime found, continuing...");
            }
            sleep((wait + 1) * 1001l, downloadLink);
            final Browser br2 = br.cloneBrowser();
            prepareBrowser(br2);
            /*
             * this causes issues in 09580 stable, no workaround known, please update to latest jd version
             */
            br2.getHeaders().put("Content-Length", "0");
            br2.postPage(ajaxmainurl + "/ajax/download3.php", "");
            br2.getHeaders().remove("Content-Length");
            /* we need to remove the newline in old browser */
            final String resp = br2.toString().replaceAll("%0D%0A", "").trim();
            if (!"1".equals(resp)) {
                if (br2.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // reCaptcha handling
            if (ajaxPostpage.contains("recaptcha")) {
                final String rcControl = new Regex(dlFunction, "var recaptcha_control_field = \\'([^<>\"]*?)\\'").getMatch(0);
                String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
                if (rcID == null) rcID = Encoding.Base64Decode("NkxjOXpkTVNBQUFBQUYtN3Myd3VRLTAzNnBMUmJNMHA4ZERhUWRBTQ==");
                if (rcID == null || rcControl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId(rcID);
                rc.load();
                for (int i = 0; i <= 5; i++) {
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode(cf, downloadLink);
                    br2.postPage(ajaxPostpage, "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&recaptcha_control_field=" + Encoding.urlEncode(rcControl));
                    if (br2.toString().length() < 2 || br2.toString().contains("error_wrong_captcha")) {
                        rc.reload();
                        continue;
                    }
                    break;
                }
                if (br2.toString().length() < 2 || br2.toString().contains("error_wrong_captcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                // Normal captcha handling, UNTESTED!
                final DecimalFormat df = new DecimalFormat("0000");
                for (int i = 0; i <= 5; i++) {
                    final String code = getCaptchaCode("letitbitnew", ajaxmainurl + "/captcha_new.php?rand=" + df.format(new Random().nextInt(1000)), downloadLink);
                    sleep(2000, downloadLink);
                    br2.postPage(ajaxPostpage, "code=" + Encoding.urlEncode(code));
                    if (br2.toString().contains("error_wrong_captcha")) continue;
                    break;
                }
                if (br2.toString().contains("error_wrong_captcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            // Download limit is per day so let's just wait 3 hours
            if (br2.containsHTML("error_free_download_blocked")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 3 * 60 * 60 * 1000l);
            if (br2.containsHTML("callback_file_unavailable")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            url = getFinalLink(br2.toString());
            if (url == null || url.length() > 1000 || !url.startsWith("http")) {
                if (br2.containsHTML("error_free_download_blocked")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Free download blocked", 2 * 60 * 60 * 1000l); }
                logger.warning("url couldn't be found!");
                logger.severe(url);
                logger.severe(br2.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        // This should never happen
        if (url.length() > 1000 || !url.startsWith("http")) {
            logger.warning("The found finallink is invalid...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* we have to wait little because server too buggy */
        sleep(2000, downloadLink);
        /* remove newline */
        return url.replaceAll("%0D%0A", "").trim().replace("\\", "");
    }

    private String getdlFunction() {
        return br.getRegex("function getLink\\(\\)(.*?)</script>").getMatch(0);
    }

    private boolean submitFreeForm() throws Exception {
        // this finds the form to "click" on the next "free download" button
        Form down = null;
        for (Form singleform : br.getForms()) {
            // id=\"phone_submit_form\" is in the 2nd free form when you have a russian IP
            if (singleform.getAction() != null) {
                if (!"".equals(singleform.getAction())) {
                    if (singleform.containsHTML("class=\"Instead_parsing_Use_API_Luke\"")) decryptingForm(singleform);
                    if (singleform.getInputField("md5crypt") != null || "/download3.php".equals(singleform.getAction())) {
                        if (!singleform.containsHTML("/sms/check") && !singleform.containsHTML("id=\"phone_submit_form\"")) {
                            down = singleform;
                            break;
                        }
                    }
                }
            }
        }
        if (down == null) return false;
        br.submitForm(down);
        return true;
    }

    private void decryptingForm(Form encryptedForm) {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");

        HashMap<String, String> encValues = new HashMap<String, String>(getFormIds(encryptedForm));
        HashMap<String, String> inputFieldMap = new HashMap<String, String>();

        String js = encryptedForm.getRegex("(eval.*?)</script>").getMatch(0);
        String jsFn[] = new Regex(js, "eval\\((?!function)(.*?\\))").getColumn(0);
        for (String s : jsFn) {
            js = js.replace("eval(" + s + ")", "");
        }

        try {
            /* preload important methods */
            for (String pJs : js.split(";;")) {
                engine.eval(pJs);
            }
            /* create decrypt method */
            StringBuilder explainJSPForm = new StringBuilder();
            explainJSPForm.append("function explainJSPForm(_19, _1a, _1b, _1c, _1d, mus) {\n");
            explainJSPForm.append("var _1f = function (id) {return String(encValues.get(id));};\n");
            explainJSPForm.append("var _20 = function (ids) {\n");
            explainJSPForm.append("var r = [];\n");
            explainJSPForm.append("for (var i = 0; i < ids.length; ++i) {r.push(_1f(ids[i]));}\n");
            explainJSPForm.append("return r.join(\"\");};\n");
            explainJSPForm.append("_1c = _1c.split(\";\");\n");
            explainJSPForm.append("for (var i = 0; i < _1c.length; ++i) {\n");
            explainJSPForm.append("var _21 = _1c[i].split(\"=\");\n");
            explainJSPForm.append("if (_21.length == 1) {_21[1] = \"\";}\n");
            explainJSPForm.append("var k = _20(_21[0].split(\",\"));\n");
            explainJSPForm.append("var v = _20(_21[1].split(\",\"));\n");
            explainJSPForm.append("k = mus(k, _1d);\n");
            explainJSPForm.append("v = mus(v, _1d);\n");
            explainJSPForm.append("formMap.put(k,v);\n");
            explainJSPForm.append("}};\n");

            /* parsing decrypt call function */
            String callMethod = jsbeautifier(engine.eval(jsFn[jsFn.length - 1]).toString());
            if (callMethod == null) return;

            engine.put("encValues", encValues);
            engine.put("formMap", inputFieldMap);
            engine.eval(explainJSPForm.toString());
            engine.put("pass", getFormKey());
            // engine.put("br", br);
            // $.cookie(key) --> call Java Method br.getCookie(key);
            // engine.eval("var $ = { cookie : function(a) { return String(unescape(br.getCookie(br.getHost(), a))); }}");

            /* decrypting Form */
            engine.eval(callMethod);
            /* remove encrypted Inputfields */
            final Iterator<InputField> it = encryptedForm.getInputFields().iterator();
            while (it.hasNext()) {
                final InputField ipf = it.next();
                if (ipf.getKey() == null) it.remove();
            }
            /* put decrypted Inputfields into final form */
            for (Entry<String, String> next : inputFieldMap.entrySet()) {
                encryptedForm.put(next.getKey(), next.getValue());
            }
        } catch (Throwable e) {
            logger.warning("letitbit: decrypting form --> FAILED!");
            e.printStackTrace();
        }
        /* done :-) */
    }

    private String getFormKey() {
        File image;
        String fileName = "captchas/letitbit_keyImage_" + System.currentTimeMillis() + new Random().nextInt(1000000);
        final Browser dlpic = br.cloneBrowser();
        String key = null;
        try {
            try {
                image = Application.getResource(fileName);
            } catch (Throwable e) {
                image = JDUtilities.getResourceFile(fileName);
            }
            image.deleteOnExit();
            Browser.download(image, dlpic.openGetConnection(new Regex(br.getBaseURL(), "(^.*\\.net)").getMatch(0) + "/jspimggen.php?n=jspcid&r=" + Math.random()));
            BufferedImage img = ImageIO.read(image);

            int w = img.getWidth();
            int pixels[] = img.getRGB(0, 0, w, 1, null, 0, w);
            int k[] = new int[pixels.length];
            for (int i = 0; i < w; i++) {
                k[i] = (pixels[i] >> 16) & 0xff;// Red
            }
            key = new String(k, 0, k.length);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return key;
    }

    private String jsbeautifier(String s) {
        String extract = new Regex(s, "(var jac\\d+.*?\\);)").getMatch(0);
        if (extract == null) return null;
        return extract;
    }

    private HashMap<String, String> getFormIds(Form encForm) {
        HashMap<String, String> ret = new HashMap<String, String>();
        String key = null;
        for (final InputField ipf : encForm.getInputFields()) {
            try {
                key = ipf.get("id");
            } catch (Throwable e) {
                key = ipf.getProperty("id", null) != null ? ipf.getProperty("id", null).toString() : null;
            }
            if (key == null) {
                try {
                    key = ipf.get("ID");
                } catch (Throwable e) {
                    key = ipf.getProperty("ID", null) != null ? ipf.getProperty("ID", null).toString() : null;
                }
            }
            if (key == null) continue;
            ret.put(key, ipf.getValue());
        }
        return ret;
    }

    private String getFinalLink(final String source) throws PluginException {
        LinkedList<String> finallinksx = new LinkedList<String>();
        String[] finallinks = new Regex(source, "\"(http:[^<>\"]*?)\"").getColumn(0);
        // More common for shareflare.net
        if ((finallinks == null || finallinks.length == 0) && source.length() < 500) finallinks = new Regex(source, "(http:[^<>\"].+)").getColumn(0);
        if (finallinks == null || finallinks.length == 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        for (final String finallink : finallinks) {
            if (!finallinksx.contains(finallink) && finallink.startsWith("http")) finallinksx.add(finallink);
        }
        // Grab last links, this might changes and has to be fixed if users get
        // "server error" in JD while it's working via browser. If this
        // is changed often we should consider trying the whole list of
        // finallinks.
        return finallinksx.peekLast();
    }

    private Map<String, String> login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                this.setBrowserExclusive();
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof Map<?, ?> && !force) {
                    final Map<String, String> cookies = (Map<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return cookies;
                    }
                }
                /*
                 * we must save the cookies, because letitbit only allows 100 logins per 24hours
                 */
                br.postPage("http://letitbit.net/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&act=login");
                String check = br.getCookie(COOKIE_HOST, "log");
                if (check == null) check = br.getCookie(COOKIE_HOST, "pas");
                if (check == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                return cookies;
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || account.getUser().trim().length() == 0) {
            // Reset stuff because it can only be checked while downloading
            ai.setValidUntil(-1);
            ai.setUnlimitedTraffic();
            ai.setStatus("Status can only be checked while downloading!");
            account.setValid(true);
            return ai;
        }
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://letitbit.net/ajax/get_attached_passwords.php", "act=get_attached_passwords");
        final String[] data = br.getRegex("<td>([^<>\"]*?)</td>").getColumn(0);
        if (data != null && data.length >= 3) {
            // 1 point = 1 GB
            ai.setTrafficLeft(SizeFormatter.getSize(data[1].trim() + "GB"));
            ai.setValidUntil(TimeFormatter.getMilliSeconds(data[2].trim(), "yyyy-MM-dd", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Valid account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String dlUrl = null;
        prepBrowser(br);
        requestFileInformation(downloadLink);
        br.setDebug(true);
        if (account.getUser() == null || account.getUser().trim().length() == 0) {
            br.postPage(APIPAGE, "r=[\"" + Encoding.Base64Decode(APIKEY) + "\",[\"download/direct_links\",{\"link\":\"" + downloadLink.getDownloadURL() + "\",\"pass\":\"" + account.getPass() + "\"}]]");
            if (br.containsHTML("data\":\"bad password\"")) {
                logger.info("Wrong password, disabling the account!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("\"data\":\"no mirrors\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("\"data\":\"file is not found\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            dlUrl = br.getRegex("\"(http:[^<>\"]*?)\"").getMatch(0);
            if (dlUrl != null)
                dlUrl = dlUrl.replace("\\", "");
            else
                dlUrl = handleOldPremiumPassWay(account, downloadLink);
        } else {
            /* account login */
            boolean freshLogin = false;
            Object currentCookies = null;
            synchronized (PREMIUMLOCK) {
                synchronized (LOCK) {
                    Object latestCookies = account.getProperty("cookies", null);
                    currentCookies = login(account, false);
                    freshLogin = currentCookies != latestCookies;
                }
                br.setFollowRedirects(true);
                br.getPage(downloadLink.getDownloadURL());
                if (br.containsHTML(">Please wait, there is a file search")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download not possible at the moment", 2 * 60 * 60 * 1000l);
                handleNonApiErrors(downloadLink);
                dlUrl = getUrl(account);
            }
            if (dlUrl == null && br.containsHTML("callback_file_unavailable")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server Error", 15 * 60 * 1000l);
            // Maybe invalid or free account
            if (dlUrl == null && br.containsHTML("If you already have a premium")) {
                if (freshLogin == false) {
                    /*
                     * no fresh login, ip could have changed, remove cookies and retry with fresh login
                     */
                    synchronized (LOCK) {
                        if (currentCookies == account.getProperty("cookies", null)) {
                            account.setProperty("cookies", null);
                        }
                    }
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                logger.info("Disabling letitbit.net account: It's either a free account or logindata invalid!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (dlUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                logger.info("Premium with directDL");
            }
        }
        /* because there can be another link to a downlodmanager first */
        if (dlUrl == null) {
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
            logger.severe(br.toString());
            if (br.containsHTML("callback_file_unavailable")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            if (br.containsHTML("callback_tied_to_another")) {
                /*
                 * premium code is bound to a registered account,must login with username/password
                 */
                AccountInfo ai = account.getAccountInfo();
                if (ai != null) ai.setStatus("You must login with username/password!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "You must login with username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.setFollowRedirects(true);
        /* remove newline */
        dlUrl = dlUrl.replaceAll("%0D%0A", "").trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 2 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // NOTE: Old, tested 15.11.12, works!
    private String handleOldPremiumPassWay(final Account account, final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        handleNonApiErrors(downloadLink);
        // Get to the premium zone
        br.postPage(br.getURL(), "way_selection=1&submit_way_selection1=HIGH+Speed+Download");
        /* normal account with only a password */
        logger.info("Premium with pw only");
        Form premiumform = null;
        final Form[] allforms = br.getForms();
        if (allforms == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (Form singleform : allforms) {
            if (singleform.containsHTML("pass") && singleform.containsHTML("uid5") && singleform.containsHTML("uid") && singleform.containsHTML("name") && singleform.containsHTML("pin") && singleform.containsHTML("realuid") && singleform.containsHTML("realname") && singleform.containsHTML("host") && singleform.containsHTML("ssserver") && singleform.containsHTML("sssize") && singleform.containsHTML("optiondir")) {
                premiumform = singleform;
                break;
            }
        }
        if (premiumform == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        premiumform.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(premiumform);
        String iFrame = br.getRegex("\"(/sms/check2_iframe\\.php\\?ids=[0-9_]+\\&ids_emerg=\\&emergency_mode=)\"").getMatch(0);
        if (iFrame != null) {
            logger.info("Found iframe(old one), accessing it...");
            br.getPage("http://letitbit.net" + iFrame);
        }
        if (iFrame == null) {
            iFrame = br.getRegex("(/sms/check2_iframe\\.php\\?.*?uid=.*?)\"").getMatch(0);
            if (iFrame != null) {
                logger.info("Found iframe(new one), accessing it...");
                br.getPage("http://letitbit.net" + iFrame);
            }
        }
        return getUrl(account);
    }

    private String getUrl(final Account account) throws IOException {
        // This information can only be found before each download so lets set
        // it here
        String points = br.getRegex("\">Points:</acronym>(.*?)</li>").getMatch(0);
        String expireDate = br.getRegex("\">Expire date:</acronym> ([0-9\\-]+) \\[<acronym class").getMatch(0);
        if (expireDate == null) expireDate = br.getRegex("\">Period of validity:</acronym>(.*?) \\[<acronym").getMatch(0);
        if (expireDate != null || points != null) {
            final AccountInfo accInfo = new AccountInfo();
            // 1 point = 1 GB
            if (points != null) accInfo.setTrafficLeft(SizeFormatter.getSize(points.trim() + "GB"));
            if (expireDate != null) {
                accInfo.setValidUntil(TimeFormatter.getMilliSeconds(expireDate.trim(), "yyyy-MM-dd", null));
            } else {
                expireDate = br.getRegex("\"Total days remaining\">(\\d+)</acronym>").getMatch(0);
                if (expireDate == null) expireDate = br.getRegex("\"Days remaining in Your account\">(\\d+)</acronym>").getMatch(0);
                if (expireDate != null) accInfo.setValidUntil(System.currentTimeMillis() + (Long.parseLong(expireDate) * 24 * 60 * 60 * 1000));
            }
            account.setAccountInfo(accInfo);
        }
        String url = br.getRegex("title=\"Link to the file download\" href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("\"(http://r\\d+\\.letitbit\\.net/f/[a-z0-0]+/[^<>\"\\']+)\"").getMatch(0);
        }
        return url;
    }

    private void handleNonApiErrors(final DownloadLink dl) throws IOException, PluginException {
        if (br.containsHTML(TEMPDISABLED)) {
            br.postPage(APIPAGE, "r=" + Encoding.urlEncode("[\"" + Encoding.Base64Decode(APIKEY) + "\",[\"download/check_link\",{\"link\":\"" + dl.getDownloadURL() + "\"}]]"));
            final String availableMirrors = br.getRegex("\"data\":\\[(\\d+)\\]").getMatch(0);
            if (availableMirrors != null) {
                if (availableMirrors.equals("0")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temporarily not downloadable, " + availableMirrors + " mirrors remaining!");
                }
            }
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void prepareBrowser(final Browser br) {
        /*
         * last time they did not block the user-agent, we just need this stuff below ;)
         */
        if (br == null) { return; }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Pragma", "no-cache");
        br.getHeaders().put("Cache-Control", "no-cache");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Length", "0");
    }

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        if (prepBr == null) prepBr = new Browser();
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent);
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.setCustomCharset("UTF-8");
        prepBr.setCookie(COOKIE_HOST, "lang", "en");
        return prepBr;
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "STATUS", JDL.L("plugins.hoster.letitbit.status", "Use SkyMonk (also enables max 20 simultaneous downloads)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), LetitBitNet.ENABLEUNLIMITEDSIMULTANMAXFREEDLS, JDL.L("plugins.hoster.letitbitnet.enableunlimitedsimultanfreedls", "Enable unlimited (20) max simultanious free downloads for non-skymonk mode (can cause problems, use at your own risc)")).setDefaultValue(false));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

}