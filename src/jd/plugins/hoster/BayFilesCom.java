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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bayfiles.com" }, urls = { "http://(www\\.)?bayfiles\\.com/file/[A-Z0-9]+/[A-Za-z0-9]+/.+" }, flags = { 0 })
public class BayFilesCom extends PluginForHost {

    public BayFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://bayfiles.com/tos";
    }

    private static final String CAPTCHAFAILED = "\"Invalid captcha\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Please check your link\\.<|>Invalid security token\\.|>The link is incorrect<|>The file you requested has been deleted<|>We have messed something up<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<h2>File:</h2>([\t\n\r ]+)?<p title=\"([^\"\\']+)\">[^\"\\']+, <strong>(.*?)</strong>");
        String filename = fileInfo.getMatch(1);
        String filesize = fileInfo.getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("(has recently downloaded a file\\.|Upgrade to premium or wait)")) {
            String reconnectWait = br.getRegex("Upgrade to premium or wait (\\d+)").getMatch(0);
            int reconWait = 60;
            if (reconnectWait != null) reconWait = Integer.parseInt(reconnectWait);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, reconWait * 60 * 1001l);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Referer", downloadLink.getDownloadURL());
        final String vFid = br.getRegex("var vfid = (\\d+);").getMatch(0);
        if (vFid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Can be skipped at the moment
        // String waittime =
        // br.getRegex("id=\"countDown\">(\\d+)</").getMatch(0);
        // int wait = 10;
        // if (waittime != null) wait = Integer.parseInt(waittime);
        br.getPage("http://bayfiles.com/ajax_download?_=" + System.currentTimeMillis() + "&action=startTimer&vfid=" + vFid);
        String token = br.getRegex("\"token\":\"(.*?)\"").getMatch(0);
        if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // sleep(wait * 1001l, downloadLink);
        br.postPage("http://bayfiles.com/ajax_captcha", "action=getCaptcha");
        final String reCaptchaID = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
        if (reCaptchaID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        Form dlForm = new Form();
        dlForm.setMethod(MethodType.POST);
        dlForm.setAction("http://bayfiles.com/ajax_captcha");
        dlForm.put("action", "verifyCaptcha");
        dlForm.put("token", token);
        rc.setForm(dlForm);
        rc.setId(reCaptchaID);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            // Standard handling does not work here because submit fields are
            // changed
            dlForm = rc.getForm();
            dlForm.put("challenge", rc.getChallenge());
            dlForm.put("response", c);
            br.submitForm(dlForm);
            if (br.containsHTML(CAPTCHAFAILED)) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        token = br.getRegex("\"token\":\"(.*?)\"").getMatch(0);
        if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://bayfiles.com/ajax_download", "action=getLink&vfid=" + vFid + "&token=" + token);
        String dllink = br.getRegex("onclick=\"javascript:window\\.location\\.href = \\'(http://.*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://s\\d+\\.baycdn\\.com/dl/.*?)\\'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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