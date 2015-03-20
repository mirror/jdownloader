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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "http://(www\\.)?flickrdecrypted\\.com/photos/[^<>\"/]+/\\d+" }, flags = { 2 })
public class FlickrCom extends PluginForHost {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://edit.yahoo.com/registration?.src=flickrsignup");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://flickr.com";
    }

    /* Settings */
    private static final String CUSTOM_DATE                      = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME                  = "CUSTOM_FILENAME";
    private static final String CUSTOM_FILENAME_EMPTY_TAG_STRING = "CUSTOM_FILENAME_EMPTY_TAG_STRING";

    private static final String MAINPAGE                         = "http://flickr.com";

    private static Object       LOCK                             = new Object();
    private String              dllink                           = null;
    private static final String intl                             = "us";
    private static final String lang_post                        = "en-US";
    private String              user                             = null;
    private String              id                               = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("https://www.flickr.com/" + new Regex(link.getDownloadURL(), "\\.com/(.+)").getMatch(0));
        try {
            if (link.getContainerUrl() == null) {
                link.setContainerUrl(link.getContentUrl());
            }
            link.setContentUrl(link.getPluginPatternMatcher());
        } catch (Throwable e) {
            // jd09
        }
    }

    /* Max 2000 requests per hour */
    @Override
    public void init() {
        try {
            Browser.setRequestIntervalLimitGlobal(this.getHost(), 3000, 20, 1900);
        } catch (final Throwable t) {
            Browser.setRequestIntervalLimitGlobal(this.getHost(), 1800);
        }
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /**
     * Keep in mind that there is this nice oauth API which might be useful in the future: https://www.flickr.com/services/oembed?url=
     *
     * Other calls of the normal API which might be useful in the future: https://www.flickr.com/services/api/flickr.photos.getInfo.html
     * https://www.flickr.com/services/api/flickr.photos.getSizes.html TODO API: Get correct csrf values so we can make requests as a
     * logged-in user
     * */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        correctDownloadLink(downloadLink);
        id = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        user = new Regex(downloadLink.getDownloadURL(), "flickr\\.com/photos/([^<>\"/]+)/").getMatch(0);
        /* Needed for custom filenames! */
        if (downloadLink.getStringProperty("username", null) == null) {
            downloadLink.setProperty("username", user);
        }
        br.clearCookies(MAINPAGE);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(aa, false, br);
        } else {
            logger.info("No account available, continuing without account...");
        }
        br.setFollowRedirects(true);

        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("div class=\"Four04Case\">") || br.containsHTML(">This member is no longer active on Flickr") || br.containsHTML("class=\"Problem\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().contains("login.yahoo.com/config")) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable via account");
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = getFilename(downloadLink);
        if (filename == null) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable for registered users [Add a flickt account to download such links!]");
            logger.warning("Filename not found, plugin must be broken...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("class=\"videoplayer main\\-photo\"")) {
            /* Last build with old handling: 26451 */
            /*
             * TODO: Add correct API csrf cookie handling so we can use this while being logged in to download videos and do not have to
             * remove the cookies here - that's just a workaround!
             */
            br.clearCookies(MAINPAGE);
            br.getPage(downloadLink.getDownloadURL() + "/in/photostream");
            final String secret = br.getRegex("\"secret\":\"([^<>\"]*)\"").getMatch(0);
            final Browser apibr = br.cloneBrowser();
            // we need to load it before calling!!
            JDUtilities.getPluginForDecrypt("flickr.com");
            final String api_key = jd.plugins.decrypter.FlickrCom.getPublicAPIKey(apibr);
            if (api_key == null || secret == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            apibr.getPage("https://api.flickr.com/services/rest?photo_id=" + id + "&secret=" + secret + "&method=flickr.video.getStreamInfo&csrf=&api_key=" + api_key + "&format=json&hermes=1&hermesClient=1&reqId=&nojsoncallback=1");

            dllink = apibr.getRegex("\"type\":\"orig\",\\s*?\"_content\":\"(https[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
            String videoExt;
            if (dllink.contains("mp4")) {
                videoExt = ".mp4";
            } else {
                videoExt = ".flv";
            }
            filename += videoExt;
            /* Needed for custom filenames! */
            downloadLink.setProperty("ext", videoExt);
        } else {
            br.getPage(downloadLink.getDownloadURL() + "/in/photostream");
            dllink = getFinalLink();
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://farm\\d+\\.(static\\.flickr|staticflickr)\\.com/\\d+/.*?)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String ext = dllink.substring(dllink.lastIndexOf("."));
            if (ext == null || ext.length() > 5) {
                ext = defaultPhotoExt;
            }
            if (!filename.endsWith(ext)) {
                filename = filename + ext;
            }
            /* Needed for custom filenames! */
            downloadLink.setProperty("photo_id", id);
        }
        /* Needed for custom filenames! */
        final String uploadedDate = getJson("datePosted");
        if (uploadedDate != null) {
            downloadLink.setProperty("dateadded", Long.parseLong(uploadedDate) * 1000);
        }
        /* Save it for the getFormattedFilename function. */
        downloadLink.setProperty("decryptedfilename", filename);
        /* TODO: Remove this backwards compatibility in March 2015 */
        downloadLink.setProperty("custom_filenames_allowed", true);
        filename = getFormattedFilename(downloadLink);
        downloadLink.setFinalFileName(filename);
        Browser br2 = br.cloneBrowser();
        /* In case the link redirects to the finallink */
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
                con = br.openHeadConnection(dllink);
            } else {
                con = br.openGetConnection(dllink);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.getURL().contains("login.yahoo.com/config")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, false, br);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) User");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force, final Browser br) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBr(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("cookie_epass") && cookies.containsKey("cookie_accid") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        final Browser brc = br.cloneBrowser();
                        if (isValid(brc)) {
                            return;
                        }
                        /* Clear existing cookies - get ready to do a full login */
                        br.clearCookies(MAINPAGE);
                    }
                }
                final String lang = System.getProperty("user.language");
                br.setFollowRedirects(true);
                br.getPage("https://www.flickr.com/signin/");
                for (int i = 1; i <= 5; i++) {
                    final String _ts = br.getRegex("name=\"_ts\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
                    final String _crumb = br.getRegex("name=\"_crumb\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                    final String _uuid = br.getRegex("name=\"_uuid\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                    String action = br.getRegex("c7: \"(http[^<>\"]*?)\"").getMatch(0);
                    if (_ts == null || _crumb == null || _uuid == null || action == null) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    action = Encoding.htmlDecode(action);
                    // action = "https://login.yahoo.com/config/login";

                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");

                    final String post_data_basic = "countrycode=49&username=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&.persistent=y&signin=&_crumb=" + _crumb + "&_ts=" + _ts + "&_format=json&_uuid=" + _uuid + "&_seqid=2&_loadtpl=1";
                    String post_data = post_data_basic;
                    /* Captcha for/before login */
                    if (action.contains("login_verify2")) {
                        post_data += getLoginCaptchaData(account);
                    }
                    br.postPage(action, post_data);
                    /* Account is valid but captcha input is needed to verify that */
                    if (br.containsHTML("\"code\":\"1213\"")) {
                        post_data += getLoginCaptchaData(account);
                        br.postPage(action, post_data);
                        break;
                    }
                    if (br.containsHTML("<legend>Login Form</legend>")) {
                        continue;
                    }
                    break;
                }
                if (br.containsHTML("\"status\"([\t\n\r]+)?:([\t\n\r]+)?\"error\"")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                String stepForward = br.getRegex("\"url\"[\t\n\r ]*?:[\t\n\r ]*?\"(https?://[^<>\"\\']+)\"").getMatch(0);
                if (stepForward == null) {
                    stepForward = br.getRegex("\"url\":\"(https?[^<>\"\\']+)\"").getMatch(0);
                }
                if (stepForward == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                stepForward = stepForward.replace("\\", "");
                br.getPage(stepForward);
                stepForward = br.getRegex("Please <a href=\"(http://(www\\.)?flickr\\.com/[^<>\"]+)\"").getMatch(0);
                if (stepForward != null) {
                    br.getPage(stepForward);
                }
                if (!isValid(this.br)) {
                    if ("de".equalsIgnoreCase(lang)) {
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

    public static void prepBr(final Browser br) {
        br.setCookie(MAINPAGE, "localization", "en-us%3Bde%3Bde");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    private boolean isValid(final Browser br) throws IOException {
        br.getPage("https://www.flickr.com/");
        if (!br.containsHTML("class=\"welcome")) {
            return false;
        }
        return true;
    }

    private String getLoginCaptchaData(final Account acc) throws Exception, IOException {
        String post_data = "";
        br.getPage("https://login.yahoo.com/captcha/CaptchaWSProxyService.php?action=createlazy&initial_view=&.intl=" + intl + "&.lang=" + lang_post + "&login=" + Encoding.urlEncode(acc.getUser()) + "&rnd=" + System.currentTimeMillis());
        final String captchaLink = br.getRegex("Enter the characters displayed\\&quot; src=\\&quot;(https?://[A-Za-z0-9\\-_\\.]+yahoo\\.com:\\d+/img/[^<>\"]*?)\\&quot;").getMatch(0);
        if (captchaLink == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final DownloadLink dummyLink = new DownloadLink(this, "Account", "flickr.com", "http://flickr.com", true);
        final String c = getCaptchaCode(captchaLink, dummyLink);
        final String valuesText = br.getRegex("\\&lt;div id=\\&quot;captchaV5ControlElements\\&quot;\\&gt;(.*?)\\&lt;audio id=\\&quot;captchaV5Audio\\&quot;").getMatch(0);
        if (valuesText != null) {
            final String[][] data = new Regex(valuesText, "type=\\&quot;hidden\\&quot; name=\\&quot;([^<>\"]*?)\\&quot; id=\\&quot;([^<>\"]*?)\\&quot; value=\\&quot;([^<>\"]*?)\\&quot;").getMatches();
            for (final String[] single_data : data) {
                final String name = single_data[0];
                final String value = single_data[2];
                post_data += "&" + name + "=" + value;
            }
        }
        post_data += "&captchaView=visual&captchaAnswer=" + Encoding.urlEncode(c) + "&.saveC=&.persistent=y";
        return post_data;
    }

    @SuppressWarnings("unused")
    private String createGuid() {
        String a = "";
        final String b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._";
        int c = 0;
        while (c < 22) {
            final int index = (int) Math.floor(Math.random() * b.length());
            a = a + b.substring(index, index + 1);
            c++;
        }
        return a;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String getFinalLink() throws Exception {
        String finallink = null;
        final String[] sizes = { "o", "k", "h", "l", "c", "z", "m", "n", "s", "t", "q", "sq" };
        String picSource;
        picSource = br.getRegex("modelExport: (\\{\"photo\\-models\".*?),[\t\n\r ]+auth: auth,").getMatch(0);
        if (picSource != null) {
            /* json handling */
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(picSource);
            final ArrayList<Object> photo_models = (ArrayList) entries.get("photo-models");
            final LinkedHashMap<String, Object> photo_data = (LinkedHashMap<String, Object>) photo_models.get(0);
            final LinkedHashMap<String, Object> photo_sizes = (LinkedHashMap<String, Object>) photo_data.get("sizes");
            for (final String size : sizes) {
                final LinkedHashMap<String, Object> single_size_map = (LinkedHashMap<String, Object>) photo_sizes.get(size);
                if (single_size_map != null) {
                    finallink = (String) single_size_map.get("url");
                    if (finallink != null) {
                        if (!finallink.startsWith("http")) {
                            finallink = "https:" + finallink;
                        }
                        break;
                    }
                }
            }
        } else {
            /* Site handling */
            br.getPage("https://www.flickr.com/photos/" + user + "/" + id + "/sizes/o");
            picSource = br.getRegex("<ol class=\"sizes\\-list\">(.*?)<div id=\"allsizes\\-photo\">").getMatch(0);
            /*
             * Fast way to get finallink via site as we always try to access the "o" (original) quality, page will redirect us to the max
             * available quality!
             */
            final String maxQuality = new Regex(br.getURL(), "/sizes/([a-z0-9]+)/").getMatch(0);
            if (maxQuality != null) {
                finallink = br.getRegex("<div id=\"allsizes\\-photo\">[\t\n\r ]+<div class=\"spaceball\" style=\"height:\\d+px; width: \\d+px;\"></div>[\t\n\r ]+<img src=\"(http[^<>\"]*?)\">").getMatch(0);
            }
            if (finallink == null) {
                for (final String size : sizes) {
                    finallink = new Regex(picSource, "\"(/photos/[A-Za-z0-9\\-_]+/\\d+/sizes/" + size + "/)\"").getMatch(0);
                    if (finallink != null) {
                        br.getPage("https://www.flickr.com" + finallink);
                        finallink = br.getRegex("id=\"allsizes\\-photo\">[\t\n\r ]+<img src=\"(http[^<>\"]*?)\"").getMatch(0);
                    }
                    if (finallink != null) {
                        break;
                    }
                }
            }

        }
        return finallink;
    }

    @SuppressWarnings("deprecation")
    private String getFilename(final DownloadLink dl) {
        final String linkid = new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
        String filename = dl.getStringProperty("decryptedfilename", null);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"photo\\-title\">(.*?)</h1").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\| Flickr \\- Photo Sharing\\!</title>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<meta name=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
                /* trim */
                while (filename != null) {
                    if (filename.endsWith(".")) {
                        filename = filename.substring(0, filename.length() - 1);
                    } else if (filename.endsWith(" ")) {
                        filename = filename.substring(0, filename.length() - 1);
                    } else {
                        break;
                    }
                }
                filename += "_" + linkid;
            }
            /* Needed for custom filenames! */
            if (dl.getStringProperty("title", null) == null) {
                dl.setProperty("title", filename);
            }
        }
        return filename;
    }

    /* Stable workaround */
    public static long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        String formattedFilename = null;
        if (downloadLink.getBooleanProperty("custom_filenames_allowed", false)) {
            final SubConfiguration cfg = SubConfiguration.getConfig("flickr.com");
            final String customStringForEmptyTags = getCustomStringForEmptyTags();
            final String owner = cfg.getStringProperty("owner", customStringForEmptyTags);

            final String site_title = downloadLink.getStringProperty("title", customStringForEmptyTags);
            final String ext = downloadLink.getStringProperty("ext", defaultPhotoExt);
            final String username = downloadLink.getStringProperty("username", customStringForEmptyTags);
            final String photo_id = downloadLink.getStringProperty("photo_id", customStringForEmptyTags);

            final long date = getLongProperty(downloadLink, "dateadded", 0l);
            String formattedDate = null;
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            Date theDate = new Date(date);
            if (userDefinedDateFormat != null) {
                try {
                    final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    /* prevent user error killing the custom filename function. */
                    formattedDate = defaultCustomStringForEmptyTags;
                }
            }

            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
            if (formattedFilename == null || formattedFilename.equals("")) {
                formattedFilename = defaultCustomFilename;
            }
            formattedFilename = formattedFilename.toLowerCase();
            /* Make sure that the user entered a VALID custom filename - if not, use the default name */
            if (!formattedFilename.contains("*extension*") || (!formattedFilename.contains("*photo_id*") && !formattedFilename.contains("*date*") && !formattedFilename.contains("*username*") && !formattedFilename.contains("*owner*"))) {
                formattedFilename = defaultCustomFilename;
            }

            formattedFilename = formattedFilename.replace("*photo_id*", photo_id);
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
            formattedFilename = formattedFilename.replace("*extension*", ext);
            formattedFilename = formattedFilename.replace("*username*", username);
            formattedFilename = formattedFilename.replace("*owner*", owner);
            formattedFilename = formattedFilename.replace("*title*", site_title);
        } else {
            formattedFilename = downloadLink.getStringProperty("decryptedfilename", null);
        }
        /* Cut filenames if they're too long */
        if (formattedFilename.length() > 180) {
            final String ext = formattedFilename.substring(formattedFilename.lastIndexOf("."));
            int extLength = ext.length();
            formattedFilename = formattedFilename.substring(0, 180 - extLength);
            formattedFilename += ext;
        }
        return formattedFilename;
    }

    public static String getCustomStringForEmptyTags() {
        final SubConfiguration cfg = SubConfiguration.getConfig("flickr.com");
        String emptytag = cfg.getStringProperty(CUSTOM_FILENAME_EMPTY_TAG_STRING, defaultCustomStringForEmptyTags);
        if (emptytag.equals("")) {
            emptytag = defaultCustomStringForEmptyTags;
        }
        return emptytag;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private static final String defaultCustomDate               = "MM-dd-yyyy";
    private static final String defaultCustomFilename           = "*username*_*photo_id*_*title**extension*";
    public final static String  defaultCustomStringForEmptyTags = "-";
    public final static String  defaultPhotoExt                 = ".jpg";

    @Override
    public String getDescription() {
        return "JDownloader's flickr.com Plugin helps downloading media from flickr. Here you can define custom filenames.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.flickrcom.customdate", "Define how the date should look like:")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Filename settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.flickrcom.customfilename", "Custom filename:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sbtags = new StringBuilder();
        sbtags.append("Explanation of the available tags:\r\n");
        sbtags.append("*photo_id* = ID of the photo\r\n");
        sbtags.append("*owner* = Name of the owner of the photo\r\n");
        sbtags.append("*username* = Username taken out of the url\r\n");
        sbtags.append("*date* = ate when the photo was uploaded - custom date format will be used here\r\n");
        sbtags.append("*title* = Title of the photo\r\n");
        sbtags.append("*extension* = Extension of the photo - usually '.jpg'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbtags.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_EMPTY_TAG_STRING, JDL.L("plugins.hoster.flirkrcom.customEmptyTagsString", "Char which will be used for empty tags (e.g. missing data):")).setDefaultValue(defaultCustomStringForEmptyTags));
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}