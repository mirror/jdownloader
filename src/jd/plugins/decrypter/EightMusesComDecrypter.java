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
import java.util.HashSet;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.EightMusesComConfig;
import org.jdownloader.plugins.components.config.EightMusesComConfig.CrawlMode;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "8muses.com" }, urls = { "https?://(?:www\\.|comics\\.)?8muses\\.com/((?:comix/|comics/)?(?:index/category/[a-z0-9\\-_]+|album(?:/[a-z0-9\\-_]+){1,6})|forum/[a-z0-9\\-]+/\\d+/[a-z0-9\\-]+/(page-\\d+)?)" })
public class EightMusesComDecrypter extends antiDDoSForDecrypt {
    public EightMusesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_FORUM = "https?://[^/]+/forum/[a-z0-9\\-]+/(\\d+)/([a-z0-9\\-]+)/(page-(\\d+))?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName;
        final FilePackage fp = FilePackage.getInstance();
        if (param.getCryptedUrl().matches(TYPE_FORUM)) {
            final CrawlMode mode = PluginJsonConfig.get(getConfigInterface()).getCrawlMode();
            final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_FORUM);
            final String urlSlug = urlinfo.getMatch(1);
            fpName = br.getRegex("property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
            if (fpName == null) {
                fpName = urlSlug.replace("-", " ");
            }
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            /* TODO: Maybe add multi page parser */
            final String[] pagesStr = br.getRegex(urlSlug + "/page-(\\d+)").getColumn(0);
            int maxPage = 1;
            for (final String pageStr : pagesStr) {
                final int pageTmp = Integer.parseInt(pageStr);
                if (pageTmp > maxPage) {
                    maxPage = pageTmp;
                }
            }
            if (maxPage > 1) {
                logger.info("Thread has " + maxPage + " pages");
            } else {
                logger.info("Thread has 1 page");
            }
            /* Grab this from the real current URL as added URL could e.g. contain "page-32" while last page is lower. */
            final int pageStart;
            final int pageMaxDesired;
            final String pageInURL = new Regex(br.getURL(), TYPE_FORUM).getMatch(3);
            if (pageInURL != null) {
                logger.info("Crawl starts from page " + pageInURL);
                pageStart = Integer.parseInt(pageInURL);
            } else {
                logger.info("Crawl starts from first page");
                pageStart = 1;
            }
            if (mode == CrawlMode.SINGLE_PAGE) {
                pageMaxDesired = pageStart;
            } else {
                pageMaxDesired = maxPage;
            }
            logger.info("Crawl ends on page: " + pageMaxDesired);
            final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
            final HashSet<String> dupes = new HashSet<String>();
            int loopNumber = 0;
            int currentPage = pageStart;
            do {
                /* Grab other URLs/thumbnails/pictures/preview images */
                int numberofActuallyAddedItemsThisLoop = 0;
                final String[] urls = br.getRegex("<a [^>]*href=\"((https?://|/)[^<>\"]+)\" target=\"_blank\"").getColumn(0);
                for (String url : urls) {
                    /* Convert relative URLs --> Absolute URLs */
                    url = br.getURL(url).toString();
                    if (this.canHandle(url)) {
                        /* Skip URLs that would go into this crawler again */
                        continue;
                    } else if (!dupes.add(url)) {
                        /* Skip dupes */
                        continue;
                    }
                    final DownloadLink dl = this.createDownloadlink(url);
                    if (plg.canHandle(url)) {
                        if (url.matches(".*/forum/attachments/[^\"]+")) {
                            final String url_name = jd.plugins.hoster.EightMusesCom.getURLNameForum(url);
                            if (url_name != null) {
                                dl.setName(url_name);
                            }
                        }
                        /* Assume that those images are online. */
                        dl.setAvailable(true);
                    }
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    numberofActuallyAddedItemsThisLoop += 1;
                }
                logger.info("Loop: " + loopNumber + " | Page: " + currentPage + "/" + pageMaxDesired + " | Real max. page: " + maxPage + " | Number of items found on this page: " + numberofActuallyAddedItemsThisLoop);
                final String nextPageURL = br.getRegex("(/forum/[^/]+/\\d+/" + urlSlug + "/page-" + (currentPage + 1) + ")").getMatch(0);
                if (this.isAbort()) {
                    break;
                } else if (numberofActuallyAddedItemsThisLoop == 0) {
                    logger.info("Stopping because: Failed to find any items on current page");
                    break;
                } else if (currentPage >= maxPage) {
                    logger.info("Stopping because: Reached last page");
                    break;
                } else if (currentPage >= pageMaxDesired) {
                    logger.info("Stopping because: Reached last DESIRED page");
                    break;
                } else if (nextPageURL == null) {
                    /* This should never happen */
                    logger.warning("Stopping because: Failed to find nextPageURL");
                    break;
                } else {
                    /* Continue to next page */
                    loopNumber += 1;
                    currentPage += 1;
                    br.getPage(nextPageURL);
                }
            } while (true);
            if (decryptedLinks.isEmpty()) {
                logger.info("This thread doesn't contain any downloadable content");
            }
        } else {
            /* Obtain packagename from URL. */
            /* /album/<category>/<author>/<title> */
            final Regex album = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/comics/album/(.+)");
            if (album.matches()) {
                fpName = album.getMatch(0).replace("/", "-");
            } else {
                fpName = param.getCryptedUrl().substring(param.getCryptedUrl().lastIndexOf("/") + 1).replace("-", " ");
            }
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            String[] categories = br.getRegex("(/index/category/[a-z0-9\\-_]+)\" data\\-original\\-title").getColumn(0);
            if (categories == null || categories.length == 0) {
                categories = br.getRegex("(\"|')(/album(?:/[a-z0-9\\-_]+){2,3})\\1").getColumn(1);
            }
            final String[] links = br.getRegex("(/picture/[^<>\"]*?)\"").getColumn(0);
            if ((links == null || links.length == 0) && (categories == null || categories.length == 0)) {
                final String[] issues = br.getRegex("href=\"([^<>\"]+/Issue-\\d+)\">").getColumn(0);
                if (issues != null && issues.length > 0) {
                    for (String issue : issues) {
                        issue = Request.getLocation(issue, br.getRequest());
                        final DownloadLink dl = createDownloadlink(issue);
                        decryptedLinks.add(dl);
                        logger.info("issue: " + issue);
                    }
                    return decryptedLinks;
                }
                logger.info("Unsupported or offline url");
                return decryptedLinks;
            }
            if (links != null && links.length > 0) {
                for (final String singleLink : links) {
                    final DownloadLink dl = createDownloadlink(Request.getLocation(singleLink, br.getRequest()));
                    dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                    dl.setAvailable(true);
                    fp.add(dl);
                    decryptedLinks.add(dl);
                }
            }
            if (categories != null && categories.length > 0) {
                final String[] current = br.getURL().split("/");
                for (final String singleLink : categories) {
                    final String[] corrected = Request.getLocation(singleLink, br.getRequest()).split("/");
                    // since you can pick up cats lower down, we can evaluate based on how many so you don't re-decrypt stuff already
                    // decrypted.
                    if (!StringUtils.endsWithCaseInsensitive(br.getURL(), singleLink) && current.length < corrected.length) {
                        decryptedLinks.add(createDownloadlink(Request.getLocation(singleLink, br.getRequest())));
                    }
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public Class<? extends EightMusesComConfig> getConfigInterface() {
        return EightMusesComConfig.class;
    }
}
