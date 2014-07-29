//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filepup.net" }, urls = { "http://(www\\.)?(sp\\d+\\.)?filepup\\.net/files/[A-Za-z0-9]+(/[^<>\"/]+)?\\.html" }, flags = { 0 })
public class FilePupNet extends PluginForHost {

    public FilePupNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/help/terms.php";
    }

    private static final String MAINPAGE = "http://www.filepup.net";
    private static final String APIKEY   = "vwUhhGH6lPH3auk6SM144PBg3PRQg";

    // Using FlexshareScript 1.1.2 API Version
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Use API with JDownloader API Key
        br.getPage("http://www." + this.getHost() + "/api/info.php?api_key=" + APIKEY + "&file_id=" + new Regex(link.getDownloadURL(), "/files/([A-Za-z0-9]+)\\.html").getMatch(0));
        if (br.containsHTML("file does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("\\[file_name\\] => (.*?)\n").getMatch(0);
        String filesize = br.getRegex("\\[file_size\\] => (\\d+)\n").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Set final filename here because hoster taggs files
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        String getLink = getLink();
        if (getLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // waittime
        final String ttt = br.getRegex("var time = (\\d+);").getMatch(0);
        int tt = 30;
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
        }
        if (tt > 240) {
            // 10 Minutes reconnect-waittime is not enough, let's wait one
            // hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        sleep(tt * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getLink, "task=download", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 405) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            }
            br.followConnection();
            if (br.containsHTML(">You have reached the limit of")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            }
            final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
            if (unknownError != null) {
                logger.warning("Unknown error occured: " + unknownError);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getLink() {
        String getLink = br.getRegex("disabled=\"disabled\" onclick=\"document\\.location=\\'(.*?)\\';\"").getMatch(0);
        if (getLink == null) {
            getLink = br.getRegex("(\\'|\")(" + "http://(www\\.)?([a-z0-9]+\\.)?" + MAINPAGE.replaceAll("(http://|www\\.)", "") + "/get/[A-Za-z0-9]+/\\d+/[^<>\"/]+)(\\'|\")").getMatch(1);
        }
        return getLink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}