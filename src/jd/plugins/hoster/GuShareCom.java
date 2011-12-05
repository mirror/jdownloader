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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gushare.com" }, urls = { "http://(www\\.)?v3\\.gushare\\.com/file\\.php\\?file=[a-z0-9]+" }, flags = { 0 })
public class GuShareCom extends PluginForHost {

    public GuShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://v3.gushare.com/term.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML("file_download\\.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://v3.gushare.com/file_download.php", "button=Download&file=" + new Regex(downloadLink.getDownloadURL(), "file\\.php\\?file=(.+)").getMatch(0));
        if (br.containsHTML("File นี้เฉพาะ PREMIUM DOWNLOAD เท่านั้น</font> <BR/>ท่านสามารถสมัคร")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.gusharecom.only4premium", "Only downloadable for premium users"));
        /** IP_BLOCKED limit can also be skipped */
        // if
        // (br.containsHTML("(\"ท่านสามารถดาวโหลดได้ครั้งละหนึ่งไฟล์|กรุณารอโหลดอีกครั้งในเวลา  <)"))
        // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 *
        // 1000l);
        Form dlForm = br.getFormbyProperty("name", "downloadform");
        if (dlForm == null) dlForm = br.getForm(0);
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Can still be skipped */
        // int wait = 32;
        // final String waittime = br.getRegex("ddl = (\\d+);").getMatch(0);
        // if (waittime != null) wait = Integer.parseInt(waittime) + 2;
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlForm, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("FILE NOT FOUND")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(File นี้ไม่มีในระบบแล้ว|Size </font></div>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex(">File : [\t\n\r ]+<font color=\"red\">(.*?)</font><br/><br/>[\t\n\r ]+Size (.*?)</font>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
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