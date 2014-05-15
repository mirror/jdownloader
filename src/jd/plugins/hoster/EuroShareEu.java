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

import java.io.IOException;
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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "euroshare.eu" }, urls = { "http://(www\\.)?euroshare\\.(eu|sk)/file/([a-zA-Z0-9]+/[^<>\"/]+|[a-zA-Z0-9]+)" }, flags = { 2 })
public class EuroShareEu extends PluginForHost {
    
    /** API documentation: http://euroshare.eu/euroshare-api/ */
    private static final String  containsPassword         = "ERR: Password protected file \\(wrong password\\)\\.";
    private static final String  TOOMANYSIMULTANDOWNLOADS = "<p>Naraz je z jednej IP adresy možné sťahovať iba jeden súbor";
    private static AtomicInteger maxPrem                  = new AtomicInteger(1);
    
    public EuroShareEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://euroshare.eu/premium-accounts");
    }
    
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("euroshare.sk", "euroshare.eu"));
    }
    
    @Override
    public String getAGBLink() {
        return "http://euroshare.eu/terms";
    }
    
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
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
            br.getPage("http://euroshare.eu/euroshare-api/?sub=checkfile&file=" + Encoding.urlEncode(downloadLink.getDownloadURL()));
            if (br.containsHTML("ERR: File does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML(containsPassword)) {
                downloadLink.getLinkStatus().setStatusText("Pre-download password protection. Please set password!");
                try {
                    downloadLink.setComment("Pre-download password protection. Please set password!");
                } catch (final Throwable e) {
                }
                filename = new Regex(downloadLink.getDownloadURL(), "/([^/]+)$").getMatch(0);
                if (filename != null) {
                    Encoding.urlDecode(filename, true);
                }
                downloadLink.setProperty("pass", "");
                return AvailableStatus.UNCHECKABLE;
            }
        }
        // end of password handling
        
        filename = getJson("file_name");
        final String description = getJson("file_description");
        if (description != null) {
            try {
                downloadLink.setComment(description);
            } catch (final Throwable e) {
            }
        }
        final String filesize = getJson("file_size");
        final String md5 = getJson("md5_hash");
        downloadLink.setFinalFileName(filename);
        downloadLink.setDownloadSize(Long.parseLong(filesize));
        downloadLink.setMD5Hash(md5);
        return AvailableStatus.TRUE;
        
    }
    
    private void handlePassword(DownloadLink downloadLink) throws IOException, PluginException {
        String pass = downloadLink.getStringProperty("pass");
        if (pass != null && !pass.equals("")) {
            br.getPage("http://euroshare.eu/euroshare-api/?sub=checkfile&file=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&file_password=" + Encoding.urlEncode(pass));
            if (br.containsHTML(containsPassword)) {
                // wrong password
                downloadLink.setProperty("pass", "");
                handlePassword(downloadLink);
            } else {
                // password isn't wrong
                downloadLink.getLinkStatus().setStatusText(null);
                try {
                    downloadLink.setComment(null);
                } catch (final Throwable e) {
                }
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
        final String expire = getJson("unlimited_download_until");
        // Not sure if this behaviour is correct
        final String availableTraffic = getJson("credit");
        if (expire.equals("0") && availableTraffic.equals("0")) {
            ai.setStatus("Free (registered) User");
            try {
                maxPrem.set(1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
            account.setProperty("FREE", true);
            ai.setUnlimitedTraffic();
        } else {
            /* There are traffic and volume accounts and both combined. For combined accounts they have unlimited traffic till they expire. */
            if (!expire.equals("0")) {
                ai.setValidUntil(Long.parseLong(expire) * 1000);
                ai.setUnlimitedTraffic();
            } else {
                ai.setTrafficLeft(Long.parseLong(availableTraffic));
            }
            ai.setStatus("Premium User");
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
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
        if (br.containsHTML(containsPassword)) throw new PluginException(LinkStatus.ERROR_FATAL);
        doFree(downloadLink);
    }
    
    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = getJson("free_link");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // ip is already loading
            if (br.containsHTML("Z Vasej IP uz prebieha stahovanie")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max simultan free downloads-limit reached!", 5 * 60 * 1000l);
            
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
            if (br.containsHTML("Server overloaded")) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server Busy. Try again later", 5 * 60 * 1000l); }
            
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }
    
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML(containsPassword)) throw new PluginException(LinkStatus.ERROR_FATAL);
        if (account.getBooleanProperty("FREE")) {
            doFree(link);
        } else {
            br.getPage("http://euroshare.eu/euroshare-api/?sub=premiumdownload&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&file=" + Encoding.urlEncode(link.getDownloadURL()));
            String dllink = getJson("link");
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = dllink.replace("\\", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }
    
    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://euroshare.eu/euroshare-api/?sub=getaccountdetails&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("(ERR: User does not exist|ERR: Invalid password)")) {
            logger.info("Cannot accept account because: " + br.toString().trim());
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }
    
    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }
    
    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }
    
    @Override
    public void reset() {
    }
    
    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
    
}