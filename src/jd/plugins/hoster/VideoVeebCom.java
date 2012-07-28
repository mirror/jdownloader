//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

//Same code as for HsUploadCom
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videoveeb.com" }, urls = { "http://(www\\.)?videoveeb\\.(com|net)/\\?dw?=[A-Z0-9]+" }, flags = { 0 })
public class VideoVeebCom extends PluginForHost {

    public VideoVeebCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://videoveeb.com/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("videoveeb.net/", "videoveeb.com/").replace("/?dw=", "?d="));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("DL_FileNotFound")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fInfo = br.getRegex("\">Download File:</span> ([^<>\"]*?) \\- (\\d+(\\.\\d+)?[^<>\"]*?)</div>");
        String filename = br.getRegex("<h2 class=\"float\\-left\">\\&nbsp;\\&nbsp;.:: ([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) filename = fInfo.getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String filesize = fInfo.getMatch(1);
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("videoveeb.com/?d=", "videoveeb.com/?dw="));
        final String dllink = findLink(br);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String findLink(final Browser br) throws Exception {
        String finalLink = br.getRegex("(http://.{5,30}getfile\\.php/\\d+[^<>\"\\']*?)(\"|\\')").getMatch(0);
        if (finalLink == null) {
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            if (sitelinks == null || sitelinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (String alink : sitelinks) {
                alink = Encoding.htmlDecode(alink);
                if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                    finalLink = alink;
                    break;
                }
            }
        }
        return finalLink;
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