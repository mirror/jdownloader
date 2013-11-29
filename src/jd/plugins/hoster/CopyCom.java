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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "copy.com" }, urls = { "https?://(www\\.)?copy\\.com/(s/)?[A-Za-z0-9]+" }, flags = { 0 })
public class CopyCom extends PluginForHost {

    public CopyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.copy.com/about/tos";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("https://www.copy.com/s/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
    }

    private static final String INVALIDLINKS = "https?://(www\\.)?copy\\.com/(price|about|barracuda|bigger|install|developer|browse|home|auth|signup|policies)";

    /** They got an API: https://www.copy.com/developer/documentation */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches(INVALIDLINKS)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getPage(link.getDownloadURL());
        String realUrl = br.getRegex("\"mime_type\":\"[^<>\"]{1,}\",\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (realUrl != null) {
            realUrl = realUrl.replace("\\", "").replace("copy.com/", "copy.com/s/");
            if (!realUrl.contains("www.")) realUrl = realUrl.replace("://", "://www.");
            br.getPage(realUrl);
        }
        if (br.containsHTML(">You\\&rsquo;ve found a page that doesn\\&rsquo;t exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String fInfo = br.getRegex("\"children\":\\[\\{(.*?)\\}").getMatch(0);
        if (fInfo == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String filename = getJson("name", fInfo);
        final String filesize = getJson("size", fInfo);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = br.getURL().replace("/s/", "/").replace("www.", "") + "?download=1";
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
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