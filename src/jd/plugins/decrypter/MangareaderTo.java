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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MangareaderTo extends PluginForDecrypt {
    public MangareaderTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mangareader.to" });
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

    private static String PATTERN_RELATIVE_CHAPTER = "/read/([a-z0-9\\-]+)-(\\d+)/([a-z]{2})/chapter-(\\d+)";
    private static String PATTERN_RELATIVE_SERIES  = "/([a-z0-9\\-]+)-(\\d+)";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_CHAPTER + "|" + PATTERN_RELATIVE_SERIES + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Pattern patternSingleChapter = Pattern.compile("(?i)https?://[^/]+" + PATTERN_RELATIVE_CHAPTER);
        final Regex singleChapterRegex = new Regex(param.getCryptedUrl(), patternSingleChapter);
        if (singleChapterRegex.patternFind()) {
            /* Crawl all images of a chapter */
            final String chapterID = br.getRegex("data-reading-id=\"(\\d+)\"").getMatch(0);
            if (chapterID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String title = HTMLSearch.searchMetaTag(br, "og:title");
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
            }
            br.getPage("/ajax/image/list/chap/" + chapterID + "?mode=vertical&quality=high&hozPageSize=1");
            /* Double-check for offline content */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (!Boolean.TRUE.equals(entries.get("status"))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getRequest().setHtmlCode(entries.get("html").toString());
            final String[] urls = br.getRegex("data-url=\"(https?://[^\"]+)").getColumn(0);
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int page = 1;
            final int padLength = StringUtils.getPadLength(urls.length);
            for (String url : urls) {
                url = Encoding.htmlOnlyDecode(url);
                final DownloadLink image = createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                if (title != null) {
                    final String ext = Plugin.getFileNameExtensionFromURL(url);
                    if (ext != null) {
                        image.setFinalFileName(title + "_" + StringUtils.formatByPadLength(padLength, page) + ext);
                    } else {
                        image.setName(title + "_" + StringUtils.formatByPadLength(padLength, page) + ".jpg");
                    }
                }
                image.setAvailable(true);
                ret.add(image);
                page++;
            }
            if (title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
        } else {
            /* Crawl all chapter of a series */
            final String seriesID = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+" + PATTERN_RELATIVE_SERIES).getMatch(1);
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            for (final String url : urls) {
                if (new Regex(url, patternSingleChapter).patternFind() && url.contains(seriesID)) {
                    ret.add(this.createDownloadlink(url));
                }
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
