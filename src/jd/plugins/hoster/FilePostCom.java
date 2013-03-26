//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filepost.com" }, urls = { "https?://(www\\.)?(filepost\\.com/files|fp\\.io)/[a-z0-9]+" }, flags = { 2 })
public class FilePostCom extends PluginForHost {

    private static final String ua                 = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.18) Gecko/20110628 Ubuntu/10.10 (maverick) Firefox/3.6.18";
    private boolean             showAccountCaptcha = false;
    private static final String FILEIDREGEX        = "filepost\\.com/files/(.+)";
    private static final String MAINPAGE           = "https://filepost.com/";
    private static Object       LOCK               = new Object();
    private static final String FREEBLOCKED        = "(>The file owner has limited free downloads of this file|premium membership is required to download this file\\.<)";
    private static final String NOCHUNKS           = "NOCHUNKS";

    public FilePostCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filepost.com/premium/");
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("urls=");
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) break;
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report anything else
                     */
                    if (c > 0) sb.append("%0D%0A");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                br.getHeaders().put("User-Agent", ua);
                br.postPage("http://filepost.com/files/checker/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", sb.toString());
                String correctedBR = br.toString().replace("\\", "");
                for (DownloadLink dl : links) {
                    String fileid = new Regex(dl.getDownloadURL(), FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Filepost availablecheck is broken!");
                        return false;
                    }
                    Regex theData = new Regex(correctedBR, ";\" target=\"_blank\">https?://filepost\\.com/files/" + fileid + "/(.*?)/</a></td>nttt<td>(.*?)</td>nttt<td>ntttt<span class=\"(x|v)\"");
                    String filename = theData.getMatch(0);
                    String filesize = theData.getMatch(1);
                    if (filename == null || filesize == null || ("x".equals(theData.getMatch(2)))) {
                        dl.setAvailable(false);
                        continue;
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(Encoding.htmlDecode(filename));
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#correctDownloadLink(jd.plugins.DownloadLink)
     */
    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https:", "http:").replace("fp.io/", "filepost.com/files/"));
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
                    File checkFile = JDUtilities.getResourceFile("tmp/fpsttmp");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog("filepost.com");
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            showAccountCaptcha = true;
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        } finally {
            showAccountCaptcha = false;
        }
        br.getPage(MAINPAGE);
        if (!br.containsHTML("<li>Account type: <span>Premium</span>")) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex("(?i)<li>(Used )?storage: <span.*?>([\\d\\.]+ ?(MB|GB))</span>").getMatch(1);
        if (space != null) ai.setUsedSpace(space.trim());
        String filesNum = br.getRegex("<li>Active files: <span.*?>(\\d+)</span>").getMatch(0);
        if (filesNum != null) ai.setFilesNum(Integer.parseInt(filesNum));
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<li>.*?Valid until: <span>(.*?)</span>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMMM dd, yyyy", null));
            account.setValid(true);
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://filepost.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        checkShowFreeDialog();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", ua);
        br.setCookie("http://filepost.com", "lang", "1");
        br.getPage(downloadLink.getDownloadURL());
        String premiumlimit = br.getRegex("Files over (.*?) can be downloaded by premium").getMatch(0);
        if (premiumlimit != null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filepostcom.only4premium", "Files over " + premiumlimit + " are only downloadable for premium users"));
        if (br.containsHTML(FREEBLOCKED)) if (br.containsHTML("The file owner has limited free downloads of this file")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filepostcom.only4premium2", "Only downloadable for premium users"));
        if (br.containsHTML("We are sorry, the server where this file is")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverissue", 60 * 60 * 1000l);
        if (br.containsHTML("(>Your IP address is already downloading a file at the moment|>Please wait till the download completion and try again)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP Already Loading", 20 * 60 * 1000l);
        String passCode = downloadLink.getStringProperty("pass", null);
        // Errorhandling in case their linkchecker lies
        if (br.containsHTML("(<title>FilePost\\.com: Download  \\- fast \\&amp; secure\\!</title>|>File not found<|>It may have been deleted by the uploader or due to the received complaint|<div class=\"file_info file_info_deleted\">)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        /* token and SID used for all requests */
        String token = getToken();
        final String action = getAction();
        if (action == null || token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String id = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);
        Browser brc = br.cloneBrowser();
        Form form = new Form();
        form.setMethod(MethodType.POST);
        form.setAction(action + System.currentTimeMillis() + "-xml");
        form.put("action", "set_download");
        // form.put("download", token);
        form.put("code", id);
        form.setEncoding("application/octet-stream; charset=UTF-8");
        /* click on low speed button */
        brc.submitForm(form);
        boolean nextD = false;
        String nextDownload = brc.getRegex("next_download\":\"(\\d+)").getMatch(0);
        if (nextDownload != null) nextD = true;
        if (nextDownload == null) nextDownload = brc.getRegex("wait_time\":\"(\\-?\\d+)").getMatch(0);
        int wait = 30;
        if (nextDownload != null) {
            if (nextDownload.contains("-")) {
                nextDownload = "0";
            }
            wait = Integer.parseInt(nextDownload);
            if (wait > 300) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1000l);
        }
        sleep(wait * 1001l, downloadLink);
        String dllink = null;
        /** Password handling */
        if (br.containsHTML("var is_pass_exists = true")) {
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            brc.postPage(action + action + System.currentTimeMillis() + "-xml", "code=" + id + "&file_pass=" + Encoding.urlEncode(passCode) + "&token=" + token);
            if (brc.containsHTML("\"Wrong file password\"")) throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            dllink = getDllink(brc.toString().replace("\\", ""));
        }
        if (dllink == null) {
            boolean captchaNeeded = br.containsHTML("show_captcha = 1");
            if (!captchaNeeded) captchaNeeded = br.containsHTML("show_captcha = true");
            form = new Form();
            form.setMethod(MethodType.POST);
            form.setAction(action + System.currentTimeMillis() + "-xml");
            form.put("download", token);
            form.put("file_pass", "undefined");
            form.put("code", id);
            form.setEncoding("application/octet-stream; charset=UTF-8");
            if (captchaNeeded) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                String cid = br.getRegex("Captcha\\.init.*?key.*?'(.*?)'").getMatch(0);
                rc.setId(cid);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                form.put("recaptcha_response_field", Encoding.urlEncode(c));
                form.put("recaptcha_challenge_field", rc.getChallenge());
            }
            brc = br.cloneBrowser();
            brc.submitForm(form);
            if (brc.containsHTML("\"file_too_big_for_user\"")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filepostcom.only4premium2", "Only downloadable for premium"));
            if (brc.containsHTML("You entered a wrong CAPTCHA code")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            String correctedBR = brc.toString().replace("\\", "");
            if (correctedBR.contains("Your download is not found or has expired")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverissue", 10 * 60 * 1000l);
            if (correctedBR.contains("Your IP address is already")) {
                if (nextD) throw new PluginException(LinkStatus.ERROR_RETRY);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP Already Loading", 5 * 60 * 1000l);
            }
            dllink = getDllink(correctedBR);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getCookie(MAINPAGE, "error") != null && new Regex(br.getCookie(MAINPAGE, "error"), "(Sorry%2C%20you%20have%20exceeded%20your%20daily%20download%20limit\\.|%3Cbr%20%2F%3ETry%20again%20tomorrow%20or%20obtain%20a%20premium%20membership\\.)").matches()) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 2 * 60 * 60 * 1000l);
            if (br.getCookie(MAINPAGE, "error") != null) logger.warning("Unhandled error: " + br.getCookie(MAINPAGE, "error"));
            if (br.containsHTML(">403 Forbidden<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    private String getToken() {
        String token = br.getRegex("flp_token\\', \\'(.*?)\\'").getMatch(0);
        if (token == null) token = br.getRegex("token\\', \\'(.*?)\\'").getMatch(0);
        if (token == null) token = br.getRegex("token: \\'(\\w+)\\'").getMatch(0);
        return token;
    }

    private String getAction() {
        String action = null;
        final String SID = br.getCookie("http://filepost.com/", "SID");
        if (SID != null) action = "http://filepost.com/files/get/?SID=" + SID + "&JsHttpRequest=";
        return action;
    }

    private String getDllink(String correctedBR) {
        String dllink = new Regex(correctedBR, "\"answer\":\\{\"link\":\"(https?://.*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(https?://fs\\d+\\.filepost\\.com/get_file/.*?)\"").getMatch(0);
        return dllink;
    }

    /**
     * Important: Handling for password protected links is not included yet (only in handleFree)!
     */
    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        // Force login because of cookie bug
        login(account, true);
        br.setFollowRedirects(true);
        br.setCookie("http://filepost.com", "lang", "1");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("We are sorry, the server where this file is")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverissue", 60 * 60 * 1000l);
        if (br.containsHTML("(>Sorry, you have reached the daily download limit|>Please contact our support team if you have questions about this limit)")) {
            logger.info("Premium downloadlimit reached, disabling account...");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        // Cookies lost/not logged in anymore? Serverside issue! Disable account
        if (br.containsHTML(FREEBLOCKED)) {
            logger.info("Not logged in anymore, this is a filepost.com issue, NOT a JDownloader issue (same in browser)!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        String dllink = null;
        String passCode = link.getStringProperty("pass", null);
        /** Password handling */
        if (br.containsHTML("var is_pass_exists = true")) {
            String action = getAction();
            String token = getToken();
            Browser brc = br.cloneBrowser();
            if (passCode == null) passCode = Plugin.getUserInput("Password?", link);
            brc.postPage(action + action + System.currentTimeMillis() + "-xml", "code=" + new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0) + "&file_pass=" + Encoding.urlEncode(passCode) + "&token=" + token);
            if (brc.containsHTML("\"Wrong file password\"")) throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            dllink = getDllink(brc.toString().replace("\\", ""));
        }
        if (dllink == null) {
            dllink = br.getRegex("<button onclick=\"download_file\\(\\'(https?://.*?)\\'\\)").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\\'(https?://fs\\d+\\.filepost\\.com/get_file/.*?)\\'").getMatch(0);
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        // Connectionlimit not tested but user said it didn't work with 2
        // connections
        int chunks = 0;
        boolean resume = true;
        if (link.getBooleanProperty(FilePostCom.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML(">403 Forbidden<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) link.setProperty("pass", passCode);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (link.getLinkStatus().getErrorMessage() != null && link.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (link.getBooleanProperty(FilePostCom.NOCHUNKS) == false) {
                    link.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(FilePostCom.NOCHUNKS, false) == false) {
                    link.setProperty(FilePostCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies, because no multiple downloads possible when always
            // loggin in for every download
            br.setCookiesExclusive(true);
            br.setCookie("http://filepost.com", "lang", "1");
            try {
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals((account.getStringProperty("name", Encoding.urlEncode(account.getUser()))));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("remembered_user") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        Browser brc = br.cloneBrowser();
                        brc.postPage("http://filepost.com/general/login_form/?SID=" + brc.getCookie(MAINPAGE, "SID"), "action=check");
                        if (brc.containsHTML("premium")) return;
                    }
                }
                br.postPage("https://filepost.com/general/login_form/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&recaptcha_response_field=");
                if (br.containsHTML("captcha\":true")) {
                    /* too many logins result in recaptcha login */
                    if (showAccountCaptcha == false) {
                        AccountInfo ai = account.getAccountInfo();
                        if (ai != null) {
                            ai.setStatus("Logout/Login in Browser please!");
                        }
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        br.getPage("https://filepost.com");
                        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        String id = br.getRegex("Captcha\\.init.*?key.*?'(.*?)'").getMatch(0);
                        rc.setId(id);
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        DownloadLink dummyLink = new DownloadLink(this, "Account", "filepost.com", "http://filepost.com", true);
                        String c = getCaptchaCode(cf, dummyLink);
                        br.postPage("https://filepost.com/general/logn_form/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&recaptcha_response_field=" + Encoding.urlEncode(c) + "&recaptcha_challenge_field=" + rc.getChallenge());
                    }
                }
                if (br.containsHTML("captcha\":true")) {
                    /* too many logins result in recaptcha login */
                    AccountInfo ai = account.getAccountInfo();
                    if (ai != null) {
                        ai.setStatus("Captcha Wrong!");
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(MAINPAGE, "u") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                /* change language to english */
                br.postPage("http://filepost.com/general/select_language/?SID=" + br.getCookie(MAINPAGE, "SID") + "&JsHttpRequest=" + System.currentTimeMillis() + "-xml", "language=1");
            } catch (PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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