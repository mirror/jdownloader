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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flickr.com" }, urls = { "https?://(?:secure\\.|www\\.)?flickr\\.com/(?:photos|groups)/.+" })
public class FlickrCom extends PluginForDecrypt {
    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String                  TYPE_FAVORITES           = "https?://[^/]+/photos/([^<>\"/]+)/favorites(/.+)?";
    private static final String                  TYPE_GROUPS              = "https?://[^/]+/groups/([^<>\"/]+)([^<>\"]+)?";
    private static final String                  TYPE_SET_SINGLE          = "https?://[^/]+/photos/([^<>\"/]+)/(?:sets|albums)/(\\d+)/?";
    private static final String                  TYPE_GALLERY             = "https?://[^/]+/photos/([^<>\"/]+)/galleries/(\\d+)/?";
    private static final String                  TYPE_SETS_OF_USER_ALL    = "^https?://[^/]+/photos/([^/]+)/(?:albums|sets)/?$";
    private static final String                  TYPE_SINGLE_PHOTO        = "https?://[^/]+/photos/(?!tags/)[^<>\"/]+/\\d+.+";
    private static final String                  TYPE_PHOTO               = "https?://[^/]+/photos/.*?";
    private static final String                  TYPE_USER                = "^https?://[^/]+/photos/([^/]+)/?$";
    private static final String                  INVALIDLINKS             = "https?://[^/]+/(photos/(me|upload|tags.*?)|groups/[^<>\"/]+/rules|groups/[^<>\"/]+/discuss.*?)";
    public static final String                   API_BASE                 = "https://api.flickr.com/";
    private static final String                  api_format               = "json";
    private static final int                     api_max_entries_per_page = 500;
    private ArrayList<DownloadLink>              decryptedLinks           = new ArrayList<DownloadLink>();
    private String                               csrf                     = null;
    private boolean                              loggedin                 = false;
    private int                                  statuscode               = 0;
    private static LinkedHashMap<String, String> INTERNAL_USERNAME_CACHE  = new LinkedHashMap<String, String>() {
                                                                              protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                                                                                  return size() > 200;
                                                                              };
                                                                          };
    private static LinkedHashMap<String, String> INTERNAL_GROUPNAME_CACHE = new LinkedHashMap<String, String>() {
                                                                              protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                                                                                  return size() > 200;
                                                                              };
                                                                          };
    private static HashMap<String, Object>       api                      = new HashMap<String, Object>();

    /**
     * Using API: https://www.flickr.com/services/api/ - without our own apikey. Site is still used for /* TODO API: Get correct csrf values
     * so we can make requests as a logged-in user.
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "localization", "en-us%3Bus%3Bde");
        br.setCookie(this.getHost(), "fldetectedlang", "en-us");
        br.setLoadLimit(br.getLoadLimit() * 2);
        correctAddedURL(param);
        /* Check if link is for hosterplugin */
        if (param.getCryptedUrl().matches(TYPE_SINGLE_PHOTO)) {
            /* Pass to hostplugin */
            decryptedLinks.add(createDownloadlink(param.getCryptedUrl()));
            return decryptedLinks;
        } else if (param.getCryptedUrl().matches(INVALIDLINKS) || param.getCryptedUrl().equals("https://www.flickr.com/photos/groups/") || param.getCryptedUrl().contains("/map")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Login is not always needed but we force it to get all pictures */
        this.loggedin = getUserLogin();
        if (param.getCryptedUrl().matches(TYPE_SETS_OF_USER_ALL)) {
            apiCrawlSetsOfUser(param);
        } else if (loggedin) {
            site_handleSite(param);
        } else {
            api_handleAPI(param);
        }
        return decryptedLinks;
    }

    /** Corrects links added by the user. */
    private void correctAddedURL(final CryptedLink param) {
        String remove_string = null;
        String newurl = Encoding.htmlDecode(param.getCryptedUrl()).replace("http://", "https://");
        newurl = newurl.replace("secure.flickr.com/", this.getHost() + "/");
        final String[] removeStuff = { "(/player/.+)", "(/with/.+)" };
        for (final String removethis : removeStuff) {
            remove_string = new Regex(newurl, removethis).getMatch(0);
            if (remove_string != null) {
                newurl = newurl.replace(remove_string, "");
            }
        }
        if (newurl.matches(TYPE_PHOTO)) {
            remove_string = new Regex(newurl, "(/sizes/.+)").getMatch(0);
            if (remove_string != null) {
                newurl = newurl.replace(remove_string, "");
            }
        }
        param.setCryptedUrl(newurl);
    }

    /**
     * Handles crawl via API.
     *
     * @throws Exception
     */
    private void api_handleAPI(final CryptedLink param) throws Exception {
        /* TODO: Fix csrf handling to make requests as logged-in user possible. */
        br.clearCookies(this.getHost());
        String fpName = null;
        final String apikey = getPublicAPIKey(this.br);
        if (StringUtils.isEmpty(apikey)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        csrf = PluginJSonUtils.getJsonValue(br, "csrf");
        if (csrf == null) {
            csrf = "";
        }
        br.getHeaders().put("Origin", "https://www." + this.getHost());
        br.getHeaders().put("Referer", "https://www." + this.getHost());
        br.getHeaders().put("Accept", "*/*");
        String usernameFromURL = null;
        String usernameSlug = null; // username lowercase (inside URLs) e.g. "exampleusername"
        String usernameFull = null; // Username full e.g. "Example Username"
        String usernameInternal = null; // Internal username e.g. "123456@N04"
        boolean givenUsernameDataIsValidForAllMediaItems;
        String setID = null;
        String galleryID = null;
        String packageDescription = null;
        /* Set this if we pre-access items to e.g. get specific fields in beforehand. */
        boolean alreadyAccessedFirstPage;
        final UrlQuery params = new UrlQuery();
        params.add("api_key", apikey);
        /* needs_interstitial = show 18+ content */
        String extras = "date_taken%2Cdate_upload%2Cdescription%2Cowner_name%2Cpath_alias%2Crealname%2Cneeds_interstitial";
        final String[] allPhotoQualities = jd.plugins.hoster.FlickrCom.getPhotoQualityStringsDescending();
        for (final String qualityStr : allPhotoQualities) {
            extras += "%2Curl_" + qualityStr;
        }
        params.add("extras", extras);
        params.add("format", "json");
        params.add("per_page", Integer.toString(api_max_entries_per_page));
        params.add("hermes", "1");
        params.add("hermesClient", "1");
        params.add("nojsoncallback", "1");
        if (this.csrf != null) {
            params.add("csrf", this.csrf);
        }
        String nameOfMainMap;
        if (param.getCryptedUrl().matches(TYPE_SET_SINGLE)) {
            usernameFromURL = new Regex(param.getCryptedUrl(), TYPE_SET_SINGLE).getMatch(0);
            setID = new Regex(param.getCryptedUrl(), TYPE_SET_SINGLE).getMatch(1);
            /* This request is only needed to get the title and owner of the photoset */
            final UrlQuery paramsSetInfo = params;
            paramsSetInfo.add("method", "flickr.photosets.getInfo");
            paramsSetInfo.add("photoset_id", Encoding.urlEncode(setID));
            api_getPage(API_BASE + "services/rest?" + paramsSetInfo.toString());
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> photoset = (Map<String, Object>) entries.get("photoset");
            usernameInternal = (String) photoset.get("owner");
            usernameFull = (String) photoset.get("ownername");
            fpName = (String) JavaScriptEngineFactory.walkJson(photoset, "title/_content");
            packageDescription = (String) JavaScriptEngineFactory.walkJson(photoset, "description/_content");
            if (StringUtils.isEmpty(fpName)) {
                fpName = "flickr.com set " + setID + " of user " + usernameFromURL;
            } else {
                fpName = Encoding.unicodeDecode(fpName);
            }
            params.add("method", "flickr.photosets.getPhotos");
            params.add("photoset_id", setID);
            nameOfMainMap = "photoset";
            alreadyAccessedFirstPage = false;
            givenUsernameDataIsValidForAllMediaItems = true;
        } else if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
            usernameFromURL = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
            galleryID = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(1);
            params.add("method", "flickr.galleries.getPhotos");
            params.add("gallery_id", galleryID);
            final UrlQuery specialQueryForFirstRequest = params;
            specialQueryForFirstRequest.add("get_gallery_info", "1");
            api_getPage(API_BASE + "services/rest?" + specialQueryForFirstRequest.toString());
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> galleryinfo = (Map<String, Object>) entries.get("gallery");
            usernameSlug = (String) galleryinfo.get("username");
            final String galleryTitle = (String) JavaScriptEngineFactory.walkJson(entries, "title/_content");
            if (!StringUtils.isEmpty(galleryTitle)) {
                fpName = "flickr.com gallery " + fpName + " of user " + usernameFromURL;
            } else {
                /* Fallback */
                fpName = "flickr.com gallery " + galleryID + " of user " + usernameFromURL;
            }
            nameOfMainMap = "photos";
            alreadyAccessedFirstPage = true;
            givenUsernameDataIsValidForAllMediaItems = false;
        } else if (param.getCryptedUrl().matches(TYPE_FAVORITES)) {
            usernameFromURL = new Regex(param.getCryptedUrl(), TYPE_FAVORITES).getMatch(0);
            usernameInternal = this.lookupUser(usernameFromURL);
            params.add("method", "flickr.favorites.getList");
            params.add("user_id", Encoding.urlEncode(usernameInternal));
            fpName = "flickr.com favourites of user " + usernameFromURL;
            nameOfMainMap = "photos";
            alreadyAccessedFirstPage = false;
            givenUsernameDataIsValidForAllMediaItems = false;
        } else if (param.getCryptedUrl().matches(TYPE_GROUPS)) {
            usernameFromURL = new Regex(param.getCryptedUrl(), TYPE_GROUPS).getMatch(0);
            usernameInternal = this.lookupGroup(usernameFromURL);
            params.add("group_id", Encoding.urlEncode(usernameInternal));
            params.add("method", "flickr.groups.pools.getPhotos");
            /* Only request group info on first request. */
            final UrlQuery specialQueryForFirstRequest = params;
            specialQueryForFirstRequest.add("get_group_info", "1");
            api_getPage(API_BASE + "services/rest?" + specialQueryForFirstRequest.toString());
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> group = (Map<String, Object>) entries.get("group");
            usernameSlug = (String) group.get("pathalias");
            usernameFull = (String) group.get("name");
            fpName = "flickr.com images of group " + usernameFromURL;
            nameOfMainMap = "photos";
            alreadyAccessedFirstPage = true;
            givenUsernameDataIsValidForAllMediaItems = false;
        } else if (param.getCryptedUrl().matches(TYPE_USER)) {
            /* Crawl all items of a user */
            usernameFromURL = new Regex(param.getCryptedUrl(), TYPE_USER).getMatch(0);
            usernameInternal = this.lookupUser(usernameFromURL);
            params.add("user_id", usernameInternal);
            params.add("method", "flickr.people.getPublicPhotos"); // Alternative: flickr.people.getPhotos
            fpName = "flickr.com images of user " + usernameFromURL;
            nameOfMainMap = "photos";
            alreadyAccessedFirstPage = false;
            givenUsernameDataIsValidForAllMediaItems = true;
        } else {
            /* Unsupported URL --> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (looksLikeInternalUsername(usernameFromURL)) {
            usernameInternal = usernameFromURL;
        } else if (usernameSlug == null) {
            usernameSlug = usernameFromURL;
        }
        final FilePackage fp = FilePackage.getInstance();
        fpName = encodeUnicode(fpName);
        fp.setName(fpName);
        if (!StringUtils.isEmpty(packageDescription)) {
            fp.setComment(packageDescription);
        }
        int totalimgs = -1;
        int totalpages = -1;
        int imagePosition = 0;
        DecimalFormat df = null;
        int page = 1;
        final String userPreferredPhotoQualityStr = jd.plugins.hoster.FlickrCom.photoQualityEnumNameToString(jd.plugins.hoster.FlickrCom.getPreferredPhotoQuality().name());
        do {
            if (page > 1 || !alreadyAccessedFirstPage) {
                params.addAndReplace("page", Integer.toString(page));
                api_getPage(API_BASE + "services/rest?" + params.toString());
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> photoInfo = (Map<String, Object>) entries.get(nameOfMainMap);
            totalpages = ((Number) photoInfo.get("pages")).intValue();
            totalimgs = ((Number) photoInfo.get("total")).intValue();
            if (totalimgs == 0) {
                logger.info("ZERO items available");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (df == null) {
                df = new DecimalFormat(String.valueOf(totalimgs).replaceAll("\\d", "0"));
            }
            logger.info("Crawling page " + page + " / " + totalpages + " | Progress: " + decryptedLinks.size() + " of " + totalimgs);
            final List<Map<String, Object>> photoList = (List<Map<String, Object>>) photoInfo.get("photo");
            final boolean seemsToContainMatureContent = page < totalpages && photoList.size() < api_max_entries_per_page;
            if (seemsToContainMatureContent) {
                logger.info("There is probably hidden mature content present? Found only " + photoList.size() + " of max " + api_max_entries_per_page + " items on page " + page + " although we're not yet on the last page");
            }
            for (final Map<String, Object> photo : photoList) {
                imagePosition += 1;
                final String thisUsernameSlug = (String) photo.get("pathalias");
                final String thisUsernameInternal = (String) photo.get("owner");
                final String thisUsernameFull = (String) photo.get("ownername");
                final String realName = (String) photo.get("realname");
                final String usernameForContentURL;
                /* E.g. in a set, all pictures got the same owner so the "owner" key is not available here. */
                if (givenUsernameDataIsValidForAllMediaItems) {
                    usernameForContentURL = usernameFromURL;
                } else {
                    usernameForContentURL = thisUsernameInternal;
                }
                if (StringUtils.isEmpty(usernameForContentURL)) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String photoID = photo.get("id").toString();
                final String title = (String) photo.get("title");
                final String dateUploaded = (String) photo.get("dateupload");
                final String description = (String) JavaScriptEngineFactory.walkJson(photo, "description/_content");
                final String contenturl;
                if (setID != null) {
                    contenturl = "https://www." + this.getHost() + "/photos/" + usernameForContentURL + "/" + photoID + "/in/album-" + setID;
                } else {
                    contenturl = "https://www." + this.getHost() + "/photos/" + usernameForContentURL + "/" + photoID;
                }
                final DownloadLink dl = createDownloadlink(contenturl);
                if (!StringUtils.isEmpty(description)) {
                    dl.setComment(Encoding.htmlDecode(description));
                }
                final String media = (String) photo.get("media");
                final String extension;
                String filenameURL = null;
                if ("video".equalsIgnoreCase(media)) {
                    extension = ".mp4";
                } else {
                    extension = ".jpg";
                    /* Try to find photo directurl right away */
                    String maxQualityName = null;
                    String maxQualityDownloadurl = null;
                    String userPreferredQualityDownloadurl = null;
                    for (final String qualityStr : allPhotoQualities) {
                        final String url = (String) photo.get("url_" + qualityStr);
                        if (url == null) {
                            continue;
                        }
                        /* First found = best */
                        if (maxQualityDownloadurl == null) {
                            maxQualityDownloadurl = url;
                            maxQualityName = qualityStr;
                        }
                        if (qualityStr.equalsIgnoreCase(userPreferredPhotoQualityStr)) {
                            /* Quit loop as this is the quality our user wants to have. */
                            userPreferredQualityDownloadurl = url;
                            break;
                        }
                    }
                    /* Check if we found anything and set to re-use later. */
                    if (!StringUtils.isEmpty(maxQualityDownloadurl) || !StringUtils.isEmpty(userPreferredQualityDownloadurl)) {
                        final String url;
                        final String chosenQualityStr;
                        if (userPreferredQualityDownloadurl != null) {
                            url = userPreferredQualityDownloadurl;
                            chosenQualityStr = userPreferredPhotoQualityStr;
                        } else {
                            url = maxQualityDownloadurl;
                            chosenQualityStr = maxQualityName;
                        }
                        dl.setProperty(String.format(jd.plugins.hoster.FlickrCom.PROPERTY_DIRECTURL, chosenQualityStr), url);
                        dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_QUALITY, chosenQualityStr);
                        filenameURL = jd.plugins.hoster.FlickrCom.getFilenameFromDirecturl(url);
                    }
                }
                dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_CONTENT_ID, photoID);
                dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_MEDIA_TYPE, media);
                if (setID != null) {
                    dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_SET_ID, setID);
                }
                {
                    /* Set different username/name properties */
                    if (givenUsernameDataIsValidForAllMediaItems) {
                        if (!StringUtils.isEmpty(usernameSlug)) {
                            dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME, usernameSlug);
                        }
                        if (!StringUtils.isEmpty(usernameInternal)) {
                            dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME_INTERNAL, usernameInternal);
                        }
                        if (!StringUtils.isEmpty(usernameFull)) {
                            dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME_FULL, usernameFull);
                        }
                    }
                    /* Overwrite previously set properties if our "photo" object has them too as we can trust those ones 100%. */
                    if (!StringUtils.isEmpty(thisUsernameSlug)) {
                        dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME, thisUsernameSlug);
                    }
                    if (!StringUtils.isEmpty(thisUsernameFull)) {
                        dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME_FULL, thisUsernameFull);
                    }
                    if (!StringUtils.isEmpty(thisUsernameInternal)) {
                        dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME_INTERNAL, thisUsernameInternal);
                    }
                    if (!StringUtils.isEmpty(realName)) {
                        dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_REAL_NAME, realName);
                    }
                }
                if (dateUploaded != null && dateUploaded.matches("\\d+")) {
                    dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_DATE, Long.parseLong(dateUploaded) * 1000);
                }
                final String dateTaken = (String) photo.get("datetaken");
                if (!StringUtils.isEmpty(dateTaken)) {
                    dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_DATE_TAKEN, dateTaken);
                }
                if (!StringUtils.isEmpty(title)) {
                    dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_TITLE, title);
                }
                dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_EXT, extension);
                dl.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_ORDER_ID, df.format(imagePosition));
                if (filenameURL != null && jd.plugins.hoster.FlickrCom.userPrefersServerFilenames()) {
                    dl.setFinalFileName(filenameURL);
                } else {
                    final String formattedFilename = getFormattedFilename(dl);
                    dl.setFinalFileName(formattedFilename);
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                distribute(dl);
                decryptedLinks.add(dl);
            }
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return;
            } else if (page >= totalpages) {
                logger.info("Stopping because: Reached last page number " + page);
                break;
            } else {
                page += 1;
                /* continue */
            }
        } while (true);
        if (decryptedLinks.size() != totalimgs) {
            logger.warning("Number of results != expected number of results: Found: " + decryptedLinks.size() + " Expected: " + totalimgs);
        }
    }

    private String getFormattedFilename(final DownloadLink dl) throws ParseException {
        return jd.plugins.hoster.FlickrCom.getFormattedFilename(dl);
    }

    private void apiCrawlSetsOfUser(final CryptedLink param) throws Exception {
        final String username = new Regex(param.getCryptedUrl(), TYPE_SETS_OF_USER_ALL).getMatch(0);
        if (username == null) {
            /* Most likely developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String nsid = lookupUser(username);
        final UrlQuery query = new UrlQuery();
        query.add("format", api_format);
        query.add("csrf", this.csrf);
        query.add("api_key", getPublicAPIKey(br));
        query.add("per_page", Integer.toString(api_max_entries_per_page));
        query.add("user_id", Encoding.urlEncode(nsid));
        query.add("method", "flickr.photosets.getList");
        query.add("hermes", "1");
        query.add("hermesClient", "1");
        query.add("nojsoncallback", "1");
        int totalitems = -1;
        int maxPage = -1;
        int page = 1;
        do {
            query.addAndReplace("page", Integer.toString(page));
            this.api_getPage(API_BASE + "services/rest?" + query.toString());
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> setInfo = (Map<String, Object>) entries.get("photosets");
            totalitems = ((Number) setInfo.get("total")).intValue();
            maxPage = ((Number) setInfo.get("pages")).intValue();
            logger.info("Crawling page " + page + " / " + maxPage + " | Sets crawler: " + decryptedLinks.size() + " / " + totalitems);
            final List<Map<String, Object>> sets = (List<Map<String, Object>>) setInfo.get("photoset");
            for (final Map<String, Object> set : sets) {
                /* Those ones go back into our crawler. */
                final String contenturl = "https://www." + this.getHost() + "/photos/" + username + "/sets/" + set.get("id") + "/";
                final DownloadLink fina = createDownloadlink(contenturl);
                decryptedLinks.add(fina);
            }
            if (sets.size() < api_max_entries_per_page) {
                logger.info("Stopping because: Current page contains less than max. item number");
                break;
            } else if (page >= maxPage) {
                logger.info("Stopping because: Reached last page: " + maxPage);
                break;
            } else {
                page += 1;
                /* Continue */
            }
        } while (!this.isAbort());
    }

    public static boolean looksLikeInternalUsername(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("\\d+@N\\d+")) {
            return true;
        } else {
            return false;
        }
    }

    private String lookupUser(final String username) throws Exception {
        if (StringUtils.isEmpty(username)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (looksLikeInternalUsername(username)) {
            return username;
        } else {
            synchronized (INTERNAL_USERNAME_CACHE) {
                if (!INTERNAL_USERNAME_CACHE.containsKey(username)) {
                    final String userURL = "https://www." + this.getHost() + "/photos/" + username + "/";
                    final UrlQuery query = new UrlQuery();
                    query.add("format", api_format);
                    query.add("csrf", this.csrf);
                    query.add("api_key", getPublicAPIKey(br));
                    query.add("method", "flickr.urls.lookupUser");
                    query.add("url", Encoding.urlEncode(userURL));
                    query.add("nojsoncallback", "1");
                    api_getPage(API_BASE + "services/rest?" + query.toString());
                    final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    final String usernameInternal = (String) JavaScriptEngineFactory.walkJson(entries, "user/id");
                    if (StringUtils.isEmpty(usernameInternal)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    INTERNAL_USERNAME_CACHE.put(username, usernameInternal);
                }
                return INTERNAL_USERNAME_CACHE.get(username);
            }
        }
    }

    private String lookupGroup(final String groupname) throws Exception {
        if (StringUtils.isEmpty(groupname)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (looksLikeInternalUsername(groupname)) {
            return groupname;
        } else {
            synchronized (INTERNAL_GROUPNAME_CACHE) {
                if (!INTERNAL_GROUPNAME_CACHE.containsKey(groupname)) {
                    final String groupURL = "https://www." + this.getHost() + "/groups/" + groupname + "/";
                    final UrlQuery query = new UrlQuery();
                    query.add("format", api_format);
                    query.add("csrf", this.csrf);
                    query.add("api_key", getPublicAPIKey(br));
                    query.add("method", "flickr.urls.lookupGroup");
                    query.add("url", Encoding.urlEncode(groupURL));
                    query.add("nojsoncallback", "1");
                    api_getPage(API_BASE + "services/rest?" + query.toString());
                    final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    final String usernameInternal = (String) JavaScriptEngineFactory.walkJson(entries, "group/id");
                    if (StringUtils.isEmpty(usernameInternal)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    INTERNAL_GROUPNAME_CACHE.put(groupname, usernameInternal);
                }
                return INTERNAL_GROUPNAME_CACHE.get(groupname);
            }
        }
    }

    private static Object LOCK = new Object();

    private void api_getPage(final String url) throws IOException, DecrypterException, InterruptedException, PluginException {
        synchronized (LOCK) {
            final URLConnectionAdapter con = br.openGetConnection(url);
            try {
                switch (con.getResponseCode()) {
                case 500:
                case 504:
                    con.setAllowedResponseCodes(new int[] { con.getResponseCode() });
                    br.followConnection();
                    sleep(1000, getCurrentLink().getCryptedLink());
                    br.getPage(url);
                    break;
                default:
                    br.followConnection();
                    break;
                }
            } finally {
                con.disconnect();
            }
        }
        updatestatuscode();
        this.handleAPIErrors(this.br);
    }

    /** Check for errorcode and set it if existant */
    private void updatestatuscode() {
        String errorcode = PluginJSonUtils.getJsonValue(br, "error");
        if (errorcode == null) {
            errorcode = PluginJSonUtils.getJsonValue(br, "code");
        }
        if (errorcode != null) {
            statuscode = Integer.parseInt(errorcode);
        } else {
            statuscode = 0;
        }
    }

    /* Handles most of the possible API errorcodes - most of them should never happen. */
    private void handleAPIErrors(final Browser br) throws DecrypterException, PluginException {
        String statusMessage = null;
        switch (statuscode) {
        case 0:
            /* Everything ok */
            break;
        case 1:
            statusMessage = "Group/user/photo not found - possibly invalid nsid";
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 2:
            statusMessage = "No user specified or permission denied";
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 98:
            statusMessage = "Login failed";
            throw new DecrypterException("API_LOGIN_FAILED");
        case 100:
            statusMessage = "Invalid api key";
            throw new DecrypterException("API_INVALID_APIKEY");
        case 105:
            statusMessage = "Service currently unavailable";
            throw new DecrypterException("API_SERVICE_CURRENTLY_UNAVAILABLE");
        case 106:
            statusMessage = "Write operation failed";
            throw new DecrypterException("API_WRITE_OPERATION FAILED");
        case 111:
            statusMessage = "Format not found";
            throw new DecrypterException("API_FORMAT_NOT_FOUND");
        case 112:
            statusMessage = "Method not found";
            throw new DecrypterException("API_METHOD_NOT_FOUND");
        case 114:
            statusMessage = "Invalid SOAP envelope";
            throw new DecrypterException("API_INVALID_SOAP_ENVELOPE");
        case 115:
            statusMessage = "Invalid XML-RPC Method Call";
            throw new DecrypterException("API_INVALID_XML_RPC_METHOD_CALL");
        case 116:
            statusMessage = "Bad URL found";
            throw new DecrypterException("API_URL_NOT_FOUND");
        default:
        }
    }

    private String getPublicAPIKey(final Browser br) throws IOException {
        synchronized (api) {
            if (!api.containsKey("apikey") || !api.containsKey("timestamp") || System.currentTimeMillis() - ((Number) api.get("timestamp")).longValue() > 1 * 60 * 60 * 1000) {
                logger.info("apikey refresh required");
                final String apikey = findPublicApikey(br);
                api.put("apikey", apikey);
                api.put("timestamp", System.currentTimeMillis());
            }
            return api.get("apikey").toString();
        }
    }

    public static String findPublicApikey(final Browser br) throws IOException {
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        brc.getPage("https://www.flickr.com/photos/groups/");
        String apikey = brc.getRegex("root\\.YUI_config\\.flickr\\.api\\.site_key\\s*?=\\s*?\"(.*?)\"").getMatch(0);
        if (apikey == null) {
            apikey = PluginJSonUtils.getJsonValue(brc, "api_key");
        }
        if (StringUtils.isEmpty(apikey)) {
            apikey = "89e6ebe516ca5928161b4884693d5995";
        }
        return apikey;
    }

    /**
     * Handles decryption via website.
     *
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked" })
    @Deprecated
    /** Deprecated! Uses website without ajax requests! */
    private void site_handleSite(final CryptedLink param) throws Exception {
        // if not logged in this is 25... need to confirm for logged in -raztoki20160717
        int maxEntriesPerPage = 25;
        String fpName;
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if ((br.containsHTML("class=\"ThinCase Interst\"") || br.getURL().contains("/login.yahoo.com/")) && !this.loggedin) {
            throw new AccountRequiredException();
        } else if (param.getCryptedUrl().matches(TYPE_FAVORITES) && br.containsHTML("id=\"no\\-faves\"")) {
            /* Favourite link but user has no favourites */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Some stuff which is different from link to link
        String picCount = br.getRegex("\"total\":(\")?(\\d+)").getMatch(1);
        fpName = br.getRegex("<title>(.*?) \\| Flickr</title>").getMatch(0);
        String username = null;
        if (param.getCryptedUrl().matches(TYPE_FAVORITES)) {
            username = new Regex(param.getCryptedUrl(), TYPE_FAVORITES).getMatch(0);
            fpName = "favourites of user " + username;
        } else {
            /* Unsupported URL type/developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        // lets allow merge, so if the user imports multiple pages manually they will go into the same favourites package.
        fp.setProperty("ALLOW_MERGE", true);
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final int totalEntries;
        if (picCount != null) {
            picCount = picCount.replaceAll("(,|\\.)", "");
            totalEntries = Integer.parseInt(picCount);
        } else {
            totalEntries = -1;
        }
        /**
         * Handling for albums/sets: Only decrypt all pages if user did NOT add a direct page link
         */
        int currentPage = -1;
        if (param.getCryptedUrl().contains("/page")) {
            currentPage = Integer.parseInt(new Regex(param.getCryptedUrl(), "page(\\d+)").getMatch(0));
        }
        String getPage = param.getCryptedUrl().replaceFirst("/page\\d+", "") + "/page%s";
        if (param.getCryptedUrl().matches(TYPE_GROUPS) && param.getCryptedUrl().endsWith("/")) {
            // Try other way of loading more pictures for groups links
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage = param.getCryptedUrl() + "page%s/?fragment=1";
        }
        int i = (currentPage != -1 ? currentPage : 1);
        /* We don't know the total count before so let's always use 4 digits. */
        final DecimalFormat df = new DecimalFormat("0000");
        int imagePosition = 0;
        do {
            if (i != 1 && currentPage == -1) {
                br.getPage(String.format(getPage, i));
                // when we are out of pages, it will redirect back to non page count
                if (br.getURL().equals(getPage.replace("/page%s", "/"))) {
                    logger.info("No more pages!");
                    break;
                }
            }
            final String json = this.br.getRegex("modelExport\\s*:\\s*(\\{.+\\}),").getMatch(0);
            if (json == null) {
                /* This should never happen but if we found links before, lets return them. */
                if (decryptedLinks.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    break;
                }
            }
            Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            List<Object> resourcelist = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "favorite-models/{0}/photoPageList/_data");
            if (resourcelist == null) {
                resourcelist = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "main/favorite-models/{0}/photoPageList/_data");
            }
            Map<String, Object> lastOwner = null;
            final ArrayList<Map<String, Object>> knownOwner = new ArrayList<Map<String, Object>>();
            for (final Object pico : resourcelist) {
                imagePosition += 1;
                Map<String, Object> entry = (Map<String, Object>) pico;
                if (entry == null) {
                    continue;
                }
                String title = (String) entry.get("title");
                String media = (String) entry.get("media");
                final String pic_id = (String) entry.get("id");
                // not all images have a title.
                if (title == null) {
                    title = "";
                }
                final Map<String, Object> owner;
                if (entry.get("owner") instanceof Map) {
                    owner = (Map<String, Object>) entry.get("owner");
                } else if (entry.get("owner").toString().startsWith("~")) {
                    final int index = Integer.parseInt(entry.get("owner").toString().substring(1));
                    if (knownOwner.size() > index) {
                        owner = knownOwner.get(index);
                    } else {
                        owner = lastOwner;
                        knownOwner.add(owner);
                    }
                } else {
                    owner = null;
                }
                lastOwner = owner;
                String pathAlias = (String) owner.get("pathAlias"); // standard
                if (pathAlias == null) {
                    // flickr 'Model' will be under the following (not username)
                    pathAlias = (String) owner.get("id");
                    if (pathAlias == null) {
                        // stupid i know but they reference other entries values. (standard users)
                        final String pa = (String) owner.get("$ref");
                        if (pa != null) {
                            final String r = new Regex(pa, "\\$\\[\"favorite-models\"\\]\\[0\\]\\[\"photoPageList\"\\]\\[\"_data\"\\]\\[(\\d+)\\]\\[\"owner\"\\]").getMatch(0);
                            pathAlias = (String) JavaScriptEngineFactory.walkJson(resourcelist, "{" + r + "}/owner/pathAlias");
                        }
                        if (pathAlias == null) {
                            // 'r' above can fail.. referenced a another record resourcelist value which doens't have result!
                            pathAlias = (String) JavaScriptEngineFactory.walkJson(entry, "engagement/ownerNsid");
                        }
                    }
                }
                if (pic_id == null || pathAlias == null) {
                    /* Skip invalid items */
                    logger.warning("Found invalid json object");
                    continue;
                }
                final String url = "https://www." + this.getHost() + "/photos/" + pathAlias + "/" + pic_id;
                final DownloadLink fina = createDownloadlink(url);
                final String extension;
                if ("video".equalsIgnoreCase(media)) {
                    extension = ".mp4";
                } else {
                    extension = ".jpg";
                }
                fina.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_MEDIA_TYPE, media);
                fina.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_EXT, extension);
                fina.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME, pathAlias);
                fina.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_CONTENT_ID, pic_id);
                fina.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_TITLE, title);
                fina.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_ORDER_ID, df.format(imagePosition));
                final String formattedFilename = getFormattedFilename(fina);
                fina.setName(formattedFilename);
                fina.setAvailable(true);
                /* No need to hide decrypted single links */
                fina.setContentUrl(url);
                fp.add(fina);
                distribute(fina);
                decryptedLinks.add(fina);
            }
            final int dsize = decryptedLinks.size();
            logger.info("Found " + dsize + " links from " + i + " pages of searching.");
            if (dsize == 0 || (dsize * i) == totalEntries || dsize != (maxEntriesPerPage * i)) {
                logger.info("Stopping at page " + i + " because it seems like we got everything decrypted.");
                break;
            } else if (currentPage != -1) {
                // we only want to decrypt the page user selected.
                logger.info("Stopped at page " + i + " because user selected a single page to decrypt!");
                break;
            } else {
                i++;
            }
        } while (!this.isAbort());
    }

    public String trimFilename(String filename) {
        while (filename != null) {
            if (filename.endsWith(".")) {
                filename = filename.substring(0, filename.length() - 1);
            } else if (filename.endsWith(" ")) {
                filename = filename.substring(0, filename.length() - 1);
            } else {
                break;
            }
        }
        return filename;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost flickrPlugin = getNewPluginForHostInstance(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        if (aa != null) {
            ((jd.plugins.hoster.FlickrCom) flickrPlugin).login(aa, false);
            return true;
        } else {
            return false;
        }
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }
}