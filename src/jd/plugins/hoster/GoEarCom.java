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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//goear.com hoster plugin by HerrDaur
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "goear.com" }, urls = { "http://(www\\.)?(goear\\.com/listen/[0-9a-f]+/|youares\\.com/reproducir/[a-z0-9]+/[0-9a-f]+/)" }, flags = { 0 })
public class GoEarCom extends PluginForHost {
    public GoEarCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.goear.com/pages/terms_and_conditions.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        String iD = new Regex(link.getDownloadURL(), "youares\\.com/reproducir/[a-z0-9]+/([0-9a-f]+)/").getMatch(0);
        if (iD != null) link.setUrlDownload("http://www.goear.com/listen/" + iD + "/");
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        br.getPage(getXmlUrl(link.getDownloadURL()));
        String file = br.getRegex("path=\"(.*?)\"").getMatch(0);
        if (file == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, file);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        String artist, title, extension;
        this.setBrowserExclusive();
        br.getPage(getXmlUrl(parameter.getDownloadURL()));
        if (br.containsHTML("404 - Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        artist = br.getRegex("artist=\"(.*?)\"").getMatch(0);
        title = br.getRegex("title=\"(.*?)\"").getMatch(0);
        extension = br.getRegex("\\/[0-9a-f]+\\.(.*?)\"").getMatch(0);
        if (extension == null || (artist == null && title == null)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(artist + " - " + title + "." + extension);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private String getXmlUrl(String url) {
        String id = new Regex(url, "/listen/([0-9a-f]+)/").getMatch(0);
        return "http://www.goear.com/tracker758.php?f=" + id;
    }
}
