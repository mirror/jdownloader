//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.XHamsterCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XHamsterGallery extends PluginForDecrypt {
    public XHamsterGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    /** Make sure this is the same in classes XHamsterCom and XHamsterGallery! */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xhamster.com", "xhamster.xxx", "xhamster.desi", "xhamster.one", "xhamster1.desi", "xhamster2.desi", "xhamster3.desi", "openxh.com", "openxh1.com", "openxh2.com", "megaxh.com" });
        return ret;
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
            final StringBuilder sb = new StringBuilder();
            sb.append("https?://(?:[a-z0-9\\-]+\\.)?" + buildHostsPatternPart(domains));
            sb.append("/(");
            sb.append("photos/gallery/[0-9A-Za-z_\\-/]+-\\d+");
            sb.append("|my/favorites/videos(?:/[a-f0-9]{24}-[\\w\\-]+)?");
            sb.append("|users/[^/]+/videos");
            sb.append("|users/[^/]+/photos");
            sb.append("|channels/[^/]+");
            sb.append("|(?:[^/]+/)?pornstars/[^/]+");
            sb.append("|(?:[^/]+/)?creators/[^/]+");
            sb.append(")");
            ret.add(sb.toString());
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_PHOTO_GALLERY             = "(?i)https?://[^/]+/photos/gallery/[0-9A-Za-z_\\-/]+-(\\d+)";
    private static final String TYPE_FAVORITES_OF_CURRENT_USER = "(?i)https?://[^/]+/my/favorites/videos(/[a-f0-9]{24}-([\\w\\-]+))?";
    private static final String TYPE_VIDEOS_OF_USER            = "(?i)https?://[^/]+/users/([^/]+)/videos";
    private static final String TYPE_PHOTO_GALLERIES_OF_USER   = "(?i)https?://[^/]+/users/([^/]+)/photos";
    private static final String TYPE_VIDEOS_OF_CHANNEL         = "(?i)https?://[^/]+/channels/([^/]+)";
    private static final String TYPE_VIDEOS_OF_USER_PORNSTAR   = "(?i)https?://[^/]+/(?:[^/]+/)?pornstars/([^/]+)";
    private static final String TYPE_VIDEOS_OF_USER_CREATOR    = "(?i)https?://[^/]+/(?:[^/]+/)?creators/([^/]+)";

    public static String buildHostsPatternPart(String[] domains) {
        final StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");
        for (int i = 0; i < domains.length; i++) {
            final String domain = domains[i];
            if (i > 0) {
                pattern.append("|");
            }
            if ("xhamster.com".equals(domain)) {
                /* Special: Allow e.g. xhamster4.com and so on. */
                pattern.append("xhamster\\d*\\.(?:com|xxx|desi|one)");
            } else {
                pattern.append(Pattern.quote(domain));
            }
        }
        pattern.append(")");
        return pattern.toString();
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        param.setCryptedUrl(XHamsterCom.getCorrectedURL(param.getCryptedUrl()));
        br.addAllowedResponseCodes(410);
        br.addAllowedResponseCodes(423);
        br.addAllowedResponseCodes(452);
        XHamsterCom.prepBr(this, br);
        // Login if possible
        final XHamsterCom hostPlugin = (XHamsterCom) this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            hostPlugin.login(account, null, false);
        }
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_VIDEOS_OF_USER)) {
            /* Crawl all videos of a user */
            return crawlUserProfile(param);
        } else if (param.getCryptedUrl().matches(TYPE_VIDEOS_OF_USER_PORNSTAR)) {
            /* Crawl all videos of a pornstar profile */
            return this.crawlUserProfilePornstar(param);
        } else if (param.getCryptedUrl().matches(TYPE_VIDEOS_OF_USER_CREATOR)) {
            /* Crawl all videos of a creator profile */
            return this.crawlUserProfileCreator(param);
        } else if (param.getCryptedUrl().matches(TYPE_VIDEOS_OF_CHANNEL)) {
            /* Crawl all videos of a channel */
            return crawlChannel(param);
        } else if (param.getCryptedUrl().matches(TYPE_FAVORITES_OF_CURRENT_USER)) {
            /* Crawl users own favorites */
            return this.crawlUserFavorites(param, account);
        } else if (param.getCryptedUrl().matches(TYPE_PHOTO_GALLERIES_OF_USER)) {
            /* Crawl all photo galleries of a user --> Goes back into crawler and crawler will crawl the single photos */
            return crawlAllGalleriesOfUser(param);
        } else {
            /* Single Photo gallery */
            return this.crawlPhotoGallery(param);
        }
    }

    private ArrayList<DownloadLink> crawlUserProfile(final CryptedLink param) throws IOException, PluginException, DecrypterRetryException {
        final String username = new Regex(param.getCryptedUrl(), TYPE_VIDEOS_OF_USER).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        final ArrayList<DownloadLink> ret = this.crawlPagination(param, fp);
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE, "EMPTY_PROFILE_" + username);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlUserProfilePornstar(final CryptedLink param) throws IOException, PluginException, DecrypterRetryException {
        final String username = new Regex(param.getCryptedUrl(), TYPE_VIDEOS_OF_USER_PORNSTAR).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        final ArrayList<DownloadLink> ret = this.crawlPagination(param, fp);
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE, "EMPTY_PROFILE_PORNSTAR_" + username);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlUserProfileCreator(final CryptedLink param) throws IOException, PluginException, DecrypterRetryException {
        final String username = new Regex(param.getCryptedUrl(), TYPE_VIDEOS_OF_USER_CREATOR).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        final ArrayList<DownloadLink> ret = this.crawlPagination(param, fp);
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE, "EMPTY_PROFILE_CREATOR_" + username);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlChannel(final CryptedLink param) throws IOException, PluginException, DecrypterRetryException {
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String channelname = new Regex(param.getCryptedUrl(), TYPE_VIDEOS_OF_CHANNEL).getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(channelname);
        final ArrayList<DownloadLink> ret = this.crawlPagination(param, fp);
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE, "EMPTY_CHANNEL_" + channelname);
        }
        return ret;
    }

    /* Users can create custom favorites collections with custom names. This function can crawl them. */
    private ArrayList<DownloadLink> crawlUserFavorites(final CryptedLink param, final Account account) throws IOException, PluginException, DecrypterRetryException {
        if (account == null) {
            throw new AccountRequiredException();
        }
        final String favoritesName = new Regex(param.getCryptedUrl(), TYPE_FAVORITES_OF_CURRENT_USER).getMatch(1);
        if (favoritesName == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(param.getCryptedUrl());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("Favorites - " + favoritesName);
        final ArrayList<DownloadLink> ret = this.crawlPagination(param, fp);
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        return ret;
    }

    /* Crawls all videos of all pages in given browsers' html. */
    private ArrayList<DownloadLink> crawlPagination(final CryptedLink param, final FilePackage fp) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final HashSet<String> dupes = new HashSet<String>();
        int page = 1;
        String nextpageurl = null;
        int maxPage = -1;
        final String[] pageNums = br.getRegex("data-page=\"(\\d+)\"").getColumn(0);
        if (pageNums != null && pageNums.length > 0) {
            for (final String pageNumStr : pageNums) {
                final int pageNumTmp = Integer.parseInt(pageNumStr);
                if (pageNumTmp > maxPage) {
                    maxPage = pageNumTmp;
                }
            }
        }
        do {
            if (nextpageurl != null) {
                br.getPage(nextpageurl);
            }
            final String[] urlParts = br.getRegex("/videos/([^<>\"']+)").getColumn(0);
            if (urlParts == null || urlParts.length == 0) {
                logger.info("Stopping because: Failed to find any items on current page");
                break;
            }
            int numberofNewItems = 0;
            for (String urlPart : urlParts) {
                if (!dupes.add(urlPart)) {
                    /* Skip dupes */
                    continue;
                }
                numberofNewItems++;
                final DownloadLink dl = this.createDownloadlink(br.getURL("/videos/" + urlPart).toString());
                /* Set temp. name -> Will change once user starts downloading. */
                dl.setName(urlPart.replace("-", " ") + ".mp4");
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
            }
            logger.info("Crawled page: " + page + "/" + maxPage + " | Found items on this page: " + numberofNewItems + " | Total: " + ret.size());
            page++;
            nextpageurl = br.getRegex("class=\"xh-paginator-button[^\"]*\"[^>]*href=\"(https?://[^<>\"]+/" + page + ")\" data-page=\"" + page + "\">").getMatch(0);
            if (nextpageurl == null) {
                final String maybeNextPage = br.getURL().replaceFirst("/\\d*$", "") + "/" + page;
                if (br.containsHTML(Pattern.quote(maybeNextPage))) {
                    logger.info("Using slightly corrected nextpageurl: " + maybeNextPage);
                    nextpageurl = maybeNextPage;
                }
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (nextpageurl == null) {
                logger.info("Stopping because: Failed to find nextpage");
                break;
            } else if (numberofNewItems == 0) {
                logger.info("Stopping because: Failed to find any new items on page: " + page);
                break;
            } else {
                /* Continue with next page */
            }
        } while (!this.isAbort());
        return ret;
    }

    private ArrayList<DownloadLink> crawlAllGalleriesOfUser(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        final String username = new Regex(param.getCryptedUrl(), TYPE_PHOTO_GALLERIES_OF_USER).getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        int page = 1;
        do {
            logger.info("Crawling page: " + page);
            final String[] urls = br.getRegex("(/photos/gallery/[a-z0-9\\-]+-\\d+)").getColumn(0);
            for (String url : urls) {
                url = br.getURL(url).toString();
                ret.add(this.createDownloadlink(url));
            }
            page++;
            final String nextpageURL = br.getRegex("(/users/" + username + "/photos/" + page + ")").getMatch(0);
            if (nextpageURL != null) {
                logger.info("Nextpage available: " + nextpageURL);
                br.getPage(nextpageURL);
            } else {
                logger.info("No nextpage available");
                break;
            }
        } while (!this.isAbort());
        return ret;
    }

    private ArrayList<DownloadLink> crawlPhotoGallery(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("(?i)Sorry, no photos found|error\">\\s*Gallery not found\\s*<|>\\s*Page Not Found\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*This gallery is visible for")) {
            throw new AccountRequiredException();
        }
        if (br.containsHTML("(?i)>\\s*This gallery (needs|requires) password\\s*<")) {
            final boolean passwordHandlingBroken = true;
            if (passwordHandlingBroken) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password-protected handling broken svn.jdownloader.org/issues/88690");
            }
            boolean failed = true;
            for (int i = 1; i <= 3; i++) {
                String passCode = getUserInput("Password?", param);
                br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(">This gallery needs password<")) {
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        if (new Regex(br.getURL(), "/gallery/[0-9]+/[0-9]+").matches()) { // Single picture
            DownloadLink dl = createDownloadlink("directhttp://" + br.getRegex("class='slideImg'\\s+src='([^']+)").getMatch(0));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // final String total_numberof_picsStr = br.getRegex("<h1 class=\"gr\">[^<>]+<small>\\[(\\d+) [^<>\"]+\\]</small>").getMatch(0);
        final String total_numberof_picsStr = br.getRegex("page-title__count\">(\\d+)<").getMatch(0);
        logger.info("total_numberof_pics: " + total_numberof_picsStr);
        final int total_numberof_picsInt = total_numberof_picsStr != null ? Integer.parseInt(total_numberof_picsStr) : -1;
        final String galleryID = new Regex(param.getCryptedUrl(), TYPE_PHOTO_GALLERY).getMatch(0);
        String fpname = br.getRegex("<title>\\s*(.*?)\\s*\\-\\s*\\d+\\s*(Pics|Bilder)\\s*(?:\\-|\\|)\\s*xHamster(\\.com|\\.xxx|\\.desi|\\.one)?\\s*</title>").getMatch(0);
        if (fpname == null) {
            fpname = br.getRegex("<title>(.*?)\\s*>\\s*").getMatch(0);
        }
        /*
         * 2020-05-12: They often have different galleries with the exact same title --> Include galleryID so we do not get multiple
         * packages with the same title --> Then gets auto merged by default
         */
        if (fpname != null && !fpname.contains(galleryID)) {
            fpname += "_" + galleryID;
        } else if (fpname == null) {
            /* Final fallback */
            fpname = galleryID;
        }
        /* Add name of uploader to the beginning of our packagename if possible */
        final String uploaderName = br.getRegex("/users/[^\"]+\"[^>]*class=\"link\">([^<>\"]+)<").getMatch(0);
        if (uploaderName != null && !fpname.contains(uploaderName)) {
            fpname = uploaderName + " - " + fpname;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpname.trim()));
        int pageIndex = 1;
        int imageIndex = 1;
        Boolean next = true;
        while (next) {
            String allLinks = br.getRegex("class='iListing'>(.*?)id='galleryInfoBox'>").getMatch(0);
            if (allLinks == null) {
                allLinks = br.getRegex("id='imgSized'(.*?)gid='\\d+").getMatch(0);
            }
            logger.info("Crawling page " + pageIndex);
            final String json_source = br.getRegex("\"photos\":(\\[\\{.*?\\}\\])").getMatch(0);
            // logger.info("json_source: " + json_source);
            if (json_source != null) {
                final List<Object> lines = (List) JavaScriptEngineFactory.jsonToJavaObject(json_source);
                for (final Object line : lines) {
                    // logger.info("line: " + line);
                    if (line instanceof Map) {
                        final Map<String, Object> entries = (Map<String, Object>) line;
                        final String imageURL = (String) entries.get("imageURL");
                        if (imageURL != null) {
                            // logger.info("imageURL: " + imageURL);
                            final DownloadLink dl = createDownloadlink(imageURL);
                            final String extension = getFileNameExtensionFromString(imageURL, ".jpg");
                            if (total_numberof_picsStr != null) {
                                dl.setFinalFileName(StringUtils.fillPre(Integer.toString(imageIndex), "0", total_numberof_picsStr.length()) + "_" + total_numberof_picsStr + extension);
                            } else {
                                dl.setFinalFileName(Integer.toString(imageIndex) + extension);
                            }
                            imageIndex++;
                            dl.setAvailable(true);
                            dl._setFilePackage(fp);
                            distribute(dl);
                            decryptedLinks.add(dl);
                        }
                    }
                }
            }
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
            String nextPage = br.getRegex("data-page=\"next\" href=\"([^<>\"]*)\"").getMatch(0);
            if (!StringUtils.isEmpty(nextPage) && nextPage != null) {
                logger.info("Getting page " + nextPage);
                br.getPage(nextPage);
                if (br.getHttpConnection().getResponseCode() == 452 || br.containsHTML("(?i)>\\s*Page Not Found\\s*<")) {
                    break;
                }
            } else {
                next = false;
            }
            pageIndex++;
        }
        if (total_numberof_picsInt != -1 && decryptedLinks.size() < total_numberof_picsInt) {
            logger.warning("Seems like not all images have been found");
        }
        return decryptedLinks;
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}