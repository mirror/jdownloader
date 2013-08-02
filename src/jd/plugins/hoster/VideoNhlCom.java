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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.nhl.com" }, urls = { "http://(www\\.)?video\\.([a-z0-9]+\\.)?nhl\\.com/videocenter/console\\?([a-z0-9\\&=]+\\&)?id=\\d+" }, flags = { 0 })
public class VideoNhlCom extends PluginForHost {

    private String DLLINK = null;

    public VideoNhlCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.nhl.com/ice/page.htm?id=26389";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        /** All those linktypes are crap, we always use the same ;) */
        link.setUrlDownload("http://video.nhl.com/videocenter/console?id=" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<title>NHL VideoCenter</title>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final Regex aLotOfStuff = br.getRegex("console\\.playVideo\\(\"(.*?)\",\"\\d+\",\"(http://[^\"]+)\",\"([^\"]+)\",\"(\\d+)\",(false|true),[a-z]+,(false|true),");
        String postURL = aLotOfStuff.getMatch(1);
        final String type = aLotOfStuff.getMatch(0);
        String filename = aLotOfStuff.getMatch(2);
        final String quality = aLotOfStuff.getMatch(3);
        final String isFlex = aLotOfStuff.getMatch(5);
        if (filename == null) {
            filename = br.getRegex("class=\"details_title\" style=\"padding\\-bottom:2px;padding\\-right:3px\">(.*?)</div>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) Video \\- NHL VideoCenter</title>").getMatch(0);
            }
        }
        if (filename == null || postURL == null || type == null || quality == null || isFlex == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final int a = postURL.lastIndexOf(".");
        String firstUrlPart = "", secondUrlPart = "", qStr = "";
        if (a == -1) {
            firstUrlPart = postURL;
        } else {
            firstUrlPart = postURL.substring(0, a);
            secondUrlPart = postURL.substring(a);
        }
        switch (Integer.parseInt(quality)) {
        case 0:
            qStr = "";
            break;
        case 1:
            qStr = "_sd";
            break;
        case 2:
            qStr = "_sh";
            break;
        case 4:
            qStr = "_hd";
            break;
        default:
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown quality value!");
        }
        postURL = firstUrlPart + qStr + secondUrlPart;

        br.postPage("http://video.nhl.com/videocenter/servlets/encryptvideopath", "path=" + urlEncode(postURL) + "&isFlex=" + isFlex + "&type=" + Encoding.urlEncode(type));
        DLLINK = br.getRegex("\\!\\[CDATA\\[(http://.*?)\\]\\]>").getMatch(0);
        if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = secondUrlPart.equals("") ? null : secondUrlPart.substring(0, secondUrlPart.indexOf("?"));
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename).replace(":", " -") + ext);
        final Browser br2 = br.cloneBrowser();
        br2.setDebug(true);
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (con.getResponseCode() == 400) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
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

    private String urlEncode(String urlcoded) {
        urlcoded = urlcoded.replaceAll("\\/", "%2F");
        urlcoded = urlcoded.replaceAll("\\:", "%3A");
        urlcoded = urlcoded.replaceAll("\\?", "%3F");
        urlcoded = urlcoded.replaceAll("\\=", "%3D");
        urlcoded = urlcoded.replaceAll("\\&", "%26");
        urlcoded = urlcoded.replaceAll("\\#", "%23");
        urlcoded = urlcoded.replaceAll("\\.", "%2E");
        urlcoded = urlcoded.replaceAll("\\_", "%5F");
        return urlcoded;
    }
}