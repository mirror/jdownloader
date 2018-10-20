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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.RandomUserAgent;
import jd.http.Request;
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
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hitfile.net" }, urls = { "https?://(www\\.)?hitfile\\.net/(download/free/)?(?!abuse|faq|files|impressum|linkchecker|premium|reseller|rules|rulesdownload|favicon|locale|login|reg|upload)[A-Za-z0-9]+" })
public class HitFileNet extends antiDDoSForHost {
    /* Settings */
    private static final String  SETTING_JAC                          = "SETTING_JAC";
    private static final String  SETTING_FREE_PARALLEL_DOWNLOADSTARTS = "SETTING_FREE_PARALLEL_DOWNLOADSTARTS";
    private static final int     FREE_MAXDOWNLOADS_PLUGINSETTING      = 20;
    private final String         UA                                   = RandomUserAgent.generate();
    private static final String  HTML_RECAPTCHATEXT                   = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    private static final String  HTML_CAPTCHATEXT                     = "hitfile\\.net/captcha/";
    /* Website will say something like "Searching file..." which means that it is offline. */
    public static final String   HTML_FILE_OFFLINE                    = "class=\"code\\-404\"|<h1>Searching for the file\\.\\.\\.Please wait… </h1>";
    private static final String  MAINPAGE                             = "https://hitfile.net";
    public static Object         LOCK                                 = new Object();
    private static final String  BLOCKED                              = "Hitfile.net is blocking JDownloader: Please contact the hitfile.net support and complain!";
    private static final boolean ENABLE_CRYPTO_STUFF                  = false;
    /* Connection stuff */
    private static final boolean FREE_RESUME                          = true;
    private static final int     FREE_MAXCHUNKS                       = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME               = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS            = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS         = 20;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload         = new AtomicInteger(FREE_MAXDOWNLOADS_PLUGINSETTING);
    /* don't touch the following! */
    private static AtomicInteger maxFree                              = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    public HitFileNet(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://hitfile.net/premium/emoney/5");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        if (link.getSetLinkID() == null) {
            link.setLinkID(getHost() + "://" + fid);
        }
        link.setUrlDownload(MAINPAGE + "/" + fid);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        login(account);
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
        return "https://hitfile.net/rules";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (this.getPluginConfig().getBooleanProperty(SETTING_FREE_PARALLEL_DOWNLOADSTARTS, false)) {
            return FREE_MAXDOWNLOADS_PLUGINSETTING;
        } else {
            return maxFree.get();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        br = new Browser();
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        requestFileInformation(downloadLink);
        setBrowserExclusive();
        br = new Browser();
        dupe.clear();
        prepareBrowser(UA);
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        simulateBrowser();
        if (br.containsHTML("'File not found\\. Probably it was deleted") || br.containsHTML(HTML_FILE_OFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fileInfo = br.getRegex("class='file-icon\\d+ [a-z0-9]+'></span><span>(.*?)</span>[\n\t\r ]+<span style=\"color: #626262; font-weight: bold; font\\-size: 14px;\">\\((.*?)\\)</span>");
        String filesize = fileInfo.getMatch(1);
        if (filesize != null) {
            filesize = filesize.replace("б", "b");
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".").replace(" ", "")));
        }
        String downloadUrl = null, waittime = null;
        final String fileID = new Regex(downloadLink.getDownloadURL(), "hitfile\\.net/(.+)").getMatch(0);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage("/download/free/" + fileID);
        if (br.getHttpConnection().getCompleteContentLength() < 200) {
            final String redirect = br.getRegex("window\\.location\\.href\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
            if (redirect != null) {
                getPage(redirect);
            }
        }
        simulateBrowser();
        if (br.containsHTML(HTML_FILE_OFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (StringUtils.equalsIgnoreCase(br.getRedirectLocation(), downloadLink.getDownloadURL().replace("www.", "")) || br.containsHTML("<div class=\"free-limit-note\">\\s*Limit reached for free download of this file\\.")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        if (!(br.containsHTML(HTML_RECAPTCHATEXT) || br.containsHTML(HTML_CAPTCHATEXT))) {
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
            if (waittime != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l);
            }
        }
        Form captchaform = br.getForm(2);
        if (captchaform == null) {
            captchaform = br.getForm(1);
        }
        if (captchaform == null) {
            captchaform = br.getForm(0);
        }
        if (captchaform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML(HTML_RECAPTCHATEXT)) {
            final Recaptcha rc = new Recaptcha(br, this);
            rc.parse();
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML(HTML_RECAPTCHATEXT) || br.containsHTML(HTML_CAPTCHATEXT)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        } else if (br.containsHTML("class=\"g-recaptcha\"")) {
            /* ReCaptchaV2 */
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            captchaform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            captchaform.put("captcha_ajax", "true");
            captchaform.put("captcha_type", "recaptcha2");
            captchaform.put("captcha_subtype", "");
            br.submitForm(captchaform);
        } else {
            if (!br.containsHTML(HTML_CAPTCHATEXT)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Handling normal captchas");
            final String captchaUrl = br.getRegex("<div><img alt=\"Captcha\" src=\"(https?://(?:\\w+\\.)?hitfile\\.net/captcha/.*?)\"").getMatch(0);
            if (captchaUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            captchaform.remove(null);
            if (StringUtils.equalsIgnoreCase(captchaform.getAction(), "#")) {
                captchaform.setAction(br.getURL());
            }
            final int retry = 3;
            for (int i = 0; i < retry; i++) {
                String captchaCode;
                if (!getPluginConfig().getBooleanProperty(SETTING_JAC, false) || i >= 1) {
                    captchaCode = getCaptchaCode("hitfile.net.disabled", captchaUrl, downloadLink);
                } else if (captchaUrl.contains("/basic/")) {
                    logger.info("Handling basic captchas");
                    captchaCode = getCaptchaCode("hitfile.net.basic", captchaUrl, downloadLink);
                } else {
                    captchaCode = getCaptchaCode("hitfile.net", captchaUrl, downloadLink);
                }
                captchaform.put("captcha_response", Encoding.urlEncode(captchaCode));
                final Browser br = this.br.cloneBrowser();
                try {
                    br.submitForm(captchaform);
                } catch (final BrowserException e) {
                    if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 500) {
                        // Server- or captcha error
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500: Server has server issues and/or user entered wrong captcha", 5 * 60 * 1000l);
                    }
                }
                if (br.containsHTML(HTML_RECAPTCHATEXT)) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else if (br.containsHTML(HTML_CAPTCHATEXT)) {
                    if (i + 1 == retry) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    continue;
                }
                this.br = br;
                break;
            }
        }
        simulateBrowser();
        if (br.containsHTML(">You have reached the limit of connections<")) {
            final String timeout = br.getRegex("id\\s*=\\s*'timeout'\\s*>\\s*(\\d+)\\s*<").getMatch(0);
            if (timeout != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(timeout) * 1000);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
        }
        // Ticket Time
        String ttt = parseImageUrl(br.getRegex(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(null)).getMatch(0), true);
        int maxWait = 9999, realWait = 0;
        for (String s : br.getRegex(hf(11)).getColumn(0)) {
            realWait = Integer.parseInt(s);
            if (realWait == 0) {
                continue;
            }
            if (realWait < maxWait) {
                maxWait = realWait;
            }
        }
        int tt = jd.plugins.hoster.TurboBitNet.getPreDownloadWaittime(this.br, 40);
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
            tt = tt < realWait ? tt : realWait;
            if (tt < 30 || tt > 600) {
                ttt = parseImageUrl(hf(2) + tt + "};" + br.getRegex(hf(3)).getMatch(0), false);
                if (ttt == null) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1000l);
                }
                tt = Integer.parseInt(ttt);
            }
            logger.info(" Waittime detected, waiting " + String.valueOf(tt) + " seconds from now on...");
            if (tt > 250) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", tt * 1001l);
            }
        }
        boolean waittimeFail = true;
        String res = "/download/getLinkTimeout/" + fileID;
        // final Browser tOut = br.cloneBrowser();
        // final String to = br.getRegex("(?i)(/\\w+/timeout\\.js\\?\\w+=[^\"\'<>]+)").getMatch(0);
        // tOut.getPage(to == null ? "/files/timeout.js?ver=" + JDHash.getMD5(String.valueOf(Math.random())).toUpperCase(Locale.ENGLISH)
        // :
        // to);
        // final String fun = escape(tOut.toString());
        // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        //
        // // realtime update
        // String rtUpdate = getPluginConfig().getStringProperty("rtupdate", null);
        // final boolean isUpdateNeeded = getPluginConfig().getBooleanProperty("isUpdateNeeded", false);
        int attemps = getPluginConfig().getIntegerProperty("attemps", 1);
        //
        // if (isUpdateNeeded || rtUpdate == null) {
        // final Browser rt = new Browser();
        // try {
        // rtUpdate = rt.getPage("http://update0.jdownloader.org/pluginstuff/tbupdate.js");
        // rtUpdate = JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(rtUpdate.split("[\r\n]+")[1]));
        // getPluginConfig().setProperty("rtupdate", rtUpdate);
        // } catch (Throwable e) {
        // }
        // getPluginConfig().setProperty("isUpdateNeeded", false);
        // getPluginConfig().setProperty("attemps", attemps++);
        // getPluginConfig().save();
        // }
        //
        // String res = rhino("var id = \'" + fileID + "\';@" + fun + "@" + rtUpdate, 666);
        // if (res == null || res != null && !res.matches(hf(10))) {
        // res = rhino("var id = \'" + fileID + "\';@" + fun + "@" + rtUpdate, 100);
        // if (new Regex(res, "/~ID~/").matches()) {
        // res = res.replaceAll("/~ID~/", fileID);
        // }
        // }
        //
        // if (res == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        final Browser br2 = br.cloneBrowser();
        if (res.matches(hf(10)) && this.ENABLE_CRYPTO_STUFF) {
            sleep(tt * 1001, downloadLink);
            for (int i = 0; i <= 4; i++) {
                getPage(br2, res);
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
            if (correctRes != null) {
                res = correctRes;
            }
            sleep(tt * 1001, downloadLink);
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage(br2, res);
        }
        downloadUrl = br2.getRegex("<br/><h1><a href=\\'(/.*?)\\'").getMatch(0);
        if (downloadUrl == null) {
            downloadUrl = br2.getRegex("\\'(/download/redirect/[^<>\"]*?)\\'").getMatch(0);
            if (downloadUrl == null) {
                if (br2.toString().matches("(?i)Error\\s*:\\s*\\d+")) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                // downloadUrl = rhino(escape(br.toString()) + "@" + rtUpdate, 999);
            }
        }
        if (downloadUrl == null) {
            getPluginConfig().setProperty("isUpdateNeeded", true);
            getPluginConfig().save();
            logger.warning("dllink couldn't be found...");
            if (attemps > 1) {
                // getPluginConfig().setProperty("isUpdateNeeded", false);
                // getPluginConfig().setProperty("attemps", 1);
                // getPluginConfig().save();
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        // md5 crapola
        br.setFollowRedirects(false);
        // Future redirects at this point! We want to catch them and not process in order to get the MD5sum! example url structure
        // http://s\\d{2}.turbobit.ru:\\d+/download.php?name=FILENAME.FILEEXTENTION&md5=793379e72eef01ed1fa3fec91eff5394&fid=b5w4jikojflm&uid=free&speed=59&till=1356198536&trycount=1&ip=YOURIP&sid=60193f81464cca228e7bb240a0c39130&browser=201c88fd294e46f9424f724b0d1a11ff&did=800927001&sign=7c2e5d7b344b4a205c71c18c923f96ab
        getPage(downloadUrl);
        downloadUrl = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getURL();
        final String md5sum = new Regex(downloadUrl, "md5=([a-f0-9]{32})").getMatch(0);
        if (md5sum != null) {
            downloadLink.setMD5Hash(md5sum);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1000l);
        }
        try {
            /* add a download slot */
            controlFree(+1);
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlFree(-1);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        getPage(link.getDownloadURL());
        String dllink = br.getRegex("<h1><a href='(https?://.*?)'><b>").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("('|\")(https?://hitfile\\.net//download/redirect/[a-z0-9]+/[A-Za-z0-9]+)\\1").getMatch(1);
        }
        if (dllink == null) {
            if (br.containsHTML("You have reached the.*? limit of premium downloads")) {
                /*
                 * "You have reached the <a href='/user/messages'>monthly</a> limit of premium downloads<"
                 */
                // they also show extra messages that are unread, which show how long its blocked for...
                /*
                 * <div class='unread-messages-text'>Your Premium access to downloads is temporarily blocked.<br/>Reason: You have reached
                 * the limit of your monthly traffic quota (300 GB)<br/>Blocking end time: 2015-10-19 17:27:48<br/></div>
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Out of premium traffic. Please open hoster messages for more about this.", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.containsHTML("'>Premium access is blocked<")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // md5 crapola
        br.setFollowRedirects(false);
        // Future redirects at this point! We want to catch them and not process in order to get the MD5sum! example url structure
        // http://s\\d{2}.turbobit.ru:\\d+/download.php?name=FILENAME.FILEEXTENTION&md5=793379e72eef01ed1fa3fec91eff5394&fid=b5w4jikojflm&uid=free&speed=59&till=1356198536&trycount=1&ip=YOURIP&sid=60193f81464cca228e7bb240a0c39130&browser=201c88fd294e46f9424f724b0d1a11ff&did=800927001&sign=7c2e5d7b344b4a205c71c18c923f96ab
        getPage(dllink);
        dllink = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getURL();
        final String md5sum = new Regex(dllink, "md5=([a-f0-9]{32})").getMatch(0);
        if (md5sum != null) {
            link.setMD5Hash(md5sum);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
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

    private void setConfigElements() {
        String lang_jac = null;
        String lang_free_dlstarts = null;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            lang_jac = "Aktiviere JAC Captchaerkennung?";
            lang_free_dlstarts = "Aktiviere parallele Downloadstarts im free Download Modus?\r\n<html><p style=\"color:#F62817\"><b>Warnung: Diese Einstellung kann zu unnötig vielen nicht akzeptierten Captchaeingaben führen!</b></p></html>";
        } else {
            lang_jac = "Activate JAC captcha recognition?";
            lang_free_dlstarts = "Activate parallel downloadstarts in free mode?\r\n<html><p style=\"color:#F62817\"><b>Warning: This setting can lead to a lot of non-accepted captcha popups!</b></p></html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_JAC, JDL.L("plugins.hoster.hitfile.jac", lang_jac)).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_FREE_PARALLEL_DOWNLOADSTARTS, JDL.L("plugins.hoster.hitfile.simultan_Free_downloadstarts", lang_free_dlstarts)).setDefaultValue(false));
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
        return JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(s[i]));
    }

    /* TODO: Make an unique login function which works for turbobit.net AND hitfile.net (same system) */
    private void login(final Account account) throws Exception {
        /* Load sister site plugin */
        setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(MAINPAGE);
        br.setCookie(MAINPAGE, "user_lang", "en");
        postPage("https://hitfile.net/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bmemory%5D=on&user%5Bsubmit%5D=");
        jd.plugins.hoster.TurboBitNet.universalLoginErrorhandling(this.br);
        if (!"1".equals(br.getCookie(MAINPAGE, "user_isloggedin"))) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.\r\n3. Gehe auf folgende Seite und deaktiviere, den Login Captcha Schutz deines Accounts und versuche es erneut: hitfile.net/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.\r\n3. Access the following site and disable the login captcha protection of your account and try again: hitfile.net/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (!br.containsHTML("Account: <b>premium</b>")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private String parseImageUrl(String fun, final boolean NULL) {
        if (fun == null) {
            return null;
        }
        if (!NULL) {
            final String[] next = fun.split(hf(9));
            if (next == null || next.length != 2) {
                fun = rhino(fun, 0);
                if (fun == null) {
                    return null;
                }
                fun = new Regex(fun, hf(4)).getMatch(0);
                return fun == null ? new Regex(fun, hf(5)).getMatch(0) : rhino(fun, 2);
            }
            return rhino(next[1], 1);
        }
        return new Regex(fun, hf(1)).getMatch(0);
    }

    private void prepareBrowser(final String userAgent) {
        // br.getHeaders().put("Pragma", null);
        br.setCookie(MAINPAGE, "user_lang", "en");
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("User-Agent", userAgent);
        br.getHeaders().put("Referer", null);
    }

    // Also check TurboBitNet plugin if this one is broken
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /** Old linkcheck code can be found in rev 16195 */
        correctDownloadLink(downloadLink);
        br.setCookie(MAINPAGE, "user_lang", "en");
        br.setCookiesExclusive(true);
        getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("File was deleted or not found|>This document was not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("You download: \\&nbsp; <span class=\\'[a-z0-9\\- ]+\\'></span><span>([^<>\"]*?)</span>").getMatch(0);
        final String filesize = br.getRegex("\\((\\d+(,\\d+)? (b|Kb|Mb|Gb))\\)").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        downloadLink.setName(filename);
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
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
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

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    private synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
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

    private LinkedHashSet<String> dupe = new LinkedHashSet<String>();

    private void simulateBrowser() throws InterruptedException {
        // dupe.clear();
        final AtomicInteger requestQ = new AtomicInteger(0);
        final AtomicInteger requestS = new AtomicInteger(0);
        final ArrayList<String> links = new ArrayList<String>();
        String[] l1 = new Regex(br, "\\s+(?:src)=(\"|')(.*?)\\1").getColumn(1);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        l1 = new Regex(br, "\\s+(?:src)=(?!\"|')([^\\s]+)").getColumn(0);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        for (final String link : links) {
            // lets only add links related to this hoster.
            final String correctedLink = Request.getLocation(link, br.getRequest());
            if (this.getHost().equals(Browser.getHost(correctedLink)) && !correctedLink.endsWith(this.getHost() + "/") && !correctedLink.contains(".html") && !correctedLink.equals(br.getURL()) && !correctedLink.contains("/captcha/") && !correctedLink.contains("'")) {
                if (dupe.add(correctedLink)) {
                    final Thread simulate = new Thread("SimulateBrowser") {
                        public void run() {
                            final Browser rb = br.cloneBrowser();
                            rb.getHeaders().put("Cache-Control", null);
                            // open get connection for images, need to confirm
                            if (correctedLink.matches(".+\\.png.*")) {
                                rb.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                            } else if (correctedLink.matches(".+\\.js.*")) {
                                rb.getHeaders().put("Accept", "*/*");
                            } else if (correctedLink.matches(".+\\.css.*")) {
                                rb.getHeaders().put("Accept", "text/css,*/*;q=0.1");
                            }
                            URLConnectionAdapter con = null;
                            try {
                                requestQ.getAndIncrement();
                                con = rb.openGetConnection(correctedLink);
                            } catch (final Exception e) {
                            } finally {
                                try {
                                    con.disconnect();
                                } catch (final Exception e) {
                                }
                                requestS.getAndIncrement();
                            }
                            return;
                        }
                    };
                    simulate.start();
                    Thread.sleep(100);
                }
            }
        }
        while (requestQ.get() != requestS.get()) {
            Thread.sleep(1000);
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Turbobit_Turbobit;
    }
}