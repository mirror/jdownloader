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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.KVSConfig;
import org.jdownloader.plugins.components.config.KVSConfig.PreferredStreamQuality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.components.kvs.Script;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

public class KernelVideoSharingComV2 extends antiDDoSForHost {
    public KernelVideoSharingComV2(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    // Version 2.0
    // other: URL to a live demo: http://www.kvs-demo.com/
    /***
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPattern(List)} AND
     * {@link #buildAnnotationUrlsDefaultVideosPatternWithoutSlashVideos(List)} (excluding "embed" URLs). </br> Examples:
     * example.com/videos/1234/title/ </br> example.com/videos/1234-title.html </br> example.com/videos/
     */
    private static final String   type_normal              = "^https?://[^/]+/(?:[a-z]{2}/)?(?:videos?/)?(\\d+)(?:/|-)([a-z0-9\\-]+)(?:/?|\\.html)$";
    /**
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPatternWithFUIDAtEnd(List)} (excluding
     * "embed" URLs). </br> You need to override {@link #hasFUIDInsideURLAtTheEnd(String)} to return true when using such a pattern! </br>
     * TODO: Consider removing support for this from this main class.
     */
    private static final String   type_normal_fuid_at_end  = "^https?://[^/]+/videos?/([a-z0-9\\-]+)-(\\d+)(?:/?|\\.html)$";
    /***
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPatternWithoutFileID(List)} and
     * {@link #buildAnnotationUrlsDefaultVideosPatternWithoutFileIDWithHTMLEnding(List)} (excluding "embed" URLs). </br> You need to
     * override {@link #hasFUIDInsideURLAtTheEnd(String)} to return false when using such a pattern!
     */
    private static final String   type_normal_without_fuid = "^https?://[^/]+/(?:videos?/)?([a-z0-9\\-]+)(?:/?|\\.html)$";
    private static final String   type_mobile              = "^https?://m\\.([^/]+/(videos?/)?\\d+/[a-z0-9\\-]+/$)";
    /**
     * Matches for Strings that match patterns returned by {@link #buildAnnotationUrlsDefaultVideosPatternOnlyNumbers(List)} (excluding
     * "embed" URLs).
     */
    protected static final String type_only_numbers        = "^https?://[^/]+/(\\d+)/?$";
    protected static final String type_embedded            = "^https?://[^/]+/embed/(\\d+)/?$";
    private String                dllink                   = null;
    private boolean               server_issues            = false;
    private boolean               private_video            = false;
    private static final String   PROPERTY_FUID            = "fuid";

    /**
     * Use this e.g. for: </br> example.com/(de/)?videos/1234/title-inside-url OR: </br> example.com/embed/1234 OR </br> OR(rare/older
     * case):</br> m.example.com/videos/1234/title-inside-url | m.example.com/embed/1234 </br> Example: <a
     * href="https://kvs-demo.com/">kvs-demo.com</a> More example hosts in generic class: {@link #KernelVideoSharingComV2HostsDefault}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPattern(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:[a-z]{2}/)?videos/\\d+/[a-z0-9\\-]+/|embed/\\d+/?)|https?://m\\." + buildHostsPatternPart(domains) + "/videos/\\d+/[a-z0-9\\-]+/");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Use this e.g. for: </br> example.com/1234/title-inside-url</br> OR: </br> example.com/embed/1234 </br> OR </br> Example: <a
     * href="https://alotporn.com/">alotporn.com</a> </br> More example hosts in generic class:
     * {@link #KernelVideoSharingComV2HostsDefault2}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternWithoutSlashVideos(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\d+/[a-z0-9\\-]+/?|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Use this e.g. for:</br> example.com/title-inside-url</br> OR:</br> example.com/embed/1234 </br> Example: <a
     * href="https://alphaporno.com/">alphaporno.com</a> </br> Special: You need to override {@link #hasFUIDInsideURLAtTheEnd(String)} to
     * return false when using this pattern! </br> More example hosts in generic class: {@link #KernelVideoSharingComV2HostsDefault3}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternWithoutFileID(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(videos/[a-z0-9\\-]+/?|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Use this e.g. for:</br> example.com/videos/title-inside-url-1234 OR:</br> example.com/embed/1234 </br> Example: <a
     * href="https://uiporn.com/">uiporn.com</a> </br> Example classses: {@link #UipornCom}, {@link #PorngemCom}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternWithFUIDAtEnd(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(videos/[a-z0-9\\-]+-\\d+/|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Use this e.g. for:</br> example.com/1234</br> OR:</br> example.com/embed/1234 </br> Example: <a
     * href="https://anyporn.com/">anyporn.com</a> </br> Example class: {@link #AnypornCom}
     */
    public static String[] buildAnnotationUrlsDefaultVideosPatternOnlyNumbers(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:embed/)?\\d+/?");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Override this if URLs can end with digits but these are not your FUID! </br> E.g. override this when adding host plugins with
     * patterns that match {@link #type_normal_fuid_at_end} . </br> Example: example.com/url-title.html</br> Override
     * {@link #type_normal_without_fuid} if the expected URLs do not contain any FUID at all (well, other than e.g. embed URLs - in this
     * case, FUID will always get detected).
     */
    protected boolean hasFUIDInsideURLAtTheEnd(final String url) {
        return false;
    }

    /**
     * Set this to false if URLs do not contain a FUID at all! </br> Especially important for e.g.: example.com/1random-title/ ('1' !=
     * FUID!)
     */
    protected boolean hasFUIDInsideURL(final String url) {
        return true;
    }

    @Override
    public String getAGBLink() {
        return "http://www.kvs-demo.com/terms.php";
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(type_mobile)) {
            /* Correct mobile urls --> Normal URLs | 2020-10-30: This is old but still needed for some sites! */
            final Regex info = new Regex(link.getPluginPatternMatcher(), "^(https?://)m\\.([^/]+/(videos/)?\\d+/[a-z0-9\\-]+/$)");
            link.setPluginPatternMatcher(String.format("%swww.%s", info.getMatch(0), info.getMatch(1)));
        }
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

    /** Override this to allow attempting to auto-fix broken embed URLs. */
    protected String generateContentURL(final String fuid, final String urlTitle) {
        return null;
    }

    protected Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    /**
     * Alternative way to linkcheck (works only for some hosts and only if FUIS is given): privat-zapisi.biz/feed/12345.xml | Als working
     * for: webcamsbabe.com
     */
    protected AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        server_issues = false;
        prepBR(this.br);
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        final String titleURL = this.getURLTitleCorrected(link.getPluginPatternMatcher());
        if (!link.isNameSet() && !StringUtils.isEmpty(titleURL)) {
            /* Set this so that offline items have "nice" titles too. */
            link.setName(titleURL + ".mp4");
        }
        getPage(link.getPluginPatternMatcher());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getPluginPatternMatcher().matches(type_embedded)) {
            /* Rare case: Embedded content -> URL does not contain a title -> Look for "real" URL in html and get title from there! */
            final String fuid = this.getFUID(link);
            /* Try to find URL-title */
            /*
             * A lot of websites will provide lower qualiy in embed mode! Let's fix that by trying to find the original URL. It is typically
             * stored in "video_alt_url" labeled as "720p" and html will also contain: "video_alt_url_redirect: '1'" (= "safe place") </br>
             */
            String realURL = br.getRegex("video_alt_url\\s*:\\s*'(https?://[^<>\"\\']+)'").getMatch(0);
            // trustURL = realURL != null && br.containsHTML("video_alt_url_redirect\\s*:\\s*'1'");
            /*
             * Tries to find original URL based on different default patterns --> "Unsafe attempt". Examples: porngem.com, nudogram.com
             */
            if (realURL == null) {
                /** {@link #buildAnnotationUrlsDefaultVideosPatternWithFUIDAtEnd(List)} */
                realURL = br.getRegex("(https?://[^/\"\\']+/videos?/[a-z0-9\\-]+-" + fuid + ")").getMatch(0);
            }
            if (realURL == null) {
                /** {@link #buildAnnotationUrlsDefaultVideosPattern(List)} */
                realURL = br.getRegex("(https?://[^/\"\\']+/videos?/" + fuid + "/[a-z0-9\\-]+/?)").getMatch(0);
            }
            if (realURL == null) {
                /** {@link #buildAnnotationUrlsDefaultVideosPatternWithoutSlashVideos(List)} */
                realURL = br.getRegex("(https?://[^/\"\\']+/" + fuid + "/[a-z0-9\\-]+/?)").getMatch(0);
            }
            if (realURL == null) {
                /** {@link #buildAnnotationUrlsDefaultVideosPatternOnlyNumbers(List)} */
                realURL = br.getRegex("(https?://[^/\"\\']+/" + fuid + "/?)").getMatch(0);
            }
            if (realURL == null) {
                /* 2020-11-10: Experimental feature: This can fix "broken" embed URLs: https://svn.jdownloader.org/issues/89009 */
                final String embedTitle = regexEmbedTitle();
                if (!StringUtils.isEmpty(embedTitle)) {
                    /*
                     * "Convert" embed title to URL-title. Unsafe attempt but this can make "embed" URLs downloadable that wouldn't be
                     * downloadable otherwise.
                     */
                    String urlTitle = embedTitle.trim().toLowerCase();
                    urlTitle = urlTitle.replaceAll("[^a-z0-9]", "-");
                    /* Make sure that that string doesn't start- or end with "-". */
                    urlTitle = new Regex(urlTitle, "^(\\-*)(.*?)(\\-*)$").getMatch(1);
                    realURL = this.generateContentURL(fuid, urlTitle);
                }
            }
            if (!StringUtils.isEmpty(realURL)) {
                logger.info("Found real URL corresponding to current embed URL: " + realURL);
                try {
                    realURL = br.getURL(realURL).toString();
                    final Browser brc = this.prepBR(new Browser());
                    brc.getPage(realURL);
                    /* Fail-safe: Only set this URL as PluginPatternMatcher if it contains our expected videoID! */
                    if ((!this.hasFUIDInsideURL(null) || (this.hasFUIDInsideURL(null) && brc.getURL().contains(fuid))) && new Regex(brc.getURL(), this.getSupportedLinks()).matches() && !this.isOffline(brc)) {
                        logger.info("Successfully found real URL: " + realURL);
                        link.setPluginPatternMatcher(brc.getURL());
                        br.setRequest(brc.getRequest());
                    } else {
                        /* This should never happen */
                        logger.warning("Cannot trust 'real' URL: " + realURL);
                    }
                } catch (final MalformedURLException e) {
                    logger.log(e);
                    logger.info("URL parsing failure");
                }
            } else {
                logger.info("Unable to convert embedded URL --> Real URL");
                if (br.containsHTML(">\\s*You are not allowed to watch this video")) {
                    /**
                     * Some websites have embedding videos disabled but nevertheless it is possible to generate- and add such URLs. It may
                     * also happen that a website owner disabled embedding after first allowing it. </br> The content should be online but
                     * we'll never be able to download it --> Treat as offline
                     */
                    if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        /*
                         * For debug purposes so that such URLs get displayed as "unchecked" in LinkGrabber so new example URLs for this
                         * edge case can be found easier.
                         */
                        throw new PluginException(LinkStatus.ERROR_FATAL, "This content cannot be embedded - try to find- and add the original URL");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            }
        } else {
            String fuidInsideHTML = br.getRegex("\"https?://" + Pattern.quote(br.getHost()) + "/embed/(\\d+)/?\"").getMatch(0);
            if (fuidInsideHTML == null) {
                /* E.g. for hosts which have embed support disabled or are using other embed URLs than default e.g. h2porn.com. */
                fuidInsideHTML = br.getRegex("video_id\\s*:\\s*\\'(\\d+)\\'").getMatch(0);
            }
            if (fuidInsideHTML == null) {
                /* Common Trait 3. E.g. tubewolf.com */
                fuidInsideHTML = br.getRegex("\\['video_id'\\]\\s*=\\s*(\\d+)").getMatch(0);
            }
            /* 2020-11-04: Other possible places: "videoId: '12345'" (without "") [e.g. privat-zapisi.biz] */
            /* 2020-11-04: Other possible places: name="video_id" value="12345" [e.g. privat-zapisi.biz] */
            if (fuidInsideHTML != null) {
                logger.info("Successfully found fuid in html: " + fuidInsideHTML);
                if (this.getFUID(link) == null) {
                    /** Most likely useful for URLs matching pattern {@link #type_normal_without_fuid}. */
                    logger.info("Setting FUID found inside URL as DownloadLink FUID");
                    link.setLinkID(this.getHost() + "://" + fuidInsideHTML);
                    link.setProperty(PROPERTY_FUID, fuidInsideHTML);
                } else if (!StringUtils.equals(this.getFUID(link), fuidInsideHTML)) {
                    /* More or less helpful for debugging: This should never happen! */
                    logger.warning("FUID inside URL doesn't match FUID found in HTML: " + this.getFUID(link) + " vs. " + fuidInsideHTML);
                } else {
                    /* Everything alright - FUID of inside URL equals FUID found in HTML! */
                }
            } else {
                /* This can happen but most of all times, a FUID should be present inside HTML. */
                logger.info("Failed to find fuid in html");
            }
        }
        String finalFilename = getFileTitle(link);
        if (!StringUtils.isEmpty(finalFilename)) {
            link.setFinalFileName(finalFilename + ".mp4");
        }
        setSpecialFlags();
        /* Only look for downloadurl if we need it! */
        if (isDownload || !this.enableFastLinkcheck()) {
            try {
                dllink = getDllink(this.br);
            } catch (final PluginException e) {
                if (this.private_video && e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    logger.info("ERROR_FILE_NOT_FOUND in getDllink but we have a private video so it is not offline ...");
                } else {
                    throw e;
                }
            }
        }
        if (!StringUtils.isEmpty(this.dllink) && !dllink.contains(".m3u8") && !isDownload && !enableFastLinkcheck()) {
            URLConnectionAdapter con = null;
            try {
                // if you don't do this then referrer is fked for the download! -raztoki
                final Browser brc = this.br.cloneBrowser();
                brc.setAllowedResponseCodes(new int[] { 405 });
                // In case the link redirects to the finallink -
                try {
                    // br.getHeaders().put("Accept-Encoding", "identity");
                    con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(dllink));
                    final String workaroundURL = getHttpServerErrorWorkaroundURL(con);
                    if (workaroundURL != null) {
                        con.disconnect();
                        con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(workaroundURL));
                    }
                } catch (final BrowserException e) {
                    logger.log(e);
                    server_issues = true;
                    return AvailableStatus.TRUE;
                }
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    }
                    final String redirect_url = brc.getHttpConnection().getRequest().getUrl();
                    if (redirect_url != null) {
                        dllink = redirect_url;
                        logger.info("dllink: " + dllink);
                    }
                    if (StringUtils.isEmpty(finalFilename)) {
                        /* Fallback - attempt to find final filename */
                        final String filenameFromFinalDownloadurl = Plugin.getFileNameFromURL(con.getURL());
                        if (con.isContentDisposition()) {
                            logger.info("Using final filename from content disposition header");
                            finalFilename = Plugin.getFileNameFromHeader(con);
                            link.setFinalFileName(finalFilename);
                        } else if (!StringUtils.isEmpty(filenameFromFinalDownloadurl)) {
                            logger.info("Using final filename from inside final downloadurl");
                            finalFilename = filenameFromFinalDownloadurl;
                            link.setFinalFileName(finalFilename);
                        } else {
                            logger.info("Failed to find any final filename so far");
                        }
                    }
                } else {
                    try {
                        brc.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (StringUtils.isEmpty(finalFilename)) {
            /* Last chance fallback */
            logger.info("Looking for last chance final filename --> Trying to use FUID as filename");
            if (this.getFUID(link) != null) {
                logger.info("Using fuid as filename");
                finalFilename = this.getFUID(link) + ".mp4";
                link.setFinalFileName(finalFilename);
            } else {
                logger.warning("Failed to find any filename!");
            }
        }
        return AvailableStatus.TRUE;
    }

    protected boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("/404.php");
    }

