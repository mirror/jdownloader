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

import org.appwork.utils.formatter.SizeFormatter;

/**
 *
 * @author psp
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "megadrv.com" }, urls = { "http://(?:www\\.)?megadrv\\.com/.+" }) 
public class MegadrvCom extends PluginForDecrypt {

    public MegadrvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (jd.plugins.hoster.MegadrvCom.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fid = new Regex(parameter, "megadrv\\.com/(.+)").getMatch(0);
        String fpName = fid;
        final String[] htmls = br.getRegex("class=\"file\\-container\"(.*?)class=\"report\"").getColumn(0);
        if (htmls == null || htmls.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String html : htmls) {
            String filename = new Regex(html, "class=\"name\">(?:Name: )?([^<>\"]*?)</td>").getMatch(0);
            final String filesize = new Regex(html, "class=\"size\">Size: <b>([^<>\"]*?)</b>").getMatch(0);
            String directlink = new Regex(html, "name=\"downloadToken\" value=\"(.*?)\"").getMatch(0);
            if (filename == null || filesize == null || directlink == null) {
                return null;
            }
            directlink = "http://" + this.getHost() + "/" + directlink;
            filename = Encoding.htmlDecode(filename);
            final String linkid = fid + "_" + filename;
            final DownloadLink dl = createDownloadlink("http://megadrvdecrypted.com/" + linkid);
            dl.setProperty("directname", filename);
            dl.setProperty("directsize", filesize);
            dl.setProperty("directlink", directlink);
            dl.setProperty("mainlink", parameter);
            dl.setAvailable(true);
            dl.setLinkID(linkid);
            dl.setName(filename);
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
