package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.BunkrAlbum;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.BunkrConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { BunkrAlbum.class })
public class Bunkr extends PluginForHost {
    public Bunkr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    @Override
    public String getAGBLink() {
        return "https://" + BunkrAlbum.MAIN_BUNKR_DOMAIN + "/faq";
    }

    public static List<String[]> getPluginDomains() {
        return BunkrAlbum.getPluginDomains();
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
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* This host is frequently changing its' main domain. */
        /* 2023-01-03: Main domain changed from bunkr.la to bunkrr.su */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static List<String> getDeadCDNDomains() {
        return Arrays.asList(new String[] { "bunkr.su" });
    }

    private int getMaxChunks(final Account account) {
        return 1;
    }

    /* Don't touch the following! */
    private static final AtomicInteger freeRunning = new AtomicInteger(0);
    private final static Pattern       PATTERN_FID = Pattern.compile("(-([A-Za-z0-9]{8}))(\\.[^\\.]+)?$");

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int max = getMaxSimultaneousFreeAnonymousDownloads();
        if (max == -1) {
            return -1;
        } else {
            final int running = freeRunning.get();
            return running + 1;
        }
    }

    private int getMaxSimultaneousFreeAnonymousDownloads() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "bunkr://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        final String lastStoredDirecturl = link.getStringProperty(PROPERTY_LAST_GRABBED_DIRECTURL);
        String fid = null;
        if (lastStoredDirecturl != null) {
            fid = getFidFromURL(lastStoredDirecturl);
        }
        if (fid == null) {
            fid = getFidFromURL(link.getPluginPatternMatcher());
        }
        if (fid != null) {
            return fid;
        } else {
            /* Fallback */
            return getFilenameFromURL(link);
        }
    }

    private String getFilenameFromURL(final DownloadLink link) {
        String filenameFromURL = getFilenameFromURL(link.getPluginPatternMatcher());
        if (filenameFromURL != null) {
            return filenameFromURL;
        } else {
            return null;
        }
    }

    private String getFilenameFromURL(final String url) {
        String filenameFromURL = new Regex(url, BunkrAlbum.PATTERN_SINGLE_FILE).getMatch(2);
        if (filenameFromURL == null) {
            try {
                filenameFromURL = getFileNameFromURL(new URL(url));
            } catch (MalformedURLException e) {
                logger.log(e);
            }
        }
        if (filenameFromURL != null) {
            return Encoding.htmlDecode(filenameFromURL).trim();
        } else {
            return null;
        }
    }

    private String getFidFromURL(final String url) {
        return new Regex(url, PATTERN_FID).getMatch(1);
    }

    private String getContentURL(final DownloadLink link) {
        final String url = Encoding.htmlOnlyDecode(link.getPluginPatternMatcher());
        final Regex singleFileRegex = new Regex(url, BunkrAlbum.PATTERN_SINGLE_FILE);
        final String hostFromAddedURLWithoutSubdomain = Browser.getHost(url, false);
        if (singleFileRegex.patternFind()) {
            final List<String> deadDomains = BunkrAlbum.getDeadDomains();
            final String type = singleFileRegex.getMatch(1);
            final String filename = singleFileRegex.getMatch(2);
            final String host;
            if (deadDomains != null && deadDomains.contains(hostFromAddedURLWithoutSubdomain)) {
                /* We know given host is dead -> Use current main domain */
                host = getHost();
            } else {
                /* Use domain from given url */
                host = hostFromAddedURLWithoutSubdomain;
            }
            return "https://" + host + "/" + type + "/" + filename;
        } else {
            final List<String> deadDomains = getDeadCDNDomains();
            if (deadDomains != null && deadDomains.contains(hostFromAddedURLWithoutSubdomain)) {
                final String newurl = url.replaceFirst(Pattern.quote(hostFromAddedURLWithoutSubdomain) + "/", getHost() + "/");
                return newurl;
            }
            return url;
        }
    }

    private String generateSingleFileURL(final String filename) {
        return "https://" + getHost() + "/d/" + filename;
    }

    private String generateSingleImageURL(final String filename) {
        return "https://" + getHost() + "/i/" + filename;
    }

    private String generateSingleVideoURL(final String filename) {
        return "https://" + getHost() + "/v/" + filename;
    }

    private final String       PROPERTY_LAST_GRABBED_DIRECTURL    = "last_grabbed_directurl";
    private final String       PROPERTY_LAST_USED_SINGLE_FILE_URL = "last_used_single_file_url";
    public static final String PROPERTY_FILENAME_FROM_ALBUM       = "filename_from_album";
    public static final String PROPERTY_PARSED_FILESIZE           = "parsed_filesize";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        final String contenturl = this.getContentURL(link);
        final String filenameFromURL = getFilenameFromURL(link);
        if (filenameFromURL != null && !link.isNameSet()) {
            /* Set unsafe filename */
            setFilename(link, filenameFromURL, true, false);
        }
        final String lastCachedDirecturl = link.getStringProperty(PROPERTY_LAST_GRABBED_DIRECTURL);
        final String lastUsedSingleFileURL = link.getStringProperty(PROPERTY_LAST_USED_SINGLE_FILE_URL);
        Exception exceptionFromDirecturlCheck = null;
        if (lastCachedDirecturl != null && lastUsedSingleFileURL != null) {
            logger.info("Trying to re-use last cached directurl: " + lastCachedDirecturl);
            br.getHeaders().put("Referer", lastUsedSingleFileURL);
            URLConnectionAdapter con = null;
            try {
                if (isDownload) {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, lastCachedDirecturl, isResumeable(link, null), this.getMaxChunks(null));
                    con = dl.getConnection();
                } else {
                    con = br.openGetConnection(lastCachedDirecturl);
                }
                handleConnectionErrors(link, br, con);
                final String filenameFromHeader = Plugin.getFileNameFromDispositionHeader(con);
                final String filenameFromDirecturl = Plugin.getFileNameFromURL(new URL(lastCachedDirecturl));
                if (filenameFromHeader != null) {
                    setFilename(link, filenameFromHeader, true, true);
                } else if (filenameFromDirecturl != null) {
                    setFilename(link, filenameFromDirecturl, true, true);
                }
                logger.info("Successfully re-used last cached directurl");
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setVerifiedFileSize(-1);
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                return AvailableStatus.TRUE;
            } catch (final Exception e) {
                exceptionFromDirecturlCheck = e;
                logger.log(e);
                logger.info("Failed to re-use last cached directurl");
                try {
                    con.disconnect();
                } catch (final Throwable ignore) {
                }
            } finally {
                if (!isDownload) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        String directurl;
        if (new Regex(link.getPluginPatternMatcher(), BunkrAlbum.PATTERN_SINGLE_FILE).patternFind()) {
            directurl = getDirecturlFromSingleFileAvailablecheck(link, contenturl, true);
            if (!isDownload) {
                return AvailableStatus.TRUE;
            } else {
                this.sleep(1000, link);
            }
        } else {
            directurl = contenturl;
            /* Set referer for download */
            final String containerURL = link.getContainerUrl();
            if (containerURL != null) {
                br.getHeaders().put("Referer", containerURL);
            } else {
                br.getHeaders().put("Referer", "https://" + Browser.getHost(directurl, false) + "/");
            }
        }
        URLConnectionAdapter con = null;
        try {
            if (isDownload) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, isResumeable(link, null), this.getMaxChunks(null));
                con = dl.getConnection();
            } else {
                con = br.openGetConnection(directurl);
            }
            try {
                handleConnectionErrors(link, br, con);
            } catch (final PluginException firstException) {
                /* E.g. redirect from cdn8.bunkr.ru/... to bukrr.su/v/... resulting in new final URL media-files8.bunkr.ru/... */
                if (firstException.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    /* Dead end */
                    throw firstException;
                } else if (new Regex(br.getURL(), BunkrAlbum.PATTERN_SINGLE_FILE).patternFind()) {
                    /* Unknown URL format -> We most likely won't be able to refresh directurl from this format. */
                    logger.info("Directurl redirected to URL with unknown format -> We most likely won't be able to refresh directurl from this format. URL: " + br.getURL());
                    throw firstException;
                } else {
                    try {
                        final String singleFileURL = br.getURL();
                        try {
                            con.disconnect();
                        } catch (final Throwable ignore) {
                        }
                        if (br.getHttpConnection().getResponseCode() == 416) {
                            /* E.g. resume of download which does not work at this stage -> Access URL to get HTML code. */
                            br.getPage(singleFileURL);
                        }
                        final String freshDirecturl = getDirecturlFromSingleFileAvailablecheck(link, br.getURL(), false);
                        /* Avoid trying again with the same directurl if we already know the result. */
                        if (StringUtils.equals(freshDirecturl, lastCachedDirecturl) && exceptionFromDirecturlCheck != null) {
                            throw exceptionFromDirecturlCheck;
                        }
                        br.getHeaders().put("Referer", singleFileURL); // Important!
                        if (isDownload) {
                            dl = jd.plugins.BrowserAdapter.openDownload(br, link, freshDirecturl, isResumeable(link, null), this.getMaxChunks(null));
                            con = dl.getConnection();
                        } else {
                            con = br.openGetConnection(freshDirecturl);
                        }
                        handleConnectionErrors(link, br, con);
                    } catch (final Exception secondaryException) {
                        throw firstException;
                    }
                }
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            final String filenameFromHeader = Plugin.getFileNameFromConnection(con);
            if (!StringUtils.isEmpty(filenameFromHeader)) {
                setFilename(link, filenameFromHeader, true, true);
            }
        } catch (final Exception e) {
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
            throw e;
        } finally {
            if (!isDownload) {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getDirecturlFromSingleFileAvailablecheck(final DownloadLink link, final String singleFileURL, final boolean accessURL) throws PluginException, IOException {
        link.setProperty(PROPERTY_LAST_GRABBED_DIRECTURL, null);
        if (accessURL) {
            br.getPage(singleFileURL);
        }
        handleResponsecodeErrors(br.getHttpConnection());
        final String filenameFromURL = Plugin.getFileNameFromURL(br._getURL());
        String filenameFromHTML = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (filenameFromHTML != null) {
            filenameFromHTML = Encoding.htmlDecode(filenameFromHTML).trim();
            filenameFromHTML = filenameFromHTML.replaceAll("(?i)\\s*\\|\\s*Bunkr\\s*", "");
        }
        if (filenameFromHTML != null) {
            /* Unsafe name */
            setFilename(link, filenameFromHTML, false, false);
        }
        String filesize = br.getRegex("Download\\s*(\\d+[^<]+)</a>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"[^>]*text[^>]*\"[^>]*>\\s*([0-9\\.]+\\s+[MKG]B)").getMatch(0);
        }
        if (filesize != null) {
            final long parsedFilesize = SizeFormatter.getSize(filesize);
            link.setDownloadSize(parsedFilesize);
            link.setProperty(PROPERTY_PARSED_FILESIZE, parsedFilesize);
        }
        /* 2024-02-16: New: Additional step required */
        final String nextStepURL = br.getRegex("(https?://get\\.[^/]+/file/\\d+)").getMatch(0);
        if (nextStepURL != null) {
            br.getPage(nextStepURL);
        }
        String directurl = br.getRegex("(?i)href\\s*=\\s*\"(https?://[^\"]+)[^>]*>\\s*Download").getMatch(0);
        if (directurl == null) {
            /* Video stream (For "/v/ URLs."URL is usually the same as downloadurl.) */
            directurl = br.getRegex("<source src\\s*=\\s*\"(https?://[^\"]+)\"[^>]*type=.video/mp4").getMatch(0);
            if (directurl == null) {
                /* Video stream (URL is usually the same as downloadurl) */
                directurl = br.getRegex("<source src\\s*=\\s*\"(https?://[^\"]+)\"[^>]*type=.video/mp4").getMatch(0);
                if (directurl == null) {
                    String unsafeDirecturlResultForFileWithoutExtOrUnknownExt = null;
                    String unsafeDirecturlResult = null;
                    final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
                    for (final String url : urls) {
                        if (url.matches(BunkrAlbum.TYPE_MEDIA_FILES_WITH_EXT) || url.matches(BunkrAlbum.TYPE_CDN_WITH_EXT)) {
                            /* Safe result */
                            directurl = url;
                            break;
                        } else if (url.matches(BunkrAlbum.TYPE_MEDIA_FILES_WITHOUT_EXT) || new Regex(url, BunkrAlbum.PATTERN_CDN_WITHOUT_EXT).patternFind()) {
                            unsafeDirecturlResultForFileWithoutExtOrUnknownExt = url;
                        } else if (StringUtils.containsIgnoreCase(url, "download=true")) {
                            /* Image URLs: bunkr.bla/i/... */
                            directurl = url;
                            break;
                        } else if (!url.equals(br.getURL()) && (StringUtils.contains(url, filenameFromURL) || StringUtils.contains(url, filenameFromHTML))) {
                            unsafeDirecturlResult = url;
                        }
                    }
                    if (directurl == null && unsafeDirecturlResult != null) {
                        logger.info("Using unsafeDirecturlResult as directurl");
                        directurl = unsafeDirecturlResult;
                    } else if (directurl == null && unsafeDirecturlResultForFileWithoutExtOrUnknownExt != null) {
                        /* File without extension or extension we don't know. */
                        logger.info("Using unsafeDirecturlResultForFileWithoutExtOrUnknownExt as directurl");
                        directurl = unsafeDirecturlResultForFileWithoutExtOrUnknownExt;
                    }
                }
                /* Last chance */
                if (directurl == null) {
                    directurl = br.getRegex("class=\"text-white hover:[^\"]*justify-center rounded[^\"]*\" href=\"(https?://[^\"]+)\">").getMatch(0);
                    if (directurl == null && filenameFromURL != null) {
                        /* 2023-10-06 e.g. burger.bunkr.ru/... or pizza.bunkr.ru/... */
                        directurl = br.getRegex("(https?://[a-z0-9\\-]+\\.[^/]+/" + Pattern.quote(filenameFromURL) + ")").getMatch(0);
                    }
                    if (directurl == null && filenameFromHTML != null) {
                        /* 2023-10-06 e.g. burger.bunkr.ru/... or pizza.bunkr.ru/... */
                        directurl = br.getRegex("(https?://[a-z0-9\\-]+\\.[^/]+/" + Pattern.quote(filenameFromHTML) + ")").getMatch(0);
                    }
                }
            }
        }
        if (directurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        directurl = Encoding.htmlOnlyDecode(directurl);
        final String filename = getFilenameFromURL(directurl);
        if (filename != null) {
            setFilename(link, filename, true, true);
        } else if (!link.isNameSet()) {
            /* Unsafe name */
            setFilename(link, filename, true, false);
        }
        link.setProperty(PROPERTY_LAST_GRABBED_DIRECTURL, directurl);
        link.setProperty(PROPERTY_LAST_USED_SINGLE_FILE_URL, singleFileURL);
        return directurl;
    }

    public static void setFilename(final DownloadLink link, final String filename, final boolean canContainFileID, final boolean setFinalName) {
        if (StringUtils.isEmpty(filename)) {
            return;
        }
        final boolean fixFilename = PluginJsonConfig.get(BunkrConfig.class).isFixFilename();
        final String filenameFromAlbum = link.getStringProperty(PROPERTY_FILENAME_FROM_ALBUM);
        String correctedFilename;
        if (fixFilename && canContainFileID) {
            final String fileID = new Regex(filename, PATTERN_FID).getMatch(0);
            if (fileID != null) {
                correctedFilename = filename.replaceFirst(fileID, "");
            } else {
                /* No fileID in link while it should be given */
                correctedFilename = filename;
            }
            final String filenameWithMinusReplacement = correctedFilename.replace("-", " ");
            if (filenameFromAlbum != null) {
                /* We know they replace spaces with minus but if the original filename already contained minus that would be a mistake. */
                if (!correctedFilename.equals(filenameFromAlbum)) {
                    correctedFilename = filenameWithMinusReplacement;
                }
                if (correctedFilename.equals(filenameFromAlbum) && !filenameFromAlbum.contains("-")) {
                    /*
                     * We were able to re-create the desired filename without the need of the one we stored -> Remove that property to save
                     * memory.
                     */
                    link.removeProperty(PROPERTY_FILENAME_FROM_ALBUM);
                }
                correctedFilename = filenameFromAlbum;
            } else {
                /* No reference "original filename" given so let's do the default replacements. */
                correctedFilename = filenameWithMinusReplacement;
            }
        } else {
            /* Do not touch given filename */
            correctedFilename = filename;
        }
        if (setFinalName) {
            link.setFinalFileName(correctedFilename);
        } else {
            link.setName(correctedFilename);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (this.dl == null) {
            /* Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Add a download slot */
        controlMaxFreeDownloads(null, link, +1);
        try {
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlMaxFreeDownloads(null, link, -1);
        }
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

    private void handleConnectionErrors(final DownloadLink link, final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        final long parsedExpectedFilesize = link.getLongProperty(PROPERTY_PARSED_FILESIZE, -1);
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            handleResponsecodeErrors(con);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken or temporarily unavailable", 2 * 60 * 60 * 1000l);
        } else if (con.getURL().getPath().equalsIgnoreCase("/maintenance-vid.mp4") || con.getURL().getPath().equalsIgnoreCase("/v/maintenance-kek-bunkr.webm") || con.getURL().getPath().equalsIgnoreCase("/maintenance.mp4")) {
            con.disconnect();
            /* https://bnkr.b-cdn.net/maintenance-vid.mp4 */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Media temporarily not available due to ongoing server maintenance.", 2 * 60 * 60 * 1000l);
        } else if (parsedExpectedFilesize > 0 && con.getCompleteContentLength() > 0 && con.getCompleteContentLength() < (parsedExpectedFilesize * 0.5)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is too small: File under maintenance?", 1 * 60 * 60 * 1000l);
        }
    }

    private void handleResponsecodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.getResponseCode() == 416) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 416", 30 * 1000l);
        } else if (con.getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 429 too many requests", 30 * 1000l);
        } else if (con.getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 503 too many connections", 60 * 1000l);
        }
    }

    @Override
    public Class<? extends BunkrConfig> getConfigInterface() {
        return BunkrConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
