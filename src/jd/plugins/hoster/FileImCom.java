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

import org.appwork.utils.formatter.SizeFormatter;

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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileim.com" }, urls = { "http://(www\\.)?fileim\\.com/file/[a-f0-9]{16}" })
public class FileImCom extends PluginForHost {

    public FileImCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileim.com/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.fileim.com/", "SiteLang", "en-us");
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("fileim.com/notfound.html") || br.containsHTML("(Sorry, the file or folder does not exist|>Not Found<|FileIM \\- Not Found)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("id=\"FileName\" title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>[\t\n\r ]+FileIM Download File: ([^<>\"]*?)</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("id=\"download_name\" title=\"([^<>\"]*?)\"").getMatch(0);
        }
        String filesize = br.getRegex("id=\"FileSize\">([^<>\"]*?)<").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll("(\\(|\\))", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("\">Another Download Is Progressing")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
        }
        final String linkid = new Regex(downloadLink.getDownloadURL(), "([a-f0-9]+)$").getMatch(0);
        final String download_fuseronlycode = this.br.getRegex("id=\"download_fuseronlycode\" value=\"([^<>\"]+)\"").getMatch(0);
        final String fid = br.getRegex("download\\.fid=(\\d+);").getMatch(0);
        if (fid == null || download_fuseronlycode == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getPage("/ajax/download/gettimer.ashx");
        String waittime = br2.getRegex("(\\d+)_\\d+").getMatch(0);
        if (waittime == null) {
            waittime = br2.getRegex("(\\d+)").getMatch(0);
        }
        int wait = 150;
        if (waittime != null) {
            wait = Integer.parseInt(waittime);
        }
        /* Longer than 10 minutes? Let's reconnect! */
        if (wait >= 600) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        sleep(wait * 1001l, downloadLink);

        br2.getPage("/ajax/download/getdownservers.ashx?type=0");
        String domain = br2.getRegex("domain%3A\\'([^<>\"\\']+)\\'").getMatch(0);
        if (domain == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        domain = Encoding.htmlDecode(domain);
        final String dllink = "http://" + domain + "/download.ashx?a=" + br2.toString();
        br2.getPage("http://" + domain + "/hi.ashx?jsoncallback=jQuery" + System.currentTimeMillis() + "_" + System.currentTimeMillis() + "&fileuseronlycode=" + download_fuseronlycode + "&fileonlycode=" + linkid + "&filesize=" + downloadLink.getDownloadSize() + "&_=" + System.currentTimeMillis());
        final String isfile = PluginJSonUtils.getJsonValue(br2, "isfile");
        if ("false".equalsIgnoreCase(isfile)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        } else if (true) {
            /* TODO */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getLongContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<div> Another download is started")) {
                logger.warning("Hoster believes your IP address is already downloading");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Hoster believes your IP address is already downloading", 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}