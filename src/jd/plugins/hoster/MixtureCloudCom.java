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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixturecloud.com" }, urls = { "http://[\\w\\.]*?mixture(cloud|audio|doc|file|image|video)\\.com/(audio|doc|download|image|video)=[A-Za-z0-9]+" }, flags = { 0 })
public class MixtureCloudCom extends PluginForHost {
    
    // They have HTTPS certificate but the site has problems returning valid pages, no HTTPS support possible at this stage. 
    // Multiple domains all redirect back to 'sub.mixturecloud.com/' uids are transferable between each (sub)?domain & section.
    // All links have recaptcha with this one size fits all download method.
    
    public MixtureCloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        final String uid = new Regex(link.getDownloadURL(), "mixture(cloud|audio|doc|file|image|video)\\.com/(audio|doc|download|image|video)=(.+)").getMatch(2);
        link.setUrlDownload("http://file.mixturecloud.com/download=" + uid);
    }

    @Override
    public String getAGBLink() {
        return "http://file.mixturecloud.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // Waittime is skippable
        Form form = br.getForm(0);
        br.submitForm(form);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) continue;
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String dllink = new Regex(br.toString().replace("\\", ""), "download icon blue \" href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://file.mixturecloud.com/" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().equals("http://www.mixturecloud.com/")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404 Not Found<|The requested document was not found on this server|<h3>Keine Seite unter dieser Adresse</h3>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("(?i)<meta property=\"og:title\" content=\"(.*?) mixturecloud\\.com \"").getMatch(0);
        if (filename == null) filename = br.getRegex("(?i)<title>(.*?) - mixturecloud\\.com</title>").getMatch(0);
        else if (filename != null) filename = br.getRegex("<h2>[\r\n\t]+(.*?)[\r\n\t]+</h2>").getMatch(0);
        String filesize = br.getRegex("Originalgröße : <span style=\"font\\-weight:bold\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}