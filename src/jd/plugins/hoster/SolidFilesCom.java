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
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.SolidFilesComConfig;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SolidFilesCom extends PluginForHost {
    public SolidFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(500l);
        this.enablePremium("https://www.solidfiles.com/premium");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "solidfiles.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:d|v|e)/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "http://www.solidfiles.com/terms/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean        ACCOUNT_FREE_RESUME          = true;
    private final int            ACCOUNT_FREE_MAXCHUNKS       = 1;
    // private final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 1;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    public static final String   PROPERTY_DIRECT_DOWNLOAD     = "directDownload";
    private static final String  TYPE_EMBED                   = "https?://[^/]+/e/([A-Za-z0-9]+)";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            link.setPluginPatternMatcher("http://www." + this.getHost() + "/v/" + this.getFID(link));
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* Offline links should also get nice filenames */
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getBooleanProperty(SolidFilesCom.PROPERTY_DIRECT_DOWNLOAD, false)) {
            return AvailableStatus.TRUE;
        }
        br.getPage(link.getPluginPatternMatcher());
        isOffline(false);
        // String filename = PluginJSonUtils.getJsonValue(br, "name");
        String filename = br.getRegex("<h1 class=\"node-name\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) (?:-|\\|) Solidfiles</title>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setName(Encoding.htmlDecode(filename).replace(" ", "_").trim());// spaces are replaced by _
        String filesize = PluginJSonUtils.getJsonValue(br, "size");
        if (filesize == null) {
            filesize = br.getRegex("class=\"filesize\">\\(([^<>\"]*?)\\)</span>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("dt>File size<.*?dd>(.*?)</").getMatch(0);
                if (filesize == null) {
                    /* 2017-03-21 */
                    filesize = br.getRegex("</copy-button>([^<>\"]*?) -").getMatch(0);
                }
            }
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private void isOffline(boolean isDownload) throws PluginException {
        if ((!isDownload && br.getURL().contains("/error/")) || br.containsHTML(">404<|>Not found<|>We couldn't find the file you requested|Access to this file was disabled|The file you are trying to download has|>File not available|This file/folder has been disabled") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleFreeDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleFreeDownload(final DownloadLink link, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception {
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, true, 3)) {
            requestFileInformation(link);
            if (br.containsHTML("We're currently processing this file and it's unfortunately not available yet|>\\s*We're preparing your download, please wait\\.{3}\\s*<")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is not available yet", 5 * 60 * 1000l);
            }
            String dllink;
            if (link.getBooleanProperty(SolidFilesCom.PROPERTY_DIRECT_DOWNLOAD, false)) {
                // direct download...
                dllink = link.getPluginPatternMatcher();
            } else {
                dllink = br.getRegex("class=\"direct-download regular-download\"[^\r\n]+href=\"(https?://[^\"']+)").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("(\"|'|)(https?://s\\d+\\.solidfilesusercontent\\.com/.*?)\\1").getMatch(1);
                }
                if (dllink == null) {
                    logger.warning("Final downloadlink is null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = dllink.trim();
            }
            // inal long downloadCurrentRaw = link.getDownloadCurrentRaw();
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 503) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 - use less connections and try again", 10 * 60 * 1000l);
                }
                isOffline(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, br.getURL());
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                String bearertoken = getBearertoken(account);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("https://www." + this.getHost() + "/login");
                String token = br.getRegex("name=token content=([^<>\"]+)>").getMatch(0);
                if (token == null) {
                    token = br.getCookie(br.getHost(), "csrftoken");
                }
                if (token == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("/login", "csrfmiddlewaretoken=" + Encoding.urlEncode(token) + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                bearertoken = PluginJSonUtils.getJsonValue(br, "access_token");
                if (br.getCookie(br.getHost(), "sessionid") == null || bearertoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("bearertoken", bearertoken);
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        prepBRAjax(account);
        br.getPage("https://www." + this.getHost() + "/api/payments?limit=25&offset=0");
        ai.setUnlimitedTraffic();
        final String subscriptioncount = PluginJSonUtils.getJsonValue(br, "count");
        if (subscriptioncount == null || subscriptioncount.equals("0")) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        } else {
            /* TODO: Add expire date */
            // final String expire = null;
            // if (expire != null) {
            // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            // }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        }
        account.setValid(true);
        return ai;
    }

    private void prepBRAjax(final Account account) {
        final String csrftoken = getCsrftoken();
        final String bearertoken = getBearertoken(account);
        if (csrftoken != null) {
            br.getHeaders().put("X-CSRFToken", csrftoken);
        }
        if (bearertoken != null) {
            br.getHeaders().put("Authorization", "Bearer " + bearertoken);
        }
    }

    private String getBearertoken(final Account account) {
        return account.getStringProperty("bearertoken", null);
    }

    private String getCsrftoken() {
        return br.getCookie(br.getHost(), "csrftoken");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            login(account, false);
            handleFreeDownload(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            /* TODO: Add premium support! */
            if (true) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public Class<SolidFilesComConfig> getConfigInterface() {
        return SolidFilesComConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}