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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imagefap.com" }, urls = { "https?://(www\\.)?imagefap.com/(imagedecrypted/\\d+|video\\.php\\?vid=\\d+)" })
public class ImageFap extends PluginForHost {
    public ImageFap(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        // this.setStartIntervall(500l);
    }

    private static final String               CUSTOM_FILENAME              = "CUSTOM_FILENAME";
    private static final String               FORCE_RECONNECT_ON_RATELIMIT = "FORCE_RECONNECT_ON_RATELIMIT";
    protected static Object                   LOCK                         = new Object();
    protected static HashMap<String, Cookies> sessionCookies               = new HashMap<String, Cookies>();

    private void loadSessionCookies(final Browser prepBr, final String host) {
        synchronized (sessionCookies) {
            if (!sessionCookies.isEmpty()) {
                for (final Map.Entry<String, Cookies> cookieEntry : sessionCookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    if (key != null && key.equals(host)) {
                        try {
                            prepBr.setCookies(key, cookieEntry.getValue(), false);
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
    }

    public void correctDownloadLink(DownloadLink link) {
        final String addedLink = link.getDownloadURL();
        if (addedLink.contains("imagedecrypted/")) {
            final String photoID = new Regex(addedLink, "(\\d+)$").getMatch(0);
            if (photoID != null) {
                final String newurl = "https://www.imagefap.com/photo/" + photoID + "/";
                link.setProperty("photoID", Long.parseLong(photoID));
                link.setUrlDownload(newurl);
                link.setContentUrl(newurl);
            }
        } else if (addedLink.contains("/photo/") && !link.hasProperty("photoID")) {
            final String photoID = new Regex(addedLink, "/photo/(\\d+)").getMatch(0);
            if (photoID != null) {
                link.setProperty("photoID", Long.parseLong(photoID));
            }
        }
    }

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 429 });
        br.setFollowRedirects(true);
        return br;
    }

    private static final String VIDEOLINK = "https?://(www\\.)?imagefap.com/video\\.php\\?vid=\\d+";

    private String DecryptLink(final String code) {
        try {
            final String s1 = Encoding.htmlDecode(code.substring(0, code.length() - 1));
            String t = "";
            for (int i = 0; i < s1.length(); i++) {
                // logger.info("decrypt4 " + i);
                // logger.info("decrypt5 " + ((int) (s1.charAt(i+1) - '0')));
                // logger.info("decrypt6 " +
                // (Integer.parseInt(code.substring(code.length()-1,code.length()
                // ))));
                final int charcode = s1.charAt(i) - Integer.parseInt(code.substring(code.length() - 1, code.length()));
                // logger.info("decrypt7 " + charcode);
                t = t + Character.valueOf((char) charcode).toString();
                // t+=new Character((char)
                // (s1.charAt(i)-code.charAt(code.length()-1)));
            }
            // logger.info(t);
            // var s1=unescape(s.substr(0,s.length-1)); var t='';
            // for(i=0;i<s1.length;i++)t+=String.fromCharCode(s1.charCodeAt(i)-s.
            // substr(s.length-1,1));
            // return unescape(t);
            // logger.info("return of DecryptLink(): " +
            // JDUtilities.htmlDecode(t));
            return Encoding.htmlDecode(t);
        } catch (final Exception e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String getAGBLink() {
        return "http://imagefap.com/faq.php";
    }

    private String getGalleryName(final DownloadLink dl) {
        String galleryName = dl.getStringProperty("galleryname");
        if (galleryName == null) {
            // galleryName = br.getRegex("<font face=verdana size=3>([^<>\"]*?)<BR>").getMatch(0);
            galleryName = br.getRegex("<font[^<>]*?itemprop=\"name\"[^<>]*?>([^<>]+)<").getMatch(0);
            if (galleryName == null) {
                galleryName = br.getRegex("<title>.*? in gallery ([^<>\"]*?) \\(Picture \\d+\\) uploaded by").getMatch(0);
            }
        }
        return galleryName;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        prepBR(this.br);
        loadSessionCookies(this.br, this.getHost());
        getRequest(this, this.br, br.createGetRequest(link.getDownloadURL()));
        if (link.getDownloadURL().matches(VIDEOLINK)) {
            final String filename = br.getRegex(">Title:</td>[\t\n\r ]+<td width=35%>([^<>\"]*?)</td>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
        } else {
            /* 2020-10-14: TODO: What was this for?? */
            // final String location = br.getRedirectLocation();
            // if (location != null) {
            // if (!location.contains("/photo/")) {
            // getPage(this.br, location);
            // }
            // getPage(this.br, location);
            // }
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(>The image you are trying to access does not exist|<title> \\(Picture 1\\) uploaded by  on ImageFap\\.com</title>)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String picture_name = link.getStringProperty("original_filename");
            if (picture_name == null) {
                picture_name = br.getRegex("<title>(.*?) in gallery").getMatch(0);
                if (picture_name == null) {
                    picture_name = br.getRegex("<title>(.*?) uploaded by").getMatch(0);
                    if (picture_name == null) {
                        picture_name = br.getRegex("<title>(.*?) Porn Pic").getMatch(0);
                    }
                }
            }
            String galleryName = getGalleryName(link);
            String username = link.getStringProperty("directusername");
            if (username == null) {
                username = br.getRegex("<b><font size=\"4\" color=\"#CC0000\">(.*?)\\'s gallery</font></b>").getMatch(0);
                if (username == null) {
                    username = br.getRegex("<td class=\"mnu0\"><a href=\"/profile\\.php\\?user=(.*?)\"").getMatch(0);
                    if (username == null) {
                        username = br.getRegex("jQuery\\.BlockWidget\\(\\d+,\"(.*?)\",\"left\"\\);").getMatch(0);
                        if (username == null) {
                            username = br.getRegex("Uploaded by ([^<>\"]+)</font>").getMatch(0);
                        }
                    }
                }
            }
            if (galleryName == null || picture_name == null) {
                logger.info("galleryName: " + galleryName + " picture_name: " + picture_name);
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            galleryName = Encoding.htmlDecode(galleryName).trim();
            if (username != null) {
                username = username.trim();
            }
            link.setProperty("galleryname", galleryName);
            link.setProperty("directusername", username);
            link.setProperty("original_filename", picture_name);
            link.setFinalFileName(getFormattedFilename(link));
            /* Only set filepackage if not set yet */
            try {
                if (FilePackage.isDefaultFilePackage(link.getFilePackage())) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(username + " - " + galleryName);
                    fp.add(link);
                }
            } catch (final Throwable e) {
                /*
                 * does not work in stable 0.9580, can be removed with next major update
                 */
                try {
                    if (link.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(username + " - " + galleryName);
                        fp.add(link);
                    }
                } catch (final Throwable e2) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        String pfilename = link.getName();
        Request request = getRequest(this, this.br, this.br.createGetRequest(link.getDownloadURL()));
        if (link.getDownloadURL().matches(VIDEOLINK)) {
            String configLink = request.getRegex("flashvars\\.config = escape\\(\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (configLink == null) {
                /* 2020-03-23 */
                configLink = request.getRegex("url\\s*:\\s*'(http[^<>\"\\']+)\\'").getMatch(0);
            }
            if (configLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            request = getRequest(this, this.br, this.br.createGetRequest(configLink));
            String finallink = request.getRegex("<videoLink>(https?://[^<>\"]*?)</videoLink>").getMatch(0);
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (Encoding.isHtmlEntityCoded(finallink)) {
                finallink = Encoding.htmlDecode(finallink);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink, true, 0);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            String imagelink = br.getRegex("name=\"mainPhoto\".*src=\"(https?://[a-z0-9\\.\\-]+\\.imagefapusercontent\\.com/[^<>\"]+)\"").getMatch(0);
            // if (imagelink == null) {
            // String ID = new Regex(downloadLink.getDownloadURL(), "(\\d+)").getMatch(0);
            // imagelink = br.getRegex("href=\"http://img\\.imagefapusercontent\\.com/images/full/\\d+/\\d+/" + ID +
            // "\\.jpe?g\" original=\"(http://fap.to/images/full/\\d+/\\d+/" + ID + "\\.jpe?g)\"").getMatch(0);
            // }
            if (imagelink == null) {
                final String returnID = new Regex(br, Pattern.compile("return lD\\(\\'(\\S+?)\\'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (returnID != null) {
                    imagelink = DecryptLink(returnID);
                }
                if (imagelink == null) {
                    imagelink = br.getRegex("onclick=\"OnPhotoClick\\(\\);\" src=\"(https?://.*?)\"").getMatch(0);
                    if (imagelink == null) {
                        imagelink = br.getRegex("href=\"#\" onclick=\"javascript:window\\.open\\(\\'(https?://.*?)\\'\\)").getMatch(0);
                        if (imagelink == null) {
                            imagelink = br.getRegex("\"contentUrl\"\\s*>\\s*(https?://cdn\\.imagefap\\.com/images/full/.*?)\\s*<").getMatch(0);
                        }
                    }
                }
            }
            if (imagelink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // Only set subdirectory if it wasn't set before or we'll get
            // subfolders
            // in subfolders which is bad
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, imagelink, false, 1);
            final long t = dl.getConnection().getContentLength();
            if (dl.getConnection().getResponseCode() == 404 || (t != -1 && t < 107)) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!pfilename.endsWith(new Regex(imagelink, "(\\.[A-Za-z0-9]+)($|\\?)").getMatch(0))) {
                pfilename += new Regex(imagelink, "(\\.[A-Za-z0-9]+)($|\\?)").getMatch(0);
            }
            link.setFinalFileName(pfilename);
        }
        dl.startDownload();
    }

    @Override
    public void init() {
        try {
            // Browser.setRequestIntervalLimitGlobal(getHost(), 750, 100, 60000);
            Browser.setRequestIntervalLimitGlobal(getHost(), 750);
        } catch (final Throwable e) {
        }
    }

    public static void main(String[] args) {
        System.out.println(ImageFap.class.getName());
    }

    public static Request getRequest(Plugin plugin, final Browser br, Request request) throws Exception {
        synchronized (LOCK) {
            br.getPage(request);
            if (br.getHttpConnection().getResponseCode() == 429) {
                /*
                 *
                 * 100 requests per 1 min 200 requests per 5 min 1000 requests per 1 hour
                 */
                /* 2020-09-22: Most likely they will allow a retry after one hour. */
                final String waitSecondsStr = br.getRequest().getResponseHeader("Retry-After");
                if (waitSecondsStr != null && waitSecondsStr.matches("^\\s*\\d+\\s*$")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Error 429 rate limit reached", Integer.parseInt(waitSecondsStr.trim()) * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Error 429 rate limit reached", 5 * 60 * 1000l);
                }
            } else if (StringUtils.containsIgnoreCase(br.getURL(), "rl_captcha.php")) {
                /*
                 * 2020-10-14: Captcha required. Solving it will remove the rate limit FOR THIS BROWSER SESSION! All other browser sessions
                 * (including new sessions) with the current IP will still be rate-limited until one captcha is solved.
                 */
                final Form captchaform = br.getForm(0);
                if (captchaform == null || !br.containsHTML("/captcha\\.php") || !captchaform.hasInputFieldByName("captcha")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Rate limit reached and failed to handle captcha", 5 * 60 * 1000l);
                }
                if (SubConfiguration.getConfig("imagefap.com").getBooleanProperty(FORCE_RECONNECT_ON_RATELIMIT, defaultFORCE_RECONNECT_ON_RATELIMIT)) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Rate limit reached user prefers reconnect over captcha solving", 5 * 60 * 1000l);
                }
                final String code;
                try {
                    if (plugin instanceof PluginForDecrypt) {
                        final PluginForDecrypt pluginForDecrypt = (PluginForDecrypt) plugin;
                        code = ReflectionUtils.invoke(plugin.getClass().getName(), "getCaptchaCode", plugin, String.class, "/captcha.php", pluginForDecrypt.getCurrentLink());
                    } else {
                        final PluginForHost pluginForHost = (PluginForHost) plugin;
                        code = ReflectionUtils.invoke(plugin.getClass().getName(), "getCaptchaCode", plugin, String.class, "/captcha.php", pluginForHost.getDownloadLink());
                    }
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() instanceof Exception) {
                        throw (Exception) e.getTargetException();
                    } else {
                        throw e;
                    }
                }
                captchaform.put("captcha", Encoding.urlEncode(code));
                br.submitForm(captchaform);
                br.followRedirect(true);
                if (StringUtils.containsIgnoreCase(br.getURL(), "rl_captcha.php")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Rate limit reached and remained after captcha", 5 * 60 * 1000l);
                } else {
                    synchronized (sessionCookies) {
                        sessionCookies.put(br.getHost(), br.getCookies(br.getHost()));
                    }
                }
            }
            return br.getRequest();
        }
    }

    /** Returns either the original server filename or one that is very similar to the original */
    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink link) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("imagefap.com");
        final String username = link.getStringProperty("directusername", "-");
        final String original_filename = link.getStringProperty("original_filename", null);
        final String galleryname = link.getStringProperty("galleryname", null);
        final String orderid = link.getStringProperty("orderid", "-");
        /* Date: Maybe add this in the future, if requested by a user. */
        // final long date = getLongProperty(downloadLink, "originaldate", 0l);
        // String formattedDate = null;
        // /* Get correctly formatted date */
        // String dateFormat = "yyyy-MM-dd";
        // SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        // Date theDate = new Date(date);
        // try {
        // formatter = new SimpleDateFormat(dateFormat);
        // formattedDate = formatter.format(theDate);
        // } catch (Exception e) {
        // /* prevent user error killing plugin */
        // formattedDate = "";
        // }
        // /* Get correctly formatted time */
        // dateFormat = "HHmm";
        // String time = "0000";
        // try {
        // formatter = new SimpleDateFormat(dateFormat);
        // time = formatter.format(theDate);
        // } catch (Exception e) {
        // /* prevent user error killing plugin */
        // time = "0000";
        // }
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (!formattedFilename.contains("*username*") && !formattedFilename.contains("*title*") && !formattedFilename.contains("*galleryname*")) {
            formattedFilename = defaultCustomFilename;
        }
        formattedFilename = formattedFilename.replace("*orderid*", orderid);
        formattedFilename = formattedFilename.replace("*username*", username);
        formattedFilename = formattedFilename.replace("*galleryname*", galleryname);
        formattedFilename = formattedFilename.replace("*title*", original_filename);
        return formattedFilename;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
        {
            put("SETTING_FORCE_RECONNECT_ON_RATELIMIT", "Reconnect if rate limit is reached and captcha is required?");
            put("LABEL_FILENAME", "Define custom filename for pictures:");
            put("SETTING_TAGS", "Explanation of the available tags:\r\n*username* = Name of the user who posted the content\r\n*title* = Original title of the picture including file extension\r\n*galleryname* = Name of the gallery in which the picture is listed\r\n*orderid* = Position of the picture in a gallery e.g. '0001'");
        }
    };
    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
        {
            put("SETTING_FORCE_RECONNECT_ON_RATELIMIT", "Führe einen Reconnect durch, wenn das Rate-Limit erreicht ist und ein Captcha benötigt wird?");
            put("LABEL_FILENAME", "Gib das Muster des benutzerdefinierten Dateinamens für Bilder an:");
            put("SETTING_TAGS", "Erklärung der verfügbaren Tags:\r\n*username* = Name des Benutzers, der den Inhalt veröffentlicht hat \r\n*title* = Originaler Dateiname mitsamt Dateiendung\r\n*galleryname* = Name der Gallerie, in der sich das Bild befand\r\n*orderid* = Position des Bildes in einer Gallerie z.B. '0001'");
        }
    };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    @Override
    public String getDescription() {
        return "JDownloader's imagefap.com plugin helps downloading videos and images from ImageFap. JDownloader provides settings for custom filenames.";
    }

    private static final String  defaultCustomFilename               = "*username* - *galleryname* - *orderid**title*";
    private static final boolean defaultFORCE_RECONNECT_ON_RATELIMIT = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FORCE_RECONNECT_ON_RATELIMIT, getPhrase("SETTING_FORCE_RECONNECT_ON_RATELIMIT")).setDefaultValue(defaultFORCE_RECONNECT_ON_RATELIMIT));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, getPhrase("LABEL_FILENAME")).setDefaultValue(defaultCustomFilename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_TAGS")));
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
}