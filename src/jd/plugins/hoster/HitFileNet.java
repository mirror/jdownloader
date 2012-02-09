//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
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
import jd.plugins.decrypter.LnkCrptWs;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hitfile.net" }, urls = { "http://(www\\.)?hitfile\\.net/(?!download/)[A-Za-z0-9]+" }, flags = { 2 })
public class HitFileNet extends PluginForHost {

    private static final String CAPTCHATEXT    = "hitfile\\.net/captcha/";
    private static final String RECAPTCHATEXT  = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    private static final String WAITTIMEREGEX1 = "limit: (\\d+)";
    private static final String WAITTIMEREGEX2 = "id=\\'timeout\\'>(\\d+)</span>";
    public static final Object  LOCK           = new Object();
    private static final String MAINPAGE       = "http://hitfile.net";
    private static final String ENPAGE         = "http://hitfile.net/lang/en";

    public HitFileNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hitfile.net/premium/emoney/5");
    }

    // Also check TurboBitNet plugin if this one is broken
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        synchronized (LOCK) {
            if (isAborted(link)) { return AvailableStatus.TRUE; }
            /* wait 3 seconds between filechecks, otherwise they'll block our IP */
            Thread.sleep(3000);
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", link.getDownloadURL());
        br.getPage(ENPAGE);
        if (!br.getURL().equals(link.getDownloadURL())) {
            br.getPage(link.getDownloadURL());
        }
        if (br.containsHTML("(<h1>File was not found|It could possibly be deleted\\.)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final Regex fileInfo = br.getRegex("class=\\'file-icon\\d+ [a-z0-9]+\\'></span><span>(.*?)</span>[\n\t\r ]+<span style=\"color: #626262; font\\-weight: bold; font\\-size: 14px;\">\\((.*?)\\)</span>");
        final String filename = fileInfo.getMatch(0);
        final String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".").replace(" ", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("Account: <b>premium</b> \\(<a href=\\'/premium\\'>(.*?)</a>\\)").getMatch(0);
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
    public String getAGBLink() {
        return "http://hitfile.net/rules";
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        final String fileID = new Regex(downloadLink.getDownloadURL(), "hitfile\\.net/(.+)").getMatch(0);
        if (fileID == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage("http://hitfile.net/download/free/" + fileID);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals(downloadLink.getDownloadURL().replace("www.", ""))) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.LF("plugins.hoster.hitfilenet.only4premium", "This file is only available for premium users!")); }
            logger.warning("Unexpected redirect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String longWaittime = br.getRegex(WAITTIMEREGEX1).getMatch(0);
        if (longWaittime == null) {
            longWaittime = br.getRegex(WAITTIMEREGEX2).getMatch(0);
        }
        if (longWaittime != null) {
            if (Integer.parseInt(longWaittime) > 60) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(longWaittime) * 1001l); }
        }
        if (br.containsHTML("(have reached the limit|>From your IP range the limit of connections is reached<)")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED); }
        if (br.containsHTML(RECAPTCHATEXT)) {
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CAPTCHATEXT)) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        } else {
            if (!br.containsHTML(CAPTCHATEXT)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            final String captchaUrl = br.getRegex("<div><img alt=\"Captcha\" src=\"(http://hitfile\\.net/captcha/.*?)\"").getMatch(0);
            final Form captchaForm = br.getForm(2);
            if (captchaForm == null || captchaUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            final String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("captcha_response", code);
            br.submitForm(captchaForm);
            if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CAPTCHATEXT)) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        }
        int waittime = 60;
        final String regexedWaittime = br.getRegex(WAITTIMEREGEX1).getMatch(0);
        if (regexedWaittime != null) {
            waittime = Integer.parseInt(regexedWaittime);
        }
        if (waittime > 250) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", waittime * 1001l); }
        sleep(waittime * 1001l, downloadLink);

        boolean waittimeFail = true;

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String res = parseImageUrl(br.getRegex(LnkCrptWs.IMAGEREGEX(null)).getMatch(0));
        if (res != null) {
            String fReq = res;
            if (!res.startsWith("http")) {
                fReq = MAINPAGE + res;
            }
            for (int i = 0; i <= 4; i++) {
                br.getPage(fReq);
                final String additionalWaittime = br.getRegex(JDHexUtils.toString(LnkCrptWs.IMAGEREGEX("FE8CFBFAFA57CDE31BC2B798DF5141AB2DC171EC0852D89A1A135E3F116C83D05D8BF8328B5AE238254154B5EA27"))).getMatch(0);
                if (additionalWaittime != null) {
                    sleep(Integer.parseInt(additionalWaittime) * 1001l, downloadLink);
                } else {
                    logger.info("No additional waittime found...");
                    waittimeFail = false;
                    break;
                }
                logger.info("Waittime-Try " + (i + 1) + ", waited additional " + additionalWaittime + "seconds");
            }
        }

        if (waittimeFail) {
            logger.warning(br.toString());
            throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL waittime error");
        }
        String dllink = br.getRegex("<br/><h1><a href=\\'(/.*?)\\'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("If the download does not start - <a href=\\'/(.*?)\\'>try again").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\\'(/download/redirect/[a-z0-9]+/[A-Za-z0-9]+/.*?)\\'").getMatch(0);
            }
        }
        if (dllink == null) {
            logger.warning("dllink couldn't be found...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("http")) {
            dllink = MAINPAGE + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<h1><a href=\\'(http://.*?)\\'><b>").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\\'(http://hitfile\\.net//download/redirect/[a-z0-9]+/[A-Za-z0-9]+)\\'").getMatch(0);
        }
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

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(MAINPAGE);
        br.setCookie(MAINPAGE, "user_lang", "en");
        br.postPage("http://hitfile.net/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bmemory%5D=on&user%5Bsubmit%5D=");
        if (br.getCookie(MAINPAGE, "kohanasession") == null && br.getCookie(MAINPAGE, "sid") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (!br.containsHTML("Account: <b>premium</b>")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    private String parseImageUrl(final String fun) {
        if (fun == null) { return null; }
        final String[] next = new Regex(fun, JDHexUtils.toString(LnkCrptWs.IMAGEREGEX(Encoding.Base64Decode("Rjk4QUZFQTVGOTUwQzlFMjE4QzdCMjk1REE1NzQ2RkIyQUM2NzJFQzA5NUNEQjlEMUU0RTVDNkYxMTNFODI4NjVBRDhGODMzOEI1QUU2NjkyMTQwNTBFQUVGNzQxOTIyRTcyNDI5OTFDMUNDNjM2N0E4MjlENkIzNkI1RDMzNkE1RUZFQzk3ODU5NzlGODlFMjVGOTJGRUY1NTNCQzhCMzQyRkM4RjhFNkJBRTk4QTE5Q0RGMkI5NTI5NjU3ODRFQzQyMDNBNUI=")))).getRow(0);
        if (next == null || next.length != 2) {
            return new Regex(fun, JDHexUtils.toString(LnkCrptWs.IMAGEREGEX("F98AFEA5F950C9E218C7B295DA5746FB2AC672EC095CDB9D1E4E5C6F113E82865AD8F8338B5AE669214050EAEF741922E7242991C1CC6367A829D6B36B5D336A5EFEC9785979FFC520A92BEA553BC8E845A6"))).getMatch(0);
        } else {
            Object result = new Object();
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                engine.eval(fun);
                result = ((Double) engine.eval(next[1])).longValue();
            } catch (final Throwable e) {
                return null;
            }
            return next[0] + result.toString();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}