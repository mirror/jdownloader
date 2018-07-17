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

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nopremium.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class NoPremiumPl extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NICE_HOST          = "nopremium.pl";
    private static final String                            NICE_HOSTproperty  = "nopremiumpl";

    public NoPremiumPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.nopremium.pl/offer");
    }

    @Override
    public String getAGBLink() {
        return "https://www.nopremium.pl/tos";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        // check if account is valid
        br.postPage("http://crypt.nopremium.pl", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + JDHash.getSHA1(JDHash.getMD5(account.getPass())) + "&info=1&site=nopremium");
        String adres = br.toString();
        br.getPage(adres);
        adres = br.getRedirectLocation();
        br.getPage(adres);
        // "Invalid login" / "Banned" / "Valid login"
        if (br.containsHTML("Zbyt wiele prób logowania - dostęp został tymczasowo zablokowany")) {
            ac.setStatus("Zbyt wiele prób logowania - dostęp został tymczasowo zablokowany");
            account.setValid(false);
            return ac;
        } else if (br.containsHTML("balance")) {
            ac.setStatus("Premium Account");
            account.setValid(true);
        } else if (br.containsHTML("Nieprawidlowa nazwa uzytkownika/haslo")) {
            ac.setStatus("Invalid login! Wrong password?");
            account.setValid(false);
            return ac;
        } else {
            // unknown error
            ac.setStatus("unknown account status");
            account.setValid(false);
            return ac;
        }
        ac.setTrafficLeft(SizeFormatter.getSize(br.getRegex("balance=(\\d+)").getMatch(0) + "MB"));
        account.setMaxSimultanDownloads(20);
        account.setConcurrentUsePossible(true);
        ac.setValidUntil(-1);
        // now let's get a list of all supported hosts:
        br.getPage("http://nopremium.pl/clipboard.php"); // different domain!
        final String[] hosts = br.toString().split("<br />");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }
        ac.setStatus("Account valid");
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /* no override to keep plugin compatible to old stable */
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
        String postData = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + JDHash.getSHA1(JDHash.getMD5(account.getPass())) + "&info=0&url=" + Encoding.urlEncode(link.getDownloadURL()) + "&site=nopremium";
        String response = br.postPage("http://crypt.nopremium.pl", postData);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, response, true, 0);
        /*
         * I really wanted to use Content Disposition below, but it just don't work for resume at hotfile -> Doesn't matter anymore, hotfile
         * is offline
         */
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
            br.followConnection();
            if (br.containsHTML("Brak")) {
                /* No transfer left */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.containsHTML("Nieprawidlowa")) {
                /* Wrong username/password */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (br.containsHTML("Niepoprawny")) {
                /* File not found */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("Konto")) {
                /* Account Expired */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (br.containsHTML("15=Hosting nie obslugiwany")) {
                /* Host not supported */
                tempUnavailableHoster(account, link, 3 * 60 * 60 * 1000l);
            }
            int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Download could not be started");
            } else {
                link.setProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", Property.NULL);
                logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
        }
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        try {
            dl.startDownload();
        } catch (Throwable e) {
            link.getLinkStatus().setStatusText("Content Disposition");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
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
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}