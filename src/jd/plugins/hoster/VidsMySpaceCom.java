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
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vids.myspace.com" }, urls = { "http://vids\\.myspace\\.com/index\\.cfm\\?fuseaction=vids\\.individual&videoid=\\d+" }, flags = { 0 })
public class VidsMySpaceCom extends PluginForHost {

    public VidsMySpaceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dlink = null;

    @Override
    public String getCoder() {
        return "typek_pb";
    }

    @Override
    public String getAGBLink() {
        return "http://www.myspace.com/index.cfm?fuseaction=misc.terms";
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
        String filename = br.getRegex("<h1 id=\"tv_tbar_title\">(.*?)</h1>").getMatch(0);
        if (null == filename || filename.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        StringBuilder linkSB = new StringBuilder("http://l3-hl1.videos02.");
        Regex regexp = new Regex(Encoding.htmlDecode(br.toString()), "<link rel=\"image_src\" href=\"http://d\\d+.ac-videos.(.*?)thumb1_(.*?).jpg\" />");

        String dlinkPart = regexp.getMatch(0);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(dlinkPart);

        dlinkPart = regexp.getMatch(1);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        linkSB.append(dlinkPart);
        linkSB.append("/vid.flv");

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
