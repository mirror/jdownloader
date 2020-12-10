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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.RequestHeader;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "instagram.com" }, urls = { "instagrammdecrypted://[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)?" })
public class InstaGramCom extends PluginForHost {
    @SuppressWarnings("deprecation")
    public InstaGramCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium(MAINPAGE + "/accounts/login/");
    }

    public static Browser prepBRAltAPI(final Browser br) {
        /* 2020-11-17: Also possible: Instagram 123.1.0.26.115 (iPhone12,1; iOS 13_3; en_US; en-US */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.3.8 (KHTML, like Gecko) Mobile/14G60 Instagram 12.0.0.16.90 (iPhone9,4; iOS 10_3_3; en_US; en-US; scale=2.61; gamut=wide; 1080x1920)");
        br.setAllowedResponseCodes(new int[] { 429 });
        return br;
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/about/legal/terms/#";
    }

    /**
     * https://instagram.api-docs.io/1.0 </br>
     */
    public static String         ALT_API_BASE                                = "https://i.instagram.com/api/v1";
    /* Connection stuff */
    private static final boolean RESUME                                      = true;
    /* Chunkload makes no sense for pictures/small files */
    private static final int     MAXCHUNKS_pictures                          = 1;
    /* 2020-01-21: Multi chunks are possible but it's better not to do this to avoid getting blocked! */
    private static final int     MAXCHUNKS_videos                            = 1;
    private static final int     MAXDOWNLOADS                                = -1;
    private static final String  MAINPAGE                                    = "https://www.instagram.com";
    public static final String   QUIT_ON_RATE_LIMIT_REACHED                  = "QUIT_ON_RATE_LIMIT_REACHED";
    public static final String   HASHTAG_CRAWLER_FIND_USERNAMES              = "HASHTAG_CRAWLER_FIND_USERNAMES";
    public static final String   PREFER_SERVER_FILENAMES                     = "PREFER_SERVER_FILENAMES";
    public static final String   ADD_ORDERID_TO_FILENAMES                    = "ADD_ORDERID_TO_FILENAMES";
    private static final String  ATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY        = "ATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY";
    public static final String   ONLY_GRAB_X_ITEMS                           = "ONLY_GRAB_X_ITEMS";
    public static final String   ONLY_GRAB_X_ITEMS_NUMBER                    = "ONLY_GRAB_X_ITEMS_NUMBER";
    public static final String   ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER    = "ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER";
    /* DownloadLink properties */
    public static final String   PROPERTY_has_tried_to_crawl_original_url    = "has_tried_to_crawl_original_url";
    public static final String   PROPERTY_is_part_of_story                   = "is_part_of_story";
    public static final String   PROPERTY_DIRECTURL                          = "directurl";
    /* Settings default values */
    public static final boolean  defaultPREFER_SERVER_FILENAMES              = false;
    public static final boolean  defaultATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY = false;
    /* 2020-11-25: Set this to false by default until we can maybe auto-detect the situation in which this is needed. */
    public static final boolean  defaultADD_ORDERID_TO_FILENAMES             = false;
    public static final boolean  defaultQUIT_ON_RATE_LIMIT_REACHED           = false;
    public static final boolean  defaultHASHTAG_CRAWLER_FIND_USERNAMES       = false;
    public static final boolean  defaultONLY_GRAB_X_ITEMS                    = false;
    public static final int      defaultONLY_GRAB_X_ITEMS_NUMBER             = 25;
    private boolean              is_private_url                              = false;

    public void correctDownloadLink(final DownloadLink link) {
        String newurl = link.getPluginPatternMatcher().replace("instagrammdecrypted://", "https://www.instagram.com/p/");
        if (!newurl.endsWith("/")) {
            /* Add slash to the end to prevent 302 redirect to speed up the download process a tiny bit. */
            newurl += "/";
        }
        link.setPluginPatternMatcher(newurl);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        this.correctDownloadLink(link);
        dllink = null;
        server_issues = false;
        is_private_url = link.getBooleanProperty("private_url", false);
        this.setBrowserExclusive();
        /*
         * Decrypter can set this status - basically to be able to handle private urls correctly in host plugin in case users' account gets
         * disabled for whatever reason.
         */
        prepBRWebsite(this.br);
        boolean isLoggedIN = false;
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(aa, false);
                isLoggedIN = true;
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        if (this.is_private_url && !isLoggedIN) {
            link.getLinkStatus().setStatusText("Login required to download this content");
            return AvailableStatus.UNCHECKABLE;
        }
        if (canGrabOriginalQualityDownloadurlViaAltAPI(link, isLoggedIN) && !link.getBooleanProperty(PROPERTY_has_tried_to_crawl_original_url, false)) {
            this.dllink = this.getHighesQualityDownloadlinkAltAPI(link, true);
        } else {
            this.dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        }
        this.dllink = this.checkLinkAndSetFilesize(link, this.dllink);
        if (this.dllink == null) {
            /* This will also act as a fallback in case that "original quality" handling fails */
            this.dllink = this.getFreshDirecturl(link, isLoggedIN);
            if (this.dllink == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh directurl");
            }
            // /* Set releasedate as property */
            // String date = PluginJSonUtils.getJson(this.br, "date");
            // if (date == null || !date.matches("\\d+")) {
            // date = PluginJSonUtils.getJson(this.br, "taken_at_timestamp");
            // }
            // if (date != null && date.matches("\\d+")) {
            // setReleaseDate(link, Long.parseLong(date));
            // }
            String ext = null;
            if (ext == null) {
                ext = getFileNameExtensionFromString(dllink, ".jpg");
            }
            String server_filename = getFileNameFromURL(new URL(dllink));
            if (this.getPluginConfig().getBooleanProperty(PREFER_SERVER_FILENAMES, defaultPREFER_SERVER_FILENAMES) && server_filename != null) {
                server_filename = fixServerFilename(server_filename, ext);
                link.setFinalFileName(server_filename);
            } else {
                // decrypter has set the proper name!
                // if the user toggles PREFER_SERVER_FILENAMES setting many times the name can change.
                final String name = link.getStringProperty("decypter_filename", null);
                if (name != null) {
                    link.setFinalFileName(name);
                } else {
                    // do not change.
                    logger.warning("missing storable, filename will not be renamed");
                }
            }
        }
        if (!isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
                } else if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getLongContentLength());
                    }
                    /* Save it to have it in case it was re-freshed! */
                    link.setProperty(PROPERTY_DIRECTURL, this.dllink);
                } else {
                    /* Will get displayed as unknown error later on */
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean canGrabOriginalQualityDownloadurlViaAltAPI(final DownloadLink link, final boolean is_logged_in) {
        // final String resolution_inside_url = new Regex(dllink, "(/s\\d+x\\d+/)").getMatch(0);
        /*
         * 2017-04-28: By removing the resolution inside the picture URL, we can download the original image - usually, resolution will be
         * higher than before then but it can also get smaller - which is okay as it is the original content.
         */
        /* 2020-10-07: Doesn't work anymore, see new method (old iPhone API endpoint) */
        // String drlink = dllink.replace(resolution_inside_url, "/");
        final String imageid = link.getStringProperty("postid");
        /* Avoids doing an extra http request for video files as they're never available in "original" quality (?) */
        final boolean userWantsToDownloadOriginalQuality = this.getPluginConfig().getBooleanProperty(ATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY, defaultATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY);
        return userWantsToDownloadOriginalQuality && is_logged_in && !isVideo(link) && imageid != null;
    }

    private boolean isVideo(final DownloadLink link) {
        return link.getBooleanProperty("isvideo", false) || (link.getFinalFileName() != null && link.getFinalFileName().contains(".mp4"));
    }

    /**
     * removePictureEffects true = grab best quality & original, removePictureEffects false = grab best quality but keep effects/filters.
     */
    private String getHighesQualityDownloadlinkAltAPI(final DownloadLink link, final boolean removePictureEffects) throws IOException, PluginException {
        // final String resolution_inside_url = new Regex(dllink, "(/s\\d+x\\d+/)").getMatch(0);
        /*
         * 2017-04-28: By removing the resolution inside the picture URL, we can download the original image - usually, resolution will be
         * higher than before then but it can also get smaller - which is okay as it is the original content.
         */
        /* 2020-10-07: Doesn't work anymore, see new method (old iPhone API endpoint) */
        // String drlink = dllink.replace(resolution_inside_url, "/");
        /* Important: Do not jump into this handling when downloading videos! */
        /*
         * Source of this idea:
         * https://github.com/instaloader/instaloader/blob/f4ecfea64cc11efba44cda2b44c8cfe41adbd28a/instaloader/instaloadercontext.py# L462
         */
        final String imageid = link.getStringProperty("postid");
        if (imageid == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        prepBRAltAPI(brc);
        getPageAltAPI(brc, ALT_API_BASE + "/media/" + imageid + "/info/");
        /* Offline errorhandling */
        if (brc.getHttpConnection().getResponseCode() != 200) {
            /* E.g. {"message": "Invalid media_id 1234561234567862322X", "status": "fail"} */
            /* E.g. {"message": "Media not found or unavailable", "status": "fail"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * New URL should be the BEST quality (resolution).
         */
        Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "items/{0}");
        final String downloadurl = getBestQualityURLAltAPI(entries);
        link.setProperty(PROPERTY_has_tried_to_crawl_original_url, true);
        link.setProperty(PROPERTY_DIRECTURL, downloadurl);
        return downloadurl;
    }

    public static void getPageAltAPI(final Browser br, final String url) throws PluginException, IOException {
        br.getPage(url);
        checkErrorsAltAPI(br);
    }

    public static void checkErrorsAltAPI(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Rate-Limit reached");
        }
    }

    public static String getBestQualityURLAltAPI(final Map<String, Object> entries) {
        final Object videoO = entries.get("video_versions");
        final boolean isVideo = videoO != null;
        String dllink = null;
        if (isVideo) {
            /* Find best video-quality */
            /*
             * TODO: 2020-11-17: What's the difference between e.g. the following "types": 101, 102, 103 - seems to be all the same quality
             * and filesize/resolution/URLs
             */
            final List<Object> ressourcelist = (List<Object>) videoO;
            if (ressourcelist != null) {
                long qualityMax = 0;
                for (final Object qualityO : ressourcelist) {
                    final Map<String, Object> imageQualityInfo = (Map<String, Object>) qualityO;
                    final long widthTmp = JavaScriptEngineFactory.toLong(imageQualityInfo.get("width"), 0);
                    if (widthTmp > qualityMax && imageQualityInfo.containsKey("url")) {
                        qualityMax = widthTmp;
                        dllink = (String) imageQualityInfo.get("url");
                    }
                }
            }
        } else {
            /* Find best image-quality */
            final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "image_versions2/candidates");
            if (ressourcelist != null) {
                long qualityMax = 0;
                for (final Object qualityO : ressourcelist) {
                    final Map<String, Object> imageQualityInfo = (Map<String, Object>) qualityO;
                    final long widthTmp = JavaScriptEngineFactory.toLong(imageQualityInfo.get("width"), 0);
                    if (widthTmp > qualityMax && imageQualityInfo.containsKey("url")) {
                        qualityMax = widthTmp;
                        dllink = (String) imageQualityInfo.get("url");
                    }
                }
            }
        }
        final boolean removePictureEffects = true;
        if (dllink != null && removePictureEffects && dllink.contains("&se=")) {
            /*
             * 2020-10-07: By replacing that one parameter, we will additionally remove all filters so we should get the original picture
             * then! The resolution will usually not change - it will only remove the filters!
             */
            /*
             * Source of this idea:
             * https://github.com/instaloader/instaloader/blob/f4ecfea64cc11efba44cda2b44c8cfe41adbd28a/instaloader/structures.py#L247
             */
            dllink = dllink.replaceAll("&se=\\d+(&)?", "&");
        }
        return dllink;
    }

    private String getFreshDirecturl(final DownloadLink link, final boolean isLoggedIN) throws IOException, PluginException {
        String directurl = null;
        logger.info("Trying to refresh directurl");
        /* Story elements can only be refreshed by ID and thus we need to use the other API for those! */
        final boolean forceOriginalQualitDownload = isLoggedIN && link.getBooleanProperty(PROPERTY_is_part_of_story, false);
        if (canGrabOriginalQualityDownloadurlViaAltAPI(link, isLoggedIN) || forceOriginalQualitDownload) {
            logger.info("Tring to obtain fresh original quality downloadurl");
            directurl = getHighesQualityDownloadlinkAltAPI(link, true);
        } else {
            logger.info("Trying to obtain fresh downloadurl via crawler");
            final PluginForDecrypt decrypter = JDUtilities.getPluginForDecrypt(this.getHost());
            decrypter.setBrowser(this.br);
            try {
                final CryptedLink forDecrypter = new CryptedLink(link.getContentUrl(), link);
                final ArrayList<DownloadLink> items = decrypter.decryptIt(forDecrypter, null);
                DownloadLink foundLink = null;
                if (items.size() == 1) {
                    foundLink = items.get(0);
                } else {
                    String orderID = link.getStringProperty("orderid");
                    if (orderID == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    for (final DownloadLink linkTmp : items) {
                        final String orderidTmp = linkTmp.getStringProperty("orderid");
                        if (orderID.equals(orderidTmp)) {
                            foundLink = linkTmp;
                            break;
                        }
                    }
                }
                directurl = foundLink.getStringProperty(PROPERTY_DIRECTURL);
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        if (directurl == null) {
            /* On failure, check for offline. */
            logger.info("Failed to find fresh directurl --> Checking for offline");
            if (br.getRequest().getHttpConnection().getResponseCode() == 404 || br.containsHTML("Oops, an error occurred")) {
                /*
                 * This will also happen if a user tries to access private urls without being logged in --> Which is why we need to know the
                 * private_url status from the crawler!
                 */
                logger.info("Seems like main URL is offline / post got deleted");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                logger.info("MainURL seems to be online --> Possible plugin error");
            }
        } else {
            logger.info("Successfully found fresh directurl");
        }
        return directurl;
    }

    public static void checkErrors(final Browser br) throws PluginException {
        /* Old trait */
        // if (br.getURL().matches("https?://[^/]+/accounts/login/\\?next=.*")) {
        /* New trait 2020-11-26 */
        if (br.getURL() != null && br.getURL().matches("https?://[^/]+/accounts/login.*")) {
            throw new AccountRequiredException();
        } else if (br.getURL() != null && br.getURL().matches("https?://[^/]+/challenge/.*")) {
            handleLoginChallenge(br);
        } else if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private String checkLinkAndSetFilesize(final DownloadLink link, final String flink) throws IOException, PluginException {
        URLConnectionAdapter con = null;
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        try {
            con = br2.openHeadConnection(flink);
            if (!looksLikeDownloadableContent(con)) {
                throw new IOException();
            } else {
                if (con.getCompleteContentLength() > 0) {
                    link.setDownloadSize(con.getCompleteContentLength());
                }
                return flink;
            }
        } catch (final Exception e) {
            logger.log(e);
            return null;
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (final Exception e) {
                }
            }
        }
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (this.is_private_url) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(link);
    }

    public void handleDownload(final DownloadLink link) throws Exception {
        int maxchunks = MAXCHUNKS_pictures;
        if (link.getFinalFileName() != null && link.getFinalFileName().contains(".mp4") || link.getName() != null && link.getName().contains(".mp4")) {
            maxchunks = MAXCHUNKS_videos;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, this.dllink, RESUME, maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            link.removeProperty(PROPERTY_DIRECTURL);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Directurl expired (?)", 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBRWebsite(br);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass());
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(MAINPAGE, cookies);
                    br.getPage(MAINPAGE + "/");
                    if (!isLoggedIn()) {
                        /* Full login required */
                        logger.info("Cookie login failed");
                        br.clearAll();
                    } else {
                        /* Saved cookies were valid */
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(MAINPAGE), "");
                        return;
                    }
                }
                if (userCookies != null) {
                    /*
                     * 2020-10-22: This can optionally be used as a workaround for login issues e.g. if a "security challenge" is demanded
                     * on login and JD fails to handle it.
                     */
                    logger.info("Attempting User-Cookie login");
                    br.setCookies(MAINPAGE, userCookies);
                    br.getPage(MAINPAGE + "/");
                    if (!isLoggedIn()) {
                        /* Full login required */
                        logger.info("User-Cookie login failed");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        /* Saved cookies were valid */
                        logger.info("User-Cookie login successful");
                        account.saveCookies(br.getCookies(MAINPAGE), "");
                        /* Make sure account has an unique username set. */
                        final String fullname = PluginJSonUtils.getJson(br, "full_name");
                        if (!StringUtils.isEmpty(fullname)) {
                            account.setUser(fullname);
                        }
                        return;
                    }
                }
                logger.info("Full login required");
                br.getPage(MAINPAGE + "/");
                final String csrftoken = br.getRegex("\"csrf_token\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                if (csrftoken == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.setCookie(MAINPAGE, "csrftoken", csrftoken);
                final String rollout_hash = br.getRegex("\"rollout_hash\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                if (rollout_hash == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final RequestHeader ajaxHeaders = new RequestHeader();
                ajaxHeaders.put("Accept", "*/*");
                ajaxHeaders.put("X-Instagram-AJAX", rollout_hash);
                ajaxHeaders.put("X-CSRFToken", csrftoken);
                ajaxHeaders.put("X-Requested-With", "XMLHttpRequest");
                final PostRequest post = new PostRequest("https://www.instagram.com/accounts/login/ajax/");
                post.setHeaders(ajaxHeaders);
                post.setContentType("application/x-www-form-urlencoded");
                /* 2020-05-19: https://github.com/instaloader/instaloader/pull/623 */
                final String enc_password = "#PWD_INSTAGRAM_BROWSER:0:" + System.currentTimeMillis() + ":" + account.getPass();
                post.setPostDataString("username=" + Encoding.urlEncode(account.getUser()) + "&enc_password=" + Encoding.urlEncode(enc_password) + "&queryParams=%7B%7D");
                br.getPage(post);
                if ("fail".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                    // 2 factor (Coded semi blind).
                    logger.info("Entering 2FA handling");
                    if (!"checkpoint_required".equals(PluginJSonUtils.getJsonValue(br, "message"))) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String page = PluginJSonUtils.getJsonValue(br, "checkpoint_url");
                    br.getPage(page);
                    handleLoginChallenge(this.br);
                    final boolean tryOldChallengeHandling = false;
                    if (tryOldChallengeHandling) {
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
                    }
                }
                if (!br.containsHTML("\"authenticated\"\\s*:\\s*true\\s*")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public static void handleLoginChallenge(final Browser br) throws AccountUnavailableException {
        final String json = br.getRegex("window._sharedData = (\\{.*?\\})</script>").getMatch(0);
        if (json != null) {
            Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final String possibleErrormessage = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/Challenge/{0}/extraData/content/{0}/title");
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* 2020-10-07: Unfinished code */
                if (!StringUtils.isEmpty(possibleErrormessage)) {
                    throw new AccountUnavailableException("Login challenge required: Complete in browser and try again or try cookie login: " + possibleErrormessage, 5 * 60 * 1000l);
                } else {
                    throw new AccountUnavailableException("Login challenge required: Complete in browser and try again or try cookie login", 5 * 60 * 1000l);
                }
            }
            // final ArrayList<Object> challenges = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "entry_data/Challenge");
            // boolean foundKnownChallenge = false;
            // for (final Object challengeO : challenges) {
            // entries = (Map<String, Object>) challengeO;
            // final String challengeType = (String) entries.get("challengeType");
            // if (StringUtils.isEmpty(challengeType)) {
            // continue;
            // }
            // if (challengeType.equalsIgnoreCase("SelectVerificationMethodForm")) {
            // foundKnownChallenge = true;
            // /* Take the simplest way: Auto-select first option and ask user for verification code */
            // entries = (Map<String, Object>) entries.get("fields");
            // /* Assume it's mail verification */
            // final String email = (String) entries.get("email");
            // if (StringUtils.isEmpty(email)) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // PostRequest loginChoiceRequest = new PostRequest(br.getURL());
            // loginChoiceRequest.setHeaders(ajaxHeaders);
            // loginChoiceRequest.setContentType("application/x-www-form-urlencoded");
            // post.setPostDataString("choice=1");
            // br.getPage(loginChoiceRequest);
            // entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            // final Object twoFaTextO = JavaScriptEngineFactory.walkJson(entries, "extraData/content/{1}/text");
            // final String twoFaText;
            // if (twoFaTextO != null && twoFaTextO instanceof String) {
            // twoFaText = (String) twoFaTextO;
            // } else {
            // twoFaText = "2 Factor Authenication\r\nPlease enter in the 6 digit code within your Instagram linked email account";
            // }
            // final DownloadLink dummyLink = new DownloadLink(null, "Account 2 Factor Auth", MAINPAGE, br.getURL(), true);
            // final String code = getUserInput(twoFaText, dummyLink);
            // if (code == null) {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid 2 Factor response format",
            // PluginException.VALUE_ID_PREMIUM_DISABLE);
            // }
            // post.setPostDataString("security_code=" + code);
            // br.getPage(post);
            // entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            // final String status = (String) entries.get("status");
            // if (!"success".equalsIgnoreCase(status)) {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n2FA login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
            // }
            // /* TODO: Fully implement this */
            // } else {
            // /* Unknown challenge-type */
            // }
            // }
            // if (!foundKnownChallenge) {
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnknown login challenge: Try cookie login",
            // PluginException.VALUE_ID_PREMIUM_DISABLE);
            // }
        }
    }

    /** Checks loggedin state based on html code (NOT cookies!) */
    private boolean isLoggedIn() {
        // return br.getCookies(MAINPAGE).get("sessionid", Cookies.NOTDELETEDPATTERN) != null && br.getCookies(MAINPAGE).get("ds_user_id",
        // Cookies.NOTDELETEDPATTERN) != null;
        final String fullname = PluginJSonUtils.getJson(br, "full_name");
        final String has_profile_pic = PluginJSonUtils.getJson(br, "has_profile_pic");
        return fullname != null && has_profile_pic != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            login(account, true);
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Free Account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
        /* We're already logged in - no need to login again here! */
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.handleDownload(link);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal("instagram.com", 250);
    }

    public static Browser prepBRWebsite(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");
        br.setCookie(MAINPAGE, "ig_pr", "1");
        // 429 == too many requests, we need to rate limit requests.
        br.setAllowedResponseCodes(new int[] { 400, 429 });
        br.setFollowRedirects(true);
        return br;
    }

    private void setConfigElements() {
        final ConfigEntry cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_SERVER_FILENAMES, "Use server-filenames whenever possible?").setDefaultValue(defaultPREFER_SERVER_FILENAMES);
        getConfig().addEntry(cfg);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ADD_ORDERID_TO_FILENAMES, "Add order-ID to filenames if an album contains more than one element?\r\nWarning: Turning this off may lead to duplicated filenames and skipped downloads!").setDefaultValue(defaultADD_ORDERID_TO_FILENAMES).setEnabledCondidtion(cfg, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY, "Try to download original quality (bigger filesize, without image-effects)? [This will slow down the download-process!]").setDefaultValue(defaultATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), QUIT_ON_RATE_LIMIT_REACHED, "Abort crawl process once rate limit is reached?").setDefaultValue(defaultQUIT_ON_RATE_LIMIT_REACHED));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), HASHTAG_CRAWLER_FIND_USERNAMES, "Crawl usernames for filenames when crawling '/explore/tags/<hashtag>' URLs? (Can slow down the crawl-process!)").setDefaultValue(defaultHASHTAG_CRAWLER_FIND_USERNAMES));
        final ConfigEntry grabXitems = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ONLY_GRAB_X_ITEMS, "Only grab the X latest items?").setDefaultValue(defaultONLY_GRAB_X_ITEMS);
        getConfig().addEntry(grabXitems);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), ONLY_GRAB_X_ITEMS_NUMBER, "How many items shall be grabbed?", defaultONLY_GRAB_X_ITEMS_NUMBER, 1025, defaultONLY_GRAB_X_ITEMS_NUMBER).setDefaultValue(defaultONLY_GRAB_X_ITEMS_NUMBER).setEnabledCondidtion(grabXitems, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER, "How many items shall be grabbed (for '/explore/tags/example')?", defaultONLY_GRAB_X_ITEMS_NUMBER, 10000, defaultONLY_GRAB_X_ITEMS_NUMBER).setDefaultValue(defaultONLY_GRAB_X_ITEMS_NUMBER));
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
