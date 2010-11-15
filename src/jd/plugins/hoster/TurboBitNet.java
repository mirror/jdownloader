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

import java.io.File;
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
import jd.utils.JDUtilities;

//When adding new domains here also add them to the turbobit.net decrypter (TurboBitNetFolder)
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "http://[\\w\\.]*?(filesmail\\.ru|hotshare\\.biz|bluetooths\\.pp\\.ru|speed-file\\.ru|sharezoid\\.com|turbobit\\.pl|dz-files\\.ru|file\\.alexforum\\.ws|file\\.grad\\.by|file\\.krut-warez\\.ru|filebit\\.org|files\\.best-trainings\\.org\\.ua|files\\.wzor\\.ws|gdefile\\.ru|letitshare\\.ru|mnogofiles\\.com|share\\.uz|sibit\\.net|turbo-bit\\.ru|turbobit\\.net|upload\\.mskvn\\.by|vipbit\\.ru|files\\.prime-speed\\.ru|filestore\\.net\\.ru|turbobit\\.ru|upload\\.dwmedia\\.ru|upload\\.uz|xrfiles\\.ru|unextfiles\\.com|e-flash\\.com\\.ua|turbobax\\.net|zharabit\\.net|download\\.uzhgorod\\.name|trium-club\\.ru)/(.*?\\.html|download/free/[a-z0-9]+)" }, flags = { 2 })
public class TurboBitNet extends PluginForHost {

    public TurboBitNet(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://turbobit.net/turbo");
    }

    @Override
    public String getAGBLink() {
        return "http://turbobit.net/rules";
    }

