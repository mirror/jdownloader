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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "inclouddrive.com" }, urls = { "https?://(www\\.)?inclouddrive\\.com/(link_download/\\?token=[A-Za-z0-9=_]+|(?:#/)?((?:file_download|link)/[0-9a-zA-Z=_-]+(?:/[^/]+)?|file/[0-9a-zA-Z=_-]+/[^/]+))" })
public class InCloudDriveCom extends antiDDoSForHost {
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
        prepBr.setAllowedResponseCodes(new int[] { 400, 500 });
        prepBr.getHeaders().put("Accept-Language", "en-AU,en;q=0.8");
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36");
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepBrowser(br);
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        return parseFileInformation(link);
    }

    @SuppressWarnings("deprecation")
    private AvailableStatus parseFileInformation(final DownloadLink link) throws Exception {
        setFUID(link);
        getPage(link.getDownloadURL());
        if (br.containsHTML(">we are performing a service upgrade please try again!")) {
            return AvailableStatus.UNCHECKABLE;
        }
        final String regexFS = "<div class=\"downloadfileinfo [^>]*\">([^<>\"]*?) \\(([^<>\"]*?)\\)<div";
        String filename = br.getRegex(regexFS).getMatch(0);
        String filesize = br.getRegex(regexFS).getMatch(1);
        // premium only files regex is different!
        if (filename == null) {
            filename = br.getRegex("<div class=\"name wordwrap\">(.*?)</div>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("fileinfo [a-z0-9 ]+\">([^<>]*?) \\([0-9\\.]*? .?B\\)<").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) \\&ndash; InCloudDrive</title>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("<div class=\"icddownloadsteptwo-filesize\">(.*?)</div>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("fileinfo [a-z0-9 ]+\">[^<>]*? \\(([0-9\\.]*? .?B)\\)<").getMatch(0);
            }
        }
        if (filename != null) {
            // this is what the filename is detected at for offline content. Don't set as url can contain say filename.rar and core will set
            // that!
            if (!"Content Unavailable".equals(filename)) {
                link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
            }
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.containsHTML(">A Database Error Occurred<|This link has been removed from system\\.|>The requested file isn't anymore!</p>|>Content you have requested is unavailable|The file you are trying to download is no longer available!</h1>|The file you are trying to download is no longer available\\.<|>The file has been removed because of a ToS/AUP violation\\.<|>Invalid URL - the link you are trying to access does not exist<|>The file has been deleted by the user\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            if (br.containsHTML("<button[^>]*file_type=\"folder\"[^>]*>Download</button>")) {
                // folder, not supported as of yet...
                return AvailableStatus.FALSE;
            }
            // tokens are never shown as error when invalid
            if (link.getDownloadURL().matches(".+/link_download/\\?token=[A-Za-z0-9=_]+")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    private void checkErrors(Browser br, DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML(">we are performing a service upgrade please try again!")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Maintenance is being done", 1 * 60 * 60 * 1000l);
        }
        if (br.containsHTML(">As a Free User; You cannot download more then one file at the moment")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        if (downloadLink.getBooleanProperty("premiumRequired", false)) {
            // canHandle for JD2, non JD2 here.
            premiumDownloadRestriction(downloadLink, downloadLink.getStringProperty("premiumRestrictionMsg", null));
        }
        // premium only check can be done here now (20141117)
        if (br.containsHTML(">The requested file is too? big for a guest or free download|>This link only for premium user")) {
            premiumDownloadRestriction(downloadLink, "The requested file is to big / only for premium user");
        }
    }

    public void doFree(final DownloadLink downloadLink, final boolean resume, final int maxchunks, final String directlinkparam) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkparam);
        if (dllink == null) {
            checkErrors(br, downloadLink);
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
            br.setFollowRedirects(false);
            getPage(Encoding.urlDecode(dlserver, false) + "download.php?accesstoken=" + token);
            dllink = br.getRedirectLocation();
            if (dllink == null || !dllink.startsWith("http")) {
                if (br.getHttpConnection().getResponseCode() == 401) {
                    /* 2018-12-03: Has been this way for some weeks now, host seems to be broken serverside ... */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // dllink = Encoding.htmlDecode(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            checkErrors(br, downloadLink);
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
            dl.setLinkID(linkID);
        }
    }

    private void ajaxPostPage(final String url, final String param) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("Connection-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getHeaders().put("Referer", "https://www.inclouddrive.com/");
        this.postPage(ajax, url, param);
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

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(br);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    /*
                     * 2019-12-27: re-use cookies whenever possible. Too many full logins may cause this:
                     * {"result":"error","message":"Your account is blocked due to security reason."}
                     */
                    logger.info("Attempting to re-use cookies");
                    br.setCookies(this.getHost(), cookies);
                    getPage("https://www." + this.getHost() + "/mycloud");
                    if (isLoggedIN()) {
                        logger.info("Successfully re-used cookies");
                        loggedIN = true;
                    } else {
                        logger.info("Failed to re-use cookies");
                        loggedIN = false;
                    }
                }
                if (!loggedIN) {
                    logger.info("Performing full login");
                    br.setFollowRedirects(false);
                    getPage("https://www." + this.getHost() + "/user/login");
                    final String js = br.cloneBrowser().getPage("/java/mycloud.js");
                    final String appToken = new Regex(js, "app:\\s*'(.*?)',").getMatch(0);
                    ajaxPostPage("https://www.inclouddrive.com/api/0/signmein", "useraccess=&access_token=" + appToken + "&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&keep=1");
                    final String doz = Encoding.urlEncode(ajax.getRegex("\"doz\":\"([^\"]+)").getMatch(0));
                    if (!ajax.containsHTML("\"result\":\"ok\"") || StringUtils.isEmpty(doz)) {
                        final String errormessage = PluginJSonUtils.getJson(ajax, "message");
                        if (errormessage != null) {
                            /* 2019-12-23: E.g. {"result":"error","message":"Your account is blocked due to security reason."} */
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, errormessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    br.setCookie(br.getHost(), "userdata", doz);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.getCookie(br.getHost(), "userdata", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (inValidate(account.getUser()) || !account.getUser().matches(".+@.+")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Username has to be Email Address!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (inValidate(account.getPass())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Password can not be empty!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        getPage("/me");
        final String[] premExpire = br.getRegex(">(Premium)</div>[^<]*<div[^>]*>Expires on (\\d+ \\w+, \\d{4})<").getRow(0);
        if (premExpire == null) {
            ai.setStatus("Free Account");
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
        } else {
            final String expire = premExpire[1];
            if (expire == null) {
                logger.warning("Failed to find expiredate");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMM, yyyy", Locale.ENGLISH));
            }
            ai.setStatus("Premium Account");
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        }
        final String space = br.getRegex(">Used Space</div>\\s*<span[^>]*>(.*?) / \\d+ GB</span>").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(SizeFormatter.getSize(Encoding.htmlDecode(space)));
        }
        final Regex trafficleft = br.getRegex(">Used Bandwidth</div>\\s*(?:<span[^>]*>)?(\\d+(?:\\.\\d+)? GB)\\s*/\\s*(\\d+(?:\\.\\d+)? GB)\\s*<");
        final String trafficusedStr = trafficleft.getMatch(0);
        final String trafficmaxStr = trafficleft.getMatch(1);
        if (trafficusedStr != null && trafficmaxStr != null) {
            final long trafficused = SizeFormatter.getSize(trafficusedStr);
            final long trafficmax = SizeFormatter.getSize(trafficmaxStr);
            ai.setTrafficMax(trafficmax);
            ai.setTrafficLeft(trafficmax - trafficused);
        } else {
            logger.warning("Could not find trafficleft!");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        if (account.getType() == AccountType.FREE) {
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
                if (br.containsHTML(">we are performing a service upgrade please try again!")) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Maintenance is being done", 1 * 60 * 60 * 1000l);
                }
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

    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getBooleanProperty("premiumRequired", false) && (account == null || account.getType() == AccountType.FREE)) {
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
     */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    public void setAjaxBrowser(final Browser ajax) {
        this.ajax = ajax;
    }
}