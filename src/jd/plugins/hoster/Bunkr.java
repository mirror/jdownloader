package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;

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
    // @Override
    // public PluginForHost assignPlugin(PluginFinder pluginFinder, DownloadLink link) {
    // final String pluginHost = getHost();
    // if (pluginFinder != null && CyberdropMe.class.equals(getClass()) && !pluginHost.equals(link.getHost())) {
    // final String url = link.getPluginPatternMatcher();
    // final boolean checkHostFlag;
    // if (CyberdropMeAlbum.MAIN_CYBERDROP_DOMAIN.equals(pluginHost) && url.matches(CyberdropMeAlbum.TYPE_FS)) {
    // checkHostFlag = true;
    // } else if (CyberdropMeAlbum.MAIN_BUNKR_DOMAIN.equals(pluginHost) && (url.matches(CyberdropMeAlbum.TYPE_CDN) ||
    // url.matches(CyberdropMeAlbum.TYPE_STREAM))) {
    // checkHostFlag = true;
    // } else {
    // checkHostFlag = false;
    // return null;
    // }
    // if (checkHostFlag) {
    // final String host = Browser.getHost(url);
    // for (String siteSupportedName : siteSupportedNames()) {
    // if (StringUtils.equalsIgnoreCase(siteSupportedName, host)) {
    // return super.assignPlugin(pluginFinder, link);
    // }
    // }
    // }
    // return null;
    // } else {
    // return super.assignPlugin(pluginFinder, link);
    // }
    // }

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
        String filenameFromURL = new Regex(link.getPluginPatternMatcher(), BunkrAlbum.TYPE_SINGLE_FILE).getMatch(0);
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
        String newurl = url;
        /* Remove/correct old/invalid subdomains */
        newurl = newurl.replaceFirst("stream\\.", "");
        /* Replace domain in URL if we know that it is dead. */
        final List<String> deadDomains;
        /* Desired domain correction depends on URL-pattern. */
        final boolean replaceSubdomain;
        if (newurl.matches(BunkrAlbum.TYPE_SINGLE_FILE)) {
            deadDomains = BunkrAlbum.getDeadDomains();
            replaceSubdomain = true;
        } else {
            deadDomains = getDeadCDNDomains();
            replaceSubdomain = false;
        }
        final String hostFromAddedURL = Browser.getHost(newurl, false);
        final String hostFromAddedURLToReplace = Browser.getHost(newurl, replaceSubdomain);
        if (deadDomains != null && deadDomains.size() > 0) {
            for (final String deadHost : deadDomains) {
                if (StringUtils.equalsIgnoreCase(hostFromAddedURL, deadHost) || StringUtils.equalsIgnoreCase(hostFromAddedURL, "www." + deadHost)) {
                    final String newHost = getHost();
                    newurl = newurl.replaceFirst(Pattern.quote(hostFromAddedURLToReplace) + "/", newHost + "/");
                    // logger.info("Corrected domain in added URL: " + hostFromAddedURL + " --> " + newHost);
                    break;
                }
            }
        }
        return newurl;
    }

    private final String PROPERTY_ALTERNATIVE_DIRECTURL = "alternative_directurl";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public static String findDirectURL(final Plugin plugin, final Browser br) {
        String directurl = br.getRegex("link\\.href\\s*=\\s*\"(https?://[^\"]+)\"").getMatch(0);
        if (directurl == null) {
            directurl = br.getRegex("(?i)href\\s*=\\s*\"(https?://[^\"]+)[^>]*>\\s*Download").getMatch(0);
            if (directurl == null) {
                /* Video stream (URL is usually the same as downloadurl) */
                directurl = br.getRegex("<source src\\s*=\\s*\"(https?://[^\"]+)\"[^>]*type=.video/mp4").getMatch(0);
            }
        }
        return directurl;
    }

    public static String findFilesize(final Plugin plugin, final Browser br) {
        String filesize = br.getRegex("Download (\\d+[^<]+)</a>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"[^>]*text[^>]*\"[^>]*>\\s*([0-9\\.]+\\s+[MKG]B)").getMatch(0);
        }
        return filesize;
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final boolean attemptToUseLastStoredAlternativeDirecturl = false;
        String directurl = link.getStringProperty(PROPERTY_ALTERNATIVE_DIRECTURL);
        if (directurl == null || !attemptToUseLastStoredAlternativeDirecturl) {
            directurl = this.getContentURL(link);
        }
        final String filenameFromURL = Plugin.getFileNameFromURL(directurl);
        if (!link.isNameSet() && filenameFromURL != null) {
            link.setName(filenameFromURL);
        }
        // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)
        // Chrome/115.0.0.0 Safari/537.36");
        // br.getHeaders().put("User-Agent", UserAgents.stringUserAgent(null));
        final String containerURL = link.getContainerUrl();
        if (containerURL != null) {
            br.getHeaders().put("Referer", containerURL);
        }
        URLConnectionAdapter con = null;
        try {
            if (isDownload) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, true, this.getMaxChunks(null));
                con = dl.getConnection();
            } else {
                con = br.openGetConnection(directurl);
            }
            try {
                handleConnectionErrors(br, con);
            } catch (final PluginException e) {
                /* E.g. cdn.bunkr.ru -> bunkr.su/v/... -> Try to find fresh directurl */
                logger.info("Directurl did not lead to downloadable content -> Looking for freh directurl");
                final String alternativeFreshDirecturl = findDirectURL(this, br);
                // final String filesize = Bunkr.findFilesize(this, br);
                if (alternativeFreshDirecturl == null) {
                    logger.info("Failed to find fresh directurl");
                    throw e;
                } else if (StringUtils.equals(directurl, alternativeFreshDirecturl)) {
                    logger.info("Fresh directurl is the same as old one -> Retrying doesn't make any sense");
                    throw e;
                } else {
                    logger.info("Trying again with fresh directurl: " + alternativeFreshDirecturl);
                    if (isDownload) {
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, alternativeFreshDirecturl, true, this.getMaxChunks(null));
                        con = dl.getConnection();
                    } else {
                        con = br.openGetConnection(alternativeFreshDirecturl);
                    }
                    handleConnectionErrors(br, con);
                    logger.info("Fresh directurl is working: " + alternativeFreshDirecturl);
                    link.setProperty(PROPERTY_ALTERNATIVE_DIRECTURL, alternativeFreshDirecturl);
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
