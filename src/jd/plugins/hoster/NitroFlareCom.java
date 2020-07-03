//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.NitroflareConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nitroflare.com" }, urls = { "https?://(?:www\\.)?nitroflare\\.com/(?:view|watch)/([A-Z0-9]+)" })
public class NitroFlareCom extends antiDDoSForHost {
    private final String         baseURL  = "https://nitroflare.com";
    /* Documentation: https://nitroflare.com/member?s=api */
    private final String         API_BASE = "http://nitroflare.com/api/v2";
    /* don't touch the following! */
    private static AtomicInteger maxFree  = new AtomicInteger(1);

    public NitroFlareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(null);
        Browser.setRequestIntervalLimitGlobal("nitroflare.com", 500);
    }

    /**
     * Use website or API: https://nitroflare.com/member?s=api </br>
     *
     * @return true: Use API for account login and premium downloading </br>
     *         false: Use website for everything (except linkcheck)
     */
    private boolean useAPIAccountMode() {
        return PluginJsonConfig.get(NitroflareConfig.class).isUsePremiumAPIEnabled();
    }

    private boolean useAPIFreeMode() {
        /** 2020-07-03: Doesn't work (yet) thus I've removed this setting RE: psp */
        // return PluginJsonConfig.get(NitroflareConfig.class).isUseFreeAPIEnabled();
        return false;
    }

    @Override
    public String getAGBLink() {
        return baseURL + "/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("https://nitroflare.com/view/" + this.getFID(link));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
                // free account
                chunks = 1;
                resumes = false;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = 0;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = false;
            isFree = true;
            directlinkproperty = "freelink";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    private boolean freedl = false;

    @Override
    protected boolean useRUA() {
        if (freedl) {
            return true;
        }
        return false;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.addAllowedResponseCodes(500);
        }
        return prepBr;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (link != null) {
            if (account == null || account.getType() == AccountType.FREE) {
                return !(link.getBooleanProperty("premiumRequired", false));
            }
        }
        return true;
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        boolean okay = true;
        try {
            final Browser br = new Browser();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    if (links.size() == 100 || index == urls.length) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                final StringBuilder sb = new StringBuilder();
                sb.append("files=");
                boolean atLeastOneDL = false;
                for (final DownloadLink dl : links) {
                    if (atLeastOneDL) {
                        sb.append(",");
                    }
                    sb.append(getFID(dl));
                    atLeastOneDL = true;
                }
                getPage(br, API_BASE + "/getFileInfo?" + sb);
                if (br.containsHTML("In these moments we are upgrading the site system")) {
                    for (final DownloadLink dl : links) {
                        dl.getLinkStatus().setStatusText("Nitroflare.com is maintenance mode. Try again later");
                        dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                    }
                    return true;
                }
                for (final DownloadLink dl : links) {
                    final String filter = br.getRegex("(\"" + getFID(dl) + "\":\\{.*?\\})").getMatch(0);
                    if (filter == null) {
                        dl.setProperty("apiInfo", Property.NULL);
                        okay = false;
                        continue;
                    }
                    final String status = PluginJSonUtils.getJsonValue(filter, "status");
                    if ("online".equalsIgnoreCase(status)) {
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                    final String name = PluginJSonUtils.getJsonValue(filter, "name");
                    final String size = PluginJSonUtils.getJsonValue(filter, "size");
                    final String md5 = PluginJSonUtils.getJsonValue(filter, "md5");
                    final String prem = PluginJSonUtils.getJsonValue(filter, "premiumOnly");
                    final String pass = PluginJSonUtils.getJsonValue(filter, "password");
                    if (name != null) {
                        dl.setFinalFileName(name);
                    }
                    if (size != null) {
                        dl.setVerifiedFileSize(Long.parseLong(size));
                    }
                    if (md5 != null) {
                        dl.setMD5Hash(md5);
                    }
                    if (PluginJsonConfig.get(NitroflareConfig.class).isTrustAPIAboutPremiumOnlyFlag()) {
                        if (prem != null) {
                            dl.setProperty("premiumRequired", Boolean.parseBoolean(prem));
                        } else {
                            dl.setProperty("premiumRequired", Property.NULL);
                        }
                    }
                    if (pass != null) {
                        dl.setProperty("passwordRequired", Boolean.parseBoolean(pass));
                    } else {
                        dl.setProperty("passwordRequired", Property.NULL);
                    }
                    dl.setProperty("apiInfo", Boolean.TRUE);
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return okay;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformationWeb(link);
    }

    private AvailableStatus requestFileInformationWeb(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*This file has been removed|>\\s*File doesn't exist<|This file has been removed due|>\\s*This file has been removed by its owner")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<b>File Name: </b><span title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("alt=\"\" /><span title=\"([^<>\"]*?)\">").getMatch(0);
        }
        final String filesize = br.getRegex("dir=\"ltr\" style=\"text-align: left;\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            if (br.containsHTML(">Your ip is been blocked, if you think it is mistake contact us")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your ip is been blocked", 30 * 60 * 1000l);
            }
        }
        if (link.getBooleanProperty("apiInfo", Boolean.FALSE) == Boolean.FALSE) {
            /* no apiInfos available, set unverified name/size here */
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename.trim()));
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        setConstants(null);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        requestFileInformationApi(link);
        doFree(null, link);
    }

    private final void doFree(final Account account, final DownloadLink link) throws Exception {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (link.getBooleanProperty("premiumRequired", false)) {
            throwPremiumRequiredException(link, true);
        }
        freedl = true;
        br = new Browser();
        dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (useAPIFreeMode()) {
                /* API mode */
                this.getPage(this.API_BASE + "/getDownloadLink?file=" + Encoding.urlEncode(this.getFID(link)));
                this.checkErrorsAPI(link, account);
                final String waittime = PluginJSonUtils.getJson(br, "delay");
                final String reCaptchaKey = PluginJSonUtils.getJson(br, "recaptchaPublic");
                String accessLink = PluginJSonUtils.getJson(br, "accessLink");
                if (StringUtils.isEmpty(waittime) || StringUtils.isEmpty(reCaptchaKey)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error in free download step 1");
                }
                long wait = Long.parseLong(waittime) * 1000l;
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey);
                final int reCaptchaV2Timeout = rc2.getSolutionTimeout();
                final long timestampBeforeCaptchaSolving = System.currentTimeMillis();
                if (wait > reCaptchaV2Timeout) {
                    final long prePrePreDownloadWait = wait - reCaptchaV2Timeout;
                    logger.info("Waittime is higher than reCaptchaV2 timeout --> Waiting a part of it before solving captcha to avoid timeouts");
                    logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
                    this.sleep(prePrePreDownloadWait, link);
                }
                final String c = rc2.getToken();
                if (inValidate(c)) {
                    // fixes timeout issues or client refresh, we have no idea at this stage
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                // remove one second from past, to prevent returning too quickly.
                final long passedTime = (System.currentTimeMillis() - timestampBeforeCaptchaSolving) - 1500;
                wait -= passedTime;
                if (wait > 0) {
                    logger.info("Pre- download waittime seconds: " + (wait / 1000));
                    sleep(wait, link);
                } else {
                    logger.info("Congratulation: Captcha solving took so long that we do not have to wait at all");
                }
                accessLink += "&captcha=" + Encoding.urlEncode(c) + "&g-recaptcha-response=" + Encoding.urlEncode(c);
                /* 2020-06-24: TODO: This would always return "Invalid captcha"?! */
                this.getPage(accessLink);
                this.checkErrorsAPI(link, account);
                this.dllink = PluginJSonUtils.getJson(br, "url");
                if (StringUtils.isEmpty(this.dllink) || !this.dllink.startsWith("http")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error in free download step 2");
                }
            } else {
                /* Website mode */
                if (br.getURL() == null) {
                    requestFileInformationWeb(link);
                }
                handleErrors(br, false);
                randomHash(br, link);
                ajaxPost(br, "/ajax/setCookie.php", "fileId=" + getFID(link));
                {
                    int i = 0;
                    while (true) {
                        // lets add some randomisation between submitting gotofreepage
                        sleep((new Random().nextInt(5) + 8) * 1001l, link);
                        // first post registers time value
                        postPage(br.getURL(), "goToFreePage=");
                        randomHash(br, link);
                        ajaxPost(br, "/ajax/setCookie.php", "fileId=" + getFID(link));
                        if (br.getURL().endsWith("/free")) {
                            break;
                        } else if (++i > 3) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            continue;
                        }
                    }
                }
                ajaxPost(br, "/ajax/freeDownload.php", "method=startTimer&fileId=" + getFID(link));
                handleErrors(ajax, false);
                final long timestampBeforeCaptchaSolving = System.currentTimeMillis();
                final String waitStr = br.getRegex("<div id=\"CountDownTimer\" data-timer=\"(\\d+)\"").getMatch(0);
                // register wait i guess, it should return 1
                final int repeat = 5;
                for (int i = 1; i <= repeat; i++) {
                    if (br.containsHTML("plugins/cool-captcha/captcha.php")) {
                        final String captchaCode = getCaptchaCode(br.getURL("/plugins/cool-captcha/captcha.php").toString(), link);
                        if (i == 1) {
                            long wait = 60;
                            if (waitStr != null) {
                                // remove one second from past, to prevent returning too quickly.
                                final long passedTime = ((System.currentTimeMillis() - timestampBeforeCaptchaSolving) / 1000) - 1;
                                wait = Long.parseLong(waitStr) - passedTime;
                            }
                            if (wait > 0) {
                                sleep(wait * 1000l, link);
                            }
                        }
                        ajaxPost(br, "/ajax/freeDownload.php", "method=fetchDownload&captcha=" + Encoding.urlEncode(captchaCode));
                    } else {
                        final int firstLoop = 1;
                        long wait = 60;
                        if (waitStr != null) {
                            wait = Long.parseLong(waitStr) * 1000l;
                        }
                        final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                        final int reCaptchaV2Timeout = rc2.getSolutionTimeout();
                        if (i == firstLoop && wait > reCaptchaV2Timeout) {
                            final int prePrePreDownloadWait = (int) (wait - reCaptchaV2Timeout);
                            logger.info("Waittime is higher than reCaptchaV2 timeout --> Waiting a part of it before solving captcha to avoid timeouts");
                            logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
                            this.sleep(prePrePreDownloadWait, link);
                        }
                        final String c = rc2.getToken();
                        if (inValidate(c)) {
                            // fixes timeout issues or client refresh, we have no idea at this stage
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        if (i == firstLoop) {
                            // remove one second from past, to prevent returning too quickly.
                            final long passedTime = (System.currentTimeMillis() - timestampBeforeCaptchaSolving) - 1500;
                            wait -= passedTime;
                            if (wait > 0) {
                                sleep(wait, link);
                            } else {
                                logger.info("Congratulation: Captcha solving took so long that we do not have to wait at all");
                            }
                        }
                        ajaxPost(br, "/ajax/freeDownload.php", "method=fetchDownload&captcha=" + Encoding.urlEncode(c) + "&g-recaptcha-response=" + Encoding.urlEncode(c));
                    }
                    if (ajax.containsHTML("The captcha wasn't entered correctly|You have to fill the captcha")) {
                        if (i + 1 == repeat) {
                            logger.info("Exhausted captcha attempts");
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        continue;
                    }
                    break;
                }
                dllink = ajax.getRegex("\"(https?://[a-z0-9\\-_]+\\.nitroflare\\.com/[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    handleErrors(ajax, true);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
        if (!isDownloadableContent(dl.getConnection())) {
            handleDownloadErrors(account, link, true);
        }
        link.setProperty(directlinkproperty, dllink);
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
        final int totalMaxSimultanFreeDownload = PluginJsonConfig.get(NitroflareConfig.class).isAllowMultipleFreeDownloads() ? 20 : 1;
        logger.info("maxFree was = " + maxFree.get() + " total is = " + totalMaxSimultanFreeDownload + " change " + num);
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload));
        logger.info("maxFree now = " + maxFree.get());
    }

    /**
     * This seems to happen in this manner
     *
     * @throws Exception
     **/
    private final void randomHash(final Browser br, final DownloadLink link) throws Exception {
        final String randomHash = JDHash.getMD5(link.getDownloadURL() + System.currentTimeMillis());
        // same cookie is set within as a cookie prior to registering
        br.setCookie(getHost(), "randHash", randomHash);
        ajaxPost(br, "https://" + this.getHost() + "/ajax/randHash.php", "randHash=" + randomHash);
    }

    private void handleErrors(final Browser br, final boolean postCaptcha) throws PluginException {
        if (postCaptcha) {
            if (br.containsHTML("You don't have an entry ticket\\. Please refresh the page to get a new one")) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("File doesn't exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (br.containsHTML("This file is available with premium key only|This file is available with Premium only")) {
            throwPremiumRequiredException(this.getDownloadLink(), false);
        } else if (br.containsHTML("﻿Downloading is not possible") || br.containsHTML("downloading is not possible")) {
            if (PluginJsonConfig.get(NitroflareConfig.class).isAllowMultipleFreeDownloads()) {
                /* We do not know exactly when the next free download is possible so let's try every 20 minutes. */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 20 * 60 * 1000l);
            } else {
                // ﻿Free downloading is not possible. You have to wait 178 minutes to download your next file.
                final String waitminutes = br.getRegex("You have to wait (\\d+) minutes to download").getMatch(0);
                if (waitminutes != null) {
                    // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitminutes) * 60 * 1001l);
                    // they have 30min wait not the 3 hours a stated with this error response
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
        } else if (StringUtils.startsWithCaseInsensitive(br.toString(), "﻿Free download is currently unavailable due to overloading in the server")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Free download Overloaded, will try again later", 5 * 60 * 1000l);
        }
    }

    private Browser ajax = null;

    private void ajaxPost(final Browser br, final String url, final String post) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajax.postPage(url, post);
    }

    /**
     * Validates account and returns correct account info, when user has provided incorrect user pass fields to JD client. Or Throws
     * exception indicating users mistake, when it's a irreversible mistake.
     *
     * @param account
     * @return
     * @throws PluginException
     */
    @Deprecated
    private String validateAccount(final Account account) throws PluginException {
        synchronized (account) {
            final String user = account.getUser().toLowerCase(Locale.ENGLISH);
            final String pass = account.getPass();
            if (inValidate(pass)) {
                // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                // "\r\nYou haven't provided a valid password or premiumKey (this field can not be empty)!",
                // PluginException.VALUE_ID_PREMIUM_DISABLE);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid password (this field can not be empty)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (pass.matches("(?-i)NF[a-zA-Z0-9]{10}")) {
                // no need to urlencode, this is always safe.
                // return "user=&premiumKey=" + pass;
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPremiumKeys not accepted, you need to use Account (email and password).", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (inValidate(user) || !user.matches(".+@.+")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            // check to see if the user added the email username with caps.. this can make login incorrect
            if (!user.equals(account.getUser())) {
                account.setUser(user);
            }
            // urlencode required!
            return "user=" + Encoding.urlEncode(user) + "&premiumKey=" + Encoding.urlEncode(pass);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (useAPIAccountMode()) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWeb(account, false, true);
        }
    }

    private AccountInfo fetchAccountInfoWeb(final Account account, boolean fullLogin, boolean fullInfo) throws Exception {
        synchronized (account) {
            if (!account.getUser().matches(".+@.+")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !fullLogin) {
                    if (account.isValid()) {
                        br.setCookies(cookies);
                        // lets do a test
                        final Browser br2 = br.cloneBrowser();
                        getPage(br2, "https://www." + this.getHost());
                        if (br2.containsHTML(">\\s*Your password has expired") || br2.containsHTML(">\\s*Change Password\\s*<")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your password has expired. Please visit website and set new password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        final String user = br2.getCookie("nitroflare.com", "user");
                        if (user != null && !"deleted".equalsIgnoreCase(user)) {
                            if (!fullInfo) {
                                return null;
                            } else {
                                // else we need to do stats!
                            }
                        } else {
                            fullLogin = true;
                        }
                    } else {
                        fullLogin = true;
                    }
                } else {
                    fullLogin = true;
                }
                if (fullLogin) {
                    getPage("https://" + this.getHost() + "/login");
                    Form f = null;
                    for (int retry = 0; retry < 3; retry++) {
                        f = br.getFormbyProperty("id", "login");
                        if (f == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        // recaptcha2
                        if (f.containsHTML("<div class=\"g-recaptcha\"")) {
                            if (this.getDownloadLink() == null) {
                                // login wont contain downloadlink
                                this.setDownloadLink(new DownloadLink(this, "Account Login!", this.getHost(), this.getHost(), true));
                            }
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            f.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                        }
                        f.put("email", Encoding.urlEncode(account.getUser().toLowerCase(Locale.ENGLISH)));
                        f.put("password", Encoding.urlEncode(account.getPass()));
                        f.put("login", "");
                        submitForm(f);
                        // place in incorrect password here
                        f = br.getFormbyProperty("id", "login");
                        if (f == null) {
                            break;
                        }
                    }
                    if (f != null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nIncorrect User/Password", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    if (br.containsHTML(">\\s*Your password has expired") || br.containsHTML(">\\s*Change Password\\s*<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your password has expired. Please visit website and set new password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    // final failover, we expect 'user' cookie
                    final String user = br.getCookie("nitroflare.com", "user");
                    if (user == null || "deleted".equalsIgnoreCase(user)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCould not find Account Cookie", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final AccountInfo ai = new AccountInfo();
                getPage("https://" + this.getHost() + "/member?s=premium");
                // status
                final String status = br.getRegex("<label>Status</label><strong[^>]+>\\s*([^<]+)\\s*</strong>").getMatch(0);
                if (!inValidate(status)) {
                    if (StringUtils.equalsIgnoreCase(status, "Active")) {
                        // Active (green) = premium
                        account.setType(AccountType.PREMIUM);
                        ai.setStatus("Premium Account");
                    } else {
                        // Expired (red) = free
                        account.setType(AccountType.FREE);
                        ai.setStatus("Free Account");
                    }
                } else {
                    account.setType(AccountType.FREE);
                    ai.setStatus("Free Account");
                }
                // extra traffic in webmode isn't added to daily traffic, so we need to do it manually. (api mode is has been added to
                // traffic left/max)
                final String extraTraffic = br.getRegex("<label>Your Extra Bandwidth</label><strong>(.*?)</strong>").getMatch(0);
                // do we have traffic?
                final String[] traffic = br.getRegex("<label>[^>]*Daily Limit</label><strong>(\\d+(?:\\.\\d+)?(?:\\s*[KMGT]{0,1}B)?) / (\\d+(?:\\.\\d+)?\\s*[KMGT]{0,1}B)</strong>").getRow(0);
                if (traffic != null) {
                    final long extratraffic = !inValidate(extraTraffic) ? SizeFormatter.getSize(extraTraffic) : 0;
                    final long trafficmax = SizeFormatter.getSize(traffic[1]);
                    // they show traffic used, not traffic left. we need to convert it.
                    final long trafficleft = trafficmax - SizeFormatter.getSize(traffic[0]);
                    // first value is traffic used, not remaining
                    ai.setTrafficLeft(trafficleft + extratraffic);
                    ai.setTrafficMax(trafficmax + extratraffic);
                }
                // expire time
                final String expire = br.getRegex("<label>Time Left</label><strong>(.*?)</strong>").getMatch(0);
                if (!inValidate(expire)) {
                    // <strong>11 days, 7 hours, 53 minutes.</strong>
                    final String tmpyears = new Regex(expire, "(\\d+)\\s*years?").getMatch(0);
                    final String tmpdays = new Regex(expire, "(\\d+)\\s*days?").getMatch(0);
                    final String tmphrs = new Regex(expire, "(\\d+)\\s*hours?").getMatch(0);
                    final String tmpmin = new Regex(expire, "(\\d+)\\s*minutes?").getMatch(0);
                    final String tmpsec = new Regex(expire, "(\\d+)\\s*seconds?").getMatch(0);
                    long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
                    if (!inValidate(tmpyears)) {
                        days = Integer.parseInt(tmpyears);
                    }
                    if (!inValidate(tmpdays)) {
                        days = Integer.parseInt(tmpdays);
                    }
                    if (!inValidate(tmphrs)) {
                        hours = Integer.parseInt(tmphrs);
                    }
                    if (!inValidate(tmpmin)) {
                        minutes = Integer.parseInt(tmpmin);
                    }
                    if (!inValidate(tmpsec)) {
                        seconds = Integer.parseInt(tmpsec);
                    }
                    long waittime = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
                    ai.setValidUntil(System.currentTimeMillis() + waittime);
                }
                account.setAccountInfo(ai);
                if (account.isValid()) {
                    /** Save cookies */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return ai;
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Non Valid Account", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        synchronized (account) {
            if (!account.getUser().matches(".+@.+")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            br.setCookiesExclusive(true);
            getPage(API_BASE + "/getKeyInfo?user=" + Encoding.urlEncode(account.getUser()) + "&premiumKey=" + Encoding.urlEncode(account.getPass()));
            checkErrorsAPI(null, account);
            final AccountInfo ai = new AccountInfo();
            final String trafficLeftStr = PluginJSonUtils.getJson(br, "trafficLeft");
            final String trafficMaxStr = PluginJSonUtils.getJson(br, "trafficMax");
            final String expiryDate = PluginJSonUtils.getJson(br, "expiryDate");
            final String accountStatus = PluginJSonUtils.getJson(br, "status");
            if (!StringUtils.isEmpty(trafficLeftStr) && !StringUtils.isEmpty(trafficMaxStr)) {
                ai.setTrafficLeft(trafficLeftStr);
                ai.setTrafficMax(trafficMaxStr);
            }
            if (!"active".equalsIgnoreCase(accountStatus) || "0".equals(expiryDate)) {
                account.setType(AccountType.FREE);
                ai.setStatus("Free Account");
            } else {
                account.setType(AccountType.PREMIUM);
                ai.setStatus("Premium Account");
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expiryDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
            }
            return ai;
        }
    }

    private void checkErrorsAPI(final DownloadLink link, final Account account) throws Exception {
        int errorcode = -1;
        String msg = null;
        try {
            /* E.g. {"type":"error","message":"Wrong login","code":8} */
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            msg = (String) entries.get("message");
            errorcode = ((Number) entries.get("code")).intValue();
        } catch (final Throwable e) {
            // logger.log(e);
        }
        switch (errorcode) {
        case -1:
            /* No error */
            break;
        case 1:
            /* {"type":"error","message":"Access denied","code":1} */
            /* File is premiumonly */
            throw new AccountRequiredException();
        case 4:
            /* {"type":"error","message":"File doesn't exist","code":4} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 6:
            /* {"type":"error","message":"Invalid captcha","code":6} */
            /* This should rarely/never happen!! */
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        case 8:
            /* Invalid logindata */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_DISABLE);
        case 12:
            /* API captcha required to continue using their API! */
            /* TODO: Add handling for this captcha */
            // this.getPage(this.API_BASE + "/solveCaptcha?user=" + Encoding.urlEncode(account.getUser()));
        default:
            /* Handle unknown errors */
            if (link == null) {
                throw new AccountUnavailableException("API error: " + msg, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "API error: " + msg, 5 * 60 * 1000l);
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account);
        /* is free user? */
        if (account.getType() == AccountType.FREE) {
            requestFileInformationWeb(link);
            fetchAccountInfoWeb(account, false, false);
            requestFileInformationApi(link); // Required, to do checkLinks to check premiumOnly
            doFree(account, link);
        } else {
            /* Premium download */
            /* check cached download */
            dllink = link.getStringProperty(directlinkproperty);
            if (!inValidate(dllink)) {
                logger.info("Trying to re-use stored generated directurl");
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                if (isDownloadableContent(dl.getConnection())) {
                    logger.info("Successfully re-used stored generated directurl");
                    link.setProperty(directlinkproperty, dllink);
                    dl.startDownload();
                    return;
                } else {
                    logger.info("Failed to re-use stored generated directurl");
                    link.setProperty(directlinkproperty, Property.NULL);
                    handleDownloadErrors(account, link, false);
                }
            }
            logger.info("Generating new directurl");
            if (this.useAPIAccountMode()) {
                /* 2020-06-24: According to API docs, we should check the file before performing premium-download-API-call! */
                requestFileInformationApi(link);
                /* Additional login is not required. */
                handlePremiumDownloadAPI(link, account);
            } else {
                // requestFileInformationWeb(link);
                fetchAccountInfoWeb(account, false, false);
                requestFileInformationApi(link);
                /* 2020-06-24: Not required anymore (?) */
                // randomHash(br, link);
                // ajaxPost(br, "https://" + this.getHost() + "/ajax/setCookie.php", "fileId=" + getFUID(link));
                /* / could be directlink */
                dllink = link.getDownloadURL();
                int counter = 0;
                final int maxtries = 2;
                do {
                    logger.info(String.format("Downloadloop %d / %d", counter + 1, maxtries));
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                    if (isDownloadableContent(dl.getConnection())) {
                        /* Directurl */
                        logger.info("Seems like user has direct downloads enabled");
                        link.setProperty(directlinkproperty, dllink);
                        dl.startDownload();
                        return;
                    }
                    logger.info("Seems like user has direct downloads disabled or VPN captcha required");
                    br.followConnection();
                    handlePremiumVPNWarningCaptcha(link);
                    counter++;
                } while (handlePremiumVPNWarningCaptcha(link) && counter < maxtries);
                handleDownloadErrors(account, link, false);
                dllink = br.getRegex("<a[^>]*id=\"download\"[^>]*href=\"([^\"]+)\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Can't find dllink!");
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                if (isDownloadableContent(dl.getConnection())) {
                    link.setProperty(directlinkproperty, dllink);
                    dl.startDownload();
                    return;
                }
                handleDownloadErrors(account, link, true);
            }
        }
    }

    protected boolean isDownloadableContent(final URLConnectionAdapter con) throws IOException {
        return con != null && con.isContentDisposition();
    }

    private void handlePremiumDownloadAPI(final DownloadLink link, final Account account) throws Exception {
        this.getPage(API_BASE + "/getDownloadLink?user=" + Encoding.urlEncode(account.getUser()) + "&premiumKey=" + Encoding.urlEncode(account.getPass()) + "&file=" + Encoding.urlEncode(this.getFID(link)));
        this.checkErrorsAPI(link, account);
        this.dllink = PluginJSonUtils.getJson(br, "url");
        if (StringUtils.isEmpty(this.dllink) || !this.dllink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API download failure");
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
        if (isDownloadableContent(dl.getConnection())) {
            link.setProperty(directlinkproperty, dllink);
            dl.startDownload();
            return;
        }
        handleDownloadErrors(account, link, true);
    }

    /**
     * Handle rare case: User uses VPN, nitroflare recognizes that and lets user solve an extra captcha to proceed via VPN. </br>
     *
     * @return: true: Captcha required and successfully solved by user </br>
     *          false: Captcha not required </br>
     *          exception: Wrong captcha
     */
    private boolean handlePremiumVPNWarningCaptcha(final DownloadLink link) throws Exception {
        if (br.containsHTML("To get rid of the captcha, please avoid using a dedicated server")) {
            logger.info("Premium VPN captcha required");
            /* 2020-02-20: Here is their reCaptchaV2 site-key for testing: 6Lenx_USAAAAAF5L1pmTWvWcH73dipAEzNnmNLgy */
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            ajaxPost(br, "/ajax/validate-dl-recaptcha", "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
            if (!ajax.containsHTML("passed")) {
                logger.info("Premium captcha failure");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            logger.info("Premium captcha success");
            return true;
        } else {
            logger.info("Premium VPN captcha NOT required!");
            return false;
        }
    }

    private final void handleDownloadErrors(final Account account, final DownloadLink downloadLink, final boolean lastChance) throws PluginException, IOException {
        // don't fill logger with crapola
        if (br.getRequest().getHtmlCode() == null) {
            br.followConnection();
        }
        final String err1 = "ERROR: Wrong IP. If you are using proxy, please turn it off / Or buy premium key to remove the limitation";
        if (br.containsHTML(err1)) {
            // I don't see why this would happening logs contain no proxy!
            throw new PluginException(LinkStatus.ERROR_FATAL, err1);
        } else if (account != null && br.getHttpConnection() != null && (br.toString().equals("Your premium has reached the maximum volume for today") || br.containsHTML("<p id=\"error\"[^>]+>Your premium has reached the maximum volume for today|>This download exceeds the daily download limit"))) {
            throw new AccountUnavailableException("Daily downloadlimit reached", 5 * 60 * 1000l);
        } else if (br.containsHTML(">This download exceeds the daily download limit\\. You can purchase")) {
            // not enough traffic to download THIS file, doesn't mean zero traffic left.
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You do not have enough traffic left to start this download.");
        }
        if (lastChance) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("ERROR: link expired. Please unlock the file again")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'link expired'", 2 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void throwPremiumRequiredException(final DownloadLink link, final boolean setProperty) throws PluginException {
        if (setProperty && link != null) {
            link.setProperty("premiumRequired", Boolean.TRUE);
        }
        throw new AccountRequiredException();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        if (property != null) {
            final String dllink = link.getStringProperty(property);
            if (dllink != null) {
                URLConnectionAdapter con = null;
                try {
                    final Browser br2 = br.cloneBrowser();
                    br2.setFollowRedirects(true);
                    con = br2.openHeadConnection(dllink);
                    if (!isDownloadableContent(dl.getConnection())) {
                        link.setProperty(property, Property.NULL);
                    } else {
                        return dllink;
                    }
                } catch (final Exception e) {
                    link.setProperty(property, Property.NULL);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return null;
    }

    private AvailableStatus requestFileInformationApi(final DownloadLink link) throws IOException, PluginException {
        final boolean checked = checkLinks(new DownloadLink[] { link });
        // we can't throw exception in checklinks! This is needed to prevent multiple captcha events!
        if (!checked && hasAntiddosCaptchaRequirement()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (!checked || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /* 2020-06-24: Do NOT reset these properties anymore! */
        if (link != null) {
            // link.setProperty("apiInfo", Property.NULL);
            // link.setProperty("freelink2", Property.NULL);
            // link.setProperty("freelink", Property.NULL);
            // link.setProperty("premlink", Property.NULL);
            link.setProperty("premiumRequired", Property.NULL);
            // link.setProperty("passwordRequired", Property.NULL);
        }
    }

    private String  dllink             = null;
    private String  directlinkproperty = null;
    private int     chunks             = 0;
    private boolean resumes            = true;
    private boolean isFree             = true;

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
        if (acc == null || acc.getType() == AccountType.FREE) {
            /* no account and free account, yes we can expect captcha */
            return true;
        } else {
            return false;
        }
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public Class<NitroflareConfig> getConfigInterface() {
        return NitroflareConfig.class;
    }
}