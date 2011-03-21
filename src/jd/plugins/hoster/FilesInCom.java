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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesin.com" }, urls = { "http://(www\\.)?filesin\\.com/[A-Z0-9]+/download\\.html" }, flags = { 0 })
public class FilesInCom extends PluginForHost {

    public FilesInCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesin.com/terms.html";
    }

    private static final String RECAPTCHAFAILED = "google\\.com/recaptcha/api/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>File Not Found|may be deleted by user or administrator\\.</|<title>FilesIn\\.com \\| Upload your files fast,easy and free</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 stlye=\"margin: 2px;\">(.*?)</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\| FilesIn\\.com</title>").getMatch(0);
        String filesize = br.getRegex("<p>File Size: (.*?)</p>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form freeform = br.getForm(0);
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(freeform);
        for (int i = 0; i <= 5; i++) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.getForm().put("recaptcha_response_field", c);
            rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, rc.getForm(), false, 1);
            if (!dl.getConnection().getContentType().contains("html")) break;
            br.followConnection();
            if (br.containsHTML(RECAPTCHAFAILED)) continue;
            break;
        }
        if (br.containsHTML(RECAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
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