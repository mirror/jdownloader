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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flickr.com" }, urls = { "https?://(www\\.)?(secure\\.)?flickr\\.com/(photos|groups)/.+" })
public class FlickrCom extends PluginForDecrypt {
    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     MAINPAGE                 = "http://flickr.com/";
    private static final String     NICE_HOST                = "flickr.com";
    private static final String     TYPE_FAVORITES           = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/favorites(/.+)?";
    private static final String     TYPE_GROUPS              = "https?://(www\\.)?flickr\\.com/groups/[^<>\"/]+([^<>\"]+)?";
    private static final String     TYPE_SET_SINGLE          = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/(?:sets|albums)/\\d+/?";
    private static final String     TYPE_SETS_OF_USER_ALL    = ".+/(?:albums|sets)/?$";
    private static final String     TYPE_SINGLE_PHOTO        = "https?://(www\\.)?flickr\\.com/photos/(?!tags/)[^<>\"/]+/\\d+.+";
    private static final String     TYPE_PHOTO               = "https?://(www\\.)?flickr\\.com/photos/.*?";
    private static final String     INVALIDLINKS             = "https?://(www\\.)?flickr\\.com/(photos/(me|upload|tags.*?)|groups/[^<>\"/]+/rules|groups/[^<>\"/]+/discuss.*?)";
    private static final String     EXCEPTION_LINKOFFLINE    = "EXCEPTION_LINKOFFLINE";
    private static final String     api_format               = "json";
    private static final int        api_max_entries_per_page = 500;
    private ArrayList<DownloadLink> decryptedLinks           = new ArrayList<DownloadLink>();
    private String                  api_apikey               = null;
    private String                  csrf                     = null;
    private String                  parameter                = null;
    private String                  username                 = null;
    private boolean                 loggedin                 = false;
    private int                     statuscode               = 0;

