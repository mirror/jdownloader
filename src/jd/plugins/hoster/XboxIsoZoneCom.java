//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xboxisozone.com" }, urls = { "http://(www\\.)?((xboxisozone|dcisozone|gcisozone|psisozone)\\.com|romgamer\\.com/download)/free/\\d+/(\\d+/)?" }, flags = { 0 })
public class XboxIsoZoneCom extends PluginForHost {

    public XboxIsoZoneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://xboxisozone.com/faq.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        if (!link.getDownloadURL().contains("www.")) link.setUrlDownload(link.getDownloadURL().replace("http://", "http://www."));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // No available check possible because this tells the server that i
        // started a download
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String mainlink = downloadLink.getStringProperty("mainlink");
        if (mainlink == null) throw new PluginException(LinkStatus.ERROR_FATAL, "mainlink missing, please delete and re-add this link to your downloadlist!");
        br.getPage(mainlink);
        br.setFollowRedirects(false);
        String rcID = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        Form rcForm = new Form();
        rcForm.setMethod(MethodType.POST);
        rcForm.put("verify_code", "");
        rcForm.setAction(downloadLink.getDownloadURL());
        rc.setForm(rcForm);
        rc.setId(rcID);
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        String finallink = br.getRedirectLocation();
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (finallink.contains("err/code-invalid/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String waittime = br.getRegex("You may download again in approximately:<b> (\\d+) Minutes").getMatch(0);
            if (waittime == null) waittime = br.getRegex("You may resume downloading in (\\d+) Minutes").getMatch(0);
            if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            if (br.containsHTML("(>Free users may only download a maximum of| is currently downloading a file|If you would like to continue downloading, please abort your current download or wait for it to finish <br|>As it may take a few seconds for our system to update once your download has ceased, Please wait at| Free users may only download 1 file at a time)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            if (br.containsHTML("(Our system shows you have recently downloaded a file\\.</strong>|The file you attempted to download was <strong>|Free users must wait 5 minutes inbetween downloads\\.)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1001l);
            if (br.containsHTML("(<TITLE>404 Not Found</TITLE>|<H1>Not Found</H1>|<title>404 \\- Not Found</title>|<h1>404 \\- Not Found</h1>|<h1>403 \\- Forbidden</h1>|<title>403 \\- Forbidden</title>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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