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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "photo.qip.ru" }, urls = { "http://(?:www\\.)?photo\\.qip\\.ru/users/[^/]+/\\d+/(?:\\d+/?)?" }) 
public class PhotoQipRu extends PluginForDecrypt {

    public PhotoQipRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(jd.plugins.hoster.PhotoQipRu.LINKTYPE_HOSTER)) {
            decryptedLinks.add(createDownloadlink(parameter.replace("photo.qip.ru/", "photo.qipdecrypted.ru/")));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final String username = new Regex(parameter, "users/([^/]+)/").getMatch(0);

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);

        short page_max = 1;
        final String[] pages = this.br.getRegex("\\?page=(\\d+)\"").getColumn(0);
        for (final String page_temp_str : pages) {
            final short page_temp = Short.parseShort(page_temp_str);
            if (page_temp > page_max) {
                page_max = page_temp;
            }
        }

        short page = 1;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user!");
                return decryptedLinks;
            }
            this.br.getPage(this.br.getBaseURL() + "?page=" + page);
            final String[] links = br.getRegex("\"(/users/[^/]+/\\d+/\\d+/)").getColumn(0);
            if (links == null || links.length == 0) {
                break;
            }
            for (final String singleLink : links) {
                final String linkid = new Regex(singleLink, "(\\d+)/$").getMatch(0);
                final DownloadLink dl = createDownloadlink("http://photo.qipdecrypted.ru" + singleLink);
                dl.setContentUrl("http://photo.qip.ru" + singleLink);
                dl.setAvailable(true);
                dl.setName(linkid);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            page++;
        } while (page <= page_max);

        return decryptedLinks;
    }
}
