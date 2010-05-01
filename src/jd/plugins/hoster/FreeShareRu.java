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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "free-share.ru" }, urls = { "http://[\\w\\.]*?free-share\\.ru/[0-9]+/[0-9]+/" }, flags = { 0 })
public class FreeShareRu extends PluginForHost {

    public FreeShareRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "info@free-share.ru";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("файл не найден/not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Имя файла: <b><a style=\"color:.*?\" href=\"http://free-share\\.ru/[0-9]+/[0-9]+/(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"http://free-share\\.ru/[0-9]+/[0-9]+/.*?\">(.*?)</a>").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        Regex info = br.getRegex("Размер файла.*?<b><strong>(.*?)</strong>(.*?)</b>");
        if (info.getMatch(0) != null && info.getMatch(1) != null) {
            String filesize = info.getMatch(0) + info.getMatch(1);
            filesize = filesize.trim();
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form freeform = new Form();
        freeform.setMethod(Form.MethodType.POST);
        freeform.setAction(downloadLink.getDownloadURL());
        freeform.put("sid", "1");
        br.submitForm(freeform);
        // Ticket Time
        String ttt = br.getRegex("id=\"w\">.*?(\\d+).*?</").getMatch(0);
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001l, downloadLink);
        }
        freeform.put("sid", "2");
        br.submitForm(freeform);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        String sid = br.getRegex("name=\"sid\" value=\"(.*?)\"").getMatch(0);
        if (sid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        rc.getForm().put("sid", sid);
        rc.getForm().setAction(downloadLink.getDownloadURL());
        rc.setCode(c);
        if (br.containsHTML("неверный код")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("name=\"sid\".*?<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://frdl[0-9]+\\.free-share\\.ru/[0-9a-z]+/.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}