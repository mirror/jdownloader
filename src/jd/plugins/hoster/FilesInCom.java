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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesin.com" }, urls = { "http://(www\\.)?filesin\\.com/[A-Z0-9]+/download\\.html" }, flags = { 0 })
public class FilesInCom extends PluginForHost {

    private static final String RECAPTCHAFAILED = "google\\.com/recaptcha/api/";

    public FilesInCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesin.com/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>File Not Found, may be deleted by user|<title>FilesIn\\.com \\| Upload your files fast,easy and free</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) \\| FilesIn\\.com</title>").getMatch(0);
        String filesize = br.getRegex("<div>File size <b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String freeLink = br.getRegex("<tr><td style=\"padding\\-left: 50px;\"><a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (freeLink == null) freeLink = br.getRegex("\"(http://(www\\.)?filesin\\.com/[A-Z0-9]+/[A-Z0-9]+/" + downloadLink.getName() + ".html)\"").getMatch(0);
        if (freeLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(freeLink);
        final String rcID = br.getRegex("\\?k=([^<>\"\\'/]+)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        for (int i = 0; i <= 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, freeLink, "download=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, false, 1);
            if (!dl.getConnection().getContentType().contains("html")) break;
            br.followConnection();
            if (br.containsHTML(RECAPTCHAFAILED)) continue;
            break;
        }
        if (br.containsHTML(RECAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        final String reconnectWait = br.getRegex("try again after \\((\\d+)\\) Minute").getMatch(0);
        if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(reconnectWait) + 1) * 60 * 1000l);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}