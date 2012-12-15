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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.net" }, urls = { "http://(www\\.|u\\d+\\.)?letitbit\\.net/d?download/(.*?\\.html|[0-9a-zA-z/.-]+)" }, flags = { 2 })
public class LetitBitNet extends PluginForHost {

    private static Object        LOCK                              = new Object();
    private static final String  COOKIE_HOST                       = "http://letitbit.net/";
    private static AtomicInteger maxFree                           = new AtomicInteger(1);
    private static final String  ENABLEUNLIMITEDSIMULTANMAXFREEDLS = "ENABLEUNLIMITEDSIMULTANMAXFREEDLS";
    /* For linkcheck and premium download we're using their API: http://api.letitbit.net/reg/static/api.pdf */
    public static final String   APIKEY                            = "VjR1U3JGUkNx";
    public static final String   APIPAGE                           = "http://api.letitbit.net/";
    private static final String  SPECIALOFFLINE                    = "onclick=\"checkCaptcha\\(\\)\" style=\"";

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        setAccountwithoutUsername(true);
        enablePremium("http://letitbit.net/page/premium.php");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* convert directdownload links to normal links */
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://letitbit.net/", "lang", "en");
        br.postPage(APIPAGE, "r=" + Encoding.urlEncode("[\"" + Encoding.Base64Decode(APIKEY) + "\",[\"download/info\",{\"link\":\"" + downloadLink.getDownloadURL() + "\"}]]"));
        if (br.containsHTML("status\":\"FAIL\",\"data\":\"bad link\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = getJson("name");
        final String filesize = getJson("size");
        final String md5 = getJson("md5");
        if (filename != null && filesize != null) {
            downloadLink.setFinalFileName(filename);
            downloadLink.setDownloadSize(Long.parseLong(filesize));
            if (md5 != null) downloadLink.setMD5Hash(md5);
            return AvailableStatus.TRUE;
        } else {
            return oldAvailableCheck(downloadLink);
        }
    }

    private AvailableStatus oldAvailableCheck(final DownloadLink downloadLink) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        // /* set english language */
        // br.postPage(downloadLink.getDownloadURL(),
        // "en.x=10&en.y=8&vote_cr=en");
        String filename = br.getRegex("\"file-info\">File:: <span>(.*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("name=\"realname\" value=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"first\">File:: <span>(.*?)</span></li>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("title>(.*?) download for free").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("name=\"sssize\" value=\"(\\d+)\"").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<li>Size of file:: <span>(.*?)</span></li>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("\\[<span>(.*?)</span>\\]</h1>").getMatch(0);
            }
        }
        if (filesize == null) filesize = br.getRegex("\\[<span>(.*?)</span>\\]</h1>").getMatch(0);
        if (filename == null || filesize == null) {
            if (br.containsHTML("(<title>404</title>|>File not found<|Запрашиваемый файл не найден<br>|>Запрашиваемая вами страница не существует\\!<|Request file .*? Deleted)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("<p style=\"color:#000\">File not found</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("Request file.*?Deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("Forbidden word")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Their names often differ from other file hosting services. I noticed
        // that when in the filenames from other hosting services there are
        // "-"'s, letitbit uses "_"'s so let's correct this here ;)
        downloadLink.setFinalFileName(filename.trim().replace("_", "-"));
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    private String getJson(final String parameter) {
        return br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        checkShowFreeDialog();
        try {
            br.setVerbose(true);
            br.setCustomCharset("UTF-8");
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
        skymonk.getHeaders().put("Accept", null);
        skymonk.getHeaders().put("Accept-Language", null);
        skymonk.getHeaders().put("User-Agent", null);
        skymonk.getHeaders().put("Referer", null);
        skymonk.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");

        int rd = (int) Math.random() * 6 + 1;
        skymonk.postPage("http://api.letitbit.net/internal/index4.php", "action=LINK_GET_DIRECT&link=" + s + "&free_link=1&sh=" + JDHash.getMD5(String.valueOf(Math.random())) + rd + "&sp=" + (49 + rd) + "&appid=" + JDHash.getMD5(String.valueOf(Math.random())) + "&version=2.12");
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

    private String handleFreeFallback(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        if (br.containsHTML(SPECIALOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        boolean passed = submitFreeForm();
        if (passed) logger.info("Sent free form #1");
        passed = submitFreeForm();
        if (passed) logger.info("Sent free form #2");
        passed = submitFreeForm();
        if (passed) logger.info("Sent free form #3");

        String urlPrefix = new Regex(br.getURL(), "http://(www\\.)?([a-z0-9]+\\.)letitbit\\.net/.+").getMatch(1);
        if (urlPrefix == null) urlPrefix = "";
        final String ajaxmainurl = "http://" + urlPrefix + "letitbit.net";

        final String dlFunction = br.getRegex("function getLink\\(\\)(.*?)</script>").getMatch(0);
        if (dlFunction == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
            final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
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
        // Downloadlimit is per day so let's just wait 3 hours
        if (br2.containsHTML("error_free_download_blocked")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 3 * 60 * 60 * 1000l);
        if (br2.containsHTML("callback_file_unavailable")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
        LinkedList<String> finallinksx = new LinkedList<String>();
        String[] finallinks = br2.getRegex("\"(http:[^<>\"]*?)\"").getColumn(0);
        // More comon for shareflare.net
        if ((finallinks == null || finallinks.length == 0) && br2.toString().length() < 500) finallinks = br2.getRegex("(http:[^<>\"].+)").getColumn(0);
        if (finallinks == null || finallinks.length == 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        for (final String finallink : finallinks) {
            if (!finallinksx.contains(finallink) && finallink.startsWith("http")) finallinksx.add(finallink);
        }
        // Grab last links, this might changes and has to be fixed if users get
        // "server error" in JD while it's working via browser. If this is
        // changed often we should consider trying the whole list of finallinks.
        final String url = finallinksx.peekLast();
        if (url == null || url.length() > 1000 || !url.startsWith("http")) {
            if (br2.containsHTML("error_free_download_blocked")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Free download blocked", 2 * 60 * 60 * 1000l); }
            logger.warning("url couldn't be found!");
            logger.severe(url);
            logger.severe(br2.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* we have to wait little because server too buggy */
        sleep(2000, downloadLink);
        /* remove newline */
        return url.replaceAll("%0D%0A", "").trim().replace("\\", "");
    }

    private boolean submitFreeForm() throws Exception {
        // this finds the form to "click" on the next "free download" button
        Form[] allforms = br.getForms();
        if (allforms == null || allforms.length == 0) return false;
        Form down = null;
        for (Form singleform : allforms) {
            if (singleform.containsHTML("md5crypt") && singleform.getAction() != null && !singleform.containsHTML("/sms/check")) {
                down = singleform;
                break;
            }
        }
        if (down == null) return false;
        br.submitForm(down);
        return true;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                this.setBrowserExclusive();
                br.setCustomCharset("UTF-8");
                br.setCookie(COOKIE_HOST, "lang", "en");
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
                        return false;
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
                return true;
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || account.getUser().trim().length() == 0) {
            account.setValid(true);
            // Reset stuff because it can only be checked while downloading
            ai.setValidUntil(-1);
            ai.setTrafficLeft(-1);
            ai.setStatus("Status can only be checked while downloading!");
            return ai;
        }
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setStatus("Valid account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String dlUrl = null;
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
            boolean freshLogin = login(account, false);
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML(SPECIALOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            dlUrl = getUrl(account);
            if (dlUrl == null && br.containsHTML("callback_file_unavailable")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l);
            // Maybe invalid or free account
            if (dlUrl == null && br.containsHTML("If you already have a premium")) {
                if (freshLogin == false) {
                    /*
                     * no fresh login, ip could have changed, remove cookies and retry with fresh login
                     */
                    synchronized (LOCK) {
                        account.setProperty("cookies", null);
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
        if (br.containsHTML(SPECIALOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
            AccountInfo accInfo = new AccountInfo();
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

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void prepareBrowser(final Browser br) {
        /*
         * last time they did not block the useragent, we just need this stuff below ;)
         */
        if (br == null) { return; }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Pragma", "no-cache");
        br.getHeaders().put("Cache-Control", "no-cache");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Length", "0");
        br.setCustomCharset("utf-8");
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "STATUS", JDL.L("plugins.hoster.letitbit.status", "Use SkyMonk?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), LetitBitNet.ENABLEUNLIMITEDSIMULTANMAXFREEDLS, JDL.L("plugins.hoster.letitbitnet.enableunlimitedsimultanfreedls", "Enable unlimited (20) max simultanious free downloads (can cause problems, use at your own risc)")).setDefaultValue(false));
    }

}