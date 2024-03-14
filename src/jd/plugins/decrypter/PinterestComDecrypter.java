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
package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.PinterestCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PinterestCom.class })
public class PinterestComDecrypter extends PluginForDecrypt {
    public PinterestComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    public static List<String[]> getPluginDomains() {
        return PinterestCom.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:(?:www|[a-z]{2})\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    private boolean             enable_crawl_alternative_URL = false;
    public static final String  TYPE_PIN                     = "(?i)https?://[^/]+/pin/([A-Za-z0-9\\-_]+)/?";
    private static final String TYPE_BOARD                   = "(?i)https?://[^/]+/([^/]+)/([^/]+)/?";
    private static final String TYPE_BOARD_SECTION           = "(?i)https?://[^/]+/([^/]+)/([^/]+)/([^/]+)/?";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        currentUsername = null;
        currentBoardSlug = null;
        currentBoardPath = null;
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        enable_crawl_alternative_URL = hostPlugin.getPluginConfig().getBooleanProperty(PinterestCom.ENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS, PinterestCom.defaultENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS);
        final String url = param.getCryptedUrl();
        final Regex singlepinregex = (new Regex(url, TYPE_PIN));
        if (singlepinregex.patternFind()) {
            return crawlSinglePIN(singlepinregex.getMatch(0));
        } else if (new Regex(url, TYPE_BOARD_SECTION).patternFind()) {
            return this.crawlSection(param);
        } else {
            if (true) {
                return crawlAllOtherItems(param.getCryptedUrl());
            } else {
                /* TODO: Remove this code after 06/2024. */
                return crawlBoardPINs(param.getCryptedUrl());
            }
        }
    }

    private String currentUsername  = null;
    private String currentBoardSlug = null;
    private String currentBoardPath = null;

