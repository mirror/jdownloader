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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ultrastar-warez.com" }, urls = { "http://(www\\.)?ultrastar-warez\\.com/index\\.php\\?section=download\\&cat=\\d+\\&id=\\d+" }, flags = { 0 })
public class LtrstrWzCom extends PluginForDecrypt {

    public LtrstrWzCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("(>Uploaded: 31\\.12\\.69 \\(23:00\\)<|class=\"title\"> - </span></td>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<span class=\"title\">(.*?)</span>").getMatch(0);
        String mirrors[] = br.getRegex("<b>Download (\\d+)?:</b></td>[\t\r\n ]+<td colspan=\"4\" align=\"left\">(.*?)</td>").getColumn(1);
        if (mirrors == null || mirrors.length == 0) return null;
        for (String mirror : mirrors) {
            String[] links = HTMLParser.getHttpLinks(mirror, "");
            if (links == null || links.length == 0) return null;
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
