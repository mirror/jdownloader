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

import org.appwork.utils.Regex;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FestivalOhneBandsDe extends PluginForDecrypt {
    public FestivalOhneBandsDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "festivalohnebands.de" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/bilder/([\\w\\-]+/)?");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_ALL_GALLERIES  = "https?://[^/]+/bilder/?$";
    private final String TYPE_SINGLE_GALLERY = "https?://[^/]+/bilder/([\\w\\-]+)/$";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_ALL_GALLERIES)) {
            return crawlAllGalleries(param);
        } else if (param.getCryptedUrl().matches(TYPE_SINGLE_GALLERY)) {
            return crawlSingleGallery(param);
        } else {
            /* Unsupported URL --> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public ArrayList<DownloadLink> crawlAllGalleries(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            if (url.matches(TYPE_SINGLE_GALLERY)) {
                ret.add(this.createDownloadlink(url));
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlSingleGallery(final CryptedLink param) throws Exception {
        final String galleryTitleSlug = new Regex(param.getCryptedUrl(), TYPE_SINGLE_GALLERY).getMatch(0);
        if (galleryTitleSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String galleryTitleSlugClean = galleryTitleSlug.replace("-", " ").trim();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String nonce = br.getRegex("data-vc-public-nonce=\"([^\"]+)\"").getMatch(0);
        /* Cheap way of "Wordpress pagination" */
        final String[] jsons = br.getRegex("data-vc-grid-settings=\"(\\{[^\"]+)\"").getColumn(0);
        if (jsons.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String year = new Regex(galleryTitleSlug, "(?i)bilder.*(\\d{4})").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        final String galleryPath;
        if (year != null) {
            fp.setName("FoB - " + year);
            galleryPath = "FoB/" + year;
        } else {
            fp.setName("FoB - " + galleryTitleSlugClean);
            galleryPath = "FoB/" + galleryTitleSlugClean;
        }
        final HashSet<String> dupes = new HashSet<String>();
        int page = 1;
        for (final String json : jsons) {
            logger.info("Crawling page " + page + "/" + jsons.length);
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json.replace("&quot;", "\""));
            final Form form = new Form();
            form.setAction("/wp-admin/admin-ajax.php");
            form.setMethod(MethodType.POST);
            form.put("action", entries.get("action").toString());
            form.put("vc_action", "vc_get_vc_grid_data");
            form.put("tag", entries.get("tag").toString());
            form.put("data%5Bvisible_pages%5D", "5");
            form.put("data%5Bpage_id%5D", entries.get("page_id").toString());
            form.put("data%5Bstyle%5D", entries.get("style").toString());
            form.put("data%5Baction%5D", entries.get("action").toString());
            form.put("data%5Bshortcode_id%5D", entries.get("shortcode_id").toString());
            form.put("data%5Bitems_per_page%5D", entries.get("items_per_page").toString());
            form.put("data%5Btag%5D", entries.get("tag").toString());
            form.put("vc_post_id", entries.get("page_id").toString());
            form.put("_vcnonce", Encoding.urlEncode(nonce));
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("x-Requested-With", "XMLHttpRequest");
            brc.getHeaders().put("Origin", "https://" + br.getHost());
            brc.submitForm(form);
            final String[] urls = brc.getRegex("(/wp-content/uploads/[^<>\"\\']+)").getColumn(0);
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Crawled page " + page + "/" + jsons.length + " | Found items so far: " + ret.size());
            for (final String url : urls) {
                if (!dupes.add(url)) {
                    /* Skip dupes */
                    continue;
                }
                final DownloadLink image = this.createDownloadlink(br.getURL(url).toString());
                image.setRelativeDownloadFolderPath(galleryPath);
                image.setAvailable(true);
                image._setFilePackage(fp);
                ret.add(image);
                distribute(image);
            }
            if (this.isAbort()) {
                logger.info("Pagination aborted by user");
                break;
            } else {
                page++;
            }
        }
        return ret;
    }
}
