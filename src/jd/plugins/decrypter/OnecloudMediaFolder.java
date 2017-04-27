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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onecloud.media" }, urls = { "https?://(?:www\\.)?onecloud\\.media/folder/[a-f0-9]{16}\\-[a-f0-9]{16}" })
public class OnecloudMediaFolder extends PluginForDecrypt {

    public OnecloudMediaFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "([a-f0-9]{16}\\-[a-f0-9]{16})$").getMatch(0);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Thư mục kh")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (fpName == null) {
            fpName = fid;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final int max_entries_per_page = 10;
        int last_numberof_entries_per_page;
        int page = 1;
        do {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            logger.info("Decrypting page: " + page);
            if (page > 1) {
                this.br.getPage("/folder/" + fid + "?page=" + page);
            }
            final String[] links = br.getRegex("(/file/[a-f0-9]{16}\\-[a-f0-9]{16})").getColumn(0);
            if (links == null || links.length == 0) {
                break;
            }
            for (final String singleLink : links) {
                final DownloadLink dl = createDownloadlink("http://" + this.getHost() + singleLink);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            last_numberof_entries_per_page = links.length;
            page++;
        } while (last_numberof_entries_per_page >= max_entries_per_page);

        return decryptedLinks;
    }

}
