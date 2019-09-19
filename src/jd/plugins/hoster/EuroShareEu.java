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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "euroshare.eu" }, urls = { "https?://(www\\.)?euroshare\\.(eu|sk)/file/([a-zA-Z0-9]+/[^<>\"/]+|[a-zA-Z0-9]+)" })
public class EuroShareEu extends antiDDoSForHost {
    /** API documentation: http://euroshare.eu/euroshare-api/ */
    /**
     * Possible undocumented API responses: <br />
     * Chyba! Nelze se pripojit k databazi.<br />
     * <br />
     * <br />
     */
    private static final String  containsPassword = "ERR: Password protected file \\(wrong password\\)\\.";
    private static String        API_BASE         = "https://euroshare.eu/euroshare-api";
    // private static final String TOOMANYSIMULTANDOWNLOADS = "<p>Naraz je z jednej IP adresy možné sťahovať iba jeden súbor";
    private static AtomicInteger maxPrem          = new AtomicInteger(1);

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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        // start of password handling crapola
        String pass = link.getDownloadPassword();
        if (pass != null) {
            handlePassword(link);
            pass = link.getDownloadPassword();
            if (!pass.equals("")) {
                logger.info("handlePassword success");
            } else {
                logger.info("handlePassword failure");
                return AvailableStatus.UNCHECKABLE;
            }
        } else {
            getPage(API_BASE + "/?sub=checkfile&file=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.containsHTML("ERR: File does not exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(containsPassword)) {
                link.getLinkStatus().setStatusText("Pre-download password protection. Please set password!");
                filename = new Regex(link.getDownloadURL(), "/([^/]+)$").getMatch(0);
                if (filename != null) {
                    Encoding.urlDecode(filename, true);
                }
                link.setDownloadPassword(null);
                ;
                return AvailableStatus.UNCHECKABLE;
            }
        }
        // end of password handling
        filename = PluginJSonUtils.getJsonValue(this.br, "file_name");
        final String description = PluginJSonUtils.getJsonValue(this.br, "file_description");
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        final String filesize = PluginJSonUtils.getJsonValue(this.br, "file_size");
        final String md5 = PluginJSonUtils.getJsonValue(this.br, "md5_hash");
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    private void handlePassword(DownloadLink link) throws Exception {
        String pass = link.getDownloadPassword();
        if (!StringUtils.isEmpty(pass)) {
            getPage(API_BASE + "/?sub=checkfile&file=" + Encoding.urlEncode(link.getDownloadURL()) + "&file_password=" + Encoding.urlEncode(pass));
            if (br.containsHTML(containsPassword)) {
                // wrong password
                link.setDownloadPassword(null);
                handlePassword(link);
            } else {
                // password is correct
                link.getLinkStatus().setStatusText(null);
            }
        } else {
            pass = Plugin.getUserInput(link.getName() + " is password protected!", link);
            if (pass != null && !pass.equals("")) {
                link.setDownloadPassword(pass);
                handlePassword(link);
            } else {
                // not sure how stable works, but jd2 never enters here on cancellation of dialog box
                link.setDownloadPassword(null);
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
        } catch (final PluginException e) {
            throw e;
        }
        final String expire = PluginJSonUtils.getJsonValue(this.br, "unlimited_download_until");
        // Not sure if this behaviour is correct
        final String availableTraffic = PluginJSonUtils.getJsonValue(this.br, "credit");
        if ("0".equals(expire) && "0".equalsIgnoreCase(availableTraffic)) {
            ai.setStatus("Registered User");
            maxPrem.set(1);
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
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
            account.setType(AccountType.PREMIUM);
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
        if (account.getType() == AccountType.FREE) {
            doFree(link);
        } else {
            getPage(API_BASE + "/?sub=premiumdownload&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&file=" + Encoding.urlEncode(link.getDownloadURL()));
            final String dllink = PluginJSonUtils.getJsonValue(this.br, "link");
            if (dllink == null) {
                logger.warning("dllink is null");
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
        /* 2019-09-19: There is no way to save- and re-use any kind of logintoken! We always have to send username and password! */
        getPage(API_BASE + "/?sub=getaccountdetails&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }
}