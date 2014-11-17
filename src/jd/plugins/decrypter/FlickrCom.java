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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
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

    private static final String MAINPAGE          = "http://flickr.com/";
    private static final String FAVORITELINK      = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/favorites";
    private static final String GROUPSLINK        = "https?://(www\\.)?flickr\\.com/groups/[^<>\"/]+([^<>\"]+)?";
    private static final String PHOTOLINK         = "https?://(www\\.)?flickr\\.com/photos/.*?";
    private static final String SETLINK           = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/sets/\\d+";

    private static final String TYPE_SINGLE_PHOTO = "https?://(www\\.)?flickr\\.com/photos/[^<>\"/]+/\\d+.+";

    private static final String INVALIDLINKS      = "https?://(www\\.)?flickr\\.com/(photos/(me|upload|tags.*?)|groups/[^<>\"/]+/rules|groups/[^<>\"/]+/discuss.*?)";

    private static final String api_key           = "44044129d5965db8c39819e54274917b";

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

    // private boolean USE_API = false;

    /* TODO: Implement API: https://api.flickr.com/services/rest?photo_id=&extras=can_ ... */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String[]> addLinks = new ArrayList<String[]>();
        HashSet<String> dupeCheckMap = new HashSet<String>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.setCookie(MAINPAGE, "localization", "en-us%3Bus%3Bde");
        br.setCookie(MAINPAGE, "fldetectedlang", "en-us");

        String parameter = correctParameter(param.toString());

        int lastPage = 1;
        /* Check if link is for hosterplugin */
        if (parameter.matches(TYPE_SINGLE_PHOTO)) {
            final DownloadLink dl = createDownloadlink(parameter.replace("flickr.com/", "flickrdecrypted.com/").replace("https://", "http://"));
            try {
                dl.setContentUrl(parameter);
            } catch (Throwable e) {

            }
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (parameter.matches(INVALIDLINKS) || parameter.contains("/map")) {
            final DownloadLink offline = createDownloadlink("http://flickrdecrypted.com/photos/xxoffline/" + System.currentTimeMillis() + new Random().nextInt(10000));
            try {
                offline.setContentUrl(parameter);
            } catch (Throwable e) {

            }
            offline.setName(new Regex(parameter, "flickr\\.com/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.containsHTML("Page Not Found<|>This member is no longer active") || br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("http://flickrdecrypted.com/photos/xxoffline/" + System.currentTimeMillis() + new Random().nextInt(10000));
            try {
                offline.setContentUrl(parameter);
            } catch (Throwable e) {

            }
            offline.setFinalFileName(new Regex(parameter, "flickr\\.com/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (parameter.matches(FAVORITELINK) && br.containsHTML("id=\"no\\-faves\"")) {
            /* Favourite link but user has no favourites */
            final DownloadLink offline = createDownloadlink("http://flickrdecrypted.com/photos/xxoffline/" + System.currentTimeMillis() + new Random().nextInt(10000));
            try {
                offline.setContentUrl(parameter);
            } catch (Throwable e) {

            }
            offline.setName(new Regex(parameter, "flickr\\.com/photos/([^<>\"/]+)/favorites").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (parameter.matches(PHOTOLINK) && br.containsHTML("class=\"refresh\\-empty\\-state\\-photostream\"")) {
            /* Photos link has no photos */
            final DownloadLink offline = createDownloadlink("http://flickrdecrypted.com/photos/xxoffline/" + System.currentTimeMillis() + new Random().nextInt(10000));
            try {
                offline.setContentUrl(parameter);
            } catch (Throwable e) {

            }
            offline.setName(new Regex(parameter, "flickr\\.com/photos/(.+)").getMatch(0) + ".jpg");
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        /* Login is not always needed but we force it to get all pictures */
        final boolean logged_in = getUserLogin();
        if (!logged_in) {
            logger.info("Login failed or no accounts active/existing -> Continuing without account");
        }
        int maxEntriesPerPage;
        String fpName = null;
        final boolean useapi = false;
        if (useapi) {
            maxEntriesPerPage = 500;
            final String username = new Regex(parameter, "flickr\\.com/photos/([^<>\"/]+)").getMatch(0);
            /* TODO: 1. Get correct csrf values 2. Implement support for single photo links */
            final String csrf = "1405808633%3Ai01dgnb1q25wxw29%3Ac82715e60f008b97cb7e8fa3529ce156";
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            getPage("https://api.flickr.com/services/rest?per_page=" + "5" + "&page=1&get_user_info=1&path_alias=" + username + "&method=flickr.people.getPhotos&csrf=" + csrf + "&api_key=" + api_key + "&format=json&hermes=1&hermesClient=1&reqId=19et3hbx&nojsoncallback=1");
            final int totalimgs = Integer.parseInt(getJson(br.toString(), "total"));
            final int totalpages = Integer.parseInt(getJson(br.toString(), "pages"));
            fpName = "flickr.com images of user " + username;
            for (int i = 1; i <= totalpages; i++) {
                logger.info("Progress: Page " + i + " of " + totalpages + " || Images: " + decryptedLinks.size() + " of " + totalimgs);
                if (i > 1) {
                    getPage("https://api.flickr.com/services/rest?per_page=" + maxEntriesPerPage + "&page=" + i + "&extras=can_addmeta%2Ccan_comment%2Ccan_download%2Ccan_share%2Ccontact%2Ccount_comments%2Ccount_faves%2Ccount_notes%2Ccount_views%2Cdate_taken%2Cdate_upload%2Cdescription%2Cicon_urls_deep%2Cisfavorite%2Cispro%2Clicense%2Cmedia%2Cneeds_interstitial%2Cowner_name%2Cowner_datecreate%2Cpath_alias%2Crealname%2Csafety_level%2Csecret_k%2Csecret_h%2Curl_c%2Curl_h%2Curl_k%2Curl_l%2Curl_m%2Curl_n%2Curl_o%2Curl_q%2Curl_s%2Curl_sq%2Curl_t%2Curl_z%2Cvisibility&get_user_info=1&path_alias=" + username + "&method=flickr.people.getPhotos&csrf=" + csrf + "&api_key=" + api_key + "&format=json&hermes=1&hermesClient=1&reqId=19et3hbx&nojsoncallback=1");
                }
                final String jsontext = br.getRegex("\"photo\":\\[(\\{.*?\\})\\]").getMatch(0);
                final String[] jsonarray = jsontext.split("\\},\\{");
                for (final String jsonentry : jsonarray) {
                    final String photoid = getJson(jsonentry, "id");
                    final String title = getJson(jsonentry, "title");
                    final String description = new Regex(jsonentry, "\"description\":\\{\"_content\":\"(.+)\"\\}").getMatch(0);
                    final DownloadLink fina = createDownloadlink("http://www.flickrdecrypted.com/photos/" + username + "/" + photoid);
                    if (description != null) {
                        try {
                            fina.setComment(Encoding.htmlDecode(description));
                        } catch (Throwable e) {
                        }
                    }
                    fina.setName(username + "_" + photoid + "_" + title + ".jpg");
                    fina.setProperty("LINKDUPEID", "flickrcom_" + username + "_" + photoid);
                    fina.setAvailable(true);
                    decryptedLinks.add(fina);
                }
            }
        } else {
            /* Old code, last change 21.05.2014 */
            // no need to reget a page you're already on
            if (logged_in) {
                br.getPage(parameter);
            }
            if ((br.containsHTML("class=\"ThinCase Interst\"") || br.getURL().contains("/login.yahoo.com/")) && !logged_in) {
                logger.info("Account needed to decrypt this link: " + parameter);
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            } else if (br.containsHTML("doesn\\'t have anything available to you")) {
                logger.info("Link offline (empty): " + parameter);
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            /* Check if we have a single link */
            if (br.containsHTML("var photo = \\{")) {
                final DownloadLink dl = createDownloadlink("http://www.flickrdecrypted.com/" + new Regex(parameter, "flickr\\.com/(.+)").getMatch(0));
                try {
                    String url = br.getRegex("<meta property=\"og\\:url\" content=\"([^\"]+)").getMatch(0);
                    dl.setContentUrl(url);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                try {
                    String url = br.getRegex("<meta property=\"flickr_photos\\:sets\" content=\"([^\"]+)").getMatch(0);
                    dl.setContainerUrl(url);
                } catch (Throwable e) {
                    e.printStackTrace();

                }
                dl.setName(getFilename());

                decryptedLinks.add(dl);
            } else {
                // Some stuff which is different from link to link
                String picCount = br.getRegex("\"total\":(\")?(\\d+)").getMatch(1);
                maxEntriesPerPage = 72;
                fpName = br.getRegex("<title>Flickr: ([^<>\"]*)</title>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("\"search_default\":\"Search ([^<>\"]*)\"").getMatch(0);
                }
                if (parameter.endsWith("/sets/") && !parameter.matches(SETLINK)) {
                    LinkedHashSet<String> set_ids = new LinkedHashSet<String>();
                    logger.info("Decrypting all set links (albums) of a user...");
                    while (true) {
                        final String[] set_id = br.getRegex("class=\"Seta\" data\\-setid=\"(\\d+)\"").getColumn(0);
                        if (set_id == null || set_id.length == 0) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        set_ids.addAll(Arrays.asList(set_id));
                        String next_page = br.getRegex("<a data-track=\"next\" href=\"(/photos/[^/]+/sets/\\?(?:&(?:amp;)?)?page=\\d+)\" class=\"Next rapidnofollow\">next &rarr;</a>").getMatch(0);
                        if (next_page != null) {
                            next_page = HTMLEntities.unhtmlentities(next_page);
                            br.getPage(next_page);
                            continue;
                        } else {
                            break;
                        }
                    }
                    for (final String set_id : set_ids) {
                        decryptedLinks.add(createDownloadlink(parameter + set_id));
                    }
                    return decryptedLinks;
                } else if (parameter.matches(SETLINK)) {
                    if (picCount == null) {
                        picCount = br.getRegex("class=\"Results\">\\((\\d+) in set\\)</div>").getMatch(0);
                    }
                    if (picCount == null) {
                        picCount = br.getRegex("<div class=\"vsNumbers\">[\t\n\r ]+(\\d+) photos").getMatch(0);
                    }
                    if (picCount == null) {
                        picCount = br.getRegex("<div class=\"stats\">.*?<h1>(\\d+)</h1>[\t\n\r ]+<h2>").getMatch(0);
                    }

                    fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
                    if (fpName == null) {
                        fpName = br.getRegex("<title>([^<>\"]*?) \\- a set on Flickr</title>").getMatch(0);
                    }
                } else if (parameter.matches(PHOTOLINK)) {
                    maxEntriesPerPage = 100;
                } else if (parameter.matches(FAVORITELINK)) {
                    fpName = br.getRegex("<title>([^<>\"]*?) \\| Flickr</title>").getMatch(0);
                } else if (parameter.matches(GROUPSLINK)) {
                    /* Correct groups links as they are sometimes crippled. */
                    final String better_link = br.getRegex("<meta property=\"og:url\" content=\"(https?://(www\\.)?flickr\\.com/groups/[^<>\"]*?)\"").getMatch(0);
                    if (better_link != null) {
                        parameter = better_link;
                    }
                    if (picCount == null) {
                        picCount = br.getRegex("<h1>(\\d+(,\\d+)?)</h1>[\t\n\r ]+<h2>Photos</h2>").getMatch(0);
                    }
                }
                if (picCount == null) {
                    logger.warning("Couldn't find total number of pictures, aborting...");
                    return null;
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
                if (parameter.matches(GROUPSLINK) && parameter.endsWith("/")) {
                    // Try other way of loading more pictures for groups links
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    getPage = parameter + "page%s/?fragment=1";
                }
                for (int i = 1; i <= lastPage; i++) {
                    try {
                        if (this.isAbort()) {
                            logger.info("Decryption aborted by user: " + parameter);
                            return decryptedLinks;
                        }
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    int addedLinksCounter = 0;
                    if (i != 1) {
                        br.getPage(String.format(getPage, i));
                    }
                    final String[] regexes = { "data\\-track=\"photo\\-click\" href=\"(/photos/[^<>\"\\'/]+/\\d+)" };
                    for (String regex : regexes) {
                        String[] links = br.getRegex(regex).getColumn(0);
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
                    }
                    logger.info("Found " + addedLinksCounter + " links on page " + i + " of approximately " + lastPage + " pages.");
                    logger.info("Found already " + addLinks.size() + " of " + totalEntries + " entries, so we still have to decrypt " + (totalEntries - addLinks.size()) + " entries!");
                    if (addedLinksCounter == 0 || addLinks.size() == totalEntries) {
                        logger.info("Stopping at page " + i + " because it seems like we got everything decrypted.");
                        break;
                    }
                }
                if (addLinks == null || addLinks.size() == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String[] aLink : addLinks) {
                    final DownloadLink dl = createDownloadlink("http://www.flickrdecrypted.com" + aLink[1]);
                    dl.setName(aLink[0] == null ? null : (aLink[0] + ".jpg"));
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
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /** Corrects links added by the user */
    private String correctParameter(String parameter) {
        parameter = Encoding.htmlDecode(parameter).replace("http://", "https://");
        parameter = parameter.replace("secure.flickr.com/", "flickr.com/");
        String remove_string = new Regex(parameter, "(/player/.+)").getMatch(0);
        if (remove_string != null) {
            parameter = parameter.replace(remove_string, "");
        }
        if (parameter.matches(PHOTOLINK)) {
            remove_string = new Regex(parameter, "(/sizes/.+)").getMatch(0);
            if (remove_string != null) {
                parameter = parameter.replace(remove_string, "");
            }
        }
        return parameter;
    }

    private void getPage(final String url) throws IOException {
        br.getPage(url);
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
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

}