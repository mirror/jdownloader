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
import java.util.Random;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "minhateca.com.br" }, urls = { "http://(www\\.)?minhateca\\.com\\.br/.+" }, flags = { 0 })
public class MinhatecaComBr extends PluginForDecrypt {

    public MinhatecaComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);

        /* empty folder | no folder */
        if (br.containsHTML("class=\"noFile\"") || !br.containsHTML("name=\"FolderId\"|id=\"fileDetails\"")) {
            final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        /* Differ between single links and folders */
        if (br.containsHTML("id=\"fileDetails\"")) {
            String filename = br.getRegex("Biaxar: <b>([^<>\"]*?)</b>").getMatch(0);
            final String filesize = br.getRegex("class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
            final String fid = br.getRegex("name=\"FileId\" value=\"(\\d+)\"").getMatch(0);
            if (filename == null || filesize == null || fid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename).trim();
            final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));

            dl.setProperty("plain_filename", filename);
            dl.setProperty("plain_filesize", filesize);
            dl.setProperty("plain_fid", fid);
            dl.setProperty("mainlink", parameter);

            dl.setName(filename);
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setAvailable(true);

            decryptedLinks.add(dl);
        } else {
            final String fpName = br.getRegex("class=\"T_selected\">([^<>\"]*?)</span>").getMatch(0);
            final String[] linkinfo = br.getRegex("<div class=\"fileinfo tab\">(.*?)<span class=\"filedescription\"").getColumn(0);
            if (linkinfo == null || linkinfo.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String lnkinfo : linkinfo) {
                final String fid = new Regex(lnkinfo, "rel=\"(\\d+)\"").getMatch(0);
                final Regex finfo = new Regex(lnkinfo, "<span class=\"bold\">([^<>\"]*?)</span>([^<>\"]*?)</a>");
                String filename = finfo.getMatch(0);
                final String ext = finfo.getMatch(1);
                String filesize = new Regex(lnkinfo, "<li><span>([^<>\"]*?)</span></li>").getMatch(0);
                if (fid == null || filename == null || ext == null || filesize == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                filesize = Encoding.htmlDecode(filesize).trim();
                filename = Encoding.htmlDecode(filename).trim() + Encoding.htmlDecode(ext).trim();

                final DownloadLink dl = createDownloadlink("http://minhatecadecrypted.com.br/" + System.currentTimeMillis() + new Random().nextInt(1000000));

                dl.setProperty("plain_filename", filename);
                dl.setProperty("plain_filesize", filesize);
                dl.setProperty("plain_fid", fid);
                dl.setProperty("mainlink", parameter);

                dl.setName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setAvailable(true);

                decryptedLinks.add(dl);
            }

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
