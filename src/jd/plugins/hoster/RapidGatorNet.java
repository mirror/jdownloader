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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidgator.net" }, urls = { "http://(www\\.)?rapidgator\\.net/file/\\d+/[^/<>]+\\.html" }, flags = { 0 })
public class RapidGatorNet extends PluginForHost {

    public RapidGatorNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://rapidgator.net/?content=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        int wait = 30;
        final String waittime = br.getRegex("var secs = (\\d+);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getPage("http://rapidgator.net/download/AjaxStartTimer?fid=" + new Regex(downloadLink.getDownloadURL(), "rapidgator\\.net/file/(\\d+)/").getMatch(0));
        final String sid = br2.getRegex("\"sid\":\"(.*?)\"").getMatch(0);
        if (sid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(wait * 1001l, downloadLink);
        br2.getPage("http://rapidgator.net/download/AjaxGetDownloadLink?sid=" + sid);
        if (!br2.containsHTML("\"state\":\"done\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://rapidgator.net/download/captcha");
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        for (int i = 0; i <= 5; i++) {
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.getForm().put("DownloadCaptchaForm%5Bcaptcha%5D", "");
            rc.setCode(c);
            if (br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) continue;
            break;
        }
        if (br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("location\\.href = \\'(http://.*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://pr_srv\\.rapidgator\\.net//\\?r=download/index\\&session_id=[A-Za-z0-9]+)\\'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setCookie("http://rapidgator.net/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Downloading:[\t\n\r ]+</strong>([^<>\"]+)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download file ([^<>\"]+)</title>").getMatch(0);
        String filesize = br.getRegex("File size:[\t\n\r ]+<script type=\"text/javascript\">[^/]+</script>[\t\n\r ]+<strong>(.*?)</strong>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}