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
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
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
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imagefap.com" }, urls = { "https?://(www\\.)?imagefap.com/(imagedecrypted/\\d+|video\\.php\\?vid=\\d+)" })
public class ImageFap extends PluginForHost {
    public ImageFap(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        // this.setStartIntervall(500l);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
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

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().contains("imagedecrypted/")) {
            final String photoID = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
            if (photoID != null) {
                final String newurl = "https://www.imagefap.com/photo/" + photoID + "/";
                link.setPluginPatternMatcher(newurl);
                link.setContentUrl(newurl);
            }
        } else if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().contains("/photo/") && !link.hasProperty("photoID")) {
            /* Set Packagizer property */
            final String photoID = new Regex(link.getPluginPatternMatcher(), "/photo/(\\d+)").getMatch(0);
            if (photoID != null) {
                link.setProperty("photoID", Long.parseLong(photoID));
            }
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        String ret = new Regex(link.getPluginPatternMatcher(), "/(?:photo|imagedecrypted)/(\\d+)").getMatch(0);
        if (ret == null) {
            ret = new Regex(link.getPluginPatternMatcher(), "/video\\.php\\?\\vid=(\\d+)").getMatch(0);
        }
        return ret;
    }

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 429 });
        br.setFollowRedirects(true);
        return br;
    }

    private static final String VIDEOLINK = "(?i)https?://[^/]+/video\\.php\\?vid=\\d+";

    private String decryptLink(final String code) {
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

    public static String getGalleryName(final Browser br, final DownloadLink dl, boolean isImageLink) {
        String galleryName = dl != null ? dl.getStringProperty("galleryname", null) : null;
        if (galleryName == null) {
            if (isImageLink) {
                galleryName = br.getRegex("Gallery:\\s*</td>\\s*<td[^>]*\\s*><a[^>]*gid=\\d+\"[^>]*>\\s*(.*?)\\s*<").getMatch(0);
            } else {
                galleryName = br.getRegex("<font[^<>]*?itemprop=\"name\"[^<>]*?>([^<>]+)<").getMatch(0);
                if (galleryName == null) {
                    galleryName = br.getRegex("<title>.*? in gallery ([^<>\"]*?) \\(Picture \\d+\\) uploaded by").getMatch(0);
                    if (galleryName == null) {
                        galleryName = br.getRegex("<title>\\s*Porn pics of\\s*(.*?)\\s*\\(Page 1\\)\\s*</title>").getMatch(0);
                        if (galleryName == null) {
                            galleryName = br.getRegex("<font face=\"verdana\" color=\"white\" size=\"4\"><b>(.*?)</b></font>").getMatch(0);
                            if (galleryName == null) {
                                galleryName = br.getRegex("<meta name=\"description\" content=\"Airplanes porn pics - Imagefap\\.com\\. The ultimate social porn pics site\" />").getMatch(0);
                                if (galleryName == null) {
                                    galleryName = br.getRegex("<font[^<>]*?itemprop=\"name\"[^<>]*?>([^<>]+)<").getMatch(0);
                                    if (galleryName == null) {
                                        galleryName = br.getRegex("<title>\\s*(.*?)\\s*(Porn Pics (&amp;|&) Porn GIFs)?\\s*</title>").getMatch(0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return galleryName;
    }

    public static String getGalleryID(final Browser br, final DownloadLink dl, boolean isImageLink) {
        String ret = dl != null ? dl.getStringProperty("galleryID", null) : null;
        if (ret == null) {
            if (isImageLink) {
                ret = br.getRegex("Gallery:\\s*</td>\\s*<td[^>]*\\s*><a[^>]*gid=(\\d+)\"[^>]*>").getMatch(0);
            }
        }
        return ret;
    }

    public static String getOrderID(final Browser br, final DownloadLink dl, boolean isImageLink) {
        String ret = dl != null ? dl.getStringProperty("orderid", null) : null;
        if (ret == null) {
            if (isImageLink) {
                final String current = br.getRegex("_start_img\\s*=\\s*(\\d+)").getMatch(0);
                if (current != null) {
                    // final String max = br.getRegex("_pics\\s*=\\s*(\\d+)").getMatch(0);
                    DecimalFormat df = new DecimalFormat("0000");
                    ret = df.format(Integer.parseInt(current) + 1);
                }
            }
        }
        return ret;
    }

    public static String getUserName(final Browser br, final DownloadLink dl, boolean isImageLink) {
        String username = dl != null ? dl.getStringProperty("directusername", null) : null;
        if (username == null) {
            username = br.getRegex("<b><font size=\"3\" color=\"#CC0000\">Uploaded by ([^<>\"]+)</font></b>").getMatch(0);
            if (username == null) {
                username = br.getRegex("<b><font size=\"4\" color=\"#CC0000\">(.*?)\\'s gallery</font></b>").getMatch(0);
                if (username == null) {
                    username = br.getRegex("<td class=\"mnu0\"><a href=\"https?://(www\\.)?imagefap\\.com/profile\\.php\\?user=([^<>\"]+)\"").getMatch(0);
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
            }
        }
        if (username == null) {
            username = "Anonymous";
        }
        return username;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        prepBR(this.br);
        loadSessionCookies(this.br, this.getHost());
        try {
            getRequest(this, this.br, br.createGetRequest(link.getPluginPatternMatcher()));
        } catch (PluginException e) {
            if (!(Thread.currentThread() instanceof SingleDownloadController) && e.getLinkStatus() == LinkStatus.ERROR_IP_BLOCKED) {
                logger.log(e);
                return AvailableStatus.UNCHECKED;
            } else {
                throw e;
            }
        }
        if (link.getPluginPatternMatcher().matches(VIDEOLINK)) {
            link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.FLV);
            /*
             * 2021-05-05: Offline videos can't be easily recognized by html code e.g.: https://www.imagefap.com/video.php?vid=999999999999
             */
            if (br.containsHTML("<td>\\s*Duration:\\s*</td>\\s*<td id=\"vid_duration\">00:00</td>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!br.containsHTML("<td>\\s*Duration:\\s*</td>\\s*<td id=\"vid_duration\">[^<]*</td>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String filename = br.getRegex(">Title:</td>\\s*<td width=35%>([^<>\"]*?)</td>").getMatch(0);
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
            final String pictureTitle = br.getRegex("<title>\\s*([^<]+)\\s*(in gallery|uploaded by|Porn Pic)").getMatch(0);
            if (StringUtils.isNotEmpty(pictureTitle)) {
                link.setProperty("original_filename", pictureTitle);
                link.removeProperty("incomplete_filename");
            }
            String galleryName = ImageFap.getGalleryName(br, link, true);
            String username = ImageFap.getUserName(br, link, true);
            final String orderID = getOrderID(br, link, true);
            if (orderID != null && !link.hasProperty("orderid")) {
                link.setProperty("orderid", orderID);
            }
            final String galleryID = getGalleryID(br, link, true);
            if (galleryID != null && !link.hasProperty("galleryID")) {
                link.setProperty("galleryID", galleryID);
            }
            if (StringUtils.isEmpty(galleryName) || StringUtils.isEmpty(pictureTitle)) {
                logger.info("Possibly missing data: galleryName: " + galleryName + " picture_name: " + pictureTitle);
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (galleryName != null && !link.hasProperty("galleryname")) {
                galleryName = Encoding.htmlDecode(galleryName);
                galleryName = galleryName.trim();
                link.setProperty("galleryname", galleryName);
            }
            if (username != null && !link.hasProperty("directusername")) {
                username = Encoding.htmlDecode(username);
                username = username.trim();
                link.setProperty("directusername", username);
            }
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

    private final String findFinalLink(final Browser br, final DownloadLink link) throws Exception {
        if (link.getPluginPatternMatcher().matches(VIDEOLINK)) {
            String configLink = br.getRegex("flashvars\\.config = escape\\(\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (configLink == null) {
                /* 2020-03-23 */
                configLink = br.getRegex("url\\s*:\\s*'(https?[^<>\"\\']+)\\'").getMatch(0);
                if (configLink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            int retry = 5;
            final Set<String> testedServerQualities = new HashSet<String>();
            final Map<Integer, String> qualities = new HashMap<Integer, String>();
            Integer bestQuality = null;
            while (retry-- >= 0) {
                final Browser brc = br.cloneBrowser();
                logger.info("Try VideoConfig, round:" + retry);
                getRequest(this, brc, brc.createGetRequest(configLink));
                final String videoLinks[] = brc.getRegex("<videoLink>(https?://[^<>\"]*?)</videoLink>").getColumn(0);
                if (videoLinks == null || videoLinks.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final String videoLinksEntry : videoLinks) {
                    final String quality = new Regex(videoLinksEntry, "-(\\d+)p\\.(mp4|flv|webm)").getMatch(0);
                    final Integer p = quality != null ? Integer.parseInt(quality) : -1;
                    final String url = Encoding.htmlDecode(videoLinksEntry);
                    URLConnectionAdapter con = null;
                    try {
                        final String testServerQuality = new URL(url).getHost() + p;
                        if (testedServerQualities.add(testServerQuality)) {
                            final Browser brc2 = br.cloneBrowser();
                            con = brc2.openHeadConnection(url);
                            if (looksLikeDownloadableContent(con)) {
                                qualities.put(p, url);
                                if (bestQuality == null || p > bestQuality) {
                                    bestQuality = p;
                                }
                            } else {
                                brc.followConnection(true);
                            }
                        }
                    } catch (final IOException e) {
                        logger.log(e);
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }
                if (isAbort()) {
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else if (bestQuality == null) {
                    sleep(2000, link);
                } else {
                    final String videoLink = qualities.get(bestQuality);
                    logger.info("VideoConfig result:" + bestQuality + "->" + videoLink);
                    return videoLink;
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final String fid = getFID(link);
            String imageLink = br.getRegex("original\\s*=\\s*\"([^\"]*)\"[^<>]*imageid\\s*=\\s*\"" + Pattern.quote(fid) + "\"").getMatch(0);
            if (imageLink == null) {
                imageLink = br.getRegex("name=\"mainPhoto\"[^>]*src\\s*=\\s*\"(https?://[^<>\"]+)\"").getMatch(0);
                if (imageLink == null) {
                    final String returnID = new Regex(br, Pattern.compile("return lD\\(\\'(\\S+?)\\'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0);
                    if (returnID != null) {
                        imageLink = decryptLink(returnID);
                    }
                    if (imageLink == null) {
                        imageLink = br.getRegex("onclick=\"OnPhotoClick\\(\\);\" src\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
                        if (imageLink == null) {
                            imageLink = br.getRegex("href=\"#\" onclick\\s*=\\s*\"javascript:window\\.open\\(\\'(https?://.*?)\\'\\)").getMatch(0);
                            if (imageLink == null) {
                                imageLink = br.getRegex("\"contentUrl\"\\s*>\\s*(https?://cdn\\.imagefap\\.com/images/full/.*?)\\s*<").getMatch(0);
                            }
                        }
                    }
                }
            }
            if (imageLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                return imageLink;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        // TODO: maybe add cache/reuse
        final String finalLink = findFinalLink(br, link);
        if (link.getDownloadURL().matches(VIDEOLINK)) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, true, 0);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else {
            // Only set subdirectory if it wasn't set before or we'll get
            // subfolders
            // in subfolders which is bad
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, false, 1);
            final long t = dl.getConnection().getContentLength();
            if (dl.getConnection().getResponseCode() == 404 || (t != -1 && t < 107)) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String expectedExtension = getExtensionFromMimeType(dl.getConnection().getContentType());
            if (expectedExtension != null && link.getName() != null) {
                final String correctedFilename = this.correctOrApplyFileNameExtension(link.getName(), "." + expectedExtension);
                link.setFinalFileName(correctedFilename);
            }
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
            } else if (isCaptchaRateLimited(br)) {
                /*
                 * 2020-10-14: Captcha required. Solving it will remove the rate limit FOR THIS BROWSER SESSION! All other browser sessions
                 * (including new sessions) with the current IP will still be rate-limited until one captcha is solved.
                 */
                if (SubConfiguration.getConfig("imagefap.com").getBooleanProperty(FORCE_RECONNECT_ON_RATELIMIT, defaultFORCE_RECONNECT_ON_RATELIMIT)) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Rate limit reached user prefers reconnect over captcha solving", 5 * 60 * 1000l);
                }
                final Form captchaform = br.getForm(0);
                final String captchaurl = br.getRegex("(/captcha\\.php|/captcha)").getMatch(0);
                if (captchaform == null || captchaurl == null || !captchaform.hasInputFieldByName("captcha")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Rate limit reached and failed to handle captcha", 5 * 60 * 1000l);
                }
                final String code;
                try {
                    if (plugin instanceof PluginForDecrypt) {
                        final PluginForDecrypt pluginForDecrypt = (PluginForDecrypt) plugin;
                        code = ReflectionUtils.invoke(plugin.getClass(), "getCaptchaCode", plugin, String.class, captchaurl, pluginForDecrypt.getCurrentLink());
                    } else {
                        final PluginForHost pluginForHost = (PluginForHost) plugin;
                        code = ReflectionUtils.invoke(plugin.getClass(), "getCaptchaCode", plugin, String.class, captchaurl, pluginForHost.getDownloadLink());
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
                if (isCaptchaRateLimited(br)) {
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

    public static boolean isCaptchaRateLimited(final Browser br) {
        if (StringUtils.containsIgnoreCase(br.getURL(), "rl_captcha.php") || StringUtils.containsIgnoreCase(br.getURL(), "/human-verification")) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns either the original server filename or one that is very similar to the original */
    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink link) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("imagefap.com");
        final String username = link.getStringProperty("directusername", "-");
        String filename = link.getStringProperty("original_filename", null);
        if (filename == null) {
            filename = link.getStringProperty("incomplete_filename", "unknown");
        }
        final String galleryname = link.getStringProperty("galleryname", "");
        final String orderid = link.getStringProperty("orderid", "-");
        final long galleryID = link.getLongProperty("galleryID", -1);
        final long photoID = link.getLongProperty("photoID", -1);
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
        formattedFilename = formattedFilename.replace("*galleryID*", galleryID == -1 ? "" : String.valueOf(galleryID));
        formattedFilename = formattedFilename.replace("*photoID*", photoID == -1 ? "" : String.valueOf(photoID));
        formattedFilename = formattedFilename.replace("*galleryname*", galleryname);
        formattedFilename = formattedFilename.replace("*title*", filename);
        return formattedFilename;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
        {
            put("SETTING_FORCE_RECONNECT_ON_RATELIMIT", "Reconnect if rate limit is reached and captcha is required?");
            put("LABEL_FILENAME", "Define custom filename for pictures:");
            put("SETTING_TAGS", "Explanation of the available tags:\r\n*username* = Name of the user who posted the content\r\n*title* = Original title of the picture including file extension\r\n*galleryname* = Name of the gallery in which the picture is listed\r\n*orderid* = Position of the picture in a gallery e.g. '0001'\r\n*photoID* = id of the image\r\n*galleryID* = id of the gallery");
        }
    };
    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
        {
            put("SETTING_FORCE_RECONNECT_ON_RATELIMIT", "Führe einen Reconnect durch, wenn das Rate-Limit erreicht ist und ein Captcha benötigt wird?");
            put("LABEL_FILENAME", "Gib das Muster des benutzerdefinierten Dateinamens für Bilder an:");
            put("SETTING_TAGS", "Erklärung der verfügbaren Tags:\r\n*username* = Name des Benutzers, der den Inhalt veröffentlicht hat \r\n*title* = Originaler Dateiname mitsamt Dateiendung\r\n*galleryname* = Name der Gallerie, in der sich das Bild befand\r\n*orderid* = Position des Bildes in einer Gallerie z.B. '0001'\r\n*photoID* = id des Bildes\r\n*galleryID* = id der Gallery");
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