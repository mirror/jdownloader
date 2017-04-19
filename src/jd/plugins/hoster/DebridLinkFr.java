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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debrid-link.fr" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class DebridLinkFr extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Map<Account, Map<String, String>>       accountInfo        = new HashMap<Account, Map<String, String>>();
    private static final String                            apiHost            = "https://debrid-link.fr/api";
    private static final String                            publicApiKey       = "kMREtSnp61OgLvG8";
    private long                                           ts                 = 0;

    /**
     * @author raztoki
     */
    public DebridLinkFr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://debrid-link.fr/?page=premium");
    }

    @Override
    public String getAGBLink() {
        return "http://debrid-link.fr/?page=mention";
    }

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCustomCharset("UTF-8");
        return prepBr;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        // dump(account);
        if (!isAccPresent(account)) {
            login(account);
        }

        // account stats
        getPage(account, null, "/account/infos", true, null);
        final String accountType = PluginJSonUtils.getJsonValue(br, "accountType");
        final String premiumLeft = PluginJSonUtils.getJsonValue(br, "premiumLeft");
        boolean isFree = false;
        if ("0".equals(accountType)) {
            // free account
            ac.setStatus("Free Account");
            account.setType(AccountType.FREE);
            ac.setValidUntil(-1);
            isFree = true;
        } else if ("1".equals(accountType)) {
            // premium account
            ac.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
            if (premiumLeft != null) {
                ac.setValidUntil(System.currentTimeMillis() + (Long.parseLong(premiumLeft) * 1000));
            }
        } else if ("2".equals(accountType)) {
            // life account
            ac.setStatus("Life Account");
            account.setType(AccountType.PREMIUM);
            ac.setValidUntil(-1);
        }
        // end of account stats

        // multihoster array
        getPage(account, null, "/downloader/status", false, null);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostitems = PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJsonArray(br, "hosters"));
        for (final String hostitem : hostitems) {
            // final String name = getJson(hostitem, "name");
            final boolean isFreeHost = Boolean.parseBoolean(PluginJSonUtils.getJsonValue(hostitem, "free_host"));
            final String status = PluginJSonUtils.getJsonValue(hostitem, "status");
            /* Don't add hosts if they are down or disabled, */
            if ("-1".equals(status) || "0".equals(status)) {
                // logger.info("NOT adding host " + name + " to host array because it is down or disabled");
                continue;
            } else if (isFree && !isFreeHost) {
                /* Don't add hosts which are not supported via the current account type - important for free accounts. */
                // logger.info("NOT adding host " + name + " to host array because user has a free account and this is not a free host");
                continue;
            } else {
                // logger.info("ADDING host " + name + " to host array");
                final String jsonarray = PluginJSonUtils.getJsonArray(hostitem, "hosts");
                final String[] domains = PluginJSonUtils.getJsonResultsFromArray(jsonarray);
                for (final String domain : domains) {
                    supportedHosts.add(domain);
                }
            }
        }
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setProperty("plain_hostinfo", br.toString());
        // end of multihoster array
        return ac;
    }

    private void login(final Account account) throws Exception {
        // new token and key
        getPage(account, null, "/token/" + publicApiKey + "/new", false, null);
        updateSession(account);
        if (true) {
            try {
                Browser br2 = new Browser();
                prepBrowser(br2);
                br2.setFollowRedirects(true);
                final String validateToken = PluginJSonUtils.getJsonValue(br, "validTokenUrl");
                if (validateToken == null) {
                    logger.warning("Can't find validateToken!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br2.getPage(validateToken);
                // recaptcha can be here
                if (br2.containsHTML(">Too many errors occurred\\.<") && br2.containsHTML(">Enter the captcha</label>")) {
                    final Form recap = br2.getForm(0);
                    final String apiKey = br2.getRegex("Recaptcha\\.create\\(\"([^\"]+)\"").getMatch(0);
                    if (apiKey == null || recap == null) {
                        logger.warning("can't find captcha regex!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    DownloadLink dummyLink = new DownloadLink(this, "Account", "http://" + this.getHost(), "http://" + this.getHost(), true);
                    final Recaptcha rc = new Recaptcha(br2, this);
                    rc.setId(apiKey);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                    recap.put("recaptcha_challenge_field", rc.getChallenge());
                    recap.put("recaptcha_response_field", Encoding.urlEncode(c));
                    recap.put("validHuman", "Submit");
                    br2.submitForm(recap);
                    if (br2.containsHTML(">Too many errors occurred\\.<") && br2.containsHTML(">Enter the captcha</label>")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid captcha!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }

                // validate token externally.. this is good idea in principle but in practice not so, as it will drive users/customers
                // NUTTS!
                // Your better off doing 2 factor to email, as it can't be bypassed like this!
                final Form vT = br2.getForm(0);
                if (vT == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (vT.hasInputFieldByName("user") && vT.hasInputFieldByName("password")) {
                    vT.put("sessidTime", "24");
                    vT.put("user", Encoding.urlEncode(account.getUser()));
                    vT.put("password", Encoding.urlEncode(account.getPass()));
                    vT.put("authorizedToken", "1");
                    br2.submitForm(vT);
                    if (br2.containsHTML("<div class=\"alert alert-success\">[\\w\\.\\s]+</div>")) {
                        logger.info("success!!");
                    } else if (br2.containsHTML(">Password or username not valid<|>Bad username or password\\.<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        logger.warning("Problemo, submitting login form!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM || e.getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) {
                    dump(account);
                }
                throw e;
            }
        }
    }

    private boolean isAccPresent(final Account account) {
        synchronized (accountInfo) {
            if (accountInfo.containsKey(account)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private void updateSession(final Account account) {
        synchronized (accountInfo) {
            HashMap<String, String> accInfo = new HashMap<String, String>();
            final String token = PluginJSonUtils.getJsonValue(br, "token");
            if (token != null) {
                accInfo.put("token", token);
            }
            final String key = PluginJSonUtils.getJsonValue(br, "key");
            if (key != null) {
                accInfo.put("key", key);
            }
            final String loginTime = String.valueOf(System.currentTimeMillis());
            accInfo.put("loginTime", loginTime);
            final String timestamp = PluginJSonUtils.getJsonValue(br, "ts");
            if (timestamp != null) {
                // some simple math find the offset between user and server time, so we can use server time later. cheat way of synch time !
                // server time is in seconds not milliseconds
                final long timeOffset = (System.currentTimeMillis() / 1000) - Integer.parseInt(timestamp);
                accInfo.put("timeOffset", Long.toString(timeOffset));
            }
            accountInfo.put(account, accInfo);
        }
    }

    /**
     * getPage sends get request with a new browser instance with each request! <br/>
     * error handling after page is returned.
     *
     * @param account
     * @param downloadLink
     * @param r
     *            :: r value
     * @param sign
     *            :: required ? true|false
     * @param other
     *            :: additional http request arguments
     * @throws Exception
     *
     */
    private void getPage(final Account account, final DownloadLink downloadLink, final String r, final boolean sign, final String other) throws Exception {
        synchronized (accountInfo) {
            if (account != null && r != null) {
                final String getThis = apiHost + r;
                br = new Browser();
                prepBrowser(br);
                if (sign) {
                    br.getHeaders().put("X-DL-TOKEN", getValue(account, "token"));
                    br.getHeaders().put("X-DL-SIGN", getSign(account, r));
                    br.getHeaders().put("X-DL-TS", ts + "");
                }
                br.getPage(getThis);
                if (errChk()) {
                    errHandling(account, downloadLink, false);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private void postPage(final Account account, final DownloadLink downloadLink, final String r, final boolean sign, final String other) throws Exception {
        synchronized (accountInfo) {
            if (account != null && r != null) {
                br = new Browser();
                prepBrowser(br);
                if (sign) {
                    br.getHeaders().put("X-DL-TOKEN", getValue(account, "token"));
                    br.getHeaders().put("X-DL-SIGN", getSign(account, r));
                    br.getHeaders().put("X-DL-TS", ts + "");
                }
                br.postPage(apiHost + r, other);
                if (errChk()) {
                    errHandling(account, downloadLink, false);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private void errHandling(final Account account, final DownloadLink downloadLink, final boolean postDl) throws PluginException {
        if (postDl) {
            // <p>
            // Download error
            // </p>
            // <p>
            // Unable to retrieve the file from the host.<br/>
            // Code erreur: #DEB-536e7081927b8<br>
            // </p>
            // </div>
            // <div class='clearfix'></div><div class='alert alert-info'>
            // <p> - The download server may be offline.<br/>
            // - Our server was banned by the host.<br/>
            // - A script problem due to a change in the host.</p>
            // </div><div class='col-sm-8'><p>Renew your request.<br/>
            // If the problem continues, please contact us.</div>
            if (br.containsHTML("Unable to retrieve the file from the host\\.<")) {
                tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        final String error = PluginJSonUtils.getJsonValue(br, "ERR");
        if (error != null) {
            // generic errors not specific to download routine!
            if ("unknowR".equals(error)) {
                // Bad r argument
                // changes with the API? this shouldn't happen
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if ("badSign".equals(error)) {
                // Check the sign parameter
                dump(account);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if ("badRequest".equals(error)) {
                // not in error table yet..........
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if ("hidedToken".equals(error)) {
                // The token is not enabled. Redirect the user to validTokenUrl
                // this is done automatic at this stage, as users will hate dialog/popups!
                dump(account);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if ("badToken".equals(error)) {
                // Token expired or not valid
                dump(account);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if ("notToken".equals(error)) {
                // The request need token argument
                // should never happen, unless API changes!
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if ("disableServerHost".equals(error) || "serverNotAllowed".equals(error)) {
                // ip ban (dedicated server)
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Dedicated Server/VPN/Proxy detected, account disabled!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("floodDetected".equals(error)) {
                // API Flood detected, retry after 1 hour
                account.setTmpDisabledTimeout(1 * 60 * 60 * 1001l);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "API Flood", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            // end of generic

            // handling for download routines!
            if (downloadLink != null) {
                if ("notDebrid".equals(error)) {
                    // Maybe the filehoster is down or the link is not online
                    tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1001l);
                } else if ("fileNotFound".equals(error)) {
                    // The filehoster return a 'file not found' error.
                    // let another download method kick in? **
                    // NOTE: ** = jiaz new handling behaviour
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                } else if ("badFileUrl".equals(error)) {
                    // The link format is not valid
                    // link generation?? lets go into another plugin **
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                } else if ("hostNotValid".equals(error)) {
                    // The filehoster is not supported
                    // shouldn't happen as we check supported array and remove hosts that are disabled/down etc.
                    tempUnavailableHoster(account, downloadLink, 6 * 60 * 60 * 1001l);
                } else if ("disabledHost".equals(error)) {
                    // The filehoster are disabled
                    // remove from array!
                    tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1001l);
                } else if ("noGetFilename".equals(error)) {
                    // Unable to retrieve the file name
                    // what todo here? revert to another plugin **
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                } else if ("notFreeHost".equals(error) || "needPremium".equals(error)) {
                    /*
                     * Filehost is disabled for current FREE account --> Disable it "forever" --> Should usually not happen as already
                     * handled below in canHandle.
                     */
                    tempUnavailableHoster(account, downloadLink, 10 * 60 * 60 * 1001l);
                } else if ("maxLinkHost".equals(error)) {
                    // max link limit reached, see linkLimit
                    tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1001l);
                } else if ("maxlink".equals(error)) {
                    // this is for any hoster, so can't effectively use account. temp disalbe?
                    account.setTmpDisabledTimeout(1 * 60 * 60 * 1001l);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }

            }
        }
    }

    private boolean errChk() {
        if (br.containsHTML("\"result\":\"KO\"")) {
            return true;
        } else {
            return false;
        }
    }

    private String getSign(final Account account, final String r) throws Exception {
        if (r == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final String to = getValue(account, "timeOffset");
        final String key = getValue(account, "key");

        if (to == null || key == null) {
            // dump account info in hashmap
            dump(account);
            // should we relogin?
        }

        // reflect time to server time
        ts = (System.currentTimeMillis() / 1000) - Long.parseLong(to);
        final String output = ts + r + key;
        return JDHash.getSHA1(output);
    }

    private void dump(final Account account) {
        synchronized (accountInfo) {
            accountInfo.remove(account);
        }
    }

    private String getValue(final Account account, final String key) throws Exception {
        synchronized (accountInfo) {
            // simulate dump
            // accountInfo.remove(account);
            // relogin required due to possible dump.
            for (int i = 0; i != 2; i++) {
                if (!isAccPresent(account)) {
                    login(account);
                    if (isAccPresent(account)) {
                        break;
                    }
                    if (!isAccPresent(account) && i + 1 == 2) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else {
                        continue;
                    }
                } else {
                    break;
                }
            }
            Map<String, String> accInfo = accountInfo.get(account);
            String value = accInfo.get(key);
            return value;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {

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

        showMessage(link, "Phase 1/2: Generating link");
        postPage(account, link, "/downloader/add", true, "link=" + Encoding.urlEncode(link.getDownloadURL()));

        int maxChunks = 0;
        boolean resumes = true;

        final String chunk = PluginJSonUtils.getJsonValue(br, "chunk");
        final String resume = PluginJSonUtils.getJsonValue(br, "resume");
        if (chunk != null && !"0".equals(chunk)) {
            maxChunks = -Integer.parseInt(chunk);
        }
        if (resume != null) {
            resumes = Boolean.parseBoolean(resume);
        }

        String dllink = PluginJSonUtils.getJsonValue(br, "downloadLink");
        if (dllink == null) {
            logger.warning("Unhandled download error on debrid-link,fr:");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        showMessage(link, "Phase 2/2: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumes, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            errHandling(account, link, true);
            if (br.containsHTML("<img src='http://debrid-link\\.fr/images/logo\\.png")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            }
            logger.warning("Unhandled download error on debrid-link.fr:");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        final String currenthost = downloadLink.getHost();
        final String plain_hostinfo = account.getAccountInfo().getStringProperty("plain_hostinfo", null);
        if (account.getType() == AccountType.FREE && plain_hostinfo != null) {
            final String[] hostinfo = PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJsonArray(plain_hostinfo, "hosters"));
            if (hostinfo != null) {
                for (final String singlehostinfo : hostinfo) {
                    final boolean free_host = Boolean.parseBoolean(PluginJSonUtils.getJsonValue(singlehostinfo, "free_host"));
                    if (singlehostinfo.contains(currenthost) && !free_host) {
                        return false;
                    }

                }
            }
        }
        return true;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}