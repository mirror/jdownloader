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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megafree.kz" }, urls = { "http://[\\w\\.]*?megafree\\.kz/file\\d+" }, flags = { 0 })
public class MegaFreeKz extends PluginForHost {

    public MegaFreeKz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://megafree.kz/";
    }

    private static final String CAPTCHATEXT = "confirm\\.php";
    private static final String AREA2       = "megafree.kz/delayfile";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains(AREA2)) {
                br.getPage(br.getRedirectLocation());
                return AvailableStatus.TRUE;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            if (br.containsHTML("(>Запрашиваемый Вами файл не найден\\.<|Файл мог быть удален на основании претензии правообладателей)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            Regex nameAndSize = br.getRegex("<title>(.*?) \\[(.*?)\\] с MegaFree.KZ</title>");
            String filename = nameAndSize.getMatch(0);
            if (filename == null) filename = br.getRegex("\\(\\'http://megafree\\.kz/file\\d+\\', \\'(.*?) на upload\\.com\\.ua\\'\\);\"").getMatch(0);
            String filesize = br.getRegex("class=\"srchSize\">(.*?)</span>").getMatch(0);
            if (filesize == null) nameAndSize.getMatch(1);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setName(filename.trim());
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.getURL().contains(AREA2)) {
            if (!br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            boolean failed = true;
            for (int i = 0; i <= 3; i++) {
                br.postPage(downloadLink.getDownloadURL(), "submit=1&capcha_code=" + getCaptchaCode("http://megafree.kz/confirm.php?rnd=1", downloadLink));
                if (br.containsHTML(CAPTCHATEXT)) continue;
                failed = false;
                break;
            }
            if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("new Array\\((.*?)\\);").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.trim().replaceAll("(\"|\"| |\r|\n|,)", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}