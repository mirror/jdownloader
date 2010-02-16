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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "http://[\\w\\.]*?(bluetooths.pp.ru|dz-files.ru|file.alexforum.ws|file.grad.by|file.krut-warez.ru|filebit.org|files.best-trainings.org.ua|files.wzor.ws|gdefile.ru|letitshare.ru|mnogofiles.com|share.uz|sibit.net|turbo-bit.ru|turbobit.net|turbobit.ru|upload.mskvn.by|vipbit.ru|files.prime-speed.ru|filestore.net.ru|turbobit.ru|upload.dwmedia.ru|upload.uz|xrfiles.ru)/[a-z0-9]+\\.html" }, flags = { 2 })
public class TurboBitNet extends PluginForHost {

    public TurboBitNet(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://turbobit.net/turbo");
    }

    @Override
    public String getAGBLink() {
        return "http://turbobit.net/rules";
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
        if (!br.getURL().matches(downloadLink.getDownloadURL())) br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<div class=\"code-404\">404</div>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileName = br.getRegex("Download it!(.*?)\\. Free download without registration from").getMatch(0);
        if (fileName == null) {
            fileName = br.getRegex("<span class='file-icon.*?'></span><b><br>(.*?)</b>").getMatch(0);
            if (fileName == null) {
                fileName = br.getRegex("name=\"keywords\" content=\"(.*?),  , download file").getMatch(0);
            }
        }
        String fileSize = br.getRegex("<b>File size:</b>(.*?)</div>").getMatch(0);
        if (fileName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        fileSize = fileSize.replaceAll("М", "M");
        fileSize = fileSize.replaceAll("к", "k");
        if (!fileSize.endsWith("b")) fileSize = fileSize + "b";
        downloadLink.setName(fileName.trim());
        if (fileSize != null) downloadLink.setDownloadSize(Regex.getSize(fileSize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile(".*/(.*?)\\.html")).getMatch(0);
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
        String captchaUrl = br.getRegex("\"(http://turbobit\\.net/captcha/.*?)\"").getMatch(0);
        if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form form = br.getForm(2);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);
            form.put("captcha_response", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("updateTime: function()")) break;
        }
        if (!br.containsHTML("updateTime: function()")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        int tt = Integer.parseInt(br.getRegex("limit: (\\d+),").getMatch(0));
        sleep(tt * 1001l, downloadLink);
        br.getPage("http://turbobit.net/download/timeout/" + id);

        String downloadUrl = br.getRegex("<a href='(.*?)'>").getMatch(0);
        if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://turbobit.net/", "lang", "english");
        br.setCustomCharset("UTF-8");
        br.getPage("http://turbobit.net/en");
        Form loginform = null;
        Form[] allforms = br.getForms();
        for (Form sform : allforms) {
            String form = Encoding.htmlDecode(sform.toString());
            if (form.contains("user[login]")) {
                loginform = sform;
                break;
            }
        }
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put(Encoding.htmlDecode("user[login]"), Encoding.htmlDecode(account.getUser()));
        loginform.put(Encoding.htmlDecode("user[pass]"), Encoding.htmlDecode(account.getPass()));
        br.submitForm(loginform);
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
        String expire = br.getRegex("<u>Turbo Access</u> to(.*?)<a").getMatch(0);
        // For the russian version
        if (expire == null) expire = br.getRegex("<u>Турбо доступ</u> до(.*?)<a").getMatch(0);
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
        String dllink = br.getRegex("<h1><a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://turbobit.net" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
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