//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "googlegroups.com" }, urls = { "http://groups.google.com/group/[^/]+/files/?" }, flags = { 0 })
public class GglGrps extends PluginForDecrypt {

    public GglGrps(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.getPage(param.toString());
        String[][] infos = br.getRegex("<td class=\"namecol\">.*?<a.*?href=\"(.*?)\">(.*?)</a>.*?<td class=\"sizecol\">(.*?)</td>").getMatches();
        for (String[] strings : infos) {
            DownloadLink dl = createDownloadlink(strings[0]);
            dl.setName(strings[1]);
            dl.setDownloadSize(SizeFormatter.getSize(strings[2]));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    // @Override

}
