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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upload.com.ua" }, urls = { "http://[\\w\\.]*?(beta\\.upload|upload)\\.com\\.ua/(link|get)/[0-9]+" }, flags = { 2 })
public class UploadComUa extends PluginForHost {

    public UploadComUa(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://upload.com.ua/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://upload.com.ua/rules.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        String downloadlinklink = link.getDownloadURL().replaceAll("(link|get|stat)", "get");
        link.setUrlDownload(downloadlinklink + "?mode=free");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Файл не найден")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\">Скачать (.*?)</a>").getMatch(0);
        String filesize = br.getRegex("file_size\">(.*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String md5 = br.getRegex("\\(md5\\):</b>(.*?)<br>").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5.trim());
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // Link zum Captcha (kann bei anderen Hostern auch mit ID sein)
        String captchaurl = "http://upload.com.ua/confirm.php";
        String code = getCaptchaCode(captchaurl, downloadLink);
        Form captchaForm = br.getForm(2);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Captcha Usereingabe in die Form einfügen
        captchaForm.put("code", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("Информация о файле")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        // holt sich den dllink und entfernt danach (dllink) bestimmte
        // Zeichen[+Zeilenumbrüche], die reingesetzt wurden um das Downloaden
        // per Downloadmanager zu erschweren
        String dllink = br.getRegex("new Array\\((.*?)\\);").getMatch(0);
        String osfilename = br.getRegex("id=\"os_filename\" value=\"(.*?)\"").getMatch(0);
        if (dllink == null || osfilename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("(,|\"| |\r|\n)", "");
        dllink = "http://dl" + dllink.replace("|", ".upload.com.ua/");
        dllink = dllink + "/" + Encoding.htmlDecode(osfilename);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("503 Service Temporarily Unavailable")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, null, 5 * 60 * 1001l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage("http://upload.com.ua/");
        br.postPage("http://upload.com.ua/#", "submit=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://upload.com.ua", "premium") == null || !br.getCookie("http://upload.com.ua", "premium").equals("1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("Премиум до: <b>(.*?)</b>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(expire.trim(), "dd.MM.yyyy HH:mm:ss", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL().replace("?mode=free", ""));
        String dllink = br.getRegex("Для начала загрузки файла нажмите на кнопку:<br>.*?<a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("или воспользуйтесь предлагаемой ссылкой:<br>.*?<a href=\"(http.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://dl[0-9]+\\.upload\\.com\\.ua/fi1e/.*?/.*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 3;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 8;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
