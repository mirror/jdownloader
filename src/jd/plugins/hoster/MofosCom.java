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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mofos.com" }, urls = { "https?://members2\\.mofos\\.com/download/\\d+/[A-Za-z0-9\\-_]+/|mofosdecrypted://.+" })
public class MofosCom extends PluginForHost {

    public MofosCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.mofos.com/tour/signup/");
    }

    @Override
    public String getAGBLink() {
        return "http://static.mofos.com/policy_files/ipp.php?site=mofos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = -1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    private final String         type_premium_pic             = ".+\\.jpg.*?";
    public static final String   html_loggedin                = "data\\-membership";
    private String               dllink                       = null;
    private boolean              server_issues                = false;
    private boolean              premiumonly                  = true;

    public static Browser prepBR(final Browser br) {
        return jd.plugins.hoster.BrazzersCom.pornportalPrepBR(br, "members2.mofos.com");
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("mofosdecrypted://", "http://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        link.setName(getFID(link));
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null && !jd.plugins.decrypter.MofosCom.isFreeVideoUrl(link.getDownloadURL())) {
            link.getLinkStatus().setStatusText("Cannot check links without valid premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        if (aa != null) {
            this.login(this.br, aa, false);
            dllink = link.getDownloadURL();
        } else {
            /* Trailer download */
            this.br.getPage(this.getMainlink(link));
            if (jd.plugins.decrypter.MofosCom.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.containsHTML("class=\"restricted\\-access\" style=\"display:block;\"")) {
                logger.info("Trailer is not available or only available for premium users");
                return AvailableStatus.TRUE;
            }
            final String json_source = this.br.getRegex("JSON\\.parse\\(\\'(.*?)\'\\)").getMatch(0);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            entries = jd.plugins.decrypter.BrazzersCom.getVideoMapHttpStreams(entries);
            /* Find highest quality */
            int bitrate_max = 0;
            int bitrate_temp = 0;
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> ipentry = it.next();
                final String quality_key = ipentry.getKey();
                final String url_temp = (String) ipentry.getValue();
                bitrate_temp = Integer.parseInt(new Regex(quality_key, "(\\d+)$").getMatch(0));
                if (bitrate_temp > bitrate_max) {
                    bitrate_max = bitrate_temp;
                    dllink = url_temp;
                }
            }
            premiumonly = false;
        }
        final String fid = link.getStringProperty("fid", null);
        if (!StringUtils.isEmpty(dllink)) {
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
                        this.br.getPage(jd.plugins.decrypter.MofosCom.getPicUrl(fid));
                        if (jd.plugins.decrypter.MofosCom.isOffline(this.br)) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        final String pictures[] = jd.plugins.decrypter.MofosCom.getPictureArray(this.br);
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
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (!jd.plugins.decrypter.MofosCom.isFreeVideoUrl(this.getMainlink(downloadLink)) || premiumonly) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("free_directlink", dllink);
        dl.startDownload();
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
                    br.getPage("http://members2." + account.getHoster() + "/");
                    if (br.containsHTML(html_loggedin)) {
                        logger.info("Cookie login successful");
                        return;
                    }
                    logger.info("Cookie login failed --> Performing full login");
                    br = prepBR(new Browser());
                }
                br.getPage("http://members2." + account.getHoster() + "/access/login/");
                String postdata = "rememberme=on&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                final DownloadLink dlinkbefore = this.getDownloadLink();
                if (br.containsHTML("div class=\"g-recaptcha\"")) {
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    postdata += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                }
                br.postPage("/access/submit/", postdata);
                final Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /* Redirect from probiller.com to main website --> Login complete */
                    br.submitForm(continueform);
                }
                if (br.getCookie("http://members2." + account.getHoster(), "loginremember") == null || !br.containsHTML(html_loggedin)) {
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

    private String getFID(final DownloadLink dl) {
        final String mainlink = getMainlink(dl);
        final String fid;
        if (jd.plugins.decrypter.MofosCom.isFreeVideoUrl(mainlink)) {
            fid = new Regex(mainlink, "(\\d+)/?$").getMatch(0);
        } else {
            fid = new Regex(mainlink, "download/(\\d+/[A-Za-z0-9\\-_]+)/").getMatch(0);
        }
        return fid;
    }

    private String getMainlink(final DownloadLink dl) {
        return dl.getStringProperty("mainlink", null);
    }

    @Override
    public String getDescription() {
        return "Lade Video- und Audioinhalte aus der ZDFMediathek herunter";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return MofosConfigInterface.class;
    }

    public static interface MofosConfigInterface extends PluginConfigInterface {

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(21)
        boolean isAddUnknownQualitiesEnabled();

        void setAddUnknownQualitiesEnabled(boolean b);

        /* Download-only */
        @DefaultBooleanValue(true)
        @Order(30)
        boolean isGrab1080_12000Enabled();

        void setGrab1080_12000Enabled(boolean b);

        /* Download-only */
        @DefaultBooleanValue(true)
        @Order(30)
        boolean isGrab720_8000Enabled();

        void setGrab720_8000Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(30)
        boolean isGrab720_3800Enabled();

        void setGrab720_3800Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(40)
        boolean isGrab720_2600Enabled();

        void setGrab720_2600Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(45)
        boolean isGrab720_2000Enabled();

        void setGrab720_2000Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(50)
        boolean isGrab480_2000Enabled();

        void setGrab480_2000Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(60)
        boolean isGrab480_1500Enabled();

        void setGrab480_1500Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(70)
        boolean isGrab480_1000Enabled();

        void setGrab480_1000Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(80)
        boolean isGrab272_650Enabled();

        void setGrab272_650Enabled(boolean b);
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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        return account != null || jd.plugins.decrypter.MofosCom.isFreeVideoUrl(getMainlink(downloadLink));
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
            /* Multihosts can only download 'trailer' URLs */
            final String url = downloadLink.getPluginPatternMatcher();
            return jd.plugins.decrypter.MofosCom.isFreeVideoUrl(getMainlink(downloadLink));
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}