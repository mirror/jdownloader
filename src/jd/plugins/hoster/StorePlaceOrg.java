//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "storeplace.org" }, urls = { "http://[\\w\\.]*?storeplace\\.(org|to)/\\?d=[A-Z0-9]+" }, flags = { 0 })
public class StorePlaceOrg extends PluginForHost {

    public StorePlaceOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.storeplace.org/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.setFollowRedirects(true);
        for (int i = 0; i < 5; i++) {
            /* hoster blocks several useragents */
            this.setBrowserExclusive();
            br.getHeaders().put("User-Agent", RandomUserAgent.generate());
            br.setCookie("http://www.storeplace.org", "setlang", "en");
            br.getPage(downloadLink.getDownloadURL());
            if (!br.containsHTML("User-Agent not allowed")) break;
            Thread.sleep(250);
        }
        if (br.containsHTML("User-Agent not allowed")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("File name:</b></td>.*?<td align=.*?width=[0-9]+px>(.*?)</td>").getMatch(0));
        String filesize = br.getRegex("File size:</b></td>.*?<td align=left>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("downloadpw")) {
            Form pwform = br.getFormbyProperty("name", "myform");
            if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String passCode = null;
            {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);

                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                pwform.put("downloadpw", passCode);
                br.submitForm(pwform);
                if (br.containsHTML("Password Error")) {
                    logger.warning("Wrong password!");
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
        }
        // Limit errorhandling, currently this host does not have any limit but
        // if they add the limit, this should work as it is the standard phrase
        // of the script which they use!
        if (br.containsHTML("You have reached the maximum")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        String dllink = br.getRegex("wnloadfile style=\"display:none\">.*?<a href=\"(.*?)\" onmouseout='window.status=\"\";return true;' onmou").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}