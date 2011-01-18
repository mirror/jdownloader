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
import jd.parser.html.Form;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megafileupload.com" }, urls = { "http://[\\w\\.]*?megafileupload.com/en/file/[0-9]+/" }, flags = { 0 })
public class MegaFileUploadCom extends PluginForHost {

    public MegaFileUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.megafileupload.com/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<b>File name:</b></td>.*?<td align=left width=[0-9]+px>(.*?)</td>").getMatch(0);

        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("\"Click this to report for (.*?)\"").getMatch(0);
            }
        }
        String filesize = br.getRegex("<b>File size:</b></td>.*?<td align=left>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // This hoster taggs the files (filenames) and by setting this name as
        // the final filename no one will ever see these tags again
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = null;
        // Password protected links handling
        if (br.containsHTML("name=downloadpw")) {
            for (int i = 0; i <= 3; i++) {
                Form pwform = br.getFormbyProperty("name", "myform");
                if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                pwform.put("downloadpw", passCode);
                br.submitForm(pwform);
                if (br.containsHTML("name=downloadpw")) {
                    logger.warning("Wrong captcha or wrong password");
                    downloadLink.setProperty("pass", null);
                    continue;
                }
                break;
            }
        }
        if (br.containsHTML("name=\"downloadpw\"")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        String dllink = br.getRegex("location=\"(.*?)\"").getMatch(0);
        String ipblockedcheck = br.getRegex("name=\"myform\" action=\"(.*?)\"").getMatch(0);
        if (dllink == null && ipblockedcheck != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1001l);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
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
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
