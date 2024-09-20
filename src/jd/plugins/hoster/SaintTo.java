package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SaintTo extends PluginForHost {
    public SaintTo(PluginWrapper wrapper) {
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
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/faq";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "saint.to", "saint2.su" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2024-09-16: Main domain has changed from saint.to to saint2.su */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    private static final Pattern TYPE_EMBED      = Pattern.compile("/embed/([A-Za-z0-9\\-_]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_API_DIRECT = Pattern.compile("/api/download\\.php\\?file=([a-zA-Z0-9_/\\+\\=\\-%]+)/?", Pattern.CASE_INSENSITIVE);

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "(" + TYPE_EMBED.pattern() + "|" + TYPE_API_DIRECT.pattern() + ")");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
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
        String fid = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), TYPE_API_DIRECT).getMatch(0);
        }
        return fid;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        if (new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).patternFind()) {
            if (!link.isNameSet()) {
                /* Fallback */
                link.setName(this.getFID(link) + ".mp4");
            }
            this.setBrowserExclusive();
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\"(Video not found|Video has been removed)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("<title>\\s*([^<]+)\\s*</title>").getMatch(0);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
                link.setName(filename);
            }
        } else {
            /* TYPE_API_DIRECT */
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_API_DIRECT);
            final String filenameBase64 = urlinfo.getMatch(0);
            final String filename = Encoding.htmlDecode(filenameBase64);
            link.setName(filename);
            if (!isDownload) {
                this.basicLinkCheck(br, br.createHeadRequest(link.getPluginPatternMatcher()), link, filename, ".mp4");
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final String directlinkproperty) throws Exception, PluginException {
        final String storedDirecturl = directlinkproperty;
        final String dllink;
        final boolean storeDirecturl;
        if (new Regex(link.getPluginPatternMatcher(), TYPE_API_DIRECT).patternFind()) {
            dllink = link.getPluginPatternMatcher();
            storeDirecturl = false;
        } else if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
            storeDirecturl = false;
        } else {
            /* TYPE_EMBED */
            requestFileInformation(link, true);
            dllink = br.getRegex("<source src\\s*=\\s*\"(https?://[^\"]+)\" type=\"video/mp4\">").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find final downloadurl");
            }
            storeDirecturl = true;
        }
        /* Important otherwise we can't re-use direct urls (hotlinking-block)! */
        br.getHeaders().put("Referer", link.getPluginPatternMatcher());
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
            handleConnectionErrors(br, dl.getConnection());
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        if (storeDirecturl) {
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        }
        dl.startDownload();
    }

    @Override
    protected void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throwConnectionExceptions(br, con);
            throwFinalConnectionException(br, con);
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}