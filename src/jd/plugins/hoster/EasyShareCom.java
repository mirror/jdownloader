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

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "easy-share.com" }, urls = { "http://[\\w\\d\\.]*?easy-share\\.com/\\d{6}.*" }, flags = { 2 })
public class EasyShareCom extends PluginForHost {

    private static Boolean longwait = null;

    public EasyShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.easy-share.com/cgi-bin/premium.cgi");
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
        String acc = br.getCookie("http://www.easy-share.com/", "ACCOUNT");
        String prem = br.getCookie("http://www.easy-share.com/", "PREMIUM");
        if (acc == null && prem == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (acc != null && prem == null) {
            /*
             * buggy easyshare server, login does not work always, it needs
             * PREMIUM cookie
             */
            br.setCookie("http://www.easy-share.com/", "PREMIUM", acc);
        }
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
        br.getPage("http://www.easy-share.com/accounts");
        String isPremium = br.getRegex("Premium membership: <.*?>(Active)<").getMatch(0);
        String ends = br.getRegex("Ends:</span>.*?<span>(.*?)<").getMatch(0);
        /* there are 2 different versions of account info pages */
        if (ends == null) ends = br.getRegex("End time:(.*?)<").getMatch(0);
        if (isPremium == null) isPremium = br.getRegex("Premium account: <.*?>(active)<").getMatch(0);
        if (ends == null || isPremium == null) {
            account.setValid(false);
            return ai;
        }
        ai.setValidUntil(Regex.getMilliSeconds(ends.replaceAll(", in", "").trim(), "dd MMM yyyy HH:mm:ss", null));
        String trafficLeft = br.getRegex("Traffic left:(.*?)<").getMatch(0);
        if (trafficLeft != null) {
            /* it seems they have unlimited traffic */
            // ai.setTrafficLeft(Regex.getSize(trafficLeft));
            ai.setUnlimitedTraffic();
        } else {
            ai.setUnlimitedTraffic();
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.easy-share.com", "language", "en");
        URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
        br.setCookie("http://www.easy-share.com", "language", "en");
        if (con.getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.followConnection();
        if (br.containsHTML("Requested file is deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("You need a premium membership to download this file")) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.host.errormsg.only4premium", "Only downloadable for premium users!"));
        String filename = br.getRegex(Pattern.compile("You are requesting:</span>(.*?)<span class=\"txtgray\">.*?\\((.*?)\\)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("You are requesting:</span>(.*?)<span class=\"txtgray\">.*?\\((.*?)\\)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (br.containsHTML("There is another download in progress from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        if (br.containsHTML("You need a premium membership to download this file")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.host.errormsg.only4premium", "Only downloadable for premium users!"));
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

                if (br.getRegex("Recaptcha.create\\(\"(.*?)\"").getMatch(0) == null) {
                    sleep(waittime * 1000l, downloadLink);
                }
            }
        }

        String id = br.getRegex("Recaptcha.create\\(\"(.*?)\"").getMatch(0);
        if (br.containsHTML("Please wait or buy a Premium membership")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);

        if (id == null) br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("There is another download in progress from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        // br = br;
        int tries = 0;
        while (true) {
            tries++;
            id = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            Browser rcBr = br.cloneBrowser();
            /* follow redirect needed as google redirects to another domain */
            rcBr.setFollowRedirects(true);
            rcBr.getPage("http://api.recaptcha.net/challenge?k=" + id);
            String challenge = rcBr.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
            String server = rcBr.getRegex("server.*?:.*?'(.*?)',").getMatch(0);
            if (challenge == null || server == null) {
                logger.severe("Recaptcha Module fails: " + br.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String captchaAddress = server + "image?c=" + challenge;
            File cf = getLocalCaptchaFile();
            Browser.download(cf, rcBr.openGetConnection(captchaAddress));
            Form form = br.getForm(3);
            String code = getCaptchaCode("recaptcha", cf, downloadLink);

            form.put("recaptcha_challenge_field", challenge);
            form.put("recaptcha_response_field", Encoding.urlEncode(code));

            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 1);
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                if (br.containsHTML("There is another download in progress from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
                if (br.containsHTML("Entered code is invalid")) {
                    if (tries <= 5) {
                        continue;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }

                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            break;
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage("http://www.easy-share.com");
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String url = null;
        if (br.getRedirectLocation() == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            url = br.getRedirectLocation();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
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
