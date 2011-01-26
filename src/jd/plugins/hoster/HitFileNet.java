//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hitfile.net" }, urls = { "http://[\\w\\.]*?hitfile\\.net/[A-Za-z0-9]+" }, flags = { 2 })
public class HitFileNet extends PluginForHost {

    public HitFileNet(PluginWrapper wrapper) {
        super(wrapper);
        // Use this if we ever have a premium implementation for this host
        // this.setStartIntervall(3000l);
        this.enablePremium("http://hitfile.net/premium/emoney/5");
    }

    @Override
    public String getAGBLink() {
        return "http://hitfile.net/rules";
    }

    private static final String CAPTCHATEXT    = "hitfile\\.net/captcha/";
    private static final String RECAPTCHATEXT  = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    private static final String WAITTIMEREGEX1 = "limit: (\\d+)";
    private static final String WAITTIMEREGEX2 = "id=\\'timeout\\'>(\\d+)</span>";
    public static final Object  LOCK           = new Object();
    private static final String MAINPAGE       = "http://hitfile.net/";
    private static final String ENPAGE         = "http://hitfile.net/en";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException, InterruptedException {
        synchronized (LOCK) {
            if (this.isAborted(link)) return AvailableStatus.TRUE;
            /* wait 3 seconds between filechecks, otherwise they'll block our IP */
            Thread.sleep(3000);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", link.getDownloadURL());
        br.getPage(ENPAGE);
        if (!br.getURL().equals(link.getDownloadURL())) br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h1>File was not found|It could possibly be deleted\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("class=\\'file-icon\\d+ [a-z0-9]+\\'></span><span>(.*?)</span>[\n\t\r ]+<span style=\"color: #626262; font-weight: bold; font-size: 14px;\">\\((.*?)\\)</span>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String fileID = new Regex(downloadLink.getDownloadURL(), "hitfile\\.net/(.+)").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://hitfile.net/download/free/" + fileID);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals(downloadLink.getDownloadURL().replace("www.", ""))) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.LF("plugins.hoster.hitfilenet.only4premium", "This file is only available for premium users!"));
            logger.warning("Unexpected redirect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("have reached the limit")) {
            String longWaittime = br.getRegex(WAITTIMEREGEX1).getMatch(0);
            if (longWaittime == null) longWaittime = br.getRegex(WAITTIMEREGEX2).getMatch(0);
            if (longWaittime != null) {
                if (Integer.parseInt(longWaittime) < 31) {
                    sleep(Integer.parseInt(longWaittime) * 1000l, downloadLink);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(longWaittime) * 1001l);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
        }
        if (br.containsHTML(RECAPTCHATEXT)) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            if (!br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchaUrl = br.getRegex("<div><img alt=\"Captcha\" src=\"(http://hitfile\\.net/captcha/.*?)\"").getMatch(0);
            Form captchaForm = br.getForm(2);
            if (captchaForm == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("captcha_response", code);
            br.submitForm(captchaForm);
            if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        int waittime = 60;
        String regexedWaittime = br.getRegex("limit: (\\d+)").getMatch(0);
        if (regexedWaittime != null) waittime = Integer.parseInt(regexedWaittime);
        sleep(waittime * 1001l, downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://hitfile.net/download/timeout/" + fileID);
        String dllink = br.getRegex("<br/><h1><a href=\\'(/.*?)\\'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("If the download does not start - <a href=\\'/(.*?)\\'>try again").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\\'(/download/redirect/[a-z0-9]+/[A-Za-z0-9]+/.*?)\\'").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://hitfile.net" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(ENPAGE);
        br.postPage("http://hitfile.net/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bmemory%5D=on&user%5Bsubmit%5D=");
        if (br.getCookie(MAINPAGE, "kohanasession") == null || br.getCookie(MAINPAGE, "sid") == null || !br.containsHTML("Account: <b>premium</b>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String expire = br.getRegex("Account: <b>premium</b> \\(<a href=\\'/premium\\'>(.*?)</a>\\)").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<h1><a href=\\'(http://.*?)\\'><b>").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://hitfile\\.net//download/redirect/[a-z0-9]+/[A-Za-z0-9]+)\\'").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                logger.info("No traffic left...");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("Вы превысили лимит скачки за эти сутки\\.")) {
                logger.info("No traffic left...");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}