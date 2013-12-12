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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multishare.cz" }, urls = { "https?://[\\w\\.]*?multishare\\.cz/stahnout/[0-9]+/" }, flags = { 2 })
public class MultiShareCz extends PluginForHost {

    public MultiShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.multishare.cz/cenik/");
    }

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            throw e;
        }
        account.setValid(true);
        try {
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(-1);
        } catch (final Throwable e) {
        }
        final String trafficleft = getJson("credit");
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft + " MB"));
        }
        ai.setStatus("Premium User");
        if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
            try {
                br.getPage("https://www.multishare.cz/api/?sub=supported-hosters");
                final String hostsText = br.getRegex("\\{\"server\":\\[(.*?)\\]").getMatch(0);
                String[] hosts = hostsText.split(",");
                ArrayList<String> supportedHosts = new ArrayList<String>();
                for (String host : hosts) {
                    host = host.replace("\"", "");
                    if ("freakshare.net".equalsIgnoreCase(host)) host = "freakshare.com";
                    supportedHosts.add(host);
                }
                /*
                 * set ArrayList<String> with all supported multiHosts of this service
                 */
                ai.setProperty("multiHostSupport", supportedHosts);
            } catch (Throwable e) {
                account.setProperty("multiHostSupport", Property.NULL);
                logger.info("Could not fetch ServerList from Multishare: " + e.toString());
            }
        }
        return ai;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    @Override
    public String getAGBLink() {
        return "http://www.multishare.cz/kontakt/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String fileid = new Regex(downloadLink.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "https://www.multishare.cz/html/download_free.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            int timesFailed = downloadLink.getIntegerProperty("timesfailedmultisharecz_unknowndlerrorfree", 0);
            downloadLink.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                logger.info("multishare.cz: Unknown download error -> Retrying");
                timesFailed++;
                downloadLink.setProperty("timesfailedmultisharecz_unknowndlerrorfree", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info("multishare.cz: Unknown download error -> Plugin is broken");
                downloadLink.setProperty("timesfailedmultisharecz_unknowndlerrorfree", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String fileid = new Regex(link.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "https://www.multishare.cz/html/download_premium.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            int timesFailed = link.getIntegerProperty("timesfailedmultisharecz_unknowndlerrorpremium", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                logger.info("multishare.cz: Unknown download error -> Retrying");
                timesFailed++;
                link.setProperty("timesfailedmultisharecz_unknowndlerrorpremium", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info("multishare.cz: Unknown download error -> Plugin is broken");
                link.setProperty("timesfailedmultisharecz_unknowndlerrorpremium", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        /* login to get u_ID and u_HASH */
        br.getPage("https://www.multishare.cz/api/?sub=download-link&login=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));
        String dllink = getJson("link");
        if (br.containsHTML("ERR: Invalid password\\.")) {
            int timesFailed = link.getIntegerProperty("timesfailedmultisharecz_passwordinvalid", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                logger.info("multishare.cz: Strange invalid password message -> Retrying");
                timesFailed++;
                link.setProperty("timesfailedmultisharecz_passwordinvalid", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error (server says: ERR: Invalid password)");
            } else {
                logger.info("multishare.cz: Strange invalid password message -> Disabling current host");
                link.setProperty("timesfailedmultisharecz_passwordinvalid", Property.NULL);
                tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000l);
            }
        }
        if (dllink == null) {
            int timesFailed = link.getIntegerProperty("timesfailedmultisharecz_unknown", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedmultisharecz_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                link.setProperty("timesfailedmultisharecz_unknown", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
        }
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage("https://www.multishare.cz/api/?sub=account-details&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("ERR: User does not exists")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (br.containsHTML("ERR: Invalid password")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid Password", PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Požadovaný soubor neexistuje|Je možné, že byl již tento soubor vymazán uploaderem nebo porušoval autorská práva)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>MultiShare\\.cz :: Stáhnout soubor \"(.*?)\"</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<li>Název: <strong>(.*?)</strong>").getMatch(0);
        String filesize = br.getRegex("Velikost: <strong>(.*?)</strong").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        filesize = filesize.replace("&nbsp;", "");
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}