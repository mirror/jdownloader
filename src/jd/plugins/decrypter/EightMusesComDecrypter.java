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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "8muses.com" }, urls = { "https?://(?:www\\.|comics\\.)?8muses\\.com/((?:comix/|comics/)?(?:index/category/[a-z0-9\\-_]+|album(?:/[a-z0-9\\-_]+){1,6})|forum/(?!.*attachments/).+)" })
public class EightMusesComDecrypter extends antiDDoSForDecrypt {
    public EightMusesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName;
        final FilePackage fp = FilePackage.getInstance();
        if (param.getCryptedUrl().matches("https?://[^/]+/forum/.+")) {
            fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (fpName == null) {
                fpName = param.getCryptedUrl().substring(param.getCryptedUrl().lastIndexOf("/") + 1);
            }
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            final String urlSlug = new Regex(param.getCryptedUrl(), "https?://[^/]+/forum/[^/]+/\\d+/([a-z0-9\\-]+).*").getMatch(0);
            /* TODO: Maybe add multi page parser */
            final String[] pagesStr = br.getRegex(urlSlug + "/page-(\\d+)").getColumn(0);
            int maxPage = -1;
            for (final String pageStr : pagesStr) {
                final int pageTmp = Integer.parseInt(pageStr);
                if (pageTmp > maxPage) {
                    maxPage = pageTmp;
                }
            }
            if (maxPage > 0) {
                logger.info("Thread has " + maxPage + " pages");
            } else {
                logger.info("Thread has 1 page");
            }
            final String pageInAddedURL = new Regex(param.getCryptedUrl(), "page-(\\d+)").getMatch(0);
            if (pageInAddedURL != null) {
                logger.info("Crawling page: " + pageInAddedURL);
            } else {
                logger.info("Crawling page 1");
            }
            /* TODO: Remove duplicated code below */
            /* Grab forum attachments */
            final String[] attachmentURLs = br.getRegex("(/forum/attachments/[^\"]+)\"").getColumn(0);
            if (attachmentURLs.length == 0) {
                logger.info("This page inside this thread does not seem to have any attachments available");
                return decryptedLinks;
            }
            final HashSet<String> dupes = new HashSet<String>();
            for (final String attachmentURL : attachmentURLs) {
                if (!dupes.add(attachmentURL)) {
                    /* Skip dupes */
                    continue;
                }
                final String forumURL = "https://" + br.getHost() + attachmentURL;
                final DownloadLink dl = this.createDownloadlink(forumURL);
                final String url_name = jd.plugins.hoster.EightMusesCom.getURLNameForum(forumURL);
                if (url_name != null) {
                    dl.setName(url_name);
                } else {
                    logger.warning("WTF");
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            /* Grab other URLs/thumbnails/pictures/preview images */
            final String[] urls = br.getRegex("<a href=\"(https[^<>\"]+)\" target=\"_blank\"").getColumn(0);
            for (final String url : urls) {
                if (new Regex(url, this.getSupportedLinks()).matches()) {
                    /* Skip URLs that would go into this crawler again */
                    continue;
                } else if (!dupes.add(url)) {
                    /* Skip dupes */
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(url);
                if (url.matches(".*/forum/attachments/[^\"]+")) {
                    final String url_name = jd.plugins.hoster.EightMusesCom.getURLNameForum(url);
                    if (url_name != null) {
                        dl.setName(url_name);
                    }
                }
                /* Assume that those images are online. */
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
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
}
