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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
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
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.PinterestCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pinterest.com" }, urls = { "https?://(?:(?:www|[a-z]{2})\\.)?pinterest\\.(?:at|com|de|fr|it|es|co\\.uk)/(pin/[A-Za-z0-9\\-_]+/|[^/]+/[^/]+/(?:[^/]+/)?)" })
public class PinterestComDecrypter extends PluginForDecrypt {
    public PinterestComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final boolean    force_api_usage                     = true;
    private ArrayList<DownloadLink> decryptedLinks                      = null;
    private ArrayList<String>       dupeList                            = new ArrayList<String>();
    /* Reset this after every function use e.g. crawlSections --> Reset --> crawlBoardPINs TODO: Remove this public variable */
    private FilePackage             fp                                  = null;
    private boolean                 loggedIN                            = false;
    private boolean                 enable_description_inside_filenames = false;
    private boolean                 enable_crawl_alternative_URL        = false;
    private static final String     TYPE_PIN                            = ".+/pin/.+";
    private static final String     TYPE_BOARD                          = "https?://[^/]+/([^/]+)/([^/]+)/?";
    private static final String     TYPE_BOARD_SECTION                  = "https?://[^/]+/[^/]+/[^/]+/([^/]+)/?";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        enable_description_inside_filenames = hostPlugin.getPluginConfig().getBooleanProperty(PinterestCom.ENABLE_DESCRIPTION_IN_FILENAMES, PinterestCom.defaultENABLE_DESCRIPTION_IN_FILENAMES);
        enable_crawl_alternative_URL = hostPlugin.getPluginConfig().getBooleanProperty(PinterestCom.ENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS, PinterestCom.defaultENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS);
        /* Correct link - remove country related language-subdomains (e.g. 'es.pinterest.com'). */
        param.setCryptedUrl("https://www.pinterest.com" + new URL(param.getCryptedUrl()).getPath());
        fp = FilePackage.getInstance();
        br.setFollowRedirects(true);
        /* Sometimes html can be very big */
        br.setLoadLimit(br.getLoadLimit() * 4);
        loggedIN = getUserLogin(false);
        if (new Regex(param.getCryptedUrl(), TYPE_PIN).matches()) {
            parseSinglePIN(param);
        } else {
            crawlBoardPINs(param);
        }
        return decryptedLinks;
    }

    private void parseSinglePIN(final CryptedLink param) throws Exception {
        final DownloadLink singlePIN = this.createPINDownloadUrl(param.getCryptedUrl());
        final String pin_id = jd.plugins.hoster.PinterestCom.getPinID(param.getCryptedUrl());
        if (enable_crawl_alternative_URL) {
            try {
                final Map<String, Object> pinMap = findPINMap(this.br, this.loggedIN, param.getCryptedUrl(), null, null, null);
                setInfoOnDownloadLink(singlePIN, pinMap, null, loggedIN);
                final String externalURL = getAlternativeExternalURLInPINMap(pinMap);
                if (externalURL != null) {
                    this.decryptedLinks.add(this.createDownloadlink(externalURL));
                }
                fp.setName(singlePIN.getRawName());
            } catch (final PluginException e) {
                /* Offline */
                singlePIN.setAvailable(false);
                /* Fallback */
                fp.setName(pin_id);
            }
        } else {
            fp.setName(pin_id);
        }
        this.decryptedLinks.add(singlePIN);
        fp.addLinks(this.decryptedLinks);
    }

    public static void setInfoOnDownloadLink(final DownloadLink dl, final Map<String, Object> pinMap, String directlink, final boolean loggedIN) {
        final String pin_id = jd.plugins.hoster.PinterestCom.getPinID(dl.getDownloadURL());
        String filename = null;
        final String description;
        if (pinMap != null) {
            if (StringUtils.isEmpty(directlink)) {
                directlink = getDirectlinkFromPINMap(pinMap);
            }
            final Map<String, Object> data = pinMap.containsKey("data") ? (Map<String, Object>) pinMap.get("data") : pinMap;
            if (loggedIN) {
                try {
                    final Map<String, Object> page_info = (Map<String, Object>) pinMap.get("page_info");
                    filename = (String) page_info.get("title");
                } catch (final Throwable e) {
                }
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
            description = (String) data.get("description");
        } else {
            filename = dl.getFinalFileName();
            if (filename == null) {
                filename = pin_id;
            }
            description = jd.plugins.hoster.PinterestCom.getPictureDescription(dl);
        }
        final String ext;
        if (!StringUtils.isEmpty(directlink)) {
            ext = getFileNameExtensionFromString(directlink, ".jpg");
        } else {
            ext = ".jpg";
        }
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(dl.getHost());
        if (hostPlugin.getPluginConfig().getBooleanProperty(jd.plugins.hoster.PinterestCom.ENABLE_DESCRIPTION_IN_FILENAMES, jd.plugins.hoster.PinterestCom.defaultENABLE_DESCRIPTION_IN_FILENAMES) && description != null) {
            filename += "_" + description;
        }
        if (!StringUtils.isEmpty(description) && dl.getComment() == null) {
            dl.setComment(description);
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (directlink != null) {
            dl.setFinalFileName(filename);
            dl.setProperty("free_directlink", directlink);
        } else {
            dl.setName(filename);
        }
        dl.setLinkID(jd.plugins.hoster.PinterestCom.getLinkidForInternalDuplicateCheck(dl.getDownloadURL(), directlink));
        dl.setAvailable(true);
    }

    public static Map<String, Object> findPINMap(final Browser br, final boolean loggedIN, final String contentURL, final String source_url, final String boardid, final String username) throws Exception {
        final String pin_id = jd.plugins.hoster.PinterestCom.getPinID(contentURL);
        Map<String, Object> pinMap = null;
        List<Object> resource_data_cache = null;
        if (loggedIN && source_url != null && boardid != null && username != null) {
            String pin_ressource_url = "http://www.pinterest.com/resource/PinResource/get/?source_url=";
            String options = "/pin/%s/&data={\"options\":{\"field_set_key\":\"detailed\",\"link_selection\":true,\"fetch_visual_search_objects\":true,\"id\":\"%s\"},\"context\":{},\"module\":{\"name\":\"CloseupContent\",\"options\":{\"unauth_pin_closeup\":false}},\"render_type\":1}&module_path=App()>BoardPage(resource=BoardResource(username=amazvicki,+slug=))>Grid(resource=BoardFeedResource(board_id=%s,+board_url=%s,+page_size=null,+prepend=true,+access=,+board_layout=default))>GridItems(resource=BoardFeedResource(board_id=%s,+board_url=%s,+page_size=null,+prepend=true,+access=,+board_layout=default))>Pin(show_pinner=false,+show_pinned_from=true,+show_board=false,+squish_giraffe_pins=false,+component_type=0,+resource=PinResource(id=%s))";
            options = String.format(options, pin_id, pin_id, username, username, boardid, source_url, boardid, source_url, pin_id);
            options = options.replace("/", "%2F");
            // options = Encoding.urlEncode(options);
            pin_ressource_url += options;
            pin_ressource_url += "&_=" + System.currentTimeMillis();
            prepAPIBR(br);
            br.getPage(pin_ressource_url);
            followSpecialRedirect(br);
            if (jd.plugins.hoster.PinterestCom.isOffline(br, pin_id)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            resource_data_cache = (ArrayList) entries.get("resource_data_cache");
        } else {
            br.getPage(contentURL);
            followSpecialRedirect(br);
            if (jd.plugins.hoster.PinterestCom.isOffline(br, pin_id)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> entries = null;
            /*
             * Site actually contains similar json compared to API --> Grab that and get the final link via that as it is not always present
             * in the normal html code.
             */
            String json = br.getRegex("P\\.(?:start\\.start|main\\.start)\\((.*?)\\);\n").getMatch(0);
            if (json == null) {
                json = br.getRegex("P\\.startArgs = (.*?);\n").getMatch(0);
            }
            if (json != null) {
                /* Website json */
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                resource_data_cache = (ArrayList) entries.get("resourceDataCache");
            } else {
                /* API json e.g. needed: https://www.pinterest.com/pin/104497653832270636/ */
                prepAPIBR(br);
                final String pin_json_url = "https://www.pinterest.com/resource/PinResource/get/?source_url=%2Fpin%2F" + pin_id + "%2F&data=%7B%22options%22%3A%7B%22field_set_key%22%3A%22detailed%22%2C%22ptrf%22%3Anull%2C%22fetch_visual_search_objects%22%3Atrue%2C%22id%22%3A%22" + pin_id + "%22%7D%2C%22context%22%3A%7B%7D%7D&module_path=Pin(show_pinner%3Dtrue%2C+show_board%3Dtrue%2C+is_original_pin_in_related_pins_grid%3Dtrue)&_=" + System.currentTimeMillis();
                br.getPage(pin_json_url);
                json = br.toString();
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                if (entries.containsKey("resource_data_cache")) {
                    resource_data_cache = (ArrayList) entries.get("resource_data_cache");
                } else {
                    /* 2020-02-17 */
                    final Object pinO = entries.get("resource_response");
                    if (pinO != null) {
                        resource_data_cache = new ArrayList<Object>();
                        resource_data_cache.add(pinO);
                    }
                }
            }
        }
        final boolean grabThis;
        if (resource_data_cache == null) {
            return null;
        }
        if (resource_data_cache.size() == 1) {
            // logger.info("resource_data_cache contains only 1 item --> This should be the one, we want");
            grabThis = true;
        } else {
            // logger.info("resource_data_cache contains multiple item --> We'll have to find the correct one");
            grabThis = false;
        }
        for (final Object resource_object : resource_data_cache) {
            final Map<String, Object> entries = (Map<String, Object>) resource_object;
            final String this_pin_id = (String) JavaScriptEngineFactory.walkJson(entries, "data/id");
            final boolean pins_matched = this_pin_id.equals(pin_id);
            if (pins_matched || grabThis) {
                /* We've reached our goal */
                pinMap = entries;
                break;
            }
        }
        return pinMap;
    }

    /** 2020-11-16 */
    public static void followSpecialRedirect(final Browser br) throws IOException, PluginException {
        final String redirect = br.getRegex("window\\.location = \"([^<>\"]+)\"").getMatch(0);
        if (redirect != null) {
            if (redirect.contains("show_error=true")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(redirect);
        }
    }

    /** Returns highest resolution image URL inside given PIN Map. */
    public static String getDirectlinkFromPINMap(final Map<String, Object> pinMap) {
        if (pinMap == null) {
            return null;
        }
        String directlink = null;
        try {
            directlink = (String) JavaScriptEngineFactory.walkJson(pinMap, "data/images/orig/url");
        } catch (final Throwable e) {
        }
        return directlink;
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

    /**
     * @return: true: target section was found and only this will be crawler false: failed to find target section - in this case we should
     *          crawl everything we find </br>
     *          This can crawl A LOT of stuff! E.g. a board contains 1000 sections, each section contains 1000 PINs...
     */
    private void crawlSections(final CryptedLink param, final Browser ajax, final String boardID, final long totalInsideSectionsPinCount) throws Exception {
        final String username_and_boardname = new Regex(param.getCryptedUrl(), "https?://[^/]+/(.+)/").getMatch(0).replace("/", " - ");
        final Map<String, Object> postDataOptions = new HashMap<String, Object>();
        final String source_url = new URL(param.getCryptedUrl()).getPath();
        postDataOptions.put("isPrefetch", false);
        postDataOptions.put("board_id", boardID);
        postDataOptions.put("redux_normalize_feed", true);
        postDataOptions.put("no_fetch_context_on_resource", false);
        Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("options", postDataOptions);
        postData.put("context", new HashMap<String, Object>());
        int sectionPage = -1;
        ajax.getPage("/resource/BoardSectionsResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postData)));
        final int maxSectionsPerPage = 25;
        sectionPagination: do {
            sectionPage += 1;
            logger.info("Crawling sections page: " + (sectionPage + 1));
            final Map<String, Object> sectionsData = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            // Map<String, Object> json_root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            final List<Object> sections = (ArrayList) JavaScriptEngineFactory.walkJson(sectionsData, "resource_response/data");
            int sectionCounter = 1;
            for (final Object sectionO : sections) {
                final Map<String, Object> entries = (Map<String, Object>) sectionO;
                final String section_title = (String) entries.get("title");
                // final String sectionSlug = (String) entries.get("slug");
                final long section_total_pin_count = JavaScriptEngineFactory.toLong(entries.get("pin_count"), 0);
                final String sectionID = (String) entries.get("id");
                if (StringUtils.isEmpty(section_title) || sectionID == null || section_total_pin_count == 0) {
                    /* Skip invalid entries and empty sections */
                    continue;
                }
                logger.info("Crawling section " + sectionCounter + " of " + sections.size() + " --> ID = " + sectionID);
                fp.setName(username_and_boardname + " - " + section_title);
                crawlSection(ajax, source_url, boardID, sectionID);
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
        return;
    }

    private void crawlSection(final Browser ajax, final String source_url, final String boardID, final String sectionID) throws Exception {
        int decryptedPINCounter = 0;
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
        ajax.getPage("/resource/BoardSectionPinsResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(pinPaginationPostData)) + "&_=" + System.currentTimeMillis());
        pinsLoop: do {
            logger.info("Crawling section " + sectionID + " page: " + pageCounter);
            final Map<String, Object> sectionPaginationInfo = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            final Object bookmarksO = JavaScriptEngineFactory.walkJson(sectionPaginationInfo, "resource/options/bookmarks");
            final String bookmarks = (String) JavaScriptEngineFactory.walkJson(sectionPaginationInfo, "resource/options/bookmarks/{0}");
            final List<Object> pins = (ArrayList) JavaScriptEngineFactory.walkJson(sectionPaginationInfo, "resource_response/data");
            for (final Object pinO : pins) {
                final Map<String, Object> pinMap = (Map<String, Object>) pinO;
                if (!proccessMap(pinMap, boardID, source_url)) {
                    logger.info("Stopping PIN pagination because: Found unprocessable PIN map");
                    break pinsLoop;
                }
                decryptedPINCounter++;
            }
            pageCounter++;
            if (this.isAbort()) {
                logger.info("Crawler aborted by user");
                break;
            } else if (StringUtils.isEmpty(bookmarks) || bookmarks.equals("-end-") || bookmarksO == null) {
                /* Looks as if we've reached the end */
                logger.info("Stopping because: Reached end");
                break pinsLoop;
            } else if (pins.size() < maxPINsPerRequest) {
                /* Fail safe */
                logger.info("Stopping because: Current page contains less items than: " + maxPINsPerRequest);
                break pinsLoop;
            } else {
                pinPaginationPostDataOptions.put("bookmarks", bookmarksO);
                ajax.getPage("/resource/BoardSectionPinsResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(pinPaginationPostData)) + "&_=" + System.currentTimeMillis());
            }
        } while (true);
        logger.info("Number of PINs in current section: " + decryptedPINCounter);
    }

    private void crawlBoardPINs(final CryptedLink param) throws Exception {
        /*
         * In case the user wants to add a specific section, we have to get to the section overview --> Find sectionID --> Finally crawl
         * section PINs
         */
        String targetSectionSlug = null;
        final String username = new Regex(param.getCryptedUrl(), TYPE_BOARD).getMatch(0);
        final String boardSlug = new Regex(param.getCryptedUrl(), TYPE_BOARD).getMatch(1);
        // final String sourceURL;
        if (param.getCryptedUrl().matches(TYPE_BOARD_SECTION)) {
            /* Remove targetSection from URL as we cannot use it in this way. */
            targetSectionSlug = new Regex(param.getCryptedUrl(), TYPE_BOARD_SECTION).getMatch(0);
        }
        final String sourceURL = new URL(param.getCryptedUrl()).getPath();
        prepAPIBRCrawler(this.br);
        br.getPage("https://www." + this.getHost() + "/resource/BoardResource/get/?source_url=" + Encoding.urlEncode(sourceURL) + "style%2F&data=%7B%22options%22%3A%7B%22isPrefetch%22%3Afalse%2C%22username%22%3A%22" + Encoding.urlEncode(username) + "%22%2C%22slug%22%3A%22" + Encoding.urlEncode(boardSlug) + "%22%2C%22field_set_key%22%3A%22detailed%22%2C%22no_fetch_context_on_resource%22%3Afalse%7D%2C%22context%22%3A%7B%7D%7D&_=1614344870050");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(getOffline(param.getCryptedUrl()));
            return;
        }
        Map<String, Object> jsonRoot = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final Map<String, Object> boardPageResource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(jsonRoot, "resource_response/data");
        final String boardID = (String) boardPageResource.get("id");
        final long section_count = JavaScriptEngineFactory.toLong(boardPageResource.get("section_count"), 0);
        /* Find out how many PINs we have to crawl. */
        final long totalPinCount = JavaScriptEngineFactory.toLong(boardPageResource.get("pin_count"), 0);
        final long sectionlessPinCount = JavaScriptEngineFactory.toLong(boardPageResource.get("sectionless_pin_count"), 0);
        final long totalInsideSectionsPinCount = (totalPinCount > 0 && sectionlessPinCount < totalPinCount) ? totalPinCount - sectionlessPinCount : 0;
        logger.info("PINs total: " + totalPinCount + " | PINs inside sections: " + totalInsideSectionsPinCount + " | PINs outside sections: " + sectionlessPinCount);
        /*
         * Sections are like folders. Now find all the PINs that are not in any sections (it may happen that we already have everything at
         * this stage!) Only decrypt these leftover PINs if either the user did not want to have a specified section only or if he wanted to
         * have a specified section only but we failed to find that.
         */
        /*
         * 2018-12-11: Anonymous users officially cannot see sections even if they exist but we can crawl them the same way we do for
         * loggedIN users. Disable this if it is not possible anymore to crawl them.
         */
        final boolean enableSectionCrawlerForAnonymousUsers = true;
        final boolean allowSectionCrawling = (loggedIN || (!loggedIN && enableSectionCrawlerForAnonymousUsers));
        if (section_count > 0) {
            logger.info("Crawling section(s)");
            if (targetSectionSlug != null && allowSectionCrawling) {
                String targetSectionID = null;
                /* Small workaround to find sectionID (I've failed to find an API endpoint that returns this section only). */
                try {
                    br.getPage(param.getCryptedUrl());
                    final String json = this.getJsonSourceFromHTML(this.br);
                    final Map<String, Object> tmpMap = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                    targetSectionID = this.findSectionID(tmpMap, targetSectionSlug);
                } catch (final Throwable e) {
                    e.printStackTrace();
                    logger.warning("Exception occured during sectionID workaround");
                }
                if (targetSectionID == null) {
                    logger.info("Failed to crawl user desired section -> Crawling sectionless PINs only...");
                } else {
                    this.crawlSection(br.cloneBrowser(), sourceURL, boardID, targetSectionID);
                    logger.info("Total number of PINs crawled in desired single section: " + decryptedLinks.size());
                }
            } else {
                this.crawlSections(param, br.cloneBrowser(), boardID, totalInsideSectionsPinCount);
                logger.info("Total number of PINs crawled in sections: " + decryptedLinks.size());
            }
        }
        if (sectionlessPinCount <= 0) {
            /* No items at all available */
            logger.info("This board doesn't contain any loose PINs");
            decryptedLinks.add(getOffline(param.getCryptedUrl()));
            return;
        }
        /* Find- and set PackageName (Board Name) */
        String boardName = (String) boardPageResource.get("name");
        if (boardName == null) {
            /* Fallback */
            boardName = boardSlug.replace("/", "_");
        }
        fp.setName(boardName);
        if (loggedIN || force_api_usage) {
            final Map<String, Object> postDataOptions = new HashMap<String, Object>();
            final String source_url = new URL(param.getCryptedUrl()).getPath();
            postDataOptions.put("isPrefetch", false);
            postDataOptions.put("is_own_profile_pins", false);
            postDataOptions.put("username", username);
            postDataOptions.put("field_set_key", "grid_item");
            postDataOptions.put("pin_filter", null);
            postDataOptions.put("no_fetch_context_on_resource", false);
            Map<String, Object> postData = new HashMap<String, Object>();
            postData.put("options", postDataOptions);
            postData.put("context", new HashMap<String, Object>());
            int page = 0;
            int crawledSectionlessPINs = 0;
            logger.info("Crawling all sectionless PINs: " + sectionlessPinCount);
            do {
                page += 1;
                logger.info("Crawling sectionless PINs page: " + page + " | " + crawledSectionlessPINs + " / " + sectionlessPinCount + " PINs crawled");
                // /*
                // * Access URL - retry on error so that the crawl process of a large gallery does not top in the middle just because the
                // * server f*cks it up one time.
                // */
                // int failcounter_http_5034 = 0;
                // /* 2016-11-03: Added retries on HTTP/1.1 503 first byte timeout | HTTP/1.1 504 GATEWAY_TIMEOUT */
                // do {
                // if (this.isAbort()) {
                // return;
                // }
                // if (failcounter_http_5034 > 0) {
                // logger.info("503/504 error retry " + failcounter_http_5034);
                // this.sleep(5000, param);
                // }
                // br.getPage(getpage);
                // failcounter_http_5034++;
                // } while ((br.getHttpConnection().getResponseCode() == 504 || br.getHttpConnection().getResponseCode() == 503) &&
                // failcounter_http_5034 <= 4);
                // if (!br.getRequest().getHttpConnection().isOK()) {
                // throw new IOException("Invalid responseCode " + br.getRequest().getHttpConnection().getResponseCode());
                // }
                br.getPage("/resource/UserPinsResource/get/?source_url=" + Encoding.urlEncode(source_url) + "&data=" + URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postData)) + "&_=" + System.currentTimeMillis());
                Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                entries = (Map<String, Object>) entries.get("resource_response");
                final String bookmark = (String) entries.get("bookmark");
                final List<Object> pinList = (List<Object>) entries.get("data");
                for (final Object pint : pinList) {
                    final Map<String, Object> single_pinterest_data = (Map<String, Object>) pint;
                    proccessMap(single_pinterest_data, boardID, sourceURL);
                }
                crawledSectionlessPINs += pinList.size();
                if (StringUtils.isEmpty(bookmark) || bookmark.equalsIgnoreCase("-end-")) {
                    logger.info("Stopping because: Reached end");
                    break;
                } else if (crawledSectionlessPINs >= sectionlessPinCount) {
                    /* Fail-safe */
                    logger.info("Stopping because: Found all items");
                    break;
                } else {
                    /* Continue to next page */
                    postDataOptions.put("bookmarks", new String[] { bookmark });
                }
            } while (!this.isAbort());
        } else {
            decryptSite(param);
            final int max_entries_per_page_free = 25;
            if (totalPinCount > max_entries_per_page_free) {
                UIOManager.I().showMessageDialog("Please add your pinterest.com account at Settings->Account manager to find more than " + max_entries_per_page_free + " images");
            }
        }
    }

    private String getJsonSourceFromHTML(final Browser br) {
        String json_source_from_html;
        if (this.loggedIN) {
            json_source_from_html = br.getRegex("id=\\'initial\\-state\\'>window\\.__INITIAL_STATE__ =(.*?)</script>").getMatch(0);
        } else {
            json_source_from_html = br.getRegex("P\\.main\\.start\\((\\{.*?\\})\\);[\t\n\r]+").getMatch(0);
            if (json_source_from_html == null) {
                json_source_from_html = br.getRegex("P\\.startArgs\\s*=\\s*(\\{.*?\\});[\t\n\r]+").getMatch(0);
            }
            /* 2018-12-11: Does not contain what we want! */
            // if (json_source_from_html == null) {
            // json_source_from_html = br.getRegex("id=\\'jsInit1\\'>(\\{.*?\\})</script>").getMatch(0);
            // }
        }
        if (json_source_from_html == null) {
            /* 2018-12-11: For loggedin- and loggedoff */
            json_source_from_html = br.getRegex("id=.initial-state.[^>]*?>(\\{.*?\\})</script>").getMatch(0);
        }
        return json_source_from_html;
    }

    private boolean proccessMap(Map<String, Object> single_pinterest_data, final String board_id, final String source_url) throws PluginException {
        final String type = getStringFromJson(single_pinterest_data, "type");
        if (type == null || !(type.equals("pin") || type.equals("interest"))) {
            /* Skip invalid objects! */
            return false;
        }
        final Map<String, Object> single_pinterest_pinner = (Map<String, Object>) single_pinterest_data.get("pinner");
        final Map<String, Object> single_pinterest_images = (Map<String, Object>) single_pinterest_data.get("images");
        Map<String, Object> single_pinterest_images_original = null;
        if (single_pinterest_images != null) {
            single_pinterest_images_original = (Map<String, Object>) single_pinterest_images.get("orig");
        }
        final Object usernameo = single_pinterest_pinner != null ? single_pinterest_pinner.get("username") : null;
        // final Object pinner_nameo = single_pinterest_pinner != null ? single_pinterest_pinner.get("full_name") : null;
        Map<String, Object> tempmap = null;
        String directlink = null;
        if (single_pinterest_images_original != null) {
            /* Original image available --> Take that */
            directlink = (String) single_pinterest_images_original.get("url");
        } else {
            if (single_pinterest_images != null) {
                /* Original image NOT available --> Take the best we can find */
                final Iterator<Entry<String, Object>> it = single_pinterest_images.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Object> ipentry = it.next();
                    tempmap = (Map<String, Object>) ipentry.getValue();
                    /* First image = highest (but original is somewhere 'in the middle') */
                    break;
                }
                directlink = (String) tempmap.get("url");
            } else {
                /* 2017-11-22: Special case, for "preview PINs" */
                final String[] image_sizes = new String[] { "image_xlarge_url", "image_large_url", "image_medium_url" };
                for (final String image_size : image_sizes) {
                    directlink = (String) single_pinterest_data.get(image_size);
                    if (directlink != null) {
                        break;
                    }
                }
            }
        }
        final String pin_id = (String) single_pinterest_data.get("id");
        final String username = usernameo != null ? (String) usernameo : null;
        // final String pinner_name = pinner_nameo != null ? (String) pinner_nameo : null;
        if (StringUtils.isEmpty(pin_id) || directlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (dupeList.contains(pin_id)) {
            logger.info("Skipping duplicate: " + pin_id);
            return true;
        }
        dupeList.add(pin_id);
        final String content_url = "https://www." + this.getHost() + "/pin/" + pin_id + "/";
        final DownloadLink dl = createPINDownloadUrl(content_url);
        if (!StringUtils.isEmpty(board_id)) {
            dl.setProperty("boardid", board_id);
        }
        if (!StringUtils.isEmpty(source_url)) {
            dl.setProperty("source_url", source_url);
        }
        if (!StringUtils.isEmpty(username)) {
            dl.setProperty("username", username);
        }
        setInfoOnDownloadLink(dl, single_pinterest_data, directlink, loggedIN);
        fp.add(dl);
        decryptedLinks.add(dl);
        distribute(dl);
        final String externalURL = getAlternativeExternalURLInPINMap(single_pinterest_data);
        if (externalURL != null && this.enable_crawl_alternative_URL) {
            this.decryptedLinks.add(this.createDownloadlink(externalURL));
        }
        return true;
    }

    /* Wrapper which either returns object as String or (e.g. it is missing or different datatype), null. */
    private String getStringFromJson(final Map<String, Object> entries, final String key) {
        final String output;
        final Object jsono = entries.get(key);
        if (jsono != null && jsono instanceof String) {
            output = (String) jsono;
        } else {
            output = null;
        }
        return output;
    }

    private String getBoardID(String json_source) {
        if (json_source == null) {
            return null;
        }
        /* This board_id RegEx will usually only work when loggedOFF */
        json_source = json_source.replaceAll("\\\\", "");
        String board_id = PluginJSonUtils.getJsonValue(json_source, "board_id");
        if (board_id == null) {
            /* For LoggedIN and loggedOFF */
            board_id = this.br.getRegex("(\\d+)_board_thumbnail").getMatch(0);
            if (board_id == null) {
                board_id = new Regex(json_source, "\"board_id=\"(\\d+)\"").getMatch(0);
            }
        }
        return board_id;
    }

    /**
     * Recursive function to crawl all PINs --> Easiest way as they often change their json.
     *
     */
    @SuppressWarnings("unchecked")
    private void processPinsKamikaze(final Object jsono, final String board_id, final String source_url) throws PluginException {
        Map<String, Object> test;
        if (jsono instanceof Map) {
            test = (Map<String, Object>) jsono;
            if (!proccessMap(test, board_id, source_url)) {
                final Iterator<Entry<String, Object>> it = test.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Object> thisentry = it.next();
                    final Object mapObject = thisentry.getValue();
                    processPinsKamikaze(mapObject, board_id, source_url);
                }
            }
        } else if (jsono instanceof ArrayList) {
            final List<Object> ressourcelist = (List<Object>) jsono;
            for (final Object listo : ressourcelist) {
                processPinsKamikaze(listo, board_id, source_url);
            }
        }
    }

    /** Recursive function to find the ID of a sectionSlug. */
    private String findSectionID(final Object jsono, final String sectionSlug) throws PluginException {
        if (jsono instanceof Map) {
            final Map<String, Object> mapTmp = (Map<String, Object>) jsono;
            final Iterator<Entry<String, Object>> iterator = mapTmp.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("slug") && value instanceof String && value.toString().equalsIgnoreCase(sectionSlug)) {
                    return (String) mapTmp.get("id");
                } else if (value instanceof List || value instanceof Map) {
                    final String result = findSectionID(value, sectionSlug);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } else if (jsono instanceof ArrayList) {
            final List<Object> ressourcelist = (List<Object>) jsono;
            for (final Object arrayo : ressourcelist) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final String result = findSectionID(arrayo, sectionSlug);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    @Deprecated
    private void decryptSite(final CryptedLink param) {
        /*
         * Also possible using json of P.start.start( to get the first 25 entries: resourceDataCache --> Last[] --> data --> Here we go --->
         * But I consider this as an unsafe method.
         */
        final String[] linkinfo = br.getRegex("<div class=\"bulkEditPinWrapper\">(.*?)class=\"creditTitle\"").getColumn(0);
        if (linkinfo == null || linkinfo.length == 0) {
            logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
            decryptedLinks = null;
            return;
        }
        for (final String sinfo : linkinfo) {
            String description = new Regex(sinfo, "title=\"([^<>\"]*?)\"").getMatch(0);
            if (description == null) {
                description = new Regex(sinfo, "<p class=\"pinDescription\">([^<>]*?)<").getMatch(0);
            }
            final String directlink = new Regex(sinfo, "\"(https?://[a-z0-9\\.\\-]+/originals/[^<>\"]*?)\"").getMatch(0);
            final String pin_id = new Regex(sinfo, "/pin/([A-Za-z0-9\\-_]+)/").getMatch(0);
            if (pin_id == null) {
                logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
                decryptedLinks = null;
                return;
            } else if (dupeList.contains(pin_id)) {
                logger.info("Skipping duplicate: " + pin_id);
                continue;
            }
            dupeList.add(pin_id);
            String filename = pin_id;
            final String content_url = "http://www.pinterest.com/pin/" + pin_id + "/";
            final DownloadLink dl = createDownloadlink(content_url);
            dl.setContentUrl(content_url);
            dl.setLinkID(jd.plugins.hoster.PinterestCom.getLinkidForInternalDuplicateCheck(content_url, directlink));
            dl._setFilePackage(fp);
            if (directlink != null) {
                dl.setProperty("free_directlink", directlink);
            }
            if (description != null) {
                dl.setComment(description);
                dl.setProperty("description", description);
                if (enable_description_inside_filenames) {
                    filename += "_" + description;
                }
            }
            filename = encodeUnicode(filename);
            dl.setProperty("decryptedfilename", filename);
            dl.setName(filename + ".jpg");
            dl.setAvailable(true);
            dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            decryptedLinks.add(dl);
            distribute(dl);
        }
    }

    private DownloadLink createPINDownloadUrl(String url) {
        url = url.replaceAll("https?://", "decryptedpinterest://");
        return this.createDownloadlink(url);
    }

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings({ "deprecation" })
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.PinterestCom) hostPlugin).login(aa, force);
        } catch (final PluginException e) {
            return false;
        }
        return true;
    }

    /** Wrapper */
    public static void prepAPIBR(final Browser br) throws PluginException {
        jd.plugins.hoster.PinterestCom.prepAPIBR(br);
    }

    private void prepAPIBRCrawler(final Browser br) throws PluginException {
        jd.plugins.hoster.PinterestCom.prepAPIBR(br);
        br.setAllowedResponseCodes(new int[] { 503, 504 });
    }
}
