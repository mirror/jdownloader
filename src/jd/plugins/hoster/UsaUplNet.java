//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "usaupload.net" }, urls = { "http://[\\w\\.]*?usaupload\\.net/d/[a-z0-9]{11}" }, flags = { 0 })
public class UsaUplNet extends PluginForHost {

    public UsaUplNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.usaupload.net/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(Sorry, the file you requested is not available|The file was deleted)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Name</strong>:(.*?)<br").getMatch(0);
        String filesize = br.getRegex("File size:</strong>(.*?)<br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        for (int i = 0; i <= 3; i++) {
            Form captchaform = br.getFormbyProperty("id", "upload");
            if (captchaform == null || !br.containsHTML("capt.tu?")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchaUrl = "http://www.usaupload.net/capt.tu?";
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaform.put("scode", code);
            br.submitForm(captchaform);
            if (br.containsHTML("(You entered wrong security code|please try again)")) {
                br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            break;
        }
        if (br.containsHTML("(You entered wrong security code|please try again)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("file=(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            // Check if the error is the limit
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Freeusers can only have 1 simultan download!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
