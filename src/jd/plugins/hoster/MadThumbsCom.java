//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.crypt.JDCrypt;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "madthumbs.com" }, urls = { "http://(www\\.)?madthumbs\\.com/videos/[\\w-]+/[\\w-]+/\\d+" }, flags = { 0 })
public class MadThumbsCom extends PluginForHost {

    private String DLLINK = null;
    private String KEY    = "YTk3OWNjMTdjZjk2NjE5MzEzNjJiOTBlMmU5Yjc2MmM=";

    public MadThumbsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.madthumbs.com/legal/terms_of_use";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Falls das vor .bismarck's Fix released wird wird immer ein- und
        // dasselbe Video geladen->So isses besser
        boolean pluginBroken = true;
        if (pluginBroken) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(false);
        br.setCookie("http://www.madthumbs.com/", "language", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(<title>Free Porn Tube \\- MadThumbs\\&trade;</title>|<meta property=\"og:title\" content=\"Free Porn Tube \\- MadThumbs\")")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("vid_title:\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) \\- MadThumbs\\&trade;</title> ").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?) \\- MadThumbs\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<div class=\"title\"><h1 class=\"fl\"><a href=\"\">(.*?)</a></h1></div>").getMatch(0);
                }
            }
        }
        String[] param = br.getRegex("flowplayer\\((.*?)\\)\\;").getMatch(0).replaceAll("\n|\\s|'", "").split(",");
        if (param == null || param.length == 0 || param.length < 4 || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Generate hashvalue
        String hash = null;
        try {
            hash = JDCrypt.decrypt(JDHexUtils.getByteArray(param[3]), JDHexUtils.getByteArray(Encoding.Base64Decode(KEY)), JDHexUtils.getByteArray(param[2]));
        } catch (final Throwable e) {
        }
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(param[1]) + "&hash=" + hash.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
