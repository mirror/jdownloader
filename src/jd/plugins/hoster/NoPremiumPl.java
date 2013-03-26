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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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

import org.appwork.utils.Hash;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nopremium.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class NoPremiumPl extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(1);

    public NoPremiumPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.nopremium.pl/offer");
    }

    @Override
    public String getAGBLink() {
        return "https://www.nopremium.pl/tos";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlEncode(account.getUser());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        br.postPage("http://crypt.nopremium.pl", "username=" + username + "&password=" + Hash.getSHA1(Hash.getMD5(account.getPass())) + "&info=1&site=nopremium");
        String adres = br.toString();
        br.getPage(adres);
        adres = br.getRedirectLocation();
        br.getPage(adres);
        // "Invalid login" / "Banned" / "Valid login"
        if (br.containsHTML("balance")) {
            ac.setStatus("Premium User");
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

        maxPrem.set(20);
        ac.setTrafficLeft(SizeFormatter.getSize(br.getRegex("balance=(\\d+)").getMatch(0) + "MB"));
        try {
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        ac.setValidUntil(-1);
        // now let's get a list of all supported hosts:
        br.getPage("http://www.nopremium.pl/clipboard.php");
        hosts = br.toString().split("<br />");
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via nopremium.pl available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via nopremium.pl available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        final String url = Encoding.urlEncode(link.getDownloadURL());

        String postData = "username=" + acc.getUser() + "&password=" + Hash.getSHA1(Hash.getMD5(acc.getPass())) + "&info=0&url=" + url + "&site=nopremium";
        String response = br.postPage("http://crypt.nopremium.pl", postData);
        br.setFollowRedirects(true);

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, response, true, 0);
        /*
         * I realy wanted to use Content Disposition below, but it just don't work for resume at hotfile
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
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    private static void showApiDisabledDialog() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        if ("de".equalsIgnoreCase(lng)) {
                            title = " API deaktiviert";
                            message = "Du hast free-way API deaktiviert. Diese wird jedoch zum Downloaden benötigt.\r\n" + "Möchtest Du diese aktivieren?";
                        } else {
                            title = "API disabled";
                            message = "The free-way API seems to be disabled. To use jdownloader you need to enable it.";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("https://www.free-way.me/account?api_enable"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

}