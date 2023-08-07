package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

import jd.PluginWrapper;
import jd.controlling.linkcrawler.CheckableLink;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.CyberdropMeAlbum;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { CyberdropMeAlbum.class })
public class CyberdropMe extends PluginForHost {
    public CyberdropMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://bunkrr.su/faq";
    }

    public static List<String[]> getPluginDomains() {
        return CyberdropMeAlbum.getPluginDomains();
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

    private int getMaxChunks(final Account account) {
        if (CyberdropMeAlbum.MAIN_BUNKR_DOMAIN.equals(getHost())) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    protected int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (CyberdropMeAlbum.MAIN_BUNKR_DOMAIN.equals(getHost())) {
            return 1;
        } else {
            return super.getMaxSimultanDownload(link, account);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (CyberdropMeAlbum.MAIN_BUNKR_DOMAIN.equals(getHost())) {
            return 1;
        } else {
            return super.getMaxSimultanFreeDownloadNum();
        }
    }

    @Override
    public PluginForHost assignPlugin(PluginFinder pluginFinder, DownloadLink link) {
        final String pluginHost = getHost();
        if (pluginFinder != null && CyberdropMe.class.equals(getClass()) && !pluginHost.equals(link.getHost())) {
            final String url = link.getPluginPatternMatcher();
            final boolean checkHostFlag;
            if (CyberdropMeAlbum.MAIN_CYBERDROP_DOMAIN.equals(pluginHost) && url.matches(CyberdropMeAlbum.TYPE_FS)) {
                checkHostFlag = true;
            } else if (CyberdropMeAlbum.MAIN_BUNKR_DOMAIN.equals(pluginHost) && (url.matches(CyberdropMeAlbum.TYPE_CDN) || url.matches(CyberdropMeAlbum.TYPE_STREAM))) {
                checkHostFlag = true;
            } else {
                checkHostFlag = false;
                return null;
            }
            if (checkHostFlag) {
                final String host = Browser.getHost(url);
                for (String siteSupportedName : siteSupportedNames()) {
                    if (StringUtils.equalsIgnoreCase(siteSupportedName, host)) {
                        return super.assignPlugin(pluginFinder, link);
                    }
                }
            }
            return null;
        } else {
            return super.assignPlugin(pluginFinder, link);
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (CyberdropMeAlbum.MAIN_BUNKR_DOMAIN.equals(getHost()) || CyberdropMeAlbum.MAIN_CYBERDROP_DOMAIN.equals(getHost())) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.ASSIGN_PLUGIN };
        } else {
            return new LazyPlugin.FEATURE[0];
        }
    }

    @Override
    protected boolean supportsUpdateDownloadLink(CheckableLink checkableLink) {
        return false;
    }

    private String getContentURL(final DownloadLink link) {
        if (false && CyberdropMeAlbum.MAIN_CYBERDROP_DOMAIN.equals(getHost())) {
            final String url = link.getPluginPatternMatcher();
            /* 2022-11-10: fs-(03|04|05|06) are offline, rewrite to fs-01, fs-02 redirects to fs-01 */
            /* 2023-03-24: looks like fs-(03|04|05|06) are working again */
            final String newUrl = url.replaceFirst("://fs-(03|04|05|06)", "://fs-01");
            return newUrl;
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    private String getDirecturl(final DownloadLink link) {
        final String directurl = link.getStringProperty(PROPERTY_ALTERNATIVE_DIRECTURL);
        if (directurl != null) {
            return directurl;
        } else {
            return this.getContentURL(link);
        }
    }

    @Override
    public String getHost(DownloadLink link, Account account, boolean includeSubdomain) {
        return getHost();
    }

    private final String PROPERTY_ALTERNATIVE_DIRECTURL = "alternative_directurl";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String directurl = getDirecturl(link);
        final String filenameFromURL = Plugin.getFileNameFromURL(directurl);
        if (!link.isNameSet() && filenameFromURL != null) {
            link.setName(filenameFromURL);
        }
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
                final String alternativeFreshDirecturl = CyberdropMeAlbum.findDirectURL(br);
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
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (con.getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 503 too many connections", 1 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken?");
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
