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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhost.com" }, urls = { "http://(www\\.)?pornhostdecrypted\\.com/([0-9]+/[0-9]+\\.html|[0-9]+|embed/\\d+)" }, flags = { 0 })
public class PornHostCom extends PluginForHost {

    private String ending = null;
    private String DDLINK = null;

    public PornHostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("pornhostdecrypted.com/", "pornhost.com/"));
        if (link.getDownloadURL().contains(".com/embed/")) {
            String protocol = new Regex(link.getDownloadURL(), "(https?)://").getMatch(0);
            String id = new Regex(link.getDownloadURL(), "embed/(\\d+)").getMatch(0);
            link.setUrlDownload(protocol + "://www.pornhost.com/" + id);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornhost.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("gallery not found") || br.containsHTML("You will be redirected to")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>pornhost\\.com  \\- ([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename.equals("")) filename = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        filename = Encoding.htmlDecode(filename.trim());
        if (br.containsHTML(">The movie needs to be converted first")) {
            downloadLink.getLinkStatus().setStatusText("The movie needs to be converted first");
            downloadLink.setFinalFileName(filename.trim() + ".flv");
            return AvailableStatus.TRUE;
        }
        if (!downloadLink.getDownloadURL().contains(".html")) {
            DDLINK = br.getRegex("\"(http://cdn\\d+\\.dl\\.pornhost\\.com/[^<>\"]*?)\"").getMatch(0);
            if (DDLINK == null) {
                DDLINK = br.getRegex("download this file</label>.*?<a href=\"(.*?)\"").getMatch(0);
            }
        } else {
            DDLINK = br.getRegex("style=\"width: 499px; height: 372px\">[\t\n\r ]+<img src=\"(http.*?)\"").getMatch(0);
            if (DDLINK == null) DDLINK = br.getRegex("\"(http://file[0-9]+\\.pornhost\\.com/[0-9]+/.*?)\"").getMatch(0);
        }
        // Maybe we have a picture
        if (DDLINK == null) DDLINK = br.getRegex("<div class=\"image\" style=\"width: \\d+px; height: \\d+px\">[\t\n\r ]+<img src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (DDLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DDLINK = Encoding.urlDecode(DDLINK, true);
        String ext = DDLINK.substring(DDLINK.lastIndexOf("."));
        if (ext.length() > 5) ext = ".flv";
        if (!filename.endsWith(ext)) filename += ext;
        downloadLink.setFinalFileName(filename);

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DDLINK);
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
        if (br.containsHTML(">The movie needs to be converted first")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The movie needs to be converted first", 30 * 60 * 1000l);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DDLINK, true, 0);
        try {
            dl.setAllowFilenameFromURL(false);
            String name = Plugin.getFileNameFromHeader(dl.getConnection());
            if (ending != null && ending.length() <= 1) {
                String name2 = downloadLink.getName();
                name = new Regex(name, ".+?(\\..{1,4})").getMatch(0);
                if (name != null && !name2.endsWith(name)) {
                    name2 = name2 + name;
                    downloadLink.setFinalFileName(name2);
                }
            }
        } catch (final Throwable e) {
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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

    @Override
    public void resetPluginGlobals() {
    }
}