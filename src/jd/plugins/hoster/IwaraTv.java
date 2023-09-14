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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.IwaraTvConfig;
import org.jdownloader.plugins.components.config.IwaraTvConfig.FilenameScheme;
import org.jdownloader.plugins.components.config.IwaraTvConfig.FilenameSchemeType;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.nutils.JDHash;
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
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.IwaraTvCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { IwaraTvCrawler.class })
public class IwaraTv extends PluginForHost {
    public IwaraTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.iwara.tv/user/register");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        return IwaraTvCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[A-Za-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/(images?|videos?)/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume                   = true;
    private static final int     free_maxchunks                = 0;
    private static final int     free_maxdownloads             = -1;
    private static final String  PROPERTY_DATE                 = "date";
    public static final String   PROPERTY_USER                 = "user";
    public static final String   PROPERTY_TITLE                = "title";
    public static final String   PROPERTY_VIDEOID              = "videoid";
    public static final String   PROPERTY_DIRECTURL            = "directurl";
    public static final String   PROPERTY_IS_PRIVATE           = "is_private";
    public static final String   PROPERTY_EMBED_URL            = "embed_url";
    public static final String   PROPERTY_DESCRIPTION          = "description";
    private final String         PROPERTY_ACCOUNT_ACCESS_TOKEN = "access_token";
    public static final String   WEBAPI_BASE                   = "https://api.iwara.tv";

    @Override
    public String getAGBLink() {
        return "https://www.iwara.tv/";
    }

    private Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setCustomCharset("UTF-8");
        br.setCookie(this.getHost(), "show_adult", "1");
        br.setCookie(br.getHost(), "has_js", "1");
        return br;
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

