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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "stahomat.cz" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class StahomatCz extends PluginForHost {
    /* IMPORTANT: superload.cz and stahomat.cz use the same api */
    private static final String                            mName              = "stahomat.cz/";
    private static final String                            mProt              = "http://";
    private static final String                            mAPI               = "http://api.stahomat.cz/a-p-i";
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    private static final int                               MAX_SIMULTAN_DLS   = 5;

    private static Object                                  LOCK               = new Object();
    private String                                         TOKEN              = null;

    public StahomatCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
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

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Download only works with a premium" + mName + "account.", PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    private void handleDL(Account account, DownloadLink link, String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            /* download is not contentdisposition, so remove this host from premiumHosts list */
            br.followConnection();
            if (br.containsHTML(">Omlouváme se, ale soubor se nepovedlo stáhnout")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            /* temp disabled the host */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            link.setProperty("superloadczdirectlink", dllink);
            /* contentdisposition, lets download it */
            dl.startDownload();
            try {
                updateCredits(null, account);
            } catch (Throwable e) {
                logger.info("Could not updateCredits handleDL!");
                logger.info(e.toString());
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink downloadLink, final Account account) throws Exception {
        prepBrowser();
        br.setFollowRedirects(true);
        final String pass = downloadLink.getStringProperty("pass", null);
        TOKEN = account.getStringProperty("token", null);
        if (TOKEN == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = checkDirectLink(downloadLink, "superloadczdirectlink");
        if (dllink == null) {
            showMessage(downloadLink, "Task 1: Generating Link");
            /* request Download */
            if (pass != null) {
                postPageSafe(account, mAPI + "/download-url", "url=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&password=" + Encoding.urlEncode(pass) + "&token=");
            } else {
                postPageSafe(account, mAPI + "/download-url", "url=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&token=");
            }
            if (br.containsHTML("\"error\":\"invalidLink\"")) {
                logger.info("Superload.cz says 'invalid link', disabling real host for 1 hour.");
                tempUnavailableHoster(account, downloadLink, 60 * 60 * 1000l);
            } else if (br.containsHTML("\"error\":\"temporarilyUnsupportedServer\"")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temp. Error. Try again later", 5 * 60 * 1000l);
            } else if (br.containsHTML("\"error\":\"fileNotFound\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\"error\":\"unsupportedServer\"")) {
                logger.info("Superload.cz says 'unsupported server', disabling real host for 1 hour.");
                tempUnavailableHoster(account, downloadLink, 60 * 60 * 1000l);
            } else if (br.containsHTML("\"error\":\"Lack of credits\"")) {
                logger.info("Superload.cz says 'Lack of credits', temporarily disabling account.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            dllink = getJson("link");
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = dllink.replaceAll("\\\\", "");
            showMessage(downloadLink, "Task 2: Download begins!");
        }
        // might need a sleep here hoster seems to have troubles with new links.
        handleDL(account, downloadLink, dllink);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        prepBrowser();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            account.setProperty("token", Property.NULL);
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        try {
            updateCredits(ai, account);
        } catch (Throwable e) {
            logger.info("Could not updateCredits fetchAccountInfo!");
            logger.info(e.toString());
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(MAX_SIMULTAN_DLS);
        ai.setValidUntil(-1);
        ai.setStatus("Premium User");
        try {
            // get the supported host list,
            String hostsSup = br.cloneBrowser().postPage(mAPI + "/get-supported-hosts", "token=" + TOKEN);
            String[] hosts = new Regex(hostsSup, "\"([^\", ]+\\.[^\", ]+)").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
                if (!supportedHosts.contains("uploaded.net")) {
                    supportedHosts.add("uploaded.net");
                }
                if (!supportedHosts.contains("ul.to")) {
                    supportedHosts.add("ul.to");
                }
                if (!supportedHosts.contains("uploaded.to")) {
                    supportedHosts.add("uploaded.to");
                }
            }
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Could not fetch ServerList from " + mName + ": " + e.toString());
        }
        return ai;
    }

    private void login(final Account acc) throws IOException, PluginException {
        synchronized (LOCK) {
            br.postPage(mAPI + "/login", "username=" + Encoding.urlEncode(acc.getUser()) + "&password=" + JDHash.getMD5(acc.getPass()));
            if (!getSuccess()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            TOKEN = getJson("token");
            if (TOKEN != null) {
                acc.setProperty("token", TOKEN);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private AccountInfo updateCredits(AccountInfo ai, Account account) throws PluginException, IOException {
        if (ai == null) ai = new AccountInfo();
        String token = account.getStringProperty("token", null);
        if (token == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.postPage(mAPI + "/get-status-bar", "token=" + token);
        Integer credits = Integer.parseInt(getJson("credits"));
        if (credits != null) {
            // 1000 credits = 1 GB, convert back into 1024 (Bytes)
            // String expression = "(" + credits + " / 1000) * 1073741824";
            // Double result = new DoubleEvaluator().evaluate(expression);
            ai.setTrafficLeft(SizeFormatter.getSize(credits + "MiB"));
        }
        return ai;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 'long timeout' to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private String getJson(final String key) {
        String result = br.getRegex("\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        if (result == null) result = br.getRegex("\"" + key + "\":([^\"\\}\\,]+)").getMatch(0);
        return result;
    }

    private boolean getSuccess() {
        return br.getRegex("\"success\":true").matches();
    }

    private void postPageSafe(final Account acc, final String page, final String postData) throws Exception {
        boolean failed = true;
        for (int i = 1; i <= 5; i++) {
            logger.info("Request try " + i + " of 5");
            try {
                br.postPage(page, postData + TOKEN);
            } catch (final BrowserException e) {
                if (br.getRequest().getHttpConnection().getResponseCode() == 401) {
                    logger.info("Request failed (401) -> Re-newing token and trying again");
                    try {
                        this.login(acc);
                    } catch (final Exception e_acc) {
                        logger.warning("Failed to re-new token!");
                        throw e_acc;
                    }
                    continue;
                }
                logger.info("Request failed (other BrowserException) -> Throwing BrowserException");
                throw e;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 10 * 60 * 1000l);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

}