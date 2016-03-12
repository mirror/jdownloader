//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "redstream.biz" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424" }, flags = { 2 })
public class RedstreamBiz extends PluginForHost {

    private static final String                            DOMAIN               = "http://redstream.biz/api";
    private static final String                            NICE_HOST            = "redstream.biz";
    private static final String                            NICE_HOSTproperty    = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NORESUME             = NICE_HOSTproperty + "NORESUME";

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap   = new HashMap<Account, HashMap<String, Long>>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Boolean>                hostResumeMap        = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>                hostMaxchunksMap     = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>                hostMaxdlsMap        = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger>          hostRunningDlsNumMap = new HashMap<String, AtomicInteger>();

    /* Last updated: 31.03.15 */
    private static final int                               defaultMAXDOWNLOADS  = 10;
    private static final int                               defaultMAXCHUNKS     = 1;
    private static final boolean                           defaultRESUME        = true;

    private static Object                                  CTRLLOCK             = new Object();
    private int                                            statuscode           = 0;
    private static AtomicInteger                           maxPrem              = new AtomicInteger(1);
    private Account                                        currAcc              = null;
    private DownloadLink                                   currDownloadLink     = null;

    @SuppressWarnings("deprecation")
    public RedstreamBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://redstream.biz/payment");
    }

    @Override
    public String getAGBLink() {
        return "http://redstream.biz/forum";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        final String currentHost = this.correctHost(downloadLink.getHost());
        /* Make sure that we do not start more than the allowed number of max simultan downloads for the current host. */
        synchronized (hostRunningDlsNumMap) {
            if (hostRunningDlsNumMap.containsKey(currentHost) && hostMaxdlsMap.containsKey(currentHost)) {
                final int maxDlsForCurrentHost = hostMaxdlsMap.get(currentHost);
                final AtomicInteger currentRunningDlsForCurrentHost = hostRunningDlsNumMap.get(currentHost);
                if (currentRunningDlsForCurrentHost.get() >= maxDlsForCurrentHost) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        this.setConstants(account, link);
        handleDL(account, link);
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            /* request creation of downloadlink */
            this.getAPISafe("http://" + NICE_HOST + "/links/downloads");
            Form dlform = this.br.getFormbyProperty("id", "filehost-links-downloads-form");
            if (dlform == null) {
                dlform = this.br.getForm(0);
            }
            if (dlform == null) {
                handleErrorRetries("dlform_null", 10, 5 * 60 * 1000l);
            }
            dlform.put("urls", Encoding.urlEncode(link.getDownloadURL()));
            this.postAPIForm(dlform);
            this.getAPISafe(DOMAIN + "/downloads");

            /* Server returns HashMap with numbers instead of simply an ArrayList containing the needed information ... */
            LinkedHashMap<String, Object> entries = (LinkedHashMap) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(this.br.toString());
            final String linkpart = new Regex(link.getDownloadURL(), "https?://[^/]+/(.+)").getMatch(0);
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            LinkedHashMap<String, Object> download = null;
            String url_temp = null;
            String url_final_temp = null;
            while (it.hasNext()) {
                download = (LinkedHashMap<String, Object>) it.next().getValue();
                if (entries == null) {
                    continue;
                }
                url_temp = (String) download.get("link");
                url_final_temp = (String) download.get("url");
                if (inValidate(url_temp) || inValidate(url_final_temp)) {
                    continue;
                }
                if (url_temp.contains(linkpart)) {
                    dllink = url_final_temp;
                    break;
                }
            }

            if (inValidate(dllink)) {
                logger.warning("Final downloadlink is null");
                handleErrorRetries("dllinknull", 10, 5 * 60 * 1000l);
            }
        }
        boolean resume = account.getBooleanProperty("resume", defaultRESUME);
        int maxChunks = account.getIntegerProperty("account_maxchunks", defaultMAXCHUNKS);
        /* Then check if we got an individual host limit. */
        if (hostMaxchunksMap != null) {
            final String thishost = link.getHost();
            synchronized (hostMaxchunksMap) {
                if (hostMaxchunksMap.containsKey(thishost)) {
                    maxChunks = hostMaxchunksMap.get(thishost);
                }
            }
        }

        if (hostResumeMap != null) {
            final String thishost = link.getHost();
            synchronized (hostResumeMap) {
                if (hostResumeMap.containsKey(thishost)) {
                    resume = hostResumeMap.get(thishost);
                }
            }
        }
        if (link.getBooleanProperty(NORESUME, false)) {
            resume = false;
        }
        if (!resume) {
            maxChunks = 1;
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            updatestatuscode();
            handleAPIErrors(this.br);
            handleErrorRetries("unknowndlerror", 10, 5 * 60 * 1000l);
        }
        try {
            controlSlot(+1);
            this.dl.startDownload();
        } finally {
            // remove usedHost slot from hostMap
            // remove download slot
            controlSlot(-1);
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        this.setConstants(account, link);
        login(false);

        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
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
    private void handleErrorRetries(final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        this.currDownloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(disableTime);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();

        login(true);

        final String accounttype = getJson("type");
        final String validuntil = getJson("date");
        final String trafficleft = getJson("traffic");
        long timestamp_validuntil = 0;
        if (validuntil != null) {
            timestamp_validuntil = Long.parseLong(validuntil) * 1000;
        }

        if ("premium".equals(accounttype)) {
            ai.setValidUntil(timestamp_validuntil);
            if (trafficleft != null && trafficleft.matches("\\d+.*?")) {
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            } else {
                ai.setUnlimitedTraffic();
            }
            ai.setStatus("Premium account");
            account.setType(AccountType.PREMIUM);
        } else {
            /* According to admin, Free Accounts cannot download anything. */
            ai.setTrafficLeft(0);
            ai.setStatus("Registered (free) account");
            account.setType(AccountType.FREE);
        }

        this.getAPISafe(DOMAIN + "/hosters");
        ArrayList<String> supportedhostslist = new ArrayList();
        LinkedHashMap<String, Object> entries = null;
        final ArrayList<Object> ressourcelist = (ArrayList) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(this.br.toString());

        for (final Object hostinfoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) hostinfoo;
            final int maxdownloads = this.correctMaxdls((int) jd.plugins.hoster.DummyScriptEnginePlugin.toLong(entries.get("maxDowloads"), defaultMAXDOWNLOADS));
            final int maxchunks = this.correctChunks((int) jd.plugins.hoster.DummyScriptEnginePlugin.toLong(entries.get("maxChunks"), defaultMAXCHUNKS));
            final String host = ((String) entries.get("host")).toLowerCase();

            boolean resumable = defaultRESUME;
            final Object resumableo = entries.get("resumable");
            if (resumableo instanceof Boolean) {
                resumable = ((Boolean) resumableo).booleanValue();
            } else {
                resumable = Boolean.parseBoolean((String) resumableo);
            }

            hostMaxchunksMap.put(host, maxchunks);
            hostMaxdlsMap.put(host, maxdownloads);
            hostResumeMap.put(host, resumable);
            supportedhostslist.add(host);
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);

        hostMaxchunksMap.clear();
        hostMaxdlsMap.clear();
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final boolean force) throws Exception {
        String user_email = null;
        final Cookies cookies = this.currAcc.loadCookies("");
        if (cookies != null) {
            this.br.setCookies(this.getHost(), cookies);
            this.getAPISafe(DOMAIN + "/check-user");
            user_email = getJson("account");
            if (user_email == null) {
                logger.info("Current logincookies are invalid --> Performing full login");
            } else {
                logger.info("Current logincookies are still valid");
                this.currAcc.saveCookies(this.br.getCookies(this.getHost()), "");
                return;
            }
        }
        this.getAPISafe("http://" + NICE_HOST);
        Form loginform = this.br.getFormbyProperty("id", "user-login-form");
        if (loginform == null) {
            loginform = this.br.getForm(0);
        }
        if (loginform == null) {

        }
        loginform.put("name", Encoding.urlEncode(this.currAcc.getUser()));
        loginform.put("pass", Encoding.urlEncode(this.currAcc.getPass()));

        this.postAPIForm(loginform);
        this.getAPISafe(DOMAIN + "/check-user");
        user_email = getJson("account");
        if (user_email == null) {
            /* No json response --> Invalid logindata! */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        this.currAcc.saveCookies(this.br.getCookies(this.getHost()), "");
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        } else if (this.currDownloadLink.getHost().equals(this.getHost())) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        this.br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        this.br.postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPIForm(final Form form) throws Exception {
        this.br.submitForm(form);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /** Performs slight domain corrections. */
    private String correctHost(String host) {
        if (host.equals("uploaded.to") || host.equals("uploaded.net")) {
            host = "uploaded.to";
        }
        return host;
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * @param controlSlot
     *            (+1|-1)
     * */
    private void controlSlot(final int num) {
        synchronized (CTRLLOCK) {
            final String currentHost = correctHost(this.currDownloadLink.getHost());
            int was = maxPrem.get();
            maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), this.currAcc.getIntegerProperty("account_maxdls", defaultMAXDOWNLOADS)));
            logger.info("maxPrem was = " + was + " && maxPrem now = " + maxPrem.get());
            AtomicInteger currentRunningDls = new AtomicInteger(0);
            if (hostRunningDlsNumMap.containsKey(currentHost)) {
                currentRunningDls = hostRunningDlsNumMap.get(currentHost);
            }
            currentRunningDls.set(currentRunningDls.get() + num);
            hostRunningDlsNumMap.put(currentHost, currentRunningDls);
        }
    }

    /**
     * 0 = everything ok, 1-99 = official errorcodes, 100-199 = other (html) errors, 666 = hell
     */
    private void updatestatuscode() {
        /* First look for errorcode */
        final String error = this.getJson("errcode");
        if (!inValidate(error)) {
            statuscode = Integer.parseInt(error);
        } else {
            if (this.br.containsHTML("Извините, это имя пользователя или пароль неверны")) {
                statuscode = 100;
            } else {
                /* Seems like everything is fine */
                statuscode = 0;
            }
        }
    }

    /** List of possible errorcodes: http://redstream.biz/api/errors-list */
    /** 2016-03-12: Codes 4, 5 and 7 were removed */
    private void handleAPIErrors(final Browser br) throws PluginException {
        final String lang = System.getProperty("user.language");
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                statusMessage = "Authorization failed";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 2:
                statusMessage = "File not found";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 3:
                statusMessage = "Traffic limit is exceeded";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 4:
                statusMessage = "Traffic quota exceeded";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 5:
                statusMessage = "Account has expired";
                this.currAcc.getAccountInfo().setExpired(true);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 6:
                statusMessage = "Premium account has expired";
                this.currAcc.getAccountInfo().setExpired(true);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 7:
                statusMessage = "Username or password is incorrect";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 8:
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_expired_download_ticket", 20, 5 * 60 * 1000l);
            case 9:
                statusMessage = "Hoster is not supported";
                tempUnavailableHoster(5 * 60 * 1000l);
            case 100:
                statusMessage = "Username or password is incorrect";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 666:
                // /* Unknown error */
                // statusMessage = "Unknown error";
                // logger.info(NICE_HOST + ": Unknown API error");
                // handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 20, 5 * 60 * 1000l);
                /* TODO: Remove all "defect" errors once test phase is finished! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            default:
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    /** Corrects input so that it fits what we use in our plugins. */
    private int correctChunks(int maxchunks) {
        if (maxchunks < 1) {
            maxchunks = 1;
        } else if (maxchunks > 1) {
            maxchunks = -maxchunks;
        }
        /* Else maxchunks == 1 */
        return maxchunks;
    }

    /** Corrects input so that it fits what we use in our plugins. */
    private int correctMaxdls(int maxdls) {
        if (maxdls < 1) {
            maxdls = 1;
        } else if (maxdls > 20) {
            maxdls = 20;
        }
        /* Else we should have a valid value! */
        return maxdls;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}