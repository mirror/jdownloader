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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eventzeiger.de" }, urls = { "http://[\\w\\.]*?eventzeiger.de/photogallery.php\\?album_id=[0-9]+" }, flags = { 0 })
public class EvntZgerDe extends PluginForDecrypt {

    public EvntZgerDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.getPage(param.toString());
        String albumName = br.getRegex("capmain'>Album ansehen: (.+?)</div>").getMatch(0, 0);

        String albumid = new Regex(param, "photogallery.php\\?album_id=([0-9]+)").getMatch(0);
        br.getPage("http://www.eventzeiger.de/slideshow.php?album_id=" + albumid);

        for (String match : br.getRegex("fadeimages\\[[0-9]+\\]=\\[\"([^\"]+)\"").getColumn(0)) {
            DownloadLink dLink = createDownloadlink("http://www.eventzeiger.de/" + match);
            decryptedLinks.add(dLink);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(albumName.trim());
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
