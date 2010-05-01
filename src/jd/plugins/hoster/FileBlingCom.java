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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebling.com" }, urls = { "http://[\\w\\.]*?filebling\\.com/(full|dl)/[0-9]+/[0-9]+" }, flags = { 0 })
public class FileBlingCom extends PluginForHost {

    public FileBlingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filebling.com/terms.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/dl/", "/full/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        // No filesize/name is shown on the page so let's just check for errors!
        if (br.containsHTML("does not exist or may have been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String stepOne = br.getRegex("verification\\. <a href='(http.*?)'").getMatch(0);
        if (stepOne == null) {
            stepOne = br.getRegex("('|\")(http://filebling\\.com/g1/[0-9]+/[0-9]+)('|\")").getMatch(1);
            if (stepOne == null) {
                logger.warning("Regex for stepOne is defect, using hardcoded stepOne...");
                stepOne = downloadLink.getDownloadURL().replace("/full/", "/g1/");
            }
        }
        br.getPage(stepOne);
        for (int i = 0; i <= 5; i++) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("check if you have entered the image verification code correctly")) {
                br.getPage(stepOne);
                continue;
            }
            break;
        }
        if (br.containsHTML("check if you have entered the image verification code correctly")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("the file you have requested does not exist or may have been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String continu = br.getRegex("href='(.*?)'>Proceed to Download</a>").getMatch(0);
        if (continu == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String wait = br.getRegex("var countdownfrom=(\\d+)").getMatch(0);
        int tt = 60;
        if (wait != null) {
            logger.info("Waittime detected, waiting " + wait.trim() + " seconds from now on...");
            tt = Integer.parseInt(wait);
        }
        sleep(tt * 1001, downloadLink);
        br.getPage(continu);
        if (br.containsHTML("(You recently downloaded a file|Please wait until this time has passed to download another file)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        String finallink = br.getRegex("Generated link:<a href='(.*?)'").getMatch(0);
        if (finallink == null) finallink = br.getRegex("('|\")(http://f[0-9]+\\.filebling\\.com/dl/[0-9]+/.*?\\&token=.*?)('|\")").getMatch(1);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
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