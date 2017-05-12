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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "instagram.com" }, urls = { "instagrammdecrypted://[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)?" })
public class InstaGramCom extends PluginForHost {
    @SuppressWarnings("deprecation")
    public InstaGramCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium(MAINPAGE + "/accounts/login/");
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/about/legal/terms/#";
    }

    /* Connection stuff */
    private static final boolean RESUME                            = true;
    /* Chunkload makes no sense for pictures/small files */
    private static final int     MAXCHUNKS_pictures                = 1;
    private static final int     MAXCHUNKS_videos                  = 0;
    private static final int     MAXDOWNLOADS                      = -1;
    private static final String  MAINPAGE                          = "https://www.instagram.com";
    public static final String   QUIT_ON_RATE_LIMIT_REACHED        = "QUIT_ON_RATE_LIMIT_REACHED";
    public static final String   PREFER_SERVER_FILENAMES           = "PREFER_SERVER_FILENAMES";
    public static final String   ONLY_GRAB_X_ITEMS                 = "ONLY_GRAB_X_ITEMS";
    public static final String   ONLY_GRAB_X_ITEMS_NUMBER          = "ONLY_GRAB_X_ITEMS_NUMBER";
    public static final boolean  defaultPREFER_SERVER_FILENAMES    = false;
    public static final boolean  defaultQUIT_ON_RATE_LIMIT_REACHED = false;
    public static final boolean  defaultONLY_GRAB_X_ITEMS          = false;
    public static final int      defaultONLY_GRAB_X_ITEMS_NUMBER   = 25;
    private static Object        LOCK                              = new Object();
    private boolean              is_private_url                    = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        is_private_url = downloadLink.getBooleanProperty("private_url", false);
        this.setBrowserExclusive();
        /*
         * Decrypter can set this status - basically to be able to handle private urls correctly in host plugin in case users' account gets
         * disabled for whatever reason.
         */
        prepBR(this.br);
        boolean is_logged_in = false;
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(this.br, aa, false);
                is_logged_in = true;
            } catch (final Throwable e) {
            }
        }
        if (this.is_private_url && !is_logged_in) {
            downloadLink.getLinkStatus().setStatusText("Login required to download this content");
            return AvailableStatus.UNCHECKABLE;
        }
        dllink = downloadLink.getStringProperty("directurl", null);
        if (dllink == null) {
            String getlink = downloadLink.getDownloadURL().replace("instagrammdecrypted://", "https://www.instagram.com/p/");
            if (!getlink.endsWith("/")) {
                /* Add slash to the end to prevent 302 redirect to speed up the download process a tiny bit. */
                getlink += "/";
            }
            br.getPage(getlink);
            if (br.getRequest().getHttpConnection().getResponseCode() == 404 || br.containsHTML("Oops, an error occurred")) {
                /*
                 * This will also happen if a user tries to access private urls without being logged in --> Which is why we need to know the
                 * private_url status from the crawler!
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Set releasedate as property */
            String date = PluginJSonUtils.getJson(this.br, "date");
            if (date == null || !date.matches("\\d+")) {
                date = PluginJSonUtils.getJson(this.br, "taken_at_timestamp");
            }
            if (date != null && date.matches("\\d+")) {
                setReleaseDate(downloadLink, Long.parseLong(date));
            }
            String ext = ".mp4";
            dllink = PluginJSonUtils.getJsonValue(this.br, "video_url");
            // Maybe we have a picture
            if (dllink == null) {
                ext = null;
                dllink = br.getRegex("property=\"og:image\" content=\"(http[^<>\"]*?)\"").getMatch(0);
                String remove = new Regex(dllink, "(/[a-z0-9]+?x[0-9]+/)").getMatch(0); // Size
                if (remove != null) {
                    dllink = dllink.replace(remove, "/");
                }
                downloadLink.setContentUrl(dllink);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(dllink.replace("\\", ""));
            if (ext == null) {
                ext = getFileNameExtensionFromString(dllink, ".jpg");
            }
            String server_filename = getFileNameFromURL(new URL(dllink));
            if (this.getPluginConfig().getBooleanProperty(PREFER_SERVER_FILENAMES, defaultPREFER_SERVER_FILENAMES) && server_filename != null) {
                server_filename = fixServerFilename(server_filename, ext);
                downloadLink.setFinalFileName(server_filename);
            } else {
                // decrypter has set the proper name!
                // if the user toggles PREFER_SERVER_FILENAMES setting many times the name can change.
                final String name = downloadLink.getStringProperty("decypter_filename", null);
                if (name != null) {
                    downloadLink.setFinalFileName(name);
                } else {
                    // do not change.
                    logger.warning("missing storable, filename will not be renamed");
                }
            }
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                server_issues = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    public static void setReleaseDate(final DownloadLink dl, final long date) {
        final String targetFormat = "yyyy-MM-dd";
        final Date theDate = new Date(date * 1000);
        final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
        final String formattedDate = formatter.format(theDate);
        dl.setProperty("date", formattedDate);
    }

    public static String fixServerFilename(String server_filename, final String correctExtension) {
        final String server_filename_ext = getFileNameExtensionFromString(server_filename, null);
        if (correctExtension != null && server_filename_ext == null) {
            server_filename += correctExtension;
        } else if (correctExtension != null && !server_filename_ext.equalsIgnoreCase(correctExtension)) {
            server_filename = server_filename.replace(server_filename_ext, correctExtension);
        }
        return server_filename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (this.is_private_url) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(downloadLink);
    }

    public void handleDownload(final DownloadLink downloadLink) throws Exception {
        int maxchunks = MAXCHUNKS_pictures;
        if (downloadLink.getFinalFileName() != null && downloadLink.getFinalFileName().contains(".mp4") || downloadLink.getName() != null && downloadLink.getName().contains(".mp4")) {
            maxchunks = MAXCHUNKS_videos;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, RESUME, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAXDOWNLOADS;
    }

    @SuppressWarnings("unchecked")
    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBR(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.getPage(MAINPAGE + "/");
                try {
                    br.setHeader("Accept", "*/*");
                    br.setHeader("X-Instagram-AJAX", "1");
                    br.setHeader("X-CSRFToken", br.getCookie("instagram.com", "csrftoken"));
                    br.setHeader("X-Requested-With", "XMLHttpRequest");
                    br.postPage("/accounts/login/ajax/", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                } finally {
                    br.setHeader("X-Instagram-AJAX", null);
                    br.setHeader("X-CSRFToken", null);
                    br.setHeader("X-Requested-With", null);
                }
                if ("fail".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                    // 2 factor (Coded semi blind).
                    if ("checkpoint_required".equals(PluginJSonUtils.getJsonValue(br, "message"))) {
                        final String page = PluginJSonUtils.getJsonValue(br, "checkpoint_url");
                        br.getPage(page);
                        // verify by email.
                        Form f = br.getFormBySubmitvalue("Verify+by+Email");
                        if (f == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        br.submitForm(f);
                        f = br.getFormBySubmitvalue("Verify+Account");
                        if (f == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        // dialog here to ask for 2factor verification 6 digit code.
                        final DownloadLink dummyLink = new DownloadLink(null, "Account 2 Factor Auth", MAINPAGE, br.getURL(), true);
                        final String code = getUserInput("2 Factor Authenication\r\nPlease enter in the 6 digit code within your Instagram linked email account", dummyLink);
                        if (code == null) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid 2 Factor response", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        f.put("response_code", Encoding.urlEncode(code));
                        // correct or incorrect?
                        if (br.containsHTML(">Please check the code we sent you and try again\\.<")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid 2 Factor response", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        // now 2factor most likely wont have the authenticated json if statement below....
                        // TODO: confirm what's next.
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unfinished code, please report issue with logs to Development Team.");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (!br.containsHTML("\"authenticated\"\\s*:\\s*true\\s*")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
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
        account.setConcurrentUsePossible(true);
        ai.setStatus("Free Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* We're already logged in - no need to login again here! */
        this.handleDownload(link);
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.73 Safari/537.36");
        br.setCookie(MAINPAGE, "ig_pr", "1");
        // 429 == too many requests, we need to rate limit requests.
        br.setAllowedResponseCodes(400, 429);
        br.setRequestIntervalLimit("instagram.com", 250);
        br.setFollowRedirects(true);
        return br;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_SERVER_FILENAMES, "Use server-filenames whenever possible?").setDefaultValue(defaultPREFER_SERVER_FILENAMES));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), QUIT_ON_RATE_LIMIT_REACHED, "Abort crawl process once rate limit is reached?").setDefaultValue(defaultQUIT_ON_RATE_LIMIT_REACHED));
        final ConfigEntry grabXitems = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ONLY_GRAB_X_ITEMS, "Only grab the X latest items?").setDefaultValue(defaultONLY_GRAB_X_ITEMS);
        getConfig().addEntry(grabXitems);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), ONLY_GRAB_X_ITEMS_NUMBER, "How many items shall be grabbed?", defaultONLY_GRAB_X_ITEMS_NUMBER, 1025, defaultONLY_GRAB_X_ITEMS_NUMBER).setDefaultValue(defaultONLY_GRAB_X_ITEMS_NUMBER).setEnabledCondidtion(grabXitems, true));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
