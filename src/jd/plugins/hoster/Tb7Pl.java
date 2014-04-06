//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tb7.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class Tb7Pl extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private String                                         Info               = null;
    private String                                         validUntil         = null;
    private boolean                                        expired            = false;
    private String                                         MAINPAGE           = "http://tb7.pl/";

    public Tb7Pl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "login");
    }

    private void login(Account account, boolean force) throws PluginException, IOException {
        boolean invalid = false;
        try {
            String username = Encoding.urlEncode(account.getUser());
            br.postPage(MAINPAGE + "login", "login=" + username + "&password=" + account.getPass());
            if (br.containsHTML("<div id=\"message\">Hasło jest nieprawidłowe</div>"))
                invalid = true;
            else {
                br.getPage(MAINPAGE + "mojekonto");
            }

        } catch (final Exception e) {
        }
        validUntil = br.getRegex("<div class=\"textPremium\">Dostęp Premium ważny do (.*?)<br>").getMatch(0);

        validUntil = validUntil.replace(" | ", " ");
        if (validUntil != null) expired = false;

        if (invalid) {
            if (invalid) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        }

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setDebug(true);
        ai.setSpecialTraffic(true);
        String hosts = null;
        ai.setProperty("multiHostSupport", Property.NULL);
        try {
            login(account, true);

        } catch (Exception e) {
            account.setTempDisabled(true);
            account.setValid(false);
            ai.setStatus("Invalid account. Wrong password?");
            return ai;
        }

        // unfortunatelly there is no list with supported hosts anywhere on the page
        final List<String> supportedHostsList = asList("egofiles.com", "catshare.net", "turbobit.net", "rapidgator.net", "rg.to", "netload.in", "uploaded.to", "uploaded.net", "ul.to", "bitshare.com", "freakshare.net", "freakshare.com");
        final ArrayList<String> supportedHosts = new ArrayList<String>(supportedHostsList.size());
        supportedHosts.addAll(supportedHostsList);

        if (expired) {
            ai.setExpired(true);
            ai.setStatus("Account expired");
            ai.setValidUntil(0);
            return ai;
        } else {
            ai.setStatus("Premium User");
            ai.setExpired(false);

            try {
                long expireTime = TimeFormatter.getMilliSeconds(validUntil, "dd.MM.yyyy HH:mm", null);
                ai.setValidUntil(expireTime);
            } catch (final Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            }
        }
        account.setValid(true);
        ai.setProperty("multiHostSupport", supportedHosts);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "regulamin";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        showMessage(link, "Phase 1/3: Login");
        login(acc, false);
        if (expired) {
            acc.setValid(false);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setDebug(true);
        dl = null;
        /* generate new downloadlink */
        String username = Encoding.urlEncode(acc.getUser());
        String url = Encoding.urlEncode(link.getDownloadURL());
        String postData = "step=1" + "&content=" + url;
        showMessage(link, "Phase 2/3: Generating Link");
        br.postPage(MAINPAGE + "mojekonto/sciagaj", postData);
        postData = "step=2" + "&0=on";
        br.postPage(MAINPAGE + "mojekonto/sciagaj", postData);

        String generatedLink = br.getRegex("<div class=\"download\"><a href=\"([^\"<>]+)\" target=\"_blank\">Pobierz</a>").getMatch(0);
        if (generatedLink == null) {
            logger.severe("Tb7.pl(Error): " + generatedLink);
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                try {
                    // disable hoster for 30min
                    tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                } catch (Exception e) {
                }
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }
        // wait, workaround
        sleep(1 * 1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedLink, true, 0);
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) // unknown
        // error
        {
            br.followConnection();
            if (br.getBaseURL().contains("notransfer")) {
                /* No transfer left */
                acc.setValid(false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.getBaseURL().contains("serviceunavailable")) {
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
            if (br.getBaseURL().contains("connecterror")) {
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
            if (br.getBaseURL().contains("invaliduserpass")) {
                acc.setValid(false);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Invalid username or password.");
            }
            if (br.getBaseURL().contains("notfound")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File not found."); }
        }

        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            tempUnavailableHoster(acc, link, 20 * 60 * 1000l);
        }
        showMessage(link, "Phase 3/3: Begin download");
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}