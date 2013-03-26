//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.Random;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.gw.kz" }, urls = { "http://[\\w\\.]*?files\\.(gw|gameworld)\\.kz/[a-z0-9]+\\.html" }, flags = { 0 })
public class FilesGwKz extends PluginForHost {

    private static final String PWPROTECTED = ">input password:</span>";

    private static final String INDEXPAGE   = "http://files.gw.kz/index.php";

    public FilesGwKz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("gameworld.", "gw."));
    }

    @Override
    public String getAGBLink() {
        return "http://files.gw.kz/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        String passCode = null;
        requestFileInformation(downloadLink);
        String fid = br.getRegex("\\&fid=(\\d+)\\&").getMatch(0);
        if (br.containsHTML(PWPROTECTED)) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            br.postPage(INDEXPAGE, "check=passtoget&fid=" + fid + "&passtoget=" + passCode);
            if (br.containsHTML(PWPROTECTED)) throw new PluginException(LinkStatus.ERROR_RETRY);
        } else {
            if (!br.containsHTML("captcha\\.src") || fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean failed = true;
        for (int i = 0; i <= 3; i++) {
            br.postPage(INDEXPAGE, "check=captcha&fid=" + fid + "&captcha=" + getCaptchaCode("http://files.gw.kz/captcha.php?rand=" + new Random().nextInt(1000), downloadLink));
            if (br.containsHTML("button_pass")) continue;
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("style=\"display: block; font-size:18px;color:#000000;font-weight:bold;\"><a href=\"(http://.*?)\">Download").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setCookie("http://files.gw.kz", "gw_lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Запрашиваемый вами файл не найден")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"span-d_name bold px14 p_l_3px\" title=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("<b>Size: </b>(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        String md5 = br.getRegex("<b>MD5: </b>(.*?)</span>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5.trim());
        if (br.containsHTML(PWPROTECTED)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filesgwkz.passwordprotectedlink", "This link is password protected"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}