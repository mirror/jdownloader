//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "euroshare.eu" }, urls = { "http://(www\\.)?euroshare\\.(eu|sk)/file/([a-zA-Z0-9]+/[^<>\"/]+|[a-zA-Z0-9]+)" })
public class EuroShareEu extends antiDDoSForHost {
    /** API documentation: http://euroshare.eu/euroshare-api/ */
    /**
     * Possible undocumented API responses: <br />
     * Chyba! Nelze se pripojit k databazi.<br />
     * <br />
     * <br />
     */
    private static final String  containsPassword         = "ERR: Password protected file \\(wrong password\\)\\.";
    private static final String  TOOMANYSIMULTANDOWNLOADS = "<p>Naraz je z jednej IP adresy možné sťahovať iba jeden súbor";
    private static AtomicInteger maxPrem                  = new AtomicInteger(1);

    public EuroShareEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://euroshare.eu/premium-accounts");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("euroshare.sk", "euroshare.eu"));
    }

    @Override
    public String getAGBLink() {
        return "http://euroshare.eu/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        // start of password handling crapola
        String pass = downloadLink.getStringProperty("pass");
        if (pass != null) {
            handlePassword(downloadLink);
            pass = downloadLink.getStringProperty("pass");
            if (!pass.equals("")) {
                logger.info("handlePassword success");
            } else {
                logger.info("handlePassword failure");
                return AvailableStatus.UNCHECKABLE;
            }
        } else {
            getPage("http://euroshare.eu/euroshare-api/?sub=checkfile&file=" + Encoding.urlEncode(downloadLink.getDownloadURL()));
            if (br.containsHTML("ERR: File does not exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(containsPassword)) {
                downloadLink.getLinkStatus().setStatusText("Pre-download password protection. Please set password!");
                filename = new Regex(downloadLink.getDownloadURL(), "/([^/]+)$").getMatch(0);
                if (filename != null) {
                    Encoding.urlDecode(filename, true);
                }
                downloadLink.setProperty("pass", "");
                return AvailableStatus.UNCHECKABLE;
            }
        }
        // end of password handling
        filename = PluginJSonUtils.getJsonValue(this.br, "file_name");
        final String description = PluginJSonUtils.getJsonValue(this.br, "file_description");
        if (description != null && downloadLink.getComment() == null) {
            downloadLink.setComment(description);
        }
        final String filesize = PluginJSonUtils.getJsonValue(this.br, "file_size");
        final String md5 = PluginJSonUtils.getJsonValue(this.br, "md5_hash");
        downloadLink.setFinalFileName(filename);
        downloadLink.setDownloadSize(Long.parseLong(filesize));
        downloadLink.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    private void handlePassword(DownloadLink downloadLink) throws Exception {
        String pass = downloadLink.getStringProperty("pass");
        if (pass != null && !pass.equals("")) {
            getPage("http://euroshare.eu/euroshare-api/?sub=checkfile&file=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&file_password=" + Encoding.urlEncode(pass));
            if (br.containsHTML(containsPassword)) {
                // wrong password
                downloadLink.setProperty("pass", "");
                handlePassword(downloadLink);
            } else {
                // password is correct
                downloadLink.getLinkStatus().setStatusText(null);
            }
        } else {
            pass = Plugin.getUserInput(downloadLink.getName() + " is password protected!", downloadLink);
            if (pass != null && !pass.equals("")) {
                downloadLink.setProperty("pass", pass);
                handlePassword(downloadLink);
            } else {
                // not sure how stable works, but jd2 never enters here on cancellation of dialog box
                downloadLink.setProperty("pass", "");
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        final String expire = PluginJSonUtils.getJsonValue(this.br, "unlimited_download_until");
        // Not sure if this behaviour is correct
        final String availableTraffic = PluginJSonUtils.getJsonValue(this.br, "credit");
        if ("0".equals(expire) && "0".equalsIgnoreCase(availableTraffic)) {
            ai.setStatus("Registered User");
            maxPrem.set(1);
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setProperty("FREE", true);
            ai.setUnlimitedTraffic();
        } else {
            /*
             * There are traffic and volume accounts and both combined. For combined accounts they have unlimited traffic till they expire.
             */
            if (expire != null && !"0".equals(expire)) {
                ai.setValidUntil(Long.parseLong(expire) * 1000);
                if (ai.isExpired()) {
                    ai.setStatus("Premium User (Credit)");
                    ai.setValidUntil(-1);
                    ai.setTrafficLeft(Long.parseLong(availableTraffic));
                } else {
                    ai.setStatus("Premium User (Time)");
                    ai.setUnlimitedTraffic();
                }
            } else {
                ai.setStatus("Premium User (Credit)");
                ai.setTrafficLeft(Long.parseLong(availableTraffic));
            }
            maxPrem.set(-1);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            account.setProperty("FREE", false);
        }
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(containsPassword)) {
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        final String dllink = PluginJSonUtils.getJsonValue(this.br, "free_link");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // ip is already loading
            if (br.containsHTML("Z Vasej IP uz prebieha stahovanie")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max simultan free downloads-limit reached!", 5 * 60 * 1000l);
            }
            // HTTP/1.1 403 Forbidden
            // Server: nginx/1.2.5
            // Date: Thu, 03 Apr 2014 07:08:04 GMT
            // Content-Type: text/html
            // Transfer-Encoding: chunked
            // Connection: close
            // X-Powered-By: PHP/5.3.19-1~dotdeb.0
            // Content-Encoding: gzip
            // ------------------------------------------------
            //
            //
            // 403 Forbidden<br><br>Server overloaded. Use PREMIUM downloading.
            if (br.containsHTML("Server overloaded")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "403 Server Busy. Try again later", 5 * 60 * 1000l);
            } else if (this.br.getHttpConnection().getResponseCode() == 403) {
                /* 403 Forbidden<br><br>Z Vasej IP uz prebieha stahovanie. Ako free uzivatel mozete stahovat iba jeden subor. */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "403 Too many free downloads active, try again later", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML(containsPassword)) {
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        if (account.getBooleanProperty("FREE")) {
            doFree(link);
        } else {
            getPage("http://euroshare.eu/euroshare-api/?sub=premiumdownload&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&file=" + Encoding.urlEncode(link.getDownloadURL()));
            final String dllink = PluginJSonUtils.getJsonValue(this.br, "link");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        getPage("http://euroshare.eu/euroshare-api/?sub=getaccountdetails&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("(ERR: User does not exist|ERR: Invalid password)")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}