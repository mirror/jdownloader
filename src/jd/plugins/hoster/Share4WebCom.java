//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share4web.com" }, urls = { "http://(www\\.)?share4web\\.com/get/[\\w\\.\\-]{32}" }, flags = { 0 })
public class Share4WebCom extends PluginForHost {

    public Share4WebCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.share4web.com/page/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.share4web.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found or removed|Page Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fileInfo = br.getRegex("<span id=\"fileName\" style=\"font\\-weight: normal;\">([^<>\"\\']+)</span>[\t\n\r ]+\\(([^<>\"\\']+)\\)[\t\n\r ]+<script>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Server sends filename with .exe ending, prevent it by setting final
        // filename here
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.postPage(downloadLink.getDownloadURL() + "/timer", "step=timer&referer=&reg=select&ad=");
        if (br.containsHTML("(>Somebody else is already downloading using your IP-address|Try to download file later)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            // Strange "Mirror" link
            dllink = br.getRegex("</div><div style=\"text\\-align:center;\"><br/><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                // "Normal" Downloadlink
                dllink = br.getRegex("\"(http://[a-z0-9]+\\.share4web\\.com/getf/[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("href=\"/get/[^\"<>]*?(/timer/free\\?step=[^\"<>]*?)&referer=").getMatch(0);
                    dllink = downloadLink.getDownloadURL() + dllink;

                    /* workaround for old stable bug */
                    dllink = dllink.replaceAll("\\/\\/", "/");
                    dllink = dllink.replaceAll("http:\\/", "http://");

                    br.getPage(dllink);
                    // some coutries (Poland, Germany) are redirected by one more page
                    // with possibility of SMS-payment
                    dllink = br.getRegex("id=\"noThanxDiv\"><a href=\"(/get/[^\"<>]*?/timer/link\\?step=[^\"<>]*?)&referer=\"").getMatch(0);
                    if (dllink != null) {
                        dllink = downloadLink.getDownloadURL() + dllink;

                        /* workaround for old stable bug */
                        dllink = dllink.replaceAll("\\/\\/", "/");
                        dllink = dllink.replaceAll("http:\\/", "http://");

                        br.getPage(dllink);
                    }
                    dllink = br.getRegex("\"(http://st\\d+\\.share4web\\.com/getf/[^\"<>]+)").getMatch(0);
                }
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}