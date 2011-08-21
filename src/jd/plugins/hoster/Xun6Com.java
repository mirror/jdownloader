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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xun6.com" }, urls = { "http://[\\w\\.]*?xun6\\.(com|net)/file/[a-z0-9]+" }, flags = { 0 })
public class Xun6Com extends PluginForHost {

    public Xun6Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://xun6.com/privacy/";
    }

    private static final String CAPTCHATEXT = "captcha\\.php\\?";

    @Override
    public void correctDownloadLink(DownloadLink link) {
        // Links with "www." don't work so we gotta change the link before
        // accessing it
        link.setUrlDownload(link.getDownloadURL().replace("www.", "").replace("xun6.com", "xun6.net"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("error") || br.getURL().contains("FileNotFound")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileandSize = br.getRegex("<title>訊6 \\- 下載 (.*?) 文件 \\((.*?)\\) \\- 美容,化妝,按摩,衣服,短裙,牛仔褲,戒指,項鏈,減肥,保健,香水,鞋子</title>");
        String filename = br.getRegex("title=\"\\{ Filereport \\}: (.*?)\"").getMatch(0);
        if (filename == null) {
            filename = fileandSize.getMatch(0);
        }
        String filesize = fileandSize.getMatch(1);
        if (filesize == null) {
            filesize = br.getRegex("<th>文件大小:</th>[\t\n\r ]+</tr>[\t\n\r ]+<tr>[\t\n\r ]+<td>(.*?)</td>").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Form captchaform = br.getFormbyProperty("name", "myform");
        String captchaurl = br.getRegex("id=\"dynimg\" src=\"(http://.*?)\"").getMatch(0);
        if (captchaurl == null) captchaurl = br.getRegex("\"(http://(www\\.)?xun6\\.net/captcha\\.php\\?rand=\\d+\\&key=[a-z0-9]+)\"").getMatch(0);
        if (captchaurl == null || captchaform == null) {
            logger.warning("Captchaform or captchaurl is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        captchaform.remove(null);
        String passCode = null;
        for (int i = 0; i <= 5; i++) {
            // Password protected links handling
            if (br.containsHTML("class=\"input_password_enabled\" type=\"password\" name=\"downloadpw\"")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                captchaform.put("downloadpw", passCode);
            }
            String code = getCaptchaCode(captchaurl, downloadLink);
            captchaform.put("captchacode", code);
            br.submitForm(captchaform);
            if (br.containsHTML("無法繼續下載！達到免費用戶下載瀏覽限制！請過一個小時後重新進行下載！<br>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            if (br.containsHTML(CAPTCHATEXT)) {
                logger.warning("Wrong captcha or wrong password");
                downloadLink.setProperty("pass", null);
                continue;
            }
            break;
        }
        if (br.containsHTML(CAPTCHATEXT)) {
            logger.warning("Wrong captcha or wrong password");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String domain = br.getRegex("domain = \"(.*?)\";").getMatch(0);
        String dirname = br.getRegex("dirname = \"(.*?)\"").getMatch(0);
        String basename = br.getRegex("basename = \"(.*?)\"").getMatch(0);
        if (domain == null || dirname == null || basename == null) {
            logger.warning("dllink regex is broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String dllink = "http://" + domain + dirname + "/" + basename;
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        // Ticket Time
        String ttt = br.getRegex("timeout=\"(\\d+)\"").getMatch(0);
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Download got Max Thread! Please Purchase Premium for Continue Downloads")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}