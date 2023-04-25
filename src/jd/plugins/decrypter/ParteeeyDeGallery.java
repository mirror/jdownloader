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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ParteeeyDe;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "parteeey.de" }, urls = { "https?://(?:www\\.)?parteeey\\.de/galerie/(?:uploads/)?[A-Za-z0-9\\-_]+\\-(\\d+)" })
public class ParteeeyDeGallery extends PluginForDecrypt {
    public ParteeeyDeGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String galleryID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (galleryID == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        if (aa == null) {
            /* Account required to access their content */
            throw new AccountRequiredException();
        }
        final ParteeeyDe hostPlugin = (ParteeeyDe) this.getNewPluginForHostInstance(this.getHost());
        /*
         * Show 1000 links per page --> Usually we'll only get one page no matter how big a gallery is and big galleries will usually only
         * have to up to 150 items.
         */
        final String galleryURL = param.getCryptedUrl() + "?oF=f.date&oD=asc&eP=1000";
        hostPlugin.login(aa, galleryURL, true);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML(">\\s*Seite nicht gefunden\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlGalleryName = new Regex(galleryURL, "(?i)parteeey\\.de/galerie/(.+)").getMatch(0);
        String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null && urlGalleryName != null) {
            /* Fallback */
            fpName = urlGalleryName.replace("_", " ");
        }
        if (fpName == null) {
            /* Last resort fallback. */
            fpName = galleryID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName).trim());
        int page_int_max = 1;
        final String[] pages = br.getRegex("\\?p=(\\d+)\">\\d+").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String page_str : pages) {
                final int page_int = Integer.parseInt(page_str);
                if (page_int > page_int_max) {
                    page_int_max = page_int;
                }
            }
        }
        int counter = 1;
        final DecimalFormat df = new DecimalFormat("0000");
        for (int i = 1; i <= page_int_max; i++) {
            logger.info("Crawling page " + i + " / " + page_int_max);
            if (i > 1) {
                br.getPage(galleryURL + "&p=" + i);
            }
            /* Grab thumbnails and build finallinks --> Is very fast */
            final String[] htmls = this.br.getRegex("<div class=\"thumbnail\">(.*?)page\\-list\\-thumb\\-info").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                break;
            }
            for (final String html : htmls) {
                String photoID = new Regex(html, "filId:[\t\n\r ]*?(\\d+)").getMatch(0);
                if (photoID == null) {
                    photoID = new Regex(html, "handleClick\\((\\d+)").getMatch(0);
                }
                if (photoID == null) {
                    photoID = new Regex(html, "datei\\?p=(\\d+)").getMatch(0);
                }
                String urlThumb = new Regex(html, "img data\\-src=\"(tmp/[^<>\"]*?)\"").getMatch(0);
                if (urlThumb == null) {
                    urlThumb = new Regex(html, "<img data\\-src=\"(http[^<>\"]+)\"").getMatch(0);
                }
                if (urlThumb == null) {
                    urlThumb = new Regex(html, "\"(https?://[^<>\"]*/thumbnails/[^<>\"]*)\"").getMatch(0);
                }
                if (urlThumb == null) {
                    /* 2021-06-14 */
                    urlThumb = new Regex(html, "(/thumb\\.php\\?f=[^<>\"\\']+)").getMatch(0);
                }
                if (photoID == null) {
                    return null;
                }
                // final String finallink = "directhttp://https://www.parteeey.de/files/mul/galleries/" + gal_ID + "/" + url_fname;
                final String url_fname = jd.plugins.hoster.ParteeeyDe.getFilenameFromThumbnailDirecturl(urlThumb);
                String finalname;
                if (url_fname != null) {
                    finalname = df.format(counter) + "_" + url_fname;
                } else {
                    finalname = df.format(counter) + "_" + photoID;
                }
                if (!finalname.endsWith(jd.plugins.hoster.ParteeeyDe.default_extension)) {
                    finalname += jd.plugins.hoster.ParteeeyDe.default_extension;
                }
                final String contenturl = "https://www." + this.getHost() + "/#mulFile-" + photoID;
                final DownloadLink dl = createDownloadlink(contenturl);
                dl.setName(finalname);
                dl.setProperty("decrypterfilename", finalname);
                dl.setProperty("thumburl", urlThumb);
                dl.setProperty("galleryid", galleryID);
                dl.setContentUrl(contenturl);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                ret.add(dl);
                this.distribute(dl);
                counter++;
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            }
        }
        if (ret.size() == 0) {
            /* WTF this should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
