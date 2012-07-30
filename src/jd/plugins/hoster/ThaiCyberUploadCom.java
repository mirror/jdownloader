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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "thaicyberupload.com" }, urls = { "http://(www\\.)?(thaicyberupload\\.com/get/|data\\d+\\.thaicyberupload\\.com/dl/)[A-Za-z0-9]+" }, flags = { 0 })
public class ThaiCyberUploadCom extends PluginForHost {

    public ThaiCyberUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.thaicyberupload.com/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.thaicyberupload.com/get/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals("http://www.thaicyberupload.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = get("File name");
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\- Upload and Download File for Free</title>").getMatch(0);
        final String filesize = get("Size");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        final String md5 = get("MD5 Sum");
        if (md5 != null) link.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("class=\"frmupload\" name=\"frm\" action=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://data\\d+\\.thaicyberupload\\.com/dl/[A-Za-z0-9]+/\\d+/[^<>\"/]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 60;
        final String waittime = br.getRegex("Please wait for (\\d+) seconds or refresh this page to renew the link").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String get(final String parameter) {
        return br.getRegex(">" + parameter + "</td>[\t\n\r ]+<td align=\"left\">([^<>\"]*?)</td>").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}