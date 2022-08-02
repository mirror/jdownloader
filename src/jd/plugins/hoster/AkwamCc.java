package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.decrypter.AkwamCc.class })
public class AkwamCc extends antiDDoSForHost {
    public AkwamCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://akwam.to/";
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.AkwamCc.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/download/[0-9]+/[0-9]+/?.*");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2022-08-02: Main domain has changed from one.akwam.cc to akwam.to */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    public DownloadLink.AvailableStatus requestFileInformation(final DownloadLink link) throws Exception, PluginException {
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        final String dllink = br.getRegex("\"([^\"]+)\" download").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    link.setFinalFileName(Plugin.getFileNameFromHeader(con));
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return DownloadLink.AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        dl = BrowserAdapter.openDownload(br, link, br.getURL(), true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}