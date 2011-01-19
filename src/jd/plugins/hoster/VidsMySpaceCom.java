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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vids.myspace.com" }, urls = { "http://vids\\.myspace\\.com/index\\.cfm\\?fuseaction=vids\\.individual\\&videoid=\\d+" }, flags = { 0 })
public class VidsMySpaceCom extends PluginForHost {

    public VidsMySpaceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.myspace.com/index.cfm?fuseaction=misc.terms";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.getRedirectLocation() != null && !br.getRedirectLocation().contains("vids.myspace")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.vidsmyspacecom.temporaryunavailable", "This link is temporary unavailable"));
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("?fuseaction=vids.splash&vs=2")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (!br.getRedirectLocation().contains("vids.myspace")) return AvailableStatus.UNCHECKABLE;
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = br.getRegex("<h1 id=\"tv_tbar_title\">(.*?)</h1>").getMatch(0);
        if (null == filename || filename.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        StringBuilder linkSB = new StringBuilder("http://l3-hl1.videos02.");
        Regex regexp = new Regex(Encoding.htmlDecode(br.toString()), "<link rel=\"image_src\" href=\"http://d\\d+.ac-videos.(.*?)thumb1_(.*?).jpg\" />");

        String dlinkPart = regexp.getMatch(0);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        linkSB.append(dlinkPart);

        dlinkPart = regexp.getMatch(1);
        if (null == dlinkPart || dlinkPart.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        linkSB.append(dlinkPart);
        linkSB.append("/vid.mp4");

        dllink = linkSB.toString();
        filename = filename.trim();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = br.openGetConnection(dllink);
        if (con.getContentType().contains("html")) {
            dllink = dllink.replace("/vid.mp4", "/vid.flv");
            con = br.openGetConnection(dllink);
        }
        if (!con.getContentType().contains("html")) {
            link.setDownloadSize(con.getLongContentLength());
            link.setFinalFileName(filename + new Regex(dllink, "(\\.(flv|mp4))$").getMatch(0));
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
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
