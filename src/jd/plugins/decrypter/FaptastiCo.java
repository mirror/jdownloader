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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "faptasti.com" }, urls = { "https://(www\\.)?faptasti\\.co/gallery/\\d+/[^/]+" }, flags = { 0 })
public class FaptastiCo extends PluginForDecrypt {

    public FaptastiCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>Faptasti\\.co \\- ([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "/([^/]+)$").getMatch(0);
        }

        int maxPage = 1;

        final String[] pagenumbers = br.getRegex("/pagenumber/(\\d+)\"").getColumn(0);
        if (pagenumbers != null) {
            for (final String pgnum : pagenumbers) {
                final int apage = Integer.parseInt(pgnum);
                if (apage > maxPage) {
                    maxPage = apage;
                }
            }
        }

        for (int i = 1; i <= maxPage; i++) {
            br.getPage(parameter + "/pagenumber/" + i);
            final String[] links = br.getRegex("<img src=\"(/image/thumbnail/[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links) {
                singleLink = singleLink.replace("/thumbnail/", "/");
                final DownloadLink dl = createDownloadlink("directhttp://https://faptasti.co" + singleLink);
                /* Server does not return filesize anyways */
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }
}
