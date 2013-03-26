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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tenfiles.com" }, urls = { "http://(www\\.)?tenfiles\\.(com|info)/file/[a-z0-9]+" }, flags = { 0 })
public class TenFilesCom extends PluginForHost {

    public TenFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Gehört zu gigapeta.com
    @Override
    public String getAGBLink() {
        return "http://tenfiles.com/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("tenfiles.info/", "tenfiles.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://tenfiles.com/", "lang", "us");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">404<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("alt=\"file\" />\\-\\->([^<>\"]*?)</td>").getMatch(0);
        final String filesize = br.getRegex("Size[\t\n\r ]+</th>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(false);
        String captchaKey = (int) (Math.random() * 100000000) + "";
        String captchaUrl = "http://tenfiles.com/img/captcha.gif?x=" + captchaKey;
        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode("gigapeta.com", captchaUrl, downloadLink);
            br.postPage(br.getURL(), "download=&captcha_key=" + captchaKey + "&captcha=" + captchaCode);
            if (br.getRedirectLocation() != null) break;
        }
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("All threads for IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.tenfilescom.unavailable", "Your IP is already downloading a file"));
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}