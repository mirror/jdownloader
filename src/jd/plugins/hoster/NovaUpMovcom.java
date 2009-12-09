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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "novaup.com" }, urls = { "http://[\\w\\.]*?nova(up|mov)\\.com/(download|sound|video)/[a-z|0-9]+" }, flags = { 0 })
public class NovaUpMovcom extends PluginForHost {

    public NovaUpMovcom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://novamov.com/terms.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String infolink = link.getDownloadURL();
        br.getPage(infolink);
        // Handling für Videolinks
        if (link.getDownloadURL().contains("video")) {
            String dllink = br.getRegex("\"file\",\"(.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (!dllink.contains("http")) dllink = "http://www.novaup.com" + dllink;
            link.setFinalFileName(null);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            dl.startDownload();

        } else {
            // handling für "nicht"-video Links
            String dllink = br.getRegex("> <strong><a href=\"(.*?)\"><span class=\"dwl_novaup").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (!dllink.contains("http")) dllink = "http://www.novaup.com" + dllink;
            link.setFinalFileName(null);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            dl.startDownload();
        }

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        // onlinecheck für Videolinks
        if (parameter.getDownloadURL().contains("video")) {
            if (br.containsHTML("This file no longer exists on our servers.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("The file is beeing transfered to our other servers. This may take few minutes.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            String filename1 = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
            if (filename1 == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String dllink = br.getRegex("\"file\",\"(.*?)\"").getMatch(0);

            URLConnectionAdapter con = br.openGetConnection(dllink);
            try {
                parameter.setDownloadSize(con.getContentLength());
                String filename = filename1 + ".flv";
                parameter.setName(filename.trim());
            } finally {
                con.disconnect();
            }

        } else {
            // Onlinecheck für "nicht"-video Links
            if (br.containsHTML("This file no longer exists on our servers.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("The file is beeing transfered to our other servers. This may take few minutes.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            String filename = br.getRegex("/([^/]{1,})/?\"><span class=\"dwl_novaup\">Click here to download</span>").getMatch(0);
            String filesize = br.getRegex("strong>File size : </strong>(.*?)</td>").getMatch(0);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(filename.trim());
            parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "")));
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
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
