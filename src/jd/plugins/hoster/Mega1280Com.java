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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.1280.com" }, urls = { "http://[\\w\\.]*?mega\\.1280\\.com/file/[0-9|A-Z]+" }, flags = { 0 })
public class Mega1280Com extends PluginForHost {

    public Mega1280Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mega.1280.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Sometime the page is extremely slow!
        br.setReadTimeout(120 * 1000);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("upload.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("clr05\"><b>(.*?)</b>").getMatch(0));
        String filesize = br.getRegex("<br />.*?<strong>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.setDebug(true);
        if (br.containsHTML("Vui lòng chờ cho lượt download kế tiếp")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        // Link zum Captcha (kann bei anderen Hostern auch mit ID sein)
        String captchaurl = "http://mega.1280.com/security_code.php";
        String code = getCaptchaCode(captchaurl, downloadLink);
        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Captcha Usereingabe in die Form einfügen
        captchaForm.put("code_security", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("frm_download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        // setzt den downloadlink zusammen (damit wird die Wartezeit umgangen)
        String dllink0 = br.getRegex("<div id=\"hddomainname\" style=\"display:none\">(.*?)</div>").getMatch(0);
        String dllink1 = br.getRegex("<div id=\"hdfolder\" style=\"display:none\">(.*?)</div>").getMatch(0);
        String dllink2 = br.getRegex("<div id=\"hdcode\" style=\"display:none\">(.*?)</div>").getMatch(0);
        String dllink3 = br.getRegex("<div id=\"hdfilename\" style=\"display:none\">(.*?)</div>").getMatch(0);
        if (dllink0 == null || dllink1 == null || dllink2 == null || dllink3 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String downloadURL = dllink0 + dllink1 + dllink2 + "/" + dllink3;
        // Waittime
        String wait = br.getRegex("hdcountdown\" style=\"display:none\">(\\d+)</div>").getMatch(0);
        if (wait == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        long tt = Long.parseLong(wait.trim());
        sleep(tt * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadURL, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Tài nguyên bạn yêu cầu không tìm thấy")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.Mega1280Com.Servererror", "Servererror!"), 60 * 60 * 1000l);
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
