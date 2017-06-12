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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumax.net" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class PremiumaxNet extends antiDDoSForHost {
    private static MultiHosterManagement mhm               = new MultiHosterManagement("premiumax.net");
    private static final String          NOCHUNKS          = "NOCHUNKS";
    private static final String          MAINPAGE          = "http://premiumax.net";
    private static final String          NICE_HOST         = "premiumax.net";
    private static final String          NICE_HOSTproperty = "premiumaxnet";

    public PremiumaxNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.premiumax.net/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.premiumax.net/more/terms-and-conditions.html";
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        // check if account is valid
        boolean is_freeaccount = false;
        if (true) {
            login(account, true);
            getPage("/profile/");
            final String expire = br.getRegex("<span>Premium until: </span><strong>([^<>\"]*?)</strong>").getMatch(0);
            if (expire != null) {
                ac.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm", Locale.ENGLISH));
                ac.setStatus("Premium Account");
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } else {
                ac.setStatus("Free Account");
                is_freeaccount = true;
                account.setMaxSimultanDownloads(20);
                account.setConcurrentUsePossible(true);
            }
            ac.setUnlimitedTraffic();
            // now let's get a list of all supported hosts:
        } else {
            // just for testing Account.setMultiHostSupport
            getPage("http://www.premiumax.net/");
        }
        getPage("/hosts.html");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomainsInfo = br.getRegex("(<img src=\"/assets/images/hosts/.*?)</tr>").getColumn(0);
        for (String hinfo : hostDomainsInfo) {
            String crippledhost = new Regex(hinfo, "images/hosts/([a-z0-9\\-\\.]+)\\.png\"").getMatch(0);
            if (crippledhost == null) {
                logger.warning("WTF");
            }
            if ("file".equals(crippledhost)) {
                // first span does have full host. re: file.al
                final String host = new Regex(hinfo, "<img src=\"/assets/images/hosts/[^>]+>\\s*<span>(.*?)</span>").getMatch(0);
                if (host != null && host.length() > crippledhost.length()) {
                    crippledhost = host;
                }
            } else if (crippledhost.equalsIgnoreCase("k2share")) {
                /* Spelling mistake on their website --> Correct that */
                crippledhost = "keep2share";
            } else if (crippledhost.equalsIgnoreCase("2shared")) {
                /* Avoid adding keep2share.cc instead of 2shared.com. */
                crippledhost = "2shared.com";
            } else if (crippledhost.equalsIgnoreCase("loaded")) {
                /* Avoid adding uploaded.net for free accounts. */
                crippledhost = "loaded.to";
            } else if (crippledhost.equalsIgnoreCase("mediafire")) {
                /* We don't want to add "mediafire.bz" by mistake --> Correct this here. */
                crippledhost = "mediafire.com";
            } else if (crippledhost.equalsIgnoreCase("filespace")) {
                /* We don't want to add "mediafire.bz" by mistake --> Correct this here. */
                crippledhost = "filespace.com";
            }
            final String[] imgs = new Regex(hinfo, "src=\"(tmpl/images/[^<>\"]*?)\"").getColumn(0);
            /* Apply supported hosts depending on account type */
            if (imgs != null && imgs.length >= 4 && imgs[3].equals("tmpl/images/ico_yes.png") && (!is_freeaccount && imgs[2].equals("tmpl/images/ico_yes.png") || is_freeaccount && imgs[1].equals("tmpl/images/ico_yes.png"))) {
                supportedHosts.add(crippledhost);
            }
        }
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        String dllink = null;
        synchronized (LOCK) {
            login(account, true);
            dllink = checkDirectLink(link, "premiumaxnetdirectlink");
            if (StringUtils.isEmpty(dllink)) {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept", "*/*");
                brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                brc.getHeaders().put("X-JDownloaderPlugin", String.valueOf(getLazyP().getVersion()));
                brc.getHeaders().put("PM-RAY", br.getCookie(getHost(), "premiumax_check"));
                postPage(brc, "/direct_link.html?rand=0." + System.currentTimeMillis(), "captcka=&key=indexKEY&urllist=" + Encoding.urlEncode(link.getDownloadURL()));
                dllink = brc.getRegex("\"(https?://(www\\.)?premiumax\\.net/dl\\d*/[a-z0-9]+/?)\"").getMatch(0);
                if (dllink == null) {
                    if (brc.getHttpConnection().getResponseCode() == 500) {
                        handleErrorRetries(link, account, "500 Internal Server Error", 20, 5 * 60 * 1000l);
                    } else if (brc.containsHTML("temporary problem")) {
                        logger.info("Current hoster is temporarily not available via premiumax.net -> Disabling it");
                        mhm.putError(account, link, 60 * 60 * 1000l, "Temporary MultiHoster issue (Disabled Host)");
                    } else if (brc.containsHTML("You do not have the rights to download from")) {
                        logger.info("Current hoster is not available via this premiumax.net account -> Disabling it");
                        mhm.putError(account, link, 60 * 60 * 1000l, "No rights to download from " + link.getHost() + " (Disabled Host)");
                    } else if (brc.containsHTML("We do not support your link")) {
                        logger.info("Current hoster is not supported by premiumax.net -> Disabling it");
                        // global issue
                        mhm.putError(null, link, 3 * 60 * 60 * 1000l, "Unsupported link format (Disabled Host)");
                    } else if (brc.containsHTML("You only can download")) {
                        /* We're too fast - usually this should not happen */
                        handleErrorRetries(link, account, "Too many active connections", 10, 5 * 60 * 1000l);
                    } else if (brc.containsHTML("> Our server can't connect to")) {
                        handleErrorRetries(link, account, "cantconnect", 20, 5 * 60 * 1000l);
                    } else if (brc.toString().equalsIgnoreCase("Traffic limit exceeded") || brc.containsHTML("^<div class=\"res_bad\">Traffic limit exceeded</div>")) {
                        // traffic limit per host, resets every 24 hours... http://www.premiumax.net/hosts.html
                        mhm.putError(account, link, determineTrafficResetTime(), "Traffic limit exceeded for " + link.getHost());
                    } else if (brc.toString().equalsIgnoreCase("nginx error") || brc.containsHTML("There are too many attempts")) {
                        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Nginx Error", 30 * 1000l);
                        dumpAccountSessionInfo(account);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    } else if (brc.containsHTML(">\\s*Our [\\w\\-\\.]+ account has reach bandwidth limit\\s*<")) {
                        // global issue
                        mhm.putError(null, link, 1 * 60 * 60 * 1000l, "Multihoster has no download traffic for " + link.getHost());
                    } else if (brc.containsHTML("<font color=red>\\s*Link Dead\\s*!!!\\s*</font>")) {
                        // not trust worthy in my opinion. see jdlog://0535035891641
                        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        throw new PluginException(LinkStatus.ERROR_FATAL, "They claim file is offline!");
                    } else if (brc.containsHTML("^<b><font color=red>\\s*There's no \\S+ account in database\\. Contact admin!\\s*</font></b>")) {
                        // <b><font color=red> There's no zippyshare.com account in database. Contact admin! </font></b>
                        /*
                         * <b><font color=red> There's no zippyshare.com account in database. Contact admin! </font></b>||var el =
                         * document.createElement("iframe");...
                         */
                        // this error is multihoster wide, not specific to account... -raztoki20160921
                        mhm.putError(null, link, 15 * 60 * 1000l, "Provider has no account's available to download with.");
                    } else {
                        // final failover! dllink == null
                        handleErrorRetries(link, account, "dllinknullerror", 10, 5 * 60 * 1000l);
                    }
                }
            }
        }
        int maxChunks = 0;
        if (link.getBooleanProperty(PremiumaxNet.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                handleErrorRetries(link, account, "404servererror", 10, 5 * 60 * 1000l);
            }
            br.followConnection();
            logger.info("Unhandled download error on premiumax.net: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Workaround for possible double filename bug: http://board.jdownloader.org/showthread.php?t=59540 */
        String finalname = link.getFinalFileName();
        if (finalname == null) {
            finalname = link.getName();
        }
        if (finalname != null) {
            link.setFinalFileName(finalname);
        }
        link.setProperty("premiumaxnetdirectlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PremiumaxNet.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumaxNet.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(PremiumaxNet.NOCHUNKS, false) == false) {
                link.setProperty(PremiumaxNet.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    /**
     * determines how much time left in current day, based on server DATE header (Assume DATE header is this is server time).
     *
     * @author raztoki
     * @return
     */
    private final long determineTrafficResetTime() {
        final String dateString = br.getHttpConnection().getHeaderField("Date");
        if (dateString != null) {
            long serverTime = -1;
            serverTime = TimeFormatter.getMilliSeconds(dateString, "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            if (serverTime <= 0) {
                final Date date = TimeFormatter.parseDateString(dateString);
                if (date != null) {
                    serverTime = date.getTime();
                }
            }
            if (serverTime > 0) {
                // we now need to determine when the next day starts.
                final Calendar c = Calendar.getInstance();
                c.setTimeInMillis(serverTime);
                c.add(Calendar.DAY_OF_MONTH, 1);
                // add one minute offset?
                c.set(Calendar.MINUTE, 1);
                final long t = c.getTimeInMillis() - serverTime;
                return t;
            }
        }
        // 1 hour
        return 60 * 60 * 1000l;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final DownloadLink link, final Account account, final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            link.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            sleep(5000l, link);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            link.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            mhm.putError(account, link, disableTime, error);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean ifrd = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser().toLowerCase()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser().toLowerCase())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        // re-use same agent from cached session.
                        final String ua = account.getStringProperty("ua", null);
                        if (ua != null && !ua.equals(userAgent.get())) {
                            // cloudflare routine sets user-agent on first request.
                            userAgent.set(ua);
                        }
                        /* Avoids unnerving login captchas */
                        if (force) {
                            getPage("http://www.premiumax.net/");
                            if (br.containsHTML(">Sign out</a>")) {
                                return true;
                            } else {
                                br.clearCookies(MAINPAGE);
                                logger.info("Seems like the cookies are no longer valid -> Doing a full refresh");
                                if (logger instanceof LogSource) {
                                    ((LogSource) logger).flush();
                                }
                            }
                        } else {
                            return true;
                        }
                    }
                }
                getPage("http://www.premiumax.net/");
                // lets use form, if they change shit we can at least have it fairly well detected. static posts will break easier/quicker.
                final Form login = br.getFormByInputFieldKeyValue("password", null);
                if (login == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // seem to only accept username when it was to lower case within webbrowser.
                login.put("username", Encoding.urlEncode(account.getUser().toLowerCase()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                // old captcha type
                if (login.containsHTML("div class=\"g-recaptcha\"")) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "premiumax.net", "http://premiumax.net", true);
                    this.setDownloadLink(dummyLink);
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else {
                    // left in as a fail over.
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "premiumax.net", "http://premiumax.net", true);
                    String captchaURL = br.getRegex("<td><a href=\"http://(?:www\\.)?premiumax\\.net//#login_re\"><img src=\"(http://[^<>\"]*?)\"").getMatch(0);
                    if (captchaURL == null) {
                        captchaURL = "/veriword.php";
                    }
                    final String code = getCaptchaCode(captchaURL, dummyLink);
                    login.put("formcode", Encoding.urlEncode(code));
                }
                login.put("serviceButtonValue", "login");
                login.put("service", "login");
                br.submitForm(login);
                final String cookie = br.getCookie(MAINPAGE, "WebLoginPE");
                if (cookie == null || StringUtils.equalsIgnoreCase(cookie, "deleted")) {
                    if (br.containsHTML(">You are blocked and cannot log in!<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "You are blocked and cannot log in!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser().toLowerCase()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(MAINPAGE));
                account.setProperty("ua", br.getHeaders().get("User-Agent"));
                return true;
            } catch (final PluginException e) {
                dumpAccountSessionInfo(account);
                throw e;
            } finally {
                br.setFollowRedirects(ifrd);
            }
        }
    }

    private void dumpAccountSessionInfo(final Account account) throws PluginException {
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setProperty("name", Property.NULL);
        account.setProperty("password", Property.NULL);
        account.setProperty("cookies", Property.NULL);
        account.setProperty("ua", Property.NULL);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_MultihosterScript;
    }
}