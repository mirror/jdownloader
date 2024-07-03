//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.SankakucomplexComCrawler;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.SankakucomplexComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sankakucomplex.com" }, urls = { "https?://(?:beta|chan|idol|www)\\.sankakucomplex\\.com/(?:[a-z]{2}/)?(?:post/show|posts)/([A-Za-z0-9]+)" })
public class SankakucomplexCom extends PluginForHost {
    public SankakucomplexCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://chan.sankakucomplex.com/users/signup");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        setDefaultCookies(br, getHost());
        br.setFollowRedirects(true);
        return br;
    }

    private static void setDefaultCookies(final Browser br, final String host) {
        br.setCookie(host, "locale", "en");
        br.setCookie(host, "lang", "en");
        br.setCookie(host, "hide-news-ticker", "1");
        br.setCookie(host, "auto_page", "1");
        br.setCookie(host, "hide_resized_notice", "1");
        br.setCookie(host, "blacklisted_tags", "");
    }

    private final boolean              useAPI                                = true;
    public static final String         PROPERTY_UPLOADER                     = "uploader";
    public static final String         PROPERTY_DIRECTURL                    = "directurl";
    public static final String         PROPERTY_BOOK_TITLE                   = "book_title";
    public static final String         PROPERTY_TAGS_COMMA_SEPARATED         = "tags_comma_separated";
    public static final String         PROPERTY_IS_PREMIUMONLY               = "is_premiumonly";
    public static final String         PROPERTY_PAGE_NUMBER                  = "page_number";
    public static final String         PROPERTY_PAGE_NUMBER_MAX              = "page_number_max";
    public static final String         PROPERTY_SOURCE                       = "source";
    private final String               TIMESTAMP_LAST_TIME_FILE_MAYBE_BROKEN = "timestamp_last_time_file_maybe_broken";
    private static final String        PROPERTY_ACCOUNT_ACCESS_TOKEN         = "access_token";
    /* 2024-04-26: Refresh-token is currently not used. */
    private static final String        PROPERTY_ACCOUNT_REFRESH_TOKEN        = "refresh_token";
    /* Don't touch the following! */
    private static final AtomicInteger freeRunning                           = new AtomicInteger(0);

    @Override
    public String getAGBLink() {
        return "https://www.sankakucomplex.com/";
    }

    @Override
    public void init() {
        try {
            Browser.setBurstRequestIntervalLimitGlobal(this.getHost(), 3000, 20, 60000);
        } catch (Exception e) {
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        /* 2024-06-25: Set to 1 based on logs. */
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final AvailableStatus status = requestFileInformation(link, null);
        if (status == AvailableStatus.TRUE && link.hasProperty(PROPERTY_IS_PREMIUMONLY) && useAPI && !link.isSizeSet() && account != null) {
            /* Workaround for when some file information is missing when link leads to account-only content and is checked via API. */
            logger.info("Failed to find filesize via API and item is only available via account while we have an account -> Checking status again via website in hope to obtain all information");
            return requestFileInformationWebsite(link, account, false);
        } else {
            return status;
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (useAPI) {
            return requestFileInformationAPI(link, account, false);
        } else {
            return requestFileInformationWebsite(link, account, false);
        }
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fileID = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fileID);
        }
        final String host = new URL(link.getPluginPatternMatcher()).getHost();
        setDefaultCookies(br, host);
        if (account != null) {
            this.login(account, false);
        }
        br.getPage("https://chan." + getHost() + "/post/show/" + fileID);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)<title>\\s*404: Page Not Found\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(">\\s*You lack the access rights required to view this content") || br.containsHTML(">\\s*Nothing is visible to you here")) {
            /* Content can only be downloaded by premium users or mature content which can only be downloaded by logged in users. */
            link.setProperty(PROPERTY_IS_PREMIUMONLY, true);
        } else {
            link.removeProperty(PROPERTY_IS_PREMIUMONLY);
        }
        final String previouslyStoredDirecturl = checkDirectLink(link, PROPERTY_DIRECTURL);
        String dllink = br.getRegex("(?i)<li>Original: <a href=\"(//[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<a href=\"(//[^<>\"]*?)\">\\s*Save this file").getMatch(0);
            if (dllink == null) {
                /* 2024-04-26 */
                dllink = br.getRegex("Post\\.prepare_download\\(\\&#39;(//[^\"<>]+)&#39;,").getMatch(0);
            }
        }
        String title = fileID;
        String ext = null;
        if (dllink == null) {
            /* 2021-02-23: Image download - if the upper handling fails on videos, this may make us download an image vs a video */
            dllink = br.getRegex("<meta content=\"(//[^<>\"]+)\" property=og:image>").getMatch(0);
        }
        if (dllink != null) {
            dllink = br.getURL(dllink).toExternalForm();
            dllink = Encoding.htmlOnlyDecode(dllink);
        }
        if (dllink != null) {
            ext = Plugin.getFileNameExtensionFromURL(dllink);
        }
        link.setFinalFileName(this.applyFilenameExtension(title, ext));
        final String filesizeBytesStr = br.getRegex("([0-9,]+) bytes").getMatch(0);
        if (filesizeBytesStr != null) {
            link.setDownloadSize(Long.parseLong(filesizeBytesStr.replace(",", "")));
        }
        if (dllink != null) {
            link.setProperty(PROPERTY_DIRECTURL, dllink);
            if (filesizeBytesStr == null && previouslyStoredDirecturl == null && !isDownload) {
                try {
                    basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, title, ext);
                } catch (Exception e) {
                    logger.log(e);
                    logger.info("Final downloadurl did not lead to file -> File broken/unavailable serverside?");
                    return AvailableStatus.TRUE;
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fileID = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fileID);
        }
        if (account != null) {
            this.login(account, false);
        }
        br.getPage(SankakucomplexComCrawler.API_BASE + "/posts?lang=de&page=1&limit=1&tags=id_range:" + fileID);
        final Object obj = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
        if (obj instanceof Map) {
            /* We only get a map if something went wrong. */
            final Map<String, Object> errormap = (Map<String, Object>) obj;
            final String errorcode = (String) errormap.get("code");
            if (StringUtils.equalsIgnoreCase(errorcode, "snackbar__content-belongs-to-premium-client")) {
                link.setProperty(PROPERTY_IS_PREMIUMONLY, true);
                return AvailableStatus.TRUE;
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final List<Object> ressourcelist = (List<Object>) obj;
        if (ressourcelist.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> item = (Map<String, Object>) ressourcelist.get(0);
        parseFileInfoAndSetFilenameAPI(this, link, item);
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfoAndSetFilenameAPI(final Plugin plugin, final DownloadLink link, final Map<String, Object> item) {
        final Map<String, Object> author = (Map<String, Object>) item.get("author");
        link.setProperty(PROPERTY_UPLOADER, author.get("name"));
        // final boolean isActive = StringUtils.equalsIgnoreCase(item.get("status").toString(), "active");
        final String mimeType = item.get("file_type").toString();
        final String ext = plugin.getExtensionFromMimeType(mimeType);
        final Number file_size = (Number) item.get("file_size");
        if (file_size != null) {
            /*
             * Do not set verifiedFileSize since we don't know if we will download the original file in the end or a lower quality version.
             */
            // link.setVerifiedFileSize(file_size.longValue());
            link.setDownloadSize(file_size.longValue());
        }
        if ((Boolean) item.get("is_premium")) {
            // throw new AccountRequiredException();
            link.setProperty(PROPERTY_IS_PREMIUMONLY, true);
        } else {
            link.removeProperty(PROPERTY_IS_PREMIUMONLY);
        }
        final List<Map<String, Object>> tags = (List<Map<String, Object>>) item.get("tags");
        if (tags != null) {
            String tagsCommaSeparated = "";
            for (final Map<String, Object> tagInfo : tags) {
                String tag = (String) tagInfo.get("name_en");
                if (StringUtils.isEmpty(tag)) {
                    tag = (String) tagInfo.get("name_ja");
                }
                if (tagsCommaSeparated.length() > 0) {
                    tagsCommaSeparated += ",";
                }
                tagsCommaSeparated += tag;
            }
            if (tagsCommaSeparated.length() > 0) {
                link.setProperty(PROPERTY_TAGS_COMMA_SEPARATED, tagsCommaSeparated);
                if (PluginJsonConfig.get(SankakucomplexComConfig.class).isSetCommaSeparatedTagsOfPostsAsComment()) {
                    link.setComment(tagsCommaSeparated);
                }
            }
        }
        /* 2022-12-20: We can't trust this hash for all items. */
        final String md5hash = (String) item.get("md5");
        if (!StringUtils.isEmpty(md5hash)) {
            /*
             * Do not set md5 hash since we don't know if we will download the original file in the end or a lower quality version.
             */
            // link.setMD5Hash(md5hash);
        }
        link.setProperty(PROPERTY_DIRECTURL, item.get("file_url"));
        link.setProperty(PROPERTY_SOURCE, item.get("source"));
        link.setAvailable(true);
        final int pageNumber = link.getIntegerProperty(PROPERTY_PAGE_NUMBER, 0) + 1;
        final int pageNumberMax = link.getIntegerProperty(PROPERTY_PAGE_NUMBER_MAX, 0) + 1;
        if (pageNumberMax > 1) {
            link.setFinalFileName(StringUtils.formatByPadLength(StringUtils.getPadLength(pageNumberMax), pageNumber) + "_" + item.get("id") + "." + ext);
        } else {
            link.setFinalFileName(item.get("id") + "." + ext);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink == null) {
            requestFileInformationWebsite(link, account, true);
            dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                if (link.hasProperty(PROPERTY_IS_PREMIUMONLY) && account == null) {
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isResumeable(link, account), 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            /* Force generation of new directurl next time */
            link.removeProperty(PROPERTY_DIRECTURL);
            final long timestampLastTimeFileMaybeBroken = link.getLongProperty(TIMESTAMP_LAST_TIME_FILE_MAYBE_BROKEN, this.getMaxChunks(link, account));
            if (System.currentTimeMillis() - timestampLastTimeFileMaybeBroken <= 5 * 60 * 1000l) {
                /* Wait longer time before retry as we've just recently tried and it failed again. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file", 60 * 60 * 1000l);
            } else {
                /* Retry soon */
                link.setProperty(TIMESTAMP_LAST_TIME_FILE_MAYBE_BROKEN, System.currentTimeMillis());
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file or expired directurl", 30 * 1000l);
            }
        }
        /* Add a download slot */
        controlMaxFreeDownloads(account, link, +1);
        try {
            /* Start download */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(account, link, -1);
        }
    }

    private String checkDirectLink(final DownloadLink link, final String directlinkproperty) {
        String dllink = link.getStringProperty(directlinkproperty);
        if (dllink != null) {
            boolean valid = false;
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                try {
                    if (this.looksLikeDownloadableContent(con) && !con.getURL().getPath().contains("expired.png")) {
                        valid = true;
                        return dllink;
                    } else {
                        return null;
                    }
                } finally {
                    con.disconnect();
                }
            } catch (final Exception e) {
                logger.log(e);
            } finally {
                if (!valid) {
                    link.setProperty(directlinkproperty, Property.NULL);
                }
            }
        }
        return null;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            final String logincheckurl = "https://chan." + this.getHost() + "/en/users/home";
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(cookies);
                if (!force) {
                    /* Do not validate cookies */
                    return false;
                }
                br.getPage(logincheckurl);
                if (this.isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            /*
             * 2024-04-26: It is really important to have the right URL here in order to properly login in a way which also works for 'old'
             * "chan." subdomain.
             */
            br.getPage("https://login." + getHost() + "/oidc/auth?response_type=code&scope=openid&client_id=sankaku-channel-legacy&redirect_uri=https%3A%2F%2Fchan.sankakucomplex.com%2Fsso%2Fcallback&route=login");
            final String _grant = br.getCookie(br.getHost(), "_grant", Cookies.NOTDELETEDPATTERN);
            if (StringUtils.isEmpty(_grant)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Object> loginpost = new HashMap<String, Object>();
            final Map<String, Object> loginpost_mfaParams = new HashMap<String, Object>();
            loginpost_mfaParams.put("login", account.getUser());
            loginpost.put("login", account.getUser());
            loginpost.put("mfaParams", loginpost_mfaParams);
            loginpost.put("password", account.getPass());
            br.postPageRaw("/auth/token", JSonStorage.serializeToJson(loginpost));
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (!Boolean.TRUE.equals(entries.get("success"))) {
                /* E.g. {"success":false,"code":"snackbar-message__not_found"} */
                throw new AccountInvalidException();
            }
            final String access_token = entries.get("access_token").toString();
            final String refresh_token = entries.get("refresh_token").toString();
            if (StringUtils.isEmpty(access_token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie(br.getHost(), "accessToken", access_token);
            br.setCookie(br.getHost(), "position", "0");
            br.setCookie(br.getHost(), "refreshToken", refresh_token);
            // br.setCookie(br.getHost(), "ssoLoginValid", System.currentTimeMillis() + "");
            final UrlQuery query = new UrlQuery();
            query.add("access_token", Encoding.urlEncode(access_token));
            query.add("state", "lang=en&theme=white");
            br.postPage("/oidc/interaction/" + _grant + "/login", query);
            /* Double-check */
            br.getPage(logincheckurl);
            if (!this.isLoggedin(br)) {
                throw new AccountInvalidException("Unknown login failure");
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, access_token);
            account.setProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN, refresh_token);
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("/users/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        final AccountInfo ai = new AccountInfo();
        ai.setUnlimitedTraffic();
        if (br.containsHTML("(?i)>\\s*Subscription Level\\s*:\\s*<a href=\"[^\"]+\">\\s*Plus\\s*<")) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* 2022-12-22: Set limit to 1 as I was too lazy to add sequential download-start handling for account mode. */
        return 1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return freeRunning.get() + 1;
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

    @Override
    public Class<? extends SankakucomplexComConfig> getConfigInterface() {
        return SankakucomplexComConfig.class;
    }
}
