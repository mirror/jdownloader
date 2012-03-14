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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "luckyshare.net" }, urls = { "http://(www\\.)?luckyshare\\.net/\\d+" }, flags = { 0 })
public class LuckyShareNet extends PluginForHost {

    public LuckyShareNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://luckyshare.net/termsofservice";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(There is no such file available|<title>LuckyShare \\- Download</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 class=\\'file_name\\'>([^<>\"/]+)</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>LuckyShare \\- ([^<>\"/]+)</title>").getMatch(0);
        String filesize = br.getRegex("<span class=\\'file_size\\'>Filesize: ([^<>\"/]+)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String filesizelimit = br.getRegex(">Files with filesize over ([^<>\"\\'/]+) are available only for Premium Users").getMatch(0);
        if (filesizelimit != null) throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can only download files up to " + filesizelimit);
        final String reconnectWait = br.getRegex("id=\"waitingtime\">(\\d+)</span>").getMatch(0);
        if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 1001l);
        final String rcID = br.getRegex("Recaptcha\\.create\\(\"([^<>\"/]+)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.getPage("http://luckyshare.net/download/request/type/time/file/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
        final String hash = ajax.getRegex("\"hash\":\"([a-z0-9]+)\"").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Waittime can still be skipped */
        // int wait = 30;
        // String waittime = ajax.getRegex("\"time\":(\\d+)").getMatch(0);
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        for (int i = 0; i <= 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            ajax.getPage("http://luckyshare.net/download/verify/challenge/" + rc.getChallenge() + "/response/" + c + "/hash/" + hash);
            if (ajax.containsHTML("(Verification failed|You can renew the verification image by clicking on a corresponding button near the validation input area)")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (ajax.containsHTML("(Verification failed|You can renew the verification image by clicking on a corresponding button near the validation input area)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = ajax.getRegex("\"link\":\"(http:[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
        /**
         * Simultan downloads possible as long as we have the hashes BEFORE any
         * download is started. Because this host is using reCaptcha this will
         * always work
         */
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}