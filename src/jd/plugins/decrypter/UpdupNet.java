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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "updup.net" }, urls = { "https?://updup\\.net/reDirect/[A-Za-z0-9]+/\\d+|https?://(?:www\\.)?updup\\.net/[A-Za-z0-9]+" })
public class UpdupNet extends PluginForDecrypt {

    public UpdupNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        if (parameter.matches(".+/reDirect/.+")) {
            /* Single redirect url */
            this.br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() < 20) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final DownloadLink dl = decryptSingle(this.br);
            if (dl == null) {
                return null;
            }
            decryptedLinks.add(dl);
        } else {
            /* Multiple reedirect urls */
            this.br.setFollowRedirects(true);
            br.getPage(parameter);
            if (isOffline(this.br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String fpName = getFilename(this.br);
            FilePackage fp = null;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
            }

            final String[] links = br.getRegex("(/reDirect/[^/]+/\\d+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            final Browser brc = this.br.cloneBrowser();
            brc.setFollowRedirects(false);
            for (final String singleLink : links) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                brc.getPage(singleLink);
                final DownloadLink dl = decryptSingle(brc);
                if (dl == null) {
                    return null;
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                decryptedLinks.add(dl);
                distribute(dl);
            }

            if (fp != null) {
                fp.addLinks(decryptedLinks);
            }
            /* Check if a directlink is available --> Add it to hosterplugin! */
            if (this.br.containsHTML("class=mainDirectLink")) {
                final DownloadLink main = this.createDownloadlink(parameter.replace("updup.net/", "updupdecrypted.net/"));
                main.setAvailableStatus(jd.plugins.hoster.UpdupNet.requestFileInformationStatic(main, br));
                decryptedLinks.add(main);
            }
        }

        return decryptedLinks;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=notfound") || !br.containsHTML("class=mirrorlink");
    }

    public static String getFilename(final Browser br) {
        String filename = br.getRegex("id=filename><h1>([^<>\"]+)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("property=og:description content=\"Download file ([^<>\"\\']+)\"").getMatch(0);
        }
        return filename;
    }

    private DownloadLink decryptSingle(final Browser br) {
        final String finallink = br.getRedirectLocation();
        if (finallink == null || finallink.contains(this.getHost())) {
            return null;
        }
        final DownloadLink dl = createDownloadlink(finallink);
        distribute(dl);
        return dl;
    }

}
