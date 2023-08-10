package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { BunkrAlbum.class })
public class Bunkr extends PluginForHost {
    public Bunkr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        br.setFollowRedirects(true);
        return br;
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
        String filenameFromURL = new Regex(link.getPluginPatternMatcher(), BunkrAlbum.TYPE_SINGLE_FILE).getMatch(2);
        if (filenameFromURL == null) {
            filenameFromURL = new Regex(link.getPluginPatternMatcher(), "(?i)https?://[^/]+/(.+)").getMatch(0);
        }
        if (filenameFromURL != null) {
            return Encoding.htmlDecode(filenameFromURL).trim();
        } else {
            return null;
        }
    }

    private String getContentURL(final DownloadLink link) {
        final String url = link.getPluginPatternMatcher();
        final Regex singleFileRegex = new Regex(url, BunkrAlbum.TYPE_SINGLE_FILE);
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

    private final String PROPERTY_LAST_GRABBED_DIRECTURL    = "last_grabbed_directurl";
    private final String PROPERTY_LAST_USED_SINGLE_FILE_URL = "last_used_single_file_url";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        final String lastCachedDirecturl = link.getStringProperty(PROPERTY_LAST_GRABBED_DIRECTURL);
        final String lastUsedSingleFileURL = link.getStringProperty(PROPERTY_LAST_USED_SINGLE_FILE_URL);
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
                handleConnectionErrors(br, con);
                logger.info("Successfully re-used last cached directurl");
                return AvailableStatus.TRUE;
            } catch (final Exception e) {
                logger.log(e);
                logger.info("Failed to re-use last cached directurl");
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
        if (link.getPluginPatternMatcher().matches(BunkrAlbum.TYPE_SINGLE_FILE)) {
            directurl = getDirecturlFromSingleFileAvailablecheck(link, this.getContentURL(link), true);
            if (!isDownload) {
                return AvailableStatus.TRUE;
            } else {
                this.sleep(1000, link);
            }
        } else {
            directurl = this.getContentURL(link);
            final String filenameFromURL = Plugin.getFileNameFromURL(directurl);
            if (!link.isNameSet() && filenameFromURL != null) {
                link.setName(filenameFromURL);
            }
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
                handleConnectionErrors(br, con);
            } catch (final PluginException e) {
                /* E.g. redirect from cdn8.bunkr.ru/... to bukrr.su/v/... resulting in new final URL media-files8.bunkr.ru/... */
                if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    /* Dead end */
                    throw e;
                } else if (!br.getURL().matches(BunkrAlbum.TYPE_SINGLE_FILE)) {
                    /* Unknown URL format -> We most likely won't be able to refresh directurl from this format. */
                    logger.info("Directurl redirected to URL with unknown format -> We most likely won't be able to refresh directurl from this format. URL: " + br.getURL());
                    throw e;
                } else {
                    final String singleFileURL = br.getURL();
                    link.setProperty(PROPERTY_LAST_USED_SINGLE_FILE_URL, singleFileURL);
                    if (br.getHttpConnection().getResponseCode() == 416) {
                        /* E.g. resume of download. */
                        br.getPage(singleFileURL);
                    }
                    final String freshDirecturl = getDirecturlFromSingleFileAvailablecheck(link, br.getURL(), false);
                    br.getHeaders().put("Referer", singleFileURL); // Important!
                    if (isDownload) {
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, freshDirecturl, isResumeable(link, null), this.getMaxChunks(null));
                        con = dl.getConnection();
                    } else {
                        con = br.openGetConnection(freshDirecturl);
                    }
                    handleConnectionErrors(br, con);
                }
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            final String filenameFromHeader = Plugin.getFileNameFromHeader(con);
            if (!StringUtils.isEmpty(filenameFromHeader)) {
                link.setFinalFileName(filenameFromHeader);
            }
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
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String directurl = br.getRegex("(?i)href\\s*=\\s*\"(https?://[^\"]+)[^>]*>\\s*Download").getMatch(0);
        if (directurl == null) {
            /* Video stream (URL is usually the same as downloadurl) */
            directurl = br.getRegex("<source src\\s*=\\s*\"(https?://[^\"]+)\"[^>]*type=.video/mp4").getMatch(0);
        }
        String filesize = br.getRegex("Download (\\d+[^<]+)</a>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"[^>]*text[^>]*\"[^>]*>\\s*([0-9\\.]+\\s+[MKG]B)").getMatch(0);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (directurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String filename = Plugin.getFileNameFromURL(directurl);
        if (filename != null) {
            link.setName(filename);
        }
        link.setProperty(PROPERTY_LAST_GRABBED_DIRECTURL, directurl);
        return directurl;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (this.dl == null) {
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

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
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
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 503 too many connections", 30 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken or temporarily unavailable");
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
