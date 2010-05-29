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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.captcha.specials.FsIuA;
import jd.http.Browser;
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

    @Override
    public String getAGBLink() {
        return "http://fileshare.in.ua/about.aspx";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://fileshare.in.ua");
        Form form = br.getFormbyKey("auto_login");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("email", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        br.setFollowRedirects(true);
        br.submitForm(form);
        if (!br.containsHTML("\">Премиум</a> до") && !br.containsHTML("Тип премиума: <b style=\"color: black;\">обычный</b>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String expires = br.getRegex("Премиум</a> до(.*?)<br>").getMatch(0);
        if (expires == null) {
            account.setValid(false);
            return ai;
        }
        expires = expires.trim();
        ai.setValidUntil(Regex.getMilliSeconds(expires, "dd.MM.yy", null));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String getlink = br.getRegex("href=\"(/get/.*?)\"").getMatch(0);
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        getlink = "http://fileshare.in.ua" + getlink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getlink, true, 0);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("Воcстановление файла...")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Using the link + "?free" we can get to the page with the captcha but
        // atm we do it without captcha
        // String freepage = link.getDownloadURL() + "?free";
        String freepage = link.getDownloadURL();
        br.getPage(freepage);
        if (br.containsHTML("Такой страницы на нашем сайте нет")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>скачать (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"file_name sr_archive\">[\n\r ]+<span>(.*?)</span>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("width=\"156px\" height=\"39px\" alt=\"(.*?) на FileShare\\.in\\.ua\"").getMatch(0);
            }
        }
        String filesize = br.getRegex("class=\"dnld_size\">.*?<strong>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setFinalFileName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Именно эта ссылка битая. Однако, это не значит, что вам нечего тут искать")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        String continueLink = br.getRegex("id=\"popunder_lnk\" href=\"(/.*?)\"").getMatch(0);
        if (continueLink == null) continueLink = br.getRegex("\"(/dl\\d+\\?c=[a-z0-9]+)\"").getMatch(0);
        if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        continueLink = "http://fileshare.in.ua" + continueLink;
        br.getPage(continueLink);
        String captchapart = br.getRegex("id=\"capture\".*?src=\"(.*?)\"").getMatch(0);
        Form captchaForm = br.getForm(2);
        // This is a part of the captcha stuff. They always have one big captcha
        // with random letters (about 10) but only 4 are needed. With this
        // number and an other default number the plugin knows which part of the
        // captcha it needsa
        String captchacut = br.getRegex("<style>.*?margin-[a-z]+:-(\\d+)px;").getMatch(0);
        if (captchapart != null && captchaForm != null && captchacut != null) {
            String captchaurl = "http://fileshare.in.ua" + captchapart;
            File file = this.getLocalCaptchaFile();
            Browser.download(file, br.cloneBrowser().openGetConnection(captchaurl));
            String code = FsIuA.getCode(-Integer.parseInt(captchacut), 100, file);
            captchaForm.put("capture", code);
            br.submitForm(captchaForm);
            if (br.containsHTML("Цифры введены неверно")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dlframe = downloadLink.getDownloadURL() + "?fr";

        br.getPage(dlframe);
        String dllink0 = br.getRegex("href=\"(/get/.*?)\"").getMatch(0);
        if (dllink0 == null) dllink0 = br.getRegex("<span id=\"time_yes\"><a  href=\"(/.*?)\"").getMatch(0);
        if (dllink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://fileshare.in.ua" + dllink0);
        String dllink = br.getRedirectLocation();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("временно недоступен")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            if (br.containsHTML("Воcстановление файла...")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}