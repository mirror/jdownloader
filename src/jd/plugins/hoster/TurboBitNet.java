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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

//When adding new domains here also add them to the turbobit.net decrypter (TurboBitNetFolder)
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "http://(www\\.)?(filesmail\\.ru|hotshare\\.biz|bluetooths\\.pp\\.ru|speed-file\\.ru|sharezoid\\.com|turbobit\\.pl|dz-files\\.ru|file\\.alexforum\\.ws|file\\.grad\\.by|file\\.krut-warez\\.ru|filebit\\.org|files\\.best-trainings\\.org\\.ua|files\\.wzor\\.ws|gdefile\\.ru|letitshare\\.ru|mnogofiles\\.com|share\\.uz|sibit\\.net|turbo-bit\\.ru|turbobit\\.net|upload\\.mskvn\\.by|vipbit\\.ru|files\\.prime-speed\\.ru|filestore\\.net\\.ru|turbobit\\.ru|upload\\.dwmedia\\.ru|upload\\.uz|xrfiles\\.ru|unextfiles\\.com|e-flash\\.com\\.ua|turbobax\\.net|zharabit\\.net|download\\.uzhgorod\\.name|trium-club\\.ru|alfa-files\\.com|turbabit\\.net|filedeluxe\\.com|turbobit\\.name|files\\.uz\\-translations\\.uz|turboblt\\.ru|fo\\.letitbook\\.ru)/(.*?\\.html|download/free/[a-z0-9]+)" }, flags = { 2 })
public class TurboBitNet extends PluginForHost {

    private final static String UA            = RandomUserAgent.generate();

    private static final String RECAPTCHATEXT = "api\\.recaptcha\\.net";

    private static final String CAPTCHAREGEX  = "\"(http://turbobit\\.net/captcha/.*?)\"";

    private static String       MAINPAGE      = "http://turbobit.net";

