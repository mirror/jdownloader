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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.RecurbateComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
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
        this.enablePremium("https://recurbate.com/signup");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags: Porn plugin
    private final int          free_maxdownloads       = 1;
    public static final String PROPERTY_DATE           = "date";
    public static final String PROPERTY_DATE_ORIGINAL  = "date_original";
    public static final String PROPERTY_DATE_TIMESTAMP = "date_timestamp";
    public static final String PROPERTY_USER           = "username";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "recurbate.com", "recurbate.cc" });
        return ret;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/play\\.php\\?video=(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://recurbate.com/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            /* Set fallback filename e.g. for offline items. */
            link.setName(fid + ".mp4");
        }
        if (account != null) {
            this.login(account, link.getPluginPatternMatcher(), true);
        } else {
            br.setFollowRedirects(true);
            br.getPage(link.getPluginPatternMatcher());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String dateStr = br.getRegex("(?i)show recorded at (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})").getMatch(0);
        if (dateStr == null) {
            dateStr = br.getRegex("(?i)show on (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})").getMatch(0);
        }
        setDate(link, dateStr);
        String performer = br.getRegex("/performer/([^/\"<>]+)").getMatch(0);
        if (performer != null) {
            performer = Encoding.htmlDecode(performer).trim();
            link.setProperty(PROPERTY_USER, performer);
        }
        setFilename(link, fid);
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

    public static void setFilename(DownloadLink link, final String fid) {
        final String performer = link.getStringProperty(PROPERTY_USER, null);
        final String dateStr = link.getStringProperty(PROPERTY_DATE, null);
        if (dateStr != null && performer != null) {
            link.setFinalFileName(dateStr + "_" + performer + "_" + fid + ".mp4");
        } else if (performer != null) {
            link.setFinalFileName(performer + "_" + fid + ".mp4");
        } else {
            /* Fallback */
            link.setFinalFileName(fid + ".mp4");
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private final boolean RESUMABLE = true;
    private final int     MAXCHUNKS = -2;

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty;
        if (account != null) {
            directurlproperty = "directlink_account";
        } else {
            directurlproperty = "directlink";
        }
        if (!this.attemptStoredDownloadurlDownload(link, directurlproperty, RESUMABLE, MAXCHUNKS)) {
            requestFileInformation(link, account);
            checkErrors(br, link, account);
            if (account != null && !this.isLoggedin(br)) {
                throw new AccountUnavailableException("Session expired?", 30 * 1000l);
            }
            /*
             * Official downloadlinks are only available for "Ultimate" users. Those can download much faster and with an "unlimited"
             * amount.
             */
            final String officialDownloadlink = br.getRegex("recu-link download\" href=\"(https?://[^\"]+)").getMatch(0);
            if (officialDownloadlink != null) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, officialDownloadlink, RESUMABLE, MAXCHUNKS);
            } else {
                final String token = br.getRegex("data-token\\s*=\\s*\"([a-f0-9]{64})\"").getMatch(0);
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
                query.add("video", this.getFID(link));
                query.add("token", token);
                brc.getPage("/api/get.php?" + query.toString());
                final String streamLink = brc.getRegex("<source\\s*src\\s*=\\s*\"(https?://[^\"]+)\"[^>]*type=\"video/mp4\"\\s*/>").getMatch(0);
                if (streamLink == null) {
                    if (StringUtils.containsIgnoreCase(brc.toString(), "shall_signin")) {
                        /**
                         * Free users can watch one video per IP per X time. </br>
                         * This error should only happen in logged-out state.
                         */
                        errorDailyDownloadlimitReached(account);
                    } else if (StringUtils.containsIgnoreCase(brc.toString(), "shall_subscribe")) {
                        errorDailyDownloadlimitReached(account);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, streamLink, RESUMABLE, MAXCHUNKS);
            }
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
             * 2021-09-01: Save to re-use later. This URL is valid for some minutes only but allows resume + chunkload (up to 3 connections
             * - we allow max. 2.).
             */
            link.setProperty(directurlproperty, dl.getConnection().getURL().toString());
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

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(directlinkproperty);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private boolean login(final Account account, final String checkURL, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
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
                this.br.setCookies(this.getHost(), userCookies);
                if (!force) {
                    /* Do not validate cookies */
                    return false;
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
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
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
        login(account, "https://" + this.getHost() + "/account.php", true);
        ai.setUnlimitedTraffic();
        final String nickname = br.getRegex("(?i)Nickname\\s*</div>\\s*<div class=\"col-sm-8\">\\s*([^<>\"]+)").getMatch(0);
        if (nickname != null) {
            /* User can theoretically enter whatever he wants in username field when doing cookie login --> We prefer unique usernames. */
            account.setUser(nickname);
        } else {
            logger.warning("Failed to find nickname in HTML");
        }
        final String plan = br.getRegex("(?i)<span class=\"plan-name\"[^>]*>\\s*([^<]+)\\s*</span>").getMatch(0);
        if (plan != null) {
            if (plan.equalsIgnoreCase("Basic")) {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(free_maxdownloads);
            } else {
                /* "Premium" or "Ultimate" plan */
                final String expire = br.getRegex("(?i)Expire on\\s*</div>\\s*<div class=\"col-sm-8\">\\s*([A-Za-z]+ \\d{1,2}, \\d{4})").getMatch(0);
                if (expire != null) {
                    /* Allow premium accounts without expire-date although all premium accounts should have an expire-date. */
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMM dd, yyyy", Locale.ENGLISH));
                } else {
                    logger.warning("Failed to find expire-date for premium account!");
                }
                account.setType(AccountType.PREMIUM);
                account.setConcurrentUsePossible(true);
                account.setMaxSimultanDownloads(PluginJsonConfig.get(RecurbateComConfig.class).getMaxSimultanPaidAccountDownloads());
                ai.setStatus(plan);
            }
            ai.setStatus("Plan: " + plan);
        } else {
            /* This should never happen */
            account.setType(AccountType.UNKNOWN);
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
        return free_maxdownloads;
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