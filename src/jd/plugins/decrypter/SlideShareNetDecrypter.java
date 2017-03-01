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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "slideshare.net" }, urls = { "http://(?:(?:www|es|de|fr|pt)\\.)?slideshare\\.net/(?!search|business)[a-z0-9\\-_]+/[a-z0-9\\-_]+" })
public class SlideShareNetDecrypter extends PluginForDecrypt {

    public SlideShareNetDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This decrypter checks whether a document is downloadable or not. In case it's not, it will try to get the document in pictures.

    private static final String NOTDOWNLOADABLE = "class=\"sprite iconNoDownload j\\-tooltip\"";

    private static final String TYPE_INVALID    = "https?://(?:[a-z0-9]+\\.)?slideshare\\.net/(?:search|business).*?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        jd.plugins.hoster.SlideShareNet.prepBR(this.br);
        final String parameter = param.toString().replaceAll("https?://(?:[a-z0-9]+\\.)?slideshare\\.net/", "http://www.slideshare.net/");
        if (parameter.matches(TYPE_INVALID)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final DownloadLink mainlink = createDownloadlink(parameter.replace("slideshare.net/", "slidesharedecrypted.net/"));
        String filename = null;
        getUserLogin(false);
        br.getPage(parameter);
        if (jd.plugins.hoster.SlideShareNet.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (this.br.containsHTML("class=\"profileHeader\"")) {
            /* All documents/presentations/videos/info graphics of a user */
            int pagenum = 0;
            String next = null;
            do {
                logger.info("Decrypting page: " + pagenum);
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                if (pagenum > 0) {
                    this.br.getPage(next);
                }
                final String[] entries = this.br.getRegex("<a class=(?:\"|\\')notranslate(?:\"|\\') title=(?:\"|\\')[^<>\"]+(?:\"|\\') href=(?:\"|\\')(/[^<>\"]*?)(?:\"|\\')").getColumn(0);
                if (entries == null || entries.length == 0) {
                    return null;
                }

                for (String url : entries) {
                    url = "http://www.slideshare.net" + url;
                    decryptedLinks.add(this.createDownloadlink(url));
                }
                next = this.br.getRegex("href=\"(/[^<>\"]*?\\d+)\" rel=\"next\"").getMatch(0);
                pagenum++;
            } while (next != null);
        } else {
            /* Single url */
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename.trim());
            }
            /* Only decrypt pictures if the document itself isn't officially downloadable */
            if (br.containsHTML(NOTDOWNLOADABLE)) {
                if (filename == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                }
                final String[] links = br.getRegex("data\\-full=\"(https?://image\\.slidesharecdn\\.com/[^<>\"]*?)\"").getColumn(0);
                if (links != null && links.length != 0) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(filename + " (" + links.length + " pages)");
                    int counter = 1;
                    final DecimalFormat df = new DecimalFormat("0000");
                    for (final String singleLink : links) {
                        final String currentfilename = filename + "_" + df.format(counter) + ".jpg";
                        final DownloadLink dl = createDownloadlink("http://slidesharepicturedecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                        dl.setProperty("directpiclink", singleLink);
                        dl.setProperty("directname", currentfilename);
                        dl.setFinalFileName(currentfilename);
                        fp.add(dl);
                        dl.setAvailable(true);
                        decryptedLinks.add(dl);
                        counter++;
                    }
                }
            }
        }
        if (filename != null) {
            mainlink.setName(filename);
            mainlink.setAvailable(true);
        }
        decryptedLinks.add(mainlink);

        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("slideshare.net");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.SlideShareNet) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hostPlugin, aa);
        return true;
    }

}
