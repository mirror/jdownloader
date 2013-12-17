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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "slideshare.net" }, urls = { "http://(www\\.)?slideshare\\.net/(?!search|business)[a-z0-9\\-_]+/[a-z0-9\\-_]+" }, flags = { 0 })
public class SlideShareNetDecrypter extends PluginForDecrypt {

    public SlideShareNetDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This decrypter checks whether a document is downloadable or not. In case it's not, it will try to get the document in pictures.

    private static final String FILENOTFOUND    = ">Sorry\\! We could not find what you were looking for|>Don\\'t worry, we will help you get to the right place|<title>404 error\\. Page Not Found\\.</title>";
    private static final String NOTDOWNLOADABLE = "class=\"sprite iconNoDownload j\\-tooltip\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final DownloadLink mainlink = createDownloadlink(parameter.replace("slideshare.net/", "slidesharedecrypted.net/"));
        String filename = null;
        if (getUserLogin(false)) {
            try {
                br.getPage(parameter);
            } catch (final Exception e) {
                if (br.getHttpConnection().getResponseCode() == 410) {
                    mainlink.setAvailable(false);
                    mainlink.setProperty("offline", true);
                    decryptedLinks.add(mainlink);
                    return decryptedLinks;
                } else {
                    decryptedLinks.add(mainlink);
                    return decryptedLinks;
                }
            }
            if (br.containsHTML(FILENOTFOUND) || br.containsHTML(">Uploaded Content Removed<")) {
                mainlink.setAvailable(false);
                mainlink.setProperty("offline", true);
                decryptedLinks.add(mainlink);
                return decryptedLinks;
            }
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            if (filename != null) filename = Encoding.htmlDecode(filename.trim());
            // Only decrypt pictures if the document itself isn't downloadable
            if (br.containsHTML(NOTDOWNLOADABLE)) {
                if (filename == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                }
                final String[] links = br.getRegex("data\\-full=\"(http://image\\.slidesharecdn\\.com/[^<>\"]*?)\"").getColumn(0);
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
                        dl._setFilePackage(fp);
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
