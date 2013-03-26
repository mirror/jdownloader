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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filelaser.com" }, urls = { "http://(www\\.)?filelaser\\.com/file/[A-Za-z0-9]+/[^<>\"/]+" }, flags = { 0 })
public class FileLaserCom extends PluginForHost {

    public FileLaserCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filelaser.com/tos/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(The file was not found or has been remove|<title>File Not Found \\- FileLaser</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fileInfo = br.getRegex("<h3>([^<>\"]*?) <strong>\\[</strong>(\\d+(\\.\\d+)? [A-Za-z]+)<strong>\\]</strong></h3>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\- FileLaser</title>").getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String freeURL = downloadLink.getDownloadURL() + "/free/";
        br.getPage(freeURL);
        final String rcID = br.getRegex("api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
        final String token = br.getRegex("name=\"token\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String token2 = br.getRegex("name=\\'csrfmiddlewaretoken\\' value=\\'([^<>\"]*?)\\'").getMatch(0);
        if (rcID == null || token == null || token2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final long timeBefore = System.currentTimeMillis();
        int wait = 20;
        final String waittime = br.getRegex("id=\"countdown\">You must wait <strong>(\\d+)</strong> seconds").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        br.setFollowRedirects(false);
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            if (i == 0) {
                int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
                wait -= passedTime;
                logger.info("Waittime detected, waiting " + wait + " - " + passedTime + " seconds from now on...");
                if (wait > 0) sleep(wait * 1000l, downloadLink);
            }
            br.postPage(freeURL, "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&token=" + token + "&csrfmiddlewaretoken=" + token2);
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("You appear to have tried to skip the wait time")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Countdown error: wrong waittime");
        final Regex reconnectWaittime = br.getRegex(">You may download another file in (\\d+) minutes, (\\d+) seconds\\.<");
        if (reconnectWaittime.getMatches().length > 0) {
            final int minutes = Integer.parseInt(reconnectWaittime.getMatch(0));
            final int seconds = Integer.parseInt(reconnectWaittime.getMatch(0));
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (minutes * 60 + seconds) * 1001l);
        }
        final String dllink = br.getRedirectLocation();
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}