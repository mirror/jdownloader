//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.LnkCrptWs;
import jd.utils.JDHexUtils;
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

    private static final Object LOCK          = new Object();
    private static final String COOKIE_HOST   = "http://turbobit.net";

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
            login(account, true);
        } catch (final PluginException e) {
            if (br.containsHTML("Our service is currently unavailable in your country.")) {
                ai.setStatus("Our service is currently unavailable in your country.");
            }
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<u>(Turbo Access|Turbo Zugang)</u> to(.*?)(<a|</di)").getMatch(1);
        if (expire == null) {
            expire = br.getRegex("<u>Турбо доступ</u> до(.*?)(<a|</di)").getMatch(0);
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
        String downloadUrl = null, waittime = null;
        String id = new Regex(downloadLink.getDownloadURL(), "turbobit\\.net/(.*?)/.*?\\.html").getMatch(0);
        if (id == null) {
            id = new Regex(downloadLink.getDownloadURL(), "turbobit\\.net/(.*?)\\.html").getMatch(0);
        }
        if (id == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage("/download/free/" + id);
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
            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
            rc.getForm().setAction(MAINPAGE + "/download/free/" + id + "#");
            rc.setCode(c);
            if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML("Incorrect, try again")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        } else {
            logger.info("Handling normal captchas");
            final String captchaUrl = br.getRegex(CAPTCHAREGEX).getMatch(0);
            if (captchaUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            for (int i = 1; i <= 3; i++) {
                final String captchaCode = getCaptchaCode(captchaUrl, downloadLink);
                captchaform.put("captcha_response", captchaCode);
                br.submitForm(captchaform);
                if (br.getRegex(CAPTCHAREGEX).getMatch(0) == null) {
                    break;
                }
            }
            if (br.getRegex(CAPTCHAREGEX).getMatch(0) != null || br.containsHTML(RECAPTCHATEXT)) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        }
        // Ticket Time
        String ttt = parseImageUrl(br.getRegex(LnkCrptWs.IMAGEREGEX(null)).getMatch(0), true);
        int tt = 60;
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
            if (tt < 60 || tt > 600) {
                ttt = parseImageUrl(parseImage("fdd9fbf2fb05cde71a97b69edf5742f1289470bb0a5bd9c81a1b5e39116c85805982fc6e880ce26a201651b8ea211874e4232d90c59b6462ac28d2b26f0537385fa6") + tt + "};" + br.getRegex(parseImage("f980f8f7fa0acdb21b91b6cbdf5043fc2ac775ea080fd8c71a4f5d68156586d05982fd3e8b5ae33f244555e8eb201d77e12128cbc1c7")).getMatch(0), false);
                if (ttt == null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Turbobit.net is blocking JDownloader: Please contact the turbobit.net support and complain!", 10 * 60 * 60 * 1000l); }
                tt = Integer.parseInt(ttt);
            }
            logger.info(" Waittime detected, waiting " + ttt + " seconds from now on...");
        }
        if (tt > 250) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", tt * 1001l); }

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String res = parseImageUrl(br.getRegex(LnkCrptWs.IMAGEREGEX(null)).getMatch(0), false);
        res = res == null ? parseImage("f9defbf7fa55cce11bc1b6cede0342f9299075e9085cd8cb1b1e5d68156586d159d9fe3b880fe338211651beec71192fe4232d9cc5986734ad25d6b0") + id : res;
        if (res != null) {
            sleep(tt * 1001, downloadLink);
            br.getPage(res);
            downloadUrl = br.getRegex("<a href=\\'(.*?)\\'>").getMatch(0);
            if (downloadUrl == null) {
                downloadUrl = br.getRegex("\"href\",\\s?\"(.*?)\"").getMatch(0);
            }
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
        login(account, false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<h1><a href=\\'(.*?)\\'>").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(\\'|\")(http://(www\\.)?turbobit\\.net//download/redirect/.*?)(\\'|\")").getMatch(1);
        }
        if (dllink == null) {
            if (br.containsHTML("Our service is currently unavailable in your country.")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Our service is currently unavailable in your country."); }
            logger.warning("dllink equals null, plugin seems to be broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.contains("turbobit.net")) {
            dllink = MAINPAGE + dllink;
        }
        br.setFollowRedirects(true);
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

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                setBrowserExclusive();
                br.getHeaders().put("User-Agent", UA);
                br.setCookie("http://turbobit.net/", "set_user_lang_change", "en");
                br.setCustomCharset("UTF-8");
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.getPage(MAINPAGE);
                br.postPage(MAINPAGE + "/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bmemory%5D=on&user%5Bsubmit%5D=Login");
                if (!br.containsHTML("yesturbo")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                if (br.getCookie(MAINPAGE + "/", "sid") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                // cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    private String parseImage(final String s) {
        return JDHexUtils.toString(LnkCrptWs.IMAGEREGEX(s));
    }

    private String parseImageUrl(final String fun, final boolean NULL) {
        if (fun == null) { return null; }
        if (!NULL) {
            final String[] next = fun.split(parseImage("ff88"));
            if (next == null || next.length != 2) { return new Regex(fun, parseImage("fedbfff7f950c9ee1f9cb29fdf0646ff2d9d74e90c58db9c19495e3c163f82d05adff8688c01e63e204054bbeb7c1c70e7242990c69d6264ac72d2b06f0d363859a4c972582dfe9b20f22eec5239c9b745a68a8f6bae9ffa9c83")).getMatch(2); }
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
        return new Regex(fun, parseImage("ffdbfbfafa57cdef1a90b5cedf5647ae2cc572ec0958dd981e125c68156882d65d82f869")).getMatch(0);
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

    // Also check HitFileNet plugin if this one is broken
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        prepareBrowser(UA);
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
        if (fileName == null) {
            if (br.containsHTML("Our service is currently unavailable in your country.")) {
                downloadLink.getLinkStatus().setStatusText("Our service is currently unavailable in your country.");
                return AvailableStatus.UNCHECKABLE;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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