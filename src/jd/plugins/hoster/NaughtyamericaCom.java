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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "naughtyamerica.com" }, urls = { "http://naughtyamericadecrypted.+" })
public class NaughtyamericaCom extends PluginForHost {

    public NaughtyamericaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://natour.naughtyamerica.com/signup/signup.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.naughtyamerica.com/terms-of-service.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    /*
     * 2017-01-23: Max 100 connections tital seems to be a stable value - I'd not recommend allowing more as this will most likely cause
     * failing downloads which start over and over.
     */
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -5;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final String         type_pic                     = ".+\\.jpg.*?";
    public static final String   html_loggedin                = "class=\"my\\-settings\"";
    private String               dllink                       = null;
    private boolean              server_issues                = false;

    public static Browser prepBR(final Browser br) {
        br.addAllowedResponseCodes(456);
        return br;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://naughtyamericadecrypted", "https://"));
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
        login(br, aa, false);
        dllink = link.getDownloadURL();
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (con.getContentType().contains("html")) {
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
            if (link.getFinalFileName() == null) {
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private void refreshDirecturl(final DownloadLink link) throws Exception {
        final String filename_url = getFilenameUrl(link);
        if (filename_url == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getDownloadURL().matches(type_pic)) {
            final String number_formatted = link.getStringProperty("picnumber_formatted", null);
            if (filename_url == null || number_formatted == null) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(jd.plugins.decrypter.NaughtyamericaCom.getPicUrl(filename_url));
            if (jd.plugins.decrypter.NaughtyamericaCom.isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String pictures[] = jd.plugins.decrypter.NaughtyamericaCom.getPictureArray(br);
            for (final String finallink : pictures) {
                if (finallink.contains(number_formatted + ".jpg")) {
                    dllink = finallink;
                    break;
                }
            }
        } else {
            final String quality_stored = link.getStringProperty("quality", null);
            final String type_stored = link.getStringProperty("type", null);
            final String type_target = type_stored != null ? "clip" : null;
            if (quality_stored == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(jd.plugins.decrypter.NaughtyamericaCom.getVideoUrlPremium(filename_url));
            /* The complicated process of finding a new directurl ... */
            final String[] videoInfo = jd.plugins.decrypter.NaughtyamericaCom.getVideoInfoArray(br);
            for (final String singleVideoInfo : videoInfo) {
                final String directlink = jd.plugins.decrypter.NaughtyamericaCom.getDirecturlFromVideoInfo(singleVideoInfo);
                final String[] videroInfoArray = jd.plugins.decrypter.NaughtyamericaCom.getVideoInfoDetailed(singleVideoInfo);
                final String type = videroInfoArray[videroInfoArray.length - 2];
                final String quality = videroInfoArray[videroInfoArray.length - 1];
                final String quality_url = directlink != null ? new Regex(directlink, "([a-z0-9]+)\\.(?:wmv|mp4)").getMatch(0) : null;
                if (directlink == null || quality_url == null) {
                    continue;
                }
                final String type_for_property = jd.plugins.decrypter.NaughtyamericaCom.getTypeForProperty(type);
                if (type_target != null && "clip".equalsIgnoreCase(type_target) && !"clip".equalsIgnoreCase(type_for_property)) {
                    /* We're looking for a clip but this is not a clip --> Not what we want --> Skip it */
                    continue;
                } else if (!quality.equalsIgnoreCase(quality_stored)) {
                    /* Not the quality we're looking for --> Skip it */
                    continue;
                }
                dllink = directlink;
                break;
            }
        }
        if (dllink == null || !dllink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private String getFID(final DownloadLink dl) {
        return dl.getStringProperty("fid", null);
    }

    private String getFilenameUrl(final DownloadLink dl) {
        return dl.getStringProperty("filename_url", null);
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
                    br.getPage("https://" + jd.plugins.decrypter.NaughtyamericaCom.DOMAIN_PREFIX_PREMIUM + account.getHoster());
                    if (br.containsHTML(html_loggedin)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                    logger.info("Cookie login failed --> Performing full login");
                    br = prepBR(new Browser());
                }
                br.getPage("https://" + jd.plugins.decrypter.NaughtyamericaCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/login");
                Form loginform = br.getFormbyKey("username");
                if (loginform == null) {
                    loginform = br.getForm(0);
                }
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("dest", "");
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("g\\-recaptcha")) {
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                br.submitForm(loginform);
                final String loginCookie = br.getCookie("nrc", jd.plugins.decrypter.NaughtyamericaCom.DOMAIN_PREFIX_PREMIUM + account.getHoster());
                final Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /* Redirect from probiller.com to main website --> Login complete */
                    br.submitForm(continueform);
                }
                if (br.getURL().contains("/postLogin")) {
                    br.getPage("//" + jd.plugins.decrypter.NaughtyamericaCom.DOMAIN_PREFIX_PREMIUM + br.getHost());
                }
                if (br.getURL().contains("beta.") || br.getURL().contains("/login")) {
                    /* 2016-12-12: Redirects to their beta-page might happen --> Go back to the old/stable version of their webpage. */
                    br.getPage("https://" + jd.plugins.decrypter.NaughtyamericaCom.DOMAIN_PREFIX_PREMIUM + account.getHoster());
                }
                if (!br.containsHTML(html_loggedin) && loginCookie == null) {
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
            login(br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        final String user_type = br.getRegex("var\\s*?user_type\\s*?=\\s*?\\'([^<>\"\\']+)'\\s*?;").getMatch(0);
        final boolean isExpired = user_type != null && user_type.equalsIgnoreCase("Expired Paid");
        if (isExpired) {
            /* Small failover - login is correct but account is expired! */
            ai.setExpired(true);
            return ai;
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
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
            /* Multihosts should not be tried for picture-downloads! */
            return !downloadLink.getDownloadURL().matches(type_pic);
        }
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (StringUtils.equals("premiumize.me", buildForThisPlugin.getHost())) {
            return jd.plugins.decrypter.NaughtyamericaCom.getVideoUrlFree(getFilenameUrl(downloadLink));
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the naughtyamerica.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ENABLE_FAST_LINKCHECK", "Enable fast linkcheck?\r\nFilesize will not be shown until downloadstart or manual linkcheck!").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_CLIPS", "Grab clips (= full videos split in multiple parts/scenes)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_PICTURES", "Grab picture galleries?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_4K", "Grab 4K (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_HD 1080p", "Grab 1080p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_HD 720p", "Grab 720p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_HD 480p", "Grab 480p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_Quicktime (Apple)", "Grab Quicktime (Apple) (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_DVD", "Grab DVD (wmv)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_mobile", "Grab Mobile (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_Quicktime On the go (5 Min version)", "Grab Quicktime On the go (5 Min version) (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_Quicktime 4k 5min", "Grab Quicktime 4k 5min (mp4)?").setDefaultValue(true));
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