    public TurboBitNet(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://turbobit.net/turbo");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String freeId = new Regex(link.getDownloadURL(), "download/free/([a-z0-9]+)").getMatch(0);
        if (freeId != null) {
            link.setUrlDownload(MAINPAGE + "/" + freeId + ".html");
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("(filesmail\\.ru|hotshare\\.biz|bluetooths\\.pp\\.ru|speed-file\\.ru|sharezoid\\.com|turbobit\\.pl|dz-files\\.ru|file\\.alexforum\\.ws|file\\.grad\\.by|file\\.krut-warez\\.ru|filebit\\.org|files\\.best-trainings\\.org\\.ua|files\\.wzor\\.ws|gdefile\\.ru|letitshare\\.ru|mnogofiles\\.com|share\\.uz|sibit\\.net|turbo-bit\\.ru|turbobit\\.net|upload\\.mskvn\\.by|vipbit\\.ru|files\\.prime-speed\\.ru|filestore\\.net\\.ru|turbobit\\.ru|upload\\.dwmedia\\.ru|upload\\.uz|xrfiles\\.ru|unextfiles\\.com|e-flash\\.com\\.ua|turbobax\\.net|zharabit\\.net|download\\.uzhgorod\\.name|trium-club\\.ru|alfa-files\\.com|turbabit\\.net|filedeluxe\\.com|turbobit\\.name|files\\.uz\\-translations\\.uz|turboblt\\.ru|fo\\.letitbook\\.ru)", "turbobit\\.net"));
        }
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
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<u>(Turbo Access|Turbo Zugang)</u> to(.*?)<a").getMatch(1);
        if (expire == null) {
            expire = br.getRegex("<u>Турбо доступ</u> до(.*?)<a").getMatch(0);
            if (expire == null) {
                expire = br.getRegex("<img src=\\'/img/icon/yesturbo\\.png\\'> <u>.{5,20}</u> .{1,5} (.*?) <a href=\\'/turbo\\'>").getMatch(0);
            }
        }
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd.MM.yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://turbobit.net/rules";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setDebug(true);
        String downloadUrl = null;
        String id = new Regex(downloadLink.getDownloadURL(), "turbobit\\.net/(.*?)/.*?\\.html").getMatch(0);
        if (id == null) {
            id = new Regex(downloadLink.getDownloadURL(), "turbobit\\.net/(.*?)\\.html").getMatch(0);
        }
        if (id == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage(MAINPAGE + "/download/free/" + id);
        if (br.containsHTML("(Попробуйте повторить через|The limit of connection was succeeded for your|Try to repeat after|the limit of connections is reached)")) {
            String waittime = br.getRegex("<span id=\\'timeout\\'>(\\d+)</span></h1>").getMatch(0);
            if (waittime == null) {
                waittime = br.getRegex("limit: (\\d+),").getMatch(0);
            }
            int wait = 0;
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            if (wait < 31) {
                sleep(wait * 1000l, downloadLink);
            } else if (wait == 0) {
            } else if (wait > 31) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l); }
        }
        String wait2 = br.getRegex("id=\\'timeout\\'>(\\d+)</span>").getMatch(0);
        if (wait2 == null) {
            wait2 = br.getRegex("limit: (\\d+),").getMatch(0);
        }
        if (wait2 != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait2) * 1001l); }
        Form captchaform = null;
        final Form[] allForms = br.getForms();
        if (allForms != null && allForms.length != 0) {
            for (final Form aForm : allForms) {
                if (aForm.containsHTML("captcha")) {
                    captchaform = aForm;
                    break;
                }
            }
        }
        if (captchaform == null) {
            logger.warning("captchaform equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML(RECAPTCHATEXT)) {
            logger.info("Handling Re Captcha");
            final String theId = new Regex(br.toString(), "challenge\\?k=(.*?)\"").getMatch(0);
            if (theId == null) {
                logger.warning("the id for Re Captcha equals null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(theId);
            rc.setForm(captchaform);
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(null, cf, downloadLink);
            rc.getForm().setAction(MAINPAGE + "/download/free/" + id + "#");
            rc.setCode(c);
            if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML("Incorrect, try again")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        } else {
            logger.info("Handling normal captchas");
            final String captchaUrl = br.getRegex(CAPTCHAREGEX).getMatch(0);
            if (captchaUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            for (int i = 1; i <= 3; i++) {
                final String captchaCode = getCaptchaCode("recaptcha", captchaUrl, downloadLink);
                captchaform.put("captcha_response", captchaCode);
                br.submitForm(captchaform);
                if (br.getRegex(CAPTCHAREGEX).getMatch(0) == null) {
                    break;
                }
            }
            if (br.getRegex(CAPTCHAREGEX).getMatch(0) != null || br.containsHTML(RECAPTCHATEXT)) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        }
        // Ticket Time
        final String ttt = new Regex(br.toString(), "limit\\s?:\\s?(\\d+),").getMatch(0);
        int tt = 60;
        if (ttt != null) {
            logger.info(" Waittime detected, waiting " + ttt + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        if (tt > 250) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", tt * 1001l); }
        // IMPORTANT: This is changed most of the time when the plugin is broken
        // maxLimit : 60
        String maxtime = br.getRegex("maxLimit([ ]+)?:([ ]+)?(\\d+)").getMatch(2);
        if (maxtime == null) {
            maxtime = br.getRegex("var Timeout.*?maxLimit: (\\d+)").getMatch(0);
            if (maxtime == null) {
                maxtime = Encoding.Base64Decode("NjA=");
            }
        }

        final String res = parseImageUrl(br.getRegex(LnkCrptWs.IMAGEREGEX).getMatch(0));
        if (res != null) {
            String fReq = res;
            if (!res.startsWith("http")) {
                fReq = MAINPAGE + res;
            }
            sleep(tt * 1001, downloadLink);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage(fReq);
            downloadUrl = br.getRegex("<a href=\\'(.*?)\\'>").getMatch(0);
        }
        if (downloadUrl == null) {
            if (br.containsHTML("Error: ") || res == null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Turbobit.net is blocking JDownloader: Please contact the turbobit.net support and complain!", 10 * 60 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Try to download it once again after")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 20 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<h1><a href=\\'(.*?)\\'>").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(\\'|\")(http://(www\\.)?turbobit\\.net//download/redirect/.*?)(\\'|\")").getMatch(1);
        }
        if (dllink == null) {
            logger.warning("dllink equals null, plugin seems to be broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.contains("turbobit.net")) {
            dllink = MAINPAGE + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                dl.getConnection().disconnect();
                logger.info("No traffic available");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            br.followConnection();
            logger.warning("dllink doesn't seem to be a file...");
            if (br.containsHTML("<h1>404 Not Found</h1>")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error"); }
            if (br.containsHTML("Try to download it once again after")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 20 * 60 * 1000l); }
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
        br.getHeaders().put("User-Agent", UA);
        br.setCookie("http://turbobit.net/", "set_user_lang_change", "en");
        br.setCustomCharset("UTF-8");
        br.getPage(MAINPAGE);
        br.postPage(MAINPAGE + "/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bmemory%5D=on&user%5Bsubmit%5D=Login");
        if (!br.containsHTML("yesturbo")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (br.getCookie(MAINPAGE + "/", "sid") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    private String parseImageUrl(final String fun) {
        if (fun == null) { return null; }
        final String[] next = new Regex(fun, "\"?\\$\\(\"#\\w+\"\\)\\." + new String(new byte[] { 0x6c, 0x6f, 0x61, 0x64 }) + "\\(\"?(.*?)\"?\\s?\\+\\s?(.*?)\\)").getRow(0);
        if (next == null || next.length != 2) { return null; }
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

    // Also check HitFileNet plugin if this one is broken
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", UA);
        br.getHeaders().put("Referer", downloadLink.getDownloadURL());
        br.setCookie(MAINPAGE + "/", "set_user_lang_change", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(<div class=\"code\\-404\">404</div>|Файл не найден\\. Возможно он был удален\\.<br|File( was)? not found\\.|It could possibly be deleted\\.)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String fileName = br.getRegex("<title>[ \t\r\n]+(Download|Datei downloaden) (.*?)\\. Free download without registration from TurboBit\\.net").getMatch(1);
        if (fileName == null) {
            fileName = br.getRegex("<span class=\\'file\\-icon.*?\\'>(.*?)</span>").getMatch(0);
        }
        String fileSize = br.getRegex("(File size|Dateiumfang):</b>(.*?)</div>").getMatch(1);
        if (fileSize == null) {
            fileSize = br.getRegex("<span class=\\'file\\-icon.*?\\'>.*?</span>.*?\\((.*?)\\)").getMatch(0);
        }
        if (fileName == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        downloadLink.setName(fileName.trim());
        if (fileSize != null) {
            fileSize = fileSize.replace("М", "M");
            fileSize = fileSize.replace("к", "k");
            fileSize = fileSize.replace("Г", "g");
            fileSize = fileSize.replace("б", "");
            if (!fileSize.endsWith("b")) {
                fileSize = fileSize + "b";
            }
            downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize.trim().replace(",", ".").replace(" ", "")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}