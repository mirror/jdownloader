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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ah-me.com" }, urls = { "http://(www\\.)?ah\\-me\\.com/pics/gallery/\\d+/\\d+/" }, flags = { 0 })
public class AhMeComGallery extends PluginForDecrypt {

    public AhMeComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("class=\"gal_thumbs spec_right\">[\t\n\r ]+</div>")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h2>([^<>\"]*?)</h2>").getMatch(0);
        if (fpName == null) {
            fpName = "ah-me.com gallery " + new Regex(parameter, "(\\d+)/\\d+/").getMatch(0);
        }
        final String[] links = br.getRegex("\"(http://ahbigpics\\.fuckandcdn\\.com/work/[A-Za-z0-9\\-_]+/\\d+/[A-Za-z0-9\\-_]+\\.jpg)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final String relevantpart = new Regex(singleLink, "(/\\d+/[A-Za-z0-9\\-_]+\\.jpg)$").getMatch(0);
            final DownloadLink dl = createDownloadlink("directhttp://http://ahbigpics.fuckandcdn.com/work/orig/" + relevantpart);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
