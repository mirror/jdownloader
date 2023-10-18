//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.LinkedHashMap;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;

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
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.SpankBangComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { SpankBangComCrawler.class })
public class SpankBangCom extends PluginForHost {
    public SpankBangCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
        this.enablePremium("https://www.spankbang.com/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    public static List<String[]> getPluginDomains() {
        return SpankBangComCrawler.getPluginDomains();
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
        return new String[] { "http://spankbangdecrypted\\.com/\\d+" };
    }

    @Override
    public String getAGBLink() {
        return "http://spankbang.com/info#dmca";
    }

    /** Settings stuff */
    public final static String  FASTLINKCHECK           = "FASTLINKCHECK";
    public final static String  ALLOW_BEST              = "ALLOW_BEST";
    public final static String  ALLOW_240p              = "ALLOW_240p";
    public final static String  ALLOW_320p              = "ALLOW_320p";
    public final static String  ALLOW_480p              = "ALLOW_480p";
    public final static String  ALLOW_720p              = "ALLOW_720p";
    public final static String  ALLOW_1080p             = "ALLOW_1080p";
    public static final String  ALLOW_4k                = "ALLOW_4k";
    public static final String  ALLOW_THUMBNAIL         = "ALLOW_thumbnail";
    public static final boolean default_FASTLINKCHECK   = true;
    public static final boolean default_ALLOW_BEST      = true;
    public static final boolean default_ALLOW_240p      = true;
    public static final boolean default_ALLOW_320p      = true;
    public static final boolean default_ALLOW_480p      = true;
    public static final boolean default_ALLOW_720p      = true;
    public static final boolean default_ALLOW_1080p     = true;
    public static final boolean default_ALLOW_4k        = true;
    public static final boolean default_ALLOW_THUMBNAIL = true;
    private String              dllink                  = null;
    private boolean             server_issues           = false;
    public static final String  PROPERTY_TITLE          = "title";
    public static final String  PROPERTY_UPLOADER       = "uploader";
    public static final String  PROPERTY_QUALITY        = "quality";
    public static final String  PROPERTY_DIRECTLINK     = "plain_directlink";
    public static final String  PROPERTY_MAINLINK       = "mainlink";

    @Override
    public void init() {
        super.init();
        /** 2021-07-27: Important else we'll run into Cloudflare Rate-Limit prohibition after about 250 requests! */
        Browser.setRequestIntervalLimitGlobal(getHost(), 3000);
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        if (link != null && StringUtils.equals(getHost(), link.getHost())) {
            return link.getLinkID();
        } else {
            return super.getMirrorID(link);
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        SpankBangComCrawler.prepBR(br);
        if (account != null) {
            this.login(account, false);
        }
        server_issues = false;
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        setFilename(link);
        dllink = link.getStringProperty(PROPERTY_DIRECTLINK);
        if (isValidURL(br, link, dllink)) {
            return AvailableStatus.TRUE;
        } else {
            final String mainlink = link.getStringProperty(PROPERTY_MAINLINK);
            final String quality = link.getStringProperty(PROPERTY_QUALITY);
            if (mainlink == null || quality == null) {
                /* Missing property - this should not happen! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(mainlink);
            if (SpankBangComCrawler.isOffline(this.br)) {
                /* Main videolink offline --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            setFilename(link);
            /* Main videolink online --> Refresh directlink ... */
            final LinkedHashMap<String, String> foundQualities = SpankBangComCrawler.findQualities(this.br, mainlink);
            if (foundQualities != null) {
                dllink = foundQualities.get(quality);
            }
            if (dllink != null) {
                if (isValidURL(br, link, dllink)) {
                    link.setProperty(PROPERTY_DIRECTLINK, dllink);
                    return AvailableStatus.TRUE;
                } else {
                    /* Link is still online but our directlink does not work for whatever reason ... */
                    server_issues = true;
                }
            }
        }
        return AvailableStatus.UNCHECKED;
    }

    public static void setFilename(final DownloadLink link) {
        String title = link.getStringProperty(PROPERTY_TITLE);
        if (title == null) {
            /* Handling for items added in revision 46976 and before */
            final String legacyStaticFilename = link.getStringProperty("plain_filename");
            if (legacyStaticFilename != null && legacyStaticFilename.contains(".")) {
                /* Remove file-extension */
                title = legacyStaticFilename.substring(0, legacyStaticFilename.lastIndexOf("."));
            }
        }
        final String uploader = link.getStringProperty(PROPERTY_UPLOADER);
        final String quality = link.getStringProperty(PROPERTY_QUALITY);
        if (title != null && uploader != null) {
            link.setFinalFileName(uploader + " - " + title + "_" + quality + ".mp4");
        } else {
            link.setFinalFileName(title + "_" + quality + ".mp4");
        }
    }

    private boolean isValidURL(final Browser br, final DownloadLink link, final String url) throws IOException {
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        // this request isn't behind cloudflare.
        final URLConnectionAdapter con = brc.openHeadConnection(url);
        try {
            if (url.contains("m3u8") && con.getResponseCode() == 200) {
                return true;
            } else if (!url.contains("m3u8") && looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                return true;
            } else {
                return false;
            }
        } finally {
            con.disconnect();
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (cookies != null || userCookies != null) {
                    logger.info("Attempting cookie login");
                    if (userCookies != null) {
                        this.br.setCookies(this.getHost(), userCookies);
                    } else {
                        this.br.setCookies(this.getHost(), cookies);
                    }
                    if (!force) {
                        /* Don't validate cookies */
                        return false;
                    }
                    br.getPage("https://www." + this.getHost() + "/");
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp -> Only if username + password was used to login. */
                        if (userCookies == null) {
                            account.saveCookies(this.br.getCookies(this.getHost()), "");
                        } else {
                            /*
                             * Try to set unique username even if user used cookie login (technically user could enter whatever he wants
                             * when cookie login is used).
                             */
                            final String usernameFromHTML = br.getRegex("class=\"user\"[^>]*><a href=\"/profile/([^/\"]+)").getMatch(0);
                            if (usernameFromHTML != null) {
                                account.setUser(Encoding.htmlDecode(usernameFromHTML).trim());
                            } else {
                                logger.warning("Failed to find username in HTML");
                            }
                        }
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                        if (userCookies != null) {
                            if (account.hasEverBeenValid()) {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                            } else {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                            }
                        } else {
                            br.clearCookies(null);
                        }
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/users/auth?ajax=1&login=1&_=" + System.currentTimeMillis());
                final Form loginform = br.getFormbyProperty("id", "auth_login_form");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("l_username", Encoding.urlEncode(account.getUser()));
                loginform.put("l_password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (StringUtils.equalsIgnoreCase(br.getRequest().getHtmlCode(), "OK")) {
                    logger.info("Looks like login was successful");
                    br.getPage("/");
                } else {
                    logger.info("Looks like login failed");
                }
                if (!isLoggedin(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/users/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public String getDescription() {
        return "JDownloader's SpankBang Plugin helps downloading Videoclips from spankbang.com. SpankBang provides different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, "Fast linkcheck (filesize won't be shown in linkgrabber)?").setDefaultValue(default_FASTLINKCHECK));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, "Always only grab best available resolution?").setDefaultValue(default_ALLOW_BEST);
        getConfig().addEntry(cfg);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_240p, "Grab 240p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_320p, "Grab 320p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480p, "Grab 480p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720p, "Grab 720p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080p, "Grab 1080p?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_4k, "Grab 4k?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_THUMBNAIL, "Grab thumbnail?").setDefaultValue(default_ALLOW_THUMBNAIL));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}