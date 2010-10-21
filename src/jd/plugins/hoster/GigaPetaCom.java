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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigapeta.com" }, urls = { "http://[\\w\\.]*?gigapeta\\.com/dl/\\w+" }, flags = { 2 })
public class GigaPetaCom extends PluginForHost {

    public GigaPetaCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://gigapeta.com/premium/");
    }

    public String getAGBLink() {
        return "http://gigapeta.com/rules/";
    }

    private static int simultanpremium = 1;
    public boolean     nopremium       = false;

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://gigapeta.com", "lang", "us");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("All threads for IP")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.gigapeta.unavailable", "Your IP is already downloading a file"));
        if (br.containsHTML("<div id=\"page_error\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex infos = br.getRegex(Pattern.compile("<img src=\".*\" alt=\"file\" />(.*?)</td>.*?</tr>.*?<tr>.*?<th>.*?</th>.*?<td>(.*?)</td>", Pattern.DOTALL));
        String fileName = infos.getMatch(0);
        String fileSize = infos.getMatch(1);
        if (fileName == null || fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fileName.trim());
        downloadLink.setDownloadSize(Regex.getSize(fileSize.trim()));

        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        String captchaKey = (int) (Math.random() * 100000000) + "";
        String captchaUrl = "http://gigapeta.com/img/captcha.gif?x=" + captchaKey;
        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);
            br.postPage(br.getURL(), "download=&captcha_key=" + captchaKey + "&captcha=" + captchaCode);
            if (br.getRedirectLocation() != null) break;
        }
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("All threads for IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.gigapeta.unavailable", "Your IP is already downloading a file"));
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://gigapeta.com", "lang", "us");
        br.setDebug(true);
        br.postPage("http://gigapeta.com/", "auth_login=" + Encoding.urlEncode(account.getUser()) + "&auth_passwd=" + Encoding.urlEncode(account.getPass()));
        String accType = br.getRegex("You have <b>(.*?)</b> account").getMatch(0);
        if (accType == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (accType.equals("basic")) nopremium = true;
        if (br.getCookie("http://gigapeta.com/", "sess") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!nopremium) simultanpremium = -1;
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
        if (!nopremium) {
            String expire = br.getRegex("You have <b>premium</b> account till(.*?)</p>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(Regex.getMilliSeconds(expire.trim(), "dd.MM.yyyy HH:mm", null));
            }
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Registered (free) User");
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (nopremium) {
            doFree(link);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                Form DLForm = br.getFormBySubmitvalue("Download");
                if (DLForm == null) DLForm = br.getForm(0);
                if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.submitForm(DLForm);
                dllink = br.getRedirectLocation();
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (!(dl.getConnection().isContentDisposition())) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}