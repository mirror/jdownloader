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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livemixtapes.com" }, urls = { "http://(www\\.)?livemixtapes\\.com/download/\\d+/.*?\\.html" }, flags = { 0 })
public class LiveMixTapesCom extends PluginForHost {

    public LiveMixTapesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.livemixtapes.com/contact.html";
    }

    private static final String CAPTCHATEXT = "/captcha/captcha\\.gif\\?";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Not Found</|The page you requested could not be found\\.<|>This mixtape is no longer available for download.<|>You must be logged in to access this page)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<td height=\"35\">\\&nbsp;\\&nbsp;\\&nbsp;(.*?)</td>[\t\n\r ]+<td align=\"center\">(.*?)</td>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String captchaUrl = "http://www.livemixtapes.com/captcha/captcha.gif?" + new Regex(downloadLink.getDownloadURL(), "livemixtapes\\.com/download/(\\d+)/").getMatch(0);
        String code = getCaptchaCode(captchaUrl, downloadLink);
        br.setFollowRedirects(false);
        // Usually we have a waittime here but it can be skipped
        br.postPage(downloadLink.getDownloadURL(), "code=" + code);
        if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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