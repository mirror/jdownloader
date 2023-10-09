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
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "lhtranslation.net" }, urls = { "https?://(?:www\\.)?lhtranslation\\.(?:com|net)/manga/([a-z0-9\\-]+)/(chapter-([0-9\\-]+)/?)?" })
public class Lhtranslation extends PluginForDecrypt {
    public Lhtranslation(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    /* Tags: MangaPictureCrawler */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String extension_fallback = ".jpg";
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String url_mangaTitle = urlinfo.getMatch(0);
        final String url_chapterNumberStr = urlinfo.getMatch(2);
        if (url_chapterNumberStr == null) {
            /* Crawl all chapters */
            final String realMangaURL = br.getURL();
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // br.getHeaders().put("", "");
            // br.getHeaders().put("", "");
            br.postPage("/manga/" + url_mangaTitle + "/ajax/chapters/", "");
            final String[] chapters = br.getRegex("(" + org.appwork.utils.Regex.escape(realMangaURL) + "/?chapter-\\d+/?)").getColumn(0);
            logger.info("Found " + chapters.length + " chapters");
            if (chapters.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String url : chapters) {
                ret.add(this.createDownloadlink(url));
            }
        } else {
            /* Crawl single chapter */
            String title_chapter = br.getRegex("<h1><font color=\"white\">([^<>\"]+) Chapter \\d+</font></h1>").getMatch(0);
            String ext = null;
            final String[] images = br.getRegex("id=\"image-\\d+\" data-src=\"\\s*(https?://[^<>\"\\']+)").getColumn(0);
            if (images.length == 0) {
                /* Assume that content is offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (title_chapter == null) {
                /* Fallback */
                title_chapter = url_mangaTitle;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title_chapter + "_" + url_chapterNumberStr);
            final int padLength = StringUtils.getPadLength(images.length);
            int page = 0;
            for (final String finallink : images) {
                page++;
                final String page_formatted = String.format(Locale.US, "%0" + padLength + "d", page);
                if (finallink == null) {
                    return null;
                }
                ext = getFileNameExtensionFromURL(finallink, extension_fallback);
                if (ext == null) {
                    ext = extension_fallback;
                }
                final String filename = title_chapter + "_" + url_chapterNumberStr + "_" + page_formatted + ext;
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl._setFilePackage(fp);
                dl.setFinalFileName(filename);
                dl.setLinkID(filename);
                dl.setAvailable(true);
                ret.add(dl);
                distribute(dl);
            }
        }
        return ret;
    }
}