    public void correctDownloadLink(DownloadLink link) {
        String freeId = new Regex(link.getDownloadURL(), "download/free/([a-z0-9]+)").getMatch(0);
        if (freeId != null) {
            link.setUrlDownload("http://turbobit.net/" + freeId + ".html");
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("(filesmail\\.ru|hotshare\\.biz|bluetooths\\.pp\\.ru|speed-file\\.ru|sharezoid\\.com|turbobit\\.pl|dz-files\\.ru|file\\.alexforum\\.ws|file\\.grad\\.by|file\\.krut-warez\\.ru|filebit\\.org|files\\.best-trainings\\.org\\.ua|files\\.wzor\\.ws|gdefile\\.ru|letitshare\\.ru|mnogofiles\\.com|share\\.uz|sibit\\.net|turbo-bit\\.ru|turbobit\\.net|upload\\.mskvn\\.by|vipbit\\.ru|files\\.prime-speed\\.ru|filestore\\.net\\.ru|turbobit\\.ru|upload\\.dwmedia\\.ru|upload\\.uz|xrfiles\\.ru|unextfiles\\.com|e-flash\\.com\\.ua|turbobax\\.net|zharabit\\.net|download\\.uzhgorod\\.name|trium-club\\.ru)", "turbobit\\.net"));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", downloadLink.getDownloadURL());
        // To get the english version of the page which then usually redirects
        // us to our link again!
        br.getPage("http://turbobit.net/en");
        // Little errorhandling in case there we're on the wrong page!
        if (!br.getURL().equals(downloadLink.getDownloadURL())) br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(<div class=\"code-404\">404</div>|Файл не найден. Возможно он был удален\\.<br|File was not found\\.|It could possibly be deleted\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileName = br.getRegex("<title>[ \t\r\n]+(Download|Datei downloaden) (.*?)\\. Free download without registration from TurboBit\\.net").getMatch(1);
        if (fileName == null) {
            fileName = br.getRegex("<span class='file-icon.*?'>(.*?)</span>").getMatch(0);
        }
        String fileSize = br.getRegex("(File size|Dateiumfang):</b>(.*?)</div>").getMatch(1);
        if (fileName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(fileName.trim());
        if (fileSize != null) {
            fileSize = fileSize.replace("М", "M");
            fileSize = fileSize.replace("к", "k");
            fileSize = fileSize.replace("Г", "g");
            fileSize = fileSize.replace("б", "");
            if (!fileSize.endsWith("b")) fileSize = fileSize + "b";
            downloadLink.setDownloadSize(Regex.getSize(fileSize.trim().replace(",", ".").replace(" ", "")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String id = new Regex(downloadLink.getDownloadURL(), "turbobit\\.net/(.*?)/.*?\\.html").getMatch(0);
        if (id == null) id = new Regex(downloadLink.getDownloadURL(), "turbobit\\.net/(.*?)\\.html").getMatch(0);
        if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://turbobit.net/download/free/" + id);
        if (br.containsHTML("(Попробуйте повторить через|The limit of connection was succeeded for your|Try to repeat after)")) {
            String waittime = br.getRegex("<span id='timeout'>(\\d+)</span></h1>").getMatch(0);
            int wait = 0;
            if (waittime != null) wait = Integer.parseInt(waittime);
            if (wait < 31) {
                sleep(wait * 1000l, downloadLink);
            } else if (wait == 0) {
            } else if (wait > 31) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        }
        String wait2 = br.getRegex("id=\\'timeout\\'>(\\d+)</span>").getMatch(0);
        if (wait2 == null) wait2 = br.getRegex("limit: (\\d+),").getMatch(0);
        if (wait2 != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait2) * 1001l);
        Form captchaform = br.getForm(3);
        if (captchaform == null) {
            logger.warning("captchaform equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("api.recaptcha.net")) {
            logger.info("Handling Re Captcha");
            String theId = new Regex(br.toString(), "challenge\\?k=(.*?)\"").getMatch(0);
            if (theId == null) {
                logger.warning("the id for Re Captcha equals null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(theId);
            rc.setForm(captchaform);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(null, cf, downloadLink);
            rc.getForm().setAction(downloadLink.getDownloadURL() + "#");
            rc.setCode(c);
            if (br.containsHTML("api.recaptcha.net")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            logger.info("Handling normal captchas");
            String captchaUrl = br.getRegex("\"(http://turbobit\\.net/captcha/.*?)\"").getMatch(0);
            if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (int i = 1; i <= 3; i++) {
                String captchaCode = getCaptchaCode(captchaUrl, downloadLink);
                captchaform.put("captcha_response", captchaCode);
                br.submitForm(captchaform);
                if (br.containsHTML("updateTime: function()")) break;
            }
            if (!br.containsHTML("updateTime: function()")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        // Ticket Time
        String ttt = new Regex(br.toString(), "limit: (\\d+),").getMatch(0);
        int tt = 60;
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        sleep(tt * 1001, downloadLink);
        br.getPage("http://turbobit.net/download/timeout/" + id);

        String downloadUrl = br.getRegex("<a href='(.*?)'>").getMatch(0);
        if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://turbobit.net/", "lang", "english");
        br.setCustomCharset("UTF-8");
        br.getPage("http://turbobit.net/en");
        br.postPage("http://www.turbobit.net/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bmemory%5D=on&user%5Bsubmit%5D=Login");
        if (!br.containsHTML("yesturbo")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://turbobit.net/", "sid") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<u>(Turbo Access|Turbo Zugang)</u> to(.*?)<a").getMatch(1);
        if (expire == null) {
            expire = br.getRegex("<u>Турбо доступ</u> до(.*?)<a").getMatch(0);
            if (expire == null) expire = br.getRegex("<img src='/img/icon/yesturbo\\.png'> <u>.{5,20}</u> .{1,5} (.*?) <a href='/turbo'>").getMatch(0);
        }
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(expire.trim(), "dd.MM.yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<h1><a href='(.*?)'>").getMatch(0);
        if (dllink == null) dllink = br.getRegex("('|\")(http://(www\\.)?turbobit\\.net//download/redirect/.*?)('|\")").getMatch(1);
        if (dllink == null) {
            logger.warning("dllink equals null, plugin seems to be broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.contains("turbobit.net")) dllink = "http://turbobit.net" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.warning("dllink doesn't seem to be a file...");
            if (br.containsHTML("<h1>404 Not Found</h1>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}