package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "500px.com" }, urls = { "https?://(?:www\\.)?500px\\.com/p/([^/\\?]+).*" })
public class FivehundretPxComCrawler extends PluginForDecrypt {
    public FivehundretPxComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal("api.500px.com", 250);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private static Map<String, String> USER_ID_MAP  = new HashMap<String, String>();
    private URL                        photoBaseURL = null;

    protected String getUserName(final String userID) {
        if (userID != null) {
            synchronized (USER_ID_MAP) {
                final Iterator<Entry<String, String>> it = USER_ID_MAP.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, String> next = it.next();
                    if (userID.equals(next.getValue())) {
                        return next.getKey();
                    }
                }
            }
        }
        return null;
    }

    protected String getUserID(final String username) {
        if (username != null) {
            synchronized (USER_ID_MAP) {
                final Iterator<Entry<String, String>> it = USER_ID_MAP.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, String> next = it.next();
                    if (username.equalsIgnoreCase(next.getKey())) {
                        return next.getValue();
                    }
                }
            }
        }
        return null;
    }

    protected String getUserID(final Browser br, final String url, String username) throws Exception {
        if (username == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        synchronized (USER_ID_MAP) {
            String userID = getUserID(username);
            if (userID == null) {
                userID = "";
                final Browser brc = br.cloneBrowser();
                brc.getPage("https://api.500px.com/v1/users/search?term=" + URLEncode.encodeURIComponent(username));
                final Map<String, Object> map = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Map<String, Object>> users = (List<Map<String, Object>>) map.get("users");
                for (Map<String, Object> user : users) {
                    final String realUserName = StringUtils.valueOfOrNull(user.get("username"));
                    if (username.equalsIgnoreCase(realUserName)) {
                        username = realUserName;
                        userID = StringUtils.valueOfOrNull(user.get("id"));
                        break;
                    }
                }
                USER_ID_MAP.put(username, userID);
            }
            if (StringUtils.isEmpty(userID)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return userID;
        }
    }

    private DownloadLink parsePhoto(Map<String, Object> photo, final String userID, final String userName) throws Exception {
        final Number photoID = (Number) photo.get("id");
        if (photoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String url = (String) photo.get("url");
        if (StringUtils.isNotEmpty(url)) {
            url = URLHelper.parseLocation(photoBaseURL, url).toString();
        } else {
            url = URLHelper.parseLocation(photoBaseURL, "/photo/" + photoID.toString()).toString();
        }
        final DownloadLink downloadLink = createDownloadlink(url);
        final String photoName = (String) photo.get("name");
        String fileName = userName;
        if (StringUtils.isNotEmpty(photoName)) {
            fileName = fileName + " - " + photoName;
        }
        fileName = fileName + " - " + photoID;
        fileName = fileName + ".jpg";
        downloadLink.setFinalFileName(fileName);
        downloadLink.setProperty("userID", userID);
        downloadLink.setProperty("photoID", photoID);
        downloadLink.setAvailable(true);
        return downloadLink;
    }

    /** Docs of public API used down below: https://github.com/500px/legacy-api-documentation/tree/master */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        photoBaseURL = new URL("https://" + this.getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String userNameFromURL = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (userNameFromURL == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String gallerySlug = new Regex(param.getCryptedUrl(), "(?i)/galleries/([\\w\\-]+)").getMatch(0);
        final String userID = getUserID(br, param.getCryptedUrl(), userNameFromURL);
        final String userName = getUserName(userID); // Now get serverside username with camelcase
        final boolean isAllGalleriesOfAUser = gallerySlug == null && StringUtils.containsIgnoreCase(param.getCryptedUrl(), "?view=galleries");
        final int itemsPerPage = 50;
        if (gallerySlug != null) {
            /* Crawl a gallery of a user */
            String galleryName = null;
            if (param.getCryptedUrl().matches(".+/featured")) {
                galleryName = br.getRegex("<option selected value='\\d+'>\\s*(.*?)\\s*</option").getMatch(0);
                if (galleryName == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                galleryName = galleryName.replace(" ", "").replace("-", "_");
            } else {
                galleryName = new Regex(param.getCryptedUrl(), ".+/galleries/(.+)").getMatch(0);
            }
            br.getPage("https://api.500px.com/v1/users/" + userID + "/galleries/" + galleryName + "?include_user=true&include_cover=1&cover_size=2048");
            checkErrorsAPI(br);
            final String galleryID = br.getRegex("\"id\"\\s*:\\s*(\\d+)").getMatch(0);
            if (galleryID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            galleryName = br.getRegex("\"name\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            String fpName = "";
            if (StringUtils.isNotEmpty(userName)) {
                fpName = userName;
            } else {
                fpName = galleryID;
            }
            if (StringUtils.isNotEmpty(galleryName)) {
                if (fpName.length() > 0) {
                    fpName = fpName + "_" + galleryName;
                }
            }
            fp.setName(fpName);
            fp.setPackageKey("500px://gallery/" + galleryID);
            int page = 1;
            while (!isAbort()) {
                br.getPage("https://api.500px.com/v1/users/" + userID + "/galleries/" + galleryID + "/items?rpp=" + itemsPerPage + "&image_size[]=1&image_size[]=2&image_size[]=32&image_size[]=31&image_size[]=33&image_size[]=34&image_size[]=35&image_size[]=36&image_size[]=2048&image_size[]=4&image_size[]=14&include_licensing=true&formats=jpeg,lytro&sort=position&sort_direction=asc&page=" + page + "&rpp=50");
                checkErrorsAPI(br);
                final Map<String, Object> map = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Map<String, Object>> photos = (List<Map<String, Object>>) map.get("photos");
                if (photos == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final Map<String, Object> photo : photos) {
                    final DownloadLink downloadLink = parsePhoto(photo, userID, userName);
                    fp.add(downloadLink);
                    distribute(downloadLink);
                    ret.add(downloadLink);
                }
                final Number total_pages = (Number) map.get("total_pages");
                logger.info("Crawled page " + page + "/" + total_pages + " | Found items so far: " + ret.size());
                if (page >= total_pages.intValue()) {
                    logger.info("Stopping because: Reached end");
                    break;
                } else {
                    page++;
                    continue;
                }
            }
            if (ret.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
            }
        } else if (isAllGalleriesOfAUser) {
            /* Crawl all galleries of a user */
            int page = 1;
            while (!isAbort()) {
                br.getPage("https://api.500px.com/v1/users/" + userID + "/galleries?rpp=" + itemsPerPage + "&page=" + page++);
                checkErrorsAPI(br);
                final Map<String, Object> map = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Map<String, Object>> galleries = (List<Map<String, Object>>) map.get("galleries");
                if (galleries == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final Map<String, Object> gallerymap : galleries) {
                    /*
                     * Do not set any FilePackage here as each gallery will go through this crawler again and go into its own package in the
                     * end.
                     */
                    final DownloadLink gallery = this.createDownloadlink("https://" + this.getHost() + "/p/" + userNameFromURL + "/galleries/" + gallerymap.get("custom_path"));
                    distribute(gallery);
                    ret.add(gallery);
                }
                final Number total_pages = (Number) map.get("total_pages");
                logger.info("Crawled page " + page + "/" + total_pages + " | Found items so far: " + ret.size());
                if (page >= total_pages.intValue()) {
                    logger.info("Stopping because: Reached end");
                    break;
                }
            }
            if (ret.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE);
            }
        } else {
            /* Crawl all images of a user */
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(userNameFromURL);
            fp.setPackageKey("500px://user/" + userID);
            br.setAllowedResponseCodes(new int[] { 500 });
            int page = 0;
            while (!isAbort()) {
                logger.info("Crawling page: " + page);
                br.getPage("https://api.500px.com/v1/photos?feature=user&stream=photos&username=" + userNameFromURL + "&include_states=true&image_size[]=1&image_size[]=2&image_size[]=32&image_size[]=31&image_size[]=33&image_size[]=34&image_size[]=35&image_size[]=36&image_size[]=2048&image_size[]=4&image_size[]=14&include_licensing=true&page=" + page++ + "&rpp=" + itemsPerPage);
                checkErrorsAPI(br);
                final Map<String, Object> map = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Map<String, Object>> photos = (List<Map<String, Object>>) map.get("photos");
                if (photos == null) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final Map<String, Object> photo : photos) {
                    final DownloadLink downloadLink = parsePhoto(photo, userID, userName);
                    fp.add(downloadLink);
                    distribute(downloadLink);
                    ret.add(downloadLink);
                }
                final Number total_pages = (Number) map.get("total_pages");
                logger.info("Crawled page " + page + "/" + total_pages + " | Found items so far: " + ret.size());
                if (page >= total_pages.intValue()) {
                    logger.info("Stopping because: Reached end");
                    break;
                }
            }
            if (ret.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE);
            }
        }
        return ret;
    }

    private void checkErrorsAPI(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }
}
