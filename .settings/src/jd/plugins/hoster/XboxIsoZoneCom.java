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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 16310 $", interfaceVersion = 2, names = { "xboxisozone.com" }, urls = { "http://(www\\.)?((xboxisozone|dcisozone|gcisozone|psisozone|theisozone)\\.com|romgamer\\.com/download)/dl\\-start/\\d+/(\\d+/)?" }, flags = { 0 })
public class XboxIsoZoneCom extends PluginForHost {

    public XboxIsoZoneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        if (!link.getDownloadURL().contains("www.")) link.setUrlDownload(link.getDownloadURL().replace("http://", "http://www."));
    }

    @Override
    public String getAGBLink() {
        return "http://xboxisozone.com/faq.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String mainlink = downloadLink.getStringProperty("mainlink");
        if (mainlink == null) throw new PluginException(LinkStatus.ERROR_FATAL, "mainlink missing, please delete and re-add this link to your downloadlist!");
        br.getPage(mainlink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        final String filesize = br.getRegex("<title>Download [^<>\"/]*? ([0-9\\.,]+ MB) \\&bull;").getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        String rcID = br.getRegex("\\?k=([^<>\"/]+)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        Form rcForm = new Form();
        rcForm.setMethod(MethodType.POST);
        rcForm.put("verify_code", "");
        rcForm.put("captcha", "");
        rcForm.setAction(downloadLink.getDownloadURL().replace("/dl-start/", "/dl-free/"));
        rc.setForm(rcForm);
        rc.setId(rcID);
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        String finallink = br.getRedirectLocation();
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (finallink.contains("/dl-start/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (finallink.contains("/download-limit-exceeded/")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String waittime = br.getRegex("You may download again in approximately:<b> (\\d+) Minutes").getMatch(0);
            if (waittime == null) waittime = br.getRegex("You may resume downloading in (\\d+) Minutes").getMatch(0);
            if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            if (br.containsHTML("(<TITLE>404 Not Found</TITLE>|<H1>Not Found</H1>|<title>404 \\- Not Found</title>|<h1>404 \\- Not Found</h1>|<h1>403 \\- Forbidden</h1>|<title>403 \\- Forbidden</title>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            if (br.containsHTML("The file you requested could not be found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // No available check possible because this tells the server that i
        // started a download
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}