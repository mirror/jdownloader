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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.IwaraTvConfig;
import org.jdownloader.plugins.components.config.IwaraTvConfig.FilenameScheme;
import org.jdownloader.plugins.components.config.IwaraTvConfig.FilenameSchemeType;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "iwara.tv" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?iwara\\.tv/videos/([A-Za-z0-9]+)" })
public class IwaraTv extends PluginForHost {
    public IwaraTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.iwara.tv/user/register");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume        = true;
    private static final int     free_maxchunks     = 0;
    private static final int     free_maxdownloads  = -1;
    private final String         html_privatevideo  = "(?i)>\\s*This video is only available for users that|>\\s*Private video<";
    private static final String  type_image         = "https?://[^/]+/images/.+";
    private static final String  PROPERTY_DATE      = "date";
    public static final String   PROPERTY_USER      = "user";
    public static final String   PROPERTY_TITLE     = "title";
    public static final String   PROPERTY_VIDEOID   = "videoid";
    public static final String   PROPERTY_DIRECTURL = "directurl";

    @Override
    public String getAGBLink() {
        return "https://www.iwara.tv/";
    }

    private Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setCustomCharset("UTF-8");
        br.setCookie(this.getHost(), "show_adult", "1");
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
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
        link.setProperty(PROPERTY_VIDEOID, this.getFID(link));
        this.setBrowserExclusive();
        prepBR(this.br);
        if (account != null) {
            /* Login if possible */
            login(account, false);
        }
        this.br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("id=\"video-player\"") && !this.br.containsHTML(html_privatevideo)) {
            /* Invalid URL and webpage does not display any kind of error e.g. "https://www.iwara.tv/videos/0" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final IwaraTvConfig cfg = PluginJsonConfig.get(IwaraTvConfig.class);
        String date = br.getRegex("(?i)class=\"username\"[^>]*>[^<]+</a>\\s*作成日:(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        String directurl = null;
        if (this.br.getURL().matches(type_image)) {
            /* Picture */
            directurl = this.br.getRegex("\"(https?://(?:[a-z0-9]+\\.)?iwara\\.tv/[^<>]+/large/public/[^<>\"]+)\"").getMatch(0);
            if (directurl == null) {
                directurl = this.br.getRegex("(//[^/]+/sites/default/files/styles/large/public/photos/[^\"]+)").getMatch(0);
                if (directurl != null) {
                    directurl = "https:" + directurl;
                }
            }
        } else {
            if (this.br.containsHTML("name=\"flashvars\"") || this.br.containsHTML("flowplayer\\.org/")) {
                /* Video */
                directurl = br.getRegex("<source src=\"(https?://[^<>\"]+)\" type=\"video/").getMatch(0);
                if (directurl == null) {
                    directurl = br.getRegex("\"(https?://(?:www\\.)?iwara\\.tv/sites/default/files/videos/[^<>\"]+)\"").getMatch(0);
                }
            }
            if (directurl == null) {
                /* Video new/current way */
                if (isDownload || cfg.isFindFilesizeDuringAvailablecheck()) {
                    final String drupal = br.getRegex("jQuery\\.extend\\([^{]+(.+)\\);").getMatch(0);
                    final String videoHash = PluginJSonUtils.getJson(PluginJSonUtils.getJsonNested(drupal, "theme"), "video_hash");
                    if (!StringUtils.isEmpty(videoHash)) {
                        final Browser brc = br.cloneBrowser();
                        brc.getPage("/api/video/" + videoHash);
                        if (brc.toString().equals("[]")) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Processing video, please check back in a while");
                        }
                        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) JSonStorage.restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.OBJECT);
                        int maxQuality = -1;
                        String maxQualityStr = null;
                        String bestQualityDownloadurl = null;
                        for (final Map<String, Object> quality : ressourcelist) {
                            String url = quality.get("uri").toString();
                            final String qualityStr = quality.get("resolution").toString();
                            final Integer qualityTmp = qualityModifierToHeight(qualityStr);
                            if (qualityTmp == null || url == null) {
                                /* Akip invalid items */
                                continue;
                            }
                            /* Fix url (protocol might be missing) */
                            url = br.getURL(url).toString();
                            if (qualityTmp.intValue() > maxQuality) {
                                maxQuality = qualityTmp.intValue();
                                maxQualityStr = qualityStr;
                                bestQualityDownloadurl = url;
                            }
                        }
                        if (bestQualityDownloadurl != null) {
                            logger.info("Found official downloadurl - quality: " + maxQualityStr);
                            directurl = bestQualityDownloadurl;
                            /* Extract upload-date from this URL */
                            final UrlQuery query = UrlQuery.parse(bestQualityDownloadurl);
                            String stringWithDate = query.get("file");
                            if (stringWithDate != null) {
                                stringWithDate = Encoding.htmlDecode(stringWithDate);
                                final Regex specialDateSource = new Regex(stringWithDate, "(\\d{4}/\\d{2}/\\d{2})");
                                if (specialDateSource.matches()) {
                                    date = specialDateSource.getMatch(0).replace("/", "-");
                                }
                            }
                        } else {
                            logger.warning("Failed to find any official downloads although it looks like they should be available!");
                        }
                    }
                }
            }
        }
        /* Collect metadata and set so we can later build filename */
        /* Important: Make sure that this RegEx is working for all possible languages! */
        final Regex usernameAndDate = br.getRegex("(?i)class=\"username\"[^>]*>([^<]+)</a>[^<]*(\\d{4}-\\d{2}-\\d{2})");
        /* Set some Packagizer properties */
        String uploader = usernameAndDate.getMatch(0);
        // if (uploader == null) {
        // /* Fallback */
        // /* 2022-03-29: Do not use this as it will return the usernameSlub but not the "real username"! */
        // uploader = br.getRegex("<div class=\"user-picture\"[^>]*>\\s*<a[^>]*href=\"/users/([<>\"]+)\"").getMatch(0);
        // }
        if (uploader != null) {
            uploader = Encoding.htmlDecode(uploader).trim();
            link.setProperty(PROPERTY_USER, uploader);
        }
        if (date == null) {
            date = usernameAndDate.getMatch(1);
        }
        if (date != null) {
            link.setProperty(PROPERTY_DATE, date);
        }
        String title = br.getRegex("<h1[^>]*class=\"title\">([^<>\"]+)</h1>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlOnlyDecode(title).trim();
            link.setProperty(PROPERTY_TITLE, title);
        }
        if (directurl != null) {
            link.setProperty(PROPERTY_DIRECTURL, directurl);
        }
        link.setFinalFileName(getFilename(link));
        if (cfg.isFindFilesizeDuringAvailablecheck() && !isDownload && directurl != null) {
            // In case the link redirects to the finallink
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(directurl);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
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

    public static String getFilename(final DownloadLink link) {
        /* Build filename */
        String filename = null;
        final FilenameScheme scheme = PluginJsonConfig.get(IwaraTvConfig.class).getPreferredFilenameScheme();
        final FilenameSchemeType preferredFilenameSchemeType = PluginJsonConfig.get(IwaraTvConfig.class).getPreferredFilenameSchemeType();
        final String directurl = link.getStringProperty(PROPERTY_DIRECTURL);
        String originalServersideFilename = null;
        try {
            originalServersideFilename = directurl != null ? UrlQuery.parse(directurl).get("file") : null;
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
            return 10000;
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
        if (this.br.containsHTML(html_privatevideo)) {
            throw new AccountRequiredException();
        } else if (StringUtils.isEmpty(directurl)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
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
        if (preferredFilenameSchemeType == FilenameSchemeType.ORIGINAL_SERVER_FILENAMES && !StringUtils.isEmpty(serverFilename)) {
            link.setFinalFileName(Encoding.htmlDecode(serverFilename));
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Trust cookies without check */
                        return;
                    }
                    logger.info("Checking login cookies");
                    br.getPage("https://" + this.getHost());
                    if (isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/user/login?destination=front");
                Form loginform = br.getFormbyProperty("id", "user-login");
                if (loginform == null) {
                    /* Fallback */
                    loginform = br.getForm(0);
                }
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("name", Encoding.urlEncode(account.getUser()));
                loginform.put("pass", Encoding.urlEncode(account.getPass()));
                if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                /* 2019-12-12: Anti-anti-bot against: https://www.iwara.tv/sites/all/modules/contrib/antibot/js/antibot.js */
                final String key = PluginJSonUtils.getJson(br, "key");
                if (key != null) {
                    loginform.put("antibot_key", key);
                }
                if (loginform.getAction() == null || loginform.getAction().contains("/antibot")) {
                    loginform.setAction(br.getURL());
                }
                br.setCookie(br.getHost(), "has_js", "1");
                /* Anti-anti-bot END */
                br.submitForm(loginform);
                if (!isLoggedIN(br)) {
                    if (br.getURL().contains("/antibot")) {
                        /* 2019-12-12: Anti-anti-bot failed :( */
                        logger.warning("Login failed due to anti-bot measures");
                    }
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/user/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
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
