//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sendfile.su" }, urls = { "http://(www\\.)?sendfile\\.su/\\d+" }, flags = { 0 })
public class SendFileSu extends PluginForHost {

    public SendFileSu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://sendfile.su/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals("http://sendfile.su/msg.php?error") || br.containsHTML("Файл не найден\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<td>Название</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        if (filename == null) filename = br.getRegex("").getMatch(0);
        String filesize = br.getRegex("\\((\\d+B)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        final String md5 = br.getRegex("<td>MD5</td>[\t\n\r ]+<td>([a-z0-9]{32})</td>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        final String serverID = br.getRegex("var server_id = (\\d+);").getMatch(0);
        if (serverID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://sendfile.su/get_download_link.php", "file_id=" + fid);
        final String result = br.toString();
        if (result.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String dllink = "http://s" + serverID + ".sendfile.su/download/" + fid + "/" + result;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -3);
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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}