    protected void setSpecialFlags() {
        /* 2020-10-09: Tested for pornyeah.com, anyporn.com, camwhoreshd.com */
        if (br.containsHTML(">\\s*This video is a private video uploaded by |Only active members can watch private videos")) {
            this.private_video = true;
        } else {
            this.private_video = false;
        }
    }

    protected String getFileTitle(final DownloadLink link) {
        String filename;
        String title_url = this.getURLTitleCorrected(br.getURL());
        if (title_url == null) {
            title_url = this.getURLTitleCorrected(link.getPluginPatternMatcher());
        }
        /* Prefer title from inside URL whenever possible. */
        if (!StringUtils.isEmpty(title_url) && !title_url.matches("\\d+")) {
            filename = title_url;
        } else {
            /* Try default traits --> Very unsafe but may sometimes work */
            if (link.getPluginPatternMatcher().matches(type_embedded)) {
                filename = regexEmbedTitle();
            } else {
                filename = br.getRegex(Pattern.compile("<title>([^<>\"]*?) \\- " + br.getHost() + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            }
            if (filename != null) {
                /* Remove html crap and spaces at the beginning and end. */
                filename = Encoding.htmlDecode(filename);
                filename = filename.trim();
                filename = cleanupFilename(br, filename);
            }
        }
        return filename;
    }

    protected String regexEmbedTitle() {
        return br.getRegex("<title>\\s*([^<>\"]*?)\\s*(/|-)\\s*Embed\\s*(Player|Video)</title>").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        this.handleDownload(link, null);
    }

    protected void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (StringUtils.isEmpty(this.dllink)) {
            if (private_video) {
                throw new AccountRequiredException("Private video");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Broken video (?)");
            }
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (StringUtils.containsIgnoreCase(dllink, ".m3u8")) {
            /* hls download */
            /* Access hls master. */
            final Browser brc = br.cloneBrowser();
            getPage(brc, this.dllink);
            if (brc.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (brc.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(brc));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            final int maxChunks = getMaxChunks(account);
            final boolean isResumeable = isResumeable(link, account);
            /* http download */
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.dllink, isResumeable, maxChunks);
            final String workaroundURL = getHttpServerErrorWorkaroundURL(dl.getConnection());
            if (workaroundURL != null) {
                dl.getConnection().disconnect();
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, workaroundURL, isResumeable, maxChunks);
            }
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
                } else if (dl.getConnection().getResponseCode() == 503) {
                    /* Should only happen in rare cases */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 connection limit reached", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
                }
            }
            dl.startDownload();
        }
    }

    public static String getHttpServerErrorWorkaroundURL(final URLConnectionAdapter con) {
        /* 2020-11-03: TODO: Check if this is still needed. */
        String workaroundURL = null;
        if (con.getResponseCode() == 403 || con.getResponseCode() == 404 || con.getResponseCode() == 405) {
            /*
             * Small workaround for buggy servers that redirect and fail if the Referer is wrong then or Cloudflare cookies were missing on
             * first attempt (e.g. clipcake.com). Examples: hdzog.com (404), txxx.com (403)
             */
            workaroundURL = con.getRequest().getUrl();
        }
        return workaroundURL;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
        this.handleDownload(link, account);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /* Registered users can watch private videos when they follow/subscribe to the uploaders. */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(false);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    protected void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!validateCookies) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    getPage("https://www." + this.getHost() + "/");
                    if (isLoggedIN()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                /* 2020-11-04: Login-URL that fits most of all websites (example): https://www.porngem.com/login-required/ */
                getPage("https://www." + this.getHost() + "/login/");
                /*
                 * 2017-01-21: This request will usually return a json with some information about the account. Until now there are no
                 * premium accounts available at all.
                 */
                postPage("/login/", "remember_me=1&action=login&email_link=http%3A%2F%2Fwww." + this.getHost() + "%2Femail%2F&format=json&mode=async&username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    protected boolean isLoggedIN() {
        return br.getCookie(br.getHost(), "kt_member", Cookies.NOTDELETEDPATTERN) != null;
    }

    protected String getDllink(final Browser br) throws PluginException, IOException {
        /*
         * Newer KVS versions also support html5 --> RegEx for that as this is a reliable source for our final downloadurl.They can contain
         * the old "video_url" as well but it will lead to 404 --> Prefer this way.
         * 
         * E.g. wankoz.com, pervclips.com, pornicom.com
         */
        // final String pc3_vars = br.getRegex("pC3\\s*:\\s*'([^<>\"\\']+)'").getMatch(0);
        // final String videoID = br.getRegex("video_id\\s*:\\s*(?:')?(\\d+)\\s*(?:')?").getMatch(0);
        // if (pc3_vars != null && videoID != null) {
        // /* 2019-11-26: TODO: Add support for this: Used by a lot of these hosts to hide their directurls */
        // br.postPage("/sn4diyux.php", "param=" + videoID + "," + pc3_vars);
        // String crypted_url = getDllinkCrypted(br);
        // }
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
            final String[][] videoInfos = br.getRegex("([a-z0-9_]+_text)\\s*:\\s*'(\\d+)p(?:\\s*HD)?'").getMatches();
            for (final String[] vidInfo : videoInfos) {
                final String varNameText = vidInfo[0];
                final String videoQualityStr = vidInfo[1];
                final int videoQuality = Integer.parseInt(videoQualityStr);
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
            /**
             * TODO: Check if it is a good idea to only go into the wider attempt if no URLs with quality information have been found. </br>
             * In this case, uncryptedUrlWithoutQualityIndicator is always null!
             */
            if (qualityMap.isEmpty()) {
                /* Wider attempt */
                foundQualities = 0;
                logger.info("Crawling crypted qualities 2");
                final String functions[] = br.getRegex("(function/0/https?://[A-Za-z0-9\\.\\-]+/get_file/[^<>\"]*?)(?:\\&amp|'|\")").getColumn(0);
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
                logger.info("Found " + foundQualities + " qualities 2 (crypted only)");
            }
            /* Prefer known qualities over those where we do not know the quality. */
            if (qualityMap.size() > 0) {
                logger.info("Found " + qualityMap.size() + " total crypted downloadurls");
                dllink = handleQualitySelection(qualityMap);
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
            final String[] dlURLs = br.getRegex("(https?://[A-Za-z0-9\\.\\-]+/get_file/[^<>\"]*?)(?:'|\")").getColumn(0);
            int foundQualities = 0;
            for (final String dllinkTmp : dlURLs) {
                if (!isValidDirectURL(dllinkTmp)) {
                    logger.info("Skipping invalid video URL: " + dllinkTmp);
                    continue;
                }
                /* TODO: Maybe skip URLs that do not contain current FUID (if FUID exists). E.g. failure: privat-zapisi.biz */
                // if (!dllinkTmp.contains(this.getFUID(this.getDownloadLink()))) {
                // logger.info("Skipping URL because it doesn't contain FUID: " + dllinkTmp);
                // continue;
                // }
                if (!addQualityURL(this.getDownloadLink(), qualityMap, dllinkTmp)) {
                    uncryptedUrlWithoutQualityIndicator = dllinkTmp;
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
            final String[][] videoInfos = br.getRegex("(video_url[a-z0-9_]*)\\s*:\\s*(?:\"|\\')(https?://[^<>\"\\']+\\.mp4[^<>\"\\']*)(?:\"|\\')").getMatches();
            for (final String[] vidInfo : videoInfos) {
                final String urlVarName = vidInfo[0];
                final String dllinkTmp = vidInfo[1];
                String possibleQualityIndicator = br.getRegex(urlVarName + "_text" + "\\s*:\\s*(?:\"|\\')([^<>\"\\']+)(?:\"|\\')").getMatch(0);
                int videoQuality = -1;
                if (possibleQualityIndicator != null && possibleQualityIndicator.matches("\\d+p")) {
                    videoQuality = Integer.parseInt(possibleQualityIndicator.replace("p", ""));
                } else {
                    /* Look for quality indicator inside URL. */
                    /*
                     * Just a logger. Some call their (mostly 720p) quality "High Definition" but usually there will only be one quality
                     * available then anyways (e.g. xxxymovies.com)!
                     */
                    logger.info("Found unidentifyable (text-) quality indicator: " + possibleQualityIndicator);
                    possibleQualityIndicator = new Regex(dllinkTmp, "(\\d+)p\\.mp4").getMatch(0);
                    if (possibleQualityIndicator != null) {
                        videoQuality = Integer.parseInt(possibleQualityIndicator);
                    }
                }
                if (videoQuality > 0) {
                    qualityMap.put(videoQuality, dllinkTmp);
                    foundQualities++;
                } else {
                    uncryptedUrlWithoutQualityIndicator = dllinkTmp;
                }
            }
            logger.info("Found " + foundQualities + " qualities in stage 3");
            if (!qualityMap.isEmpty()) {
                dllink = handleQualitySelection(qualityMap);
            } else if (uncryptedUrlWithoutQualityIndicator != null) {
                /* Rare case */
                logger.info("Selected URL without quality indicator: " + uncryptedUrlWithoutQualityIndicator);
                dllink = uncryptedUrlWithoutQualityIndicator;
            } else {
                /* Rare case */
                logger.info("Failed to find any quality so far");
            }
            /*
             * TODO: Find/Implement/prefer download of "official" downloadlinks e.g. xcafe.com - in this case, "get_file" URLs won't contain
             * a quality identifier (??) at least not in the format "720p" and they will contain either "download=true" or "download=1".
             */
        }
        /* For most of all website, we should have found a result by now! */
        if (StringUtils.isEmpty(dllink)) {
            /* 2020-10-30: Older fallbacks */
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("(?:file|video)\\s*?:\\s*?(?:\"|')(http[^<>\"\\']*?\\.(?:m3u8|mp4|flv)[^<>\"]*?)(?:\"|')").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("(?:file|url):[\t\n\r ]*?(\"|')(http[^<>\"\\']*?\\.(?:m3u8|mp4|flv)[^<>\"]*?)\\1").getMatch(1);
            }
            if (StringUtils.isEmpty(dllink)) { // tryboobs.com
                dllink = br.getRegex("<video src=\"(https?://[^<>\"]*?)\" controls").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (dllink != null && dllink.contains(".m3u8")) {
            /* 2016-12-02 - txxx.com, tubecup.com, hdzog.com */
            /* Prefer httpp over hls */
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
            } catch (final Throwable e) {
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
            }
            // hdzog.com, hclips.com
            String video_url_append = br.getRegex("video_url\\s*\\+=\\s*(\"|')(.*?)(\"|')\\s*;").getMatch(1);
            if (video_url != null && video_url_append != null) {
                video_url += video_url_append;
            }
            if (video_url != null) {
                String[] videoUrlSplit = video_url.split("\\|\\|");
                video_url = videoUrlSplit[0];
                if (!video_url.startsWith("http")) {
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    final Invocable inv = (Invocable) engine;
                    try {
                        engine.eval(IO.readURLToString(Script.class.getResource("script.js")));
                        final Object result = inv.invokeFunction("result", video_url);
                        if (result != null) {
                            dllink = result.toString();
                        }
                    } catch (final Throwable e) {
                        this.getLogger().log(e);
                    }
                } else {
                    dllink = video_url;
                }
                if (videoUrlSplit.length == 2) {
                    dllink = dllink.replaceFirst("/get_file/\\d+/[0-9a-z]{32}/", videoUrlSplit[1]);
                } else if (videoUrlSplit.length == 4) {
                    dllink = dllink.replaceFirst("/get_file/\\d+/[0-9a-z]{32}/", videoUrlSplit[1]);
                    dllink = dllink + "&lip=" + videoUrlSplit[2];
                    dllink = dllink + "&lt=" + videoUrlSplit[3];
                }
                return dllink;
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            if (!br.containsHTML("license_code:") && !br.containsHTML("kt_player_[0-9\\.]+\\.swfx?")) {
                /* No licence key present in html and/or no player --> No video --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // dllink = Encoding.htmlDecode(dllink); - Why?
        dllink = Encoding.urlDecode(dllink, true);
        if (dllink.contains("&amp;")) {
            dllink = Encoding.htmlDecode(dllink);
        }
        return dllink;
    }

    private boolean addQualityURL(final DownloadLink link, final HashMap<Integer, String> qualityMap, final String url) {
        String qualityTmpStr = new Regex(url, "(\\d+)p\\.mp4").getMatch(0);
        if (qualityTmpStr == null) {
            /* Wider approach */
            qualityTmpStr = new Regex(url, "(\\d+)\\.mp4").getMatch(0);
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
    private String handleQualitySelection(final HashMap<Integer, String> qualityMap) {
        if (qualityMap.isEmpty()) {
            return null;
        }
        String downloadurl = null;
        logger.info("Total found qualities: " + qualityMap.size());
        final Iterator<Entry<Integer, String>> iterator = qualityMap.entrySet().iterator();
        int maxQuality = 0;
        final int userSelectedQuality = this.getPreferredStreamQuality();
        while (iterator.hasNext()) {
            final Entry<Integer, String> entry = iterator.next();
            final int qualityTmp = entry.getKey();
            if (qualityTmp == userSelectedQuality) {
                logger.info("Found user selected quality: " + userSelectedQuality + "p");
                maxQuality = entry.getKey();
                downloadurl = entry.getValue();
                break;
            } else if (entry.getKey() > maxQuality) {
                maxQuality = entry.getKey();
                downloadurl = entry.getValue();
            }
        }
        logger.info("Selected quality: " + maxQuality + "p");
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.getDownloadLink().setComment("SelectedQuality: " + maxQuality + "p");
        }
        return downloadurl;
    }

    /** Checks "/get_file/"-style URLs for validity by "blacklist"-style behavior. */
    protected boolean isValidDirectURL(final String url) {
        if (url == null) {
            return false;
        } else if (!url.matches("https?://.+get_file.+\\.mp4.*")) {
            // logger.info("Skipping invalid video URL (= doesn't match expected pattern): " + url);
            return false;
        } else if (StringUtils.endsWithCaseInsensitive(url, "jpg/")) {
            // logger.info("Skipping invalid video URL (= picture): " + url);
            return false;
        } else if (url.contains("_preview.mp4")) {
            /* E.g. a lot of websites! */
            // logger.info("Skipping invalid video URL (= preview): " + url);
            return false;
        } else if (url.contains("_trailer.mp4")) {
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
        } else if (url.startsWith("function/0/http") && this.isValidDirectURL(url.replace("function/0/", ""))) {
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

    /** Returns "better human readable" file-title from URL. */
    protected String getURLTitleCorrected(final String url) {
        String urltitle = getURLTitle(url);
        if (!StringUtils.isEmpty(urltitle)) {
            /* Special: Remove unwanted stuff e.g.: private-shows.net, anon-v.com */
            final String removeme = new Regex(urltitle, "(-?[a-f0-9]{16})").getMatch(0);
            if (removeme != null) {
                urltitle = urltitle.replace(removeme, "");
            }
            /* Make the url-filenames look better by using spaces instead of '-'. */
            urltitle = urltitle.replace("-", " ");
            /* Remove eventually existing spaces at the end */
            urltitle = urltitle.trim();
        }
        return urltitle;
    }

    /**
     * Finds title inside given URL. <br />
     */
    protected String getURLTitle(final String url) {
        if (url == null) {
            return null;
        }
        String urlTitle = null;
        if (url.matches(type_normal)) {
            urlTitle = new Regex(url, type_normal).getMatch(1);
        } else if (url.matches(type_normal_fuid_at_end) && hasFUIDInsideURLAtTheEnd(url)) {
            urlTitle = new Regex(url, type_normal_fuid_at_end).getMatch(0);
        } else if (url.matches(type_normal_without_fuid)) {
            urlTitle = new Regex(url, type_normal_without_fuid).getMatch(0);
        }
        return urlTitle;
    }

    /**
     * This is supposed to return a numeric ID. Rather return null than anything else here! </br> Override {@link #hasFUIDInsideURL(String)}
     * to return false if you know that your URLs do not contain a FUID for sure.
     */
    protected String getFUID(final DownloadLink link) {
        /* Prefer stored unique ID over ID inside URL because sometimes none is given inside URL. */
        String fuid = link.getStringProperty(PROPERTY_FUID, null);
        if (fuid == null) {
            fuid = this.getFUIDFromURL(link.getPluginPatternMatcher());
        }
        return fuid;
    }

    /**
     * Tries to return unique contentID found inside URL. It is not guaranteed to return anything (depends on source URL/website) but it
     * should in most of all cases!
     */
    protected String getFUIDFromURL(final String url) {
        String fuid = null;
        if (url != null) {
            if (url.matches(type_only_numbers)) {
                fuid = new Regex(url, type_only_numbers).getMatch(0);
            } else if (url.matches(type_embedded)) {
                fuid = new Regex(url, type_embedded).getMatch(0);
            } else if (url.matches(type_normal_fuid_at_end) && hasFUIDInsideURL(url) && hasFUIDInsideURLAtTheEnd(url)) {
                fuid = new Regex(url, type_normal_fuid_at_end).getMatch(1);
            } else if (url.matches(type_normal) && hasFUIDInsideURL(url)) {
                fuid = new Regex(url, type_normal).getMatch(0);
            }
        }
        return fuid;
    }

    /** Returns user selected stream quality. -1 = BEST/default */
    private final int getPreferredStreamQuality() {
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
        // final String host_without_tld = host.split("\\.")[0];
        String filename_clean = filename_normal.replace(" - " + host, "");
        filename_clean = filename_clean.replace("- " + host, "");
        filename_clean = filename_clean.replace(" " + host, "");
        filename_clean = filename_clean.replace(" - " + host, "");
        filename_clean = filename_clean.replace("- " + host, "");
        filename_clean = filename_clean.replace(" " + host, "");
        if (StringUtils.isEmpty(filename_clean)) {
            /* If e.g. filename only consisted of hostname, return original as fallback though this should never happen! */
            return filename_normal;
        }
        return filename_clean;
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
