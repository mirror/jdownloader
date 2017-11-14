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
import java.util.List;
import java.util.Map.Entry;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.translate._JDT;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "brazzers.com" }, urls = { "http://brazzersdecrypted\\.com/scenes/view/id/\\d+/|https?://ma\\.brazzers\\.com/download/\\d+/\\d+/mp4_\\d+_\\d+/|https?://brazzersdecrypted\\.photos\\.[a-z0-9]+\\.contentdef\\.com/\\d+/pics/img/\\d+\\.jpg\\?.+" })
public class BrazzersCom extends antiDDoSForHost {
    public BrazzersCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://enter.brazzers.com/signup/signup.php");
    }

    @Override
    public String getAGBLink() {
        return "http://brazzerssupport.com/terms-of-service/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private boolean              not_yet_released             = false;
    private final String         type_normal_moch             = "http://brazzersdecrypted\\.com/scenes/view/id/\\d+/";
    private final String         type_premium_video           = "https?://ma\\.brazzers\\.com/download/.+";
    private final String         type_premium_pic             = "https?://(?:brazzersdecrypted\\.)?photos\\.[a-z0-9]+\\.contentdef\\.com/\\d+/pics/img/\\d+\\.jpg\\?.+";
    public static final String   html_loggedin                = "id=\"my\\-account\"";
    private String               dllink                       = null;
    private boolean              server_issues                = false;

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
        if (link.getDownloadURL().matches(type_normal_moch)) {
            /* Make MOCH download possible --> We have to correct the downloadurl again! */
            final String fid = getFidMOCH(link);
            link.setUrlDownload(jd.plugins.decrypter.BrazzersCom.getVideoUrlFree(fid));
        } else if (link.getDownloadURL().matches(type_premium_pic)) {
            link.setUrlDownload(link.getDownloadURL().replaceAll("https?://brazzersdecrypted\\.photos\\.", "http://photos."));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        Account moch_account = null;
        final List<Account> moch_accounts = AccountController.getInstance().getMultiHostAccounts(this.getHost());
        if (moch_accounts != null) {
            for (final Account moch_account_temp : moch_accounts) {
                if (moch_account_temp.isValid() && moch_account_temp.isEnabled()) {
                    moch_account = moch_account_temp;
                    break;
                }
            }
        }
        final BrazzersConfigInterface cfg = PluginJsonConfig.get(BrazzersConfigInterface.class);
        final String decrypter_filename = link.getStringProperty("decrypter_filename", null);
        final boolean use_server_filenames = cfg.isUseServerFilenames();
        final String fid;
        if (link.getDownloadURL().matches(type_premium_video) || link.getDownloadURL().matches(type_premium_pic)) {
            fid = link.getStringProperty("fid", null);
            this.login(this.br, aa, false);
            dllink = link.getDownloadURL();
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    if (use_server_filenames || StringUtils.isEmpty(decrypter_filename)) {
                        link.setFinalFileName(getFileNameFromHeader(con));
                    } else {
                        link.setFinalFileName(decrypter_filename);
                    }
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    if (link.getDownloadURL().matches(type_premium_pic)) {
                        /* Refresh directurl */
                        final String number_formatted = link.getStringProperty("picnumber_formatted", null);
                        if (fid == null || number_formatted == null) {
                            /* User added url without decrypter --> Impossible to refresh this directurl! */
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        this.br.getPage(jd.plugins.decrypter.BrazzersCom.getPicUrl(fid));
                        if (jd.plugins.decrypter.BrazzersCom.isOffline(this.br)) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        final String pic_format_string = jd.plugins.decrypter.BrazzersCom.getPicFormatString(this.br);
                        if (pic_format_string == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        dllink = String.format(pic_format_string, number_formatted);
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
        } else {
            fid = getFidMOCH(link);
            getPage(jd.plugins.decrypter.BrazzersCom.getVideoUrlFree(fid));
            /* Offline will usually return 404 */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final boolean isTrailerDownloadable = jd.plugins.decrypter.BrazzersCom.isBrazzersTrailerAvailable(this.br);
            final String url_name = new Regex(link.getDownloadURL(), "/id/\\d+/([^/]+)").getMatch(0);
            String filename = br.getRegex("<h1[^>]*?itemprop=\"name\">([^<>\"]+)<span").getMatch(0);
            /* This way we have a better dupe-detection! */
            link.setLinkID(fid);
            /* Two fallbacks in case we do not get any filename via html code */
            if (inValidate(filename)) {
                filename = url_name;
            }
            if (inValidate(filename)) {
                /* Finally - fallback to fid because we found nothing better. */
                filename = fid;
            } else {
                /* Add fileid in front of the filename to make it look nicer - will usually be removed in the final filename. */
                filename = fid + "_" + filename;
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename = encodeUnicode(filename);
            /* Do NOT set final filename yet!! */
            link.setName(filename + ".mp4");
            if (moch_account != null || !isTrailerDownloadable) {
                /* MOCH account available OR trailer not available --> Try to display a realistic filesize */
                long filesize_final = 0;
                long filesize_max = -1;
                long filesize_temp = -1;
                final String[] filesizes = br.getRegex("\\[(\\d{1,5}(?:\\.\\d{1,2})? (?:GB|GiB|MB))\\]").getColumn(0);
                for (final String filesize_temp_str : filesizes) {
                    filesize_temp = SizeFormatter.getSize(filesize_temp_str);
                    if (filesize_temp > filesize_max) {
                        filesize_max = filesize_temp;
                    }
                }
                if (filesize_max > -1) {
                    if (aa != null) {
                        /* Original brazzers account available --> Set highest filesize found --> Best Quality possible */
                        filesize_final = filesize_max;
                    } else if (moch_account != null && moch_account.getHoster().contains("debriditalia")) {
                        /* Multihoster debriditalia usually returns a medium quality - about 1 / 4 the size of the best possible! */
                        filesize_final = (long) (filesize_max * 0.25);
                    } else if (moch_account != null && moch_account.getHoster().contains("premiumize")) {
                        /* Multihoster premiumize usually returns 720p quality (or less, if not possible). */
                        if (this.br.containsHTML("HD MP4 1080P") && filesizes.length == 5) {
                            final String filesize_720p_temp_str = filesizes[1];
                            filesize_final = SizeFormatter.getSize(filesize_720p_temp_str);
                        } else {
                            /* 1080p not available. This else is also used as a fallback! */
                            filesize_final = filesize_max;
                        }
                    } else {
                        filesize_final = filesize_max;
                    }
                    link.setProperty("not_yet_released", false);
                    not_yet_released = false;
                    link.setDownloadSize(filesize_final);
                } else {
                    /* No filesize available --> Content is (probably) not (yet) released/downloadable */
                    link.getLinkStatus().setStatusText("Content has not yet been released");
                    link.setProperty("not_yet_released", true);
                    not_yet_released = true;
                }
            } else {
                /* No account available but trailer available --> Download trailer in highest quality possible */
                final String json = this.br.getRegex("streams\\s*?:\\s*?(\\{.*?\\}),?\\s+").getMatch(0);
                if (json == null) {
                    return null;
                }
                final LinkedHashMap<String, Object> entries = jd.plugins.decrypter.BrazzersCom.getVideoMapHttpStreams(json);
                int bitrate_max = 0;
                int bitrate_temp = 0;
                final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Object> entry = it.next();
                    final String quality_key = entry.getKey();
                    String quality_url = (String) entry.getValue();
                    if (quality_url != null && quality_url.startsWith("//")) {
                        /* Fix bad URLs */
                        quality_url = "http:" + quality_url;
                    }
                    if (quality_url == null || !quality_url.startsWith("http")) {
                        /* Skip invalid items */
                        continue;
                    }
                    if (quality_key.matches("mp4_\\d+_\\d+")) {
                        bitrate_temp = Integer.parseInt(quality_key.replaceAll("mp4|_", ""));
                        if (bitrate_temp > bitrate_max) {
                            bitrate_max = bitrate_temp;
                            dllink = quality_url;
                        }
                    }
                }
                if (dllink != null) {
                    URLConnectionAdapter con = null;
                    try {
                        con = br.openHeadConnection(dllink);
                        if (!con.getContentType().contains("html")) {
                            link.setDownloadSize(con.getLongContentLength());
                        } else {
                            server_issues = true;
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (final Throwable e) {
                        }
                    }
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

    private void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        handleGeneralErrors();
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            /*
             * If trailer download is possible but dllink == null in theory this would be a PLUGIN_DEFECT but I think that premiumonly
             * message is more suitable here as a trailer is usually not what you'd want to download.
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        resumable = ACCOUNT_PREMIUM_RESUME;
        maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        if (downloadLink.getDownloadURL().matches(type_premium_pic)) {
            /* Not needed for pictures / avoid connection issues. */
            resumable = false;
            maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("free_directlink", dllink);
        dl.startDownload();
    }

    private void handleGeneralErrors() throws PluginException {
        if (not_yet_released) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Content has not (yet) been released", 1 * 60 * 60 * 1000l);
        }
    }

    private String getFidMOCH(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "^.+/(\\d+)/?").getMatch(0);
    }

    private boolean isMOCHUrlOnly(final DownloadLink dl) {
        return isVideoURL(dl);
    }

    private boolean isVideoURL(final DownloadLink dl) {
        final String url = dl.getPluginPatternMatcher();
        return url != null && (url.matches(type_normal_moch) || url.matches(jd.plugins.decrypter.BrazzersCom.type_video_free));
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        /*
         * Usually an account is needed for this host but in case content has not yet been released the plugin should jump into download
         * mode to display this errormessage to the user!
         */
        return account != null || isVideoURL(downloadLink) || contentHasNotYetBeenReleased(downloadLink);
    }

    private boolean contentHasNotYetBeenReleased(final DownloadLink dl) {
        return dl.getBooleanProperty("not_yet_released", false);
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        final boolean is_this_plugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original brazzers plugin is always allowed to download. */
            return true;
        } else if (!downloadLink.isEnabled() && "".equals(downloadLink.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            /* Multihosts should not be tried if we know that content is not yet downloadable! */
            return !contentHasNotYetBeenReleased(downloadLink) && isMOCHUrlOnly(downloadLink);
        }
    }

    private static Object LOCK = new Object();

    public void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                pornportalPrepBR(br, "ma.brazzers.com");
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Try to avoid login captcha at all cost! Important: ALWAYS check this as their cookies can easily become invalid e.g.
                     * when the user logs in via browser.
                     */
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://ma." + account.getHoster() + "/home/");
                    if (br.containsHTML(html_loggedin)) {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                    br = pornportalPrepBR(new Browser(), "ma.brazzers.com");
                }
                br.getPage("https://ma." + account.getHoster() + "/access/login/");
                final DownloadLink dlinkbefore = this.getDownloadLink();
                String postdata = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                if (br.containsHTML("div class=\"g-recaptcha\"")) {
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    postdata += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }
                br.postPage("/access/submit/", postdata);
                final Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /* Redirect from probiller.com to main website --> Login complete */
                    br.submitForm(continueform);
                }
                if (br.getCookie(account.getHoster(), "login_usr") == null || !br.containsHTML(html_loggedin) || br.getURL().contains("/banned")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
                if (dlinkbefore != null) {
                    this.setDownloadLink(dlinkbefore);
                }
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
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
        /*
         * 2016-09-28: No way to verify premium status and/or expire date - I guess if an account works, it always has a subscription
         * (premium status) ...
         */
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (account.getType() == AccountType.FREE) {
            /* This should never happen! */
            doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            handleGeneralErrors();
            if (isMOCHUrlOnly(link)) {
                /*
                 * Only downloadable via multihoster - if a user owns a premiumaccount for this service he will usually never add such URLs!
                 */
                logger.info("This url is only downloadable via MOCH account");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            if (server_issues) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } else if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean resume = ACCOUNT_PREMIUM_RESUME;
            int maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
            if (link.getDownloadURL().matches(type_premium_pic)) {
                /* Not needed for pictures / avoid connection issues. */
                resume = false;
                maxchunks = 1;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (buildForThisPlugin != null && !StringUtils.equals(this.getHost(), buildForThisPlugin.getHost()) && isMOCHUrlOnly(downloadLink)) {
            return jd.plugins.decrypter.BrazzersCom.getVideoUrlFree(getFidMOCH(downloadLink));
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the brazzers.com plugin.";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return BrazzersConfigInterface.class;
    }

    public static interface BrazzersConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getUseServerFilenames_label() {
                return "Use original server filenames? If disabled, plugin-filenames will be used.";
            }

            public String getGrabBESTEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality();
            }

            public String getGrabHTTPMp4_1080pEnabled_label() {
                return "Grab 1080p HD MP4 1080P (mp4)?";
            }

            public String getGrabHTTPMp4_720pHDEnabled_label() {
                return "Grab 720p HD MP4 720P (mp4)?";
            }

            public String getGrabHTTPMp4_480pSDEnabled_label() {
                return "Grab 480p SD MP4 (mp4)?";
            }

            public String getGrabHTTPMp4_480pMPEG4Enabled_label() {
                return "Grab 480p MPEG4 (mp4)?";
            }

            public String getGrabHTTPMp4_270piPHONEMOBILEEnabled_label() {
                return "Grab 270p IPHONE/MOBILE (mp4)?";
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isUseServerFilenames();

        void setUseServerFilenames(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrabHTTPMp4_1080pEnabled();

        void setGrabHTTPMp4_1080pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrabHTTPMp4_720pHDEnabled();

        void setGrabHTTPMp4_720pHDEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(110)
        boolean isGrabHTTPMp4_480pSDEnabled();

        void setGrabHTTPMp4_480pSDEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(120)
        boolean isGrabHTTPMp4_480pMPEG4Enabled();

        void setGrabHTTPMp4_480pMPEG4Enabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(130)
        boolean isGrabHTTPMp4_270piPHONEMOBILEEnabled();

        void setGrabHTTPMp4_270piPHONEMOBILEEnabled(boolean b);
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