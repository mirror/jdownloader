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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "narod.ru" }, urls = { "http://[\\w\\.]*?narod\\.ru/disk/\\d+/.*\\.html" }, flags = { 0 })
public class NarodRu extends PluginForHost {

    public NarodRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        setBrowserExclusive();
        return "http://narod.ru/agreement/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<title>404</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = br.getRegex(Pattern.compile("<dt class=\"name\"><i class=\"b-old-icon b-old-icon-arc\"></i>(.*?)</dt>")).getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String md5Hash = br.getRegex(Pattern.compile("<dt class=\"size\">md5:</dt>.*<dd class=\"size\">(.*?)</dd>", Pattern.DOTALL)).getMatch(0);
        if (md5Hash == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileSize = br.getRegex(Pattern.compile("<td class=\"l-download-info-right\">.*?<dl class=\"b-download-item g-line\">.*?<dt class=\"size\">.*?</dt>.*?<dd class=\"size\">(.*?).</dd>", Pattern.DOTALL)).getMatch(0);
        if (fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        fileSize = fileSize.replaceAll("М", "M");
        fileSize = fileSize.replaceAll("к", "k");
        fileSize = fileSize + "b";
        downloadLink.setMD5Hash(md5Hash.trim());
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getPage("http://narod.ru/disk/getcapchaxml/?rnd=1");
        System.out.print(br);
        String captchaKey = br.getRegex(Pattern.compile("<number.*>(.*?)</number>")).getMatch(0);
        String captchaUrl = "http://u.captcha.yandex.net/image?key=" + captchaKey;

        Form form = new Form();
        form.setMethod(Form.MethodType.POST);
        form.setAction(downloadLink.getDownloadURL());
        form.put("key", captchaKey);
        form.put("action", "sendcapcha");

        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);

            form.put("rep", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("href=\"/disk/start/")) break;
        }
        if (!br.containsHTML("href=\"/disk/start/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        String downloadSuffix = br.getRegex(Pattern.compile("<a class=\"h-link\" rel=\"yandex_bar\" href=\"(.*?)\">")).getMatch(0);
        String dlLink = Encoding.htmlDecode("http://narod.ru" + downloadSuffix);

        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, dlLink, true, 1);
        dl.startDownload();

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
    }
}