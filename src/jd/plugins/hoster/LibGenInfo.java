//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class LibGenInfo extends PluginForHost {
    public LibGenInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "libgen.pw".equals(host) || "libgen.me".equals(host) || "libgen.info".equals(host)) {
            return "libgen.lc";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://libgen.lc/";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        /* Keep in sync with hoster- and crawler plugin! */
        ret.add(new String[] { "libgen.lc", "libgen.gs", "libgen.li", "libgen.org", "gen.lib.rus.ec", "libgen.io", "booksdl.org" });
        return ret;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/ads\\.php\\?md5=([A-Fa-f0-9]{32})");
        }
        return ret.toArray(new String[0]);
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
        String fid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (fid != null) {
            return fid.toUpperCase(Locale.ENGLISH);
        } else {
            return null;
        }
    }

    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 2;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        final String md5 = new Regex(link.getPluginPatternMatcher(), "md5=([a-f0-9]{32})").getMatch(0);
        link.setMD5Hash(md5);
        br.getPage("http://" + this.getHost() + "/item/index.php?md5=" + md5);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        parseFileInfo(link, this.br);
        return AvailableStatus.TRUE;
    }

    public static void parseFileInfo(final DownloadLink link, final Browser br) throws MalformedURLException {
        final String md5 = UrlQuery.parse(link.getPluginPatternMatcher()).get("md5");
        String filesizeStr = null;
        filesizeStr = br.getRegex("\\((\\d+) B\\)").getMatch(0);
        String filename = br.getRegex("ed2k://\\|file\\|([^\\|]+)\\|\\d+\\|").getMatch(0);
        if (filename != null) {
            link.setName(filename);
        } else if (md5 != null && !link.isNameSet()) {
            /* Fallback */
            link.setName(md5);
        }
        if (filesizeStr != null) {
            final long filesize = Long.parseLong(filesizeStr);
            link.setDownloadSize(filesize);
            link.setVerifiedFileSize(filesize);
        }
    }

    private String getBracketResult(final String key) {
        final String result = br.getRegex(key + "\\s*=\\s*\\{(.*?)\\},?[\r\n]+").getMatch(0);
        return result;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        br.getPage("/ads.php?md5=" + this.getFID(link));
        final String dllink = br.getRegex("<a\\s*href\\s*=\\s*(\"|')((?:https?:)?(?://[\\w\\-\\./]+)?/get\\.php\\?md5=[a-f0-9]{32}.*?)\\1").getMatch(1);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML(">Sorry, huge and large files are available to download in local network only, try later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            } else if (br.containsHTML("too many or too often downloads\\.\\.\\.")) {
                final String wait = br.getRegex("wait for (\\d+)hrs automatic amnesty").getMatch(0);
                if (wait != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many downloads", Integer.parseInt(wait) * 60 * 60 * 1001l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many downloads", 1 * 60 * 60 * 1001l);
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to a file");
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}