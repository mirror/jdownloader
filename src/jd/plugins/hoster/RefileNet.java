//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "refile.net" }, urls = { "http://(www\\.)?refile\\.net/(d|f)/\\?[\\w]+" }, flags = { 0 })
public class RefileNet extends PluginForHost {

    // No HTTPS
    // redirects = www.refile.net > refile.net && /d/ > /f/ if recaptcha isn't
    // solved.

    public RefileNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("http://www.", "http://").replace(".net/d/", ".net/f/"));
    }

    @Override
    public String getAGBLink() {
        return "http://refile.net/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // tested with 20 seems fine.
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            for (int i = 0; i <= 5; i++) {
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.setCode(c);
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) continue;
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRegex("<a href=\"(https?://[\\w\\.]+refile\\.net/file/\\?[^\"\\>]+)\">").getMatch(0);
            if (dllink == null) br.getRegex("\"(https?://[\\w\\.]+refile\\.net/file/\\?[^\"\\>]+)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://refile.net/", "lang", "en");
        br.getPage(link.getDownloadURL());
        String uid = new Regex(br.getURL(), "refile\\.net/(d|f)/\\?([\\w]+)").getMatch(1);
        if (br.containsHTML("(?i)<title>\r\n\tDownload File Not Found|<h1>Download File Not Found</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("(?i)<title>\r\n\tDownload (.*?) \\| refile\\.net").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("(?i)<h1>Download (.*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<span style=\"font\\-size:14px\\;\"><a href=\"http://refile.net/f/\\?[a-zA-Z0-9]+\">(.*?)</a></span>").getMatch(0);
                if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String filesize = br.getRegex("([\\d\\.]+\\&nbsp\\;[a-zA-Z]+)<br />[\r\n\t]+<span id=\"fd_" + uid + "\">Description").getMatch(0);
        if (filesize == null) filesize = br.getRegex(" \\(([\\d\\.]+\\&nbsp\\;[a-zA-Z]+)\\)\\[/b\\]").getMatch(0);
        filesize = filesize.replace("&nbsp;", "");
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