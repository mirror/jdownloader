//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.SimpleFTP;
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
import jd.plugins.components.UserAgents.BrowserName;
import jd.utils.locale.JDL;

//Links are coming from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://vkontaktedecrypted\\.ru/(picturelink/(?:\\-)?\\d+_\\d+(\\?tag=[\\d\\-]+)?|audiolink/(?:\\-)?\\d+_\\d+|videolink/[\\d\\-]+)|https?://(?:new\\.)?vk\\.com/doc[\\d\\-]+_[\\d\\-]+(\\?hash=[a-z0-9]+)?|https?://(?:c|p)s[a-z0-9\\-]+\\.(?:vk\\.com|userapi\\.com|vk\\.me|vkuservideo\\.net)/[^<>\"]+\\.(?:mp[34]|(?:rar|zip).+|[rz][0-9]{2}.+)" })
public class VKontakteRuHoster extends PluginForHost {
    private static final String DOMAIN                                          = "vk.com";
    private static final String TYPE_AUDIOLINK                                  = "http://vkontaktedecrypted\\.ru/audiolink/((?:\\-)?\\d+)_(\\d+)";
    private static final String TYPE_VIDEOLINK                                  = "http://vkontaktedecrypted\\.ru/videolink/[\\d\\-]+";
    private static final String TYPE_DIRECT                                     = "https?://(?:c|p)s[a-z0-9\\-]+\\.(?:vk\\.com|userapi\\.com|vk\\.me|vkuservideo\\.net)/[^<>\"]+\\.(?:[A-Za-z0-9]{1,5})(?:.*)";
    private static final String TYPE_PICTURELINK                                = "http://vkontaktedecrypted\\.ru/picturelink/((?:\\-)?\\d+)_(\\d+)(\\?tag=[\\d\\-]+)?";
    private static final String TYPE_DOCLINK                                    = "https?://(?:new\\.)?vk\\.com/doc[\\d\\-]+_\\d+(\\?hash=[a-z0-9]+)?";
    public static final long    trust_cookie_age                                = 300000l;
    private static final String TEMPORARILYBLOCKED                              = jd.plugins.decrypter.VKontakteRu.TEMPORARILYBLOCKED;
    /* Settings stuff */
    private static final String FASTLINKCHECK_VIDEO                             = "FASTLINKCHECK_VIDEO";
    private static final String FASTLINKCHECK_PICTURES                          = "FASTLINKCHECK_PICTURES_V2";
    private static final String FASTLINKCHECK_AUDIO                             = "FASTLINKCHECK_AUDIO";
    private static final String ALLOW_BEST                                      = "ALLOW_BEST";
    private static final String ALLOW_240P                                      = "ALLOW_240P";
    private static final String ALLOW_360P                                      = "ALLOW_360P";
    private static final String ALLOW_480P                                      = "ALLOW_480P";
    private static final String ALLOW_720P                                      = "ALLOW_720P";
    private static final String ALLOW_1080P                                     = "ALLOW_1080P";
    private static final String VKWALL_GRAB_ALBUMS                              = "VKWALL_GRAB_ALBUMS";
    private static final String VKWALL_GRAB_PHOTOS                              = "VKWALL_GRAB_PHOTOS";
    private static final String VKWALL_GRAB_AUDIO                               = "VKWALL_GRAB_AUDIO";
    private static final String VKWALL_GRAB_VIDEO                               = "VKWALL_GRAB_VIDEO";
    private static final String VKWALL_GRAB_URLS                                = "VKWALL_GRAB_URLS";
    public static final String  VKWALL_GRAB_DOCS                                = "VKWALL_GRAB_DOCS";
    public static final String  VKWALL_GRAB_URLS_INSIDE_POSTS                   = "VKWALL_GRAB_URLS_INSIDE_POSTS";
    public static final String  VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX             = "VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX";
    public static final String  VKWALL_GRAB_COMMENTS_PHOTOS                     = "VKWALL_GRAB_COMMENTS_PHOTOS";
    public static final String  VKWALL_GRAB_COMMENTS_AUDIO                      = "VKWALL_GRAB_COMMENTS_AUDIO";
    public static final String  VKWALL_GRAB_COMMENTS_VIDEO                      = "VKWALL_GRAB_COMMENTS_VIDEO";
    public static final String  VKWALL_GRAB_COMMENTS_URLS                       = "VKWALL_GRAB_COMMENTS_URLS";
    private static final String VKVIDEO_USEIDASPACKAGENAME                      = "VKVIDEO_USEIDASPACKAGENAME";
    private static final String VKAUDIOS_USEIDASPACKAGENAME                     = "VKAUDIOS_USEIDASPACKAGENAME";
    private static final String VKDOCS_USEIDASPACKAGENAME                       = "VKDOCS_USEIDASPACKAGENAME";
    private static final String VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME = "VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME";
    private static final String VKPHOTO_CORRECT_FINAL_LINKS                     = "VKPHOTO_CORRECT_FINAL_LINKS";
    public static final String  VKWALL_USE_API                                  = "VKWALL_USE_API";
    public static final String  VKADVANCED_USER_AGENT                           = "VKADVANCED_USER_AGENT";
    /* html patterns */
    public static final String  HTML_VIDEO_NO_ACCESS                            = "NO_ACCESS";
    public static final String  HTML_VIDEO_REMOVED_FROM_PUBLIC_ACCESS           = "This video has been removed from public access";
    private final boolean       docs_add_unique_id                              = true;
    public static Object        LOCK                                            = new Object();
    private String              finalUrl                                        = null;
    private String              ownerID                                         = null;
    private String              contentID                                       = null;
    private String              mainlink                                        = null;
    private static final String ALPHANUMERIC                                    = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN0PQRSTUVWXYZO123456789+/=";
    private String              vkID                                            = null;

