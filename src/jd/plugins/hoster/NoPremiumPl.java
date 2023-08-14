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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nopremium.pl" }, urls = { "" })
public class NoPremiumPl extends PluginForHost {
    protected static MultiHosterManagement mhm = new MultiHosterManagement("rapidtraffic.pl");

    public NoPremiumPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.nopremium.pl/offer");
    }

    @Override
    public String getAGBLink() {
        return "https://www.nopremium.pl/tos";
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
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
            throw new AccountInvalidException("Zbyt wiele prób logowania - dostęp został tymczasowo zablokowany");
        } else if (br.containsHTML("balance")) {
            ac.setStatus("Premium Account");
        } else if (br.containsHTML("Nieprawidlowa nazwa uzytkownika/haslo")) {
            throw new AccountInvalidException();
        } else {
            // unknown error
            throw new AccountInvalidException("Unknown account status");
        }
        ac.setTrafficLeft(SizeFormatter.getSize(br.getRegex("balance=(\\d+)").getMatch(0) + "MB"));
        account.setMaxSimultanDownloads(20);
        account.setConcurrentUsePossible(true);
        ac.setValidUntil(-1);
        // now let's get a list of all supported hosts:
        br.getPage("https://nopremium.pl/clipboard.php"); // different domain!
        final String[] hosts = br.toString().split("<br />");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }
        /** 2023-08-14: Workaround because they sometimes don't update their API list of supported hosts... */
        final boolean tryToFindMoreHostsViaWebsite = true;
        if (tryToFindMoreHostsViaWebsite) {
            try {
                br.getPage("https://www." + this.getHost() + "/");
                final String[] hostsWithoutTld = br.getRegex("class=\"ServerLogo\"[^>]*alt=\"([^\"]+)").getColumn(0);
                if (hostsWithoutTld != null && hostsWithoutTld.length != 0) {
                    for (final String hostWithoutTld : hostsWithoutTld) {
                        supportedHosts.add(hostWithoutTld);
                    }
                } else {
                    logger.warning("Website workaround for finding additional supported hosts failed");
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.warning("Website workaround for finding additional supported hosts failed with exception");
            }
        }
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
        mhm.runCheck(account, link);
        String postData = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + JDHash.getSHA1(JDHash.getMD5(account.getPass())) + "&info=0&url=" + Encoding.urlEncode(link.getDownloadURL()) + "&site=nopremium";
        String response = br.postPage("http://crypt.nopremium.pl", postData);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, response, true, 0);
        /*
         * I really wanted to use Content Disposition below, but it just don't work for resume at hotfile -> Doesn't matter anymore, hotfile
         * is offline
         */
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("Brak")) {
                /* No transfer left */
                throw new AccountUnavailableException("No traffic left", 5 * 60 * 1000l);
            } else if (br.containsHTML("Nieprawidlowa")) {
                /* Wrong username/password */
                throw new AccountInvalidException();
            } else if (br.containsHTML("Konto")) {
                /* Account Expired */
                throw new AccountInvalidException("Account expired");
            } else if (br.containsHTML("15=Hosting nie obslugiwany")) {
                /* Host not supported */
                mhm.putError(account, link, 5 * 60 * 1000l, "Host not supported");
            } else {
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to downloadable file", 50, 5 * 60 * 1000);
            }
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}