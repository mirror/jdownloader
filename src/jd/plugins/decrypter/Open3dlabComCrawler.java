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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.Open3dlabComConfig;
import org.jdownloader.plugins.components.config.Open3dlabComConfig.MirrorFallbackMode;
import org.jdownloader.plugins.components.config.Open3dlabComConfigSmutbaSe;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
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
import jd.plugins.hoster.Open3dlabCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { Open3dlabCom.class })
public class Open3dlabComCrawler extends PluginForDecrypt {
    public Open3dlabComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return Open3dlabCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(user/\\d+/?|project/[a-f0-9\\-]+/?)");
        }
        return ret.toArray(new String[0]);
    }

    private final String PATTERN_PROJECT = "(?i)https?://[^/]+/project/([a-f0-9\\-]+)/?";
    private final String PATTERN_PROFILE = "(?i)https?://[^/]+/user/(\\d+)/?";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Regex project;
        final Regex profile;
        if ((project = new Regex(param.getCryptedUrl(), PATTERN_PROJECT)).patternFind()) {
            return crawlProject(param);
        } else if ((profile = new Regex(param.getCryptedUrl(), PATTERN_PROFILE)).patternFind()) {
            return this.crawlProfile(param);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public ArrayList<DownloadLink> crawlProject(final CryptedLink param) throws Exception {
        final String projectIDFromSourceurl = new Regex(param.getCryptedUrl(), PATTERN_PROJECT).getMatch(0);
        if (projectIDFromSourceurl == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String projectID;
        /*
         * 2024-04-05: ProjectIDs have changed from only numbers to alphanumeric so let's try to extract the "real" projectID from current
         * browser-URL.
         */
        final String projectIDFromBrowserURL = new Regex(br.getURL(), PATTERN_PROJECT).getMatch(0);
        if (projectIDFromBrowserURL != null) {
            projectID = projectIDFromBrowserURL;
        } else {
            projectID = projectIDFromSourceurl;
        }
        final String[] dlHTMLs = br.getRegex("<td class=\"text-wrap-word js-edit-input\"(.*?)</div>\\s*</td>\\s*</tr>").getColumn(0);
        if (dlHTMLs == null || dlHTMLs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String title = br.getRegex("\"name\"\\s*:\\s*\"([^\"]+)\"").getMatch(0);
        if (title == null) {
            title = br.getRegex("<meta name=\"twitter:title\" content=\"([^\"]+)").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        final Open3dlabComConfig cfg;
        if (this.getHost().equals("open3dlab.com")) {
            cfg = PluginJsonConfig.get(Open3dlabComConfig.class);
        } else {
            cfg = PluginJsonConfig.get(Open3dlabComConfigSmutbaSe.class);
        }
        String[] mirrorPrioList = null;
        String userHosterMirrorListStr = cfg.getMirrorPriorityString();
        if (userHosterMirrorListStr != null) {
            userHosterMirrorListStr = userHosterMirrorListStr.replace(" ", "").toLowerCase(Locale.ENGLISH);
            mirrorPrioList = userHosterMirrorListStr.split(",");
        }
        final MirrorFallbackMode fallbackMode = cfg.getMirrorFallbackMode();
        for (final String dlHTML : dlHTMLs) {
            /* Find all mirrors */
            String filename = new Regex(dlHTML, "span class=\"js-edit-input__wrapper\"><strong>([^<]+)</strong>").getMatch(0);
            if (filename == null) {
                filename = new Regex(dlHTML, "(?i)You are about to download \"([^\"]+)").getMatch(0);
            }
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
            }
            String filesizeStr = new Regex(dlHTML, "<td>(\\d+[^<]+)</td>\\s*</tr>").getMatch(0);
            if (filesizeStr == null) {
                filesizeStr = new Regex(dlHTML, ">\\s*([0-9\\.]+\\s*(TB|GB|MB|KB))\\s*<").getMatch(0);
            }
            long filesize = -1;
            if (filesizeStr != null) {
                filesize = SizeFormatter.getSize(filesizeStr);
            }
            /* Collect all mirror links. */
            final String[] urls = HTMLParser.getHttpLinks(dlHTML, br.getURL());
            final HashSet<String> mirrorURLs = new HashSet<String>();
            for (final String url : urls) {
                if (url.matches("https?://[^/]+" + Open3dlabCom.pattern_supported_links_path_relative)) {
                    mirrorURLs.add(url);
                }
            }
            if (mirrorURLs.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final HashMap<String, DownloadLink> mirrorMap = new HashMap<String, DownloadLink>();
            for (final String mirrorURL : mirrorURLs) {
                final DownloadLink dl = this.createDownloadlink(mirrorURL);
                if (filename != null) {
                    dl.setName(filename);
                }
                if (filesize != -1) {
                    dl.setDownloadSize(filesize);
                }
                final String mirrorStr = new Regex(mirrorURL, "/\\d+/([^/]+)/?$").getMatch(0);
                if (mirrorStr != null) {
                    mirrorMap.put(mirrorStr, dl);
                }
            }
            DownloadLink preferredMirror = null;
            if (mirrorPrioList != null && mirrorPrioList.length > 0) {
                for (final String mirrorStr : mirrorPrioList) {
                    preferredMirror = mirrorMap.get(mirrorStr);
                    if (preferredMirror != null) {
                        break;
                    }
                }
            }
            if (preferredMirror != null) {
                /* User prefers single mirror and that desired mirror has been found. */
                ret.add(preferredMirror);
            } else if (fallbackMode == MirrorFallbackMode.ONE) {
                logger.info("Failed to find desired mirror: Returning random mirror as fallback");
                final List<DownloadLink> mirrors = new ArrayList<DownloadLink>(mirrorMap.values());
                ret.add(mirrors.get(new Random().nextInt(mirrors.size())));
            } else {
                logger.info("Failed to find desired mirror: Returning all mirrors as fallback");
                ret.addAll(mirrorMap.values());
            }
        }
        final String thumbnailURLRightSide = br.getRegex("\"image\"\\s*:\\s*\"(https?://[^\"]+)\"").getMatch(0);
        if (thumbnailURLRightSide != null && cfg.isCrawlThumbnail()) {
            final DownloadLink thumb = this.createDownloadlink(thumbnailURLRightSide);
            ret.add(thumb);
        }
        final String[] userUploadedPreviewMediaURLs = br.getRegex("\"(https?://[^/]+/content/content/[^/]+/user_uploads/[^\"]+)").getColumn(0);
        if (userUploadedPreviewMediaURLs != null && userUploadedPreviewMediaURLs.length > 0 && cfg.isCrawlPreviewSlashPromoMedia()) {
            for (final String userUploadedPreviewMediaURL : userUploadedPreviewMediaURLs) {
                final DownloadLink userUpload = this.createDownloadlink(userUploadedPreviewMediaURL);
                ret.add(userUpload);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(title);
        } else {
            /* Fallback */
            fp.setName(projectID);
        }
        fp.setCleanupPackageName(false);
        fp.setPackageKey(this.getHost() + "://project/" + projectID);
        for (final DownloadLink result : ret) {
            result.setAvailable(true);
            result._setFilePackage(fp);
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlProfile(final CryptedLink param) throws Exception {
        final String profileID = new Regex(param.getCryptedUrl(), PATTERN_PROFILE).getMatch(0);
        if (profileID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String profilename = br.getRegex("profilename=\"([^\"]+)").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (profilename != null) {
            fp.setName(Encoding.htmlDecode(profilename).trim());
        } else {
            fp.setName(profileID);
        }
        fp.setCleanupPackageName(false);
        int page = 1;
        do {
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            final HashSet<String> dupes = new HashSet<String>();
            int numberofNewItems = 0;
            for (final String url : urls) {
                if (url.matches(PATTERN_PROJECT) && dupes.add(url)) {
                    numberofNewItems++;
                    final DownloadLink result = this.createDownloadlink(url);
                    result._setFilePackage(fp);
                    ret.add(result);
                    distribute(result);
                }
            }
            logger.info("Crawled page " + page + " | New items on this page: " + numberofNewItems + " | Total number of items so far: " + ret.size());
            page++;
            final String nextPageURL = br.getRegex("(/user/" + profileID + "/\\?page=" + page + ")").getMatch(0);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (numberofNewItems == 0) {
                logger.info("Stopping becaus: Failed to find any new items on current page");
                break;
            } else if (nextPageURL == null) {
                logger.info("Stopping because: Failed to find next page -> Reached end?");
                break;
            } else {
                /* Continue to next page */
                br.getPage(nextPageURL);
                continue;
            }
        } while (true);
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE, "EMPTY_PROFILE_" + profileID);
        }
        return ret;
    }
}
