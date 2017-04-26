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
//
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "https?://(?:www\\.|new\\.|m\\.)?(ifolder\\.com\\.ua|wayupload\\.com|turo-bit\\.net|depositfiles\\.com\\.ua|dlbit\\.net|hotshare\\.biz|mnogofiles\\.com|sibit\\.net|turbobit\\.net|turbobit\\.ru|xrfiles\\.ru|turbabit\\.net|filedeluxe\\.com|filemaster\\.ru|файлообменник\\.рф|turboot\\.ru|kilofile\\.com|twobit\\.ru)/([A-Za-z0-9]+(/[^<>\"/]*?)?\\.html|download/free/[a-z0-9]+|/?download/redirect/[A-Za-z0-9]+/[a-z0-9]+)" })
public class TurboBitNet extends PluginForHost {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "ifolder.com.ua", "wayupload.com", "turo-bit.net", "depositfiles.com.ua", "dlbit.net", "hotshare.biz", "mnogofiles.com", "sibit.net", "turbobit.net", "turbobit.ru", "xrfiles.ru", "turbabit.net", "filedeluxe.com", "filemaster.ru", "файлообменник.рф", "turboot.ru", "kilofile.com", "twobit.ru" };
    }

    /**
     * TODO: Check if we already got errorhandling for this kind of error http://turbobit.net/error/download/dcount/xxxtesst --> "
     *
     * An amount of maximum downloads for this link has been exceeded "
     *
     * When adding new domains here also add them to the turbobit.net decrypter (TurboBitNetFolder)
     *
     */

    /* Settings */
    private static final String SETTING_JAC                           = "SETTING_JAC";
    private static final String SETTING_FREE_PARALLEL_DOWNLOADSTARTS  = "SETTING_FREE_PARALLEL_DOWNLOADSTARTS";
    private static final int    FREE_MAXDOWNLOADS_PLUGINSETTING       = 20;

    private static final String HTML_RECAPTCHAV1                      = "api\\.recaptcha\\.net";
    private final String        CAPTCHAREGEX                          = "\"(https?://(?:\\w+\\.)?turbobit\\.net/captcha/.*?)\"";
    private static final String MAINPAGE                              = "http://turbobit.net";
    private static Object       LOCK                                  = new Object();
    private static final String BLOCKED                               = "Turbobit.net is blocking JDownloader: Please contact the turbobit.net support and complain!";
    private boolean             prefer_single_linkcheck_linkcheckpage = false;
    private final String        premRedirectLinks                     = ".*//?download/redirect/[A-Za-z0-9]+/[a-z0-9]+";
    private static final String NICE_HOST                             = "turbobit.net";
    private static final String NICE_HOSTproperty                     = "turbobitnet";

    public TurboBitNet(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        enablePremium(MAINPAGE + "/turbo/emoney/12");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) throws PluginException {
        // changing to temp hosts (subdomains causes issues with multihosters.
        final String protocol = new Regex(link.getDownloadURL(), "https?://").getMatch(-1);
        final String uid = getFUID(link);
        if (uid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // We don't rename match format because these are generated links. Leave as is!
        if (!link.getDownloadURL().matches(premRedirectLinks)) {
            link.setUrlDownload(protocol + NICE_HOST + "/" + uid + ".html");
            // we wont use linkid for match format either.
            final String linkID = getHost() + "://" + uid;
            link.setLinkID(linkID);
        }
    }

    /** 01.12.14: turbobit.net & hitfile.net Linkchecker is broken - will hopefully be back soon! */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        final ArrayList<DownloadLink> deepChecks = new ArrayList<DownloadLink>();
        try {
            final Browser br = new Browser();
            prepBrowser(br, null);
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
                    correctDownloadLink(dl);
                    sb.append(Encoding.urlEncode(MAINPAGE + "/" + getFUID(dl) + ".html"));
                    sb.append("%0A");
                }
                // remove last
                sb.delete(sb.length() - 3, sb.length());
                /*
                 * '/linkchecker/csv' is the official "API" method but this will only return fileID and online/offline - not even the
                 * filename
                 */
                br.postPage("http://" + NICE_HOST + "/linkchecker/check", sb.toString());
                for (final DownloadLink dllink : links) {
                    final Regex fileInfo = br.getRegex("<td>" + getFUID(dllink) + "</td>[\t\n\r ]*<td>([^<]*)</td>[\t\n\r ]*<td style=\"text-align:center;\">(?:[\t\n\r ]*)?<img src=\"(?:[^\"]+)?/(done|error)\\.png\"");
                    if (fileInfo.getMatches() == null || fileInfo.getMatches().length == 0) {
                        dllink.setAvailable(false);
                        logger.warning("Linkchecker broken for " + getHost() + " Example link: " + dllink.getDownloadURL());
                    } else {
                        if (fileInfo.getMatch(1).equals("error")) {
                            dllink.setAvailable(false);
                        } else {
                            final String name = fileInfo.getMatch(0);
                            dllink.setAvailable(true);
                            dllink.setFinalFileName(Encoding.htmlDecode(name.trim()));
                            if (dllink.getKnownDownloadSize() < 0) {
                                deepChecks.add(dllink);
                            }
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        } finally {
            for (final DownloadLink deepCheck : deepChecks) {
                try {
                    requestFileInformation_Web(deepCheck);
                } catch (final Throwable e) {
                }
            }
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
        try {
            login(account, true);
        } catch (final PluginException e) {
            if (br.containsHTML("Our service is currently unavailable in your country\\.")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nTurbobit.net is currently unavailable in your country!\r\nTurbobit.net ist in deinem Land momentan nicht verfügbar!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw e;
        }
        if (br.containsHTML("<span class='glyphicon glyphicon-ok banturbo'>") || ((br.containsHTML("You have reached") && br.containsHTML("limit of premium downloads")))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You have reached limit of premium downloads", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        ai.setUnlimitedTraffic();
        // >Turbo access till 27.09.2015</span>
        final String expire = br.getRegex(">Turbo access till (.*?)</span>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd.MM.yyyy", Locale.ENGLISH));
        }
        ai.setStatus("Premium Account");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/rules";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (this.getPluginConfig().getBooleanProperty(SETTING_FREE_PARALLEL_DOWNLOADSTARTS, false)) {
            return FREE_MAXDOWNLOADS_PLUGINSETTING;
        } else {
            return 1;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private String id = null;

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        // support for public premium links
        if (downloadLink.getDownloadURL().matches(premRedirectLinks)) {
            handlePremiumLink(downloadLink);
            return;
        }
        requestFileInformation(downloadLink);
        if (checkShowFreeDialog(getHost())) {
            super.showFreeDialog(getHost());
        }
        br = new Browser();
        dupe.clear();
        prepBrowser(br, userAgent.get());
        String dllink = downloadLink.getDownloadURL();
        sleep(2500, downloadLink);
        br.getPage(dllink);
        simulateBrowser();
        if (br.containsHTML("'File not found\\. Probably it was deleted") || br.containsHTML(HitFileNet.HTML_FILE_OFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fileSize = br.getRegex("class=\"file-size\">([^<>\"]*?)</span>").getMatch(0);
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
        id = getFUID(downloadLink);
        br.setCookie(br.getHost(), "turbobit1", getCurrentTimeCookie(br));
        br.getPage("/download/free/" + id);
        if (br.getHttpConnection().getCompleteContentLength() < 200) {
            String redirect = br.getRegex("window\\.location\\.href\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
            /* 2017-02-07: Possible redirect from https to http */
            if (redirect == null) {
                redirect = br.getRedirectLocation();
            }
            if (redirect != null) {
                br.getPage(redirect);
            }
        }
        simulateBrowser();
        if (br.containsHTML(HitFileNet.HTML_FILE_OFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (StringUtils.equalsIgnoreCase(br.getRedirectLocation(), downloadLink.getDownloadURL().replace("www.", "")) || br.containsHTML("<div class=\"free-limit-note\">\\s*Limit reached for free download of this file\\.")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
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
        partTwo(downloadLink, captchaform);
    }

    private final void partTwo(final DownloadLink downloadLink, final Form captchaform) throws Exception {
        String downloadUrl = null, waittime = null;
        if (captchaform == null) {
            if (br.containsHTML(tb(0))) {
                waittime = br.getRegex(tb(1)).getMatch(0);
                final int wait = waittime != null ? Integer.parseInt(waittime) : -1;

                if (wait > 31) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                } else if (wait < 0) {
                } else {
                    sleep(wait * 1000l, downloadLink);
                }
            }
            waittime = br.getRegex(tb(1)).getMatch(0);
            if (waittime != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l);
            }
        }

        if (captchaform == null) {
            if (br.containsHTML("Our service is currently unavailable in your country\\.")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Turbobit.net is currently unavailable in your country.");
            }
            logger.warning("captchaform equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.equalsIgnoreCase(captchaform.getAction(), "#")) {
            captchaform.setAction(br.getURL());
        }
        if (br.containsHTML(HTML_RECAPTCHAV1)) {
            /* ReCaptchaV1 */
            logger.info("Handling Re Captcha");
            final String theId = new Regex(br.toString(), "challenge\\?k=(.*?)\"").getMatch(0);
            if (theId == null) {
                logger.warning("the id for Re Captcha equals null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Recaptcha rc = new Recaptcha(br, this);
            rc.setId(theId);
            rc.setForm(captchaform);
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
            rc.getForm().setAction("/download/free/" + id + "#");
            rc.setCode(c);
            if (br.containsHTML(HTML_RECAPTCHAV1) || br.containsHTML("Incorrect, try again")) {
                invalidateLastChallengeResponse();
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        } else if (br.containsHTML("class=\"g-recaptcha\"")) {
            /* ReCaptchaV2 */
            String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            captchaform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(captchaform);
        } else {
            logger.info("Handling normal captchas");
            final String captchaUrl = br.getRegex(CAPTCHAREGEX).getMatch(0);
            if (captchaUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final int retry = 3;
            for (int i = 0; i < retry; i++) {
                String captchaCode;
                if (!getPluginConfig().getBooleanProperty(SETTING_JAC, false) || i >= 1) {
                    captchaCode = getCaptchaCode("turbobit.net.disabled", captchaUrl, downloadLink);
                } else if (captchaUrl.contains("/basic/")) {
                    logger.info("Handling basic captchas");
                    captchaCode = getCaptchaCode("turbobit.net.basic", captchaUrl, downloadLink);
                } else {
                    captchaCode = getCaptchaCode(captchaUrl, downloadLink);
                }
                if (captchaCode == null || "".equals(captchaCode) || captchaCode.matches("\\s+")) {
                    final String msg = "user didn't enter in valid captcha response";
                    logger.warning(msg);
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA, msg);
                }
                captchaform.put("captcha_response", Encoding.urlEncode(captchaCode));
                final Browser br = this.br.cloneBrowser();
                br.submitForm(captchaform);
                if (br.getRegex(CAPTCHAREGEX).getMatch(0) == null) {
                    this.br = br;
                    break;
                } else {
                    if (i + 1 == retry) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    continue;
                }
            }
            if (br.getRegex(CAPTCHAREGEX).getMatch(0) != null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }

        if (br.getHttpConnection().getResponseCode() == 302) {
            // Solving took too long?
            invalidateLastChallengeResponse();
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        }
        simulateBrowser();
        // Ticket Time
        String ttt = parseImageUrl(br.getRegex(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(null)).getMatch(0), true);
        int maxWait = 9999, realWait = 0;
        for (String s : br.getRegex(tb(11)).getColumn(0)) {
            realWait = Integer.parseInt(s);
            if (realWait == 0) {
                continue;
            }
            if (realWait < maxWait) {
                maxWait = realWait;
            }
        }
        boolean waited = false;
        int tt = getPreDownloadWaittime(this.br, 220);
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
            tt = tt < realWait ? tt : realWait;
            if (tt < 30 || tt > 600) {
                ttt = parseImageUrl(tb(2) + tt + "};" + br.getRegex(tb(3)).getMatch(0), false);
                if (ttt == null) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1001l);
                }
                tt = Integer.parseInt(ttt);
            }
            logger.info(" Waittime detected, waiting " + String.valueOf(tt) + " seconds from now on...");
            if (tt > 250) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", tt * 1001l);
            }
            waited = true;
        }
        final boolean use_js = false;

        if (use_js) {
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

            String res = rhino("var id = \'" + id + "\';@" + fun + "@" + rtUpdate, 666);
            if (res == null || res != null && !res.matches(tb(10))) {
                res = rhino("var id = \'" + id + "\';@" + fun + "@" + rtUpdate, 100);
                if (new Regex(res, "/~ID~/").matches()) {
                    res = res.replaceAll("/~ID~/", id);
                }
            }

            if (res != null && res.matches(tb(10))) {
                sleep(tt * 1001, downloadLink);
                // Wed Jun 13 12:29:47 UTC 0200 2012
                SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zZ yyyy");
                Date date = new Date();
                br.setCookie(br.getHost(), "turbobit1", Encoding.urlEncode_light(df.format(date)).replace(":", "%3A"));

                br.getPage(res);
                downloadUrl = rhino(escape(br.toString()) + "@" + rtUpdate, 999);
                if (downloadUrl != null) {
                    downloadUrl = downloadUrl.replaceAll(MAINPAGE, "");
                    if (downloadUrl.equals("/download/free/" + id)) {
                        downloadUrl = null;
                    }
                }
                if (downloadUrl == null) {
                    downloadUrl = br.getRegex("(/download/redirect/[0-9A-F]{32}/" + id + ")").getMatch(0);
                    if (downloadUrl == null) {
                        downloadUrl = br.getRegex("<a href=\'([^\']+)").getMatch(0);
                    }
                }
            }
            if (downloadUrl == null) {
                getPluginConfig().setProperty("isUpdateNeeded", true);
                if (br.containsHTML("The file is not avaliable now because of technical problems")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60 * 1000l);
                }

                if (attemps > 1) {
                    getPluginConfig().setProperty("isUpdateNeeded", false);
                    getPluginConfig().setProperty("attemps", 1);
                    getPluginConfig().save();
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } else {
            String continueLink = br.getRegex("\\$\\('#timeoutBox'\\)\\.load\\(\"(/[^\"]+)\"\\);").getMatch(0);
            if (continueLink == null) {
                continueLink = "/download/getLinkTimeout/" + id;
            }
            if (!waited) {
                this.sleep(tt * 1001l, downloadLink);
            }
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getPage(continueLink);
            downloadUrl = br2.getRegex("(\"|')(/?/download/redirect/.*?)\\1").getMatch(1);
            if (downloadUrl == null) {
                handleDownloadRedirectErrors(br2);
            }
            br.setCookie(br.getHost(), "turbobit2", getCurrentTimeCookie(br2));
        }
        br.setFollowRedirects(false);
        // Future redirects at this point! We want to catch them and not process in order to get the MD5sum! example url structure
        // http://s\\d{2}.turbobit.ru:\\d+/download.php?name=FILENAME.FILEEXTENTION&md5=793379e72eef01ed1fa3fec91eff5394&fid=b5w4jikojflm&uid=free&speed=59&till=1356198536&trycount=1&ip=YOURIP&sid=60193f81464cca228e7bb240a0c39130&browser=201c88fd294e46f9424f724b0d1a11ff&did=800927001&sign=7c2e5d7b344b4a205c71c18c923f96ab
        br.getPage(downloadUrl);
        downloadUrl = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getURL();
        final String md5sum = new Regex(downloadUrl, "md5=([a-f0-9]{32})").getMatch(0);
        if (md5sum != null) {
            downloadLink.setMD5Hash(md5sum);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (dl.getConnection().getResponseCode() == 403 || br.getURL().contains("error/download/ip")) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You cannot download this file with your current IP", 60 * 60 * 1000l);
            }
            br.followConnection();
            if (br.containsHTML("Try to download it once again after")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 20 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, BLOCKED, 10 * 60 * 1000l);
        }
        handleServerErrors();
        dl.startDownload();
    }

    public static int getPreDownloadWaittime(final Browser br, final int wait_fallback) {
        int wait = wait_fallback;
        /* This is NOT a corrent implementation - they use js for the waittime but usually this will do just fine! */
        final String wait_str = br.getRegex("minLimit[\t\n\r ]*?:[\t\n\r ]*?(\\d+)").getMatch(0);
        if (wait_str != null) {
            wait = Integer.parseInt(wait_str);
            if (wait > 800 || wait < 60) {
                /* We do not want to wait too long! */
                wait = wait_fallback;
            }
        }
        return wait;
    }

    private void handleDownloadRedirectErrors(final Browser br) throws PluginException {
        if (br.toString().matches("Error: \\d+")) {
            // unknown error...
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.toString().matches("^The file is not avaliable now because of technical problems\\. <br> Try to download it once again after 10-15 minutes\\..*?")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File not avaiable due to technical problems.", 15 * 60 * 1001l);
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Error");
    }

    private String getCurrentTimeCookie(Browser ibr) throws PluginException {
        if (ibr == null) {
            ibr = br;
        }
        String output = ibr.getRequest().getResponseHeader("Date");
        if (output == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        output = Encoding.urlEncode_light(output);
        output = output.replace(":", "%3A");
        return output;
    }

    /**
     * fuid = case sensitive.
     *
     * @param downloadLink
     * @return
     * @throws PluginException
     */
    @SuppressWarnings("deprecation")
    private String getFUID(DownloadLink downloadLink) throws PluginException {
        // standard links turbobit.net/uid.html && turbobit.net/uid/filename.html
        String fuid = new Regex(downloadLink.getDownloadURL(), "https?://[^/]+/([a-zA-F0-9]+)(/[^/]+)?\\.html").getMatch(0);
        if (fuid == null) {
            // download/free/
            fuid = new Regex(downloadLink.getDownloadURL(), "download/free/([a-zA-F0-9]+)").getMatch(0);
            if (fuid == null) {
                // support for public premium links
                fuid = new Regex(downloadLink.getDownloadURL(), "download/redirect/[A-Za-z0-9]+/([a-zA-F0-9]+)").getMatch(0);
                if (fuid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        return fuid;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        // support for public premium links
        if (link.getDownloadURL().matches(premRedirectLinks)) {
            handlePremiumLink(link);
            return;
        }
        requestFileInformation(link);
        login(account, false);
        sleep(2000, link);
        br.getPage(link.getDownloadURL());
        String dllink = null;
        final String[] mirrors = br.getRegex("('|\")(https?://([a-z0-9\\.]+)?turbobit\\.net//?download/redirect/.*?)\\1").getColumn(1);
        if (mirrors == null || mirrors.length == 0) {
            if (br.containsHTML("You have reached the.*? limit of premium downloads")) {
                logger.info("You have reached the.*? limit of premium downloads");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.containsHTML("'>Premium access is blocked<")) {
                logger.info("No traffic available");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.containsHTML("Our service is currently unavailable in your country\\.")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Turbobit.net is currently unavailable in your country.");
            }
            logger.warning("dllink equals null, plugin seems to be broken!");
            if (br.getCookie("http://turbobit.net", "user_isloggedin") == null || "deleted".equalsIgnoreCase(br.getCookie("http://turbobit.net", "user_isloggedin"))) {
                synchronized (LOCK) {
                    account.setProperty("UA", null);
                    account.setProperty("cookies", null);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            logger.info(NICE_HOST + ": unknown_dl_error_premium");
            int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "unknown_dl_error_premium", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty(NICE_HOSTproperty + "unknown_dl_error_premium", timesFailed);
                logger.info(NICE_HOST + ": unknown_dl_error_premium -> Retrying");
                throw new PluginException(LinkStatus.ERROR_RETRY, "unknown_dl_error_premium");
            } else {
                link.setProperty(NICE_HOSTproperty + "unknown_dl_error_premium", Property.NULL);
                logger.info(NICE_HOST + ": unknown_dl_error_premium - Plugin might be broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.setFollowRedirects(false);
        boolean isdllable = false;
        for (final String currentlink : mirrors) {
            logger.info("Checking mirror: " + currentlink);
            br.getPage(currentlink);
            if (this.br.getHttpConnection().getResponseCode() == 503) {
                logger.info("Too many connections on current account via current IP");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (br.getRedirectLocation() != null) {
                dllink = br.getRedirectLocation();
                if (dllink != null) {
                    isdllable = isDownloadable(dllink);
                    if (isdllable) {
                        logger.info("Mirror is okay: " + currentlink);
                        break;
                    } else {
                        logger.info("Mirror is down: " + currentlink);
                    }
                }
            }
        }
        if (!isdllable) {
            logger.info("Mirror: All mirrors failed -> Server error ");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        final String md5sum = new Regex(dllink, "md5=([a-f0-9]{32})").getMatch(0);
        if (md5sum != null) {
            link.setMD5Hash(md5sum);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                logger.info("No traffic available");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (dl.getConnection().getResponseCode() == 404) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                logger.info("File is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
            logger.warning("dllink doesn't seem to be a file...");
            handleGeneralServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean isDownloadable(final String directlink) {
        URLConnectionAdapter con = null;
        try {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            con = br2.openGetConnection(directlink);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                return false;
            }
        } catch (final Exception e) {
            return false;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable t) {
            }
        }
        return true;
    }

    public void handlePremiumLink(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setCookie(MAINPAGE, "JD", "1");
        String dllink = link.getDownloadURL();
        br.setFollowRedirects(false);
        // Future redirects at this point, but we want to catch them not process
        // them in order to get the MD5sum. Which provided within the
        // URL args, within the final redirect
        // example url structure
        // http://s\\d{2}.turbobit.ru:\\d+/download.php?name=FILENAME.FILEEXTENTION&md5=793379e72eef01ed1fa3fec91eff5394&fid=b5w4jikojflm&uid=free&speed=59&till=1356198536&trycount=1&ip=YOURIP&sid=60193f81464cca228e7bb240a0c39130&browser=201c88fd294e46f9424f724b0d1a11ff&did=800927001&sign=7c2e5d7b344b4a205c71c18c923f96ab
        br.getPage(dllink);
        if (br.getRedirectLocation() != null) {
            dllink = br.getRedirectLocation();
            // we expect md5 redirect here...
            final String md5sum = new Regex(dllink, "md5=([a-f0-9]{32})").getMatch(0);
            if (md5sum != null) {
                link.setMD5Hash(md5sum);
            } else {
                // errors can happen here
                if (StringUtils.endsWithCaseInsensitive(dllink, "://turbobit.net/")) {
                    // expired/invalid?
                    // @see Link; 0418034739341.log; 1111047; jdlog://0418034739341
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Premium link no longer valid");
                } else if (br.containsHTML(">Der Link ist abgelaufen\\. Fordern Sie bitte <a href='/" + getFUID(link) + "\\.html'>new</a> download link\\.<")) {
                    /*
                     * <div class="action-block"><p>Der Link ist abgelaufen. Fordern Sie bitte <a href='/FUID.html'>new</a> download
                     * link.</p></div></div> </div>
                     */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Generated Premium link has expired!");
                }
                handleDownloadRedirectErrors(br);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                logger.info("No traffic available");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            if (dl.getConnection().getResponseCode() == 404) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                logger.info("File is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
            logger.warning("dllink doesn't seem to be a file...");
            if (br.containsHTML("Our service is currently unavailable in your country\\.")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Turbobit.net is currently unavailable in your country.");
            }
            handleGeneralServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleServerErrors();
        dl.startDownload();
    }

    private void handleGeneralServerErrors() throws PluginException {
        if (br.containsHTML("<h1>404 Not Found</h1>") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        }
        if (br.containsHTML("Try to download it once again after")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Try again later'", 20 * 60 * 1000l);
        }
        /* Either user waited too long for the captcha or maybe slow servers */
        if (br.containsHTML(">Ссылка просрочена\\. Пожалуйста получите")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'link expired'", 5 * 60 * 1000l);
        }
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getLongContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, server sends empty file", 5 * 60 * 1000l);
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    private static AtomicReference<String> userAgent = new AtomicReference<String>(null);

    private Browser prepBrowser(final Browser prepBr, String UA) {
        // br.setCookie(MAINPAGE, "JD", "1");
        if (UA == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
            UA = userAgent.get();
        }
        prepBr.getHeaders().put("Pragma", null);
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Accept", "text/html, application/xhtml+xml, */*");
        prepBr.getHeaders().put("Accept-Language", "en-EN");
        prepBr.getHeaders().put("User-Agent", UA);
        prepBr.getHeaders().put("Referer", null);
        prepBr.setCustomCharset("UTF-8");
        prepBr.setCookie(MAINPAGE, "JD", "1");
        return prepBr;
    }

    /* TODO: Make an unique login function which works for turbobit.net AND hitfile.net (same system) */
    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                setBrowserExclusive();
                String ua = null;
                if (force == false) {
                    /*
                     * we have to reuse old UA, else the cookie will become invalid
                     */
                    ua = account.getStringProperty("UA", null);
                }
                prepBrowser(br, ua);
                br.setCookie(MAINPAGE, "set_user_lang_change", "en");
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                // lets set a new agent
                prepBrowser(br, null);
                br.getPage(MAINPAGE);
                br.postPage("/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bmemory%5D=on&user%5Bsubmit%5D=Sign+in");
                // Check for stupid login captcha
                if (br.containsHTML(">Limit of login attempts exceeded|>Please enter the captcha")) {
                    logger.info("processing login captcha...");
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", NICE_HOST, MAINPAGE, true);
                    final String captchaLink = br.getRegex("\"(https?://turbobit\\.net/captcha/[^<>\"]*?)\"").getMatch(0);
                    if (captchaLink != null) {
                        final String code = getCaptchaCode("NOTOLDTRBT", captchaLink, dummyLink);
                        String captchaSubtype = "3";
                        if (captchaLink.contains("/basic/")) {
                            captchaSubtype = "5";
                        }
                        br.postPage("/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bcaptcha_response%5D=" + Encoding.urlEncode(code) + "&user%5Bcaptcha_type%5D=securimg&user%5Bcaptcha_subtype%5D=" + captchaSubtype + "&user%5Bsubmit%5D=Sign+in");
                    } else if (br.containsHTML("class=\"g-recaptcha\"")) {
                        this.setDownloadLink(dummyLink);
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        br.postPage("/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response) + "&user%5Bcaptcha_type%5D=recaptcha2&user%5Bcaptcha_subtype%5D=&user%5Bmemory%5D=on&user%5Bsubmit%5D=Sign+in");
                    } else {
                        final Recaptcha rc = new Recaptcha(br, this);
                        final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                        if (id == null) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin failed, please contact our support!\r\nLogin fehlgeschlagen, bitte kontaktiere unseren Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        rc.setId(id);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("RECAPTCHA", cf, dummyLink);
                        br.postPage("/user/login", "user%5Blogin%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpass%5D=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&user%5Bcaptcha_type%5D=recaptcha&user%5Bcaptcha_subtype%5D=&user%5Bmemory%5D=on&user%5Bsubmit%5D=Sign+in");
                    }
                }
                universalLoginErrorhandling(this.br);
                if (!"1".equals(br.getCookie(MAINPAGE, "user_isloggedin"))) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.\r\n3. Gehe auf folgende Seite und deaktiviere, den Login Captcha Schutz deines Accounts und versuche es erneut: turbobit.net/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.\r\n3. Access the following site and disable the login captcha protection of your account and try again: turbobit.net/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML("<notsure....>|<span class='glyphicon glyphicon-ok yesturbo'>") || br.containsHTML("<span class='glyphicon glyphicon-ok banturbo'>") || ((br.containsHTML("You have reached") && br.containsHTML("limit of premium downloads")))) {
                    // cookies
                    final HashMap<String, String> cookies = new HashMap<String, String>();
                    final Cookies add = br.getCookies(br.getHost());
                    for (final Cookie c : add.getCookies()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                    account.setProperty("name", Encoding.urlEncode(account.getUser()));
                    account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                    account.setProperty("cookies", cookies);
                    account.setProperty("UA", userAgent.get());
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid account type (Not premium account)!\r\nUngültiger Accounttyp (kein Premium Account)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } catch (final PluginException e) {
                account.setProperty("UA", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    public static void universalLoginErrorhandling(final Browser br) throws PluginException {
        if (br.containsHTML(">Limit of login attempts exceeded for your account")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nMaximale Anzahl von Loginversuchen überschritten - dein Account wurde temporär gesperrt!\r\nBestätige deinen Account per E-Mail um ihn zu entsperren.\r\nFalls du keine E-Mail bekommen hast, gib deine E-Mail Adresse auf folgender Seite ein und lasse dir erneut eine zuschicken: " + br.getHost() + "/restoreaccess", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLimit of login attempts exceeded for your account - your account is locked!\r\nConfirm your account via e-mail to unlock it.\r\nIf you haven't received an e-mail, enter your e-mail address on the following site so the service can send you a new confirmation mail: " + br.getHost() + "/restoreaccess", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private String parseImageUrl(String fun, final boolean NULL) {
        if (fun == null) {
            return null;
        }
        if (!NULL) {
            final String[] next = fun.split(tb(9));
            if (next == null || next.length != 2) {
                fun = rhino(fun, 0);
                if (fun == null) {
                    return null;
                }
                fun = new Regex(fun, tb(4)).getMatch(0);
                return fun == null ? new Regex(fun, tb(5)).getMatch(0) : rhino(fun, 2);
            }
            return rhino(next[1], 1);
        }
        return new Regex(fun, tb(1)).getMatch(0);
    }

    // Also check HitFileNet plugin if this one is broken
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        if (prefer_single_linkcheck_linkcheckpage) {
            requestFileInformation_LinkCheck(downloadLink);
        } else {
            requestFileInformation_Web(downloadLink);
        }
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformation_LinkCheck(final DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformation_Web(final DownloadLink downloadLink) throws IOException, PluginException {
        // premium links should not open here, we will just return true
        if (downloadLink.getDownloadURL().matches(premRedirectLinks)) {
            return AvailableStatus.TRUE;
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(br, userAgent.get());
        br.setCookie(MAINPAGE + "/", "set_user_lang_change", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(<div class=\"code-404\">404</div>|Файл не найден\\. Возможно он был удален\\.<br|File( was)? not found\\.|It could possibly be deleted\\.)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filenameSize = "<title>\\s*(?:Download\\s+file|Datei\\s+downloaden)\\s*(.*?)\\s*\\(([\\d\\.,]+\\s*[BMGTP]{1,2})\\)\\s*\\|\\s*TurboBit\\.net";
        String fileName = br.getRegex(filenameSize).getMatch(0);
        if (fileName == null) {
            fileName = br.getRegex("<span class=(\"|')file-title\\1[^>]*>(.*?)</span>").getMatch(1);
        }
        String fileSize = br.getRegex(filenameSize).getMatch(1);
        if (fileSize == null) {
            br.getRegex("class=\"file-size\">([^<>\"]*?)<").getMatch(0);
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

    private String rhino(final String s, final int b) {
        Object result = new Object();
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            switch (b) {
            case 0:
                engine.eval(s + tb(6));
                result = engine.get(tb(7));
                break;
            case 1:
                result = ((Double) engine.eval(tb(8))).longValue();
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_JAC, JDL.L("plugins.hoster.turbobit.jac", lang_jac)).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_FREE_PARALLEL_DOWNLOADSTARTS, JDL.L("plugins.hoster.turbobit.simultan_Free_downloadstarts", lang_free_dlstarts)).setDefaultValue(false));
    }

    private String tb(final int i) {
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
        /*
         * we have to load the plugin first! we must not reference a plugin class without loading it before
         */
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        return JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(s[i]));
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