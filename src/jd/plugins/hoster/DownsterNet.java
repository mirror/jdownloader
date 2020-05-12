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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "downster.net" }, urls = { "https?://downster\\.net/api/download/get/\\d+/.+" })
public class DownsterNet extends antiDDoSForHost {
    public DownsterNet(PluginWrapper wrapper) throws FileNotFoundException, IOException {
        super(wrapper);
        this.enablePremium("https://downster.net");
        this.loadUserFlowId();
    }

    @Override
    public String getAGBLink() {
        return "https://downster.net/legal";
    }

    private static final String          API_BASE                      = "https://downster.net/api";
    private static MultiHosterManagement mhm                           = new MultiHosterManagement("downster.net");
    private static final String          DLLINK_PROP_NAME              = "downsterdirectlink";
    private static final String          NOCHUNKS                      = "NOCHUNKS";
    private static final String          MAX_RETRIES_DL_ERROR_PROPERTY = "MAX_RETRIES_DL_ERROR";
    private static final int             DEFAULT_MAX_RETRIES_DL_ERROR  = 50;
    private String                       userFlowId                    = "";
    private String                       dllink                        = null;

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setConnectTimeout(60 * 1000);
            prepBr.setReadTimeout(60 * 1000);
            prepBr.setHeader("Content-Type", "application/json");
            prepBr.addAllowedResponseCodes(new int[] { 401, 403, 412, 422, 503, 512 });
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
            prepBr.getHeaders().put("X-Flow-ID", "JDL_" + userFlowId + "_" + randomFlowId());
        }
        return prepBr;
    }

    private void loadUserFlowId() throws FileNotFoundException, IOException {
        File flowIdFile = JDUtilities.getResourceFile("flowId.txt");
        if (flowIdFile.exists() && flowIdFile.canRead()) {
            userFlowId = new BufferedReader(new FileReader(flowIdFile)).readLine();
        }
        if (userFlowId.isEmpty()) {
            userFlowId = this.randomFlowId();
            if (flowIdFile.createNewFile()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(flowIdFile));
                writer.write(userFlowId);
                writer.flush();
            }
        }
    }

    private String randomFlowId() {
        String flowId = "";
        for (int i = 0; i < 2; i++) {
            String hex = Integer.toHexString((int) (Math.random() * 255));
            flowId += hex.length() > 1 ? hex : "0" + hex;
        }
        return flowId;
    }

    private String login(final Account account) throws Exception {
        getPage(API_BASE + "/user/info");
        if ("true".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
            return null;
        }
        String json = null;
        json = PluginJSonUtils.ammendJson(json, "email", account.getUser());
        json = PluginJSonUtils.ammendJson(json, "password", account.getPass());
        postPage(API_BASE + "/user/authenticate", json);
        if ("true".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
            account.saveCookies(br.getCookies(this.getHost()), "");
            return null;
        }
        return PluginJSonUtils.getJsonValue(br, "error");
    }

    private class Hoster {
        public String  name;
        public Long    limit;
        public Long    used;
        public Integer percentage;

        public Hoster() {
        }
    }

    private List<Hoster> getHosters(final Account account) throws Exception {
        getPage(API_BASE + "/download/usage");
        if ("false".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCould not get hoster list: " + PluginJSonUtils.getJsonValue(br, "error"));
        }
        List<Hoster> hosters = new ArrayList<Hoster>();
        String data = PluginJSonUtils.getJsonArray(br, "data");
        for (String hosterJson : PluginJSonUtils.getJsonResultsFromArray(data)) {
            Hoster hoster = new Hoster();
            hoster.name = PluginJSonUtils.getJsonValue(hosterJson, "hoster");
            hoster.limit = Long.parseLong(PluginJSonUtils.getJsonValue(hosterJson, "limit"));
            hoster.used = Long.parseLong(PluginJSonUtils.getJsonValue(hosterJson, "used"));
            hoster.percentage = Integer.parseInt(PluginJSonUtils.getJsonValue(hosterJson, "percentage"));
            hosters.add(hoster);
        }
        return hosters;
    }

    private void accountInvalid() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBrowser(br, "downster.net");
        br.setCookies(API_BASE, account.loadCookies(""));
        if (account.getUser().equals("") || account.getPass().equals("")) {
            /* Server returns 401 if you send empty fields (logindata) */
            accountInvalid();
        }
        final AccountInfo ac = new AccountInfo();
        ac.setUnlimitedTraffic();
        String loginError = login(account);
        if (loginError != null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + loginError);
        }
        Long premiumUntil = Long.parseLong(PluginJSonUtils.getJsonValue(br, "premiumUntil"));
        // premiumUntil is reported in seconds but java needs milliseconds
        ac.setValidUntil(premiumUntil * 1000L, br);
        if (premiumUntil > 0) {
            ac.setStatus("Premium active");
            ac.setExpired(false);
        } else {
            ac.setStatus("No premium");
            ac.setExpired(true);
        }
        List<String> hosters = new ArrayList<String>();
        for (Hoster hoster : getHosters(account)) {
            hosters.add(hoster.name);
        }
        ac.setMultiHostSupport(this, hosters);
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBrowser(br, "downster.net");
        br.setCookies(API_BASE, account.loadCookies(""));
        String loginError = login(account);
        if (loginError != null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + loginError);
        }
        mhm.runCheck(account, link);
        dllink = checkDirectLink(link);
        if (dllink == null) {
            String downloadlink = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            getPage(API_BASE + "/download/get?url=" + downloadlink);
            /* Either server error or the host is broken (we have to find out by retrying) */
            int status = br.getHttpConnection().getResponseCode();
            String error = PluginJSonUtils.getJsonValue(br, "error");
            if (status == 510 || status == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, error);
            } else if (status >= 500 || "false".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "success"))) {
                mhm.putError(account, link, 5 * 60 * 1000l, error);
            }
            dllink = PluginJSonUtils.getJsonValue(br, "downloadUrl");
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "No download link could be generated", 20, 5 * 60 * 1000l);
            }
            // link.setFinalFileName(PluginJSonUtils.getJsonValue(br, "name"));
            // Don't set size as it does not match exact byte amount
            // link.setVerifiedFileSize(Long.parseLong(PluginJSonUtils.getJsonValue(br, "size")));
            // Direct download links can be used for up to 30 minutes
            link.setProperty(DLLINK_PROP_NAME, dllink);
        }
        handleDl(link, account);
    }

    private void handleDl(final DownloadLink link, final Account account) throws Exception {
        int chunks = 0;
        if (link.getBooleanProperty(DownsterNet.NOCHUNKS, false)) {
            chunks = 1;
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, chunks);
        } catch (final SocketTimeoutException e) {
            throw new PluginException(LinkStatus.ERROR_RETRY, e.getMessage());
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            int maxRetriesOnDownloadError = getPluginConfig().getIntegerProperty(MAX_RETRIES_DL_ERROR_PROPERTY, DEFAULT_MAX_RETRIES_DL_ERROR);
            mhm.handleErrorGeneric(account, link, "unknowndlerror", maxRetriesOnDownloadError, 5 * 60 * 1000l);
        }
        try {
            // start the dl
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(DownsterNet.NOCHUNKS, false) == false) {
                    link.setProperty(DownsterNet.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(DownsterNet.NOCHUNKS, false) == false) {
                link.setProperty(DownsterNet.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private String checkDirectLink(final DownloadLink downloadLink) {
        final String dllink = downloadLink.getStringProperty(DLLINK_PROP_NAME);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                final URLConnectionAdapter con = br2.openGetConnection(dllink);
                try {
                    if (con.getResponseCode() >= 400 || !con.isOK() || con.getLongContentLength() == -1) {
                        downloadLink.setProperty(DLLINK_PROP_NAME, Property.NULL);
                        return null;
                    } else {
                        return dllink;
                    }
                } finally {
                    con.disconnect();
                }
            } catch (Exception e) {
                logger.log(e);
                downloadLink.setProperty(DLLINK_PROP_NAME, Property.NULL);
            }
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }
}