    /**
     * One function which can handle _any_ type of supported pinterest link (except for single PIN links). </br>
     * WORK IN PROGRESS
     */
    private ArrayList<DownloadLink> crawlAllOtherItems(final String contenturl) throws Exception {
        /* Login whenever possible to be able to crawl private pinterest boards. */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final PinterestCom hostPlugin = (PinterestCom) this.getNewPluginForHostInstance(this.getHost());
            hostPlugin.login(account, false);
        }
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String json = br.getRegex("<script id=\"__PWS_DATA__\" type=\"application/json\">(\\{.*?)</script>").getMatch(0);
        final Map<String, Object> root = restoreFromString(json, TypeRef.MAP);
        final Map<String, Object> initialReduxState = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "props/initialReduxState");
        final Map<String, Object> resources = (Map<String, Object>) initialReduxState.get("resources");
        // final Map<String, Object> resourcesUserResource = (Map<String, Object>) resources.get("UserResource");
        int expectedNumberofItems = 0;
        final Map<String, Object> resourcesBoardResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(resources, "BoardResource/{0}/data");
        final Map<String, Object> resourcesBoardFeedResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(resources, "BoardFeedResource/{0}");
        if (resourcesBoardResource != null && resourcesBoardFeedResource != null) {
            /* This is a board -> Crawl all PINs from this board */
            currentBoardSlug = "";
            currentBoardPath = resourcesBoardResource.get("url").toString();
            final HashSet<String> boardSectionsPIN_IDs = new HashSet<String>();
            final String boardName = resourcesBoardResource.get("name").toString();
            final String boardID = resourcesBoardResource.get("id").toString();
            final String boardDescription = resourcesBoardResource.get("seo_description").toString();
            final int boardTotalPinCount = ((Number) resourcesBoardResource.get("pin_count")).intValue();
            final int boardSectionCount = ((Number) resourcesBoardResource.get("section_count")).intValue();
            if (boardSectionCount > 0) {
                logger.info("Crawling all sections of board" + boardName + " | " + boardSectionCount);
                final boolean useAsyncHandling = false;
                this.displayBubblenotifyMessage("Board " + boardName + " | sections", "Crawling all " + boardSectionCount + " sections of board " + boardName);
                if (useAsyncHandling) {
                    expectedNumberofItems += boardSectionCount;
                    final Map<String, Object> postDataOptions = new HashMap<String, Object>();
                    postDataOptions.put("board_id", boardID);
                    final Map<String, Object> postData = new HashMap<String, Object>();
                    postData.put("options", postDataOptions);
                    postData.put("context", new HashMap<String, Object>());
                    final Map<String, Object> resourcesBoardSectionsResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(resources, "BoardSectionsResource/{0}");
                    ret.addAll(this.crawlPaginationGeneric("BoardSectionsResource", resourcesBoardSectionsResource, postData, boardSectionCount, null, true));
                } else {
                    /*
                     * The hard way: First crawl all PINs in sections so that we can ignore those when crawling all sectionless board PINs
                     */
                    final ArrayList<DownloadLink> sectionPINsResult = this.crawlSections(boardDescription, boardID, boardName, br, contenturl);
                    for (final DownloadLink result : sectionPINsResult) {
                        final String pinStr = new Regex(result.getPluginPatternMatcher(), "(?i)/pin/(\\d+)").getMatch(0);
                        if (pinStr != null) {
                            boardSectionsPIN_IDs.add(pinStr);
                        }
                        ret.add(result);
                    }
                    logger.info("Total found PINs inside sections: " + boardSectionsPIN_IDs.size() + "/" + boardTotalPinCount);
                }
            }
            final int sectionlessPinCount = boardTotalPinCount - boardSectionsPIN_IDs.size();
            if (sectionlessPinCount > 0) {
                /* Crawl all loose/sectionless PINs */
                logger.info("Crawling all sectionless PINs of board" + boardName + " | " + boardTotalPinCount);
                this.displayBubblenotifyMessage("Board " + boardName + " | Sectionless PINs", "Crawling " + sectionlessPinCount + " sectionless PINs of board " + boardName);
                expectedNumberofItems += boardTotalPinCount;
                final int maxItemsPerPage = 15;
                final Map<String, Object> postDataOptions = new HashMap<String, Object>();
                postDataOptions.put("add_vase", true);
                postDataOptions.put("board_id", boardID);
                postDataOptions.put("field_set_key", "react_grid_pin");
                postDataOptions.put("filter_section_pins", false);
                postDataOptions.put("is_react", true);
                postDataOptions.put("prepend", false);
                postDataOptions.put("page_size", maxItemsPerPage);
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("options", postDataOptions);
                postData.put("context", new HashMap<String, Object>());
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(boardName);
                if (!StringUtils.isEmpty(boardDescription)) {
                    fp.setComment(boardDescription);
                }
                fp.setPackageKey("pinterest://board/" + boardID);
                ret.addAll(this.crawlPaginationGeneric("BoardFeedResource", resourcesBoardFeedResource, postData, boardTotalPinCount, fp, true));
            } else {
                logger.info("Skipping board " + boardName + " because it does not contain any [sectionless] items.");
            }
        }
        final Map<String, Object> resourcesUserPinsResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(resources, "UserPinsResource/{0}");
        if (resourcesUserPinsResource != null) {
            /* This is a user/profile -> Crawl all loose PINs from this profile */
            /* Find user-map */
            final Map<String, Object> usersmap = (Map<String, Object>) initialReduxState.get("users");
            Map<String, Object> usermap = null;
            if (usersmap != null) {
                for (final Object mapO : usersmap.values()) {
                    final Map<String, Object> map = (Map<String, Object>) mapO;
                    if (map.containsKey("full_name")) {
                        usermap = map;
                        break;
                    }
                }
            }
            if (usermap == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String username = usermap.get("username").toString();
            final String userID = usermap.get("id").toString();
            final int userPinCount = ((Integer) usermap.get("pin_count")).intValue();
            final int userBoardCount = ((Integer) usermap.get("board_count")).intValue();
            if (userPinCount > 0) {
                logger.info("Crawling all loose PINs: " + userPinCount);
                this.displayBubblenotifyMessage("Profile " + username, "Crawling all " + userPinCount + " loose PINs of profile " + username);
                expectedNumberofItems += userPinCount;
                final Map<String, Object> postDataOptions = new HashMap<String, Object>();
                postDataOptions.put("add_vase", true);
                postDataOptions.put("field_set_key", "mobile_grid_item");
                postDataOptions.put("is_own_profile_pins", false);
                postDataOptions.put("username", username);
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("options", postDataOptions);
                postData.put("context", new HashMap<String, Object>());
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(username);
                final String description = (String) usermap.get("seo_description");
                if (!StringUtils.isEmpty(description)) {
                    fp.setComment(description);
                }
                fp.setPackageKey("pinterest://profile_sectionless_pins/" + userID);
                ret.addAll(this.crawlPaginationGeneric("UserPinsResource", resourcesUserPinsResource, postData, userPinCount, fp, true));
            } else {
                logger.info("Skipping profile " + username + " PINs because this profile does not contain any items.");
            }
            /* TODO: Maybe add a setting for this */
            final boolean allowCrawlAllBoardsOfUser = false;
            if (userBoardCount > 0 && allowCrawlAllBoardsOfUser) {
                logger.info("Crawling all boards: " + userBoardCount);
                this.displayBubblenotifyMessage("Profile " + username, "Crawling all " + userBoardCount + " boards of profile " + username);
                final Map<String, Object> resourcesBoardsFeedResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(resources, "BoardsFeedResource/{0}");
                expectedNumberofItems += userBoardCount;
                final Map<String, Object> postDataOptions = new HashMap<String, Object>();
                postDataOptions.put("field_set_key", "profile_grid_item");
                postDataOptions.put("filter_stories", false);
                postDataOptions.put("sort", "last_pinned_to");
                postDataOptions.put("username", username);
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("options", postDataOptions);
                postData.put("context", new HashMap<String, Object>());
                ret.addAll(this.crawlPaginationGeneric("BoardsFeedResource", resourcesBoardsFeedResource, postData, userBoardCount, null, true));
            }
        }
        if (expectedNumberofItems == 0) {
            /* Empty board/profile */
            throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlPaginationGeneric(final String resourceType, final Map<String, Object> startMap, final Map<String, Object> postData, final int expectedNumberofItems, final FilePackage fp, final boolean distributeResults) throws Exception {
        final String source_url = br._getURL().getPath();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) startMap.get("data");
        if (itemsList == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String nextbookmark = startMap.get("nextBookmark").toString();
        final Map<String, Object> postDataOptions = (Map<String, Object>) postData.get("options");
        final String boardID = (String) postDataOptions.get("board_id");
        /**
         * A page size is not always given. It is controlled serverside via the "bookmark" parameter. </br>
         * Any page can have any number of items.
         */
        final Number page_sizeO = (Number) postDataOptions.get("page_size");
        final int maxItemsPerPage = page_sizeO != null ? page_sizeO.intValue() : -1;
        int page = 1;
        int crawledItems = 0;
        final List<Integer> pagesWithPossiblyMissingItems = new ArrayList<Integer>();
        do {
            for (final Map<String, Object> item : itemsList) {
                final ArrayList<DownloadLink> results = proccessMap(item, boardID, fp, distributeResults);
                ret.addAll(results);
            }
            crawledItems += itemsList.size();
            logger.info("Crawled page: " + page + " | " + crawledItems + "/" + expectedNumberofItems + " items crawled | retSize=" + ret.size() + " | nextbookmark= " + nextbookmark);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (StringUtils.isEmpty(nextbookmark) || nextbookmark.equalsIgnoreCase("-end-")) {
                logger.info("Stopping because: Reached end");
                break;
            } else if (expectedNumberofItems != -1 && crawledItems >= expectedNumberofItems) {
                /* Fail-safe */
                logger.info("Stopping because: Found all sectionless items");
                break;
            } else {
                /* Continue to next page */
                /* Collect pages with possibly missing items. Only do this if we're not on the last page. */
                if (itemsList.size() < maxItemsPerPage) {
                    /* Fail-safe */
                    logger.info("Found page with possibly missing items: " + page);
                    pagesWithPossiblyMissingItems.add(page);
                }
                postDataOptions.put("bookmarks", new String[] { nextbookmark });
                br.getPage("/resource/" + resourceType + "/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postData)) + "&_=" + System.currentTimeMillis());
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> resource_response = (Map<String, Object>) entries.get("resource_response");
                nextbookmark = (String) resource_response.get("bookmark");
                itemsList = (List<Map<String, Object>>) resource_response.get("data");
                page += 1;
            }
        } while (!this.isAbort());
        final long numberofMissingItems = expectedNumberofItems != -1 ? expectedNumberofItems - crawledItems : 0;
        if (numberofMissingItems > 0) {
            /*
             * 2024-02-13: Sometimes items are missing for unknown reasons e.g. one is missing here:
             * https://www.pinterest.de/josielindatoth/deserts/
             */
            String msg = "Missing items: " + numberofMissingItems;
            if (pagesWithPossiblyMissingItems.size() > 0) {
                msg += "\nPages where those items should be located: " + pagesWithPossiblyMissingItems.toString();
            }
            this.displayBubblenotifyMessage("Missing PINs in package " + fp.getName(), msg);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlSinglePIN(final String pinID) throws Exception {
        if (pinID == null) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String contenturl = "https://www." + this.getHost() + "/pin/" + pinID + "/";
        final DownloadLink singlePIN = this.createDownloadlink(contenturl);
        if (enable_crawl_alternative_URL) {
            /* The more complicated way (if wished by user). */
            /**
             * 2021-03-02: PINs may redirect to other PINs in very rare cases -> Handle that </br>
             * If that wasn't the case, we could rely on API-only!
             */
            br.getPage(contenturl);
            String redirect = br.getRegex("window\\.location\\s*=\\s*\"([^\"]+)\"").getMatch(0);
            if (redirect != null) {
                /* We want the full URL. */
                redirect = br.getURL(redirect).toExternalForm();
            }
            if (!br.getURL().matches(PinterestComDecrypter.TYPE_PIN)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (redirect != null && redirect.matches(PinterestComDecrypter.TYPE_PIN) && !redirect.contains(pinID)) {
                final String newPinID = PinterestCom.getPinID(redirect);
                logger.info("Old pinID: " + pinID + " | New pinID: " + newPinID + " | New URL: " + redirect);
                contenturl = redirect;
            } else if (redirect != null && redirect.contains("show_error=true")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> pinMap = getPINMap(this.br, contenturl);
            setInfoOnDownloadLink(singlePIN, pinMap);
            final String externalURL = getAlternativeExternalURLInPINMap(pinMap);
            if (externalURL != null) {
                ret.add(this.createDownloadlink(externalURL));
            }
        }
        ret.add(singlePIN);
        return ret;
    }

    public static void setInfoOnDownloadLink(final DownloadLink dl, final Map<String, Object> map) {
        final String pin_id = PinterestCom.getPinID(dl.getPluginPatternMatcher());
        String filename = null;
        final Map<String, Object> data = map.containsKey("data") ? (Map<String, Object>) map.get("data") : map;
        // final String directlink = getDirectlinkFromPINMap(data);
        final List<String> directurlsList = getDirectlinkFromPINMap(data);
        final String directlink;
        if (directurlsList != null && !directurlsList.isEmpty()) {
            directlink = directurlsList.get(0);
        } else {
            directlink = null;
        }
        if (StringUtils.isEmpty(filename)) {
            filename = (String) data.get("title");
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = pin_id;
        } else {
            filename = Encoding.htmlDecode(filename).trim();
            filename = pin_id + "_" + filename;
        }
        final String description = (String) data.get("description");
        final String ext;
        if (!StringUtils.isEmpty(directlink)) {
            if (directlink.contains(".m3u8")) {
                /* HLS stream */
                ext = ".mp4";
            } else {
                ext = getFileNameExtensionFromString(directlink, ".jpg");
            }
        } else {
            ext = ".jpg";
        }
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(dl.getHost());
        if (hostPlugin.getPluginConfig().getBooleanProperty(PinterestCom.ENABLE_DESCRIPTION_IN_FILENAMES, PinterestCom.defaultENABLE_DESCRIPTION_IN_FILENAMES) && !StringUtils.isEmpty(description)) {
            filename += "_" + description;
        }
        if (!StringUtils.isEmpty(description) && dl.getComment() == null) {
            dl.setComment(description);
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (directurlsList != null && !directurlsList.isEmpty()) {
            dl.setProperty(PinterestCom.PROPERTY_DIRECTURL_LIST, directurlsList);
        }
        dl.setFinalFileName(filename);
        dl.setAvailable(true);
    }

    /** Accesses pinterest API and retrn map of PIN. */
    public static Map<String, Object> getPINMap(final Browser br, final String pinURL) throws Exception {
        final String pinID = PinterestCom.getPinID(pinURL);
        if (pinID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        List<Object> resource_data_cache = null;
        final String pin_json_url = "https://www.pinterest.com/resource/PinResource/get/?source_url=%2Fpin%2F" + pinID + "%2F&data=%7B%22options%22%3A%7B%22field_set_key%22%3A%22detailed%22%2C%22ptrf%22%3Anull%2C%22fetch_visual_search_objects%22%3Atrue%2C%22id%22%3A%22" + pinID + "%22%7D%2C%22context%22%3A%7B%7D%7D&module_path=Pin(show_pinner%3Dtrue%2C+show_board%3Dtrue%2C+is_original_pin_in_related_pins_grid%3Dtrue)&_=" + System.currentTimeMillis();
        br.getPage(pin_json_url);
        final Map<String, Object> root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.getRequest().getHtmlCode());
        if (root.containsKey("resource_data_cache")) {
            resource_data_cache = (List) root.get("resource_data_cache");
        } else {
            /* 2020-02-17 */
            final Object pinO = root.get("resource_response");
            if (pinO != null) {
                resource_data_cache = new ArrayList<Object>();
                resource_data_cache.add(pinO);
            }
        }
        if (resource_data_cache == null) {
            return null;
        }
        for (final Object resource_object : resource_data_cache) {
            final Map<String, Object> map = (Map<String, Object>) resource_object;
            final String this_pin_id = (String) JavaScriptEngineFactory.walkJson(map, "data/id");
            if (StringUtils.equals(this_pin_id, pinID) || resource_data_cache.size() == 1) {
                /* We've reached our goal */
                return map;
            }
        }
        /* PIN does not exist(?) */
        return null;
    }

    /** Returns highest resolution image URL inside given PIN Map. */
    public static List<String> getDirectlinkFromPINMap(final Map<String, Object> map) {
        final List<String> ret = new ArrayList<String>();
        // TODO: Return list of possible URLs here since sometimes e.g. one/the "best" image quality is unavailable while another one is
        // available.
        /* First check if we have a video */
        final Map<String, Object> video_list = (Map<String, Object>) (JavaScriptEngineFactory.walkJson(map, "videos/video_list"));
        if (video_list != null) {
            for (final String knownVideoQualities : new String[] { "V_1080P", "V_720P", "V_480P" }) {
                final Map<String, Object> video = (Map<String, Object>) video_list.get(knownVideoQualities);
                if (video == null) {
                    /* Video quality doesn't exist */
                    continue;
                }
                final String videourl = (String) video.get("url");
                if (!StringUtils.isEmpty(videourl)) {
                    ret.add(videourl.toString());
                    return ret;
                }
            }
            /* No known video quality was found */
        }
        /* No video --> Must be photo item */
        final Map<String, Object> imagesmap = (Map<String, Object>) map.get("images");
        if (imagesmap != null) {
            /* Original image NOT available --> Take the best we can find */
            String originalImageURL = null;
            String bestNonOriginalImage = null;
            int bestHeight = -1;
            final Iterator<Entry<String, Object>> it = imagesmap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                final String label = entry.getKey();
                final Map<String, Object> imagemap = (Map<String, Object>) entry.getValue();
                final int height = ((Number) imagemap.get("height")).intValue();
                final String imageurl = imagemap.get("url").toString();
                if (label.equalsIgnoreCase("orig")) {
                    originalImageURL = imageurl;
                }
                if (bestNonOriginalImage == null || height > bestHeight) {
                    bestNonOriginalImage = imageurl;
                    bestHeight = height;
                }
            }
            if (originalImageURL != null) {
                ret.add(originalImageURL);
            }
            ret.add(bestNonOriginalImage);
            return ret;
        }
        return null;
    }

    /** Returns e.g. an alternative, probably higher quality imgur.com URL to the same image which we have as Pinterest PIN here. */
    private String getAlternativeExternalURLInPINMap(final Map<String, Object> pinMap) {
        String externalURL = null;
        try {
            String path;
            if (pinMap.containsKey("data")) {
                path = "data/rich_metadata/url";
            } else {
                path = "rich_metadata/url";
            }
            externalURL = (String) JavaScriptEngineFactory.walkJson(pinMap, path);
        } catch (final Throwable e) {
        }
        return externalURL;
    }

    /** Crawls a section for CryptedLink items which have properties set which are needed to crawl a section (new method 2024). */
    private ArrayList<DownloadLink> crawlSection(final CryptedLink param) throws Exception {
        final String url = param.getCryptedUrl();
        final Regex boardSectionRegex = new Regex(url, TYPE_BOARD_SECTION);
        if (!boardSectionRegex.patternFind()) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        final DownloadLink sourceItem = param.getDownloadLink();
        if (sourceItem == null) {
            logger.info("Section URL without DownloadLink context");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Obtain cached data */
        final String boardID = sourceItem.getStringProperty("board_id");
        final String sectionID = sourceItem.getStringProperty("section_id");
        if (boardID == null || sectionID == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String username = boardSectionRegex.getMatch(0);
        final String boardname = boardSectionRegex.getMatch(1);
        final String sectionname = boardSectionRegex.getMatch(2);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + " - " + boardname + " - " + sectionname);
        fp.setPackageKey("pinterest://board/" + boardID + "/section/" + sectionID);
        return this.crawlSection(br, url, boardID, sectionID, fp);
    }

    private ArrayList<DownloadLink> crawlSection(final Browser br, final String source_url, final String boardID, final String sectionID, final FilePackage fp) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int processedPINCounter = 0;
        int pageCounter = 1;
        /* Single section pagination */
        // final String url_section = "https://www.pinterest.com/" + source_url + section_title + "/";
        final int maxPINsPerRequest = 25;
        final Map<String, Object> pinPaginationPostDataOptions = new HashMap<String, Object>();
        pinPaginationPostDataOptions.put("isPrefetch", false);
        pinPaginationPostDataOptions.put("currentFilter", -1);
        pinPaginationPostDataOptions.put("field_set_key", "react_grid_pin");
        pinPaginationPostDataOptions.put("is_own_profile_pins", false);
        pinPaginationPostDataOptions.put("page_size", maxPINsPerRequest);
        pinPaginationPostDataOptions.put("redux_normalize_feed", true);
        pinPaginationPostDataOptions.put("section_id", sectionID);
        pinPaginationPostDataOptions.put("no_fetch_context_on_resource", false);
        final Map<String, Object> pinPaginationpostDataContext = new HashMap<String, Object>();
        Map<String, Object> pinPaginationPostData = new HashMap<String, Object>();
        pinPaginationPostData.put("options", pinPaginationPostDataOptions);
        pinPaginationPostData.put("context", pinPaginationpostDataContext);
        do {
            String url = "/resource/BoardSectionPinsResource/get/?source_url=" + URLEncode.encodeURIComponent(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(pinPaginationPostData)) + "&_=" + System.currentTimeMillis();
            if (br.getRequest() == null) {
                /* First request */
                url = "https://" + getHost() + url;
            }
            br.getPage(url);
            final Map<String, Object> sectionPaginationInfo = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Object bookmarksO = JavaScriptEngineFactory.walkJson(sectionPaginationInfo, "resource/options/bookmarks");
            final String bookmarks = (String) JavaScriptEngineFactory.walkJson(sectionPaginationInfo, "resource/options/bookmarks/{0}");
            final List<Map<String, Object>> pins = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(sectionPaginationInfo, "resource_response/data");
            int numberofNewItemsThisPage = 0;
            for (final Map<String, Object> pinmap : pins) {
                final ArrayList<DownloadLink> thisRet = proccessMap(pinmap, boardID, fp, true);
                ret.addAll(thisRet);
                numberofNewItemsThisPage++;
            }
            processedPINCounter += pins.size();
            logger.info("Crawled section " + sectionID + " page: " + pageCounter + "Processed items on this page: " + numberofNewItemsThisPage + " | Processed PINs so far: " + processedPINCounter);
            if (this.isAbort()) {
                logger.info("Crawler aborted by user");
                break;
            } else if (StringUtils.isEmpty(bookmarks) || bookmarks.equals("-end-") || bookmarksO == null) {
                /* Looks as if we've reached the end */
                logger.info("Stopping because: Reached end");
                break;
            } else if (numberofNewItemsThisPage == 0) {
                /* Fail safe */
                logger.info("Stopping because: Current page did not contain any new items");
                break;
            } else {
                /* Load next page */
                pinPaginationPostDataOptions.put("bookmarks", bookmarksO);
                pageCounter++;
            }
        } while (true);
        logger.info("Number of PINs in current section: " + processedPINCounter);
        return ret;
    }

    /**
     * Crawls single PIN from given Map.
     *
     * @throws IOException
     */
    private ArrayList<DownloadLink> proccessMap(final Map<String, Object> map, final String board_id, final FilePackage fp, final boolean distributeResults) throws PluginException, IOException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String type = map.get("type").toString();
        if (type.equals("pin") || type.equals("interest")) {
            final Map<String, Object> user = (Map<String, Object>) map.get("pinner");
            final String pin_id = map.get("id").toString();
            final String username = user != null ? user.get("username").toString() : null;
            final DownloadLink dl = this.createDownloadlink("https://www." + this.getHost() + "/pin/" + pin_id + "/");
            if (!StringUtils.isEmpty(board_id)) {
                dl.setProperty("boardid", board_id);
            }
            if (!StringUtils.isEmpty(username)) {
                dl.setProperty("username", username);
            }
            setInfoOnDownloadLink(dl, map);
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            ret.add(dl);
            String externalURL = null;
            if (this.enable_crawl_alternative_URL && (externalURL = getAlternativeExternalURLInPINMap(map)) != null) {
                ret.add(this.createDownloadlink(externalURL));
            }
        } else if (type.equals("board")) {
            final String boardID = map.get("id").toString();
            final String boardURL = map.get("url").toString();
            final String fullurl = br.getURL(boardURL).toExternalForm();
            final DownloadLink dl = this.createDownloadlink(fullurl);
            dl.setProperty("board_id", boardID);
            ret.add(dl);
        } else if (type.equals("board_section")) {
            if (currentBoardPath == null) {
                /* Developer mistake */
                throw new IllegalArgumentException();
            }
            final String sectionSlug = map.get("slug").toString();
            final DownloadLink section = this.createDownloadlink("https://" + getHost() + currentBoardPath + sectionSlug);
            /* Important for next crawl-round */
            section.setProperty("board_id", board_id);
            section.setProperty("section_id", map.get("id"));
            ret.add(section);
        } else {
            logger.info("Ignoring invalid type: " + type);
        }
        if (distributeResults) {
            distribute(ret);
        }
        return ret;
    }

    /** Recursive function to find the ID of a sectionSlug. */
    private String recursiveFindSectionID(final Object jsono, final String sectionSlug) throws PluginException {
        if (jsono instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) jsono;
            final Object slugO = map.get("slug");
            if (slugO != null && slugO instanceof String && slugO.toString().equals(sectionSlug)) {
                return map.get("id").toString();
            }
            final Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                // final String key = entry.getKey();
                final Object value = entry.getValue();
                if (value instanceof List || value instanceof Map) {
                    final String result = recursiveFindSectionID(value, sectionSlug);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } else if (jsono instanceof ArrayList) {
            final List<Object> ressourcelist = (List<Object>) jsono;
            for (final Object arrayo : ressourcelist) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final String result = recursiveFindSectionID(arrayo, sectionSlug);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private void prepAPIBRCrawler(final Browser br) throws PluginException {
        /* 2021-03-01: Not needed anymore */
        // PinterestCom.prepAPIBR(br);
        br.setAllowedResponseCodes(new int[] { 503, 504 });
        br.setLoadLimit(br.getLoadLimit() * 4);
    }

    private void displayBubblenotifyMessage(final String title, final String msg) {
        BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
            @Override
            public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                return new BasicNotify("Pinterest: " + title, msg, new AbstractIcon(IconKey.ICON_INFO, 32));
            }
        });
    }

    private ArrayList<DownloadLink> crawlBoardPINs(final String contenturl) throws Exception {
        String targetSectionSlug = null;
        final String username = URLEncode.decodeURIComponent(new Regex(contenturl, TYPE_BOARD).getMatch(0));
        final String boardSlug = URLEncode.decodeURIComponent(new Regex(contenturl, TYPE_BOARD).getMatch(1));
        // final String sourceURL;
        if (contenturl.matches(TYPE_BOARD_SECTION)) {
            /* Remove targetSection from URL as we cannot use it in this way. */
            targetSectionSlug = new Regex(contenturl, TYPE_BOARD_SECTION).getMatch(2);
        }
        return crawlBoardPINs(username, boardSlug, targetSectionSlug, contenturl);
    }

    @Deprecated
    /**
     * Deprecated since 2024-02-15. Use {@link #crawlAllOtherItems(String)}
     */
    private ArrayList<DownloadLink> crawlBoardPINs(final String username, final String boardSlug, final String targetSectionSlug, final String contenturl) throws Exception {
        /*
         * In case the user wants to add a specific section, we have to get to the section overview --> Find sectionID --> Finally crawl
         * section PINs
         */
        if (username == null || boardSlug == null) {
            throw new IllegalArgumentException();
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String sourceURL = URLEncode.decodeURIComponent(new URL(contenturl).getPath());
        prepAPIBRCrawler(this.br);
        br.getPage("https://www." + this.getHost() + "/resource/BoardResource/get/?source_url=" + URLEncode.encodeURIComponent(sourceURL) + "style%2F&data=%7B%22options%22%3A%7B%22isPrefetch%22%3Afalse%2C%22username%22%3A%22" + URLEncode.encodeURIComponent(username) + "%22%2C%22slug%22%3A%22" + URLEncode.encodeURIComponent(boardSlug) + "%22%2C%22field_set_key%22%3A%22detailed%22%2C%22no_fetch_context_on_resource%22%3Afalse%7D%2C%22context%22%3A%7B%7D%7D&_=" + System.currentTimeMillis());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> jsonRoot = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> boardPageResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(jsonRoot, "resource_response/data");
        final String boardID = boardPageResource.get("id").toString();
        final long section_count = ((Number) boardPageResource.get("section_count")).longValue();
        /* Find out how many PINs we have to crawl. */
        final long totalPinCount = ((Number) boardPageResource.get("pin_count")).longValue();
        final long sectionlessPinCount = ((Number) boardPageResource.get("sectionless_pin_count")).longValue();
        final long totalInsideSectionsPinCount = (totalPinCount > 0 && sectionlessPinCount < totalPinCount) ? totalPinCount - sectionlessPinCount : 0;
        logger.info("PINs total: " + totalPinCount + " | PINs inside sections: " + totalInsideSectionsPinCount + " | PINs outside sections: " + sectionlessPinCount);
        if (totalPinCount == 0) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        /*
         * Sections are like folders. Now find all the PINs that are not in any sections (it may happen that we already have everything at
         * this stage!) Only decrypt these leftover PINs if either the user did not want to have a specified section only or if he wanted to
         * have a specified section only but we failed to find that.
         */
        br.getPage(contenturl);
        final String json = br.getRegex("<script id=\"__PWS_DATA__\" type=\"application/json\">(\\{.*?)</script>").getMatch(0);
        final Map<String, Object> tmpMap = restoreFromString(json, TypeRef.MAP);
        String targetSectionID = null;
        if (section_count > 0 && targetSectionSlug != null) {
            /* Small workaround to find sectionID (I've failed to find an API endpoint that returns this section only). */
            targetSectionID = this.recursiveFindSectionID(tmpMap, targetSectionSlug);
            if (targetSectionID == null) {
                logger.warning("Failed to crawl user desired section -> Crawling sectionless PINs only...");
            } else {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(username + " - " + boardSlug + " - " + targetSectionSlug);
                fp.setPackageKey("pinterest://board/" + boardID + "/section/" + targetSectionID);
                ret.addAll(this.crawlSection(br.cloneBrowser(), sourceURL, boardID, targetSectionID, fp));
                logger.info("Total number of PINs crawled in desired single section: " + ret.size());
            }
        } else if (totalInsideSectionsPinCount > 0) {
            ret.addAll(this.crawlSections(username, boardID, boardSlug, br.cloneBrowser(), contenturl));
            logger.info("Total number of PINs crawled in sections: " + ret.size());
        }
        if (sectionlessPinCount <= 0) {
            /* No items at all available */
            logger.info("This board doesn't contain any loose PINs");
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* We got results -> Return them */
                return ret;
            }
        }
        /* Find- and set PackageName (Board Name) */
        final String boardName = boardPageResource.get("name").toString();
        final Map<String, Object> boardFeedResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(tmpMap, "props/initialReduxState/resources/BoardFeedResource/{0}");
        List<Map<String, Object>> pinList = (List<Map<String, Object>>) boardFeedResource.get("data");
        String nextbookmark = boardFeedResource.get("nextBookmark").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(boardName);
        fp.setPackageKey("pinterest://board/" + boardID);
        final int maxItemsPerPage = 15;
        final Map<String, Object> postDataOptions = new HashMap<String, Object>();
        final String source_url = new URL(contenturl).getPath();
        postDataOptions.put("add_vase", true);
        postDataOptions.put("board_id", boardID);
        postDataOptions.put("field_set_key", "react_grid_pin");
        postDataOptions.put("filter_section_pins", false);
        postDataOptions.put("is_react", true);
        postDataOptions.put("prepend", false);
        postDataOptions.put("page_size", maxItemsPerPage);
        final Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("options", postDataOptions);
        postData.put("context", new HashMap<String, Object>());
        int page = 1;
        int crawledSectionlessPINs = 0;
        logger.info("Crawling all sectionless PINs: " + sectionlessPinCount);
        final List<Integer> pagesWithPossiblyMissingItems = new ArrayList<Integer>();
        do {
            for (final Map<String, Object> pin : pinList) {
                proccessMap(pin, boardID, fp, true);
            }
            crawledSectionlessPINs += pinList.size();
            logger.info("Crawled sectionless PINs page: " + page + " | " + crawledSectionlessPINs + "/" + sectionlessPinCount + " PINs crawled | nextbookmark= " + nextbookmark);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (StringUtils.isEmpty(nextbookmark) || nextbookmark.equalsIgnoreCase("-end-")) {
                logger.info("Stopping because: Reached end");
                break;
            } else if (crawledSectionlessPINs >= sectionlessPinCount) {
                /* Fail-safe */
                logger.info("Stopping because: Found all sectionless items");
                break;
            } else {
                /* Continue to next page */
                /* Collect pages with possibly missing items. Only do this if we're not on the last page. */
                if (pinList.size() < maxItemsPerPage) {
                    /* Fail-safe */
                    logger.info("Found page with possibly missing items: " + page);
                    pagesWithPossiblyMissingItems.add(page);
                }
                postDataOptions.put("bookmarks", new String[] { nextbookmark });
                br.getPage("/resource/BoardFeedResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postData)) + "&_=" + System.currentTimeMillis());
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> resource_response = (Map<String, Object>) entries.get("resource_response");
                nextbookmark = (String) resource_response.get("bookmark");
                pinList = (List<Map<String, Object>>) resource_response.get("data");
                page += 1;
            }
        } while (!this.isAbort());
        final long numberofMissingItems = sectionlessPinCount - crawledSectionlessPINs;
        if (numberofMissingItems > 0) {
            /*
             * 2024-02-13: Sometimes items are missing for unknown reasons e.g. one is missing here:
             * https://www.pinterest.de/josielindatoth/deserts/
             */
            String msg = "Missing items: " + numberofMissingItems;
            if (pagesWithPossiblyMissingItems.size() > 0) {
                msg += "\nPages where those items should be located: " + pagesWithPossiblyMissingItems.toString();
            }
            this.displayBubblenotifyMessage("Missing PINs in board " + boardName, msg);
        }
        return ret;
    }

    /**
     * @return: true: target section was found and only this will be crawler false: failed to find target section - in this case we should
     *          crawl everything we find </br>
     *          This can crawl A LOT of stuff! E.g. a board contains 1000 sections, each section contains 1000 PINs...
     */
    @Deprecated
    private ArrayList<DownloadLink> crawlSections(final String username, final String boardID, final String boardName, final Browser ajax, final String contenturl) throws Exception {
        if (username == null || boardID == null || boardName == null) {
            throw new IllegalArgumentException();
        }
        final Map<String, Object> postDataOptions = new HashMap<String, Object>();
        final String source_url = new URL(contenturl).getPath();
        postDataOptions.put("isPrefetch", false);
        postDataOptions.put("board_id", boardID);
        postDataOptions.put("redux_normalize_feed", true);
        postDataOptions.put("no_fetch_context_on_resource", false);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("options", postDataOptions);
        postData.put("context", new HashMap<String, Object>());
        int sectionPage = -1;
        ajax.getPage("/resource/BoardSectionsResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postData)));
        final int maxSectionsPerPage = 25;
        sectionPagination: do {
            sectionPage += 1;
            logger.info("Crawling sections page: " + (sectionPage + 1));
            final Map<String, Object> sectionsData = restoreFromString(ajax.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> sections = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(sectionsData, "resource_response/data");
            int sectionCounter = 1;
            for (final Map<String, Object> entries : sections) {
                final String section_title = entries.get("title").toString();
                // final String sectionSlug = (String) entries.get("slug");
                // final long section_total_pin_count = ((Number) entries.get("pin_count")).longValue();
                final String sectionID = entries.get("id").toString();
                logger.info("Crawling section " + sectionCounter + " of " + sections.size() + " --> ID = " + sectionID);
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(username + " - " + boardName + " - " + section_title);
                fp.setPackageKey("pinterest://board/" + boardID + "/section/" + sectionID);
                ret.addAll(crawlSection(ajax, source_url, boardID, sectionID, fp));
                sectionCounter += 1;
                if (this.isAbort()) {
                    break sectionPagination;
                }
            }
            final String sectionsNextBookmark = (String) JavaScriptEngineFactory.walkJson(sectionsData, "resource_response/bookmark");
            if (StringUtils.isEmpty(sectionsNextBookmark) || sectionsNextBookmark.equalsIgnoreCase("-end-")) {
                logger.info("Stopping sections crawling because: Reached end");
                break sectionPagination;
            } else if (sections.size() < maxSectionsPerPage) {
                /* Fail safe */
                logger.info("Stopping because: Current page contains less than " + maxSectionsPerPage + " items");
                break sectionPagination;
            } else {
                postDataOptions.put("bookmarks", new String[] { sectionsNextBookmark });
                ajax.getPage("/resource/BoardSectionsResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postData)) + "&_=" + System.currentTimeMillis());
            }
        } while (!this.isAbort());
        logger.info("Section crawler done");
        return ret;
    }
}
