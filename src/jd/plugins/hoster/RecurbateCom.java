//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.RecurbateComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RecurbateCom extends PluginForHost {
    public RecurbateCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/signup");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
    }

    /* DEV NOTES */
    public static final String PROPERTY_DATE           = "date";
    public static final String PROPERTY_DATE_ORIGINAL  = "date_original";
    public static final String PROPERTY_DATE_TIMESTAMP = "date_timestamp";
    public static final String PROPERTY_USER           = "username";

    @Override
    public Browser createNewBrowserInstance() {
        final Browser brnew = new Browser();
        brnew.setFollowRedirects(true);
        final String customUserAgent = PluginJsonConfig.get(RecurbateComConfig.class).getCustomUserAgent();
        if (!StringUtils.isEmpty(customUserAgent) && !StringUtils.equalsIgnoreCase(customUserAgent, "JDDEFAULT")) {
            brnew.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, customUserAgent);
        }
        return brnew;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "recu.me", "recurbate.me", "recurbate.com", "recurbate.xyz", "recurbate.cc" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* Their main domain is frequently changing. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("recurbate.com");
        deadDomains.add("recurbate.xyz");
        deadDomains.add("recurbate.cc");
        /* 2024-02-19 */
        deadDomains.add("recurbate.me");
        return deadDomains;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:play\\.php\\?video=|(?:[\\w\\-]+/)?video/)(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://recurbate.com/terms";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final Account account) {
        /* Max chunks for progressive / full-video downloads. */
        return -2;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getVideoID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getVideoID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        return getContentURL(link);
    }

    private String getContentURL(final DownloadLink link) {
        /* Get full host with subdomain and correct base domain. */
        final List<String> deadDomains = this.getDeadDomains();
        final String domainFromURL = Browser.getHost(link.getPluginPatternMatcher());
        final String domainToUse;
        if (deadDomains != null && deadDomains.contains(domainFromURL)) {
            /* Fallback to plugin domain */
            /* e.g. down.xx.com -> down.yy.com, keep subdomain(s) */
            domainToUse = this.getHost();
        } else {
            domainToUse = domainFromURL;
        }
        return "https://" + domainToUse + "/video/" + this.getVideoID(link) + "/play";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            /* Set fallback filename e.g. for offline items. */
            setFilename(link, false);
        }
        final String contenturl = getContentURL(link);
        if (account != null) {
            login(account, contenturl, true);
        } else {
            br.getPage(contenturl);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String dateStr = br.getRegex("show recorded at (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})").getMatch(0);
        if (dateStr == null) {
            dateStr = br.getRegex("show on (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})").getMatch(0);
        }
        setDate(link, dateStr);
        String performer = br.getRegex("class=\"performer\"[^<]*href=\"/performer/([^\"]+)\"").getMatch(0);
        if (performer == null) {
            performer = br.getRegex("href=\"/performer/([^/\"]+)/similar\"[^>]*>\\s*See all similar recordings").getMatch(0);
        }
        if (performer != null) {
            performer = Encoding.htmlDecode(performer).trim();
            link.setProperty(PROPERTY_USER, performer);
        }
        setFilename(link, true);
        return AvailableStatus.TRUE;
    }

    public static void setDate(DownloadLink link, final String dateStr) {
        if (dateStr != null) {
            final long dateTimestamp = TimeFormatter.getMilliSeconds(dateStr, "yyyy-MM-dd HH:mm", Locale.ENGLISH);
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            final Date theDate = new Date(dateTimestamp);
            /* Save timestamp separately so we can later convert that into any format we want. */
            link.setProperty(PROPERTY_DATE_TIMESTAMP, dateTimestamp);
            /* Save date in the form it's presented by the website. */
            link.setProperty(PROPERTY_DATE_ORIGINAL, dateStr);
            /* Save date only in format yyyy-MM-dd. */
            link.setProperty(PROPERTY_DATE, formatter.format(theDate));
        }
    }

    public void setFilename(final DownloadLink link, final boolean setFinalFilename) {
        final String extDefault = ".mp4";
        final String videoid = this.getVideoID(link);
        String performer = link.getStringProperty(PROPERTY_USER);
        final String performerFromURL = new Regex(link.getPluginPatternMatcher(), "(?i)/([\\w\\-]+)/video/\\d+$").getMatch(0);
        if (performer == null) {
            /* 2024-06-12: Performer/username can be in added URL from now on. */
            performer = performerFromURL;
        }
        final String dateStr = link.getStringProperty(PROPERTY_DATE);
        final String filename;
        if (dateStr != null && performer != null) {
            filename = dateStr + "_" + performer + "_" + videoid + extDefault;
        } else if (performer != null) {
            filename = performer + "_" + videoid + extDefault;
        } else {
            /* Fallback */
            filename = videoid + extDefault;
        }
        if (setFinalFilename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty;
        if (account != null) {
            directurlproperty = "directlink_account";
        } else {
            directurlproperty = "directlink";
        }
        String dllink = null;
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
            /* 2023-12-21: Not mandatory but we're setting this header anyways. */
            br.getHeaders().put("Referer", this.getContentURL(link));
        } else {
            requestFileInformation(link, account);
            checkErrors(br, link, account);
            if (account != null && !this.isLoggedin(br)) {
                throw new AccountUnavailableException("Session expired?", 30 * 1000l);
            }
            /*
             * Official downloadlinks are only available for "Ultimate" users. Those can download much faster and with an "unlimited"
             * amount.
             */
            final String officialHighspeedDownloadlink = br.getRegex("(/video/\\d+/download/?)").getMatch(0);
            if (officialHighspeedDownloadlink != null) {
                dllink = officialHighspeedDownloadlink;
                logger.info("Found highspeed downloadurl: " + officialHighspeedDownloadlink);
            } else {
                logger.info("Failed to find highspeed downloadurl -> Trying to download stream");
                final String token = br.getRegex("data-token\\s*=\\s*\"([^\"]+)\"\\s*data-video-id").getMatch(0);
                if (token == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.setCookie(br.getHost(), "im18", "true");
                br.setCookie(br.getHost(), "im18_ets", Long.toString(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
                br.setCookie(br.getHost(), "im18_its", Long.toString(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                brc.getHeaders().put("Accept", "*/*");
                final UrlQuery query = new UrlQuery();
                query.add("token", Encoding.urlEncode(token));
                brc.getPage("/api/video/" + this.getVideoID(link) + "?" + query.toString());
                final String streamLink = brc.getRegex("<source\\s*src\\s*=\\s*\"(https?://[^\"]+)\"[^>]*type=\"video/mp4\"\\s*/>").getMatch(0);
                final String hlsURL = brc.getRegex("<source\\s*src\\s*=\\s*\"(https?://[^\"]+)\"[^>]*type=\"application/x-mpegURL\"").getMatch(0);
                if (streamLink == null && hlsURL == null) {
                    final String html = brc.getRequest().getHtmlCode();
                    if (StringUtils.containsIgnoreCase(html, "shall_signin")) {
                        /**
                         * Free users can watch one video per IP per X time. </br>
                         * This error should only happen in logged-out state.
                         */
                        errorDailyDownloadlimitReached(account);
                    } else if (StringUtils.containsIgnoreCase(html, "shall_subscribe")) {
                        errorDailyDownloadlimitReached(account);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (streamLink != null) {
                    dllink = streamLink;
                } else {
                    dllink = hlsURL;
                }
            }
            if (Encoding.isHtmlEntityCoded(dllink)) {
                /* Fix encoding */
                dllink = Encoding.htmlOnlyDecode(dllink);
            }
        }
        try {
            if (StringUtils.containsIgnoreCase(dllink, ".m3u8")) {
                /* HLS download (new 2023-12-19) */
                checkFFmpeg(link, "Download a HLS Stream");
                dl = new HLSDownloader(link, br, dllink);
                /*
                 * Save direct-url for later.
                 */
                link.setProperty(directurlproperty, dllink);
            } else {
                /* Progressive HTTP video stream download */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 429) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 429 too many requests", 60 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                    }
                }
                /*
                 * Save direct-url for later.
                 */
                link.setProperty(directurlproperty, dl.getConnection().getURL().toExternalForm());
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    private void errorDailyDownloadlimitReached(final Account account) throws PluginException {
        if (account != null) {
            throw new AccountUnavailableException("Daily downloadlimit reached", 60 * 60 * 1000l);
        } else {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached", 10 * 60 * 1000l);
        }
    }

    public boolean login(final Account account, final String checkURL, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies userCookies = account.loadUserCookies();
            if (userCookies == null) {
                /**
                 * 2021-09-28: They're using Cloudflare on their login page thus we only accept cookie login at this moment.</br>
                 * Login page: https://recurbate.com/signin
                 */
                /* Only display cookie login instructions on first login attempt */
                if (!account.hasEverBeenValid()) {
                    showCookieLoginInfo();
                }
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            logger.info("Attempting user cookie login");
            /* Set cookies on all supported domains */
            br.setCookies(userCookies);
            for (final String[] domains : getPluginDomains()) {
                for (final String domain : domains) {
                    br.setCookies(domain, userCookies);
                }
            }
            if (!force) {
                /* Do not validate cookies */
                return false;
            }
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* 2024-06-12: debug-test */
                br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                br.getHeaders().put("Accept-Encoding", "gzip, deflate, br, zstd");
                br.getHeaders().put("Accept-Language", "de-DE,de;q=0.9,en;q=0.8,en-US;q=0.7");
                br.getHeaders().put("Cache-Control", "max-age=0");
                br.getHeaders().put("Priority", "u=0, i");
                // br.getHeaders().put("Referer", "https://recu.me/username/video/12345678/play");
                br.getHeaders().put("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"");
                br.getHeaders().put("Sec-Ch-Ua-Arch", "\"x86\"");
                br.getHeaders().put("Sec-Ch-Ua-Bitness", "\"64\"");
                br.getHeaders().put("Sec-Ch-Ua-Full-Version", "\"125.0.6422.142\"");
                br.getHeaders().put("Sec-Ch-Ua-Full-Version-List", "\"Google Chrome\";v=\"125.0.6422.142\", \"Chromium\";v=\"125.0.6422.142\", \"Not.A/Brand\";v=\"24.0.0.0\"");
                br.getHeaders().put("Sec-Ch-Ua-Mobile", "?0");
                br.getHeaders().put("Sec-Ch-Ua-Model", "\"\"");
                br.getHeaders().put("Sec-Ch-Ua-Platform", "\"Windows\"");
                br.getHeaders().put("Sec-Ch-Ua-Platform-Version", "\"10.0.0\"");
                br.getHeaders().put("Sec-Fetch-Dest", "document");
                br.getHeaders().put("Sec-Fetch-Mode", "navigate");
                br.getHeaders().put("Sec-Fetch-Site", "same-origin");
                br.getHeaders().put("Sec-Fetch-User", "?1");
                br.getHeaders().put("Upgrade-Insecure-Requests", "1");
            }
            br.getPage(checkURL);
            checkForIPBlocked(br, null, account);
            if (this.isLoggedin(br)) {
                logger.info("User cookie login successful");
                return true;
            } else {
                logger.info("Cookie login failed");
                if (account.hasEverBeenValid()) {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                } else {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                }
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("/signout\"")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, "https://" + this.getHost() + "/account", true);
        ai.setUnlimitedTraffic();
        String username = br.getRegex("(?i)Nickname\\s*</div>\\s*<div class=\"col-sm-8\">\\s*([^<>\"]+)").getMatch(0);
        if (username == null) {
            username = br.getRegex("class='fas fa-user-circle'></i>\\s*<span class=\"crop-ellipsis\"[^>]*title=\"([^\"]+)").getMatch(0);
            if (username == null) {
                // 2024-02-20
                username = br.getRegex("class='far fa-user-circle'></i>\\s*<span class=\"crop-ellipsis\" title=\"([^\"]+)").getMatch(0);
            }
        }
        if (username != null) {
            /* User can theoretically enter whatever he wants in username field when doing cookie login --> We prefer unique usernames. */
            account.setUser(username);
        } else {
            logger.warning("Failed to find nickname in HTML");
        }
        final String plan = br.getRegex("(?i)<span class=\"plan-name\"[^>]*>\\s*([^<]+)\\s*</span>").getMatch(0);
        if (plan != null) {
            if (plan.equalsIgnoreCase("Basic")) {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(this.getMaxSimultanFreeDownloadNum());
            } else {
                /* "Premium" or "Ultimate" plan */
                final String expire = br.getRegex("(?i)Expire on\\s*</div>\\s*<div class=\"col-sm-8\">\\s*([A-Za-z]+ \\d{1,2}, \\d{4})").getMatch(0);
                if (expire != null) {
                    /* Allow premium accounts without expire-date although all premium accounts should have an expire-date. */
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMM dd, yyyy", Locale.ENGLISH), br);
                } else {
                    logger.warning("Failed to find expire-date for premium account!");
                }
                account.setType(AccountType.PREMIUM);
                account.setConcurrentUsePossible(true);
                account.setMaxSimultanDownloads(PluginJsonConfig.get(RecurbateComConfig.class).getMaxSimultanPaidAccountDownloads());
                ai.setStatus(plan);
            }
        } else {
            /* This should never happen */
            account.setType(AccountType.UNKNOWN);
        }
        ai.setStatus("Plan: " + plan);
        /* 2023-04-06: Some debug stuff down below. */
        final boolean devSetCookieExpiredateAsAccountExpiredate = false;
        final Cookie loginCookie = br.getCookies(br.getHost()).get("akey");
        if (loginCookie == null) {
            logger.warning("Failed to find cookie with key 'akey'");
        } else if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && devSetCookieExpiredateAsAccountExpiredate) {
            logger.info("!Developer: Setting cookie expiredate as account expiredate!");
            ai.setValidUntil(loginCookie.getExpireDate(), br);
        }
        checkErrors(br, null, account);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return PluginJsonConfig.get(RecurbateComConfig.class).getMaxSimultanPaidAccountDownloads();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private void checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        /* 2021-10-11: Very interesting: While this is happening, users will still get 1 free view without account. */
        checkForIPBlocked(br, link, account);
        if ((account == null || account.getType() == AccountType.FREE || account.getType() == AccountType.UNKNOWN) && br.containsHTML("(?i)Sorry guys, but due to the high load.*Basic \\(Free\\).*accounts are temporary limited to")) {
            throw new AccountUnavailableException("Free accounts are temporarily limited to 0 video views", 5 * 60 * 1000);
        }
    }

    private void checkForIPBlocked(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (isIPBlocked(br)) {
            if (link == null) {
                throw new AccountUnavailableException("IP blocked", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "IP blocked", 5 * 60 * 1000l);
            }
        }
    }

    /** Checks for rate-limit/Cloudflare block. */
    private boolean isIPBlocked(final Browser br) {
        if (br.containsHTML("(?i)RecuSec Access Forbidden\\s*<|Hey dude, you want to break my site")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Class<? extends RecurbateComConfig> getConfigInterface() {
        return RecurbateComConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}