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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debriditalia.com" }, urls = { "https?://\\w+\\.debriditalia\\.com/dl/\\d+/.+" })
public class DebridItaliaCom extends antiDDoSForHost {
    public DebridItaliaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.debriditalia.com/premium.php");
        try {
            /* 2021-06-21: 30 Requests per 1 Minute, block of 1 hour when exceeded */
            Browser.setBurstRequestIntervalLimitGlobal("debriditalia.com", 2000, 30, 60000);
        } catch (final Throwable ignore) {
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.debriditalia.com/premium.php";
    }

    private static final String          API_BASE = "https://debriditalia.com/api.php";
    private static MultiHosterManagement mhm      = new MultiHosterManagement("debriditalia.com");
    private static final String          NOCHUNKS = "NOCHUNKS";

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setConnectTimeout(60 * 1000);
            prepBr.setReadTimeout(60 * 1000);
            /* 401 can happen when user enters invalid logindata */
            prepBr.addAllowedResponseCodes(new int[] { 401 });
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
        }
        return prepBr;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (StringUtils.isEmpty(account.getUser()) || StringUtils.isEmpty(account.getPass())) {
            /* Server returns 401 if you send empty fields (logindata) */
            accountInvalid();
        }
        final AccountInfo ac = new AccountInfo();
        ac.setUnlimitedTraffic();
        if (!loginAPI(account)) {
            accountInvalid();
        }
        if (br.containsHTML("(?i)<status>\\s*expired\\s*</status>")) {
            ac.setExpired(true);
            return ac;
        }
        final String expire = br.getRegex("<expiration>(\\d+)</expiration>").getMatch(0);
        if (expire == null) {
            accountInvalid();
        }
        ac.setValidUntil(Long.parseLong(expire) * 1000l, this.br);
        getPage(API_BASE + "?hosts");
        final String[] hosts = br.getRegex("\"([^<>\"]*?)\"").getColumn(0);
        ac.setMultiHostSupport(this, new ArrayList<String>(Arrays.asList(hosts)));
        account.setType(AccountType.PREMIUM);
        return ac;
    }

    private void accountInvalid() throws PluginException {
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!\r\nFalls du einen VPN verwendet hast, deaktiviere diesen und versuche es erneut.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!\r\nIn case you were using a VPN: Disable it and try again.", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        handleDl(link, null, link.getPluginPatternMatcher());
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleDl(link, account, link.getPluginPatternMatcher());
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        showMessage(link, "Generating link");
        /* since no requests are done with this.br we need to manually set so checkdirectlink is correct */
        prepBrowser(br, "https://" + this.getHost());
        String dllink = checkDirectLink(link, "debriditaliadirectlink");
        if (dllink == null) {
            String host_downloadlink = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            /* Workaround for server side debriditalia bug. */
            /*
             * Known hosts for which they do definitely not accept https urls [last updated 2015-10-05]: inclouddrive.com
             */
            host_downloadlink = host_downloadlink.replaceFirst("^https", "http");
            final UrlQuery query = new UrlQuery();
            query.add("generate", "on");
            query.add("u", Encoding.urlEncode(account.getUser()));
            query.add("p", encodePassword(account.getPass()));
            query.add("link", Encoding.urlEncode(host_downloadlink));
            if (link.getDownloadPassword() != null) {
                query.add("pass", Encoding.urlEncode(link.getDownloadPassword()));
            }
            getPage(API_BASE + "?" + query.toString());
            checkResponsecodeErrors(this.br);
            final String error = br.getRegex("(?i)^ERROR: (.+)$").getMatch(0);
            if (error != null) {
                /* First known errors with immediate waittime, then generic */
                if (error.equalsIgnoreCase("not_supported")) {
                    mhm.putError(account, link, 5 * 60 * 1000l, "Host not supported");
                } else if (error.equalsIgnoreCase("bandwidth_limit")) {
                    mhm.putError(account, link, 5 * 60 * 1000l, "Bandwidth limit reached");
                } else {
                    /* Treat a generic error e.g. "not_available" or "bandwidth_limit" (daily host specific bandwidth limit reached) */
                    mhm.handleErrorGeneric(account, link, "not_available", 20, 1 * 60 * 1000l);
                }
            }
            try {
                dllink = new URL(br.toString()).toString();
                /* Directlinks can be used for up to 2 days */
                link.setProperty("debriditaliadirectlink", dllink);
            } catch (final MalformedURLException e) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 20, 5 * 60 * 1000l);
            }
        }
        handleDl(link, account, dllink);
    }

    private void updateFileName(final DownloadLink link, final URLConnectionAdapter connection) {
        if (link.getFinalFileName() == null) {
            final String serverFilename = getFileNameFromHeader(this.dl.getConnection());
            if (serverFilename != null) {
                link.setFinalFileName(serverFilename);
            }
        }
    }

    private void handleDl(final DownloadLink link, final Account account, final String dllink) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int chunks = 0;
        if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false)) {
            chunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, chunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            mhm.handleErrorGeneric(account, link, "Final downloadurl does not lead to file", 50);
        }
        updateFileName(link, dl.getConnection());
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

    @Override
    public Downloadable newDownloadable(DownloadLink downloadLink, final Browser br) {
        return new DownloadLinkDownloadable(downloadLink, br) {

            @Override
            protected DispositionHeader parseDispositionHeader(URLConnectionAdapter connection) {
                final DispositionHeader ret = super.parseDispositionHeader(connection);
                if (ret != null && isValidFileNameFromHeader(ret.getFilename())) {
                    return ret;
                } else {
                    return null;
                }
            }
        };
    }

    public static String getFileNameFromHeader(final URLConnectionAdapter urlConnection) {
        final String ret = Plugin.getFileNameFromConnection(urlConnection);
        if (!isValidFileNameFromHeader(ret)) {
            return null;
        } else {
            /* They sometimes return html-encoded filenames - let's fix this! */
            return Encoding.htmlDecode(ret);
        }
    }

    private static boolean isValidFileNameFromHeader(final String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        } else if (StringUtils.equalsIgnoreCase("Download", fileName)) {
            return false;
        } else if ("0".equals(fileName)) {
            // Content-Disposition: attachment; filename="0"
            return false;
        } else {
            return true;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = new Browser();
        URLConnectionAdapter con = null;
        try {
            br.setFollowRedirects(true);
            // head connection not possible. -raztoki-20160112
            final GetRequest request = br.createGetRequest(link.getPluginPatternMatcher());
            request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity");
            con = openAntiDDoSRequestConnection(br, request);
            if (looksLikeDownloadableContent(con)) {
                updateFileName(link, con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                link.setAvailable(true);
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
        checkResponsecodeErrors(br);
        if (br.containsHTML("(?i)<status>\\s*valid\\s*</status>") || br.containsHTML("(?i)<status>\\s*expired\\s*</status>")) {
            return true;
        } else if (br.containsHTML("(?i)<status>\\s*invalid\\s*</status>")) {
            return false;
        } else {
            /* This should never happen */
            throw new AccountUnavailableException("Unknown login status", 3 * 60 * 1000l);
        }
    }

    private void checkResponsecodeErrors(final Browser br) throws AccountUnavailableException {
        if (br.getHttpConnection().getResponseCode() == 429) {
            /*
             * 429 too many requests via Cloudflare: <title>Access denied | debriditalia.com used Cloudflare to restrict access</title>
             */
            throw new AccountUnavailableException("Error 429 Too Many Requests", 1 * 60 * 1000l);
        }
    }

    /** Workaround(s) for special chars issues with login passwords. */
    private String encodePassword(final String password) {
        if (password.contains("%")) {
            return password;
        } else {
            return Encoding.urlEncode(password);
        }
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
                    final GetRequest request = br2.createGetRequest(dllink);
                    request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity");
                    con = openAntiDDoSRequestConnection(br2, request);
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

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (isDirectLink(link)) {
            /* Generated links do not require an account to download */
            return true;
        } else if (account == null) {
            // no non account handleMultiHost support.
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    private boolean isDirectLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(this.getLazyP().getPatternSource())) {
            return true;
        } else {
            return false;
        }
    }
}