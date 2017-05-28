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
import java.io.IOException;

import org.appwork.utils.StringUtils;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "digitalplayground.com" }, urls = { "http://digitalplaygrounddecrypted.+" })
public class DigitalplaygroundCom extends PluginForHost {

    public DigitalplaygroundCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://join.digitalplayground.com/signup/signup.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.dpincsupport.com/terms-of-service/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final String         type_premium_pic             = ".+\\.jpg.*?";
    private final String         type_premium_pic_archive     = ".+\\.zip.*?";
    public static final String   html_loggedin                = "class=\"member\\-nav\"";
    private String               dllink                       = null;
    private boolean              server_issues                = false;

    public static Browser prepBR(final Browser br) {
        return jd.plugins.hoster.BrazzersCom.pornportalPrepBR(br, jd.plugins.decrypter.WickedCom.DOMAIN_PREFIX_PREMIUM + jd.plugins.decrypter.WickedCom.DOMAIN_BASE);
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://digitalplaygrounddecrypted", "http://"));
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
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (con.getContentType().contains("html")) {
                /* Refresh directurl */
                refreshDirecturl(link);
                con = br.openHeadConnection(dllink);
                if (con.getContentType().contains("html")) {
                    server_issues = true;
                    return AvailableStatus.TRUE;
                }
                /* If user copies url he should always get a valid one too :) */
                link.setContentUrl(dllink);
            }
            link.setDownloadSize(con.getLongContentLength());
            link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private void refreshDirecturl(final DownloadLink link) throws PluginException, IOException {
        final String fid = link.getStringProperty("fid", null);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getDownloadURL().matches(type_premium_pic)) {
            final String number_formatted = link.getStringProperty("picnumber_formatted", null);
            if (fid == null || number_formatted == null) {
                /* User added url without decrypter --> Impossible to refresh this directurl! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.br.getPage(jd.plugins.decrypter.DigitalplaygroundCom.getPicUrl(fid));
            if (jd.plugins.decrypter.DigitalplaygroundCom.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String pictures[] = jd.plugins.decrypter.DigitalplaygroundCom.getPictureArray(this.br);
            for (final String finallink : pictures) {
                if (finallink.contains(number_formatted + ".jpg")) {
                    dllink = finallink;
                    break;
                }
            }
        } else if (link.getDownloadURL().matches(type_premium_pic_archive)) {
            this.br.getPage(jd.plugins.decrypter.DigitalplaygroundCom.getPicUrl(fid));
            if (jd.plugins.decrypter.DigitalplaygroundCom.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /*
             * There is only one .zip downloadurl so we can use a simple RegEx as there is not really any possibility for us to fail and
             * ppick the wrong url compared to the other linktypes.
             */
            dllink = jd.plugins.decrypter.DigitalplaygroundCom.getPicArchiveDownloadlink(this.br);
        } else {
            final String quality = link.getStringProperty("quality", null);
            if (quality == null) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.br.getPage(jd.plugins.decrypter.DigitalplaygroundCom.getVideoUrlPremium(fid));
            if (jd.plugins.decrypter.DigitalplaygroundCom.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* We don't need the exact json source for that but we have to make sure to grab the http source, not the rtmp source! */
            final String json_src_http = PluginJSonUtils.getJsonNested(this.br, "http");
            dllink = PluginJSonUtils.getJsonValue(json_src_http, quality);
        }
        if (dllink == null || !dllink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l) {
                        /* Trust cookies without verifying them. */
                        return;
                    }
                    br.getPage("http://" + jd.plugins.decrypter.DigitalplaygroundCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/");
                    if (br.containsHTML(html_loggedin)) {
                        logger.info("Cookie login successful");
                        return;
                    }
                    logger.info("Cookie login failed --> Performing full login");
                    br = prepBR(new Browser());
                }
                br.getPage("http://" + jd.plugins.decrypter.DigitalplaygroundCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/access/login/");
                String postdata = "rememberme=on&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                if (br.containsHTML("api\\.recaptcha\\.net|google\\.com/recaptcha/api/")) {
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.findID();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "http://" + jd.plugins.decrypter.DigitalplaygroundCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/", true);
                    final String code = getCaptchaCode("recaptcha", cf, dummyLink);
                    postdata += "&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(code);
                }
                br.postPage("http://" + jd.plugins.decrypter.DigitalplaygroundCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/access/submit/", postdata);
                final Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /* Redirect from probiller.com to main website --> Login complete */
                    br.submitForm(continueform);
                    /* Some errorhandling in case submitting the Form does not take us on their main page. */
                    if (br.getURL().contains("/postlogin")) {
                        br.getPage("/home/");
                    }
                }
                if (!br.containsHTML(html_loggedin)) {
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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        return account != null;
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        final boolean is_this_plugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original plugin is always allowed to download. */
            return true;
        } else if (!downloadLink.isEnabled() && "".equals(downloadLink.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            final String mainlink = getMainlink(downloadLink);
            /* Multihosts should only be tried if we have the correct url. */
            return jd.plugins.decrypter.DigitalplaygroundCom.isTrailerUrl(mainlink);
        }
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (!StringUtils.equals(this.getHost(), buildForThisPlugin.getHost())) {
            final String mainlink = getMainlink(downloadLink);
            final String extern_url = jd.plugins.decrypter.DigitalplaygroundCom.getVideoUrlFree(mainlink);
            return extern_url;
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    private String getMainlink(final DownloadLink dl) {
        String mainlink = dl.getStringProperty("mainlink", null);
        if (mainlink == null) {
            mainlink = dl.getDownloadURL();
        }
        return mainlink;
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the digitalplayground.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ENABLE_FAST_LINKCHECK", "Enable fast linkcheck?\r\nFilesize will not be shown until downloadstart or manual linkcheck!").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "AUTO_PICTURES", "Grab picture galleries automatically when adding movie urls?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "AUTO_MOVIES", "Grab movies automatically when grabbing picture urls?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), "PREFERRED_PICTURE_FILE_TYPE", new String[] { "single zip file", "One by one" }, "Download images via: ").setDefaultValue(0));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_1080p_6000", "Grab 1080p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_720p_4000", "Grab 720p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_480p_1500", "Grab 480p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_320p_500", "Grab 320p (mp4)?").setDefaultValue(true));
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