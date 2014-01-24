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
import java.util.Random;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "androidfilehost.com" }, urls = { "http://(www\\.)?androidfilehost\\.com/\\?fid=\\d+" }, flags = { 0 })
public class AndroidFileHostCom extends PluginForHost {

    public AndroidFileHostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.androidfilehost.com/terms-of-use.php";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("<h2>([^<>\"]*?)</h2>").getMatch(0);
        final String filesize = br.getRegex("name=\"file_size\" id=\"file_size\" value=\"(\\d+)\"").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        final String md5 = br.getRegex(">md5</span>([^<>\"]*?)</p>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String filesize = br.getRegex("name=\"file_size\" id=\"file_size\" value=\"(\\d+)\"").getMatch(0);
        final String flid = br.getRegex("name=\"flid\" id=\"flid\" value=\"(\\d+)\"").getMatch(0);
        final String hc = br.getRegex("name=\"hc\" id=\"hc\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String tid = br.getRegex("name=\"tid\" id=\"tid\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String download_id = br.getRegex("name=\"download_id\" id=\"download_id\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (filesize == null || flid == null || hc == null || tid == null || download_id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // sleep(10 * 1001l, downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://www.androidfilehost.com/libs/otf/mirrors.otf.php", "submit=submit&action=getdownloadmirrors&fid=" + fid);
        final String[] adresses = br.getRegex("\"address\":\"([^<>\"]*?)\"").getColumn(0);
        if (adresses == null || adresses.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int position = 0;
        if (adresses.length > 1) position = new Random().nextInt(adresses.length - 1);
        final String adress = adresses[position];
        if (adress == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + adress + "/download.php?fid=" + fid + "&registered=0&waittime=10&fid=" + fid + "&flid=" + flid + "&uid=&file_size=" + filesize + "&hc=" + hc + "&tid=" + tid + "&download_id=" + download_id + "&filename=" + downloadLink.getFinalFileName() + "&action=download";
        // Disabled chunks and resume because different downloadserver = different connection limits
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}