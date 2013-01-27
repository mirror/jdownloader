//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uitzendinggemist.nl" }, urls = { "http://(www\\.)?uitzendinggemist\\.nl/afleveringen/\\d+" }, flags = { 0 })
public class UitzendinggemistNl extends PluginForHost {

    private enum Quality {
        sb,
        bb,
        std;
    }

    private String  clipData;
    private String  finalURL         = null;
    private boolean NEEDSSILVERLIGHT = false;

    public UitzendinggemistNl(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.publiekeomroep.nl/disclaimer";
    }

    private String getClipData(final String tag) {
        return new Regex(clipData, "<" + tag + ">(.*?)</" + tag + ">").getMatch(0);
    }

    private void getfinalLink(final String episodeID) throws Exception {
        /* Diverse urls besorgen. Link ist statisch, kommt aus dem Flashplayer */
        final Browser br2 = br.cloneBrowser();
        clipData = br2.getPage("http://embed.player.omroep.nl/fl/ug_config.xml");
        String nextUrl = getClipData("sessionURL");
        if (nextUrl == null) { return; }

        clipData = br2.getPage(nextUrl);
        final String[] params = Encoding.Base64Decode(getClipData("key")).split("\\|");
        if (params == null || params.length != 4) { return; }

        nextUrl = "/info/stream/aflevering/" + episodeID + "/" + JDHash.getMD5(episodeID + "|" + params[1]).toUpperCase();
        clipData = br2.getPage(nextUrl);
        if (getClipData("code") != null ? true : false) { return; }

        final String[] streamUrls = br2.getRegex("(http://[^<>]+)(\n|\r\n)").getColumn(0);
        if (streamUrls == null || streamUrls.length == 0) { return; }

        /* sb (low), bb (normal) or std (high) */
        String[] Q = new String[2];
        for (final String streamUrl : streamUrls) {
            if (streamUrl.contains("type=http")) { // !mms

                Q = new Regex(streamUrl, "/([0-9a-z]+_[a-z]+)/").getMatch(0).split("_");
                Q[0] = Q[0] == null ? "fallback" : Q[0];
                Q[1] = Q[1] == null ? "fallback" : Q[1];

                switch (Quality.valueOf(Q[1])) {
                case sb:
                    if (finalURL == null || !finalURL.contains("_bb/")) {
                        finalURL = streamUrl;
                    }
                    break;
                case bb:
                    finalURL = streamUrl;
                    break;
                case std:
                    finalURL = streamUrl;
                    return;
                default:
                    finalURL = streamUrl;
                    break;
                }
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (NEEDSSILVERLIGHT) { throw new PluginException(LinkStatus.ERROR_FATAL, "Can't download MS Silverlight videos!"); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("text")) {
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dl.getConnection().getContentType().contains("plain")) {
                final String error = br.toString();
                if (error.length() < 100) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, error);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        clipData = br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Oeps\\.\\.\\.|Helaas, de opgevraagde pagina bestaat niet|<title>Home \\- Uitzending Gemist</title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = getClipData("title").replace("- Uitzending Gemist", "");
        if (filename == null) {
            filename = br.getRegex("<meta content=\"(.*?)\" property=\"og:title\"").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim().replaceAll("(:|,|\\s)", "_"));
        link.setFinalFileName(filename + ".mp4");
        String episodeID = br.getRegex("episodeID=(\\d+)\\&").getMatch(0);
        if (episodeID == null) {
            episodeID = br.getRegex("data\\-episode\\-id=\"(\\d+)\"").getMatch(0);
        }
        if (episodeID == null) {
            link.getLinkStatus().setStatusText("Can't download MS Silverlight videos!");
            NEEDSSILVERLIGHT = true;
            return AvailableStatus.TRUE;
        }

        /*
         * Es gibt mehrere Video urls in verschiedenen Qualitäten und Formaten. Methode ermittelt die höchst mögliche Qualität. mms:// Links
         * werden ignoriert.
         */
        getfinalLink(episodeID);

        if (finalURL == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(finalURL);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}