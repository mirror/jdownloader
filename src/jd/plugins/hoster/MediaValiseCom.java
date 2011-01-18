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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediavalise.com" }, urls = { "http://(www\\.)?mediavalise\\.com/file/[a-z0-9]+/.*?\\.html" }, flags = { 0 })
public class MediaValiseCom extends PluginForHost {

    public MediaValiseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediavalise.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(due to Terms of Service or deleted by user\\.<|>The requested file has been deleted<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("class=\"download_block_title\">(.*?)<div>File size: <span>(.*?)</span></div>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML("api\\.recaptcha\\.net") && !br.containsHTML("google\\.com/recaptcha/api/")) {
            br.getPage("http://www.mediavalise.com/files/choice");
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            int wait = 30;
            String waitTime = br.getRegex("var begin_sec = parseInt\\(\"(\\d+)\"\\)").getMatch(0);
            if (waitTime != null) {
                wait = Integer.parseInt(waitTime);
            } else {
                waitTime = br.getRegex("<strong id=\"v_timer\">[\t\n\r ]+<span class=\\'red\\'>(\\d+)</span> minutes").getMatch(0);
                if (waitTime != null) wait = Integer.parseInt(waitTime) * 60;
            }
            if (wait > 120) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
            sleep(wait * 1001l, downloadLink);
            br.getPage(downloadLink.getDownloadURL());
        }
        String reCaptchaId = br.getRegex("k=(.*?)\"").getMatch(0);
        if (reCaptchaId == null || reCaptchaId.equals("")) {
            reCaptchaId = br.getRegex("\\?k=([A-Za-z0-9%]+)\"").getMatch(0);
        }
        if (reCaptchaId == null) {
            logger.warning("reCaptchaId is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(reCaptchaId);
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        br.postPage(downloadLink.getDownloadURL().replace(".html", "") + ".js", "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
        if (br.containsHTML(">Wrong captcha code<")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("id=\"donwload_link\" class=\"center\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://(www\\.)?mediavalise\\.com/exp_download/[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}