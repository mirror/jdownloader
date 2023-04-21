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
import java.util.List;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MydirtyhobbyCom extends PluginForHost {
    public MydirtyhobbyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.mydirtyhobby.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://cdn1-l-ha-e11.mdhcdn.com/u/TermsofUse_de.pdf";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mydirtyhobby.com", "mydirtyhobby.de" });
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
            ret.add("https?://(?:[a-z]+\\.)?" + buildHostsPatternPart(domains) + "/profil/\\d+[A-Za-z0-9\\-]+/videos/\\d+[A-Za-z0-9\\-]+");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = -1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    private final String         html_buy                     = "name=\"buy\"";
    private final String         html_logout                  = "(/\\?ac=dologout|/logout\")";
    private final String         default_extension            = ".mp4";
    private String               dllink                       = null;
    private boolean              premiumonly                  = false;
    private boolean              serverissues                 = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String urlSlug = new Regex(link.getPluginPatternMatcher(), "/videos/\\d+([A-Za-z0-9\\-]+)$").getMatch(0);
        String titleUrl = null;
        if (urlSlug != null) {
            titleUrl = urlSlug.replace("-", " ").trim();
        }
        if (!link.isNameSet()) {
            link.setName(titleUrl + default_extension);
        }
        dllink = null;
        premiumonly = false;
        serverissues = false;
        prepBR(this.br);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String username = this.br.getRegex("class=\"fa fa-calendar fa-fw fa-dt\"></i>.*?title=\"([^<>\"]+)\">([^<>\"]+)</a>").getMatch(0);
        // if (username == null) {
        // username = "amateur";
        // }
        String filename = br.getRegex("<h\\d+ class=\"page\\-title pull\\-left\">([^<>\"]+)</h\\d+>").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = titleUrl;
        }
        if (br.containsHTML(html_buy) || aa == null) {
            /* User has an account but he did not buy this video or user does not even have an account --> No way to download it */
            link.setName(filename + default_extension);
            premiumonly = true;
            return AvailableStatus.TRUE;
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            if (username != null) {
                filename = username + " - " + filename;
            }
            link.setFinalFileName(filename);
        }
        dllink = this.br.getRegex("data\\-(?:flv|mp4)=\"(https?://[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) {
            dllink = this.br.getRegex("\"(https?://[^<>\"\\']+\\.flv[^<>\"\\']+)\"").getMatch(0);
        }
        if (dllink != null) {
            /* Fix final downloadlink */
            dllink = dllink.replace("%252525", "%25");
            /* Get- and set filesize */
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    serverissues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @SuppressWarnings("deprecation")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                /* Re-use cookies whenever possible - avoid login captcha! */
                if (cookies != null) {
                    this.br.setCookies(cookies);
                    this.br.getPage("https://" + this.getHost());
                    if (this.br.containsHTML(html_logout)) {
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    }
                    /* Full login needed */
                    this.br = prepBR(new Browser());
                }
                br.getPage("http://www.mydirtyhobby.com/n/login");
                /*
                 * 2016-07-22: In browser it might happen too that the first login attempt will always fail with correct logindata - second
                 * attempt must not even require a captcha but will usually be successful!!
                 */
                boolean loginFailed = true;
                for (int totalCounter = 0; totalCounter <= 1; totalCounter++) {
                    /*
                     * In case we need a captcha it will only appear after the first login attempt so we need (max) 2 attempts to ensure
                     * that user can enter the captcha if needed.
                     */
                    String postdata = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                    if (this.br.containsHTML("class=\"g\\-recaptcha\"")) {
                        if (this.getDownloadLink() == null) {
                            // login wont contain downloadlink
                            this.setDownloadLink(new DownloadLink(this, "Account Login!", this.getHost(), this.getHost(), true));
                        }
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, this.br).getToken();
                        postdata += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                    }
                    br.postPage("/n/login", postdata);
                    if (!this.br.containsHTML(html_logout) || this.br.containsHTML("class=\"g\\-recaptcha\"")) {
                        continue;
                    } else {
                        loginFailed = false;
                        break;
                    }
                }
                if (loginFailed) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        return br;
    }

    /** There are no free- or premium accounts. Users can only watch the videos they bought. */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (premiumonly) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (serverissues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
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

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* Without account its not possible to download any link for this host. */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}