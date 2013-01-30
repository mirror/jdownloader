//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rayfile.com" }, urls = { "http://[\\w]*?\\.rayfile\\.com/[^/]+/files/[^/]+/" }, flags = { 0 })
public class RayFileCom extends PluginForHost {

    private String userAgent = null;

    public RayFileCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie("http://rayfile.com", "lang", "english");
        if (userAgent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", userAgent);
        br.setConnectTimeout(2 * 60 * 1000);
        br.setReadTimeout(2 * 60 * 1000);

    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        // use their regex
        String vid = br.getRegex("var vid = \"(.*?)\"").getMatch(0);
        String vkey = br.getRegex("var vkey = \"(.*?)\"").getMatch(0);

        Browser ajax = this.br.cloneBrowser();
        ajax.getPage("http://www.rayfile.com/zh-cn/files/" + vid + "/" + vkey + "/");

        String downloadUrl = ajax.getRegex("downloads_url = \\['(.*?)'\\]").getMatch(0);
        if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        br.clearCookies("rayfile.com");
        br.getHeaders().put("Accept-Encoding", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Referer", null);
        br.getHeaders().put("User-Agent", "Grid Service 2.1.10.8366");
        downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);

        // IMPORTANT: resuming must be set to false.
        // Range: bytes=resuming bytes - filesize -> not work
        // Range: bytes=resuming bytes - resuming bytes + 5242880 -> work
        // resuming limitations: 1 chunk @ max 5242880 bytes Range!

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.getPage(link.getDownloadURL());

        if (br.containsHTML("Not HTML Code. Redirect to: ")) {
            String redirectUrl = br.getRequest().getLocation();
            link.setUrlDownload(redirectUrl);
            br.getPage(redirectUrl);
        }

        if (br.containsHTML("page404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("var fname = \"(.*?)\";").getMatch(0);
        String filesize = br.getRegex("formatsize = \"(.*?)\";").getMatch(0);

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null && !filesize.equals("")) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}