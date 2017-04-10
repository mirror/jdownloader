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
import java.util.HashSet;

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
import jd.plugins.PluginException;
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
        boolean loggedIn = false;
        if (aa != null) {
            try {
                jd.plugins.hoster.PixivNet.login(this.br, aa, false);
                loggedIn = true;
            } catch (PluginException e) {
                logger.log(e);
            }
        }
        final String lid = new Regex(parameter, "id=(\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        String fpName = null;
        if (parameter.matches(TYPE_GALLERY)) {
            /* Decrypt gallery */
            br.getPage(jd.plugins.hoster.PixivNet.createGalleryUrl(lid));
            String[] links;
            if (this.br.containsHTML("指定されたIDは複数枚投稿ではありません|t a multiple-image submission<")) {
                /* Not multiple urls --> Switch to single-url view */
                br.getPage(jd.plugins.hoster.PixivNet.createSingleImageUrl(lid));
                fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)(?:\\[pixiv\\])?\">").getMatch(0);
                links = br.getRegex("data-illust-id=\"\\d+\"><img src=\"(http[^<>\"']+)\"").getColumn(0);
                if (links.length == 0) {
                    links = this.br.getRegex("data-title=\"registerImage\"><img src=\"(http[^<>\"']+)\"").getColumn(0);
                }
                if (links.length == 0) {
                    links = this.br.getRegex("data-src=\"(http[^<>\"]+)\"[^>]+class=\"original-image\"").getColumn(0);
                }
                if (links.length == 0) {
                    links = this.br.getRegex("pixiv\\.context\\.ugokuIllustData\\s*=\\s*\\{\\s*\"src\"\\s*:\\s*\"(https?.*?)\"").getColumn(0);
                }
                if (links.length == 0 && isAdultImageLoginRequired() && !loggedIn) {
                    logger.info("Adult content: Account required");
                    return decryptedLinks;
                }
            } else {
                /* Multiple urls */
                /* Check for offline */
                if (isOffline(this.br)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                } else if (isAccountOrRightsRequired(this.br) && !loggedIn) {
                    logger.info("Account required to crawl this particular content");
                    return decryptedLinks;
                } else if (isAdultImageLoginRequired() && !loggedIn) {
                    logger.info("Adult content: Account required");
                    return decryptedLinks;
                }
                fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)(?:\\[pixiv\\])?\">").getMatch(0);
                links = br.getRegex("data-filter=\"manga-image\" data-src=\"(http[^<>\"']+)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DecimalFormat df = new DecimalFormat("000");
            int counter = 1;
            for (String singleLink : links) {
                singleLink = singleLink.replaceAll("\\\\", "");
                String filename = lid + (fpName != null ? fpName : "") + "_" + df.format(counter);
                final String ext = getFileNameExtensionFromString(singleLink, jd.plugins.hoster.PixivNet.default_extension);
                filename += ext;

                final DownloadLink dl = createDownloadlink(singleLink.replaceAll("https?://", "decryptedpixivnet://"));
                dl.setProperty("mainlink", parameter);
                dl.setProperty("galleryid", lid);
                dl.setProperty("galleryurl", this.br.getURL());
                dl.setContentUrl(parameter);
                dl.setFinalFileName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                counter++;
            }
        } else {
            /* Decrypt user */
            br.getPage(parameter);
            fpName = br.getRegex("<meta property=\"og:title\" content=\"(.*?)(?:\\s*\\[pixiv\\])?\">").getMatch(0);
            if (isOffline(this.br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            } else if (isAccountOrRightsRequired(this.br) && !loggedIn) {
                logger.info("Account required to crawl this particular content");
                return decryptedLinks;
            }
            // final String total_numberof_items = this.br.getRegex("class=\"count-badge\">(\\d+) results").getMatch(0);
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
                final HashSet<String> dups = new HashSet<String>();
                final String[] links = br.getRegex("<a href=\"[^<>\"]*?[^/]+illust_id=(\\d+)\"").getColumn(0);
                for (final String galleryID : links) {
                    if (dups.add(galleryID)) {
                        final DownloadLink dl = createDownloadlink(jd.plugins.hoster.PixivNet.createGalleryUrl(galleryID));
                        decryptedLinks.add(dl);
                        distribute(dl);
                    }
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
        if (decryptedLinks.size() == 0) {
            System.out.println("debug me");
        }
        return decryptedLinks;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("この作品は削除されました。|>This work was deleted");
    }

    private boolean isAdultImageLoginRequired() {
        return this.br.containsHTML("r18=true");
    }

    public static boolean isAccountOrRightsRequired(final Browser br) {
        return br.getURL().contains("return_to=");
    }

}
