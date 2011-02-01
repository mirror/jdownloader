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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "novaup.com" }, urls = { "http://(www\\.)?(nova(up|mov)\\.com/(download|sound|video)/[a-z0-9]+|embed\\.novamov\\.com/embed\\.php\\?width=\\d+\\&height=\\d+\\&v=[a-z0-9]+)" }, flags = { 0 })
public class NovaUpMovcom extends PluginForHost {

    public NovaUpMovcom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.novamov.com/terms.php";
    }

    private static final String VIDEOREGEX                   = "flashvars\\.file=\"(http.*?)\"";
    private static final String VIDEOREGEX2                  = "\"(http://s\\d+\\.novamov\\.com/dl/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.flv)\"";
    private static final String TEMPORARYUNAVAILABLE         = "(The file is being transfered to our other servers\\.|This may take few minutes\\.</)";
    private static final String TEMPORARYUNAVAILABLEUSERTEXT = "Temporary unavailable";

    public void correctDownloadLink(DownloadLink link) {
        String videoID = new Regex(link.getDownloadURL(), "embed\\.novamov\\.com/embed\\.php\\?width=\\d+\\&height=\\d+\\&v=([a-z0-9]+)").getMatch(0);
        if (videoID != null) link.setUrlDownload("http://www.novamov.com/video/" + videoID);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.novaupmovcom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT), 30 * 60 * 1000l);
        br.setFollowRedirects(false);
        String infolink = link.getDownloadURL();
        br.getPage(infolink);
        String dllink = null;
        // Handling für Videolinks
        if (link.getDownloadURL().contains("video")) {
            dllink = br.getRegex(VIDEOREGEX).getMatch(0);
            if (dllink == null) dllink = br.getRegex(VIDEOREGEX2).getMatch(0);
        } else {
            // handling für "nicht"-video Links
            dllink = br.getRegex("class= \"click_download\"><a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://e\\d+\\.novaup\\.com/dl/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (!dllink.contains("http://")) dllink = "http://novaup.com" + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("This file no longer exists on our servers")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // onlinecheck für Videolinks
        if (parameter.getDownloadURL().contains("video")) {
            String dllink = br.getRegex(VIDEOREGEX).getMatch(0);
            if (dllink == null) dllink = br.getRegex(VIDEOREGEX2).getMatch(0);
            String filename = br.getRegex("name=\"title\" content=\"Watch(.*?)online\"").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>Watch(.*?)online \\| NovaMov - Free and reliable flash video hosting</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            filename = filename.trim() + ".flv";
            parameter.setFinalFileName(filename);
            if (br.containsHTML(TEMPORARYUNAVAILABLE)) {
                parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.novaupmovcom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT));
                return AvailableStatus.TRUE;
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            URLConnectionAdapter con = br.openGetConnection(dllink);
            try {
                parameter.setDownloadSize(con.getContentLength());
            } finally {
                con.disconnect();
            }

        } else {
            // Onlinecheck für "nicht"-video Links
            String filename = br.getRegex("<h3><a href=\"#\"><h3>(.*?)</h3></a></h3>").getMatch(0);
            if (filename == null) filename = br.getRegex("style=\"text-indent:0;\"><h3>(.*?)</h3></h5>").getMatch(0);
            String filesize = br.getRegex("strong>File size :</strong>(.*?)</div>").getMatch(0);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            parameter.setName(filename.trim());
            parameter.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "")));
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
