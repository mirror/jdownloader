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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debrid-link.fr" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class DebridLinkFr extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Map<Account, Map<String, String>>       accountInfo        = new HashMap<Account, Map<String, String>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(1);
    private static final String                            apiHost            = "https://api.debrid-link.fr/1.1/";
    private static final String                            publicApiKey       = "kMREtSnp61OgLvG8";

    /**
     * @author raztoki
     * */
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
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        // dump(account);
        if (!isAccPresent(account)) login(account);

        // account stats
        getPage(account, null, "infoMember", true, null);
        final String accountType = getJson("accountType");
        final String premiumLeft = getJson("premiumLeft");
        if ("0".equals(accountType)) {
            // free account
            ac.setStatus("Free Account");
            ac.setValidUntil(-1);
            account.setProperty("free", true);
        } else if ("1".equals(accountType)) {
            // premium account
            ac.setStatus("Premium Account");
            if (premiumLeft != null) ac.setValidUntil(System.currentTimeMillis() + (Long.parseLong(premiumLeft) * 1000));
            account.setProperty("free", false);
        } else if ("2".equals(accountType)) {
            // life account
            ac.setStatus("Life Account");
            ac.setValidUntil(-1);
            account.setProperty("free", false);
        }
        // end of account stats

        // multihoster array
        getPage(account, null, "statusDownloader", true, null);
        String[] status = br.getRegex("\\{\"status\":[\\d\\-]+.*?\\]\\}").getColumn(-1);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String stat : status) {
            final String s = getJson(stat, "status");
            // don't add host if they are down! or disabled
            if ("-1".equals(s) || "0".equals(s))
                continue;
            else {
                String[] hosts = new Regex(stat, "\"(([^\",]+\\.){1,}[a-z]+)\"").getColumn(0);
                for (String host : hosts) {
                    supportedHosts.add(host);
                }
            }
        }
        ac.setProperty("multiHostSupport", supportedHosts);
        // end of multihoster array
        return ac;
    }

    private void login(final Account account) throws Exception {
        getPage(account, null, "getToken", false, "publickey=" + publicApiKey);
        updateSession(account);
        if (true) {
            try {
                Browser br2 = new Browser();
                prepBrowser(br2);
                br2.setFollowRedirects(true);
                final String validateToken = getJson("validTokenUrl");
                if (validateToken == null) {
                    logger.warning("problemo!");
                    dump(account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br2.getPage(validateToken);
                // validate token externally.. this is good idea in principle but in practice not so, as it will drive users/customers
                // NUTTS!
                // Your better off doing 2 factor to email, as it can't be bypassed like this!
                Form vT = br2.getForm(0);
                if (vT == null) {
                    dump(account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                vT.put("sessidTime", "24");
                vT.put("user", Encoding.urlEncode(account.getUser()));
                vT.put("password", Encoding.urlEncode(account.getPass()));
                vT.put("authorizedToken", "1");
                br2.submitForm(vT);
                if (br2.containsHTML("La session à bien été activé. Vous pouvez utiliser l'application Jdownloader|The session has been activated\\. You can use the application Jdownloader")) {
                    logger.info("success!!");
                } else {
                    logger.warning("problemo!");
                    dump(account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } catch (Exception e) {
                throw e;
            }
        }
    }

    private boolean isAccPresent(final Account account) {
        synchronized (accountInfo) {
            if (accountInfo.containsKey(account))
                return true;
            else
                return false;
        }
    }

    private void updateSession(final Account account) {
        synchronized (accountInfo) {
            HashMap<String, String> accInfo = new HashMap<String, String>();
            final String token = getJson("token");
            if (token != null) accInfo.put("token", token);
            final String key = getJson("key");
            if (key != null) accInfo.put("key", key);
            final String loginTime = String.valueOf(System.currentTimeMillis());
            accInfo.put("loginTime", loginTime);
            final String timestamp = getJson("ts");
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
    private synchronized void getPage(final Account account, final DownloadLink downloadLink, final String r, final boolean sign, final String other) throws Exception {
        br = new Browser();
        prepBrowser(br);
        if (account != null && r != null) {
            br.getPage(apiHost + "?r=" + r + (sign ? "&token=" + getValue(account, "token") + "&sign=" + getSign(account, r) : "") + (other != null ? (!other.startsWith("&") ? "&" : "") + other : ""));
            if (errChk()) {
                errHandling(account, downloadLink);
            }
        } else
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void errHandling(final Account account, final DownloadLink downloadLink) throws PluginException {
        final String error = getJson("ERR");
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
            }

            // handling for download routines!

            else if (downloadLink != null) {

                if ("notDebrid".equals(error)) {
                    // Maybe the filehoster is down or the link is not online
                    tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1000l);
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
                    tempUnavailableHoster(account, downloadLink, 6 * 60 * 60 * 1000l);
                } else if ("disabledHost".equals(error)) {
                    // The filehoster are disabled
                    // remove from array!
                    tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1000l);
                } else if ("noGetFilename".equals(error)) {
                    // Unable to retrieve the file name
                    // what todo here? revert to another plugin **
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
            }
        }
    }

    private boolean errChk() {
        if (br.containsHTML("\"result\":\"KO\""))
            return true;
        else
            return false;
    }

    private String getSign(final Account account, final String r) throws Exception {
        if (r == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        final String to = getValue(account, "timeOffset");
        final String key = getValue(account, "key");

        if (to == null || key == null) {
            // dump account info in hashmap
            dump(account);
            // should we relogin?
        }

        // reflect time to server time
        final long ts = (System.currentTimeMillis() / 1000) - Long.parseLong(to);
        return JDHash.getSHA1(ts + r + key);
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
                    if (isAccPresent(account)) break;
                    if (!isAccPresent(account) && i + 1 == 2)
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    else
                        continue;
                } else
                    break;
            }
            Map<String, String> accInfo = accountInfo.get(account);
            String value = accInfo.get(key);
            return value;
        }
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     * 
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        if (result != null) result = result.replaceAll("\\\\/", "/");
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    /**
     * Tries to return value of key from JSon response, from provided Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return getJson(ibr.toString(), key);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink downloadLink, final Account account) throws Exception {
        showMessage(downloadLink, "Phase 1/2: Generating link");
        getPage(account, downloadLink, "addLink", true, "link=" + Encoding.urlEncode(downloadLink.getDownloadURL()));

        int chunks = 0;
        boolean resumes = true;

        final String chunk = getJson("chunk");
        final String resume = getJson("resume");
        if (chunk != null && !"0".equals(chunk)) chunks = -Integer.parseInt(chunk);
        if (resume != null) resumes = Boolean.parseBoolean(resume);

        String dllink = getJson("downloadLink");
        if (dllink == null) {
            logger.warning("Unhandled download error on debrid-link,fr:");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        showMessage(downloadLink, "Phase 2/2: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<img src='http://debrid-link\\.fr/images/logo\\.png")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
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
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
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

}