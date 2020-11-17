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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.StringUtils;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debriditalia.com" }, urls = { "https?://\\w+\\.debriditalia\\.com/dl/\\d+/.+" })
public class DebridItaliaCom extends antiDDoSForHost {
    public DebridItaliaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.debriditalia.com/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "https://www.debriditalia.com/premium.php";
    }

    private static final String          API_BASE = "https://debriditalia.com/api.php";
    private static MultiHosterManagement mhm      = new MultiHosterManagement("debriditalia.com");
    private static final String          NOCHUNKS = "NOCHUNKS";
    private String                       dllink   = null;

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setConnectTimeout(60 * 1000);
            prepBr.setReadTimeout(60 * 1000);
            /* 401 can happen when user enters invalid logindata */
            prepBr.addAllowedResponseCodes(401);
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
        }
        return prepBr;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (account.getUser().equals("") || account.getPass().equals("")) {
            /* Server returns 401 if you send empty fields (logindata) */
            accountInvalid();
        }
        final AccountInfo ac = new AccountInfo();
        ac.setProperty("multiHostSupport", Property.NULL);
        ac.setUnlimitedTraffic();
        if (!loginAPI(account)) {
            if (br.containsHTML("<status>expired</status>")) {
                ac.setStatus("Account is expired!");
                ac.setExpired(true);
                return ac;
            }
            accountInvalid();
        }
        final String expire = br.getRegex("<expiration>(\\d+)</expiration>").getMatch(0);
        if (expire == null) {
            ac.setStatus("Account is invalid. Invalid or unsupported accounttype!");
            accountInvalid();
        }
        ac.setValidUntil(Long.parseLong(expire) * 1000l);
        getPage(API_BASE + "?hosts");
        final String[] hosts = br.getRegex("\"([^<>\"]*?)\"").getColumn(0);
        final List<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setStatus("Premium Account");
        return ac;
    }

    private void accountInvalid() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDl(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleDl(link, account);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        showMessage(link, "Generating link");
        /* since no requests are done with this.br we need to manually set so checkdirectlink is correct */
        prepBrowser(br, "https://debriditalia.com/");
        dllink = checkDirectLink(link, "debriditaliadirectlink");
        if (dllink == null) {
            String host_downloadlink = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            /* Workaround for server side debriditalia bug. */
            /*
             * Known hosts for which they do definitely not accept https urls [ last updated 2015-10-05]: share-online.biz, inclouddrive.com
             */
            host_downloadlink = host_downloadlink.replace("https://", "http://");
            final String encodedLink = Encoding.urlEncode(host_downloadlink);
            getPage(API_BASE + "?generate=on&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + encodePassword(account.getPass()) + "&link=" + encodedLink);
            /* Either server error or the host is broken (we have to find out by retrying) */
            if (br.containsHTML("ERROR: not_available")) {
                mhm.handleErrorGeneric(account, link, "not_available", 20, 5 * 60 * 1000l);
            } else if (br.containsHTML("ERROR: not_supported")) {
                logger.info("Current host is not supported");
                mhm.putError(account, link, 5 * 60 * 1000l, "not_supported");
            }
            try {
                dllink = new URL(br.toString()).toString();
            } catch (final MalformedURLException e) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 20, 5 * 60 * 1000l);
            }
        }
        handleDl(link, account);
    }

    private void handleDl(final DownloadLink link, final Account account) throws Exception {
        int chunks = 0;
        if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false)) {
            chunks = 1;
        }
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, chunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            mhm.handleErrorGeneric(account, link, "", 50);
        }
        /* Directlinks can be used for up to 2 days */
        link.setProperty("debriditaliadirectlink", dllink);
        if (link.getFinalFileName() == null) {
            /* They sometimes return html-encoded filenames - let's fix this! */
            final String server_filename = getFileNameFromHeader(this.dl.getConnection());
            link.setFinalFileName(server_filename);
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
                if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                    link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY, null, e);
            } else {
                throw e;
            }
        }
    }

    public static String getFileNameFromHeader(final URLConnectionAdapter urlConnection) {
        final String ret = Plugin.getFileNameFromHeader(urlConnection);
        if (StringUtils.equalsIgnoreCase("Download", ret)) {
            return null;
        } else {
            return Encoding.htmlDecode(ret);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = new Browser();
        URLConnectionAdapter con = null;
        try {
            br.setFollowRedirects(true);
            // head connection not possible. -raztoki-20160112
            con = openAntiDDoSRequestConnection(br, br.createGetRequest(link.getPluginPatternMatcher()));
            if (looksLikeDownloadableContent(con)) {
                if (link.getFinalFileName() == null) {
                    final String server_filename = getFileNameFromHeader(con);
                    link.setFinalFileName(server_filename);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                link.setAvailable(true);
                dllink = br.getURL();
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } catch (final PluginException e) {
            throw e;
        } catch (final Throwable e) {
            logger.log(e);
            return AvailableStatus.UNCHECKABLE;
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private boolean loginAPI(final Account account) throws Exception {
        getPage(API_BASE + "?check=on&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + encodePassword(account.getPass()));
        if (!br.containsHTML("<status>valid</status>") || br.getHttpConnection().getResponseCode() == 401) {
            return false;
        }
        return true;
    }

    /** Workaround(s) for special chars issues with login passwords. */
    private String encodePassword(String password) {
        if (!password.contains("%")) {
            password = Encoding.urlEncode(password);
        }
        return password;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    // 2020-11-17: head connection not possible
                    con = openAntiDDoSRequestConnection(br2, br2.createGetRequest(dllink));
                    if (!looksLikeDownloadableContent(con)) {
                        throw new IOException();
                    } else {
                        return dllink;
                    }
                } finally {
                    con.disconnect();
                }
            } catch (Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                return null;
            }
        } else {
            return null;
        }
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
        if (isDirectLink(downloadLink)) {
            // generated links do not require an account to download
            return true;
        } else if (account == null) {
            // no non account handleMultiHost support.
            return false;
        } else {
            return true;
        }
    }

    private boolean isDirectLink(final DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches(this.getLazyP().getPatternSource())) {
            return true;
        } else {
            return false;
        }
    }
}