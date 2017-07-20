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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;
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
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.UserAgents;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "decryptedforVimeoHosterPlugin\\d?://(www\\.|player\\.)?vimeo\\.com/((video/)?\\d+|ondemand/[A-Za-z0-9\\-_]+)" })
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

    private static AtomicReference<String> userAgent = new AtomicReference<String>(null);

    public Browser prepBrGeneral(final DownloadLink dl, final Browser prepBr) {
        final String vimeo_forced_referer = dl != null ? getForcedReferer(dl) : null;
        if (vimeo_forced_referer != null) {
            prepBr.getHeaders().put("Referer", vimeo_forced_referer);
        }
        /* we do not want German headers! */
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        while (userAgent.get() == null || userAgent.get().contains(" Chrome/")) {
            userAgent.set(UserAgents.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", userAgent.get());
        prepBr.setAllowedResponseCodes(418);
        return prepBr;
    }

    /* API - might be useful for the future: https://github.com/bromix/plugin.video.vimeo/blob/master/resources/lib/vimeo/client.py */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        prepBrGeneral(downloadLink, br);
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        finalURL = downloadLink.getStringProperty("directURL", null);
        if (finalURL != null) {
            try {
                /* @since JD2 */
                con = br.openHeadConnection(finalURL);
                if (con.getContentType() != null && !con.getContentType().contains("html") && con.isOK()) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
                    return AvailableStatus.TRUE;
                } else {
                    /* durectURL no longer valid */
                    downloadLink.setProperty("directURL", Property.NULL);
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
        setBrowserExclusive();
        br = prepBrGeneral(downloadLink, new Browser());
        br.setFollowRedirects(true);
        if (usePrivateHandling(downloadLink)) {
            br.getPage("http://player.vimeo.com/video/" + ID);
            if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || "This video does not exist\\.".equals(PluginJSonUtils.getJsonValue(br, "message"))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            br.getPage("https://vimeo.com/" + ID);
            /* Workaround for User from Iran */
            if (br.containsHTML("<body><iframe src=\"http://10\\.10\\.\\d+\\.\\d+\\?type=(Invalid Site)?\\&policy=MainPolicy")) {
                br.getPage("http://player.vimeo.com/config/" + ID);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }

            if (br.getHttpConnection().getResponseCode() == 404) {
                /* 2016-12-26: Workaround related for forum report 72025 */
                br.getPage("http://player.vimeo.com/video/" + ID);
            }
            if (br.containsHTML(containsPass)) {
                handlePW(downloadLink, br, "https://vimeo.com/" + ID + "/password");
            }
        }
        // because names can often change by the uploader, like youtube.
        String name = getTitle(br);
        final String qualities[][] = getQualities(br, ID);
        if (qualities == null || qualities.length == 0) {
            logger.warning("vimeo.com: Qualities could not be found");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String downloadlinkId = downloadLink.getLinkID().replace("_ORIGINAL", "");
        final boolean hasType = downloadLink.hasProperty("videoType");
        for (String quality[] : qualities) {
            final String linkdupeid;
            if (hasType) {
                linkdupeid = ID + "_" + quality[2] + "_" + quality[3] + (StringUtils.isNotEmpty(quality[7]) ? "_" + quality[7] : "") + quality[8];
            } else {
                linkdupeid = ID + "_" + quality[2] + "_" + quality[3] + (StringUtils.isNotEmpty(quality[7]) ? "_" + quality[7] : "");
            }

            // match refreshed qualities to stored reference, to make sure we have the same format for resume! we never want to cross
            // over!
            if (StringUtils.equalsIgnoreCase(linkdupeid, downloadlinkId)) {
                finalURL = quality[0];
                break;
            }
        }
        if (finalURL == null) {
            for (String quality[] : qualities) {
                // match refreshed qualities to stored reference, to make sure we have the same format for resume! we never want to cross
                // over!
                if (downloadLink.getStringProperty("videoQuality", null).equalsIgnoreCase(quality[2])) {
                    finalURL = quality[0];
                    break;
                }
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
        br.getPage(downloadLink.getDownloadURL());
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean usePrivateHandling(final DownloadLink dl) {
        final boolean is_private_link = dl.getBooleanProperty("private_player_link", false);
        final boolean has_forced_referer = getForcedReferer(dl) != null;
        final boolean use_private_handling = (is_private_link || has_forced_referer);
        return use_private_handling;
    }

    @SuppressWarnings("deprecation")
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
        final boolean isPrivateLink = usePrivateHandling(link);
        if (!isPrivateLink) {
            br.getPage(link.getDownloadURL());
        }
        if (isPrivateLink || br.containsHTML("\">Sorry, not available for download")) {
            /* Premium / account users cannot download private URLs. */
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
        if (!dllink.startsWith("http")) {
            if (!dllink.startsWith("/")) {
                dllink = MAINPAGE + "/" + dllink;
            } else {
                dllink = MAINPAGE + dllink;
            }
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = new AccountInfo();
            if (!account.getUser().matches(".+@.+\\..+")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            try {
                login(account, true);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                account.setValid(false);
                return ai;
            }
            br.getPage("/settings");
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
                prepBrGeneral(null, br);
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
                br.getPage("https://www.vimeo.com/log_in");
                final String xsrft = getXsrft(br);
                // static post are bad idea, always use form.
                final Form login = br.getFormbyProperty("id", "login_form");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("token", Encoding.urlEncode(xsrft));
                login.put("email", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
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

    public static final String getXsrft(Browser br) throws PluginException {
        String xsrft = br.getRegex("vimeo\\.xsrft\\s*=\\s*('|\"|)([a-z0-9\\.]{32,})\\1").getMatch(1);
        if (xsrft == null) {
            xsrft = br.getRegex("\"xsrft\"\\s*:\\s*\"([a-z0-9\\.]{32,})\"").getMatch(0);
        }
        if (xsrft == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setCookie(br.getHost(), "xsrft", xsrft);
        return xsrft;
    }

    public static final String containsPass = "<title>Private Video on Vimeo</title>|To watch this video, please provide the correct password";

    private void handlePW(final DownloadLink downloadLink, final Browser br, final String url) throws PluginException, IOException {
        if (br.containsHTML(containsPass)) {
            final String xsrft = getXsrft(br);
            String passCode = downloadLink.getStringProperty("pass", null);
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            }
            if (passCode == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            }
            br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode) + "&token=" + xsrft);
            if (br.containsHTML(containsPass)) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            }
            downloadLink.setProperty("pass", passCode);
        }
    }

    public static final int quality_info_length = 9;

    @SuppressWarnings({ "unchecked", "unused" })
    public static String[][] getQualities(final Browser ibr, final String ID) throws Exception {
        /*
         * little pause needed so the next call does not return trash
         */
        Thread.sleep(1000);
        boolean debug = false;

        // qx[0] = url
        // qx[1] = extension
        // qx[2] = format (mobile|sd|hd|original)
        // qx[3] = frameSize (\d+x\d+)
        // qx[4] = bitrate (\d+)
        // qx[5] = fileSize (\d [a-zA-Z]{2})
        // qx[6] = Codec
        // qx[7] = ID
        // qx[8] = DOWNLOAD/STREAM

        String configURL = ibr.getRegex("data-config-url=\"(https?://player\\.vimeo\\.com/(v2/)?video/\\d+/config.*?)\"").getMatch(0);
        if (configURL == null) {
            // can be within json on the given page now.. but this is easy to just request again raz20151215
            configURL = PluginJSonUtils.getJsonValue(ibr, "config_url");
        }
        final ArrayList<String[]> results = new ArrayList<String[]>();
        if (ibr.containsHTML("download_config\"\\s*?:\\s*?\\[")) {
            // new//
            Browser gq = ibr.cloneBrowser();
            /* With dl button */
            gq.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String json = gq.getPage("/" + ID + "?action=load_download_config");
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            if (entries != null) {
                final List<Object> files = (List<Object>) entries.get("files");
                if (files != null) {
                    for (final Object file : files) {
                        final String[] result = new String[quality_info_length];
                        results.add(result);
                        final Map<String, Object> info = (Map<String, Object>) file;
                        result[0] = String.valueOf(info.get("download_url"));
                        final String ext = String.valueOf(info.get("extension"));
                        if (StringUtils.isNotEmpty(ext)) {
                            result[1] = "." + ext;
                        }
                        if (StringUtils.containsIgnoreCase(String.valueOf(info.get("public_name")), "sd")) {
                            result[2] = "sd";
                        } else if (StringUtils.containsIgnoreCase(String.valueOf(info.get("public_name")), "hd")) {
                            result[2] = "hd";
                        }
                        result[3] = String.valueOf(info.get("width")) + "x" + String.valueOf(info.get("height"));
                        result[4] = null;
                        result[5] = String.valueOf(info.get("size"));
                        /* No codec given */
                        result[6] = null;
                        /* ID */
                        result[7] = null;
                        result[8] = "DOWNLOAD";
                    }
                }
                if (entries.containsKey("source_file")) {
                    final Map<String, Object> file = (Map<String, Object>) entries.get("source_file");
                    final String[] result = new String[quality_info_length];
                    results.add(result);
                    final Map<String, Object> info = file;
                    result[0] = String.valueOf(info.get("download_url"));
                    final String ext = String.valueOf(info.get("extension"));
                    if (StringUtils.isNotEmpty(ext)) {
                        result[1] = "." + ext;
                    }
                    final String height = Integer.toString(((Number) info.get("height")).intValue());
                    final String width = Integer.toString(((Number) info.get("width")).intValue());
                    result[2] = "original";
                    result[3] = width + "x" + height;
                    result[4] = null;
                    result[5] = String.valueOf(info.get("size"));
                    /* No codec given */
                    result[6] = null;
                    /* ID */
                    result[7] = null;
                    result[8] = "ORIGINAL";
                }
            }
        } else if (ibr.containsHTML("iconify_down_b")) {
            // old//
            /* E.g. video 1761235 */
            /* download button.. does this give you all qualities? If not we should drop this. */
            Browser gq = ibr.cloneBrowser();
            /* With dl button */
            gq.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            gq.getPage("/" + ID + "?action=download");
            /* german accept language will effect the language of this response, Datei instead of file. */
            String[][] q = gq.getRegex("<a href=\"(https?://[^<>\"]*?)\" download=\"([^<>\"]*?)\" rel=\"nofollow\">(Mobile(?: ?(?:SD|HD))?|MP4|SD|HD)[^>]*</a>\\s*<span>\\((\\d+x\\d+) / ((?:\\d+\\.)?\\d+MB)\\)</span>").getMatches();
            if (q != null) {
                for (int i = 0; i < q.length; i++) {
                    final String[] result = new String[quality_info_length];
                    results.add(result);
                    // does not have reference to bitrate here.
                    result[0] = q[i][0]; // download button link expires just like the rest!
                    result[1] = new Regex(q[i][1], ".+(\\.[a-z0-9]{3,4})$").getMatch(0);
                    result[2] = q[i][2];
                    result[3] = q[i][3];
                    result[4] = null;
                    result[5] = q[i][4];
                    /* No codec given */
                    result[6] = null;
                    /* ID */
                    result[7] = null;
                    result[8] = "DOWNLOAD";
                }
            }
        }
        /* player.vimeo.com links = Special case as the needed information is already in our current browser. */
        if (configURL != null || ibr.getURL().contains("player.vimeo.com/")) {
            // iconify_down_b could fail, revert to the following if statements.
            final Browser gq = ibr.cloneBrowser();
            gq.getHeaders().put("Accept", "*/*");
            String json;
            if (configURL != null) {
                configURL = configURL.replaceAll("&amp;", "&");
                gq.getPage(configURL);
                json = gq.toString();
            } else {
                json = ibr.getRegex("a=(\\{\"cdn_url\".*?);if\\(a\\.request\\)").getMatch(0);
                if (json == null) {
                    json = ibr.getRegex("t=(\\{\"cdn_url\".*?);if\\(\\!t\\.request\\)").getMatch(0);
                }
            }
            /* Old handling without DummyScriptEnginePlugin removed AFTER revision 28754 */
            if (json != null) {
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                final LinkedHashMap<String, Object> request = (LinkedHashMap<String, Object>) entries.get("request");
                final LinkedHashMap<String, Object> files = (LinkedHashMap<String, Object>) request.get("files");
                // progressive = web, hls = hls
                if (files.containsKey("progressive")) {
                    final ArrayList<Object> progressive = (ArrayList<Object>) files.get("progressive");
                    // atm they only have one object in array [] and then wrapped in {}
                    for (final Object obj : progressive) {
                        // todo some code to map...
                        final LinkedHashMap<String, Object> abc = (LinkedHashMap<String, Object>) obj;
                        final String url = (String) abc.get("url");
                        Integer.toString(((Number) abc.get("height")).intValue());
                        final String height = Integer.toString(((Number) abc.get("height")).intValue());
                        final String width = Integer.toString(((Number) abc.get("width")).intValue());
                        String bitrate = null;
                        final Object o_bitrate = abc.get("bitrate");
                        if (o_bitrate != null) {
                            /* Bitrate is 'null' for vp6 codec */
                            bitrate = Integer.toString(((Number) o_bitrate).intValue());
                        }
                        String ext = new Regex(url, "(\\.[a-z0-9]{3,4})\\?").getMatch(0);
                        if (ext == null) {
                            ext = new Regex(url, ".+(\\.[a-z0-9]{3,4})$").getMatch(0);
                        }
                        final String quality = (String) abc.get("quality");
                        final String[] result = new String[quality_info_length];
                        results.add(result);
                        result[0] = url;
                        result[1] = ext;
                        result[2] = Integer.parseInt(height) >= 720 ? "hd" : "sd";
                        if (StringUtils.containsIgnoreCase(quality, "720") || StringUtils.containsIgnoreCase(quality, "1080")) {
                            result[2] = "hd";
                        }
                        result[3] = (height == null || width == null ? null : width + "x" + height);
                        result[4] = bitrate;
                        /* No filesize given */
                        result[5] = null;
                        result[6] = ".mp4".equalsIgnoreCase(ext) ? "h264" : "vp5";
                        /* ID */
                        result[7] = String.valueOf(abc.get("id"));
                        result[8] = "STREAM";
                    }
                }
                /*
                 * h264 with sd, mobile and sometimes hd is available most times. vp6 is only available if a download button is available
                 * (as far as we know) and thus should never be decrypted.
                 */
                final String[] codecs = { "h264", "vp6" };
                for (final String codec : codecs) {
                    final LinkedHashMap<String, Object> codecmap = (LinkedHashMap<String, Object>) files.get(codec);
                    if (codecmap != null) {
                        final String[] possibleQualities = { "mobile", "hd", "sd" };
                        int counter = 0;
                        for (final String currentQuality : possibleQualities) {
                            final LinkedHashMap<String, Object> qualitymap = (LinkedHashMap<String, Object>) codecmap.get(currentQuality);
                            if (qualitymap != null) {
                                final String url = (String) qualitymap.get("url");
                                Integer.toString(((Number) qualitymap.get("height")).intValue());
                                final String height = Integer.toString(((Number) qualitymap.get("height")).intValue());
                                final String width = Integer.toString(((Number) qualitymap.get("width")).intValue());
                                String bitrate = null;
                                final Object o_bitrate = qualitymap.get("bitrate");
                                if (o_bitrate != null) {
                                    /* Bitrate is 'null' for vp6 codec */
                                    bitrate = Integer.toString(((Number) o_bitrate).intValue());
                                }
                                String ext = new Regex(url, "(\\.[a-z0-9]{3,4})\\?token2=").getMatch(0);
                                if (ext == null) {
                                    ext = new Regex(url, ".+(\\.[a-z0-9]{3,4})$").getMatch(0);
                                }
                                final String[] result = new String[quality_info_length];
                                results.add(result);
                                result[0] = url;
                                result[1] = ext;
                                result[2] = currentQuality;
                                result[3] = (height == null || width == null ? null : width + "x" + height);
                                result[4] = bitrate;
                                /* No filesize given */
                                result[5] = null;
                                result[6] = codec;
                                /* ID */
                                result[7] = null;
                            }
                            counter++;
                        }
                    }
                }
            }
        }
        return results.toArray(new String[][] {});
    }

    @SuppressWarnings("deprecation")
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
        final String videoFrameSize = downloadLink.getStringProperty("videoFrameSize", "");
        final String videoBitrate = downloadLink.getStringProperty("videoBitrate", "");
        final String videoType = downloadLink.getStringProperty("videoType", null);

        String formattedDate = null;
        if (date != null) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
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
        }
        if (formattedDate != null) {
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        } else {
            formattedFilename = formattedFilename.replace("*date*", "");
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
        if (videoType != null) {
            formattedFilename = formattedFilename.replace("*type*", videoType);
        } else {
            formattedFilename = formattedFilename.replace("*type*", "");
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
        // size
        formattedFilename = formattedFilename.replace("*videoFrameSize*", videoFrameSize);
        // bitrate
        formattedFilename = formattedFilename.replace("*videoBitrate*", videoBitrate);

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
        format = Encoding.unicodeDecode(format);
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

    private String getForcedReferer(final DownloadLink dl) {
        return dl.getStringProperty("vimeo_forced_referer", null);
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

    private final static String defaultCustomFilename = "*videoname*_*quality*_*type**ext*";
    private final static String defaultCustomDate     = "dd.MM.yyyy";

    private void setConfigElements() {
        final ConfigEntry loadbest = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.vimeo.best", "Load Best Version ONLY")).setDefaultValue(false);
        getConfig().addEntry(loadbest);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_ORIGINAL, JDL.L("plugins.hoster.vimeo.loadoriginal", "Load Original Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.vimeo.loadsd", "Load HD Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SD, JDL.L("plugins.hoster.vimeo.loadhd", "Load SD Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MOBILE, JDL.L("plugins.hoster.vimeo.loadmobile", "Load Mobile Version")).setEnabledCondidtion(loadbest, false).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filenames"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.vimeocom.customdate", "Define how the date should look.")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*videoname*_*quality**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.vimeocom.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*videoname* = name of the video without extension\r\n");
        sb.append("*quality* = mobile or sd or hd\r\n");
        sb.append("*videoid* = id of the video\r\n");
        sb.append("*videoFrameSize* = size of video eg. 640x480 (not always available)\r\n");
        sb.append("*videoBitrate* = bitrate of video eg. xxxkbits (not always available)\r\n");
        sb.append("*type* = STREAM or DOWNLOAD\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
    }

}