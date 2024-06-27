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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.EvilangelComConfig.Quality;
import org.jdownloader.plugins.components.config.EvilangelCoreConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class EvilangelCore extends PluginForHost {
    public EvilangelCore(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        final List<LazyPlugin.FEATURE> ret = new ArrayList<LazyPlugin.FEATURE>();
        ret.add(LazyPlugin.FEATURE.XXX);
        if (allowCookieLoginOnly()) {
            ret.add(LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY);
        } else {
            ret.add(LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL);
        }
        return ret.toArray(new LazyPlugin.FEATURE[0]);
    }

    protected Browser prepBrowser(final Browser br) {
        /* Define custom browser headers and language settings */
        br.setCookie(this.getHost(), "enterSite", "en");
        br.setFollowRedirects(true);
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @Override
    public String getAGBLink() {
        /* All websites using this script will usually link to this support portal. */
        return "http://www.famesupport.com/";
    }

    private String       dllink                          = null;
    @Deprecated
    private final String URL_EVILANGEL_FILM              = "(?i)https?://members\\.evilangel.com/[a-z]{2}/([A-Za-z0-9\\-_]+)/film/(\\d+)";
    @Deprecated
    private final String URL_EVILANGEL_FREE_TRAILER      = "(?i)https?://(?:www\\.)?evilangel\\.com/[a-z]{2}/video/([A-Za-z0-9\\-]+)/(\\d+)";
    private final String URL_VIDEO                       = "(?i)https?://[^/]+/[a-z]{2}/(?:video|movie)/([A-Za-z0-9\\-_]+)(?:/([A-Za-z0-9\\-_]+))?/(\\d+)";
    private final String PROPERTY_ACTORS                 = "actors";
    private final String PROPERTY_DATE                   = "date";
    private final String PROPERTY_QUALITY                = "quality";
    private final String PROPERTY_TITLE                  = "title";
    private final String PROPERTY_ACCOUNT_CONTENT_SOURCE = "content_source";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            /* Default regex for most of all supported websites. */
            ret.add("https?://(?:(?:www|members)\\.)?" + buildHostsPatternPart(domains) + "/[a-z]{2}/(?:movie|video)/[A-Za-z0-9\\-_]+(?:/[A-Za-z0-9\\-_]+)?/\\d+");
        }
        return ret.toArray(new String[0]);
    }

    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    protected boolean allowCookieLoginOnly() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return this.requestFileInformation(link, account, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            /* Set fallback filename */
            final String urlTitle = getURLTitle(link);
            if (urlTitle != null) {
                link.setName(urlTitle + ".mp4");
            }
        }
        this.dllink = null;
        this.setBrowserExclusive();
        final String fileID = this.getFID(link);
        String filename = null;
        final String host = Browser.getHost(link.getPluginPatternMatcher(), true);
        if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FREE_TRAILER) && !host.contains("members.")) {
            /* Free (trailer) download */
            br.getPage(link.getPluginPatternMatcher());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String jsonPlayer = br.getRegex("ScenePlayerOptions = (\\{.*?\\});window\\.").getMatch(0);
            String server = null;
            if (jsonPlayer != null) {
                try {
                    Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(jsonPlayer);
                    entries = (Map<String, Object>) entries.get("playerOptions");
                    server = (String) entries.get("host");
                    final String sceneTitle = (String) entries.get("sceneTitle");
                    if (!StringUtils.isEmpty(sceneTitle)) {
                        filename = fileID + "_" + sceneTitle;
                    }
                } catch (final Throwable e) {
                }
            }
            if (StringUtils.isEmpty(filename)) {
                filename = getURLTitle(link);
            }
            if (StringUtils.isEmpty(server)) {
                server = "https://trailers-evilangel.gammacdn.com";
            }
            this.dllink = getDllinkTrailer(this.br);
            if (!StringUtils.isEmpty(this.dllink)) {
                this.dllink = server + this.dllink;
                final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                if (quality == null) {
                    filename += ".mp4";
                } else {
                    filename = filename + "-" + quality + ".mp4";
                }
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = brc.openHeadConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                        if (StringUtils.isEmpty(filename)) {
                            /* Fallback if everything else fails */
                            filename = Encoding.htmlDecode(getFileNameFromHeader(con));
                        }
                        link.setFinalFileName(filename);
                    } else {
                        brc.followConnection(true);
                        handleErrorsAfterDirecturlAccess(link, account, brc, true);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        } else {
            if (account == null) {
                throw new AccountRequiredException();
            }
            login(account, false);
            long filesize = -1;
            if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FILM)) {
                br.getPage(link.getPluginPatternMatcher());
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* 2021-09-01: Use title from URL as this should be just fine. */
                filename = getURLTitle(link);
                filename = Encoding.htmlDecode(filename.trim());
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = "http://members.evilangel.com" + dllink;
                final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                if (quality != null) {
                    filename = filename + "-" + quality;
                }
            } else {
                br.getPage(link.getPluginPatternMatcher());
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* 2021-09-01: Use title from URL as this should be just fine. */
                filename = getURLTitle(link);
                filename = Encoding.htmlDecode(filename).replace("-", " ").trim();
                /* Find downloadlink */
                final String preferredQualityStr = this.getUserPreferredqualityStr();
                if (preferredQualityStr == null) {
                    logger.info("User has selected BEST quality");
                } else {
                    logger.info("User has selected quality: " + preferredQualityStr);
                }
                /*
                 * Users have to buy an extra package to get download buttons (official downloads). For now we'll just always download the
                 * streams as this should work fine for all premium accounts.
                 */
                final String htmlVideoJson = br.getRegex("window\\.defaultStateScene\\s*=\\s*(\\{.+\\});").getMatch(0);
                final String htmlVideoJson2 = br.getRegex("new Cms_Player\\((\\{.*?\\})\\);").getMatch(0);
                List<Map<String, Object>> qualitiesList = null;
                if (htmlVideoJson == null && htmlVideoJson2 == null) {
                    /**
                     * 2023-04-19: New (tested with: evilangel.com) </br>
                     * TODO: Test this with other supported websites such as wicked.com.
                     */
                    final Browser brc = br.cloneBrowser();
                    brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    brc.getPage("/media/streamingUrls/" + this.getFID(link));
                    /**
                     * No error-page but also no player --> Assume content is offline e.g. </br>
                     * https://members.wicked.com/en/movie/bla-bla/123456
                     */
                    qualitiesList = (List<Map<String, Object>>) restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.OBJECT);
                }
                Map<String, String> qualityMap = null;
                if (htmlVideoJson != null) {
                    final Map<String, Object> entries = restoreFromString(htmlVideoJson, TypeRef.MAP);
                    final Map<String, Object> videoInfo = (Map<String, Object>) entries.get(this.getFID(getDownloadLink()));
                    final Object qualityMapO = videoInfo.get("videos");
                    if (qualityMapO == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (qualityMapO instanceof List) {
                        qualitiesList = (List<Map<String, Object>>) qualityMapO;
                        if (qualitiesList.isEmpty()) {
                            /*
                             * Empty list --> User is not allowed to watch this full video -> Trailer only but we do not (yet) have a
                             * handling to find the trailer streams -> Throw Exception instead
                             */
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Only trailer available");
                        }
                    } else {
                        qualityMap = (Map<String, String>) qualityMapO;
                        if (qualityMap.isEmpty()) {
                            /* This should never happen */
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
                if (qualitiesList != null) {
                    /* Make map out of list */
                    qualityMap = new HashMap<String, String>();
                    for (final Map<String, Object> map : qualitiesList) {
                        final String format = map.get("format").toString();
                        final String url = map.get("url").toString();
                        qualityMap.put(format, url);
                    }
                }
                if (qualityMap != null) {
                    String chosenQualityLabel = null;
                    String fallbackDirecturl = null;
                    String fallbackFormat = null;
                    int bestHeight = -1;
                    final Iterator<Entry<String, String>> iterator = qualityMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        final Entry<String, String> entry = iterator.next();
                        final String format = entry.getKey();
                        final String url = entry.getValue();
                        final Regex heightRegex = new Regex(format, "(?i)(\\d+)p");
                        if (!format.matches("(?i)\\d+p")) {
                            continue;
                        }
                        final int height = Integer.parseInt(heightRegex.getMatch(0));
                        final String heightNormalizedStr = this.getNormalizedHeight(height) + "p";
                        if (heightNormalizedStr.equalsIgnoreCase(preferredQualityStr)) {
                            chosenQualityLabel = format;
                            this.dllink = url;
                        }
                        if (fallbackDirecturl == null || height > bestHeight) {
                            fallbackDirecturl = url;
                            fallbackFormat = format;
                            bestHeight = height;
                        }
                    }
                    if (StringUtils.isEmpty(this.dllink)) {
                        logger.info("Failed to find user selected format -> Fallback to: " + fallbackFormat);
                        chosenQualityLabel = fallbackFormat;
                        this.dllink = fallbackDirecturl;
                    }
                    logger.info("Chosen quality: " + chosenQualityLabel);
                    link.setProperty(PROPERTY_QUALITY, chosenQualityLabel);
                }
                if (htmlVideoJson2 != null) {
                    /* 2022-01-14: For some wicked.com URLs e.g. https://members.wicked.com/en/movie/bla-bla/123456 */
                    /*
                     * 2nd json with more information but no downloadlinks is also available in html as json see: dataLayer = [{"dvdDetails"
                     */
                    final Map<String, Object> root = restoreFromString(htmlVideoJson2, TypeRef.MAP);
                    final Map<String, Object> sceneInfos = (Map<String, Object>) root.get("sceneInfos");
                    if (sceneInfos != null) {
                        final Object sceneIdO = sceneInfos.get("sceneId");
                        if (sceneIdO != null && br.getURL().contains(sceneIdO.toString())) {
                            final String sceneReleaseDate = (String) sceneInfos.get("sceneReleaseDate");
                            final String sceneTitle = (String) sceneInfos.get("sceneTitle");
                            if (sceneReleaseDate != null) {
                                link.setProperty(PROPERTY_DATE, sceneReleaseDate);
                            }
                            if (!StringUtils.isEmpty(sceneTitle)) {
                                filename = sceneTitle;
                            }
                        }
                    }
                    /**
                     * A scene can also contain DVD-information. </br>
                     * --> Ensure to set the correct information which is later used for filenames.
                     */
                    final Map<String, Object> movieInfos = (Map<String, Object>) root.get("movieInfos");
                    if (movieInfos != null) {
                        final String dvdId = movieInfos.get("dvdId").toString();
                        if (br.getURL().contains(dvdId)) {
                            final String dvdReleaseDate = (String) movieInfos.get("dvdReleaseDate");
                            final String dvdName = (String) movieInfos.get("dvdName");
                            if (dvdReleaseDate != null) {
                                link.setProperty(PROPERTY_DATE, dvdReleaseDate);
                            }
                            if (!StringUtils.isEmpty(dvdName)) {
                                filename = dvdName;
                            }
                        }
                    }
                    final List<Map<String, Object>> streamingSrcs = (List<Map<String, Object>>) root.get("streamingSources");
                    if (streamingSrcs == null || streamingSrcs.isEmpty()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    String bestQualityDownloadurl = null;
                    String userSelectedQualityDownloadurl = null;
                    String bestQualityLabel = null;
                    for (final Map<String, Object> streamingSrc : streamingSrcs) {
                        final String url = streamingSrc.get("src").toString();
                        String label = streamingSrc.get("label").toString();
                        /* Correct label e.g. "576p" -> "540p" -> Needed for quality selection */
                        final Regex labelWithWidth = new Regex(label, "(\\d+)p");
                        if (labelWithWidth.matches()) {
                            final int widthTmp = Integer.parseInt(labelWithWidth.getMatch(0));
                            final String labelStringCorrected = getDisplayWidth(widthTmp) + "p";
                            if (!labelStringCorrected.equals(label)) {
                                logger.info("Corrected label: " + label + " --> " + labelStringCorrected);
                                label = labelStringCorrected;
                            }
                        }
                        /* First == BEST */
                        if (bestQualityDownloadurl == null) {
                            bestQualityDownloadurl = url;
                            bestQualityLabel = label;
                        }
                        if (label.equalsIgnoreCase(preferredQualityStr)) {
                            userSelectedQualityDownloadurl = url;
                            break;
                        }
                    }
                    final String chosenQualityLabel;
                    if (userSelectedQualityDownloadurl != null) {
                        logger.info("Chose user selected quality: " + preferredQualityStr);
                        this.dllink = userSelectedQualityDownloadurl;
                        chosenQualityLabel = preferredQualityStr;
                    } else {
                        if (preferredQualityStr == null) {
                            logger.info("Chose best quality: " + bestQualityLabel);
                        } else {
                            logger.info("Chose best quality as FALLBACK: " + bestQualityLabel);
                        }
                        this.dllink = bestQualityDownloadurl;
                        chosenQualityLabel = bestQualityLabel;
                    }
                    /* If video is also officially downloadable, we might be able to find an estimated filesize in html code. */
                    final String filesizeStr = br.getRegex(chosenQualityLabel + "</span>\\s*<span class=\"movieSize\"[^>]*>([^<]+)</span>").getMatch(0);
                    if (filesizeStr != null) {
                        filesize = SizeFormatter.getSize(filesizeStr);
                    }
                    link.setProperty(PROPERTY_QUALITY, chosenQualityLabel);
                }
                if (dllink != null) {
                    final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                    if (quality != null) {
                        filename = filename + "-" + quality;
                    }
                }
                String contentSource = new Regex(link.getPluginPatternMatcher(), URL_VIDEO).getMatch(0);
                if (contentSource == null) {
                    contentSource = account.getStringProperty(PROPERTY_ACCOUNT_CONTENT_SOURCE);
                }
                // siteName = "wicked";
                if (!link.hasProperty(PROPERTY_DATE) && contentSource != null) {
                    logger.info("Looking for additional metadata...");
                    try {
                        final String jsonAPI = br.getRegex("window\\.env\\s*=\\s*(\\{.*?\\});").getMatch(0);
                        Map<String, Object> entries = restoreFromString(jsonAPI, TypeRef.MAP);
                        final Map<String, Object> algoliaAPI = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "api/algolia");
                        final String appID = algoliaAPI.get("applicationID").toString();
                        final Browser brc = br.cloneBrowser();
                        final UrlQuery query = new UrlQuery();
                        query.add("x-algolia-agent", Encoding.urlEncode("Algolia for vanilla JavaScript 3.27.1;JS Helper 2.26.0"));
                        query.add("x-algolia-application-id", Encoding.urlEncode(appID));
                        query.add("x-algolia-api-key", Encoding.urlEncode(algoliaAPI.get("apiKey").toString()));
                        brc.getHeaders().put("Accept", "application/json");
                        brc.getHeaders().put("x-algolia-application-id", appID);
                        brc.getHeaders().put("x-algolia-api-key", algoliaAPI.get("apiKey").toString());
                        final String url = "https://" + appID.toLowerCase(Locale.ENGLISH) + "-dsn.algolia.net/1/indexes/*/queries?" + query.toString();
                        final String postData = "{\"requests\":[{\"indexName\":\"all_scenes\",\"params\":\"query=&page=0&facets=%5B%5D&tagFilters=&facetFilters=%5B%22sitename%3A" + contentSource + "%22%2C%5B%22clip_id%3A" + this.getFID(link) + "%22%5D%5D\"},{\"indexName\":\"all_scenes\",\"params\":\"query=&page=0&hitsPerPage=1&attributesToRetrieve=%5B%5D&attributesToHighlight=%5B%5D&attributesToSnippet=%5B%5D&tagFilters=&analytics=false&clickAnalytics=false&facets=clip_id&facetFilters=%5B%22sitename%3A" + contentSource + "%22%5D\"}]}";
                        brc.postPageRaw(url, postData);
                        entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
                        final Map<String, Object> clipInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "results/{0}/hits/{0}");
                        /* Prefer title from URL so it's always in the same parttern. */
                        final String title = (String) clipInfo.get("title");
                        if (!StringUtils.isEmpty(title)) {
                            link.setProperty(PROPERTY_TITLE, title);
                        }
                        final String releaseDate = (String) clipInfo.get("release_date");
                        if (!StringUtils.isEmpty(releaseDate)) {
                            link.setProperty(PROPERTY_DATE, releaseDate);
                        }
                        /* Now try to find the filesize of our previously chosen download quality. */
                        final Map<String, Object> downloadFileSizes = (Map<String, Object>) clipInfo.get("download_file_sizes");
                        final String chosenQuality = getQualityFilesizeMapping(link.getStringProperty(PROPERTY_QUALITY));
                        if (downloadFileSizes != null && link.hasProperty(PROPERTY_QUALITY)) {
                            if (downloadFileSizes.containsKey(chosenQuality)) {
                                filesize = ((Number) downloadFileSizes.get(chosenQuality)).longValue();
                            } else {
                                logger.warning("Failed filesize for chosen quality: " + chosenQuality);
                            }
                        }
                        final List<Map<String, Object>> actors = (List<Map<String, Object>>) clipInfo.get("actors");
                        if (actors.size() > 0) {
                            String actorsCommaSeparated = "";
                            int index = 0;
                            for (final Map<String, Object> actor : actors) {
                                final boolean isLastItem = index == actors.size() - 1;
                                actorsCommaSeparated += actor.get("name").toString();
                                if (!isLastItem) {
                                    actorsCommaSeparated += ",";
                                }
                                index += 1;
                            }
                            link.setProperty(PROPERTY_ACTORS, actorsCommaSeparated);
                        }
                    } catch (final Throwable ignore) {
                        logger.warning("Error while trying to find additional metadata");
                        logger.log(ignore);
                    }
                }
            }
            if (filename != null) {
                filename = applyFilenameExtension(filename, ".mp4");
                link.setFinalFileName(filename);
            }
            /* Try to find filesize if possible and needed. */
            if (filesize > 0) {
                link.setDownloadSize(filesize);
            } else if (!isDownload && dllink != null && !dllink.contains(".m3u8")) {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = brc.openHeadConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            if (con.isContentDecoded()) {
                                link.setDownloadSize(con.getCompleteContentLength());
                            } else {
                                link.setVerifiedFileSize(con.getCompleteContentLength());
                            }
                        }
                        final String serverFilename = getFileNameFromHeader(con);
                        if (serverFilename != null) {
                            if (link.getFinalFileName() == null) {
                                link.setFinalFileName(serverFilename);
                            }
                        } else if (filename != null) {
                            link.setFinalFileName(this.correctOrApplyFileNameExtension(filename, con));
                        }
                    } else {
                        brc.followConnection(true);
                        handleErrorsAfterDirecturlAccess(link, account, brc, true);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    /** E.g. 288(p) -> 240(p) */
    private int getNormalizedHeight(final int height) {
        if (height <= 200) {
            return 160;
        } else if (height > 200 && height <= 300) {
            return 240;
        } else if (height > 300 && height <= 400) {
            return 360;
        } else if (height > 400 && height <= 510) {
            return 480;
        } else if (height > 510 && height <= 600) {
            return 540;
        } else if (height > 600 && height <= 900) {
            return 720;
        } else if (height > 900 && height <= 1500) {
            return 1080;
        } else if (height > 1500 && height <= 2500) {
            return 2160;
        } else {
            return height;
        }
    }

    private void handleErrorsAfterDirecturlAccess(final DownloadLink link, final Account account, final Browser br, final boolean lastChance) throws PluginException {
        if (br.containsHTML("limit reached:")) {
            /* 2022-01-17 adulttime.com: E.g. full response: limit reached: 1.1.1.1:12345678 */
            if (account != null) {
                throw new AccountUnavailableException("Limit reached", 1 * 60 * 1000);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached");
            }
        }
        if (lastChance) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - broken video?");
        }
    }

    private String getUserPreferredqualityStr() {
        final Quality quality = PluginJsonConfig.get(getConfigInterface()).getPreferredQuality();
        switch (quality) {
        case Q160:
            return "160p";
        case Q240:
            return "240p";
        case Q360:
            return "360p";
        case Q480:
            return "480p";
        case Q540:
            return "540p";
        case Q720:
            return "720p";
        case Q1080:
            return "1080p";
        case Q2160:
            return "2160p";
        default:
            /* E.g. BEST */
            return null;
        }
    }

    private int getDisplayWidth(final int preciseWidth) {
        if (preciseWidth > 200 && preciseWidth <= 300) {
            return 240;
        } else if (preciseWidth > 300 && preciseWidth <= 500) {
            return 480;
        } else if (preciseWidth > 500 && preciseWidth <= 680) {
            return 540;
        } else if (preciseWidth > 680 && preciseWidth <= 1000) {
            return 720;
        } else if (preciseWidth > 1000 && preciseWidth <= 2000) {
            return 1080;
        } else if (preciseWidth > 2000 && preciseWidth <= 3500) {
            return 2160;
        } else {
            /* Fallback */
            return preciseWidth;
        }
    }

    public boolean isFreeDownloadable(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FREE_TRAILER)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null && !isFreeDownloadable(link)) {
            return false;
        } else {
            return super.canHandle(link, account);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        if (isFreeDownloadable(link)) {
            handleDownload(link, null);
        } else {
            throw new AccountRequiredException();
        }
    }

    protected String getURLTitle(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FILM)) {
            return new Regex(link.getPluginPatternMatcher(), URL_EVILANGEL_FILM).getMatch(0);
        } else if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FREE_TRAILER)) {
            return new Regex(link.getPluginPatternMatcher(), URL_EVILANGEL_FREE_TRAILER).getMatch(0);
        } else if (link.getPluginPatternMatcher().matches(URL_VIDEO)) {
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), URL_VIDEO);
            /* Sometimes author/studio + title is given and sometimes title(=param1) only. */
            final String param1 = urlinfo.getMatch(0);
            final String param2 = urlinfo.getMatch(1);
            if (param1 != null && param2 != null) {
                return param1 + "_" + param2;
            } else {
                return param1;
            }
        } else {
            logger.warning("!Developer mistake! Unsupported URL!");
            return null;
        }
    }

    private String getQualityFilesizeMapping(final String str) {
        if (str == null) {
            return null;
        } else if (str.equals("2160p")) {
            return "4k";
        } else {
            /* Given value should be already correct. */
            return str;
        }
    }

    // private String findDesiredQuality(final Browser br, final String qualityStr) {
    // return br.getRegex("\"(/[^\"]*/download/\\d+/" + qualityStr + "[^\"]*)\"").getMatch(0);
    // }
    /** Find highest quality trailer downloadurl (sometimes higher quality than what website player is using) */
    public static String getDllinkTrailer(final Browser br) {
        String dllink = null;
        final String[] qualities = { "2160p", "1080p", "720p", "540p", "480p", "240p", "160p" };
        for (final String quality : qualities) {
            // dllink = br.getRegex("file=\"(/[^<>\"]*?/trailers/[^<>\"]+" + quality + "\\.mp4)\"").getMatch(0);
            dllink = br.getRegex("file=\"(/[^<>\"]*?/trailers/[^<>\"]+\\.mp4)\" size=\"" + quality).getMatch(0);
            if (dllink != null) {
                break;
            }
        }
        return dllink;
    }

    public void login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            final String host = this.getHost();
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            if (allowCookieLoginOnly() && userCookies == null) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            this.prepBrowser(br);
            if (userCookies != null) {
                logger.info("Attempting user cookie login");
                br.setCookies(userCookies);
                if (!verifyCookies) {
                    /* Do not verify cookies */
                    return;
                }
                if (verifyCookies(account, userCookies)) {
                    /* Cookie login successful */
                    logger.info("User cookie login successful");
                    return;
                } else {
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            }
            if (cookies != null) {
                br.setCookies(host, cookies);
                if (!verifyCookies) {
                    /* Do not verify cookies */
                    return;
                }
                if (verifyCookies(account, cookies)) {
                    /* Cookie login successful */
                    logger.info("Cookie login successful");
                    /* Update cookies */
                    account.saveCookies(br.getCookies(host), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.setFollowRedirects(true);
            br.getPage(getNamespaceLogin());
            if (br.containsHTML("(?i)>\\s*We are experiencing some problems\\!<")) {
                throw new AccountInvalidException("Your IP is banned. Please re-connect to get a new IP to be able to log-in!");
            }
            // final String realLoginURL = br.getURL();
            final Form login = br.getFormbyProperty("id", "loginForm");
            if (login == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie(br.getHost(), "origin", "promo");
            br.setCookie(br.getHost(), "enterSite", "true");
            br.setCookie(br.getHost(), "activeMemberValidator", "1");
            final boolean fillTimeFalues = true;
            if (fillTimeFalues) {
                final Date d = new Date();
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
                final String date = sd.format(d);
                sd = new SimpleDateFormat("k:mm");
                final String time = sd.format(d);
                final String timedatestring = date + " " + time;
                br.setCookie(br.getHost(), "mDateTime", Encoding.urlEncode(timedatestring));
                br.setCookie(br.getHost(), "mOffset", "2");
                br.setCookie(br.getHost(), "timestamp", Long.toString(System.currentTimeMillis()));
                /* 2021-09-01: Some tests for adulttime.com */
                br.setCookie(br.getHost(), "loginLander", "/en");
                br.setCookie(br.getHost(), "_hjIncludedInSessionSample", "0");
            } else {
                // do nothing
            }
            // login.setAction("/en/login");
            login.put("username", Encoding.urlEncode(account.getUser()));
            login.put("password", Encoding.urlEncode(account.getPass()));
            if (login.containsHTML("g-recaptcha")) {
                // recaptchav2
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                    @Override
                    public String getSiteKey() {
                        return getSiteKey(login.getHtmlCode());
                    }
                }.getToken();
                login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            login.remove("submit");
            /**
             * 2021-09-01: Form may contain "rememberme" two times with value "0" AND "1"! Same via browser! </br>
             * Only add "rememberme": "1" if that is not already present in our form.
             */
            final String remembermeCookieKey = "rememberme";
            boolean containsRemembermeFieldWithValue1 = false;
            for (final InputField ifield : login.getInputFields()) {
                if (StringUtils.equals(ifield.getKey(), remembermeCookieKey) && StringUtils.equals(ifield.getValue(), "1")) {
                    containsRemembermeFieldWithValue1 = true;
                    break;
                }
            }
            if (!containsRemembermeFieldWithValue1) {
                login.put(remembermeCookieKey, "1");
            }
            br.submitForm(login);
            if (br.getURL().contains("/compliance/terms")) {
                /* 2023-06-28: dfxtra.com */
                logger.warning("!DEVELOPER! This step hasn't been implemented yet! Login will most likely fail now!");
                final String url = br.getRegex("(/api/compliance/terms/acknowledgement)").getMatch(0);
                final String uuid = br.getRegex("'uuid'\\s*:\\s*'([^<>\"\\']+)").getMatch(0);
                // br.getPage(realLoginURL);
            }
            /* TODO: 2021-09-01: Add support for 2FA login (security code gets sent via mail) */
            if (br.containsHTML("(?i)>\\s*Your account is deactivated for abuse")) {
                throw new AccountInvalidException("Your account is deactivated for abuse. Please re-activate it to use it in JDownloader.");
            } else if (br.getURL().contains("/reactivate")) {
                /* TODO: Expired free account(?) */
                throw new AccountInvalidException("Premium subscription expired");
            }
            if (!isLoggedIn(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(host), "");
        }
    }

    /** Sets given cookies and checks if we can login with them. */
    protected boolean verifyCookies(final Account account, final Cookies cookies) throws Exception {
        br.setCookies(account.getHoster(), cookies);
        br.getPage(getNamespaceMembersMain());
        if (isLoggedIn(this.br)) {
            logger.info("Successfully logged in via cookies");
            return true;
        } else {
            logger.info("Cookie login failed");
            br.clearCookies(br.getHost());
            return false;
        }
    }

    protected boolean hasUsedCookieLogin(final Account account) {
        if (this.allowCookieLoginOnly()) {
            return true;
        } else if (account.loadUserCookies() != null) {
            return true;
        } else {
            return false;
        }
    }

    /** TODO: Add functionality. */
    protected boolean accountAllowsOfficialDownloads(final Account account) {
        return false;
    }

    protected String getNamespaceMembersMain() {
        return "https://members." + this.getHost() + "/en";
    }

    protected String getNamespaceLogin() {
        return "https://freetour." + this.getHost() + "/en/login";
    }

    private boolean isLoggedIn(final Browser br) {
        final boolean loggedIN_html = br.containsHTML("\"siteSection\":\"members\"");
        // final boolean loggedINCookie = br.getCookie(br.getHost(), "autologin_userid", Cookies.NOTDELETEDPATTERN) != null &&
        // br.getCookie(br.getHost(), "autologin_hash", Cookies.NOTDELETEDPATTERN) != null;
        // return loggedIN_html || loggedINCookie;
        /* 2021-10-11: Based on HTML only is safer than html and/OR cookies! */
        if (loggedIN_html) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = account.getAccountInfo() != null ? account.getAccountInfo() : new AccountInfo();
        login(account, true);
        final String json = br.getRegex("window\\.context\\s*=\\s*(\\{.*?\\});\n").getMatch(0);
        final Map<String, Object> root = restoreFromString(json, TypeRef.MAP);
        final Map<String, Object> site = (Map<String, Object>) root.get("site");
        final List<String> contentSources = (List<String>) site.get("contentSource");
        if (!contentSources.isEmpty()) {
            account.setProperty(PROPERTY_ACCOUNT_CONTENT_SOURCE, contentSources.get(contentSources.size() - 1));
        }
        final Map<String, Object> user = (Map<String, Object>) root.get("user");
        /* Try to set unique string as username when cookie login was used. */
        final String email = (String) user.get("email");
        final String username = (String) user.get("username");
        if (hasUsedCookieLogin(account) && !StringUtils.isEmpty(email) && account.getUser().contains("@")) {
            account.setUser(email);
        } else if (hasUsedCookieLogin(account) && !StringUtils.isEmpty(username)) {
            account.setUser(username);
        }
        /**
         * TODO: Add support for "scheduledCancelDate" whenever a test account with such a date is available. </br>
         * "scheduledCancelDate" can also be a Boolean!
         */
        if (Boolean.TRUE.equals(user.get("isExpired"))) {
            ai.setExpired(true);
            account.setType(AccountType.FREE);
        } else {
            ai.setUnlimitedTraffic();
            account.setType(AccountType.PREMIUM);
        }
        /* This date is not always given. */
        final String expirationDate = (String) user.get("expirationDate");
        if (!StringUtils.isEmpty(expirationDate)) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expirationDate + " 23:59:59", "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        handleDownload(link, account);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, this.dllink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    handleErrorsAfterDirecturlAccess(link, account, br, true);
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_QUALITY);
        link.removeProperty(PROPERTY_DATE);
    }

    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* No captchas at all apart from maybe login captchas. */
        return false;
    }

    @Override
    public Class<? extends EvilangelCoreConfig> getConfigInterface() {
        return null;
    }
}