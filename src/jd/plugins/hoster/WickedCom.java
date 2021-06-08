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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wicked.com" }, urls = { "https://wickeddecrypted.+" })
public class WickedCom extends PluginForHost {
    public WickedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://join.wicked.com/signup/signup.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://ma.wicked.com/docs/terms/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final String         type_pic                     = ".+\\.jpg.*?";
    public static final String   html_loggedin                = "class=\"account\\-info\"";
    private String               dllink                       = null;
    private boolean              server_issues                = false;
    private boolean              logged_in                    = false;

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return pornportalPrepBR(br, "ma.wicked.com");
    }

    public static Browser pornportalPrepBR(final Browser br, final String host) {
        br.setFollowRedirects(true);
        pornportalPrepCookies(br, host);
        return br;
    }

    public static Browser pornportalPrepCookies(final Browser br, final String host) {
        /* Skips redirect to stupid advertising page after login. */
        br.setCookie(host, "skipPostLogin", "1");
        return br;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("https://wickeddecrypted", "https://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null && !link.getDownloadURL().matches(type_pic)) {
            link.getLinkStatus().setStatusText("Cannot check links without valid premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        if (aa != null) {
            this.login(aa, false);
            logged_in = true;
        } else {
            logged_in = false;
        }
        dllink = link.getDownloadURL();
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!this.looksLikeDownloadableContent(con)) {
                /* Refresh directurl */
                refreshDirecturl(link);
                con = br.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    logger.info("Fresh directurl has failed too...");
                    server_issues = true;
                    return AvailableStatus.TRUE;
                }
                /* If user copies url he should always get a valid one too :) */
                link.setContentUrl(dllink);
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
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
        final String fid = getFID(link);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getDownloadURL().matches(type_pic)) {
            if (logged_in) {
                this.br.getPage(jd.plugins.decrypter.WickedCom.getPicUrl(fid));
            } else {
                this.br.getPage(jd.plugins.decrypter.WickedCom.getVideoUrlFree(fid));
            }
            final String number_formatted = link.getStringProperty("picnumber_formatted", null);
            if (fid == null || number_formatted == null) {
                /* User added url without decrypter --> Impossible to refresh this directurl! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (jd.plugins.decrypter.WickedCom.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* TODO */
            return;
            // final String pictures[] = jd.plugins.decrypter.WickedCom.getPictureArray(this.br);
            // for (final String finallink : pictures) {
            // if (finallink.contains(number_formatted + ".jpg")) {
            // dllink = finallink;
            // break;
            // }
            // }
        } else {
            this.br.getPage(jd.plugins.decrypter.WickedCom.getVideoUrlPremium(fid));
            final String quality = link.getStringProperty("quality", null);
            if (quality == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* We don't need the exact json source for that! */
            dllink = PluginJSonUtils.getJsonValue(this.br, quality);
        }
        if (dllink == null || !dllink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private String getFID(final DownloadLink dl) {
        return dl.getStringProperty("fid", null);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        /* 2016-11-03: Free Users can download image galleries (only). */
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Try to avoid login captcha at all cost! Important: ALWAYS check this as their cookies can easily become invalid e.g.
                     * when the user logs in via browser.
                     */
                    logger.info("Attempting cookie login...");
                    br.setCookies(account.getHoster(), cookies);
                    if (!force) {
                        logger.info("Trust cookies without checking");
                        return;
                    }
                    br.getPage("https://" + jd.plugins.decrypter.WickedCom.DOMAIN_PREFIX_PREMIUM + this.getHost() + "/");
                    /* Else redirect to mainpage */
                    if (br.getURL().contains(jd.plugins.decrypter.WickedCom.DOMAIN_PREFIX_PREMIUM + this.getHost())) {
                        logger.info("Cookie login successful");
                        return;
                    } else {
                        logger.info("Cookie login failed --> Performing full login");
                        br.clearAll();
                        prepBR(new Browser());
                    }
                }
                br.getPage("https://www." + this.getHost() + "/en/login");
                final Form loginform = br.getFormbyProperty("id", "loginForm");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("rememberme", "1");
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.remove("submit");
                loginform.put("back", "JTJG");
                if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform) || loginform.containsHTML("onRecaptchaSubmit")) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                br.submitForm(loginform);
                // final Form continueform = br.getFormbyKey("response");
                // if (continueform != null) {
                // /* Redirect from probiller.com to main website --> Login complete */
                // br.submitForm(continueform);
                // }
                if (!br.getURL().contains(jd.plugins.decrypter.WickedCom.DOMAIN_PREFIX_PREMIUM + this.getHost())) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium Account");
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

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        final boolean is_this_plugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original plugin is always allowed to download. */
            return true;
        } else {
            /* Multihosts should not be tried for picture-downloads! */
            return !downloadLink.getDownloadURL().matches(type_pic);
        }
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost buildForThisPlugin) {
        if (StringUtils.equals("premiumize.me", buildForThisPlugin.getHost())) {
            try {
                return jd.plugins.decrypter.WickedCom.getVideoUrlFree(getFID(link));
            } catch (final Throwable ignore) {
                return null;
            }
        } else {
            return super.buildExternalDownloadURL(link, buildForThisPlugin);
        }
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the wicked.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "1080p", "Grab 1080p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "720p", "Grab 720p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "576p", "Grab 576p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "432p", "Grab 432p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "288p", "Grab 288p?").setDefaultValue(true));
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