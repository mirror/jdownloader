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
import java.util.ArrayList;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
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
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hitfile.net" }, urls = { "http://(www\\.)?hitfile\\.net/(?!download/)[A-Za-z0-9]+" }, flags = { 2 })
public class HitFileNet extends PluginForHost {

    private final String         UA                  = RandomUserAgent.generate();
    private static final String  RECAPTCHATEXT       = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    private static final String  CAPTCHATEXT         = "hitfile\\.net/captcha/";
    private static final String  MAINPAGE            = "http://hitfile.net";
    public static Object         LOCK                = new Object();
    private static final String  BLOCKED             = "Hitfile.net is blocking JDownloader: Please contact the hitfile.net support and complain!";
    private static final boolean ENABLE_CRYPTO_STUFF = false;

    public HitFileNet(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://hitfile.net/premium/emoney/5");
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.setCookie(MAINPAGE, "user_lang", "en");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 49) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links_to_check=");
                for (final DownloadLink dl : links) {
                    sb.append(dl.getDownloadURL());
                    sb.append("%0A");
                }
                br.postPage("http://" + getHost() + "/linkchecker/check", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String linkID = getID(dllink.getDownloadURL());
                    final Regex fileInfo = br.getRegex("<td>" + linkID + "</td>[\t\n\r ]+<td>([^<>/\"]*?)</td>[\t\n\r ]+<td style=\"text\\-align:center;\"><img src=\"/img/icon/(done|error)\\.png\"");
                    if (fileInfo.getMatches() == null || fileInfo.getMatches().length == 0) {
                        dllink.setAvailable(false);
                        logger.warning("Linkchecker broken for " + getHost());
                    } else {
                        if (fileInfo.getMatch(1).equals("error")) {
                            dllink.setAvailable(false);
                        } else {
                            final String name = fileInfo.getMatch(0);
                            dllink.setAvailable(true);
                            dllink.setFinalFileName(Encoding.htmlDecode(name.trim()));
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private String escape(final String s) {
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
        final byte[] org = s.getBytes();
        final StringBuilder sb = new StringBuilder();
        String code;
        for (final byte element : org) {
            sb.append('%');
            code = Integer.toHexString(element);
            code = code.length() % 2 > 0 ? "0" + code : code;
            sb.append(code.substring(code.length() - 2));
        }
        return sb + "";
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

    private String getID(final String downloadlink) {
        return new Regex(downloadlink, getHost() + "/(.+)").getMatch(0);
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
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        requestFileInformation(downloadLink);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        prepareBrowser(UA);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>Please wait, searching file|\\'File not found\\. Probably it was deleted)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final Regex fileInfo = br.getRegex("class=\\'file-icon\\d+ [a-z0-9]+\\'></span><span>(.*?)</span>[\n\t\r ]+<span style=\"color: #626262; font\\-weight: bold; font\\-size: 14px;\">\\((.*?)\\)</span>");
        final String filesize = fileInfo.getMatch(1);
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".").replace(" ", "")));
        }
        String downloadUrl = null, waittime = null;
        final String fileID = new Regex(downloadLink.getDownloadURL(), "hitfile\\.net/(.+)").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        try {
            br.getPage("http://hitfile.net/turbolight?file=" + fileID);
            br.setFollowRedirects(false);
            this.sleep(3 * 1000l, downloadLink);
        } catch (final Throwable e) {
        }
        br.getPage("/download/free/" + fileID);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals(downloadLink.getDownloadURL().replace("www.", ""))) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.LF("plugins.hoster.hitfilenet.only4premium", "This file is only available for premium users!")); }
            logger.warning("Unexpected redirect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (!(br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CAPTCHATEXT))) {
            if (br.containsHTML(hf(0))) {
                waittime = br.getRegex(hf(1)).getMatch(0);
                final int wait = waittime != null ? Integer.parseInt(waittime) : -1;

                if (wait > 31) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                } else if (wait < 0) {
                } else {
                    sleep(wait * 1000l, downloadLink);
                }
            }
            waittime = br.getRegex(hf(1)).getMatch(0);
            if (waittime != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l); }
        }

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
            logger.info("Handling normal captchas");
            final String captchaUrl = br.getRegex("<div><img alt=\"Captcha\" src=\"(http://hitfile\\.net/captcha/.*?)\"").getMatch(0);
            Form captchaForm = br.getForm(2);
            if (captchaForm == null) captchaForm = br.getForm(1);
            if (captchaForm == null) captchaForm = br.getForm(0);
            if (captchaForm == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaForm.remove(null);
            for (int i = 1; i <= 2; i++) {
                String captchaCode;
                if (!getPluginConfig().getBooleanProperty("JAC", false) || i == 2) {
                    captchaCode = getCaptchaCode("hitfile.net.disabled", captchaUrl, downloadLink);
                } else if (captchaUrl.contains("/basic/")) {
                    logger.info("Handling basic captchas");
                    captchaCode = getCaptchaCode("hitfile.net.basic", captchaUrl, downloadLink);
                } else {
                    captchaCode = getCaptchaCode("hitfile.net", captchaUrl, downloadLink);
                }
                captchaForm.put("captcha_response", captchaCode);
                try {
                    br.submitForm(captchaForm);
                } catch (final BrowserException e) {
                    if (br.getRequest().getHttpConnection().getResponseCode() == 500) {
                        // Server- or captcha error
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                if (!br.containsHTML(CAPTCHATEXT)) {
                    try {
                        validateLastChallengeResponse();
                    } catch (final Throwable e) {
                    }
                    break;
                } else {
                    try {
                        invalidateLastChallengeResponse();
                    } catch (final Throwable e) {
                    }
                }
            }
            if (br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        // Ticket Time
        String ttt = parseImageUrl(br.getRegex(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(null)).getMatch(0), true);
        int maxWait = 9999, realWait = 0;
        for (String s : br.getRegex(hf(11)).getColumn(0)) {
            realWait = Integer.parseInt(s);
            if (realWait == 0) continue;
            if (realWait < maxWait) maxWait = realWait;
        }
        int tt = 60;
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
            tt = tt < realWait ? tt : realWait;
            if (tt < 30 || tt > 600) {
                ttt = parseImageUrl(hf(2) + tt + "};" + br.getRegex(hf(3)).getMatch(0), false);
                if (ttt == null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1000l); }
                tt = Integer.parseInt(ttt);
            }
            logger.info(" Waittime detected, waiting " + String.valueOf(tt) + " seconds from now on...");
            if (tt > 250) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", tt * 1001l); }
        }

        boolean waittimeFail = true;

        final Browser tOut = br.cloneBrowser();
        final String to = br.getRegex("(?i)(/\\w+/timeout\\.js\\?\\w+=[^\"\'<>]+)").getMatch(0);
        tOut.getPage(to == null ? "/files/timeout.js?ver=" + JDHash.getMD5(String.valueOf(Math.random())).toUpperCase(Locale.ENGLISH) : to);
        final String fun = escape(tOut.toString());
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");

        // realtime update
        String rtUpdate = getPluginConfig().getStringProperty("rtupdate", null);
        final boolean isUpdateNeeded = getPluginConfig().getBooleanProperty("isUpdateNeeded", false);
        int attemps = getPluginConfig().getIntegerProperty("attemps", 1);

        if (isUpdateNeeded || rtUpdate == null) {
            final Browser rt = new Browser();
            try {
                rtUpdate = rt.getPage("http://update0.jdownloader.org/pluginstuff/tbupdate.js");
                rtUpdate = JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(rtUpdate.split("[\r\n]+")[1]));
                getPluginConfig().setProperty("rtupdate", rtUpdate);
            } catch (Throwable e) {
            }
            getPluginConfig().setProperty("isUpdateNeeded", false);
            getPluginConfig().setProperty("attemps", attemps++);
            getPluginConfig().save();
        }

        String res = rhino("var id = \'" + fileID + "\';@" + fun + "@" + rtUpdate, 666);
        if (res == null || res != null && !res.matches(hf(10))) {
            res = rhino("var id = \'" + fileID + "\';@" + fun + "@" + rtUpdate, 100);
            if (new Regex(res, "/~ID~/").matches()) {
                res = res.replaceAll("/~ID~/", fileID);
            }
        }

        if (res == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (res.matches(hf(10)) && this.ENABLE_CRYPTO_STUFF) {
            sleep(tt * 1001, downloadLink);
            for (int i = 0; i <= 4; i++) {
                br.getPage(res);
                final String additionalWaittime = br.getRegex(hf(1)).getMatch(0);
                if (additionalWaittime != null) {
                    sleep(Integer.parseInt(additionalWaittime) * 1001l, downloadLink);
                } else {
                    logger.info("No additional waittime found...");
                    waittimeFail = false;
                    break;
                }
                logger.info("Waittime-Try " + (i + 1) + ", waited additional " + additionalWaittime + "seconds");
            }
            if (waittimeFail) {
                logger.warning(br.toString());
                throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL waittime error");
            }
        } else {
            final String correctRes = new Regex(res, "(/download/[A-Za-z0-9\\-_]+/[A-Za-z0-9]+)").getMatch(0);
            if (correctRes != null) res = correctRes;
            sleep(tt * 1001, downloadLink);
            br.getPage(res);
        }
        downloadUrl = br.getRegex("<br/><h1><a href=\\'(/.*?)\\'").getMatch(0);
        if (downloadUrl == null) {
            downloadUrl = br.getRegex("\\'(/download/redirect/[^<>\"]*?)\\'").getMatch(0);
            if (downloadUrl == null) {
                downloadUrl = rhino(escape(br.toString()) + "@" + rtUpdate, 999);
            }
        }
        if (downloadUrl == null) {
            getPluginConfig().setProperty("isUpdateNeeded", true);
            getPluginConfig().save();
            logger.warning("dllink couldn't be found...");

            if (attemps > 1) {
                getPluginConfig().setProperty("isUpdateNeeded", false);
                getPluginConfig().setProperty("attemps", 1);
                getPluginConfig().save();
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1000l);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "JAC", JDL.L("plugins.hoster.hitfile.jac", "Enable JAC?")).setDefaultValue(true));
    }

    private String hf(final int i) {
        final String[] s = new String[12];
        s[0] = "fe8cfbfafa57cde31bc2b798df5141ab2dc171ec0852d89a1a135e3f116c83d15d8bf93a";
        s[1] = "fddbfbfafa57cdef1a90b5cedf5647ae2cc572ec0958dd981e125c68156882d65d82f869";
        s[2] = "fdd9fbf2fb05cde71a97b69edf5742f1289470bb0a5bd9c81a1b5e39116c85805982fc6e880ce26a201651b8ea211874e4232d90c59b6462ac28d2b26f0537385fa6";
        s[3] = "f980f8f7fa0acdb21b91b6cbdf5043fc2ac775ea080fd8c71a4f5d68156586d05982fd3e8b5ae33f244555e8eb201d77e12128cbc1c7";
        s[4] = "f980ffa5fa07cdb01a93b6c8de0642ae299571bb0c0ddb9c1a1b5b6f143d84855ddfff6b8b5de66e254553eeea751d72e17e2d98c19a6760af75d6b46b05";
        s[5] = "f980ffa5f951ceb31ec7b3c8da5246fa2ac770bc0b0fdc9c1e13";
        s[6] = "fc8efbf2fb01c9e61bc2b798df5146f82cc075bf0b5fd8c71a4e5f3e153a8781588ff86f890de26a221050eaee701824e4742d9cc1c66238a973";
        s[7] = "fddefaf6fb07";
        s[8] = "fe8cfbfafa57cde31bc2b798df5146ad29c071b6080edbca1a135f6f156984d75982fc6e8800e338";
        s[9] = "ff88";
        s[10] = "f9def8a1fa02c9b21ac5b5c9da0746ae2ac671be0c0fd99f194e5b69113a85d65c8bf86e8d00e23d254751eded741d72e7262ecdc19c6267af72d2e26b5e326a59a5ce295d28f89e21ae29ea523acfb545fd8adb";
        s[11] = "f980fea5fa0ac9ef1bc7b694de0142f1289075bd0d0ddb9d1b195a6d103d82865cddff69890ae76a251b53efef711d74e07e299bc098";
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        return JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(s[i]));
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

    private String parseImageUrl(String fun, final boolean NULL) {
        if (fun == null) { return null; }
        if (!NULL) {
            final String[] next = fun.split(hf(9));
            if (next == null || next.length != 2) {
                fun = rhino(fun, 0);
                if (fun == null) { return null; }
                fun = new Regex(fun, hf(4)).getMatch(0);
                return fun == null ? new Regex(fun, hf(5)).getMatch(0) : rhino(fun, 2);
            }
            return rhino(next[1], 1);
        }
        return new Regex(fun, hf(1)).getMatch(0);
    }

    private void prepareBrowser(final String userAgent) {
        // br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept", "text/html, application/xhtml+xml, */*");
        br.getHeaders().put("Accept-Language", "en-EN");
        br.getHeaders().put("User-Agent", userAgent);
        br.getHeaders().put("Referer", null);
    }

    // Also check TurboBitNet plugin if this one is broken
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /** Old linkcheck code can be found in rev 16195 */
        correctDownloadLink(downloadLink);
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private String rhino(final String s, final int b) {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            switch (b) {
            case 0:
                engine.eval(s + hf(6));
                result = engine.get(hf(7));
                break;
            case 1:
                result = ((Double) engine.eval(hf(8))).longValue();
                break;
            case 2:
                engine.eval("var out=\"" + s + "\";");
                result = engine.get("out");
                break;
            case 100:
                String[] code = s.split("@");
                engine.eval(code[0] + "var b = 3;var inn = \'" + code[1] + "\';" + code[2]);
                result = engine.get("out");
                break;
            case 666:
                code = s.split("@");
                engine.eval(code[0] + "var b = 1;var inn = \'" + code[1] + "\';" + code[2]);
                result = engine.get("out");
                break;
            case 999:
                code = s.split("@");
                engine.eval("var b = 2;var inn = \'" + code[0] + "\';" + code[1]);
                result = engine.get("out");
                break;
            }
        } catch (final Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}