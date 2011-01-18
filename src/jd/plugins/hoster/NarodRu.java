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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "narod.ru" }, urls = { "http://[\\w\\.]*?narod\\.ru/disk/(\\d+/.*|start/[0-9]+\\.[0-9a-z]+-narod\\.yandex\\.ru/[0-9]{6,15}/[0-9a-z]+/[a-zA-Z0-9%.]+)" }, flags = { 0 })
public class NarodRu extends PluginForHost {

    public NarodRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        setBrowserExclusive();
        return "http://narod.ru/agreement/";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Correct added link because some guys are spreading narod direct links
        // which only causes problems so correcting the link is the best
        // solution here
        if (link.getDownloadURL().contains("/start/")) {
            String linkid = new Regex(link.getDownloadURL(), "/start/[0-9]+\\.[0-9a-z]+-narod\\.yandex\\.ru/([0-9]{6,15})/[0-9a-z]+/[a-zA-Z0-9%.]+").getMatch(0);
            String filename = new Regex(link.getDownloadURL(), "/start/[0-9]+\\.[0-9a-z]+-narod\\.yandex\\.ru/[0-9]{6,15}/[0-9a-z]+/([a-zA-Z0-9%.]+)").getMatch(0);
            String finallink = "http://narod.ru/disk/" + linkid + "/" + filename;
            link.setUrlDownload(finallink);
        }
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        /*
         * no captcha because of http://userscripts.org/scripts/review/64343
         */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; chrome://global/locale/intl.properties; rv:1.8.1.12) Gecko/2008102920  Firefox/3.0.0 YB/4.2.0");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<title>404</title>") || br.containsHTML("Файл удален с сервиса") || br.containsHTML("Закончился срок хранения файла\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = br.getRegex(Pattern.compile("<dt class=\"name\"><i class=\"b-old-icon b-old-icon-arc\"></i>(.*?)</dt>")).getMatch(0);
        if (name == null) name = br.getRegex(Pattern.compile("class=\"name\"><i class=.*?></i>(.*?)</dt>")).getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String md5Hash = br.getRegex(Pattern.compile("<dt class=\"size\">md5:</dt>.*<dd class=\"size\">(.*?)</dd>", Pattern.DOTALL)).getMatch(0);
        if (md5Hash == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileSize = br.getRegex(Pattern.compile("<td class=\"l-download-info-right\">.*?<dl class=\"b-download-item g-line\">.*?<dt class=\"size\">.*?</dt>.*?<dd class=\"size\">(.*?).</dd>", Pattern.DOTALL)).getMatch(0);
        if (fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        fileSize = fileSize.replaceAll("Г", "G");
        fileSize = fileSize.replaceAll("М", "M");
        fileSize = fileSize.replaceAll("к", "k");
        fileSize = fileSize + "b";
        downloadLink.setMD5Hash(md5Hash.trim());
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        boolean needpassword = br.containsHTML("input id=\"password");
        String passCode = null;
        for (int i = 1; i <= 10; i++) {
            /*
             * first check because of our little workaround we no longer need a
             * captcha ;)
             */
            if (br.containsHTML("href=\"/disk/start/")) break;
            br.getPage("http://narod.ru/disk/getcapchaxml/?rnd=1");
            String captchaKey = br.getRegex(Pattern.compile("<number.*>(.*?)</number>")).getMatch(0);
            if (captchaKey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchaUrl = "http://u.captcha.yandex.net/image?key=" + captchaKey;

            Form form = new Form();
            form.setMethod(Form.MethodType.POST);
            form.setAction(downloadLink.getDownloadURL());
            form.put("key", captchaKey);
            form.put("action", "sendcapcha");
            if (needpassword) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                form.put("passwd", Encoding.urlEncode(passCode));
            }
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);

            form.put("rep", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("input id=\"password")) {
                downloadLink.getStringProperty("pass", null);
                continue;
            }
        }
        if (!br.containsHTML("href=\"/disk/start/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("input id=\"password")) {
            downloadLink.getStringProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password wrong");
        }

        String downloadSuffix = br.getRegex(Pattern.compile("<a class=\"h-link\" rel=\"yandex_bar\" href=\"(.*?)\">")).getMatch(0);
        String dlLink = Encoding.htmlDecode("http://narod.ru" + downloadSuffix);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, true, 1);
        if (needpassword && passCode == null) {
            downloadLink.getStringProperty("pass", passCode);
        }
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
    }
}