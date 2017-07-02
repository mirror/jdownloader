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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uploadgig.com" }, urls = { "https?://(?:www\\.)?uploadgig\\.com/file/download/[A-Za-z0-9]+(/.+)?" })
public class UploadgigCom extends antiDDoSForHost {

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    public UploadgigCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "http://uploadgig.com/page/content/term-of-service";
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                  = false;
    private final int            FREE_MAXCHUNKS               = 1;
    private final int            FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -3;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 10;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        final String url_filename = new Regex(link.getDownloadURL(), "([^/]+)$").getMatch(0);
        if (!link.isNameSet() && url_filename != null) {
            /* Set temp name */
            link.setName(url_filename);
        }
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"filename\">([^<>\"]+)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"pg_title\">Download \"([^<>\"]+)\"").getMatch(0);
        }
        if (filename == null) {
            if (!link.isNameSet()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // we continue.
        } else {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        final String filesize = br.getRegex("class=\"filesize\">\\[([^<>\"]+)\\]<").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            // premium only content
            if (br.containsHTML(">This file can be downloaded by Premium Member only\\.<")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            String csrf_tester = br.getCookie(this.getHost(), "firewall");
            if (csrf_tester == null) {
                csrf_tester = br.getRegex("name=\"csrf_tester\"\\s*?value=\"([^<>\"]+)\"").getMatch(0);
                if (csrf_tester != null) {
                    br.setCookie(this.br.getHost(), "firewall", csrf_tester);
                }
            }
            final String fid = getFID(downloadLink);
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            String postData = "file_id=" + fid + "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
            if (csrf_tester != null) {
                postData += "&csrf_tester=" + csrf_tester;
            }
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("Accept", "*/*");
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage(br2, "/file/free_dl", postData);
            errorhandlingFree(br2);
            if (br2.getHttpConnection().getResponseCode() == 403) {
                /* Usually only happens with wrong POST values */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
            }
            // they use javascript to determine finallink...
            dllink = getDllink(br2);
            int wait = 60;
            final String waittime_str = PluginJSonUtils.getJsonValue(br2, "cd");
            if (waittime_str != null) {
                wait = Integer.parseInt(waittime_str);
            }
            this.sleep(wait * 1001l, downloadLink);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            /* E.g. "The download link has expired, please buy premium account or start download file from the beginning." */
            else if (br.containsHTML("The download link has expired")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'The download link has expired'", 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String getDllink(final Browser br) throws PluginException {
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        final String js = br.getRegex("\\$\\('#countdownContainer'\\)\\.html\\('<a class=\"btn btn-success btn-lg\" href=\"'\\+pres\\['(\\w+)'\\]+'\">Download now</a>');").getMatch(0);
        if (js != null) {
            String dllink = PluginJSonUtils.getJsonValue(br, js);
            if (dupe.add(dllink) && testLink(dllink)) {
                return dllink;
            }
        }
        // fail over
        final String[] jokesonyou = br.getRegex("https?://[a-zA-Z0-9_\\-.]*uploadgig\\.com/dl/[a-zA-Z0-9]+/dlfile").getColumn(-1);
        if (jokesonyou != null) {
            final List<String> list = Arrays.asList(jokesonyou);
            Collections.shuffle(list);
            for (final String link : list) {
                if (dupe.add(link) && testLink(link)) {
                    return link;
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private boolean testLink(String dllink) {
        URLConnectionAdapter con = null;
        try {
            final Browser br2 = this.br.cloneBrowser();
            con = br2.openHeadConnection(dllink);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                return false;
            }
            return true;
        } catch (final Exception e) {
            return false;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/download/([^/]+)").getMatch(0);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            if (!testLink(dllink)) {
                downloadLink.setProperty(property, Property.NULL);
                return null;
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void errorhandlingFree(final Browser br) throws PluginException {
        if ("m".equals(br.toString())) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Reached the download limit for the hour", 1 * 60 * 60 * 1000l);
        } else if ("rfd".equals(br.toString()) || "fl".equals(br.toString())) {
            throw new AccountRequiredException();
        }
        // "0" and "e" shouldn't happen
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                getPage("http://" + account.getHoster() + "/login/form");
                final Form loginform = br.getForm(0);
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("email", Encoding.urlEncode(account.getUser()));
                loginform.put("pass", Encoding.urlEncode(account.getPass()));
                loginform.put("rememberme", "1");
                submitForm(loginform);
                if (isAccountCookiesMissing()) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    private boolean isAccountCookiesMissing() {
        final boolean missing = br.getCookie(this.getHost(), "fs_secure") == null || br.getCookie(this.getHost(), "fs_secure").equalsIgnoreCase("deleted");
        return missing;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        getPage("/user/my_account");
        final Regex trafficregex = this.br.getRegex("<dt>Daily traffic usage</dt>\\s*<dd>(\\d+)/(\\d+) MB");
        final String traffic_used_str = trafficregex.getMatch(0);
        final String traffic_max_str = trafficregex.getMatch(1);
        String expire = br.getRegex("Package expire date:</dt>\\s*<dd>(\\d{4}/\\d{2}/\\d{2})").getMatch(0);
        if (expire == null) {
            expire = br.getRegex(">(\\d{4}/\\d{2}/\\d{2})<").getMatch(0);
        }
        if (expire == null) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        if (traffic_used_str != null && traffic_max_str != null) {
            final long traffic_used = SizeFormatter.getSize(traffic_used_str + "MB");
            final long traffic_max = SizeFormatter.getSize(traffic_max_str + " MB");
            final long traffic_left = Math.max(0, traffic_max - traffic_used);
            ai.setTrafficMax(traffic_max);
            ai.setTrafficLeft(traffic_left);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        synchronized (LOCK) {
            login(account, false);
            br.setFollowRedirects(false);
            getPage(link.getDownloadURL());
            // ok we need a check that cookie session hasn't been deleted!!!
            if (isAccountCookiesMissing()) {
                // to ensure cookies are gone!
                account.clearCookies("");
                // you can't not have the cookies here, login method will throw exception.
                login(account, true);
                getPage(link.getDownloadURL());
                // if cookies are now gone.. wtf, site issue???
                if (isAccountCookiesMissing()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                // can be redirect
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    dllink = link.getDownloadURL();
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) {
                    // some blocked message here
                    if (br.containsHTML(">Blocked")) {
                        // TODO:
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (br.containsHTML("Your \\d+Gb daily download traffic has been used\\.")) {
                    account.setNextDayAsTempTimeout(br);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}