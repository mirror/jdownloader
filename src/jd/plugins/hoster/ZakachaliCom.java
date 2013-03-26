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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zakachali.com" }, urls = { "http://(www\\.)?dl\\d+\\.zakachali\\.(net|com)/file/\\d+" }, flags = { 0 })
public class ZakachaliCom extends PluginForHost {

    public ZakachaliCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://zakachali.com/rules.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("zakachali.net/", "zakachali.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Файл не найден<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("href=\"\" style=\"text\\-decoration: none; font\\-size: 18px; color: #005a9c;\">([^<>\"/]*?)</a></td>").getMatch(0);
        if (filename == null) filename = br.getRegex("<TITLE>([^<>\"/]*?) \\- скачать</TITLE>").getMatch(0);
        String filesize = br.getRegex(">Размер файла: ([^<>\"/]*?)<").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        /**
         * Only accept filesizes under 1 GB because sometimes they show buggy filesizes (a dew TB instead of unter 1 MB)
         */
        if (SizeFormatter.getSize(filesize) <= 1048576000) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String finallink = checkDirectLink(downloadLink, "savedlink");
        if (finallink == null) {
            final Form dlForm = br.getForm(0);
            if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.submitForm(dlForm);
            finallink = br.getRegex("style=\"text\\-decoration: none; font\\-size: 16px; font\\-weight: bold; color: #005a9c;\">(http://[^<>\"]*?)</a>").getMatch(0);
            if (finallink == null) finallink = br.getRegex("(http://dl\\d+\\.zakachali\\.net/download_mem/[a-z0-9]*?)").getMatch(0);
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("savedlink", finallink);
        dl.startDownload();
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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