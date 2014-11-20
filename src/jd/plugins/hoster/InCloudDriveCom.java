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
import jd.nutils.Formatter;
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
    private final boolean        FREE_RESUME                  = false;
    private final int            FREE_MAXCHUNKS               = 1;
    private final int            FREE_MAXDOWNLOADS            = 1;
    private final boolean        ACCOUNT_FREE_RESUME          = false;
    private final int            ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int            ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = -4;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    private Browser prepBrowser(final Browser prepBr) {
        try {
            prepBr.setAllowedResponseCodes(400, 500);
        } catch (final Throwable e) {
        }
        prepBr.getHeaders().put("Accept-Language", "en-AU,en;q=0.8");
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36");
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        return parseFileInformation(link);
    }

    private AvailableStatus parseFileInformation(final DownloadLink link) throws Exception {
        setFUID(link);
        br.getPage("https://www.inclouddrive.com/file/" + hashTag[1]);
        final String regexFS = "<div class=\"downloadfileinfo[^>]*>(.*?) \\((.*?)\\)";
        String filename = br.getRegex(regexFS).getMatch(0);
        String filesize = br.getRegex(regexFS).getMatch(1);
        // premium only files regex is different!
        if (filename == null) {
            filename = br.getRegex("<div class=\"name wordwrap\">(.*?)</div>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("fileinfo [a-z0-9 ]+\">([^<>]*?) \\([0-9\\.]*? .?B\\)<").getMatch(0);
            }
        }
        if (filesize == null) {
            filesize = br.getRegex("<div class=\"icddownloadsteptwo-filesize\">(.*?)</div>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("fileinfo [a-z0-9 ]+\">[^<>]*? \\(([0-9\\.]*? .?B)\\)<").getMatch(0);
            }
        }

        if (filename != null) {
            link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.containsHTML(">A Database Error Occurred<|This link has been removed from system\\.|>The requested file isn't anymore!</p>|>Content you have requested is unavailable")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            if (br.containsHTML("<button[^>]*file_type=\"folder\"[^>]*>Download</button>")) {
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
            // premium only check can be done here now (20141117)
            if (br.containsHTML(">The requested file is to big for a guest or free download\\.")) {
                premiumDownloadRestriction(downloadLink, "The requested file is to big! You need premium!");
            }
            final String token = Encoding.urlDecode(br.getRegex("freetoken=\"(.*?)\"").getMatch(0), false);
            final String predlwait = br.getRegex("id=\"freetimer\"[^>]*>*(\\d+)</div>").getMatch(0);
            final String dlserver = br.getRegex("freeaccess=\"(.*?)\"").getMatch(0);
            if (token == null || predlwait == null || dlserver == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            long captchaRequest = System.currentTimeMillis();
            final int repeat = 5;
            for (int i = 1; i <= repeat; i++) {
                final String code = getCaptchaCode("https://www.inclouddrive.com/captcha/php/captcha.php", downloadLink);
                // lets try this
                if (i == 1 && predlwait != null) {
                    // waittime[to ms] - elapsed time
                    final long wait = (Long.parseLong(predlwait) * 1000) - (System.currentTimeMillis() - captchaRequest);
                    if (wait > 0) {
                        sleep(wait, downloadLink);
                    }
                }
                ajaxPostPage("/captcha/php/check_captcha.php", "captcha_code=" + Encoding.urlEncode(code) + "&token=" + token);
                if (ajax.toString().equals("not_match") && i + 1 != repeat) {
                    continue;
                } else if (ajax.toString().equals("not_match") && i + 1 == repeat) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    break;
                }
            }
            //
            br.setFollowRedirects(false);
            br.getPage(Encoding.urlDecode(dlserver, false) + "download.php?accesstoken=" + token);
            dllink = br.getRedirectLocation();
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
                prepBrowser(br);
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
                br.getPage("https://www.inclouddrive.com/user/login");
                final String js = br.cloneBrowser().getPage("/java/mycloud.js");
                final String appToken = new Regex(js, "app:\\s*'(.*?)',").getMatch(0);
                ajaxPostPage("https://www.inclouddrive.com/api/0/signmein", "useraccess=&access_token=" + appToken + "&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&keep=1");
                final String doz = Encoding.urlEncode(ajax.getRegex("\"doz\":\"([^\"]+)").getMatch(0));
                if (!ajax.containsHTML("\"result\":\"ok\"") || doz == null || "".equals(doz)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.setCookie(MAINPAGE, "userdata", doz);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = ajax.getCookies(MAINPAGE);
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
        if (inValidate(account.getUser()) || !account.getUser().matches(".+@.+")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Username has to be Email Address!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (inValidate(account.getPass())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Password can not be empty!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }

        final String lang = System.getProperty("user.language");
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/me");
        final String[] premExpire = br.getRegex(">(Premium)</div>[^<]*<div[^>]*>Expires on (\\d+ \\w+, \\d{4})<").getRow(0);
        if (premExpire == null) {
            account.setProperty("free", true);
            ai.setStatus("Free Account");
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            try {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
        } else {
            final String expire = premExpire[1];
            if (expire == null) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMM, yyyy", Locale.ENGLISH));
            }
            account.setProperty("free", false);
            ai.setStatus("Premium Account");
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
        }

        final String space = br.getRegex(">Used Space</div>\\s*<span[^>]*>(.*?) / \\d+ GB</span>").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(SizeFormatter.getSize(space.trim()));
        } else {
            logger.warning("Could not determine Used Space! Account type: " + getAccountType(account));
        }

        final String[] traffic = br.getRegex(">Used Bandwidth</div>\\s*<span[^>]*>(.*?)\\s*/\\s*(\\d+ GB)<").getRow(0);
        if (traffic != null) {
            final long trafficused = SizeFormatter.getSize(traffic[0].contains("&nbsp;") ? "0" : traffic[0].replace("BT", "Byte"));
            final long trafficmax = SizeFormatter.getSize(traffic[1].replace("BT", "Byte"));
            ai.setTrafficMax(trafficmax);
            ai.setTrafficLeft(trafficmax - trafficused);
        } else {
            logger.warning("Could not determine Used Bandwidth! Account type: " + getAccountType(account));
        }

        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        if (account.getBooleanProperty("free", false)) {
            requestFileInformation(link);
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                br.setFollowRedirects(true);
                try {
                    br.setAllowedResponseCodes(400, 500);
                } catch (final Throwable e) {
                }
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(link.getDownloadURL());
                    if (con.isContentDisposition()) {
                        dllink = link.getDownloadURL();
                    } else {
                        br.followConnection();
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable t) {
                    }
                }
            }
            if (dllink == null) {
                parseFileInformation(link);
                dllink = br.getRegex("\"(https?://\\w+\\.inclouddrive\\.com/download/\\?token=[^\"]+)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(https?://\\w+\\.inclouddrive\\.com/download\\.php\\?accesstoken=[^\"]+)\"").getMatch(0);
                    if (dllink == null || !dllink.startsWith("http")) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
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

    public void showAccountDetailsDialog(final Account account) {
        AccountInfo ai = account.getAccountInfo();
        String message = "";
        message += "Account type: " + getAccountType(account) + "\r\n";
        if (ai.getUsedSpace() != -1) {
            message += "  Used Space: " + Formatter.formatReadable(ai.getUsedSpace()) + "\r\n";
        }
        if (ai.getPremiumPoints() != -1) {
            message += "Premium Points: " + ai.getPremiumPoints() + "\r\n";
        }

        jd.gui.UserIO.getInstance().requestMessageDialog(this.getHost() + " Account", message);
    }

    private String getAccountType(final Account account) {
        return account.getBooleanProperty("free", false) ? "Free" : "Premium";
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

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("\\s+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

}