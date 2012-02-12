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
import jd.http.RandomUserAgent;
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

    private final static String UA            = RandomUserAgent.generate();

    private static final String RECAPTCHATEXT = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";

    private static final String CAPTCHATEXT   = "hitfile\\.net/captcha/";

    private static final String MAINPAGE      = "http://hitfile.net";

    public static final Object  LOCK          = new Object();

    public HitFileNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hitfile.net/premium/emoney/5");
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
        br.setDebug(true);
        br.setFollowRedirects(false);
        String downloadUrl = null, waittime = null;
        final String fileID = new Regex(downloadLink.getDownloadURL(), "hitfile\\.net/(.+)").getMatch(0);
        if (fileID == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage("/download/free/" + fileID);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals(downloadLink.getDownloadURL().replace("www.", ""))) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.LF("plugins.hoster.hitfilenet.only4premium", "This file is only available for premium users!")); }
            logger.warning("Unexpected redirect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (br.containsHTML(parseImage("FE8CFBFAFA57CDE31BC2B798DF5141AB2DC171EC0852D89A1A135E3F116C83D15D8BF93A"))) {
            waittime = br.getRegex(parseImage("FDDBFBFAFA57CDEF1A90B5CEDF5647AE2CC572EC0958DD981E125C68156882D65D82F869")).getMatch(0);
            final int wait = waittime != null ? Integer.parseInt(waittime) : -1;

            if (wait > 31) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
            } else if (wait < 0) {
            } else {
                sleep(wait * 1000l, downloadLink);
            }
        }
        waittime = br.getRegex(parseImage("FDDBFBFAFA57CDEF1A90B5CEDF5647AE2CC572EC0958DD981E125C68156882D65D82F869")).getMatch(0);
        if (waittime != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l); }

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

        // Ticket Time
        String ttt = parseImageUrl(br.getRegex(LnkCrptWs.IMAGEREGEX(null)).getMatch(0), true);
        int tt = 60;
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
            if (tt < 60 || tt > 600) {
                ttt = parseImageUrl(parseImage("fdd9fbf2fb05cde71a97b69edf5742f1289470bb0a5bd9c81a1b5e39116c85805982fc6e880ce26a201651b8ea211874e4232d90c59b6462ac28d2b26f0537385fa6") + tt + "};" + br.getRegex(parseImage("f980f8f7fa0acdb21b91b6cbdf5043fc2ac775ea080fd8c71a4f5d68156586d05982fd3e8b5ae33f244555e8eb201d77e12128cbc1c7")).getMatch(0), false);
                if (ttt == null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Hitfile.net is blocking JDownloader: Please contact the hitfile.net support and complain!", 10 * 60 * 60 * 1000l); }
                tt = Integer.parseInt(ttt);
            }
            logger.info(" Waittime detected, waiting " + ttt + " seconds from now on...");
        }
        if (tt > 250) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", tt * 1001l); }

        boolean waittimeFail = true;

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String res = parseImageUrl(br.getRegex(LnkCrptWs.IMAGEREGEX(null)).getMatch(0), false);
        if (res != null) {
            sleep(tt * 1001, downloadLink);
            for (int i = 0; i <= 4; i++) {
                br.getPage(res);
                final String additionalWaittime = br.getRegex(parseImage("FE8CFBFAFA57CDE31BC2B798DF5141AB2DC171EC0852D89A1A135E3F116C83D05D8BF8328B5AE238254154B5EA27")).getMatch(0);
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
        downloadUrl = br.getRegex("<br/><h1><a href=\\'(/.*?)\\'").getMatch(0);
        if (downloadUrl == null) {
            downloadUrl = br.getRegex("If the download does not start - <a href=\\'/(.*?)\\'>try again").getMatch(0);
            if (downloadUrl == null) {
                downloadUrl = br.getRegex("\\'(/download/redirect/[a-z0-9]+/[A-Za-z0-9]+/.*?)\\'").getMatch(0);
            }
        }
        if (downloadUrl == null) {
            logger.warning("dllink couldn't be found...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
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

    private String parseImage(final String s) {
        return JDHexUtils.toString(LnkCrptWs.IMAGEREGEX(s));
    }

    private String parseImageUrl(final String fun, final boolean NULL) {
        if (fun == null) { return null; }
        if (!NULL) {
            final String[] next = fun.split(parseImage("ff88"));
            if (next == null || next.length != 2) { return new Regex(fun, parseImage("f98afea5f950c9e218c7b295da5746fb2ac770b80c09dccc19495b32163f82d159d8fc6c8808e238224054b4eb771c70e07f29ccc19f6367a828d6e46a5a32345ea4cc295f7cffc420f3")).getMatch(0); }
            Object result = new Object();
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                engine.eval(next[1]);
                result = ((Double) engine.eval("Timeout.minLimit")).longValue();
            } catch (final Throwable e) {
                return null;
            }
            return result.toString();
        }
        return new Regex(fun, parseImage("FDDCFBFAFA56CFB51B9DB6C9DE5C43FC2AC770BC0D0DDD9F19495E38103A828C5AD8FC3E8C5BE6352540")).getMatch(0);
    }

    private void prepareBrowser(final String userAgent) {
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept", "text/html, application/xhtml+xml, */*");
        br.getHeaders().put("Accept-Language", "en-EN");
        br.getHeaders().put("User-Agent", userAgent);
        br.getHeaders().put("Referer", null);
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
        prepareBrowser(UA);
        br.setCookie(MAINPAGE + "/", "set_user_lang_change", "en");
        br.getPage(link.getDownloadURL());
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}