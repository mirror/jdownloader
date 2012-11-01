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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "howfile.com" }, urls = { "http://(www\\.)?howfile\\.com/file/[a-z0-9]+/[a-z0-9]+" }, flags = { 0 })
public class HowFileCom extends PluginForHost {

    private static final String MAINPAGE = "http://howfile.com/";

    public HowFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.howfile.com/user/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String vid = br.getRegex("setCookie\\(\"vid\", \"(.*?)\"").getMatch(0);
        String vid1 = br.getRegex("setCookie\\(\"vid1\", \"(.*?)\"").getMatch(0);
        if (vid == null || vid1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Those cookies are important, no downloadstart without them!
        br.setCookie(MAINPAGE, "vid", vid);
        br.setCookie(MAINPAGE, "vid1", vid1);
        final String dllink = br.getRegex("\"(http://dl\\d+\\.howfile\\.com/downfile/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            if (br.containsHTML("This is only for our premium member")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This is only for our premium member!");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // Works like YunFileCom
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setReadTimeout(3 * 60 * 1000);
        br.setCookie(MAINPAGE, "language", "en_us");
        // Workaround for Range header
        link.setProperty("ServerComaptibleForByteRangeRequest", false);
        // Workaround for Stable
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        // If exception file offline
        try {
            br.getPage(link.getDownloadURL());
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(File not found or System under maintanence\\.|File Name: <b> </b><br>|File Size: <b>0 B </b><br>|<title> \\- howfile\\.com \\- Free File Hosting and Sharing, Unlimit Download </title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File Name: <b>(.*?) </b><br>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\- howfile\\.com \\- Free File Hosting and Sharing, Unlimit Download </title>").getMatch(0);
        String filesize = br.getRegex("File Size: <b>(.*?) </b><br>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("(?i)([\\d\\.]+ ?(GB|MB))").getMatch(0);
            if (filesize == null) {
                logger.warning("Can not find filesize, please report this issue to JDownloader Development Team!");
                logger.warning("Continuing... (filesize will get updated once the download starts)");
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}