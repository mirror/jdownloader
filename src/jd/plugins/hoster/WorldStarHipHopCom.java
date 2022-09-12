//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.WorldStarHipHopComDecrypter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { WorldStarHipHopComDecrypter.class })
public class WorldStarHipHopCom extends PluginForHost {
    private String dllink = null;

    public WorldStarHipHopCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return WorldStarHipHopComDecrypter.getPluginDomains();
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
            /* Dummy pattern as crawler-plugin decides which links are handled by this hosterplugin. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://worldstarhiphop.com/videos/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return requestFileInformation(link, br);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Browser br) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        if (br.containsHTML("(?i)<title>\\s*Video: No Video\\s*</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">This video isn\\'t here right now\\.\\.\\.")) {
            link.setName(new Regex(link.getDownloadURL(), "([a-zA-Z0-9]+)$").getMatch(0));
            link.getLinkStatus().setStatusText("Video temporarily unavailable");
            return AvailableStatus.TRUE;
        }
        filename = br.getRegex("\"content-heading\">\\s*<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)( | Video )?</title>").getMatch(0);
        }
        if (br.containsHTML("videoplayer\\.vevo\\.com/embed/embedded\"")) {
            filename = Encoding.htmlDecode(filename).trim();
            link.getLinkStatus().setStatusText("This video is blocked in your country");
            link.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        }
        dllink = br.getRegex("v=playFLV\\.php\\?loc=(https?://.*?\\.(mp4|flv))\\&amp;").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(https?://hwcdn\\.net/[a-z0-9]+/cds/\\d+/\\d+/\\d+/.*?\\.(mp4|flv))").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("v=(https?://.*?\\.com/.*?/vid/.*?\\.(mp4|flv))").getMatch(0);
                if (dllink == null) {
                    dllink = getURLUniversal(br);
                }
            }
        }
        if (!StringUtils.isEmpty(filename)) {
            filename = Encoding.htmlDecode(filename).trim();
            filename = encodeUnicode(filename);
            link.setFinalFileName(filename + ".mp4");
        }
        if (!StringUtils.isEmpty(dllink)) {
            br.getHeaders().put("Referer", "http://hw-static.worldstarhiphop.com/videos/wplayer/NAPP3e.swf");
            dllink = dllink.replace(" ", "");
            Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static final boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML("videoplayer\\.vevo\\.com/embed/embedded\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is blocked in your country");
        }
        if (br.getURL().contains("worldstarhiphop.com/")) {
            if (br.containsHTML(">This video isn\\'t here right now\\.\\.\\.")) {
                link.getLinkStatus().setStatusText("Video temporarily unavailable");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video temporarily unavailable", 30 * 60 * 1000l);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getURLUniversal(final Browser br) {
        String directurl = br.getRegex("addVariable\\(\"file\",\"(https?://.*?)\"").getMatch(0);
        if (directurl == null) {
            directurl = br.getRegex("\"(https?://hw\\-videos\\.worldstarhiphop\\.com/u/vid/.*?)\"").getMatch(0);
        }
        return directurl;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}