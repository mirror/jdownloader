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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharingmatrix.com" }, urls = { "http://[\\w\\.]*?sharingmatrix\\.com/folder/[0-9a-z]+" }, flags = { 0 })
public class ShrngMtrxCm extends PluginForDecrypt {

    public ShrngMtrxCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        boolean failed = false;
        br.getPage(parameter);
        String[] links = br.getRegex("<td width=\"70%\" align=\"left\" valign=\"top\">(.*?)</tr>").getColumn(0);
        if (links == null || links.length == 0) {
            failed = true;
            links = br.getRegex("<td><a href=\"(http://.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(http://sharingmatrix\\.com/file/\\d+/.*?)\"").getColumn(0);
        }
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String data : links) {
            if (failed) {
                if (!data.contains("/folder/")) decryptedLinks.add(createDownloadlink(data));
            } else {
                String filename = new Regex(data, "sharingmatrix\\.com/file/.*?/(.*?)\"").getMatch(0);
                String filesize = new Regex(data, "valign=\"top\">.*?\\|.*?\\|.*?\\|.*?\\|(.*?)\\(.*?</td>").getMatch(0);
                String dlink = new Regex(data, "href=\"(http.*?)\"").getMatch(0);
                if (dlink == null) return null;
                DownloadLink aLink = createDownloadlink(dlink);
                if (filename != null) aLink.setName(filename.trim());
                if (filesize != null) aLink.setDownloadSize(Regex.getSize(filesize.trim()));
                if (filename != null && filesize != null) aLink.setAvailable(true);
                if (!dlink.contains("/folder/")) decryptedLinks.add(createDownloadlink(dlink));
            }
            progress.increase(1);
        }
        return decryptedLinks;
    }
}
