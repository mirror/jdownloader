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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "webshare.cz" }, urls = { "https?://(?:www\\.)?webshare\\.cz/#/folder/[a-z0-9]{8,}" }) 
public class WebShareCzFolder extends PluginForDecrypt {

    public WebShareCzFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderid);

        int offset = 0;
        final int maxItemsPerPage = 100;
        int decryptedItems = 0;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            decryptedItems = 0;
            this.br.postPage("https://" + this.getHost() + "/api/folder/", "ident=" + folderid + "&offset=" + offset + "&limit=" + maxItemsPerPage + "&wst=");
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Folder not found")) {
                /*
                 * <response><status>FATAL</status><code>FOLDER_FATAL_1</code><message>Folder not
                 * found.</message><app_version>26</app_version></response>
                 */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String[] xmls = br.getRegex("<file>(.*?)</file>").getColumn(0);
            if (xmls == null || xmls.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleXML : xmls) {
                final String fileid = new Regex(singleXML, "<ident>([^<>\"]+)</ident>").getMatch(0);
                final String filesize = new Regex(singleXML, "<size>([^<>\"]+)</size>").getMatch(0);
                final String filename = new Regex(singleXML, "<name>([^<>\"]+)</name>").getMatch(0);

                final String content_url = "https://webshare.cz/#/file/" + fileid;
                final DownloadLink dl = createDownloadlink(content_url);
                dl.setContentUrl(content_url);
                dl.setLinkID(fileid);
                dl.setName(filename);
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                decryptedItems++;
                offset++;
            }
        } while (decryptedItems >= maxItemsPerPage);

        return decryptedLinks;
    }

}
