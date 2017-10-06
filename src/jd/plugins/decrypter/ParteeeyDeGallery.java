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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "parteeey.de" }, urls = { "https?://(?:www\\.)?parteeey\\.de/galerie/(?:uploads/)?[A-Za-z0-9\\-_]*?\\d+$" })
public class ParteeeyDeGallery extends PluginForDecrypt {
    public ParteeeyDeGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String galleryID = new Regex(param.toString(), "(\\d+)$").getMatch(0);
        if (galleryID == null) {
            final DownloadLink offline = this.createOfflinelink(param.toString());
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        if (aa == null) {
            return decryptedLinks;
        }
        try {
            jd.plugins.hoster.ParteeeyDe.login(this.br, aa);
        } catch (final Throwable e) {
            logger.info("Login failure");
            return decryptedLinks;
        }
        /* Show 1000 links per page --> Usually we'll only get one page no matter how big a gallery is. */
        final String parameter = param.toString() + "?oF=f.date&oD=asc&eP=1000";
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">Seite nicht gefunden<")) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String url_name = new Regex(parameter, "parteeey\\.de/galerie/(.+)").getMatch(0);
        String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            /* Packagename fallback */
            fpName = url_name;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
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
            if (this.isAbort()) {
                logger.info("User aborted decryption for link: " + parameter);
                return decryptedLinks;
            }
            logger.info("Decrypting page " + i + " / " + page_int_max);
            if (i > 1) {
                br.getPage(parameter + "&p=" + i);
            }
            /* Grab thumbnails and build finallinks --> Is very fast */
            final String[] htmls = this.br.getRegex("<div class=\"thumbnail\">(.*?)page\\-list\\-thumb\\-info").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                break;
            }
            for (final String html : htmls) {
                String linkid = new Regex(html, "filId:[\t\n\r ]*?(\\d+)").getMatch(0);
                if (linkid == null) {
                    linkid = new Regex(html, "handleClick\\((\\d+)").getMatch(0);
                }
                if (linkid == null) {
                    linkid = new Regex(html, "datei\\?p=(\\d+)").getMatch(0);
                }
                String url_thumb = new Regex(html, "img data\\-src=\"(tmp/[^<>\"]*?)\"").getMatch(0);
                if (url_thumb == null) {
                    url_thumb = new Regex(html, "<img data\\-src=\"(http[^<>\"]+)\"").getMatch(0);
                }
                if (linkid == null) {
                    return null;
                }
                // final String finallink = "directhttp://https://www.parteeey.de/files/mul/galleries/" + gal_ID + "/" + url_fname;
                final String url_fname = jd.plugins.hoster.ParteeeyDe.getFilenameFromDirecturl(url_thumb);
                String finalname;
                if (url_fname != null) {
                    finalname = df.format(counter) + "_" + url_fname;
                } else {
                    finalname = df.format(counter) + "_" + linkid;
                }
                if (!finalname.endsWith(jd.plugins.hoster.ParteeeyDe.default_extension)) {
                    finalname += jd.plugins.hoster.ParteeeyDe.default_extension;
                }
                final String contenturl = "https://www.parteeey.de/#mulFile-" + linkid;
                final DownloadLink dl = createDownloadlink(contenturl);
                dl.setName(finalname);
                dl.setProperty("decrypterfilename", finalname);
                dl.setProperty("thumburl", url_thumb);
                dl.setContentUrl(contenturl);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                this.distribute(dl);
                counter++;
            }
        }
        if (decryptedLinks.size() == 0) {
            /* WTF, empty galleries should NOT exist! */
            logger.warning("Gallery empty or fatal crawler failure");
            return null;
        }
        return decryptedLinks;
    }
}
