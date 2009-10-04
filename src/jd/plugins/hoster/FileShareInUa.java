//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org & pspzockerscene
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
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshare.in.ua" }, urls = { "http://[\\w\\.]*?fileshare\\.in\\.ua/[0-9]+" }, flags = { 2 })
public class FileShareInUa extends PluginForHost {

    public FileShareInUa(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fileshare.in.ua/premium.aspx");
    }

    // @Override
    public String getAGBLink() {
        return "http://fileshare.in.ua/about.aspx";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://fileshare.in.ua");
        Form form = br.getFormbyKey("auto_login");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.put("email", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        br.setFollowRedirects(true);
        br.submitForm(form);
        if (!br.containsHTML("\">Премиум</a> до") && !br.containsHTML("Тип премиума: <b style=\"color: black;\">обычный</b>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String expires = br.getRegex("Премиум</a> до(.*?)<br>").getMatch(0);
        if (expires == null) {
            account.setValid(false);
            return ai;
        }
        expires = expires.trim();
        ai.setValidUntil(Regex.getMilliSeconds(expires, "dd.MM.yy", null));
        account.setValid(true);
        ai.isUnlimitedTraffic();
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String getlink = br.getRegex("href=\"(/get/.*?)\"").getMatch(0);
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        getlink = "http://fileshare.in.ua" + getlink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getlink, true, 0);
        if (!(dl.getConnection().isContentDisposition())){
            br.followConnection();
            if (br.containsHTML("Воcстановление файла...")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return 20;
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        String freepage = link.getDownloadURL() + "?free";
        br.getPage(freepage);
        if (br.containsHTML("Возможно, файл был удален по просьбе владельца авторских прав")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("class=\"dnld_filename\">(.*?)</h1></td>").getMatch(0));
        String filesize = br.getRegex("class=\"dnld_size\">.*?<strong>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        link.setFinalFileName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String freepage = downloadLink.getDownloadURL() + "?free";
        br.getPage(freepage);
        String captchaid = br.getRegex("=\"border: 1px solid #e0e0e0;\" src=\"(.*?)\" width=\"10").getMatch(0);
        if (captchaid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String captchaurl = "http://fileshare.in.ua" + captchaid;
        String code = getCaptchaCode(captchaurl, downloadLink);
        Form captchaForm = br.getForm(2);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        captchaForm.put("capture", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("Цифры введены неверно")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        Form DLForm0 = br.getForm(2);
        if (DLForm0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.submitForm(DLForm0);
        String dlframe = downloadLink.getDownloadURL() + "?fr";
        br.getPage(dlframe);
        String dllink0 = br.getRegex("yandex_bar\"  href=\"(.*?)\" id=\"dl_li").getMatch(0);
        if (dllink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String dllink1 = "http://fileshare.in.ua" + dllink0;
        br.getPage(dllink1);
        String dllink = br.getRedirectLocation();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            System.out.println("MUH");
            br.followConnection();
            if (br.containsHTML("временно недоступен")) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            if (br.containsHTML("Воcстановление файла...")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */
}