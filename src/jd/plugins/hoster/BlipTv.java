//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 * @author typek_pb
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blip.tv" }, urls = { "http://(\\w+\\.)?blip\\.tv/(file/(\\d+(/)?|get/.+)|[\\p{L}\\w-%]+/[\\p{L}\\w-%]+)" }, flags = { 0 })
public class BlipTv extends PluginForHost {

    private String         DLLINK  = null;
    private final String[] QUALITY = { "Source", "Web", "Blip SD", "Blip HD 720", "Blip HD 1080" };

    public BlipTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String fixName(String link) {
        link = link.replaceAll("(&#\\d+)", "$1;");
        link = HTMLEntities.unhtmlentities(link);
        return link;
    }

    @Override
    public String getAGBLink() {
        return "http://blip.tv/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        final String dllink = link.getDownloadURL();
        try {
            br.getPage(dllink);
        } catch (final BrowserException eb) {
            final long response = br.getRequest().getHttpConnection().getResponseCode();
            if (response == 404 || response == 410) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw eb;
        }
        if (!new Regex(dllink, "http://(.*?)/file/get/(.*?)\\.\\w{3}$").matches()) {
            /* 0.95xx comp */
            if (br.getRedirectLocation() != null) {
                // deutsche Umlaute fuehren in der 0.95xx zu einem redirect
                // loop!
                br.getPage(br.getRedirectLocation().replaceAll("%83%C2", ""));
            }
            String id = br.getRegex("data-posts-id=\"(\\d+)").getMatch(0);
            if (id == null) {
                id = br.getRegex("\tdata-episode=\"(\\d+)").getMatch(0);
            }
            if (id == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("<title>(.*?)\\s\\|").getMatch(0);
            br.getPage("http://blip.tv/rss/flash/" + id);
            if (filename == null) {
                filename = br.getRegex("<item>.*?<title>(.*?)</title>").getMatch(0);
            }

            // Wähle immer die höchste Qualität
            String fnQ = "";
            final String[][] dllinktmp = br.getRegex("<media:content url=\"(.*?)\"\\sblip:role=\"(.*?)\"").getMatches();
            for (int i = 0; i < QUALITY.length; i++) {
                for (final String[] e : dllinktmp) {
                    if (e.length != 2) {
                        continue;
                    }
                    if (!e[1].equalsIgnoreCase(QUALITY[i])) {
                        continue;
                    }
                    switch (i) {
                    case 0:
                        DLLINK = e[0];
                        break;
                    case 1:
                        DLLINK = e[0];
                        fnQ = "(Web_quality)";
                        break;
                    case 2:
                        DLLINK = e[0];
                        fnQ = "(SD_quality)";
                        break;
                    case 3:
                        DLLINK = e[0];
                        fnQ = "(HD_720_quality)";
                        break;
                    case 4:
                        DLLINK = e[0];
                        fnQ = "(HD_1080_quality)";
                        break;
                    default:
                        continue;
                    }
                }
            }
            if (DLLINK == null) {
                DLLINK = br.getRegex("<enclosure url=\"(.*?)\"").getMatch(0);
            }
            if (DLLINK == null || filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."), DLLINK.length());
            ext = ext == null ? ".flv" : ext;
            if (filename.endsWith(".")) {
                filename = filename.substring(0, filename.length() - 1);
            }
            filename = fixName(filename);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + fnQ + ext);
        } else {
            DLLINK = dllink;
        }
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(DLLINK).getContentType().contains("html")) {
                link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) {
                br.getHttpConnection().disconnect();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}