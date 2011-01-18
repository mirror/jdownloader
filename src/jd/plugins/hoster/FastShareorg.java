//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fastshare.org" }, urls = { "http://[\\w\\.]*?fastshare\\.org/download/(.*)" }, flags = { 0 })
public class FastShareorg extends PluginForHost {

    public FastShareorg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.fastshare.org/discl.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            br.setFollowRedirects(false);
            String url = downloadLink.getDownloadURL();
            br.getPage(url);
            if (!br.containsHTML("No filename specified or the file has been deleted")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("Wenn sie die Datei \"<b>(.*?)<\\/b>\"").getMatch(0)));
                String filesize = null;
                if ((filesize = br.getRegex("<i>\\((.*)MB\\)</i>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize(Math.round(Double.parseDouble(filesize)) * 1024 * 1024l);
                } else if ((filesize = br.getRegex("<i>\\((.*)KB\\)</i>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize(Math.round(Double.parseDouble(filesize)) * 1024l);
                }
                return AvailableStatus.TRUE;
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String url = downloadLink.getDownloadURL();
        requestFileInformation(downloadLink);

        /* Link holen */
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(form);
        if ((url = new Regex(br, "Link: <a href=(.*?)><b>").getMatch(0)) == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        url = "http://www.fastshare.org" + url;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Number of maximum connections/downloads is reached!", 600 * 1000l);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
