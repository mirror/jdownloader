//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filecloud.io" }, urls = { "http://(www\\.)?(ifile\\.it|filecloud\\.io)/_[a-z0-9]+" }, flags = { 0 })
public class IFileItFldr extends PluginForDecrypt {

    public IFileItFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("ifile.it/", "filecloud.io/");
        br.getPage(parameter);
        String fpName = br.getRegex("<title>(.*?)\\| filecloud\\.io</title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<legend>(.*?)</legend>").getMatch(0);
        String[] linkinformation = br.getRegex("<tr class=\"context_row\" id=\"row.*?\">(.*?)</tr>").getColumn(0);
        boolean fail = false;
        if (linkinformation == null || linkinformation.length == 0) {
            fail = true;
            linkinformation = br.getRegex("class=\"file_link\" href=\"(http.*?)\"").getColumn(0);
        }
        if (linkinformation == null || linkinformation.length == 0) return null;
        for (String info : linkinformation) {
            String filename = new Regex(info, "class=\"file_name\" id=\".*?\">(.*?)</span>").getMatch(0);
            String filesize = new Regex(info, "<td align=\"center\" valign=\"middle\"  class=\"(even|odd)\">(.*?)</td>").getMatch(1);
            String filelink = new Regex(info, "class=\"file_link\" href=\"(http.*?)\"").getMatch(0);
            if (fail) filelink = info;
            DownloadLink dl = createDownloadlink(filelink);
            if (filename != null && filesize != null) dl.setAvailable(true);
            if (filesize != null) dl.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            if (filename != null) dl.setName(filename.trim());
            if (filelink != null) {
                decryptedLinks.add(dl);
            } else {
                logger.warning("filelink for \"" + info + "\" could not be found!");
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
