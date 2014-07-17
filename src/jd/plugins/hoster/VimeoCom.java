//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "decryptedforVimeoHosterPlugin\\d?://(www\\.|player\\.)?vimeo\\.com/(video/)?\\d+" }, flags = { 2 })
public class VimeoCom extends PluginForHost {

    private static final String MAINPAGE           = "http://vimeo.com";
    private String              finalURL;
    private static Object       LOCK               = new Object();
    private static final String Q_MOBILE           = "Q_MOBILE";
    private static final String Q_ORIGINAL         = "Q_ORIGINAL";
    private static final String Q_HD               = "Q_HD";
    private static final String Q_SD               = "Q_SD";
    private static final String Q_BEST             = "Q_BEST";
    private static final String CUSTOM_DATE        = "CUSTOM_DATE_3";
    private static final String CUSTOM_FILENAME    = "CUSTOM_FILENAME_3";
    private static final String CUSTOM_PACKAGENAME = "CUSTOM_PACKAGENAME_3";

    public VimeoCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://vimeo.com/join");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        String quality = new Regex(link.getDownloadURL(), "decryptedforVimeoHosterPlugin(\\d+):").getMatch(0);
        String url = link.getDownloadURL().replaceFirst("decryptedforVimeoHosterPlugin\\d+?://", "http://");
        if (quality != null) {
            url = url + "?_=" + quality;
        }
        link.setUrlDownload(url);
    }

    @Override
    public String getAGBLink() {
        return "http://www.vimeo.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        finalURL = downloadLink.getStringProperty("directURL", null);
        if (finalURL != null) {
            try {
                con = br.openGetConnection(finalURL);
                if (con.getContentType() != null && !con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
                    return AvailableStatus.TRUE;
                } else {
                    /* durectURL no longer valid */
                    downloadLink.setProperty("directURL", null);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        String ID = downloadLink.getStringProperty("videoID", null);
        if (ID == null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Run decrypter again!");
        }
        br = new Browser();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("vimeo.com", "v6f", "1");
        // we do not want German headers!
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.getPage("http://vimeo.com/" + ID);

        /* Workaround for User from Iran */
        if (br.containsHTML("<body><iframe src=\"http://10\\.10\\.\\d+\\.\\d+\\?type=(Invalid Site)?\\&policy=MainPolicy")) {
            br.getPage("http://player.vimeo.com/config/" + ID);
        }

        handlePW(downloadLink, br, "http://vimeo.com/" + ID + "/password");
        String newURL = null;
        // because names can often change by the uploader, like youtube.
        String name = getTitle(br);
        final String qualities[][] = getQualities(br, ID);
        if (qualities == null || qualities.length == 0) {
            logger.warning("vimeo.com: Qualities could not be found");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String quality[] : qualities) {
            // match refreshed qualities to stored reference, to make sure we have the same format for resume! we never want to cross over!
            if (downloadLink.getStringProperty("videoQuality", null).equalsIgnoreCase(quality[2])) {
                finalURL = quality[0];
                if (finalURL != null && !finalURL.startsWith("http://")) {
                    finalURL = "http://vimeo.com" + finalURL;
                }
                break;
            }
        }
        if (finalURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directURL", finalURL);
        downloadLink.setProperty("videoTitle", name);
        downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (finalURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        // TODO: review this method, for now everything ports into free, as every link will have directURL.
        if (link.getStringProperty("directURL", null) != null) {
            handleFree(link);
            return;
        }
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("\">Sorry, not available for download")) {
            logger.info("No download available for link: " + link.getDownloadURL() + " , downloading as unregistered user...");
            doFree(link);
            return;
        }
        String dllink = br.getRegex("class=\"download\">[\t\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/?download/video:\\d+\\?v=\\d+\\&e=\\d+\\&h=[a-z0-9]+\\&uh=[a-z0-9]+)\"").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("/")) {
            dllink = MAINPAGE + "/" + dllink;
        } else {
            dllink = MAINPAGE + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String oldName = link.getName();
        final String newName = getFileNameFromHeader(dl.getConnection());
        final String name = oldName.substring(0, oldName.lastIndexOf(".")) + newName.substring(newName.lastIndexOf("."));
        link.setName(name);
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = new AccountInfo();
            if (!new Regex(account.getUser(), ".+@.+\\..+").matches()) {
                account.setProperty("cookies", null);
                account.setValid(false);
                ai.setStatus("Invalid email address");
                return ai;

            }
            try {
                login(account, true);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                account.setValid(false);
                return ai;
            }
            br.getPage("http://vimeo.com/settings");
            String type = br.getRegex("acct_status\">.*?>(.*?)<").getMatch(0);
            if (type == null) {
                type = br.getRegex("user_type', '(.*?)'").getMatch(0);
            }
            if (type != null) {
                ai.setStatus(type);
            } else {
                ai.setStatus(null);
            }
            account.setValid(true);
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.setDebug(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = account.getUser().matches(account.getStringProperty("name", account.getUser()));
                if (acmatch) {
                    acmatch = account.getPass().matches(account.getStringProperty("pass", account.getPass()));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("vimeo") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.getPage("https://vimeo.com/log_in");
                final String xsrft = br.getRegex("xsrft: \\'(.*?)\\'").getMatch(0);
                if (xsrft == null) {
                    account.setProperty("cookies", null);
                    logger.warning("Login is broken!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* important, else we get a 401 */
                br.setCookie(MAINPAGE, "xsrft", xsrft);
                if (!new Regex(account.getUser(), ".*?@.*?\\..+").matches()) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage("https://vimeo.com/log_in", "action=login&service=vimeo&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&token=" + Encoding.urlEncode(xsrft));
                if (br.getCookie(MAINPAGE, "vimeo") == null) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", account.getUser());
                account.setProperty("pass", account.getPass());
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    public static final String containsPass = "<title>Private Video on Vimeo</title>|To watch this video, please provide the correct password";

    private void handlePW(DownloadLink downloadLink, Browser br, String url) throws PluginException, IOException {
        if (br.containsHTML(containsPass)) {
            final String xsrft = br.getRegex("xsrft: '(.*?)'").getMatch(0);
            if (xsrft != null) {
                br.setCookie(br.getHost(), "xsrft", xsrft);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String passCode = downloadLink.getStringProperty("pass", null);
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            }
            if (passCode == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            }
            br.postPage(url, "password=" + Encoding.urlEncode(passCode) + "&token=" + xsrft);
            if (br.containsHTML("This is a private video")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            }
            downloadLink.setProperty("pass", passCode);
        }
    }

    // IMPOORTANT: Sync with HOSTER AND DECRYPTER plugin
    public String[][] getQualities(final Browser ibr, final String ID) throws Exception {
        /*
         * little pause needed so the next call does not return trash
         */
        Thread.sleep(1000);
        // process the different page layouts
        String qualities[][] = null;
        // qx[0] = url
        // qx[1] = extension
        // qx[2] = format (mobile|sd|hd)
        // qx[3] = frameSize (\d+x\d+)
        // qx[4] = bitrate (\d+)
        // qx[5] = fileSize (\d [a-zA-Z]{2})

        String configURL = ibr.getRegex("data-config-url=\"(https?://player\\.vimeo\\.com/(v2/)?video/\\d+/config.*?)\"").getMatch(0);
        if (ibr.containsHTML("iconify_down_b")) {
            // download button.. does this give you all qualities? If not we should drop this.
            Browser gq = ibr.cloneBrowser();
            /* With dl button */
            gq.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            gq.getPage("http://vimeo.com/" + ID + "?action=download");
            // german accept language will effect the language of this response, Datei instead of file.
            String[][] q = gq.getRegex("href=\"([^\"]*/" + ID + "/download.*?)\" download=\"(.*?)\".*?>([A-Za-z0-9]+) \\.[A-Z0-9]+ file<.*?\\((\\d+x\\d+) / (.*?)\\)").getMatches();
            qualities = new String[q.length][6];
            for (int i = 0; i < q.length; i++) {
                // does not have reference to bitrate here.
                qualities[i][0] = q[i][0]; // download button link expires just like the rest!
                qualities[i][1] = new Regex(q[i][1], ".+(\\.[a-z0-9]{3,4})$").getMatch(0);
                qualities[i][2] = q[i][2];
                qualities[i][3] = q[i][3];
                qualities[i][4] = null;
                qualities[i][5] = q[i][4];
            }
        }
        if (configURL != null && (qualities == null || (qualities != null && qualities.length == 0))) {
            // iconify_down_b could fail, revert to the following if statements.
            Browser gq = ibr.cloneBrowser();
            configURL = configURL.replaceAll("&amp;", "&");
            gq.getPage(configURL);
            final String fmts = gq.getRegex("\"files\":\\{\"(h264|vp6)\":\\{(.*?)\\}\\}").getMatch(1);
            if (fmts != null) {
                String quality[][] = new Regex(fmts, "\"(.*?)\":\\{(.*?)(\\}|$)").getMatches();
                qualities = new String[quality.length][6];
                for (int i = 0; i < quality.length; i++) {
                    final String url = new Regex(quality[i][1], "\"url\":\"(http.*?)\"").getMatch(0);
                    final String height = new Regex(quality[i][1], "\"height\":(\\d+)").getMatch(0);
                    final String width = new Regex(quality[i][1], "\"width\":(\\d+)").getMatch(0);
                    final String bitrate = new Regex(quality[i][1], "\"bitrate\":(\\d+)").getMatch(0);
                    final String ext = new Regex(url, ".+(\\.[a-z0-9]{3,4})$").getMatch(0);
                    qualities[i][0] = url;
                    qualities[i][1] = ext;
                    qualities[i][2] = quality[i][0];
                    qualities[i][3] = (height == null || width == null ? null : width + "x" + height);
                    qualities[i][4] = bitrate;
                    qualities[i][5] = null;
                }
            }
        } else if (configURL == null || (qualities == null || (qualities != null && qualities.length == 0))) {
            // TODO: need new to rewrite/review this.. need to find links to test.
            String sig = ibr.getRegex("\"signature\":\"([0-9a-f]+)\"").getMatch(0);
            String time = ibr.getRegex("\"timestamp\":(\\d+)").getMatch(0);
            if (sig != null && time != null) {
                String fmts = ibr.getRegex("\"files\":\\{\"h264\":\\[(.*?)\\]\\}").getMatch(0);
                if (fmts != null) {
                    String quality[] = fmts.replaceAll("\"", "").split(",");
                    qualities = new String[quality.length][4];
                    for (int i = 0; i < quality.length; i++) {
                        qualities[i][0] = "http://player.vimeo.com/play_redirect?clip_id=" + ID + "&sig=" + sig + "&time=" + time + "&quality=" + quality[i];
                        qualities[i][2] = quality[i];
                        qualities[i][3] = null;
                    }
                } else {
                    // Nothing found so SD should be available at least...
                    qualities = new String[1][4];
                    qualities[0][0] = ibr.getRegex("").getMatch(0);
                    qualities[0][0] = "http://player.vimeo.com/play_redirect?clip_id=" + ID + "&sig=" + sig + "&time=" + time + "&quality=sd&codecs=H264,VP8,VP6&type=moogaloop_local&embed_location=&seek=0";
                    qualities[0][2] = "sd";
                    qualities[0][3] = null;
                }
            }
        }
        return qualities;
    }

    public String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        String videoTitle = downloadLink.getStringProperty("videoTitle", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        if (!formattedFilename.contains("*videoname") && !formattedFilename.contains("*ext*") && !formattedFilename.contains("*videoid*")) {
            formattedFilename = defaultCustomFilename;
        }

        final String videoExt = downloadLink.getStringProperty("videoExt", null);
        final String date = downloadLink.getStringProperty("originalDate", null);
        final String channelName = downloadLink.getStringProperty("channel", null);
        final String videoQuality = downloadLink.getStringProperty("videoQuality", null);
        final String videoID = downloadLink.getStringProperty("videoID", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
            final String[] dateStuff = date.split("T");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");
            Date dateStr = formatter.parse(dateStuff[0] + ":" + dateStuff[1]);

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
            if (formattedDate != null) {
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            } else {
                formattedFilename = formattedFilename.replace("*date*", "");
            }
        }
        if (formattedFilename.contains("*videoid*")) {
            formattedFilename = formattedFilename.replace("*videoid*", videoID);
        }

        if (formattedFilename.contains("*channelname*")) {
            if (channelName != null) {
                formattedFilename = formattedFilename.replace("*channelname*", channelName);
            } else {
                formattedFilename = formattedFilename.replace("*channelname*", "");
            }
        }
        // quality
        if (videoQuality != null) {
            formattedFilename = formattedFilename.replace("*quality*", videoQuality);
        } else {
            formattedFilename = formattedFilename.replace("*quality*", "");
        }
        // file extension
        if (videoExt != null) {
            formattedFilename = formattedFilename.replace("*ext*", videoExt);
        } else {
            formattedFilename = formattedFilename.replace("*ext*", ".mp4");
        }
        // Insert filename at the end to prevent errors with tags
        if (videoTitle != null) {
            formattedFilename = formattedFilename.replace("*videoname*", videoTitle);
        }

        return formattedFilename;
    }

    public String getTitle(final Browser ibr) throws PluginException {
        String title = ibr.getRegex("\"title\":\"([^<>\"]+)\"").getMatch(0);
        if (title == null) {
            title = ibr.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\">").getMatch(0);
        }
        if (title == null) {
            logger.warning("Decrypter broken for link: " + ibr.getURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = getFormattedString(title);
        return title;
    }

    public String getFormattedString(final String s) {
        String format = s;
        format = unescape(format);
        format = Encoding.htmlDecode(format);
        format = charRemoval(format);
        return format.trim();
    }

    private String charRemoval(final String s) {
        String output = s;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        // not illegal
        // output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private static boolean ut_pluginLoaded = false;

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (ut_pluginLoaded == false) {

            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) {
                throw new IllegalStateException("youtube plugin not found!");
            }
            ut_pluginLoaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getDescription() {
        return "JDownloader's Vimeo Plugin helps downloading videoclips from vimeo.com. Vimeo provides different video qualities.";
    }

    private final static String defaultCustomFilename = "*videoname**ext*";

    private void setConfigElements() {
        final ConfigEntry loadbest = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.vimeo.best", "Load Best Version ONLY")).setDefaultValue(false);
        getConfig().addEntry(loadbest);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_ORIGINAL, JDL.L("plugins.hoster.vimeo.loadoriginal", "Load Original Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.vimeo.loadsd", "Load HD Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SD, JDL.L("plugins.hoster.vimeo.loadhd", "Load SD Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MOBILE, JDL.L("plugins.hoster.vimeo.loadmobile", "Load Mobile Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.vimeocom.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy_HH-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*videoname**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.vimeocom.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*videoname* = name of the video without extension\r\n");
        sb.append("*videoid* = id of the video\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
    }

}