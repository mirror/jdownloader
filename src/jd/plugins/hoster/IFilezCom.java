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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "i-filez.com" }, urls = { "http://(www\\.)?i-filez\\.com/downloads/i/\\d+/f/.+" }, flags = { 0 })
public class IFilezCom extends PluginForHost {

    public IFilezCom(PluginWrapper wrapper) {
        super(wrapper);
        // Would be needed if multiple downloads were possible
        // this.setStartIntervall(11 * 1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://i-filez.com/terms";
    }

    // Linkchecker: http://i-filez.com/checkfiles (not usable, doesn't show
    // filesize)
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Файл не найден в базе i-filez\\.com\\. Возможно Вы неправильно указали ссылку\\.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = new Regex(link.getDownloadURL(), "i-filez\\.com/downloads/i/\\d+/f/(.+)").getMatch(0);
        String filesize = br.getRegex("<th>Размер:</th>[\r\t\n ]+<td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        String md5 = br.getRegex("<th>MD5:</th>[\r\t\n ]+<td>([a-z0-9]{32})</td>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "s_pair=&FREE=%D0%A1%D0%BA%D0%B0%D1%87%D0%B0%D1%82%D1%8C+%D0%B1%D0%B5%D1%81%D0%BF%D0%BB%D0%B0%D1%82%D0%BD%D0%BE");
        String additionalWaittime = br.getRegex("Для выдачи новой ссылки должно пройти не менее (\\d+) сек").getMatch(0);
        if (additionalWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(additionalWaittime) * 1001l);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("document\\.getElementById\\(\"wait_input\"\\)\\.value= unescape\\(\\'(.*?)\\'\\);").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.deepHtmlDecode(dllink).trim();
        int wait = 30;
        String regexedWaittime = br.getRegex("var sec=(\\d+);").getMatch(0);
        if (regexedWaittime != null) wait = Integer.parseInt(regexedWaittime);
        sleep(wait * 1001l, downloadLink);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}