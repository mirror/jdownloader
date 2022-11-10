package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.linkcrawler.CheckableLink;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.CyberdropMeAlbum;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { CyberdropMeAlbum.class })
public class CyberdropMe extends DirectHTTP {
    public CyberdropMe(PluginWrapper wrapper) {
        super(wrapper);
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

    @Override
    protected int getMaxChunks(DownloadLink downloadLink, Set<String> optionSet, int chunks) {
        if ("bunkr.is".equals(getHost())) {
            return 1;
        } else {
            return chunks;
        }
    }

    @Override
    protected int getMaxSimultanDownload(DownloadLink link, Account account) {
        if ("bunkr.is".equals(getHost())) {
            return 1;
        } else {
            return super.getMaxSimultanDownload(link, account);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if ("bunkr.is".equals(getHost())) {
            return 1;
        } else {
            return super.getMaxSimultanFreeDownloadNum();
        }
    }

    @Override
    public PluginForHost assignPlugin(PluginFinder pluginFinder, DownloadLink link) {
        if (pluginFinder != null && CyberdropMe.class.equals(getClass()) && !getHost().equals(link.getHost())) {
            if (false && link.getPluginPatternMatcher().matches(CyberdropMeAlbum.TYPE_CDN) || link.getPluginPatternMatcher().matches(CyberdropMeAlbum.TYPE_STREAM)) {
                return super.assignPlugin(pluginFinder, link);
            } else {
                return null;
            }
        } else {
            return super.assignPlugin(pluginFinder, link);
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if ("bunkr.is".equals(getHost()) || "cyberdrop.me".equals(getHost())) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.ASSIGN_PLUGIN };
        } else {
            return new LazyPlugin.FEATURE[0];
        }
    }

    @Override
    protected boolean supportsUpdateDownloadLink(CheckableLink checkableLink) {
        return false;
    }

    @Override
    protected String getDownloadURL(DownloadLink downloadLink) throws IOException {
        if (!hasCustomDownloadURL() && "cyberdrop.me".equals(getHost())) {
            final String url = downloadLink.getPluginPatternMatcher();
            /* 2022-11-10: fs-(03|04|05|06) are offline, rewrite to fs-01, fs-02 redirects to fs-01 */
            final String newUrl = url.replaceFirst("://fs-(03|04|05|06)", "://fs-01");
            return newUrl;
        } else {
            return super.getDownloadURL(downloadLink);
        }
    }

    @Override
    public String getHost(DownloadLink link, Account account, boolean includeSubdomain) {
        return getHost();
    }
}
