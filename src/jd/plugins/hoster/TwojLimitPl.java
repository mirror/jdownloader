//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
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

import org.appwork.utils.Hash;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "twojlimit.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class TwojLimitPl extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private String                                         Info               = null;
    private String                                         validUntil         = null;
    private boolean                                        expired            = false;

    public TwojLimitPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.twojlimit.pl/");
    }

    private void login(Account account, boolean force) throws PluginException, IOException {
        try {
            String username = Encoding.urlEncode(account.getUser());
            br.postPage("http://crypt.twojlimit.pl", "username=" + username + "&password=" + JDHash.getMD5(account.getPass()) + "&info=1&site=twojlimit");
            String adres = br.toString();
            br.getPage(adres);
            adres = br.getRedirectLocation();
            br.getPage(adres);
            if (this.br.containsHTML("balance")) {
                Info = br.toString();
            }
            if (this.br.containsHTML("expire")) {
                char temp = Info.charAt(Info.length() - 11);
                validUntil = Info.substring(Info.length() - 10);
                expired = temp != '1';
            } else {
                expired = false;
            }
            if (GetTrasferLeft(br.toString()) > 10) {
                expired = false;
            }
        } catch (final Exception e) {
        }
        boolean invalid = false;
        if (this.br.containsHTML("Nieprawidlowa")) {
            invalid = true;
        }
        if (invalid) {
            if (invalid) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) {
                ai = new AccountInfo();
                account.setAccountInfo(ai);
            }
            ai.setStatus("ServerProblems(1), will try again in few minutes!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }

    }

    /* function returns transfer left */
    private long GetTrasferLeft(String wynik) {
        String[] temp = wynik.split(" ");
        String[] tab = temp[0].split("=");
        long rozmiar = Long.parseLong(tab[1]);
        rozmiar *= 1024;
        return rozmiar;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setDebug(true);
        ac.setSpecialTraffic(true);
        String hosts = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        try {
            hosts = br.getPage("https://www.twojlimit.pl/clipboard.php");
            login(account, true);

        } catch (Exception e) {
            account.setTempDisabled(true);
            account.setValid(false);
            ac.setStatus("invalid account. Wrong password?");
            return ac;
        }

        ac.setTrafficLeft(GetTrasferLeft(Info));

        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (hosts != null) {
            String hoster[] = new Regex(hosts, "(.*?)(<br />|$)").getColumn(0);
            if (hosts != null) {
                for (String host : hoster) {
                    if (hosts == null || host.length() == 0) continue;
                    supportedHosts.add(host.trim());
                }
            }
        }
        if (expired) {
            ac.setExpired(true);
            ac.setStatus("Account expired");
            ac.setValidUntil(0);
            return ac;
        } else {
            ac.setExpired(false);
            if (validUntil != null) {
                if (validUntil.trim().equals("expire=00")) {
                    ac.setValidUntil(-1);
                } else {
                    ac.setValidUntil(TimeFormatter.getMilliSeconds(validUntil));
                }
            }

        }
        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via twojlimit.pl available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via twojlimit.pl available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "https://www.twojlimit.pl/terms";
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
        String postData = "username=" + username + "&password=" + Hash.getMD5(acc.getPass()) + "&info=0&url=" + url + "&site=twojlimit";
        showMessage(link, "Phase 2/3: Generating Link");
        String genlink = br.postPage("http://crypt.twojlimit.pl", postData);

        // link.setProperty("apilink", response);
        if (!(genlink.startsWith("http://") || genlink.startsWith("https://"))) {
            logger.severe("Twojlimit.pl(Error): " + genlink);
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
        br.setFollowRedirects(true);
        // wait, workaround
        sleep(1 * 1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 1);
        /*
         * I realy wanted to use Content Disposition below, but it just don't work for resume at hotfile -> Doesn't matter anymore, hotfile
         * is offline
         */
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