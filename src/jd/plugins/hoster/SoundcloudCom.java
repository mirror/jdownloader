//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https://(www\\.)?soundclouddecrypted\\.com/[A-Za-z\\-_0-9]+/[A-Za-z\\-_0-9]+(/[A-Za-z\\-_0-9]+)?" }, flags = { 2 })
public class SoundcloudCom extends PluginForHost {

    private String url;

    public SoundcloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    public final static String  CLIENTID           = "b45b1aa10f1ac2941910a7f0d10f8e28";
    private static final String CUSTOM_DATE        = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME    = "CUSTOM_FILENAME";
    private static final String GRAB500THUMB       = "GRAB500THUMB";
    private static final String CUSTOM_PACKAGENAME = "CUSTOM_PACKAGENAME";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("soundclouddecrypted", "soundcloud"));
    }

    @Override
    public String getAGBLink() {
        return "http://soundcloud.com/terms-of-use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        doFree(link);
    }

    private void doFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // start of file extension correction.
        // required because original-format implies the uploaded format might not be what the end user downloads.
        String oldName = link.getFinalFileName();
        if (oldName == null) oldName = link.getName();
        String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        String newExtension = null;
        if (serverFilename == null) {
            logger.info("Server filename is null, keeping filename: " + oldName);
        } else {
            if (serverFilename.contains(".")) {
                newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
            } else {
                logger.info("HTTP headers don't contain filename.extension information");
            }
        }
        if (newExtension != null && !oldName.endsWith(newExtension)) {
            String oldExtension = null;
            if (oldName.contains(".")) oldExtension = oldName.substring(oldName.lastIndexOf("."));
            if (oldExtension != null && oldExtension.length() <= 5) {
                link.setFinalFileName(oldName.replace(oldExtension, newExtension));
            } else {
                link.setFinalFileName(oldName + newExtension);
            }
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(this.br, aa, false);
            } catch (final PluginException e) {
            }
        }
        url = parameter.getStringProperty("directlink");
        if (url != null) {
            checkDirectLink(parameter, url);
            if (url != null) {
                parameter.setFinalFileName(getFormattedFilename(parameter));
                return AvailableStatus.TRUE;
            }
        }
        br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(parameter.getDownloadURL()) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + CLIENTID);
        if (br.containsHTML("\"404 \\- Not Found\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final AvailableStatus status = checkStatus(parameter, this.br.toString());
        if (status.equals(AvailableStatus.FALSE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        checkDirectLink(parameter, url);
        return AvailableStatus.TRUE;
    }

    public AvailableStatus checkStatus(final DownloadLink parameter, final String source) throws ParseException {
        String filename = getXML("title", source);
        if (filename == null) {
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.pluginBroken", "The host plugin is broken!"));
            return AvailableStatus.FALSE;
        }
        filename = Encoding.htmlDecode(filename.trim().replace("\"", "'"));
        final String filesize = getXML("original-content-size", source);
        if (filesize != null) parameter.setDownloadSize(Long.parseLong(filesize));
        final String description = getXML("description", source);
        if (description != null) {
            try {
                parameter.setComment(description);
            } catch (Throwable e) {
            }
        }
        String date = new Regex(source, "<created\\-at type=\"datetime\">([^<>\"]*?)</created-at>").getMatch(0);
        String username = getXML("username", source);
        String type = getXML("original-format", source);
        if (type == null) type = "mp3";
        username = Encoding.htmlDecode(username.trim());
        url = getXML("download-url", source);
        if (url != null) {
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.downloadavailable", "Original file is downloadable"));
        } else {
            url = getXML("stream-url", source);
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.previewavailable", "Preview (Stream) is downloadable"));
        }
        if (url == null) {
            parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.pluginBroken", "The host plugin is broken!"));
            return AvailableStatus.FALSE;
        }

        parameter.setProperty("directlink", url + "?client_id=" + CLIENTID);
        parameter.setProperty("channel", username);
        parameter.setProperty("plainfilename", filename);
        parameter.setProperty("originaldate", date);
        parameter.setProperty("type", type);
        final String formattedfilename = getFormattedFilename(parameter);
        parameter.setFinalFileName(formattedfilename);
        return AvailableStatus.TRUE;
    }

    private void checkDirectLink(final DownloadLink downloadLink, final String property) {
        URLConnectionAdapter con = null;
        try {
            Browser br2 = br.cloneBrowser();
            con = br2.openGetConnection(url);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getResponseCode() == 401) {
                downloadLink.setProperty(property, Property.NULL);
                url = null;
                return;
            }
            downloadLink.setDownloadSize(con.getLongContentLength());

        } catch (Exception e) {
            downloadLink.setProperty(property, Property.NULL);
            url = null;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private static final String MAINPAGE = "http://soundcloud.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
                try {
                    /* not available in old stable */
                    br.setAllowedResponseCodes(new int[] { 422 });
                } catch (Throwable e) {
                }
                br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
                br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
                br.getPage("https://soundcloud.com/connect?client_id=" + CLIENTID + "&response_type=token&scope=non-expiring%20fast-connect%20purchase%20upload&display=next&redirect_uri=https%3A//soundcloud.com/soundcloud-callback.html");
                br.setFollowRedirects(false);
                URLConnectionAdapter con = null;
                try {
                    con = br.openPostConnection("https://soundcloud.com/connect/login", "remember_me=on&redirect_uri=https%3A%2F%2Fsoundcloud.com%2Fsoundcloud-callback.html&response_type=token&scope=non-expiring+fast-connect+purchase+upload&display=next&client_id=" + CLIENTID + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (con.getResponseCode() == 422) {
                        br.followConnection();
                        final String rcID = br.getRegex("\\?k=([^<>\"]*?)\"").getMatch(0);
                        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login function broken, please contact our support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.setId(rcID);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "soundcloud.com", "http://soundcloud.com", true);
                        final String c = getCaptchaCode(cf, dummyLink);
                        con = br.openPostConnection("https://soundcloud.com/connect/login", "remember_me=on&redirect_uri=https%3A%2F%2Fsoundcloud.com%2Fsoundcloud-callback.html&response_type=token&scope=non-expiring+fast-connect+purchase+upload&display=next&client_id=" + CLIENTID + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                        br.followConnection();
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
                final String continueLogin = br.getRegex("\"(https://soundcloud\\.com/soundcloud\\-callback\\.html[^<>\"]*?)\"").getMatch(0);
                if (continueLogin == null || !"free".equals(br.getCookie("https://soundcloud.com/", "c"))) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.getPage(continueLogin);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        doFree(link);
    }

    public String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        String songTitle = downloadLink.getStringProperty("plainfilename", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("soundcloud.com");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
        if (!formattedFilename.contains("*songtitle*") || !formattedFilename.contains("*ext*")) formattedFilename = defaultCustomFilename;
        String ext = downloadLink.getStringProperty("type", null);
        if (ext != null)
            ext = "." + ext;
        else
            ext = ".mp3";

        String date = downloadLink.getStringProperty("originaldate", null);
        final String channelName = downloadLink.getStringProperty("channel", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            // 2011-08-10T22:50:49Z
            date = date.replace("T", ":");
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
            Date dateStr = formatter.parse(date);

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null)
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            else
                formattedFilename = formattedFilename.replace("*date*", "");
        }
        if (formattedFilename.contains("*channelname*") && channelName != null) {
            formattedFilename = formattedFilename.replace("*channelname*", channelName);
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*songtitle*", songTitle);

        return formattedFilename;
    }

    @Override
    public String getDescription() {
        return "JDownloader's soundcloud.com plugin helps downloading audiofiles. JDownloader provides settings for the filenames.";
    }

    private final static String defaultCustomFilename    = "*songtitle* - *channelname**ext*";
    private final static String defaultCustomPackagename = "*channelname* - *playlistname*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB500THUMB, JDL.L("plugins.hoster.soundcloud.grab500thumb", "Grab 500x500 thumbnail (.jpg)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename/packagename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.soundcloud.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy_HH-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*songtitle**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.soundcloud.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*songtitle* = name of the song without extension\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.mp3'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the packagename for playlists and 'soundcloud.com/user' links! Example: '*channelname* - *playlistname*':"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, JDL.L("plugins.hoster.soundcloud.custompackagename", "Define how the packagenames should look:")).setDefaultValue(defaultCustomPackagename));
        final StringBuilder sbpack = new StringBuilder();
        sbpack.append("Explanation of the available tags:\r\n");
        sbpack.append("*channelname* = name of the channel/uploader\r\n");
        sbpack.append("*playlistname* = name of the playlist (= username for 'soundcloud.com/user' links)\r\n");
        sbpack.append("*date* = date when the linklist was created - appears in the user-defined format above\r\n");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbpack.toString()));
    }

    // CUSTOM_PACKAGENAME

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private String getJson(final String parameter) {
        return br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
    }

    public String getXML(final String parameter, final String source) {
        return new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}