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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pinterest.com" }, urls = { "decryptedpinterest://(?:(?:www|[a-z]{2})\\.)?pinterest\\.(?:com|de|fr)/pin/[A-Za-z0-9\\-_]+/" })
public class PinterestCom extends PluginForHost {
    public PinterestCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pinterest.com/");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://about.pinterest.com/de/terms-service";
    }

    public void correctDownloadLink(final DownloadLink link) {
        /* Correct link - remove country related subdomains (e.g. 'es.pinterest.com'). */
        final String pin_id = getPinID(link.getPluginPatternMatcher());
        link.setContentUrl("https://www.pinterest.com/pin/" + pin_id + "/");
    }

    public static String getPinID(final String pin_url) {
        return new Regex(pin_url, "pin/([^/]+)/?$").getMatch(0);
    }

    public static final long   trust_cookie_age = 300000l;
    /* Site constants */
    public static final String x_app_version    = "6cedd5c";
    /* don't touch the following! */
    private String             dllink           = null;

    @SuppressWarnings({ "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String pin_id = getPinID(link.getContentUrl());
        /* Display ids for offline links */
        link.setName(pin_id);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        dllink = checkDirectLink(link, "free_directlink");
        LinkedHashMap<String, Object> pinMap = null;
        boolean loggedIN = false;
        if (dllink == null) {
            final String source_url = link.getStringProperty("source_url", null);
            final String boardid = link.getStringProperty("boardid", null);
            final String username = link.getStringProperty("username", null);
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa != null && source_url != null && boardid != null && username != null) {
                login(this.br, aa, false);
                loggedIN = true;
                pinMap = jd.plugins.decrypter.PinterestComDecrypter.findPINMap(this.br, true, link.getContentUrl(), source_url, boardid, username);
                /* We don't have to be logged in to perform downloads so better log out to avoid account bans. */
                br.clearCookies(br.getHost());
            } else {
                loggedIN = false;
                pinMap = jd.plugins.decrypter.PinterestComDecrypter.findPINMap(this.br, false, link.getContentUrl(), source_url, boardid, username);
            }
            dllink = jd.plugins.decrypter.PinterestComDecrypter.getDirectlinkFromPINMap(pinMap);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        jd.plugins.decrypter.PinterestComDecrypter.setInfoOnDownloadLink(this.br, link, pinMap, dllink, loggedIN);
        /* Check if our directlink is actually valid. */
        dllink = checkDirectLink(link, "free_directlink");
        if (dllink == null) {
            logger.info("Final downloadurl is not downloadable --> Offline?");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    /**
     * Sets Headers for Pinterest API usage. <br />
     */
    public static void prepAPIBR(final Browser br) throws PluginException {
        if (br.getRequest() == null) {
            try {
                /* Access mainpage to get that cookie which we'll need later. */
                br.getPage("https://www.pinterest.de/");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        final String csrftoken = br.getCookie(br.getHost(), "csrftoken");
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Pinterest-AppState", "active");
        br.getHeaders().put("X-NEW-APP", "1");
        br.getHeaders().put("X-APP-VERSION", x_app_version);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br.getHeaders().put("X-CSRFToken", csrftoken);
    }

    public static boolean isOffline(final Browser br, final String pin_id) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (!br.getURL().contains(pin_id)) {
            /* Pin redirected to other pin --> initial pin is offline! */
            return true;
        }
        return false;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, false, 1, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    /** Checks directurl and sets filesize on success. */
    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                } else {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    /**
     * Returns internal linkid, uses pin_id only if it fails to find something better. pin_ids can vary for usage via account vs. non
     * account which means one and the same PIN object can have 2 different IDs e.g. <br />
     * 561824122247791853 == AWgdN4e_KINL2m6FsLk-aGJiMvq_NZ7BIW0pc5rDZqb1BcwCpPxGRAE <br />
     * Ideal directlink for linkid: https://s-media-cache-ak0.pinimg.com/originals/aa/f2/04/aaf204b422904144d1bb0fd763866d05.jpg <br />
     * Non-ideal directlink for linkid:
     * https://s-media-cache-ak0.pinimg.com/736x/aa/f2/04/aaf204b422904144d1bb0fd763866d05--sacred-garden-garden-statues.jpg <br />
     */
    public static String getLinkidForInternalDuplicateCheck(final String pin_url, final String directlink) {
        if (pin_url == null && directlink == null) {
            return null;
        }
        String internal_linkid = directlink != null ? new Regex(directlink, "https?://[^/]+/[^/]+/(.+)").getMatch(0) : null;
        if (internal_linkid == null) {
            /* Fallback */
            internal_linkid = getPinID(pin_url);
        }
        return internal_linkid;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static Object LOCK = new Object();

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setAllowedResponseCodes(new int[] { 401 });
                String last_used_host = account.getStringProperty("host");
                if (last_used_host == null) {
                    /* Fallback */
                    last_used_host = "pinterest.com";
                }
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(last_used_host, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    return;
                }
                br.setFollowRedirects(true);
                /* May redirect to e.g. pinterest.de */
                br.getPage("https://www.pinterest.com/login/?action=login");
                last_used_host = br.getHost();
                prepAPIBR(br);
                String postData = "source_url=/login/&data={\"options\":{\"username_or_email\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\"},\"context\":{}}&module_path=App()>LoginPage()>Login()>Button(class_name=primary,+text=Anmelden,+type=submit,+size=large)";
                // postData = Encoding.urlEncode(postData);
                final String urlpart = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0);
                br.postPageRaw(urlpart + "/resource/UserSessionResource/create/", postData);
                if (br.getHttpConnection().getResponseCode() == 401 || br.containsHTML("jax CsrfErrorPage Module") || br.getCookie(last_used_host, "_b") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(last_used_host), "");
                account.setProperty("host", last_used_host);
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
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(false);
        ai.setStatus("Free Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* We already logged in in requestFileInformation */
        br.setFollowRedirects(false);
        doFree(link, false, 1, "account_free_directlink");
    }

    public static String getPictureDescription(final DownloadLink dl) {
        return dl.getStringProperty("description", null);
    }

    @Override
    public String getDescription() {
        return "JDownloader's Pinterest plugin helps downloading pictures from pinterest.com.";
    }

    public static final String  ENABLE_DESCRIPTION_IN_FILENAMES                      = "ENABLE_DESCRIPTION_IN_FILENAMES";
    public static final boolean defaultENABLE_DESCRIPTION_IN_FILENAMES               = false;
    public static final String  ENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS        = "ENABLE_CRAWL_ALTERNATIVE_URLS_INSIDE_COMMENTS";
    public static final boolean defaultENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_DESCRIPTION_IN_FILENAMES, "Add pin-description to filenames?\r\nNOTE: If enabled, Filenames might get very long!").setDefaultValue(defaultENABLE_DESCRIPTION_IN_FILENAMES));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS, "Crawl alternative source URLs e.g. (higher quality) imgur.com URLs?").setDefaultValue(defaultENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}