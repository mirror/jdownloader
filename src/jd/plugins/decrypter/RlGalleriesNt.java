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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.GetRequest;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "urlgalleries.net" }, urls = { "https?://(?:[a-z0-9_\\-]+\\.)?urlgalleries\\.net/(porn-gallery-\\d+/.+|blog_gallery\\.php\\?id=\\d+.+)|https?://go\\.urlgalleries\\.net/[a-z0-9]+" })
public class RlGalleriesNt extends PluginForDecrypt {
    private static String agent = null;

    public RlGalleriesNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedurl = param.getCryptedUrl().replace("http://", "https://");
        br.setReadTimeout(3 * 60 * 1000);
        // br.setCookie(".urlgalleries.net", "popundr", "1");
        if (agent == null) {
            agent = UserAgents.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setFollowRedirects(true);
        final String galleryID = new Regex(addedurl, "(?:porn-gallery-|blog_gallery\\.php\\?id=)(\\d+)").getMatch(0);
        if (galleryID != null) {
            /* Gallery */
            logger.info("Crawling gallery");
            final String domain = Browser.getHost(addedurl, true);
            /* Display as many items as possible to avoid having to deal with pagination. */
            // br.getPage(parameter);
            br.getPage(new GetRequest(new URL(String.format("https://%s/porn-gallery-%s//&a=1000", domain, galleryID))));
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
                int numberofNewItems = 0;
                final String[] redirecturls = br.getRegex("rel='nofollow noopener' href='(/[^/\\']+)' target='_blank'").getColumn(0);
                /*
                 * Check for imagevenue thumbnails which we can change to the original URLs without needing to crawl the individual
                 * urlgalleries.net URLs -> Saves a lot of time
                 */
                final String[] thumbnailurls = br.getRegex("class='gallery' src='(https?://[^/]*\\.imagevenue\\.com/[^<>\"\\']+)'").getColumn(0);
                for (final String thumbnailurl : thumbnailurls) {
                    if (dupes.add(thumbnailurl)) {
                        final DownloadLink link = this.createDownloadlink(thumbnailurl);
                        link._setFilePackage(fp);
                        ret.add(link);
                        numberofNewItems++;
                    }
                }
                if (redirecturls.length > thumbnailurls.length) {
                    for (String redirecturl : redirecturls) {
                        if (dupes.add(redirecturl)) {
                            redirecturl = br.getURL(redirecturl).toString();
                            final DownloadLink link = this.createDownloadlink(redirecturl);
                            link._setFilePackage(fp);
                            ret.add(link);
                            numberofNewItems++;
                        }
                    }
                } else {
                    logger.info("Thumbnail crawling was successful");
                }
                nextpage = br.getRegex("(" + Pattern.quote(br._getURL().getPath()) + "\\?p=" + (page + 1) + ")").getMatch(0);
                if (isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else if (numberofNewItems == 0) {
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
            return ret;
        } else {
            /* Single image */
            logger.info("Crawling single image");
            br.setFollowRedirects(false);
            br.getPage(addedurl);
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
            if (isOffline(br) || br.containsHTML("/not_found_adult\\.php") || (redirect != null && redirect.contains(this.getHost()))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String finallink;
            if (redirect != null) {
                finallink = redirect;
            } else {
                finallink = br.getRegex("linkDestUrl\\s*=\\s*\\'(http[^<>\"\\']+)\\'").getMatch(0);
            }
            if (finallink == null) {
                return null;
            }
            ret.add(this.createDownloadlink(finallink));
            return ret;
        }
    }

    private boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 | br.containsHTML("(?i)<center>\\s*This blog has been closed down") | br.containsHTML("<title> - urlgalleries\\.net</title>|>ERROR - NO IMAGES AVAILABLE") || br.getURL().contains("/not_found_adult.php");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}