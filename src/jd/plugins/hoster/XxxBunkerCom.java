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
import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XxxBunkerCom extends PluginForHost {
    @SuppressWarnings("deprecation")
    public XxxBunkerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.XxxBunkerCom.getPluginDomains();
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
            /* TODO: Add support for embed URLs */
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z0-9_\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://xxxbunker.com/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = findFileTitle(br);
        if (title == null) {
            /* Final fallback */
            title = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        }
        String externID_extern = br.getRegex("postbackurl(?:=|%3D)([^<>\"\\&]*?)%26amp%3B").getMatch(0);
        String externID = br.getRegex("player\\.swf\\?config=(https?%3A%2F%2Fxxxbunker\\.com%2FplayerConfig\\.php%3F[^<>\"]*?)\"").getMatch(0);
        final String externID3 = br.getRegex("lvid=(\\d+)").getMatch(0);
        if (externID_extern == null && externID == null && externID3 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // leads to incorrect shit.
        if (false && externID_extern != null) {
            /* E.g. http://xxxbunker.com/3568499 pornhub direct */
            externID_extern = Encoding.htmlDecode(externID_extern);
            externID_extern = Encoding.htmlDecode(externID_extern);
            dllink = Encoding.Base64Decode(externID_extern);
            // this.sleep(3000, downloadLink);
        } else if (externID != null) {
            br.getPage(Encoding.htmlDecode(externID));
            dllink = br.getRegex("<relayurl>([^<>\"]*?)</relayurl>").getMatch(0);
            externID = br.getRegex("<file>(http[^<>\"]*?)</file>").getMatch(0);
            if (dllink == null) {
                dllink = externID;
            }
        } else {
            // html5!
            br.getPage("/html5player.php?videoid=" + externID3 + "&autoplay=false&index=false");
            dllink = br.getRegex("<source src=(\"|')(http[^<>\"]*?)\\1").getMatch(1);
        }
        if (dllink != null) {
            dllink = Encoding.htmlOnlyDecode(dllink);
        }
        title = Encoding.htmlDecode(title).trim();
        title = title.trim();
        String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (ext.equals(".php")) {
            ext = ".mp4";
        }
        link.setFinalFileName(title + ext);
        br.getHeaders().put("Accept-Encoding", "identity");
        return AvailableStatus.TRUE;
    }

    public static String findFileTitle(final Browser br) {
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            title = br.getRegex("class=vpVideoTitle><h1 itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        }
        return title;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Referer", "");
        br.setCookie(br.getURL(), "ageconfirm", "20150302");
        br.setCookie(br.getURL(), "autostart", "1");
        link.setProperty("ServerComaptibleForByteRangeRequest", true);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
