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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nitroflare.com" }, urls = { "https?://(?:www\\.)?nitroflare\\.com/(?:view|watch)/[A-Z0-9]+" })
public class NitroFlareCom extends antiDDoSForHost {

    private final String         language = System.getProperty("user.language");
    private final String         baseURL  = "https://nitroflare.com";
    private final String         apiURL   = "http://nitroflare.com/api/v2";

    /* don't touch the following! */
    private static AtomicInteger maxFree  = new AtomicInteger(1);

    public NitroFlareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(null);
        setConfigElement();
        Browser.setRequestIntervalLimitGlobal("nitroflare.com", 500);
    }

    @Override
    public String getAGBLink() {
        return baseURL + "/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://").replace(".com/watch/", ".com/view/"));
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
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink != null) {
            if (account == null || account.getType() == AccountType.FREE) {
                return !(downloadLink.getBooleanProperty("premiumRequired", false));
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
                    if (links.size() > 100 || index == urls.length) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                final StringBuilder sb = new StringBuilder();
                sb.append("files=");
                boolean atLeastOneDL = false;
                for (final DownloadLink dl : links) {
                    if (atLeastOneDL) {
                        sb.append(",");
                    }
                    sb.append(getFUID(dl));
                    atLeastOneDL = true;
                }
                getPage(br, apiURL + "/getFileInfo?" + sb);
                if (br.containsHTML("In these moments we are upgrading the site system")) {

                    for (final DownloadLink dl : links) {
                        dl.getLinkStatus().setStatusText("Nitroflare.com is maintenance mode. Try again later");
                        dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);

                    }
                    return true;
                }
                for (final DownloadLink dl : links) {
                    final String filter = br.getRegex("(\"" + getFUID(dl) + "\":\\{.*?\\})").getMatch(0);
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
                    if (getPluginConfig().getBooleanProperty(trustAPIPremiumOnly, true)) {
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
        if (useAPI()) {
            return requestFileInformationApi(link);
        } else {
            return requestFileInformationWeb(link);
        }
    }

    private AvailableStatus requestFileInformationWeb(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML(">File doesn't exist<|This file has been removed due|>\\s*This file has been removed by its owner\\.\\s*<")) {
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
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getBooleanProperty("apiInfo", Boolean.FALSE) == Boolean.FALSE) {
            /* no apiInfos available, set unverified name/size here */
            link.setName(Encoding.htmlDecode(filename.trim()));
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        if (useAPI() && false) {
            handleDownload_API(downloadLink, null);
        } else {
            this.setBrowserExclusive();
            br.setFollowRedirects(true);
            requestFileInformationApi(downloadLink);
            doFree(null, downloadLink);
        }
    }

    private final void doFree(final Account account, final DownloadLink downloadLink) throws Exception {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (downloadLink.getBooleanProperty("premiumRequired", false)) {
            throwPremiumRequiredException(downloadLink, true);
        }
        freedl = true;
        br = new Browser();
        dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            if (br.getURL() == null) {
                requestFileInformationWeb(downloadLink);
            }
            handleErrors(br, false);
            randomHash(downloadLink);
            ajaxPost(br, "/ajax/setCookie.php", "fileId=" + getFUID(downloadLink));
            {
                int i = 0;
                while (true) {
                    // lets add some randomisation between submitting gotofreepage
                    sleep((new Random().nextInt(5) + 8) * 1000l, downloadLink);
                    // first post registers time value
                    postPage(br.getURL(), "goToFreePage=");
                    randomHash(downloadLink);
                    ajaxPost(br, "/ajax/setCookie.php", "fileId=" + getFUID(downloadLink));
                    if (br.getURL().endsWith("/free")) {
                        break;
                    } else if (++i > 3) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        continue;
                    }
                }
            }
            ajaxPost(br, "/ajax/freeDownload.php", "method=startTimer&fileId=" + getFUID(downloadLink));
            handleErrors(ajax, false);
            final long t = System.currentTimeMillis();
            final String waittime = br.getRegex("<div id=\"CountDownTimer\" data-timer=\"(\\d+)\"").getMatch(0);
            // register wait i guess, it should return 1
            final Recaptcha rc = new Recaptcha(br, this);
            rc.findID();
            final int repeat = 5;
            for (int i = 1; i <= repeat; i++) {
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                if (inValidate(c)) {
                    // fixes timeout issues or client refresh, we have no idea at this stage
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (i == 1) {
                    long wait = 60;
                    if (waittime != null) {
                        // remove one second from past, to prevent returning too quickly.
                        final long passedTime = ((System.currentTimeMillis() - t) / 1000) - 1;
                        wait = Long.parseLong(waittime) - passedTime;
                    }
                    if (wait > 0) {
                        sleep(wait * 1000l, downloadLink);
                    }
                }
                ajaxPost(br, "/ajax/freeDownload.php", "method=fetchDownload&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (ajax.containsHTML("The captcha wasn't entered correctly|You have to fill the captcha")) {
                    if (i + 1 == repeat) {
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            downloadLink.setProperty(directlinkproperty, Property.NULL);
            handleDownloadErrors(account, downloadLink, true);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
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
        int totalMaxSimultanFreeDownload = this.getPluginConfig().getBooleanProperty(allowMultipleFreeDownloads, false) ? 20 : 1;
        logger.info("maxFree was = " + maxFree.get() + " total is = " + totalMaxSimultanFreeDownload + " change " + num);
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload));
        logger.info("maxFree now = " + maxFree.get());
    }

    /**
     * this seems to happen in this manner
     *
     * @throws Exception
     **/
    private final void randomHash(final DownloadLink downloadLink) throws Exception {
        final String randomHash = JDHash.getMD5(downloadLink.getDownloadURL() + System.currentTimeMillis());
        // same cookie is set within as a cookie prior to registering
        br.setCookie(getHost(), "randHash", randomHash);
        ajaxPost(br, "/ajax/randHash.php", "randHash=" + randomHash);
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
        }
        if (br.containsHTML("﻿Downloading is not possible") || br.containsHTML("downloading is not possible")) {
            if (this.getPluginConfig().getBooleanProperty(allowMultipleFreeDownloads, false)) {
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
        }
        if (StringUtils.startsWithCaseInsensitive(br.toString(), "﻿Free download is currently unavailable due to overloading in the server. <br>Please try again later")) {
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

    protected static Object LOCK = new Object();

    /**
     * Validates account and returns correct account info, when user has provided incorrect user pass fields to JD client. Or Throws
     * exception indicating users mistake, when it's a irreversible mistake.
     *
     * @param account
     * @return
     * @throws PluginException
     */
    private String validateAccount(final Account account) throws PluginException {
        synchronized (LOCK) {
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
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                String title = "Nitroflare no longer accepts PremiumKeys";
                                String message = "You are using PremiumKey! These are no longer accepted on Nitroflare.\r\n";
                                message += "You will need to bind your PremiumKey to account. Please Click -YES- for more information.";
                                if (CrossSystem.isOpenBrowserSupported()) {
                                    int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                                    if (JOptionPane.OK_OPTION == result) {
                                        CrossSystem.openURL(new URL(baseURL + "/upgradeTutorial"));
                                    }
                                }
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (Throwable e) {
                }

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

    private static String  preferAPI                  = "preferAPI";
    private static String  trustAPIPremiumOnly        = "trustAPIPremiumOnly";
    private static String  allowMultipleFreeDownloads = "allowMultipleFreeDownloads";
    private static boolean preferAPIdefault           = false;

    private void setConfigElement() {
        final ConfigEntry apie = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), preferAPI, "Use API for Premium Accounts and single url linkcheck (API = lots of recaptchav1, WEB = recaptchav2 once.)").setDefaultValue(preferAPIdefault);
        getConfig().addEntry(apie);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), allowMultipleFreeDownloads, "Allow multiple free downloads?\r\nThis might result in fatal errors!").setDefaultValue(false).setEnabledCondidtion(apie, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), trustAPIPremiumOnly, "Trust API about Premium Only flag?").setDefaultValue(true));
    }

    /**
     * useAPI frame work? <br />
     * Override this when incorrect
     *
     * @return
     */
    private boolean useAPI() {
        return false; // getPluginConfig().getBooleanProperty(preferAPI, preferAPIdefault);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (useAPI()) {
            return fetchAccountInfoApi(account);
        } else {
            return fetchAccountInfoWeb(account, false, true);
        }
    }

    private AccountInfo fetchAccountInfoApi(final Account account) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = new AccountInfo();
            final String req = apiURL + "/getKeyInfo?" + validateAccount(account);
            getPage(req);
            handleApiErrors(account, null);
            // recaptcha can happen here on brute force attack
            String recap = PluginJSonUtils.getJsonValue(br, "recaptchaPublic");
            if (!inValidate(recap)) {
                logger.info("Detected captcha method \"Re Captcha\"");
                final Browser captcha = br.cloneBrowser();
                final DownloadLink dummyLink = new DownloadLink(null, "Account Login Requires Recaptcha", this.getHost(), br.getURL(), true);
                final Recaptcha rc = new Recaptcha(captcha, this);
                // after 5 wrong guesses they ban ip/account
                rc.setId(recap);
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                if (inValidate(c)) {
                    // fixes timeout issues or client refresh, we have no idea at this stage
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                getPage(req + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                handleApiErrors(account, null);
                if ("error".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "type")) && "6".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "code"))) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            final String expire = PluginJSonUtils.getJsonValue(br, "expiryDate");
            final String status = PluginJSonUtils.getJsonValue(br, "status");
            final String storage = PluginJSonUtils.getJsonValue(br, "storageUsed");
            final String trafficLeft = PluginJSonUtils.getJsonValue(br, "trafficLeft");
            final String trafficMax = PluginJSonUtils.getJsonValue(br, "trafficMax");
            if (inValidate(status)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!inValidate(expire) && !"0".equalsIgnoreCase(expire)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
            }
            if ("banned".equalsIgnoreCase(status)) {
                if ("de".equalsIgnoreCase(language)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has been banned! (transate me)", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has been banned!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else if ("expired".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status) || ai.isExpired()) {
                // expired(free)? account
                account.setType(AccountType.FREE);
                // dont support free account?
                ai.setStatus("Free Account");
                ai.setExpired(true);
            } else if ("active".equalsIgnoreCase(status)) {
                // premium account
                account.setType(AccountType.PREMIUM);
                ai.setStatus("Premium Account");
                account.setValid(true);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!inValidate(storage)) {
                ai.setUsedSpace(Long.parseLong(storage));
            }
            if (!inValidate(trafficLeft)) {
                ai.setTrafficLeft(Long.parseLong(trafficLeft));
            }
            if (!inValidate(trafficMax)) {
                ai.setTrafficMax(Long.parseLong(trafficMax));
            }
            return ai;
        }
    }

    private AccountInfo fetchAccountInfoWeb(final Account account, boolean fullLogin, boolean fullInfo) throws Exception {
        synchronized (LOCK) {
            if (!account.getUser().matches(".+@.+")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (inValidate(account.getPass())) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPassword can't be empty!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !fullLogin) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(this.getHost(), key, value);
                        }
                        // lets do a test
                        final Browser br2 = br.cloneBrowser();
                        getPage(br2, "https://www.nitroflare.com/");
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
                    getPage("https://nitroflare.com/login");
                    Form f = br.getFormbyProperty("id", "login");
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
                    if (f != null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nIncorrect User/Password", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    // final failover, we expect 'user' cookie
                    final String user = br.getCookie("nitroflare.com", "user");
                    if (user == null || "deleted".equalsIgnoreCase(user)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCould not find Account Cookie", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }

                final AccountInfo ai = new AccountInfo();
                getPage("https://nitroflare.com/member?s=premium");
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
                    account.setProperty("name", Encoding.urlEncode(account.getUser()));
                    account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                    account.setProperty("cookies", fetchCookies(getHost()));
                    return ai;
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Non Valid Account", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } catch (final PluginException e) {
                account.setProperty("name", Property.NULL);
                account.setProperty("pass", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account);
        if (useAPI()) {
            handleDownload_API(downloadLink, account);
            return;
        }
        // when they turn off additional captchas within api vs website, we will go back to website
        requestFileInformationWeb(downloadLink);
        fetchAccountInfoWeb(account, false, false);
        // is free user?
        if (account.getType() == AccountType.FREE) {
            requestFileInformationApi(downloadLink); // Required, to do checkLinks to check premiumOnly
            doFree(account, downloadLink);
            return;
        }
        // check cached download
        dllink = downloadLink.getStringProperty(directlinkproperty);
        if (!inValidate(dllink)) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
            if (dl.getConnection().isContentDisposition()) {
                downloadLink.setProperty(directlinkproperty, dllink);
                dl.startDownload();
                return;
            }
            downloadLink.setProperty(directlinkproperty, Property.NULL);
            handleDownloadErrors(account, downloadLink, false);
        }
        // could be directlink
        dllink = downloadLink.getDownloadURL();
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
        if (dl.getConnection().isContentDisposition()) {
            downloadLink.setProperty(directlinkproperty, dllink);
            dl.startDownload();
            return;
        }
        br.followConnection();
        // not directlink
        randomHash(downloadLink);
        ajaxPost(br, "/ajax/setCookie.php", "fileId=" + getFUID(downloadLink));
        handleDownloadErrors(account, downloadLink, false);
        dllink = br.getRegex("<a id=\"download\" href=\"([^\"]+)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Can't find dllink!");
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
        if (dl.getConnection().isContentDisposition()) {
            downloadLink.setProperty(directlinkproperty, dllink);
            dl.startDownload();
            return;
        }
        handleDownloadErrors(account, downloadLink, true);
    }

    private void handleDownload_API(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformationApi(downloadLink);
        dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (inValidate(dllink)) {
            // links that require premium...
            if (downloadLink.getBooleanProperty("premiumRequired", false) && account == null) {
                throwPremiumRequiredException(downloadLink, true);
            }
            String req = apiURL + "/getDownloadLink?file=" + getFUID(downloadLink) + (account != null ? "&" + validateAccount(account) : "");
            // needed for when dropping to frame, the cookie session seems to carry over current position in download sequence and you get
            // recaptcha error codes at first step.
            br = new Browser();
            getPage(req);
            handleApiErrors(account, downloadLink);
            // error handling here.
            if ("free".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "linkType"))) {
                String accessLink = PluginJSonUtils.getJsonValue(br, "accessLink");
                if (inValidate(accessLink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                req = apiURL + "/" + accessLink;
                // wait
                String delay = PluginJSonUtils.getJsonValue(br, "delay");
                long startTime = System.currentTimeMillis();
                String recap = PluginJSonUtils.getJsonValue(br, "recaptchaPublic");
                if (!inValidate(recap)) {
                    logger.info("Detected captcha method \"Re Captcha\"");
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.setId(recap);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    if (!inValidate(delay)) {
                        sleep((Long.parseLong(delay) * 1000) - (System.currentTimeMillis() - startTime), downloadLink);
                    }
                    if (inValidate(c)) {
                        // fixes timeout issues or client refresh, we have no idea at this stage
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    getPage(req + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                    if (("error".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "type")) && "6".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "code"))) || (!inValidate(PluginJSonUtils.getJsonValue(br, "accessLink")) && !inValidate(PluginJSonUtils.getJsonValue(br, "recaptchaPublic")))) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
            }
            // some times error 4 is found here
            handleApiErrors(account, downloadLink);
            dllink = PluginJSonUtils.getJsonValue(br, "url");
            if (inValidate(dllink)) {
                if (br.toString().matches("Connect failed: Can't connect to local MySQL server.+")) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
        if (!dl.getConnection().isContentDisposition()) {
            downloadLink.setProperty(directlinkproperty, Property.NULL);
            handleDownloadErrors(account, downloadLink, true);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
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
            synchronized (LOCK) {
                final AccountInfo ai = account.getAccountInfo();
                ai.setTrafficLeft(0);
                account.setAccountInfo(ai);
                account.setTempDisabled(true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        } else if (br.containsHTML(">This download exceeds the daily download limit\\. You can purchase")) {
            // not enough traffic to download THIS file, doesn't mean 0 traffic left.
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You do not have enough traffic left to start this download.");
        }
        if (lastChance) {
            if (br.containsHTML("ERROR: link expired. Please unlock the file again")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'link expired'", 2 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void handleApiErrors(final Account account, final DownloadLink downloadLink) throws Exception {
        // API Error handling codes.
        // 1 => 'Access denied', (banned for trying incorrect x times for y minutes
        // 2 => 'Invalid premium key',
        // 3 => 'Bad input',
        // 4 => 'File doesn't exist',
        // 5 => 'Free downloading is not possible. You have to wait 60 minutes between free downloads.',
        // 6 => 'Invalid captcha',
        // 7 => 'Free users can download only 1 file at the same time'
        // 8 => ﻿{"type":"error","message":"Wrong login","code":8}
        if (br.containsHTML("In these moments we are upgrading the site system")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Nitroflare.com is maintenance mode. Try again later", 60 * 60 * 1000);

        }
        final String type = PluginJSonUtils.getJsonValue(br, "type");
        final String code = PluginJSonUtils.getJsonValue(br, "code");
        final String msg = PluginJSonUtils.getJsonValue(br, "message");
        final int cde = (!inValidate(code) && code.matches("\\d+") ? Integer.parseInt(code) : -1);
        if ("error".equalsIgnoreCase(type)) {
            try {
                if (cde == 1) {
                    if (account == null) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, (!inValidate(msg) ? msg : null), 60 * 60 * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, (!inValidate(msg) ? msg : null), 60 * 60 * 1000);
                    }
                } else if (cde == 2) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (cde == 3 && downloadLink != null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, (!inValidate(msg) ? msg : null));
                } else if (cde == 4 && downloadLink != null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, (!inValidate(msg) ? msg : null));
                } else if (cde == 5 && downloadLink != null) {
                    final String time = new Regex(msg, "You have to wait (\\d+) minutes").getMatch(0);
                    final long t = (!inValidate(time) ? Long.parseLong(time) * 60 * 1000 : 1 * 60 * 60 * 1000);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (!inValidate(msg) ? msg : null), t);
                } else if (cde == 7 && downloadLink != null) {
                    // shouldn't happen as its hard limited.
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (!inValidate(msg) ? msg : "You can't download more than one file within a certain time period in free mode"), 60 * 60 * 1000l);
                } else if (cde == 8) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nIncorrect login attempt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (cde == 9) {
                    if (account != null) {
                        account.setAccountInfo(fetchAccountInfo(account));
                        throw new PluginException(LinkStatus.ERROR_RETRY, (!inValidate(msg) ? msg : null));
                    } else {
                        // this shouldn't happen
                    }
                }
            } catch (final PluginException p) {
                if (!inValidate(msg)) {
                    logger.warning("ERROR :: " + msg);
                }
                throw p;
            }
        }
    }

    private void throwPremiumRequiredException(DownloadLink link, boolean setProperty) throws PluginException {
        if (setProperty && link != null) {
            link.setProperty("premiumRequired", Boolean.TRUE);
        }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        if (property != null) {
            final String dllink = downloadLink.getStringProperty(property);
            if (dllink != null) {
                URLConnectionAdapter con = null;
                try {
                    final Browser br2 = br.cloneBrowser();
                    br2.setFollowRedirects(true);
                    con = br2.openHeadConnection(dllink);
                    if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                        downloadLink.setProperty(property, Property.NULL);
                    } else {
                        return dllink;
                    }
                } catch (final Exception e) {
                    downloadLink.setProperty(property, Property.NULL);
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
        if (link != null) {
            link.setProperty("apiInfo", Property.NULL);
            link.setProperty("freelink2", Property.NULL);
            link.setProperty("freelink", Property.NULL);
            link.setProperty("premlink", Property.NULL);
            link.setProperty("premiumRequired", Property.NULL);
            link.setProperty("passwordRequired", Property.NULL);
        }
    }

    private String  dllink             = null;
    private String  directlinkproperty = null;
    private int     chunks             = 0;
    private boolean resumes            = true;
    private boolean isFree             = true;

    private String getFUID(DownloadLink downloadLink) {
        final String fuid = new Regex(downloadLink.getDownloadURL(), "nitroflare\\.com/view/([A-Z0-9]+)").getMatch(0);
        return fuid;
    }

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    protected void runPostRequestTask(final Browser ibr) throws PluginException {
        if (ibr.containsHTML(">OUR WEBSITE IS UNDER CONSTRUCTION</strong>")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Website Under Construction!", 15 * 60 * 1000l);
        }
    }

}