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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 41183 $", interfaceVersion = 2, names = { "projectfreetv.ag" }, urls = { "https?://(www\\d*\\.)?projectfreetv\\.ag/(free|episode|watch)/.*" })
public class ProjectFreeTvAg extends antiDDoSForDecrypt {
    public ProjectFreeTvAg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String page = br.toString();
        String fpName = br.getRegex("name=\"description\" content=\"(?:Watch episodes online from\\s+)?([^\"]+)\\s+(?:for free|watch online)").getMatch(0);
        String[] links = br.getRegex("<a[^>]+href=\"([^\"]+)\"[^>]*>\\s*<input type=\"button\"").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("<tr><td><a href=\"([^\"]+)\"").getColumn(0);
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                if (link.startsWith("/")) {
                    link = br.getURL(link).toString();
                }
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}