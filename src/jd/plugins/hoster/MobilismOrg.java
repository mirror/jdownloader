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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkInfo;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

/**
 *
 * note: cloudflare<br/>
 * note: transloads downloads<br/>
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mobilism.org" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class MobilismOrg extends antiDDoSForHost {

    /* Tags: Script vinaget.us */
    private static final String                   DOMAIN               = "http://mblservices.org";
    private static final String                   NICE_HOST            = "mobilism.org";
    private static final String                   NICE_HOSTproperty    = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                   NORESUME             = NICE_HOSTproperty + "NORESUME";

    private static MultiHosterManagement          mhm                  = new MultiHosterManagement("mobilism.org");
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Boolean>       hostResumeMap        = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>       hostMaxchunksMap     = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>       hostMaxdlsMap        = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger> hostRunningDlsNumMap = new HashMap<String, AtomicInteger>();

    /* Last updated: 31.03.15 */
    private static final int                      defaultMAXDOWNLOADS  = 20;
    private static final int                      defaultMAXCHUNKS     = 0;
    private static final boolean                  defaultRESUME        = true;

    private static Object                         CTRLLOCK             = new Object();
    private static AtomicInteger                  maxPrem              = new AtomicInteger(1);
    private Account                               currAcc              = null;
    private DownloadLink                          currDownloadLink     = null;
    private static Object                         LOCK                 = new Object();

    @SuppressWarnings("deprecation")
    public MobilismOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("forum.mobilism.org/viewtopic.php?f=398&t=284351");
    }

    @Override
    public String getAGBLink() {
        return "http://images.mobilism.org/";
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.addAllowedResponseCodes(401);
        }
        return prepBr;
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        // long fsize = downloadLink.getVerifiedFileSize();
        // if (fsize == -1) {
        // fsize = downloadLink.getDownloadSize();
        // }
        final LinkInfo linkInfo = downloadLink.getLinkInfo();
        if (CompiledFiletypeFilter.VideoExtensions.MP4.isSameExtensionGroup(linkInfo.getExtension())) {
            // videos are no longer allowed
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
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxChunks);
        if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(MobilismOrg.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            mhm.handleErrorGeneric(this.currAcc, this.currDownloadLink, "unknowndlerror", 10, 5 * 60 * 1000l);
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
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        br = new Browser();
        final boolean forceNewLinkGeneration = true;
        /*
         * When JD is started the first time and the user starts downloads right away, a full login might not yet have happened but it is
         * needed to get the individual host limits.
         */
        synchronized (CTRLLOCK) {
            if (hostMaxchunksMap.isEmpty() || hostMaxdlsMap.isEmpty()) {
                logger.info("Performing full login to set individual host limits");
                this.fetchAccountInfo(account);
            } else {
                login(account, false);
            }
        }
        this.setConstants(account, link);

        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null || forceNewLinkGeneration) {
            /* request creation of downloadlink */
            br = new Browser();
            br.setFollowRedirects(true);
            login(account, false);
            getPage("http://mblservices.org/amember/downloader/manual/?");
            // form
            final String urlInputFieldName = "add_to_url";
            final Form d1 = br.getFormByInputFieldKeyValue(urlInputFieldName, "");
            if (d1 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            d1.put(urlInputFieldName, Encoding.urlEncode(link.getDownloadURL()));
            submitForm(d1);
            // another form effectively
            final Form d2 = br.getFormbyProperty("name", "transload");
            if (d2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            d2.put("premium_acc", "on");
            submitForm(d2);
            final Form d3 = br.getFormbyAction("/amember/downloader/downloader/app/index.php");
            if (d3 == null) {
                // instead of d3 form you can get errors outputs directly from there scripts. for example rapidgator an premium cookie could
                // not be found
                // <div id="mesg" width="100%" align="center">Processing. Remain patient.</div><div align="center"><span
                // class='htmlerror'><b>Login Error: Cannot find 'user__' cookie.</b></span>
                final boolean header = br.containsHTML("<div id=\"mesg\" width=\"100%\" align=\"center\">.*?</div>");
                final String error = br.getRegex("<span class='htmlerror'><b>(.*?)</b></span>").getMatch(0);
                if (header && error != null) {
                    // server side issues...
                    mhm.handleErrorGeneric(this.currAcc, this.currDownloadLink, "multihoster_issue", 2, 10 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            submitForm(d3);
            final String regex_dllink = "(https?://[^\"]+/downloader/app/files/[^\"]+)\"";
            dllink = br.getRegex(regex_dllink).getMatch(0);
            long time = 0;
            long wait = 5 * 60 * 1000l;
            if (dllink == null) {
                do {
                    // transloading bullshit...
                    sleep(1 * 60 * 1000l, link);
                    submitForm(d3);
                    dllink = br.getRegex(regex_dllink).getMatch(0);
                } while (dllink == null && time < 5 * 60 * 1000l);
            }
            if (dllink == null && time > wait) {
                logger.warning("Final downloadlink is null");
                mhm.handleErrorGeneric(this.currAcc, this.currDownloadLink, "dllinknull", 2, 10 * 60 * 1000l);
            }
        }
        handleDL(account, link, dllink);
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

    @SuppressWarnings({ "deprecation" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        br = new Browser();
        final AccountInfo ai = new AccountInfo();
        setConstants(account, null);
        br.setFollowRedirects(true);
        login(account, true);
        final String url = PluginJSonUtils.getJsonValue(br, "url");
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(url);
        // here is free account check
        // url(/amember/downloader/manual/) will redirect, /amember/no-access/folder/id/4?url=/amember/downloader/manual/?
        if (br.getURL().contains("/amember/no-access/") && br.containsHTML("<title>Access Denied</title>")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final HashSet<String> supported = new HashSet<String>();
        String[] supportedHosts = br.getRegex("td>\\-([A-Za-z0-9\\-\\.]+)").getColumn(0);
        if (supportedHosts != null) {
            supported.addAll(Arrays.asList(supportedHosts));
        }
        br.getPage("http://forum.mobilism.org/filehosts.xml");
        supportedHosts = br.getRegex("host url=\"([A-Za-z0-9\\-\\.]+)\"").getColumn(0);
        if (supportedHosts != null) {
            supported.addAll(Arrays.asList(supportedHosts));
        }
        ai.setMultiHostSupport(this, new ArrayList<String>(supported));
        account.setType(AccountType.PREMIUM);
        ai.setUnlimitedTraffic();
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
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
                            br.setCookie(DOMAIN, key, value);
                        }
                        return;
                    }
                }
                /*
                 * 2016-01-24: When logged in and want to download it leads us to: http://mblservices.org/amember/login --> Here our initial
                 * login data does not work ...
                 */
                /*
                 * 20160201: above statement is no longer valid.. website login works in via browser (tested in chrome)
                 */
                if (false) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // this will redirect.
                br.setFollowRedirects(true);
                getPage("http://mblservices.org/amember/downloader/manual/");
                final Form login = br.getFormbyProperty("name", "login");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // note that the login form looks like it CAN has recaptcha event....
                login.put("amember_pass", Encoding.urlEncode(account.getPass()));
                login.put("amember_login", Encoding.urlEncode(account.getUser()));
                // json with these, html and standard redirect without!
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                submitForm(login);
                br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                br.getHeaders().put("X-Requested-With", null);
                // double check
                if (br.getCookie(DOMAIN, "amember_nr") == null && !PluginJSonUtils.parseBoolean(PluginJSonUtils.getJsonValue(br, "ok"))) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // save session
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(DOMAIN));
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
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
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlSlot
     *            (+1|-1)
     */
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