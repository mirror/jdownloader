//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixturecloud.com" }, urls = { "https?://(www\\.)?mixture(cloud|audio|doc|file|image|video)\\.com/(media/(download/)?|download=)[A-Za-z0-9]+" }, flags = { 0 })
public class MixtureCloudCom extends PluginForHost {

    // free: 1maxdl * 1 chunk
    // protocol: They have HTTPS certificate but httpd not setup correctly
    // captchatype: recaptcha
    // other: Multiple domains all redirect back to 'sub.mixturecloud.com/' uids
    // are transferable between each (sub)?domain & section. All links have
    // recaptcha with this one size fits all download method.
    private static final String PREMIUMONLY         = "File access is limited to users with unlimited";
    private static final String PREMIUMONLYUSERTEXT = JDL.L("plugins.hoster.mixturecloudcom", "Only downloadable for premium users");

    public MixtureCloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        final String uid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        link.setUrlDownload("http://www.mixturecloud.com/media/download/" + uid);
    }

    @Override
    public String getAGBLink() {
        return "http://file.mixturecloud.com/terms";
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // de = English in this case
        br.setCookie("http://mixturecloud.com/", "lang", "de");
        br.setCookie("http://mixturecloud.com/", "cc", "DE");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(There is no file here<|404: page not found \\!<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<\\!\\-\\- File header informations  \\-\\->[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?) \\- MixtureCloud\\.com \\-").getMatch(0);
        }
        String filesize = br.getRegex("<h5>Größe : ([^<>\"]*?)</h5>").getMatch(0);
        if (filesize == null) {
            logger.warning("MixtureCloud: Couldn't find filesize. Please report this to the JDownloader Development Team.");
            logger.warning("MixtureCloud: Continuing...");
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML(PREMIUMONLY)) link.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Sie haben in den letzten 30 Minuten eine Datei")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
        if (br.containsHTML(PREMIUMONLY)) throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        String dllink = br.getRegex("style=\"padding\\-left:30px\"></div>[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.mixturecloud\\.com/down\\.php\\?d=[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Waittime can be skipped */
        int wait = 52;
        final String waittime = br.getRegex("var time=(\\d+)").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}