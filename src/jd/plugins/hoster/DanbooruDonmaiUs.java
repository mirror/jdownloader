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

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.plugins.components.config.DanbooruDonmaiUsConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "danbooru.donmai.us" }, urls = { "https?://(?:www\\.)?danbooru\\.donmai\\.us/posts/(\\d+)" })
public class DanbooruDonmaiUs extends PluginForHost {
    public DanbooruDonmaiUs(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://danbooru.donmai.us/users/new");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean resume                       = false;
    private static final int     maxchunks                    = 1;
    private static final int     free_maxdownloads            = -1;
    public static final String   PROPERTY_DIRECTURL           = "directurl";
    public static final String   PROPERTY_PREMIUMONLY         = "premiumonly";
    public static final String   PROPERTY_ACCOUNT_QUERY_LIMIT = "query_limit";
    public static final String   API_BASE                     = "https://danbooru.donmai.us";

    @Override
    public String getAGBLink() {
        return API_BASE + "/static/terms_of_service";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("https://", "http://"));
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
    public void setBrowser(final Browser br) {
        super.setBrowser(br);
        final String userAgent = PluginJsonConfig.get(DanbooruDonmaiUsConfig.class).getUserAgent();
        if (!StringUtils.isEmpty(userAgent) && !userAgent.equalsIgnoreCase("JDDEFAULT")) {
            this.br.setHeader(HTTPConstants.HEADER_REQUEST_USER_AGENT, userAgent);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (account != null) {
            return requestFileInformationAPI(link, account, isDownload);
        } else {
            return requestFileInformationWebsite(link, account, isDownload);
        }
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (account == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.loginAPI(account, false);
        br.setFollowRedirects(true);
        if (checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
            logger.info("Availablecheck via directurl successful");
            return AvailableStatus.TRUE;
        }
        br.getPage(API_BASE + "/posts/" + this.getFID(link) + ".json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (!entries.containsKey("id")) {
            throw new AccountUnavailableException("Invalid apikey or insufficient permissions", 5 * 60 * 1000l);
        }
        parseFileInformationAPI(link, entries);
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
            logger.info("Availablecheck via directurl successful");
            return AvailableStatus.TRUE;
        }
        br.setAllowedResponseCodes(451);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 451) {
            // Unavailable For Legal Reasons
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*The artist requested removal of this page")) {
            /* 2021-04-19 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*You need a gold account to see this image")) {
            /* Premiumonly */
            link.setProperty(PROPERTY_PREMIUMONLY, true);
            if (isDownload) {
                throw new AccountRequiredException();
            } else {
                return AvailableStatus.TRUE;
            }
        }
        link.removeProperty(PROPERTY_PREMIUMONLY);
        String title = br.getRegex("<title>([^<>\"]+). \\s*Danbooru\\s*</title>").getMatch(0);
        if (title != null) {
            title = this.getFID(link) + "_" + title;
        } else {
            /* Fallback */
            title = this.getFID(link);
        }
        String dllink = br.getRegex("href=\"([^<>\"]+)\">\\s*view original").getMatch(0); // Not always available
        if (dllink != null && dllink.contains("?original=1")) { // https://board.jdownloader.org/showthread.php?t=77260&post#3
            br.getPage(dllink);
            dllink = br.getRegex("<a href=\"([^<>\"]+)\">\\s*Save as").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("Size: <a href=\"([^<>\"]+)\"").getMatch(0); // Picture or video
            if (dllink == null) {
                dllink = br.getRegex("property=\"og:image\" content=\"(http[^<>\"]+)\"").getMatch(0); // Always picture
            }
        }
        final String size = br.getRegex("Size:\\s*<a href=\"(?:(?:https?://danbooru\\.donmai\\.us)?/data/[^<>\"]+)\">(.*?)<").getMatch(0);
        final long orgSize = size != null ? SizeFormatter.getSize(size) : -1l;
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title);
        title = title.trim();
        if (dllink != null) {
            String filename = title;
            final String ext = getFileNameExtensionFromString(dllink, ".jpg");
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
            final String md5 = new Regex(dllink, "([a-f0-9]{32})").getMatch(0);
            if (md5 != null) {
                link.setMD5Hash(md5);
            }
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        } else {
            link.setName(title + ".jpg");
        }
        if (orgSize > 0) {
            link.setDownloadSize(orgSize);
            return AvailableStatus.TRUE;
        }
        if (dllink != null && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static void parseFileInformationAPI(final DownloadLink dl, final Map<String, Object> entries) {
        final long filesize = ((Number) entries.get("file_size")).longValue();
        /* This md5 is also contained in their final download-URLs! */
        final String md5 = (String) entries.get("md5");
        final String ext = (String) entries.get("file_ext");
        final String tagStringCharacter = (String) entries.get("tag_string_character");
        final String tagStringArtist = (String) entries.get("tag_string_artist");
        // String directurl = (String) entries.get("large_file_url");
        // if (StringUtils.isEmpty(directurl)) {
        // directurl = (String) entries.get("file_url");
        // }
        final String directurl = (String) entries.get("file_url");
        /* 2021-04-19: Use old website-crawler naming scheme. */
        dl.setFinalFileName(entries.get("id") + "_" + tagStringCharacter + " drawn by " + tagStringArtist + "." + ext);
        // dl.setFinalFileName(tagStringArtist + "_" + postID + "_" + tagStringCharacter + "." + ext);
        if (!StringUtils.isEmpty(directurl)) {
            dl.setProperty(jd.plugins.hoster.DanbooruDonmaiUs.PROPERTY_DIRECTURL, directurl);
        }
        if (!StringUtils.isEmpty(md5)) {
            dl.setMD5Hash(md5);
        }
        if (filesize > 0) {
            dl.setVerifiedFileSize(filesize);
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        if (!attemptStoredDownloadurlDownload(link, PROPERTY_DIRECTURL, resume, maxchunks)) {
            requestFileInformation(link, null, true);
            final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection();
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: No downloadable content");
            }
        }
        dl.startDownload();
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
                link.removeProperty(directlinkproperty);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    /**
     * Using API: https://danbooru.donmai.us/wiki_pages/help:api </br> Account types and associated API limits:
     * https://danbooru.donmai.us/wiki_pages/help%3Ausers
     */
    public boolean loginAPI(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                /* 2021-04-19: Seems like lowercase username is required. */
                br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser().toLowerCase(Locale.ENGLISH) + ":" + account.getPass()));
                if (!force) {
                    return false;
                }
                br.getPage(API_BASE + "/profile.json");
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final boolean success = entries.containsKey("success") ? ((Boolean) entries.get("success")).booleanValue() : true;
                if (!success) {
                    if (account.hasProperty(PROPERTY_ACCOUNT_QUERY_LIMIT)) {
                        /* User has been logged in successfully at least once -> Maybe user has revoked API access (or deleted apikey). */
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid apikey or username", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        showAPILoginInformation();
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login with username and apikey required", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                return true;
            } catch (final PluginException e) {
                throw e;
            }
        }
    }

    private Thread showAPILoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String title = "Danbooru - Login";
                    String message = "Hello dear Danbooru user";
                    message += "\r\nIn order to use an account of this service in JDownloader, you need to follow these instructions:";
                    message += "\r\n1. Open the following page in your browser and login if you haven't already: danbooru.donmai.us/profile";
                    message += "\r\n2. See 'API Key' -> Click on 'View' --> Click on 'Add' (top right corner) to add a new API key if you haven't created one already.";
                    message += "\r\nEnter an API key name of your choice and leave the 'IP Addresses' field blank unless you know what you're doing.";
                    message += "\r\nIf you care about account security, select only the following lines under 'Permissions': 'posts:index', 'posts:show', 'users:profile'.";
                    message += "\r\n3. Close this message and try add your account to JD again: This time put your Danbooru username into the username field and apikey into the password field.";
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(5 * 60 * 1000);
                    /* 2021-04-19: I've decided not to auto-open this URL! */
                    // final String profileURL = "https://danbooru.donmai.us/profile";
                    // if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                    // CrossSystem.openURL(profileURL);
                    // }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    // /** 2021-04-19: We can't use this due to them using Cloudflare! */
    // public boolean loginWebsite(final Account account, final boolean force) throws Exception {
    // synchronized (account) {
    // try {
    // br.setFollowRedirects(true);
    // br.setCookiesExclusive(true);
    // final Cookies cookies = account.loadCookies("");
    // if (cookies != null) {
    // logger.info("Attempting cookie login");
    // this.br.setCookies(this.getHost(), cookies);
    // if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
    // logger.info("Cookies are still fresh --> Trust cookies without login");
    // return false;
    // }
    // br.getPage("https://" + this.getHost() + "/");
    // if (this.isLoggedinWebsite()) {
    // logger.info("Cookie login successful");
    // /* Refresh cookie timestamp */
    // account.saveCookies(this.br.getCookies(this.getHost()), "");
    // return true;
    // } else {
    // logger.info("Cookie login failed");
    // }
    // }
    // logger.info("Performing full login");
    // br.getPage("https://" + this.getHost() + "/login?url=%2F");
    // final Form loginform = br.getFormbyActionRegex("/session.*");
    // if (loginform == null) {
    // logger.warning("Failed to find loginform");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // loginform.put("session[name]", Encoding.urlEncode(account.getUser()));
    // loginform.put("session[password]", Encoding.urlEncode(account.getPass()));
    // br.submitForm(loginform);
    // if (!isLoggedinWebsite()) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // account.saveCookies(this.br.getCookies(this.getHost()), "");
    // return true;
    // } catch (final PluginException e) {
    // if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
    // account.clearCookies("");
    // }
    // throw e;
    // }
    // }
    // }
    //
    // private boolean isLoggedinWebsite() {
    // return br.containsHTML("/logout\"");
    // }
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPI(account, true);
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        /* Level == AccountType --> https://danbooru.donmai.us/user_upgrades/new */
        final String levelString = (String) entries.get("level_string");
        final Number tagQueryLimit = ((Number) entries.get("tag_query_limit"));
        if (tagQueryLimit != null) {
            account.setProperty(PROPERTY_ACCOUNT_QUERY_LIMIT, tagQueryLimit.intValue());
        }
        if (levelString != null && levelString.matches("(?i)Gold|Platinum")) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        ai.setStatus("Level: " + levelString);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /**
         * 2021-04-19: Account benefits are only used in crawler and not required for downloading single items! </br> Also, website is used
         * for downloading and API is only used for crawling.
         */
        if (!attemptStoredDownloadurlDownload(link, PROPERTY_DIRECTURL, resume, maxchunks)) {
            requestFileInformationAPI(link, account, true);
            final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: No downloadable content");
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (link.hasProperty(PROPERTY_PREMIUMONLY)) {
            /* Without account its not possible to download this link. */
            if (account != null && account.getType() == AccountType.PREMIUM) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
    public Class<? extends DanbooruDonmaiUsConfig> getConfigInterface() {
        return DanbooruDonmaiUsConfig.class;
    }
}
