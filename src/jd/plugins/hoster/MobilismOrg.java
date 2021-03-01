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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;

/**
 *
 * note: cloudflare<br/>
 * note: transloads downloads<br/>
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mobilism.org" }, urls = { "" })
public class MobilismOrg extends antiDDoSForHost {
    /* Tags: Script vinaget.us */
    private static final String          WEBSITE_BASE        = "https://mblservices.org";
    private static final String          NORESUME            = "mobilismorg_NORESUME";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("mobilism.org");
    /* Last updated: 31.03.15 */
    private static final int             defaultMAXDOWNLOADS = 20;
    private static final int             defaultMAXCHUNKS    = 0;
    private static final boolean         defaultRESUME       = true;

    public MobilismOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://forum.mobilism.org/viewtopic.php?f=398&t=284351");
    }

    @Override
    public String getAGBLink() {
        return "https://forum.mobilism.org/";
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.addAllowedResponseCodes(new int[] { 401 });
        }
        return prepBr;
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        boolean resume = account.getBooleanProperty("resume", defaultRESUME);
        int maxChunks = account.getIntegerProperty("account_maxchunks", defaultMAXCHUNKS);
        if (link.getBooleanProperty(NORESUME, false)) {
            resume = false;
        }
        if (!resume) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxChunks);
        if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(MobilismOrg.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            mhm.handleErrorGeneric(account, link, "unknowndlerror", 10, 5 * 60 * 1000l);
        }
        link.setProperty(this.getHost() + "directlink", dl.getConnection().getURL().toString());
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        login(account, false);
        /* 2021-02-23: Allow re-using previously generated directURLs */
        final boolean forceNewLinkGeneration = false;
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        if (dllink == null || forceNewLinkGeneration) {
            /* request creation of downloadlink */
            login(account, true);
            br.setFollowRedirects(true);
            getPage(WEBSITE_BASE + "/amember/downloader/downloader/app/bindex3.php?dl=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            final String regex_dllink = "(https?://[^\"]+/downloader/app/files/[^\"]+)\"";
            dllink = br.getRegex(regex_dllink).getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("Download: <a href=\"([^<>\"]+)\"").getMatch(0);
            }
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 10, 5 * 60 * 1000l);
            }
            /* TODO: Check if this is still required */
            // final Form d3 = br.getFormbyActionRegex(".*/amember/downloader/downloader/app/bindex3\\.php.*");
            // if (d3 == null) {
            // // instead of d3 form you can get errors outputs directly from there scripts. for example rapidgator an premium cookie could
            // // not be found
            // // <div id="mesg" width="100%" align="center">Processing. Remain patient.</div><div align="center"><span
            // // class='htmlerror'><b>Login Error: Cannot find 'user__' cookie.</b></span>
            // final boolean header = br.containsHTML("<div id=\"mesg\" width=\"100%\" align=\"center\">.*?</div>");
            // final String error = br.getRegex("<span class='htmlerror'><b>(.*?)</b></span>").getMatch(0);
            // if (header && error != null) {
            // // server side issues...
            // mhm.handleErrorGeneric(account, link, "multihoster_issue", 10, 5 * 60 * 1000l);
            // }
            // mhm.handleErrorGeneric(account, link, "Failed to find transload Form #2", 10, 5 * 60 * 1000l);
            // }
            // submitForm(d3);
            // dllink = br.getRegex(regex_dllink).getMatch(0);
            // long time = 0;
            // long wait = 5 * 60 * 1000l;
            // if (dllink == null) {
            // do {
            // /* Transloading bullshit... */
            // sleep(1 * 60 * 1000l, link);
            // submitForm(d3);
            // dllink = br.getRegex(regex_dllink).getMatch(0);
            // } while (dllink == null && time < 5 * 60 * 1000l);
            // }
            // if (dllink == null && time > wait) {
            // logger.warning("Final downloadlink is null");
            // mhm.handleErrorGeneric(account, link, "dllinknull", 2, 10 * 60 * 1000l);
            // }
        }
        handleDL(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!br.getURL().contains("/amember/member")) {
            this.getPage("/amember/member");
        }
        /* Here is free account check */
        // url(/amember/downloader/manual/) will redirect, /amember/no-access/folder/id/4?url=/amember/downloader/manual/?
        if (br.getURL().contains("/amember/no-access/") && br.containsHTML("<title>Access Denied</title>")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final String expireDateStr = br.getRegex("expires (\\d{1,2}/\\d{1,2}/\\d{1,2})").getMatch(0);
        if (expireDateStr != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDateStr, "MM/dd/yy", Locale.ENGLISH), this.br);
        }
        final HashSet<String> supported = new HashSet<String>();
        String[] supportedHosts = br.getRegex("td>\\-([A-Za-z0-9\\-\\.]+)").getColumn(0);
        if (supportedHosts != null) {
            supported.addAll(Arrays.asList(supportedHosts));
        }
        getPage("https://forum." + this.getHost() + "/filehosts.xml");
        supportedHosts = br.getRegex("host url=\"([A-Za-z0-9\\-\\.]+)\"").getColumn(0);
        if (supportedHosts != null) {
            supported.addAll(Arrays.asList(supportedHosts));
        }
        ai.setMultiHostSupport(this, new ArrayList<String>(supported));
        account.setType(AccountType.PREMIUM);
        ai.setUnlimitedTraffic();
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(cookies);
                    if (!force) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    logger.info("Checking login cookies...");
                    this.checkAndHandleLogin2(account);
                    br.getPage(WEBSITE_BASE + "/amember/member");
                    if (this.isLoggedinHTML()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                /* this will redirect. */
                br.setFollowRedirects(true);
                getPage(WEBSITE_BASE + "/amember/login");
                final Form login = br.getFormbyProperty("name", "login");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("amember_pass", Encoding.urlEncode(account.getPass()));
                login.put("amember_login", Encoding.urlEncode(account.getUser()));
                /* json with these, html and standard redirect without! */
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                submitForm(login);
                br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                br.getHeaders().put("X-Requested-With", null);
                /* E.g. successful response: {"ok":true,"url":"\/amember\/member"} */
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final boolean loginIsOK = ((Boolean) entries.get("ok")).booleanValue();
                if (br.getCookie(this.br.getHost(), "amember_nr", Cookies.NOTDELETEDPATTERN) == null || !loginIsOK) {
                    /*
                     * 2021-03-01: This may also happen sometimes:
                     * {"ok":false,"error":["Please wait 55 seconds before next login attempt"],"code":-4}
                     */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                this.checkAndHandleLogin2(account);
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    /** 2nd login endpoint: We can't download without doing this once per session! */
    private void checkAndHandleLogin2(final Account account) throws Exception {
        br.getPage(WEBSITE_BASE + "/amember/downloader/downloader/app/bindex3.php");
        final Form login2 = br.getFormbyKey("password");
        if (login2 != null) {
            logger.info("Processing login2...");
            login2.put("username", Encoding.urlEncode(account.getUser()));
            login2.put("password", Encoding.urlEncode(account.getPass()));
            this.submitForm(login2);
            if (br.containsHTML(">\\s*The username or password is incorrect")) {
                /* Do not throw exception here although downloads will very likely fail... */
                logger.warning("Login2 seems to have failed");
            } else {
                logger.info("Login2 looks good");
            }
        } else {
            logger.info("Login2: All good we're already logged in");
        }
    }

    private boolean isLoggedinHTML() {
        return br.containsHTML("/amember/logout");
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return defaultMAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}