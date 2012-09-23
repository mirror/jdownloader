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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "iranfilm16.com" }, urls = { "http://(www\\.)?iranfilm16\\.com/forum/dl\\.php\\?serverid=\\d+\\&file=[^<>\"]+" }, flags = { 0 })
public class Iranfilm16Com extends PluginForHost {

    public Iranfilm16Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://iranfilm16.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // No way to check if the availablestatus
        br.setFollowRedirects(true);
        link.setFinalFileName(Encoding.htmlDecode(new Regex(link.getDownloadURL(), "([^<>\"/]+)$").getMatch(0)));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");

        if (dllink == null) {
            br.getPage(downloadLink.getDownloadURL().replace("/dl.php?", "/free.php?"));

            // Can be skipped
            // final String waittime =
            // br.getRegex("<div class=\"sec\"><span id=\"Timer\">(\\d+)</span>").getMatch(0);
            // int wait = 60;
            // if (waittime != null) wait = Integer.parseInt(waittime);
            // sleep(wait * 1001l, downloadLink);

            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final Regex info = new Regex(downloadLink.getDownloadURL(), "iranfilm16\\.com/forum/dl\\.php\\?serverid=(\\d+)\\&file=(.+)");
            final String server = info.getMatch(0);
            final String file = info.getMatch(1);
            for (int i = 0; i <= 3; i++) {
                br.postPage("http://iranfilm16.com/forum/dl_captcha.php", "Level=Serial");
                if (!br.containsHTML("/captcha\\.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                final String code = getCaptchaCode("http://iranfilm16.com/forum/captcha.php", downloadLink);
                br.postPage("http://iranfilm16.com/forum/free.php", "Captcha=" + code + "&file=" + Encoding.urlEncode(file) + "&serverid=" + server);
                if (br.containsHTML("No htmlCode read")) continue;
                break;
            }
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.toString().trim();
            if (!dllink.startsWith("http") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // Only way to tell if a link is offline
            if (br.containsHTML(">404 \\- Not Found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}