    public VKontakteRuHoster(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
        // need this twice, because hoster plugin might not be loaded yet
        try {
            Browser.setRequestIntervalLimitGlobal("vk.com", 500, 15, 30000);
        } catch (final Throwable e) {
        }
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void onPluginAssigned(DownloadLink link) throws Exception {
        if (link != null) {
            link.removeProperty("directlinks");// remove, never used but requires a lot of memory
        }
    }

    @Override
    public CrawledLink convert(DownloadLink link) {
        final CrawledLink ret = super.convert(link);
        final String url = link.getDownloadURL();
        if (url != null && url.matches(TYPE_DIRECT)) {
            final String filename = extractFileNameFromURL(url);
            if (filename != null) {
                try {
                    final String urlDecoded = SimpleFTP.BestEncodingGuessingURLDecode(filename);
                    link.setFinalFileName(urlDecoded);
                } catch (final Throwable e) {
                    link.setName(filename);
                }
            }
        }
        return ret;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        // nullify previous
        br = new Browser();
        dl = null;
        finalUrl = null;
        // Initialise
        int checkstatus = 0;
        String filename = null;
        // setters
        prepBrowser(br, false);
        setConstants(link);
        /* Check if offline was set via decrypter */
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getDownloadURL().matches(TYPE_DIRECT)) {
            finalUrl = link.getDownloadURL();
            /* Prefer filename inside url */
            filename = extractFileNameFromURL(finalUrl);
            checkstatus = linkOk(link, filename, isDownload);
            if (checkstatus != 1) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else if (link.getDownloadURL().matches(VKontakteRuHoster.TYPE_DOCLINK)) {
            if (link.getLinkID() == null || !link.getLinkID().matches("")) {
                link.setLinkID(new Regex(link.getDownloadURL(), "/doc((?:\\-)?\\d+_\\d+)").getMatch(0));
            }
            br.getPage(link.getDownloadURL());
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().matches(VKontakteRuHoster.TYPE_DOCLINK)) {
                    logger.info("Doc Link type redirect");
                    br.getPage(br.getRedirectLocation());
                } else if (br.getRedirectLocation().matches(VKontakteRuHoster.TYPE_DIRECT)) {
                    logger.info("Direct Link type redirect");
                    finalUrl = br.getRedirectLocation();
                    /* Prefer filename inside url */
                    filename = extractFileNameFromURL(finalUrl);
                    checkstatus = linkOk(link, filename, isDownload);
                    if (checkstatus != 1) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    return AvailableStatus.TRUE;
                } else {
                    // ??
                    logger.info("Other redirect");
                    br.followConnection();
                }
            }
            if (br.containsHTML("File deleted")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("This document is available only to its owner\\.")) {
                final Account acc = account != null ? account : AccountController.getInstance().getValidAccount(this);
                if (acc != null) {
                    login(br, acc);
                    br.setFollowRedirects(true);
                    br.getPage(link.getDownloadURL());
                }
                if (br.containsHTML("This document is available only to its owner\\.")) {
                    link.getLinkStatus().setStatusText("This document is available only to its owner");
                    link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
                    return AvailableStatus.TRUE;
                }
            }
            filename = br.getRegex("title>([^<>\"]*?)</title>").getMatch(0);
            finalUrl = br.getRegex("var src = \\'(https?://[^<>\"]*?)\\';").getMatch(0);
            if (filename == null || finalUrl == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Sometimes filenames on site are cut - finallink usually contains the full filenames */
            final String betterFilename = new Regex(finalUrl, "docs/[a-z0-9]+/([^<>\"]*?)\\?extra=.+").getMatch(0);
            if (betterFilename != null) {
                filename = Encoding.htmlDecode(betterFilename).trim();
            } else {
                filename = Encoding.htmlDecode(filename.trim());
            }
            if (docs_add_unique_id) {
                filename = link.getLinkID() + filename;
            }
            checkstatus = linkOk(link, filename, isDownload);
            if (checkstatus != 1) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /* Check if login is required to check/download */
            final boolean noLogin = checkNoLoginNeeded(link);
            final Account aa = account != null ? account : AccountController.getInstance().getValidAccount(this);
            if (!noLogin && aa == null) {
                link.getLinkStatus().setStatusText("Only downlodable via account!");
                return AvailableStatus.UNCHECKABLE;
            } else if (aa != null) {
                /* Always login if possible. */
                login(br, aa);
            }
            br.setFollowRedirects(true);
            br.getPage(getBaseURL() + "/");
            vkID = br.getRegex("\\(\\{\"id\":(\\d+),").getMatch(0);
            if (vkID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (link.getDownloadURL().matches(VKontakteRuHoster.TYPE_AUDIOLINK)) {
                String finalFilename = link.getFinalFileName();
                if (finalFilename == null) {
                    finalFilename = link.getName();
                }
                finalUrl = link.getStringProperty("directlink", null);
                if (!audioIsValidDirecturl(finalUrl)) {
                    checkstatus = 0;
                } else {
                    checkstatus = linkOk(link, finalFilename, isDownload);
                }
                if (checkstatus != 1) {
                    String url = null;
                    final Browser br = this.br.cloneBrowser();
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    /*
                     * If these two values are present, we know that the content initially came from a 'wall' which requires us to use a different method to
                     * grab it as without that, permissions to play the track might be missing as it can only be accessed inside that particular wall!
                     */
                    final String postID = link.getStringProperty("postID", null);
                    final String fromId = link.getStringProperty("fromId", null);
                    boolean failed = false;
                    if (postID != null && fromId != null) {
                        logger.info("Trying to refresh audiolink directlink via wall-handling");
                        final String post = "act=get_wall_playlist&al=1&local_id=" + postID + "&oid=" + fromId + "&wall_type=own";
                        br.postPage(getBaseURL() + "/audio", post);
                        url = br.getRegex("\"0\":\"" + Pattern.quote(ownerID) + "\",\"1\":\"" + Pattern.quote(contentID) + "\",\"2\":(\"[^\"]+\")").getMatch(0);
                        if (url == null) {
                            /* Try other way below. */
                            failed = true;
                        } else {
                            /* Decodes the json string */
                            url = (String) JavaScriptEngineFactory.jsonToJavaObject(url);
                        }
                    } else {
                        failed = true;
                    }
                    if (failed) {
                        logger.info("refreshing audiolink directlink via album-handling");
                        /*
                         * No way to easily get the needed info directly --> Load the complete audio album and find a fresh directlink for our ID.
                         *
                         * E.g. get-play-link: https://vk.com/audio?id=<ownerID>&audio_id=<contentID>
                         */
                        /*
                         * 2017-01-05: They often change the order of the ownerID and contentID parameters here so from now on, let's try both variants.
                         */
                        postPageSafe(aa, link, getBaseURL() + "/al_audio.php", "act=reload_audio&al=1&ids=" + ownerID + "_" + contentID + "," + ownerID + "_" + contentID);
                        url = audioGetDirectURL();
                        if (url == null) {
                            postPageSafe(aa, link, getBaseURL() + "/al_audio.php", "act=reload_audio&al=1&ids=" + contentID + "_" + ownerID);
                            url = audioGetDirectURL();
                            if (url == null) {
                                postPageSafe(aa, link, getBaseURL() + "/al_audio.php", "act=reload_audio&al=1&ids=" + ownerID + "_" + contentID);
                                url = audioGetDirectURL();
                            }
                        }
                    }
                    if (url == null) {
                        if (failed) {
                            /*
                             * 2017-01-05: Changed from ERROR_FILE_NOT_FOUND to ERROR_TEMPORARILY_UNAVAILABLE --> Until now we never had a good test case to identify
                             * offline urls.
                             */
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server issue - track might be offline", 5 * 60 * 1000l);
                        }
                        logger.warning("Failed to refresh audiolink directlink");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    finalUrl = url;
                    checkstatus = linkOk(link, finalFilename, isDownload);
                    if (checkstatus != 1) {
                        logger.info("Refreshed audiolink directlink seems not to work --> Link is probably offline");
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    link.setProperty("directlink", finalUrl);
                }
            } else if (link.getDownloadURL().matches(VKontakteRuHoster.TYPE_VIDEOLINK)) {
                br.setFollowRedirects(true);
                finalUrl = link.getStringProperty("directlink", null);
                /* Check if directlink is expired */
                checkstatus = linkOk(link, link.getFinalFileName(), isDownload);
                if (checkstatus != 1) {
                    /* Refresh directlink */
                    final String oid = link.getStringProperty("userid", null);
                    final String id = link.getStringProperty("videoid", null);
                    accessVideo(this.br, oid, id, null, false);
                    if (br.containsHTML(VKontakteRuHoster.HTML_VIDEO_NO_ACCESS) || br.containsHTML(VKontakteRuHoster.HTML_VIDEO_REMOVED_FROM_PUBLIC_ACCESS)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final LinkedHashMap<String, String> availableQualities = findAvailableVideoQualities(br.toString());
                    if (availableQualities == null) {
                        logger.info("vk.com: Couldn't find any available qualities for videolink");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    finalUrl = availableQualities.get(link.getStringProperty("selectedquality", null));
                    if (finalUrl == null) {
                        logger.warning("Failed to find new link for selected quality...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    checkstatus = linkOk(link, link.getStringProperty("directfilename", null), isDownload);
                    if (checkstatus != 1) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - this video might be offline");
                    }
                }
            } else {
                finalUrl = link.getStringProperty("picturedirectlink", null);
                if (finalUrl == null) {
                    final String photo_list_id = link.getStringProperty("photo_list_id", null);
                    final String module = link.getStringProperty("photo_module", null);
                    final String photoID = getPhotoID(link);
                    if (module != null && photo_list_id != null) {
                        /* Access photo inside wall-post */
                        setHeadersPhoto(this.br);
                        postPageSafe(aa, link, getBaseURL() + "/al_photos.php", "act=show&al=1&list=" + module + photo_list_id + "&module=" + module + "&photo=" + photoID);
                    } else {
                        /* Access normal photo / photo inside album */
                        String albumID = link.getStringProperty("albumid");
                        boolean jsonSourceAvailableFromHtml = false;
                        if (albumID == null) {
                            /* No albumID available? Search it in the html! */
                            getPageSafe(aa, link, getBaseURL() + "/photo" + photoID);
                            if (br.containsHTML("Unknown error|Unbekannter Fehler|Access denied") || this.br.getHttpConnection().getResponseCode() == 404) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            albumID = br.getRegex("class=\"active_link\">[\t\n\r ]+<a href=\"/(.*?)\"").getMatch(0);
                            if (albumID == null) { /* new.vk.com */
                                albumID = br.getRegex("<span class=\"photos_album_info\"><a href=\"/(.*?)\\?.*?\"").getMatch(0);
                            }
                            if (albumID == null) {
                                /* New 2016-08-23 */
                                final String json = this.br.getRegex("ajax\\.preload\\(\\'al_photos\\.php\\'\\s*?,\\s*?(\\{.*?)\\);").getMatch(0);
                                if (json != null) {
                                    albumID = PluginJSonUtils.getJsonValue(json, "list");
                                    if (albumID != null) {
                                        /* Fix id */
                                        albumID = albumID.replace("album", "");
                                    }
                                }
                            }
                            if (albumID != null) {
                                /* Save this! Important! */
                                link.setProperty("albumid", albumID);
                            }
                            if (picturesGetJsonFromHtml() != null) {
                                jsonSourceAvailableFromHtml = true;
                            }
                        }
                        if (!jsonSourceAvailableFromHtml) {
                            /* Only go the json-way if we have to! */
                            if (albumID == null) {
                                logger.info("vk.com: albumID is null and failed to find picture json");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            setHeadersPhoto(this.br);
                            postPageSafe(aa, link, getBaseURL() + "/al_photos.php", "act=show&al=1&module=photos&list=" + albumID + "&photo=" + photoID);
                            if (br.containsHTML(">Unfortunately, this photo has been deleted") || br.containsHTML(">Access denied<")) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                        }
                    }
                }
                /* 2016-10-07: Implemented to avoid host-side block although results tell me that this does not improve anything. */
                setHeaderRefererPhoto(this.br);
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (downloadLink.getDownloadURL().matches(TYPE_PICTURELINK)) {
            // this is for resume of cached link.
            if (finalUrl != null) {
                if (!photolinkOk(downloadLink, null, false)) {
                    // failed, lets nuke cached entry and retry.
                    downloadLink.setProperty("picturedirectlink", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                return;
            }
            // virgin download.
            if (finalUrl == null) {
                /*
                 * Because of the availableCheck, we already know that the picture is online but we can't be sure that it really is downloadable!
                 */
                getHighestQualityPic(downloadLink);
                return;
            }
        }
        if (downloadLink.getDownloadURL().matches(VKontakteRuHoster.TYPE_DOCLINK)) {
            if (br.containsHTML("This document is available only to its owner\\.")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This document is available only to its owner");
            }
        }
        if (dl == null) {
            // most if not all components already opened connection via either linkOk or photolinkOk
            br.getHeaders().put("Accept-Encoding", "identity");
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, finalUrl, true, getMaxChunks(downloadLink, finalUrl));
        }
        handleServerErrors(downloadLink);
        dl.startDownload();
    }

    private String decryptURLSubL(String decryptType, String t, String e) {
        final String result;
        if (decryptType == null) {
            result = null;
        } else {
            if ("v".equals(decryptType)) {
                result = new StringBuffer(t).reverse().toString();
            } else if ("r".equals(decryptType)) {
                final int pos = Integer.parseInt(e);
                final StringBuffer sb = new StringBuffer(t);
                String o = ALPHANUMERIC + ALPHANUMERIC;
                for (int a = sb.length() - 1; a >= 0; a--) {
                    int i = o.indexOf(sb.charAt(a));
                    if (i != -1) {
                        i = i - pos;
                        if (i < 0) {
                            i = o.length() + i;
                        }
                        sb.setCharAt(a, o.substring(i, i + 1).charAt(0));
                    }
                }
                result = sb.toString();
            } else if ("s".equals(decryptType)) {
                int eVal = Integer.parseInt(e);
                result = decryptURLSubLS(t, eVal);
            } else if ("i".equals(decryptType)) {
                int eVal = Integer.parseInt(e);
                result = decryptURLSubLS(t, eVal ^ Integer.parseInt(vkID));
            } else if ("x".equals(decryptType)) {
                final char eCharValue = e.charAt(0);
                final StringBuffer sb = new StringBuffer();
                for (int i = 0; i < t.length(); i++) {
                    sb.append(Character.valueOf((char) (t.charAt(i) ^ eCharValue)));
                }
                result = sb.toString();
            } else {
                result = null;
            }
        }
        return result;
    }

    private String decryptURLSubLS(final String t, final int e) {
        if (t.length() > 0) {
            List<Integer> o = decryptURLSubS(t, e);
            StringBuffer result = new StringBuffer(t);
            int i = 1;
            o.remove(0);
            for (int oIndex : o) {
                String tmp = result.substring(oIndex, oIndex + 1);
                result.replace(oIndex, oIndex + 1, result.substring(i, i + 1));
                result.replace(i, i + 1, tmp);
                i++;
            }
            return result.toString();
        } else {
            return null;
        }
    }

    private String decryptURLSubA(String t) {
        if (t == null || t.length() % 4 == 1) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        int e = 0, i, o = 0;
        for (int a = 0; a < t.length(); a++) {
            i = ALPHANUMERIC.indexOf(t.charAt(a));
            if (i != -1) {
                if (o % 4 != 0) {
                    e = 64 * e + i;
                } else {
                    e = i;
                }
                if (o++ % 4 != 0) {
                    result.append(Character.valueOf((char) (255 & e >> (-2 * o & 6))));
                }
            }
        }
        return result.toString();
    }

    private List<Integer> decryptURLSubS(String t, final int e) {
        int i = t.length();
        List<Integer> result = new ArrayList<Integer>();
        if (i > 0) {
            int eVal = e;
            for (int a = i; a > 0; a--) {
                eVal = Math.abs(eVal);
                eVal = (i * a ^ eVal + (a - 1)) % i;
                result.add(eVal);
            }
        }
        return result;
    }

    private String decryptURL(final String url) {
        String result = url;
        if (!url.contains("audio_api_unavailable")) {
            return result;
        }
        String[] hash = url.split("\\?extra=")[1].split("#");
        String o = decryptURLSubA(hash[1]);
        String e = decryptURLSubA(hash[0]);
        if (o == null || e == null) {
            return result;
        }
        String[] oa = o.split(Character.valueOf((char) 9).toString());
        for (int n = oa.length - 1; n >= 0; n--) {
            String[] l = oa[n].split(Character.valueOf((char) 11).toString());
            if (l.length == 0) {
                return result;
            }
            if (!"v".equals(l[0]) && !"r".equals(l[0]) && !"s".equals(l[0]) && !"i".equals(l[0]) && !"x".equals(l[0])) {
                return result;
            }
            if ("v".equals(l[0])) {
                e = decryptURLSubL(l[0], e, null);
            } else {
                e = decryptURLSubL(l[0], e, l[1]);
            }
        }
        if (e != null && e.startsWith("http")) {
            result = e;
        }
        return result;
    }

    private String audioGetDirectURL() {
        String url = this.br.getRegex("\"(http[^<>\"\\']+\\.mp3[^<>\"\\']*?)\"").getMatch(0);
        if (url != null) {
            url = url.replace("\\", "");
            url = decryptURL(url);
            if (!audioIsValidDirecturl(url)) {
                url = null;
            }
        }
        return url;
    }

    /* 2016-01-05: Check for invalid audioURL (e.g. decryption fails)! */
    public static boolean audioIsValidDirecturl(final String url) {
        if (url == null || (url != null && url.matches(".+audio_api_unavailable\\.mp3.*?"))) {
            return false;
        } else {
            return true;
        }
    }

    private void setHeadersPhoto(final Browser br) {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }

    public static void setHeaderRefererPhoto(final Browser br) {
        br.getHeaders().put("Referer", "https://" + DOMAIN + "/al_photos.php");
    }

    public static void accessVideo(final Browser br, final String oid, final String id, final String listID, final boolean useApi) throws Exception {
        final String videoids_together = oid + "_" + id;
        if (listID == null && useApi) {
            /*
             * 2016-08-10: Seems like this API method does not löonger work/return the information we need. The new method seems to require
             * authentication: https://api.vk.com/method/video.get?format=json&owner_id=&videos=-12345678_87654321 See here:
             * https://new.vk.com/dev/video.get
             */
            br.getPage(getProtocol() + "vk.com/video.php?act=a_flash_vars&vid=" + videoids_together);
        } else if (listID == null) {
            br.getPage(getProtocol() + "vk.com/video" + videoids_together);
        } else {
            br.postPage(getProtocol() + "vk.com/al_video.php", "act=show_inline&al=1&list=" + listID + "&module=public&video=" + videoids_together);
        }
    }

    @SuppressWarnings("deprecation")
    private void handleServerErrors(final DownloadLink downloadLink) throws PluginException, IOException {
        final URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() == 416) {
            con.disconnect();
            logger.info("Resume failed --> Retrying from zero");
            downloadLink.setChunksProgress(null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (con.getContentType().contains("html")) {
            logger.info("vk.com: Plugin broken after download-try");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            logger.info("Logging in without cookies (forced login)...");
            login(br, account);
            logger.info("Logged in successfully without cookies (forced login)!");
        } catch (final PluginException e) {
            logger.info("Login failed!");
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Free Account");
        return ai;
    }

    /* Same function in hoster and decrypterplugin, sync it!! */
    private LinkedHashMap<String, String> findAvailableVideoQualities(final String source) throws Exception {
        return jd.plugins.decrypter.VKontakteRu.findAvailableVideoQualities(source);
    }

    private void generalErrorhandling() throws PluginException {
        if (br.containsHTML(VKontakteRuHoster.TEMPORARILYBLOCKED)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many requests in a short time", 60 * 1000l);
        }
    }

    @Override
    public String getAGBLink() {
        return getBaseURL() + "/help.php?page=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /* Doc-links and other links with permission can be downloaded without login */
        if (downloadLink.getDownloadURL().matches(VKontakteRuHoster.TYPE_DOCLINK) || downloadLink.getDownloadURL().matches(VKontakteRuHoster.TYPE_DIRECT)) {
            requestFileInformation(downloadLink, null, true);
            doFree(downloadLink);
        } else if (checkNoLoginNeeded(downloadLink)) {
            requestFileInformation(downloadLink, null, true);
            doFree(downloadLink);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean checkNoLoginNeeded(final DownloadLink dl) {
        boolean noLogin = dl.getBooleanProperty("nologin", false);
        if (!noLogin) {
            noLogin = dl.getDownloadURL().matches(TYPE_PICTURELINK);
        }
        return noLogin;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        doFree(link);
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        return true;
    }

    /**
     * Checks a given directlink for content. Sets finalfilename as final filename if finalfilename != null - else sets server filename as final
     * filename.
     *
     * @return <b>1</b>: Link is valid and can be downloaded, <b>0</b>: Link leads to HTML, times out or other problems occured, <b>404</b>:
     *         Server 404 response
     */
    private int linkOk(final DownloadLink downloadLink, final String finalfilename, final boolean isDownload) throws Exception {
        // invalidate is required!
        if (StringUtils.isEmpty(finalUrl)) {
            return 0;
        }
        final Browser br2 = this.br.cloneBrowser();
        br2.setFollowRedirects(true);
        br2.getHeaders().put("Accept-Encoding", "identity");
        final PluginForHost orginalPlugin = downloadLink.getLivePlugin();
        if (!isDownload) {
            downloadLink.setLivePlugin(this);
        }
        URLConnectionAdapter con = null;
        boolean closeConnection = true;
        try {
            if (isDownload) {
                dl = new jd.plugins.BrowserAdapter().openDownload(br2, downloadLink, finalUrl, true, getMaxChunks(downloadLink, finalUrl));
                con = dl.getConnection();
            } else {
                con = br2.openGetConnection(finalUrl);
            }
            if (!con.getContentType().contains("html")) {
                final long foundFilesize = con.getLongContentLength();
                if (finalfilename == null) {
                    downloadLink.setFinalFileName(Encoding.htmlDecode(Plugin.getFileNameFromHeader(con)));
                } else {
                    downloadLink.setFinalFileName(Encoding.urlDecode(finalfilename, false));
                }
                /* 2016-12-01: Set filesize if it has not been set before. */
                if (downloadLink.getDownloadSize() < foundFilesize) {
                    downloadLink.setDownloadSize(foundFilesize);
                }
                if (isDownload) {
                    closeConnection = false;
                }
                return 1;
            } else {
                // request range fucked
                if (con.getResponseCode() == 416) {
                    logger.info("Resume failed --> Retrying from zero");
                    downloadLink.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (con.getResponseCode() == 404) {
                    if (!downloadLink.isNameSet() && finalfilename != null) {
                        downloadLink.setFinalFileName(Encoding.urlDecode(finalfilename, false));
                    }
                    return 404;
                }
                return 0;
            }
        } catch (final BrowserException ebr) {
            /* This happens e.g. for temporarily unavailable videos. */
            throw ebr;
        } catch (final Exception e) {
            return 0;
        } finally {
            if (closeConnection) {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable t) {
                }
                downloadLink.setLivePlugin(orginalPlugin);
            }
        }
    }

    private int getMaxChunks(final DownloadLink downloadLink, final String url) {
        if (downloadLink.getDownloadURL().matches(VKontakteRuHoster.TYPE_VIDEOLINK)) {
            return 0;
        } else if (downloadLink.getDownloadURL().matches(VKontakteRuHoster.TYPE_VIDEOLINK) && StringUtils.containsIgnoreCase(downloadLink.getDownloadURL(), ".mp4")) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Checks a given photo directlink for content. Sets finalfilename as final filename if finalfilename != null - else sets server filename as
     * final filename.
     *
     * @return <b>true</b>: Link is valid and can be downloaded <b>false</b>: Link leads to HTML, times out or other problems occured - link is
     *         not downloadable!
     */
    private boolean photolinkOk(final DownloadLink downloadLink, String finalfilename, final boolean isLast) throws Exception {
        final Browser br2 = this.br.cloneBrowser();
        /* Correct final URLs according to users' plugin settings. */
        photo_correctLink();
        /* Ignore invalid urls. Usually if we have such an url the picture is serverside temporarily unavailable. */
        if (finalUrl.contains("_null_")) {
            return false;
        }
        br2.getHeaders().put("Accept-Encoding", "identity");
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br2, downloadLink, finalUrl, true, getMaxChunks(downloadLink, finalUrl));
            // request range fucked
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume failed --> Retrying from zero");
                downloadLink.setChunksProgress(null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (dl.getConnection().getLongContentLength() <= 100 || dl.getConnection().getResponseCode() == 404 || dl.getConnection().getResponseCode() == 502) {
                /* Photo is supposed to be online but it's not downloadable */
                return false;
            }
            if (!dl.getConnection().getContentType().contains("html")) {
                finalfilename = photoGetFinalFilename(downloadLink, finalfilename, finalUrl);
                if (finalfilename == null) {
                    /* This should actually never happen. */
                    finalfilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
                }
                downloadLink.setFinalFileName(finalfilename);
                downloadLink.setProperty("picturedirectlink", finalUrl);
                dl.startDownload();
                return true;
            } else {
                if (isLast) {
                    handleServerErrors(downloadLink);
                }
                return false;
            }
        } catch (final BrowserException ebr) {
            logger.info("BrowserException on directlink: " + finalUrl);
            if (isLast) {
                throw ebr;
            }
            return false;
        } catch (final ConnectException ec) {
            logger.info("Directlink timed out: " + finalUrl);
            if (isLast) {
                throw ec;
            }
            return false;
        } catch (final PluginException p) {
            // required for exists on disk and set mirror as complete.
            throw p;
        } catch (final SkipReasonException s) {
            // required for file exists on disk (standard).
            throw s;
        } catch (final Exception e) {
            if (isLast) {
                throw e;
            }
            return false;
        } finally {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable t) {
            }
        }
    }

    /**
     * Returns the final filename for photourls based on given circumstances and user-setting VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME .
     */
    private String photoGetFinalFilename(final DownloadLink dl, String finalfilename, final String directlink) throws MalformedURLException {
        final String url_filename = this.getFileNameFromURL(new URL(directlink));
        if (finalfilename != null) {
            /* Do nothing - final filename has already been set (usually this is NOT the case). */
        } else if (this.getPluginConfig().getBooleanProperty(VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME, default_VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME) && url_filename != null) {
            finalfilename = url_filename;
        } else {
            finalfilename = getPhotoID(dl) + getFileNameExtensionFromString(finalUrl, ".jpg");
        }
        return finalfilename;
    }

    /** TODO: Maybe add login via API: https://vk.com/dev/auth_mobile */
    public void login(Browser br, final Account account) throws Exception {
        synchronized (VKontakteRuHoster.LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser(br, false);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(DOMAIN, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    /* Check cookies */
                    br.setFollowRedirects(true);
                    br.getPage(getBaseURL());
                    // non language, check
                    if (br.containsHTML("id=\"logout_link_td\"|id=\"(?:top_)?logout_link\"")) {
                        // language set in user profile, so after 'login' OR 'login check' it could be changed!
                        if (!"3".equals(br.getCookie(DOMAIN, "remixlang"))) {
                            br.setCookie(DOMAIN, "remixlang", "3");
                        }
                        /* Refresh timestamp */
                        account.saveCookies(br.getCookies(DOMAIN), "");
                        return;
                    }
                    /* Delete cookies / Headers to perform a full login */
                    br = prepBrowser(new Browser(), false);
                }
                br.setFollowRedirects(true);
                br.getPage(getBaseURL() + "/");
                final Form login = br.getFormbyProperty("id", "quick_login_form");
                if (login == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                login.put("email", Encoding.urlEncode(account.getUser()));
                login.put("pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                // should redirect to /login/act=slogin....
                br.getPage("/");
                // language set in user profile, so after login it could be changed! We don't want this, we need to save and use ENGLISH
                if (!"3".equals(br.getCookie(DOMAIN, "remixlang"))) {
                    br.setCookie(DOMAIN, "remixlang", "3");
                    br.getPage(br.getURL());
                }
                /* Do NOT check based on cookies as they sometimes change them! */
                if (!br.containsHTML("id=\"logout_link_td\"|id=\"(?:top_)?logout_link\"")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Finish login if needed */
                final Form lol = br.getFormbyProperty("name", "login");
                if (lol != null) {
                    lol.put("email", Encoding.urlEncode(account.getUser()));
                    lol.put("pass", Encoding.urlEncode(account.getPass()));
                    lol.put("expire", "0");
                    br.submitForm(lol);
                }
                /* Save cookies */
                account.saveCookies(br.getCookies(DOMAIN), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    /* Handle all kinds of stuff that disturbs the downloadflow */
    private void getPageSafe(final Account acc, final DownloadLink dl, final String page) throws Exception {
        br.getPage(page);
        if (acc != null && br.getRedirectLocation() != null && br.getRedirectLocation().contains("login.vk.com/?role=fast")) {
            logger.info("Avoiding 'login.vk.com/?role=fast&_origin=' security check by re-logging in...");
            // Force login
            login(br, acc);
            br.getPage(page);
        } else if (acc != null && br.toString().length() < 100 && br.toString().trim().matches("\\d+<\\!><\\!>\\d+<\\!>\\d+<\\!>\\d+<\\!>[a-z0-9]+|\\d+<!><\\!>.+/login\\.php\\?act=security_check.+")) {
            logger.info("Avoiding possible outdated cookie/invalid account problem by re-logging in...");
            // Force login
            login(br, acc);
            br.getPage(page);
        } else if (br.getRedirectLocation() != null && br.getRedirectLocation().replaceAll("https?://(\\w+\\.)?vk\\.com", "").equals(page.replaceAll("https?://(\\w+\\.)?vk\\.com", ""))) {
            br.getPage(br.getRedirectLocation());
        }
        generalErrorhandling();
    }

    private void postPageSafe(final Account acc, final DownloadLink dl, final String page, final String postData) throws Exception {
        br.postPage(page, postData);
        if (acc != null && br.getRedirectLocation() != null && br.getRedirectLocation().contains("login.vk.com/?role=fast")) {
            logger.info("Avoiding 'login.vk.com/?role=fast&_origin=' security check by re-logging in...");
            // Force login
            login(br, acc);
            br.postPage(page, postData);
        } else if (acc != null && br.toString().length() < 100 && br.toString().trim().matches("\\d+<\\!><\\!>\\d+<\\!>\\d+<\\!>\\d+<\\!>[a-z0-9]+|\\d+<!><\\!>.+/login\\.php\\?act=security_check.+")) {
            logger.info("Avoiding possible outdated cookie/invalid account problem by re-logging in...");
            // TODO: Change/remove this - should not be needed anymore!
            // Force login
            login(br, acc);
            br.postPage(page, postData);
        }
        generalErrorhandling();
    }

    private static AtomicBoolean match = null;

    public static Browser prepBrowser(final Browser br, final boolean isDecryption) {
        // debug
        if (match == null) {
            if ("0dd0ecf7f742873d745106adea00b64a".equals(System.getProperty("k", null))) {
                match = new AtomicBoolean(true);
            } else {
                match = new AtomicBoolean(false);
            }
        }
        if (match != null && match.get()) {
            br.setDebug(true);
            br.setVerbose(true);
        }
        String useragent = SubConfiguration.getConfig("vkontakte.ru").getStringProperty(VKADVANCED_USER_AGENT, default_user_agent);
        if (useragent.equals("") || useragent.length() <= 3) {
            useragent = default_user_agent;
        }
        br.getHeaders().put("User-Agent", useragent);
        /* Set English language */
        br.setCookie(DOMAIN, "remixlang", "3");
        if (isDecryption) {
            // this causes epic issues in download tasks not timing out in reasonable time. We should refrain from setting in plugin
            // timeouts unless its _REALLY_ needed! 20160612-raztoki
            br.setReadTimeout(1 * 60 * 1000);
            br.setConnectTimeout(2 * 60 * 1000);
        }
        /* Loads can be very high. Site sometimes returns more than 10 000 entries with 1 request. */
        br.setLoadLimit(br.getLoadLimit() * 4);
        return br;
    }

    @Override
    public void errLog(Throwable e, Browser br, LogSource log, DownloadLink link, Account account) {
        if (e != null && e instanceof PluginException && ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) {
            final LogSource errlogger = LogController.getInstance().getLogger("PluginErrors");
            try {
                errlogger.severe("-START OF REPORT-");
                errlogger.severe("HosterPlugin out of date: " + this + " :" + getVersion());
                errlogger.severe("URL: " + link.getPluginPatternMatcher() + " | ContentUrl: " + link.getContentUrl() + " | ContainerUrl: " + link.getContainerUrl() + " | OriginUrl: " + link.getOriginUrl() + " | ReferrerUrl: " + link.getReferrerUrl());
                if (e != null) {
                    errlogger.log(e);
                }
                if (br != null && br.getRequest() != null) {
                    errlogger.info("\r\n" + br.getRequest().toString());
                    errlogger.severe("-END OF REPORT-");
                }
            } finally {
                errlogger.close();
            }
        }
    }

    /**
     * Try to get best quality and test links until a working link is found. will also handle errors in case
     *
     * @throws IOException
     */
    @SuppressWarnings({ "unchecked" })
    private void getHighestQualityPic(final DownloadLink dl) throws Exception {
        String json = picturesGetJsonFromHtml();
        if (json == null) {
            json = picturesGetJsonFromXml();
        }
        if (json == null) {
            if (br.containsHTML("<!>deleted<!>")) {
                // we suffer some desync between website and api. I guess due to website pages been held in cache.
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.warning("Failed to find source json of picturelink");
            final PluginException e = new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            errLog(e, br, dl);
            throw e;
        }
        final String thisid = getPhotoID(dl);
        final Object photoo = findPictureObject(JavaScriptEngineFactory.jsonToJavaObject(json), thisid);
        final Map<String, Object> sourcemap = (Map<String, Object>) photoo;
        if (sourcemap == null) {
            logger.info(json);
            logger.warning("Failed to find specified source json of picturelink:" + thisid);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean success = false;
        /* Count how many possible downloadlinks we have */
        int links_count = 0;
        final String[] qs = { "w_", "z_", "y_", "x_", "m_" };
        for (final String q : qs) {
            if (this.isAbort()) {
                logger.info("User stopped downloads --> Stepping out of getHighestQualityPic to avoid 'freeze' of the current DownloadLink");
                /* Avoid unnecessary 'plugin defect's in the logs. */
                throw new PluginException(LinkStatus.ERROR_RETRY, "User aborted download");
            }
            final String srcstring = q + "src";
            final Object picobject = sourcemap.get(srcstring);
            /* Check if the link we eventually found is downloadable. */
            if (picobject != null) {
                finalUrl = (String) picobject;
                links_count++;
                if (photolinkOk(dl, null, "m_".equals(q))) {
                    return;
                }
            }
        }
        if (links_count == 0) {
            logger.warning("Found no possible downloadlink for current picturelink --> Plugin broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (links_count > 0 && !success) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Photo is temporarily unavailable or offline (server issues)", 30 * 60 * 1000l);
        }
    }

    /** Recursive function to find object containing picture (download) information. */
    private Object findPictureObject(final Object o, final String picid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> cookieEntry : entrymap.entrySet()) {
                final String key = cookieEntry.getKey();
                final Object value = cookieEntry.getValue();
                if (key.equals("id") && value instanceof String) {
                    final String entry_id = (String) value;
                    if (entry_id.equals(picid)) {
                        return o;
                    } else {
                        continue;
                    }
                } else if (value instanceof List || value instanceof Map) {
                    final Object pico = findPictureObject(value, picid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (ArrayList) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object pico = findPictureObject(arrayo, picid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /** RegEx source-json from html. */
    private String picturesGetJsonFromHtml() {
        return this.br.getRegex("ajax\\.preload\\(\\'al_photos\\.php\\'\\s*?,\\s*?\\{[^\\}]*?\\}\\s*?,\\s*?(\\[.+)").getMatch(0);
    }

    /** RegEx source-json from xml. */
    private String picturesGetJsonFromXml() {
        return this.br.getRegex("<\\!json>(.*?)<\\!><\\!json>").getMatch(0);
    }

    /**
     * Try to get best quality and test links till a working link is found as it can happen that the found link is offline but others are
     * online. This function is made to check the information which has been saved via decrypter as the property "directlinks" on the
     * DownloadLink.
     *
     * @throws IOException
     */
    @SuppressWarnings({ "unused", "unchecked" })
    private void getHighestQualityPicFromSavedJson(final DownloadLink dl, final Object o) throws Exception {
        if (o == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean success = false;
        /* Count how many possible downloadlinks we have */
        int links_count = 0;
        Map<String, Object> attachments = (Map<String, Object>) o;
        final String qualities[] = { "src_xxxbig", "src_xxbig", "src_xbig", "src_big", "src", "src_small" };
        for (final String quality : qualities) {
            final Object finurl = attachments.get(quality);
            if (finurl != null) {
                links_count++;
                finalUrl = finurl.toString();
                if (photolinkOk(dl, null, "src_small".equals(quality))) {
                    return;
                }
            } else {
                continue;
            }
        }
        if (links_count == 0) {
            logger.warning("Found no possible downloadlink for current picturelink --> Plugin broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (links_count > 0 && !success) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Photo is temporarily unavailable or offline (server issues)", 30 * 60 * 1000l);
        }
    }

    /**
     * Changes server of picture links if wished by user - if not it will change them back to their "original" format. On error (server does not
     * match expected) it won't touch the current finallink at all! Only use this for photo links!
     */
    private void photo_correctLink() {
        if (true || this.getPluginConfig().getBooleanProperty(VKPHOTO_CORRECT_FINAL_LINKS, false)) {
            if (finalUrl.matches("https://pp\\.vk\\.me/c\\d+/.+")) {
                logger.info("VKPHOTO_CORRECT_FINAL_LINKS enabled --> final link is already in desired format ::: " + finalUrl);
            } else {
                /*
                 * Correct server to get files that are otherwise inaccessible - note that this can also make the finallinks unusable (e.g. server returns
                 * errorcode 500 instead of the file) but this is a very rare problem.
                 */
                final String was = finalUrl;
                final String oldserver = new Regex(finalUrl, "(https?://cs\\d+\\.vk\\.me/)").getMatch(0);
                final String serv_id = new Regex(finalUrl, "cs(\\d+)\\.vk\\.me/").getMatch(0);
                if (oldserver != null && serv_id != null) {
                    final String newserver = "https://pp.vk.me/c" + serv_id + "/";
                    finalUrl = finalUrl.replace(oldserver, newserver);
                    logger.info("VKPHOTO_CORRECT_FINAL_LINKS enabled --> SUCCEEDED to correct finallink ::: Was = " + was + " Now = " + finalUrl);
                } else {
                    logger.warning("VKPHOTO_CORRECT_FINAL_LINKS enabled --> FAILED to correct finallink ::: " + finalUrl);
                }
            }
        } else {
            // disabled as it fucks up links - raztoki20160612
            if (true) {
                return;
            }
            logger.info("VKPHOTO_CORRECT_FINAL_LINKS DISABLED --> changing final link back to standard");
            if (finalUrl.matches("http://cs\\d+\\.vk\\.me/v\\d+/.+")) {
                logger.info("final link is already in desired format --> Doing nothing");
            } else {
                /* Correct links to standard format */
                final Regex dataregex = new Regex(finalUrl, "(https?://pp\\.vk\\.me/c)(\\d+)/v(\\d+)/");
                final String serv_id = dataregex.getMatch(1);
                final String oldserver = dataregex.getMatch(0) + serv_id + "/";
                if (oldserver != null && serv_id != null) {
                    final String newserver = "http://cs" + serv_id + ".vk.me/";
                    finalUrl = finalUrl.replace(oldserver, newserver);
                    logger.info("VKPHOTO_CORRECT_FINAL_LINKS disabled --> SUCCEEDED to revert corrected finallink");
                } else {
                    logger.warning("VKPHOTO_CORRECT_FINAL_LINKS disabled --> FAILED to revert corrected finallink");
                }
            }
        }
    }

    /** Returns photoID in url-form: oid_id (userID_pictureID). */
    @SuppressWarnings("deprecation")
    private String getPhotoID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "vkontaktedecrypted\\.ru/picturelink/((\\-)?[\\d\\-]+_[\\d\\-]+)").getMatch(0);
    }

    private void setConstants(final DownloadLink dl) {
        this.ownerID = getOwnerID(dl);
        this.contentID = getContentID(dl);
        this.mainlink = dl.getStringProperty("mainlink", null);
    }

    /** Returns ArrayList of audio Objects for Playlists/Albums after '/al_audio.php' request. */
    public static ArrayList<Object> getAudioDataArray(final Browser br) throws Exception {
        final String json = jd.plugins.decrypter.VKontakteRu.regexJsonInsideHTML(br);
        if (json == null) {
            return null;
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("list");
        return ressourcelist;
    }

    private String getOwnerID(final DownloadLink dl) {
        String ownerID = dl.getStringProperty("owner_id", null);
        if (ownerID == null && dl.getDownloadURL().matches(TYPE_AUDIOLINK)) {
            /* E.g. Single audios which get added via wall single post crawler from inside comments of a post. */
            ownerID = new Regex(dl.getDownloadURL(), TYPE_AUDIOLINK).getMatch(0);
        } else if (ownerID == null && dl.getDownloadURL().matches(TYPE_PICTURELINK)) {
            ownerID = new Regex(dl.getDownloadURL(), TYPE_PICTURELINK).getMatch(0);
        }
        return ownerID;
    }

    private String getContentID(final DownloadLink dl) {
        String contentID = dl.getStringProperty("content_id", null);
        if (contentID == null && dl.getDownloadURL().matches(TYPE_AUDIOLINK)) {
            /* E.g. Single audios which get added via wall single post crawler from inside comments of a post. */
            contentID = new Regex(dl.getDownloadURL(), TYPE_AUDIOLINK).getMatch(1);
        } else if (contentID == null && dl.getDownloadURL().matches(TYPE_PICTURELINK)) {
            contentID = new Regex(dl.getDownloadURL(), TYPE_PICTURELINK).getMatch(1);
        }
        return contentID;
    }

    public static String getProtocol() {
        return "https://";
    }

    public static String getBaseURL() {
        return getProtocol() + DOMAIN;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public String getDescription() {
        return "JDownloader's Vk Plugin helps downloading all sorts of media from vk.com.";
    }

    public static final String   SLEEP_PAGINATION_GENERAL                                = "SLEEP_PAGINATION_GENERAL";
    public static final String   SLEEP_PAGINATION_COMMUNITY_VIDEO                        = "SLEEP_PAGINATION_COMMUNITY_VIDEO";
    public static final String   SLEEP_TOO_MANY_REQUESTS                                 = "SLEEP_TOO_MANY_REQUESTS_V1";
    /* Default values... */
    private static final boolean default_fastlinkcheck_FASTLINKCHECK                     = true;
    private static final boolean default_fastlinkcheck_FASTPICTURELINKCHECK              = true;
    private static final boolean default_fastlinkcheck_FASTAUDIOLINKCHECK                = true;
    private static final boolean default_ALLOW_BEST                                      = false;
    private static final boolean default_ALLOW_240p                                      = true;
    private static final boolean default_ALLOW_360p                                      = true;
    private static final boolean default_ALLOW_480p                                      = true;
    private static final boolean default_ALLOW_720p                                      = true;
    private static final boolean default_ALLOW_1080p                                     = true;
    private static final boolean default_WALL_ALLOW_albums                               = true;
    private static final boolean default_WALL_ALLOW_photo                                = true;
    private static final boolean default_WALL_ALLOW_audio                                = true;
    private static final boolean default_WALL_ALLOW_video                                = true;
    private static final boolean default_WALL_ALLOW_urls                                 = false;
    private static final boolean default_WALL_ALLOW_documents                            = true;
    public static final boolean  default_WALL_ALLOW_lookforurlsinsidewallposts           = false;
    public static final boolean  default_VKWALL_GRAB_COMMENTS_PHOTOS                     = false;
    public static final boolean  default_VKWALL_GRAB_COMMENTS_AUDIO                      = false;
    public static final boolean  default_VKWALL_GRAB_COMMENTS_VIDEO                      = false;
    public static final boolean  default_VKWALL_GRAB_COMMENTS_URLS                       = false;
    public static final String   default_VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX             = ".+";
    private static final boolean default_VKVIDEO_USEIDASPACKAGENAME                      = false;
    private static final boolean default_VKAUDIO_USEIDASPACKAGENAME                      = false;
    private static final boolean default_VKDOCS_USEIDASPACKAGENAME                       = false;
    private static final boolean default_VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME = false;
    private static final boolean default_VKPHOTO_CORRECT_FINAL_LINKS                     = false;
    public static final boolean  default_VKWALL_USE_API                                  = true;
    public static final String   default_user_agent                                      = UserAgents.stringUserAgent(BrowserName.Firefox);
    public static final long     defaultSLEEP_PAGINATION_GENERAL                         = 1000;
    public static final long     defaultSLEEP_SLEEP_PAGINATION_COMMUNITY_VIDEO           = 1000;
    public static final long     defaultSLEEP_TOO_MANY_REQUESTS                          = 3000;

    public void setConfigElements() {
        // this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "General settings:"));
        // this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        // this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Linkcheck settings:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), FASTLINKCHECK_VIDEO, JDL.L("plugins.hoster.vkontakteruhoster.fastLinkcheck", "Fast linkcheck for video links (filesize won't be shown in linkgrabber)?")).setDefaultValue(default_fastlinkcheck_FASTLINKCHECK));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), FASTLINKCHECK_PICTURES, JDL.L("plugins.hoster.vkontakteruhoster.fastPictureLinkcheck", "Fast linkcheck for all picture links (when true or false filename & filesize wont be shown until download starts, when false only task performed is to check if picture has been deleted!)?")).setDefaultValue(default_fastlinkcheck_FASTPICTURELINKCHECK));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), FASTLINKCHECK_AUDIO, JDL.L("plugins.hoster.vkontakteruhoster.fastAudioLinkcheck", "Fast linkcheck for audio links (filesize won't be shown in linkgrabber)?")).setDefaultValue(default_fastlinkcheck_FASTAUDIOLINKCHECK));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video settings:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_BEST, JDL.L("plugins.hoster.vkontakteruhoster.checkbest", "Only grab the best available resolution")).setDefaultValue(default_ALLOW_BEST);
        this.getConfig().addEntry(hq);
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_240P, JDL.L("plugins.hoster.vkontakteruhoster.check240", "Grab 240p MP4/FLV?")).setDefaultValue(default_ALLOW_240p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_360P, JDL.L("plugins.hoster.vkontakteruhoster.check360", "Grab 360p MP4?")).setDefaultValue(default_ALLOW_360p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_480P, JDL.L("plugins.hoster.vkontakteruhoster.check480", "Grab 480p MP4?")).setDefaultValue(default_ALLOW_480p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_720P, JDL.L("plugins.hoster.vkontakteruhoster.check720", "Grab 720p MP4?")).setDefaultValue(default_ALLOW_720p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.ALLOW_1080P, JDL.L("plugins.hoster.vkontakteruhoster.check1080", "Grab 1080p MP4?")).setDefaultValue(default_ALLOW_1080p).setEnabledCondidtion(hq, false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/wall-123...' and 'vk.com/wall-123..._123...' links:\r\n NOTE: You can't turn off all types. If you do that, JD will decrypt all instead!"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_ALBUMS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckalbums", "Grab album links ('vk.com/album')?")).setDefaultValue(default_WALL_ALLOW_albums));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_PHOTOS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckphotos", "Grab photo links ('vk.com/photo')?")).setDefaultValue(default_WALL_ALLOW_photo));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_AUDIO, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckaudio", "Grab audio links (.mp3 directlinks)?")).setDefaultValue(default_WALL_ALLOW_audio));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_VIDEO, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckvideo", "Grab video links ('vk.com/video')?")).setDefaultValue(default_WALL_ALLOW_video));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_DOCS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheckdocs", "Grab documents?")).setDefaultValue(default_WALL_ALLOW_documents));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_URLS, JDL.L("plugins.hoster.vkontakteruhoster.wallchecklink", "Grab other urls?")).setDefaultValue(default_WALL_ALLOW_urls));
        final ConfigEntry cfg_graburlsinsideposts = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_URLS_INSIDE_POSTS, JDL.L("plugins.hoster.vkontakteruhoster.wallcheck_look_for_urls_inside_posts", "Grab URLs inside wall posts?")).setDefaultValue(default_WALL_ALLOW_lookforurlsinsidewallposts);
        this.getConfig().addEntry(cfg_graburlsinsideposts);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX, JDL.L("plugins.hoster.vkontakteruhoster.regExForUrlsInsideWallPosts", "RegEx for URLs from inside wall posts (black-/whitelist): ")).setDefaultValue(default_VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX).setEnabledCondidtion(cfg_graburlsinsideposts, true));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for comments inside 'vk.com/wall-123_123...' links:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_COMMENTS_PHOTOS, "Grab photo urls inside comments below single wall posts?").setDefaultValue(default_VKWALL_GRAB_COMMENTS_PHOTOS));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_COMMENTS_AUDIO, "Grab audio urls inside comments below single wall posts?").setDefaultValue(default_VKWALL_GRAB_COMMENTS_AUDIO));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_COMMENTS_VIDEO, "Grab video urls inside comments below single wall posts?").setDefaultValue(default_VKWALL_GRAB_COMMENTS_VIDEO));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_GRAB_COMMENTS_URLS, "Grab other urls inside comments below single wall posts?").setDefaultValue(default_VKWALL_GRAB_COMMENTS_URLS));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/video' links:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKVIDEO_USEIDASPACKAGENAME, JDL.L("plugins.hoster.vkontakteruhoster.videoUseIdAsPackagename", "Use video-ID as packagename ('videoXXXX_XXXX' or 'video-XXXX_XXXX')?")).setDefaultValue(default_VKVIDEO_USEIDASPACKAGENAME));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/audios' links:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKAUDIOS_USEIDASPACKAGENAME, JDL.L("plugins.hoster.vkontakteruhoster.audiosUseIdAsPackagename", "Use audio-Owner-ID as packagename ('audiosXXXX' or 'audios-XXXX')?")).setDefaultValue(default_VKAUDIO_USEIDASPACKAGENAME));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/docs' links:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKDOCS_USEIDASPACKAGENAME, JDL.L("plugins.hoster.vkontakteruhoster.docsUseIdAsPackagename", "Use doc-Owner-ID as packagename ('docsXXXX' or 'docs-XXXX')?")).setDefaultValue(default_VKDOCS_USEIDASPACKAGENAME));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for 'vk.com/photo' links:"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME, JDL.L("plugins.hoster.vkontakteruhoster.photosTempServerFilenameAsFinalFilename", "Use (temporary) server filename as final filename instead of E.g.) 'oid_id.jpg'?")).setDefaultValue(default_VKPHOTOS_TEMP_SERVER_FILENAME_AS_FINAL_FILENAME));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Advanced settings:\r\n<html><p style=\"color:#F62817\">WARNING: Only change these settings if you really know what you're doing!</p></html>"));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKPHOTO_CORRECT_FINAL_LINKS, JDL.L("plugins.hoster.vkontakteruhoster.correctFinallinks", "For 'vk.com/photo' links: Change final downloadlinks from 'https?://csXXX.vk.me/vXXX/...' to 'https://pp.vk.me/cXXX/vXXX/...' (forces HTTPS)?")).setDefaultValue(default_VKPHOTO_CORRECT_FINAL_LINKS));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), VKontakteRuHoster.VKWALL_USE_API, "For 'vk.com/wall' links: Use API?").setDefaultValue(default_VKWALL_USE_API));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), VKADVANCED_USER_AGENT, JDL.L("plugins.hoster.vkontakteruhoster.customUserAgent", "User-Agent: ")).setDefaultValue(default_user_agent));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), VKontakteRuHoster.SLEEP_PAGINATION_GENERAL, JDL.L("plugins.hoster.vkontakteruhoster.sleep.paginationGeneral", "Define sleep time for general pagination"), 1000, 15000, 500).setDefaultValue(defaultSLEEP_PAGINATION_GENERAL));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), VKontakteRuHoster.SLEEP_PAGINATION_COMMUNITY_VIDEO, JDL.L("plugins.hoster.vkontakteruhoster.sleep.paginationCommunityVideos", "Define sleep time for community videos pagination"), 1000, 15000, 500).setDefaultValue(defaultSLEEP_SLEEP_PAGINATION_COMMUNITY_VIDEO));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), VKontakteRuHoster.SLEEP_TOO_MANY_REQUESTS, JDL.L("plugins.hoster.vkontakteruhoster.sleep.tooManyRequests", "Define sleep time for 'Temp Blocked' event"), (int) defaultSLEEP_TOO_MANY_REQUESTS, 15000, 500).setDefaultValue(defaultSLEEP_TOO_MANY_REQUESTS));
    }
}