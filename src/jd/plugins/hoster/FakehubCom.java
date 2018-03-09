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

import java.io.File;

import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fakehub.com" }, urls = { "https?://ma\\.fakehub\\.com/download/\\d+/[A-Za-z0-9\\-_]+/|http://fakehubdecrypted.+" })
public class FakehubCom extends PluginForHost {
    public FakehubCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://join.fakehub.com/signup/signup.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.supportmg.com/terms-of-service";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final String         type_premium_pic             = ".+\\.jpg.*?";
    public static final String   html_loggedin                = "/member/profile/";
    private String               dllink                       = null;
    private boolean              server_issues                = false;

    public static Browser prepBR(final Browser br) {
        return jd.plugins.hoster.BrazzersCom.pornportalPrepBR(br, jd.plugins.decrypter.FakehubCom.DOMAIN_PREFIX_PREMIUM + jd.plugins.decrypter.FakehubCom.DOMAIN_BASE);
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://fakehubdecrypted", "http://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Cannot check links without valid premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        this.login(this.br, aa, false);
        dllink = link.getDownloadURL();
        final String fid = link.getStringProperty("fid", null);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            } else {
                if (link.getDownloadURL().matches(type_premium_pic)) {
                    /* Refresh directurl */
                    final String number_formatted = link.getStringProperty("picnumber_formatted", null);
                    if (fid == null || number_formatted == null) {
                        /* User added url without decrypter --> Impossible to refresh this directurl! */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    this.br.getPage(jd.plugins.decrypter.FakehubCom.getPicUrl(fid));
                    if (jd.plugins.decrypter.FakehubCom.isOffline(this.br)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final String pictures[] = jd.plugins.decrypter.FakehubCom.getPictureArray(this.br);
                    for (final String finallink : pictures) {
                        if (finallink.contains(number_formatted + ".jpg")) {
                            dllink = finallink;
                            break;
                        }
                    }
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* ... new URL should work! */
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        /* Set new url */
                        link.setUrlDownload(dllink);
                        /* If user copies url he should always get a valid one too :) */
                        link.setContentUrl(dllink);
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        server_issues = true;
                    }
                } else {
                    server_issues = true;
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    public void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Try to avoid login captcha at all cost! Important: ALWAYS check this as their cookies can easily become invalid e.g.
                     * when the user logs in via browser.
                     */
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage(jd.plugins.decrypter.FakehubCom.getProtocol() + jd.plugins.decrypter.FakehubCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/");
                    if (br.containsHTML(html_loggedin)) {
                        logger.info("Cookie login successful");
                        return;
                    }
                    logger.info("Cookie login failed --> Performing full login");
                    br = prepBR(new Browser());
                }
                br.getPage(jd.plugins.decrypter.FakehubCom.getProtocol() + jd.plugins.decrypter.FakehubCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/access/login/");
                String postdata = "rememberme=on&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                if (br.containsHTML("api\\.recaptcha\\.net|google\\.com/recaptcha/api/")) {
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.findID();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), jd.plugins.decrypter.FakehubCom.getProtocol() + jd.plugins.decrypter.FakehubCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/", true);
                    final String code = getCaptchaCode("recaptcha", cf, dummyLink);
                    postdata += "&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(code);
                }
                br.postPage(jd.plugins.decrypter.FakehubCom.getProtocol() + jd.plugins.decrypter.FakehubCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/access/submit/", postdata);
                final Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /* Redirect from probiller.com to main website --> Login complete */
                    br.submitForm(continueform);
                }
                if (br.getCookie(jd.plugins.decrypter.FakehubCom.getProtocol() + jd.plugins.decrypter.FakehubCom.DOMAIN_PREFIX_PREMIUM + account.getHoster(), "loginremember") == null || !br.containsHTML(html_loggedin)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername,Passwort und/oder login Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password/login captcha!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        try {
            /* 2018-03-09: Expiredate might not always be available */
            br.getPage("/member/profile/");
            final String days_remaining = br.getRegex("class=\"membership\\-details\\-data\">\\s*?(\\d+) days\\s*?</span>").getMatch(0);
            if (days_remaining != null) {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(days_remaining) * 24 * 60 * 60 * 1000, br);
            }
        } catch (final Throwable e) {
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the fakehub.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_1080p_6000", "Grab 1080p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_720p_2600", "Grab 720p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_480p_1500", "Grab 480p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_368p_850", "Grab 360p (mp4)?").setDefaultValue(true));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}