    public String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        /* Set Packagizer property */
        final String fid = this.getFID(link);
        link.setProperty(PROPERTY_VIDEOID, fid);
        final String type = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        final boolean isVideo = type.startsWith("video");
        if (!link.isNameSet()) {
            /* Set fallback name */
            if (isVideo) {
                link.setName(fid + ".mp4");
            } else {
                link.setName(fid + ".jpg");
            }
        }
        this.setBrowserExclusive();
        prepBR(this.br);
        if (account != null) {
            /* Login if possible */
            login(account, false);
        }
        if (isVideo) {
            br.getPage(WEBAPI_BASE + "/video/" + this.getFID(link));
        } else {
            br.getPage(WEBAPI_BASE + "/image/" + this.getFID(link));
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            /*
             * Offline. In some very rare cases, API will return status 404 for anonymous users while the content is available for users who
             * are logged in.
             */
            // {"message":"errors.notFound"}
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        parseFileInfo(link, entries);
        final String embedUrl = link.getStringProperty(PROPERTY_EMBED_URL);
        if (embedUrl != null) {
            /* This should never happen! */
            link.setProperty(PROPERTY_EMBED_URL, embedUrl);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported embedded content");
        }
        String directurl = null;
        if (isVideo) {
            final String continueURL = (String) entries.get("fileUrl");
            if (!StringUtils.isEmpty(continueURL) && isDownload) {
                final UrlQuery query = UrlQuery.parse(continueURL);
                final String expires = query.get("expires");
                final String partOfPath = new Regex(continueURL, "/([^/]+)\\?.*$").getMatch(0);
                if (expires == null || partOfPath == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* As described here: https://github.com/yt-dlp/yt-dlp/issues/6549#issuecomment-1473771047 */
                final String specialHash = JDHash.getSHA1(partOfPath + "_" + expires + "_5nFp9kmbNnHdAFhaqMvt");
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Origin", "https://www." + this.getHost());
                brc.getHeaders().put("Referer", "https://www." + this.getHost() + "/");
                brc.getHeaders().put("X-Version", specialHash);
                brc.getPage(continueURL);
                if (brc.toString().equals("[]")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Processing video, please check back in a while");
                }
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.OBJECT);
                int maxQuality = -1;
                String maxQualityStr = null;
                String bestQualityDownloadurl = null;
                for (final Map<String, Object> quality : ressourcelist) {
                    final Map<String, Object> src = (Map<String, Object>) quality.get("src");
                    String url = src.get("view").toString();
                    // String url = src.get("download").toString();
                    final String qualityStr = quality.get("name").toString();
                    final Integer qualityTmp = qualityModifierToHeight(qualityStr);
                    if (qualityTmp == null || url == null) {
                        /* Akip invalid items */
                        continue;
                    }
                    /* Fix url as protocol might be missing */
                    url = br.getURL(url).toString();
                    if (qualityTmp.intValue() > maxQuality) {
                        maxQuality = qualityTmp.intValue();
                        maxQualityStr = qualityStr;
                        bestQualityDownloadurl = url;
                    }
                }
                if (bestQualityDownloadurl != null) {
                    logger.info("Found download/stream downloadurl - quality: " + maxQualityStr);
                    directurl = bestQualityDownloadurl;
                    link.setProperty(PROPERTY_DIRECTURL, directurl);
                } else {
                    logger.warning("Failed to find any download/stream");
                }
            }
        }
        link.setFinalFileName(getFilename(link));
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfo(final DownloadLink link, final Map<String, Object> entries) {
        final String message = (String) entries.get("message");
        Map<String, Object> user = (Map<String, Object>) entries.get("user");
        if (user == null) {
            /* For private videos */
            user = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/user");
        }
        final String createdAt = (String) entries.get("createdAt");
        if (createdAt != null) {
            final String date = new Regex(entries.get("createdAt").toString(), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            link.setProperty(PROPERTY_DATE, date);
        }
        /* Collect metadata and set so we can later build filename */
        if (user != null) {
            link.setProperty(PROPERTY_USER, user.get("name"));
        }
        final String title = (String) entries.get("title");
        if (title != null) {
            link.setProperty(PROPERTY_TITLE, title.trim());
        }
        if (Boolean.TRUE.equals(entries.get("private")) || StringUtils.equalsIgnoreCase(message, "errors.privateVideo")) {
            link.setProperty(PROPERTY_IS_PRIVATE, true);
        } else {
            link.removeProperty(PROPERTY_IS_PRIVATE);
        }
        final Map<String, Object> file = (Map<String, Object>) entries.get("file");
        final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
        String directurl = null;
        if (file != null) {
            link.setDownloadSize(((Number) file.get("size")).longValue());
        } else if (files != null) {
            for (final Map<String, Object> filemap : files) {
                /* First = best */
                link.setDownloadSize(((Number) filemap.get("size")).longValue());
                directurl = "https://files.iwara.tv/image/large/" + filemap.get("id") + "/" + Encoding.urlEncode(filemap.get("name").toString());
                break;
            }
        }
        if (!StringUtils.isEmpty(directurl)) {
            link.setProperty(PROPERTY_DIRECTURL, directurl);
        }
        final String embedUrl = (String) entries.get("embedUrl");
        if (embedUrl != null) {
            /* This should never happen! */
            link.setProperty(PROPERTY_EMBED_URL, embedUrl);
        }
        final String descriptionText = (String) entries.get("body");
        if (!StringUtils.isEmpty(descriptionText)) {
            link.setProperty(PROPERTY_DESCRIPTION, descriptionText);
            if (link.getComment() == null) {
                link.setComment(descriptionText);
            }
        }
    }

    public static String getFilename(final DownloadLink link) {
        /* Build filename */
        String filename = null;
        final FilenameScheme scheme = PluginJsonConfig.get(IwaraTvConfig.class).getPreferredFilenameScheme();
        final FilenameSchemeType preferredFilenameSchemeType = PluginJsonConfig.get(IwaraTvConfig.class).getPreferredFilenameSchemeType();
        final String directurl = link.getStringProperty(PROPERTY_DIRECTURL);
        String originalServersideFilename = null;
        try {
            final UrlQuery query = UrlQuery.parse(directurl);
            originalServersideFilename = query.get("file");
            if (originalServersideFilename == null) {
                /* 2023-03-21 */
                originalServersideFilename = query.get("filename");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (preferredFilenameSchemeType == FilenameSchemeType.ORIGINAL_SERVER_FILENAMES && !StringUtils.isEmpty(originalServersideFilename)) {
            filename = Encoding.htmlDecode(originalServersideFilename).trim();
        } else {
            final String date = link.getStringProperty(PROPERTY_DATE);
            final String videoid = link.getStringProperty(PROPERTY_VIDEOID);
            final String uploader = link.getStringProperty(PROPERTY_USER);
            final String title = link.getStringProperty(PROPERTY_TITLE);
            if (scheme == FilenameScheme.DATE_UPLOADER_VIDEOID && date != null && uploader != null) {
                filename = date + "_" + uploader + "_" + videoid;
            } else if (scheme == FilenameScheme.DATE_UPLOADER_SPACE_TITLE && date != null && uploader != null && title != null) {
                filename = date + "_" + uploader + " " + title;
            } else if (scheme == FilenameScheme.DATE_IN_BRACKETS_SPACE_TITLE && date != null && title != null) {
                filename = "[" + date + "]" + " " + title;
            } else if (scheme == FilenameScheme.UPLOADER_VIDEOID_TITLE && uploader != null && title != null) {
                filename = uploader + "_" + videoid + "_" + title;
            } else if (scheme == FilenameScheme.DATE_UPLOADER_VIDEOID_TITLE && date != null && uploader != null && title != null) {
                filename = date + "_" + uploader + "_" + videoid + "_" + title;
            } else if (scheme == FilenameScheme.TITLE && title != null) {
                filename = title;
            } else if (title != null && uploader != null) {
                /* Fallback 1 */
                filename = uploader + "_" + videoid + "_" + title;
            } else if (title != null) {
                /* Fallback 2 */
                filename = videoid + "_" + title;
            } else {
                /* Fallback 3 */
                filename = videoid;
            }
            /* Add extension to filename */
            String ext = directurl != null ? Plugin.getFileNameExtensionFromURL(directurl) : null;
            if (StringUtils.isEmpty(ext)) {
                /* Fallback: Assume we got a video */
                ext = ".mp4";
            }
            filename += ext;
        }
        return filename;
    }

    private Integer qualityModifierToHeight(final String qualityStr) {
        if (qualityStr == null) {
            return null;
        }
        if (qualityStr.equalsIgnoreCase("Source")) {
            /* Best quality/original */
            return Integer.MAX_VALUE;
        } else if (qualityStr.matches("\\d+")) {
            return Integer.parseInt(qualityStr);
        } else if (qualityStr.matches("(?i)\\d+p")) {
            return Integer.parseInt(qualityStr.toLowerCase(Locale.ENGLISH).replace("p", ""));
        } else {
            /* Unknown quality identifier */
            return -1;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        String directurl = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(directurl)) {
            if (link.hasProperty(PROPERTY_IS_PRIVATE)) {
                throw new AccountRequiredException();
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final FilenameSchemeType preferredFilenameSchemeType = PluginJsonConfig.get(IwaraTvConfig.class).getPreferredFilenameSchemeType();
        final String serverFilename = Plugin.getFileNameFromHeader(dl.getConnection());
        final String mimeExt = getExtensionFromMimeType(dl.getConnection().getContentType());
        if (preferredFilenameSchemeType == FilenameSchemeType.ORIGINAL_SERVER_FILENAMES) {
            if (StringUtils.endsWithCaseInsensitive(serverFilename, mimeExt)) {
                link.setFinalFileName(Encoding.htmlDecode(serverFilename));
            } else {
                logger.info("Ignoring bad filename from header: " + serverFilename);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    public Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            prepBR(br);
            final int[] allowedResponseCodesBefore = br.getAllowedResponseCodes();
            br.setAllowedResponseCodes(400);
            try {
                final String storedAccessToken = account.getStringProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN);
                if (storedAccessToken != null) {
                    br.getHeaders().put("Authorization", "Bearer " + storedAccessToken);
                    if (!force) {
                        /* Trust cookies without check */
                        return null;
                    }
                    logger.info("Checking login token");
                    /* Returns responsecode 401 on failure */
                    final Map<String, Object> userinfo = apiGetUserInfo(br);
                    if (br.getHttpConnection().getResponseCode() == 200) {
                        logger.info("Token login successful");
                        return userinfo;
                    } else {
                        logger.info("Token login failed");
                        br.getHeaders().remove("Authorization");
                    }
                }
                logger.info("Performing full login");
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("email", account.getUser());
                postData.put("password", account.getPass());
                br.getHeaders().put("Referer", "https://www." + this.getHost() + "/login");
                br.postPageRaw(WEBAPI_BASE + "/user/login", JSonStorage.serializeToJson(postData));
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                if (br.getHttpConnection().getResponseCode() == 400) {
                    /* E.g. {"message":"errors.invalidLogin"} */
                    throw new AccountInvalidException();
                }
                logger.info("Looks good - obtaining final login token");
                final String token = entries.get("token").toString();
                br.getHeaders().put("Authorization", "Bearer " + token);
                /* Get final token. */
                br.postPage("/user/token", "");
                final Map<String, Object> entries2 = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final String accessToken = entries2.get("accessToken").toString();
                br.getHeaders().put("Authorization", "Bearer " + accessToken);
                account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, accessToken);
                return null;
            } finally {
                br.setAllowedResponseCodes(allowedResponseCodesBefore);
            }
        }
    }

    private Map<String, Object> apiGetUserInfo(final Browser br) throws IOException {
        br.getPage(WEBAPI_BASE + "/user");
        return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        Map<String, Object> usermap = login(account, true);
        if (usermap == null) {
            usermap = apiGetUserInfo(br);
        }
        final Map<String, Object> user = (Map<String, Object>) usermap.get("user");
        ai.setUnlimitedTraffic();
        final String createdAt = user.get("createdAt").toString();
        ai.setCreateTime(TimeFormatter.getMilliSeconds(createdAt, "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH));
        if (Boolean.TRUE.equals(user.get("premium"))) {
            final String premiumUntil = (String) user.get("premiumUntil");
            if (premiumUntil != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(premiumUntil, "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
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
    public Class<? extends IwaraTvConfig> getConfigInterface() {
        return IwaraTvConfig.class;
    }
}
