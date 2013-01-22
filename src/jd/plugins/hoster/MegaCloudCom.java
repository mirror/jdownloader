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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 19117 $", interfaceVersion = 2, names = { "megacloud.com" }, urls = { "https?://(www\\.)?megacloud\\.com/s/[a-zA-Z0-9]{10}/[^/<> ]+|http://mc\\.tt/[a-zA-Z0-9]{10}" }, flags = { 0 })
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
    public void correctDownloadLink(DownloadLink link) throws IOException {
        if (link.getDownloadURL().contains("mc.tt/")) {
            Browser cbr = new Browser();
            prepBrowser(cbr);
            cbr.getPage(link.getDownloadURL());
            String redirect = cbr.getRedirectLocation();
            if (redirect != null) {
                link.setUrlDownload(redirect);
            }
        }
    }

    public void prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
    }

    @Override
    public String getAGBLink() {
        return mainPage + "/toc";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
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

        String user_id = new Regex(filter, "user_id\":(\\d+)").getMatch(0);
        String file_system_id = new Regex(filter, "file_system_id\":(\\d+)").getMatch(0);
        if (user_id == null || file_system_id == null) {
            logger.warning("Could not find the first required vaules");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        Browser ajax = this.br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        ajax.postPage("https://www.megacloud.com/ajax/get_download_token.php", "form_submit=true&user_id=" + user_id + "&fsid=" + file_system_id);

        String download_token = ajax.getRegex("download_token\":\"([^\"]+)").getMatch(0);
        if (download_token == null) {
            logger.warning("Could not find the 'download_token'");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        Form download = br.getFormbyProperty("id", "dlForm");
        if (download == null) {
            logger.warning("Could not find the 'download' form");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        download.put("fsid", file_system_id);
        download.put("token", download_token.replaceAll("\\\\/", "/"));
        if ((download.getAction() == null || download.getAction().equals("")) && download.containsHTML("action_root")) {
            String action = new Regex(download, "action_root=(http[^\"\\}]+)").getMatch(0);
            if (action == null) {
                logger.warning("Could not find 'action_root'");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            download.setAction(action);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download, true, 0);
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