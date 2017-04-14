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
import java.util.Arrays;
import java.util.HashMap;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multishare.cz" }, urls = { "https?://[\\w\\.]*?multishare\\.cz/((?:[a-z]{2}/)?stahnout/[0-9]+/|html/mms_process\\.php\\?(&?u_ID=\\d+|&?u_hash=[a-f0-9]+|(&?link=https?%3A%2F%2F[^&\\?]+|&?fid=\\d+)){3})" })
public class MultiShareCz extends antiDDoSForHost {

    public MultiShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.multishare.cz/cenik/");
    }

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private final String                                   mhLink             = "https?://[\\w\\.]*?multishare\\.cz/html/mms_process\\.php\\?.+";
    private static final String                            MAINPAGE           = "http://multishare.cz";
    private static final String                            NICE_HOST          = MAINPAGE.replaceAll("(https://|http://)", "");
    private static final String                            NICE_HOSTproperty  = MAINPAGE.replaceAll("(https://|http://|\\.|\\-)", "");

    private Account                                        currentAcc         = null;

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("Accept-Encoding", "json");
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCookie(this.getHost(), "lang", "en");
        prepBr.setCustomCharset("utf-8");
        return prepBr;
    }

    private void setConstants(final Account acc) {
        this.currentAcc = acc;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    /**
     * They only have premium accounts. When you register as a free user you get 5 GB of traffic - after that you ll simply have an empty
     * premium account.
     */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account);
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
        final String trafficleft = PluginJSonUtils.getJsonValue(br, "credit");
        if (trafficleft != null) {
            // 1 credit = 1 MB
            long traffic = (long) Double.parseDouble(trafficleft) * (1024 * 1024l);
            if (traffic >= 0) {
                ai.setTrafficLeft(traffic);
            } else {
                ai.setTrafficLeft(0);
            }
        }
        ai.setStatus("Premium User");
        if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
            try {
                getPage("https://www.multishare.cz/api/?sub=supported-hosters");
                final String[] hosts = PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJsonArray(br, "server"));
                final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
                /*
                 * set ArrayList<String> with all supported multiHosts of this service
                 */
                ai.setMultiHostSupport(this, supportedHosts);
            } catch (Throwable e) {
                logger.info("Could not fetch ServerList from Multishare: " + e.toString());
            }
        }
        return ai;
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
        if (downloadLink.getDownloadURL().matches(mhLink)) {
            dlGeneratedMhLink(downloadLink);
            return;
        }
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = "https://www.multishare.cz/html/download_free.php?ID=" + getFuid(downloadLink);
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
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account);
        if (downloadLink.getDownloadURL().matches(mhLink)) {
            dlGeneratedMhLink(downloadLink);
            return;
        }
        requestFileInformation(downloadLink);
        login(account);
        getPage(downloadLink.getDownloadURL());
        String dllink = "https://www.multishare.cz/html/download_premium.php?ID=" + getFuid(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Soubor na zdrojovém serveru pravděpodobně neexistuje")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            int timesFailed = downloadLink.getIntegerProperty("timesfailedmultisharecz_unknowndlerrorpremium", 0);
            downloadLink.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                logger.info("multishare.cz: Unknown download error -> Retrying");
                timesFailed++;
                downloadLink.setProperty("timesfailedmultisharecz_unknowndlerrorpremium", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info("multishare.cz: Unknown download error -> Plugin is broken");
                downloadLink.setProperty("timesfailedmultisharecz_unknowndlerrorpremium", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private void dlGeneratedMhLink(final DownloadLink downloadLink) throws Exception {
        requestFileInformationMh(downloadLink);
        handleDl(downloadLink, br.getURL(), 1);
    }

    public AvailableStatus requestFileInformationMh(DownloadLink dl) throws PluginException, IOException {
        prepBrowser(br);
        URLConnectionAdapter con = null;
        try {
            br.setFollowRedirects(true);
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition()) {
                if (dl.getFinalFileName() == null) {
                    dl.setFinalFileName(getFileNameFromHeader(con));
                }
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                dl.setAvailable(false);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
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

        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(false);
        /* login to get u_ID and u_HASH */
        getPage("https://www.multishare.cz/api/?sub=download-link&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));
        if (br.containsHTML("ERR: Invalid password\\.")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Wrong password", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        final String dllink = PluginJSonUtils.getJsonValue(br, "link");
        final String maxChunks = PluginJSonUtils.getJsonValue(br, "chunks");
        if (dllink == null) {
            handleUnknownErrors(this.currentAcc, link, "dllinknull", 10);
        }
        int chunks = 1;
        if (maxChunks != null) {
            try {
                final int tmp = Integer.parseInt(maxChunks);
                if (tmp > 1) {
                    chunks = -tmp;
                }
            } catch (final Throwable e) {
            }
        }
        handleDl(link, dllink, chunks);
    }

    private void handleDl(final DownloadLink downloadLink, final String dllink, int chunks) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, chunks);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
            if (br.getURL().contains("typ=nedostatecny-kredit")) {
                logger.info("No traffic available -> Temporarily disabling account");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.containsHTML("Soubor na zdrojovém serveru pravděpodobně neexistuje")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("<h1>Chyba stahování</h1>")) {
                if (downloadLink.getDownloadURL().contains("multishare.cz/")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
                }
                handleUnknownErrors(this.currentAcc, downloadLink, "known_unknown_downloaderror", 10);
            }
            logger.warning("Received html code instead of file -> Unknown error");
            handleUnknownErrors(this.currentAcc, downloadLink, "unknown_downloaderror", 20);
        }
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        getPage("https://www.multishare.cz/api/?sub=account-details&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("ERR: User does not exists")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (br.containsHTML("ERR: Invalid password")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid Password", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().matches(mhLink)) {
            return requestFileInformationMh(downloadLink);
        }
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        // support English page as its easier to understand for all our programmers.
        getPage("https://www.multishare.cz/en/stahnout/" + getFuid(downloadLink) + "/");
        // need to find the new error response in English!!
        if (br.containsHTML("(Požadovaný soubor neexistuje|Je možné, že byl již tento soubor vymazán uploaderem nebo porušoval autorská práva)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = "<title>Stáhnout (.*?)\\s*\\(([\\d\\.\\, a-zA-Z]+)\\)\\s*\\|\\s*MultiShare.cz</title>";
        String filename = br.getRegex(title).getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        }
        String filesize = br.getRegex(title).getMatch(1);
        if (filesize == null) {
            filesize = br.getRegex("<span class=\"download-file-size\"><span>([\\d\\.\\, a-zA-Z]+)</span>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            filesize = filesize.replace("&nbsp;", "");
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private String getFuid(DownloadLink downloadLink) {
        final String fuid = new Regex(downloadLink.getDownloadURL(), "/(\\d+)/?$").getMatch(0);
        return fuid;
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
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    /**
     * Is intended to handle unknown errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleUnknownErrors(final Account acc, final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            if (downloadLink.getDownloadURL().matches(mhLink)) {
                // multihoster link
                return true;
            }

            /* without account its not possible to download the link */
            return false;
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