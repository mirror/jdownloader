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

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "transitfiles.com" }, urls = { "http://(www\\.)?transitfiles\\.com/dl/[A-Za-z0-9]+" }, flags = { 0 })
public class TransitFilesCom extends PluginForHost {

    public TransitFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("www.", ""));
    }

    @Override
    public String getAGBLink() {
        return "http://transitfiles.com/tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://transitfiles.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        // This is their API, it also works for multiple links but doesn't show
        // the filename!
        // br.postPage("http://api.transitfiles.com/linktester.php", "linktest="
        // + link.getDownloadURL());
        if (br.containsHTML("(>The following download is not available on our server|<title>TransitFiles \\- File Sharing made easy\\!</title>|>The link file above no longer exists\\. This could be due to several reasons|>The file link is invalid<|>The uploader has deleted the file in question or change the access rights<|>The file was illegal and was deleted by our staff<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"nom_de_fichier\">(.*?)</div>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\\$\\(\"#inputrenamelinked\"\\)\\.val\\(\"(.*?)\"\\);").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- TransitFiles</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex(">Filesize: </span><strong>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Regex ipblocked = br.getRegex("block_download\\.php\\?min=(\\d+)\\&sec=(\\d+)\"");
        long wait = 0;
        String minutes = ipblocked.getMatch(0);
        String seconds = ipblocked.getMatch(1);
        if (minutes != null) wait = Integer.parseInt(minutes) * 60;
        if (seconds != null) wait += Integer.parseInt(seconds);
        if (wait > 60) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        } else if (wait > 0) {
            // Not worth reconnecting, lets wait and try again
            sleep(wait * 1000l, downloadLink);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        String captchaLink = getCaptchaLink();
        if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaLink = "http://transitfiles.com" + captchaLink;
        String code = getCaptchaCode(captchaLink, downloadLink);
        br.getPage(downloadLink.getDownloadURL() + "?code=" + code);
        // Not a good way to check if the captcha was entered correctly
        if (!br.containsHTML("fliptimer\\(")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("var magicomfg = \\'<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://s\\d+\\.transitfiles\\.com/\\?d=[A-Za-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Waittime is akippable at the moment
        // int littleWait = 45;
        // String littleWaittime =
        // br.getRegex("seconds:date\\.getSeconds\\(\\)\\+(\\d+)\\}").getMatch(0);
        // if (littleWaittime != null) littleWait =
        // Integer.parseInt(littleWaittime);
        // sleep(littleWait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getCaptchaLink() {
        String captchaLink = br.getRegex("<div class=\"anti_robot\">[\t\n\r ]+<img src=\"(/.*?)\"").getMatch(0);
        if (captchaLink == null) captchaLink = br.getRegex("\"(/captchadl\\.php\\?[a-z0-9]+)\"").getMatch(0);
        return captchaLink;
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