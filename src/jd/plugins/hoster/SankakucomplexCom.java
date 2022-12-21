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
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.SankakucomplexComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sankakucomplex.com" }, urls = { "https?://(?:www\\.)?(?:beta|chan|idol)\\.sankakucomplex\\.com/(?:[a-z]{2}/)?post/show/(\\d+)" })
public class SankakucomplexCom extends antiDDoSForHost {
    public SankakucomplexCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://chan.sankakucomplex.com/user/signup");
    }

    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension             = ".jpg";
    private final boolean       useAPI                        = true;
    public static final String  PROPERTY_UPLOADER             = "uploader";
    public static final String  PROPERTY_DIRECTURL            = "directurl";
    public static final String  PROPERTY_BOOK_TITLE           = "book_title";
    public static final String  PROPERTY_TAGS_COMMA_SEPARATED = "tags_comma_separated";
    public static final String  PROPERTY_IS_PREMIUMONLY       = "is_premiumonly";
    public static final String  PROPERTY_PAGE_NUMBER          = "page_number";
    public static final String  PROPERTY_PAGE_NUMBER_MAX      = "page_number_max";

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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (useAPI) {
            return requestFileInformationAPI(link, account, false);
        } else {
            return requestFileInformationWebsite(link, account, false);
        }
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fileID = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
        if (!link.isNameSet()) {
            link.setName(fileID);
        }
        br.setFollowRedirects(true);
        final String host = new URL(link.getPluginPatternMatcher()).getHost();
        br.setCookie("https://" + host, "locale", "en");
        br.setCookie("https://" + host, "hide-news-ticker", "1");
        br.setCookie("https://" + host, "auto_page", "1");
        br.setCookie("https://" + host, "hide_resized_notice", "1");
        br.setCookie("https://" + host, "blacklisted_tags", "");
        if (account != null) {
            this.login(account, false);
        }
        getPage("https://chan.sankakucomplex.com/post/show/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)<title>\\s*404: Page Not Found\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(">\\s*You lack the access rights required to view this content")) {
            link.setProperty(PROPERTY_IS_PREMIUMONLY, true);
        } else {
            link.removeProperty(PROPERTY_IS_PREMIUMONLY);
        }
        final String storedDirecturl = checkDirectLink(link, PROPERTY_DIRECTURL);
        if (storedDirecturl != null) {
            /* This means we must have checked this one before so filesize/name has already been set -> Done! */
            return AvailableStatus.TRUE;
        }
        String dllink = br.getRegex("(?i)<li>Original: <a href=\"(//[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<a href=\"(//[^<>\"]*?)\">Save this file").getMatch(0);
        }
        if (dllink == null) {
            /* 2021-02-23 */
            dllink = br.getRegex("<meta content=\"(//[^<>\"]+)\" property=og:image>").getMatch(0);
        }
        if (dllink != null) {
            dllink = br.getURL(dllink).toString();
            if (Encoding.isHtmlEntityCoded(dllink)) {
                dllink = Encoding.htmlDecode(dllink);
            }
        }
        String filename = fileID;
        String ext = null;
        if (dllink != null) {
            ext = new Regex(dllink, "[a-z0-9]+(\\.[a-z]+)(\\?|$)").getMatch(0);
        }
        if (ext == null) {
            ext = getFileNameExtensionFromString(dllink, default_Extension);
        }
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        final String sizeStr = br.getRegex("(?i)<li>\\s*Original\\s*:\\s*<a href.*?title=\"([0-9\\,]+) bytes").getMatch(0);
        if (sizeStr != null) {
            /* Size is given --> We don't have to check for it! */
            link.setDownloadSize(Long.parseLong(sizeStr.replace(",", "")));
        }
        if (dllink != null) {
            link.setProperty(PROPERTY_DIRECTURL, dllink);
            if (sizeStr == null && !isDownload) {
                final Browser br2 = br.cloneBrowser();
                // In case the link redirects to the finallink
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    con = br2.openHeadConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fileID = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
        if (!link.isNameSet()) {
            link.setName(fileID);
        }
        br.setFollowRedirects(true);
        final String storedDirecturl = checkDirectLink(link, PROPERTY_DIRECTURL);
        if (storedDirecturl != null) {
            /* This means we must have checked this one before so filesize/name has already been set -> Done! */
            return AvailableStatus.TRUE;
        }
        if (account != null) {
            this.login(account, false);
        }
        getPage("https://capi-v2.sankakucomplex.com/posts?lang=de&page=1&limit=1&tags=id_range:" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
        if (ressourcelist.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> item = (Map<String, Object>) ressourcelist.get(0);
        parseFileInfoAndSetFilenameAPI(link, item);
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfoAndSetFilenameAPI(final DownloadLink link, final Map<String, Object> item) {
        final Map<String, Object> author = (Map<String, Object>) item.get("author");
        link.setProperty(PROPERTY_UPLOADER, author.get("name"));
        /* 2022-12-20: We can't trust filesize of non-active items e.g. fileID: 28977868 -> Status "pending" */
        final boolean isActive = StringUtils.equalsIgnoreCase(item.get("status").toString(), "active");
        final String mimeType = item.get("file_type").toString();
        final String ext = getExtensionFromMimeTypeStatic(mimeType);
        final Number file_size = (Number) item.get("file_size");
        if (file_size != null) {
            if (isActive) {
                link.setVerifiedFileSize(file_size.longValue());
            } else {
                link.setDownloadSize(file_size.longValue());
            }
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
        if (!StringUtils.isEmpty(md5hash) && isActive) {
            link.setMD5Hash(md5hash);
        }
        link.setProperty(PROPERTY_DIRECTURL, item.get("file_url"));
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
        /* Disable chunks as we only download small files */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file or expired directurl", 1 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            boolean valid = false;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                try {
                    if (this.looksLikeDownloadableContent(con) && !con.getURL().toString().contains("expired.png")) {
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
                    link.setProperty(property, Property.NULL);
                }
            }
        }
        return null;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    br.getPage("https://chan." + this.getHost() + "/user/home");
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://chan." + this.getHost() + "/user/login");
                final Form loginform = br.getFormbyActionRegex(".*user/authenticate");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("user%5Bname%5D", Encoding.urlEncode(account.getUser()));
                loginform.put("user%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.getCookie(this.getHost(), "pass_hash", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
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

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
