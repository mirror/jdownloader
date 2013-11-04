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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcloud.com" }, urls = { "https?://(www\\.)?(my\\.pcloud\\.com/#page=publink\\&code=|pc\\.cd/)[A-Za-z0-9]+" }, flags = { 0 })
public class PCloudCom extends PluginForHost {

    public PCloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://my.pcloud.com/#page=policies&tab=terms-of-service";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("https://api.pcloud.com/showpublink?code=" + getFID(link));
        if (br.containsHTML("\"error\": \"Invalid link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = getJson("name");
        final String filesize = getJson("size");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage("https://api.pcloud.com/getpublinkdownload?code=" + getFID(downloadLink) + "&forcedownload=1");
        final String hoststext = br.getRegex("\"hosts\": \\[(.*?)\\]").getMatch(0);
        if (hoststext == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String[] hosts = new Regex(hoststext, "\"([^<>\"]*?)\"").getColumn(0);
        String dllink = getJson("path");
        if (dllink == null || hosts == null || hosts.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dllink = "https://" + hosts[new Random().nextInt(hosts.length - 1)] + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\": (\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\": \"([^<>\"]*?)\"").getMatch(0);
        return result;
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