    /**
     * Using API: https://www.flickr.com/services/api/ - without our own apikey. Site is still used for /* TODO API: Get correct csrf values
     * so we can make requests as a logged-in user
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "localization", "en-us%3Bus%3Bde");
        br.setCookie(MAINPAGE, "fldetectedlang", "en-us");
        br.setLoadLimit(br.getLoadLimit() * 2);
        parameter = correctParameter(param.toString());
        username = new Regex(parameter, "(?:photos|groups)/([^<>\"/]+)").getMatch(0);
        try {
            /* Check if link is for hosterplugin */
            if (parameter.matches(TYPE_SINGLE_PHOTO)) {
                final DownloadLink dl = createDownloadlink(parameter.replace("flickr.com/", "flickrdecrypted.com/").replace("https://", "http://"));
                dl.setContentUrl(parameter);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            if (parameter.matches(INVALIDLINKS) || parameter.equals("https://www.flickr.com/photos/groups/") || parameter.contains("/map")) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            /* Login is not always needed but we force it to get all pictures */
            this.loggedin = getUserLogin();
            if (!this.loggedin) {
                logger.info("Login failed or no accounts active/existing -> Continuing without account");
            }
            if (parameter.matches(TYPE_FAVORITES)) {
                site_handleSite();
            } else {
                api_handleAPI();
            }
        } catch (final DecrypterException e) {
            if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setContentUrl(parameter);
                offline.setFinalFileName(new Regex(parameter, "flickr\\.com/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            throw e;
        }
        return decryptedLinks;
    }

    /** Corrects links added by the user */
    private String correctParameter(String parameter) {
        String remove_string = null;
        parameter = Encoding.htmlDecode(parameter).replace("http://", "https://");
        parameter = parameter.replace("secure.flickr.com/", "flickr.com/");
        final String[] removeStuff = { "(/player/.+)", "(/with/.+)" };
        for (final String removethis : removeStuff) {
            remove_string = new Regex(parameter, removethis).getMatch(0);
            if (remove_string != null) {
                parameter = parameter.replace(remove_string, "");
            }
        }
        if (parameter.matches(TYPE_PHOTO)) {
            remove_string = new Regex(parameter, "(/sizes/.+)").getMatch(0);
            if (remove_string != null) {
                parameter = parameter.replace(remove_string, "");
            }
        }
        return parameter;
    }

    /**
     * Handles decryption via API
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private void api_handleAPI() throws Exception {
        /* TODO: Fix csrf handling to make requests as logged-in user possible. */
        br.clearCookies(MAINPAGE);
        String fpName = null;
        api_apikey = getPublicAPIKey(this.br);
        if (api_apikey == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        br.getHeaders().put("Referer", "");
        csrf = PluginJSonUtils.getJsonValue(br, "csrf");
        if (csrf == null) {
            // csrf = "1405808633%3Ai01dgnb1q25wxw29%3Ac82715e60f008b97cb7e8fa3529ce156";
            csrf = "";
        }
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        String forcedOwner = null;
        String apilink = null;
        String path_alias = null;
        if (parameter.matches(TYPE_SET_SINGLE)) {
            final String setid = new Regex(parameter, "(\\d+)/?$").getMatch(0);
            /* This request is only needed to get the title and owner of the photoset, */
            api_getPage("https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&method=flickr.photosets.getInfo&photoset_id=" + Encoding.urlEncode(setid));
            forcedOwner = PluginJSonUtils.getJsonValue(br, "owner");
            if (forcedOwner == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                decryptedLinks = null;
                return;
            }
            fpName = br.getRegex("\"title\":\\{\"_content\":\"([^<>\"]*?)\"\\}").getMatch(0);
            if (fpName == null || fpName.equals("")) {
                fpName = "flickr.com set " + setid + " of user " + this.username;
            }
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&extras=media&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&photoset_id=" + Encoding.urlEncode(setid) + "&method=flickr.photosets.getPhotos" + "&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
        } else if (parameter.matches(TYPE_SETS_OF_USER_ALL) && !parameter.matches(TYPE_SET_SINGLE)) {
            apiGetSetsOfUser();
            return;
        } else if (parameter.matches(TYPE_FAVORITES)) {
            final String nsid = get_NSID(null);
            if (nsid == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&extras=media&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&user_id=" + Encoding.urlEncode(nsid) + "&method=flickr.favorites.getList&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
            fpName = "flickr.com favourites of user " + this.username;
        } else if (parameter.matches(TYPE_GROUPS)) {
            br.getPage("https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&method=flickr.urls.lookupGroup&url=" + Encoding.urlEncode(parameter));
            final String json = br.getRegex("^jsonFlickrApi\\((\\{.*?\\})\\)$").getMatch(0);
            final HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            final HashMap<String, Object> group = (HashMap<String, Object>) entries.get("group");
            final String group_id = (String) group.get("id");
            final String groupname = (String) JavaScriptEngineFactory.walkJson(group, "groupname/_content");
            path_alias = new Regex(parameter, "flickr\\.com/groups/([^<>\"/]+)").getMatch(0);
            if (group_id == null || path_alias == null || groupname == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&extras=media&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&get_group_info=1&group_id=" + Encoding.urlEncode(group_id) + "&method=flickr.groups.pools.getPhotos&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
            fpName = "flickr.com images of group " + group_id;
        } else {
            final String nsid = get_NSID(null);
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&extras=media&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&user_id=" + Encoding.urlEncode(nsid) + "&method=flickr.people.getPublicPhotos&hermes=1&hermesClient=1&nojsoncallback=1";
            /* Use this public request if the other one fails though this might return all but the last picture...?!: */
            // apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey +
            // "&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&user_id=" + Encoding.urlEncode(nsid) +
            // "&method=flickr.people.getPhotos&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
            fpName = "flickr.com images of user " + this.username;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        final int totalimgs = Integer.parseInt(PluginJSonUtils.getJsonValue(br, "total"));
        final int totalpages = Integer.parseInt(PluginJSonUtils.getJsonValue(br, "pages"));
        for (int i = 1; i <= totalpages; i++) {
            logger.info("Progress: Page " + i + " of " + totalpages + " || Images: " + decryptedLinks.size() + " of " + totalimgs);
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return;
            }
            if (i > 1) {
                api_getPage(apilink.replace("GETJDPAGE", Integer.toString(i)));
            }
            final String jsontext = br.getRegex("\"photo\":\\[(\\{.*?\\})\\]").getMatch(0);
            final String[] jsonarray = jsontext.split("\\},\\{");
            for (final String jsonentry : jsonarray) {
                String owner = null;
                /* E.g. in a set, all pictures got the same owner so the "owner" key is not available here. */
                if (forcedOwner != null) {
                    owner = forcedOwner;
                } else {
                    owner = PluginJSonUtils.getJsonValue(jsonentry, "owner");
                }
                final String photo_id = PluginJSonUtils.getJsonValue(jsonentry, "id");
                String title = PluginJSonUtils.getJsonValue(jsonentry, "title");
                final String dateadded = PluginJSonUtils.getJsonValue(jsonentry, "dateadded");
                if (owner == null || photo_id == null || title == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    decryptedLinks = null;
                    return;
                }
                title = encodeUnicode(title);
                String description = new Regex(jsonentry, "\"description\":\\{\"_content\":\"(.+)\"\\}").getMatch(0);
                final DownloadLink fina = createDownloadlink("http://www.flickrdecrypted.com/photos/" + owner + "/" + photo_id);
                if (description != null) {
                    try {
                        description = Encoding.htmlDecode(description);
                        fina.setComment(description);
                    } catch (Throwable e) {
                    }
                }
                final String contenturl = "https://www.flickr.com/photos/" + owner + "/" + photo_id;
                fina.setContentUrl(contenturl);
                if (title.equals("")) {
                    title = null;
                }
                final String media = PluginJSonUtils.getJsonValue(jsonentry, "media");
                final String extension;
                if ("video".equalsIgnoreCase(media)) {
                    fina.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                    extension = ".mp4";
                } else {
                    fina.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPEG);
                    extension = ".jpg";
                }
                final String decryptedfilename = username + "_" + photo_id + (title != null ? "_" + title : jd.plugins.hoster.FlickrCom.getCustomStringForEmptyTags()) + extension;
                fina.setProperty("decryptedfilename", decryptedfilename);
                fina.setProperty("photo_id", photo_id);
                fina.setProperty("media", media);
                fina.setProperty("owner", owner);
                fina.setProperty("username", username);
                if (dateadded != null) {
                    fina.setProperty("dateadded", Long.parseLong(dateadded) * 1000);
                }
                if (title != null) {
                    fina.setProperty("title", title);
                }
                fina.setProperty("ext", extension);
                fina.setProperty("LINKDUPEID", "flickrcom_" + username + "_" + photo_id);
                fina.setProperty("custom_filenames_allowed", true);
                final String formattedFilename = getFormattedFilename(fina);
                fina.setName(formattedFilename);
                fina.setAvailable(true);
                fina._setFilePackage(fp);
                distribute(fina);
                fp.addLinks(decryptedLinks);
                decryptedLinks.add(fina);
            }
        }
    }

    private String getFormattedFilename(final DownloadLink dl) throws ParseException {
        return jd.plugins.hoster.FlickrCom.getFormattedFilename(dl);
    }

    private void apiGetSetsOfUser() throws IOException, DecrypterException {
        final String nsid = get_NSID(null);
        if (nsid == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        String apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&user_id=" + Encoding.urlEncode(nsid) + "&method=flickr.photosets.getList&csrf=&api_key=" + api_apikey + "&format=json&hermes=1&hermesClient=1&reqId=9my34ua&nojsoncallback=1";
        api_getPage(apilink.replace("GETJDPAGE", "1"));
        final int totalimgs = Integer.parseInt(PluginJSonUtils.getJsonValue(br, "total"));
        final int totalpages = Integer.parseInt(PluginJSonUtils.getJsonValue(br, "pages"));
        for (int i = 1; i <= totalpages; i++) {
            logger.info("Progress: Page " + i + " of " + totalpages + " || Images: " + decryptedLinks.size() + " of " + totalimgs);
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                decryptedLinks = null;
                return;
            }
            if (i > 1) {
                api_getPage(apilink.replace("GETJDPAGE", Integer.toString(i)));
            }
            final String jsontext = br.getRegex("\"photoset\":\\[(\\{.*?\\})\\]").getMatch(0);
            final String[] jsonarray = jsontext.split("\\},\\{");
            for (final String jsonentry : jsonarray) {
                final String setid = PluginJSonUtils.getJsonValue(jsonentry, "id");
                final String contenturl = "https://www.flickr.com/photos/" + username + "/sets/" + setid + "/";
                final DownloadLink fina = createDownloadlink(contenturl);
                decryptedLinks.add(fina);
            }
        }
    }

    private String get_NSID(String username) throws IOException, DecrypterException {
        String nsid;
        if (username == null) {
            username = this.username;
        }
        /* Check if we already have the id */
        if (username.matches("\\d+@N\\d+")) {
            nsid = username;
        } else {
            /* We don't have it --> Grab it */
            nsid = apiLookupUser(username);
        }
        return nsid;
    }

    /** (API) Function to find the nsid of a user. */
    private String apiLookupUser(String username) throws IOException, DecrypterException {
        if (username == null) {
            username = this.username;
        }
        final String user_url = "https://www.flickr.com/photos/" + username + "/";
        api_getPage("https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&method=flickr.urls.lookupUser&url=" + Encoding.urlEncode(user_url));
        return PluginJSonUtils.getJsonValue(br, "id");
    }

    private void api_getPage(final String url) throws IOException, DecrypterException {
        br.getPage(url);
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
    private void handleAPIErrors(final Browser br) throws DecrypterException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                statusMessage = "Group/user/photo not found - possibly invalid nsid";
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            case 2:
                statusMessage = "No user specified or permission denied";
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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
        } catch (final DecrypterException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    public static String getPublicAPIKey(final Browser br) throws IOException {
        br.getPage("https://www.flickr.com/photos/groups/");
        String api_apikey = br.getRegex("root\\.YUI_config\\.flickr\\.api\\.site_key\\s*?=\\s*?\"(.*?)\"").getMatch(0);
        /* Handle API decryption for GROUPS and complete users here */
        if (api_apikey == null) {
            api_apikey = PluginJSonUtils.getJsonValue(br, "api_key");
        }
        if (api_apikey == null) {
            api_apikey = "80bd84ccc43c9992edf04205340abe2f";
        }
        return api_apikey;
    }

    /**
     * Handles decryption via site WITHOUT JAVASCRIPT ENABLED!
     *
     * @throws Exception
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    private void site_handleSite() throws Exception {
        // if not logged in this is 25... need to confirm for logged in -raztoki20160717
        int maxEntriesPerPage = 25;
        String fpName;
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        } else if ((br.containsHTML("class=\"ThinCase Interst\"") || br.getURL().contains("/login.yahoo.com/")) && !this.loggedin) {
            logger.info("Account needed to decrypt this link: " + parameter);
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        } else if (parameter.matches(TYPE_FAVORITES) && br.containsHTML("id=\"no\\-faves\"")) {
            /* Favourite link but user has no favourites */
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        // Some stuff which is different from link to link
        String picCount = br.getRegex("\"total\":(\")?(\\d+)").getMatch(1);
        fpName = br.getRegex("<title>(.*?) \\| Flickr</title>").getMatch(0);
        if (fpName == null) {
            fpName = "favourites of user " + username;
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
        if (parameter.contains("/page")) {
            currentPage = Integer.parseInt(new Regex(parameter, "page(\\d+)").getMatch(0));
        }
        String getPage = parameter.replaceFirst("/page\\d+", "") + "/page%s";
        if (parameter.matches(TYPE_GROUPS) && parameter.endsWith("/")) {
            // Try other way of loading more pictures for groups links
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage = parameter + "page%s/?fragment=1";
        }
        int i = (currentPage != -1 ? currentPage : 1);
        while (true) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return;
            }
            if (i != 1 && currentPage == -1) {
                br.getPage(String.format(getPage, i));
                // when we are out of pages, it will redirect back to non page count
                if (br.getURL().equals(getPage.replace("/page%s", "/"))) {
                    logger.info("No more pages!");
                    break;
                }
            }
            final String json = this.br.getRegex("modelExport[\n\t\r ]*?:[\n\t\r ]*?(\\{.+\\}),").getMatch(0);
            if (json == null) {
                /* This should never happen but if we found links before, lets return them. */
                break;
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            final ArrayList<Object> resourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "favorite-models/{0}/photoPageList/_data");
            int resourcelistCount = 0;
            for (final Object pico : resourcelist) {
                LinkedHashMap<String, Object> entry = (LinkedHashMap<String, Object>) pico;
                if (entry == null) {
                    continue;
                }
                resourcelistCount++;
                String title = (String) entry.get("title");
                String media = (String) entry.get("media");
                final String pic_id = (String) entry.get("id");
                // not all images have a title.
                if (title == null) {
                    title = "";
                }
                LinkedHashMap<String, Object> owner = (LinkedHashMap<String, Object>) entry.get("owner");
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
                    continue;
                }
                /*
                 * TODO: Improve upper part to be able to find the posted-date as well (if possible) so users who user custom filenames get
                 * better filenames right after the decryption process.
                 */
                final String url = "https://www.flickr.com/photos/" + pathAlias + "/" + pic_id;
                final DownloadLink fina = createDownloadlink(url.replace("flickr.com/", "flickrdecrypted.com/"));
                final String extension;
                if ("video".equalsIgnoreCase(media)) {
                    fina.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                    extension = ".mp4";
                } else {
                    fina.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPEG);
                    extension = ".jpg";
                }
                final String decryptedfilename = title == null ? null : (Encoding.htmlDecode(title) + extension);
                fina.setProperty("decryptedfilename", decryptedfilename);
                fina.setProperty("media", media);
                fina.setProperty("ext", extension);
                fina.setProperty("username", pathAlias);
                fina.setProperty("photo_id", pic_id);
                fina.setProperty("title", title);
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
        }
    }

    @SuppressWarnings("unused")
    private String getFilename() {
        String filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"photo\\-title\">(.*?)</h1").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\| Flickr \\- Photo Sharing\\!</title>").getMatch(0);
            }
        }
        if (filename == null) {
            filename = br.getRegex("<meta name=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        filename = trimFilename(filename);
        return filename;
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

    @SuppressWarnings("deprecation")
    private boolean getUserLogin() throws Exception {
        final PluginForHost flickrPlugin = JDUtilities.getPluginForHost("flickr.com");
        final Account aa = AccountController.getInstance().getValidAccount(flickrPlugin);
        if (aa != null) {
            try {
                ((jd.plugins.hoster.FlickrCom) flickrPlugin).login(aa, false, this.br);
            } catch (final PluginException e) {
                aa.setValid(false);
                logger.info("Account seems to be invalid!");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }
}