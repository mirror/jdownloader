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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

//goear.com hoster plugin by HerrDaur
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "goear.com" }, urls = { "http://(www\\.)?(goear\\.com/listen/[0-9a-f]+/|youares\\.com/reproducir/[a-z0-9]+/[0-9a-f]+/)" }, flags = { 0 })
public class GoEarCom extends PluginForHost {
    public GoEarCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        String iD = new Regex(link.getDownloadURL(), "youares\\.com/reproducir/[a-z0-9]+/([0-9a-f]+)/").getMatch(0);
        if (iD != null) link.setUrlDownload("http://www.goear.com/listen/" + iD + "/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.goear.com/pages/terms_and_conditions.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("404 \\- Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String title = br.getRegex("<title>([^<<\"]*?) \\- goear\\.com</title>").getMatch(0);
        if (title == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(Encoding.htmlDecode(title.trim()) + ".mp3");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        final String finallink = "http://www.goear.com/action/sound/get/" + new Regex(link.getDownloadURL(), "([0-9a-f]+)/$").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink);
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
}