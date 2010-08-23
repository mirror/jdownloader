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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadmachine.com" }, urls = { "http://[\\w\\.]*?uploadmachine\\.com/(download\\.php\\?id=[0-9]+&type=[0-9]{1}|file/[0-9]+/)" }, flags = { 0 })
public class UploadMachineCom extends PluginForHost {

    public UploadMachineCom(PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadmachine.com/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.setFollowRedirects(true);
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("colspan=2>Downloading the file  (.*?)</td>").getMatch(0));
        String filesize = br.getRegex("Size of the file:.*? align=left>(.*?)</td>").getMatch(0);
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
                if (br.containsHTML("Invalid Password")) {
                    logger.warning("Wrong password!");
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
        }

        if (br.containsHTML("You have reached the maximum")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        String dllink = br.getRegex(Pattern.compile("document.location=\"(.*?)\";this.disabled=true")).getMatch(0);
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