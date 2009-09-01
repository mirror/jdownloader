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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//rghost.ru by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rghost.ru" }, urls = { "http://[\\w\\.]*?(rghost\\.net|rghost\\.ru|phonon\\.rghost\\.ru)/([0-9]+|download/[0-9]+)" }, flags = { 0 })
public class RGhostRu extends PluginForHost {

    public RGhostRu(PluginWrapper wrapper) {
        super(wrapper);
        // this host blocks if there is no timegap between the simultan
        // downloads so waittime is 3,5 sec right now, works good!
        this.setStartIntervall(3500l);
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Access to the file was restricted") || br.containsHTML("<title>404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) — RGhost — file sharing</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("title=\"Comments for the file (.*?)\"").getMatch(0);
        }
        String filesize = br.getRegex("<small>\\((.*?)\\)</small></h1>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("<b>MD5</b>")) {
            String md5 = br.getRegex("<b>MD5</b></td><td> (.*?)</td></tr>").getMatch(0);
            link.setMD5Hash(md5);
        }
        if (br.containsHTML("<b>SHA1</b>")) {
            String sha1 = br.getRegex("<b>SHA1</b></td><td>(.*?)</td></tr>").getMatch(0);
            link.setSha1Hash(sha1);
        }
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("<h1 class=\"header_link\">.*?<a href=\"(.*?)\" class=\"hea").getMatch(0);

        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -20);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML(">409</div>")) {
                sleep(20000l, link);
            }
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public String getAGBLink() {
        return "http://hamstershare.com/terms";
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
