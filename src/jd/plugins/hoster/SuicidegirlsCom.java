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

import java.util.ArrayList;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "suicidegirls.com" }, urls = { "http://suicidegirlsdecrypted/\\d+|https?://(?:www\\.)?suicidegirls\\.com/videos/\\d+/[A-Za-z0-9\\-_]+/" })
public class SuicidegirlsCom extends PluginForHost {
    public SuicidegirlsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.suicidegirls.com/shop/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        prepBR(br);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.suicidegirls.com/legal/";
    }

    /* Linktypes */
    private static final String  TYPE_DECRYPTED            = "http://suicidegirlsdecrypted/\\d+";
    private static final String  TYPE_VIDEO                = "(?i)https?://(?:www\\.)?suicidegirls\\.com/videos/(\\d+)/[A-Za-z0-9\\-_]+/";
    /* Properties */
    public static final String   PROPERTY_DIRECTURL        = "directlink";
    public static final String   PROPERTY_IMAGE_NAME       = "imageName";
    /* Connection stuff */
    private static final boolean FREE_RESUME               = false;
    private static final int     FREE_MAXCHUNKS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME       = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS    = 0;
    private static final boolean ACCOUNT_PREMIUM_RESUME    = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private String               dllink                    = null;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_VIDEO)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_VIDEO).getMatch(0);
        } else {
            // TYPE_DECRYPTED
            return link.getStringProperty(PROPERTY_IMAGE_NAME);
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (account != null) {
            this.login(account, false);
        }
        String filename = null;
        if (link.getPluginPatternMatcher().matches(TYPE_VIDEO)) {
            final String videoID = new Regex(link.getPluginPatternMatcher(), TYPE_VIDEO).getMatch(0);
            link.setLinkID(this.getHost() + "//" + videoID);
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRegex("<source src=\"(http[^<>\"]*?)\" type=\\'video/mp4\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://[^/]+/videos/[^/]+\\.(?:mp4|m3u8))\"").getMatch(0);
            }
            filename = br.getRegex("<h2 class=\"title\">(?:SuicideGirls:\\s*)?([^<>\"]*?)</h2>").getMatch(0);
            if (filename == null) {
                /* Fallback to url-filename */
                filename = new Regex(link.getPluginPatternMatcher(), "(?i)/videos/\\d+/([A-Za-z0-9\\-_]+)/").getMatch(0);
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename += ".mp4";
        } else {
            filename = link.getStringProperty(PROPERTY_IMAGE_NAME);
            dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        }
        link.setName(filename);
        if (!StringUtils.containsIgnoreCase(dllink, ".m3u8") && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                if (filename == null) {
                    filename = getFileNameFromHeader(con);
                }
                link.setProperty(PROPERTY_DIRECTURL, dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (link.getFinalFileName() == null && filename != null) {
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        // String dllink = checkDirectLink(downloadLink, directlinkproperty);
        // if (dllink == null) {
        // dllink = br.getRegex("").getMatch(0);
        // if (dllink == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // }
        if (dllink == null) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            /* 2020-03-02: New: HLS streams */
            br.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                /* No content available --> Probably the user wants to download hasn't aired yet --> Wait and retry later! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Sendung wurde noch nicht ausgestrahlt", 60 * 60 * 1000l);
            }
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumable, maxchunks);
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
            link.setProperty(directlinkproperty, dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            prepBR(br);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(this.getHost(), cookies);
                if (!validateCookies) {
                    logger.info("Trust cookies without checking");
                    return;
                }
                logger.info("Checking login cookies");
                // do a test
                br.getPage("https://www." + this.getHost() + "/member/account/");
                if (isLoggedin(br)) {
                    logger.info("Successfully logged in via cookies");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.getPage("https://www." + this.getHost());
            final Form loginform = br.getFormbyProperty("id", "login-form");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // login can contain recaptchav2
            if (loginform.containsHTML("g-recaptcha") && loginform.containsHTML("data-sitekey")) {
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                    @Override
                    public String getSiteKey() {
                        return getSiteKey(loginform.getHtmlCode());
                    };
                }.getToken();
                loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            loginform.put("username", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginform);
            final String msg = PluginJSonUtils.getJsonValue(br, "message");
            /* 2020-10-19: E.g. {"message":"Invalid username or password.","code":"invalid_credentials"} */
            // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" +
            // Encoding.urlEncode(account.getPass()));
            if (!StringUtils.isEmpty(msg)) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML(">\\s*Log Out\\s*<");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (br.getURL() == null || !br.getURL().contains("/member/account")) {
            br.getPage("https://www." + this.getHost() + "/member/account/");
        }
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("YOUR ACCOUNT IS CLOSING IN\\s*<a>(\\d+ weeks?, \\d+ days?)<").getMatch(0);
        long expire_long = -1;
        if (expire != null) {
            final Regex info = new Regex(expire, "(\\d+) weeks?, (\\d+) days?");
            final String weeks = info.getMatch(0);
            final String days = info.getMatch(1);
            final long days_total = Long.parseLong(weeks) * 7 * +Long.parseLong(days);
            expire_long = System.currentTimeMillis() + days_total * 24 * 60 * 60 * 1000l;
        }
        if (expire == null && expire_long == -1) {
            expire = br.getRegex("Your account will be cancelled on (\\w+ \\d+, \\d{4})").getMatch(0);
            expire_long = TimeFormatter.getMilliSeconds(expire, "MMMM dd, yyyy", Locale.ENGLISH);
        }
        if (expire_long > -1) {
            ai.setValidUntil(expire_long);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(getMaxSimultanPremiumDownloadNum());
            account.setConcurrentUsePossible(true);
        } else {
            // free account
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(getMaxSimultanFreeDownloadNum());
            account.setConcurrentUsePossible(false);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link, account, true);
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            // String dllink = this.checkDirectLink(link, "premium_directlink");
            // if (dllink == null) {
            // dllink = br.getRegex("").getMatch(0);
            // if (dllink == null) {
            // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
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
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    public Account login(final Browser br) {
        final ArrayList<Account> accounts = AccountController.getInstance().list("suicidegirls.com");
        if (accounts != null && accounts.size() != 0) {
            final LogInterface logger = br.getLogger();
            for (final Account account : accounts) {
                try {
                    login(account, false);
                    return account;
                } catch (final PluginException e) {
                    logger.log(e);
                    account.setValid(false);
                } catch (final Exception e) {
                    logger.log(e);
                }
            }
        }
        return null;
    }

    public Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "burlesque_ad_closed", "True");
        br.setCookie(this.getHost(), "django_language", "en");
        return br;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}