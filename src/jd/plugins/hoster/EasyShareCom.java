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
import java.net.MalformedURLException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "easy-share.com" }, urls = { "http://[\\w\\d\\.]*?easy-share\\.com/\\d{6}.*" }, flags = { 2 })
public class EasyShareCom extends PluginForHost {

    private static Boolean longwait = null;

    public EasyShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.easy-share.com/cgi-bin/premium.cgi");
        /* brauche neuen prem account zum einbauen und testen */
    }

    @Override
    public String getAGBLink() {
        return "http://www.easy-share.com/tos.html";
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.easy-share.com", "language", "en");
        br.getPage("http://www.easy-share.com/");
        br.setDebug(true);
        br.postPage("http://www.easy-share.com/accounts/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=1");
        if (br.getCookie("http://www.easy-share.com/", "PREMIUM") == null || br.getCookie("http://www.easy-share.com/", "PREMIUMSTATUS") == null || !br.getCookie("http://www.easy-share.com/", "PREMIUMSTATUS").equalsIgnoreCase("ACTIVE")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private Cookie isExpired(Account account) throws MalformedURLException, PluginException {
        Cookies cookies = br.getCookies("easy-share.com");
        Cookie premstatus = cookies.get("PREMIUMSTATUS");
        if (premstatus == null || !premstatus.getValue().equalsIgnoreCase("ACTIVE")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        return premstatus;
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
        try {
            ai.setValidUntil(isExpired(account).getExpireDate());
        } catch (PluginException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            account.setValid(false);
            ai.setExpired(true);
            return ai;
        }
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.easy-share.com", "language", "en");
        br.getPage(downloadLink.getDownloadURL());
        br.setCookie("http://www.easy-share.com", "language", "en");
        if (br.containsHTML("Requested file is deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(Pattern.compile("You are requesting (.*?)\\(", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex("You are requesting.*? \\((.*?)\\)<").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        String wait = br.getRegex("w='(\\d+)'").getMatch(0);
        int waittime = 0;
        if (wait != null) waittime = Integer.parseInt(wait.trim());
        if (waittime > 90 && (longwait == null || longwait == true)) {
            /* first time >90 secs, it can be we are country with long waittime */
            longwait = true;
            sleep(waittime * 1000l, downloadLink);
        } else {
            if (longwait == null) longwait = false;
            if (waittime > 90 && longwait == false) {
                /*
                 * only request reconnect if we dont have to wait long on every
                 * download
                 */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 1000l);
            } else {
                sleep(waittime * 1000l, downloadLink);
            }
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Please wait or buy a Premium membership")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        Form form = br.getForm(3);
        String captcha = br.getRegex("<img src=\"/(kapt.*?)\"").getMatch(0);
        String captchaUrl = "http://" + br.getHost() + "/" + captcha;
        br.setDebug(true);
        if (captcha != null) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);
            form.put("captcha", captchaCode);
        }
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Invalid characters")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        isExpired(account);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
