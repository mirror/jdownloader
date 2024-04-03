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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FastShareCz extends PluginForHost {
    public FastShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://fastshare.cz/cenik_cs");
    }

    @Override
    public String getAGBLink() {
        return "https://www.fastshare.cz/podminky";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "fastshare.cloud", "fastshare.live", "fastshare.cz", "fastshare.pl", "netshare.cz", "dinoshare.cz" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /**
         * 2023-04-11: Main domain has changed from fastshare.cz to fastshare.live. </br>
         * 2024-03-21: Use fastshare.cloud as main domain because some users got problems reaching fastshare.live.
         */
        return this.rewriteHost(getPluginDomains(), host);
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\d+|[a-f0-9]{32,})/?[^<>\"#]*");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setCookie(this.getHost(), "lang", "cs");
        return br;
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

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceFirst("(?i)http://", "https://");
    }

    private static String getDirecturlProperty(final Account account) {
        String str = "directurl";
        if (account != null) {
            str += "_" + account.getType();
        }
        return str;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (isPremiumAccount(account)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final Account account) {
        if (isPremiumAccount(account)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) or unknown account type */
            return 1;
        }
    }

    private boolean isPremiumAccount(final Account account) {
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /*
         * 2023-09-18: Only registered users can see/download files. For non-logged-in-users it seems like all files are displayed as
         * offline (website redirects to mainpage).
         */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(this.getHost(), "lang", "cs");
        br.setCustomCharset("utf-8");
        /**
         * 2023-08-20: The following information only applies for users of specific countries such as Germany: </br>
         * When a user is not logged in, all files appear to be offline so effectively a linkcheck is only possible when an account is
         * given.
         */
        final boolean linkcheckOnlyPossibleWhenLoggedIn = false;
        if (linkcheckOnlyPossibleWhenLoggedIn && account == null) {
            throw new AccountRequiredException();
        }
        if (account != null) {
            this.login(account, false);
        }
        br.setFollowRedirects(true);
        final String directurlproperty = getDirecturlProperty(account);
        final String contenturl = getContentURL(link);
        URLConnectionAdapter con = br.openGetConnection(contenturl);
        if (this.looksLikeDownloadableContent(con)) {
            logger.info("Detected direct-download");
            if (con.getCompleteContentLength() > 0) {
                if (con.isContentDecoded()) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            }
            final String filename = Plugin.getFileNameFromDispositionHeader(con);
            if (filename != null) {
                link.setFinalFileName(filename);
            }
            link.setProperty(directurlproperty, con.getURL().toExternalForm());
            return AvailableStatus.TRUE;
        } else {
            br.followConnection();
        }
        final String htmlRefresh = br.getRequest().getHTMLRefresh();
        if (htmlRefresh != null) {
            if (htmlRefresh.matches("^https?://[^/]+/?$")) {
                /* Redirect to mainpage */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                br.getPage(htmlRefresh);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)(<title>\\s*FastShare\\.[a-z]+\\s*</title>|>Tento soubor byl smazán na základě požadavku vlastníka autorských)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*Soubor byl smazán")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1\\s*title\\s*=\\s*\"(.*?)\\s*\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h2><b><span style=color:black;>([^<>\"]*?)</b></h2>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>\"]*?)\\s*\\|\\s*FastShare\\.[a-z]+\\s*</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("(?i)<tr><td>\\s*(Velikost|Size): </td><td style=font\\-weight:bold>([^<>\"]*?)</td></tr>").getMatch(1);
        if (filesize == null) {
            filesize = br.getRegex("(Velikost|Size): ([0-9]+ .*?),").getMatch(1);
            if (filesize == null) {
                filesize = br.getRegex("(?i)<strong>\\s*(Velikost|Size) :</strong>([^<>\"]*?)<").getMatch(1);
                if (filesize == null) {
                    filesize = br.getRegex("class\\s*=\\s*\"footer-video-size\"\\s*>\\s*(<i.*?</i>\\s*)?([0-9\\.,]+\\s*(?:&nbsp;)?[MBTKG]+)\\s*<").getMatch(1);
                }
            }
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize)));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String directurlproperty = getDirecturlProperty(account);
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            /* First check if linkcheck found a direct-url */
            dllink = link.getStringProperty(directurlproperty);
            if (dllink == null) {
                requestFileInformation(link, account);
                this.checkErrors(br, link, account);
                if (this.isPremiumAccount(account)) {
                    dllink = br.getRegex("\"(https?://[a-z0-9]+\\." + Pattern.quote(br.getHost()) + "/download\\.php[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("class=\"speed\">\\s*<a href=\"(https?://[^/]*" + Pattern.quote(br.getHost()) + "/[^<>\"]*?)\"").getMatch(0);
                    }
                } else {
                    br.setFollowRedirects(false);
                    final String captchaLink = br.getRegex("\"(/securimage_show\\.php\\?sid=[a-z0-9]+)\"").getMatch(0);
                    String action = br.getRegex("=\\s*\"(/free/[^<>\"]*?)\"").getMatch(0);
                    if (action == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (captchaLink != null) {
                        final String captcha = getCaptchaCode(captchaLink, link);
                        br.postPage(action, "code=" + Encoding.urlEncode(captcha));
                    } else {
                        br.postPage(action, "");
                    }
                    if (br.containsHTML("Špatně zadaný kód\\. Zkuste to znovu")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    dllink = br.getRedirectLocation();
                }
                if (dllink != null && canHandle(dllink)) {
                    // TODO: Check if this is still needed
                    // eg redirect http->https or cut-off ref parameter
                    br.getPage(dllink);
                    dllink = br.getRedirectLocation();
                }
                if (dllink == null) {
                    this.checkErrors(br, link, account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        br.setFollowRedirects(true);
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        if (storedDirecturl == null) {
            /* Save directurl to be able to re-use it next time. */
            link.setProperty(directurlproperty, dllink);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            checkErrors(br, link, account);
            if (br.getRequest().getHtmlCode().length() <= 100) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.setFilenameFix(isContentDispositionFixRequired(dl, dl.getConnection(), link));
        dl.startDownload();
    }

    private void login(final Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    setCookies(br, cookies);
                    if (!force) {
                        /* Do not validate cookies. */
                        return;
                    }
                    logger.info("Attempting cookie login...");
                    br.getPage("https://" + this.getHost() + "/user");
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.setFollowRedirects(true);
                br.postPage("https://" + this.getHost() + "/sql.php", "login=" + Encoding.urlEncode(account.getUser()) + "&heslo=" + Encoding.urlEncode(account.getPass()));
                final String redirect = br.getRequest().getHTMLRefresh();
                if (redirect != null) {
                    br.getPage(redirect);
                }
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                final Cookies freshCookies = br.getCookies(br.getHost());
                account.saveCookies(freshCookies, "");
                setCookies(br, freshCookies);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    /** Sets given cookies on all domains we support. */
    private void setCookies(final Browser br, final Cookies cookies) {
        for (final String[] domains : getPluginDomains()) {
            for (final String domain : domains) {
                br.setCookies(domain, cookies);
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/logout\\.php")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (br.getRequest() == null || !br.getURL().contains("/user")) {
            br.getPage("/user");
        }
        /*
         * 2022-12-20: Free accounts typically o not have any traffic/credit. Premium accounts typically do but can at the same time have
         * unlimited traffic.
         */
        /* 2021-02-12: E.g. <td>3 445.56 GB </td> */
        String trafficLeftStr = br.getRegex(">\\s*(?:Kredit|Credit|Kredyty)\\s*:\\s*</td>\\s*<td[^>]*?>([^<>\"&]+)").getMatch(0);
        if (trafficLeftStr != null) {
            trafficLeftStr = trafficLeftStr.trim().replace(" ", "");
        }
        final boolean userHasUnlimitedTraffic = br.containsHTML("(?i)href=\"/user\">\\s*Neomezené stahování\\s*</span>");
        final String unlimitedTrafficInfo = br.getRegex("(?:Neomezené stahování)\\s*:\\s*</td>\\s*<td>\\s*<span[^>]*>\\s*(.*?)\\s*<").getMatch(0);
        final boolean isPremiumUnlimitedTrafficUser = (unlimitedTrafficInfo != null && !StringUtils.equalsIgnoreCase(unlimitedTrafficInfo, "Neaktivní")) || userHasUnlimitedTraffic;
        if (trafficLeftStr == null && !isPremiumUnlimitedTrafficUser) {
            account.setType(AccountType.FREE);
        } else {
            if (isPremiumUnlimitedTrafficUser) {
                final String until = new Regex(unlimitedTrafficInfo, "do\\s*(\\d+\\.\\d+\\.\\d+)").getMatch(0);
                if (until != null) {
                    final long validUntil = TimeFormatter.getMilliSeconds(until, "dd.MM.yyyy", Locale.ENGLISH) + (23 * 60 * 60 * 1000l);
                    if (validUntil > 0) {
                        ai.setValidUntil(validUntil, this.br);
                    }
                } else {
                    logger.warning("Failed to find expire date of unlimited traffic");
                }
                ai.setUnlimitedTraffic();
                if (trafficLeftStr != null) {
                    /* User has unlimited traffic and also still some traffic left on account --> Display that in status text. */
                    ai.setStatus("Unlimited traffic and " + trafficLeftStr);
                } else {
                    ai.setStatus("Unlimited traffic");
                }
            } else if (trafficLeftStr != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(trafficLeftStr));
            }
            account.setType(AccountType.PREMIUM);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    private void checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        {
            /* [Premium-] account related error messages */
            if (br.containsHTML("(?i)máte dostatečný kredit pro stažení tohoto souboru")) {
                throw new AccountUnavailableException("Traffic limit reached", 5 * 60 * 1000l);
            }
        }
        final URLConnectionAdapter con = br.getRequest().getHttpConnection();
        if (con.getResponseCode() == 200 && con.getCompleteContentLength() == 0 && StringUtils.containsIgnoreCase(con.getContentType(), "text/html")) {
            final String directurlproperty = getDirecturlProperty(account);
            if (link.removeProperty(directurlproperty)) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired");
            }
        }
        final String errortextMaxConcurrentDownloadsLimit = "Reached max concurrent downloads limit";
        if (br.containsHTML("(?i)(>100% FREE slotů je plných|>Využijte PROFI nebo zkuste později)")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 10 * 60 * 1000l);
        } else if (br.containsHTML("Přes FREE můžete stahovat jen jeden soubor současně|Pres FREE muzete stahovat jen jeden soubor najednou")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errortextMaxConcurrentDownloadsLimit, 3 * 60 * 1000l);
        } else if (br.containsHTML("<script>alert\\(")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error #1", 30 * 60 * 1000l);
        } else if (br.getRequest().getHtmlCode().length() <= 100) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error #2 (blank page)", 30 * 60 * 1000l);
        } else if (br.containsHTML("/free-stahovani")) {
            /* Free downloadlimit reached or message "As a free user you can download max 1 file at a time" */
            if (account != null) {
                throw new AccountUnavailableException(errortextMaxConcurrentDownloadsLimit, 1 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errortextMaxConcurrentDownloadsLimit, 1 * 60 * 1000l);
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (AccountType.FREE.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        } else {
            return false;
        }
    }
}