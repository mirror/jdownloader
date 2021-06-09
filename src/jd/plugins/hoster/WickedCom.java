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
import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wicked.com" }, urls = { "" })
public class WickedCom extends PluginForHost {
    public WickedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://join.wicked.com/signup/signup.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.wicked.com/de/terms";
    }

    /* Connection stuff */
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String               dllink                       = null;

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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        dllink = link.getPluginPatternMatcher();
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (this.looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                return AvailableStatus.TRUE;
            } else {
                logger.info("Cannot perform linkcheck as directurl needs refresh");
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.UNCHECKABLE;
    }

    private void refreshDirecturl(final DownloadLink link) throws Exception {
        logger.info("Refreshing final downloadurl");
        final PluginForDecrypt decrypter = getNewPluginForDecryptInstance(getHost());
        final CryptedLink param = new CryptedLink(link.getContainerUrl(), link);
        /* 2021-04-24: Handling has been changed so array should only contain the one element we need! */
        final ArrayList<DownloadLink> items = ((jd.plugins.decrypter.WickedCom) decrypter).crawl(param, true);
        DownloadLink target = null;
        for (final DownloadLink tmp : items) {
            if (StringUtils.equals(link.getLinkID(), tmp.getLinkID())) {
                target = tmp;
                break;
            }
        }
        if (target == null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Failed to refresh directurl - content offline?");
        } else {
            logger.info("Successfully refreshed final downloadurl");
            /* Store new directurl */
            link.setPluginPatternMatcher(target.getPluginPatternMatcher());
        }
    }

    private boolean isVideoHLS(final DownloadLink link) {
        return link.getPluginPatternMatcher().contains(".m3u8");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
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
        if (this.isVideoHLS(link)) {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                this.refreshDirecturl(link);
            }
            dl = new HLSDownloader(link, br, link.getPluginPatternMatcher());
            dl.startDownload();
        } else {
            /* Photo or http video */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                this.refreshDirecturl(link);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            }
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        final boolean is_this_plugin = link.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original plugin is always allowed to download. */
            return true;
        } else {
            /* Multihosts should not be tried! */
            return false;
        }
    }
    // @Override
    // public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost buildForThisPlugin) {
    // if (StringUtils.equals("premiumize.me", buildForThisPlugin.getHost())) {
    // try {
    // return jd.plugins.decrypter.WickedCom.getVideoUrlFree(getFID(link));
    // } catch (final Throwable ignore) {
    // return null;
    // }
    // } else {
    // return super.buildExternalDownloadURL(link, buildForThisPlugin);
    // }
    // }

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