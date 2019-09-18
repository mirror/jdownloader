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

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shaanig.se" }, urls = { "https?://(www\\.)?(?:shaanig\\.se|prfrtv\\.co)/(?:series|tvshows|episodes?|movies)/.+" })
public class Shaanig extends antiDDoSForDecrypt {
    public Shaanig(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta (?:name|property)=\"og:title\" content=[\"'](?:Watch ?)?([^<>\"]*?)(?: Free Online \\| Shaanig)?[\"'] ?/>").getMatch(0);
        String linkBlock = br.getRegex("(<div id=[\'\"]episodes[\'\"].+)<div id=[\'\"](?:cast|trailer|info)[\'\"]").getMatch(0);
        String[] links = null;
        if (linkBlock == null || linkBlock.length() == 0) {
            linkBlock = br.getRegex("(<div class=[\'\"]links_table[\'\"].+)<div class=[\'\"]sbox[\'\"]").getMatch(0);
        }
        if (linkBlock != null && linkBlock.length() > 0) {
            links = new Regex(linkBlock, "href=[\'\"]([^\'\"]+/(?:episodes|links)/[^\'\"]+)[\'\"]").getColumn(0);
        }
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}