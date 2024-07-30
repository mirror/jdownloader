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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.KVSConfig;
import org.jdownloader.plugins.components.config.KVSConfig.PreferredStreamQuality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.components.kvs.Script;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.HTMLSearch;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public abstract class KernelVideoSharingComV2 extends antiDDoSForHost {
    public KernelVideoSharingComV2(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (this.requiresCookieLogin()) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    /* DEV NOTES */
    /* Porn_plugin */
    // Version 2.0
    // other: URL to a live demo: http://www.kvs-demo.com/
    /***
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPattern(List)} AND
     * {@link #buildAnnotationUrlsDefaultVideosPatternWithoutSlashVideos(List)} (excluding "embed" URLs). </br>
     * Examples: example.com/videos/1234/title/ </br>
     * example.com/videos/1234-title.html </br>
     * example.com/videos/
     */
    private static final String   type_normal               = "(?i)^https?://[^/]+/(?:[a-z]{2}/)?(?:videos?/)?(\\d+)(?:/|-)([^/\\?#]+)(?:/?|\\.html)$";
    /**
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPatternWithFUIDAtEnd(List)} (excluding
     * "embed" URLs). </br>
     * You need to override {@link #hasFUIDInsideURLAtTheEnd(String)} to return true when using such a pattern! </br>
     * TODO: Consider removing support for this from this main class.
     */
    private static final String   type_normal_fuid_at_end   = "^(?i)https?://[^/]+/videos?/([^/\\?#]+)-(\\d+)(?:/?|\\.html)$";
    /***
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPatternWithoutFileID(List)} and
     * {@link #buildAnnotationUrlsDefaultVideosPatternWithoutFileIDWithHTMLEnding(List)} (excluding "embed" URLs). </br>
     * You need to override {@link #hasFUIDInsideURLAtTheEnd(String)} to return false when using such a pattern!
     */
    private static final String   type_normal_without_fuid  = "(?i)^https?://[^/]+/(?:videos?/)?([^/\\?#]*?)(?:/?|\\.html)$";
    private static final String   type_mobile               = "(?i)^https?://m\\.([^/]+/(videos?/)?\\d+/[^/\\?#]+/$)";
    /**
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPatternOnlyNumbers(List)} (excluding
     * "embed" URLs).
     */
    protected static final String pattern_only_numbers      = "(?i)^https?://[^/]+/(\\d+)/?$";
    protected static final String pattern_embedded          = "(?i)^https?://[^/]+/embed/(\\d+)/?$";
    protected static final String PROPERTY_FUID             = "fuid";
    protected static final String PROPERTY_USERNAME         = "user";
    protected static final String PROPERTY_USERID           = "userid";
    protected static final String PROPERTY_CHANNELNAME      = "channel";
    protected static final String PROPERTY_CHANNELID        = "channelid";
    protected static final String PROPERTY_TITLE            = "title";
    protected static final String PROPERTY_DATE             = "date";
    protected static final String PROPERTY_IS_PRIVATE_VIDEO = "privatevideo";
    protected static final String PROPERTY_CHOSEN_QUALITY   = "chosen_quality";
    protected static final String PROPERTY_DIRECTURL        = "directurl";
    private static final String   extDefault                = ".mp4";
    private String                dllink                    = null;

    /**
     * Use this e.g. for: </br>
     * example.com/(de/)?videos/1234/title-inside-url OR: </br>
     * example.com/embed/1234 OR </br>
     * OR(rare/older case):</br>
     * m.example.com/videos/1234/title-inside-url | m.example.com/embed/1234 </br>
     * Example: <a href="https://kvs-demo.com/">kvs-demo.com</a> More example hosts in generic class:
     * {@link #KernelVideoSharingComV2HostsDefault}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPattern(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:[a-z]{2}/)?videos?/\\d+/[^/\\?#]+/?|embed/\\d+/?)|https?://(?:m|member)\\." + buildHostsPatternPart(domains) + "/videos?/\\d+/[^/\\?#]+/?");
        }
        return ret.toArray(new String[0]);
    }

    protected String generateContentURLDefaultVideosPattern(final String host, final String fuid, final String urlSlug) {
        if (host == null || fuid == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/videos/" + fuid + "/" + urlSlug + "/";
    }

    /**
     * Use this e.g. for: </br>
     * example.com/1234/title-inside-url</br>
     * OR: </br>
     * example.com/embed/1234 </br>
     * OR </br>
     * Example: <a href="https://alotporn.com/">alotporn.com</a> </br>
     * More example hosts in generic class: {@link #KernelVideoSharingComV2HostsDefault2}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternWithoutSlashVideos(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\d+/[^/\\?#]+/?|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    protected String generateContentURLDefaultVideosPatternWithoutSlashVideos(final String host, final String fuid, final String urlSlug) {
        if (host == null || fuid == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/" + fuid + "/" + urlSlug + "/";
    }

    /**
     * Use this e.g. for:</br>
     * example.com/title-inside-url</br>
     * OR:</br>
     * example.com/embed/1234 </br>
     * Example: <a href="https://alphaporno.com/">alphaporno.com</a> </br>
     * Special: You need to override {@link #hasFUIDInsideURLAtTheEnd(String)} to return false when using this pattern! </br>
     * More example hosts in generic class: {@link #KernelVideoSharingComV2HostsDefault3}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternWithoutFileID(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(videos?/[^/\\?#]+/?|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    protected String generateContentURLDefaultVideosPatternWithoutFileID(final String host, final String fuid, final String urlSlug) {
        if (host == null || fuid == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/videos/" + urlSlug + "/";
    }

    /**
     * Use this e.g. for:</br>
     * example.com/videos/title-inside-url-1234 OR:</br>
     * example.com/embed/1234 </br>
     * Example: <a href="https://uiporn.com/">uiporn.com</a> </br>
     * Example classses: {@link #UipornCom}, {@link #PorngemCom}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternWithFUIDAtEnd(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(videos?/[^/\\?#]+-\\d+/|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    protected String generateContentURLDefaultVideosPatternWithFUIDAtEnd(final String host, final String fuid, final String urlSlug) {
        if (host == null || fuid == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/videos/" + urlSlug + "-" + fuid;
    }

    /**
     * Use this e.g. for:</br>
     * example.com/title-inside-url OR:</br>
     * example.com/embed/1234 </br>
     * Example: <a href="https://yogaporn.net/">yogaporn.net</a> </br>
     * Example classses: {@link #YogapornNet} </br>
     * Very rarely used pattern!
     */
    public static String[] buildAnnotationUrlsDefaultNoVideosNoFUID(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(embed/\\d+/?|[^/\\?#]+/?)");
        }
        return ret.toArray(new String[0]);
    }

    protected String appendWWWIfRequired(final String host) {
        if (!isRequiresWWW() || StringUtils.startsWithCaseInsensitive(host, "www.")) {
            // do not modify host
            return host;
        } else {
            final String hostTld = Browser.getHost(host, false);
            if (!StringUtils.equalsIgnoreCase(host, hostTld)) {
                // keep subdomain
                return host;
            } else {
                // add www.
                return "www." + host;
            }
        }
    }

    protected String generateContentURLDefaultNoVideosNoFUID(final String host, final String urlSlug) {
        if (host == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/" + urlSlug + "/";
    }

    protected String generateContentURLDefaultVideoNoFUID(final String host, final String urlSlug) {
        if (host == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/video/" + urlSlug + "/";
    }

    /**
     * Use this e.g. for:</br>
     * example.com/1234</br>
     * OR:</br>
     * example.com/embed/1234 </br>
     * Example: <a href="https://anyporn.com/">anyporn.com</a> </br>
     * Example class: {@link #AnypornCom}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternOnlyNumbers(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:embed/)?\\d+/?");
        }
        return ret.toArray(new String[0]);
    }

    protected String generateContentURLDefaultVideosPatternOnlyNumbers(final String host, final String fuid) {
        if (host == null || fuid == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/" + fuid + "/";
    }

    /**
     * Override this if URLs can end with digits but these are not your FUID! </br>
     * E.g. override this when adding host plugins with patterns that match {@link #type_normal_fuid_at_end} . </br>
     * Example: example.com/url-title.html</br>
     * Override {@link #type_normal_without_fuid} if the expected URLs do not contain any FUID at all (well, other than e.g. embed URLs - in
     * this case, FUID will always get detected).
     */
    protected boolean hasFUIDInsideURLAtTheEnd(final String url) {
        return false;
    }

    /**
     * Set this to false if URLs do not contain a FUID at all! </br>
     * Especially important for e.g.: example.com/1random-title/ or example.com/random-title-version10/ ('1' != FUID!)
     */
    protected boolean hasFUIDInsideURL(final String url) {
        return true;
    }

    @Override
    public String getAGBLink() {
        return "https://www.kvs-demo.com/terms.php";
    }

    /**
     * Override this and add dead domains so upper handling can auto update added URLs and change domain if it contains a dead domain. This
     * way a lot of "old" URLs will continue to work in JD while they may fail in browser.
     */
    protected ArrayList<String> getDeadDomains() {
        return null;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = this.getFUID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    /** Items with different FUIDs but same filenames should not get treated as mirrors! */
    @Override
    public String getMirrorID(DownloadLink link) {
        final String fuid = getFUID(link);
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && fuid != null) {
            return getHost() + "://" + fuid;
        } else {
            return super.getMirrorID(link);
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    protected int getMaxChunks(final Account account) {
        return 0;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /** Enable this for hosts which e.g. have really slow fileservers as this would otherwise slow down linkchecking e.g. camwhores.tv. */
    protected boolean enableFastLinkcheck() {
        return false;
    }

    /** Enable this to use API for all requests. */
    protected boolean useAPI() {
        return false;
    }

    /**
     * By default, title inside URL will be used whenever found. </br>
     * Let this return true to prefer title returned by {@link #regexNormalTitleWebsite(Browser) } [if not null] even if title from inside
     * URL is given.
     */
    protected boolean preferTitleHTML() {
        return false;
    }

    /**
     * Enable this for websites which have embed URLs but they're broken e.g. motherporno.com, hclips.com, privatehomeclips.com . </br>
     * EWmbed URLs will be changed to "fake" normal content URLs which should then redirect to the correct contentURL. </br>
     * <b>Warning:</b> Enabling this without testing can break embed support!!
     */
    protected boolean useEmbedWorkaround() {
        return false;
    }

    /**
     * Returns domain which is considered to work. </br>
     * Prefers to return domain of added URL.
     */
    protected String getWorkingDomain(final DownloadLink link) {
        return getWorkingDomain(link.getPluginPatternMatcher());
    }

    /** Returns working domain if domain in given URL is known to be a dead domain. */
    protected String getWorkingDomain(final String url) {
        final String addedLinkDomain = Browser.getHost(url, true);
        final ArrayList<String> deadDomains = this.getDeadDomains();
        if (deadDomains != null && deadDomains.size() > 0) {
            for (final String deadDomain : deadDomains) {
                if (StringUtils.containsIgnoreCase(addedLinkDomain, deadDomain)) {
                    /* Domain in url is dead -> Return this plugins' main domain. */
                    return this.getHost();
                }
            }
        }
        /* Assume that domain in user added URL is working. */
        return addedLinkDomain;
    }

    /**
     * Returns URL to content with a hopefully working domain. </br>
     */
    protected String getContentURL(final DownloadLink link) {
        final String domain = getWorkingDomain(link);
        /*
         * Fallback and for websites where it is not always possible to generate contentURLs e.g. videocelebs.net, pornhat.com,
         * theyarehuge.com.
         */
        final String originalurl = link.getPluginPatternMatcher();
        final String url = originalurl.replaceFirst(Pattern.quote(Browser.getHost(originalurl, true)), domain);
        return url;
    }

    protected String getWeakFiletitle(final DownloadLink link) {
        final String titleURL = this.getURLTitleCorrected(link.getPluginPatternMatcher());
        if (!StringUtils.isEmpty(titleURL)) {
            /* Set this so that offline items have "nice" titles too. */
            return titleURL;
        } else {
            return this.getFUID(link);
        }
    }

    /**
     * Returns non-embed URL of this website e.g.: https://www.camwhoreshd.com/videos/1234567/blablub-blablub-slug/ </br>
     */
    abstract String generateContentURL(final String host, final String fuid, final String urlSlug);

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* Provide account for availablecheck whenever possible. */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    protected AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (useAPI()) {
            return this.requestFileInformationAPI(link, account, isDownload);
        } else {
            return this.requestFileInformationWebsite(link, account, isDownload);
        }
    }

    protected void login(final Account account, final boolean validateCookies, final DownloadLink link) throws Exception {
        login(account, validateCookies);
        final String workingDomain = Browser.getHost(this.getWorkingDomain(link));
        if (!StringUtils.equalsIgnoreCase(workingDomain, getHost())) {
            // forward cookies to different domain(s)
            final Cookies cookies = br.getCookies(getHost());
            if (cookies != null && !cookies.isEmpty()) {
                br.setCookies(workingDomain, cookies);
            }
        }
    }

    /**
     * Alternative way to linkcheck (works only for some hosts and only if FUID is given): privat-zapisi.biz/feed/12345.xml </br>
     * Also working for: webcamsbabe.com
     */
    protected AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        final String weakFilename = getWeakFiletitle(link);
        if (!link.isNameSet() && weakFilename != null) {
            /* Set this so that offline items have "nice" titles too. */
            link.setName(weakFilename + extDefault);
        }
        if (link.getReferrerUrl() != null) {
            /* Rarely needed e.g. for embedded videos from camwhores.tv by camseek.com. */
            br.getHeaders().put("Referer", link.getReferrerUrl());
        }
        /* Login if we got an account. */
        if (account != null) {
            this.login(account, false, link);
        }
        final String fuid = this.getFUID(link);
        final String contenturl = this.getContentURL(link);
        boolean useEmbedWorkaround = false;
        if (isEmbedURL(contenturl) && this.useEmbedWorkaround()) {
            useEmbedWorkaround = true;
        } else {
            embedHandling: if (isEmbedURL(contenturl)) {
                /* Embed URL */
                getPage(contenturl);
                /* in case there is http<->https or url format redirect */
                br.followRedirect();
                if (br.getHttpConnection().getResponseCode() == 403) {
                    /* E.g. hdzog.com */
                    logger.info("Looks like this website doesn't support embed links (anymore?) -> Try auto embed workaround");
                    useEmbedWorkaround = true;
                    break embedHandling;
                }
                if (isOfflineWebsite(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* Rare case: Embedded content -> URL does not contain a title -> Look for "real" URL in html and get title from there! */
                /* Try to find URL-title */
                /*
                 * A lot of websites will provide lower qualiy in embed mode! Let's fix that by trying to find the original URL. It is
                 * typically stored in "video_alt_url" labeled as "720p" and html will also contain: "video_alt_url_redirect: '1'" (=
                 * "safe place") </br>
                 */
                String realURL = null;
                if (br.containsHTML("video_alt_url_redirect\\s*:\\s*'1'")) {
                    /* Examples which would fail without this extra check: frprn.com */
                    realURL = br.getRegex("video_alt_url\\s*:\\s*'(https?://[^<>\"\\']+)'").getMatch(0);
                }
                /*
                 * Tries to find original URL based on different default patterns --> "Unsafe attempt". Examples: porngem.com, nudogram.com
                 */
                if (realURL == null) {
                    /** {@link #buildAnnotationUrlsDefaultVideosPatternWithFUIDAtEnd(List) } */
                    realURL = br.getRegex("(https?://[^/\"\\']+/videos?/[^/\\?#<>\"]+-" + fuid + ")").getMatch(0);
                }
                if (realURL == null) {
                    /** {@link #buildAnnotationUrlsDefaultVideosPattern(List)} */
                    realURL = br.getRegex("(https?://[^/\"\\']+/videos?/" + fuid + "/[^/\\?#<>\"]+/?)").getMatch(0);
                }
                if (realURL == null) {
                    /** {@link #buildAnnotationUrlsDefaultVideosPatternWithoutSlashVideos(List)} */
                    realURL = br.getRegex("(https?://[^/\"\\']+/" + fuid + "/[^/\\?#<>\"]+/?)").getMatch(0);
                }
                if (realURL == null) {
                    /** {@link #buildAnnotationUrlsDefaultVideosPatternOnlyNumbers(List)} */
                    realURL = br.getRegex("(https?://[^/\"\\']+/" + fuid + "/?)").getMatch(0);
                }
                if (realURL == null) {
                    /* 2020-11-10: Experimental feature: This can fix "broken" embed URLs: https://svn.jdownloader.org/issues/89009 */
                    final String embedTitle = regexEmbedTitleWebsite(br);
                    if (!StringUtils.isEmpty(embedTitle)) {
                        /*
                         * "Convert" embed title to URL-title (slug). Unsafe attempt but this can make "embed" URLs downloadable that
                         * wouldn't be downloadable otherwise.
                         */
                        String urlTitle = Encoding.htmlDecode(embedTitle).trim().toLowerCase();
                        urlTitle = urlTitle.replaceAll("[^a-z0-9]", "-");
                        /* Make sure that that string doesn't start- or end with "-". */
                        urlTitle = new Regex(urlTitle, "^(\\-*)(.*?)(\\-*)$").getMatch(1);
                        realURL = this.generateContentURL(this.getHost(), fuid, urlTitle);
                        logger.info("Generated real URL corresponding to embed URL: " + realURL);
                    } else {
                        logger.info("Failed to find- and generate real URL corresponding to embed URL");
                    }
                } else {
                    logger.info("Found real URL corresponding to embed URL: " + realURL);
                }
                if (!StringUtils.isEmpty(realURL)) {
                    logger.info("Found real URL corresponding to embed URL: " + realURL);
                    try {
                        realURL = br.getURL(realURL).toExternalForm();
                        final Browser brc = this.createNewBrowserInstance();
                        brc.getPage(realURL);
                        /* Fail-safe: Only set this URL as PluginPatternMatcher if it contains our expected videoID! */
                        if ((!this.hasFUIDInsideURL(null) || (this.hasFUIDInsideURL(null) && brc.getURL().contains(fuid))) && new Regex(brc.getURL(), this.getSupportedLinks()).matches() && !this.isOfflineWebsite(brc)) {
                            logger.info("Successfully found real URL: " + realURL);
                            link.setPluginPatternMatcher(brc.getURL());
                            br.setRequest(brc.getRequest());
                        } else {
                            /* This should never happen */
                            logger.warning("Cannot trust 'real' URL: " + realURL);
                        }
                    } catch (final MalformedURLException ignore) {
                        logger.log(ignore);
                        logger.info("URL parsing failure");
                    }
                } else {
                    logger.info("Unable to convert embedded URL --> Real URL");
                    if (br.containsHTML(">\\s*You are not allowed to watch this video")) {
                        /**
                         * Some websites have embedding videos disabled but nevertheless it is possible to generate- and add such URLs. It
                         * may also happen that a website owner disabled embedding after first allowing it. </br>
                         * The content should be online but we'll never be able to download it --> Treat as offline
                         */
                        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                            /*
                             * For debug purposes so that such URLs get displayed as "unchecked" in LinkGrabber so new example URLs for this
                             * edge case can be found easier.
                             */
                            throw new PluginException(LinkStatus.ERROR_FATAL, "This content cannot be embedded - try to find- and add the original URL");
                        } else {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "This content cannot be embedded - try to find- and add the original URL");
                        }
                    }
                }
            } else {
                /* Normal URL */
                getPage(contenturl);
                /* in case there is http<->https or url format redirect */
                br.followRedirect();
                if (isOfflineWebsite(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* Look for real unique videoID just in case it is not present in our use-given URL. */
                String fuidInsideHTML = br.getRegex("\"https?://" + Pattern.quote(br.getHost()) + "/embed/(\\d+)/?\"").getMatch(0);
                if (fuidInsideHTML == null) {
                    /* E.g. for hosts which have embed support disabled or are using other embed URLs than default e.g. h2porn.com. */
                    fuidInsideHTML = br.getRegex("video_?id\\s*:\\s*\\'(\\d+)\\'").getMatch(0);
                }
                if (fuidInsideHTML == null) {
                    /* Common Trait 3. E.g. tubewolf.com */
                    fuidInsideHTML = br.getRegex("\\['video_?id'\\]\\s*=\\s*(\\d+)").getMatch(0);
                }
                /* 2020-11-04: Other possible places: "videoId: '12345'" (without "") [e.g. privat-zapisi.biz] */
                /* 2020-11-04: Other possible places: name="video_id" value="12345" [e.g. privat-zapisi.biz] */
                if (fuidInsideHTML != null) {
                    if (fuid == null) {
                        /**
                         * We have no FUID -> Store the one we found in html.
                         */
                        logger.info("Setting FUID found inside HTML as DownloadLink FUID");
                        link.setProperty(PROPERTY_FUID, fuidInsideHTML);
                    } else if (!StringUtils.equals(fuid, fuidInsideHTML)) {
                        /* Fuid that we found in html does not match FUID we already have -> Log this */
                        logger.warning("FUID inside URL doesn't match FUID found in HTML: URL: " + fuid + " | HTML: " + fuidInsideHTML);
                    } else {
                        /* Everything alright - FUID of inside URL equals FUID found in HTML! */
                    }
                } else {
                    /* This can happen but most of all times, a FUID should be present inside HTML. */
                    logger.info("Failed to find fuid in html");
                }
            }
        }
        if (useEmbedWorkaround) {
            /* Embed URL --> Build fake real URL and just go for it */
            final String fakeContentURL = this.generateContentURL(this.getWorkingDomain(link), fuid, "dummystring");
            br.getPage(fakeContentURL);
            if (isOfflineWebsite(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.info("Embed workaround result: Presumed real ContentURL: " + br.getURL());
        }
        if (br.containsHTML("\"iab_extended\"")) {
            /* 2024-06-18: New e.g. vjav.com, txxx.com, upornia.com, mrgay.com and many more */
            logger.info("Auto detected the need to use API for linkcheck");
            return this.requestFileInformationAPI(link, account, isDownload);
        }
        final String title = getFileTitle(link);
        if (!StringUtils.isEmpty(title)) {
            link.setFinalFileName(title + extDefault);
        }
        if (this.isPrivateVideoWebsite(this.br)) {
            link.setProperty(PROPERTY_IS_PRIVATE_VIDEO, true);
            return AvailableStatus.TRUE;
        }
        /* Not a private video or we're currently logged in [and friends with the uploader] so we can watch it. */
        link.removeProperty(PROPERTY_IS_PRIVATE_VIDEO);
        /* Only look for downloadurl if we need it! */
        if (isDownload || !this.enableFastLinkcheck()) {
            try {
                dllink = getDllink(link, this.br);
            } catch (final PluginException e) {
                // TODO: Check if this check is still required - the "video is private" status should be known before here already.
                if (this.isPrivateVideo(link) && e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    logger.log(e);
                    logger.info("ERROR_FILE_NOT_FOUND in getDllink but we have a private video so it is not offline ...");
                } else {
                    throw e;
                }
            }
        }
        if (!StringUtils.isEmpty(this.dllink) && !isDownload && !enableFastLinkcheck() && !this.isHLS(this.dllink)) {
            URLConnectionAdapter con = null;
            try {
                /* if you don't do this then referrer is fked for the download! -raztoki */
                final Browser brc = this.br.cloneBrowser();
                brc.setFollowRedirects(true);
                brc.setAllowedResponseCodes(new int[] { 405 });
                // In case the link redirects to the finallink -
                // brc.getHeaders().put("Accept-Encoding", "identity");
                con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(dllink));
                if (this.looksLikeHLS(con)) {
                    logger.info("Got HLS stream instead of progressive video");
                    return AvailableStatus.TRUE;
                }
                final String workaroundURL = getHttpServerErrorWorkaroundURL(con);
                if (workaroundURL != null && !this.looksLikeDownloadableContent(con)) {
                    con.disconnect();
                    con = openAntiDDoSRequestConnection(brc, brc.createGetRequest(workaroundURL));
                }
                this.handleConnectionErrors(brc, con);
                if (con.isContentDecoded()) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                this.dllink = con.getRequest().getUrl();
                logger.info("dllink: " + dllink);
                /* Save directurl for later usage */
                link.setProperty(PROPERTY_DIRECTURL, this.dllink);
                if (StringUtils.isEmpty(title)) {
                    /* Fallback - attempt to find final filename */
                    final String contentDispositionFilename = Plugin.getFileNameFromDispositionHeader(con);
                    final String filenameFromFinalDownloadurl;
                    if (!StringUtils.isEmpty(contentDispositionFilename)) {
                        logger.info("Using final filename from content-disposition header: " + contentDispositionFilename);
                        link.setFinalFileName(contentDispositionFilename);
                    } else if (!StringUtils.isEmpty(filenameFromFinalDownloadurl = Plugin.getFileNameFromURL(con.getURL()))) {
                        logger.info("Using final filename from inside final downloadurl: " + filenameFromFinalDownloadurl);
                        link.setFinalFileName(filenameFromFinalDownloadurl);
                    } else {
                        logger.warning("Failed to find any final filename so far");
                    }
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

    protected boolean isHLS(final String url) {
        if (StringUtils.containsIgnoreCase(url, ".m3u8")) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isEmbedURL(final String url) {
        if (url == null) {
            return false;
        } else if (url.matches(pattern_embedded)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isHTTPsSupported() {
        return true;
    }

    protected boolean isRequiresWWW() {
        return true;
    }

    protected String getProtocol() {
        if (isHTTPsSupported()) {
            return "https://";
        } else {
            return "http://";
        }
    }

    protected AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (account != null) {
            this.login(account, false, link);
        }
        final String videoID = this.getFUID(link);
        if (!link.isNameSet()) {
            /* Set this so that offline items have "nice" titles too. */
            final String weakFilename = getWeakFiletitle(link);
            if (weakFilename != null) {
                link.setName(weakFilename);
            }
        }
        final String lifetime = "86400";
        Request request = br.createGetRequest(getProtocol() + this.getHost() + "/api/json/video/" + lifetime + "/" + getAPIParam1(videoID) + "/" + this.getAPICroppedVideoID(videoID) + "/" + videoID + ".json");
        br.getPage(request);
        if (br.getHttpConnection().getResponseCode() == 403) {
            // 2022-12-09: doing the same request again may just work fine
            logger.info("Retry due to http response 403");
            sleep(500, link);
            request = request.cloneRequest();
            br.getPage(request);
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> video = (Map<String, Object>) entries.get("video");
        if (video == null) {
            if ("video_not_found".equals(entries.get("code"))) {
                /**
                 * 2023-04-28 e.g.: https://txxx.com/embed/882346568220/ </br>
                 * Typically also comes with this json: "error":1,"code":"video_not_found"
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final Map<String, Object> channel = (Map<String, Object>) video.get("channel");
        final Map<String, Object> user = (Map<String, Object>) video.get("user");
        final String status_idStr = (String) video.get("status_id");
        if (!status_idStr.equals("1")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = (String) video.get("title");
        final String description = (String) video.get("description");
        if (!StringUtils.isEmpty(title)) {
            link.setFinalFileName(title + extDefault);
        }
        if (StringUtils.isEmpty(link.getComment()) && !StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        /* Set some Packagizer properties */
        link.setProperty(PROPERTY_TITLE, title);
        if (channel != null) {
            link.setProperty(PROPERTY_CHANNELNAME, channel.get("title"));
            link.setProperty(PROPERTY_CHANNELID, channel.get("id"));
        }
        link.setProperty(PROPERTY_USERNAME, user.get("username"));
        link.setProperty(PROPERTY_USERID, user.get("id"));
        link.setProperty(PROPERTY_DATE, video.get("post_date"));
        final long is_private = JavaScriptEngineFactory.toLong(video.get("is_private"), 0);
        if (is_private == 1) {
            link.setProperty(PROPERTY_IS_PRIVATE_VIDEO, true);
            return AvailableStatus.TRUE;
        } else {
            link.removeProperty(PROPERTY_IS_PRIVATE_VIDEO);
        }
        if (this.enableFastLinkcheck() && !isDownload) {
            return AvailableStatus.TRUE;
        } else {
            this.dllink = this.getDllinkViaAPI(br, link, videoID);
            if (!isDownload) {
                /* Only check directurl during availablecheck, not if user has started downloading. */
                URLConnectionAdapter con = null;
                try {
                    con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
                    this.handleConnectionErrors(br, con);
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
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
        // Enf of function
    }

    protected String getAPIParam1(final String videoID) {
        String ret = videoID != null ? videoID.replaceFirst("(.{6})$", "000000") : "0";
        if (ret != null) {
            // remove trailing zero
            ret = Long.toString(Long.parseLong(ret));
        }
        return ret;
    }

    protected String getAPICroppedVideoID(final String videoID) {
        String ret = videoID != null ? videoID.replaceFirst("(.{3})$", "000") : "0";
        if (ret != null) {
            // remove trailing zero
            ret = Long.toString(Long.parseLong(ret));
        }
        return ret;
    }

    protected boolean isOfflineWebsite(final Browser br) {
        final int responsecode = br.getHttpConnection().getResponseCode();
        if (responsecode == 404 || responsecode == 410) {
            return true;
        } else if (br._getURL().getPath().matches("(?i)/4(04|10)\\.php.*")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*(Sorry, this video was deleted per copyright owner request|Video has been removed due to a violation of the Terms and Conditions|Deleted by the owner request|This video has been removed|Video removed due to DMCA)")) {
            return true;
        } else {
            return false;
        }
    }

    /** If this returns true we for sure got a private video or paid content which can only be viewed with a paid account. */
    protected boolean isPrivateVideoWebsite(final Browser br) {
        /* 2020-10-09: Tested for pornyeah.com, anyporn.com, camwhoreshd.com */
        if (br.containsHTML("(?i)>\\s*Cette vidéo est privée d'utilisateur|Seulement les utilisateurs enregistrés du site peuvent avoir accès à des vidéos privées")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*Questo è il video personale dell'utente|Solo gli utenti iscritti al sito possono avere l'accesso ai video personali")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*Este video privado e de usuario|So os usuarios registados no sitio podem ter acesso aos videos privados")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*Это личное видео пользователя|Только зарегистрированные пользователи сайта могут иметь доступ к личным видео")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*Es el video privado del usuario|Seulement les utilisateurs enregistrés du site peuvent avoir accès à des vidéos privées")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*Das ist ein privates Video des Benutzers|Zugang zu den privaten Videos haben nur registrierte Benutzer")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*This video is a private video uploaded by |Only active members can watch private videos")) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*This is a private video\\. You must be")) {
            // xfreehd.com
            // This is a private video. You must be subscribers or friends with XYZ to view it
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if we know that this is a private video based on prior check and stored property. */
    protected boolean isPrivateVideo(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_PRIVATE_VIDEO)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Contains logic to determine best file title. </br>
     * Override the following functions if you want to modify filenames: {@link #regexNormalTitleWebsite()},
     * {@link #regexEmbedTitleWebsite()}, {@link #preferTitleHTML()}
     */
    private String getFileTitle(final DownloadLink link) {
        final String fuid = this.getFUID(link);
        final String titleUrl = getTitleURL(br, link);
        String titleFromHtml = null;
        /* For embed URLs the title might be in a different part of the html -> Check for this first */
        if (link.getPluginPatternMatcher().matches(pattern_embedded)) {
            titleFromHtml = regexEmbedTitleWebsite(br);
        }
        if (StringUtils.isEmpty(titleFromHtml)) {
            titleFromHtml = regexNormalTitleWebsite(br);
        }
        if (titleFromHtml != null) {
            /* Remove html crap and spaces at the beginning and end. */
            titleFromHtml = Encoding.htmlDecode(titleFromHtml).trim();
            titleFromHtml = cleanupFilename(br, titleFromHtml);
        }
        if (titleFromHtml != null && (this.preferTitleHTML() || StringUtils.equals(fuid, titleUrl))) {
            return titleFromHtml;
        } else if (!StringUtils.isEmpty(titleUrl)) {
            return titleUrl;
        } else if (!StringUtils.isEmpty(titleFromHtml)) {
            return titleFromHtml;
        } else {
            return null;
        }
    }

    protected String getTitleURL(final Browser br, final DownloadLink link) {
        String title_url = this.getURLTitleCorrected(br.getURL());
        if (title_url == null) {
            title_url = this.getURLTitleCorrected(link.getPluginPatternMatcher());
        }
        return title_url;
    }

    protected String regexEmbedTitleWebsite(final Browser br) {
        final String ret = br.getRegex("<title>\\s*([^<>\"]*?)\\s*(/|-)\\s*Embed\\s*(Player|Video)</title>").getMatch(0);
        return ret;
    }

    protected String regexNormalTitleWebsite(final Browser br) {
        String best = null;
        final String header = br.getRegex("<h(?:1|2)>\\s*(.*?)\\s*</h(?:1|2)>").getMatch(0);
        if (StringUtils.isNotEmpty(header)) {
            best = header;
        }
        final String videoInfo = br.getRegex("\"title-panel clearfix\"\\s*>\\s*<h\\d+>\\s*(.*?)\\s*</h\\d+>").getMatch(0);
        if (best == null || (videoInfo != null && videoInfo.length() < best.length())) {
            best = videoInfo;
        }
        final String ogTitle = HTMLSearch.searchMetaTag(br, "og:title");
        if (best == null || (ogTitle != null && ogTitle.length() < best.length())) {
            best = ogTitle;
        }
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            /* Remove "mytitle - domain.tld" and similar */
            title = title.replaceAll("(?i)\\s+[\\-/]\\s+" + Pattern.quote(br.getHost()), "");
        }
        if (best == null || (title != null && title.length() < best.length())) {
            best = title;
        }
        if (best == null || (header != null && header.length() > best.length() && !header.contains(">"))) {
            best = header;
        }
        return best;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        this.handleDownload(link, null);
    }

    protected void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (attemptStoredDownloadurlDownload(link, account)) {
            logger.info("Re-using stored directurl");
        } else {
            requestFileInformation(link, account, true);
            if (StringUtils.isEmpty(this.dllink)) {
                if (this.isPrivateVideo(link)) {
                    throw new AccountRequiredException("Private videos can only be watched by registered users or friends of the uploader");
                } else {
                    /* Broken video or broken plugin. */
                    this.checkErrorsLastResort(br, link, account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Broken video (?)");
                }
            }
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final boolean isHLS = isHLS(this.dllink);
            boolean isSurpriseHLS = false;
            if (!isHLS) {
                /* Progressive video stream download */
                final int maxChunks = getMaxChunks(account);
                final boolean isResumeable = isResumeable(link, account);
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.dllink, isResumeable, maxChunks);
                if (looksLikeHLS(dl.getConnection())) {
                    /* Surprise-HLS (e.g. ok.xxx) */
                    logger.info("Got HLS instead of progressive video download");
                    isSurpriseHLS = true;
                    br.followConnection();
                    dl = null;
                } else {
                    final String workaroundURL = getHttpServerErrorWorkaroundURL(dl.getConnection());
                    if (workaroundURL != null && !this.looksLikeDownloadableContent(dl.getConnection())) {
                        dl.getConnection().disconnect();
                        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, workaroundURL, isResumeable, maxChunks);
                    }
                    handleConnectionErrors(br, dl.getConnection());
                    link.setProperty(PROPERTY_DIRECTURL, dl.getConnection().getURL().toExternalForm());
                }
            }
            if (isHLS || isSurpriseHLS) {
                /* hls download */
                final Browser brc = br.cloneBrowser();
                if (isSurpriseHLS) {
                    /* Small hack */
                    brc.setRequest(br.getRequest());
                } else {
                    /* Access hls master */
                    getPage(brc, this.dllink);
                }
                handleConnectionErrors(brc, brc.getHttpConnection());
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(brc));
                if (hlsbest == null) {
                    this.checkErrorsLastResort(brc, link, account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                checkFFmpeg(link, "Download a HLS Stream");
                dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
                link.setProperty(PROPERTY_DIRECTURL, hlsbest.getDownloadurl());
            }
        }
        dl.startDownload();
    }

    private boolean looksLikeHLS(final URLConnectionAdapter urlConnection) {
        return LinkCrawlerDeepInspector.looksLikeMpegURL(urlConnection);
    }

    /** Use this during download handling instead of just throwing PluginException with LinkStatus ERROR_PLUGIN_DEFECT! */
    protected void checkErrorsLastResort(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        logger.info("Last resort errorhandling");
        String website_error = br.getRegex("class=\"message\"[^>]*>([^<]+)<").getMatch(0);
        if (website_error != null) {
            website_error = Encoding.htmlDecode(website_error).trim();
            logger.info("Found website error: " + website_error);
            final long waitMillis = 5 * 60 * 1000l;
            if (link == null) {
                throw new AccountUnavailableException(website_error, waitMillis);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, website_error);
            }
        }
        logger.warning("Unknown error happened");
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, this.isResumeable(link, account), this.getMaxChunks(account));
            this.handleConnectionErrors(br, dl.getConnection());
            valid = true;
            if (StringUtils.equalsIgnoreCase(dl.getConnection().getContentType(), "application/vnd.apple.mpegurl")) {
                /* HLS download */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable ignore) {
                }
                dl = new HLSDownloader(link, br, url);
                return true;
            } else {
                return true;
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    public static String getHttpServerErrorWorkaroundURL(final URLConnectionAdapter con) {
        /* 2020-11-03: TODO: Check if this is still needed. */
        if (con.getResponseCode() == 403 || con.getResponseCode() == 404 || con.getResponseCode() == 405 || con.getResponseCode() == 410) {
            /*
             * Small workaround for buggy servers that redirect and fail if the Referer is wrong then or Cloudflare cookies were missing on
             * first attempt Examples: hdzog.com (404), xozilla.xxx (410)
             */
            return con.getRequest().getUrl();
        } else {
            return null;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        /**
         * Registered users can watch private videos when they follow/subscribe to the uploaders. </br>
         * Apart from this there aren't much advantages. Sometimes registered users can watch higher quality streams.
         */
        final AccountInfo ai = new AccountInfo();
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies;
            final Cookies userCookies;
            if ((userCookies = account.loadUserCookies()) != null) {
                this.br.setCookies(userCookies);
                if (!validateCookies) {
                    return;
                }
                if (checkLogin(br)) {
                    logger.info("User cookie login successful");
                    return;
                } else {
                    logger.info("User Cookie login failed");
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            } else if ((cookies = account.loadCookies("")) != null) {
                br.setCookies(cookies);
                if (!validateCookies) {
                    return;
                }
                if (checkLogin(br)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            /* 2020-11-04: Login-URL that fits most of all websites (example): https://www.porngem.com/login-required/ */
            logger.info("Performing full login");
            getPage(getProtocol() + this.appendWWWIfRequired(this.getHost()) + "/login/");
            /*
             * 2017-01-21: This request will usually return a json with some information about the account.
             */
            Form loginform = br.getFormbyActionRegex(".*login.*");
            if (loginform == null) {
                /* Try using a static Form. */
                logger.warning("Failed to find loginform -> Using hardcoded loginform");
                loginform = new Form();
                loginform.setMethod(MethodType.POST);
            }
            fillWebsiteLoginForm(br, loginform, account);
            this.submitForm(loginform);
            if (!isLoggedIN(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    /** Override this and return true if conventional login via username/email and password is not possible. */
    protected boolean requiresCookieLogin() {
        return false;
    }

    /** Validates login state of given browser instance. */
    private boolean checkLogin(final Browser br) throws Exception {
        getPage(br, getProtocol() + this.appendWWWIfRequired(this.getHost()) + "/");
        if (isLoggedIN(br)) {
            return true;
        } else {
            return false;
        }
    }

    protected void fillWebsiteLoginForm(Browser br, Form loginform, Account account) {
        {
            final String user = Encoding.urlEncode(account.getUser());
            InputField userField = null;
            final String userFieldNames[] = new String[] { "username", "email" };
            for (String userFieldName : userFieldNames) {
                userField = loginform.getInputFieldByName(userFieldName);
                if (userField != null) {
                    break;
                }
            }
            if (userField != null) {
                userField.setValue(user);
            } else {
                loginform.put(userFieldNames[0], user);
            }
        }
        {
            final String password = Encoding.urlEncode(account.getPass());
            InputField passwordField = null;
            final String passwordFieldNames[] = new String[] { "pass", "password" };
            for (String passwordFieldName : passwordFieldNames) {
                passwordField = loginform.getInputFieldByName(passwordFieldName);
                if (passwordField != null) {
                    break;
                }
            }
            if (passwordField != null) {
                passwordField.setValue(password);
            } else {
                loginform.put(passwordFieldNames[0], password);
            }
        }
    }

    protected boolean isLoggedIN(final Browser br) {
        if (br.getCookie(br.getHost(), "kt_member", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    /** Finds direct download link to [video stream] file. */
    protected String getDllink(final DownloadLink link, final Browser br) throws PluginException, IOException {
        String dllink = null;
        final String json_playlist_source = br.getRegex("sources\\s*?:\\s*?(\\[.*?\\])").getMatch(0);
        String httpurl_temp = null;
        if (json_playlist_source != null) {
            /* 2017-03-16: E.g. txxx.com */
            // see if there are non hls streams first. since regex does this based on first in entry of source == =[ raztoki20170507
            dllink = new Regex(json_playlist_source, "'file'\\s*:\\s*'((?!.*\\.m3u8)http[^<>\"']*?(mp4|flv)[^<>\"']*?)'").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                dllink = new Regex(json_playlist_source, "'file'\\s*:\\s*'(http[^<>\"']*?(mp4|flv|m3u8)[^<>\"']*?)'").getMatch(0);
            }
        }
        final HashMap<Integer, String> qualityMap = new HashMap<Integer, String>();
        /* Assume there always only exists 1 video quality without quality identifier. */
        String uncryptedUrlWithoutQualityIndicator = null;
        if (StringUtils.isEmpty(dllink)) {
            // function/0/http camwheres.tv, pornyeah, rule34video.com, videocelebs.net
            logger.info("Crawling qualities 1");
            int foundQualities = 0;
            /* Try to find the highest quality possible --> Example website that has multiple qualities available: camwhoresbay.com */
            final String[][] videoInfos = br.getRegex("(?i)([a-z0-9_]+_text)\\s*:\\s*'(\\d+p(?:\\s*(?:HD|HQ|2K|4K))?|(?:HD|HQ|2K|4K))'").getMatches();
            for (final String[] vidInfo : videoInfos) {
                final String varNameText = vidInfo[0];
                final String videoQualityStr = vidInfo[1];
                final int videoQuality;
                if (videoQualityStr.matches("\\d+p.*")) {
                    videoQuality = Integer.parseInt(new Regex(videoQualityStr, "^(\\d+)p.*").getMatch(0));
                } else if (videoQualityStr.equalsIgnoreCase("HQ")) {
                    /* 2020-12-14: Rare case e.g. thisvid.com */
                    videoQuality = 720;
                } else if (videoQualityStr.equalsIgnoreCase("2K")) {
                    videoQuality = 1440;
                } else if (videoQualityStr.equalsIgnoreCase("4K")) {
                    videoQuality = 2160;
                } else if (videoQualityStr.equalsIgnoreCase("HD")) {
                    videoQuality = 1080;
                } else {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String varNameVideoURL = varNameText.replace("_text", "");
                String dllinkTmp = br.getRegex(varNameVideoURL + "\\s*:\\s*'((?:http|/|function/0/)[^<>\"']*?)'").getMatch(0);
                if (this.isCryptedDirectURL(dllinkTmp)) {
                    final String decryptedDllinkTmp = getDllinkCrypted(br, dllinkTmp);
                    if (decryptedDllinkTmp == null) {
                        logger.warning("Failed to decrypt URL: " + dllinkTmp);
                        continue;
                    } else {
                        dllinkTmp = decryptedDllinkTmp;
                    }
                } else if (!this.isValidDirectURL(dllinkTmp)) {
                    logger.info("Skipping invalid directurl: " + dllinkTmp);
                    continue;
                }
                qualityMap.put(videoQuality, dllinkTmp);
                foundQualities++;
            }
            logger.info("Found " + foundQualities + " crypted qualities 1");
            if (qualityMap.isEmpty()) {
                /* Wider attempt */
                foundQualities = 0;
                logger.info("Crawling crypted qualities #2");
                final String functions[] = br.getRegex("(function/0/https?://[A-Za-z0-9\\.\\-/]+/get_file/[^<>\"]*?)(?:\\&amp|'|\")").getColumn(0);
                if (functions.length > 0) {
                    logger.info("Found " + functions.length + " possible crypted downloadurls");
                    for (final String cryptedDllinkTmp : functions) {
                        final String dllinkTmp = getDllinkCrypted(br, cryptedDllinkTmp);
                        if (!isValidDirectURL(dllinkTmp)) {
                            logger.warning("Failed to decrypt URL: " + cryptedDllinkTmp);
                            continue;
                        }
                        if (!addQualityURL(this.getDownloadLink(), qualityMap, dllinkTmp)) {
                            uncryptedUrlWithoutQualityIndicator = dllinkTmp;
                            continue;
                        }
                    }
                }
                logger.info("Found " + foundQualities + " crypted qualities #2");
            }
            /* Prefer known qualities over those where we do not know the quality. */
            if (qualityMap.size() > 0) {
                logger.info("Found " + qualityMap.size() + " total crypted downloadurls");
                dllink = handleQualitySelection(link, qualityMap);
            } else if (uncryptedUrlWithoutQualityIndicator != null) {
                logger.info("Seems like there is only a single quality available --> Using that one");
                dllink = uncryptedUrlWithoutQualityIndicator;
            } else {
                /* This should never happen */
                logger.warning("Failed to find any (encrypted) downloadurls");
            }
        }
        /* Only try to crawl uncrypted URLs if we failed to find crypted URLs. */
        if (StringUtils.isEmpty(dllink)) {
            /* Find the best between possibly multiple uncrypted streaming URLs */
            /* Stage 1 */
            logger.info("Crawling uncrypted qualities");
            /* Example multiple qualities available: xbabe.com */
            /*
             * Example multiple qualities available but "get_file" URL with highest quality has no quality modifier in URL (= Stage 3
             * required): fapality.com, xcum.com, camwhoresbay.com
             */
            /* Example relative URLs: shooshtime.com */
            final Set<String> dups = new HashSet<String>();
            final String[] dlURLs = br.getRegex("((?:https?://[A-Za-z0-9\\.\\-/]+)?/get_file/[^<>\"\\']*?)(?:'|\")").getColumn(0);
            int foundQualities = 0;
            for (final String dllinkTmp : dlURLs) {
                if (!isValidDirectURL(dllinkTmp)) {
                    logger.info("Skipping invalid video URL: " + dllinkTmp);
                    continue;
                } else if (!dups.add(dllinkTmp)) {
                    /* Skip duplicates */
                    continue;
                }
                checkurl: if (!this.isHLS(dllinkTmp)) {
                    URLConnectionAdapter con = null;
                    try {
                        final Browser brc = this.br.cloneBrowser();
                        brc.setFollowRedirects(true);
                        brc.setAllowedResponseCodes(new int[] { 405 });
                        con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(dllinkTmp));
                        if (this.looksLikeHLS(con)) {
                            logger.info("Found HLS stream instead of expected progressive video stream download");
                            break checkurl;
                        }
                        final String workaroundURL = getHttpServerErrorWorkaroundURL(con);
                        if (workaroundURL != null && !this.looksLikeDownloadableContent(con)) {
                            con.disconnect();
                            con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(workaroundURL));
                        }
                        if (!this.looksLikeDownloadableContent(con)) {
                            brc.followConnection(true);
                            logger.info("Skipping invalid directurl: " + dllinkTmp);
                            continue;
                        } else {
                            break checkurl;
                        }
                    } catch (Exception e) {
                        logger.log(e);
                        continue;
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }
                if (!addQualityURL(this.getDownloadLink(), qualityMap, dllinkTmp)) {
                    if (uncryptedUrlWithoutQualityIndicator == null) {
                        uncryptedUrlWithoutQualityIndicator = dllinkTmp;
                    }
                    continue;
                } else {
                    foundQualities++;
                }
            }
            logger.info("Found " + foundQualities + " qualities in stage 1");
            /* Stage 2 */
            foundQualities = 0;
            /* This can fix mistakes/detect qualities missed in stage 1 */
            final String[] sources = br.getRegex("<source[^>]*?src=\"(https?://[^<>\"]*?)\"[^>]*?type=(\"|')video/[a-z0-9]+\\2[^>]+>").getColumn(-1);
            for (final String source : sources) {
                final String dllinkTmp = new Regex(source, "src=\"(https?://[^<>\"]+)\"").getMatch(0);
                String qualityTempStr = new Regex(source, "title=\"(\\d+)p\"").getMatch(0);
                if (qualityTempStr == null) {
                    /* 2020-01-29: More open RegEx e.g. pornhat.com */
                    qualityTempStr = new Regex(source, "(\\d+)p").getMatch(0);
                }
                if (dllinkTmp == null && qualityTempStr == null) {
                    /* Skip invalid items */
                    continue;
                } else if (qualityTempStr == null) {
                    logger.info("Found item without qlaity indicator: " + dllinkTmp);
                    if (uncryptedUrlWithoutQualityIndicator == null) {
                        uncryptedUrlWithoutQualityIndicator = dllinkTmp;
                    }
                    continue;
                }
                final int qualityTmp = Integer.parseInt(qualityTempStr);
                qualityMap.put(qualityTmp, dllinkTmp);
                foundQualities++;
            }
            logger.info("Found " + foundQualities + " qualities in stage 2");
            /* Stage 3 - wider attempt of "stage 1" in "crypted" handling. Aso allows URLs without the typical "get_file" KVS pattern. */
            foundQualities = 0;
            /* E.g. good for websites like: gottanut.com */
            final String[][] videoInfos = br.getRegex("(video_(?:alt_)?url[a-z0-9_]*)\\s*:\\s*(?:\"|\\')((?:https?://|/)[^<>\"\\']+\\.mp4[^<>\"\\']*)(?:\"|\\')").getMatches();
            for (final String[] vidInfo : videoInfos) {
                final String urlVarName = vidInfo[0];
                final String dllinkTmp = vidInfo[1];
                String videoQualityText = br.getRegex(Pattern.quote(urlVarName) + "_text" + "\\s*:\\s*(?:\"|\\')([^<>\"\\']+)(?:\"|\\')").getMatch(0);
                int videoQualityHeight = -1;
                if (videoQualityText != null && videoQualityText.matches("\\d+p")) {
                    videoQualityHeight = Integer.parseInt(videoQualityText.replace("p", ""));
                } else if (videoQualityText != null && videoQualityText.matches("\\d+")) {
                    videoQualityHeight = Integer.parseInt(videoQualityText);
                } else {
                    /* Look for quality indicator inside URL. */
                    /*
                     * Just a logger. Some call their (mostly 720p) quality "High Definition" but usually there will only be one quality
                     * available then anyways (e.g. xxxymovies.com)!
                     */
                    logger.info("Found unidentifyable (text-) quality indicator: " + videoQualityText);
                    videoQualityText = new Regex(dllinkTmp, "(\\d+)p\\.mp4").getMatch(0);
                    // if (videoQualityText == null) {
                    // /* 2024-07-30: e.g. thepornbang.com */
                    // videoQualityText = new Regex(dllinkTmp, "\\d+-(\\d+)\\.mp4").getMatch(0);
                    // }
                    if (videoQualityText != null) {
                        videoQualityHeight = Integer.parseInt(videoQualityText);
                    }
                }
                if (videoQualityHeight > 0) {
                    qualityMap.put(videoQualityHeight, dllinkTmp);
                    foundQualities++;
                } else {
                    uncryptedUrlWithoutQualityIndicator = dllinkTmp;
                }
            }
            logger.info("Found " + foundQualities + " qualities in stage 3");
            if (!qualityMap.isEmpty()) {
                dllink = handleQualitySelection(link, qualityMap);
            } else if (uncryptedUrlWithoutQualityIndicator != null) {
                /* Rare case */
                logger.info("Selected URL without quality indicator: " + uncryptedUrlWithoutQualityIndicator);
                dllink = uncryptedUrlWithoutQualityIndicator;
            } else {
                /* Rare case */
                logger.info("Failed to find any quality in stage 3");
            }
        }
        /* For most of all websites, we should have found a result by now! */
        if (StringUtils.isEmpty(dllink)) {
            /* 2020-10-30: Older fallbacks */
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("(?:file|video)\\s*?:\\s*?(?:\"|')(http[^<>\"\\']*?\\.(?:m3u8|mp4|flv)[^<>\"]*?)(?:\"|')").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("(?:file|url):\\s*(\"|')(http[^<>\"\\']*?\\.(?:m3u8|mp4|flv)[^<>\"]*?)\\1").getMatch(1);
            }
            if (StringUtils.isEmpty(dllink)) { // tryboobs.com
                dllink = br.getRegex("<video src=\"(https?://[^<>\"]*?)\" controls").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (dllink != null && this.isHLS(dllink)) {
            /* 2016-12-02 - txxx.com */
            /* Prefer http over hls */
            try {
                /* First try to find highest quality */
                final String fallback_player_json = br.getRegex("\\.on\\(\\'setupError\\',function\\(\\)\\{[^>]*?jwsettings\\.playlist\\[0\\]\\.sources=(\\[.*?\\])").getMatch(0);
                final String[][] qualities = new Regex(fallback_player_json, "\\'label\\'\\s*?:\\s*?\\'(\\d+)p\\',\\s*?\\'file\\'\\s*?:\\s*?\\'(http[^<>\\']+)\\'").getMatches();
                int quality_max = 0;
                for (final String[] qualityInfo : qualities) {
                    final String quality_temp_str = qualityInfo[0];
                    final String quality_url_temp = qualityInfo[1];
                    final int quality_temp = Integer.parseInt(quality_temp_str);
                    if (quality_temp > quality_max) {
                        quality_max = quality_temp;
                        httpurl_temp = quality_url_temp;
                    }
                }
            } catch (final Throwable ignore) {
            }
            /* Last chance */
            if (httpurl_temp == null) {
                httpurl_temp = br.getRegex("\\.on\\(\\'setupError\\',function\\(\\)\\{[^>]*?\\'file\\'\\s*?:\\s*?\\'(http[^<>\"\\']*?\\.mp4[^<>\"\\']*?)\\'").getMatch(0);
            }
            if (httpurl_temp != null) {
                /* Prefer http over hls */
                dllink = httpurl_temp;
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            String video_url = br.getRegex("var\\s+video_url\\s*=\\s*(\"|')(.*?)(\"|')\\s*;").getMatch(1);
            if (video_url == null) {
                video_url = br.getRegex("var\\s+video_url=Dpww3Dw64\\(\"([^\"]+)").getMatch(0);
                if (video_url == null) {
                    // tubepornclassic.com, vjav.com
                    video_url = br.getRegex("\"video_url\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                    if (video_url != null) {
                        video_url = restoreFromString("\"" + video_url + "\"", TypeRef.STRING);
                    }
                }
            }
            /* Hosts with crypted URLs: hdzog.com, hclips.com */
            String video_url_append = br.getRegex("video_url\\s*\\+=\\s*(\"|')(.*?)(\"|')\\s*;").getMatch(1);
            if (video_url != null && video_url_append != null) {
                video_url += video_url_append;
            }
            if (video_url != null) {
                return decryptVideoURL(video_url);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            final String query = br.getRegex("\"query\"\\s*:\\s*(\\{[^\\{\\}]*?\\})").getMatch(0);
            if (query != null) {
                final Map<String, Object> queryMap = restoreFromString(query, TypeRef.MAP);
                String videoID = (String) queryMap.get("video_id");
                if (StringUtils.isEmpty(videoID)) {
                    videoID = (String) queryMap.get("videoid");
                }
                if (StringUtils.isNotEmpty(videoID) && queryMap.containsKey("lifetime")) {
                    final String dllinkAPI = getDllinkViaAPI(br.cloneBrowser(), this.getDownloadLink(), videoID);
                    if (!StringUtils.isEmpty(dllinkAPI)) {
                        return dllinkAPI;
                    }
                }
            }
            this.checkErrorsLastResort(br, link, null);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlOnlyDecode(dllink);
        return dllink;
    }

    /* Example websites: tubepornclassic.com, vjav.com, hotmovs.com, txxx.tube, vxxx.com, txxx.com */
    protected String getDllinkViaAPI(final Browser br, final DownloadLink link, final String videoID) throws IOException, PluginException {
        br.getPage("/api/videofile.php?video_id=" + videoID + "&lifetime=8640000");
        final Object responseO = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
        if (responseO instanceof Map) {
            /*
             * E.g. {"error":1,"code":"private_video"} --> E.g. if we fail to properly detect this in beforehand and try to download such
             * videos anyways.
             */
            final Map<String, Object> response = (Map<String, Object>) responseO;
            final String errorKey = response.get("code").toString();
            if (errorKey.equalsIgnoreCase("private_video")) {
                link.setProperty(PROPERTY_IS_PRIVATE_VIDEO, true);
                /* Do not throw Exception here - this will be handled later! */
                return null;
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, errorKey);
            }
        }
        final List<Map<String, Object>> renditions = (List<Map<String, Object>>) responseO;
        // String bestDownloadurl = null;
        int maxHeight = 0;
        final HashMap<Integer, String> qualityMap = new HashMap<Integer, String>();
        /* This list is usually sorted from best to worst. */
        for (final Map<String, Object> rendition : renditions) {
            final String format = (String) rendition.get("format");
            final String cryptedVideoURL = rendition.get("video_url").toString();
            final String decryptedVideoURL = decryptVideoURL(cryptedVideoURL);
            try {
                /*
                 * Check for valid URL. We're not using their API to verify onlinestatus, instead we're using the result here as an
                 * offline-indicator.
                 */
                if (decryptedVideoURL.startsWith("/")) {
                    /* Add dummy-host because relative URLs would lead to Exception. */
                    new URL("https://example.com" + decryptedVideoURL);
                } else {
                    new URL(decryptedVideoURL);
                }
            } catch (final Throwable ignore) {
                /* 2021-07-22: E.g. instead of a valid URL we get: "c46d....&ti=<timestamp>" */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final int height;
            if (format.equalsIgnoreCase("_sd.mp4")) {
                height = 480;
            } else if (format.equalsIgnoreCase(".mp4")) {
                /* E.g. tubepornclassic.com when only one quality is available */
                height = 480;
            } else if (format.equalsIgnoreCase("_hd.mp4")) {
                height = 720;
            } else if (format.equalsIgnoreCase("_hq.mp4")) {
                /* 2021-12-06 e.g. txxx.com */
                height = 720;
            } else if (format.equalsIgnoreCase("_fhd.mp4")) {
                height = 1080;
            } else {
                logger.warning("Unknown format value: " + format);
                height = 1;
            }
            if (height > maxHeight) {
                maxHeight = height;
                // bestDownloadurl = decryptedVideoURL;
            }
            qualityMap.put(height, decryptedVideoURL);
        }
        return this.handleQualitySelection(link, qualityMap);
    }

    /** Decrypts given URL if needed. */
    protected String decryptVideoURL(final String url) {
        final String[] videoUrlSplit = url.split("\\|\\|");
        final String videoUrl = videoUrlSplit[0];
        String finalURL = null;
        if (!videoUrl.startsWith("http")) {
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            final Invocable inv = (Invocable) engine;
            try {
                engine.eval(IO.readURLToString(Script.class.getResource("script.js")));
                final Object result = inv.invokeFunction("result", videoUrl);
                return result.toString();
            } catch (final Throwable ignore) {
                this.getLogger().log(ignore);
            }
        } else {
            /* URL is not crypted */
            finalURL = videoUrl;
            if (videoUrlSplit.length == 2) {
                finalURL = finalURL.replaceFirst("/get_file/\\d+/[0-9a-z]{32}/", videoUrlSplit[1]);
            } else if (videoUrlSplit.length == 4) {
                finalURL = finalURL.replaceFirst("/get_file/\\d+/[0-9a-z]{32}/", videoUrlSplit[1]);
                finalURL = finalURL + "&lip=" + videoUrlSplit[2];
                finalURL = finalURL + "&lt=" + videoUrlSplit[3];
            }
        }
        return finalURL;
    }

    private boolean addQualityURL(final DownloadLink link, final HashMap<Integer, String> qualityMap, final String url) {
        String qualityTmpStr = new Regex(url, "(?i)(\\d+)(p|m)\\.mp4").getMatch(0);
        if (qualityTmpStr == null) {
            /* Wider approach */
            qualityTmpStr = new Regex(url, "(?i)(\\d+)\\.mp4").getMatch(0);
        }
        /* Sometimes, found "quality" == fuid --> == no quality indicator at all */
        final String fuid = this.getFUID(link);
        if (qualityTmpStr == null) {
            logger.info("Failed to find quality identifier for URL: " + url);
            return false;
        } else if (qualityTmpStr == null || (qualityTmpStr != null && StringUtils.equals(qualityTmpStr, fuid))) {
            /* Sometimes, found "quality" == fuid --> == no quality indicator at all */
            logger.info("Failed to find quality identifier for URL: " + url);
            return false;
        } else {
            final int qualityTmp = Integer.parseInt(qualityTmpStr);
            qualityMap.put(qualityTmp, url);
            return true;
        }
    }

    /** Returns user preferred quality inside given quality map. Returns best, if user selection is not present in map. */
    protected final String handleQualitySelection(final DownloadLink link, final HashMap<Integer, String> qualityMap) {
        if (qualityMap.isEmpty()) {
            logger.info("Cannot perform quality selection: qualityMap is empty");
            return null;
        }
        logger.info("Total found qualities: " + qualityMap.size());
        final Iterator<Entry<Integer, String>> iterator = qualityMap.entrySet().iterator();
        int maxQuality = 0;
        String maxQualityDownloadurl = null;
        final int userSelectedQuality = this.getPreferredStreamQuality();
        String selectedQualityDownloadurl = null;
        while (iterator.hasNext()) {
            final Entry<Integer, String> entry = iterator.next();
            final int qualityTmp = entry.getKey();
            if (qualityTmp > maxQuality) {
                maxQuality = entry.getKey();
                maxQualityDownloadurl = entry.getValue();
            }
            if (qualityTmp == userSelectedQuality) {
                selectedQualityDownloadurl = entry.getValue();
                break;
            }
        }
        final int chosenQuality;
        final String downloadurl;
        if (selectedQualityDownloadurl != null) {
            logger.info("Found user selected quality: " + userSelectedQuality + "p");
            chosenQuality = userSelectedQuality;
            downloadurl = selectedQualityDownloadurl;
        } else {
            logger.info("Auto-Chosen quality: " + maxQuality + "p");
            chosenQuality = maxQuality;
            downloadurl = maxQualityDownloadurl;
        }
        link.setProperty(PROPERTY_CHOSEN_QUALITY, chosenQuality);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.setComment("ChosenQuality: " + chosenQuality + "p");
        }
        return downloadurl;
    }

    /** Checks "/get_file/"-style URLs for validity by "blacklist"-style behavior. */
    protected boolean isValidDirectURL(final String url) {
        if (url == null) {
            return false;
        } else if (!url.matches("(?i)^(?:https?://|/).*get_file.+\\.mp4.*")) {
            // logger.info("Skipping invalid video URL (= doesn't match expected pattern): " + url);
            return false;
        } else if (StringUtils.endsWithCaseInsensitive(url, "jpg/")) {
            // logger.info("Skipping invalid video URL (= picture): " + url);
            return false;
        } else if (StringUtils.containsIgnoreCase(url, "_preview.mp4")) {
            /* E.g. a lot of websites! */
            // logger.info("Skipping invalid video URL (= preview): " + url);
            return false;
        } else if (StringUtils.containsIgnoreCase(url, "_trailer.mp4")) {
            /* 2020-11-04: E.g. privat-zapisi.biz! */
            // logger.info("Skipping invalid video URL (= trailer): " + url);
            return false;
        } else {
            return true;
        }
    }

    protected boolean isCryptedDirectURL(final String url) {
        if (url == null) {
            return false;
        } else if (StringUtils.startsWithCaseInsensitive(url, "function/0/http") && this.isValidDirectURL(url.replaceFirst("(?i)function/0/", ""))) {
            return true;
        } else {
            return false;
        }
    }

    private static String getDllinkCrypted(final Browser br, final String videoUrl) {
        String dllink = null;
        // final String scriptUrl = br.getRegex("src=\"([^\"]+kt_player\\.js.*?)\"").getMatch(0);
        final String licenseCode = br.getRegex("license_code\\s*?:\\s*?\\'(.+?)\\'").getMatch(0);
        if (videoUrl.startsWith("function")) {
            if (videoUrl != null && licenseCode != null) {
                // final Browser cbr = br.cloneBrowser();
                // cbr.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                // cbr.getPage(scriptUrl);
                // final String hashRange = cbr.getRegex("(\\d+)px").getMatch(0);
                String hashRange = "16";
                dllink = decryptHash(videoUrl, licenseCode, hashRange);
            }
        } else {
            /* Return input */
            dllink = videoUrl;
        }
        return dllink;
    }

    private static String decryptHash(final String videoUrl, final String licenseCode, final String hashRange) {
        String result = null;
        List<String> videoUrlPart = new ArrayList<String>();
        Collections.addAll(videoUrlPart, videoUrl.split("/"));
        // hash
        String hash = videoUrlPart.get(7).substring(0, 2 * Integer.parseInt(hashRange));
        String nonConvertHash = videoUrlPart.get(7).substring(2 * Integer.parseInt(hashRange));
        String seed = calcSeed(licenseCode, hashRange);
        String[] seedArray = new String[seed.length()];
        for (int i = 0; i < seed.length(); i++) {
            seedArray[i] = seed.substring(i, i + 1);
        }
        if (seed != null && hash != null) {
            for (int k = hash.length() - 1; k >= 0; k--) {
                String[] hashArray = new String[hash.length()];
                for (int i = 0; i < hash.length(); i++) {
                    hashArray[i] = hash.substring(i, i + 1);
                }
                int l = k;
                for (int m = k; m < seedArray.length; m++) {
                    l += Integer.parseInt(seedArray[m]);
                }
                for (; l >= hashArray.length;) {
                    l -= hashArray.length;
                }
                StringBuffer n = new StringBuffer();
                for (int o = 0; o < hashArray.length; o++) {
                    n.append(o == k ? hashArray[l] : o == l ? hashArray[k] : hashArray[o]);
                }
                hash = n.toString();
            }
            videoUrlPart.set(7, hash + nonConvertHash);
            for (String string : videoUrlPart.subList(2, videoUrlPart.size())) {
                if (result == null) {
                    result = string;
                } else {
                    result = result + "/" + string;
                }
            }
            /* 2020-12-10: E.g. porndr.com */
            if (videoUrl.endsWith("/") && !result.endsWith("/")) {
                result += "/";
            }
        }
        return result;
    }

    private static String calcSeed(final String licenseCode, final String hashRange) {
        StringBuffer fb = new StringBuffer();
        String[] licenseCodeArray = new String[licenseCode.length()];
        for (int i = 0; i < licenseCode.length(); i++) {
            licenseCodeArray[i] = licenseCode.substring(i, i + 1);
        }
        for (String c : licenseCodeArray) {
            if (c.equals("$")) {
                continue;
            }
            int v = Integer.parseInt(c);
            fb.append(v != 0 ? c : "1");
        }
        String f = fb.toString();
        int j = f.length() / 2;
        int k = Integer.parseInt(f.substring(0, j + 1));
        int l = Integer.parseInt(f.substring(j));
        int g = l - k;
        g = Math.abs(g);
        int fi = g;
        g = k - l;
        g = Math.abs(g);
        fi += g;
        fi *= 2;
        String s = String.valueOf(fi);
        String[] fArray = new String[s.length()];
        for (int i = 0; i < s.length(); i++) {
            fArray[i] = s.substring(i, i + 1);
        }
        int i = Integer.parseInt(hashRange) / 2 + 2;
        StringBuffer m = new StringBuffer();
        for (int g2 = 0; g2 < j + 1; g2++) {
            for (int h = 1; h <= 4; h++) {
                int n = Integer.parseInt(licenseCodeArray[g2 + h]) + Integer.parseInt(fArray[g2]);
                if (n >= i) {
                    n -= i;
                }
                m.append(String.valueOf(n));
            }
        }
        return m.toString();
    }

    /** Removes unwanted stuff from url-title/slug. */
    protected String removeUnwantedURLTitleStuff(String urltitle) {
        if (urltitle == null) {
            return null;
        }
        if (!StringUtils.isEmpty(urltitle)) {
            if (removeHashSegmentsFromURLTitles()) {
                /* E.g. camhub.cc, pornado.xxx, camwhoreshd.com, */
                final String removeMe = new Regex(urltitle, "(-[a-f0-9]{16}-?)$").getMatch(0);
                if (removeMe != null) {
                    /* Special 2024-02-05 */
                    urltitle = urltitle.replace(removeMe, "");
                }
            }
            /* Make the url-filenames look better by using spaces instead of '-'. */
            urltitle = urltitle.replace("-", " ");
            /* Remove eventually existing spaces at the end. */
            urltitle = urltitle.trim();
        }
        return urltitle;
    }

    protected boolean removeHashSegmentsFromURLTitles() {
        return true;
    }

    /** Returns "better human readable" file-title from URL. */
    protected String getURLTitleCorrected(final String url) {
        String urltitle = getURLTitle(url);
        urltitle = removeUnwantedURLTitleStuff(urltitle);
        return urltitle;
    }

    /**
     * Finds title inside given URL. <br />
     */
    protected String getURLTitle(final String url) {
        if (url == null) {
            return null;
        } else {
            String ret = null;
            if (url.matches(type_normal)) {
                ret = new Regex(url, type_normal).getMatch(1);
            } else if (url.matches(type_normal_fuid_at_end) && hasFUIDInsideURLAtTheEnd(url)) {
                ret = new Regex(url, type_normal_fuid_at_end).getMatch(0);
            } else if (url.matches(type_normal_without_fuid)) {
                ret = new Regex(url, type_normal_without_fuid).getMatch(0);
            } else {
                return null;
            }
            if (ret != null && ret.contains("%")) {
                ret = URLEncode.decodeURIComponent(ret);
            }
            return ret;
        }
    }

    /**
     * This is supposed to return a numeric ID. Rather return null than anything else here! </br>
     * Override {@link #hasFUIDInsideURL(String)} to return false if you know that your URLs do not contain a FUID for sure.
     */
    protected String getFUID(final DownloadLink link) {
        /* Prefer stored unique ID over ID inside URL because sometimes none is given inside URL. */
        if (link.hasProperty(PROPERTY_FUID)) {
            return link.getStringProperty(PROPERTY_FUID);
        } else {
            return this.getFUIDFromURL(link.getPluginPatternMatcher());
        }
    }

    /**
     * Tries to return unique contentID found inside URL. It is not guaranteed to return anything (depends on source URL/website) but it
     * should in most of all cases!
     */
    protected String getFUIDFromURL(final String url) {
        if (url == null) {
            return null;
        } else {
            if (url.matches(pattern_only_numbers)) {
                return new Regex(url, pattern_only_numbers).getMatch(0);
            } else if (url.matches(pattern_embedded)) {
                return new Regex(url, pattern_embedded).getMatch(0);
            } else if (url.matches(type_normal_fuid_at_end) && hasFUIDInsideURL(url) && hasFUIDInsideURLAtTheEnd(url)) {
                return new Regex(url, type_normal_fuid_at_end).getMatch(1);
            } else if (url.matches(type_normal) && hasFUIDInsideURL(url)) {
                return new Regex(url, type_normal).getMatch(0);
            } else {
                return null;
            }
        }
    }

    /** Returns user selected stream quality (video height). -1 = BEST/default */
    protected final int getPreferredStreamQuality() {
        final Class<? extends KVSConfig> cfgO = this.getConfigInterface();
        if (cfgO != null) {
            final KVSConfig cfg = PluginJsonConfig.get(cfgO);
            final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
            switch (quality) {
            case Q2160P:
                return 2160;
            case Q1440P:
                return 1440;
            case Q1080P:
                return 1080;
            case Q720P:
                return 720;
            case Q480P:
                return 480;
            case Q360P:
                return 360;
            case BEST:
            default:
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * Removes parts of hostname from filename e.g. if host is "testhost.com", it will remove things such as " - TestHost", "testhost.com"
     * and so on.
     */
    private static String cleanupFilename(final Browser br, final String filename_normal) {
        final String host = br.getHost();
        if (host == null) {
            return filename_normal;
        }
        String filename_clean = filename_normal.replace(" - " + host, "");
        filename_clean = filename_clean.replace("- " + host, "");
        filename_clean = filename_clean.replace(" " + host, "");
        filename_clean = filename_clean.replace(" - " + host, "");
        filename_clean = filename_clean.replace("- " + host, "");
        filename_clean = filename_clean.replace(" " + host, "");
        if (StringUtils.isEmpty(filename_clean)) {
            /* If e.g. filename only consisted of hostname, return original as fallback though this should never happen! */
            return filename_normal;
        } else {
            return filename_clean;
        }
    }

    @Override
    public Class<? extends KVSConfig> getConfigInterface() {
        return null;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
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
