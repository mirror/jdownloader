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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

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

    private static final String MAINPAGE     = "http://www.easy-share.com/";
    private static final String FILENOTFOUND = "Requested file is deleted";

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "language", "en");
        br.getPage(MAINPAGE);
        br.setDebug(true);
        br.postPage("http://www.easy-share.com/accounts/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=1");
        String acc = br.getCookie(MAINPAGE, "ACCOUNT");
        String prem = br.getCookie(MAINPAGE, "PREMIUM");
        if (acc == null && prem == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (acc != null && prem == null) {
            /*
             * buggy easyshare server, login does not work always, it needs
             * PREMIUM cookie
             */
            br.setCookie(MAINPAGE, "PREMIUM", acc);
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
        ai.setValidUntil(TimeFormatter.getMilliSeconds(ends.replaceAll(", in", "").trim(), "dd MMM yyyy HH:mm:ss", null));
        String trafficLeft = br.getRegex("Traffic left:(.*?)<").getMatch(0);
        if (trafficLeft != null) {
            /* it seems they have unlimited traffic */
            // ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
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
        br.setCookie(MAINPAGE, "language", "en");
        String fileID = new Regex(downloadLink.getDownloadURL(), "easy-share\\.com/(\\d+)").getMatch(0);
        br.getPage("http://api.easy-share.com/files/" + fileID);
        if (!br.containsHTML("ed:status>O<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("(>errorFileNotFound<|>File Not Found<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(Pattern.compile("<title>(?!File info)(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("rel=\"enclosure\" length=\"(\\d+)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize + "b"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        URLConnectionAdapter con = null;
        try {
            br.setCookie(MAINPAGE, "language", "en");
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML(FILENOTFOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("There is another download in progress from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        if (br.containsHTML("You need a premium membership to download this file")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.host.errormsg.only4premium", "Only downloadable for premium users!"));
        String wait = br.getRegex("w=\\'(\\d+)\\'").getMatch(0);
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
            Form form = null;
            Form[] allForms = br.getForms();
            if (allForms == null || allForms.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (Form singleForm : allForms)
                if (singleForm.containsHTML("\"id\"") && !singleForm.containsHTML("lang_select")) {
                    form = singleForm;
                    break;
                }
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            /*
             * another as default cause current stable has easy-captcha method
             * that does not work
             */
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
        br.getPage(MAINPAGE);
        br.setFollowRedirects(false);
        URLConnectionAdapter con = null;
        try {
            br.setCookie(MAINPAGE, "language", "en");
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML(FILENOTFOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String url = br.getRedirectLocation();
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* limited easyshare to max 5 chunks cause too much can create issues */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, -5);
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
