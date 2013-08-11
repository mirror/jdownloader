//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 19117 $", interfaceVersion = 2, names = { "megacloud.com" }, urls = { "https?://(www\\.)?megaclouddecrypted\\.com/s/[a-zA-Z0-9]{10,}/[^/<>]+" }, flags = { 0 })
public class MegaCloudCom extends PluginForHost {

    private static final String mainPage = "http://megacloud.com";
    private String              filter   = null;

    /**
     * @author raztoki
     */
    public MegaCloudCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws IOException {
        link.setUrlDownload(link.getDownloadURL().replace("megaclouddecrypted.com/", "megacloud.com/"));
    }

    public void prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
    }

    @Override
    public String getAGBLink() {
        return mainPage + "/toc";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        // Check offline from decrypter
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        prepBrowser(br);
        br.getPage("https://www.megacloud.com/s/b7CC8CmllT2");
        br.getPage(link.getDownloadURL());

        if (br.containsHTML(">Error 404<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        filter = br.getRegex("(\"type\":\"FILE\",.*?</script>)").getMatch(0);
        String filename = new Regex(filter, "filename\":\"([^\"]+)").getMatch(0);
        if (filename == null) br.getRegex("<h1 class=\"shareable_name\">([^<]+)</h1>").getMatch(0);
        String filesize = new Regex(filter, "\"size\":(\\d+),").getMatch(0);

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null && !filesize.equals("")) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        if (filter == null) {
            logger.warning("Could not find 'filter'");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final String user_id = new Regex(filter, "user_id\":(\\d+)").getMatch(0);
        final String file_system_id = new Regex(filter, "file_system_id\":(\\d+)").getMatch(0);
        final String action = br.getRegex("action_root=\"(http[^<>\"]*?)\"").getMatch(0);
        if (user_id == null || file_system_id == null || action == null) {
            if (br.containsHTML(">File unavailable \\- This file has exceeded the daily bandwidth limit, please try again later")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file has exceeded the daily bandwidth limit, please try again later", 60 * 60 * 1000l);
            logger.warning("Could not find the first required vaules");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final Browser ajax = this.br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        ajax.getHeaders().put("accept-charseactiont", null);
        ajax.postPage("https://www.megacloud.com/ajax/get_download_token.php", "form_submit=true&user_id=" + user_id + "&fsid=" + file_system_id);

        final String download_token = ajax.getRegex("download_token\":\"([^\"]+)").getMatch(0);
        if (download_token == null) {
            logger.warning("Could not find the 'download_token'");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final String postData = "fsid=" + file_system_id + "&userid=" + user_id + "&token=" + Encoding.urlEncode(download_token);

        ajax.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        ajax.getHeaders().put("Accept-Charset", null);
        ajax.getHeaders().put("Accept-Language", "en-US,en;q=0.5");

        dl = jd.plugins.BrowserAdapter.openDownload(ajax, downloadLink, action, postData, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}