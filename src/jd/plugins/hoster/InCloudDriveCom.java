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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "inclouddrive.com" }, urls = { "https?://(www\\.)?inclouddrive\\.com/(link_download/\\?token=[A-Za-z0-9=_]+|(#/)?(file_download|file|link)/[0-9a-zA-Z=_-]+)" }, flags = { 2 })
public class InCloudDriveCom extends PluginForHost {

    // DEV NOTE:
    // links are not correctable to a standard url format

    public InCloudDriveCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.inclouddrive.com/");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.inclouddrive.com/#/terms_condition";
    }

    private String[]             hashTag;
    private Browser              ajax                         = null;

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = false;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(FREE_MAXDOWNLOADS);
    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        try {
            br.setAllowedResponseCodes(400, 500);
        } catch (final Throwable e) {
        }
        br.getPage(link.getDownloadURL());
        setFUID(link);
        if ("link".equals(hashTag[0])) {
            ajaxPostPage("https://www.inclouddrive.com/index.php/link", "user_id=&user_loged_in=no&link_value=" + Encoding.urlEncode(hashTag[1]));
        } else {
            ajaxPostPage("https://www.inclouddrive.com/index.php/" + hashTag[0] + "/" + hashTag[1], "user_id=");
        }
        final String filename = ajax.getRegex("class=\"propreties-file-count\">[\t\n\r ]+<b>([^<>\"]+)</b>").getMatch(0);
        final String filesize = ajax.getRegex(">Total size:</span><span class=\"propreties-dark-txt\">([^<>\"]+)</span>").getMatch(0);
        if (filename != null) {
            link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (ajax.containsHTML(">A Database Error Occurred<|This link has been removed from system.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            if (ajax.containsHTML("<button[^>]*file_type=\"folder\"[^>]*>Download</button>")) {
                // folder, not supported as of yet...
                return AvailableStatus.FALSE;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    public void doFree(final DownloadLink downloadLink, final boolean resume, final int maxchunks, final String directlinkparam) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkparam);
        if (dllink == null) {
            if (downloadLink.getBooleanProperty("premiumRequired", false)) {
                // canHandle for JD2, non JD2 here.
                premiumDownloadRestriction(downloadLink, downloadLink.getStringProperty("premiumRestrictionMsg", null));
            }

            final String uplid = ajax.getRegex("uploader_id=\"(\\d+)\"").getMatch(0);
            final String fileid = ajax.getRegex("file_id=\"(\\d+)\"").getMatch(0);
            final String predlwait = ajax.getRegex("var pre_download_timer_set\\s*=\\s*'(\\d+)';").getMatch(0);
            if (uplid == null || fileid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ajaxPostPage("/index.php/download_page_captcha", "type=yes");
            long captchaRequest = System.currentTimeMillis();
            final int repeat = 5;
            for (int i = 1; i <= repeat; i++) {
                final String code = getCaptchaCode("https://www.inclouddrive.com/captcha/php/captcha.php", downloadLink);
                // lets try this
                if (i == 1 && predlwait != null) {
                    // waittime[to ms] - elapsed time
                    final long wait = (Long.parseLong(predlwait) * 1000) - (System.currentTimeMillis() - captchaRequest);
                    sleep(wait, downloadLink);
                }
                ajaxPostPage("/captcha/php/check_captcha.php", "captcha_code=" + Encoding.urlEncode(code));
                if (ajax.toString().equals("not_match") && i + 1 != repeat) {
                    continue;
                } else if (ajax.toString().equals("not_match") && i + 1 == repeat) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    break;
                }
            }
            ajaxPostPage("/index.php/download_page_captcha/check_download_delay", "check_download_size=yes&uploder_id=" + uplid + "&file_id=" + fileid);
            if ("d_no".equals(ajax.toString())) {
                // this message is shown in browser regardless if your concurrent downloading or not! its also used as a history that you've
                // downloadded.

                // var msg = '<h3 class="pageheading" style="font-weight:bold;font-size:42px;margin-left:100px;">SORRY!</h3><p
                // style="text-align:center;font-size:17px;">You can not download more than 1 file at a time in free mode. Wish to remove
                // the restrictions? <span id="buy_premium_access_id" style="font-weight:bold;color:#3AB2F0;cursor: pointer;">Buy Premium
                // access</span>.</p>';
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download session limit reached!", 5 * 60 * 1000l);
            } else if ("max_download_error".equals(ajax.toString())) {
                // var msg = '<h3 class="pageheading" style="font-weight:bold;font-size:42px;margin-left:100px;">SORRY!</h3><p
                // style="text-align:center;font-size:17px;">The requested file is to big for a guest or free download. Please upgrade your
                // account. <span id="buy_premium_access_id" style="font-weight:bold;color:#3AB2F0;cursor: pointer;">Buy Premium
                // access</span>.</p>';
                premiumDownloadRestriction(downloadLink, "The requested file is to big! You need premium!");
            } else if (!"d_yes".equals(ajax.toString())) {
                // uncaught error
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ajaxPostPage("/index.php/get_download_server/download_page_link", "contact_id=" + uplid + "&table_id=" + fileid);
            dllink = ajax.toString();
            if (dllink == null || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // dllink = Encoding.htmlDecode(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkparam, dllink);
        dl.startDownload();
    }

    private void setFUID(final DownloadLink dl) throws PluginException {
        hashTag = new Regex(br.getURL(), "/(link_download)/\\?token=([A-Za-z0-9=_]+)").getRow(0);
        if (hashTag == null) {
            hashTag = new Regex(br.getURL(), "/(?:#/)?(file_download|file|link)/([0-9a-zA-Z_=-]+)").getRow(0);
            if (hashTag == null) {
                logger.info("hashTag is not in url --> Offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (hashTag == null || hashTag.length != 2) {
            logger.warning("Can not determin hashTag");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (hashTag[1] != null) {
            final String linkID = getHost() + "://" + hashTag[1];
            try {
                dl.setLinkID(linkID);
            } catch (final Throwable e) {
                dl.setProperty("LINKDUPEID", linkID);
            }
        }
    }

    private void ajaxPostPage(final String url, final String param) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("Connection-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getHeaders().put("Referer", "https://www.inclouddrive.com/");
        ajax.postPage(url, param);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable t) {
                }
            }
        }
        return dllink;
    }

    private static final String MAINPAGE = "http://inclouddrive.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                ajaxPostPage("https://www.inclouddrive.com/index.php/user/login_post", "remember_me=1&email_id=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!ajax.toString().equals("1")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final String lang = System.getProperty("user.language");
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.setFollowRedirects(true);
        br.postPage("https://www.inclouddrive.com/index.php/user/after_login_html", "");
        final String user_id = br.getRegex("var user_id = \\'(\\d+)\\';").getMatch(0);
        if (user_id == null) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        ajaxPostPage("https://www.inclouddrive.com/index.php/my_account/overview", "user_id=" + user_id);
        final String space = ajax.getRegex("class=\"leftusage active\">([^<>\"]*?)</span>").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(space.trim());
        }

        String used_traffic = ajax.getRegex("<span class=\"leftusage\">([^<>\"]*?)</span>").getMatch(0);
        String max_traffic = ajax.getRegex("<span class=\"rightusage\">([^<>\"]*?)</span>").getMatch(0);
        used_traffic = used_traffic.replace("BT", "b");
        max_traffic = max_traffic.replace("BT", "b");
        final long trafficmax = SizeFormatter.getSize(max_traffic);
        ai.setTrafficMax(trafficmax);
        final long trafficLeft = trafficmax - SizeFormatter.getSize(used_traffic);
        ai.setTrafficLeft(trafficLeft);

        if (ajax.containsHTML("class=\"memberplandetails leftbox\">[\t\n\r ]+<h5>Free</h5>")) {
            try {
                account.setType(AccountType.FREE);
                maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                /* free accounts can still have captcha */
                totalMaxSimultanFreeDownload.set(maxPrem.get());
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            account.setProperty("free", true);
            ai.setStatus("Registered (free) user");
        } else {
            final String expire = ajax.getRegex("Expiry date: (\\d+\\-\\d{2}\\-\\d{4})").getMatch(0);
            if (expire == null) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", Locale.ENGLISH));
            }
            try {
                account.setType(AccountType.PREMIUM);
                maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            account.setProperty("free", false);
            ai.setStatus("Premium User");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getBooleanProperty("free", false)) {
            login(account, false);
            requestFileInformation(link);
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            login(account, false);
            String dllink = checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                requestFileInformation(link);
                final String uplid = ajax.getRegex("uploader_id=\"(\\d+)\"").getMatch(0);
                final String fileid = ajax.getRegex("file_id=\"(\\d+)\"").getMatch(0);
                if (uplid == null || fileid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                ajaxPostPage("https://www.inclouddrive.com/index.php/download_page_captcha", "type=yes");
                ajaxPostPage("https://www.inclouddrive.com/index.php/get_download_server/download_page_link", "contact_id=" + uplid + "&table_id=" + fileid);
                dllink = ajax.toString();
                if (dllink == null || !dllink.startsWith("http")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        // not invalid on any filesystem
        // output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink downloadLink) {
        downloadLink.setProperty("premiumRequired", Property.NULL);
        downloadLink.setProperty("premiumRestrictionMsg", Property.NULL);
    }

    /**
     * When premium only download restriction (eg. filesize), throws exception with given message
     *
     * @param msg
     * @throws PluginException
     */
    public void premiumDownloadRestriction(final DownloadLink downloadLink, final String msg) throws PluginException {
        downloadLink.setProperty("premiumRequired", true);
        downloadLink.setProperty("premiumRestrictionMsg", msg);
        try {
            downloadLink.setComment(msg);
        } catch (final Throwable e) {
        }
        try {
            downloadLink.getLinkStatus().setStatusText(msg);
        } catch (final Throwable e) {
            // if used outside intended methods it will throw exception
        }
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, msg);
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (downloadLink.getBooleanProperty("premiumRequired", false) && (account == null || account.getBooleanProperty("free", false))) {
            return false;
        } else {
            return true;
        }
    }

    public String  folderLinks         = "folderLinks";
    public boolean default_folderLinks = true;

    public void setConfigElements() {
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "How to treat folder links?"));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), folderLinks,
        // JDL.L("plugins.hoster.inclouddrive.folderlinks",
        // "Process folder pages as individual links")).setDefaultValue(default_folderLinks));
    }

    /**
     * because stable is lame!
     * */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    public void setAjaxBrowser(final Browser ajax) {
        this.ajax = ajax;
    }

}