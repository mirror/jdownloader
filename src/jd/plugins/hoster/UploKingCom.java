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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploking.com" }, urls = { "http://(www\\.)?uploking\\.com/file/[A-Za-z0-9_\\-]+/" }, flags = { 0 })
public class UploKingCom extends PluginForHost {

    public UploKingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uploking.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        // Skip shitty redirector
        if (!br.getURL().contains("uploking.com/")) br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Page not found|<title>UploKing\\.com \\- </title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filter = "(fileDownload\">)?<a href=\"https?://uploking\\.com/file/[A-Za-z0-9_\\-]+/[^\"]+\">(.*?)</a><span[^>]+>\\((\\d+(\\.\\d+) (B|KB|MB|GB))\\) <";
        String filename = br.getRegex(filter).getMatch(1);
        if (filename == null) {
            filename = br.getRegex("<title>UploKing\\.com \\- ([^<>\"]*?)</title>").getMatch(0);
            if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        }
        link.setName(Encoding.htmlDecode(filename.trim()));

        // Filesize handling begin
        String filesize = br.getRegex(filter).getMatch(2);
        if (filesize == null) {
            filesize = br.getRegex("(\\d+(\\.\\d+) (B|KB|MB|GB))").getMatch(0);
        }
        if (filesize != null && !filesize.equals("")) link.setDownloadSize(SizeFormatter.getSize(filesize));
        // Filesize handling end
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("id=\"free_download\"><span>DOWNLOADING")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        final String fk = br.getRegex("name=\"fk\" id=\"fk\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (fk == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String referer = br.getURL();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        br.getPage("http://uploking.com/ajax.php?mode=box&function=predownload&fk=" + fk);
        int wait = 45;
        String waittime = br.getRegex("var wait = parseInt\\((\\d+)\\);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001, downloadLink);
        br.getHeaders().put("Referer", referer);
        br.getPage("http://uploking.com/ajax.php?mode=element&function=fses&fk=" + fk + "&free=1");
        // http://uploking.com/download.php?k=29c160f389e135b658421524391378d7
        // == "http://uploking.com/download.php?k=" + fk
        String dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        // Skip captcha
        // final String code = getCaptchaCode("http://uploking.com/captcha.php?"
        // + new Random().nextInt(1000), downloadLink);
        // br.getPage("http://uploking.com/ajax.php?mode=check&function=captcha&code="
        // + Encoding.urlEncode(code));
        // if(br.containsHTML("status\":\"err\""))throw new
        // PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getHeaders().put("Referer", referer);
        br.getHeaders().put("X-Requested-With", null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">Limit of free downloads for your country has been ended.<")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Download limit for your country has been reached", 2 * 60 * 60 * 1000);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // Possible as long as we don't have to enter captchas!
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}