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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rabidfiles.com" }, urls = { "http://(www\\.)?rabidfiles\\.com/(?!faq|terms|register)[A-Za-z0-9]+" }, flags = { 0 })
public class RabitFilesCom extends PluginForHost {

    // TODO: refactor (rename) plugin when jd2 becomes stable

    public RabitFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.rabidfiles.com/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("rabidfiles.com/index.html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex infoRegex = br.getRegex("<th class=\"descr\">[\t\n\r ]+<strong>([^<>\"]*?) \\((\\d+(\\.\\d+)? [A-Za-z]+)\\)<br/");
        String filename = infoRegex.getMatch(0);
        String filesize = infoRegex.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String waittime = br.getRegex("var seconds = (\\d+);").getMatch(0);
        int wait = waittime == null ? 60 : Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL() + "?d=1", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // reCaptcha
            final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
            if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int i = 0; i <= 5; i++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL() + "/" + downloadLink.getName(), "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, false, 1);
                if (dl.getConnection().getContentType().contains("html")) {
                    br.followConnection();
                }
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
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