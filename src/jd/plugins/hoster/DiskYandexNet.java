//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "disk.yandex.net" }, urls = { "http://yandexdecrypted\\.net/\\d+" }, flags = { 0 })
public class DiskYandexNet extends PluginForHost {

    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://disk.yandex.net/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (!link.getDownloadURL().matches("http://yandexdecrypted\\.net/\\d+")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        setBrowserExclusive();
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setFollowRedirects(true);
        br.getPage(link.getStringProperty("mainlink", null));
        if (br.containsHTML("(<title>The file you are looking for could not be found\\.|>Nothing found</span>|<title>Nothing found \\— Yandex\\.Disk</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = link.getStringProperty("plain_filename", null);
        final String filesize = link.getStringProperty("plain_size", null);

        link.setName(filename);
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String hash = downloadLink.getStringProperty("hash_plain", null);
        final String ckey = br.getRegex("\"ckey\":\"([^\"]+)\"").getMatch(0);
        if (ckey == null) {
            logger.warning("Could not find ckey");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("/handlers.jsx", "_ckey=" + ckey + "&_name=getLinkFileDownload&hash=" + Encoding.urlEncode(hash));
        String dllink = parse("url");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.startsWith("//")) dllink = "http:" + dllink;
        // Don't do htmldecode as the link will be invalid then
        dllink = dllink.replace("amp;", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String parse(String var) {
        if (var == null) return null;
        String result = br.getRegex("<" + var + ">([^<>\"]*?)</" + var + ">").getMatch(0);
        if (result == null) result = br.getRegex("\"" + var + "\":\"([^\"]+)").getMatch(0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}