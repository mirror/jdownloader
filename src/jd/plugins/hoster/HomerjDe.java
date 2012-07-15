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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "homerj.de" }, urls = { "http://(www\\.)?homerj\\.de/index\\.php\\?show=vods\\&play=\\d+(\\&res=\\d+p)?" }, flags = { 0 })
public class HomerjDe extends PluginForHost {

    private String DLLINK = null;

    public HomerjDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.homerj.de/index.php?show=impressum";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String res = getHighestResolution();
        if (res != null) br.getPage(downloadLink.getDownloadURL() + "&res=" + res);
        String filename = br.getRegex("<title>HomerJ.de \\- (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"PAGE\\-TOPIC\" content=\"(.*?)\">").getMatch(0);
        }
        String content = br.getRegex("pseudo_VodPlayer\\((.*)\\)\\);").getMatch(0);
        if (content == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        String tmpDllink = null;
        for (String s : new Regex(content, "base64_decode\\(\'(.*?)\'\\)").getColumn(0)) {
            tmpDllink = Encoding.Base64Decode(s);
            if (tmpDllink.startsWith("http") && tmpDllink.matches("http://vods\\d+\\.fr\\.ovh\\.homerj\\.de(:80)?/vods_homerj/[0-9a-f]+/[0-9a-f]+/[0-9a-f]+\\.mp4")) {
                DLLINK = tmpDllink;
            }
        }

        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setFinalFileName(filename.trim() + "(" + res + ").mp4");
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
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

    private String getHighestResolution() {
        String[] res = { "1080p", "720p", "480p", "360p", "240p" };
        for (String r : res) {
            if (br.getRegex("res=" + r).matches()) return r;
        }
        return null;
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