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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wuala.com" }, urls = { "https://www\\.wuala\\.com/[A-Za-z0-9\\-_]+/[^<>\"/]+/[^<>\"/]+" }, flags = { 0 })
public class WualaCom extends PluginForHost {

    public WualaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.wuala.com/de/about/legal";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getPage("https://api2.wuala.com/previewSorted/" + getLinkpart(link) + "?il=0&ff=1");
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<name>([^<>\"]*?)</name>").getMatch(0);
        if (filename == null) filename = getJson("basename");
        final String ext = getJson("ext");
        String filesize = br.getRegex("<size>(\\d+)</size>").getMatch(0);
        if (filesize == null) filesize = getJson("bytes");
        if (filename == null || filesize == null || ext == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()) + "." + ext);
        link.setDownloadSize(Long.parseLong(filesize));
        // final String md5 = br.getRegex("<hash>([A-F0-9]+)</hash>").getMatch(0);
        // if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = "https://content.wuala.com/contents/" + getLinkpart(downloadLink) + "/?dl=1";
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    private String getLinkpart(final DownloadLink dl) {
        final String addedlink = Encoding.htmlDecode(dl.getDownloadURL());
        return Encoding.urlEncode(new Regex(addedlink, "wuala\\.com/(.+)").getMatch(0));
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