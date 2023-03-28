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

import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fakku.net" }, urls = { "https://books\\.fakku\\.net/images/manga/[^/]+/.+" })
public class FakkuNet extends antiDDoSForHost {
    public FakkuNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.fakku.net/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://www.fakku.net/about";
    }

    private static final String  TYPE_PREMIUM              = "https://books\\.fakku\\.net/images/manga/[^/]+/.+";
    /* Connection stuff */
    private static final boolean FREE_RESUME               = false;
    private static final int     FREE_MAXCHUNKS            = 1;
    private static final int     FREE_MAXDOWNLOADS         = 20;
    private static final boolean ACCOUNT_FREE_RESUME       = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS = 20;
    private String               dllink                    = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, AccountController.getInstance().getValidAccount(this.getHost()));
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        dllink = link.getPluginPatternMatcher();
        this.setBrowserExclusive();
        prepBRForLink(link);
        final String filename = link.getStringProperty("decrypterfilename", null);
        if (account != null) {
            login(this.br, account, false);
        }
        if (filename != null) {
            link.setFinalFileName(filename);
        }
        this.br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            /* Dont do HEAD requests here! */
            con = br.openGetConnection(dllink);
            if (!this.looksLikeDownloadableContent(con)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            link.setProperty("directlink", dllink);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private void prepBRForLink(final DownloadLink dl) {
        final String mainlink = dl.getStringProperty("mainlink", null);
        // this.br.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
        // this.br.getHeaders().put("Accept-Language", "de,en-US;q=0.7,en;q=0.3");
        if (mainlink != null) {
            /* Without this we will usually get a 404 and/or redirect to their mainpage */
            this.br.getHeaders().put("Referer", mainlink);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (link.getPluginPatternMatcher().matches(TYPE_PREMIUM)) {
            /* Account only */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        prepBRForLink(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    public boolean login(final Browser br, final Account account, final boolean validateCookies) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* Try to avoid login-captcha! */
                    br.setCookies(this.getHost(), cookies);
                    if (!validateCookies) {
                        return false;
                    }
                    getPage("https://www." + account.getHoster() + "/account/subscription");
                    if (isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        return true;
                    }
                    /* Full login required */
                    logger.info("Cookie login failed");
                    br.setCookies(this.getHost(), cookies);
                }
                getPage("https://www." + account.getHoster() + "/login");
                final Form loginform = br.getFormbyProperty("name", "login");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", account.getUser());
                InputField pwField = null;
                for (final InputField ifield : loginform.getInputFields()) {
                    if (ifield.isType(InputField.InputType.PASSWORD)) {
                        pwField = ifield;
                        break;
                    }
                }
                if (pwField == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                pwField.setValue(Encoding.urlEncode(account.getPass()));
                /* Handle login-captcha if required */
                if (this.containsRecaptchaV2Class(br)) {
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    final DownloadLink dl_dummy;
                    if (dlinkbefore != null) {
                        dl_dummy = dlinkbefore;
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                        this.setDownloadLink(dl_dummy);
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                }
                this.submitForm(loginform);
                if (!isLoggedin(br)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        if (br.getCookie(br.getHost(), "fakku_sid", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(br, account, true);
        ai.setUnlimitedTraffic();
        account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        if (!br.getURL().endsWith("/account/subscription")) {
            this.getPage("/account/subscription");
        }
        final Regex dateInfo = br.getRegex("(?i)>\\s*Next Billing Date\\s*</span>.*?placeholder=\"([A-Za-z]+ \\d+)[a-z]+, ([0-9]{4})\"");
        if (dateInfo.matches()) {
            final String nextBillingDate = dateInfo.getMatch(0) + " " + dateInfo.getMatch(1);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(nextBillingDate, "MMM dd yyyy", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        /* No need to login again here as we already logged in in the linkcheck. */
        handleDownload(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}