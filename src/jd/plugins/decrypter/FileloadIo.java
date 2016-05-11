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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fileload.io" }, urls = { "https?://(?:www\\.)?fileload\\.io/[A-Za-z0-9]+" }, flags = { 0 })
public class FileloadIo extends PluginForDecrypt {

    public FileloadIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /*
     * This crawler is VERY basic because it is not clear whether this service will gain any popularity and maybe there will be an API in
     * the future!
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (jd.plugins.hoster.FileloadIo.mainlinkIsOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (this.br.containsHTML("Please wait a moment while the files are being prepared for download|The page will reload automatically once the files are ready")) {
            /* Happens directly after uploading new files to this host. */
            logger.info("There is nothing to download YET ...");
            return decryptedLinks;
        }
        final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        final String[] fileids = br.getRegex("data\\-fileid=\"(\\d+)\"").getColumn(0);
        if (fileids == null || fileids.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String linkid : fileids) {
            final String link = "https://fileloaddecrypted.io/" + folderid + "/s/" + linkid;
            final String internal_linkid = folderid + "_" + linkid;
            final DownloadLink dl = createDownloadlink(link);
            /* No single links aavailable for users! */
            dl.setContentUrl(parameter);
            dl.setName(internal_linkid);
            dl.setLinkID(internal_linkid);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName("fileload.io folder " + folderid);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
