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

@DecrypterPlugin(revision = "$Revision: 13393 $", interfaceVersion = 2, names = { "hi-living.de" }, urls = { "http://[\\w\\.]*?hi-living.de/galerie/thumbnails.php\\?album=[0-9]+" }, flags = { 0 })
public class HiLvng extends PluginForDecrypt {

    public HiLvng(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        // This site is using a somehow modified CopperMine gallery which allows
        // us to get data from the AJAX-Slideshow feature

        String albumid = new Regex(param, "thumbnails.php\\?album=([0-9]+)").getMatch(0);

        br.getPage("http://www.hi-living.de/galerie/thumbnails.php?album=" + albumid);
        String albumName = br.getRegex("<title>(.+?) - hi-living Galerie</title>").getMatch(0, 0);
        int imageCount = Integer.valueOf(br.getRegex("([0-9]+) Dateien auf [0-9]+ Seite\\(n\\)</td>").getMatch(0, 0));
        progress.setRange(imageCount);

        // Threading would give a significant speedup, but lets spend our time
        // on more important things.
        for (int i = 0; i < imageCount; i++) {
            br.getPage("http://www.hi-living.de/galerie/displayimage.php?album=" + albumid + "&ajax_show=1&pos=" + i);
            String url = "http://www.hi-living.de/galerie/" + br.getRegex("\"url\":\"(.+?)\"").getMatch(0, 0).replace("\\/", "/");
            decryptedLinks.add(createDownloadlink(url));
            progress.increase(1);
        }

        FilePackage fp = FilePackage.getInstance();
        fp.setName(albumName.trim());
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
