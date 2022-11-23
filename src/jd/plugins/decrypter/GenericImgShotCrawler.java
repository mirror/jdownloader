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
import java.util.List;

import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericImgShot;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { GenericImgShot.class })
public class GenericImgShotCrawler extends PluginForDecrypt {
    public GenericImgShotCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return GenericImgShot.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(gallery|user)-(\\d+)\\.html(\\?p=\\d+)?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"message warn\"")) {
            /*
             * E.g. <div class="message warn"
             * style="display: inline-block; padding: 15px 10px; background: #ffd; border: 1px solid #aa9; margin: 10px 0 30px">This is a
             * private gallery</div>
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<h1[^>]*>([^<]+)</h1>").getMatch(0);
        FilePackage fp = null;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
        } else {
            /* Fallback */
            final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
            final String urlType = urlinfo.getMatch(0);
            final String itemID = urlinfo.getMatch(1);
            fp = FilePackage.getInstance();
            fp.setName(urlType + " - " + itemID);
        }
        final UrlQuery query = UrlQuery.parse(param.getCryptedUrl());
        final String forcedPageStr = query.get("p");
        int forcedPage = forcedPageStr != null ? Integer.parseInt(forcedPageStr) : -1;
        int page;
        if (forcedPageStr != null && forcedPageStr.matches("\\d+")) {
            forcedPage = Integer.parseInt(forcedPageStr);
            page = forcedPage;
        } else {
            page = 0; // starts from 0 on website
        }
        int pageMax = -1;
        /* Find max page value */
        final String[] pageNumbersStr = br.getRegex("p=(\\d+)").getColumn(0);
        for (final String pageNumberStr : pageNumbersStr) {
            final int pageNumberInt = Integer.parseInt(pageNumberStr);
            if (pageNumberInt > pageMax) {
                pageMax = pageNumberInt;
            }
        }
        final String urlWithoutParams = URLHelper.getUrlWithoutParams(br._getURL());
        final HashSet<String> dupes = new HashSet<String>();
        do {
            int numberofNewItems = 0;
            final String[] htmls = br.getRegex("<div class='all_images'.*?/></a></div></div>").getColumn(-1);
            if (htmls == null || htmls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String html : htmls) {
                final String thumbnailurl = new Regex(html, "(https?://[^/]+/images/small/[^<>\"\\']+)").getMatch(0);
                String url = new Regex(html, "(/img-[a-z0-9]+\\.html)").getMatch(0);
                if (url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (!dupes.add(url)) {
                    continue;
                }
                numberofNewItems++;
                url = br.getURL(url).toString();
                final DownloadLink image = createDownloadlink(url);
                /* This can be useful later to create directurls out of thumbnailurls by changing "/small" to "/big/". */
                if (thumbnailurl != null) {
                    image.setProperty(GenericImgShot.PROPERTY_THUMBNAILURL, thumbnailurl);
                    image.setName(Plugin.getFileNameFromURL(new URL(thumbnailurl)));
                } else {
                    logger.warning("Failed to find thumbnailurl");
                }
                image.setAvailable(true);
                if (fp != null) {
                    image._setFilePackage(fp);
                }
                ret.add(image);
                distribute(image);
            }
            logger.info("Crawled page " + page + "/" + pageMax + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopped because: Aborted by user");
                break;
            } else if (page >= pageMax) {
                logger.info("Stopping because: Reached last page: " + pageMax);
                break;
            } else if (numberofNewItems == 0) {
                /* Additional fail-safe */
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (forcedPage != -1) {
                logger.info("Stopping because: Crawled user defined page: " + forcedPage);
                break;
            } else {
                page++;
                br.getPage(urlWithoutParams + "?p=" + page);
            }
        } while (true);
        return ret;
    }
}
