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
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshare.in.ua" }, urls = { "http://(www\\.)?fileshare\\.in\\.ua/[0-9]+" }, flags = { 2 })
public class FileShareInUa extends PluginForHost {

    private static final String TEMPORARYUNAVAILABLE = "<b>Файл временно недоступен</b>";

    public FileShareInUa(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fileshare.in.ua/premium.aspx");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("www.fileshare.in.ua/", "fileshare.in.ua/"));
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
        String expires = br.getRegex("обычный премиум</a> до (.*?)<br>").getMatch(0);
        if (expires == null) {
            account.setValid(false);
            return ai;
        }
        expires = expires.trim();
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "dd.MM.yy", null));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://fileshare.in.ua/about.aspx";
    }

    private Form getFormWhichContainsHTML(String searchFor) {
        Form[] allForms = br.getForms();
        if (allForms == null || allForms.length == 0) return null;
        Form returnform = null;
        for (Form singleForm : allForms) {
            if (singleForm.containsHTML(searchFor)) {
                returnform = singleForm;
                break;
            }
        }
        return returnform;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.FileShareInUa.errors.temporaryunavailable", "This file is temporary unavailable"));
        if (br.containsHTML("Именно эта ссылка битая. Однако, это не значит, что вам нечего тут искать")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        String continueLink = br.getRegex("id=\"popunder_lnk\" href=\"(/.*?)\"").getMatch(0);
        if (continueLink == null) continueLink = br.getRegex("\"(/dl\\d+\\?c=[a-z0-9]+)\"").getMatch(0);
        if (continueLink == null) {
            // Some countries got captchas
            br.postPage(downloadLink.getDownloadURL(), "post2=1");
            if (br.containsHTML("(Файл удален|К сожалению, cсылка на этот файл не больше не действительна и запрашиваемый файл отсутствует на сервер)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getURL().equals(downloadLink.getDownloadURL())) throw new PluginException(LinkStatus.ERROR_FATAL, "Site is broken, please contact the fileshare.in.ua support!");
            String captchapart = br.getRegex("\"(/capture\\.gif\\?m=\\d+)\"").getMatch(0);
            if (captchapart == null) captchapart = br.getRegex("<img height=\"18\" align=\"absmiddle\"  src=\"(/.*?)\"").getMatch(0);
            Form captchaForm = getFormWhichContainsHTML("capture");
            // This is a part of the captcha stuff. They always have one big
            // captcha
            // with random letters (about 10) but only 4 are needed. With this
            // number and an other default number the plugin knows which part of
            // the captcha it needs
            String captchacut = br.getRegex("position: absolute;[\t\n\r ]+left:\\-(\\d+)px;").getMatch(0);
            if (captchapart == null || captchaForm == null || captchacut == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchaurl = "http://fileshare.in.ua" + captchapart;
            File file = this.getLocalCaptchaFile();
            Browser.download(file, br.cloneBrowser().openGetConnection(captchaurl));
            String code = FsIuA.getCode(-Integer.parseInt(captchacut), 100, file);
            captchaForm.put("capture", code);
            br.setFollowRedirects(true);
            captchaForm.remove(null);
            br.submitForm(captchaForm);
            if (br.containsHTML("Цифры введены неверно\\!<")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            Form fastForm = getFormWhichContainsHTML("dl_fast_btn");
            if (fastForm == null) {
                logger.warning("Error after captcha...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.submitForm(fastForm);
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL() + "?gogogo");
            Form lastForm = br.getForm(0);
            if (lastForm == null) {
                logger.warning("Couldn't find last form...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int wait = 60;
            String waittime = br.getRegex("var sec=(\\d+);").getMatch(0);
            if (waittime == null) waittime = br.getRegex("id=\"sec\">(\\d+)</span>").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
            if (lastForm.getAction() == null) lastForm.setAction(downloadLink.getDownloadURL() + "?gogogo");
            br.submitForm(lastForm);
        } else {
            continueLink = "http://fileshare.in.ua" + continueLink;
            br.getPage(continueLink);
        }
        String dllink0 = br.getRegex("href=\"(/get/.*?)\"").getMatch(0);
        if (dllink0 == null) dllink0 = br.getRegex("popunder\\(\\'http://goo\\.gl/[A-Za-z0-9]+\\'\\);\" href=\"(/.*?)\"").getMatch(0);
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
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.FileShareInUa.errors.temporaryunavailable", "This file is temporary unavailable"));
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String getlink = br.getRegex("href=\"(/get/.*?)\"").getMatch(0);
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        getlink = "http://fileshare.in.ua" + getlink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Воcстановление файла\\.\\.\\.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
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
        if (br.getCookie("http://fileshare.in.ua/", "bfileshare_uid") == null || br.getCookie("http://fileshare.in.ua/", "bfileshare_hash") == null || br.getCookie("http://fileshare.in.ua/", "bfileshare_pass") == null || !br.containsHTML("обычный премиум</a> до")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        // Using the link + "?free" we can get to the page with the captcha but
        // atm we do it without captcha
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Файл удален|К сожалению мы не смогли найти файл, который вы запрашиваете|Такой страницы на нашем сайте нет")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>скачать (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"file_name sr_archive\">[\n\r ]+<span>(.*?)</span>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("width=\"156px\" height=\"39px\" alt=\"(.*?) на FileShare\\.in\\.ua\"").getMatch(0);
            }
        }
        String filesize = br.getRegex("Размер: <b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.FileShareInUa.errors.temporaryunavailable", "This file is temporary unavailable"));
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}