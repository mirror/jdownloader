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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigapeta.com" }, urls = { "http://[\\w\\.]*?gigapeta\\.com/dl/\\w+" }, flags = { 0 })
public class GigaPetaCom extends PluginForHost {

    public GigaPetaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://gigapeta.com/rules/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("<div id=\"page_error\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex infos = br.getRegex(Pattern.compile("<img src=\".*\" alt=\"file\" />(.*?)</td>.*?</tr>.*?<tr>.*?<th>.*?</th>.*?<td>(.*?)</td>", Pattern.DOTALL));
        String fileName = infos.getMatch(0);
        String fileSize = infos.getMatch(1);
        if (fileName == null || fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fileName.trim());
        downloadLink.setDownloadSize(Regex.getSize(fileSize.trim()));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String captchaKey = (int) (Math.random() * 100000000) + "";
        String captchaUrl = "http://gigapeta.com/img/captcha.gif?x=" + captchaKey;

        Form form = br.getForm(1);

        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);

            form.put("captcha", captchaCode);
            form.put("captcha_key", captchaKey);
            form.put("download", "");
            br.submitForm(form);
            if (br.getRedirectLocation() != null) break;
        }
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        dl = br.openDownload(downloadLink, br.getRedirectLocation(), false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

}