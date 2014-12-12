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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "https?://(www\\.)?(secure\\.)?flickr\\.com/(photos|groups)/.+" }, flags = { 0 })
public class FlickrCom extends PluginForDecrypt {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     MAINPAGE                 = "http://flickr.com/";
    private static final String     NICE_HOST                = "flickr.com";
    private static final String     TYPE_FAVORITES           = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/favorites/?";
    private static final String     TYPE_GROUPS              = "https?://(www\\.)?flickr\\.com/groups/[^<>\"/]+([^<>\"]+)?";
    private static final String     TYPE_SET                 = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/sets/\\d+/?";
    private static final String     TYPE_SINGLE_PHOTO        = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/\\d+.+";

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
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.setCookie(MAINPAGE, "localization", "en-us%3Bus%3Bde");
        br.setCookie(MAINPAGE, "fldetectedlang", "en-us");
        parameter = correctParameter(param.toString());
        username = new Regex(parameter, "(?:photos|groups)/([^<>\"/]+)").getMatch(0);

        try {
            /* Check if link is for hosterplugin */
            if (parameter.matches(TYPE_SINGLE_PHOTO)) {
                final DownloadLink dl = createDownloadlink(parameter.replace("flickr.com/", "flickrdecrypted.com/").replace("https://", "http://"));
                try {
                    dl.setContentUrl(parameter);
                } catch (Throwable e) {
                    /* Not available in old 0.9.591 Stable */
                    dl.setBrowserUrl(parameter);
                }
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
            if (!parameter.matches(TYPE_FAVORITES)) {
                api_handleAPI();
            } else {
                site_handleSite();
            }
        } catch (final DecrypterException e) {
            if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                try {
                    offline.setContentUrl(parameter);
                } catch (Throwable eold) {
                    /* Not available in old 0.9.591 Stable */
                    offline.setBrowserUrl(parameter);
                }
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

    /** Handles decryption via API */
    @SuppressWarnings("deprecation")
    private void api_handleAPI() throws IOException, DecrypterException {
        /* TODO: Fix csrf handling to make requests as logged-in user possible. */
        br.clearCookies(MAINPAGE);
        String fpName = null;
        api_apikey = getPublicAPIKey(this.br);
        if (api_apikey == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        br.getHeaders().put("Referer", "");
        csrf = "1405808633%3Ai01dgnb1q25wxw29%3Ac82715e60f008b97cb7e8fa3529ce156";
        csrf = getJson("csrf");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        String apilink = null;
        String path_alias = null;
        String owner = null;
        if (parameter.matches(TYPE_SET)) {
            final String setid = new Regex(parameter, "(\\d+)/?$").getMatch(0);
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&photoset_id=" + Encoding.urlEncode(setid) + "&method=flickr.photosets.getPhotos" + "&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
            owner = getJson("owner");
            if (owner == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                decryptedLinks = null;
                return;
            }
            fpName = "flickr.com set " + setid + " of user " + this.username;
        } else if (parameter.endsWith("/sets") && !parameter.matches(TYPE_SET)) {
            apiGetSetsOfUser();
            return;
        } else if (parameter.matches(TYPE_FAVORITES)) {
            final String nsid = apiLookupUser(null);
            if (nsid == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&user_id=" + Encoding.urlEncode(nsid) + "&method=flickr.favorites.getList&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
            fpName = "flickr.com favourites of user " + this.username;
        } else if (parameter.matches(TYPE_GROUPS)) {
            br.getPage("https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&method=flickr.urls.lookupGroup&url=" + Encoding.urlEncode(parameter));
            final String group_id = getJson("id");
            final String groupname = br.getRegex("\"groupname\":\\{\"_content\":\"([^<>\"]*?)\"\\}").getMatch(0);
            path_alias = new Regex(parameter, "flickr\\.com/groups/([^<>\"/]+)").getMatch(0);
            if (group_id == null || path_alias == null || groupname == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&extras=&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&get_group_info=1&group_id=" + Encoding.urlEncode(group_id) + "&method=flickr.groups.pools.getPhotos&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
            fpName = "flickr.com images of group " + group_id;
        } else {
            path_alias = this.username;
            apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&get_user_info=1&path_alias=" + this.username + "&method=flickr.people.getPhotos&hermes=1&hermesClient=1&nojsoncallback=1";
            api_getPage(apilink.replace("GETJDPAGE", "1"));
            fpName = "flickr.com images of user " + this.username;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        final int totalimgs = Integer.parseInt(getJson("total"));
        final int totalpages = Integer.parseInt(getJson("pages"));
        for (int i = 1; i <= totalpages; i++) {
            logger.info("Progress: Page " + i + " of " + totalpages + " || Images: " + decryptedLinks.size() + " of " + totalimgs);
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            if (i > 1) {
                api_getPage(apilink.replace("GETJDPAGE", Integer.toString(i)));
            }
            final String jsontext = br.getRegex("\"photo\":\\[(\\{.*?\\})\\]").getMatch(0);
            final String[] jsonarray = jsontext.split("\\},\\{");
            for (final String jsonentry : jsonarray) {
                if (owner == null) {
                    owner = getJson(jsonentry, "owner");
                }
                final String photoid = getJson(jsonentry, "id");
                String title = getJson(jsonentry, "title");
                if (owner == null || photoid == null || title == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    decryptedLinks = null;
                    return;
                }
                title = encodeUnicode(title);
                String description = new Regex(jsonentry, "\"description\":\\{\"_content\":\"(.+)\"\\}").getMatch(0);
                final DownloadLink fina = createDownloadlink("http://www.flickrdecrypted.com/photos/" + owner + "/" + photoid);
                if (description != null) {
                    try {
                        description = Encoding.htmlDecode(description);
                        fina.setComment(description);
                    } catch (Throwable e) {
                    }
                }
                final String contenturl = "https://www.flickr.com/photos/" + owner + "/" + photoid;
                try {
                    fina.setContentUrl(contenturl);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.591 Stable */
                    fina.setBrowserUrl(contenturl);
                }
                final String phototitle = username + "_" + photoid + "_" + title + ".jpg";
                fina.setName(phototitle);
                fina.setProperty("decryptedfilename", phototitle);
                fina.setProperty("LINKDUPEID", "flickrcom_" + username + "_" + photoid);
                fina.setAvailable(true);
                fina._setFilePackage(fp);
                try {
                    distribute(fina);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                }
                fp.addLinks(decryptedLinks);
                decryptedLinks.add(fina);
            }
        }
    }

    private void apiGetSetsOfUser() throws IOException, DecrypterException {
        final String nsid = apiLookupUser(null);
        if (nsid == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        String apilink = "https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&per_page=" + api_max_entries_per_page + "&page=GETJDPAGE&user_id=" + Encoding.urlEncode(nsid) + "&method=flickr.photosets.getList&csrf=&api_key=" + api_apikey + "&format=json&hermes=1&hermesClient=1&reqId=9my34ua&nojsoncallback=1";
        api_getPage(apilink.replace("GETJDPAGE", "1"));
        final int totalimgs = Integer.parseInt(getJson("total"));
        final int totalpages = Integer.parseInt(getJson("pages"));
        for (int i = 1; i <= totalpages; i++) {
            logger.info("Progress: Page " + i + " of " + totalpages + " || Images: " + decryptedLinks.size() + " of " + totalimgs);
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    decryptedLinks = null;
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            if (i > 1) {
                api_getPage(apilink.replace("GETJDPAGE", Integer.toString(i)));
            }
            final String jsontext = br.getRegex("\"photoset\":\\[(\\{.*?\\})\\]").getMatch(0);
            final String[] jsonarray = jsontext.split("\\},\\{");
            for (final String jsonentry : jsonarray) {
                final String setid = getJson(jsonentry, "id");
                final String contenturl = "https://www.flickr.com/photos/" + username + "/sets/" + setid + "/";
                final DownloadLink fina = createDownloadlink(contenturl);
                decryptedLinks.add(fina);
            }
        }
    }

    private String apiLookupUser(String username) throws IOException, DecrypterException {
        if (username == null) {
            username = this.username;
        }
        final String user_url = "https://www.flickr.com/photos/" + username + "/";
        api_getPage("https://api.flickr.com/services/rest?format=" + api_format + "&csrf=" + this.csrf + "&api_key=" + api_apikey + "&method=flickr.urls.lookupUser&url=" + Encoding.urlEncode(user_url));
        return getJson("id");
    }

    private void api_getPage(final String url) throws IOException, DecrypterException {
        br.getPage(url);
        updatestatuscode();
        this.handleAPIErrors(this.br);
    }

    /** Check for errorcode and set it if existant */
    private void updatestatuscode() {
        String errorcode = getJson("error");
        if (errorcode == null) {
            errorcode = getJson("code");
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
            api_apikey = getJson(br.toString(), "api_key");
        }
        if (api_apikey == null) {
            api_apikey = "80bd84ccc43c9992edf04205340abe2f";
        }
        return api_apikey;
    }

    /**
     * Handles decryption via site.
     *
     * @throws IOException
     * @throws DecrypterException
     */
    private void site_handleSite() throws IOException, DecrypterException {
        ArrayList<String[]> addLinks = new ArrayList<String[]>();
        HashSet<String> dupeCheckMap = new HashSet<String>();
        int lastPage = 1;
        int maxEntriesPerPage;
        String fpName;
        br.getPage(parameter);
        if ((br.containsHTML("class=\"ThinCase Interst\"") || br.getURL().contains("/login.yahoo.com/")) && !this.loggedin) {
            logger.info("Account needed to decrypt this link: " + parameter);
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        } else if (parameter.matches(TYPE_FAVORITES) && br.containsHTML("id=\"no\\-faves\"")) {
            /* Favourite link but user has no favourites */
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        // Some stuff which is different from link to link
        String picCount = br.getRegex("\"total\":(\")?(\\d+)").getMatch(1);
        maxEntriesPerPage = 72;
        fpName = br.getRegex("<title>Flickr: ([^<>\"]*)</title>").getMatch(0);
        if (fpName == null) {
            fpName = "favourites of user " + username;
        }
        if (picCount == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }

        final int totalEntries = Integer.parseInt(picCount.replace(",", ""));

        /**
         * Handling for albums/sets: Only decrypt all pages if user did NOT add a direct page link
         * */
        int lastPageCalculated = 0;
        if (!parameter.contains("/page")) {
            logger.info("Decrypting all available pages.");
            // Removed old way of finding page number on the 27.07.12
            // Add 2 extra pages because usually the decrypter should already
            // stop before
            lastPageCalculated = (int) StrictMath.ceil(totalEntries / maxEntriesPerPage);
            lastPage = lastPageCalculated + 2;
            logger.info("Found " + lastPageCalculated + " pages using the calculation method.");
        }

        String getPage = parameter + "/page%s";
        if (parameter.matches(TYPE_GROUPS) && parameter.endsWith("/")) {
            // Try other way of loading more pictures for groups links
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage = parameter + "page%s/?fragment=1";
        }
        for (int i = 1; i <= lastPage; i++) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            int addedLinksCounter = 0;
            if (i != 1) {
                br.getPage(String.format(getPage, i));
            }
            String[] links = br.getRegex("data\\-track=\"photo\\-click\" href=\"(/photos/[^<>\"\\'/]+/\\d+)").getColumn(0);
            if (links != null && links.length != 0) {
                for (String singleLink : links) {
                    // Regex catches links twice, correct that here
                    if (dupeCheckMap.add(singleLink)) {
                        String pattern = Pattern.quote(singleLink) + "[^\"]*\"[^>]+title=\"([^\"]+)";
                        String name = trimFilename(br.getRegex(pattern).getMatch(0));
                        addLinks.add(new String[] { name, singleLink });
                        addedLinksCounter++;
                    }
                }
            }
            logger.info("Found " + addedLinksCounter + " links on page " + i + " of approximately " + lastPage + " pages.");
            logger.info("Found already " + addLinks.size() + " of " + totalEntries + " entries, so we still have to decrypt " + (totalEntries - addLinks.size()) + " entries!");
            if (addedLinksCounter == 0 || addLinks.size() == totalEntries) {
                logger.info("Stopping at page " + i + " because it seems like we got everything decrypted.");
                break;
            }
        }
        if (addLinks == null || addLinks.size() == 0) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (final String[] aLink : addLinks) {
            final DownloadLink dl = createDownloadlink("http://www.flickrdecrypted.com" + aLink[1]);
            dl.setName(aLink[0] == null ? null : (Encoding.htmlDecode(aLink[0]) + ".jpg"));
            dl.setAvailable(true);
            /* No need to hide decrypted single links */
            try {/* JD2 only */
                dl.setContentUrl("http://www.flickr.com" + aLink);
            } catch (Throwable e) {/* Stable */
                dl.setBrowserUrl("http://www.flickr.com" + aLink);
            }
            try {
                distribute(dl);
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private static String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
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

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

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

        // trim
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

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

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