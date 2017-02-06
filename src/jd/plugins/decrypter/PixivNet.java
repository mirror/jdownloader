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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pixiv.net" }, urls = { "https?://(?:www\\.)?pixiv\\.net/(?:member_illust\\.php\\?mode=[a-z]+\\&illust_id=\\d+|member_illust\\.php\\?id=\\d+)" })
public class PixivNet extends PluginForDecrypt {

    public PixivNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_GALLERY = ".+/member_illust\\.php\\?mode=[a-z]+\\&illust_id=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final PluginForHost hostplugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostplugin);
        jd.plugins.hoster.PixivNet.prepBR(this.br);
        if (aa != null) {
            jd.plugins.hoster.PixivNet.login(this.br, aa, false);
        }
        final String lid = new Regex(parameter, "id=(\\d+)").getMatch(0);
        br.getPage(parameter);
        if (isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        String fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)(?:\\[pixiv\\])?\">").getMatch(0);
        if (parameter.matches(TYPE_GALLERY)) {
            /* Decrypt gallery */
            br.getPage(jd.plugins.hoster.PixivNet.createGalleryUrl(lid));
            if (isOffline(this.br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String[] links = br.getRegex("data\\-filter=\"manga\\-image\" data\\-src=\"(http[^<>\"\\']+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DecimalFormat df = new DecimalFormat("000");
            int counter = 1;
            for (final String singleLink : links) {
                String filename = lid + (fpName != null ? fpName : fpName) + "_" + df.format(counter);
                final String ext = getFileNameExtensionFromString(singleLink, jd.plugins.hoster.PixivNet.default_extension);
                filename += ext;

                final DownloadLink dl = createDownloadlink(singleLink.replaceAll("https?://", "decryptedpixivnet://"));
                dl.setProperty("mainlink", parameter);
                dl.setProperty("galleryid", lid);
                dl.setFinalFileName(filename);
                decryptedLinks.add(dl);
                counter++;
            }
        } else {
            /* Decrypt user */
            br.getPage(parameter);
            if (isOffline(this.br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            // final String total_numberof_items = this.br.getRegex("class=\"count\\-badge\">(\\d+) results").getMatch(0);
            int numberofitems_found_on_current_page = 0;
            final int max_numbeofitems_per_page = 20;
            int page = 0;
            do {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                if (page > 0) {
                    this.br.getPage(String.format("/member_illust.php?id=%s&type=all&p=%s", lid, Integer.toString(page)));
                }
                if (this.br.containsHTML("No results found for your query")) {
                    break;
                }
                final String[] links = br.getRegex("class=\"image\\-item\"><a href=\"[^<>\"]*?[^/]+illust_id=(\\d+)\"").getColumn(0);
                for (final String galleryID : links) {
                    final DownloadLink dl = createDownloadlink(jd.plugins.hoster.PixivNet.createGalleryUrl(galleryID));
                    decryptedLinks.add(dl);
                    distribute(dl);
                }

                numberofitems_found_on_current_page = links.length;
                page++;
            } while (numberofitems_found_on_current_page >= max_numbeofitems_per_page);
        }

        if (fpName == null) {
            fpName = lid;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404;
    }

}
