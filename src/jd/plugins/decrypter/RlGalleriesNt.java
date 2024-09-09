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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "urlgalleries.net" }, urls = { "https?://(?:[\\w\\-]+\\.)?urlgalleries\\.net/[\\w\\-]+.+" })
public class RlGalleriesNt extends PluginForDecrypt {
    private static String agent = null;

    public RlGalleriesNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setReadTimeout(3 * 60 * 1000);
        // br.setCookie(".urlgalleries.net", "popundr", "1");
        if (agent == null) {
            agent = UserAgents.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl().replace("http://", "https://");
        String galleryID = new Regex(contenturl, "(?i)https?://[^/]+/[^/]+/(\\d+)").getMatch(0);
        if (galleryID == null) {
            /* For old links */
            galleryID = new Regex(contenturl, "(?i)(?:porn-gallery-|blog_gallery\\.php\\?id=)(\\d+)").getMatch(0);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (galleryID != null) {
            /* Gallery */
            logger.info("Crawling gallery");
            /* 2023-11-09: API does not work anymore */
            final boolean useAPI = false;
            if (useAPI) {
                br.getPage("https://urlgalleries.net/api/v1.php?endpoint=get_gallery&gallery_id=" + galleryID + "&exclude_cat=undefined&_=" + System.currentTimeMillis());
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                final String title = data.get("name").toString();
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                int page = 1;
                String nextpage = null;
                final HashSet<String> dupes = new HashSet<String>();
                do {
                    logger.info("Crawling page " + page + " of ??");
                    final List<Map<String, Object>> thumbs = (List<Map<String, Object>>) data.get("thumbs");
                    final ArrayList<DownloadLink> newitems = new ArrayList<DownloadLink>();
                    for (final Map<String, Object> thumb : thumbs) {
                        final String redirecturl = thumb.get("url").toString();
                        final String thumbnailurl = thumb.get("imgcode").toString();
                        if (dupes.add(redirecturl)) {
                            newitems.add(this.createDownloadlink(redirecturl));
                        }
                        if (dupes.add(thumbnailurl)) {
                            newitems.add(this.createDownloadlink(thumbnailurl));
                        }
                    }
                    for (final DownloadLink newitem : newitems) {
                        newitem._setFilePackage(fp);
                        ret.add(newitem);
                        distribute(newitem);
                    }
                    if (isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break;
                    } else if (newitems.isEmpty()) {
                        /* Fail-safe */
                        logger.info("Stopping because: Failed to find any new item on this page");
                        break;
                    } else if (nextpage == null) {
                        logger.info("Stopping because: Reached last page?");
                        break;
                    } else {
                        br.getPage(nextpage);
                        page++;
                    }
                } while (true);
            } else {
                /* Website */
                final String url = URLHelper.getUrlWithoutParams(contenturl);
                /* Display as many items as possible to avoid having to deal with pagination. */
                final UrlQuery query = UrlQuery.parse(contenturl);
                /* Display all images on one page */
                query.addAndReplace("a", "10000");
                /* Start from page 1 else we may get an empty page (website is buggy). */
                query.addAndReplace("p", "1");
                br.getPage(url + "?" + query.toString());
                if (isOffline(br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String title = br.getRegex("border='0' /></a></div>(?:\\s*<h\\d+[^>]*>\\s*)?(.*?)(?:\\s*</h\\d+>\\s*)?</td></tr><tr>").getMatch(0);
                if (title == null) {
                    title = br.getRegex("<title>([^<]*?)</title>").getMatch(0);
                }
                final FilePackage fp = FilePackage.getInstance();
                if (title != null) {
                    title = Encoding.htmlDecode(title).trim();
                    title = title.replaceAll("(?i) - URLGalleries$", "");
                }
                if (!StringUtils.isEmpty(title)) {
                    fp.setName(title);
                } else {
                    /* Fallback */
                    fp.setName(br._getURL().getPath());
                }
                int page = 1;
                String nextpage = null;
                final HashSet<String> dupes = new HashSet<String>();
                do {
                    logger.info("Crawling page " + page + " of ??");
                    final ArrayList<DownloadLink> newitems = new ArrayList<DownloadLink>();
                    final String[] redirecturls = br.getRegex("rel='nofollow noopener' href='(/[^/\\']+)' target='_blank'").getColumn(0);
                    /*
                     * Check for special thumbnails that our host plugins will change to the original URLs without needing to crawl the
                     * individual urlgalleries.net URLs -> Speeds up things a lot!
                     */
                    final String[] thumbnailurls = br.getRegex("class='gallery' src='(https?://[^/]*\\.(imagevenue\\.com|fappic\\.com|imagetwist\\.com)/[^<>\"\\']+)'").getColumn(0);
                    for (final String thumbnailurl : thumbnailurls) {
                        if (dupes.add(thumbnailurl)) {
                            final DownloadLink link = this.createDownloadlink(thumbnailurl);
                            newitems.add(link);
                        }
                    }
                    if (redirecturls != null && thumbnailurls != null && redirecturls.length > thumbnailurls.length) {
                        for (String redirecturl : redirecturls) {
                            if (!dupes.add(redirecturl)) {
                                continue;
                            }
                            redirecturl = br.getURL(redirecturl).toExternalForm();
                            final DownloadLink link = this.createDownloadlink(redirecturl);
                            newitems.add(link);
                        }
                    } else {
                        logger.info("Thumbnail crawling was successful");
                    }
                    for (final DownloadLink newitem : newitems) {
                        newitem._setFilePackage(fp);
                        ret.add(newitem);
                        distribute(newitem);
                    }
                    logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
                    nextpage = br.getRegex("(" + Pattern.quote(br._getURL().getPath()) + "\\?p=" + (page + 1) + ")").getMatch(0);
                    if (isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break;
                    } else if (newitems.size() == 0) {
                        /* Fail-safe */
                        logger.info("Stopping because: Failed to find any new item on this page");
                        break;
                    } else if (nextpage == null || !dupes.add(nextpage)) {
                        logger.info("Stopping because: Reached last page?");
                        break;
                    } else {
                        br.getPage(nextpage);
                        page++;
                    }
                } while (!this.isAbort());
            }
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return ret;
        } else {
            /* Single image */
            logger.info("Crawling single image");
            br.setFollowRedirects(false);
            br.getPage(contenturl.replaceFirst("(?i)http://", "https://"));
            int counter = 0;
            String redirect = null;
            do {
                counter++;
                redirect = br.getRedirectLocation();
                if (redirect == null || !redirect.contains(this.getHost())) {
                    break;
                }
                br.getPage(redirect);
            } while (counter <= 5);
            if (isOffline(br) || br.containsHTML("/not_found_adult\\.php")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String finallink = null;
            if (redirect != null) {
                finallink = redirect;
            } else {
                finallink = br.getRegex("linkDestUrl\\s*=\\s*\\'(http[^<>\"\\']+)\\'").getMatch(0);
                if (finallink == null) {
                    /* 2023-02-17 */
                    finallink = br.getRegex("externalUrl\\s*=\\s*(?:\\'|\")(http[^<>\"\\']+)").getMatch(0);
                }
            }
            if (finallink == null) {
                /* Invalid link or plugin broken */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ret.add(this.createDownloadlink(finallink));
            return ret;
        }
    }

    private boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 | br.containsHTML("(?i)<center>\\s*This blog has been closed down") | br.containsHTML("<title> - urlgalleries\\.net</title>|>ERROR - NO IMAGES AVAILABLE") || br.getURL().contains("/not_found_adult.php");
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}