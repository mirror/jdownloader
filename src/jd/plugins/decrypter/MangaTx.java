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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MangaTx extends PluginForDecrypt {
    public MangaTx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "manga-tx.com" });
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

    private static String PATTERN_RELATIVE_CHAPTER = "/manga/([\\w\\-]+)/chapter-([0-9\\.]+)/?$";
    private static String PATTERN_RELATIVE_SERIES  = "/manga/([\\w\\-]+)/?$";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_CHAPTER + "|" + PATTERN_RELATIVE_SERIES + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Pattern patternSingleChapter = Pattern.compile("(?i)https?://[^/]+" + PATTERN_RELATIVE_CHAPTER);
        final Regex singleChapterRegex = new Regex(param.getCryptedUrl(), patternSingleChapter);
        if (singleChapterRegex.patternFind()) {
            /* Find all pages of a chapter */
            final String seriesTitleSlug = singleChapterRegex.getMatch(0);
            final String chapterNumberStr = singleChapterRegex.getMatch(1);
            final String[] pages = br.getRegex("<img[^>]+id\\s*=\\s*\"\\s*image-\\d+\\s*\"[^>]+data-src\\s*=\\s*\"\\s*([^\"]+)\\s*\"[^>]+class\\s*=\\s*\"\\s*\\s*wp-manga-chapter-img[^\"]*\"").getColumn(0);
            int pageCount = pages == null ? 0 : pages.length;
            if (pageCount <= 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String fpName = Encoding.htmlOnlyDecode(br.getRegex("<h1[^>]+id\\s*=\\s*\"\\s*chapter-heading\\s*\"[^>]*>\\s*([^<]+)(?:\\s+-\\s+Chapter \\d+)\\s*").getMatch(0));
            final String seriesTitle = seriesTitleSlug.replace("-", " ").trim();
            String[] chapters = br.getRegex("option[^>]+class\\s*=\\s*\"\\s*short\\s*\"[^>]+value\\s*=\\s*\"\\s*chapter-\\d+\"[^>]+data-redirect\\s*=\\s*\"\\s*([^\"]+)\"[^>]+>\\s*Chapter[^<]+").getColumn(0);
            if (chapters == null) {
                getLogger().warning("Unable to determine chapter count!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Eliminate duplicates */
            chapters = new HashSet<String>(Arrays.asList(chapters)).toArray(new String[0]);
            final int chapterCount = chapters.length;
            int pageNumber = 1;
            final int chapterPadlength = StringUtils.getPadLength(chapterCount);
            final int pagePadlength = StringUtils.getPadLength(pageCount);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(seriesTitle + " - Chapter " + chapterNumberStr);
            for (String page : pages) {
                final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(page));
                String page_formatted = String.format(Locale.US, "%0" + pagePadlength + "d", pageNumber++);
                String ext = getFileNameExtensionFromURL(page, ".jpg").replace("jpeg", "jpg");
                dl.setFinalFileName(seriesTitle + "_" + chapterNumberStr + "_" + page_formatted + ext);
                fp.add(dl);
                distribute(dl);
                ret.add(dl);
            }
        } else {
            /* Find all chapters of a series */
            String[] chapters = br.getRegex("<li[^>]+class\\s*=\\s*\"\\s*[^\"]*wp-manga-chapter[^\"]*\"[^>]*>\\s*<a href\\s*=\\s*\"\\s*([^\"]+)\\s*").getColumn(0);
            if (chapters == null || chapters.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String chapter : chapters) {
                final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(chapter));
                distribute(dl);
                ret.add(dl);
            }
        }
        return ret;
    }
}