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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XHamsterGallery extends PluginForDecrypt {
    public XHamsterGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Make sure this is the same in classes XHamsterCom and XHamsterGallery! */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xhamster.com", "xhamster.xxx", "xhamster.desi", "xhamster.one", "xhamster1.desi", "xhamster2.desi", "xhamster3.desi" });
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
            sb.append("|");
            sb.append("users/[^/]+/videos");
            sb.append("|");
            sb.append("users/[^/]+/photos");
            sb.append(")");
            ret.add(sb.toString());
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_PHOTO_GALLERY           = "https?://[^/]+/photos/gallery/[0-9A-Za-z_\\-/]+-(\\d+)";
    private static final String TYPE_VIDEOS_OF_USER          = "https?://[^/]+/users/([^/]+)/videos";
    private static final String TYPE_PHOTO_GALLERIES_OF_USER = "https?://[^/]+/users/([^/]+)/photos";

    public static String buildHostsPatternPart(String[] domains) {
        final StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");
        for (int i = 0; i < domains.length; i++) {
            final String domain = domains[i];
            if (i > 0) {
                pattern.append("|");
            }
            if ("xhamster.com".equals(domain)) {
                pattern.append("xhamster\\d*\\.(?:com|xxx|desi|one)");
            } else {
                pattern.append(Pattern.quote(domain));
            }
        }
        pattern.append(")");
        return pattern.toString();
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("www.", "");
        /* Force english language */
        final String replace_string = new Regex(parameter, "(https?://(?:www\\.)?[^/]+/)").getMatch(0);
        parameter = parameter.replace(replace_string, "https://xhamster.com/");
        br.addAllowedResponseCodes(410);
        br.addAllowedResponseCodes(423);
        br.addAllowedResponseCodes(452);
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        // Login if possible
        getUserLogin(false);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (parameter.matches(TYPE_VIDEOS_OF_USER)) {
            /* Crawl all videos of a user */
            final String username = new Regex(parameter, TYPE_VIDEOS_OF_USER).getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(username);
            int page = 1;
            do {
                logger.info("Crawling page: " + page);
                final String[] urls = br.getRegex("(/videos/[a-z0-9\\-]+-\\d+)").getColumn(0);
                for (String url : urls) {
                    url = br.getURL(url).toString();
                    final String url_title = new Regex(url, "/videos/(.+)-\\d+$").getMatch(0);
                    final DownloadLink dl = this.createDownloadlink(url);
                    dl.setName(username + "_" + url_title.replace("-", " ") + ".mp4");
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                }
                page++;
                final String nextpage = br.getRegex("(/users/" + username + "/videos/" + page + ")").getMatch(0);
                if (nextpage != null) {
                    logger.info("Nextpage available: " + nextpage);
                    br.getPage(nextpage);
                } else {
                    logger.info("No nextpage available");
                    break;
                }
            } while (!this.isAbort());
        } else if (parameter.matches(TYPE_PHOTO_GALLERIES_OF_USER)) {
            /* Crawl all photo galleries of a user --> Goes back into crawler and crawler will crawl the single photos */
            final String username = new Regex(parameter, TYPE_PHOTO_GALLERIES_OF_USER).getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(username);
            int page = 1;
            do {
                logger.info("Crawling page: " + page);
                final String[] urls = br.getRegex("(/photos/gallery/[a-z0-9\\-]+-\\d+)").getColumn(0);
                for (String url : urls) {
                    url = br.getURL(url).toString();
                    decryptedLinks.add(this.createDownloadlink(url));
                }
                page++;
                final String nextpage = br.getRegex("(/users/" + username + "/photos/" + page + ")").getMatch(0);
                if (nextpage != null) {
                    logger.info("Nextpage available: " + nextpage);
                    br.getPage(nextpage);
                } else {
                    logger.info("No nextpage available");
                    break;
                }
            } while (!this.isAbort());
        } else {
            /* Single Photo gallery */
            /* Error handling */
            if (br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Sorry, no photos found|error\">Gallery not found<|>Page Not Found<")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (br.containsHTML(">This gallery is visible for")) {
                logger.info("This gallery is only visible for specified users, account needed: " + parameter);
                decryptedLinks.add(createOfflinelink(parameter, "Private gallery"));
                return decryptedLinks;
            }
            if (br.containsHTML(">This gallery (needs|requires) password<")) {
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
            final String urlWithoutPageParameter = br.getURL();
            // final String total_numberof_picsStr = br.getRegex("<h1 class=\"gr\">[^<>]+<small>\\[(\\d+) [^<>\"]+\\]</small>").getMatch(0);
            final String total_numberof_picsStr = br.getRegex("page-title__count\">(\\d+)<").getMatch(0);
            logger.info("total_numberof_pics: " + total_numberof_picsStr);
            final int total_numberof_picsInt = total_numberof_picsStr != null ? Integer.parseInt(total_numberof_picsStr) : -1;
            final String galleryID = new Regex(parameter, TYPE_PHOTO_GALLERY).getMatch(0);
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
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    break;
                }
                String allLinks = br.getRegex("class='iListing'>(.*?)id='galleryInfoBox'>").getMatch(0);
                if (allLinks == null) {
                    allLinks = br.getRegex("id='imgSized'(.*?)gid='\\d+").getMatch(0);
                }
                logger.info("Crawling page " + pageIndex);
                final String json_source = br.getRegex("\"photos\":(\\[\\{.*?\\}\\])").getMatch(0);
                // logger.info("json_source: " + json_source);
                if (json_source != null) {
                    final ArrayList<Object> lines = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json_source);
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
                String nextPage = br.getRegex("data-page=\"next\" href=\"([^<>\"]*)\"").getMatch(0);
                if (!StringUtils.isEmpty(nextPage) && nextPage != null) {
                    logger.info("Getting page " + nextPage);
                    // br.getPage(urlWithoutPageParameter + "/" + pageIndex);
                    br.getPage(nextPage);
                    if (br.getHttpConnection().getResponseCode() == 452 || br.containsHTML(">Page Not Found<")) {
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
        }
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("xhamster.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.XHamsterCom) hostPlugin).setBrowser(br);
            ((jd.plugins.hoster.XHamsterCom) hostPlugin).login(aa, force);
        } catch (final PluginException e) {
            handleAccountException(aa, e);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}