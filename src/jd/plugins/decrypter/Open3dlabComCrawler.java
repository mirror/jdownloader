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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.Open3dlabComConfig;
import org.jdownloader.plugins.components.config.Open3dlabComConfig.MirrorFallbackMode;
import org.jdownloader.plugins.components.config.Open3dlabComConfigSmutbaSe;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { Open3dlabCom.class })
public class Open3dlabComCrawler extends PluginForDecrypt {
    public Open3dlabComCrawler(PluginWrapper wrapper) {
        super(wrapper);
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:project|user)/\\d+/?");
        }
        return ret.toArray(new String[0]);
    }

    private final String PATTERN_PROJECT = "https?://[^/]+/project/(\\d+)/?";
    private final String PATTERN_PROFILE = "https?://[^/]+/user/(\\d+)/?";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(PATTERN_PROJECT)) {
            return crawlProject(param);
        } else {
            return this.crawlProfile(param);
        }
    }

    public ArrayList<DownloadLink> crawlProject(final CryptedLink param) throws Exception {
        final String projectID = new Regex(param.getCryptedUrl(), PATTERN_PROJECT).getMatch(0);
        if (projectID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Open3dlabCom hosterplugin = (Open3dlabCom) this.getNewPluginForHostInstance(this.getHost());
        String title = br.getRegex("\"name\"\\s*:\\s*\"([^\"]+)\"").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
        } else {
            fp.setName(projectID);
        }
        fp.setCleanupPackageName(false);
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
        final String[] dlHTMLs = br.getRegex("<td class=\"text-wrap-word js-edit-input\"(.*?)</div>\\s*</div>\\s*</td>\\s*</tr>").getColumn(0);
        if (dlHTMLs == null || dlHTMLs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String dlHTML : dlHTMLs) {
            /* Find all mirrors */
            String filename = new Regex(dlHTML, "span class=\"js-edit-input__wrapper\"><strong>([^<]+)</strong>").getMatch(0);
            if (filename == null) {
                filename = new Regex(dlHTML, "(?i)You are about to download \"([^\"]+)").getMatch(0);
            }
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
            }
            final String filesizeStr = new Regex(dlHTML, "<td>(\\d+[^<]+)</td>\\s*</tr>").getMatch(0);
            long filesize = -1;
            if (filesizeStr != null) {
                filesize = SizeFormatter.getSize(filesizeStr);
            }
            final String[] urls = HTMLParser.getHttpLinks(dlHTML, br.getURL());
            final HashSet<String> mirrorURLs = new HashSet<String>();
            for (final String url : urls) {
                if (hosterplugin.canHandle(url)) {
                    mirrorURLs.add(url);
                }
            }
            if (mirrorURLs.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final HashMap<String, DownloadLink> mirrorMap = new HashMap<String, DownloadLink>();
            for (final String mirrorURL : mirrorURLs) {
                final DownloadLink dl = this.createDownloadlink(mirrorURL);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
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
        return ret;
    }

    public ArrayList<DownloadLink> crawlProfile(final CryptedLink param) throws Exception {
        final String profileID = new Regex(param.getCryptedUrl(), PATTERN_PROFILE).getMatch(0);
        if (profileID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
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
                if (url.matches(PATTERN_PROJECT)) {
                    if (dupes.add(url)) {
                        numberofNewItems++;
                        final DownloadLink result = this.createDownloadlink(url);
                        result._setFilePackage(fp);
                        ret.add(result);
                        distribute(result);
                    }
                }
            }
            logger.info("Crawled page " + page + " | Total number of items so far: " + ret.size());
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
