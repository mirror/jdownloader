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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

/**
 * @author typek_pb
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "break.com" }, urls = { "http://[\\w\\.]*?break\\.com/index/.*html" }, flags = { 0 })
public class BreakCom extends PluginForHost {

    private String dlink = null;

    public BreakCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://info.break.com/static/live/v1/pages/terms.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.getPage(link.getDownloadURL());
        String filename = br.getRegex("<title>Watch (.*?) Video | Break.com</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("content=\"(.*?) video at Break.com.").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?) | Break.com\" />").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<meta name=\"embed_video_title\" id=\"vid_title\" content=\"(.*?)\" />").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\" />").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("class=\"hd_player_title\">(.*?)</div>").getMatch(0);
                            if (filename == null) {
                                filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
                            }
                        }
                    }
                }
            }
        }
        if (null == filename || filename.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        StringBuilder linkSB = new StringBuilder();
        // http part
        String dlinkPart = new Regex(Encoding.htmlDecode(br.toString()), "var videoPath = \"(.*?)\" +").getMatch(0);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(dlinkPart);
        // dir
        dlinkPart = new Regex(Encoding.htmlDecode(br.toString()), "sGlobalContentFilePath='(.*?)'").getMatch(0);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(dlinkPart);
        linkSB.append("/");
        // filename
        dlinkPart = new Regex(Encoding.htmlDecode(br.toString()), "sGlobalFileName='(.*?)'").getMatch(0);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(dlinkPart);
        // post filename
        dlinkPart = new Regex(Encoding.htmlDecode(br.toString()), "flashVars.icon = \"(.*?)\" +").getMatch(0);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(".flv?");
        linkSB.append(dlinkPart);

        dlink = linkSB.toString();
        if (dlink == null || dlink.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        filename = filename.trim();
        link.setFinalFileName(filename + ".flv");
        br.setFollowRedirects(true);
        URLConnectionAdapter con = br.openGetConnection(dlink);
        if (!con.getContentType().contains("html"))
            link.setDownloadSize(con.getLongContentLength());
        else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
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
